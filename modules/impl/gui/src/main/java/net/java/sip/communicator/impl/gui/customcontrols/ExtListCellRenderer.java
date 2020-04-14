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

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * @author Yana Stamcheva
 */
public class ExtListCellRenderer
    extends JPanel
    implements ListCellRenderer
{

    private static final long serialVersionUID = 1L;
    private final JLabel label = new JLabel();
    private boolean isSelected;

    public ExtListCellRenderer()
    {
        super(new BorderLayout());

        this.add(label);
    }

    /**
     * Implements the <tt>ListCellRenderer</tt> method.
     */
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelect, boolean cellHasFocus)
    {
        this.label.setText(value.toString());
        this.isSelected = isSelect;

        return this;
    }

    /**
     * Paint a round background for all selected cells.
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        g = g.create();
        try
        {
            AntialiasingManager.activateAntialiasing(g);

            if (this.isSelected)
            {
                Graphics2D g2 = (Graphics2D) g;
                int width = getWidth();
                int height = getHeight();

                g2.setColor(Constants.SELECTED_COLOR);
                g2.fillRoundRect(0, 0, width, height, 7, 7);

                g2.setColor(Constants.LIST_SELECTION_BORDER_COLOR);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, width - 1, height - 1, 7, 7);
            }
        }
        finally
        {
            g.dispose();
        }
    }
}
