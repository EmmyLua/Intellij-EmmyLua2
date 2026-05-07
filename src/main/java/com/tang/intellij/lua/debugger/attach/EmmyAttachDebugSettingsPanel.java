package com.tang.intellij.lua.debugger.attach;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class EmmyAttachDebugSettingsPanel
        extends SettingsEditor<EmmyAttachDebugConfiguration>
        implements DocumentListener {

    private JTextField ProcessId;
    private JPanel panel;
    private JTextField ProcessName;
    private JTextField Encoding;
    private JRadioButton UsePid;
    private JRadioButton UseProcessName;
    private JPanel AttachMode;
    private ButtonGroup AttachModeGroup;
    private JComboBox<EmmyAttachWinArch> WinArch;

    public EmmyAttachDebugSettingsPanel(Project project) {
        ProcessId.getDocument().addDocumentListener(this);
        ProcessName.getDocument().addDocumentListener(this);
        Encoding.setText("gbk");
        Encoding.getDocument().addDocumentListener(this);

        AttachModeGroup = new ButtonGroup();
        AttachModeGroup.add(UsePid);
        AttachModeGroup.add(UseProcessName);
        UsePid.addChangeListener(e -> onChanged());
        UseProcessName.addChangeListener(e -> onChanged());

        WinArch.setModel(new DefaultComboBoxModel<>(EmmyAttachWinArch.values()));
        WinArch.addActionListener(e -> onChanged());
    }

    @Override
    protected void resetEditorFrom(@NotNull EmmyAttachDebugConfiguration configuration) {
        ProcessId.setText(configuration.getPid());
        ProcessName.setText(configuration.getProcessName());
        Encoding.setText(configuration.getEncoding());
        WinArch.setSelectedItem(configuration.getWinArch());

        if (configuration.getAttachMode() == EmmyAttachMode.Pid) {
            UsePid.setSelected(true);
        } else {
            UseProcessName.setSelected(true);
        }
    }

    @Override
    protected void applyEditorTo(@NotNull EmmyAttachDebugConfiguration configuration)
            throws ConfigurationException {
        configuration.setPid(ProcessId.getText());
        configuration.setProcessName(ProcessName.getText());
        configuration.setEncoding(Encoding.getText());
        configuration.setAttachMode(UsePid.isSelected() ? EmmyAttachMode.Pid : EmmyAttachMode.ProcessName);
        EmmyAttachWinArch selected = (EmmyAttachWinArch) WinArch.getSelectedItem();
        if (selected != null) {
            configuration.setWinArch(selected);
        }
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
