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
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.skin.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>DialPanel</tt> is the panel that contains the buttons to dial a
 * phone number.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 * @author Lyubomir Marinov
 */
public class DialPanel
    extends JPanel
    implements MouseListener,
               Skinnable
{
    /**
     * The dial panel.
     */
    private final JPanel dialPadPanel;

    /**
     * Handles DTMFs.
     */
    private final DTMFHandler dtmfHandler;

    /**
     * Creates an instance of <tt>DialPanel</tt> for a specific call, by
     * specifying the parent <tt>CallManager</tt> and the
     * <tt>CallPeer</tt>.
     *
     * @param dtmfHandler handles DTMFs.
     */
    public DialPanel(DTMFHandler dtmfHandler)
    {
        this.dtmfHandler = dtmfHandler;

        // Initialize this panel by adding all dial buttons to it.
        ResourceManagementService r = GuiActivator.getResources();
        int hgap = r.getSettingsInt("impl.gui.DIAL_PAD_HORIZONTAL_GAP");
        int vgap = r.getSettingsInt("impl.gui.DIAL_PAD_VERTICAL_GAP");
        int width = r.getSettingsInt("impl.gui.DIAL_PAD_WIDTH");
        int height = r.getSettingsInt("impl.gui.DIAL_PAD_HEIGHT");

        dialPadPanel = new JPanel(new GridLayout(4, 3, hgap, vgap));
        dialPadPanel.setOpaque(false);
        dialPadPanel.setPreferredSize(new Dimension(width, height));

        setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(dialPadPanel, BorderLayout.CENTER);

        loadSkin();
    }

    /**
     * Creates DTMF button.
     *
     * @param bgImage
     * @param iconImage
     * @param name
     * @return the created dial button
     */
    private JButton createDialButton(
            Image bgImage,
            ImageID iconImage,
            String name)
    {
        JButton button
            = new SIPCommButton(bgImage, ImageLoader.getImage(iconImage));

        button.setAlignmentY(JButton.LEFT_ALIGNMENT);
        button.setName(name);
        button.setOpaque(false);
        button.addMouseListener(this);
        return button;
    }

    /**
     * Reloads dial buttons.
     */
    public void loadSkin()
    {
        dialPadPanel.removeAll();

        Image bgImage = ImageLoader.getImage(ImageLoader.DIAL_BUTTON_BG);
        DTMFHandler.DTMFToneInfo[] availableTones = DTMFHandler.AVAILABLE_TONES;

        for (int i = 0; i < availableTones.length; i++)
        {
            DTMFHandler.DTMFToneInfo info = availableTones[i];

            // we add only buttons having image
            if (info.imageID != null)
            {
                dialPadPanel.add(
                        createDialButton(
                                bgImage,
                                info.imageID,
                                info.tone.getValue()));
            }
        }
    }

    public void mouseClicked(MouseEvent ev) {}

    public void mouseEntered(MouseEvent ev) {}

    public void mouseExited(MouseEvent ev) {}

    /**
     * Handles the <tt>MouseEvent</tt> triggered when user presses one of the
     * dial buttons.
     *
     * @param ev the event
     */
    public void mousePressed(MouseEvent ev)
    {
        dtmfHandler.startSendingDtmfTone(ev.getComponent().getName());
    }

    /**
     * Handles the <tt>MouseEvent</tt> triggered when user releases one of the
     * dial buttons.
     *
     * @param ev the event
     */
    public void mouseReleased(MouseEvent ev)
    {
        dtmfHandler.stopSendingDtmfTone();
    }

    /**
     * Paints the main background image to the background of this dial panel.
     *
     * @param g the <tt>Graphics</tt> object used for painting
     */
    @Override
    public void paintComponent(Graphics g)
    {
        // do the superclass behavior first
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        BufferedImage bgImage
            = ImageLoader.getImage(ImageLoader.MAIN_WINDOW_BACKGROUND);

        // paint the image
        if (bgImage != null)
        {
            ResourceManagementService r = GuiActivator.getResources();
            boolean isTextureBackground
                = Boolean.parseBoolean(
                        r.getSettingsString(
                                "impl.gui.IS_CONTACT_LIST_TEXTURE_BG_ENABLED"));
            int width = getWidth(), height = getHeight();

            if (isTextureBackground)
            {
                Rectangle rect
                    = new Rectangle(
                            0,
                            0,
                            bgImage.getWidth(null),
                            bgImage.getHeight(null));
                TexturePaint texture = new TexturePaint(bgImage, rect);

                g2.setPaint(texture);
                g2.fillRect(0, 0, width, height);
            }
            else
            {
                g.setColor(new Color(r.getColor("contactListBackground")));
                // paint the background with the chosen color
                g.fillRect(0, 0, width, height);
                g2.drawImage(
                        bgImage,
                        width - bgImage.getWidth(),
                        height - bgImage.getHeight(),
                        this);
            }
        }
    }
}
