/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.rss;

import java.io.*;
import java.net.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * An implementation of a rss Contact.
 *
 * @author Jean-Albert Vescovo
 * @author Mihai Balan
 */
public class ContactRssImpl
    implements Contact
{

    /**
     * Item key identifying the last item retrieved and displayed.
     */
    //private RssItemKey lastItem = new RssItemKey(new Date(0));

    /**
     * Contact's nickname.
     */
    private String nickName = null;

    /**
     * Stores the contact's display image to avoid downloading it multiple times.
     */
    private byte[] icon;

    /**
     * This contact's URL (URL of the RSS feed).
     */
    //private URL rssURL = null;

    /**
     * This contact id (http://... or feed://...)
     */
    private String contactID = null;

    /**
     * The provider that created us.
     */
    private ProtocolProviderServiceRssImpl parentProvider = null;

    /**
     * The group that the contact belongs to.
     */
    private ContactGroupRssImpl parentGroup = null;

    /**
     * The presence status of the contact.
     */
    private PresenceStatus presenceStatus = RssStatusEnum.OFFLINE;

    /**
     * Determines whether this contact is persistent, i.e. member of the contact
     * list or whether it is here only temporarily.
     */
    private boolean isPersistent = true;

    /**
     * Determines whether the contact has been resolved (i.e. we have a
     * confirmation that it is still on the server contact list).
     */
    private boolean isResolved = false;

    /**
     * The feed reader that we'll be using to retrieve the RSS flow associated
     * with this contact.
     */
    private RssFeedReader rssFeedReader = null;

    /**
     * Creates an instance of a meta contact with the specified string used
     * as a name and identifier.
     *
     * @param rssURL the URL of the rss feed that this contact will be wrapping.
     * @param parentProvider the provider that created us.
     */
    public ContactRssImpl(String contactID,
                          URL rssURL,
                          //RssFeedReader rssFeedReader,
                          String persistentData,
                          ProtocolProviderServiceRssImpl parentProvider)
        throws OperationFailedException, FileNotFoundException
    {
        this.contactID = contactID;
        //this.rssURL = rssURL;
        this.parentProvider = parentProvider;
        if(persistentData == null)
        {
            this.rssFeedReader = new RssFeedReader(rssURL);
        }
        else
        {
            this.rssFeedReader = RssFeedReader.deserialize(rssURL, persistentData);
            this.nickName = this.rssFeedReader.getTitle();
        }
        //this.rssFeedReader = rssFeedReader;
    }

    /**
     * This method is only called when the contact is added to a new
     * <tt>ContactGroupRssImpl</tt> by the
     * <tt>ContactGroupRssImpl</tt> itself.
     *
     * @param newParentGroup the <tt>ContactGroupRssImpl</tt> that is now
     * parent of this <tt>ContactRssImpl</tt>
     */
    void setParentGroup(ContactGroupRssImpl newParentGroup)
    {
        this.parentGroup = newParentGroup;
    }

    /**
     * Returns a String that can be used for identifying the contact.
     *
     * @return a String id representing that uniquely identifies the contact.
     */
    public String getAddress()
    {
        return contactID;
    }

    /**
     * Returns the URL that this contact is representing.
     *
     * @return the URL of the RSS flow that this contact represents.
     */
    /*public URL getRssURL()
    {
        return rssURL;
    }*/

    /**
     * Returns a String that could be used by any user interacting modules
     * for referring to this contact.
     *
     * @return a String that can be used for referring to this contact when
     *   interacting with the user.
     */
    public String getDisplayName()
    {
        if(nickName == null)
        {
            return contactID;
        }
        else
        {
            return nickName;
        }
    }

    /***
     * Sets the contact's nickname.
     * @param nickName
     */
    public void setDisplayName(String nickName)
    {
        this.nickName = nickName;
    }

    /***
     * Returns a <tt>RssItemKey</tt> object that identifies the last retrieved
     * item in the feed.
     * @return key identifying the last item in the feed.
     */
    /*public RssItemKey getLastItemKey()
    {
        return this.lastItem;
    }*/

    /***
     * Sets the key identifying the last item in the feed. It's usually used in
     * conjunction with a new <tt>RssItemKey</tt> object. For instance:
     * <code>contact.setLastItemKey(new RssItemKey(new Date()));</code>
     *
     * @param key key identifying the last item in the feed or (at least)
     * allowing differencing for newer items.
     *
     * @see RssItemKey
     */
    /*public void setLastItemKey(RssItemKey key)
    {
        this.lastItem= key;
    }*/

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
    byte[] getImage(boolean retrieveIfNecessary)
    {
        if(icon == null && retrieveIfNecessary)
        {
            OperationSetPersistentPresenceRssImpl pres
                = (OperationSetPersistentPresenceRssImpl)this.parentProvider
                    .getOperationSet(OperationSetPersistentPresence.class);

            pres.getImageRetriever().addContact(this);
        }

        return icon;
    }

    /**
     *  Set the image of the contact.
     *
     * @param imgBytes the bytes of the image that we'd like to set.
     */
    void setImage(byte[] imgBytes)
    {
        this.icon = imgBytes;
    }

    /**
     * Returns the status of the contact.
     *
     * @return Current presence status of the contact.
     */
    public PresenceStatus getPresenceStatus()
    {
        return this.presenceStatus;
    }

    /**
     * Sets <tt>rssPresenceStatus</tt> as the PresenceStatus that this
     * contact is currently in.
     * @param rssPresenceStatus the <tt>RssPresenceStatus</tt>
     * currently valid for this contact.
     */
    public void setPresenceStatus(PresenceStatus rssPresenceStatus)
    {
        this.presenceStatus = rssPresenceStatus;
    }

    /**
     * Returns a reference to the protocol provider that created the contact.
     *
     * @return a reference to an instance of the ProtocolProviderService
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
     * @return a reference to the <tt>ContactGroupRssImpl</tt> that
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
    public String toString()
    {
        StringBuffer buff
            = new StringBuffer("ContactRssImpl[ DisplayName=")
                .append(getDisplayName()).append("]");

        return buff.toString();
    }

    /**
     * Determines whether or not this contact is being stored by the server.
     * Non persistent contacts are common in the case of simple, non-persistent
     * presence operation sets. They could however also be seen in persistent
     * presence operation sets when for example we have received an event
     * from someone not on our contact list. Non persistent contacts are
     * volatile even when coming from a persistent presence operation set. They
     * would only exist until the application is closed and will not be there
     * next time it is loaded.
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
     * volatile even when coming from a persistent presence operation set. They
     * would only exist until the application is closed and will not be there
     * next time it is loaded.
     *
     * @param isPersistent true if the contact is persistent and false
     * otherwise.
     */
    public void setPersistent(boolean isPersistent)
    {
        this.isPersistent = isPersistent;
    }

    /***
     * Produces a textual representation of contact data that can be used to
     * restore the contact even before the network connection has been
     * initialized. This data contains the key identifying the last displayed
     * item, so that upon restart, items that have already been displayed in
     * older sessions don't get displayed again.
     */
    public String getPersistentData()
    {
        /*RssItemKey lastItem = rssFeedReader.getLastItemKey();
        if (lastItem != null)
            return lastItem.serialize();
        else
            return null;*/
        return  rssFeedReader.serialize();
    }

    /***
     * Restores feed item identification data from their textual representation.
     * @param persistentData textual representation of item key.
     *
     * #setPersistentData()
     */
    /*public void setPersistentData(String persistentData)
    {
        lastItem = RssItemKey.deserialize(persistentData);
    }*/

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
     * @param resolved true to make the contact resolved; false to
     * make it unresolved
     */
    public void setResolved(boolean resolved)
    {
        this.isResolved = resolved;
    }

    /**
     * Indicates whether some other object is "equal to" this one which in terms
     * of contacts translates to having equal IDs. The resolved status of the
     * contacts is deliberately ignored so that contacts would be declared equal
     * even if one contact is resolved and the other is not.
     * <p>
     * @param obj the reference object with which to compare.
     * @return  <code>true</code> if this contact has the same ID as that of the
     * <code>obj</code> argument.
     */
    public boolean equals(Object obj)
    {
        if (obj == null
                || ! (obj instanceof ContactRssImpl))
            return false;

        ContactRssImpl rssContact = (ContactRssImpl) obj;

        return this.getAddress().equals(rssContact.getAddress());
    }

    /**
     * Overrides <tt>hashCode</tt> from <tt>Object</tt> to ensure that
     * equal objects have same hashcode
     *
     * http://java.sun.com/j2se/1.4.2/docs/api/java/lang/Object.html#equals(java.lang.Object)
     */
    public int hashCode() {
        return getAddress().hashCode();
    }

    /**
     * Returns the persistent presence operation set that this contact belongs
     * to.
     *
     * @return the <tt>OperationSetPersistentPresenceRssImpl</tt> that
     * this contact belongs to.
     */
    public OperationSetPersistentPresenceRssImpl
                                            getParentPresenceOperationSet()
    {
        return (OperationSetPersistentPresenceRssImpl)parentProvider
            .getOperationSet(OperationSetPersistentPresence.class);
    }

    /**
     * Returns the RSS feed reader that we are using to retrieve flows
     * associated with this contact.
     *
     * @return a reference to the <tt>RssFeedReader</tt> that we are using to
     * retrieve the flow associated with this contact.
     */
    public RssFeedReader getRssFeedReader()
    {
        return rssFeedReader;
    }

    /**
     * Return the current status message of this contact.
     *
     * @return null as the protocol has no support of status messages
     */
    public String getStatusMessage()
    {
        return null;
    }
}
