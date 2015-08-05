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
package net.java.sip.communicator.impl.protocol.yahoo;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import ymsg.network.*;
import ymsg.network.event.*;

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
public class ServerStoredContactListYahooImpl
{
    private static final Logger logger =
        Logger.getLogger(ServerStoredContactListYahooImpl.class);

    /**
     * If there is no group and we add contact with no parent
     * a default group is created with name : DEFAULT_GROUP_NAME
     */
    private static final String DEFAULT_GROUP_NAME = "General";

    /**
     * The root contagroup. The container for all yahoo buddies and groups.
     */
    private final RootContactGroupYahooImpl rootGroup;

    /**
     * The operation set that created us and that we could use when dispatching
     * subscription events.
     */
    private final OperationSetPersistentPresenceYahooImpl parentOperationSet;

    /**
     * The provider that is on top of us.
     */
    private final ProtocolProviderServiceYahooImpl yahooProvider;

    private YahooSession yahooSession = null;

    /**
     * Listeners that would receive event notifications for changes in group
     * names or other properties, removal or creation of groups.
     */
    private Vector<ServerStoredGroupListener> serverStoredGroupListeners
        = new Vector<ServerStoredGroupListener>();

    private ContactListModListenerImpl contactListModListenerImpl
        = new ContactListModListenerImpl();

    /**
     * Handler for incoming authorization requests.
     */
    private AuthorizationHandler handler = null;

    private Hashtable<String, String> addedCustomYahooIds
        = new Hashtable<String, String>();

    /**
     * Creates a ServerStoredContactList wrapper for the specified BuddyList.
     *
     * @param parentOperationSet the operation set that created us and that
     * we could use for dispatching subscription events
     * @param provider the provider that has instantiated us.
     */
    ServerStoredContactListYahooImpl(
        OperationSetPersistentPresenceYahooImpl parentOperationSet,
        ProtocolProviderServiceYahooImpl        provider)
    {
        //We need to init these as early as possible to ensure that the provider
        //and the operationsset would not be null in the incoming events.
        this.parentOperationSet = parentOperationSet;

        this.yahooProvider = provider;
        this.rootGroup = new RootContactGroupYahooImpl(this.yahooProvider);
    }

