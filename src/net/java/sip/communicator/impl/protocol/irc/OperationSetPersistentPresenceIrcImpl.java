/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Implementation of support for Persistent Presence for IRC.
 * 
 * @author Danny van Heumen
 */
public class OperationSetPersistentPresenceIrcImpl extends AbstractOperationSetPersistentPresence<ProtocolProviderServiceIrcImpl>
{
    /**
     * Logger.
     */
    private final Logger LOGGER = Logger
        .getLogger(OperationSetPersistentPresenceIrcImpl.class);

    /**
     * Root contact group for IRC contacts.
     */
    private final ContactGroupIrcImpl rootGroup = new ContactGroupIrcImpl(this.parentProvider);

    /**
     * IRC implementation for OperationSetPersistentPresence.
     * 
     * @param parentProvider IRC instance of protocol provider service.
     */
    protected OperationSetPersistentPresenceIrcImpl(
        ProtocolProviderServiceIrcImpl parentProvider)
    {
        super(parentProvider);
    }
    
    ContactIrcImpl createVolatileContact(String id)
    {
        // Check whether a volatile group already exists and if not create
        // one
        ContactGroupIrcImpl volatileGroup = getNonPersistentGroup();

        if (volatileGroup == null)
        {
            volatileGroup =
                new ContactGroupIrcImpl(this.parentProvider, this.rootGroup,
                    IrcActivator.getResources().getI18NString(
                        "service.gui.NOT_IN_CONTACT_LIST_GROUP_NAME"));

            this.rootGroup.addSubGroup(volatileGroup);
            
            this.fireServerStoredGroupEvent(volatileGroup,
                ServerStoredGroupEvent.GROUP_CREATED_EVENT);
        }

        // Create volatile contact
        ContactIrcImpl newVolatileContact =
            new ContactIrcImpl(this.parentProvider, id, volatileGroup);
        volatileGroup.addContact(newVolatileContact);

        this.fireSubscriptionEvent(newVolatileContact, volatileGroup,
            SubscriptionEvent.SUBSCRIPTION_CREATED);

        return newVolatileContact;
    }

    ContactGroupIrcImpl getNonPersistentGroup()
    {
        String groupName
            = IrcActivator.getResources().getI18NString(
                "service.gui.NOT_IN_CONTACT_LIST_GROUP_NAME");

        for (int i = 0; i < getRootGroup().countSubgroups(); i++)
        {
            ContactGroupIrcImpl gr =
                (ContactGroupIrcImpl)getRootGroup().getGroup(i);

            if(!gr.isPersistent() && gr.getGroupName().equals(groupName))
                return gr;
        }

        return null;
    }

    public ContactGroup getRootGroup()
    {
        return rootGroup;
    }

    @Override
    public void subscribe(String contactIdentifier)
        throws IllegalArgumentException,
        IllegalStateException,
        OperationFailedException
    {
        System.out.println("subscribe(\"" + contactIdentifier + "\") called");
        // TODO Auto-generated method stub
    }

    @Override
    public void subscribe(ContactGroup parent, String contactIdentifier)
        throws IllegalArgumentException,
        IllegalStateException,
        OperationFailedException
    {
        System.out.println("subscribe(\"" + parent.getGroupName() + "\", \""
            + contactIdentifier + "\") called");
        // TODO Auto-generated method stub
    }

    @Override
    public void unsubscribe(Contact contact)
        throws IllegalArgumentException,
        IllegalStateException,
        OperationFailedException
    {
        System.out.println("unsubscribe(\"" + contact.getAddress()
            + "\") called");
        // TODO Auto-generated method stub
    }

    @Override
    public void createServerStoredContactGroup(ContactGroup parent,
        String groupName) throws OperationFailedException
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeServerStoredContactGroup(ContactGroup group)
        throws OperationFailedException
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void renameServerStoredContactGroup(ContactGroup group,
        String newName)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void moveContactToGroup(Contact contactToMove, ContactGroup newParent)
        throws OperationFailedException
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public ContactGroup getServerStoredContactListRoot()
    {
        return this.rootGroup;
    }

    @Override
    public Contact createUnresolvedContact(String address,
        String persistentData, ContactGroup parentGroup)
    {
        LOGGER.warn("Unresolved contact: " + address + " " + persistentData
            + " group: " + parentGroup.getGroupName());
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ContactGroup createUnresolvedContactGroup(String groupUID,
        String persistentData, ContactGroup parentGroup)
    {
        LOGGER.warn("Unresolved contactgroup: " + groupUID + " "
            + persistentData + " parent: " + parentGroup.getGroupName());
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PresenceStatus getPresenceStatus()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void publishPresenceStatus(PresenceStatus status,
        String statusMessage)
        throws IllegalArgumentException,
        IllegalStateException,
        OperationFailedException
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Iterator<PresenceStatus> getSupportedStatusSet()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PresenceStatus queryContactStatus(String contactIdentifier)
        throws IllegalArgumentException,
        IllegalStateException,
        OperationFailedException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Contact findContactByID(String contactID)
    {
        // FIXME DEBUG
        LOGGER.warn("findContactByID(\"" + contactID + "\") called");
        if (contactID == null)
            return null;
        Contact contact = this.rootGroup.getContact(contactID);
        if (contact != null)
            return contact;
        Iterator<ContactGroup> groups = this.rootGroup.subgroups();
        while (groups.hasNext())
        {
            ContactGroup group = groups.next();
            contact = group.getContact(contactID);
            if (contact != null)
                return contact;
        }
        // FIXME currently just creates a new volatile contact
        return createVolatileContact(contactID);
    }

    @Override
    public void setAuthorizationHandler(AuthorizationHandler handler)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getCurrentStatusMessage()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Contact createUnresolvedContact(String address, String persistentData)
    {
        // TODO Auto-generated method stub
        return null;
    }    
}
