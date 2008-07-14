package net.java.sip.communicator.plugin.guicustomization;

import java.awt.*;

import javax.swing.*;

/**
 * @version 1.0 11/09/98
 */
public class ButtonTableEditor
    extends DefaultCellEditor
{
    private JButton button;

    private JPanel buttonPanel;

    private boolean isPushed;

    public ButtonTableEditor()
    {
        super(new JCheckBox());
    }

    public Component getTableCellEditorComponent(JTable table, Object value,
        boolean isSelected, int row, int column)
    {
        isPushed = true;
        if (value instanceof JButton)
        {
            this.button = (JButton) value;

            buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

            buttonPanel.add(button);

            return buttonPanel;
        }

        return null;
    }

    public Object getCellEditorValue()
    {
        isPushed = false;
        return button;
    }

    public boolean stopCellEditing()
    {
        isPushed = false;
        return super.stopCellEditing();
    }

    protected void fireEditingStopped()
    {
        super.fireEditingStopped();
    }
}