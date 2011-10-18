/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.awt.event.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The dial pad button in the contact list.
 *
 * @author Yana Stamcheva
 */
public class DialPadButton
    extends SIPCommTextButton
{
    /**
     * The dial pad dialog that this button opens.
     */
    GeneralDialPadDialog dialPad;

    /**
     * Creates an instance of <tt>DialPadButton</tt>.
     */
    public DialPadButton()
    {
        super("");

        loadSkin();

        addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (dialPad == null)
                    dialPad = new GeneralDialPadDialog();

                dialPad.clear();
                dialPad.setVisible(true);
            }
        });
    }

    /**
     * Loads images and sets history view.
     */
    public void loadSkin()
    {
        Image image = ImageLoader.getImage(ImageLoader.CONTACT_LIST_DIAL_BUTTON);

        setBgImage(image);

        this.setPreferredSize(new Dimension(image.getWidth(this),
                                            image.getHeight(this)));
    }
}
