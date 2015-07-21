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
package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.icqconstants.*;
import net.kano.joustsim.oscar.oscar.service.ssi.*;

/**
 * The ICQ implementation of the service.protocol.Contact interface.
 *
 * @author Emil Ivov
 */
public class ContactIcqImpl
    extends AbstractContact
{
    Buddy joustSimBuddy = null;
    private boolean isLocal = false;
    private byte[] image = null;
    private PresenceStatus icqStatus = null;
    private ServerStoredContactListIcqImpl ssclCallback = null;
    private boolean isPersistent = false;
    private boolean isResolved = false;

    private String nickName = null;
    private String statusMessage = null;

    /**
     * Creates an IcqContactImpl
     * @param buddy the JoustSIM object that we will be encapsulating.
     * @param ssclCallback a reference to the ServerStoredContactListIcqImpl
     * instance that created us.
     * @param isPersistent determines whether this contact is persistent or not.
     * @param isResolved specifies whether the contact has been resolved against
     * the server contact list
     */
    ContactIcqImpl(Buddy buddy,
                   ServerStoredContactListIcqImpl ssclCallback,
                   boolean isPersistent,
                   boolean isResolved)
    {
        this.joustSimBuddy = buddy;
        this.ssclCallback = ssclCallback;
        this.isPersistent = isPersistent;
        this.isResolved = isResolved;
    }

    /**
     * Returns the ICQ uin (or AIM screen name)of this contact
     * @return the ICQ uin (or AIM screen name)of this contact
     */
    public String getUIN()
    {
        return joustSimBuddy.getScreenname().getFormatted();
    }

    /**
     * Returns the ICQ uin (or AIM screen name)of this contact
     * @return the ICQ uin (or AIM screen name)of this contact
     */
    public String getAddress(){
        return getUIN();
    }

    /**
     * Determines whether or not this Contact instance represents the user used
     * by this protocol provider to connect to the service.
     *
     * @return true if this Contact represents us (the local user) and false
     * otherwise.
     */
    public boolean isLocal()
    {
        return isLocal;
    }

    /**
     * Checks if an avatar or an image already exists for this contact and
     * returns it. This method DOES NOT perform any network operations.
     */
    public byte[] getImage()
    {
        return image;
    }

    /**
     * Returns the joust sim buddy that this Contact is encapsulating.
     * @return Buddy
     */
    Buddy getJoustSimBuddy()
    {
        return joustSimBuddy;
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
        StringBuffer buff =  new StringBuffer("IcqContact[ uin=");
        buff.append(getAddress()).append(", alias=")
            .append(getJoustSimBuddy().getAlias()).append("]");

        return buff.toString();
    }

    /**
     * Changes the buddy encapsulated by this method to be <tt>newBuddy</tt>.
     * This method is to be used _only_ when converting a non-persistent buddy
     * into a normal one.
     * @param newBuddy the new <tt>Buddy</tt> reference that this contact will
     * encapsulate.
     */
    void setJoustSimBuddy(Buddy newBuddy)
    {
        this.joustSimBuddy = newBuddy;
    }

    /**
     * Sets the status that this contact is currently in. The method is to
     * only be called as a result of a status update received from the AIM
     * server.
     *
     * @param status the IcqStatusEnum that this contact is currently in.
     */
    void updatePresenceStatus(PresenceStatus status)
    {
        this.icqStatus = status;
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
        if(icqStatus == null)
        {
            if(ssclCallback.getParentProvider().USING_ICQ)
                return IcqStatusEnum.OFFLINE;
            else
                return AimStatusEnum.OFFLINE;
        }
        else
            return icqStatus;
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
        String alias = joustSimBuddy.getAlias();

        if(alias != null)
            return alias;
        else if (nickName != null)
            return nickName;
        else
        {
            // if there is no alias we put this contact
            // for future update, as we may be not registered yet
            ssclCallback.addContactForUpdate(this);

            return getUIN();
        }
    }

    /**
     * Used to set the nickname of the contact if it is update
     * in the ContactList
     *
     * @param nickname String the value
     */
    protected void setNickname(String nickname)
    {
        this.nickName = nickname;
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
     * with a standard joustsim buddy.
     * @param persistent true if the buddy is to be considered persistent and
     * false for volatile.
     */
    void setPersistent(boolean persistent)
    {
        this.isPersistent = persistent;
    }

    /**
     * Specifies whether this contact has been resolved, or in other words that
     * its presence in the server stored contact list has been confirmed.
     * Unresolved contacts are created by services which need to quickly obtain
     * a reference to the contact corresponding to a specific address possibly
     * even before logging on the net.
     * @param resolved true if the buddy is resolved against the server stored
     * contact list and false otherwise.
     */
    void setResolved(boolean resolved)
    {
        this.isResolved = resolved;
    }

    /**
     * Returns the persistent data - for now only the nickname is needed
     * for restoring the contact data. Fields are properties separated by ;
     * <p>
     * @return the persistent data
     */
    public String getPersistentData()
    {
        // to store data only when nick is set
        if(nickName != null)
            return "nickname=" + nickName + ";";
        else
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
        if(persistentData == null)
        {
            return;
        }

        StringTokenizer dataToks = new StringTokenizer(persistentData, ";");
        while(dataToks.hasMoreTokens())
        {
            String data[] = dataToks.nextToken().split("=");
            if(data[0].equals("nickname") && data.length > 1)
            {
                nickName = data[1];
            }
        }
    }

    /**
     * Used to set the image of the contact if it is updated
     *
     * @param image a photo/avatar associated with this contact.
     */
    protected void setImage(byte[] image)
    {
        this.image = image;
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
