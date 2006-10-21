/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;

import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The <tt>MsgToolbarButton</tt> is a <tt>SIPCommButton</tt> that has
 * specific background and rollover images. It is used in the chat window
 * toolbar.
 * 
 * @author Yana Stamcheva
 */
public class ChatToolbarButton extends SIPCommButton {

    /**
     * Creates an instance of <tt>MsgToolbarButton</tt>.
     * @param iconImage The icon to display on this button.
     */
    public ChatToolbarButton(Image iconImage) {
        super(ImageLoader.getImage(ImageLoader.CHAT_TOOLBAR_BUTTON_BG),
                ImageLoader
                        .getImage(ImageLoader.CHAT_TOOLBAR_ROLLOVER_BUTTON_BG),
                iconImage,
                null);
    }
}
