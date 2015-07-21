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
package net.java.sip.communicator.impl.protocol.zeroconf;

import java.net.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * A simple, straightforward implementation of a zeroconf Contact. Since
 * the Zeroconf protocol is not a real one, we simply store all contact details
 * in class fields. You should know that when implementing a real protocol,
 * the contact implementation would rather encapsulate contact objects from
 * the protocol stack and group property values should be returned after
 * consulting the encapsulated object.
 *
 * @author Christian Vincenot
 * @author Maxime Catelin
 * @author Jonathan Martin
 */
public class ContactZeroconfImpl
    extends AbstractContact
{
    private static final Logger logger
        = Logger.getLogger(ContactZeroconfImpl.class);


    /**
     * The id of the contact.
     */
    private String contactID = null;

    /**
     * The ClientThread attached to this contact if we're already chatting
     * with him.
     */
    private ClientThread thread = null;

    /*
     * Type of Client.
     */
    /**
     * Gaim/Pidgin client type
     */
    public static final int GAIM = 1;
    /**
     * iChat client type
     */
    public static final int ICHAT = 2;
    /**
     * XMPP - XEP-0174 client type
     */
    public static final int XMPP = 3;
    /**
     * Another SIP Communicator client
     */
    public static final int SIPCOM = 4;
    private int clientType = XMPP;


    /**
     * The provider that created us.
     */
    private ProtocolProviderServiceZeroconfImpl parentProvider = null;


    /*
     * The Bonjour Service who discovered this contact.
     * TODO: This could probably be avoided using only the
     * Protocol Provider.
     */
    private BonjourService bonjourService;

    /**
     * The group that belong to.
     */
    private ContactGroupZeroconfImpl parentGroup = null;

    /**
     * The presence status of the contact.
     */
    private PresenceStatus presenceStatus = ZeroconfStatusEnum.OFFLINE;

    /**
     * Determines whether this contact is persistent,
     * i.e. member of the contact list or whether it is here only temporarily.
     * Chris: should be set to false here
     */
    private boolean isPersistent = false;

    /**
     * Determines whether the contact has been resolved (i.e. we have a
     * confirmation that it is still on the server contact list).
     */
    private boolean isResolved = true;

    /**
     * IP Address
     */
    private InetAddress ipAddress;

    /**
     * Port on which Bonjour is listening.
     */
    private int port;

    /**
     * Name announced by Bonjour.
     */
    private String name;

    /**
     * Contact personal message
     */
    private String message;


    /**
     * Creates an instance of a meta contact with the specified string used
     * as a name and identifier.
     * @param bonjourId ID of the contact
     * @param bonjourService BonjourService responsible for handling chat with
     * this contact
     * @param name Display name of this contact
     * @param ipAddress IP address of this contact
     * @param port Port declared by this contact for direct point-to-point chat
     * @param parentProvider the provider that created us.
     */
    public ContactZeroconfImpl(
                String bonjourId,
                ProtocolProviderServiceZeroconfImpl parentProvider,
                BonjourService bonjourService,
                String name,
                InetAddress ipAddress,
                int port)
    {
        this.contactID = bonjourId;
        this.parentProvider = parentProvider;
        this.bonjourService = bonjourService;
        this.name = name;
        this.ipAddress = ipAddress;
        this.port = port;
        bonjourService.addContact(this);
    }

    /**
     * This method is only called when the contact is added to a new
     * <tt>ContactGroupZeroconfImpl</tt> by the
     * <tt>ContactGroupZeroconfImpl</tt> itself.
     *
     * @param newParentGroup the <tt>ContactGroupZeroconfImpl</tt> that is now
     * parent of this <tt>ContactZeroconfImpl</tt>
     */
    void setParentGroup(ContactGroupZeroconfImpl newParentGroup)
    {
        this.parentGroup = newParentGroup;
    }

    /**
     * Return the BonjourService
     * @return BonjourService
     */
    public BonjourService getBonjourService()
    {
        return bonjourService;
    }

    /**
     * Return the ClientThread responsible for handling with this contact
     * @return ClientThread corresponding to the chat with this contact or null
     * if no chat was started
     */
    protected ClientThread getClientThread()
    {
        return thread;
    }

    /**
     * Set the ClientThread responsible for handling with this contact
     * @param thread ClientThread corresponding to the chat with this contact
     * or null if the chat is over
     */
    protected void setClientThread(ClientThread thread)
    {
        this.thread = thread;
    }

    /**
     * Return the type of client
     * @return Type of client used by this contact
     */
    public int getClientType()
    {
        return clientType;
    }

    /**
     * Sets the type of client
     * @param clientType Type of client used by this contact
     */
    public void setClientType(int clientType)
    {
        this.clientType = clientType;
    }

    /**
     * Returns a String that can be used for identifying the contact.
     *
     * @return a String id representing and uniquely identifying the contact.
     */
    public String getAddress()
    {
        return contactID;
    }

    /**
     * Returns a String that could be used by any user interacting modules
     * for referring to this contact.
     *
     * @return a String that can be used for referring to this contact when
     *   interacting with the user.
     */
    public String getDisplayName()
    {
        return name;
    }

    /**
     * Returns the IP address declared by this Contact
     * @return IP address declared by this Contact
     */
    public InetAddress getIpAddress()
    {
        return ipAddress;
    }

    /**
     * Returns the TCP port declared by this Contact for direct chat
     * @return the TCP port declared by this Contact for direct chat
     */
    public int getPort()
    {
        return port;
    }


    /**
     * Returns the status/private message displayed by this contact
     * @return the status/private message displayed by this contact
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Sets the status/private message displayed by this contact
     * @param message the status/private message displayed by this contact
     */
    public void setMessage(String message)
    {
        this.message = message;
    }


    /**
     * Returns a byte array containing an image (most often a photo or an
     * avatar) that the contact uses as a representation.
     *
     * @return byte[] an image representing the contact.
     */
    public byte[] getImage()
    {
        return null;
    }

    /**
     * Returns the status of the contact.
     *
     * @return always ZeroconfStatusEnum.
     */
    public PresenceStatus getPresenceStatus()
    {
        return this.presenceStatus;
    }

    /**
     * Sets <tt>zeroconfPresenceStatus</tt> as the PresenceStatus that this
     * contact is currently in.
     * @param zeroconfPresenceStatus the <tt>ZeroconfPresenceStatus</tt>
     * currently valid for this contact.
     */
    public void setPresenceStatus(PresenceStatus zeroconfPresenceStatus)
    {
        this.presenceStatus = zeroconfPresenceStatus;

        if (zeroconfPresenceStatus == ZeroconfStatusEnum.OFFLINE) {
            try
            {
                bonjourService.opSetPersPresence.unsubscribe(this);
            }
            catch (Exception ex)
            {
                logger.error(ex);
            }
        }
    }

    /**
     * Returns a reference to the protocol provider that created the contact.
     *
     * @return a refererence to an instance of the ProtocolProviderService
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return parentProvider;
    }

    /**
     * Determines whether or not this contact represents our own identity.
     *
     * @return true in case this is a contact that represents ourselves and
     *   false otherwise.
     */
    public boolean isLocal()
    {
        return false;
    }

    /**
     * Returns the group that contains this contact.
     * @return a reference to the <tt>ContactGroupZeroconfImpl</tt> that
     * contains this contact.
     */
    public ContactGroup getParentContactGroup()
    {
        return this.parentGroup;
    }

    /**
     * Returns a string representation of this contact, containing most of its
     * representative details.
     *
     * @return  a string representation of this contact.
     */
    @Override
    public String toString()
    {
        StringBuffer buff
            = new StringBuffer("ContactZeroconfImpl[ DisplayName=")
                .append(getDisplayName()).append("]");

        return buff.toString();
    }

    /**
     * Determines whether or not this contact is being stored by the server.
     * Non persistent contacts are common in the case of simple, non-persistent
     * presence operation sets. They could however also be seen in persistent
     * presence operation sets when for example we have received an event
     * from someone not on our contact list. Non persistent contacts are
     * volatile even when coming from a persistent presence op. set. They would
     * only exist until the application is closed and will not be there next
     * time it is loaded.
     *
     * @return true if the contact is persistent and false otherwise.
     */
    public boolean isPersistent()
    {
        return isPersistent;
    }

    /**
     * Specifies whether or not this contact is being stored by the server.
     * Non persistent contacts are common in the case of simple, non-persistent
     * presence operation sets. They could however also be seen in persistent
     * presence operation sets when for example we have received an event
     * from someone not on our contact list. Non persistent contacts are
     * volatile even when coming from a persistent presence op. set. They would
     * only exist until the application is closed and will not be there next
     * time it is loaded.
     *
     * @param isPersistent true if the contact is persistent and false
     * otherwise.
     */
    public void setPersistent(boolean isPersistent)
    {
        this.isPersistent = isPersistent;
    }


    /**
     * Returns null as no persistent data is required and the contact address is
     * sufficient for restoring the contact.
     * <p>
     * @return null as no such data is needed.
     */
    public String getPersistentData()
    {
        return null;
    }

    /**
     * Determines whether or not this contact has been resolved against the
     * server. Unresolved contacts are used when initially loading a contact
     * list that has been stored in a local file until the presence operation
     * set has managed to retrieve all the contact list from the server and has
     * properly mapped contacts to their on-line buddies.
     *
     * @return true if the contact has been resolved (mapped against a buddy)
     * and false otherwise.
     */
    public boolean isResolved()
    {
        return isResolved;
    }

    /**
     * Makes the contact resolved or unresolved.
     *
     * @param resolved  true to make the contact resolved; false to
     *                  make it unresolved
     */
    public void setResolved(boolean resolved)
    {
        this.isResolved = resolved;
    }

    /**
     * Returns the persistent presence operation set that this contact belongs
     * to.
     *
     * @return the <tt>OperationSetPersistentPresenceZeroconfImpl</tt> that
     * this contact belongs to.
     */
    public OperationSetPersistentPresenceZeroconfImpl
                                            getParentPresenceOperationSet()
    {
        return (OperationSetPersistentPresenceZeroconfImpl)parentProvider
            .getOperationSet(OperationSetPersistentPresence.class);
    }

    /**
     * Return the current status message of this contact.
     *
     * @return null as the protocol has currently no support of status messages
     */
    public String getStatusMessage()
    {
        return null;
    }
}
