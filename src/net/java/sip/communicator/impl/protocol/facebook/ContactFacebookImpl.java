/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

import java.awt.image.*;
import java.io.*;
import java.net.*;

import javax.imageio.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * A simple, straightforward implementation of a facebook Contact. the contact
 * implementation encapsulate contact objects from the protocol stack and group
 * property values are returned after consulting the encapsulated object.
 * 
 * @author Dai Zhiwei
 */
public class ContactFacebookImpl
    implements Contact
{
    private static final Logger logger
        = Logger.getLogger(ContactFacebookImpl.class);

    /**
     * The facebook contact.
     */
    private FacebookUser contactMetaInfo = null;

    /**
     * The UNIQUE identifier of this contact, it should be the facebook user
     * number.
     */
    private final String contactID;

    /**
     * The facebook profile image
     */
    private byte[] image = null;

    /**
     * The provider that created us.
     */
    private final ProtocolProviderServiceFacebookImpl parentProvider;

    /**
     * The group that belong to.
     */
    private ContactGroupFacebookImpl parentGroup = null;

    /**
     * The presence status of the contact.
     */
    private PresenceStatus presenceStatus = FacebookStatusEnum.ONLINE;

    /**
     * Determines whether this contact is persistent, i.e. member of the contact
     * list or whether it is here only temporarily.
     */
    private boolean isPersistent = true;

    /**
     * Determines whether the contact has been resolved (i.e. we have a
     * confirmation that it is still on the server contact list).
     */
    private boolean isResolved = true;

    /**
     * Creates an instance of a meta contact with the specified string used as a
     * name and identifier.
     * 
     * @param contactID the identifier of this contact (also used as a name).
     * @param parentProvider the provider that created us.
     */
    public ContactFacebookImpl(
        String contactID,
        ProtocolProviderServiceFacebookImpl parentProvider)
    {
        this.contactID = contactID;
        this.parentProvider = parentProvider;

        updateContactInfo();

        logger.trace("init contactID: " + contactID);
    }

    /**
     * This method is only called when the contact is added to a new
     * <tt>ContactGroupFacebookImpl</tt> by the
     * <tt>ContactGroupFacebookImpl</tt> itself.
     * 
     * @param newParentGroup the <tt>ContactGroupFacebookImpl</tt> that is now
     *            parent of this <tt>ContactFacebookImpl</tt>
     */
    void setParentGroup(ContactGroupFacebookImpl newParentGroup)
    {
        this.parentGroup = newParentGroup;
    }

    /**
     * Returns a String that can be used for uniquely identifying the contact.
     * It should be the unique number of the facebook user.
     * 
     * @return a String id representing and uniquely identifying the contact.
     */
    public String getAddress()
    {
        return contactID;
    }

    /**
     * Returns a String that could be used by any user interacting modules for
     * referring to this contact.
     * 
     * @return a String that can be used for referring to this contact when
     *         interacting with the user.
     * FIXME should be facebook "name"
     */
    public String getDisplayName()
    {
        updateContactInfo();
        if (contactMetaInfo != null)
            return contactMetaInfo.name;
        else
            return contactID;
    }

    /**
     * Update the user information we just got from the server. so this data
     * shouldn't be modified manually.
     * 
     * @param newInfo
     */
    public void setContactInfo(FacebookUser newInfo)
    {
        this.contactMetaInfo = newInfo;
    }

    /**
     * Update contact info from the cache.
     */
    private void updateContactInfo()
    {
        FacebookUser newInfo = parentProvider.getContactMetaInfoByID(contactID);

        if(newInfo != null)
            this.contactMetaInfo = newInfo;
    }

    /**
     * update the user information we just got from the server. so this data
     * shouldn't be modified manually.
     */
    public FacebookUser getContactInfo()
    {
        return this.contactMetaInfo;
    }

    /**
     * Returns a byte array containing an image (most often a photo or an
     * avatar) that the contact uses as a representation.
     * 
     * @return byte[] an image representing the contact.
     */
    public byte[] getImage()
    {
        logger.trace("getImage()");

        // TODO handle java.lang.NullPointerException
        /*
         * if(image == null) ssclCallback.addContactForImageUpdate(this);
         */
        // if we've already gotten the avatar of our buddy,
        // just return it
        if (image != null)
            return image;

        updateContactInfo();
        // if we have not gotten the information of this buddy,
        // return the default avatar
        if (contactMetaInfo == null)
            return null;

        // if we get here, the contact is not null but the image is null.
        // That means we've gotten the information of this buddy,
        // but we still havn't fetch the avatar from the server,
        // so we do it now
        logger.trace("fetch the avatar from the server");
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        try
        {
            BufferedImage newAvatar
                = ImageIO.read(new URL(contactMetaInfo.thumbSrc));

            javax.imageio.ImageIO.write(newAvatar, "PNG", byteArrayOS);
            image = byteArrayOS.toByteArray();
        }
        catch (IOException e)
        {
            logger.warn("IOException occured when loading avatar", e);
        }
        finally
        {
            try
            {
                byteArrayOS.close();
            }
            catch (IOException ioe)
            {
                logger
                    .warn("Failed to close avatar ByteArrayOutputStream", ioe);
            }
        }

        return image;
    }

    /**
     * get big version avatar.<br>
     * small avatar at  http://profile.ak.facebook.com/profile6/1845/74/q800753867_2878.jpg<br>
     * bigger avatar at  http://profile.ak.facebook.com/profile6/1845/74/s800753867_2878.jpg<br>
     * biggest avatar at http://profile.ak.facebook.com/profile6/1845/74/n800753867_2878.jpg<br>
     * default avatar at http://static.ak.fbcdn.net/pics/q_silhouette.gif<br>
     * default bigger avatar at http://static.ak.fbcdn.net/pics/d_silhouette.gif
     * @return byte[] an big image representing the contact.
     */
    public byte[] getBigImage(){
        logger.trace("getBigImage()");
        updateContactInfo();
        // if we have not gotten the information of this buddy,
        // return the default avatar
        if (contactMetaInfo == null || contactMetaInfo.thumbSrc == null)
            return null;
        
        String avatarSrcStr;
        
        if(!contactMetaInfo.thumbSrc.equalsIgnoreCase(FacebookUser.defaultThumbSrc))
        {
            StringBuffer avatarSrcStrBuff = new StringBuffer(contactMetaInfo.thumbSrc);
            
            int tempPos = avatarSrcStrBuff.lastIndexOf("/q");
            if(tempPos > 0)
                avatarSrcStrBuff.replace(tempPos, tempPos + 2, "/n");
            
            avatarSrcStr = avatarSrcStrBuff.toString();
        }
        else
            avatarSrcStr = FacebookUser.defaultAvatarSrc;
        
        logger.trace("fetch the avatar from the server");
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        try
        {
            BufferedImage newAvatar = ImageIO.read(new URL(avatarSrcStr));

            javax.imageio.ImageIO.write(newAvatar, "PNG", byteArrayOS);
        }
        catch (IOException e)
        {
            logger.warn("IOException occured when loading avatar", e);
            // OK, we use the defaultAvatar temporarily
            return getImage();
        }
        finally
        {
            try
            {
                byteArrayOS.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        byte[] bigImage = byteArrayOS.toByteArray();
        // failed to get the avatar
        if (bigImage == null)
            return getImage();
        
        return bigImage;
    }

    /**
     * Used to set the image of the contact if it is updated
     * 
     * @param newImage a photo/avatar associated with this contact
     */
    public void setImage(byte[] newImage)
    {
        this.image = newImage;
    }

    /**
     * Returns the status of the contact.
     * 
     * @return presence status,(online or offline)
     */
    public PresenceStatus getPresenceStatus()
    {
        return this.presenceStatus;
    }

    /**
     * Sets <tt>facebookPresenceStatus</tt> as the PresenceStatus that this
     * contact is currently in.
     * 
     * @param facebookPresenceStatus the <tt>FacebookPresenceStatus</tt>
     *            currently valid for this contact.
     */
    public void setPresenceStatus(PresenceStatus facebookPresenceStatus)
    {
        this.presenceStatus = facebookPresenceStatus;
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
     *         false otherwise.
     */
    public boolean isLocal()
    {
        return false;
    }

    /**
     * Returns the group that contains this contact.
     * 
     * @return a reference to the <tt>ContactGroupFacebookImpl</tt> that
     *         contains this contact.
     */
    public ContactGroup getParentContactGroup()
    {
        return this.parentGroup;
    }

    /**
     * Returns a string representation of this contact, containing most of its
     * representative details.
     * 
     * @return a string representation of this contact.
     */
    public String toString()
    {
        StringBuffer buff
            = new StringBuffer("ContactFacebookImpl[ DisplayName=")
                    .append(getDisplayName())
                        .append("]");

        return buff.toString();
    }

    /**
     * Determines whether or not this contact is being stored by the server. Non
     * persistent contacts are common in the case of simple, non-persistent
     * presence operation sets. They could however also be seen in persistent
     * presence operation sets when for example we have received an event from
     * someone not on our contact list. Non persistent contacts are volatile
     * even when coming from a persistent presence op. set. They would only
     * exist until the application is closed and will not be there next time it
     * is loaded.
     * 
     * @return true if the contact is persistent and false otherwise.
     */
    public boolean isPersistent()
    {
        return isPersistent;
    }

    /**
     * Specifies whether or not this contact is being stored by the server. Non
     * persistent contacts are common in the case of simple, non-persistent
     * presence operation sets. They could however also be seen in persistent
     * presence operation sets when for example we have received an event from
     * someone not on our contact list. Non persistent contacts are volatile
     * even when coming from a persistent presence op. set. They would only
     * exist until the application is closed and will not be there next time it
     * is loaded.
     * 
     * @param isPersistent true if the contact is persistent and false
     *            otherwise.
     */
    public void setPersistent(boolean isPersistent)
    {
        this.isPersistent = isPersistent;
    }

    /**
     * Returns null as no persistent data is required and the contact address is
     * sufficient for restoring the contact.
     * <p>
     * 
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
     *         and false otherwise.
     */
    public boolean isResolved()
    {
        return isResolved;
    }

    /**
     * Return the current status message of this contact.
     * 
     * @return null as the protocol has currently no support of status messages
     */
    public String getStatusMessage()
    {
        updateContactInfo();
        if ((contactMetaInfo != null)
                && (contactMetaInfo.status != null)
                && !contactMetaInfo.status.trim().equals(""))
            return
                contactMetaInfo.status
                    + "[" + contactMetaInfo.statusTimeRel + "]";
        else
            return null;
    }

    /**
     * Makes the contact resolved or unresolved.
     * 
     * @param resolved true to make the contact resolved; false to make it
     *            unresolved
     */
    public void setResolved(boolean resolved)
    {
        this.isResolved = resolved;
    }

    /**
     * Indicates whether some other object is "equal to" this one which in terms
     * of contacts translates to having equal ids. The resolved status of the
     * contacts deliberately ignored so that contacts would be declared equal
     * even if it differs.
     * <p>
     * 
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this contact has the same id as that of
     *         the <code>obj</code> argument.
     */
    public boolean equals(Object obj)
    {
        if (obj == null || !(obj instanceof ContactFacebookImpl))
            return false;

        ContactFacebookImpl facebookContact = (ContactFacebookImpl) obj;

        return this.getAddress().equals(facebookContact.getAddress());
    }
}
