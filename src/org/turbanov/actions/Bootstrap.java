package org.turbanov.actions;

import javax.swing.Icon;
import java.lang.reflect.Field;
import java.util.List;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.RunManagerAdapter;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.ide.ui.customization.CustomActionsSchema;
import com.intellij.ide.ui.customization.CustomisedActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Andrey Turbanov
 * @since 27.01.2017
 */
public class Bootstrap extends AbstractProjectComponent {
    private static final Logger log = Logger.getInstance(RunConfigurationAsAction.class);
    private static final String ACTION_ID_PREFIX = "RunConfigurationAsAction";
    private static final PluginId PLUGIN_ID = PluginId.getId("org.turbanov.run.configuration.as.action");

    public Bootstrap(Project project) {
        super(project);
    }

    private void registerAction(@NotNull RunnerAndConfigurationSettings runConfig, @NotNull Executor executor) {
        String actionId = makeActionId(executor, runConfig.getName());
        ActionManager actionManager = ActionManagerEx.getInstance();
        AnAction action = actionManager.getAction(actionId);
        if (action == null) {
            Icon icon = makeIcon(runConfig, executor);
            String text = executor.getActionName() + " " + runConfig.getName();
            action = new RunConfigurationAsAction(runConfig.getName(), executor.getId(), icon, text);
            actionManager.registerAction(actionId, action, PLUGIN_ID);
        } else if (action instanceof RunConfigurationAsAction) {
            ((RunConfigurationAsAction) action).register();
        } else {
            log.warn("Someone uses our action id = " + actionId + ": " + action.getClass() + " " + action);
        }
    }

    private Icon makeIcon(@NotNull RunnerAndConfigurationSettings runConfig, @NotNull Executor executor) {
        Icon icon = executor.getIcon();
        Icon result = IconUtil.addText(icon, runConfig.getName());
        return result;
    }

    private static String makeActionId(Executor executor, String runConfigName) {
        return ACTION_ID_PREFIX + "_" + executor.getId() + "_" + runConfigName;
    }

    public void removeForAllExecutors(String runConfigName) {
        Executor[] executors = ExecutorRegistry.getInstance().getRegisteredExecutors();
        ActionManager actionManager = ActionManager.getInstance();
        for (Executor executor : executors) {
            String actionId = makeActionId(executor, runConfigName);
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
        return ACTION_ID_PREFIX;
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

        hackNavToolbar();
    }

    private void hackNavToolbar() {
        AnAction correctAction = CustomActionsSchema.getInstance().getCorrectedAction("NavBarToolBar");
        if (correctAction instanceof CustomisedActionGroup) {
            CustomisedActionGroup action = (CustomisedActionGroup) correctAction;
            Field field = null;
            try {
                field = action.getClass().getDeclaredField("myForceUpdate");
            } catch (NoSuchFieldException e) {
                try {
                    field = action.getClass().getDeclaredField("f");
                } catch (NoSuchFieldException ex) {
                    //do nothing
                }
            }
            if (field == null) {
                log.info("Can't field field");
                return;
            }
            try {
                field.setAccessible(true);
                field.set(action, true);
            } catch (IllegalAccessException e) {
                log.info("Access denied to field myForceUpdate");
            }
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


