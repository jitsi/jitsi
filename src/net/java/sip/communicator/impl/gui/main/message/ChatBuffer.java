/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.util.ArrayList;

import net.java.sip.communicator.impl.gui.utils.Constants;

/**
 * Not used for now.
 * @author Yana Stamcheva
 */
public class ChatBuffer extends ArrayList {

    public ChatBuffer() {
        super(Constants.CHAT_BUFFER_SIZE);
    }

    private void recalculateBuffer() {

        if (this.size() >= Constants.CHAT_BUFFER_SIZE) {
            this.remove(0);
        }
    }

    public boolean add(Object o) {

        this.recalculateBuffer();

        return super.add(o);
    }
}
