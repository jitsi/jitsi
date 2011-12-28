/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.sf.jml.*;
import net.sf.jml.impl.*;
import net.sf.jml.message.p2p.*;

/**
 * This class encapsulates the Roster class. Once created, it will
 * register itself as a listener to the encapsulated Roster and modify it's
 * local copy of Contacts and ContactGroups every time an event is generated
 * by the underlying framework. The class would also generate
 * corresponding sip-communicator events to all events coming from smack.
 *
 * @author Damian Minkov
 * @author Emil Ivov
 */
public class ServerStoredContactListMsnImpl
{
    private static final Logger logger =
        Logger.getLogger(ServerStoredContactListMsnImpl.class);

    /**
     * The root group. The container for all msn buddies and groups.
     */
    private final RootContactGroupMsnImpl rootGroup;

    /**
     * The operation set that created us and that we could use when dispatching
     * subscription events.
     */
    private OperationSetPersistentPresenceMsnImpl parentOperationSet = null;

    /**
     * The provider that is on top of us.
     */
    private final ProtocolProviderServiceMsnImpl msnProvider;

    private MsnMessenger messenger = null;

    /**
     * Listeners that would receive event notifications for changes in group
     * names or other properties, removal or creation of groups.
     */
    private Vector<ServerStoredGroupListener> serverStoredGroupListeners
        = new Vector<ServerStoredGroupListener>();

    /**
     * Manages and fires contact list modifications
     */
    private EventManager contactListModManager = null;

    private ContactListModListenerImpl contactListModListenerImpl
        = new ContactListModListenerImpl();

    /**
     * indicates whether or not the contactlist is initialized and ready.
     */
    private boolean isInitialized = false;

    private Vector<String> skipAddEvent = new Vector<String>();

    /**
     * Contact list listener.
     */
    ContactListListener contactListListener = null;

    /**
     * Creates a ServerStoredContactList wrapper for the specified BuddyList.
     *
     * @param parentOperationSet the operation set that created us and that
     * we could use for dispatching subscription events
     * @param provider the provider that has instantiated us.
     */
    ServerStoredContactListMsnImpl(
        OperationSetPersistentPresenceMsnImpl parentOperationSet,
        ProtocolProviderServiceMsnImpl        provider)
    {
        //We need to init these as early as possible to ensure that the provider
        //and the operationsset would not be null in the incoming events.
        this.parentOperationSet = parentOperationSet;

        this.msnProvider = provider;
        this.rootGroup = new RootContactGroupMsnImpl(this.msnProvider);

        // listens for provider registered events to set the isInitialized state
        // of the contact list
        provider.addRegistrationStateChangeListener(
            new RegistrationStateChangeListener()
            {
                public void registrationStateChanged(
                    RegistrationStateChangeEvent evt)
                {
                    if (evt.getNewState() == RegistrationState.UNREGISTERED
                        || evt.getNewState()
                                == RegistrationState.AUTHENTICATION_FAILED
                        || evt.getNewState()
                                == RegistrationState.CONNECTION_FAILED)
                    {
                        isInitialized = false;
                    }
                }
            }
        );
    }

    /**
     * Returns the root group of the contact list.
     *
     * @return the root ContactGroup for the ContactList
     */
    public ContactGroup getRootGroup()
    {
        return rootGroup;
    }

    /**
     * Registers the specified group listener so that it would receive events
     * on group modification/creation/destruction.
     * @param listener the ServerStoredGroupListener to register for group
     * events
     */
    void addGroupListener(ServerStoredGroupListener listener)
    {
        synchronized(serverStoredGroupListeners)
        {
            if(!serverStoredGroupListeners.contains(listener))
            this.serverStoredGroupListeners.add(listener);
        }
    }

    /**
     * Removes the specified group listener so that it won't receive further
     * events on group modification/creation/destruction.
     * @param listener the ServerStoredGroupListener to unregister
     */
    void removeGroupListener(ServerStoredGroupListener listener)
    {
        synchronized(serverStoredGroupListeners)
        {
            this.serverStoredGroupListeners.remove(listener);
        }
    }

    /**
     * Creates the corresponding event and notifies all
     * <tt>ServerStoredGroupListener</tt>s that the source group has been
     * removed, changed, renamed or whatever happened to it.
     * @param group the ContactGroup that has been created/modified/removed
     * @param eventID the id of the event to generate.
     */
    private void fireGroupEvent(ContactGroupMsnImpl group, int eventID)
    {
        //bail out if no one's listening
        if(parentOperationSet == null){
            if (logger.isDebugEnabled())
                logger.debug("No presence op. set available. Bailing out.");
            return;
        }

        ServerStoredGroupEvent evt = new ServerStoredGroupEvent(
                  group
                , eventID
                , parentOperationSet.getServerStoredContactListRoot()
                , msnProvider
                , parentOperationSet);

        if (logger.isTraceEnabled())
            logger.trace("Will dispatch the following grp event: " + evt);

        Iterator<ServerStoredGroupListener> listeners = null;
        synchronized (serverStoredGroupListeners)
        {
            listeners = new ArrayList<ServerStoredGroupListener>(
                            serverStoredGroupListeners).iterator();
        }

        while (listeners.hasNext())
        {
            ServerStoredGroupListener listener = listeners.next();

            if (eventID == ServerStoredGroupEvent.GROUP_REMOVED_EVENT)
                listener.groupRemoved(evt);
            else if (eventID == ServerStoredGroupEvent.GROUP_RENAMED_EVENT)
                listener.groupNameChanged(evt);
            else if (eventID == ServerStoredGroupEvent.GROUP_CREATED_EVENT)
                listener.groupCreated(evt);
            else if (eventID == ServerStoredGroupEvent.GROUP_RESOLVED_EVENT)
                listener.groupResolved(evt);
        }
    }

