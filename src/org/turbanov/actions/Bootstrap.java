package org.turbanov.actions;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.execution.DefaultExecutionTarget;
import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.ExecutionTargetManager;
import com.intellij.execution.Executor;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunManagerListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.ide.ui.customization.CustomActionsSchema;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.IconUtil;
import com.intellij.util.ImageLoader;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBImageIcon;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * @author Andrey Turbanov
 * @since 27.01.2017
 */
public class Bootstrap implements ProjectComponent {
    private static final Logger log = Logger.getInstance(RunConfigurationAsAction.class);
    private static final String ACTION_ID_PREFIX = "RunConfigurationAsAction";
    private static final PluginId PLUGIN_ID = PluginId.getId("org.turbanov.run.configuration.as.action");
    private static final ExecutorService ourExecutor = AppExecutorUtil.createBoundedApplicationPoolExecutor("RunConfigurationAsAction", 1);

    private final Project myProject;
    private final Set<String> registeredActions = ConcurrentHashMap.newKeySet();
    private final int baselineVersion;

    public Bootstrap(@NotNull Project project) {
        this.myProject = project;
        this.baselineVersion = ApplicationInfo.getInstance().getBuild().getBaselineVersion();
    }

    private void registerAction(@NotNull RunnerAndConfigurationSettings runConfig,
                                @NotNull Executor executor,
                                @Nullable ExecutionTarget target) {
        String actionId = makeActionId(executor, runConfig, target);
        ActionManager actionManager = ActionManagerEx.getInstance();
        AnAction action = actionManager.getAction(actionId);
        if (action == null) {
            String text = getActionText(runConfig, executor, target);
            String executionTargetId = target == null ? null : target.getId();
            Icon icon = loadIcon(actionId);
            if (icon == null) {
                icon = makeIcon(runConfig, executor);
            }
            action = new RunConfigurationAsAction(runConfig.getName(), executor.getId(), icon, text, executionTargetId, baselineVersion);
            actionManager.registerAction(actionId, action, PLUGIN_ID);
            registeredActions.add(actionId);
        } else if (action instanceof RunConfigurationAsAction) {
            ((RunConfigurationAsAction) action).register();
            registeredActions.add(actionId);
        } else {
            log.warn("Someone uses our action id = " + actionId + ": " + action.getClass() + " " + action);
        }
    }

    @NotNull
    private String getActionText(@NotNull RunnerAndConfigurationSettings runConfig, @NotNull Executor executor, @Nullable ExecutionTarget target) {
        String targetAdd;
        if (target == null || DefaultExecutionTarget.INSTANCE.equals(target)) {
            targetAdd = "";
        } else {
            targetAdd = " " + target.getDisplayName();
        }
        String text = executor.getActionName() + " '" + runConfig.getName() + "'" + targetAdd;
        return StringUtil.escapeMnemonics(text);
    }

    @NotNull
    private Icon makeIcon(@NotNull RunnerAndConfigurationSettings runConfig, @NotNull Executor executor) {
        Icon icon = executor.getIcon();
        Icon result = IconUtil.addText(icon, runConfig.getName());
        return result;
    }

    @Nullable
    private Icon loadIcon(@NotNull String actionId) {
        String iconPath = CustomActionsSchema.getInstance().getIconPath(actionId);
        if (StringUtil.isEmpty(iconPath)) {
            return null;
        }
        //copied from CustomActionsSchema.initActionIcons
        final File f = new File(FileUtil.toSystemDependentName(iconPath));
        if (f.exists()) {
            Image image = null;
            try {
                image = ImageLoader.loadCustomIcon(f);
            } catch (IOException e) {
                log.debug(e);
            }
            if (image != null) {
                return new JBImageIcon(image);
            }
        }
        return null;
    }

    private static String makeActionId(@NotNull Executor executor,
                                       @NotNull RunnerAndConfigurationSettings runConfig,
                                       @Nullable ExecutionTarget target) {
        String targetAdditional = target == null ? "" : "_" + target.getId();
        return ACTION_ID_PREFIX + "_" + executor.getId() + "_" + runConfig.getName() + targetAdditional;
    }

    public void removeForAllExecutors(@NotNull RunnerAndConfigurationSettings runConfig) {
        ourExecutor.execute(() -> removeForAllExecutorsImpl(runConfig));
    }

    private void removeForAllExecutorsImpl(@NotNull RunnerAndConfigurationSettings runConfig) {
        List<Executor> executors = Executor.EXECUTOR_EXTENSION_NAME.getExtensionList();
        ActionManager actionManager = ActionManager.getInstance();
        List<ExecutionTarget> targets = getTargets(runConfig);
        for (Executor executor : executors) {
            for (ExecutionTarget target : targets) {
                String actionId = makeActionId(executor, runConfig, target);
                unregisterAction(actionManager, actionId);
            }
        }
    }

    private void registerForAllExecutors(@NotNull RunnerAndConfigurationSettings runConfig) {
        ourExecutor.execute(() -> registerForAllExecutorsImpl(runConfig));
    }

    private void registerForAllExecutorsImpl(@NotNull RunnerAndConfigurationSettings settings) {
        List<Executor> executors = Executor.EXECUTOR_EXTENSION_NAME.getExtensionList();
        List<ExecutionTarget> targets = getTargets(settings);
        for (Executor executor : executors) {
            for (ExecutionTarget target : targets) {
                registerAction(settings, executor, target);
            }
        }
    }

    @NotNull
    private List<ExecutionTarget> getTargets(@NotNull RunnerAndConfigurationSettings runConfig) {
        List<ExecutionTarget> targets = ExecutionTargetManager.getInstance(myProject).getTargetsFor(runConfig.getConfiguration());
        if (targets.size() == 1 && DefaultExecutionTarget.INSTANCE.equals(targets.get(0))) {
            return targets;
        }
        targets = new ArrayList<>(targets);
        targets.add(null);
        return targets;
    }

    @NotNull
    @Override
    public String getComponentName() {
        return ACTION_ID_PREFIX;
    }

    @Override
    public void projectOpened() {
        myProject.getMessageBus().connect().subscribe(RunManagerListener.TOPIC, new RunManagerListener() {
            @Override
            public void runConfigurationAdded(@NotNull RunnerAndConfigurationSettings settings) {
                registerForAllExecutors(settings);
            }

            @Override
            public void runConfigurationRemoved(@NotNull RunnerAndConfigurationSettings settings) {
                removeForAllExecutors(settings);
            }
        });

        RunManagerEx runManager = RunManagerEx.getInstanceEx(myProject);
        List<RunnerAndConfigurationSettings> allSettings = runManager.getAllSettings();
        for (RunnerAndConfigurationSettings setting : allSettings) {
            registerForAllExecutors(setting);
        }
    }

    @Override
    public void projectClosed() {
        ActionManager actionManager = ActionManager.getInstance();
        for (String actionId : registeredActions) {
            unregisterAction(actionManager, actionId);
        }
    }

    private void unregisterAction(ActionManager actionManager, String actionId) {
        registeredActions.remove(actionId);
        AnAction action = actionManager.getAction(actionId);
        if (!(action instanceof RunConfigurationAsAction)) {
            return;
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


