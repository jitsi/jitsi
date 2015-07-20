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

import javax.swing.*;
import javax.swing.table.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Custom <tt>TableCellRenderer</tt> that renders
 * <tt>ProtocolProviderService</tt> objects, <tt>MetaContactGroup</tt>
 * objects and JLabels.
 *
 * @author Yana Stamcheva
 */
public class LabelTableCellRenderer
    extends JLabel
    implements TableCellRenderer
{
    private static final long serialVersionUID = 0L;

    public LabelTableCellRenderer()
    {
        this.setHorizontalAlignment(JLabel.LEFT);
        this.setOpaque(true);
        this.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column)
    {

        if(value instanceof JLabel) {
            JLabel labelValue = (JLabel)value;

            this.setText(labelValue.getText());
            this.setIcon(labelValue.getIcon());
        }
        else if (value instanceof ProtocolProviderService) {
            ProtocolProviderService pps = (ProtocolProviderService)value;
            this.setText(pps.getAccountID().getDisplayName());
        }
        else if (value instanceof MetaContactGroup) {
            MetaContactGroup group = (MetaContactGroup) value;
            this.setText(group.getGroupName());
        }
        else if (value instanceof ChatRoomProviderWrapper) {
            ChatRoomProviderWrapper provider = (ChatRoomProviderWrapper) value;
            this.setText(
                provider.getProtocolProvider().getAccountID().getDisplayName());
        } else {
            this.setText(value.toString());
        }

        if(isSelected)
            this.setBackground(Constants.SELECTED_COLOR);
        else
            this.setBackground(UIManager.getColor("Table.background"));

        return this;
    }
}