    /**
     * Make the parent persistent presence operation set dispatch a contact
     * removed event.
     * @param parentGroup the group where that the removed contact belonged to.
     * @param contact the contact that was removed.
     */
    private void fireContactRemoved( ContactGroup parentGroup,
                                     ContactMsnImpl contact)
    {
        //bail out if no one's listening
        if(parentOperationSet == null){
            if (logger.isDebugEnabled())
                logger.debug("No presence op. set available. Bailing out.");
            return;
        }

        //dispatch
        parentOperationSet.fireSubscriptionEvent(
            contact, parentGroup, SubscriptionEvent.SUBSCRIPTION_REMOVED);
    }

    /**
     * Make the parent persistent presence operation set dispatch a subscription
     * moved event.
     * @param oldParentGroup the group where the source contact was located
     * before being moved
     * @param newParentGroup the group that the source contact is currently in.
     * @param contact the contact that was added
     */
    private void fireContactMoved( ContactGroup oldParentGroup,
                                   ContactGroup newParentGroup,
                                   ContactMsnImpl contact)
    {
        //bail out if no one's listening
        if(parentOperationSet == null){
            if (logger.isDebugEnabled())
                logger.debug("No presence op. set available. Bailing out.");
            return;
        }

        //dispatch
        parentOperationSet.fireSubscriptionMovedEvent(
            contact, oldParentGroup, newParentGroup);
    }

    /**
     * Returns a reference to the provider that created us.
     * @return a reference to a ProtocolProviderServiceImpl instance.
     */
    ProtocolProviderServiceMsnImpl getParentProvider()
    {
        return msnProvider;
    }

    /**
     * Returns the ConntactGroup with the specified name or null if no such
     * group was found.
     * <p>
     * @param name the name of the group we're looking for.
     * @return a reference to the ContactGroupMsnImpl instance we're looking for
     * or null if no such group was found.
     */
    public ContactGroupMsnImpl findContactGroup(String name)
    {
        Iterator<ContactGroup> contactGroups = rootGroup.subgroups();

        while(contactGroups.hasNext())
        {
            ContactGroup contactGroup = contactGroups.next();

            if (contactGroup.getGroupName().equals(name))
                return (ContactGroupMsnImpl)contactGroup;
        }

        return null;
    }

    /**
     * Returns the Contact with the specified id or null if
     * no such id was found.
     *
     * @param id the id of the contact to find.
     * @return the <tt>Contact</tt> carrying the specified
     * <tt>screenName</tt> or <tt>null</tt> if no such contact exits.
     */
    public ContactMsnImpl findContactById(String id)
    {
        Iterator<ContactGroup> contactGroups = rootGroup.subgroups();
        ContactMsnImpl result = null;

        while(contactGroups.hasNext())
        {
            ContactGroupMsnImpl contactGroup
                = (ContactGroupMsnImpl)contactGroups.next();

            result = contactGroup.findContact(id);

            if (result != null)
                return result;

        }

        Iterator<Contact> rootContacts = rootGroup.contacts();
        while (rootContacts.hasNext())
        {
            Contact item = rootContacts.next();

            if(item.getAddress().equals(id))
                return (ContactMsnImpl)item;
        }

        return null;
    }

    /**
     * Returns the ContactGroup containing the specified contact or null
     * if no such group or contact exist.
     *
     * @param child the contact whose parent group we're looking for.
     * @return the <tt>ContactGroup</tt> containing the specified
     * <tt>contact</tt> or <tt>null</tt> if no such group or contact
     * exist.
     */
    public ContactGroup findContactGroup(ContactMsnImpl child)
    {
        Iterator<ContactGroup> contactGroups = rootGroup.subgroups();

        while(contactGroups.hasNext())
        {
            ContactGroupMsnImpl contactGroup
                = (ContactGroupMsnImpl)contactGroups.next();

            if( contactGroup.findContact(child.getAddress())!= null)
                return contactGroup;
        }

        Iterator<Contact> contacts = rootGroup.contacts();

        while(contacts.hasNext())
        {
            Contact contact = contacts.next();

            if( contact.equals(child))
                return rootGroup;
        }

        return null;
    }

    /**
     * Adds a new contact with the specified screenname to the list under a
     * default location.
     * @param id the id of the contact to add.
     * @throws OperationFailedException
     */
    public void addContact(String id)
        throws OperationFailedException
    {
        addContact(null, id);
    }

