/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.customavatar.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.packet.*;
import org.osgi.framework.*;

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
public class ServerStoredContactListJabberImpl
{
    /**
     * The logger.
     */
    private static final Logger logger =
        Logger.getLogger(ServerStoredContactListJabberImpl.class);

    /**
     * The jabber list that we encapsulate
     */
    private Roster roster = null;

    /**
     * The root <code>ContactGroup</code>. The container for all jabber buddies
     * and groups.
     */
    private final RootContactGroupJabberImpl rootGroup;

    /**
     * The operation set that created us and that we could use when dispatching
     * subscription events.
     */
    private final OperationSetPersistentPresenceJabberImpl parentOperationSet;

    /**
     * The provider that is on top of us.
     */
    private final ProtocolProviderServiceJabberImpl jabberProvider;

    /**
     * Listeners that would receive event notifications for changes in group
     * names or other properties, removal or creation of groups.
     */
    private Vector<ServerStoredGroupListener> serverStoredGroupListeners
        = new Vector<ServerStoredGroupListener>();

    /**
     *  Thread retreiving images for contacts
     */
    private ImageRetriever imageRetriever = null;

    /**
     * Listens for roster changes.
     */
    private ChangeListener rosterChangeListener = null;

    /**
     * Creates a ServerStoredContactList wrapper for the specified BuddyList.
     *
     * @param parentOperationSet the operation set that created us and that
     * we could use for dispatching subscription events
     * @param provider the provider that has instantiated us.
     */
    ServerStoredContactListJabberImpl(
        OperationSetPersistentPresenceJabberImpl parentOperationSet,
        ProtocolProviderServiceJabberImpl        provider)
    {
        //We need to init these as early as possible to ensure that the provider
        //and the operationsset would not be null in the incoming events.
        this.parentOperationSet = parentOperationSet;

        this.jabberProvider = provider;
        this.rootGroup = new RootContactGroupJabberImpl(this.jabberProvider);
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
     * Returns the roster entry associated with the given XMPP address or
     * <tt>null</tt> if the user is not an entry in the roster.
     *
     * @param user the XMPP address of the user (e.g. "jsmith@example.com").
     * The address could be in any valid format (e.g. "domain/resource",
     * "user@domain" or "user@domain/resource").
     *
     * @return the roster entry or <tt>null</tt> if it does not exist.
     */
    RosterEntry getRosterEntry(String user)
    {
        if(roster == null)
            return null;
        else
            return roster.getEntry(user);
    }

    /**
     * Returns the roster group with the specified name, or <tt>null</tt> if the
     * group doesn't exist.
     *
     * @param name the name of the group.
     * @return the roster group with the specified name.
     */
    RosterGroup getRosterGroup(String name)
    {
        return roster.getGroup(name);
    }

    /**
     * Registers the specified group listener so that it would receive events
     * on group modification/creation/destruction.
     * @param listener the ServerStoredGroupListener to register for group events
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
    private void fireGroupEvent(ContactGroupJabberImpl group, int eventID)
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
                , jabberProvider
                , parentOperationSet);

        if (logger.isTraceEnabled())
            logger.trace("Will dispatch the following grp event: " + evt);

        Iterable<ServerStoredGroupListener> listeners;
        synchronized (serverStoredGroupListeners)
        {
            listeners
                = new ArrayList<ServerStoredGroupListener>(
                        serverStoredGroupListeners);
        }

        /**
         * Sometimes contact statuses are received before the groups and
         * contacts are being created. This is a problem when we don't have
         * already created unresolved contacts. So we will check contact
         * statuses to be sure they are correct.
         */
        if(eventID == ServerStoredGroupEvent.GROUP_CREATED_EVENT)
        {
            Iterator<Contact> iter = group.contacts();
            while (iter.hasNext())
            {
                ContactJabberImpl c = (ContactJabberImpl)iter.next();

                // roster can be null, receiving system messages from server
                // before we are log in
                if(roster != null)
                {
                    parentOperationSet.firePresenceStatusChanged(
                            roster.getPresence(c.getAddress()));
                }
            }
        }

        for (ServerStoredGroupListener listener : listeners)
        {
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
    void fireContactRemoved( ContactGroup parentGroup,
                                     ContactJabberImpl contact)
    {
        //bail out if no one's listening
        if(parentOperationSet == null){
            if (logger.isDebugEnabled())
                logger.debug("No presence op. set available. Bailing out.");
            return;
        }
        if (logger.isTraceEnabled())
            logger.trace("Removing " + contact.getAddress()
                        + " from " + parentGroup.getGroupName());

        // dispatch
        parentOperationSet.fireSubscriptionEvent(contact, parentGroup,
            SubscriptionEvent.SUBSCRIPTION_REMOVED);
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
                                   ContactGroupJabberImpl newParentGroup,
                                   ContactJabberImpl contact)
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
     * Retrns a reference to the provider that created us.
     * @return a reference to a ProtocolProviderServiceImpl instance.
     */
    ProtocolProviderServiceJabberImpl getParentProvider()
    {
        return jabberProvider;
    }

    /**
     * Returns the ConntactGroup with the specified name or null if no such
     * group was found.
     * <p>
     * @param name the name of the group we're looking for.
     * @return a reference to the ContactGroupJabberImpl instance we're looking for
     * or null if no such group was found.
     */
    public ContactGroupJabberImpl findContactGroup(String name)
    {
        Iterator<ContactGroup> contactGroups = rootGroup.subgroups();

        // make sure we ignore any whitespaces
        name = name.trim();

        while(contactGroups.hasNext())
        {
            ContactGroupJabberImpl contactGroup
                = (ContactGroupJabberImpl) contactGroups.next();

            if (contactGroup.getGroupName().trim().equals(name))
                return contactGroup;
        }

        return null;
    }

    /**
     * Find a group with the specified Copy of Name. Used to track when
     * a group name has changed
     * @param name String
     * @return ContactGroupJabberImpl
     */
    private ContactGroupJabberImpl findContactGroupByNameCopy(String name)
    {
        Iterator<ContactGroup> contactGroups = rootGroup.subgroups();

        // make sure we ignore any whitespaces
        name = name.trim();

        while(contactGroups.hasNext())
        {
            ContactGroupJabberImpl contactGroup
                = (ContactGroupJabberImpl) contactGroups.next();

            if (contactGroup.getNameCopy().trim().equals(name))
                return contactGroup;

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
    public ContactJabberImpl findContactById(String id)
    {
        Iterator<ContactGroup> contactGroups = rootGroup.subgroups();
        ContactJabberImpl result = null;
        String userId = StringUtils.parseBareAddress(id);

        while(contactGroups.hasNext())
        {
            ContactGroupJabberImpl contactGroup
                = (ContactGroupJabberImpl)contactGroups.next();

            result = contactGroup.findContact(userId);

            if (result != null)
                return result;
        }

        //try the root group now
        return rootGroup.findContact(userId);
    }

    /**
     * Returns the ContactGroup containing the specified contact or null
     * if no such group or contact exist.
     *
     * @param child the contact whose parent group we're looking for.
     * @return the <tt>ContactGroup</tt> containing the specified
     * <tt>contact</tt> or <tt>null</tt> if no such groupo or contact
     * exist.
     */
    public ContactGroup findContactGroup(ContactJabberImpl child)
    {
        Iterator<ContactGroup> contactGroups = rootGroup.subgroups();
        String contactAddress = child.getAddress();

        while(contactGroups.hasNext())
        {
            ContactGroupJabberImpl contactGroup
                = (ContactGroupJabberImpl)contactGroups.next();

            if( contactGroup.findContact(contactAddress)!= null)
                return contactGroup;
        }

        if ( rootGroup.findContact(contactAddress) != null)
            return rootGroup;

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
    public void addContact(ContactGroup parent, String id)
        throws OperationFailedException
    {
        if (logger.isTraceEnabled())
            logger.trace("Adding contact " + id + " to parent=" + parent);

        String completeID = parseAddressString(id);

        //if the contact is already in the contact list and is not volatile,
        //then only broadcast an event
        ContactJabberImpl existingContact = findContactById(completeID);

        if( existingContact != null
            && existingContact.isPersistent() )
        {
            if(logger.isDebugEnabled())
                logger.debug("Contact " + completeID
                                + " already exists in group "
                                + findContactGroup(existingContact));
            throw new OperationFailedException(
                "Contact " + completeID + " already exists.",
                OperationFailedException.SUBSCRIPTION_ALREADY_EXISTS);
        }

        try
        {
            String[] parentNames = null;

            if(parent != null)
                parentNames = new String[]{parent.getGroupName()};

            this.roster.createEntry(completeID, completeID, parentNames);
        }
        catch (XMPPException ex)
        {
            String errTxt = "Error adding new jabber entry";
            logger.error(errTxt, ex);

            int errorCode = OperationFailedException.INTERNAL_ERROR;

            XMPPError err = ex.getXMPPError();
            if(err != null)
            {
                if(err.getCode() > 400 && err.getCode() < 500)
                    errorCode = OperationFailedException.FORBIDDEN;
                else if(err.getCode() > 500)
                    errorCode = OperationFailedException.INTERNAL_SERVER_ERROR;

                errTxt = err.getCondition();
            }

            throw new OperationFailedException(errTxt, errorCode, ex);
        }
    }

    /**
     * Creates a non persistent contact for the specified address. This would
     * also create (if necessary) a group for volatile contacts that would not
     * be added to the server stored contact list. This method would have no
     * effect on the server stored contact list.
     * @param id the address of the contact to create.
     * @return the newly created volatile <tt>ContactImpl</tt>
     */
    ContactJabberImpl createVolatileContact(String id)
    {
        VolatileContactJabberImpl newVolatileContact
            = new VolatileContactJabberImpl(id, this);

        //Check whether a volatile group already exists and if not create
        //one
        ContactGroupJabberImpl theVolatileGroup = getNonPersistentGroup();

        //if the parent group is null then add necessary create the group
        if (theVolatileGroup == null)
        {
            theVolatileGroup = new VolatileContactGroupJabberImpl(
                JabberActivator.getResources().getI18NString(
                    "service.gui.NOT_IN_CONTACT_LIST_GROUP_NAME"),
                this);

            theVolatileGroup.addContact(newVolatileContact);

            this.rootGroup.addSubGroup(theVolatileGroup);

            fireGroupEvent(theVolatileGroup
                           , ServerStoredGroupEvent.GROUP_CREATED_EVENT);
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
     * @param parentGroup the group where the unersolved contact is to be
     * created
     * @param id the Address of the contact to create.
     * @return the newly created unresolved <tt>ContactImpl</tt>
     */
    ContactJabberImpl createUnresolvedContact(ContactGroup parentGroup,
                                              String  id)
    {
        ContactJabberImpl newUnresolvedContact
            = new ContactJabberImpl(id, this, false);

        if(parentGroup instanceof ContactGroupJabberImpl)
            ((ContactGroupJabberImpl)parentGroup).
                addContact(newUnresolvedContact);
        else if(parentGroup instanceof RootContactGroupJabberImpl)
            ((RootContactGroupJabberImpl)parentGroup).
                addContact(newUnresolvedContact);

        fireContactAdded(parentGroup, newUnresolvedContact);

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
    ContactGroupJabberImpl createUnresolvedContactGroup(String groupName)
    {
        ContactGroupJabberImpl newUnresolvedGroup =
            new ContactGroupJabberImpl(groupName, this);

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

        ContactGroupJabberImpl existingGroup = findContactGroup(groupName);

        if( existingGroup != null && existingGroup.isPersistent() )
        {
            if (logger.isDebugEnabled())
                logger.debug("ContactGroup " + groupName + " already exists.");
            throw new OperationFailedException(
                           "ContactGroup " + groupName + " already exists.",
                OperationFailedException.CONTACT_GROUP_ALREADY_EXISTS);
        }

        RosterGroup newRosterGroup = roster.createGroup(groupName);

        ContactGroupJabberImpl newGroup =
            new ContactGroupJabberImpl(newRosterGroup,
                                       new Vector<RosterEntry>().iterator(),
                                       this,
                                       true);
        rootGroup.addSubGroup(newGroup);

        fireGroupEvent(newGroup, ServerStoredGroupEvent.GROUP_CREATED_EVENT);

        if (logger.isTraceEnabled())
            logger.trace("Group " +groupName+ " created.");
    }

    /**
     * Removes the specified group from the buddy list.
     * @param groupToRemove the group that we'd like removed.
     */
    public void removeGroup(ContactGroupJabberImpl groupToRemove)
    {
        try
        {
            // first copy the item that will be removed
            // when iterating over group contacts and removing them
            // concurrent exception occures
            Vector<Contact> localCopy = new Vector<Contact>();
            Iterator<Contact> iter = groupToRemove.contacts();

            while (iter.hasNext())
            {
                localCopy.add(iter.next());
            }

            iter = localCopy.iterator();
            while (iter.hasNext())
            {
                ContactJabberImpl item = (ContactJabberImpl) iter.next();
                if(item.isPersistent())
                    roster.removeEntry(item.getSourceEntry());
            }
        }
        catch (XMPPException ex)
        {
            logger.error("Error removing group", ex);
        }
    }

    /**
     * Removes a contact from the serverside list
     * Event will come for successful operation
     * @param contactToRemove ContactJabberImpl
     */
    void removeContact(ContactJabberImpl contactToRemove)
        throws OperationFailedException
    {
        try
        {
            RosterEntry entry = contactToRemove.getSourceEntry();

            if (entry != null)//don't try to remove non-existing contacts.
                this.roster.removeEntry(entry);
        }
        catch (XMPPException ex)
        {
            String errTxt = "Error removing contact";
            logger.error(errTxt, ex);

            int errorCode = OperationFailedException.INTERNAL_ERROR;

            XMPPError err = ex.getXMPPError();
            if(err != null)
            {
                if(err.getCode() > 400 && err.getCode() < 500)
                    errorCode = OperationFailedException.FORBIDDEN;
                else if(err.getCode() > 500)
                    errorCode = OperationFailedException.INTERNAL_SERVER_ERROR;

                errTxt = err.getCondition();
            }

            throw new OperationFailedException(errTxt, errorCode, ex);
        }
    }


    /**
     * Renames the specified group according to the specified new name..
     * @param groupToRename the group that we'd like removed.
     * @param newName the new name of the group
     */
    public void renameGroup(ContactGroupJabberImpl groupToRename, String newName)
    {
        groupToRename.getSourceGroup().setName(newName);
        groupToRename.setNameCopy(newName);
    }

    /**
     * Moves the specified <tt>contact</tt> to the group indicated by
     * <tt>newParent</tt>.
     * @param contact the contact that we'd like moved under the new group.
     * @param newParent the group where we'd like the parent placed.
     */
    public void moveContact(ContactJabberImpl contact,
                            ContactGroupJabberImpl newParent)
    {
        // when the contact is not persistent, coming
        // from NotInContactList group, we need just to add it to the list
        if(!contact.isPersistent())
        {
            try
            {
                addContact(newParent, contact.getAddress());
                return;
            }
            catch(OperationFailedException ex)
            {
                logger.error("Cannot move contact! ", ex);
            }
        }

        newParent.addContact(contact);

        try
        {
            // will create the entry with the new group so it can be removed
            // from other groups if any
            roster.createEntry(contact.getSourceEntry().getUser(),
                               contact.getDisplayName(),
                               new String[]{newParent.getGroupName()});
        }
        catch (XMPPException ex)
        {
            logger.error("Cannot move contact! ", ex);
        }
    }

    /**
     * Sets a reference to the currently active and valid instance of
     * roster that this list is to use for retrieving
     * server stored information
     */
    void init()
    {
        this.roster = jabberProvider.getConnection().getRoster();

        initRoster();

        rosterChangeListener = new ChangeListener();
        this.roster.addRosterListener(rosterChangeListener);
    }

    /**
     * Cleanups references and listers.
     */
    void cleanup()
    {
        if(imageRetriever != null)
        {
            imageRetriever.quit();
            imageRetriever = null;
        }

        if(this.roster != null)
            this.roster.removeRosterListener(rosterChangeListener);

        this.rosterChangeListener = null;
        this.roster = null;
    }

    /**
     * When the protocol is online this method is used to fill or resolve
     * the current contact list
     */
    private void initRoster()
    {
        // first if unfiled entries will move them in a group
        if(roster.getUnfiledEntryCount() > 0)
        {
            for (RosterEntry item : roster.getUnfiledEntries())
            {
                ContactJabberImpl contact =
                    findContactById(item.getUser());

                // some services automatically add contacts from their
                // addressbook to the roster and those contacts are
                // with subscription none. If such already exist,
                // remove them. This is typically our own contact
                if(!isEntryDisplayable(item))
                {
                    if(contact != null)
                    {
                        ContactGroup parent = contact.getParentContactGroup();

                        if(parent instanceof RootContactGroupJabberImpl)
                            ((RootContactGroupJabberImpl)parent)
                                .removeContact(contact);
                        else
                            ((ContactGroupJabberImpl)parent)
                                .removeContact(contact);

                        fireContactRemoved(parent, contact);
                    }
                    continue;
                }

                if(contact == null)
                {
                    // if there is no such contact create it
                    contact = new ContactJabberImpl(item, this, true, true);
                    rootGroup.addContact(contact);

                    fireContactAdded(rootGroup, contact);
                }
                else
                {
                    // if contact exist so resolve it
                    contact.setResolved(item);

                    //fire an event saying that the unfiled contact has been
                    //resolved
                    fireContactResolved(rootGroup, contact);
                }
            }
        }

        // now search all root contacts for unresolved ones
        Iterator<Contact> iter = rootGroup.contacts();
        List<ContactJabberImpl> contactsToRemove
            = new ArrayList<ContactJabberImpl>();
        while(iter.hasNext())
        {
            ContactJabberImpl contact = (ContactJabberImpl)iter.next();
            if(!contact.isResolved())
            {
                contactsToRemove.add(contact);
            }
        }

        for(ContactJabberImpl contact : contactsToRemove)
        {
            rootGroup.removeContact(contact);

            fireContactRemoved(rootGroup, contact);
        }
        contactsToRemove.clear();

        // fill in root group
        for (RosterGroup item : roster.getGroups())
        {
            ContactGroupJabberImpl group =
                findContactGroup(item.getName());

            if(group == null)
            {
                // create the group as it doesn't exist
                ContactGroupJabberImpl newGroup =
                new ContactGroupJabberImpl(
                    item, item.getEntries().iterator(), this, true);

                rootGroup.addSubGroup(newGroup);

                //tell listeners about the added group
                fireGroupEvent(newGroup
                               , ServerStoredGroupEvent.GROUP_CREATED_EVENT);
            }
            else
            {
                // the group exist so just resolved. The group will check and
                // create or resolve its entries
                group.setResolved(item);

                //fire an event saying that the group has been resolved
                fireGroupEvent(group
                               , ServerStoredGroupEvent.GROUP_RESOLVED_EVENT);
            }
        }

        Iterator<ContactGroup> iterGroups = rootGroup.subgroups();
        List<ContactGroupJabberImpl> groupsToRemove
            = new ArrayList<ContactGroupJabberImpl>();
        while(iterGroups.hasNext())
        {
            ContactGroupJabberImpl group =
                (ContactGroupJabberImpl)iterGroups.next();

            if(!group.isResolved())
            {
                groupsToRemove.add(group);
            }

            Iterator<Contact> iterContacts = group.contacts();
            while(iterContacts.hasNext())
            {
                ContactJabberImpl contact =
                    (ContactJabberImpl)iterContacts.next();
                if(!contact.isResolved())
                {
                    contactsToRemove.add(contact);
                }
            }
            for(ContactJabberImpl contact : contactsToRemove)
            {
                group.removeContact(contact);

                fireContactRemoved(group, contact);
            }
            contactsToRemove.clear();
        }

        for(ContactGroupJabberImpl group: groupsToRemove)
        {
            rootGroup.removeSubGroup(group);

            fireGroupEvent(
                group, ServerStoredGroupEvent.GROUP_REMOVED_EVENT);
        }
    }

    /**
     * Returns the volatile group
     *
     * @return ContactGroupJabberImpl
     */
    private ContactGroupJabberImpl getNonPersistentGroup()
    {
        for (int i = 0; i < getRootGroup().countSubgroups(); i++)
        {
            ContactGroupJabberImpl gr =
                (ContactGroupJabberImpl)getRootGroup().getGroup(i);

            if(!gr.isPersistent())
                return gr;
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
                           ContactJabberImpl contact)
    {
        //bail out if no one's listening
        if(parentOperationSet == null){
            if (logger.isDebugEnabled())
                logger.debug("No presence op. set available. Bailing out.");
            return;
        }

        // if we are already registered(roster != null) and we are currently
        // creating the contact list, presences maybe already received
        // before we have created the contacts, so lets check
        if(roster != null)
        {
            parentOperationSet.firePresenceStatusChanged(
                    roster.getPresence(contact.getAddress()));
        }

        // dispatch
        parentOperationSet.fireSubscriptionEvent(contact, parentGroup,
            SubscriptionEvent.SUBSCRIPTION_CREATED);
    }

    /**
     * Make the parent persistent presence operation set dispatch a contact
     * resolved event.
     * @param parentGroup the group that the resolved contact belongs to.
     * @param contact the contact that was resolved
     */
    void fireContactResolved( ContactGroup parentGroup,
                              ContactJabberImpl contact)
    {
        //bail out if no one's listening
        if(parentOperationSet == null){
            if (logger.isDebugEnabled())
                logger.debug("No presence op. set available. Bailing out.");
            return;
        }

        // dispatch
        parentOperationSet.fireSubscriptionEvent(contact, parentGroup,
            SubscriptionEvent.SUBSCRIPTION_RESOLVED);
    }

    /**
     * when there is no image for contact we must retrieve it
     * add contacts for image update
     *
     * @param c ContactJabberImpl
     */
    protected void addContactForImageUpdate(ContactJabberImpl c)
    {
        if(imageRetriever == null)
        {
            imageRetriever = new ImageRetriever();
            imageRetriever.start();
        }

        imageRetriever.addContact(c);
    }

    /**
     * Some roster entries are not supposed to be seen.
     * Like some services automatically add contacts from their
     * addressbook to the roster and those contacts are with subscription none.
     * Best practices in XEP-0162.
     * - subscription='both' or subscription='to'
     * - ((subscription='none' or subscription='from') and ask='subscribe')
     * - ((subscription='none' or subscription='from')
     *          and (name attribute or group child))
     *
     * @param entry the entry to check.
     *
     * @return is item to be hidden/ignored.
     */
    static boolean isEntryDisplayable(RosterEntry entry)
    {
        if(entry.getType() == RosterPacket.ItemType.both
           || entry.getType() == RosterPacket.ItemType.to)
        {
            return true;
        }
        else if((entry.getType() == RosterPacket.ItemType.none
                    || entry.getType() == RosterPacket.ItemType.from)
                && (RosterPacket.ItemStatus.SUBSCRIPTION_PENDING.equals(
                    entry.getStatus())
                    || (entry.getGroups() != null
                        && entry.getGroups().size() > 0)))
        {
            return true;
        }

        return false;
    }

    /**
     * Receives changes in roster.
     */
    private class ChangeListener
        implements RosterListener
    {
        /**
         * Received event when entry is added to the server stored list
         * @param addresses Collection
         */
        public void entriesAdded(Collection<String> addresses)
        {
            if (logger.isTraceEnabled())
                logger.trace("entriesAdded " + addresses);

            for (String id : addresses)
            {
                RosterEntry entry = roster.getEntry(id);

                if(!isEntryDisplayable(entry))
                    continue;

                ContactJabberImpl contact =
                    findContactById(entry.getUser());

                if(contact != null)
                {
                    if(contact.isPersistent())
                    {
                        contact.setResolved(entry);
                        continue;
                    }
                    else
                    {
                        ContactGroup oldParentGroup =
                            contact.getParentContactGroup();
                        // if contact is in not in contact list
                        // we must remove it from there in order to correctlly
                        // process adding contact
                        // this happens if we accept subscribe request
                        // not from sip-communicator
                        if(oldParentGroup instanceof ContactGroupJabberImpl
                            && !oldParentGroup.isPersistent())
                        {
                            ((ContactGroupJabberImpl)oldParentGroup)
                                .removeContact(contact);
                            fireContactRemoved(oldParentGroup, contact);
                        }
                    }
                }

                contact = addEntryToContactList(id);
            }
        }

        /**
         * Adds the entry to our local contactlist.
         * @param rosterEntryID the entry id.
         * @return the newly created contact.
         */
        private ContactJabberImpl addEntryToContactList(String rosterEntryID)
        {
            RosterEntry entry = roster.getEntry(rosterEntryID);

            ContactJabberImpl contact = new ContactJabberImpl(
                    entry,
                    ServerStoredContactListJabberImpl.this,
                    true,
                    true);

            if(entry.getGroups() == null || entry.getGroups().size() == 0)
            {
                // no parent group so its in the root group
                rootGroup.addContact(contact);
                fireContactAdded(rootGroup, contact);

                return contact;
            }

            for (RosterGroup group : entry.getGroups())
            {
                ContactGroupJabberImpl parentGroup =
                    findContactGroup(group.getName());

                if(parentGroup != null)
                {
                    parentGroup.addContact(contact);
                    fireContactAdded(findContactGroup(contact), contact);
                }
                else
                {
                    // create the group as it doesn't exist
                    ContactGroupJabberImpl newGroup =
                        new ContactGroupJabberImpl(
                        group, group.getEntries().iterator(),
                        ServerStoredContactListJabberImpl.this,
                        true);

                    rootGroup.addSubGroup(newGroup);

                    //tell listeners about the added group
                    fireGroupEvent(newGroup,
                            ServerStoredGroupEvent.GROUP_CREATED_EVENT);
                }

                // as for now we only support contact only in one group
                return contact;
            }

            return contact;
        }

        /**
         * Event when an entry is updated. Something for the entry data
         * or have been added to a new group or removed from one
         * @param addresses Collection
         */
        public void entriesUpdated(Collection<String> addresses)
        {
            if (logger.isTraceEnabled())
                logger.trace("entriesUpdated  " + addresses);

            // will search for group renamed
            for (String contactID : addresses)
            {
                RosterEntry entry = roster.getEntry(contactID);

                ContactJabberImpl contact = findContactById(contactID);

                if(contact == null && isEntryDisplayable(entry))
                {
                    // this can be an entry which has been update
                    // was not displayable, but now it is, so lets
                    // display it
                    contact = addEntryToContactList(contactID);
                }

                for (RosterGroup gr : entry.getGroups())
                {
                    if(findContactGroup(gr.getName()) == null)
                    {
                        // such group does not exist. so it must be
                        // renamed one
                        ContactGroupJabberImpl group =
                            findContactGroupByNameCopy(gr.getName());
                        if(group != null)
                        {
                            // just change the source entry
                            group.setSourceGroup(gr);

                            fireGroupEvent(group,
                                           ServerStoredGroupEvent.GROUP_RENAMED_EVENT);
                        }
                        else
                        {
                            // strange ???
                        }
                    }
                    else
                    {
                        // the group is found the contact may be moved from one group
                        // to another
                        ContactGroup contactGroup =
                            contact.getParentContactGroup();

                        if(!gr.getName().equals(contactGroup.getGroupName()))
                        {
                            // the contact is moved to onether group
                            // first remove it from the original one
                            if(contactGroup instanceof ContactGroupJabberImpl)
                                ((ContactGroupJabberImpl)contactGroup).
                                    removeContact(contact);
                            else if(contactGroup instanceof RootContactGroupJabberImpl)
                                ((RootContactGroupJabberImpl)contactGroup).
                                    removeContact(contact);

                            // the add it to the new one
                            ContactGroupJabberImpl newParentGroup =
                                findContactGroup(gr.getName());

                            newParentGroup.addContact(contact);

                            fireContactMoved(contactGroup,
                                             newParentGroup,
                                             contact);
                        }
                    }
                }
            }
        }

        /**
         * Event received when entry has been removed from the list
         * @param addresses Collection
         */
        public void entriesDeleted(Collection<String> addresses)
        {
            Iterator<String> iter = addresses.iterator();
            while (iter.hasNext())
            {
                String address = iter.next();
                if (logger.isTraceEnabled())
                    logger.trace("entry deleted " + address);

                ContactJabberImpl contact = findContactById(address);

                if(contact == null)
                {
                    if (logger.isTraceEnabled())
                        logger.trace("Could not find contact for deleted entry:"
                                    + address);
                    continue;
                }

                ContactGroup group = findContactGroup(contact);

                if(group == null)
                {
                    if (logger.isTraceEnabled())
                        logger.trace("Could not find ParentGroup for deleted entry:"
                                    + address);
                    continue;
                }

                if(group instanceof ContactGroupJabberImpl)
                {
                    ContactGroupJabberImpl groupImpl
                        = (ContactGroupJabberImpl)group;

                    // remove the contact from parrent group
                    groupImpl.removeContact(contact);

                    // if the group is empty remove it from
                    // root group. This group will be removed
                    // from server if empty
                    if (groupImpl.countContacts() == 0)
                    {
                        rootGroup.removeSubGroup(groupImpl);

                        fireContactRemoved(groupImpl, contact);
                        fireGroupEvent(groupImpl,
                                   ServerStoredGroupEvent.GROUP_REMOVED_EVENT);
                    }
                    else
                        fireContactRemoved(groupImpl, contact);
                }
                else if(group instanceof RootContactGroupJabberImpl)
                {
                    rootGroup.removeContact(contact);

                    fireContactRemoved(rootGroup, contact);
                }

            }
        }

        /**
         * Not used here.
         * @param presence
         */
        public void presenceChanged(Presence presence)
        {}
    }

    /**
     * Thread retrieving images.
     */
    private class ImageRetriever
        extends Thread
    {
        /**
         * list with the accounts with missing image
         */
        private final List<ContactJabberImpl> contactsForUpdate
            = new Vector<ContactJabberImpl>();

        /**
         * Should we stop.
         */
        private boolean running = false;

        /**
         * Creates image retrieving.
         */
        ImageRetriever()
        {
            setDaemon(true);
        }

        /**
         * Thread entry point.
         */
        public void run()
        {
            try
            {
                Collection<ContactJabberImpl> copyContactsForUpdate = null;
                running = true;
                while (running)
                {
                    synchronized(contactsForUpdate)
                    {
                        if(contactsForUpdate.isEmpty())
                            contactsForUpdate.wait();

                        if(!running)
                            return;

                        copyContactsForUpdate
                            = new Vector<ContactJabberImpl>(contactsForUpdate);
                        contactsForUpdate.clear();
                    }

                    Iterator<ContactJabberImpl> iter
                        = copyContactsForUpdate.iterator();
                    while (iter.hasNext())
                    {
                        ContactJabberImpl contact = iter.next();

                        byte[] imgBytes = getAvatar(contact);

                        if(imgBytes != null)
                        {
                            byte[] oldImage = contact.getImage(false);

                            contact.setImage(imgBytes);
                            parentOperationSet.fireContactPropertyChangeEvent(
                                ContactPropertyChangeEvent.PROPERTY_IMAGE,
                                contact, oldImage, imgBytes);
                        }
                        else
                            // set an empty image data so it won't be queried again
                            contact.setImage(new byte[0]);
                    }
                }
            }
            catch (InterruptedException ex)
            {
                logger.error("ImageRetriever error waiting will stop now!", ex);
            }
        }

        /**
         * Add contact for retrieving
         * if the provider is register notify the retriever to get the nicks
         * if we are not registered add a listener to wait for registering
         *
         * @param contact ContactJabberImpl
         */
        synchronized void addContact(ContactJabberImpl contact)
        {
            synchronized(contactsForUpdate)
            {
                if (!contactsForUpdate.contains(contact))
                {
                    contactsForUpdate.add(contact);
                    contactsForUpdate.notifyAll();
                }
            }
        }

        /**
         * Stops this thread.
         */
        void quit()
        {
            synchronized(contactsForUpdate)
            {
                running = false;
                contactsForUpdate.notifyAll();
            }
        }

        /**
         * Retrieves the avatar.
         * @param contact the contact.
         * @return the contact avatar.
         */
        private byte[] getAvatar(ContactJabberImpl contact)
        {
            byte[] result;
            try
            {
                XMPPConnection connection = jabberProvider.getConnection();

                if(connection == null || !connection.isAuthenticated())
                    return null;

                VCard card = new VCard();

                card.load(connection, contact.getAddress());
                result = card.getAvatar();

                if(result == null)
                {
                    result = searchForCustomAvatar(contact.getAddress());
                }

                return result;
            }
            catch (Exception ex)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(
                            "Cannot load image for contact "
                                + this
                                + ": "
                                + ex.getMessage(),
                            ex);
                }

                result = searchForCustomAvatar(contact.getAddress());
                if(result == null)
                    result = new byte[0];
            }

            return result;
        }
    }

    /**
     * Query custom avatar services and returns the first found avtar.
     * @return the found avatar if any.
     */
    private byte[] searchForCustomAvatar(String address)
    {
        try
        {
            ServiceReference[] refs =  JabberActivator.bundleContext
                .getServiceReferences(CustomAvatarService.class.getName(), null);

            if(refs == null)
                return null;

            for(ServiceReference r : refs)
            {
                CustomAvatarService avatarService =
                    (CustomAvatarService)JabberActivator
                        .bundleContext.getService(r);

                byte[] res = avatarService.getAvatar(address);

                if(res != null)
                    return res;
            }
        }
        catch(Throwable t)
        {
            // if something is wrong just return empty image
        }

        return null;
    }

    /**
     * Completes the identifier with the server part if no server part was
     * previously added.
     *
     * @param id the initial identifier as added by the user
     */
    private String parseAddressString(String id)
    {
        if (id.indexOf("@") < 0)
        {
            AccountID accountID
                = jabberProvider.getAccountID();

            String serverPart;
            String userID = accountID.getUserID();
            int atIndex = userID.indexOf('@');
            if (atIndex > 0)
                serverPart = userID.substring(atIndex + 1);
            else
                serverPart = accountID.getService();

            return id + "@" + serverPart;
        }

        return id;
    }
}
