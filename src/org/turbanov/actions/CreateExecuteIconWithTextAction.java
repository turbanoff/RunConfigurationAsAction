package org.turbanov.actions;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

/**
 * @author Andrey Turbanov
 * @since 30.01.2017
 */
public class CreateExecuteIconWithTextAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        MyDialogWrapper wrapper = new MyDialogWrapper();
        wrapper.show();
        if (wrapper.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            Icon resultIcon = wrapper.resultIconLabel.getIcon();
            String fileName = wrapper.current.getText() + " " + wrapper.textField.getText() + ".png";
            FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor().withTitle("Choose Directory to Save Icon File: " + fileName);
            FileChooser.chooseFile(descriptor, null, null,
                    dir -> {
                        if (!dir.isDirectory()) {
                            return;
                        }
                        VirtualFile child = dir.findChild(fileName);
                        if (child != null) {
                            if (Messages.OK != Messages.showOkCancelDialog(IdeBundle.message("prompt.overwrite.settings.file", child.toString()), IdeBundle.message("title.file.already.exists"), Messages.getWarningIcon()))
                                return;
                        }
                        ApplicationManager.getApplication().runWriteAction(() -> {
                            try {
                                VirtualFile file;
                                if (child == null) {
                                    file = dir.createChildData("Run Configuration as Action Plugin", fileName);
                                } else {
                                    file = child;
                                }

                                try (OutputStream output = file.getOutputStream(CreateExecuteIconWithTextAction.this)) {
                                    BufferedImage image = ImageUtil.toBufferedImage(IconUtil.toImage(resultIcon));
                                    ImageIO.write(image, "png", output);
                                }

                                ApplicationManager.getApplication().invokeLater(() -> Messages.showInfoMessage("File saved " + file.getPath(), "Icon Saved"));
                            } catch (IOException ex) {
                                ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog("Error: " + ExceptionUtil.getThrowableText(ex), "Unable to Save Icon File"));
                            }
                        });
                    });
        }
    }

    private static class MyDialogWrapper extends DialogWrapper {
        private JLabel current;
        private SpinnerNumberModel fontScaleModel;
        private JLabel resultIconLabel;
        private JBTextField textField;

        public MyDialogWrapper() {
            super(false);
            init();
            setTitle("Create Icon with Text");
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            Executor[] executors = ExecutorRegistry.getInstance().getRegisteredExecutors();
            ButtonGroup buttonGroup = new ButtonGroup();
            Box boxWithExecutors = Box.createVerticalBox();
            textField = new JBTextField();
            resultIconLabel = new JLabel();
            resultIconLabel.setMaximumSize(resultIconLabel.getPreferredSize());
            fontScaleModel = new SpinnerNumberModel(6, 1, 100, 1);
            fontScaleModel.addChangeListener(e -> updateIcon());
            JSpinner fontScaleSpinner = new JSpinner(fontScaleModel);
            boolean first = true;
            for (Executor executor : executors) {
                JPanel panel = new JPanel();
                JBRadioButton rb = new JBRadioButton();
                panel.add(rb);
                JLabel labelWithExecutorIcon = new JLabel(executor.getActionName(), executor.getIcon(), SwingConstants.LEFT);
                panel.add(labelWithExecutorIcon);
                if (first) {
                    rb.setSelected(true);
                    first = false;
                    current = labelWithExecutorIcon;
                    updateIcon();
                }
                buttonGroup.add(rb);
                boxWithExecutors.add(panel);
                rb.addActionListener(e -> {
                    current = labelWithExecutorIcon;
                    updateIcon();
                });
            }
            textField.getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(DocumentEvent e) {
                    updateIcon();
                }
            });
            Box box = Box.createHorizontalBox();
            box.add(boxWithExecutors);

            JPanel textAndResult = new JPanel(new VerticalLayout(5));
            textAndResult.add(textField, VerticalLayout.TOP);
            textAndResult.add(fontScaleSpinner, VerticalLayout.BOTTOM);
            textAndResult.add(resultIconLabel, VerticalLayout.BOTTOM);
            box.add(textAndResult);

            return box;
        }

        private void updateIcon() {
            LayeredIcon icon = new LayeredIcon(2);
            icon.setIcon(current.getIcon(), 0);
            Icon iconWithText = IconUtil.textToIcon(textField.getText(), new JLabel(), JBUI.scale(fontScaleModel.getNumber().floatValue()));
            icon.setIcon(iconWithText, 1, SwingConstants.SOUTH_EAST);
            //crop to allow to use as action icon. Copied from CustomizableActionsPanel.doSetIcon()
            Icon result = IconUtil.cropIcon(icon, EmptyIcon.ICON_18.getIconWidth(), EmptyIcon.ICON_16.getIconHeight());
            resultIconLabel.setIcon(result);
            resultIconLabel.setMaximumSize(resultIconLabel.getPreferredSize());
        }
    }
}