    /**
     * Adds a new contact with the specified screenname to the list under the
     * specified group.
     * @param id the id of the contact to add.
     * @param parent the group under which we want the new contact placed.
     * @throws OperationFailedException if the contact already exist
     */
    public void addContact(final ContactGroupMsnImpl parent, final String id)
        throws OperationFailedException
    {
        if (logger.isTraceEnabled())
            logger.trace("Adding contact " + id + " to parent=" + parent);

        //if the contact is already in the contact list and is not volatile,
        //then only broadcast an event
        ContactMsnImpl existingContact = findContactById(id);

        if( existingContact != null
            && existingContact.isPersistent() )
        {
            if (logger.isDebugEnabled())
                logger.debug("Contact " + id + " already exists.");
            throw new OperationFailedException(
                "Contact " + id + " already exists.",
                OperationFailedException.SUBSCRIPTION_ALREADY_EXISTS);
        }

        if(parent != null)
        {
            // add the buddy to the list as its not there
            msnProvider.getMessenger().unblockFriend(Email.parseStr(id));
            skipAddEvent.add(id);
            msnProvider.getMessenger().addFriend(Email.parseStr(id), id);
            msnProvider.getMessenger().copyFriend(
                        Email.parseStr(id),
                        parent.getSourceGroup().getGroupId());
        }
        else
        {
            // add the buddy to the list as its not there
//            msnProvider.getMessenger().unblockFriend(Email.parseStr(id));
            msnProvider.getMessenger().addFriend(Email.parseStr(id), id);
        }
    }

    /**
     * Creates a non persistent contact for the specified address. This would
     * also create (if necessary) a group for volatile contacts that would not
     * be added to the server stored contact list. This method would have no
     * effect on the server stored contact list.
     * @param contact the address of the contact to create.
     * @return the newly created volatile <tt>ContactImpl</tt>
     */
    ContactMsnImpl createVolatileContact(MsnContact contact)
    {
        return
            createVolatileContact(
                contact.getId(),
                contact.getEmail(),
                contact.getDisplayName());
    }

    /**
     * Creates a non persistent contact for the specified address. This would
     * also create (if necessary) a group for volatile contacts that would not
     * be added to the server stored contact list. This method would have no
     * effect on the server stored contact list.
     *
     * @param id the id of the contact to create
     * @param email the e-mail address of the contact to create
     * @param displayName the display name of the contact to create
     * @return the newly created volatile <tt>ContactImpl</tt>
     */
    ContactMsnImpl createVolatileContact(
            String id,
            Email email,
            String displayName)
    {
        //First create the new volatile contact;
        VolatileContact volatileBuddy
            = new VolatileContact(id, email, displayName);
        ContactMsnImpl newVolatileContact
            = new ContactMsnImpl(volatileBuddy, this, false, false);

        //Check whether a volatile group already exists and if not create one.
        ContactGroupMsnImpl theVolatileGroup = getNonPersistentGroup();

        //if the parent group is null then add necessary create the group
        if (theVolatileGroup == null)
        {
            MsnContact[] emptyBuddies = new MsnContact[]{};

            theVolatileGroup = new ContactGroupMsnImpl(
                new VolatileGroup(), emptyBuddies, this, false, false);
            theVolatileGroup.addContact(newVolatileContact);

            this.rootGroup.addSubGroup(theVolatileGroup);

            fireGroupEvent(theVolatileGroup,
                           ServerStoredGroupEvent.GROUP_CREATED_EVENT);
        }
        else
        {
            theVolatileGroup.addContact(newVolatileContact);
            fireContactAdded(theVolatileGroup, newVolatileContact);
        }

        return newVolatileContact;
    }


    /**
     * Creates a non resolved contact for the specified address and inside the
     * specified group. The newly created contact would be added to the local
     * contact list as a standard contact but when an event is received from the
     * server concerning this contact, then it will be reused and only its
     * isResolved field would be updated instead of creating the whole contact
     * again.
     *
     * @param parentGroup the group where the unresolved contact is to be
     * created
     * @param id the Address of the contact to create.
     * @return the newly created unresolved <tt>ContactImpl</tt>
     */
    ContactMsnImpl createUnresolvedContact(ContactGroup parentGroup, String id)
    {
        //First create the new volatile contact;
        VolatileContact volatileBuddy = new VolatileContact(id);

        ContactMsnImpl newUnresolvedContact
            = new ContactMsnImpl(volatileBuddy, this, false, false);

        if(parentGroup instanceof ContactGroupMsnImpl)
            ((ContactGroupMsnImpl)parentGroup).
                addContact(newUnresolvedContact);
        else if(parentGroup instanceof RootContactGroupMsnImpl)
            ((RootContactGroupMsnImpl)parentGroup).
                addContact(newUnresolvedContact);

        fireContactAdded(  parentGroup
                         , newUnresolvedContact);

        return newUnresolvedContact;
    }

    /**
     * Creates a non resolved contact group for the specified name. The newly
     * created group would be added to the local contact list as any other group
     * but when an event is received from the server concerning this group, then
     * it will be reused and only its isResolved field would be updated instead
     * of creating the whole group again.
     * <p>
     * @param groupName the name of the group to create.
     * @return the newly created unresolved <tt>ContactGroupImpl</tt>
     */
    ContactGroupMsnImpl createUnresolvedContactGroup(String groupName)
    {
        //First create the new volatile contact;
        MsnContact[] emptyBuddies = new MsnContact[]{};
        ContactGroupMsnImpl newUnresolvedGroup = new ContactGroupMsnImpl(
                new VolatileGroup(groupName), emptyBuddies, this, false, true);

        this.rootGroup.addSubGroup(newUnresolvedGroup);

        fireGroupEvent(newUnresolvedGroup
                        , ServerStoredGroupEvent.GROUP_CREATED_EVENT);

        return newUnresolvedGroup;
    }

