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
package net.java.sip.communicator.impl.gui.main.chat;

import java.util.*;

import net.java.sip.communicator.service.gui.*;

/**
 * Should be used to limit the number of messages shown in the chat conversation
 * area. NOT USED FOR NOW.
 *
 * @author Yana Stamcheva
 */
public class ChatBuffer<E> extends ArrayList<E> {
    private static final long serialVersionUID = 0L;

    /**
     * Creates an instance of <tt>ChatBuffer</tt>. The buffer
     * is initialized with the CHAT_BUFFER_SIZE constant in the
     * <tt>Constants</tt> class.
     */
    public ChatBuffer() {
        super(Chat.CHAT_BUFFER_SIZE);
    }

    /**
     * Removes the first buffer element when the buffer limit is
     * reached.
     */
    private void recalculateBuffer() {
        if (this.size() >= Chat.CHAT_BUFFER_SIZE) {
            this.remove(0);
        }
    }

    /**
     * Adds an object to the buffer.
     * @param o The <tt>Object</tt> to add.
     * @return <code>true</code> (as per the general contract of the
     * Collection.add)
     */
    @Override
    public boolean add(E o) {
        this.recalculateBuffer();

        return super.add(o);
    }
}
