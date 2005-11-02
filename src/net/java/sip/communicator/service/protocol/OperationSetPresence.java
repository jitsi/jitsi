/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.PresenceStatusListener;

/**
 * @todo Say that ContactList management and Presence functions
 * @todo Say that presence implementations should not contain any persistant
 * data on contactlists and who they should be keeping records of
 *
 * @author Emil Ivov
 */
public interface OperationSetPresence
    extends OperationSet
{
    public ContactList retrieveContactList();

    public void addPresenceStatusListener(PresenceStatusListener listener);

    public PresenceStatus getStatusForContact();

    /**
     * Some protos support batch status requests. We should therefore give them
     * the possibility to execute them instead of demanding status one by one
     * @param list ContactList
     */
    public void addPresenceStatusSubscriptions(ContactList list);


    public void setAuthorizationHandler(AuthorizationHandler handler);

}