    /**
     * Creates the specified group on the server stored contact list.
     * @param groupName a String containing the name of the new group.
     * @throws OperationFailedException with code CONTACT_GROUP_ALREADY_EXISTS
     * if the group we're trying to create is already in our contact list.
     */
    public void createGroup(String groupName)
        throws OperationFailedException
    {
        if (logger.isTraceEnabled())
            logger.trace("Creating group: " + groupName);

        ContactGroupMsnImpl existingGroup = findContactGroup(groupName);

        if( existingGroup != null && existingGroup.isPersistent() )
        {
            if (logger.isDebugEnabled())
                logger.debug("ContactGroup " + groupName + " already exists.");
            throw new OperationFailedException(
                           "ContactGroup " + groupName + " already exists.",
                OperationFailedException.CONTACT_GROUP_ALREADY_EXISTS);
        }

        msnProvider.getMessenger().addGroup(groupName);
    }

    /**
     * Removes the specified group from the buddy list.
     * @param groupToRemove the group that we'd like removed.
     */
    public void removeGroup(ContactGroupMsnImpl groupToRemove)
    {
        // there is a default group in the msn lib. we cannot remove it
        // as server doesn't know about it
        if(groupToRemove.getSourceGroup().getGroupId().equals("0"))
            return;

        if (logger.isTraceEnabled())
            logger.trace("removing group " + groupToRemove);
        MsnContact[] contacts = groupToRemove.getSourceGroup().getContacts();

        ModListenerRemoveGroup removedContactsListener =
            new ModListenerRemoveGroup(contacts.length);
        contactListModManager.addModificationListener(removedContactsListener);

        for (int i = 0; i < contacts.length; i++)
        {
            if (logger.isTraceEnabled())
                logger.trace("removing contact from group " + contacts[i]);

            msnProvider.getMessenger().removeFriend(contacts[i].getEmail(),
                groupToRemove.getSourceGroup().getGroupId());
        }

        // wait max seconds or the last removed contact
        removedContactsListener.waitForLastEvent(contacts.length*1000);
        contactListModManager.removeModificationListener(removedContactsListener);

        msnProvider.getMessenger().
            removeGroup(groupToRemove.getSourceGroup().getGroupId());
    }

    /**
     * Removes a contact from the serverside list
     * Event will come for successful operation
     * @param contactToRemove ContactMsnImpl
     */
    void removeContact(ContactMsnImpl contactToRemove)
    {
        if (logger.isTraceEnabled())
            logger.trace("Removing msn contact "
                        + contactToRemove.getSourceContact());

        Email contactsEmail = contactToRemove.getSourceContact().getEmail();

        MsnGroup[] belongGroups =
            contactToRemove.getSourceContact().getBelongGroups();
        if(belongGroups != null)
            for (int i = 0; i < belongGroups.length; i++)
            {
                msnProvider.getMessenger().
                    removeFriend(contactsEmail, belongGroups[i].getGroupId());
            }

        msnProvider.getMessenger().
            removeFriend(contactsEmail, false);
    }


    /**
     * Renames the specified group according to the specified new name..
     * @param groupToRename the group that we'd like removed.
     * @param newName the new name of the group
     */
    public void renameGroup(ContactGroupMsnImpl groupToRename, String newName)
    {
        //printList();
        msnProvider.getMessenger().
            renameGroup(groupToRename.getSourceGroup().getGroupId(), newName);
    }

    /**
     * Moves the specified <tt>contact</tt> to the group indicated by
     * <tt>newParent</tt>.
     * @param contact the contact that we'd like moved under the new group.
     * @param newParent the group where we'd like the parent placed.
     */
    public void moveContact(ContactMsnImpl contact,
                            ContactGroup newParent)
    {
        ContactGroup oldParent = contact.getParentContactGroup();

        if(oldParent instanceof RootContactGroupMsnImpl)
        {
            if (logger.isTraceEnabled())
                logger.trace("Will Move from root " + contact);
            msnProvider.getMessenger().
                copyFriend(
                    contact.getSourceContact().getEmail(),
                    ((ContactGroupMsnImpl)newParent).getSourceGroup()
                        .getGroupId());
        }
        else
        {
            if(newParent instanceof RootContactGroupMsnImpl &&
                !contact.isPersistent())
            {
                try
                {
                    addContact(contact.getAddress());
                }
                catch (OperationFailedException ex)
                {
                    logger.error("Failed to add contact from " +
                        MsnActivator.getResources().getI18NString(
                            "service.gui.NOT_IN_CONTACT_LIST_GROUP_NAME") +
                        " group to new group: " + newParent, ex);
                }

                return;
            }

            if( !contact.isPersistent() &&
                !contact.getParentContactGroup().isPersistent())
            {
                try
                {
                    addContact(
                        (ContactGroupMsnImpl)newParent, contact.getAddress());
                }
                catch (OperationFailedException ex)
                {
                    logger.error("Failed to add contact from " +
                        MsnActivator.getResources().getI18NString(
                            "service.gui.NOT_IN_CONTACT_LIST_GROUP_NAME") +
                        " group to new group: " + newParent, ex);
                }
            }
            else
            {
                if (logger.isTraceEnabled())
                    logger.trace("Will Move from "
                                +  contact.getParentContactGroup()
                    + " to : " + newParent + " - contact: " + contact);
//                contactListModListenerImpl.waitForMove(contact.getAddress());
                msnProvider.getMessenger().moveFriend(
                    contact.getSourceContact().getEmail(),
                    ( (ContactGroupMsnImpl) contact.getParentContactGroup()).
                        getSourceGroup().getGroupId(),
                    ((ContactGroupMsnImpl)newParent).getSourceGroup()
                        .getGroupId());
            }
        }
    }

