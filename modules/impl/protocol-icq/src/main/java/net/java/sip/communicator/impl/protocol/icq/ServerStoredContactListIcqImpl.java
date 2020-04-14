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
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.kano.joscar.snaccmd.ssi.*;
import net.kano.joustsim.*;
import net.kano.joustsim.oscar.*;
import net.kano.joustsim.oscar.oscar.service.ssi.*;

/**
 * This class encapsulates the net.kano BuddyList class. Once created, it will
 * register itself as a listener to the encapsulated BuddyList and modify it's
 * local copy of Contacts and ContactGroups every time an event is generated
 * by the underlying joustsim framework. The class would also generate
 * corresponding sip-communicator events to all events coming from joustsim.
 *
 * @author Emil Ivov
 * @author Damian Minkov
 */
public class ServerStoredContactListIcqImpl
        implements BuddyInfoTrackerListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>ServerStoredContactListIcqImpl</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ServerStoredContactListIcqImpl.class);

    /**
     * The joustsim buddy list that we encapsulate
     */
    private MutableBuddyList buddyList = null;

    /**
     * Our joustsim buddy list event listener
     */
    private BuddyListListener buddyListListener = new BuddyListListener();

    /**
     * Our joustsim group change listener.
     */
    private GroupChangeListener jsimGroupChangeListener
        = new GroupChangeListener();

    /**
     * A joust sim item change listener.
     */
    private JoustSimItemChangeListener jsimItemChangeListener
        = new JoustSimItemChangeListener();

    /**
     * Our joustsim buddy change listener.
     */
    private JoustSimBuddyListener jsimBuddyListener
        = new JoustSimBuddyListener();

    /**
     * The root contagroup. The container for all ICQ buddies and groups.
     */
    private final RootContactGroupIcqImpl rootGroup;

    /**
     * The joust sim service that deals with server stored information.
     */
    private SsiService jSimSsiService = null;

    /**
     * The operation set that created us and that we could use when dispatching
     * subscription events.
     */
    private OperationSetPersistentPresenceIcqImpl parentOperationSet = null;

    /**
     * The icqProvider that is on top of us.
     */
    private ProtocolProviderServiceIcqImpl icqProvider = null;

    /**
     * Listeners that would receive event notifications for changes in group
     * names or other properties, removal or creation of groups.
     */
    private final List<ServerStoredGroupListener> serverStoredGroupListeners
        = new Vector<ServerStoredGroupListener>();

    /**
     * Used for retrieving missing nicks on specified contacts
     */
    private NickRetriever nickRetriever = null;

    /**
     * Creates a ServerStoredContactList wrapper for the specified BuddyList.
     *
     * @param parentOperationSet the operation set that created us and that
     * we could use for dispatching subscription events
     * @param icqProvider the icqProvider that has instantiated us.
     */
    ServerStoredContactListIcqImpl(
        OperationSetPersistentPresenceIcqImpl parentOperationSet,
        ProtocolProviderServiceIcqImpl        icqProvider)
    {
        //don't add the sub ICQ groups to rootGroup here as we'll be having
        //event notifications for every one of them through the
        //RetroactiveBuddyListListener

        //We need to init these as early as possible to ensure that the provider
        //and the operationsset would not be null in the incoming events.
        this.parentOperationSet = parentOperationSet;

        this.icqProvider = icqProvider;
        this.rootGroup = new RootContactGroupIcqImpl(this.icqProvider);

        // waiting for the first contact to come
        // to start retreiving the missing nicknames
        if(icqProvider.USING_ICQ)
        {
            nickRetriever = new NickRetriever();

            parentOperationSet.addContactPresenceStatusListener(nickRetriever);

            // start the nick retreiver thread
            nickRetriever.start();
        }
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
    void fireGroupEvent(ContactGroup group, int eventID)
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
                , icqProvider
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

    private void fireGroupsReordered()
    {
        /** @todo implement fireGroupsReordered *///no need of args since it
        //could only mean one thing
    }

    /**
     * Make the parent persistent presence operation set dispatch a contact
     * added event.
     * @param parentGroup the group where the new contact was added
     * @param contact the contact that was added
     */
    private void fireContactAdded( ContactGroup parentGroup,
                                   Contact contact)
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
    }

    /**
     * Make the parent persistent presence operation set dispatch a contact
     * resolved event.
     * @param parentGroup the group that the resolved contact belongs to.
     * @param contact the contact that was resolved
     */
    private void fireContactResolved( ContactGroup parentGroup,
                                      Contact contact)
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
     * Make the parent persistent presence operation set dispatch a subscription
     * moved event.
     * @param oldParentGroup the group where the source contact was located
     * before being moved
     * @param newParentGroup the group that the source contact is currently in.
     * @param contact the contact that was added
     * @param index the index at which it was added.
     */
    private void fireContactMoved( ContactGroup oldParentGroup,
                                   ContactGroup newParentGroup,
                                   Contact contact,
                                   int index)
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
     * Make the parent persistent presence operation set dispatch a contact
     * removed event.
     * @param parentGroup the group where that the removed contact belonged to.
     * @param contact the contact that was removed.
     */
    private void fireContactRemoved( ContactGroup parentGroup,
                                     Contact contact)
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
     * Retrns a reference to the provider that created us.
     * @return a reference to a ProtocolProviderServiceIcqImpl instance.
     */
    ProtocolProviderServiceIcqImpl getParentProvider()
    {
        return icqProvider;
    }

    /**
     * Returns the index of the ContactGroup containing the specified joust sim
     * group.
     * @param joustSimGroup the joust sim group we're looking for.
     * @return the index of the ContactGroup containing the specified
     * joustSimGroup or -1 if no containing ContactGroup exists.
     */
    public int findContactGroupIndex(Group joustSimGroup)
    {
        Iterator<ContactGroup> contactGroups = rootGroup.subgroups();
        int index = 0;

        for (; contactGroups.hasNext(); index++)
        {
            ContactGroupIcqImpl contactGroup
                = (ContactGroupIcqImpl) contactGroups.next();

            if (joustSimGroup == contactGroup.getJoustSimSourceGroup())
                return index;

        }
        return -1;
    }

    /**
     * Returns the ConntactGroup with the specified name or null if no such
     * group was found.
     * <p>
     * @param name the name of the group we're looking for.
     * @return a reference to the ContactGroupIcqImpl instance we're looking for
     * or null if no such group was found.
     */
    public ContactGroupIcqImpl findContactGroup(String name)
    {
        Iterator<ContactGroup> contactGroups = rootGroup.subgroups();

        while(contactGroups.hasNext())
        {
            ContactGroup contactGroup = contactGroups.next();

            if (contactGroup.getGroupName().equals(name))
                return (ContactGroupIcqImpl)contactGroup;

        }
        return null;
    }


    /**
     * Returns the ContactGroup corresponding to the specified joust sim group.
     * @param joustSimGroup the joust sim group we're looking for.
     * @return the ContactGroup corresponding to the specified joustSimGroup
     * null if no containing ContactGroup exists.
     */
    public ContactGroupIcqImpl findContactGroup(Group joustSimGroup)
    {
        Iterator<ContactGroup> contactGroups = rootGroup.subgroups();

        while(contactGroups.hasNext())
        {
            ContactGroupIcqImpl contactGroup
                = (ContactGroupIcqImpl)contactGroups.next();

            if (joustSimGroup == contactGroup.getJoustSimSourceGroup())
                return contactGroup;

        }
        return null;
    }

    /**
     * Returns the Contact with the specified screenname (or icq UIN) or null if
     * no such screenname was found.
     *
     * @param screenName the screen name (or ICQ UIN) of the contact to find.
     * @return the <tt>Contact</tt> carrying the specified
     * <tt>screenName</tt> or <tt>null</tt> if no such contact exits.
     */
    public ContactIcqImpl findContactByScreenName(String screenName)
    {
        Iterator<ContactGroup> contactGroups = rootGroup.subgroups();
        ContactIcqImpl result = null;

        while(contactGroups.hasNext())
        {
            ContactGroupIcqImpl contactGroup
                = (ContactGroupIcqImpl)contactGroups.next();

            result = contactGroup.findContact(screenName);

            if (result != null)
                return result;

        }
        return null;
    }

    /**
     * Returns the Contact with the specified screenname (or icq UIN) or null if
     * no such screenname was found.
     *
     * @param buddy the buddy (or ICQ UIN) of the contact to find.
     * @return the <tt>Contact</tt> carrying the specified
     * <tt>screenName</tt> or <tt>null</tt> if no such contact exits.
     */
    public ContactIcqImpl findContactByJoustSimBuddy(Buddy buddy)
    {
        Iterator<ContactGroup> contactGroups = rootGroup.subgroups();
        String screenName = buddy.getScreenname().getFormatted();
        ContactIcqImpl result = null;

        while(contactGroups.hasNext())
        {
            ContactGroupIcqImpl contactGroup
                = (ContactGroupIcqImpl)contactGroups.next();

            result = contactGroup.findContact(screenName);

            if (result != null)
                return result;

        }
        return null;
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
    public ContactGroupIcqImpl findContactGroup(ContactIcqImpl child)
    {
        Iterator<ContactGroup> contactGroups = rootGroup.subgroups();

        while(contactGroups.hasNext())
        {
            ContactGroupIcqImpl contactGroup
                = (ContactGroupIcqImpl)contactGroups.next();

            if( contactGroup.findContact(child.getJoustSimBuddy())!= null)
                return contactGroup;
        }
        return null;
    }

    /**
     * Adds a new contact with the specified screenname to the list under a
     * default location.
     * @param screenname the screenname or icq uin of the contact to add.
     */
    public void addContact(String screenname)
    {
        ContactGroupIcqImpl parent = getFirstPersistentGroup();

        addContact(parent, screenname);
    }

    /**
     * Creates a non persistent contact for the specified address. This would
     * also create (if necessary) a group for volatile contacts that would not
     * be added to the server stored contact list. This method would have no
     * effect on the server stored contact list.
     * @param screenname the UIN/Screenname of the contact to create.
     * @return the newly created volatile <tt>ContactIcqImpl</tt>
     */
    ContactIcqImpl createVolatileContact(Screenname screenname)
    {
        if (logger.isTraceEnabled())
            logger.trace("createVolatileContact " + screenname);
        //First create the new volatile contact;
        Buddy volatileBuddy = new VolatileBuddy(screenname);

        ContactIcqImpl newVolatileContact
            = new ContactIcqImpl(volatileBuddy, this, false, false);

        //Check whether a volatile group already exists and if not create
        //one
        ContactGroupIcqImpl theVolatileGroup = getNonPersistentGroup();

        //if the parent group is null then add necessary create the group
        if (theVolatileGroup == null)
        {
            List<Buddy> emptyBuddies = new LinkedList<Buddy>();
            theVolatileGroup = new ContactGroupIcqImpl(
                new VolatileGroup(), emptyBuddies, this, false, false);
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
     * @param screenname the UIN/Screenname of the contact to create.
     * @return the newly created unresolved <tt>ContactIcqImpl</tt>
     */
    ContactIcqImpl createUnresolvedContact(ContactGroupIcqImpl parentGroup,
                                           Screenname  screenname)
    {
        if (logger.isTraceEnabled())
            logger.trace("createUnresolvedContact " + screenname);

        ContactIcqImpl existingContact
            = findContactByScreenName(screenname.getFormatted());

        if( existingContact != null)
        {
            return existingContact;
        }

        //First create the new volatile contact;
        Buddy volatileBuddy = new VolatileBuddy(screenname);

        ContactIcqImpl newUnresolvedContact
            = new ContactIcqImpl(volatileBuddy, this, true, false);

        parentGroup.addContact(newUnresolvedContact);

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
     * @return the newly created unresolved <tt>ContactGroupIcqImpl</tt>
     */
    ContactGroupIcqImpl createUnresolvedContactGroup(String groupName)
    {
        ContactGroupIcqImpl existingGroup = findContactGroup(groupName);

        if( existingGroup != null)
        {
            return existingGroup;
        }

        //First create the new volatile contact;
        List<Buddy> emptyBuddies = new LinkedList<Buddy>();
        ContactGroupIcqImpl newUnresolvedGroup = new ContactGroupIcqImpl(
                new VolatileGroup(groupName), emptyBuddies, this, false, true);

        this.rootGroup.addSubGroup(newUnresolvedGroup);

        fireGroupEvent(newUnresolvedGroup
                        , ServerStoredGroupEvent.GROUP_CREATED_EVENT);

        return newUnresolvedGroup;
    }

    /**
     * Adds a new contact with the specified screenname to the list under the
     * specified group.
     * @param screenname the screenname or icq uin of the contact to add.
     * @param parent the group under which we want the new contact placed.
     */
    public void addContact(ContactGroupIcqImpl parent, String screenname)
    {
        if (logger.isTraceEnabled())
            logger.trace("Adding contact " + screenname
                     + " to parent=" + parent.getGroupName());

        //if the contact is already in the contact list and is not volatile,
        //then only broadcast an event
        final ContactIcqImpl existingContact
            = findContactByScreenName(screenname);

        if( existingContact != null
            && existingContact.isPersistent() )
        {
            if (logger.isDebugEnabled())
                logger.debug("Contact " + screenname + " already exists. Gen. evt.");
            //broadcast the event in a separate thread so that we don't
            //block the calling thread.
            new Thread()
            {
                @Override
                public void run()
                {
                    parentOperationSet.fireSubscriptionEvent(
                        existingContact,
                        findContactGroup(existingContact),
                        SubscriptionEvent.SUBSCRIPTION_CREATED);
                }
            }.start();
            return;
        }

        if (logger.isTraceEnabled())
            logger.trace("Adding the contact to the specified group.");
        //extract the top level group
        AddMutableGroup group = parent.getJoustSimSourceGroup();

        group.addBuddy(screenname);
    }

    /**
     * Creates the specified group on the server stored contact list.
     * @param groupName a String containing the name of the new group.
     */
    public void createGroup(String groupName)
    {
        if (logger.isTraceEnabled())
            logger.trace("Creating group: " + groupName);
        buddyList.addGroup(groupName);
        if (logger.isTraceEnabled())
            logger.trace("Group " +groupName+ " created.");
    }

    /**
     * Removes the specified group from the icq buddy list.
     * @param groupToRemove the group that we'd like removed.
     */
    public void removeGroup(ContactGroupIcqImpl groupToRemove)
    {
        buddyList.deleteGroupAndBuddies(
            groupToRemove.getJoustSimSourceGroup());
    }

    /**
     * Renames the specified group according to the specified new name..
     * @param groupToRename the group that we'd like removed.
     * @param newName the new name of the group
     */
    public void renameGroup(ContactGroupIcqImpl groupToRename, String newName)
    {
        groupToRename.getJoustSimSourceGroup().rename(newName);
    }



    /**
     * Moves the specified <tt>contact</tt> to the group indicated by
     * <tt>newParent</tt>.
     * @param contact the contact that we'd like moved under the new group.
     * @param newParent the group where we'd like the parent placed.
     */
    public void moveContact(ContactIcqImpl contact,
                            ContactGroupIcqImpl newParent)
    {
        if(contact.isPersistent())
        {
            List<Buddy> contactsToMove = new ArrayList<Buddy>();

            contactsToMove.add(contact.getJoustSimBuddy());

            buddyList.moveBuddies(contactsToMove,
                                  newParent.getJoustSimSourceGroup());
        }
        else
        {
            // if the contact buddy is volatile
            // just add the buddy to the new group
            // if everything is ok. The volatile contact will be reused

            addContact(newParent, contact.getUIN());
        }
    }

    /**
     * Sets a reference to the currently active and valid instance of
     * the JoustSIM SsiService that this list is to use for retrieving
     * server stored information
     * @param joustSimSsiService a valid reference to the currently active JoustSIM
     * SsiService.
     */
    void init(  SsiService joustSimSsiService )
    {
        this.jSimSsiService = joustSimSsiService;
        jSimSsiService.addItemChangeListener(jsimItemChangeListener);

        this.buddyList = jSimSsiService.getBuddyList();
        buddyList.addRetroactiveLayoutListener(buddyListListener);
    }

    /**
     * Returns the first persistent group
     *
     * @return ContactGroupIcqImpl
     */
    private ContactGroupIcqImpl getFirstPersistentGroup()
    {
        for (int i = 0; i < getRootGroup().countSubgroups(); i++)
        {
            ContactGroupIcqImpl gr =
                (ContactGroupIcqImpl)getRootGroup().getGroup(i);

            if(gr.isPersistent())
                return gr;
        }

        return null;
    }

    /**
     * Returns the volatile group
     *
     * @return ContactGroupIcqImpl
     */
    private ContactGroupIcqImpl getNonPersistentGroup()
    {
        for (int i = 0; i < getRootGroup().countSubgroups(); i++)
        {
            ContactGroupIcqImpl gr =
                (ContactGroupIcqImpl)getRootGroup().getGroup(i);

            if(!gr.isPersistent())
                return gr;
        }

        return null;
    }


    /**
     * when there is no alias for contact we must retreive its nickname from server
     * but when the contact list is loaded the client is not yet registered to
     * server we wait this and then retreive the nicknames
     *
     * @param c ContactIcqImpl
     */
    protected void addContactForUpdate(ContactIcqImpl c)
    {
        if(icqProvider.USING_ICQ)
            nickRetriever.addContact(c);
    }

    ContactGroupIcqImpl findGroup(Buddy buddy)
    {
        Iterator<ContactGroup> iter = rootGroup.subgroups();
        while (iter.hasNext())
        {
            ContactGroupIcqImpl elem = (ContactGroupIcqImpl)iter.next();

            if(!elem.isPersistent() || !elem.isResolved())
                continue;

            for (Buddy b : elem.getJoustSimSourceGroup().getBuddiesCopy())
            {
                if(b == buddy)
                    return elem;
            }
        }

        return null;
    }

    private class BuddyListListener
        implements BuddyListLayoutListener
    {
        /**
         * Called by joustsim as a notification of the fact that the server has
         * sent the specified group and that it is actually a member from
         * our contact list. We copy the group locally and generate the
         * corresponding sip-communicator events
         *
         * @param list the BuddyList where this is happening.
         * @param oldItems we don't use it
         * @param newItems we don't use it
         * @param group the new Group that has been added
         * @param buddies the members of the new group.
         */
        public void groupAdded(BuddyList list,
                               List<? extends Group> oldItems,
                               List<? extends Group> newItems,
                               Group group,
                               List<? extends Buddy> buddies)
        {
            if (logger.isTraceEnabled())
                logger.trace("Group added: " + group.getName());
            if (logger.isTraceEnabled())
                logger.trace("Buddies: " + buddies);

            ContactGroupIcqImpl newGroup = findContactGroup(group.getName());

            //verify that this is indeed a new group
            if(newGroup == null)
            {
                newGroup = new ContactGroupIcqImpl(
                      (MutableGroup) group
                    , buddies
                    , ServerStoredContactListIcqImpl.this, true, true);

                //this is the first group so insert at 0.
                rootGroup.addSubGroup(newGroup);

                //tell listeners about the added group
                fireGroupEvent(newGroup
                               , ServerStoredGroupEvent.GROUP_CREATED_EVENT);
            }
            else
            {
                // if this is not a new group then it must be a unresolved one.
                // set it to resolved, do the same with its child buddies, fire
                // the corresponding events and bail out.
                List<Contact> newContacts = new ArrayList<Contact>();
                List<ContactIcqImpl> deletedContacts
                    = new ArrayList<ContactIcqImpl>();
                newGroup.updateGroup((MutableGroup)group, buddies
                                     , newContacts, deletedContacts);

                //fire an event saying that the group has been resolved
                fireGroupEvent(newGroup
                               , ServerStoredGroupEvent.GROUP_RESOLVED_EVENT);

                //fire events for contacts that have been removed;
                for (ContactIcqImpl contact : deletedContacts)
                    fireContactRemoved(newGroup, contact);

                //fire events for that contacts have been resolved or added
                Iterator<Contact> contactsIter = newGroup.contacts();
                while(contactsIter.hasNext())
                {
                    Contact contact = contactsIter.next();

                    if(newContacts.contains(contact))
                        fireContactAdded(newGroup, contact);
                    else
                        fireContactResolved(newGroup, contact);
                }
            }

            //add a joust sim buddy listener to all of the buddies in this group
            for (Buddy buddy : buddies)
                buddy.addBuddyListener(jsimBuddyListener);

            //register a listener for name changes of this group
            group.addGroupListener(jsimGroupChangeListener);
        }

        /**
         * Called by joust sim when a group is removed.
         *
         * @param list the <tt>BuddyList</tt> owning the removed group.
         * @param oldItems the list of items as it was before removing the group.
         * @param newItems the list of items as it is after the group is removed.
         * @param group the group that was removed.
         */
        public void groupRemoved(BuddyList list,
                                 List<? extends Group> oldItems,
                                 List<? extends Group> newItems,
                                 Group group)
        {
            if (logger.isTraceEnabled())
                logger.trace("Group Removed: " + group.getName());
            int index = findContactGroupIndex(group);

            if (index == -1)
            {
                if (logger.isDebugEnabled())
                    logger.debug("non existing group: " + group.getName());
                return;
            }

            ContactGroup removedGroup = rootGroup.getGroup(index);

            group.removeGroupListener(jsimGroupChangeListener);

            rootGroup.removeSubGroup(index);

            fireGroupEvent(removedGroup,
                           ServerStoredGroupEvent.GROUP_REMOVED_EVENT);
        }

        /**
         * Called by joust sim to notify us that a new buddy has been added
         * to the contact list.
         *
         * @param list the <tt>BuddyList</tt> owning the newly added buddy.
         * @param joustSimGroup the parent group of the added buddy.
         * @param oldItems unused
         * @param newItems unused
         * @param buddy the newly added <tt>buddy</tt>
         */
        public void buddyAdded( BuddyList list,
                                Group joustSimGroup,
                                List<? extends Buddy> oldItems,
                                List<? extends Buddy> newItems,
                                Buddy buddy)
        {
            if (logger.isTraceEnabled())
                logger.trace("Received buddyAdded " + buddy);
            //it is possible that the buddy being added is already in our
            //contact list. For example if they have sent a message to us they
            //would have been added to the local contact list as a
            //volatile/non-persistent contact without being added to the server
            //stored contact list. if this is the case make sure we keep the
            //same contact instance and issue a contact moved event instead of
            //a contact added event.
            ContactGroupIcqImpl oldParentGroup = null;
            ContactIcqImpl newContact = findContactByJoustSimBuddy(buddy);
            ContactGroupIcqImpl parentGroup = findContactGroup(joustSimGroup);

            boolean fireResolvedEvent = false;

            if (parentGroup == null)
            {
                if (logger.isDebugEnabled())
                    logger.debug("no parent group "
                             + joustSimGroup + " found for buddy: " + buddy);
                return;
            }

            if(newContact == null)
            {
                newContact = new ContactIcqImpl(
                    buddy, ServerStoredContactListIcqImpl.this, true, true);
            }
            else
            {
                oldParentGroup = findContactGroup(newContact);
                if(oldParentGroup != parentGroup)
                    oldParentGroup.removeContact(newContact);

                newContact.setJoustSimBuddy(buddy);
                newContact.setPersistent(true);
                if(!newContact.isResolved())
                {
                    newContact.setResolved(true);
                    fireResolvedEvent = true;
                }
            }

            parentGroup.addContact(newContact);

            int index = parentGroup.findContactIndex(newContact);

            //register a listener for name changes of this buddy
            buddy.addBuddyListener(jsimBuddyListener);

            //tell listeners about the added group
            if(oldParentGroup == null)
            {
                fireContactAdded(parentGroup, newContact);
            }
            else if(oldParentGroup != parentGroup)
            {
                fireContactMoved(oldParentGroup, parentGroup
                                 , newContact, index);
            }

            //fire an event in case the contact has just been resolved.
            if(fireResolvedEvent)
            {
                fireContactResolved(parentGroup, newContact);
            }
        }

        /**
         * Called by joust sim when a buddy is removed
         *
         * @param list the <tt>BuddyList</tt> containing the buddy
         * @param group the joust sim group that the buddy is removed from.
         * @param oldItems unused
         * @param newItems unused
         * @param buddy Buddy
         */
        public void buddyRemoved(BuddyList list,
                                 Group group,
                                 List<? extends Buddy> oldItems,
                                 List<? extends Buddy> newItems,
                                 Buddy buddy)
        {
            ContactGroupIcqImpl parentGroup = findContactGroup(group);
            ContactIcqImpl contactToRemove = parentGroup.findContact(buddy);

            if(contactToRemove != null)
            {
                parentGroup.removeContact(contactToRemove);

                buddy.removeBuddyListener(jsimBuddyListener);

                fireContactRemoved(parentGroup, contactToRemove);
            }
        }

        /**
         * Called by joust sim when contacts in a group have been reordered.
         * Removes all Contacts from the concerned group and reinserts them
         * in the right order.
         *
         * @param list the <tt>BuddyList</tt> where all this happens
         * @param group the group whose buddies have been reordered.
         * @param oldBuddies unused
         * @param newBuddies the list containing the buddies in their new order.
         */
        public void buddiesReordered(BuddyList list,
                                     Group group,
                                     List<? extends Buddy> oldBuddies,
                                     List<? extends Buddy> newBuddies)
        {
            //we don't support this any longer. check out SVN archives if
            //you need it for some reason.
        }

        /**
         * Called by joust sim to indicate that the server stored groups
         * have been reordered. We filter this list for contact groups that
         * we've already heard of and pass it to the root contact group
         * so that it woul reorder its subgroups.
         *
         * @param list the <tt>BuddyList</tt> where all this is happening
         * @param oldOrder unused
         * @param newOrder the order in which groups are now stored by the
         * AIM/ICQ server.
         */
        public void groupsReordered(BuddyList list,
                                    List<? extends Group> oldOrder,
                                    List<? extends Group> newOrder)
        {
            List<ContactGroupIcqImpl> reorderedGroups
                = new ArrayList<ContactGroupIcqImpl>();
            for (Group group : newOrder)
            {
                ContactGroupIcqImpl contactGroup = findContactGroup(group);

                //make sure that this was not an empty buddy.
                if (contactGroup == null)
                    continue;
                reorderedGroups.add(contactGroup);
            }

            rootGroup.reorderSubGroups(reorderedGroups);

            fireGroupsReordered();
        }
    }

    /**
     * Proxies events notifying of a change in the group name.
     */
    private class GroupChangeListener
        implements GroupListener
    {
        /**
         * Verifies whether the concerned group really exists and fires
         * a corresponding event
         * @param group the group that changed name.
         * @param oldName the name, before it changed
         * @param newName the current name of the group.
         */
        public void groupNameChanged(Group group, String oldName,
                                     String newName)
        {
            if (logger.isTraceEnabled())
                logger.trace("Group name for "+group.getName()+"changed from="
                         + oldName + " to=" + newName);
            ContactGroupIcqImpl contactGroup = findContactGroup(group);

            if (contactGroup == null)
            {
                if (logger.isDebugEnabled())
                    logger.debug(
                    "group name changed event received for unknown group"
                    + group);
                return;
            }

            //check whether the name has really changed (the joust sim stack
            //would call this method even when the name has not really changed
            //and values of oldName and newName would almost always be null)
            if (contactGroup.getGroupName()
                    .equals( contactGroup.getNameCopy() )){
                if (logger.isTraceEnabled())
                    logger.trace("Group name hasn't really changed("
                             +contactGroup.getGroupName()+"). Ignoring");
                return;
            }

            //we do have a new name. store a copy of it for our next deteciton
            //and fire the corresponding event.
            if (logger.isTraceEnabled())
                logger.trace("Dispatching group change event.");
            contactGroup.initNameCopy();

            fireGroupEvent(contactGroup,
                           ServerStoredGroupEvent.GROUP_RENAMED_EVENT);
        }

    }

    private static class JoustSimBuddyListener implements BuddyListener
    {
        /**
         * screennameChanged
         *
         * @param buddy Buddy
         * @param oldScreenname Screenname
         * @param newScreenname Screenname
         */
        public void screennameChanged(Buddy buddy, Screenname oldScreenname,
                                      Screenname newScreenname)
        {
            /** @todo implement screennameChanged() */
            if (logger.isDebugEnabled())
                logger.debug("/** @todo implement screennameChanged() */=");
            if (logger.isDebugEnabled())
                logger.debug("buddy="+buddy);
        }

        /**
         * alertActionChanged
         *
         * @param buddy Buddy
         * @param oldAlertAction int
         * @param newAlertAction int
         */
        public void alertActionChanged(Buddy buddy, int oldAlertAction,
                                       int newAlertAction)
        {
            /** @todo implement alertActionChanged() */
            if (logger.isDebugEnabled())
                logger.debug("/** @todo implement alertActionChanged() */=");
        }

        /**
         * alertSoundChanged
         *
         * @param buddy Buddy
         * @param oldAlertSound String
         * @param newAlertSound String
         */
        public void alertSoundChanged(Buddy buddy, String oldAlertSound,
                                      String newAlertSound)
        {
            /** @todo implement alertSoundChanged() */
            if (logger.isDebugEnabled())
                logger.debug("/** @todo implement alertSoundChanged() */");
        }

        /**
         * alertTimeChanged
         *
         * @param buddy Buddy
         * @param oldAlertEvent int
         * @param newAlertEvent int
         */
        public void alertTimeChanged(Buddy buddy, int oldAlertEvent,
                                     int newAlertEvent)
        {
            /** @todo implement alertTimeChanged() */
            if (logger.isDebugEnabled())
                logger.debug("/** @todo implement alertTimeChanged() */");
        }

        /**
         * aliasChanged
         *
         * @param buddy Buddy
         * @param oldAlias String
         * @param newAlias String
         */
        public void aliasChanged(Buddy buddy, String oldAlias, String newAlias)
        {
            /** @todo implement aliasChanged() */
            if (logger.isDebugEnabled())
                logger.debug("/** @todo implement aliasChanged() */");
        }

        /**
         * buddyCommentChanged
         *
         * @param buddy Buddy
         * @param oldComment String
         * @param newComment String
         */
        public void buddyCommentChanged(Buddy buddy, String oldComment,
                                        String newComment)
        {
            /** @todo implement buddyCommentChanged() */
            if (logger.isDebugEnabled())
                logger.debug("/** @todo implement buddyCommentChanged() */");
        }

        public void awaitingAuthChanged(Buddy simpleBuddy,
                                        boolean oldAwaitingAuth,
                                        boolean newAwaitingAuth)
        {
            /** @todo  */
            if (logger.isDebugEnabled())
                logger.debug("awaitingAuthChanged for " + simpleBuddy
                + " oldAwaitingAuth: " + oldAwaitingAuth
                + " newAwaitingAuth: " + newAwaitingAuth);
        }
    }

    /**
     * A dummy implementation of the JoustSIM SsiItemChangeListener.
     *
     * @author Emil Ivov
     */
    private static class JoustSimItemChangeListener
        implements SsiItemChangeListener
    {
        public void handleItemCreated(SsiItem item)
        {
            /** @todo implement handleItemCreated() */
            if (logger.isDebugEnabled())
                logger.debug("!!! TODO: implement handleItemCreated() !!!" + item
                         + " DATA=" + item.getData().toString());
        }

        public void handleItemDeleted(SsiItem item)
        {
            /** @todo implement handleItemDeleted() */
            if (logger.isDebugEnabled())
                logger.debug("!!! TODO: implement handleItemDeleted()!!!" + item);
        }

        public void handleItemModified(SsiItem item)
        {
            /** @todo implement handleItemModified() */
            if (logger.isDebugEnabled())
                logger.debug("!!! TODO: implement handleItemModified() !!!" + item
                         + " DATA=" + item.getData().toString());
        }
    }

    /**
     * Thread retreiving nickname and firing event for the change
     */
    private class NickRetriever
        extends Thread
        implements ContactPresenceStatusListener
    {
        /**
         * list with the accounts with missing nicknames
         */
        private final List<ContactIcqImpl> contactsForUpdate
            = new Vector<ContactIcqImpl>();

        private boolean isReadyForRetreive = false;

        @Override
        public void run()
        {
            try
            {
                Collection<ContactIcqImpl> copyContactsForUpdate = null;
                while (true)
                {
                    synchronized(contactsForUpdate){

                        if(contactsForUpdate.isEmpty())
                            contactsForUpdate.wait();

                        copyContactsForUpdate
                            = new Vector<ContactIcqImpl>(contactsForUpdate);
                        contactsForUpdate.clear();
                    }

                    Iterator<ContactIcqImpl> iter
                        = copyContactsForUpdate.iterator();
                    while (iter.hasNext())
                    {
                        ContactIcqImpl contact = iter.next();

                        String oldNickname = contact.getUIN();

                        String nickName = null;

                        try
                        {
                            nickName = getParentProvider().
                                getInfoRetreiver().getNickName(contact.getUIN());
                        }
                        catch (Exception e)
                        {
                            // if something happens do not interrupt
                            // the nickname retreiver
                        }

                        if(nickName != null)
                        {
                            contact.setNickname(nickName);
                            parentOperationSet.fireContactPropertyChangeEvent(
                                ContactPropertyChangeEvent.
                                PROPERTY_DISPLAY_NAME, contact,
                                oldNickname, nickName);
                        }
                        else
                            contact.setNickname(oldNickname);
                    }
                }
            }
            catch (InterruptedException ex)
            {
                logger.error("NickRetriever error waiting will stop now!", ex);
            }
        }

        /**
         * Add contact for retrieving
         * if the provider is register notify the retriever to get the nicks
         * if we are not registered add a listener to wait for registering
         *
         * @param contact ContactIcqImpl
         */
        synchronized void addContact(ContactIcqImpl contact)
        {
            synchronized(contactsForUpdate)
            {
                if (!contactsForUpdate.contains(contact))
                {
                    if (isReadyForRetreive)
                    {
                        contactsForUpdate.add(contact);
                        contactsForUpdate.notifyAll();
                    }
                    else
                    {
                        contactsForUpdate.add(contact);
                    }
                }
            }
        }

        /**
         * This is one of the first events after the client ready command
         * Used to start retrieving.
         * @param evt ContactPresenceStatusChangeEvent
         */
        public void contactPresenceStatusChanged(
            ContactPresenceStatusChangeEvent evt)
        {
            if(!isReadyForRetreive)
            {
               isReadyForRetreive = true;
               synchronized(contactsForUpdate){
                   contactsForUpdate.notifyAll();
               }
            }
        }
    }
}
