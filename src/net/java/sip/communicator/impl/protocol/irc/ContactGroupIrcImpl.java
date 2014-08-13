/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
    private final List<ContactGroupIrcImpl> subgroups =
        new ArrayList<ContactGroupIrcImpl>();

    /**
     * Contacts in this group.
     */
    private final List<ContactIrcImpl> contacts =
        new ArrayList<ContactIrcImpl>();

    /**
     * Parent contact group.
     */
    private ContactGroup parent;

    /**
     * Contact Group IRC implementation.
     *
     * @param provider IRC protocol provider service instance.
     */
    public ContactGroupIrcImpl(final ProtocolProviderServiceIrcImpl provider)
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
    public Contact getContact(final String id)
    {
        if (id == null)
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
        return false;
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
        return false;
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
}
