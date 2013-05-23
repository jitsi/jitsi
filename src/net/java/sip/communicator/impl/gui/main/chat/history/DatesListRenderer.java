/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.history;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>DatesListRenderer</tt> is a <tt>ListCellRenderer</tt>, specialized
 * to show dates. It's meant to be used in the history window in order to
 * represent the list of history dates.
 *
 * @author Yana Stamcheva
 */
public class DatesListRenderer
    extends JPanel
    implements ListCellRenderer
{
    private JLabel label = new JLabel();
    private boolean isSelected;

    public DatesListRenderer()
    {
        super(new BorderLayout());

        this.add(label);
    }

    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
    {
        Date dateValue = (Date) value;

        StringBuffer dateStrBuf = new StringBuffer();

        GuiUtils.formatDate(dateValue.getTime(), dateStrBuf);

        this.label.setText(dateStrBuf.toString());
        this.isSelected = isSelected;

        return this;
    }

    /**
     * Paint a round background for all selected cells.
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g = g.create();
        try
        {
            AntialiasingManager.activateAntialiasing(g);

            Graphics2D g2 = (Graphics2D) g;

            if (this.isSelected)
            {

                g2.setColor(Constants.SELECTED_COLOR);
                g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 7, 7);

                g2.setColor(Constants.LIST_SELECTION_BORDER_COLOR);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, this.getWidth() - 1,
                    this.getHeight() - 1, 7, 7);
            }
        }
        finally
        {
            g.dispose();
        }
    }
}
