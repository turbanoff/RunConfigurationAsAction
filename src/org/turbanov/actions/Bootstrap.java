package org.turbanov.actions;

import java.util.List;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.RunManagerAdapter;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author Andrey Turbanov
 * @since 27.01.2017
 */
public class Bootstrap extends AbstractProjectComponent {
    private static final Logger log = Logger.getInstance(RunConfigurationAsAction.class);
    private static final String ACTION_PREFIX = "Action";
    private static final PluginId PLUGIN_ID = PluginId.getId("org.turbanov.run.configurations.as.action");

    public Bootstrap(Project project) {
        super(project);
    }

    private void registerAction(@NotNull RunnerAndConfigurationSettings runConfig, @NotNull Executor executor) {
        String actionId = ACTION_PREFIX  + executor.getId() + " " + runConfig.getName();
        ActionManager actionManager = ActionManagerEx.getInstance();
        AnAction action = actionManager.getAction(actionId);
        if (action == null) {
            action = new RunConfigurationAsAction(runConfig.getName(), executor.getId());
            actionManager.registerAction(actionId, action, PLUGIN_ID);
        } else if (action instanceof RunConfigurationAsAction) {
            ((RunConfigurationAsAction) action).register();
        } else {
            log.warn("Someone uses our action id! " + action);
        }
    }

    public void removeForAllExecutors(String runConfigName) {
        Executor[] executors = ExecutorRegistry.getInstance().getRegisteredExecutors();
        ActionManager actionManager = ActionManager.getInstance();
        for (Executor executor : executors) {
            String actionId = ACTION_PREFIX  + executor.getId() + " " + runConfigName;
            AnAction action = actionManager.getAction(actionId);
            if (action == null || !(action instanceof RunConfigurationAsAction)) {
                continue;
            }
            int count = ((RunConfigurationAsAction) action).unregister();
            if (count <= 0) {
                if (count < 0) {
                    log.warn("Someone remove more action than register " + action);
                }
                actionManager.unregisterAction(actionId);
            }
        }
    }

    private void registerForAllExecutors(@NotNull RunnerAndConfigurationSettings settings) {
        Executor[] executors = ExecutorRegistry.getInstance().getRegisteredExecutors();
        for (Executor executor : executors) {
            registerAction(settings, executor);
        }
    }

    @NotNull
    @Override
    public String getComponentName() {
        return ACTION_PREFIX;
    }

    @Override
    public void projectOpened() {
        RunManagerEx runManager = RunManagerEx.getInstanceEx(myProject);
        runManager.addRunManagerListener(new RunManagerAdapter() {
            @Override
            public void runConfigurationAdded(@NotNull RunnerAndConfigurationSettings settings) {
                registerForAllExecutors(settings);
            }

            @Override
            public void runConfigurationRemoved(@NotNull RunnerAndConfigurationSettings settings) {
                removeForAllExecutors(settings.getName());
            }
        });

        List<RunnerAndConfigurationSettings> allSettings = runManager.getAllSettings();
        for (RunnerAndConfigurationSettings setting : allSettings) {
            registerForAllExecutors(setting);
        }
    }

    @Override
    public void projectClosed() {
        RunManagerEx runManager = RunManagerEx.getInstanceEx(myProject);
        List<RunnerAndConfigurationSettings> allSettings = runManager.getAllSettings();
        for (RunnerAndConfigurationSettings setting : allSettings) {
            removeForAllExecutors(setting.getName());
        }
    }
}


