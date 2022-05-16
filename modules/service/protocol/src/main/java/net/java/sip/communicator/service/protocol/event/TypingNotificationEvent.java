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
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * <tt>TypingNotificationEvent</tt>s are delievered upon reception of a
 * corresponding message from a remote contact.
 * <tt>TypingNotificationEvent</tt>s contain a state id, identifying the exact
 * typing event that has occurrend (a user has started or stopped typing at us),
 * the source <tt>Contact</tt> that generated the event and others.
 * @author Emil Ivov
 */
public class TypingNotificationEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private int typingState = OperationSetTypingNotifications.STATE_UNKNOWN;

    /**
     * Creats a TypingNotificationEvent with the specified parameters.
     * <p>
     * @param sourceContact the <tt>Contact</tt> that has sen the notification.
     * @param state
     */
    public TypingNotificationEvent(Contact sourceContact, int state)
    {
        super(sourceContact);
        this.typingState = state;
    }

    /**
     * Returns the typing state that this <tt>TypingNotificationEvent</tt> is
     * carrying.
     * @return one of the <tt>STATE_XXX int</tt>s indicating the typing state
     * that this notification is about.
     */
    public int getTypingState()
    {
        return typingState;
    }

    /**
     * Returns a reference to the <tt>Contact</tt> that has sent this event.
     * @return a reference to the <tt>Contact</tt> whose typing state we're
     * being notified about.
     */
    public Contact getSourceContact()
    {
        return (Contact)getSource();
    }

    /**
     * Returns a String representation of this EventObject.
     *
     * @return  A a String representation of this EventObject.
     */
    @Override
    public String toString()
    {
        return new StringBuffer("TypingNotificationEvent[from=")
            .append(getSourceContact().getDisplayName())
                .append(" state="+getTypingState()).toString();
    }

}
