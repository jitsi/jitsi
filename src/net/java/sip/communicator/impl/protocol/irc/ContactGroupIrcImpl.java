package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

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
    private final String name;
    
    /**
     * Subgroups
     */
    private final List<ContactGroupIrcImpl> subgroups = new ArrayList<ContactGroupIrcImpl>();
    
    /**
     * Contacts in this group.
     */
    private final List<ContactIrcImpl> contacts = new ArrayList<ContactIrcImpl>();

    /**
     * Parent contact group.
     */
    private ContactGroup parent;
    
    /**
     * Contact Group IRC implementation.
     * 
     * @param provider IRC protocol provider service instance.
     */
    public ContactGroupIrcImpl(ProtocolProviderServiceIrcImpl provider)
    {
        this(provider, null, "root");
    }
    
    /**
     * Contact Group IRC implementation.
     * 
     * @param provider IRC protocol provider service instance.
     * @param name Group name
     */
    public ContactGroupIrcImpl(ProtocolProviderServiceIrcImpl provider,
        ContactGroupIrcImpl parentGroup, String name)
    {
        if (provider == null)
            throw new IllegalArgumentException("provider cannot be null");
        this.provider = provider;
        this.parent = parentGroup;
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");
        this.name = name;
    }

    @Override
    public Iterator<ContactGroup> subgroups()
    {
        return new ArrayList<ContactGroup>(this.subgroups).iterator();
    }

    @Override
    public int countSubgroups()
    {
        return this.subgroups.size();
    }

    @Override
    public ContactGroup getGroup(int index)
    {
        return this.subgroups.get(index);
    }

    @Override
    public ContactGroup getGroup(String groupName)
    {
        if (groupName == null)
            return null;
        for (ContactGroupIrcImpl group : this.subgroups)
        {
            if (groupName.equals(group.getGroupName()))
            {
                return group;
            }
        }
        return null;
    }

    @Override
    public Iterator<Contact> contacts()
    {
        return new ArrayList<Contact>(this.contacts).iterator();
    }

    @Override
    public int countContacts()
    {
        return this.contacts.size();
    }

    @Override
    public Contact getContact(String id)
    {
        if (id == null)
            return null;
        for (ContactIrcImpl contact : this.contacts)
        {
            if (id.equals(contact.getAddress()))
            {
                return contact;
            }
        }
        return null;
    }

    @Override
    public boolean canContainSubgroups()
    {
        return true;
    }

    @Override
    public String getGroupName()
    {
        return this.name;
    }

    @Override
    public ProtocolProviderServiceIrcImpl getProtocolProvider()
    {
        return this.provider;
    }

    @Override
    public ContactGroup getParentContactGroup()
    {
        return this.parent;
    }

    @Override
    public boolean isPersistent()
    {
        return false;
    }

    @Override
    public String getUID()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isResolved()
    {
        return false;
    }

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
    public void addContact(ContactIrcImpl contact)
    {
        if (contact == null)
            throw new IllegalArgumentException("contact cannot be null");
        this.contacts.add(contact);
    }

    /**
     * Add group as subgroup to this group.
     * 
     * @param group the group
     */
    public void addSubGroup(ContactGroupIrcImpl group)
    {
        if (group == null)
            throw new IllegalArgumentException("group cannot be null");
        this.subgroups.add(group);
    }
}
