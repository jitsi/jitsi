/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.jabberconstants.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.StringUtils; // disambiguation
import org.jivesoftware.smackx.packet.*;

/**
 * The Jabber implementation of a Persistent Presence Operation set. This class
 * manages our own presence status as well as subscriptions for the presence
 * status of our buddies. It also offers methods for retrieving and modifying
 * the buddy contact list and adding listeners for changes in its layout.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class OperationSetPersistentPresenceJabberImpl
    extends AbstractOperationSetPersistentPresence<
                ProtocolProviderServiceJabberImpl>
{
    /**
     * The logger.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetPersistentPresenceJabberImpl.class);

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
     * A map containing bindings between SIP Communicator's jabber presence
     * status instances and Jabber status codes
     */
    private static Map<String, Presence.Mode> scToJabberModesMappings
        = new Hashtable<String, Presence.Mode>();

    static
    {
        scToJabberModesMappings.put(JabberStatusEnum.AWAY,
                                  Presence.Mode.away);
        scToJabberModesMappings.put(JabberStatusEnum.ON_THE_PHONE,
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
     * The server stored contact list that will be encapsulating smack's
     * buddy list.
     */
    private final ServerStoredContactListJabberImpl ssContactList;

    /**
     * Listens for subscriptions.
     */
    private JabberSubscriptionListener subscribtionPacketListener = null;

    /**
     * Current resource priority. 10 is default value.
     */
    private int resourcePriority = 10;

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

        ssContactList = new ServerStoredContactListJabberImpl(
            this , provider, infoRetreiver);

        parentProvider.addRegistrationStateChangeListener(
            new RegistrationStateListener());
    }

    /**
     * Registers a listener that would receive events upon changes in server
     * stored groups.
     *
     * @param listener a ServerStoredGroupChangeListener impl that would
     *   receive events upon group changes.
     */
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
    public ContactJabberImpl createVolatileContact(String id)
    {
        return ssContactList.createVolatileContact(id);
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

        ContactJabberImpl contact =
            ssContactList.createUnresolvedContact(parentGroup, address);

        contact.setPersistentData(persistentData);

        return contact;
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
        return ssContactList.findContactById(contactID);
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
        return null;
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
     * Removes the specified contact from its current parent and places it
     * under <tt>newParent</tt>.
     *
     * @param contactToMove the <tt>Contact</tt> to move
     * @param newParent the <tt>ContactGroup</tt> where <tt>Contact</tt>
     *   would be placed.
     */
    public void moveContactToGroup(Contact contactToMove,
                                   ContactGroup newParent)
    {
        assertConnected();

        if( !(contactToMove instanceof ContactJabberImpl) )
            throw new IllegalArgumentException(
                "The specified contact is not an jabber contact." +
                contactToMove);
        if( !(newParent instanceof ContactGroupJabberImpl) )
            throw new IllegalArgumentException(
                "The specified group is not an jabber contact group."
                + newParent);

        ssContactList.moveContact((ContactJabberImpl)contactToMove,
                                  (ContactGroupJabberImpl)newParent);
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

        if (status.equals(jabberStatusEnum.getStatus(JabberStatusEnum.OFFLINE)))
        {
            parentProvider.unregister();
        }
        else
        {
            Presence presence = new Presence(Presence.Type.available);
            presence.setMode(presenceStatusToJabberMode(status));
            presence.setPriority(resourcePriority);

            // on the phone is a special status which is away
            // with custom status message
            if(status.equals(jabberStatusEnum.getStatus(
                    JabberStatusEnum.ON_THE_PHONE)))
            {
                presence.setStatus(JabberStatusEnum.ON_THE_PHONE);
            }
            else
                presence.setStatus(statusMessage);
            //presence.addExtension(new Version());

            parentProvider.getConnection().sendPacket(presence);
        }

        fireProviderStatusChangeEvent(currentStatus, status);

        if(!getCurrentStatusMessage().equals(statusMessage))
        {
            String oldStatusMessage = getCurrentStatusMessage();
            currentStatusMessage = statusMessage;
            fireProviderStatusMessageChangeEvent(oldStatusMessage,
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

        Presence presence
            = xmppConnection.getRoster().getPresence(contactIdentifier);

        if(presence != null)
            return jabberStatusToPresenceStatus(presence, parentProvider);
        else
        {
            return
                parentProvider.getJabberStatusEnum().getStatus(
                        JabberStatusEnum.OFFLINE);
        }
    }

    /**
     * Removes the specified group from the server stored contact list.
     *
     * @param group the group to remove.
     */
    public void removeServerStoredContactGroup(ContactGroup group)
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
        if(subscribtionPacketListener == null)
        {
            subscribtionPacketListener = new JabberSubscriptionListener();
            parentProvider
                .getConnection()
                    .addPacketListener(
                        subscribtionPacketListener,
                        new PacketTypeFilter(Presence.class));
        }

        subscribtionPacketListener.handler = handler;
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
        JabberStatusEnum jabberStatusEnum =
            jabberProvider.getJabberStatusEnum();
        // fixing issue: 336
        // from the smack api :
        // A null presence mode value is interpreted to be the same thing
        // as Presence.Mode.available.
        if(presence.getMode() == null && presence.isAvailable())
            return jabberStatusEnum.getStatus(JabberStatusEnum.AVAILABLE);
        else if(presence.getMode() == null && !presence.isAvailable())
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

                        PresenceStatus oldContactStatus
                            = contact.getPresenceStatus();

                        if(!oldContactStatus.isOnline())
                            continue;

                        contact.updatePresenceStatus(offlineStatus);

                        fireContactPresenceStatusChangeEvent(
                            contact,
                            contact.getParentContactGroup(),
                            oldContactStatus,
                            offlineStatus);
                    }
                }

                //do the same for all contacts in the root group
                Iterator<Contact> contactsIter
                    = getServerStoredContactListRoot().contacts();

                while (contactsIter.hasNext())
                {
                    ContactJabberImpl contact
                        = (ContactJabberImpl) contactsIter.next();

                    PresenceStatus oldContactStatus
                        = contact.getPresenceStatus();

                    if (!oldContactStatus.isOnline())
                        continue;

                    contact.updatePresenceStatus(offlineStatus);

                    fireContactPresenceStatusChangeEvent(
                        contact
                        , contact.getParentContactGroup()
                        , oldContactStatus, offlineStatus);
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
    public void setDisplayName(Contact contact, String newName)
        throws IllegalArgumentException
    {
        assertConnected();

        if(! (contact instanceof ContactJabberImpl) )
            throw new IllegalArgumentException(
                "Argument is not an jabber contact (contact=" + contact + ")");

        ((ContactJabberImpl)contact).getSourceEntry().setName(newName);
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
                // (inside method XMPPConnection.getRoaster())
                parentProvider.getConnection().addPacketListener(
                    new ServerStoredListInit(),
                    new PacketTypeFilter(RosterPacket.class)
                );
            }
            else if(evt.getNewState() == RegistrationState.REGISTERED)
            {
                fireProviderStatusChangeEvent(
                    currentStatus,
                    parentProvider
                        .getJabberStatusEnum()
                            .getStatus(JabberStatusEnum.AVAILABLE));

                createContactPhotoPresenceListener();
                createAccountPhotoPresenceInterceptor();
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
                PresenceStatus offlineStatus =
                    parentProvider.getJabberStatusEnum().getStatus(
                        JabberStatusEnum.OFFLINE);
                currentStatus = offlineStatus;

                fireProviderStatusChangeEvent(oldStatus, currentStatus);

                ssContactList.cleanup();
                subscribtionPacketListener = null;

                if(parentProvider.getConnection() != null &&
                    parentProvider.getConnection().getRoster() != null)
                    parentProvider.getConnection().getRoster()
                        .removeRosterListener(contactChangesListener);
                contactChangesListener = null;
            }
        }
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
        private final Map<String, TreeSet<Presence>> statuses =
            new Hashtable<String, TreeSet<Presence>>();

        /**
         * Not used here.
         * @param addresses list of addresses added
         */
        public void entriesAdded(Collection<String> addresses)
        {}

        /**
         * Not used here.
         * @param addresses list of addresses updated
         */
        public void entriesUpdated(Collection<String> addresses)
        {}

        /**
         * Not used here.
         * @param addresses list of addresses deleted
         */
        public void entriesDeleted(Collection<String> addresses)
        {}

        /**
         * Received on resource status change.
         * @param presence presence that has changed
         */
        public void presenceChanged(Presence presence)
        {
            firePresenceStatusChanged(presence);
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
                String userID
                    = StringUtils.parseBareAddress(presence.getFrom());

                if (logger.isDebugEnabled())
                    logger.debug("Received a status update for buddy=" + userID);

                // all contact statuses that are received from all its resources
                // ordered by priority(higher first) and those with equal
                // priorities order with the one that is most connected as
                // first
                TreeSet<Presence> userStats = statuses.get(userID);
                if(userStats == null)
                {
                    userStats = new TreeSet<Presence>(new Comparator<Presence>()
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
                            }

                            return res;
                        }
                    });
                    statuses.put(userID, userStats);
                }
                else
                {
                    String resource = StringUtils.parseResource(
                            presence.getFrom());

                    // remove the status for this resource
                    // if we are online we will update its value with the new
                    // status
                    for (Iterator<Presence> iter = userStats.iterator();
                            iter.hasNext();)
                    {
                        Presence p = iter.next();

                        if (StringUtils.parseResource(p.getFrom()).equals(
                                resource))
                            iter.remove();
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

                PresenceStatus oldStatus
                    = sourceContact.getPresenceStatus();
                PresenceStatus newStatus
                    = jabberStatusToPresenceStatus(
                            currentPresence,
                            parentProvider);

                // when old and new status are the same do nothing
                // no change
                if(oldStatus.equals(newStatus))
                    return;

                sourceContact.updatePresenceStatus(newStatus);

                ContactGroup parent
                    = ssContactList.findContactGroup(sourceContact);

                if (logger.isDebugEnabled())
                    logger.debug("Will Dispatch the contact status event.");
                fireContactPresenceStatusChangeEvent(sourceContact, parent,
                    oldStatus, newStatus);
            }
            catch (IllegalStateException ex)
            {
                logger.error("Failed changing status", ex);
            }
            catch (IllegalArgumentException ex)
            {
                logger.error("Failed changing status", ex);
            }
        }
    }

    /**
     * Listens for subscription events coming from stack.
     */
    private class JabberSubscriptionListener
        implements PacketListener
    {
        /**
         * The authorization handler.
         */
        AuthorizationHandler handler = null;

        /**
         * Process packets.
         * @param packet packet received to be processed
         */
        public void processPacket(Packet packet)
        {
            Presence presence = (Presence) packet;

            if (presence == null)
                return;

            Presence.Type presenceType = presence.getType();
            final String fromID = presence.getFrom();

            if (presenceType == Presence.Type.subscribe)
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
                        srcContact = createVolatileContact(fromID);
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
                    parentProvider.getConnection().sendPacket(responsePacket);

                }}).start();
            }
            else if (presenceType == Presence.Type.unsubscribed)
            {
                if (logger.isTraceEnabled())
                    logger.trace(fromID + " does not allow your subscription");

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
                ContactJabberImpl contact
                    = ssContactList.findContactById(fromID);
                AuthorizationResponse response
                    = new AuthorizationResponse(
                            AuthorizationResponse.ACCEPT,
                            "");

                handler.processAuthorizationResponse(response, contact);
            }
        }
    }

    /**
     * Returns the jabber account resource priority property value.
     *
     * @return the jabber account resource priority property value
     */
    public int getResourcePriority()
    {
        return resourcePriority;
    }

    /**
     * Updates the jabber account resource priority property value.
     *
     * @param resourcePriority the new priority to set
     */
    public void setResourcePriority(int resourcePriority)
    {
        this.resourcePriority = resourcePriority;
    }

    /**
     * Runnable that resolves our list against the server side roster.
     * This thread is the one which will call getRoaster for the first time.
     * And if roaster is currently processing will wait for it (the wait
     * is internal into XMPPConnection.getRoaster method).
     */
    private class ServerStoredListInit
        implements Runnable,
                   PacketListener
    {
        public void run()
        {
            // we are already notified lets remove us from the packet
            // listener
            parentProvider.getConnection()
                .removePacketListener(this);

            // init ssList
            contactChangesListener = new ContactChangesListener();
            ssContactList.init(contactChangesListener);

            // as we have dispatched the contact list and Roaster is ready
            // lets start the jingle nodes discovery
            parentProvider.startJingleNodesDiscovery();
        }

        /**
         * When roaster packet with no error is received we are ready to
         * to dispatch the contact list, doing it in different thread
         * to avoid blocking xmpp packet receiving.
         * @param packet the roaster packet
         */
        public void processPacket(Packet packet)
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
        if(this.vCardTempXUpdatePresenceExtension == null)
        {
            byte[] avatar = null;
            try
            {
                // Retrieves the current server avatar.
                VCard vCard = new VCard();
                vCard.load(parentProvider.getConnection());
                avatar = vCard.getAvatar();
            }
            catch(XMPPException ex)
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
            parentProvider.getConnection().addPacketInterceptor(
                    this.vCardTempXUpdatePresenceExtension,
                    new PacketTypeFilter(Presence.class));
        }
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
        parentProvider.getConnection().addPacketListener(
            new PacketListener()
            { 
                public void processPacket(Packet packet)
                {
                    // Calls the parser to manages this presence packet.
                    parseContactPhotoPresence(packet);
                }
            },
            // Creates a filter to only listen to presence packet with the
            // element name "x" and the namespace "vcard-temp:x:update".
            new AndFilter(new PacketTypeFilter(Presence.class),
                new PacketExtensionFilter(
                    VCardTempXUpdatePresenceExtension.ELEMENT_NAME,
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
    public void parseContactPhotoPresence(Packet packet)
    {
        // Retrieves the contact ID and its avatar that Jitsi currently
        // managed concerning the peer that has send this presence packet.
        String userID
            = StringUtils.parseBareAddress(packet.getFrom());
        ContactJabberImpl sourceContact
            = ssContactList.findContactById(userID);

        /**
         * If this contact is not yet in our contact list, then there is no need
         * to manage this photo update.
         */
        if(sourceContact == null)
        {
            return;
        }

        byte[] currentAvatar = sourceContact.getImage(false);

        // Get the packet extension which contains the photo tag.
        DefaultPacketExtension defaultPacketExtension =
            (DefaultPacketExtension) packet.getExtension(
                    VCardTempXUpdatePresenceExtension.ELEMENT_NAME,
                    VCardTempXUpdatePresenceExtension.NAMESPACE);
        if(defaultPacketExtension != null)
        {
            try
            {
                String packetPhotoSHA1 =
                    defaultPacketExtension.getValue("photo");
                // If this presence packet has a photo tag with a SHA-1 hash
                // which differs from the current avatar SHA-1 hash, then Jitsi
                // retreives the new avatar image and updates this contact image
                // in the contact list.
                if(packetPhotoSHA1 != null
                        && !packetPhotoSHA1.equals(
                            VCardTempXUpdatePresenceExtension.getImageSha1(
                                currentAvatar))
                  )
                {
                    byte[] newAvatar = null;

                    // If there is an avatar image, retreives it. 
                    if(packetPhotoSHA1.length() != 0)
                    {
                        // Retrieves the new contact avatar image.
                        VCard vCard = new VCard();
                        vCard.load(parentProvider.getConnection(), userID);
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
            catch(XMPPException ex)
            {
                logger.info("Can not retrieve vCard from: " + packet.getFrom(), ex);
            }
        }
    }
}