    /**
     * Returns the volatile group
     *
     * @return ContactGroupMsnImpl
     */
    private ContactGroupMsnImpl getNonPersistentGroup()
    {
        for (int i = 0; i < getRootGroup().countSubgroups(); i++)
        {
            ContactGroupMsnImpl gr =
                (ContactGroupMsnImpl)getRootGroup().getGroup(i);

            if(!gr.isPersistent())
                return gr;
        }

        return null;
    }

    /**
     * Finds Group by provided its msn ID
     * @param id String
     * @return ContactGroupMsnImpl
     */
    private ContactGroupMsnImpl findContactGroupByMsnId(String id)
    {
        Iterator<ContactGroup> contactGroups = rootGroup.subgroups();

        while(contactGroups.hasNext())
        {
            ContactGroupMsnImpl contactGroup
                = (ContactGroupMsnImpl)contactGroups.next();

            if (contactGroup.getSourceGroup().getGroupId().equals(id))
                return contactGroup;
        }

        return null;
    }

    /**
     * Make the parent persistent presence operation set dispatch a contact
     * added event.
     * @param parentGroup the group where the new contact was added
     * @param contact the contact that was added
     */
    void fireContactAdded( ContactGroup parentGroup,
                                   ContactMsnImpl contact)
    {
        //bail out if no one's listening
        if(parentOperationSet == null){
            if (logger.isDebugEnabled())
                logger.debug("No presence op. set available. Bailing out.");
            return;
        }

        //dispatch
        parentOperationSet.fireSubscriptionEvent(
            contact, parentGroup, SubscriptionEvent.SUBSCRIPTION_CREATED);

        // sometimes when adding msn contact
        // status updates comes before event for adding contact and so
        // statuses are not dispatched, we check this here
        MsnUserStatus msnStatus =
            contact.getSourceContact().getStatus();

        // for some reason when creating unresolved contact this status is null
        if(msnStatus == null)
            return;

        PresenceStatus oldStatus
            = contact.getPresenceStatus();

        PresenceStatus newStatus
            = parentOperationSet.msnStatusToPresenceStatus(
                contact.getSourceContact().getStatus());

        if(oldStatus.equals(newStatus))
            return;

        contact.updatePresenceStatus(newStatus);

        parentOperationSet.fireContactPresenceStatusChangeEvent(
            contact,
            parentGroup,
            oldStatus,
            newStatus
        );
    }

    /**
     * Make the parent persistent presence operation set dispatch a contact
     * resolved event.
     * @param parentGroup the group that the resolved contact belongs to.
     * @param contact the contact that was resolved
     */
    void fireContactResolved( ContactGroup parentGroup,
                                      ContactMsnImpl contact)
    {
        //bail out if no one's listening
        if(parentOperationSet == null){
            if (logger.isDebugEnabled())
                logger.debug("No presence op. set available. Bailing out.");
            return;
        }

        //dispatch
        parentOperationSet.fireSubscriptionEvent(
            contact, parentGroup, SubscriptionEvent.SUBSCRIPTION_RESOLVED);
    }

    /**
     * Returns true if the contact list is initialized and
     * ready for use, and false otherwise.
     *
     * @return true if the contact list is initialized and ready for use and
     * false otherwise
     */
    boolean isInitialized()
    {
        return isInitialized;
    }

    /**
     * Changes the contact display name on the server.
     * @param contact the contact to change,
     * @param newName the new name.
     */
    void setDisplayName(ContactMsnImpl contact, String newName)
    {
        messenger.renameFriend(contact.getSourceContact().getEmail(), newName);
    }

