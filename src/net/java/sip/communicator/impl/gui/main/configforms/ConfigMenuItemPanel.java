/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.utils.*;

/**
 * @author Yana Stamcheva
 */
public class ConfigMenuItemPanel extends ListCellPanel
    implements MouseListener {

    private JLabel textLabel;

    private JLabel iconLabel;

    public ConfigMenuItemPanel(String text, Icon icon) {

        this.setPreferredSize(new Dimension(80, 50));

        this.setLayout(new GridLayout(0, 1));

        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        textLabel = new JLabel(text, JLabel.CENTER);

        iconLabel = new JLabel(icon, JLabel.CENTER);

        this.textLabel.setFont(this.getFont().deriveFont(Font.BOLD, 10));

        this.add(iconLabel);

        this.add(textLabel);
    }

    public String getText() {

        return this.textLabel.getText();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        if (this.isSelected()) {

            g2.setColor(Constants.BLUE_GRAY_BORDER_DARKER_COLOR);
            g2.drawRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1,
                    5, 5);
        } else if (this.isMouseOver()) {

            g2.setColor(Constants.BLUE_GRAY_BORDER_COLOR);
            g2.drawRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1,
                    5, 5);
        }
    }
}
