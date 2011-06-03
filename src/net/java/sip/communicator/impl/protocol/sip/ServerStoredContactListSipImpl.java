/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import net.java.sip.communicator.impl.protocol.sip.xcap.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.presrules.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.resourcelists.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import javax.sip.address.*;
import java.text.*;
import java.util.*;

/**
 * Encapsulates XCapClient, it's responsible for generate corresponding
 * sip-communicator events to all action that are made with XCAP contacts and
 * groups.
 *
 * @author Grigorii Balutsel
 */
public class ServerStoredContactListSipImpl
{
    /**
     * Logger class
     */
    private static final Logger logger =
            Logger.getLogger(ServerStoredContactListSipImpl.class);

    /**
     * Root group name.
     */
    private final static String ROOT_GROUP_NAME = "RootGroup";

    /**
     * Default "White" rule identifier.
     */
    private final static String DEFAULT_WHITE_RULE_ID = "presence_allow";

    /**
     * Default "Block" rule identifier.
     */
    private final static String DEFAULT_BLOCK_RULE_ID = "presence_block";

    /**
     * Default "Polite Block" rule identifier.
     */
    private final static String DEFAULT_POLITE_BLOCK_RULE_ID
            = "presence_polite_block";

    /**
     * The provider that is on top of us.
     */
    private final ProtocolProviderServiceSipImpl sipProvider;

    /**
     * The operation set that created us and that we could use when dispatching
     * subscription events.
     */
    private final OperationSetPresenceSipImpl parentOperationSet;

    /**
     * Listeners that would receive event notifications for changes in group
     * names or other properties, removal or creation of groups.
     */
    private final Vector<ServerStoredGroupListener> serverStoredGroupListeners;

    /**
     * The root contact group. The container for all SIP contacts and groups.
     */
    private final ContactGroupSipImpl rootGroup;

    /**
     * Current presence rules.
     */
    private RulesetType presRules;

    /**
     * Creates a ServerStoredContactList wrapper for the specified BuddyList.
     *
     * @param sipProvider        the provider that has instantiated us.
     * @param parentOperationSet the operation set that created us and that
     *                           we could use for dispatching subscription events
     */
    ServerStoredContactListSipImpl(
            ProtocolProviderServiceSipImpl sipProvider,
            OperationSetPresenceSipImpl parentOperationSet)
    {
        this.sipProvider = sipProvider;
        this.parentOperationSet = parentOperationSet;
        this.serverStoredGroupListeners =
                new Vector<ServerStoredGroupListener>();
        this.rootGroup = new ContactGroupSipImpl(ROOT_GROUP_NAME, sipProvider);
    }

    /**
     * Returns the root group of the contact list.
     *
     * @return the root ContactGroup for the ContactList.
     */
    public ContactGroupSipImpl getRootGroup()
    {
        return rootGroup;
    }

    /**
     * Registers the specified group listener so that it would receive events
     * on group modification/creation/destruction.
     *
     * @param listener the ServerStoredGroupListener to register for group
     *                 events.
     */
    public void addGroupListener(ServerStoredGroupListener listener)
    {
        synchronized (serverStoredGroupListeners)
        {
            if (!serverStoredGroupListeners.contains(listener))
            {
                this.serverStoredGroupListeners.add(listener);
            }
        }
    }

    /**
     * Removes the specified group listener so that it won't receive further
     * events on group modification/creation/destruction.
     *
     * @param listener the ServerStoredGroupListener to unregister.
     */
    public void removeGroupListener(ServerStoredGroupListener listener)
    {
        synchronized (serverStoredGroupListeners)
        {
            this.serverStoredGroupListeners.remove(listener);
        }
    }

    /**
     * Creates the corresponding event and notifies all
     * <tt>ServerStoredGroupListener</tt>s that the source group has been
     * removed, changed, renamed or whatever happened to it.
     *
     * @param group   the ContactGroup that has been created/modified/removed.
     * @param eventID the id of the event to generate.
     */
    void fireGroupEvent(ContactGroup group, int eventID)
    {
        ServerStoredGroupEvent event = new ServerStoredGroupEvent(
                group,
                eventID,
                parentOperationSet.getServerStoredContactListRoot(),
                sipProvider,
                parentOperationSet);
        if (logger.isTraceEnabled())
        {
            logger.trace("Will dispatch the following group event: " + event);
        }
        Iterable<ServerStoredGroupListener> listeners;
        synchronized (serverStoredGroupListeners)
        {
            listeners =
                    new ArrayList<ServerStoredGroupListener>(
                            serverStoredGroupListeners);
        }
        for (ServerStoredGroupListener listener : listeners)
        {
            if (eventID == ServerStoredGroupEvent.GROUP_REMOVED_EVENT)
            {
                listener.groupRemoved(event);
            }
            else if (eventID == ServerStoredGroupEvent.GROUP_RENAMED_EVENT)
            {
                listener.groupNameChanged(event);
            }
            else if (eventID == ServerStoredGroupEvent.GROUP_CREATED_EVENT)
            {
                listener.groupCreated(event);
            }
            else if (eventID == ServerStoredGroupEvent.GROUP_RESOLVED_EVENT)
            {
                listener.groupResolved(event);
            }
        }
    }

