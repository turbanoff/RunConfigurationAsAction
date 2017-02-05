package org.turbanov.actions;

import javax.swing.Icon;
import java.util.concurrent.atomic.AtomicInteger;

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
@SuppressWarnings("ComponentNotRegistered")
public class RunConfigurationAsAction extends AnAction {

    private static final Logger log = Logger.getInstance(RunConfigurationAsAction.class);

    private final String runConfigurationName;
    private final String executorId;
    private final AtomicInteger counter = new AtomicInteger();

    public RunConfigurationAsAction(String runConfigurationName, String executorId, Icon icon, String text) {
        super(text, null, icon);
        this.runConfigurationName = runConfigurationName;
        this.executorId = executorId;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        RunManagerEx runManager = RunManagerEx.getInstanceEx(project);
        RunnerAndConfigurationSettings runConfig = runManager.findConfigurationByName(runConfigurationName);

        if (runConfig == null) {
            log.info("Unable to find Run configuration with name: " + runConfigurationName);
            return;
        }

        Executor executor = ExecutorRegistry.getInstance().getExecutorById(executorId);
        if (executor == null) {
            log.info("Unable to find Executor by id: " + executorId);
            return;
        }

        ProgramRunnerUtil.executeConfiguration(project, runConfig, executor);
    }

    public void register() {
        counter.incrementAndGet();
    }

    public int unregister() {
        return counter.decrementAndGet();
    }
}
