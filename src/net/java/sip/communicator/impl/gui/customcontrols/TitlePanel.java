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

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The <tt>TitlePanel</tt> is a decorated panel, that could be used for a header
 * or a title area. This panel is used for example in the
 * <tt>ConfigurationFrame</tt>.
 *
 * @author Yana Stamcheva
 */
public class TitlePanel
    extends TransparentPanel
{
    private static final long serialVersionUID = 1L;

    private final JLabel titleLabel = new JLabel();

    private final Color gradientStartColor = new Color(255, 255, 255, 200);

    private final Color gradientEndColor = new Color(255, 255, 255, 50);

    /**
     * Creates an instance of <tt>TitlePanel</tt>.
     */
    public TitlePanel()
    {
        this(null);
    }

    /**
     * Creates an instance of <tt>TitlePanel</tt> by specifying the title
     * String.
     *
     * @param title A String title.
     */
    public TitlePanel(String title)
    {
        super(new FlowLayout(FlowLayout.CENTER));

        Font font = getFont();
        titleLabel.setFont(font.deriveFont(Font.BOLD, font.getSize() + 2));

        if (title != null)
            setTitleText(title);
        else
            setPreferredSize(new Dimension(0, 30));
    }

    /**
     * Overrides the <code>paintComponent</code> method of <tt>JPanel</tt> to
     * paint a gradient background of this panel.
     */
    @Override
    public void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;

        AntialiasingManager.activateAntialiasing(g2);

        int width = getWidth();
        int height = getHeight();
        GradientPaint p =
            new GradientPaint(width / 2,
                              0,
                              gradientStartColor,
                              width / 2,
                              height,
                              gradientEndColor);

        g2.setPaint(p);
        g2.fillRoundRect(0, 0, width, height, 10, 10);

        super.paintComponent(g2);
    }

    /**
     * Sets the title String.
     *
     * @param title The title String.
     */
    public void setTitleText(String title)
    {
        this.removeAll();

        this.titleLabel.setText(title);

        this.add(titleLabel);
    }
}