    /**
     * Creates a non resolved contact for the specified address and inside the
     * specified group. The newly created contact would be added to the local
     * contact list as a standard contact but when an event is received from the
     * server concerning this contact, then it will be reused and only its
     * isResolved field would be updated instead of creating the whole contact
     * again. If creation is successfull event will be fired.
     *
     * @param parentGroup the group where the unersolved contact is to be
     *                    created.
     * @param contactId   the sip id of the contact to create.
     * @param persistentData a String returned Contact's getPersistentData()
     * method during a previous run and that has been persistently stored
     * locally.
     * @return the newly created unresolved <tt>ContactSipImpl</tt>.
     */
    public synchronized ContactSipImpl createUnresolvedContact(
            ContactGroupSipImpl parentGroup, String contactId,
            String persistentData)
    {
        if (parentGroup == null)
        {
            throw new IllegalArgumentException("Parent group cannot be null");
        }
        if (contactId == null || contactId.length() == 0)
        {
            throw new IllegalArgumentException(
                    "Creating contact id name cannot be null or empty");
        }
        Address contactAddress;
        try
        {
            contactAddress = sipProvider.parseAddressString(contactId);
        }
        catch (ParseException ex)
        {
            throw new IllegalArgumentException(
                    String.format("%1s is no a valid SIP identifier",
                            contactId),
                    ex);
        }

        if(logger.isTraceEnabled())
            logger.trace("createUnresolvedContact " + contactId);

        ContactSipImpl newUnresolvedContact = new ContactSipImpl(contactAddress,
                sipProvider);
        parentGroup.addContact(newUnresolvedContact);
        newUnresolvedContact.setPersistentData(persistentData);
        fireContactAdded(parentGroup, newUnresolvedContact);
        return newUnresolvedContact;
    }

    /**
     * Creates contact for the specified address and inside the
     * specified group . If creation is successfull event will be fired.
     *
     * @param parentGroup the group where the unersolved contact is to be
     *                    created.
     * @param contactId   the sip id of the contact to create.
     * @param persistent  specify whether created contact is persistent ot not.
     * @return the newly created <tt>ContactSipImpl</tt>.
     * @throws OperationFailedException with code NETWORK_FAILURE if the
     *                                  operation if failed during network
     *                                  communication.
     */
    synchronized public ContactSipImpl createContact(
            ContactGroupSipImpl parentGroup, String contactId,
            boolean persistent)
            throws OperationFailedException
    {
        if (parentGroup == null)
        {
            throw new IllegalArgumentException("Parent group cannot be null");
        }
        if (contactId == null || contactId.trim().length() == 0)
        {
            throw new IllegalArgumentException(
                    "Contact identifier cannot be null or empty");
        }
        if (logger.isTraceEnabled())
        {
            logger.trace(
                    String.format("createContact %1s, %2s, %3s",
                            parentGroup.getGroupName(), contactId, persistent));
        }
        if (parentGroup.getContact(contactId) != null)
        {
            throw new OperationFailedException(
                    "Contact " + contactId + " already exists.",
                    OperationFailedException.SUBSCRIPTION_ALREADY_EXISTS);
        }
        Address contactAddress;
        try
        {
            contactAddress = sipProvider.parseAddressString(contactId);
        }
        catch (ParseException ex)
        {
            throw new IllegalArgumentException(contactId +
                    " is not a valid string.", ex);
        }

        ContactSipImpl newContact = parentOperationSet.resolveContactID(
                contactAddress.getURI().toString());

        if(newContact != null && !newContact.isPersistent() &&
                !newContact.getParentContactGroup().isPersistent())
        {
            // this is a contact from not in contact list group
            // we must remove it
            ContactGroupSipImpl oldParentGroup =
                    (ContactGroupSipImpl)newContact.getParentContactGroup();
            oldParentGroup.removeContact(newContact);
            fireContactRemoved(oldParentGroup, newContact);
        }

        newContact = new ContactSipImpl(contactAddress,
                sipProvider);
        newContact.setPersistent(persistent);
        String name = ((SipURI) contactAddress.getURI()).getUser();
        newContact.setDisplayName(name);
        parentGroup.addContact(newContact);
        if (newContact.isPersistent())
        {
            // Update resoure-lists
            try
            {
                updateResourceLists();
            }
            catch (XCapException e)
            {
                parentGroup.removeContact(newContact);
                throw new OperationFailedException(
                        "Error while creating XCAP contact",
                        OperationFailedException.NETWORK_FAILURE, e);
            }
            newContact.setResolved(true);
            XCapClient xCapClient = sipProvider.getXCapClient();
            if (xCapClient.isConnected() &&
                    xCapClient.isResourceListsSupported())
            {
                newContact.setXCapResolved(true);

                try
                {
                    // Update pres-rules if needed
                    if (!isContactInWhiteRule(contactId))
                    {
                        // Update pres-rules
                        if(addContactToWhiteList(newContact))
                            updatePresRules();
                    }
                }
                catch (XCapException e)
                {
                    logger.error("Cannot add contact to white list while " +
                            "creating it", e);
                }
            }
        }
        fireContactAdded(parentGroup, newContact);
        return newContact;
    }

    /**
     * Removes a contact. If creation is successfull event will be fired.
     *
     * @param contact contact to be removed.
     * @throws OperationFailedException with code NETWORK_FAILURE if the
     *                                  operation if failed during network
     *                                  communication.
     */
    synchronized public void removeContact(ContactSipImpl contact)
            throws OperationFailedException
    {
        if (contact == null)
        {
            throw new IllegalArgumentException(
                    "Removing contact cannot be null");
        }

        if(logger.isTraceEnabled())
            logger.trace("removeContact " + contact.getUri());

        ContactGroupSipImpl parentGroup =
                (ContactGroupSipImpl) contact.getParentContactGroup();
        parentGroup.removeContact(contact);
        if (contact.isPersistent())
        {
            try
            {
                // when removing contact add it to polite block list, cause
                // as soon as we remove it we will receive notification
                // for authorization (watcher info - pending)
                boolean updateRules = removeContactFromWhiteList(contact);
                updateRules = removeContactFromBlockList(contact)
                        || updateRules;
                updateRules = removeContactFromPoliteBlockList(contact)
                        || updateRules;

                if(updateRules)
                    updatePresRules();
            }
            catch (XCapException e)
            {
                logger.error("Error while removing XCAP contact", e);
            }

            // Update resoure-lists
            try
            {
                updateResourceLists();
            }
            catch (XCapException e)
            {
                parentGroup.removeContact(contact);
                throw new OperationFailedException(
                        "Error while removing XCAP contact",
                        OperationFailedException.NETWORK_FAILURE, e);
            }
        }
        fireContactRemoved(parentGroup, contact);
    }

