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
package net.java.sip.communicator.service.gui.event;

import java.util.*;

import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>ChatFocusEvent</tt> indicates that a <tt>Chat</tt> has gained or lost
 * the current focus.
 *
 * @author Yana Stamcheva
 */
public class ChatFocusEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * ID of the event.
     */
    private int eventID = -1;

    /**
     * Indicates that the ChatFocusEvent instance was triggered by
     * <tt>Chat</tt> gaining the focus.
     */
    public static final int FOCUS_GAINED = 1;

    /**
     * Indicates that the ChatFocusEvent instance was triggered by
     * <tt>Chat</tt> losing the focus.
     */
    public static final int FOCUS_LOST = 2;

    /**
     * Creates a new <tt>ChatFocusEvent</tt> according to the
     * specified parameters.
     * @param source The <tt>Chat</tt> that triggers the event.
     * @param eventID one of the FOCUS_XXX static fields indicating the
     * nature of the event.
     */
    public ChatFocusEvent(Object source, int eventID)
    {
        super(source);
        this.eventID = eventID;
    }

    /**
     * Returns an event id specifying what is the type of this event
     * (FOCUS_GAINED or FOCUS_LOST)
     * @return one of the REGISTRATION_XXX int fields of this class.
     */
    public int getEventID(){
        return eventID;
    }

    /**
     * Returns the <tt>Chat</tt> object that corresponds to this event.
     *
     * @return the <tt>Chat</tt> object that corresponds to this event
     */
    public Chat getChat()
    {
        return (Chat) source;
    }
}
