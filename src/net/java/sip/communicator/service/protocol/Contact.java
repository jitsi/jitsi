/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

/**
 * This class represents the notion of a Contact or Buddy, that is widely used
 * in instant messaging today. From a protocol point of view, a contact is
 * generally considered to be another user of the service that proposes the
 * protocol
 *
 *
 * @author Emil Ivov
 */
public interface Contact
{
    //address
    public String getAddress();

    //image
    public byte[] getImage();

    //islocal
    public boolean isLocal();

    //getLastReceivedPresenceStatus
    /**
     * Returns the status of the contact as per the last status update we've
     * received for it. Note that this method is not to perform any network
     * operations and will simply return the status received in the last
     * status update message. If you want a reliable way of retrieving someone's
     * status, you should use the <code>queryContactStatus()</code> method in
     * <code>OperationSetPresence</code>.
     * @return the PresenceStatus that we've received in the last status update
     * pertaining to this contact.
     */
    public PresenceStatus getPresenceStatus();
}