    /**
     * Removes the specified contact from its current parent and places it
     * under <tt>newParent</tt>.
     *
     * @param contact        the <tt>Contact</tt> to move
     * @param newParentGroup the <tt>ContactGroup</tt> where <tt>Contact</tt>
     *                       would be placed.
     * @throws OperationFailedException with code NETWORK_FAILURE if the
     *                                  operation if failed during network
     *                                  communication.
     */
    public void moveContactToGroup(
            ContactSipImpl contact,
            ContactGroupSipImpl newParentGroup)
            throws OperationFailedException
    {
        if (contact == null)
        {
            throw new IllegalArgumentException(
                    "Moving contact cannot be null");
        }
        if (newParentGroup == null)
        {
            throw new IllegalArgumentException(
                    "New contact's parent group  be null");
        }
        if (newParentGroup.getContact(contact.getUri()) != null)
        {
            throw new OperationFailedException(
                    "Contact " + contact.getUri() + " already exists.",
                    OperationFailedException.SUBSCRIPTION_ALREADY_EXISTS);
        }
        ContactGroupSipImpl oldParentGroup =
                (ContactGroupSipImpl) contact.getParentContactGroup();
        oldParentGroup.removeContact(contact);

        boolean wasContactPersistent = contact.isPersistent();

        // if contact is not persistent we make it persistent if
        // new parent is persistent
        if(newParentGroup.isPersistent())
            contact.setPersistent(true);

        newParentGroup.addContact(contact);

        if (contact.isPersistent())
        {
            try
            {
                updateResourceLists();
            }
            catch (XCapException e)
            {
                newParentGroup.removeContact(contact);
                oldParentGroup.addContact(contact);
                throw new OperationFailedException(
                        "Error while moving XCAP contact",
                        OperationFailedException.NETWORK_FAILURE, e);
            }

            if(!wasContactPersistent)
            {
                contact.setResolved(true);
                XCapClient xCapClient = sipProvider.getXCapClient();
                if (xCapClient.isConnected() &&
                        xCapClient.isResourceListsSupported())
                {
                    contact.setXCapResolved(true);

                    try
                    {
                        // Update pres-rules if needed
                        if (!isContactInWhiteRule(contact.getAddress()))
                        {
                            // Update pres-rules
                            if(addContactToWhiteList(contact))
                                updatePresRules();
                        }
                    }
                    catch (XCapException e)
                    {
                        logger.error("Cannot add contact to white list while " +
                                "creating it", e);
                    }
                }
            }
        }
        fireContactMoved(oldParentGroup, newParentGroup, contact);
    }

    /**
     * Renames the specified contac.
     *
     * @param contact the contact to be renameed.
     * @param newName the new contact name.
     * @throws OperationFailedException with code NETWORK_FAILURE if the
     *                                  operation if failed during network
     *                                  communication.
     */
    synchronized public void renameContact(
            ContactSipImpl contact,
            String newName)
    {
        if (contact == null)
        {
            throw new IllegalArgumentException(
                    "Renaming contact cannot be null");
        }
        String oldName = contact.getDisplayName();
        if (oldName.equals(newName))
        {
            return;
        }
        contact.setDisplayName(newName);
        if (contact.isPersistent())
        {
            try
            {
                updateResourceLists();
            }
            catch (XCapException e)
            {
                contact.setDisplayName(oldName);
                throw new IllegalStateException(
                        "Error while renaming XCAP group", e);
            }
        }
        parentOperationSet.fireContactPropertyChangeEvent(
                ContactPropertyChangeEvent.PROPERTY_DISPLAY_NAME,
                contact,
                oldName,
                newName);
    }


    /**
     * Creates a non resolved contact group for the specified name. The newly
     * created group would be added to the local contact list as any other group
     * but when an event is received from the server concerning this group, then
     * it will be reused and only its isResolved field would be updated instead
     * of creating the whole group again.
     * <p/>
     *
     * @param parentGroup the group under which the new group is to be created.
     * @param groupName   the name of the group to create.
     * @return the newly created unresolved <tt>ContactGroupSipImpl</tt>.
     */
    synchronized public ContactGroupSipImpl createUnresolvedContactGroup(
            ContactGroupSipImpl parentGroup,
            String groupName)
    {
        if (parentGroup == null)
        {
            throw new IllegalArgumentException("Parent group cannot be null");
        }
        if (groupName == null || groupName.length() == 0)
        {
            throw new IllegalArgumentException(
                    "Creating group name cannot be null or empry");
        }
        if (logger.isTraceEnabled())
        {
            logger.trace("createUnresolvedContactGroup " + groupName);
        }
        ContactGroupSipImpl subGroup = new ContactGroupSipImpl(groupName,
                sipProvider);
        subGroup.setResolved(false);
        parentGroup.addSubgroup(subGroup);
        fireGroupEvent(subGroup, ServerStoredGroupEvent.GROUP_CREATED_EVENT);
        return subGroup;
    }

    /**
     * Creates a group with the specified name and parent in the server stored
     * contact list.
     *
     * @param parentGroup the group where the new group should be created.
     * @param groupName   the name of the new group to create.
     * @param persistent  specify whether created contact is persistent ot not.
     * @return the newly created <tt>ContactGroupSipImpl</tt>.
     * @throws OperationFailedException with code NETWORK_FAILURE if creating
     *                                  the group fails because of XCAP server
     *                                  error or with code
     *                                  CONTACT_GROUP_ALREADY_EXISTS if contact
     *                                  group with such name already exists.
     */
    synchronized public ContactGroupSipImpl createGroup(
            ContactGroupSipImpl parentGroup, String groupName,
            boolean persistent)
            throws OperationFailedException
    {
        if (parentGroup == null)
        {
            throw new IllegalArgumentException("Parent group cannot be null");
        }
        if (groupName == null || groupName.length() == 0)
        {
            throw new IllegalArgumentException(
                    "Creating group name cannot be null or empry");
        }
        if (logger.isTraceEnabled())
        {
            logger.trace("createGroup " + parentGroup.getGroupName() + ","
                    + groupName + "," + persistent);
        }
        if (parentGroup.getGroup(groupName) != null)
        {
            throw new OperationFailedException(
                    String.format("Group %1s already exists.", groupName),
                    OperationFailedException.CONTACT_GROUP_ALREADY_EXISTS);
        }
        ContactGroupSipImpl subGroup =
                new ContactGroupSipImpl(groupName, sipProvider);
        subGroup.setPersistent(persistent);
        parentGroup.addSubgroup(subGroup);
        if (subGroup.isPersistent())
        {
            try
            {
                updateResourceLists();
            }
            catch (XCapException e)
            {
                parentGroup.removeSubGroup(subGroup);
                throw new OperationFailedException(
                        "Error while creating XCAP group",
                        OperationFailedException.NETWORK_FAILURE, e);
            }
            subGroup.setResolved(true);
        }
        fireGroupEvent(subGroup, ServerStoredGroupEvent.GROUP_CREATED_EVENT);
        return subGroup;
    }