    /**
     * Handler for incoming authorization requests.
     *
     * @param handler an instance of an AuthorizationHandler for
     *   authorization requests coming from other users requesting
     *   permission add us to their contact list.
     */
    public void setAuthorizationHandler(AuthorizationHandler handler)
    {
        this.handler = handler;
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
                serverStoredGroupListeners.add(listener);
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
    private void fireGroupEvent(ContactGroupYahooImpl group, int eventID)
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
                , yahooProvider
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
            try{
                if (eventID == ServerStoredGroupEvent.GROUP_REMOVED_EVENT)
                    listener.groupRemoved(evt);
                else if (eventID == ServerStoredGroupEvent.GROUP_RENAMED_EVENT)
                    listener.groupNameChanged(evt);
                else if (eventID == ServerStoredGroupEvent.GROUP_CREATED_EVENT)
                    listener.groupCreated(evt);
                else if (eventID == ServerStoredGroupEvent.GROUP_RESOLVED_EVENT)
                    listener.groupResolved(evt);
            }catch(Exception ex){
                logger.warn("Unhandled Exception! ", ex);
            }
        }
    }

    /**
     * Make the parent persistent presence operation set dispatch a contact
     * removed event.
     * @param parentGroup the group where that the removed contact belonged to.
     * @param contact the contact that was removed.
     */
    private void fireContactRemoved( ContactGroup parentGroup,
                                     ContactYahooImpl contact)
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
                                   ContactGroupYahooImpl newParentGroup,
                                   ContactYahooImpl contact)
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
    ProtocolProviderServiceYahooImpl getParentProvider()
    {
        return yahooProvider;
    }

    /**
     * Returns the ConntactGroup with the specified name or null if no such
     * group was found.
     * <p>
     * @param name the name of the group we're looking for.
     * @return a reference to the ContactGroupYahooImpl instance we're looking
     * for or null if no such group was found.
     */
    public ContactGroupYahooImpl findContactGroup(String name)
    {
        String nameToLookFor = replaceIllegalChars(name);
        Iterator<ContactGroup> contactGroups = rootGroup.subgroups();

        while(contactGroups.hasNext())
        {
            ContactGroupYahooImpl contactGroup
                = (ContactGroupYahooImpl) contactGroups.next();

            if (contactGroup.getGroupName().equals(nameToLookFor))
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
    public ContactYahooImpl findContactById(String id)
    {
        Iterator<ContactGroup> contactGroups = rootGroup.subgroups();
        ContactYahooImpl result = null;

        while(contactGroups.hasNext())
        {
            ContactGroupYahooImpl contactGroup
                = (ContactGroupYahooImpl) contactGroups.next();

            result = contactGroup.findContact(id);

            if (result != null)
                return result;
        }

        return null;
    }

    /**
     * Returns the Contact corresponding to the specified <tt>YahooUser</tt>
     * or null if no such id was found.
     *
     * @param yahooUser the YahooUser of the contact to find.
     * @return the <tt>Contact</tt> carrying the specified
     * <tt>screenName</tt> or <tt>null</tt> if no such contact exits.
     */
    public ContactYahooImpl findContactByYahooUser(YahooUser yahooUser)
    {
        return findContactById(yahooUser.getId().toLowerCase());
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
    public ContactGroup findContactGroup(ContactYahooImpl child)
    {
        Iterator<ContactGroup> contactGroups = rootGroup.subgroups();
        String contactAddress = child.getAddress();

        while(contactGroups.hasNext())
        {
            ContactGroupYahooImpl contactGroup
                = (ContactGroupYahooImpl) contactGroups.next();

            if( contactGroup.findContact(contactAddress)!= null)
                return contactGroup;
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
        ContactGroupYahooImpl parent = getFirstPersistentGroup();

        if(parent == null)
        {
            // if there is no group create it
            parent = createUnresolvedContactGroup(DEFAULT_GROUP_NAME);
        }

        addContact(parent, id);
    }

    /**
     * Adds a new contact with the specified screenname to the list under the
     * specified group.
     * @param id the id of the contact to add.
     * @param parent the group under which we want the new contact placed.
     * @throws OperationFailedException if the contact already exist
     */
    public void addContact(final ContactGroupYahooImpl parent, String id)
        throws OperationFailedException
    {
        if (logger.isTraceEnabled())
            logger.trace("Adding contact " + id + " to parent=" + parent);

        //if the contact is already in the contact list and is not volatile,
        //then only broadcast an event
        ContactYahooImpl existingContact = findContactById(id);

        if( existingContact != null
            && existingContact.isPersistent() )
        {
            if (logger.isDebugEnabled())
                logger.debug("Contact " + id + " already exists.");
            throw new OperationFailedException(
                "Contact " + id + " already exists.",
                OperationFailedException.SUBSCRIPTION_ALREADY_EXISTS);
        }

        if(id.indexOf("@") > -1 )
            addedCustomYahooIds.put(YahooSession.getYahooUserID(id), id);

        try
        {
            yahooSession.addFriend(YahooSession.getYahooUserID(id),
                                   parent.getGroupName());
        }
        catch(IOException ex)
        {
            throw new OperationFailedException(
                "Contact cannot be added " + id,
                OperationFailedException.NETWORK_FAILURE);
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
    ContactYahooImpl createVolatileContact(String id)
    {
        if (logger.isTraceEnabled())
            logger.trace("Creating volatile contact " + id);
        ContactYahooImpl newVolatileContact =
            new ContactYahooImpl(id, this, false, false, true);

        //Check whether a volatile group already exists and if not create one
        ContactGroupYahooImpl theVolatileGroup = getNonPersistentGroup();

        //if the parent group is null then create it
        if (theVolatileGroup == null)
        {
            theVolatileGroup = new VolatileContactGroupYahooImpl(
                YahooActivator.getResources().getI18NString(
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
    ContactYahooImpl createUnresolvedContact(ContactGroup parentGroup,
                                            String id)
    {
        if (logger.isTraceEnabled())
            logger.trace("Creating unresolved contact " + id
                        + " to parent=" + parentGroup);

        ContactYahooImpl existingContact = findContactById(id);

        if( existingContact != null)
        {
            return existingContact;
        }

        ContactYahooImpl newUnresolvedContact
            = new ContactYahooImpl(id, this, false, true, false);

        if(parentGroup instanceof ContactGroupYahooImpl)
            ((ContactGroupYahooImpl)parentGroup).
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
    ContactGroupYahooImpl createUnresolvedContactGroup(String groupName)
    {
        ContactGroupYahooImpl existingGroup = findContactGroup(groupName);

        if( existingGroup != null )
        {
            if (logger.isDebugEnabled())
                logger.debug("ContactGroup " + groupName + " already exists.");
            return existingGroup;
        }

        ContactGroupYahooImpl newUnresolvedGroup =
            new ContactGroupYahooImpl(groupName, this);

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

        ContactGroupYahooImpl existingGroup = findContactGroup(groupName);

        if( existingGroup != null && existingGroup.isPersistent() )
        {
            if (logger.isDebugEnabled())
                logger.debug("ContactGroup " + groupName + " already exists.");
            throw new OperationFailedException(
                           "ContactGroup " + groupName + " already exists.",
                OperationFailedException.CONTACT_GROUP_ALREADY_EXISTS);
        }

        // create unresolved group if friend is added - group will be resolved
        createUnresolvedContactGroup(groupName);
    }

    /**
     * Removes the specified group from the buddy list.
     * @param groupToRemove the group that we'd like removed.
     */
    @SuppressWarnings("unchecked") //jymsg legacy code
    public void removeGroup(ContactGroupYahooImpl groupToRemove)
    {
        // to remove group just remove all the contacts in it

        if (logger.isTraceEnabled())
            logger.trace("removing group " + groupToRemove);

        // if its not persistent group just remove it
        if(!groupToRemove.isPersistent() || !groupToRemove.isResolved())
        {
            rootGroup.removeSubGroup(groupToRemove);
            fireGroupEvent(groupToRemove,
                ServerStoredGroupEvent.GROUP_REMOVED_EVENT);
            return;
        }

        Vector<YahooUser> contacts
            = groupToRemove.getSourceGroup().getMembers();

        if(contacts.size() == 0)
        {
            // the group is empty just remove it
            rootGroup.removeSubGroup(groupToRemove);
            fireGroupEvent(groupToRemove,
                ServerStoredGroupEvent.GROUP_REMOVED_EVENT);
            return;
        }

        /*
         * ContactGroupYahooImpl#getGroupName() isn't a plain getter so
         * performance-wise we're better off not calling it multiple times in
         * the following loop.
         */
        String groupToRemoveName = groupToRemove.getGroupName();

        for (YahooUser item : contacts)
        {
            try
            {
                yahooSession.removeFriend(item.getId(), groupToRemoveName);
            }
            catch(IOException ex)
            {
                if (logger.isInfoEnabled())
                    logger.info("Cannot Remove contact " + item.getId());
            }
        }
    }

    /**
     * Removes a contact from the serverside list
     * Event will come for successful operation
     * @param contactToRemove ContactYahooImpl
     */
    void removeContact(ContactYahooImpl contactToRemove)
    {
        if (logger.isTraceEnabled())
            logger.trace("Removing yahoo contact "
                        + contactToRemove.getSourceContact());

        if(contactToRemove.isVolatile())
        {
            ContactGroupYahooImpl parent =
                (ContactGroupYahooImpl)contactToRemove.getParentContactGroup();

            parent.removeContact(contactToRemove);
            fireContactRemoved(parent, contactToRemove);
            return;
        }

        try
        {
            yahooSession.removeFriend(
                contactToRemove.getSourceContact().getId(),
                contactToRemove.getParentContactGroup().getGroupName());
        }
        catch(IOException ex)
        {
            if (logger.isInfoEnabled())
                logger.info("Cannot Remove contact " + contactToRemove);
        }
    }

    /**
     * Renames the specified group according to the specified new name..
     * @param groupToRename the group that we'd like removed.
     * @param newName the new name of the group
     */
    public void renameGroup(ContactGroupYahooImpl groupToRename, String newName)
    {
        // not working
        /*
            try
            {
                yahooSession.renameGroup(groupToRename.getGroupName(), newName);
            }
            catch(IOException ex)
            {
                if (logger.isInfoEnabled())
                    logger.info("Cannot rename group " + groupToRename);
            }

            fireGroupEvent(groupToRename,
                           ServerStoredGroupEvent.GROUP_RENAMED_EVENT);
         */
    }

    /**
     * Moves the specified <tt>contact</tt> to the group indicated by
     * <tt>newParent</tt>.
     * @param contact the contact that we'd like moved under the new group.
     * @param newParent the group where we'd like the parent placed.
     */
    public void moveContact(ContactYahooImpl contact,
                            ContactGroupYahooImpl newParent)
    {
        String userID = contact.getID();
        try
        {
            contactListModListenerImpl.
                waitForMove(userID,
                contact.getParentContactGroup().getGroupName());

            yahooSession.addFriend(
                userID,
                newParent.getGroupName());
        }
        catch(IOException ex)
        {
            contactListModListenerImpl.removeWaitForMove(userID);
            logger.error("Contact cannot be added " + ex.getMessage());
        }
    }

    /**
     * Returns the volatile group
     *
     * @return ContactGroupYahooImpl
     */
    private ContactGroupYahooImpl getNonPersistentGroup()
    {
        for (int i = 0; i < getRootGroup().countSubgroups(); i++)
        {
            ContactGroupYahooImpl gr =
                (ContactGroupYahooImpl)getRootGroup().getGroup(i);

            if(!gr.isPersistent())
                return gr;
        }

        return null;
    }

    /**
     * Returns the first persistent group
     *
     * @return ContactGroupIcqImpl
     */
    private ContactGroupYahooImpl getFirstPersistentGroup()
    {
        for (int i = 0; i < getRootGroup().countSubgroups(); i++)
        {
            ContactGroupYahooImpl gr =
                (ContactGroupYahooImpl)getRootGroup().getGroup(i);

            if(gr.isPersistent())
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
                                   ContactYahooImpl contact)
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
    void fireContactResolved( ContactGroup parentGroup,
                                      ContactYahooImpl contact)
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
     * When the protocol is online this method is used to fill or resolve
     * the current contact list
     */
    @SuppressWarnings("unchecked") //jymsg legacy code
    private void initList()
    {
        if (logger.isTraceEnabled())
            logger.trace("Start init list of "
                        + yahooProvider.getAccountID().getUserID());

        for (YahooGroup item : yahooSession.getGroups())
        {
            ContactGroupYahooImpl group = findContactGroup(item.getName());

            if(group == null)
            {
                // create the group as it doesn't exist
                group = new ContactGroupYahooImpl(
                                    item, item.getMembers(), this, true);

                rootGroup.addSubGroup(group);

                //tell listeners about the added group
                fireGroupEvent(group,
                               ServerStoredGroupEvent.GROUP_CREATED_EVENT);
            }
            else
            {
                // the group exist so just resolved. The group will check and
                // create or resolve its entries
                group.setResolved(item);

                //fire an event saying that the group has been resolved
                fireGroupEvent(group
                               , ServerStoredGroupEvent.GROUP_RESOLVED_EVENT);

                /** @todo  if something to delete . delete it */
            }

            if (logger.isTraceEnabled())
                logger.trace("Init of group done! : " + group);
        }
    }

    /**
     * @param name Name of the group to search
     * @return The yahoo group with given name
     */
    private YahooGroup findGroup(String name)
    {
        for (YahooGroup elem : yahooSession.getGroups())
        {
            if(elem.getName().equals(name))
                return elem;
        }
        return null;
    }

    /**
     * Process incoming authorization requests.
     * @param ev the event to process.
     */
    void processAuthorizationRequest(SessionAuthorizationEvent ev)
    {
        if(handler == null)
            return;

        Contact srcContact = findContactById(ev.getFrom());

        // if there is no such contact we create it as
        // volatile so we can fire notification
        // and then if accepted add it in the protocol
        // so we can receive its states
        boolean isCurrentlyCreated = false;
        if(srcContact == null)
        {
            srcContact = createVolatileContact(ev.getFrom());
            isCurrentlyCreated = true;
        }

        AuthorizationRequest authRequest = new AuthorizationRequest();
            authRequest.setReason(ev.getMessage());

        AuthorizationResponse authResponse =
            handler.processAuthorisationRequest(
                authRequest, srcContact);

        if (authResponse.getResponseCode() == AuthorizationResponse.IGNORE)
        {
            return;
        }
        else if (authResponse.getResponseCode() == AuthorizationResponse.REJECT)
        {
            removeContact((ContactYahooImpl)srcContact);
            try
            {
                yahooSession.rejectFriendAuthorization(
                    ev, ev.getFrom(), authResponse.getReason());
            }
            catch(IOException ex)
            {
                logger.error("cannot send auth deny", ex);
            }

            return;
        }

        // else we accepted it
        try
        {
            yahooSession.acceptFriendAuthorization(ev, ev.getFrom());
        }
        catch(IOException ex)
        {
            logger.error("cannot send auth deny", ex);
        }

        if(isCurrentlyCreated)
        try
        {
            addContact(ev.getFrom());
        }
        catch (OperationFailedException ex)
        {
            logger.error("Cannot add friend", ex);
        }
    }

    /**
     * Imulates firing adding contact in group and moving contact to group.
     * When moving contact it is first adding to the new group then
     * it is removed from the old one.
     */
    private class ContactListModListenerImpl
        extends SessionAdapter
    {
        private final Hashtable<String, Object> waitMove
            = new Hashtable<String, Object>();

        public void waitForMove(String id, String oldParent)
        {
            waitMove.put(id, oldParent);
        }

        public void removeWaitForMove(String id)
        {
            waitMove.remove(id);
        }

        /**
         * Successfully added a friend
         * friend - YahooUser of friend
         * group - name of group added to
         * @param ev fired event
         */
        @Override
        public void friendAddedReceived(SessionFriendEvent ev)
        {
            if (logger.isTraceEnabled())
                logger.trace("Receive event for adding a friend : " + ev);

            ContactGroupYahooImpl group =
                findContactGroup(ev.getGroup());

            if(group == null){
                if (logger.isTraceEnabled())
                    logger.trace("Group not found!" + ev.getGroup());
                return;
            }

            String contactID = ev.getFriend().getId();
            ContactYahooImpl contactToAdd = findContactById(contactID);

            // if group is note resolved resolve it
            // this means newly created group
            if(!group.isResolved())
            {
                // if the contact is volatile me must remove it
                // as new one will be created
                if(contactToAdd != null && contactToAdd.isVolatile())
                {
                    ContactGroupYahooImpl parent
                        = (ContactGroupYahooImpl)contactToAdd
                            .getParentContactGroup();

                    parent.removeContact(contactToAdd);
                    fireContactRemoved(parent, contactToAdd);
                }

                YahooGroup gr = findGroup(ev.getGroup());

                if(gr != null)
                    group.setResolved(gr);

                // contact will be added when resolving the group

                return;
            }


            boolean isVolatile = false;

            if(contactToAdd == null)
            {
                if(addedCustomYahooIds.containsKey(contactID))
                {
                    String expectedContactID =
                         addedCustomYahooIds.remove(contactID);

                    contactToAdd =
                        new ContactYahooImpl(expectedContactID, ev.getFriend(),
                            ServerStoredContactListYahooImpl.this, true, true);
                }
                else
                {
                    contactToAdd =
                        new ContactYahooImpl(ev.getFriend(),
                            ServerStoredContactListYahooImpl.this, true, true);
                }
            }
            else
            {
                isVolatile = contactToAdd.isVolatile();
            }

            //first check is contact is moving from a group
            Object isWaitingForMove = waitMove.get(contactID);

            if(isWaitingForMove != null && isWaitingForMove instanceof String)
            {
                // waits for move into group
                // will remove it from old group and will wait for event remove
                // from group, then will fire moved to group event
                String oldParent = (String)isWaitingForMove;

                group.addContact(contactToAdd);
                waitMove.put(contactID, group.getSourceGroup());
                try
                {
                    yahooSession.removeFriend(contactID, oldParent);
                }
                catch(IOException ex)
                {
                    if (logger.isInfoEnabled())
                        logger.info("Cannot Remove(till moving) contact :" +
                        contactToAdd + " from group " + oldParent);
                }
                return;
            }

            if(isVolatile)
            {
                // we must remove the volatile buddy as we will add
                // the persistent one.
                // Volatile buddy is moving from the volatile group
                // to the new one
                ContactGroupYahooImpl parent =
                    (ContactGroupYahooImpl)contactToAdd.getParentContactGroup();

                parent.removeContact(contactToAdd);
                fireContactRemoved(parent, contactToAdd);

                contactToAdd.setPersistent(true);
                contactToAdd.setResolved(ev.getFriend());

                group.addContact(contactToAdd);

                fireContactAdded(group, contactToAdd);
                waitMove.remove(contactID);

                return;
            }

            group.addContact(contactToAdd);
            fireContactAdded(group, contactToAdd);
        }

        /**
         * Successfully removed a friend
         * friend - YahooUser of friend
         * group - name of group removed from
         * @param ev fired event
         */
        @Override
        public void friendRemovedReceived(SessionFriendEvent ev)
        {
            if (logger.isTraceEnabled())
                logger.trace("Receive event for removing a friend : " + ev);

            String contactID = ev.getFriend().getId();

            // first check is this part of move action
            Object waitForMoveObj = waitMove.get(contactID);
            if(waitForMoveObj != null && waitForMoveObj instanceof YahooGroup)
            {
                // first get the group - oldParent
                ContactGroupYahooImpl oldParent
                    = findContactGroup(ev.getGroup());
                ContactYahooImpl contactToRemove
                    = oldParent.findContact(contactID);

                oldParent.removeContact(contactToRemove);
                waitMove.remove(contactID);

                ContactGroupYahooImpl newParent =
                    findContactGroup(((YahooGroup)waitForMoveObj).getName());

                fireContactMoved(oldParent, newParent, contactToRemove);
                return;
            }

            ContactYahooImpl contactToRemove = findContactById(contactID);

            // strange we cannot find the contact to be removed
            if(contactToRemove == null)
                return;

            ContactGroupYahooImpl parentGroup =
                    (ContactGroupYahooImpl)contactToRemove.
                        getParentContactGroup();
            parentGroup.removeContact(contactToRemove);
            fireContactRemoved(parentGroup, contactToRemove);

            // check if the group is deleted. If the contact is the last one in
            // the group. The group is also deleted
            if(findGroup(ev.getGroup()) == null)
            {
                rootGroup.removeSubGroup(parentGroup);
                fireGroupEvent(parentGroup,
                               ServerStoredGroupEvent.GROUP_REMOVED_EVENT);
            }
        }

        /**
         * Someone wants to add us to their friends list
         * to - the target (us!)
         * from - the user who wants to add us
         * message - the request message text
         * @param ev fired event
         */
        @Override
        public void contactRequestReceived(SessionEvent ev)
        {
            if (logger.isInfoEnabled())
                logger.info("contactRequestReceived : " + ev);

            if(handler == null || ev.getFrom() == null)
                return;

            ContactYahooImpl contact = findContactById(ev.getFrom());

            if(contact == null)
                contact = createVolatileContact(ev.getFrom());

            AuthorizationRequest request = new AuthorizationRequest();
            request.setReason(ev.getMessage());

            AuthorizationResponse resp =
                handler.processAuthorisationRequest(request, contact);

            if (resp.getResponseCode() == AuthorizationResponse.REJECT)
            {
                try{
                    yahooSession.rejectContact(ev, resp.getReason());
                }catch(IOException ex){
                    logger.error("Cannot send reject : " + ex.getMessage());
                }
            }
        }

        /**
         * Someone has rejected our attempts to add them to our friends list
         * from - the user who rejected us
         * message - rejection message text
         * @param ev fired event
         */
        @Override
        public void contactRejectionReceived(SessionEvent ev)
        {
            if (logger.isInfoEnabled())
                logger.info("contactRejectionReceived : " + ev);

            if(handler == null)
                return;

            ContactYahooImpl contact = findContactById(ev.getFrom());

            AuthorizationResponse resp =
                new AuthorizationResponse(AuthorizationResponse.REJECT,
                                          ev.getMessage());
            handler.processAuthorizationResponse(resp, contact);
        }

        /**
         * Invoked on picture received.
         * @param ev fired event
         */
        @Override
        public void pictureReceived(SessionPictureEvent ev)
        {
            ContactYahooImpl contact = findContactById(ev.getFrom());

            if(contact == null)
                return;

            contact.setImage(ev.getPictureData());

            parentOperationSet.fireContactPropertyChangeEvent(
                                ContactPropertyChangeEvent.PROPERTY_IMAGE,
                                contact, null, ev.getPictureData());
        }

        /**
         * Process Authorization responses
         * @param ev the event to process
         */
        @Override
        public void authorizationReceived(SessionAuthorizationEvent ev)
        {
            if(ev.isAuthorizationAccepted())
            {
                if (logger.isTraceEnabled())
                    logger.trace("authorizationAccepted from " + ev.getFrom());
                Contact srcContact = findContactById(ev.getFrom());

                if(srcContact == null)
                    if (logger.isTraceEnabled())
                        logger.trace("No contact found");
                else
                    handler.processAuthorizationResponse(
                        new AuthorizationResponse(
                                AuthorizationResponse.ACCEPT,
                                ev.getMessage()),
                        srcContact);
            }
            else if(ev.isAuthorizationDenied())
            {
                if (logger.isTraceEnabled())
                    logger.trace("authorizationDenied from " + ev.getFrom());
                Contact srcContact = findContactById(ev.getFrom());

                if(srcContact == null)
                    if (logger.isTraceEnabled())
                        logger.trace("No contact found");
                else
                {
                    handler.processAuthorizationResponse(
                        new AuthorizationResponse(
                                AuthorizationResponse.REJECT,
                                ev.getMessage()),
                        srcContact);
                    try
                    {
                        removeContact((ContactYahooImpl)srcContact);
                    } catch (Exception ex)
                    {
                        logger.error("cannot remove denied contact : " +
                                srcContact, ex);
                    }
                }
            }
            else if(ev.isAuthorizationRequest())
            {
                if (logger.isTraceEnabled())
                    logger.trace("authorizationRequestReceived from "
                             + ev.getFrom());
                processAuthorizationRequest(ev);
            }
        }
    }

    /**
     * Sets the yahoo session instance of the lib
     * which comunicates with the server
     * @param session YahooSession
     */
    void setYahooSession(YahooSession session)
    {
        this.yahooSession = session;
        session.addSessionListener(contactListModListenerImpl);
        initList();
    }

    /**
     * It seems that ymsg (or the Yahoo! service itself as the problem also
     * appears with libpurple) would return illegal chars for names that were
     * entered in cyrillic. We use this method to translate their names into
     * something that we could actually display and store here.
     *
     * @param ymsgString the <tt>String</tt> containing illegal chars.
     *
     * @return a String where all illegal chars are converted into human
     * readable ones
     */
    static String replaceIllegalChars(String ymsgString)
    {
        return ymsgString.replace((char)26, '?');
    }
}
