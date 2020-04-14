/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
        boolean isSelected, int row, int column)
    {
        if (value == null)
            return null;

        button = (JRadioButton) value;
        button.addItemListener(this);

        return (Component) value;
    }

    @Override
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