    /**
     * Removes the specified group from the server stored contact list.
     *
     * @param group the group to delete.
     */
    synchronized public void removeGroup(ContactGroupSipImpl group)
    {
        if (group == null)
        {
            throw new IllegalArgumentException("Removing group cannot be null");
        }
        if (rootGroup.equals(group))
        {
            throw new IllegalArgumentException("Root group cannot be deleted");
        }
        if (logger.isTraceEnabled())
        {
            logger.trace("removeGroup " + group.getGroupName());
        }
        ContactGroupSipImpl parentGroup =
                (ContactGroupSipImpl) group.getParentContactGroup();
        parentGroup.removeSubGroup(group);
        if (group.isPersistent())
        {
            try
            {
                updateResourceLists();

                Iterator<Contact>  iter = group.contacts();
                boolean updateRules = false;
                while(iter.hasNext())
                {
                    ContactSipImpl c = (ContactSipImpl)iter.next();
                    updateRules = removeContactFromWhiteList(c) || updateRules;
                    updateRules = removeContactFromBlockList(c) || updateRules;
                    updateRules = removeContactFromPoliteBlockList(c)
                            || updateRules;
                }
                if(updateRules)
                    updatePresRules();
            }
            catch (XCapException e)
            {
                parentGroup.addSubgroup(group);
                throw new IllegalStateException(
                        "Error while removing XCAP group", e);
            }
        }
        fireGroupEvent(group, ServerStoredGroupEvent.GROUP_REMOVED_EVENT);
    }

    /**
     * Renames the specified group from the server stored contact list.
     *
     * @param group   the group to rename.
     * @param newName the new name of the group.
     */
    synchronized public void renameGroup(
            ContactGroupSipImpl group,
            String newName)
    {
        if (group == null)
        {
            throw new IllegalArgumentException("Renaming group cannot be null");
        }
        if (rootGroup.equals(group))
        {
            throw new IllegalArgumentException("Root group cannot be renamed");
        }
        String oldName = group.getGroupName();
        if (oldName.equals(newName))
        {
            return;
        }
        ContactGroupSipImpl parentGroup =
                (ContactGroupSipImpl) group.getParentContactGroup();
        if (parentGroup.getGroup(newName) != null)
        {
            throw new IllegalStateException(
                    String.format("Group with name %1s already exists",
                            newName));
        }
        group.setName(newName);
        if (group.isPersistent())
        {
            try
            {
                updateResourceLists();
            }
            catch (XCapException e)
            {
                group.setName(oldName);
                throw new IllegalStateException(
                        "Error while renaming XCAP group", e);
            }
        }
        fireGroupEvent(group, ServerStoredGroupEvent.GROUP_RENAMED_EVENT);
    }

    /**
     * Make the parent persistent presence operation set dispatch a contact
     * added event.
     *
     * @param parentGroup the group where the new contact was added.
     * @param contact     the contact that was added.
     */
    private void fireContactAdded(
            ContactGroupSipImpl parentGroup,
            ContactSipImpl contact)
    {
        parentOperationSet.fireSubscriptionEvent(
                contact,
                parentGroup,
                SubscriptionEvent.SUBSCRIPTION_CREATED);
    }

    /**
     * Make the parent persistent presence operation set dispatch a subscription
     * moved event.
     *
     * @param oldParentGroup the group where the source contact was located
     *                       before being moved.
     * @param newParentGroup the group that the source contact is currently in.
     * @param contact        the contact that was added.
     */
    private void fireContactMoved(
            ContactGroupSipImpl oldParentGroup,
            ContactGroupSipImpl newParentGroup,
            ContactSipImpl contact)
    {
        parentOperationSet.fireSubscriptionMovedEvent(
                contact,
                oldParentGroup,
                newParentGroup);
    }

    /**
     * Make the parent persistent presence operation set dispatch a contact
     * removed event.
     *
     * @param parentGroup the group where that the removed contact belonged to.
     * @param contact     the contact that was removed.
     */
    private void fireContactRemoved(
            ContactGroupSipImpl parentGroup,
            ContactSipImpl contact)
    {
        parentOperationSet.fireSubscriptionEvent(
                contact,
                parentGroup,
                SubscriptionEvent.SUBSCRIPTION_REMOVED);
    }

    /**
     * Make the parent persistent presence operation set dispatch a contact
     * resolved event.
     *
     * @param parentGroup the group that the resolved contact belongs to.
     * @param contact     the contact that was resolved.
     */
    private void fireContactResolved(
            ContactGroupSipImpl parentGroup,
            ContactSipImpl contact)
    {
        parentOperationSet.fireSubscriptionEvent(
                contact,
                parentGroup,
                SubscriptionEvent.SUBSCRIPTION_RESOLVED);
    }

