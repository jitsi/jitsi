/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.Image;

import net.java.sip.communicator.impl.gui.utils.ImageLoader;

/**
 * The <tt>MsgToolbarButton</tt> is a <tt>SIPCommButton</tt> that has
 * specific background and rollover images. It is used in the chat window
 * toolbar.
 * 
 * @author Yana Stamcheva
 */
public class MsgToolbarButton extends SIPCommButton {

    /**
     * Creates an instance of <tt>MsgToolbarButton</tt>.
     * @param iconImage The icon to display on this button.
     */
    public MsgToolbarButton(Image iconImage) {
        super(ImageLoader.getImage(ImageLoader.MSG_TOOLBAR_BUTTON_BG),
                ImageLoader
                        .getImage(ImageLoader.MSG_TOOLBAR_ROLLOVER_BUTTON_BG),
                iconImage);
    }
}
