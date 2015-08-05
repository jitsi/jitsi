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
package net.java.sip.communicator.impl.protocol.sip;

import gov.nist.javax.sip.address.*;

import java.net.URI;
import java.text.*;
import java.util.*;

import javax.sip.address.*;

import net.java.sip.communicator.impl.protocol.sip.xcap.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.prescontent.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.presrules.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.resourcelists.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.ImageDetail;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Base64; // disambiguation

import org.jitsi.util.xml.XMLUtils;
import org.w3c.dom.*;

/**
 * Encapsulates XCapClient, it's responsible for generate corresponding
 * events to all action that are made with XCAP contacts and
 * groups.
 *
 * @author Grigorii Balutsel
 */
public class ServerStoredContactListSipImpl
    extends ServerStoredContactList
{
    /**
     * Logger class
     */
    private static final Logger logger =
            Logger.getLogger(ServerStoredContactListSipImpl.class);

    /**
     * Presence content for image.
     */
    public static final String PRES_CONTENT_IMAGE_NAME = "sip_communicator";

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
     * The contact type element name used in xcap documents.
     */
    private static final String CONTACT_TYPE_ELEMENT_NAME = "contact-type";

    /**
     * The namespace used for contact type.
     */
    private static final String CONTACT_TYPE_NS =
        "http://jitsi.org/contact-type";

    /**
     * The XCAP client.
     */
    private final XCapClient xCapClient = new XCapClientImpl();


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
        super(sipProvider, parentOperationSet);
    }

    /**
     * Creates contact for the specified address and inside the
     * specified group . If creation is successfull event will be fired.
     *
     * @param parentGroup the group where the unersolved contact is to be
     *                    created.
     * @param contactId   the sip id of the contact to create.
     * @param displayName the display name of the contact to create
     * @param persistent  specify whether created contact is persistent ot not.
     * @param contactType the contact type to create, if missing null.
     * @return the newly created <tt>ContactSipImpl</tt>.
     * @throws OperationFailedException with code NETWORK_FAILURE if the
     *                                  operation if failed during network
     *                                  communication.
     */
    @Override
    synchronized public ContactSipImpl createContact(
            ContactGroupSipImpl parentGroup,
            String contactId,
            String displayName,
            boolean persistent,
            String contactType)
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

        // Set the display name.
        if (displayName == null || displayName.length() <= 0)
            displayName = ((SipURI) contactAddress.getURI()).getUser();

        newContact.setDisplayName(displayName);

        if(contactType != null)
        {
            setContactType(newContact, contactType);
        }

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
    @Override
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
    @Override
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
     * Renames the specified contact.
     *
     * @param contact the contact to be renamed.
     * @param newName the new contact name.
     */
    @Override
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
    @Override
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
    @Override
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
    @Override
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
     * Initializes the server stored list. Synchronize server stored groups and
     * contacts with the local groups and contacts.
     */
    @Override
    synchronized public void init()
    {
        try
        {
            SipAccountIDImpl accountID
                    = (SipAccountIDImpl) sipProvider.getAccountID();

            if(!accountID.isXCapEnable())
                return;

            String serverUri = accountID.getClistOptionServerUri();

            String username = accountID.getAccountPropertyString(
                              ProtocolProviderFactory.USER_ID);
            Address userAddress = sipProvider.parseAddressString(username);
            String password;
            if (accountID.isClistOptionUseSipCredentials())
            {
                username = ((SipUri)userAddress.getURI()).getUser();
                password = SipActivator.getProtocolProviderFactory().
                        loadPassword(accountID);
            }
            else
            {
                username = accountID.getClistOptionUser();
                password = accountID.getClistOptionPassword();
            }
            // Connect to xcap server
            if(serverUri != null)
            {
                URI uri = new URI(serverUri.trim());
                if(uri.getHost() != null && uri.getPath() != null)
                {
                    xCapClient.connect(uri, userAddress, username, password);
                }
            }
        }
        catch(Throwable ex)
        {
            logger.error("Error while connecting to XCAP server. " +
                        "Contact list won't be saved", ex);
        }

        try
        {
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
            xCapClient.disconnect();
        }
    }

    /**
     * Gets the pres-content image uri.
     *
     * @return the pres-content image uri.
     * @throws IllegalStateException if the user has not been connected.
     */
    @Override
    public URI getImageUri()
    {
        if (xCapClient.isConnected() && xCapClient.isPresContentSupported())
        {
            return xCapClient.getPresContentImageUri(PRES_CONTENT_IMAGE_NAME);
        }

        return null;
    }

    /**
     * Gets image from the specified uri.
     *
     * @param imageUri the image uri.
     * @return the image.
     */
    @Override
    public byte[] getImage(URI imageUri)
    {
        if(xCapClient.isConnected())
        {
            try
            {
                return xCapClient.getImage(imageUri);
            }
            catch (XCapException e)
            {
                String errorMessage = String.format(
                        "Error while getting icon %1s", imageUri);
                logger.warn(errorMessage);
                if(logger.isDebugEnabled())
                    logger.debug(errorMessage, e);
            }
        }

        return null;
    }

    /**
     * Destroys the server stored list.
     */
    @Override
    synchronized public void destroy()
    {
        xCapClient.disconnect();

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
            if (!xCapClient.isConnected() ||
                    !xCapClient.isResourceListsSupported())
            {
                return null;
            }

            presRules = xCapClient.getPresRules();
        }

        for (RuleType rule : presRules.getRules())
        {
            SubHandlingType currType = rule.getActions().getSubHandling();
            if (currType != null && currType.equals(type))
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
    synchronized void updateResourceLists()
            throws XCapException
    {
        if (!xCapClient.isConnected()
            || !xCapClient.isResourceListsSupported())
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
        if (!xCapClient.isConnected() || !xCapClient.isPresRulesSupported())
        {
            return;
        }
        xCapClient.putPresRules(presRules);
    }

    /**
     * The user accepted authorization request for <tt>contact</tt>
     * @param contact the user has accepted.
     */
    @Override
    public void authorizationAccepted(ContactSipImpl contact)
    {
        try
        {
            if(addContactToWhiteList(contact))
                updatePresRules();
        }
        catch(XCapException ex)
        {
            logger.error("Cannot save presence rules!", ex);
        }
    }

    /**
     * The user rejected authorization request for <tt>contact</tt>
     * @param contact the user has rejected.
     */
    @Override
    public void authorizationRejected(ContactSipImpl contact)
    {
        try
        {
            if(addContactToBlockList(contact))
                updatePresRules();
        }
        catch(XCapException ex)
        {
            logger.error("Cannot save presence rules!", ex);
        }
    }

    /**
     * The user ignored authorization request for <tt>contact</tt>
     * @param contact the user has ignored.
     */
    @Override
    public void authorizationIgnored(ContactSipImpl contact)
    {
        try
        {
            if(addContactToPoliteBlockList(contact))
                updatePresRules();
        }
        catch(XCapException ex)
        {
            logger.error("Cannot save presence rules!", ex);
        }
    }

    /**
     * Get current account image from server if any.
     * @return the account image content.
     */
    @Override
    public ImageDetail getAccountImage()
        throws OperationFailedException
    {
        ImageDetail imageDetail;

        try
        {
            ContentType presContent = xCapClient.getPresContent(
                    PRES_CONTENT_IMAGE_NAME);
            if (presContent == null)
            {
                return null;
            }
            String description = null;
            byte[] content = null;
            if (presContent.getDescription().size() > 0)
            {
                description = presContent.getDescription().get(0).getValue();
            }
            if (presContent.getData() != null)
            {
                content = Base64.decode(presContent.getData().getValue());
            }
            imageDetail = new ServerStoredDetails.ImageDetail(description, content);
        }
        catch (XCapException e)
        {
            throw new OperationFailedException("Cannot get image detail",
                    OperationFailedException.NETWORK_FAILURE);
        }

        return imageDetail;
    }

    /**
     * Deletes current account image from server.
     */
    @Override
    public void deleteAccountImage()
        throws OperationFailedException
    {
        try
        {
            xCapClient.deletePresContent(PRES_CONTENT_IMAGE_NAME);
        }
        catch (XCapException e)
        {
            throw new OperationFailedException("Cannot delete image detail",
                    OperationFailedException.NETWORK_FAILURE);
        }
    }

    /**
     * Whether current contact list supports account image.
     * @return does current contact list supports account image.
     */
    @Override
    public boolean isAccountImageSupported()
    {
        return xCapClient != null &&
                xCapClient.isConnected() &&
                xCapClient.isPresContentSupported();
    }

    /**
     * Change the image of the account on server.
     * @param newImageBytes the new image.
     */
    @Override
    public void setAccountImage(byte[] newImageBytes)
        throws OperationFailedException
    {
        ContentType presContent = new ContentType();
        MimeType mimeType = new MimeType();
        mimeType.setValue("image/png");
        presContent.setMimeType(mimeType);
        EncodingType encoding = new EncodingType();
        encoding.setValue("base64");
        presContent.setEncoding(encoding);
        String encodedImageContent =
                new String(Base64.encode(newImageBytes));
        DataType data = new DataType();
        data.setValue(encodedImageContent);
        presContent.setData(data);
        try
        {
            xCapClient.putPresContent(presContent, PRES_CONTENT_IMAGE_NAME);
        }
        catch (XCapException e)
        {
            throw new OperationFailedException("Cannot put image detail",
                    OperationFailedException.NETWORK_FAILURE);
        }
    }

    /**
     * Access the contact type. If none specified null is returned.
     * @param contact the contact to be queried for type.
     * @return the contact type or null if missing.
     */
    public String getContactType(Contact contact)
    {
        if (!(contact instanceof ContactSipImpl))
        {
            String errorMessage = String.format(
                "Contact %1s does not seem to belong to this protocol's " +
                    "contact list", contact.getAddress());
            throw new IllegalArgumentException(errorMessage);
        }

        ContactSipImpl contactSip = (ContactSipImpl)contact;

        List<Element> anyElements = contactSip.getAny();
        for(Element e : anyElements)
        {
            if(e.getNodeName().equals(CONTACT_TYPE_ELEMENT_NAME))
                return XMLUtils.getText(e);
        }

        return null;
    }

    /**
     * Sets the contact type of the contact.
     * @param contact the contact to be changed.
     * @param contactType the type set to the contact.
     */
    public void setContactType(Contact contact, String contactType)
    {
        if (!(contact instanceof ContactSipImpl))
        {
            String errorMessage = String.format(
                "Contact %1s does not seem to belong to this protocol's " +
                    "contact list", contact.getAddress());
            throw new IllegalArgumentException(errorMessage);
        }

        ContactSipImpl contactSip = (ContactSipImpl)contact;

        List<Element> anyElements = contactSip.getAny();

        try
        {
            Element typeElement = null;

            for(Element el : anyElements)
            {
                if(el.getNodeName().equals(CONTACT_TYPE_ELEMENT_NAME))
                {
                    typeElement = el;
                    break;
                }
            }

            // if its missing create it
            if(typeElement == null)
            {
                Document document = XMLUtils.createDocument();
                typeElement =
                    document.createElementNS(
                        CONTACT_TYPE_NS,
                        CONTACT_TYPE_ELEMENT_NAME);
                anyElements.add(typeElement);
            }

            typeElement.setTextContent(contactType);

            contactSip.setAny(anyElements);
        }
        catch(Throwable t)
        {
            logger.error("Error creating element", t);
        }
    }
}
