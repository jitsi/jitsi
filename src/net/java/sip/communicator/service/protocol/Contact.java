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
 * protocol. Instances of Contact could be used for delivery of presence
 * notifications or when addressing instant messages.
 *
 *
 * @author Emil Ivov
 */
public interface Contact
{
    /**
     * Returns a String that can be used for identifying the contact. The
     * exact contents of the string depends on the protocol. In the case of
     * SIP, for example, that would be the SIP uri (e.g. sip:alice@biloxi.com)
     * in the case of icq - a UIN (12345653) and for AIM a screenname (mysname).
     * Jabber (and hence Google) would be having e-mail like addresses.
     * @return a String id representing and uniquely identifying the contact.
     */
    public String getAddress();

    /**
     * Returns a String that could be used by any user interacting modules for
     * referring to this contact. An alias is not necessarily unique but is
     * often more human readable than an address (or id).
     * @return a String that can be used for referring to this contact when
     * interacting with the user.
     */
    public String getAlias();

    /**
     * Returns a byte array containing an image (most often a photo or an avatar)
     * that the contact uses as a representation.
     * @return byte[] an image representing the contact.
     */
    public byte[] getImage();

    /**
     * Determines whether or not this contact represents our own identity.
     * @return true in case this is a contact that represents ourselves and
     * false otherwise.
     */
    public boolean isLocal();

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

    /**
     * Returns a reference to the protocol provider that created the contact.
     * @return a refererence to an instance of the ProtocolProviderService
     */
    public ProtocolProviderService getProtocolProvider();

}