    /**
     * Waits for init in the contact list and populates the contacts and fills
     * or resolves our contact list
     */
    private class ContactListListener
        implements net.sf.jml.event.MsnContactListListener
    {
        public void contactPersonalMessageChanged(MsnMessenger messenger ,
            MsnContact contact)
        {
            ContactMsnImpl contactImpl =
                findContactById(contact.getEmail().getEmailAddress());

            if (contactImpl == null)
                return;

            contactImpl.setStatusMessage(contact.getPersonalMessage());
        }

        public void contactListSyncCompleted(MsnMessenger messenger)
        {
        }

        public void contactListInitCompleted(MsnMessenger messenger)
        {
            if (logger.isTraceEnabled())
                logger.trace("contactListInitCompleted");
            isInitialized = true;

            if(logger.isDebugEnabled())
                printList();

            // first init groups
            MsnContactList contactList = messenger.getContactList();
            MsnGroup[] groups = contactList.getGroups();
            for (int i = 0; i < groups.length; i++)
            {
                MsnGroup item = groups[i];

                if(item.isDefaultGroup())
                    continue;

                ContactGroupMsnImpl group
                    = findContactGroup(item.getGroupName());

                if (group == null)
                {
                    // create the group as it doesn't exist
                    ContactGroupMsnImpl newGroup =
                        new ContactGroupMsnImpl(
                                item,
                                item.getContacts(),
                                ServerStoredContactListMsnImpl.this,
                                true,
                                true);

                    rootGroup.addSubGroup(newGroup);

                    //tell listeners about the added group
                    fireGroupEvent(newGroup,
                                   ServerStoredGroupEvent.GROUP_CREATED_EVENT);
                }
                else
                {
                    // the group exist so just resolved. The group will check and
                    // create or resolve its entries
                    group.setResolved(item);

                    //fire an event saying that the group has been resolved
                    fireGroupEvent(group,
                                   ServerStoredGroupEvent.GROUP_RESOLVED_EVENT);

                    /** @todo  if something to delete . delete it */
                }
            }

            // so all groups and its contacts are created
            // lets create the users that are not in a group
            MsnContact[] contacts =
                messenger.getContactList().getContactsInList(MsnList.FL);
            for (int i = 0; i < contacts.length; i++)
            {
                MsnContact item = contacts[i];

                if(item.getBelongGroups().length == 0)
                {
                    ContactMsnImpl contact =
                    findContactById(item.getEmail().getEmailAddress());

                    if (contact == null)
                    {
                        // if there is no such contact create it
                        contact = new ContactMsnImpl(
                                        item,
                                        ServerStoredContactListMsnImpl.this,
                                        true,
                                        true);
                        rootGroup.addContact(contact);

                        fireContactAdded(rootGroup, contact);
                    }
                    else
                    {
                        // if contact exist so resolve it
                        contact.setResolved(item);
                    }
                }
            }

            // if we have received status before we have inited the list
            // sho them correctly
            parentOperationSet.earlyStatusesDispatch();

            // retreive offline messages
            messenger.retreiveOfflineMessages();
        }

        public void contactStatusChanged(MsnMessenger messenger,
                                         MsnContact contact)
        {
        }

        public void ownerStatusChanged(MsnMessenger messenger)
        {
        }

        public void contactAddedMe(MsnMessenger messenger,
            MsnContactPending[] pendingContacts)
        {
            if (logger.isTraceEnabled())
                logger.trace("Contact add us ");
            /**
             * TODO this method introduced for msn15 currently used.
             * the old one below no more used.
             */
        }

        public void contactAddedMe(MsnMessenger messenger, MsnContact contact)
        {
//            try
//            {
                if (logger.isTraceEnabled())
                    logger.trace("Contact add us " + contact);

                if(parentOperationSet.getAuthorizationHandler() == null)
                    return;

                ContactMsnImpl contactImpl =
                    findContactById(contact.getEmail().getEmailAddress());

                if (contactImpl != null)
                    return;

                contactImpl = createVolatileContact(contact);

                AuthorizationRequest authRequest = new AuthorizationRequest();

                AuthorizationResponse authResponse =
                    parentOperationSet.getAuthorizationHandler()
                        .processAuthorisationRequest( authRequest, contactImpl);

                if (authResponse.getResponseCode()
                                == AuthorizationResponse.IGNORE)
                {
                    return;
                }
                else if (authResponse.getResponseCode()
                            == AuthorizationResponse.REJECT)
                {
                    msnProvider.getMessenger().blockFriend(contact.getEmail());
                }
                else  if (authResponse.getResponseCode()
                            == AuthorizationResponse.ACCEPT)
                {
                    moveContact(contactImpl, rootGroup);
//                  addContact(contact.getEmail().getEmailAddress());
//                  msnProvider.getMessenger()
//                      .unblockFriend(contact.getEmail());
                }
//            }
//            catch (OperationFailedException ex)
//            {
//                logger.error("cannot add ", ex);
//            }
        }

        public void contactRemovedMe(MsnMessenger messenger, MsnContact contact)
        {
        }

        public void contactAddCompleted(MsnMessenger messenger,
                                        MsnContact contact,
                                        MsnList list)
        {
            String contactID = contact.getEmail().getEmailAddress();

            if(!skipAddEvent.remove(contactID))
            {
                ContactMsnImpl contactToAdd =
                    findContactById(contact.getEmail().getEmailAddress());

                if(contactToAdd == null)
                {
                    contactToAdd =
                        new ContactMsnImpl(
                            contact,
                            ServerStoredContactListMsnImpl.this, true, true);
                    rootGroup.addContact(contactToAdd);
                    fireContactAdded(rootGroup, contactToAdd);
                }
                else
                {
                    ContactGroup oldGroup = contactToAdd.getParentContactGroup();

                    fireContactMoved(oldGroup, rootGroup, contactToAdd);
                }
            }
        }

        public void contactRemoveCompleted(MsnMessenger messenger,
                                           MsnContact contact,
                                           MsnList list)
        {
            ContactMsnImpl contactToRemove =
                findContactById(contact.getEmail().getEmailAddress());

            if(contactToRemove == null)
            {
                if (logger.isTraceEnabled())
                    logger.trace("Contact not found!" + contact);
                return;
            }

            if(contactToRemove.getParentContactGroup()
                            instanceof RootContactGroupMsnImpl)
            {
                rootGroup.removeContact(contactToRemove);
                fireContactRemoved(rootGroup, contactToRemove);
            }
            else
            {
                ContactGroupMsnImpl parentGroup = (ContactGroupMsnImpl)
                    contactToRemove.getParentContactGroup();
                parentGroup.removeContact(contactToRemove);
                fireContactRemoved(parentGroup, contactToRemove);
            }
        }

        public void groupAddCompleted(MsnMessenger messenger, MsnGroup group)
        {
            if (logger.isTraceEnabled())
                logger.trace("groupAdded " + group);
            ContactGroupMsnImpl newGroup =
                new ContactGroupMsnImpl(group,
                               new MsnContact[]{},
                               ServerStoredContactListMsnImpl.this,
                               true,
                               true);

            rootGroup.addSubGroup(newGroup);
            fireGroupEvent(newGroup, ServerStoredGroupEvent.GROUP_CREATED_EVENT);
        }
        public void groupRemoveCompleted(MsnMessenger messenger, MsnGroup g)
        {
            ContactGroupMsnImpl group = findContactGroupByMsnId(g.getGroupId());

            if(group == null)
            {
                if (logger.isTraceEnabled())
                    logger.trace("Group not found!" + g);
                return;
            }

            rootGroup.removeSubGroup(group);
            fireGroupEvent(group, ServerStoredGroupEvent.GROUP_REMOVED_EVENT);
        }

        /**
         * Called when a contact has been added to a group. Note that this
         * method would also be called after moving a contact.
         *
         * @param messenger a reference to the currently valid messenger
         * @param c the contact that has been added
         * @param g the group that the contact was added to
         */
        public void contactAddInGroupCompleted( MsnMessenger messenger,
                                                MsnContact c,
                                                MsnGroup g)
        {
            ContactMsnImpl contactToAdd =
                    findContactById(c.getEmail().getEmailAddress());

            if(contactToAdd == null)
            {
                //this is really a new contact
                contactToAdd =
                    new ContactMsnImpl(
                            c,
                            ServerStoredContactListMsnImpl.this, true, true);

                ContactGroupMsnImpl group =
                    findContactGroupByMsnId(g.getGroupId());

                if(group == null)
                {
                    logger.error("Group is missing!");
                    return;
                }

                group.addContact(contactToAdd);

                fireContactAdded(group, contactToAdd);
            }
            else
            {
                //the contact was added here.
                ContactGroup oldGroup =
                    contactToAdd.getParentContactGroup();

                ContactGroupMsnImpl group =
                    findContactGroupByMsnId(g.getGroupId());

                if(group == null)
                {
                    logger.error("Group is missing!");
                    return;
                }

                group.addContact(contactToAdd);


                if(oldGroup instanceof RootContactGroupMsnImpl)
                {
                    ((RootContactGroupMsnImpl)oldGroup)
                        .removeContact(contactToAdd);
                }
                else
                {
                    ((ContactGroupMsnImpl)oldGroup).removeContact(contactToAdd);
                }

                fireContactMoved(oldGroup, group, contactToAdd);
            }
        }

        public void contactRemoveFromGroupCompleted(MsnMessenger messenger,
            MsnContact c, MsnGroup g)
        {
            String contactID = c.getEmail().getEmailAddress();
            ContactMsnImpl contactToRemove = findContactById(contactID);

            if(g == null)
            {
                if (logger.isTraceEnabled())
                    logger.trace("Group is null! ");
                return;
            }

            ContactGroupMsnImpl dstGroup =
                findContactGroupByMsnId(g.getGroupId());

            if(contactToRemove == null)
            {
                if (logger.isTraceEnabled())
                    logger.trace("Contact not found " + c);
                return;
            }

            if(dstGroup == null)
            {
                if (logger.isTraceEnabled())
                    logger.trace("Group not found " + g);
                return;
            }

            //make sure that the contact was really in the group because when
            //moving contacts with multiple parent groups we get multiple
            //removal events. That's a problem because we don't support multiple
            //parent groups and we only show a single parent so all remove
            //events for the parent groups we are not displaying will cause
            //exceptions
            if(dstGroup.removeContact(contactToRemove))
                fireContactRemoved(dstGroup, contactToRemove);
        }

        public void ownerDisplayNameChanged(MsnMessenger arg0)
        {
        }
    }

