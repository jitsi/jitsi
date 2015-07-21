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
package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The <tt>ConfigFormListCellRenderer</tt> is the custom cell renderer used in
 * the Jitsi's <tt>ConfigFormList</tt>. It extends TransparentPanel
 * instead of JLabel, which allows adding different buttons and icons to the
 * cell.
 * <br>
 * The cell border and background are repainted.
 *
 * @author Yana Stamcheva
 */
public class ConfigFormListCellRenderer
    extends TransparentPanel
    implements ListCellRenderer
{

    /**
     * The size of the gradient used for painting the selected background of
     * some components.
     */
    public static final int SELECTED_GRADIENT_SIZE = 5;

    private final JLabel textLabel = new EmphasizedLabel("");

    private final JLabel iconLabel = new JLabel();

    private boolean isSelected = false;

    /**
     * Initialize the panel containing the node.
     */
    public ConfigFormListCellRenderer()
    {
        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 5, 3));
        this.setLayout(new BorderLayout(0, 0));
        this.setPreferredSize(new Dimension(60, 50));

        Font font = getFont();
        this.textLabel.setFont(font.deriveFont(11f));

        this.iconLabel.setHorizontalAlignment(JLabel.CENTER);
        this.textLabel.setHorizontalAlignment(JLabel.CENTER);

        this.add(iconLabel, BorderLayout.CENTER);
        this.add(textLabel, BorderLayout.SOUTH);
    }

    /**
     * Implements the <tt>ListCellRenderer</tt> method.
     *
     * Returns this panel that has been configured to display the meta contact
     * and meta contact group cells.
     */
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
    {
        ConfigFormDescriptor cfDescriptor = (ConfigFormDescriptor) value;

        ImageIcon icon = cfDescriptor.getConfigFormIcon();
        if(icon != null)
            iconLabel.setIcon(icon);

        String title = cfDescriptor.getConfigFormTitle();
        if (title != null)
            textLabel.setText(title);

        this.isSelected = isSelected;

        return this;
    }

    /**
     * Overrides the <code>paintComponent</code> method of <tt>JPanel</tt>
     * to provide a custom look for this panel. A gradient background is
     * painted when the panel is selected and when the mouse is over it.
     * @param g the <tt>Graphics</tt> object
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        AntialiasingManager.activateAntialiasing(g);

        Graphics2D g2 = (Graphics2D) g;

        if (isSelected)
        {
            g2.setPaint(new Color(100, 100, 100, 100));
            g2.fillRoundRect(   0, 0,
                               this.getWidth(), this.getHeight(),
                               10, 10);

            g2.setColor(Color.GRAY);
            g2.drawRoundRect(   0, 0,
                               this.getWidth() - 1, this.getHeight() - 1,
                               10, 10);
        }
    }
}