    /**
     * Initializes the server stored list. Synchronize server stored groups and
     * contacts with the local groups and contacts.
     */
    synchronized public void init()
    {
        try
        {
            XCapClient xCapClient = sipProvider.getXCapClient();
            if (!xCapClient.isConnected() ||
                    !xCapClient.isResourceListsSupported())
            {
                return;
            }
            // Process resource-lists
            ResourceListsType resourceLists = xCapClient.getResourceLists();
            // Collect all root's subgroups to check if some of them were deleted
            ListType serverRootList = new ListType();
            for (ListType list : resourceLists.getList())
            {
                // If root group has sub group with ROOT_GROUP_NAME - it is
                // special group for storing contacts that is not allowed by RFC
                if (list.getName().equals(ROOT_GROUP_NAME))
                {
                    serverRootList.setName(ROOT_GROUP_NAME);
                    serverRootList.setDisplayName(list.getDisplayName());
                    serverRootList.getEntries().addAll(list.getEntries());
                    serverRootList.getEntryRefs().addAll(list.getEntryRefs());
                    serverRootList.getExternals().addAll(list.getExternals());
                    serverRootList.setAny(list.getAny());
                    serverRootList
                            .setAnyAttributes(list.getAnyAttributes());
                }
                else
                {
                    serverRootList.getLists().add(list);
                }
            }
            boolean updateResourceLists = false;
            // Resolve localy saved contacts and groups with server stored
            // contacts and groups
            resolveContactGroup(rootGroup, serverRootList, false);
            // Upload unresolved contacts and groups to the server.
            for (ContactSipImpl contact : getAllContacts(rootGroup))
            {
                if (!contact.isResolved() && contact.isPersistent())
                {
                    contact.setResolved(true);
                    ContactGroupSipImpl parentGroup = ((ContactGroupSipImpl)
                            contact.getParentContactGroup());
                    // If contact is xcap.resolved and is not on the server we
                    // delete it
                    if (contact.isXCapResolved())
                    {
                        parentGroup.removeContact(contact);
                        fireContactRemoved(parentGroup, contact);
                    }
                    // If contact is added localy we upload it
                    else
                    {
                        updateResourceLists = true;
                        String oldValue = contact.getPersistentData();
                        contact.setXCapResolved(true);
                        fireContactResolved(parentGroup, contact);

                        // fire that property is changed in order
                        // to save change, event resolved doesn't save it
                        parentOperationSet.fireContactPropertyChangeEvent(
                            ContactPropertyChangeEvent.PROPERTY_PERSISTENT_DATA,
                            contact,
                            oldValue,
                            contact.getPersistentData()
                        );
                    }
                }
            }
            for (ContactGroupSipImpl group : getAllGroups(rootGroup))
            {
                if (!group.isResolved() && group.isPersistent())
                {
                    updateResourceLists = true;
                    group.setResolved(true);
                    fireGroupEvent(group,
                            ServerStoredGroupEvent.GROUP_RESOLVED_EVENT);
                }
            }
            // Update resource-lists if needed
            if(updateResourceLists)
            {
                updateResourceLists();
            }
            // Process pres-rules
            if (xCapClient.isPresRulesSupported())
            {
                // Get white pres-rules and analyze it
                RuleType whiteRule = getRule(SubHandlingType.Allow);

                boolean updateRules = false;

                // If "white" rule is available refresh it
                if (whiteRule == null)
                {
                    whiteRule = createWhiteRule();
                    presRules.getRules().add(whiteRule);
                }

                // Add contacts into the "white" rule if missing
                List<ContactSipImpl> uniqueContacts =
                        getUniqueContacts(rootGroup);
                for (ContactSipImpl contact : uniqueContacts)
                {
                    if(contact.isPersistent()
                        && !isContactInRule(whiteRule, contact.getUri()))
                    {
                        addContactToRule(whiteRule, contact);
                        updateRules = true;
                    }
                }

                if(updateRules)
                    updatePresRules();
            }
        }
        catch (XCapException e)
        {
            logger.error("Error initializing serverside list!", e);

            // if for some reason we cannot init the contact list
            // disconnect xcap client
            sipProvider.getXCapClient().disconnect();
        }
    }

    /**
     * Destroys the server stored list.
     */
    synchronized public void destroy()
    {
        List<ContactSipImpl> contacts = getAllContacts(rootGroup);
        for (ContactSipImpl contact : contacts)
        {
            contact.setResolved(false);
        }
        presRules = null;
    }

    /**
     * Creates "white" rule with full permissions.
     *
     * @return created rule.
     */
    private static RuleType createWhiteRule()
    {
        RuleType whiteList = new RuleType();
        whiteList.setId(DEFAULT_WHITE_RULE_ID);

        ConditionsType conditions = new ConditionsType();
        whiteList.setConditions(conditions);

        ActionsType actions = new ActionsType();
        actions.setSubHandling(SubHandlingType.Allow);
        whiteList.setActions(actions);

        TransformationsType transformations = new TransformationsType();
        ProvideServicePermissionType servicePermission =
                new ProvideServicePermissionType();
        servicePermission.setAllServices(
                new ProvideServicePermissionType.AllServicesType());
        transformations.setServicePermission(servicePermission);
        ProvidePersonPermissionType personPermission =
                new ProvidePersonPermissionType();
        personPermission.setAllPersons(
                new ProvidePersonPermissionType.AllPersonsType());
        transformations.setPersonPermission(personPermission);
        ProvideDevicePermissionType devicePermission =
                new ProvideDevicePermissionType();
        devicePermission.setAllDevices(
                new ProvideDevicePermissionType.AllDevicesType());
        transformations.setDevicePermission(devicePermission);
        whiteList.setTransformations(transformations);

        return whiteList;
    }

    /**
     * Creates "block" rule.
     *
     * @return created rule.
     */
    private static RuleType createBlockRule()
    {
        RuleType blackList = new RuleType();
        blackList.setId(DEFAULT_BLOCK_RULE_ID);

        ConditionsType conditions = new ConditionsType();
        blackList.setConditions(conditions);

        ActionsType actions = new ActionsType();
        actions.setSubHandling(SubHandlingType.Block);
        blackList.setActions(actions);

        TransformationsType transformations = new TransformationsType();
        blackList.setTransformations(transformations);

        return blackList;
    }

