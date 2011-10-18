/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>DialPanel</tt> is the panel that contains the buttons to dial a
 * phone number.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class GeneralDialPanel
    extends TransparentPanel
    implements  MouseListener,
                Skinnable
{
    /**
     * The dial panel.
     */
    private final JPanel dialPadPanel =
        new TransparentPanel(new GridLayout(4, 3,
            GuiActivator.getResources()
                .getSettingsInt("impl.gui.DIAL_PAD_HORIZONTAL_GAP"),
            GuiActivator.getResources()
                .getSettingsInt("impl.gui.DIAL_PAD_VERTICAL_GAP")));

    /**
     * Handles DTMFs.
     */
    private DTMFHandler dtmfHandler;

    /**
     * The parent dial pad dialog.
     */
    private final GeneralDialPadDialog dialPadDialog;

    /**
     * Creates an instance of <tt>DialPanel</tt> for a specific call, by
     * specifying the parent <tt>CallManager</tt> and the
     * <tt>CallPeer</tt>.
     *
     * @param dtmfHandler handles DTMFs.
     */
    public GeneralDialPanel(GeneralDialPadDialog dialPadDialog,
                            DTMFHandler dtmfHandler)
    {
        this.dialPadDialog = dialPadDialog;
        this.dtmfHandler = dtmfHandler;

        this.init();
    }

    /**
     * Initializes this panel by adding all dial buttons to it.
     */
    public void init()
    {
        this.dialPadPanel.setOpaque(false);

        this.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        loadSkin();

        this.add(dialPadPanel, BorderLayout.CENTER);
    }

    /**
     * Creates DTMF button.
     *
     * @param bgImage
     * @param iconImage
     * @param name
     * @return the created dial button
     */
    private JButton createDialButton(Image bgImage, ImageID iconImage,
        String name)
    {
        JButton button =
            new SIPCommButton(bgImage, ImageLoader.getImage(iconImage));

        button.setAlignmentY(JButton.LEFT_ALIGNMENT);
        button.setName(name);
        button.setOpaque(false);
        button.addMouseListener(this);
        return button;
    }

    /**
     * Creates DTMF button.
     *
     * @param bgImage
     * @param iconImage
     * @param name
     * @return the created dial button
     */
    private JButton createMacOSXDialButton( ImageID imageID,
                                            ImageID rolloverImageID,
                                            String name)
    {
        JButton button = new SIPCommButton(
            ImageLoader.getImage(imageID),
            ImageLoader.getImage(rolloverImageID),
            ImageLoader.getImage(rolloverImageID),
            null,
            null,
            null);

        button.setName(name);
        button.addMouseListener(this);

        return button;
    }

    public void mouseClicked(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    /**
     * Handles the <tt>MouseEvent</tt> triggered when user presses one of the
     * dial buttons.
     * @param e the event
     */
    public void mousePressed(MouseEvent e)
    {
        JButton button = (JButton) e.getSource();

        dialPadDialog.dialButtonPressed(button.getName());
        dtmfHandler.startSendingDtmfTone(button.getName());
    }

    /**
     * Handles the <tt>MouseEvent</tt> triggered when user releases one of the
     * dial buttons.
     * @param e the event
     */
    public void mouseReleased(MouseEvent e)
    {
        dtmfHandler.stopSendingDtmfTone();
    }

    /**
     * Paints the main background image to the background of this dial panel.
     *
     * @param g the <tt>Graphics</tt> object used for painting
     */
    public void paintComponent(Graphics g)
    {
     // do the superclass behavior first
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        boolean isTextureBackground
            = Boolean.parseBoolean(GuiActivator.getResources()
            .getSettingsString("impl.gui.IS_CONTACT_LIST_TEXTURE_BG_ENABLED"));

        BufferedImage bgImage
            = ImageLoader.getImage(ImageLoader.MAIN_WINDOW_BACKGROUND);

        // paint the image
        if (bgImage != null)
        {
            if (isTextureBackground)
            {
                Rectangle rect
                    = new Rectangle(0, 0,
                            bgImage.getWidth(null),
                            bgImage.getHeight(null));

                TexturePaint texture = new TexturePaint(bgImage, rect);

                g2.setPaint(texture);

                g2.fillRect(0, 0, this.getWidth(), this.getHeight());
            }
            else
            {
                g.setColor(new Color(
                    GuiActivator.getResources()
                        .getColor("contactListBackground")));

                // paint the background with the choosen color
                g.fillRect(0, 0, getWidth(), getHeight());

                g2.drawImage(bgImage,
                        this.getWidth() - bgImage.getWidth(),
                        this.getHeight() - bgImage.getHeight(),
                        this);
            }
        }
    }

    /**
     * Reloads dial buttons.
     */
    public void loadSkin()
    {
        dialPadPanel.removeAll();

        Image bgImage = ImageLoader.getImage(ImageLoader.DIAL_BUTTON_BG);

        DTMFHandler.DTMFToneInfo[] availableTones = DTMFHandler.availableTones;
        for (int i = 0; i < availableTones.length; i++)
        {
            DTMFHandler.DTMFToneInfo info = availableTones[i];

            // we add only buttons having image
            if(info.imageID == null)
                continue;

            JComponent c;
            if (OSUtils.IS_MAC)
                c = createMacOSXDialButton(
                        info.macImageID,
                        info.macImageRolloverID,
                        info.tone.getValue());
            else
                c = createDialButton(
                        bgImage, info.imageID, info.tone.getValue());

            dialPadPanel.add(c);
        }
    }
}
