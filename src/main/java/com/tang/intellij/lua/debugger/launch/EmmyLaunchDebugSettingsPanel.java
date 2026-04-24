package com.tang.intellij.lua.debugger.launch;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class EmmyLaunchDebugSettingsPanel
        extends SettingsEditor<EmmyLaunchDebugConfiguration>
        implements DocumentListener {

    private JTextField Program;
    private JTextField WorkingDirectory;
    private JTextField Parameters;
    private JCheckBox useWindowsTerminalCheckBox;
    private JPanel panel;

    public EmmyLaunchDebugSettingsPanel(Project project) {
        Program.getDocument().addDocumentListener(this);
        WorkingDirectory.getDocument().addDocumentListener(this);
        Parameters.getDocument().addDocumentListener(this);
        useWindowsTerminalCheckBox.addActionListener(e -> onChanged());
    }

    @Override
    protected void resetEditorFrom(@NotNull EmmyLaunchDebugConfiguration configuration) {
        Program.setText(configuration.getProgram());
        WorkingDirectory.setText(configuration.getWorkingDirectory());
        Parameters.setText(configuration.getParameter());
        useWindowsTerminalCheckBox.setSelected(configuration.getUseWindowsTerminal());
    }

    @Override
    protected void applyEditorTo(@NotNull EmmyLaunchDebugConfiguration configuration)
            throws ConfigurationException {
        configuration.setProgram(Program.getText());
        configuration.setWorkingDirectory(WorkingDirectory.getText());
        configuration.setParameter(Parameters.getText());
        configuration.setUseWindowsTerminal(useWindowsTerminalCheckBox.isSelected());
    }

    @Override
    protected @NotNull JComponent createEditor() {
        return panel;
    }

    @Override public void insertUpdate(DocumentEvent e)  { onChanged(); }
    @Override public void removeUpdate(DocumentEvent e)  { onChanged(); }
    @Override public void changedUpdate(DocumentEvent e) { onChanged(); }

    private void onChanged() { fireEditorStateChanged(); }
}
