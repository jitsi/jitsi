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

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Contact group for IRC.
 *
 * @author Danny van Heumen
 */
public class ContactGroupIrcImpl
    implements ContactGroup
{
    /**
     * The protocol provider service instance.
     */
    private final ProtocolProviderServiceIrcImpl provider;

    /**
     * Group name.
     */
    private String name;

    /**
     * Subgroups.
     */
    private final ArrayList<ContactGroupIrcImpl> subgroups =
        new ArrayList<ContactGroupIrcImpl>();

    /**
     * Contacts in this group.
     */
    private final ArrayList<ContactIrcImpl> contacts =
        new ArrayList<ContactIrcImpl>();

    /**
     * Parent contact group.
     */
    private ContactGroup parent;

    /**
     * Flag for persistence.
     */
    private boolean persistent;

    /**
     * Contact Group IRC implementation.
     *
     * @param provider IRC protocol provider service instance.
     */
    ContactGroupIrcImpl(final ProtocolProviderServiceIrcImpl provider)
    {
        this(provider, null, "root");
    }

    /**
     * Contact Group IRC implementation.
     *
     * @param provider IRC protocol provider service instance.
     * @param parentGroup Parent group
     * @param name Group name
     */
    public ContactGroupIrcImpl(final ProtocolProviderServiceIrcImpl provider,
        final ContactGroupIrcImpl parentGroup, final String name)
    {
        if (provider == null)
        {
            throw new IllegalArgumentException("provider cannot be null");
        }
        this.provider = provider;
        this.parent = parentGroup;
        if (name == null)
        {
            throw new IllegalArgumentException("name cannot be null");
        }
        this.name = name;
        this.persistent = true;
    }

    /**
     * Get subgroups of this group.
     *
     * @return returns subgroups iterator
     */
    @Override
    public Iterator<ContactGroup> subgroups()
    {
        return new ArrayList<ContactGroup>(this.subgroups).iterator();
    }

    /**
     * Get number of subgroups.
     *
     * @return returns number of subgroups
     */
    @Override
    public int countSubgroups()
    {
        return this.subgroups.size();
    }

    /**
     * Get subgroup by index.
     *
     * @param index index of subgroup
     * @return returns subgroup
     */
    @Override
    public ContactGroup getGroup(final int index)
    {
        return this.subgroups.get(index);
    }

    /**
     * Get subgroup by name.
     *
     * @param groupName Name of subgroup.
     * @return returns subgroup or null if no group exists with that name
     */
    @Override
    public ContactGroup getGroup(final String groupName)
    {
        if (groupName == null)
        {
            return null;
        }
        for (ContactGroupIrcImpl group : this.subgroups)
        {
            if (groupName.equals(group.getGroupName()))
            {
                return group;
            }
        }
        return null;
    }

    /**
     * Get contacts in group.
     *
     * @return returns group's contacts
     */
    @Override
    public Iterator<Contact> contacts()
    {
        return new ArrayList<Contact>(this.contacts).iterator();
    }

    /**
     * Get number of contacts in group.
     *
     * @return returns number of contacts in group
     */
    @Override
    public int countContacts()
    {
        return this.contacts.size();
    }

    /**
     * Get group contact by id.
     *
     * @param id contact ID
     * @return returns contact or null if contact cannot be found
     */
    @Override
    public ContactIrcImpl getContact(final String id)
    {
        if (id == null || id.isEmpty())
        {
            return null;
        }
        for (ContactIrcImpl contact : this.contacts)
        {
            if (id.equals(contact.getAddress()))
            {
                return contact;
            }
        }
        return null;
    }

    /**
     * Find contact by searching through direct contacts and subsequently
     * continue searching in subgroups.
     *
     * @param id the contact id
     * @return returns found contact instance or <tt>null</tt> if contact is not
     *         found
     */
    public ContactIrcImpl findContact(final String id)
    {
        // search own contacts
        ContactIrcImpl contact = getContact(id);
        if (contact != null)
        {
            return contact;
        }
        // search in subgroups
        for (ContactGroupIrcImpl subgroup : this.subgroups)
        {
            contact = subgroup.findContact(id);
            if (contact != null)
            {
                return contact;
            }
        }
        return null;
    }

    /**
     * Check if group can contain subgroups.
     *
     * @return returns true if group can contain subgroups, or false otherwise.
     */
    @Override
    public boolean canContainSubgroups()
    {
        return true;
    }

    /**
     * Get name of the group.
     *
     * @return returns group name
     */
    @Override
    public String getGroupName()
    {
        return this.name;
    }

    /**
     * Set name of the group.
     *
     * @param name new name
     */
    public void setGroupName(final String name)
    {
        if (name == null)
        {
            throw new IllegalArgumentException("name cannot be null");
        }
        this.name = name;
    }

    /**
     * Get protocol provider service implementation.
     *
     * @return returns protocol provider service implementation
     */
    @Override
    public ProtocolProviderServiceIrcImpl getProtocolProvider()
    {
        return this.provider;
    }

    /**
     * Get parent contact group.
     *
     * @return returns parent contact group or null if no parent group exists
     */
    @Override
    public ContactGroup getParentContactGroup()
    {
        return this.parent;
    }

    /**
     * Is persistent group.
     *
     * @return returns true if group is persistent, or false if not.
     */
    @Override
    public boolean isPersistent()
    {
        return this.persistent;
    }

    /**
     * Set persistence.
     *
     * @param persistent <tt>true</tt> for persistent group, <tt>false</tt> for
     *            non-persistent group
     */
    public void setPersistent(final boolean persistent)
    {
        this.persistent = persistent;
    }

    /**
     * Get group UUID.
     *
     * @return returns group UUID
     */
    @Override
    public String getUID()
    {
        return this.name;
    }

    /**
     * Is group resolved.
     *
     * @return returns true if group is resolved, or false otherwise
     */
    @Override
    public boolean isResolved()
    {
        return true;
    }

    /**
     * Get group persistent data.
     *
     * @return returns persistent data
     */
    @Override
    public String getPersistentData()
    {
        return null;
    }

    /**
     * Add contact to the group.
     *
     * @param contact Contact to be added.
     */
    public void addContact(final ContactIrcImpl contact)
    {
        if (contact == null)
        {
            throw new IllegalArgumentException("contact cannot be null");
        }
        this.contacts.add(contact);
    }

    /**
     * Remove contact.
     *
     * @param contact the contact to remove
     */
    public void removeContact(final ContactIrcImpl contact)
    {
        if (contact == null)
        {
            throw new IllegalArgumentException("contact cannot be null");
        }
        this.contacts.remove(contact);
    }

    /**
     * Add group as subgroup to this group.
     *
     * @param group the group
     */
    public void addSubGroup(final ContactGroupIrcImpl group)
    {
        if (group == null)
        {
            throw new IllegalArgumentException("group cannot be null");
        }
        this.subgroups.add(group);
    }

    /**
     * Remove subgroup from this group.
     *
     * @param group the group
     */
    public void removeSubGroup(final ContactGroupIrcImpl group)
    {
        if (group == null)
        {
            throw new IllegalArgumentException("group cannot be null");
        }
        this.subgroups.remove(group);
    }
}