    /**
     * Waits for removing a group
     */
    private static class ModListenerRemoveGroup
        extends EventAdapter
    {
        private int contactCount = 0;

        public ModListenerRemoveGroup(int c)
        {
            this.contactCount = c;
        }

        public void contactRemoved(MsnContact contact)
        {
            synchronized(this)
            {
                contactCount--;

                if(contactCount == 0)
                    notifyAll();
            }
        }

        public void waitForLastEvent(long waitFor)
        {
            synchronized(this)
            {
                if(contactCount == 0)
                    return;

                try{
                    wait(waitFor);
                }
                catch (InterruptedException ex)
                {
                    if (logger.isDebugEnabled())
                        logger.debug(
                        "Interrupted while waiting for a subscription evt", ex);
                }
            }
        }
    }

    /**
     * Emulates firing adding contact in group and moving contact to group.
     * When moving contact it is first adding to the new group then
     * it is removed from the old one.
     */
    private class ContactListModListenerImpl
        extends EventAdapter
    {
        @Override
        public void groupRenamed(MsnGroup group)
        {
            ContactGroupMsnImpl groupToRename =
                findContactGroupByMsnId(group.getGroupId());

            if(groupToRename == null)
            {
                if (logger.isTraceEnabled())
                    logger.trace("Group not found!" + group);
                return;
            }

            groupToRename.setSourceGroup(group);

            fireGroupEvent(groupToRename,
                           ServerStoredGroupEvent.GROUP_RENAMED_EVENT);
        }

        @Override
        public void loggingFromOtherLocation()
        {
            msnProvider.unregister(false);
            msnProvider.fireRegistrationStateChanged(
                msnProvider.getRegistrationState(),
                RegistrationState.UNREGISTERED,
                RegistrationStateChangeEvent.REASON_MULTIPLE_LOGINS, null);
        }
    }

