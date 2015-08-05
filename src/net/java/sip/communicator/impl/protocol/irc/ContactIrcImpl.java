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
package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.service.protocol.*;

/**
 * IRC contact implementation.
 *
 * @author Danny van Heumen
 */
public class ContactIrcImpl
    extends AbstractContact
{
    /**
     * Parent provider.
     */
    private final ProtocolProviderServiceIrcImpl provider;

    /**
     * Contact id.
     */
    private String id;

    /**
     * Contact's parent group.
     */
    private ContactGroupIrcImpl parentGroup;

    /**
     * Contact's presence status.
     */
    private PresenceStatus presence;

    /**
     * Constructor.
     *
     * @param provider Protocol provider service instance.
     * @param id Contact id.
     * @param parentGroup The parent group of the contact.
     * @param presence the initial presence status of the new contact
     */
    public ContactIrcImpl(final ProtocolProviderServiceIrcImpl provider,
        final String id, final ContactGroupIrcImpl parentGroup,
        final IrcStatusEnum presence)
    {
        if (provider == null)
        {
            throw new IllegalArgumentException("provider cannot be null");
        }
        this.provider = provider;
        if (id == null)
        {
            throw new IllegalArgumentException("id cannot be null");
        }
        this.id = id;
        if (parentGroup == null)
        {
            throw new IllegalArgumentException("parentGroup cannot be null");
        }
        this.parentGroup = parentGroup;
        if (presence == null)
        {
            throw new IllegalArgumentException("presence cannot be null");
        }
        this.presence = presence;
    }

    /**
     * Get contact id (a.k.a. address)
     *
     * @return returns id
     */
    @Override
    public String getAddress()
    {
        return this.id;
    }

    /**
     * Set a new contact id (a.k.a. address)
     *
     * IRC allows nick change and the nick is also the identity of the user on
     * the IRC networks, so we allow nick changes.
     *
     * @param address the new address
     */
    public void setAddress(final String address)
    {
        if (address == null)
        {
            throw new IllegalArgumentException("address cannot be null");
        }
        this.id = address;
    }

    /**
     * Get contact display name.
     *
     * @return returns display name
     */
    @Override
    public String getDisplayName()
    {
        return this.id;
    }

    /**
     * Get contact image (avatar).
     *
     * @return returns image data
     */
    @Override
    public byte[] getImage()
    {
        return null;
    }

    /**
     * Get presence status.
     *
     * @return returns presence status
     */
    @Override
    public PresenceStatus getPresenceStatus()
    {
        return this.presence;
    }

    /**
     * Set a new presence status for contact.
     *
     * @param status new presence status (cannot be null)
     */
    protected void setPresenceStatus(final PresenceStatus status)
    {
        if (status == null)
        {
            throw new IllegalArgumentException("status cannot be null");
        }
        this.presence = status;
    }

    /**
     * Get parent contact group.
     *
     * @return returns parent contact group
     */
    @Override
    public ContactGroup getParentContactGroup()
    {
        return this.parentGroup;
    }

    /**
     * Set a new parent group.
     *
     * @param group the new parent group
     */
    public void setParentContactGroup(final ContactGroupIrcImpl group)
    {
        if (group == null)
        {
            throw new IllegalArgumentException("group cannot be null");
        }
        this.parentGroup = group;
    }

    /**
     * Get protocol provider service.
     *
     * @return returns IRC protocol provider service.
     */
    @Override
    public ProtocolProviderService getProtocolProvider()
    {
        return this.provider;
    }

    /**
     * Is persistent contact.
     * 
     * Determine persistence by the group in which it is stored. As long as a
     * contact is stored in the non-persistent contact group, we will consider
     * it non-persistent. As soon as a contact is moved to another group, we
     * assume it is valuable, so we consider it persistent.
     *
     * @return Returns true if contact is persistent, or false otherwise.
     */
    @Override
    public boolean isPersistent()
    {
        return this.parentGroup.isPersistent();
    }

    /**
     * Is contact resolved.
     *
     * @return Returns true if contact is resolved, or false otherwise.
     */
    @Override
    public boolean isResolved()
    {
        // TODO implement resolved status based on whether or not the nick name
        // is registered and the nick is currently "active" according to the
        // server, i.e. NickServ.
        // For now, we consider the contact unresolved ...
        return true;
    }

    /**
     * Get persistent data (if any).
     *
     * @return returns persistent data if available or null otherwise.
     */
    @Override
    public String getPersistentData()
    {
        return null;
    }

    /**
     * Get status message.
     *
     * @return returns status message
     */
    @Override
    public String getStatusMessage()
    {
        return null;
    }

}
