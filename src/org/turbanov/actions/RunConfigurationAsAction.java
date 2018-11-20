package org.turbanov.actions;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.ExecutionTargetManager;
import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

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

        selectRequiredExecutionTarget(project, runConfig);

        ProgramRunnerUtil.executeConfiguration(project, runConfig, executor);
    }

    private void selectRequiredExecutionTarget(@NotNull Project project, @NotNull RunnerAndConfigurationSettings runConfig) {
        if (executionTargetId == null) {
            return; //use selected as is
        }
        ExecutionTargetManager targetManager = ExecutionTargetManager.getInstance(project);
        ExecutionTarget executionTarget = targetManager.getActiveTarget();
        if (executionTargetId.equals(executionTarget.getId())) {
            return; //already selected ours
        }
        List<ExecutionTarget> targets = targetManager.getTargetsFor(runConfig);
        for (ExecutionTarget target : targets) {
            if (target.getId().equals(executionTargetId)) {
                targetManager.setActiveTarget(target);
                return;
            }
        }
    }

    public void register() {
        counter.incrementAndGet();
    }

    public int unregister() {
        return counter.decrementAndGet();
    }
}
