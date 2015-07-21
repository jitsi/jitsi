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

package net.java.sip.communicator.plugin.pluginmanager;

import java.awt.*;

import javax.swing.*;

/**
 * The <tt>TitlePanel</tt> is a decorated panel, that could be used for a
 * header or a title area. This panel is used for example in the
 * <tt>ConfigurationFrame</tt>.
 *
 * @author Yana Stamcheva
 */
public class TitlePanel extends JPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * A color between blue and gray used to paint some borders.
     */
    public static final Color BORDER_COLOR
        = new Color(Resources.getColor("service.gui.BORDER_COLOR"));

    /**
     * The size of the gradient used for painting the background.
     */
    private static final int GRADIENT_SIZE = 10;

    /**
     * The start color used to paint a gradient mouse over background.
     */
    private static final Color GRADIENT_DARK_COLOR
        = new Color(Resources.getColor("service.gui.GRADIENT_DARK_COLOR"));

    /**
     * The end color used to paint a gradient mouse over background.
     */
    private static final Color GRADIENT_LIGHT_COLOR
        = new Color(Resources.getColor("service.gui.GRADIENT_LIGHT_COLOR"));

    private JLabel titleLabel = new JLabel();

    /**
     * Creates an instance of <tt>TitlePanel</tt>.
     */
    public TitlePanel() {

        super(new FlowLayout(FlowLayout.CENTER));

        this.setPreferredSize(new Dimension(0, 30));

        this.titleLabel.setFont(this.getFont().deriveFont(Font.BOLD, 14));
    }

    /**
     * Creates an instance of <tt>TitlePanel</tt> by specifying the title
     * String.
     *
     * @param title A String title.
     */
    public TitlePanel(String title) {

        super(new FlowLayout(FlowLayout.CENTER));

        this.titleLabel.setFont(this.getFont().deriveFont(Font.BOLD, 14));

        this.titleLabel.setText(title);

        this.add(titleLabel);
    }

    /**
     * Overrides the <code>paintComponent</code> method of <tt>JPanel</tt>
     * to paint a gradient background of this panel.
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        GradientPaint p = new GradientPaint(this.getWidth() / 2, 0,
                GRADIENT_DARK_COLOR, this.getWidth() / 2,
                GRADIENT_SIZE,
                GRADIENT_LIGHT_COLOR);

        GradientPaint p1 = new GradientPaint(this.getWidth() / 2, this
                .getHeight()
                - GRADIENT_SIZE,
                GRADIENT_LIGHT_COLOR, this.getWidth() / 2,
                this.getHeight(), GRADIENT_DARK_COLOR);

        g2.setPaint(p);
        g2.fillRect(0, 0, this.getWidth(), GRADIENT_SIZE);

        g2.setColor(GRADIENT_LIGHT_COLOR);
        g2.fillRect(0, GRADIENT_SIZE, this.getWidth(),
                this.getHeight() - GRADIENT_SIZE);

        g2.setPaint(p1);
        g2.fillRect(0, this.getHeight() - GRADIENT_SIZE
                - 1, this.getWidth(), this.getHeight() - 1);

        g2.setColor(BORDER_COLOR);
        g2.drawRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1, 5, 5);
    }

    /**
     * Sets the title String.
     * @param title The title String.
     */
    public void setTitleText(String title) {
        this.titleLabel.setText(title);

        this.add(titleLabel);
    }
}
