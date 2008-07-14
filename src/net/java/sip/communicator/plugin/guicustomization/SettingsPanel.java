package net.java.sip.communicator.plugin.guicustomization;

import java.awt.*;
import java.util.*;

import javax.swing.*;

public class SettingsPanel
    extends JScrollPane
{
    private JTable settingsTable = new JTable();

    private CustomTableModel settingsTableModel = new CustomTableModel();

    public SettingsPanel()
    {
        this.getViewport().add(settingsTable);

        settingsTable.setModel(settingsTableModel);
        settingsTableModel.addColumn("Key");
        settingsTableModel.addColumn("Text");

        settingsTable.getColumnModel().getColumn(1)
            .setCellRenderer(new TextAreaCellRenderer());
        settingsTable.setShowGrid(true);
        settingsTable.setGridColor(Color.GRAY);
        this.initSettingTable();
    }
    
    private void initSettingTable()
    {
        Iterator settingKeys
            = GuiCustomizationActivator.getResources()
                .getCurrentSettings();

        while (settingKeys.hasNext())
        {
            String key = (String) settingKeys.next();
            String value
                = GuiCustomizationActivator.getResources()
                    .getSettingsString(key);

            settingsTableModel.addRow(new Object[]{  key,
                                                    value});
        }
    }
    
    Hashtable<String, String> getSettings()
    {
        Hashtable res = new Hashtable();
        int rows = settingsTableModel.getRowCount();
        for (int i = 0; i < rows; i++)
        {
            String key = (String)settingsTableModel.getValueAt(i, 0);
            String value = (String)settingsTableModel.getValueAt(i, 1);
            
            res.put(key, value);
        }
        
        return res;
    }
}
