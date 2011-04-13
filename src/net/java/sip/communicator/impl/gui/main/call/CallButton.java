/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * 
 * @author Yana Stamcheva
 */
public class CallButton
    extends SIPCommButton
    implements Skinnable
{
    /**
     * The history icon.
     */
    private Image image;

    /**
     * The history pressed icon.
     */
    private Image pressedImage;

    /**
     * The rollover image.
     */
    private Image rolloverImage;

    /**
     * Creates the contact list call button.
     *
     * @param mainFrame the main window
     */
    public CallButton(final MainFrame mainFrame)
    {
        loadSkin();

        addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                List<ProtocolProviderService> telephonyProviders
                    = CallManager.getTelephonyProviders();

                String searchText = mainFrame.getCurrentSearchText();
                if (telephonyProviders.size() == 1)
                {
                    CallManager.createCall(
                        telephonyProviders.get(0),
                        searchText);
                }
                else if (telephonyProviders.size() > 1)
                {
                    ChooseCallAccountPopupMenu chooseAccountDialog
                        = new ChooseCallAccountPopupMenu(
                            CallButton.this,
                            searchText,
                            telephonyProviders);

                    chooseAccountDialog
                        .setLocation(CallButton.this.getLocation());
                    chooseAccountDialog.showPopupMenu();
                }
            }
        });
    }

    /**
     * Loads button images.
     */
    public void loadSkin()
    {
        image
            = GuiActivator.getResources()
                .getImage("service.gui.buttons.CALL_NUMBER_BUTTON").getImage();

        pressedImage
            = GuiActivator.getResources()
                .getImage("service.gui.buttons.CALL_NUMBER_BUTTON_PRESSED")
                    .getImage();

        rolloverImage
            = GuiActivator.getResources()
                .getImage("service.gui.buttons.CALL_NUMBER_BUTTON_ROLLOVER")
                    .getImage();

        setBackgroundImage(image);
        setPressedImage(pressedImage);
        setRolloverIcon(rolloverImage);

        this.setPreferredSize(new Dimension(image.getWidth(this),
                                            image.getHeight(this)));
    }
}