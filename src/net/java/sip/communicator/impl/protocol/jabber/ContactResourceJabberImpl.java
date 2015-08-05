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
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.*;

/**
 *
 * @author Yana Stamcheva
 */
public class ContactResourceJabberImpl
    extends ContactResource
{
    private final String fullJid;

    /**
     * Creates a <tt>ContactResource</tt> by specifying the
     * <tt>resourceName</tt>, the <tt>presenceStatus</tt> and the
     * <tt>priority</tt>.
     *
     * @param fullJid the full jid corresponding to this contact resource
     * @param contact
     * @param resourceName
     * @param presenceStatus
     * @param priority
     */
    public ContactResourceJabberImpl(   String fullJid,
                                        Contact contact,
                                        String resourceName,
                                        PresenceStatus presenceStatus,
                                        int priority,
                                        boolean isMobile)
    {
        super(  contact,
                resourceName,
                presenceStatus,
                priority,
                isMobile);

        this.fullJid = fullJid;
    }

    /**
     * Returns the full jid corresponding to this contact resource.
     *
     * @return the full jid corresponding to this contact resource
     */
    public String getFullJid()
    {
        return fullJid;
    }

    /**
     * Sets the new <tt>PresenceStatus</tt> of this resource.
     *
     * @param newStatus the new <tt>PresenceStatus</tt> to set
     */
    protected void setPresenceStatus(PresenceStatus newStatus)
    {
        this.presenceStatus = newStatus;
    }

    /**
     * Changed whether contact is mobile one. Logged in only from mobile device.
     * @param isMobile whether contact is mobile one.
     */
    public void setMobile(boolean isMobile)
    {
        this.mobile = isMobile;
    }

    /**
     * Changes resource priority.
     * @param priority the new priority
     */
    public void setPriority(int priority)
    {
        this.priority = priority;
    }
}
