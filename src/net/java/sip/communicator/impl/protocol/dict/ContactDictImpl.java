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
package net.java.sip.communicator.impl.protocol.dict;

import net.java.dict4j.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * An implementation of a Dict contact
 *
 * @author ROTH Damien
 * @author LITZELMANN Cedric
 */
public class ContactDictImpl
    extends AbstractContact
{
    private Logger logger = Logger.getLogger(ContactDictImpl.class);

    /**
     * Icon
     */
    private static byte[] icon = DictActivator.getResources()
        .getImageInBytes("service.protocol.dict.DICT_64x64");

    /**
     * The id of the contact.
     */
    private String contactID = null;

    /**
     * The provider that created us.
     */
    private ProtocolProviderServiceDictImpl parentProvider = null;

    /**
     * The group that belong to.
     */
    private ContactGroupDictImpl parentGroup = null;

    /**
     * The presence status of the contact.
     */
    private PresenceStatus presenceStatus = DictStatusEnum.ONLINE;

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
     * The string in a "humain readable and understandable representation" of
     * the dictionnaire. In brief this is a short description of the dictionary.
     */
    private String dictName = null;

    /**
     * Creates an instance of a meta contact with the specified string used
     * as a name and identifier.
     *
     * @param databaseCode The identifier of this contact (also used as a name).
     * @param parentProvider The provider that created us.
     */
    public ContactDictImpl(
                String databaseCode,
                ProtocolProviderServiceDictImpl parentProvider)
    {
        this.contactID = databaseCode;
        this.parentProvider = parentProvider;
    }

    /**
     * This method is only called when the contact is added to a new
     * <tt>ContactGroupDictImpl</tt> by the
     * <tt>ContactGroupDictImpl</tt> itself.
     *
     * @param newParentGroup the <tt>ContactGroupDictImpl</tt> that is now
     * parent of this <tt>ContactDictImpl</tt>
     */
    void setParentGroup(ContactGroupDictImpl newParentGroup)
    {
        this.parentGroup = newParentGroup;
    }

    /**
     * Returns a String that can be used for identifying the contact.
     *
     * @return a String id representing and uniquely identifying the contact.
     */
    public String getContactID()
    {
        return contactID;
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
        if (dictName == null)
        {
            if (this.contactID.equals("*"))
            {
                this.dictName =  DictActivator.getResources()
                            .getI18NString("plugin.dictaccregwizz.ANY_DICTIONARY");
            }
            else if (this.contactID.equals("!"))
            {
                this.dictName = DictActivator.getResources()
                            .getI18NString("plugin.dictaccregwizz.FIRST_MATCH");
            }
            else
            {
                try
                {
                    this.dictName = this.parentProvider.getConnection()
                            .getDictionaryName(this.contactID);
                }
                catch (DictException dx)
                {
                    logger.error("Error while getting dictionary long name", dx);
                }

                if (this.dictName == null)
                    this.dictName = this.contactID;
            }
        }

        return dictName;
    }

    /**
     * Returns a byte array containing an image (most often a photo or an
     * avatar) that the contact uses as a representation.
     *
     * @return byte[] an image representing the contact.
     */
    public byte[] getImage()
    {
        return icon;
    }

    /**
     * Returns the status of the contact.
     *
     * @return always DictStatusEnum.ONLINE.
     */
    public PresenceStatus getPresenceStatus()
    {
        return this.presenceStatus;
    }

    /**
     * Sets <tt>dictPresenceStatus</tt> as the PresenceStatus that this
     * contact is currently in.
     * @param dictPresenceStatus the <tt>DictPresenceStatus</tt>
     * currently valid for this contact.
     */
    public void setPresenceStatus(PresenceStatus dictPresenceStatus)
    {
        this.presenceStatus = dictPresenceStatus;
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
     * @return a reference to the <tt>ContactGroupDictImpl</tt> that
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
            = new StringBuffer("ContactDictImpl[ DisplayName=")
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
     * Return the current status message of this contact.
     *
     * @return null as the protocol has currently no support of status messages
     */
    public String getStatusMessage()
    {
        return null;
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
     * @return the <tt>OperationSetPersistentPresenceGibberishImpl</tt> that
     * this contact belongs to.
     */
    public OperationSetPersistentPresenceDictImpl
                                            getParentPresenceOperationSet()
    {
        return (OperationSetPersistentPresenceDictImpl) parentProvider
            .getOperationSet(OperationSetPersistentPresence.class);
    }
}
