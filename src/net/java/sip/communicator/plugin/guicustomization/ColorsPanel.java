package net.java.sip.communicator.plugin.guicustomization;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

public class ColorsPanel
    extends JScrollPane
{
    private JTable colorsTable = new JTable();

    private CustomTableModel colorsTableModel = new CustomTableModel();

    public ColorsPanel()
    {
        this.getViewport().add(colorsTable);

        colorsTable.setModel(colorsTableModel);
        colorsTableModel.addColumn("Description");
        colorsTableModel.addColumn("Color");
        colorsTableModel.addColumn("Change color");

        colorsTable.getColumnModel().getColumn(1).setCellRenderer(
            new LabelTableCellRenderer());

        TableColumn buttonColumn
            = colorsTable.getColumnModel().getColumn(2);

        buttonColumn.setCellRenderer(new ButtonTableCellRenderer());
        buttonColumn.setCellEditor(new ButtonTableEditor());

        this.initColorTable();
    }

    private void initColorTable()
    {
        Iterator colorKeys
            = GuiCustomizationActivator.getResources()
                .getCurrentColors();

        while (colorKeys.hasNext())
        {
            String key = (String) colorKeys.next();
            final Color color = new Color(
                GuiCustomizationActivator.getResources().getColor(key));

            final JLabel colorLabel = new JLabel();
            colorLabel.setBackground(color);

            JButton colorChooserButton = new JButton();
            colorChooserButton.setAction(
                new ChooseColorAction(color, colorLabel));

            colorsTableModel.addRow(new Object[]{   key,
                                                    colorLabel,
                                                    colorChooserButton});
            colorChooserButton.setText("Choose a colour");

            int rowHeight = 40;
            colorsTable.setRowHeight(   colorsTableModel.getRowCount() - 1,
                                        rowHeight );
        }
    }
    
    private class ChooseColorAction extends AbstractAction
    {
        private Color defaultColor;
        private JLabel colorLabel;

        public ChooseColorAction(Color defaultColor, JLabel colorLabel)
        {
            this.defaultColor = defaultColor;
            this.colorLabel = colorLabel;
        }
        public void actionPerformed(ActionEvent evt)
        {
            Color newColor
                = JColorChooser.showDialog( new JColorChooser(),
                                            "Choose a colour",
                                            defaultColor);

            colorLabel.setBackground(newColor);
        }
    }
    
    Hashtable<String, String> getColors()
    {
        Hashtable res = new Hashtable();
        int rows = colorsTableModel.getRowCount();
        for (int i = 0; i < rows; i++)
        {
            String key = (String)colorsTableModel.getValueAt(i, 0);
            JLabel colorLabel = (JLabel)colorsTableModel.getValueAt(i, 1);
            
            res.put(
                key,
                Integer.toHexString(colorLabel.getBackground().getRGB()).substring(2));
        }
        
        return res;
    }
}
