package org.turbanov.actions;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.ExecutionTargetManager;
import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.ide.macro.MacroManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
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
    private final AtomicInteger counter = new AtomicInteger(1);

    public RunConfigurationAsAction(@NotNull String runConfigurationName, @NotNull String executorId, @Nullable Icon icon, @NotNull String text, @Nullable String executionTargetId) {
        super(text, null, icon);
        this.runConfigurationName = runConfigurationName;
        this.executorId = executorId;
        this.executionTargetId = executionTargetId;
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
        MacroManager.getInstance().cacheMacrosPreview(e.getDataContext());
        ExecutionUtil.doRunConfiguration(runConfig, executor, target, null, e.getDataContext());
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
