/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.ldap.event;

import java.util.*;


/**
 * An LdapEvent is triggered when
 * the state of the LDAP connection changes
 * or when a search result is received
 *
 * @author Sebastien Mazy
 */
public interface LdapListener
    extends EventListener
{
    /**
     * This method gets called when
     * a server need to send a message (person found, search status)
     *
     * @param event An LdapEvent probably sent by an LdapDirectory
     */
    public void ldapEventReceived(LdapEvent event);
}
