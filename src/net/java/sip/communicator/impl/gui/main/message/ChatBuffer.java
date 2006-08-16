/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.util.*;

import net.java.sip.communicator.impl.gui.utils.*;

/**
 * Should be used to limit the number of messages shown in the chat conversation
 * area. NOT USED FOR NOW.
 * 
 * @author Yana Stamcheva
 */
public class ChatBuffer extends ArrayList {

    /**
     * Creates an instance of <tt>ChatBuffer</tt>. The buffer
     * is initialized with the CHAT_BUFFER_SIZE constant in the
     * <tt>Constants</tt> class.
     */
    public ChatBuffer() {
        super(Constants.CHAT_BUFFER_SIZE);
    }

    /**
     * Removes the first buffer element when the buffer limit is
     * reached.
     */
    private void recalculateBuffer() {

        if (this.size() >= Constants.CHAT_BUFFER_SIZE) {
            this.remove(0);
        }
    }

    /**
     * Adds an object to the buffer.
     * @param o The <tt>Object</tt> to add.
     * @return <code>true</code> (as per the general contract of the
     * Collection.add)
     */
    public boolean add(Object o) {
        this.recalculateBuffer();

        return super.add(o);
    }
}
