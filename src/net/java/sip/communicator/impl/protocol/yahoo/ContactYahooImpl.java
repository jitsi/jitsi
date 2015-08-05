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
package net.java.sip.communicator.impl.protocol.yahoo;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.yahooconstants.*;
import net.java.sip.communicator.util.*;
import ymsg.network.*;

/**
 * The Yahoo implementation of the service.protocol.Contact interface.
 * @author Damian Minkov
 * @author Emil Ivov
 */
public class ContactYahooImpl
    extends AbstractContact
{
    private static final Logger logger =
        Logger.getLogger(ContactYahooImpl.class);

    private YahooUser contact = null;
    private byte[] image = null;
    private PresenceStatus status = YahooStatusEnum.OFFLINE;
    private ServerStoredContactListYahooImpl ssclCallback = null;
    private boolean isPersistent = false;
    private boolean isResolved = false;
    private boolean isVolatile = false;

    private String yahooID = null;
    private String id = null;

    private String statusMessage = null;

    /**
     * Creates an YahooContactImpl with custom yahooID
     * @param yahooID sets the contact Id if its different from the YahooUser id
     * @param contact the contact object that we will be encapsulating.
     * @param ssclCallback a reference to the ServerStoredContactListImpl
     * instance that created us.
     * @param isPersistent determines whether this contact is persistent or not.
     * @param isResolved specifies whether the contact has been resolved against
     * the server contact list
     */
    ContactYahooImpl(
                   String yahooID,
                   YahooUser contact,
                   ServerStoredContactListYahooImpl ssclCallback,
                   boolean isPersistent,
                   boolean isResolved)
    {
        this.yahooID = yahooID;

        this.contact = contact;
        this.ssclCallback = ssclCallback;
        this.isPersistent = isPersistent;
        this.isResolved = isResolved;

        if(contact != null)
            id = contact.getId();
        else if(yahooID != null)
            id = YahooSession.getYahooUserID(yahooID);
    }

    /**
     * Creates an YahooContactImpl
     * @param contact the contact object that we will be encapsulating.
     * @param ssclCallback a reference to the ServerStoredContactListImpl
     * instance that created us.
     * @param isPersistent determines whether this contact is persistent or not.
     * @param isResolved specifies whether the contact has been resolved against
     * the server contact list
     */
    ContactYahooImpl(
                   YahooUser contact,
                   ServerStoredContactListYahooImpl ssclCallback,
                   boolean isPersistent,
                   boolean isResolved)
    {
        this(null, contact, ssclCallback, isPersistent, isResolved);
    }

    /**
     * Creates volatile or unresolved contact
     */
    ContactYahooImpl(
        String id,
        ServerStoredContactListYahooImpl ssclCallback,
        boolean isResolved,
        boolean isPersistent,
        boolean isVolatile)
    {
        this.yahooID = id;
        this.ssclCallback = ssclCallback;
        this.isPersistent = isPersistent;
        this.isResolved = isResolved;
        this.isVolatile = isVolatile;

        if(id != null)
            this.id = YahooSession.getYahooUserID(yahooID);
    }

    /**
     * Returns the Yahoo Userid of this contact
     * @return the Yahoo Userid of this contact
     */
    public String getAddress()
    {
        // if the contact is volatile or with custom id return it
        if(yahooID != null)
            return yahooID;
        // otherwise return the supplied contact id
        else
            return contact.getId();
    }

    /**
     * Returns the custom yahooID if set
     */
    String getYahooID()
    {
        return yahooID;
    }

    /**
     * Returns the contact Id.
     * If contact missing the yahooID without @yahoo.com part is returned
     */
    String getID()
    {
        return id;
    }

    /**
     * Returns whether the contact is volatile.
     */
    boolean isVolatile()
    {
        return isVolatile;
    }

    /**
     * Returns an avatar if one is already present or <tt>null</tt> in case it
     * is not in which case it the method also queues the contact for image
     * updates.
     *
     * @return the avatar of this contact or <tt>null</tt> if no avatar is
     * currently available.
     */
    public byte[] getImage()
    {
        return getImage(true);
    }

    /**
     * Returns a reference to the image assigned to this contact. If no image
     * is present and the retrieveIfNecessary flag is true, we schedule the
     * image for retrieval from the server.
     *
     * @param retrieveIfNecessary specifies whether the method should queue
     * this contact for avatar update from the server.
     *
     * @return a reference to the image currently stored by this contact.
     */
    public byte[] getImage(boolean retrieveIfNecessary)
    {
        try
        {
            if(retrieveIfNecessary)
            {
                if(ssclCallback.getParentProvider() == null
                    || !ssclCallback.getParentProvider().isRegistered())
                {
                    throw new IllegalStateException(
                        "The provider must be signed on the service before "
                        +"being able to communicate.");
                }

                YahooSession ses = ssclCallback.getParentProvider().
                    getYahooSession();
                if(image == null && ses != null)
                    ses.requestPicture(id);
            }
        }
        catch (Exception e)
        {
            if (logger.isInfoEnabled())
                logger.info("Error requesting image!", e);
        }

        if(logger.isDebugEnabled())
            logger.debug("returning picture " + image);

        return image;
    }

    /**
     * Used to set the image of the contact if it is updated
     *
     * @param image a photo/avatar associated with this contact.
     */
    protected void setImage(byte[] image)
    {
        if (logger.isInfoEnabled())
            logger.info("setting image " + image);

        this.image = image;
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
        StringBuffer buff =  new StringBuffer("YahooContact[ id=");
        buff.append(getAddress()).append("]");

        return buff.toString();
    }

    /**
     * Sets the status that this contact is currently in. The method is to
     * only be called as a result of a status update received from the server.
     *
     * @param status the YahooStatusEnum that this contact is currently in.
     */
    void updatePresenceStatus(PresenceStatus status)
    {
        this.status = status;
    }

    /**
     * Returns the status of the contact as per the last status update we've
     * received for it. Note that this method is not to perform any network
     * operations and will simply return the status received in the last
     * status update message. If you want a reliable way of retrieving someone's
     * status, you should use the <tt>queryContactStatus()</tt> method in
     * <tt>OperationSetPresence</tt>.
     * @return the PresenceStatus that we've received in the last status update
     * pertaining to this contact.
     */
    public PresenceStatus getPresenceStatus()
    {
        return status;
    }

    /**
     * Returns a String that could be used by any user interacting modules for
     * referring to this contact. An alias is not necessarily unique but is
     * often more human readable than an address (or id).
     * @return a String that can be used for referring to this contact when
     * interacting with the user.
     */
    public String getDisplayName()
    {
        return getAddress();
    }

    /**
     * Returns a reference to the contact group that this contact is currently
     * a child of or null if the underlying protocol does not suppord persistent
     * presence.
     * @return a reference to the contact group that this contact is currently
     * a child of or null if the underlying protocol does not suppord persistent
     * presence.
     */
    public ContactGroup getParentContactGroup()
    {
        return ssclCallback.findContactGroup(this);
    }


    /**
     * Returns a reference to the protocol provider that created the contact.
     * @return a refererence to an instance of the ProtocolProviderService
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return ssclCallback.getParentProvider();
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
     * @return true if the contact is persistent and false otherwise.
     */
    public boolean isPersistent()
    {
        return isPersistent;
    }

    /**
     * Specifies whether this contact is to be considered persistent or not. The
     * method is to be used _only_ when a non-persistent contact has been added
     * to the contact list and its encapsulated VolatileBuddy has been repalced
     * with a standard buddy.
     * @param persistent true if the buddy is to be considered persistent and
     * false for volatile.
     */
    void setPersistent(boolean persistent)
    {
        this.isPersistent = persistent;
    }

    /**
     * Resolve this contact against the given entry
     * @param entry the server stored entry
     */
    void setResolved(YahooUser entry)
    {
        if(isResolved)
            return;

        this.isResolved = true;
        contact = entry;
        isVolatile = false;
    }

    /**
     * Returns the persistent data
     * @return the persistent data
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
     * @return true if the contact has been resolved (mapped against a buddy)
     * and false otherwise.
     */
    public boolean isResolved()
    {
        return isResolved;
    }

    public void setPersistentData(String persistentData)
    {
    }

    /**
     * Get source contact
     * @return YahooContact
     */
    YahooUser getSourceContact()
    {
        return contact;
    }

    /**
     * Return the current status message of this contact.
     *
     * @return the current status message
     */
    public String getStatusMessage()
    {
        return statusMessage;
    }

    /**
     * Sets the current status message for this contact
     * @param statusMessage the message
     */
    protected void setStatusMessage(String statusMessage)
    {
        this.statusMessage = statusMessage;
    }
}