    /**
     * Sets the messenger instance impl of the lib
     * which communicates with the server
     * @param messenger MsnMessenger
     */
    void setMessenger(MsnMessenger messenger)
    {
        if (this.messenger == messenger)
            return;

        if (this.messenger != null)
        {
            if (contactListModManager != null)
            {
                contactListModManager.removeModificationListener(
                        contactListModListenerImpl);
                contactListModManager = null;
            }
            if (contactListListener != null)
            {
                this.messenger.removeContactListListener(contactListListener);
                contactListListener = null;
            }
        }

        this.messenger = messenger;

        if (this.messenger != null)
        {
            contactListModManager
                = new EventManager(
                        msnProvider,
                        (BasicMessenger) this.messenger);
            contactListModManager.addModificationListener(
                    contactListModListenerImpl);

            contactListListener = new ContactListListener();
            this.messenger.addContactListListener(contactListListener);
        }
    }

    /**
     * when there is no image for contact we must retrieve it
     * add contacts for image update
     *
     * @param c ContactMsnImpl
     */
    protected void addContactForImageUpdate(ContactMsnImpl c)
    {
        if (messenger != null)
        {
            // Get the MsnObject
            MsnObject avatar = c.getSourceContact().getAvatar();

            if (avatar != null)
            {
                messenger.retrieveDisplayPicture(
                        avatar,
                        new ImageUpdater(c));
            }
        }
    }

    /**
     * used for debugging. Printing the serverside lists that msn supports
     */
    public void printList()
    {
        MsnMessenger messenger = msnProvider.getMessenger();

        if (logger.isInfoEnabled())
            logger.info("---=Start Printing contact list=---");

        MsnContactList list = messenger.getContactList();

        if (logger.isInfoEnabled())
            logger.info("Forward list");
        MsnContact[] c = list.getContactsInList(MsnList.FL);
        for(int i = 0; i < c.length; i++)
        {
            if (logger.isInfoEnabled())
                logger.info("c : " + c[i]);
            MsnGroup[] groups = c[i].getBelongGroups();
            for(int j = 0; j < groups.length; j++)
            {
                if (logger.isInfoEnabled())
                    logger.info("in group " + groups[j]);
            }
        }

        if (logger.isInfoEnabled())
            logger.info("Allow list");
        c = list.getContactsInList(MsnList.AL);
        for(int i = 0; i < c.length; i++)
        {
            if (logger.isInfoEnabled())
                logger.info("c : " + c[i] + " g:" + c[i].getBelongGroups().length);
        }

        if (logger.isInfoEnabled())
            logger.info("Block list");
        c = list.getContactsInList(MsnList.BL);
        for(int i = 0; i < c.length; i++)
        {
            if (logger.isInfoEnabled())
                logger.info("c : " + c[i] + " g:" + c[i].getBelongGroups().length);
        }

        if (logger.isInfoEnabled())
            logger.info("pending list");
        c = list.getContactsInList(MsnList.PL);
        for(int i = 0; i < c.length; i++)
        {
            if (logger.isInfoEnabled())
                logger.info("c : " + c[i] + " g:" + c[i].getBelongGroups().length);
        }

        if (logger.isInfoEnabled())
            logger.info("Reverse list");
        c = list.getContactsInList(MsnList.RL);
        for(int i = 0; i < c.length; i++)
        {
            if (logger.isInfoEnabled())
                logger.info("c : " + c[i] + " g:" +
                               c[i].getBelongGroups().length);
        }

        if (logger.isInfoEnabled())
            logger.info("Number of groups : "
                        + messenger.getContactList().getGroups().length);
        MsnGroup[] groups = messenger.getContactList().getGroups();
        for(int j = 0; j < groups.length; j++)
        {
            if (logger.isInfoEnabled())
                logger.info("group " + groups[j]);
        }
        if (logger.isInfoEnabled())
            logger.info("---=End Printing contact list=---");
    }

    private class ImageUpdater
            implements DisplayPictureListener
    {
        private ContactMsnImpl contact;
        ImageUpdater(ContactMsnImpl contact)
        {
            this.contact = contact;
        }

        public void notifyMsnObjectRetrieval(
                        MsnMessenger arg0messenger,
                        DisplayPictureRetrieveWorker worker,
                        MsnObject msnObject,
                        ResultStatus result,
                        byte[] resultBytes,
                        Object context)
        {
            if (result == ResultStatus.GOOD)
            {
                byte[] oldImage = contact.getImage(false);

                contact.setImage(resultBytes);

                parentOperationSet.fireContactPropertyChangeEvent(
                                ContactPropertyChangeEvent.PROPERTY_IMAGE,
                                contact, oldImage, resultBytes);
            }
        }

    }
}