    /**
     * Creates "polite block" rule.
     *
     * @return created rule.
     */
    private static RuleType createPoliteBlockRule()
    {
        RuleType blackList = new RuleType();
        blackList.setId(DEFAULT_POLITE_BLOCK_RULE_ID);

        ConditionsType conditions = new ConditionsType();
        blackList.setConditions(conditions);

        ActionsType actions = new ActionsType();
        actions.setSubHandling(SubHandlingType.PoliteBlock);
        blackList.setActions(actions);

        TransformationsType transformations = new TransformationsType();
        blackList.setTransformations(transformations);

        return blackList;
    }

    /**
     * Finds the rule with the given action type.
     * @param type the action type to search for.
     * @return the rule if any or null.
     */
    private RuleType getRule(SubHandlingType type)
        throws XCapException
    {
        if(presRules == null)
        {
            XCapClient xCapClient = sipProvider.getXCapClient();
            if (!xCapClient.isConnected() ||
                    !xCapClient.isResourceListsSupported())
            {
                return null;
            }

            presRules = xCapClient.getPresRules();
        }

        for (RuleType rule : presRules.getRules())
        {
            if (rule.getActions().getSubHandling().equals(type))
            {
                return rule;
            }
        }

        return null;
    }

    /**
     * Checks whether the contact in the specified rule.
     *
     * @param rule the rule.
     * @param contactUri the contact uri to check.
     * @return is the contact in the rule.
     */
    private static boolean isContactInRule(RuleType rule, String contactUri)
    {
        IdentityType identity;
        if (rule.getConditions().getIdentities().size() == 0)
        {
            return false;
        }
        identity = rule.getConditions().getIdentities().get(0);
        for (OneType one : identity.getOneList())
        {
            if (one.getId().equals(contactUri))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds contact to the rule.
     *
     * @param contact the contact to add.
     * @param rule the rule to use to add contact to.
     * @return true if present rules were updated, false otherwise.
     */
    private static boolean addContactToRule(RuleType rule, ContactSipImpl contact)
    {
        if(isContactInRule(rule, contact.getUri()))
            return false;

        IdentityType identity;
        if (rule.getConditions().getIdentities().size() == 0)
        {
            identity = new IdentityType();
            rule.getConditions().getIdentities().add(identity);
        }
        else
        {
            identity = rule.getConditions().getIdentities().get(0);
        }
        OneType one = new OneType();
        one.setId(contact.getUri());
        identity.getOneList().add(one);

        return true;
    }

    /**
     * Removes contact from the rule.
     *
     * @param contact the contact to remove.
     * @param rule the rule to use to remove contact from.
     * @return true if present rules were updated, false otherwise.
     */
    private static boolean removeContactFromRule(
            RuleType rule, ContactSipImpl contact)
    {
        if(rule.getConditions().getIdentities().size() == 0)
            return false;

        IdentityType identity =
                rule.getConditions().getIdentities().get(0);
        OneType contactOne = null;
        for (OneType one : identity.getOneList())
        {
            if (contact.getUri().equals(one.getId()))
            {
                contactOne = one;
                break;
            }
        }

        if (contactOne != null)
        {
            identity.getOneList().remove(contactOne);
        }
        if (identity.getOneList().size() == 0)
        {
            rule.getConditions().getIdentities().remove(identity);
            rule.getConditions().getIdentities().remove(identity);
        }

        return true;
    }

    /**
     * Adds contact to the "white" rule.
     *
     * @param contact the contact to add.
     * @return true if present rules were updated, false otherwise.
     */
    boolean addContactToWhiteList(ContactSipImpl contact)
        throws XCapException
    {
        RuleType whiteRule = getRule(SubHandlingType.Allow);
        RuleType blockRule = getRule(SubHandlingType.Block);
        RuleType politeBlockRule = getRule(SubHandlingType.PoliteBlock);

        if(whiteRule == null)
        {
            whiteRule = createWhiteRule();
            presRules.getRules().add(whiteRule);
        }

        boolean updateRule =
            addContactToRule(whiteRule, contact);

        if(blockRule != null)
            updateRule = removeContactFromRule(blockRule, contact)
                    || updateRule;

        if(politeBlockRule != null)
            updateRule = removeContactFromRule(politeBlockRule, contact)
                    || updateRule;

        return updateRule;
    }

    /**
     * Adds contact to the "block" rule.
     *
     * @param contact the contact to add.
     */
    boolean addContactToBlockList(ContactSipImpl contact)
        throws XCapException
    {
        RuleType whiteRule = getRule(SubHandlingType.Allow);
        RuleType blockRule = getRule(SubHandlingType.Block);
        RuleType politeBlockRule = getRule(SubHandlingType.PoliteBlock);

        if(blockRule == null)
        {
            blockRule = createBlockRule();
            presRules.getRules().add(blockRule);
        }

        boolean updateRule =
            addContactToRule(blockRule, contact);
        if(whiteRule != null)
            updateRule = removeContactFromRule(whiteRule, contact)
                    || updateRule;
        if(politeBlockRule != null)
            updateRule = removeContactFromRule(politeBlockRule, contact)
                    || updateRule;

        return updateRule;
    }

     /**
     * Adds contact to the "polite block" rule.
     *
     * @param contact the contact to add.
     */
    boolean addContactToPoliteBlockList(ContactSipImpl contact)
        throws XCapException
    {
        RuleType whiteRule = getRule(SubHandlingType.Allow);
        RuleType blockRule = getRule(SubHandlingType.Block);
        RuleType politeBlockRule = getRule(SubHandlingType.PoliteBlock);

        if(politeBlockRule == null)
        {
            politeBlockRule = createPoliteBlockRule();
            presRules.getRules().add(politeBlockRule);
        }

        boolean updateRule =
            addContactToRule(politeBlockRule, contact);
        if(whiteRule != null)
            updateRule = removeContactFromRule(whiteRule, contact)
                    || updateRule;
        if(blockRule != null)
            updateRule = removeContactFromRule(blockRule, contact)
                    || updateRule;

        return updateRule;
    }

    /**
     * Indicates whether or not contact is exists in the "white" rule.
     *
     * @param contactUri the contact uri.
     * @return true if contact is exists, false if not.
     */
    private boolean isContactInWhiteRule(String contactUri)
        throws XCapException
    {
        RuleType whiteRule = getRule(SubHandlingType.Allow);

        if(whiteRule == null)
            return false;

        return isContactInRule(whiteRule, contactUri);
    }

    /**
     * Removes contact from the "white" rule.
     *
     * @param contact the contact to remove.
     * @return true if present rules were updated, false otherwise.
     */
    boolean removeContactFromWhiteList(ContactSipImpl contact)
        throws XCapException
    {
        RuleType whiteRule = getRule(SubHandlingType.Allow);

        if(whiteRule != null)
            return removeContactFromRule(whiteRule, contact);
        else
            return false;
    }

    /**
     * Removes contact from the "block" rule.
     *
     * @param contact the contact to remove.
     * @return true if present rules were updated, false otherwise.
     */
    boolean removeContactFromBlockList(ContactSipImpl contact)
        throws XCapException
    {
        RuleType blockRule = getRule(SubHandlingType.Block);

        if(blockRule != null)
            return removeContactFromRule(blockRule, contact);
        else
            return false;
    }

    /**
     * Removes contact from the "polite block" rule.
     *
     * @param contact the contact to remove.
     * @return true if present rules were updated, false otherwise.
     */
    boolean removeContactFromPoliteBlockList(ContactSipImpl contact)
        throws XCapException
    {
        RuleType blockRule = getRule(SubHandlingType.PoliteBlock);

        if(blockRule != null)
            return removeContactFromRule(blockRule, contact);
        else
            return false;
    }

    /**
     * Returns all avaliable contacts from group and all subgroups.
     *
     * @param group the parent of the contacts.
     * @return the list of availcable contacts.
     */
    public synchronized List<ContactSipImpl> getAllContacts(
            ContactGroupSipImpl group)
    {
        List<ContactSipImpl> contacts = new ArrayList<ContactSipImpl>();
        Iterator<ContactGroup> groupIterator = group.subgroups();
        while (groupIterator.hasNext())
        {
            contacts.addAll(
                    getAllContacts((ContactGroupSipImpl) groupIterator.next()));
        }
        Iterator<Contact> contactIterator = group.contacts();
        while (contactIterator.hasNext())
        {
            ContactSipImpl contact = (ContactSipImpl) contactIterator.next();
            contacts.add(contact);
        }
        return contacts;
    }

    /**
     * Returns all avaliable groups from group and all subgroups.
     *
     * @param group the parent of the contacts.
     * @return the list of availcable groups.
     */
    public synchronized List<ContactGroupSipImpl> getAllGroups(
            ContactGroupSipImpl group)
    {
        List<ContactGroupSipImpl> groups = new ArrayList<ContactGroupSipImpl>();
        Iterator<ContactGroup> groupIterator = group.subgroups();
        while (groupIterator.hasNext())
        {
            groups.addAll(
                    getAllGroups((ContactGroupSipImpl) groupIterator.next()));
        }
        return groups;
    }

    /**
     * Gets all unique contacts from group and all subgroups.
     *
     * @param group the parent of the contacts.
     * @return List of available contacts
     */
    public synchronized List<ContactSipImpl> getUniqueContacts(
            ContactGroupSipImpl group)
    {
        Map<String, ContactSipImpl> uniqueContacts =
                new HashMap<String, ContactSipImpl>();
        List<ContactSipImpl> contacts = getAllContacts(group);
        for (ContactSipImpl contact : contacts)
        {
            uniqueContacts.put(contact.getUri(), contact);
        }
        return new ArrayList<ContactSipImpl>(uniqueContacts.values());
    }

    /**
     * Indicates whether or not contact is exists.
     *
     * @param contactUri the contact uri.
     * @return true if contact is exists, false if not.
     */
    private boolean isContactExists(String contactUri)
    {
        for (ContactSipImpl uniqueContact : getUniqueContacts(rootGroup))
        {
            if (uniqueContact.getUri().equals(contactUri))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all contacts with the specified uri.
     *
     * @param contactUri the contact uri.
     * @return the list of the contacts.
     */
    private List<ContactSipImpl> getContacts(String contactUri)
    {
        List<ContactSipImpl> result = new ArrayList<ContactSipImpl>();
        for (ContactSipImpl contact : getAllContacts(rootGroup))
        {
            if (contact.getUri().equals(contactUri))
            {
                result.add(contact);
            }
        }
        return result;
    }

    /**
     * Indicates whether or not contact is exists.
     *
     * @param contactUri contactUri the contact uri.
     * @return true if at least one contact is persistent, false if not.
     */
    private boolean isContactPersistent(String contactUri)
    {
        for (ContactSipImpl contact : getContacts(contactUri))
        {
            if (contact.isPersistent())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves local group with server stored group.
     * <p/>
     * If local group exsists GROUP_CREATED_RESOLVED will be fired.
     * <p/>
     * If local group doesn't exsist GROUP_CREATED_EVENT will be fired.
     * <p/>
     * If server group doesn't represented GROUP_REMOVED_EVENT will be fired.
     *
     * @param clientGroup the local group.
     * @param serverGroup the server stored group.
     * @param deleteUnresolved indicates whether to delete unresolved contacts
     *                         and group. If true they will be removed otherwise
     *                         they will be skiped.
     */
    private void resolveContactGroup(
            ContactGroupSipImpl clientGroup,
            ListType serverGroup,
            boolean deleteUnresolved)
    {
        // Gather client information
        List<ContactGroupSipImpl> unresolvedGroups =
                new ArrayList<ContactGroupSipImpl>();
        Iterator<ContactGroup> groupIterator = clientGroup.subgroups();
        while (groupIterator.hasNext())
        {
            ContactGroupSipImpl group =
                    (ContactGroupSipImpl) groupIterator.next();
            unresolvedGroups.add(group);
        }
        List<ContactSipImpl> unresolvedContacts =
                new ArrayList<ContactSipImpl>();
        Iterator<Contact> contactIterator = clientGroup.contacts();
        while (contactIterator.hasNext())
        {
            ContactSipImpl contact = (ContactSipImpl) contactIterator.next();
            unresolvedContacts.add(contact);
        }
        // Process all server groups and fire events
        for (ListType serverList : serverGroup.getLists())
        {
            ContactGroupSipImpl newGroup =
                    (ContactGroupSipImpl) clientGroup.getGroup(
                            serverList.getName());
            if (newGroup == null)
            {
                newGroup = new ContactGroupSipImpl(serverList.getName(),
                        sipProvider);
                newGroup.setOtherAttributes(serverList.getAnyAttributes());
                newGroup.setAny(serverList.getAny());
                newGroup.setResolved(true);
                clientGroup.addSubgroup(newGroup);
                // Tell listeners about the added group
                fireGroupEvent(newGroup,
                        ServerStoredGroupEvent.GROUP_CREATED_EVENT);
                resolveContactGroup(newGroup, serverList, deleteUnresolved);
            }
            else
            {
                newGroup.setResolved(true);
                newGroup.setOtherAttributes(serverList.getAnyAttributes());
                newGroup.setAny(serverList.getAny());
                unresolvedGroups.remove(newGroup);
                // Tell listeners about the resolved group
                fireGroupEvent(newGroup,
                        ServerStoredGroupEvent.GROUP_RESOLVED_EVENT);
                resolveContactGroup(newGroup, serverList, deleteUnresolved);
            }
        }
        // Process all server contacts and fire events
        for (EntryType serverEntry : serverGroup.getEntries())
        {
            ContactSipImpl newContact = (ContactSipImpl)
                    clientGroup.getContact(serverEntry.getUri());
            if (newContact == null)
            {
                Address sipAddress;
                try
                {
                    sipAddress = sipProvider.parseAddressString(
                            serverEntry.getUri());
                }
                catch (ParseException e)
                {
                    logger.error(e);
                    continue;
                }
                newContact = new ContactSipImpl(sipAddress, sipProvider);
                newContact.setDisplayName(serverEntry.getDisplayName());
                newContact.setOtherAttributes(serverEntry.getAnyAttributes());
                newContact.setAny(serverEntry.getAny());
                newContact.setResolved(true);
                newContact.setXCapResolved(true);
                clientGroup.addContact(newContact);

                fireContactAdded(clientGroup, newContact);
            }
            else
            {
                newContact.setDisplayName(serverEntry.getDisplayName());
                newContact.setOtherAttributes(serverEntry.getAnyAttributes());
                newContact.setAny(serverEntry.getAny());
                newContact.setResolved(true);
                newContact.setXCapResolved(true);
                unresolvedContacts.remove(newContact);

                fireContactResolved(clientGroup, newContact);
            }
        }
        // Save all others
        // TODO: process externals and enrty-refs after OpenXCAP fixes
        clientGroup.getList().getExternals().addAll(serverGroup.getExternals());
        clientGroup.getList().getEntryRefs().addAll(serverGroup.getEntryRefs());
        clientGroup.getList().getAny().addAll(serverGroup.getAny());

        // Process all unresolved contacts
        if (deleteUnresolved)
        {
            for (ContactSipImpl unresolvedContact : unresolvedContacts)
            {
                if(!unresolvedContact.isPersistent())
                {
                    continue;
                }
                unresolvedContact.setResolved(true);
                unresolvedContact.setXCapResolved(true);
                // Remove unresolved contacts
                clientGroup.removeContact(unresolvedContact);
                // Tell listeners about the removed contact
                fireContactRemoved(clientGroup, unresolvedContact);
            }
        }
        // Process all unresolved groups
        if (deleteUnresolved)
        {
            for (ContactGroupSipImpl unresolvedGroup : unresolvedGroups)
            {
                if(!unresolvedGroup.isPersistent())
                {
                    continue;
                }
                unresolvedGroup.setResolved(true);
                // Remove unresolved groups
                clientGroup.removeSubGroup(unresolvedGroup);
                // Tell listeners about the removed group
                fireGroupEvent(unresolvedGroup,
                        ServerStoredGroupEvent.GROUP_REMOVED_EVENT);
            }
        }
    }

    /**
     * Puts resource-lists to the server.
     *
     * @throws XCapException if there is some error during operation.
     */
    synchronized private void updateResourceLists()
            throws XCapException
    {
        XCapClient xCapClient = sipProvider.getXCapClient();
        if (!xCapClient.isConnected() || !xCapClient.isResourceListsSupported())
        {
            return;
        }
        ResourceListsType resourceLists = new ResourceListsType();
        for (ListType list : rootGroup.getList().getLists())
        {
            resourceLists.getList().add(list);
        }
        // Create special root group
        ListType serverRootList = new ListType();
        serverRootList.setName(ROOT_GROUP_NAME);
        serverRootList.setDisplayName(rootGroup.getList().getDisplayName());
        serverRootList.getEntries().addAll(rootGroup.getList().getEntries());
        serverRootList.getEntryRefs()
                .addAll(rootGroup.getList().getEntryRefs());
        serverRootList.getExternals()
                .addAll(rootGroup.getList().getExternals());
        serverRootList.setAny(rootGroup.getList().getAny());
        serverRootList
                .setAnyAttributes(rootGroup.getList().getAnyAttributes());
        resourceLists.getList().add(serverRootList);

        xCapClient.putResourceLists(resourceLists);
    }

    /**
     * Puts pres-rules to the server.
     *
     * @throws XCapException if there is some error during operation.
     */
    synchronized void updatePresRules()
            throws XCapException
    {
        XCapClient xCapClient = sipProvider.getXCapClient();
        if (!xCapClient.isConnected() || !xCapClient.isPresRulesSupported())
        {
            return;
        }
        xCapClient.putPresRules(presRules);
    }
}
