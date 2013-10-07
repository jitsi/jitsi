/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
