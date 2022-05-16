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
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import org.jitsi.xmpp.extensions.vcardavatar.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.jabberconstants.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.SmackException.*;
import org.jivesoftware.smack.XMPPException.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.roster.*;
import org.jivesoftware.smack.roster.packet.*;
import org.jivesoftware.smackx.nick.packet.*;
import org.jivesoftware.smackx.vcardtemp.*;
import org.jivesoftware.smackx.vcardtemp.packet.*;
import org.jxmpp.jid.*;
import org.jxmpp.jid.impl.*;
import org.jxmpp.jid.parts.*;
import org.jxmpp.stringprep.*;

/**
 * The Jabber implementation of a Persistent Presence Operation set. This class
 * manages our own presence status as well as subscriptions for the presence
 * status of our buddies. It also offers methods for retrieving and modifying
 * the buddy contact list and adding listeners for changes in its layout.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 * @author Hristo Terezov
 */
public class OperationSetPersistentPresenceJabberImpl
    extends AbstractOperationSetPersistentPresence<
                ProtocolProviderServiceJabberImpl>
{
    /**
     * The logger.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OperationSetPersistentPresenceJabberImpl.class);

    /**
     * Contains our current status message. Note that this field would only
     * be changed once the server has confirmed the new status message and
     * not immediately upon setting a new one..
     */
    private String currentStatusMessage = "";

    /**
     * The presence status that we were last notified of entering.
     * The initial one is OFFLINE
     */
    private PresenceStatus currentStatus;

    /**
     * A map containing bindings between Jitsi's jabber presence
     * status instances and Jabber status codes
     */
    private static Map<String, Presence.Mode> scToJabberModesMappings
        = new Hashtable<>();

    static
    {
        scToJabberModesMappings.put(JabberStatusEnum.AWAY,
                                  Presence.Mode.away);
        scToJabberModesMappings.put(JabberStatusEnum.ON_THE_PHONE,
                                  Presence.Mode.away);
        scToJabberModesMappings.put(JabberStatusEnum.IN_A_MEETING,
            Presence.Mode.away);
        scToJabberModesMappings.put(JabberStatusEnum.EXTENDED_AWAY,
                                  Presence.Mode.xa);
        scToJabberModesMappings.put(JabberStatusEnum.DO_NOT_DISTURB,
                                  Presence.Mode.dnd);
        scToJabberModesMappings.put(JabberStatusEnum.FREE_FOR_CHAT,
                                  Presence.Mode.chat);
        scToJabberModesMappings.put(JabberStatusEnum.AVAILABLE,
                                  Presence.Mode.available);
    }

    /**
     * A map containing bindings between Jitsi's xmpp presence
     * status instances and priorities to use for statuses.
     */
    private static Map<String, Integer> statusToPriorityMappings
        = new Hashtable<>();

    /**
     * The server stored contact list that will be encapsulating smack's
     * buddy list.
     */
    private final ServerStoredContactListJabberImpl ssContactList;

    /**
     * Listens for subscriptions.
     */
    private JabberSubscriptionListener subscribtionPacketListener = null;

    /**
     * Current resource priority.
     */
    private int resourcePriorityAvailable = 30;

    /**
     * Manages statuses and different user resources.
     */
    private ContactChangesListener contactChangesListener = null;

    /**
     * Manages the presence extension to advertise the SHA-1 hash of this
     * account avatar as defined in XEP-0153.
     */
    private VCardTempXUpdatePresenceExtension vCardTempXUpdatePresenceExtension
        = null;

    /**
     * Handles all the logic about mobile indicator for contacts.
     */
    private final MobileIndicator mobileIndicator;

    /**
     * The last sent presence to server, contains the status, the resource
     * and its priority.
     */
    private Presence currentPresence = null;

    /**
     * The local contact presented by the provider.
     */
    private ContactJabberImpl localContact = null;

    /**
     * Creates the OperationSet.
     * @param provider the parent provider.
     * @param infoRetreiver retrieve contact information.
     */
    public OperationSetPersistentPresenceJabberImpl(
        ProtocolProviderServiceJabberImpl provider,
        InfoRetreiver infoRetreiver)
    {
        super(provider);

        currentStatus =
            parentProvider.getJabberStatusEnum().getStatus(
                JabberStatusEnum.OFFLINE);

        initializePriorities();

        ssContactList = new ServerStoredContactListJabberImpl(
            this , provider, infoRetreiver);

        parentProvider.addRegistrationStateChangeListener(
            new RegistrationStateListener());

        mobileIndicator = new MobileIndicator(parentProvider, ssContactList);
    }

    /**
     * Registers a listener that would receive events upon changes in server
     * stored groups.
     *
     * @param listener a ServerStoredGroupChangeListener impl that would
     *   receive events upon group changes.
     */
    @Override
    public void addServerStoredGroupChangeListener(ServerStoredGroupListener
        listener)
    {
        ssContactList.addGroupListener(listener);
    }

    /**
     * Creates a group with the specified name and parent in the server
     * stored contact list.
     *
     * @param parent the group where the new group should be created
     * @param groupName the name of the new group to create.
     * @throws OperationFailedException if such group already exists
     */
    public void createServerStoredContactGroup(ContactGroup parent,
                                               String groupName)
        throws OperationFailedException
    {
        assertConnected();

        if (!parent.canContainSubgroups())
           throw new IllegalArgumentException(
               "The specified contact group cannot contain child groups. Group:"
               + parent );

        ssContactList.createGroup(groupName);
    }

    /**
     * Creates a non persistent contact for the specified address. This would
     * also create (if necessary) a group for volatile contacts that would not
     * be added to the server stored contact list. The volatile contact would
     * remain in the list until it is really added to the contact list or
     * until the application is terminated.
     * @param id the address of the contact to create.
     * @return the newly created volatile <tt>ContactImpl</tt>
     */
    public synchronized ContactJabberImpl createVolatileContact(Jid id)
    {
        return createVolatileContact(id, null);
    }

    /**
     * Creates a non persistent contact for the specified address. This would
     * also create (if necessary) a group for volatile contacts that would not
     * be added to the server stored contact list. The volatile contact would
     * remain in the list until it is really added to the contact list or
     * until the application is terminated.
     * @param id the address of the contact to create.
     * @param displayName the display name of the contact.
     * @return the newly created volatile <tt>ContactImpl</tt>
     */
    public synchronized ContactJabberImpl createVolatileContact(Jid id,
                                                                String displayName)
    {
        return createVolatileContact(id, false, displayName);
    }

    /**
     * Creates a non persistent contact for the specified address. This would
     * also create (if necessary) a group for volatile contacts that would not
     * be added to the server stored contact list. The volatile contact would
     * remain in the list until it is really added to the contact list or
     * until the application is terminated.
     * @param id the address of the contact to create.
     * @param isPrivateMessagingContact indicates whether the contact should be private
     * messaging contact or not.
     * @return the newly created volatile <tt>ContactImpl</tt>
     */
    public synchronized ContactJabberImpl createVolatileContact(Jid id,
                                                                boolean isPrivateMessagingContact)
    {
        return createVolatileContact(id, isPrivateMessagingContact, null);
    }

    /**
     * Creates a non persistent contact for the specified address. This would
     * also create (if necessary) a group for volatile contacts that would not
     * be added to the server stored contact list. The volatile contact would
     * remain in the list until it is really added to the contact list or
     * until the application is terminated.
     * @param id the address of the contact to create.
     * @param isPrivateMessagingContact indicates whether the contact should be private
     * messaging contact or not.
     * @param displayName the display name of the contact.
     * @return the newly created volatile <tt>ContactImpl</tt>
     */
    public synchronized ContactJabberImpl createVolatileContact(Jid id,
        boolean isPrivateMessagingContact, String displayName)
    {
        // first check for already created one.
        ContactGroupJabberImpl notInContactListGroup =
            ssContactList.getNonPersistentGroup();
        ContactJabberImpl sourceContact = null;
        if (notInContactListGroup != null)
        {
            sourceContact = notInContactListGroup.findContact(id.asBareJid());
        }

        if(sourceContact != null)
        {
            return sourceContact;
        }
        else
        {
            sourceContact = ssContactList.createVolatileContact(
                id, isPrivateMessagingContact, displayName);
            if(isPrivateMessagingContact && id.hasResource())
            {
                updateResources(sourceContact, false);
            }

            return sourceContact;
        }
    }

    /**
     * Creates and returns a unresolved contact from the specified
     * <tt>address</tt> and <tt>persistentData</tt>.
     *
     * @param address an identifier of the contact that we'll be creating.
     * @param persistentData a String returned Contact's getPersistentData()
     *   method during a previous run and that has been persistently stored
     *   locally.
     * @param parentGroup the group where the unresolved contact is supposed
     *   to belong to.
     * @return the unresolved <tt>Contact</tt> created from the specified
     *   <tt>address</tt> and <tt>persistentData</tt>
     */
    public Contact createUnresolvedContact(String address,
                                           String persistentData,
                                           ContactGroup parentGroup)
    {
        if(! (parentGroup instanceof ContactGroupJabberImpl ||
              parentGroup instanceof RootContactGroupJabberImpl) )
            throw new IllegalArgumentException(
                "Argument is not an jabber contact group (group="
                + parentGroup + ")");

        try
        {
            return ssContactList.createUnresolvedContact(
                    parentGroup,
                    JidCreate.from(address));
        }
        catch (XmppStringprepException e)
        {
            throw new IllegalArgumentException("Invalid JID", e);
        }
    }

    /**
     * Creates and returns a unresolved contact from the specified
     * <tt>address</tt> and <tt>persistentData</tt>.
     *
     * @param address an identifier of the contact that we'll be creating.
     * @param persistentData a String returned Contact's getPersistentData()
     *   method during a previous run and that has been persistently stored
     *   locally.
     * @return the unresolved <tt>Contact</tt> created from the specified
     *   <tt>address</tt> and <tt>persistentData</tt>
     */
    public Contact createUnresolvedContact(String address,
                                           String persistentData)
    {
        return createUnresolvedContact(  address
                                       , persistentData
                                       , getServerStoredContactListRoot());
    }

    /**
     * Creates and returns a unresolved contact group from the specified
     * <tt>address</tt> and <tt>persistentData</tt>.
     *
     * @param groupUID an identifier, returned by ContactGroup's
     *   getGroupUID, that the protocol provider may use in order to create
     *   the group.
     * @param persistentData a String returned ContactGroups's
     *   getPersistentData() method during a previous run and that has been
     *   persistently stored locally.
     * @param parentGroup the group under which the new group is to be
     *   created or null if this is group directly underneath the root.
     * @return the unresolved <tt>ContactGroup</tt> created from the
     *   specified <tt>uid</tt> and <tt>persistentData</tt>
     */
    public ContactGroup createUnresolvedContactGroup(String groupUID,
        String persistentData, ContactGroup parentGroup)
    {
        return ssContactList.createUnresolvedContactGroup(groupUID);
    }

    /**
     * Returns a reference to the contact with the specified ID in case we
     * have a subscription for it and null otherwise/
     *
     * @param contactID a String identifier of the contact which we're
     *   seeking a reference of.
     * @return a reference to the Contact with the specified
     *   <tt>contactID</tt> or null if we don't have a subscription for the
     *   that identifier.
     */
    public Contact findContactByID(String contactID)
    {
        try
        {
            return ssContactList.findContactById(JidCreate.from(contactID));
        }
        catch (XmppStringprepException e)
        {
            logger.error("Could not parse contact into Jid: " + contactID, e);
            return null;
        }
    }

    public Contact findContactByID(Jid contactId)
    {
        return ssContactList.findContactById(contactId);
    }

    /**
     * Returns the status message that was confirmed by the serfver
     *
     * @return the last status message that we have requested and the aim
     *   server has confirmed.
     */
    public String getCurrentStatusMessage()
    {
        return currentStatusMessage;
    }

    /**
     * Returns the protocol specific contact instance representing the local
     * user.
     *
     * @return the Contact (address, phone number, or uin) that the Provider
     *   implementation is communicating on behalf of.
     */
    public Contact getLocalContact()
    {
        if(localContact != null)
            return localContact;

        final BareJid id;
        try
        {
            id = JidCreate.from(parentProvider.getAccountID().getUserID()).asBareJid();
        }
        catch (XmppStringprepException e)
        {
            return null;
        }

        localContact
            = new ContactJabberImpl(null, ssContactList, false, true);
        localContact.setLocal(true);
        localContact.updatePresenceStatus(currentStatus);
        localContact.setJid(parentProvider.getOurJID());

        Map<FullJid, ContactResourceJabberImpl> rs = localContact.getResourcesMap();

        if(currentPresence != null)
            rs.put(parentProvider.getOurJID().asFullJidIfPossible(),
                createResource( currentPresence,
                                parentProvider.getOurJID().asFullJidIfPossible(),
                                localContact));

        for (Presence p : ssContactList.getPresences(id))
        {
            FullJid fullJid = p.getFrom().asFullJidIfPossible();
            rs.put(fullJid, createResource(
                p,
                p.getFrom().asFullJidIfPossible(),
                localContact));
        }

        // adds xmpp listener for changes in the local contact resources
        StanzaFilter presenceFilter = new StanzaTypeFilter(Presence.class);
        parentProvider.getConnection()
            .addAsyncStanzaListener(
                new StanzaListener()
                {
                    @Override
                    public void processStanza(Stanza packet)
                    {
                        Presence presence = (Presence) packet;
                        Jid from = presence.getFrom();

                        if(from == null || !from.asBareJid().equals(id))
                            return;

                        // own resource update, let's process it
                        updateResource(localContact, null, presence);
                    }
                }, presenceFilter);

        return localContact;
    }

    /**
     * Creates ContactResource from the presence, full jid and contact.
     * @param presence the presence object.
     * @param fullJid the full jid for the resource.
     * @param contact the contact.
     * @return the newly created resource.
     */
    private ContactResourceJabberImpl createResource(
        Presence presence,
        FullJid fullJid,
        Contact contact)
    {
        return new ContactResourceJabberImpl(
            fullJid,
            contact,
            jabberStatusToPresenceStatus(presence, parentProvider),
            presence.getPriority(),
            mobileIndicator.isMobileResource(fullJid));
    }

    /**
     * Clear resources used for local contact and before that update its
     * resources in order to fire the needed events.
     */
    private void clearLocalContactResources()
    {
        if(localContact != null)
        {
            removeResource(
                localContact,
                localContact
                    .getAddressAsJid()
                    .asFullJidIfPossible());
        }

        currentPresence = null;
        localContact = null;
    }

    /**
     * Returns a PresenceStatus instance representing the state this provider
     * is currently in.
     *
     * @return the PresenceStatus last published by this provider.
     */
    public PresenceStatus getPresenceStatus()
    {
        return currentStatus;
    }

    /**
     * Returns the root group of the server stored contact list.
     *
     * @return the root ContactGroup for the ContactList stored by this
     *   service.
     */
    public ContactGroup getServerStoredContactListRoot()
    {
        return ssContactList.getRootGroup();
    }

    /**
     * Returns the set of PresenceStatus objects that a user of this service
     * may request the provider to enter.
     *
     * @return Iterator a PresenceStatus array containing "enterable" status
     *   instances.
     */
    public Iterator<PresenceStatus> getSupportedStatusSet()
    {
        return parentProvider.getJabberStatusEnum().getSupportedStatusSet();
    }

    /**
     * Checks if the contact address is associated with private messaging
     * contact or not.
     * @param contactAddress the address of the contact.
     * @return <tt>true</tt> the contact address is associated with private
     * messaging contact and <tt>false</tt> if not.
     */
    public boolean isPrivateMessagingContact(Jid contactAddress)
    {
        return ssContactList.isPrivateMessagingContact(contactAddress);
    }

    /**
     * Removes the specified contact from its current parent and places it
     * under <tt>newParent</tt>.
     *
     * @param contactToMove the <tt>Contact</tt> to move
     * @param newParent the <tt>ContactGroup</tt> where <tt>Contact</tt>
     *   would be placed.
     */
    public void moveContactToGroup(Contact contactToMove,
                                   ContactGroup newParent)
        throws OperationFailedException
    {
        assertConnected();

        if( !(contactToMove instanceof ContactJabberImpl) )
            throw new IllegalArgumentException(
                "The specified contact is not an jabber contact." +
                contactToMove);
        if( !(newParent instanceof AbstractContactGroupJabberImpl) )
            throw new IllegalArgumentException(
                "The specified group is not an jabber contact group."
                + newParent);

        ssContactList.moveContact((ContactJabberImpl)contactToMove,
                                  (AbstractContactGroupJabberImpl)newParent);
    }

    /**
     * Requests the provider to enter into a status corresponding to the
     * specified parameters.
     *
     * @param status the PresenceStatus as returned by
     *   getRequestableStatusSet
     * @param statusMessage the message that should be set as the reason to
     *   enter that status
     * @throws IllegalArgumentException if the status requested is not a
     *   valid PresenceStatus supported by this provider.
     * @throws IllegalStateException if the provider is not currently
     *   registered.
     * @throws OperationFailedException with code NETWORK_FAILURE if
     *   publishing the status fails due to a network error.
     */
    public void publishPresenceStatus(PresenceStatus status,
                                      String statusMessage)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException
    {
        assertConnected();

        JabberStatusEnum jabberStatusEnum =
            parentProvider.getJabberStatusEnum();
        boolean isValidStatus = false;
        for (Iterator<PresenceStatus> supportedStatusIter
                        = jabberStatusEnum.getSupportedStatusSet();
             supportedStatusIter.hasNext();)
        {
            if (supportedStatusIter.next().equals(status))
            {
                isValidStatus = true;
                break;
            }
        }
        if (!isValidStatus)
            throw new IllegalArgumentException(status
                + " is not a valid Jabber status");

        // if we got publish presence and we are still in a process of
        // initializing the roster, just save the status and we will dispatch
        // it when we are ready with the roster as sending initial presence
        // is recommended to be done after requesting the roster, but we want
        // to also dispatch it
        synchronized(ssContactList.getRosterInitLock())
        {
            if(!ssContactList.isRosterInitialized())
            {
                // store it
                ssContactList.setInitialStatus(status);
                ssContactList.setInitialStatusMessage(statusMessage);
                return;
            }
        }

        if (status.equals(jabberStatusEnum.getStatus(JabberStatusEnum.OFFLINE)))
        {
            parentProvider.unregister();
            clearLocalContactResources();
        }
        else
        {
            Presence presence = new Presence(Presence.Type.available);
            currentPresence = presence;
            presence.setMode(presenceStatusToJabberMode(status));
            presence.setPriority(
                getPriorityForPresenceStatus(status.getStatusName()));

            // on the phone is a special status which is away
            // with custom status message
            if(status.equals(jabberStatusEnum.getStatus(
                    JabberStatusEnum.ON_THE_PHONE)))
            {
                presence.setStatus(JabberStatusEnum.ON_THE_PHONE);
            }
            else if(status.equals(jabberStatusEnum.getStatus(
                JabberStatusEnum.IN_A_MEETING)))
            {
                presence.setStatus(JabberStatusEnum.IN_A_MEETING);
            }
            else
            {
                presence.setStatus(statusMessage);
            }

            try
            {
                parentProvider.getConnection().sendStanza(presence);
            }
            catch (NotConnectedException | InterruptedException e)
            {
                throw new OperationFailedException(
                    "Could not send new presense status",
                    OperationFailedException.GENERAL_ERROR,
                    e
                );
            }

            if(localContact != null)
                updateResource(localContact,
                    parentProvider.getOurJID().asFullJidIfPossible(), presence);
        }

        fireProviderStatusChangeEvent(currentStatus, status);

        String oldStatusMessage = getCurrentStatusMessage();

        /*
         * XXX Use StringUtils.isEquals instead of String.equals to avoid a
         * NullPointerException.
         */
        if(!java.util.Objects.equals(oldStatusMessage, statusMessage))
        {
            currentStatusMessage = statusMessage;
            fireProviderStatusMessageChangeEvent(
                    oldStatusMessage,
                    getCurrentStatusMessage());
        }
    }

    /**
     * Gets the <tt>PresenceStatus</tt> of a contact with a specific
     * <tt>String</tt> identifier.
     *
     * @param contactIdentifier the identifier of the contact whose status we're
     * interested in.
     * @return the <tt>PresenceStatus</tt> of the contact with the specified
     * <tt>contactIdentifier</tt>
     * @throws IllegalArgumentException if the specified
     * <tt>contactIdentifier</tt> does not identify a contact known to the
     * underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is not
     * registered/signed on a public service
     * @throws OperationFailedException with code NETWORK_FAILURE if retrieving
     * the status fails due to errors experienced during network communication
     */
    public PresenceStatus queryContactStatus(String contactIdentifier)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException
    {
        BareJid contactJid;
        try
        {
            contactJid = JidCreate.bareFrom(contactIdentifier);
        }
        catch (XmppStringprepException e)
        {
            throw new OperationFailedException(
                "Contact is not a valid JID",
                OperationFailedException.GENERAL_ERROR,
                e
            );
        }

        /*
         * As stated by the javadoc, IllegalStateException signals that the
         * ProtocolProviderService is not registered.
         */
        assertConnected();
        XMPPConnection xmppConnection = parentProvider.getConnection();
        if (xmppConnection == null)
        {
            throw
                new IllegalArgumentException(
                        "The provider/account must be signed on in order to"
                            + " query the status of a contact in its roster");
        }

        Roster r = Roster.getInstanceFor(xmppConnection);
        Presence presence = r.getPresence(contactJid);
        return jabberStatusToPresenceStatus(presence, parentProvider);
    }

    /**
     * Removes the specified group from the server stored contact list.
     *
     * @param group the group to remove.
     */
    public void removeServerStoredContactGroup(ContactGroup group)
        throws OperationFailedException
    {
        assertConnected();

        if( !(group instanceof ContactGroupJabberImpl) )
            throw new IllegalArgumentException(
                "The specified group is not an jabber contact group: " + group);

        ssContactList.removeGroup(((ContactGroupJabberImpl)group));
    }

    /**
     * Removes the specified group change listener so that it won't receive
     * any further events.
     *
     * @param listener the ServerStoredGroupChangeListener to remove
     */
    @Override
    public void removeServerStoredGroupChangeListener(ServerStoredGroupListener
        listener)
    {
        ssContactList.removeGroupListener(listener);
    }

    /**
     * Renames the specified group from the server stored contact list.
     *
     * @param group the group to rename.
     * @param newName the new name of the group.
     */
    public void renameServerStoredContactGroup(ContactGroup group,
                                               String newName)
    {
        assertConnected();

        if( !(group instanceof ContactGroupJabberImpl) )
            throw new IllegalArgumentException(
                "The specified group is not an jabber contact group: " + group);

        ssContactList.renameGroup((ContactGroupJabberImpl)group, newName);
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
        subscribtionPacketListener.setHandler(handler);
    }

    /**
     * Persistently adds a subscription for the presence status of the
     * contact corresponding to the specified contactIdentifier and indicates
     * that it should be added to the specified group of the server stored
     * contact list.
     *
     * @param parent the parent group of the server stored contact list
     *   where the contact should be added. <p>
     * @param contactIdentifier the contact whose status updates we are
     *   subscribing for.
     * @throws IllegalArgumentException if <tt>contact</tt> or
     *   <tt>parent</tt> are not a contact known to the underlying protocol
     *   provider.
     * @throws IllegalStateException if the underlying protocol provider is
     *   not registered/signed on a public service.
     * @throws OperationFailedException with code NETWORK_FAILURE if
     *   subscribing fails due to errors experienced during network
     *   communication
     */
    public void subscribe(ContactGroup parent, String contactIdentifier)
        throws IllegalArgumentException, IllegalStateException,
               OperationFailedException
    {
        assertConnected();

        if(! (parent instanceof ContactGroupJabberImpl) )
            throw new IllegalArgumentException(
                "Argument is not an jabber contact group (group="
                            + parent + ")");

        ssContactList.addContact(parent, contactIdentifier);
    }

    /**
     * Adds a subscription for the presence status of the contact
     * corresponding to the specified contactIdentifier.
     *
     * @param contactIdentifier the identifier of the contact whose status
     *   updates we are subscribing for. <p>
     * @throws IllegalArgumentException if <tt>contact</tt> is not a contact
     *   known to the underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is
     *   not registered/signed on a public service.
     * @throws OperationFailedException with code NETWORK_FAILURE if
     *   subscribing fails due to errors experienced during network
     *   communication
     */
    public void subscribe(String contactIdentifier) throws
        IllegalArgumentException, IllegalStateException,
        OperationFailedException
    {
        assertConnected();

        ssContactList.addContact(contactIdentifier);
    }

    /**
     * Removes a subscription for the presence status of the specified
     * contact.
     *
     * @param contact the contact whose status updates we are unsubscribing
     *   from.
     * @throws IllegalArgumentException if <tt>contact</tt> is not a contact
     *   known to the underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is
     *   not registered/signed on a public service.
     * @throws OperationFailedException with code NETWORK_FAILURE if
     *   unsubscribing fails due to errors experienced during network
     *   communication
     */
    public void unsubscribe(Contact contact) throws IllegalArgumentException,
        IllegalStateException, OperationFailedException
    {
        assertConnected();

        if(! (contact instanceof ContactJabberImpl) )
            throw new IllegalArgumentException(
                "Argument is not an jabber contact (contact=" + contact + ")");

        ssContactList.removeContact((ContactJabberImpl)contact);
    }

    /**
     * Converts the specified jabber status to one of the status fields of the
     * JabberStatusEnum class.
     *
     * @param presence the Jabber Status
     * @param jabberProvider the parent provider.
     * @return a PresenceStatus instance representation of the Jabber Status
     * parameter. The returned result is one of the JabberStatusEnum fields.
     */
    public static PresenceStatus jabberStatusToPresenceStatus(
        Presence presence, ProtocolProviderServiceJabberImpl jabberProvider)
    {
        JabberStatusEnum jabberStatusEnum = jabberProvider.getJabberStatusEnum();
        if(!presence.isAvailable())
            return jabberStatusEnum.getStatus(JabberStatusEnum.OFFLINE);

        Presence.Mode mode = presence.getMode();

        if(mode.equals(Presence.Mode.available))
            return jabberStatusEnum.getStatus(JabberStatusEnum.AVAILABLE);
        else if(mode.equals(Presence.Mode.away))
        {
            // on the phone a special status
            // which is away with custom status message
            if(presence.getStatus() != null
                && presence.getStatus().contains(JabberStatusEnum.ON_THE_PHONE))
                return jabberStatusEnum.getStatus(JabberStatusEnum.ON_THE_PHONE);
            else if(presence.getStatus() != null
                && presence.getStatus().contains(JabberStatusEnum.IN_A_MEETING))
                return jabberStatusEnum.getStatus(JabberStatusEnum.IN_A_MEETING);
            else
                return jabberStatusEnum.getStatus(JabberStatusEnum.AWAY);
        }
        else if(mode.equals(Presence.Mode.chat))
            return jabberStatusEnum.getStatus(JabberStatusEnum.FREE_FOR_CHAT);
        else if(mode.equals(Presence.Mode.dnd))
            return jabberStatusEnum.getStatus(JabberStatusEnum.DO_NOT_DISTURB);
        else if(mode.equals(Presence.Mode.xa))
            return jabberStatusEnum.getStatus(JabberStatusEnum.EXTENDED_AWAY);
        else
        {
            //unknown status
            if(presence.isAway())
                return jabberStatusEnum.getStatus(JabberStatusEnum.AWAY);
            if(presence.isAvailable())
                return jabberStatusEnum.getStatus(JabberStatusEnum.AVAILABLE);

            return jabberStatusEnum.getStatus(JabberStatusEnum.OFFLINE);
        }
    }

    /**
     * Converts the specified JabberStatusEnum member to the corresponding
     * Jabber Mode
     *
     * @param status the jabberStatus
     * @return a PresenceStatus instance
     */
    public static Presence.Mode presenceStatusToJabberMode(
            PresenceStatus status)
    {
        return scToJabberModesMappings.get(status
            .getStatusName());
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     *
     * @throws IllegalStateException if the underlying stack is not registered
     * and initialized.
     */
    void assertConnected()
        throws IllegalStateException
    {
        if (parentProvider == null)
        {
            throw
                new IllegalStateException(
                        "The provider must be non-null and signed on the"
                            + " Jabber service before being able to"
                            + " communicate.");
        }
        if (!parentProvider.isRegistered())
        {
            // if we are not registered but the current status is online
            // change the current status
            if((currentStatus != null) && currentStatus.isOnline())
            {
                fireProviderStatusChangeEvent(
                    currentStatus,
                    parentProvider.getJabberStatusEnum().getStatus(
                            JabberStatusEnum.OFFLINE));
            }

            throw
                new IllegalStateException(
                        "The provider must be signed on the Jabber service"
                            + " before being able to communicate.");
        }
    }

    /**
     * Fires provider status change.
     *
     * @param oldStatus old status
     * @param newStatus new status
     */
    @Override
    public void fireProviderStatusChangeEvent(
        PresenceStatus oldStatus,
        PresenceStatus newStatus)
    {
        if (!oldStatus.equals(newStatus))
        {
            currentStatus = newStatus;

            super.fireProviderStatusChangeEvent(oldStatus, newStatus);

            PresenceStatus offlineStatus =
                    parentProvider.getJabberStatusEnum().getStatus(
                        JabberStatusEnum.OFFLINE);

            if(newStatus.equals(offlineStatus))
            {
                //send event notifications saying that all our buddies are
                //offline. The protocol does not implement top level buddies
                //nor subgroups for top level groups so a simple nested loop
                //would be enough.
                Iterator<ContactGroup> groupsIter =
                    getServerStoredContactListRoot().subgroups();
                while(groupsIter.hasNext())
                {
                    ContactGroup group = groupsIter.next();

                    Iterator<Contact> contactsIter = group.contacts();

                    while(contactsIter.hasNext())
                    {
                        ContactJabberImpl contact
                            = (ContactJabberImpl)contactsIter.next();

                        updateContactStatus(contact, offlineStatus);
                    }
                }

                //do the same for all contacts in the root group
                Iterator<Contact> contactsIter
                    = getServerStoredContactListRoot().contacts();

                while (contactsIter.hasNext())
                {
                    ContactJabberImpl contact
                        = (ContactJabberImpl) contactsIter.next();

                    updateContactStatus(contact, offlineStatus);
                }
            }
        }
    }

    /**
     * Sets the display name for <tt>contact</tt> to be <tt>newName</tt>.
     * <p>
     * @param contact the <tt>Contact</tt> that we are renaming
     * @param newName a <tt>String</tt> containing the new display name for
     * <tt>metaContact</tt>.
     * @throws IllegalArgumentException if <tt>contact</tt> is not an
     * instance that belongs to the underlying implementation.
     */
    @Override
    public void setDisplayName(Contact contact, String newName)
        throws IllegalArgumentException
    {
        assertConnected();

        if(! (contact instanceof ContactJabberImpl) )
            throw new IllegalArgumentException(
                "Argument is not an jabber contact (contact=" + contact + ")");

        RosterEntry entry = ((ContactJabberImpl)contact).getSourceEntry();
        if(entry == null)
        {
            return;
        }
        try
        {
            entry.setName(newName);
        }
        catch (NotConnectedException
            | InterruptedException
            | XMPPErrorException
            | NoResponseException e)
        {
            throw new IllegalArgumentException("Could not update name", e);
        }
    }

    /**
     * Our listener that will tell us when we're registered to server
     * and is ready to accept us as a listener.
     */
    private class RegistrationStateListener
        implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenever
         * a change in the registration state of the corresponding provider had
         * occurred.
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if (logger.isDebugEnabled())
                logger.debug("The Jabber provider changed state from: "
                         + evt.getOldState()
                         + " to: " + evt.getNewState());

            if(evt.getNewState() == RegistrationState.REGISTERING)
            {
                // we will add listener for RosterPackets
                // as this will indicate when one is received
                // and we are ready to dispatch the contact list
                // note that our listener will be added just before the
                // one used in the Roster itself, but later we
                // will wait for it to be ready
                // (inside method XMPPConnection.getRoster())
                parentProvider.getConnection().addAsyncStanzaListener(
                    new ServerStoredListInit(),
                    new StanzaTypeFilter(RosterPacket.class)
                );

                // will be used to store presence events till roster is
                // initialized
                contactChangesListener = new ContactChangesListener();

                // Adds subscription listener as soon as connection is created
                // or we can miss some subscription requests
                if(subscribtionPacketListener == null)
                {
                    subscribtionPacketListener =
                        new JabberSubscriptionListener();
                    parentProvider
                        .getConnection()
                            .addAsyncStanzaListener(
                                subscribtionPacketListener,
                                new StanzaTypeFilter(Presence.class));
                }
            }
            else if(evt.getNewState() == RegistrationState.REGISTERED)
            {
                JabberAccountIDImpl accountID
                    = (JabberAccountIDImpl)parentProvider.getAccountID();
                boolean isServerStoredInfoEnabled
                    = !accountID.getAccountPropertyBoolean(
                        ProtocolProviderServiceJabberImpl
                            .IS_SERVER_STORED_INFO_DISABLED_PROPERTY, false);

                if (isServerStoredInfoEnabled)
                {
                    createContactPhotoPresenceListener();

                    // we cannot manipulate our vcards when using anonymous
                    if(!accountID.isAnonymousAuthUsed())
                    {
                        createAccountPhotoPresenceInterceptor();
                    }
                }
            }
            else if(evt.getNewState() == RegistrationState.UNREGISTERING)
            {
                clearConnectionListeners();
            }
            else if(evt.getNewState() == RegistrationState.UNREGISTERED
                 || evt.getNewState() == RegistrationState.AUTHENTICATION_FAILED
                 || evt.getNewState() == RegistrationState.CONNECTION_FAILED)
            {
                //since we are disconnected, we won't receive any further status
                //updates so we need to change by ourselves our own status as
                //well as set to offline all contacts in our contact list that
                //were online
                PresenceStatus oldStatus = currentStatus;
                currentStatus = parentProvider.getJabberStatusEnum().getStatus(
                    JabberStatusEnum.OFFLINE);
                clearLocalContactResources();

                fireProviderStatusChangeEvent(oldStatus, currentStatus);

                ssContactList.cleanup();

                clearConnectionListeners();
            }
        }
    }

    /**
     * Clear all listeners that depends on XMPPConnection.
     */
    private void clearConnectionListeners()
    {
        XMPPConnection connection = parentProvider.getConnection();
        if(connection != null
            && subscribtionPacketListener != null
            && contactChangesListener != null)
        {
            connection.removeAsyncStanzaListener(
                subscribtionPacketListener);
            Roster.getInstanceFor(connection)
                .removeRosterListener(contactChangesListener);

            subscribtionPacketListener = null;
            contactChangesListener = null;
        }
    }

    /**
     * Updates the resources for the contact.
     * @param contact the contact which resources to update.
     * @param removeUnavailable whether to remove unavailable resources.
     * @return whether resource has been updated
     */
    private boolean updateResources(ContactJabberImpl contact,
                                 boolean removeUnavailable)
    {
        if (!contact.isResolved()
            || (contact instanceof VolatileContactJabberImpl
                && ((VolatileContactJabberImpl)contact)
                        .isPrivateMessagingContact()))
            return false;

        boolean eventFired = false;
        Map<FullJid, ContactResourceJabberImpl> resources =
            contact.getResourcesMap();

        // Do not obtain getRoster if we are not connected, or new Roster
        // will be created, all the resources that will be returned will be
        // unavailable. As we are not connected if set remove all resources
        if( parentProvider.getConnection() == null
            || !parentProvider.getConnection().isConnected())
        {
            if(removeUnavailable)
            {
                Iterator<Map.Entry<FullJid, ContactResourceJabberImpl>>
                    iter = resources.entrySet().iterator();
                while(iter.hasNext())
                {
                    Map.Entry<FullJid, ContactResourceJabberImpl> entry
                        = iter.next();

                    iter.remove();

                    contact.fireContactResourceEvent(
                        new ContactResourceEvent(contact, entry.getValue(),
                            ContactResourceEvent.RESOURCE_REMOVED));
                    eventFired = true;
                }
            }
            return eventFired;
        }


        // Choose the resource which has the highest priority AND supports
        // Jingle, if we have two resources with same priority take
        // the most available.
        Roster r = Roster.getInstanceFor(parentProvider.getConnection());
        BareJid bareJid = contact.getAddressAsJid().asBareJid();
        for (Presence presence : r.getPresences(bareJid))
        {
            eventFired = updateResource(contact, null, presence) || eventFired;
        }

        if(!removeUnavailable)
            return eventFired;

        for (FullJid fullJid : resources.keySet())
        {
            if(!r.getPresenceResource(fullJid).isAvailable())
            {
                eventFired = removeResource(contact, fullJid) || eventFired;
            }
        }

        return eventFired;
    }

    /**
     * Update the resources for the contact for the received presence.
     * @param contact the contact which resources to update.
     * @param fullJid the full jid to use, if null will use those from the
     * presence packet
     * @param presence the presence packet to use to get info.
     * @return whether resource has been updated
     */
    private boolean updateResource(ContactJabberImpl contact,
                                   FullJid fullJid,
                                   Presence presence)
    {

        if(fullJid == null)
            fullJid = presence.getFrom().asFullJidIfPossible();
        if (fullJid == null)
            return false;

        Resourcepart resource = fullJid.getResourceOrNull();
        if (resource != null && resource.length() > 0)
        {
            Map<FullJid, ContactResourceJabberImpl> resources =
                contact.getResourcesMap();

            ContactResourceJabberImpl contactResource
                = resources.get(fullJid);

            PresenceStatus newPresenceStatus
                = OperationSetPersistentPresenceJabberImpl
                    .jabberStatusToPresenceStatus(presence, parentProvider);

            if (contactResource == null)
            {
                contactResource = createResource(presence, fullJid, contact);

                resources.put(fullJid, contactResource);

                contact.fireContactResourceEvent(
                    new ContactResourceEvent(contact, contactResource,
                        ContactResourceEvent.RESOURCE_ADDED));
                return true;
            }
            else
            {
                boolean oldIndicator = contactResource.isMobile();
                boolean newIndicator =
                    mobileIndicator.isMobileResource(fullJid);
                int oldPriority = contactResource.getPriority();

                // update mobile indicator, as cabs maybe added after
                // creating the resource for the contact
                contactResource.setMobile(newIndicator);

                contactResource.setPriority(presence.getPriority());
                if(oldPriority != contactResource.getPriority())
                {
                    // priority has been updated so update and the
                    // mobile indicator before firing an event
                    mobileIndicator.resourcesUpdated(contact);
                }

                if (contactResource.getPresenceStatus().getStatus()
                        != newPresenceStatus.getStatus()
                    || (oldIndicator != newIndicator)
                    || (oldPriority != contactResource.getPriority()))
                {
                    contactResource.setPresenceStatus(newPresenceStatus);

                    contact.fireContactResourceEvent(
                        new ContactResourceEvent(contact, contactResource,
                            ContactResourceEvent.RESOURCE_MODIFIED));
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Removes the resource indicated by the fullJid from the list with
     * resources for the contact.
     * @param contact from its list of resources to remove
     * @param fullJid the full jid.
     * @return whether resource has been updated
     */
    private boolean removeResource(ContactJabberImpl contact, FullJid fullJid)
    {
        Map<FullJid, ContactResourceJabberImpl> resources =
            contact.getResourcesMap();

        if (resources.containsKey(fullJid))
        {
            ContactResource removedResource = resources.remove(fullJid);

            contact.fireContactResourceEvent(
                new ContactResourceEvent(contact, removedResource,
                    ContactResourceEvent.RESOURCE_REMOVED));
            return true;
        }

        return false;
    }

    /**
     * Fires the status change, respecting resource priorities.
     * @param presence the presence changed.
     */
    void firePresenceStatusChanged(Presence presence)
    {
        if(contactChangesListener != null)
            contactChangesListener.firePresenceStatusChanged(presence);
    }

    /**
     * Updates contact status and its resources, fires PresenceStatusChange
     * events.
     *
     * @param contact the contact which presence to update if needed.
     * @param newStatus the new status.
     */
    private void updateContactStatus(
        ContactJabberImpl contact, PresenceStatus newStatus)
    {
        // When status changes this may be related to a change in the
        // available resources.
        boolean oldMobileIndicator = contact.isMobile();
        boolean resourceUpdated = updateResources(contact, true);
        mobileIndicator.resourcesUpdated(contact);

        PresenceStatus oldStatus
            = contact.getPresenceStatus();

        // when old and new status are the same do nothing
        // no change
        if(oldStatus.equals(newStatus)
            && oldMobileIndicator == contact.isMobile())
        {
            return;
        }

        contact.updatePresenceStatus(newStatus);

        if (logger.isDebugEnabled())
            logger.debug("Will Dispatch the contact status event.");

        fireContactPresenceStatusChangeEvent(
            contact, contact.getParentContactGroup(),
            oldStatus, newStatus,
            resourceUpdated);
    }

    /**
     * Manage changes of statuses by resource.
     */
    class ContactChangesListener
        implements RosterListener
    {
        /**
         * Store events for later processing, used when
         * initializing contactlist.
         */
        private boolean storeEvents = false;

        /**
         * Stored presences for later processing.
         */
        private List<Presence> storedPresences = null;

        /**
         * Map containing all statuses for a userID.
         */
        private final Map<Jid, TreeSet<Presence>> statuses =
            new Hashtable<>();

        /**
         * Not used here.
         * @param addresses list of addresses added
         */
        @Override
        public void entriesAdded(Collection<Jid> addresses)
        {}

        /**
         * Not used here.
         * @param addresses list of addresses updated
         */
        @Override
        public void entriesUpdated(Collection<Jid> addresses)
        {}

        /**
         * Not used here.
         * @param addresses list of addresses deleted
         */
        @Override
        public void entriesDeleted(Collection<Jid> addresses)
        {}

        /**
         * Received on resource status change.
         * @param presence presence that has changed
         */
        @Override
        public void presenceChanged(Presence presence)
        {
            firePresenceStatusChanged(presence);
        }

        /**
         * Whether listener is currently storing presence events.
         * @return true or false
         */
        boolean isStoringPresenceEvents()
        {
            return storeEvents;
        }

        /**
         * Adds presence packet to the list.
         * @param presence presence packet
         */
        void addPresenceEvent(Presence presence)
        {
            storedPresences.add(presence);
        }

        /**
         * Sets store events to true.
         */
        void storeEvents()
        {
            this.storedPresences = new ArrayList<Presence>();
            this.storeEvents = true;
        }

        /**
         * Process stored presences.
         */
        void processStoredEvents()
        {
            storeEvents = false;
            for(Presence p : storedPresences)
            {
                firePresenceStatusChanged(p);
            }
            storedPresences.clear();
            storedPresences = null;
        }

        /**
         * Fires the status change, respecting resource priorities.
         *
         * @param presence the presence changed.
         */
        void firePresenceStatusChanged(Presence presence)
        {
            if(storeEvents && storedPresences != null)
            {
                storedPresences.add(presence);
                return;
            }

            try
            {
                Jid userID = presence.getFrom().asBareJid();
                OperationSetMultiUserChat mucOpSet =
                    parentProvider.getOperationSet(
                        OperationSetMultiUserChat.class);
                if(mucOpSet != null)
                {
                    List<ChatRoom> chatRooms
                        = mucOpSet.getCurrentlyJoinedChatRooms();
                    for(ChatRoom chatRoom : chatRooms)
                    {
                        if(chatRoom.getName().equals(userID.toString()))
                        {
                            userID = presence.getFrom();
                            break;
                        }
                    }
                }

                if (logger.isDebugEnabled())
                    logger.debug("Received a status update for buddy=" + userID);

                // all contact statuses that are received from all its resources
                // ordered by priority(higher first) and those with equal
                // priorities order with the one that is most connected as
                // first
                TreeSet<Presence> userStats = statuses.get(userID);
                if(userStats == null)
                {
                    userStats = new TreeSet<>(new Comparator<Presence>()
                     {
                        public int compare(Presence o1, Presence o2)
                        {
                            int res = o2.getPriority() - o1.getPriority();

                            // if statuses are with same priorities
                            // return which one is more available
                            // counts the JabberStatusEnum order
                            if(res == 0)
                            {
                                res = jabberStatusToPresenceStatus(
                                        o2, parentProvider).getStatus()
                                      - jabberStatusToPresenceStatus(
                                            o1, parentProvider).getStatus();
                                // We have run out of "logical" ways to order
                                // the presences inside the TreeSet. We have
                                // make sure we are consinstent with equals.
                                // We do this by comparing the unique resource
                                // names. If this evaluates to 0 again, then we
                                // can safely assume this presence object
                                // represents the same resource and by that the
                                // same client.
                                if(res == 0)
                                {
                                    res = o1.getFrom().compareTo(
                                        o2.getFrom());
                                }
                            }

                            return res;
                        }
                    });
                    statuses.put(userID, userStats);
                }
                else
                {
                    Resourcepart resource = presence.getFrom().getResourceOrEmpty();

                    // remove the status for this resource
                    // if we are online we will update its value with the new
                    // status
                    for (Iterator<Presence> iter = userStats.iterator();
                            iter.hasNext();)
                    {
                        Presence p = iter.next();
                        if (p.getFrom().getResourceOrEmpty().equals(resource))
                        {
                            iter.remove();
                        }
                    }
                }

                if(!jabberStatusToPresenceStatus(presence, parentProvider)
                        .equals(
                            parentProvider
                                .getJabberStatusEnum()
                                    .getStatus(JabberStatusEnum.OFFLINE)))
                {
                    userStats.add(presence);
                }

                Presence currentPresence;
                if (userStats.size() == 0)
                {
                    currentPresence = presence;

                    /*
                     * We no longer have statuses for userID so it doesn't make
                     * sense to retain (1) the TreeSet and (2) its slot in the
                     * statuses Map.
                     */
                    statuses.remove(userID);
                }
                else
                    currentPresence = userStats.first();

                ContactJabberImpl sourceContact
                    = ssContactList.findContactById(userID);

                if (sourceContact == null)
                {
                    logger.warn("No source contact found for id=" + userID);
                    return;
                }

                // statuses may be the same and only change in status message
                sourceContact.setStatusMessage(currentPresence.getStatus());

                updateContactStatus(
                    sourceContact,
                    jabberStatusToPresenceStatus(
                        currentPresence, parentProvider));
            }
            catch (IllegalStateException | IllegalArgumentException ex)
            {
                logger.error("Failed changing status", ex);
            }
        }
    }

    /**
     * Listens for subscription events coming from stack.
     */
    private class JabberSubscriptionListener
        implements StanzaListener
    {
        /**
         * The authorization handler.
         */
        private AuthorizationHandler handler = null;

        /**
         * List of early subscriptions.
         */
        private Map<Jid, String> earlySubscriptions
            = new HashMap<Jid, String>();

        /**
         * Adds auth handler.
         *
         * @param handler the handler to add.
         */
        private synchronized void setHandler(AuthorizationHandler handler)
        {
            this.handler = handler;
            handleEarlySubscribeReceived();
        }

        /**
         * Process packets.
         * @param packet packet received to be processed
         */
        @Override
        public void processStanza(Stanza packet)
        {
            Presence presence = (Presence) packet;

            if (presence == null)
                return;

            Presence.Type presenceType = presence.getType();
            final Jid fromID = presence.getFrom();

            if (presenceType == Presence.Type.subscribe)
            {
                String displayName = null;
                Nick ext = (Nick) presence.getExtension(Nick.NAMESPACE);
                if(ext != null)
                    displayName = ext.getName();

                synchronized(this)
                {
                    if(handler == null)
                    {
                        earlySubscriptions.put(fromID, displayName);

                        // nothing to handle
                        return;
                    }
                }

                handleSubscribeReceived(fromID, displayName);
            }
            else if (presenceType == Presence.Type.unsubscribed)
            {
                if (logger.isTraceEnabled())
                    logger.trace(fromID + " does not allow your subscription");

                if(handler == null)
                {
                    logger.warn(
                        "No to handle unsubscribed AuthorizationHandler for "
                            + fromID);
                    return;
                }

                ContactJabberImpl contact
                    = ssContactList.findContactById(fromID);

                if(contact != null)
                {
                    AuthorizationResponse response
                        = new AuthorizationResponse(
                                AuthorizationResponse.REJECT,
                                "");

                    handler.processAuthorizationResponse(response, contact);
                    try{
                        ssContactList.removeContact(contact);
                    }
                    catch(OperationFailedException e)
                    {
                        logger.error(
                                "Cannot remove contact that unsubscribed.");
                    }
                }
            }
            else if (presenceType == Presence.Type.subscribed)
            {
                if(handler == null)
                {
                    logger.warn(
                        "No AuthorizationHandler to handle subscribed for "
                            + fromID);
                    return;
                }

                ContactJabberImpl contact
                    = ssContactList.findContactById(fromID);
                AuthorizationResponse response
                    = new AuthorizationResponse(
                            AuthorizationResponse.ACCEPT,
                            "");

                handler.processAuthorizationResponse(response, contact);
            }
            else if (presenceType == Presence.Type.available
                    && contactChangesListener != null
                    && contactChangesListener.isStoringPresenceEvents())
            {
                contactChangesListener.addPresenceEvent(presence);
            }
        }

        /**
         * Handles early presence subscribe that were received.
         */
        private void handleEarlySubscribeReceived()
        {
            for(Jid from : earlySubscriptions.keySet())
            {
                handleSubscribeReceived(from, earlySubscriptions.get(from));
            }

            earlySubscriptions.clear();
        }

        /**
         * Handles receiving a presence subscribe
         * @param fromID sender
         */
        private void handleSubscribeReceived(final Jid fromID,
            final String displayName)
        {
            // run waiting for user response in different thread
            // as this seems to block the packet dispatch thread
            // and we don't receive anything till we unblock it
            new Thread(new Runnable() {
            public void run()
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace(
                            fromID
                                + " wants to add you to its contact list");
                }

                // buddy want to add you to its roster
                ContactJabberImpl srcContact
                    = ssContactList.findContactById(fromID);

                Presence.Type responsePresenceType = null;

                if(srcContact == null)
                {
                    srcContact = createVolatileContact(fromID, displayName);
                }
                else
                {
                    if(srcContact.isPersistent())
                        responsePresenceType = Presence.Type.subscribed;
                }

                if(responsePresenceType == null)
                {
                    AuthorizationRequest req = new AuthorizationRequest();
                    AuthorizationResponse response
                        = handler.processAuthorisationRequest(
                                        req, srcContact);

                    if(response != null)
                    {
                        if(response.getResponseCode()
                               .equals(AuthorizationResponse.ACCEPT))
                        {
                            responsePresenceType
                                = Presence.Type.subscribed;
                            if (logger.isInfoEnabled())
                                logger.info(
                                    "Sending Accepted Subscription");
                        }
                        else if(response.getResponseCode()
                                .equals(AuthorizationResponse.REJECT))
                        {
                            responsePresenceType
                                = Presence.Type.unsubscribed;
                            if (logger.isInfoEnabled())
                                logger.info(
                                    "Sending Rejected Subscription");
                        }
                    }
                }

                // subscription ignored
                if(responsePresenceType == null)
                    return;

                Presence responsePacket = new Presence(
                        responsePresenceType);

                responsePacket.setTo(fromID);
                try
                {
                    parentProvider.getConnection().sendStanza(responsePacket);
                }
                catch (NotConnectedException | InterruptedException e)
                {
                    logger.error("Could not send presence repsonse", e);
                }
            }}).start();
        }
    }

    /**
     * Runnable that resolves our list against the server side roster.
     * This thread is the one which will call getRoster for the first time.
     * And if roster is currently processing will wait for it (the wait
     * is internal into XMPPConnection.getRoster method).
     */
    private class ServerStoredListInit
        implements Runnable,
                   StanzaListener
    {
        public void run()
        {
            // we are already notified lets remove us from the packet
            // listener
            parentProvider.getConnection()
                .removeAsyncStanzaListener(this);

            // init ssList
            ssContactList.init(contactChangesListener);

            // as we have dispatched the contact list and Roster is ready
            // lets start the jingle nodes discovery
            parentProvider.startJingleNodesDiscovery();
        }

        /**
         * When roster packet with no error is received we are ready to
         * to dispatch the contact list, doing it in different thread
         * to avoid blocking xmpp packet receiving.
         * @param packet the roster packet
         */
        public void processStanza(Stanza packet)
        {
            // don't process packets that are errors
            if(packet.getError() != null)
            {
                return;
            }

            new Thread(this, getClass().getName()).start();
        }
    }

    /**
     * Creates an interceptor which modifies presence packet in order to add the
     * the element name "x" and the namespace "vcard-temp:x:update" in order to
     * advertise the avatar SHA-1 hash.
     */
    public void createAccountPhotoPresenceInterceptor()
    {
        // Verifies that we creates only one interceptor of this type.
        if(this.vCardTempXUpdatePresenceExtension != null)
        {
            return;
        }

        byte[] avatar = null;
        try
        {
            // Retrieves the current server avatar.
            VCardManager manager = VCardManager.getInstanceFor(
                parentProvider.getConnection());
            VCard vCard = manager.loadVCard();
            avatar = vCard.getAvatar();
        }
        catch(XMPPException
            | InterruptedException
            | NotConnectedException
            | NoResponseException ex)
        {
            logger.info("Can not retrieve account avatar for "
                + parentProvider.getOurJID() + ": " + ex.getMessage());
        }

        // Creates the presence extension to generates the  the element
        // name "x" and the namespace "vcard-temp:x:update" containing
        // the avatar SHA-1 hash.
        this.vCardTempXUpdatePresenceExtension =
            new VCardTempXUpdatePresenceExtension(avatar);

        // Intercepts all sent presence packet in order to add the
        // photo tag.
        parentProvider.getConnection().addStanzaInterceptor(
            new VCardTempXUpdateInterceptor(vCardTempXUpdatePresenceExtension),
            new StanzaTypeFilter(Presence.class));
    }

    /**
     * Updates the presence extension to advertise a new photo SHA-1 hash
     * corresponding to the new avatar given in parameter.
     *
     * @param imageBytes The new avatar set for this account.
     */
    public void updateAccountPhotoPresenceExtension(byte[] imageBytes)
    {
        try
        {
            // If the image has changed, then updates the presence extension and
            // send immediately a presence packet to advertise the photo update.
            if(this.vCardTempXUpdatePresenceExtension.updateImage(imageBytes))
            {
                this.publishPresenceStatus(currentStatus, currentStatusMessage);
            }
        }
        catch(OperationFailedException ex)
        {
            logger.info(
                    "Can not send presence extension to broadcast photo update",
                    ex);
        }
    }

    /**
     * Creates a listener to call a parser which manages presence packets with
     * the element name "x" and the namespace "vcard-temp:x:update".
     */
    public void createContactPhotoPresenceListener()
    {
        // Registers the listener.
        parentProvider.getConnection().addAsyncStanzaListener(
            new StanzaListener()
            {
                @Override
                public void processStanza(Stanza packet)
                {
                    // Calls the parser to manages this presence packet.
                    parseContactPhotoPresence(packet);
                }
            },
            // Creates a filter to only listen to presence packet with the
            // element name "x" and the namespace "vcard-temp:x:update".
            new AndFilter(new StanzaTypeFilter(Presence.class),
                new StanzaExtensionFilter(
                    VCardTempXUpdatePresenceExtension.ELEMENT,
                    VCardTempXUpdatePresenceExtension.NAMESPACE)
                )
            );
    }

    /**
     * Parses a contact presence packet with the element name "x" and the
     * namespace "vcard-temp:x:update", in order to decide if the SHA-1 avatar
     * contained in the photo tag represents a new avatar for this contact.
     *
     * @param packet The packet received to parse.
     */
    public void parseContactPhotoPresence(Stanza packet)
    {
        // Retrieves the contact ID and its avatar that Jitsi currently
        // managed concerning the peer that has send this presence packet.
        EntityBareJid userID = packet.getFrom().asEntityBareJidOrThrow();
        ContactJabberImpl sourceContact
            = ssContactList.findContactById(userID);

        // If this contact is not yet in our contact list, then there is no need
        // to manage this photo update.
        if(sourceContact == null)
        {
            return;
        }

        byte[] currentAvatar = sourceContact.getImage(false);

        // Get the packet extension which contains the photo tag.
        StandardExtensionElement defaultPacketExtension =
            packet.getExtension(
                    VCardTempXUpdatePresenceExtension.ELEMENT,
                    VCardTempXUpdatePresenceExtension.NAMESPACE);
        if(defaultPacketExtension == null)
        {
            return;
        }
        try
        {
            StandardExtensionElement photoElement
                = defaultPacketExtension.getFirstElement("photo");
            if (photoElement == null)
            {
                return;
            }

            // If this presence packet has a photo tag with a SHA-1 hash
            // which differs from the current avatar SHA-1 hash, then Jitsi
            // retrieves the new avatar image and updates this contact image
            // in the contact list.
            String packetPhotoSHA1 = photoElement.getText();
            if(packetPhotoSHA1 != null
                    && !packetPhotoSHA1.equals(
                        VCardTempXUpdatePresenceExtension.getImageSha1(
                            currentAvatar))
              )
            {
                byte[] newAvatar = null;

                // If there is an avatar image, retrieves it.
                if(packetPhotoSHA1.length() != 0)
                {
                    // Retrieves the new contact avatar image.
                    VCardManager manager = VCardManager.getInstanceFor(
                        parentProvider.getConnection());
                    VCard vCard = manager.loadVCard(userID);
                    newAvatar = vCard.getAvatar();
                }
                // Else removes the current avatar image, since the contact
                // has removed it from the server.
                else
                {
                    newAvatar = new byte[0];
                }

                // Sets the new avatar image to the Jitsi contact.
                sourceContact.setImage(newAvatar);
                // Fires a property change event to update the contact list.
                this.fireContactPropertyChangeEvent(
                    ContactPropertyChangeEvent.PROPERTY_IMAGE,
                    sourceContact,
                    currentAvatar,
                    newAvatar);
            }
        }
        catch(XMPPException
            | InterruptedException
            | NotConnectedException
            | NoResponseException ex)
        {
            logger.info("Cannot retrieve vCard from: " + packet.getFrom());
            if(logger.isTraceEnabled())
                logger.trace("vCard retrieval exception was: ", ex);
        }
    }

    /**
     * Initializes the map with priorities and statuses which we will use when
     * changing statuses.
     */
    private void initializePriorities()
    {
        try
        {
            this.resourcePriorityAvailable =
                Integer.parseInt(parentProvider.getAccountID()
                    .getAccountPropertyString(
                        ProtocolProviderFactory.RESOURCE_PRIORITY));
        }
        catch(NumberFormatException ex)
        {
            logger.error("Wrong value for resource priority", ex);
        }

        addDefaultValue(JabberStatusEnum.AWAY, -5);
        addDefaultValue(JabberStatusEnum.EXTENDED_AWAY, -10);
        addDefaultValue(JabberStatusEnum.ON_THE_PHONE, -15);
        addDefaultValue(JabberStatusEnum.IN_A_MEETING, -16);
        addDefaultValue(JabberStatusEnum.DO_NOT_DISTURB, -20);
        addDefaultValue(JabberStatusEnum.FREE_FOR_CHAT, +5);
    }

    /**
     * Checks for account property that can override this status.
     * If missing use the shift value to create the priority to use, make sure
     * it is not zero or less than it.
     * @param statusName the status to check/create priority
     * @param availableShift the difference from available resource
     *                       value to use.
     */
    private void addDefaultValue(String statusName, int availableShift)
    {
        String resourcePriority = getAccountPriorityForStatus(statusName);
        if(resourcePriority != null)
        {
            try
            {
                addPresenceToPriorityMapping(
                    statusName,
                    Integer.parseInt(resourcePriority));
            }
            catch(NumberFormatException ex)
            {
                logger.error(
                    "Wrong value for resource priority for status: "
                        + statusName, ex);
            }
        }
        else
        {
            // if priority is less than zero, use the available priority
            int priority = resourcePriorityAvailable + availableShift;
            if(priority <= 0)
                priority = resourcePriorityAvailable;

            addPresenceToPriorityMapping(statusName, priority);
        }
    }

    /**
     * Adds the priority mapping for the <tt>statusName</tt>.
     * Make sure we replace ' ' with '_' and use upper case as this will be
     * and the property names used in account properties that can override
     * this values.
     * @param statusName the status name to use
     * @param value and its priority
     */
    private static void addPresenceToPriorityMapping(String statusName,
                                                     int value)
    {
        statusToPriorityMappings.put(
            statusName.replaceAll(" ", "_").toUpperCase(), value);
    }

    /**
     * Returns the priority which will be used for <tt>statusName</tt>.
     * Make sure we replace ' ' with '_' and use upper case as this will be
     * and the property names used in account properties that can override
     * this values.
     * @param statusName the status name
     * @return the priority which will be used for <tt>statusName</tt>.
     */
    private int getPriorityForPresenceStatus(String statusName)
    {
        Integer priority = statusToPriorityMappings.get(
                                statusName.replaceAll(" ", "_").toUpperCase());
        if(priority == null)
            return resourcePriorityAvailable;

        return priority;
    }

    /**
     * Returns the account property value for a status name, if missing return
     * null.
     * Make sure we replace ' ' with '_' and use upper case as this will be
     * and the property names used in account properties that can override
     * this values.
     * @param statusName
     * @return the account property value for a status name, if missing return
     * null.
     */
    private String getAccountPriorityForStatus(String statusName)
    {
        return parentProvider.getAccountID().getAccountPropertyString(
                    ProtocolProviderFactory.RESOURCE_PRIORITY + "_" +
                        statusName.replaceAll(" ", "_").toUpperCase());
    }

    /**
     * Returns the contactlist impl.
     * @return
     */
    public ServerStoredContactListJabberImpl getSsContactList()
    {
        return ssContactList;
    }
}
