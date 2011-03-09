/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.ldap.event;

import java.util.*;

import net.java.sip.communicator.service.ldap.*;


/**
 * An LdapEvent is triggered when
 * the state of the LDAP connection changes.
 * Available states for the moment are :
 * connected
 * disconnected
 * connecting
 * disconnecting
 *
 * @author Sebastien Mazy
 */
public class LdapEvent
    extends EventObject
{
    /**
     * Indicates the possible causes why an event was triggered
     */
    public static enum LdapEventCause { NEW_SEARCH_RESULT, SEARCH_ACHIEVED, SEARCH_CANCELLED, SEARCH_ERROR }

    /**
     * the cause of this event
     */
    private final LdapEventCause cause;

    /**
     * the content of this event
     */
    private final Object content;


    /**
     * Simple constructor for this class
     *
     * @param source the source of the event (most likely an LdapDirectory)
     * @param cause the cause why it was triggered
     */
    public LdapEvent(LdapEventManager source, LdapEventCause cause)
    {
        this(source, cause, null);
    }

    /**
     * Another constructor for this class. Use that one to pass more
     * information to the listener using any Object.
     *
     * @param source the source of the event (most likely an LdapDirectory)
     * @param cause the cause why it was triggered
     * @param content related content
     */
    public LdapEvent(LdapEventManager source, LdapEventCause cause, Object content)
    {
        super(source);
        this.cause = cause;
        this.content = content;
    }

    /**
     * @return the cause why this event was triggered
     */
    public LdapEventCause getCause()
    {
        return this.cause;
    }

    /**
     * @return the object embedded in this event, or null if there isn't one
     */
    public Object getContent()
    {
        return this.content;
    }

    /**
     * Returns the LdapEventManager which sent the LdapEvent.
     *
     * @return the LdapEventManager which sent the LdapEvent
     */
    public LdapEventManager getSource()
    {
        return (LdapEventManager) super.getSource();
    }
}
