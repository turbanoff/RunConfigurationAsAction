package org.turbanov.actions;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.ExecutionTargetManager;
import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.ExecutorRegistryImpl;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.compound.CompoundRunConfiguration;
import com.intellij.execution.compound.SettingsAndEffectiveTarget;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.ide.macro.MacroManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Andrey Turbanov
 * @since 27.01.2017
 */
public class RunConfigurationAsAction extends AnAction {

    private static final Logger log = Logger.getInstance(RunConfigurationAsAction.class);

    private final String runConfigurationName;
    private final String executorId;
    private final String executionTargetId;
    private final int baselineVersion;
    private final AtomicInteger counter = new AtomicInteger(1);

    public RunConfigurationAsAction(@NotNull String runConfigurationName, @NotNull String executorId, @Nullable Icon icon, @NotNull String text, @Nullable String executionTargetId, int baselineVersion) {
        super(text, null, icon);
        this.runConfigurationName = runConfigurationName;
        this.executorId = executorId;
        this.executionTargetId = executionTargetId;
        this.baselineVersion = baselineVersion;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        RunManagerEx runManager = RunManagerEx.getInstanceEx(project);
        RunnerAndConfigurationSettings runConfig = runManager.findConfigurationByName(runConfigurationName);

        if (runConfig == null) {
            log.info("Unable to find Run configuration with name: " + runConfigurationName + " in project " + project);
            return;
        }

        Executor executor = ExecutorRegistry.getInstance().getExecutorById(executorId);
        if (executor == null) {
            log.info("Unable to find Executor by id: " + executorId);
            return;
        }

        ExecutionTarget target = getExecutionTarget(project, runConfig);

        if (baselineVersion < 232) { //not needed for 2023.2+
            https://github.com/JetBrains/intellij-community/commit/76170089e6b2521942ddf54b53606d5fd4956b33
            MacroManager.getInstance().cacheMacrosPreview(e.getDataContext());
        }

        // Copy of 'com.intellij.execution.ExecutorRegistryImpl.RunnerHelper.runSubProcess'
        RunConfiguration configuration = runConfig.getConfiguration();
        DataContext dataContext = e.getDataContext();

        if (configuration instanceof CompoundRunConfiguration) {
            for (SettingsAndEffectiveTarget settingsAndEffectiveTarget : ((CompoundRunConfiguration) configuration)
                    .getConfigurationsWithEffectiveRunTargets()) {
                RunConfiguration subConfiguration = settingsAndEffectiveTarget.getConfiguration();
                ExecutorRegistryImpl.RunnerHelper.run(project, subConfiguration, runManager.findSettings(subConfiguration), dataContext, executor);
            }
        } else {
            ExecutionEnvironmentBuilder builder = ExecutionEnvironmentBuilder.createOrNull(executor, runConfig);
            if (builder == null) {
                return;
            }
            ExecutionEnvironment environment = builder.target(target).dataContext(dataContext).build();
            ExecutionManager.getInstance(project).restartRunProfile(environment);
        }
    }

    @NotNull
    private ExecutionTarget getExecutionTarget(@NotNull Project project, @NotNull RunnerAndConfigurationSettings runConfig) {
        ExecutionTargetManager targetManager = ExecutionTargetManager.getInstance(project);
        ExecutionTarget active = targetManager.getActiveTarget();
        if (executionTargetId == null || executionTargetId.equals(active.getId())) {
            return active; //use selected as is
        }

        List<ExecutionTarget> targets = targetManager.getTargetsFor(runConfig.getConfiguration());
        for (ExecutionTarget target : targets) {
            if (target.getId().equals(executionTargetId)) {
                return target;
            }
        }
        return active; //fallback to active
    }

    public void register() {
        counter.incrementAndGet();
    }

    public int unregister() {
        return counter.decrementAndGet();
    }
}
