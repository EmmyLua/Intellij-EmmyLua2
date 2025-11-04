package com.tang.intellij.lua.project

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.openapi.options.ConfigurationException
import java.awt.BorderLayout
import javax.swing.*

/**
 * Project type selection step in the wizard
 */
class LuaProjectTypeStep(private val builder: LuaModuleBuilder) : ModuleWizardStep() {
    
    private val panel = JPanel(BorderLayout())
    private val buttonGroup = ButtonGroup()
    private val radioButtons = mutableMapOf<LuaProjectType, JRadioButton>()
    
    init {
        createUI()
    }
    
    private fun createUI() {
        val contentPanel = JPanel()
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
        contentPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        
        val titleLabel = JLabel("Select Lua Project Type:")
        titleLabel.font = titleLabel.font.deriveFont(14f).deriveFont(java.awt.Font.BOLD)
        titleLabel.border = BorderFactory.createEmptyBorder(0, 0, 10, 0)
        contentPanel.add(titleLabel)
        
        // Add radio buttons for each project type
        for (type in LuaProjectType.values()) {
            val radioButton = JRadioButton(type.displayName)
            radioButton.isSelected = (type == builder.getProjectTypeInternal())
            radioButton.border = BorderFactory.createEmptyBorder(5, 0, 5, 0)
            
            val descLabel = JLabel("<html><body style='margin-left: 25px; color: gray;'>${type.description}</body></html>")
            descLabel.border = BorderFactory.createEmptyBorder(0, 0, 10, 0)
            
            radioButton.addActionListener {
                if (radioButton.isSelected) {
                    builder.setProjectType(type)
                }
            }
            
            buttonGroup.add(radioButton)
            radioButtons[type] = radioButton
            
            contentPanel.add(radioButton)
            contentPanel.add(descLabel)
        }
        
        panel.add(contentPanel, BorderLayout.NORTH)
    }
    
    override fun getComponent(): JComponent = panel
    
    override fun updateDataModel() {
        // Data is updated via radio button listeners
    }
    
    @Throws(ConfigurationException::class)
    override fun validate(): Boolean {
        return true
    }
}
