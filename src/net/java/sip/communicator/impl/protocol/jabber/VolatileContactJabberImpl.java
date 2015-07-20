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
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.*;

import org.jivesoftware.smack.util.*;

/**
 * The Jabber implementation for Volatile Contact
 * @author Damian Minkov
 */
public class VolatileContactJabberImpl
    extends ContactJabberImpl
{
    /**
     * This contact id
     */
    private String contactId = null;

    /**
     * Indicates whether the contact is private messaging contact or not.
     */
    private boolean isPrivateMessagingContact = false;

    /**
     * The display name of the contact. This property is used only for private
     * messaging contacts.
     */
    protected String displayName = null;

    /**
     * Creates an Volatile JabberContactImpl with the specified id
     * @param id String the user id/address
     * @param ssclCallback a reference to the ServerStoredContactListImpl
     * instance that created us.
     */
    VolatileContactJabberImpl(String id,
                              ServerStoredContactListJabberImpl ssclCallback)
    {
        this(id, ssclCallback, false, null);
    }

    /**
     * Creates an Volatile JabberContactImpl with the specified id
     * @param id String the user id/address
     * @param ssclCallback a reference to the ServerStoredContactListImpl
     * instance that created us.
     * @param isPrivateMessagingContact if <tt>true</tt> this should be private
     * messaging contact.
     */
    VolatileContactJabberImpl(String id,
          ServerStoredContactListJabberImpl ssclCallback,
          boolean isPrivateMessagingContact)
    {
        this(id, ssclCallback, isPrivateMessagingContact, null);
    }

    /**
     * Creates an Volatile JabberContactImpl with the specified id
     * @param id String the user id/address
     * @param ssclCallback a reference to the ServerStoredContactListImpl
     * instance that created us.
     * @param isPrivateMessagingContact if <tt>true</tt> this should be private
     * messaging contact.
     * @param displayName the display name of the contact
     */
    VolatileContactJabberImpl(String id,
          ServerStoredContactListJabberImpl ssclCallback,
          boolean isPrivateMessagingContact, String displayName)
    {
        super(null, ssclCallback, false, false);

        this.isPrivateMessagingContact = isPrivateMessagingContact;

        if(this.isPrivateMessagingContact)
        {
            this.displayName = StringUtils.parseResource(id) + " from " +
                StringUtils.parseBareAddress(id);
            this.contactId = id;
            setJid(id);
        }
        else
        {
            this.contactId = StringUtils.parseBareAddress(id);
            this.displayName = (displayName == null)? contactId : displayName;
            String resource = StringUtils.parseResource(id);
            if(resource != null)
            {
                setJid(id);
            }
        }
    }

    /**
     * Returns the Jabber Userid of this contact
     * @return the Jabber Userid of this contact
     */
    @Override
    public String getAddress()
    {
        return contactId;
    }

    /**
     * Returns a String that could be used by any user interacting modules for
     * referring to this contact. An alias is not necessarily unique but is
     * often more human readable than an address (or id).
     * @return a String that can be used for referring to this contact when
     * interacting with the user.
     */
    @Override
    public String getDisplayName()
    {
        return displayName;
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
        StringBuffer buff =  new StringBuffer("VolatileJabberContact[ id=");
        buff.append(getAddress()).append("]");

        return buff.toString();
    }

    /**
     * Determines whether or not this contact group is being stored by the
     * server. Non persistent contact groups exist for the sole purpose of
     * containing non persistent contacts.
     * @return true if the contact group is persistent and false otherwise.
     */
    @Override
    public boolean isPersistent()
    {
        return false;
    }

    /**
     * Checks if the contact is private messaging contact or not.
     *
     * @return <tt>true</tt> if this is private messaging contact and
     * <tt>false</tt> if it isn't.
     */
    public boolean isPrivateMessagingContact()
    {
        return isPrivateMessagingContact;
    }

    /**
     * Returns the real address of the contact. If the contact is not private
     * messaging contact the result will be the same as <tt>getAddress</tt>'s
     * result.
     *
     * @return the real address of the contact.
     */
    @Override
    public String getPersistableAddress()
    {
        if(!isPrivateMessagingContact)
            return getAddress();


        ChatRoomMemberJabberImpl chatRoomMember = null;
        OperationSetMultiUserChatJabberImpl mucOpSet =
            (OperationSetMultiUserChatJabberImpl)getProtocolProvider()
                .getOperationSet(OperationSetMultiUserChat.class);
        if(mucOpSet != null)
        {
            chatRoomMember = mucOpSet
                .getChatRoom(StringUtils.parseBareAddress(contactId))
                .findMemberForNickName(
                    StringUtils.parseResource(contactId));
        }
        return ((chatRoomMember == null)? null : StringUtils.parseBareAddress(
            chatRoomMember.getJabberID()));
    }

}
