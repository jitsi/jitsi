/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class RadioButtonCellEditor
    extends DefaultCellEditor
    implements ItemListener
{
    private static final long serialVersionUID = 1L;
    private JRadioButton button;

    public RadioButtonCellEditor(JCheckBox checkBox)
    {
        super(checkBox);
    }

    public Component getTableCellEditorComponent(JTable table, Object value,
        boolean isSelected, int row, int column)
    {
        if (value == null)
            return null;
        
        button = (JRadioButton) value;
        button.addItemListener(this);
        
        return (Component) value;
    }

    public Object getCellEditorValue()
    {
        button.removeItemListener(this);
        return button;
    }

    public void itemStateChanged(ItemEvent e)
    {
        super.fireEditingStopped();
    }
  }
