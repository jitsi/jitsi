/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.version.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.jabberconstants.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.*;

/**
 * The Jabber implementation of a Persistent Presence Operation set. This class
 * manages our own presence status as well as subscriptions for the presence
 * status of our buddies. It also offers methods for retrieving and modifying
 * the buddy contact list and adding listeners for changes in its layout.
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class OperationSetPersistentPresenceJabberImpl
    extends AbstractOperationSetPersistentPresence<
                ProtocolProviderServiceJabberImpl>
{
    private static final Logger logger =
        Logger.getLogger(OperationSetPersistentPresenceJabberImpl.class);

    /**
     * Contains our current status message. Note that this field would only
     * be changed once the server has confirmed the new status message and
     * not immediately upon setting a new one..
     */
    private String currentStatusMessage = "";

    /**
     * The presence status that we were last notified of etnering.
     * The initial one is OFFLINE
     */
    private PresenceStatus currentStatus;

    /**
     * A map containing bindings between SIP Communicator's jabber presence status
     * instances and Jabber status codes
     */
    private static Map<String, Presence.Mode> scToJabberModesMappings
        = new Hashtable<String, Presence.Mode>();
    static{
        scToJabberModesMappings.put(JabberStatusEnum.AWAY,
                                  Presence.Mode.away);
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

    private JabberSubscriptionListener subscribtionPacketListener = null;

    private int resourcePriority = 10;

    public OperationSetPersistentPresenceJabberImpl(
        ProtocolProviderServiceJabberImpl provider)
    {
        super(provider);

        currentStatus =
            parentProvider.getJabberStatusEnum().getStatus(
                JabberStatusEnum.OFFLINE);

        ssContactList = new ServerStoredContactListJabberImpl( this , provider);

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
                "The specified contact is not an jabber contact." + contactToMove);
        if( !(newParent instanceof ContactGroupJabberImpl) )
            throw new IllegalArgumentException(
                "The specified group is not an jabber contact group."
                + newParent);

        ssContactList.moveContact((ContactJabberImpl)contactToMove,
                                  (ContactGroupJabberImpl)newParent);
    }

    /**
     * Requests the provider to enter into a status corresponding to the
     * specified paramters.
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
                                      String statusMessage) throws
        IllegalArgumentException, IllegalStateException,
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
            presence.setStatus(statusMessage);
            presence.addExtension(new Version());

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
     * Get the PresenceStatus for a particular contact.
     *
     * @param contactIdentifier the identifier of the contact whose status
     *   we're interested in.
     * @return PresenceStatus the <tt>PresenceStatus</tt> of the specified
     *   <tt>contact</tt>
     * @throws IllegalArgumentException if <tt>contact</tt> is not a contact
     *   known to the underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is
     *   not registered/signed on a public service.
     * @throws OperationFailedException with code NETWORK_FAILURE if
     *   retrieving the status fails due to errors experienced during
     *   network communication
     */
    public PresenceStatus queryContactStatus(String contactIdentifier) throws
        IllegalArgumentException, IllegalStateException,
        OperationFailedException
    {
        Presence presence = parentProvider.getConnection().getRoster().
                getPresence(contactIdentifier);

        if(presence != null)
            return jabberStatusToPresenceStatus(presence, parentProvider);
        else
            return parentProvider.getJabberStatusEnum().getStatus(
                JabberStatusEnum.OFFLINE);
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
            PacketFilter packetFilter = new PacketTypeFilter(Presence.class);

            parentProvider.getConnection().
                addPacketListener(subscribtionPacketListener, packetFilter);
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
    public void subscribe(ContactGroup parent, String contactIdentifier) throws
        IllegalArgumentException, IllegalStateException,
        OperationFailedException
    {
        assertConnected();

        if(! (parent instanceof ContactGroupJabberImpl) )
            throw new IllegalArgumentException(
                "Argument is not an jabber contact group (group="
                            + parent + ")");

        ssContactList.addContact(
                        (ContactGroupJabberImpl)parent, contactIdentifier);
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
            return jabberStatusEnum.getStatus(JabberStatusEnum.AWAY);
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
    public static Presence.Mode presenceStatusToJabberMode(PresenceStatus status)
    {
        return scToJabberModesMappings.get(status
            .getStatusName());
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     */
    private void assertConnected() throws IllegalStateException
    {
        if (parentProvider == null)
            throw new IllegalStateException(
                "The provider must be non-null and signed on the Jabber "
                +"service before being able to communicate.");
        if (!parentProvider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the Jabber service before "
                +"being able to communicate.");
    }

    public void fireProviderStatusChangeEvent(
        PresenceStatus oldStatus,
        PresenceStatus newStatus)
    {
        if (!oldStatus.equals(newStatus))
        {
            currentStatus = newStatus;

            super.fireProviderStatusChangeEvent(oldStatus, newStatus);
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
         * The method is called by a ProtocolProvider implementation whenver
         * a change in the registration state of the corresponding provider had
         * occurred.
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            logger.debug("The Jabber provider changed state from: "
                         + evt.getOldState()
                         + " to: " + evt.getNewState());

            if(evt.getNewState() == RegistrationState.REGISTERED)
            {
                parentProvider.getConnection().getRoster().addRosterListener(
                    new ContactChangesListener());

                fireProviderStatusChangeEvent(
                    currentStatus,
                    parentProvider
                        .getJabberStatusEnum()
                            .getStatus(JabberStatusEnum.AVAILABLE));

                // init ssList
                ssContactList.init();
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
                              contact
                            , contact.getParentContactGroup()
                            , oldContactStatus, offlineStatus);
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

    private class ContactChangesListener
        implements RosterListener
    {
        private final Map<String, TreeSet<Presence>> statuses =
            new Hashtable<String, TreeSet<Presence>>();

        public void entriesAdded(Collection<String> addresses)
        {}
        public void entriesUpdated(Collection<String> addresses)
        {}
        public void entriesDeleted(Collection<String> addresses)
        {}

        public void presenceChanged(Presence presence)
        {
            try
            {
                String userID =
                    StringUtils.parseBareAddress(presence.getFrom());
                logger.info("Received a status update for buddy=" + userID);

                // all contact statuses that are received from all its resources
                // ordered by priority
                TreeSet<Presence> userStats = statuses.get(userID);
                if(userStats == null)
                {
                    userStats = new TreeSet<Presence>(new Comparator<Presence>(){
                        public int compare(Presence o1, Presence o2)
                        {
                            int res = o1.getPriority() - o2.getPriority();

                            // if statuses are with same priorities
                            // return which one is more available
                            // counts the JabberStatusEnum order
                            if(res == 0)
                            {
                                res =
                                    jabberStatusToPresenceStatus(o1, parentProvider).getStatus() -
                                    jabberStatusToPresenceStatus(o2, parentProvider).getStatus();
                            }

                            return res;
                        }
                    });
                    statuses.put(userID, userStats);
                }
                else
                {
                    String resource = StringUtils.parseResource(presence.getFrom());

                    // remove the status for this resource
                    // if we are online we will update its value with the new status
                    for (Iterator<Presence> iter = userStats.iterator();
                            iter.hasNext();)
                    {
                        Presence p = iter.next();
                        if (StringUtils.parseResource(p.getFrom()).equals(resource))
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
                {
                    currentPresence = userStats.first();
                }

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

                PresenceStatus newStatus =
                    jabberStatusToPresenceStatus(currentPresence, parentProvider);

                // when old and new status are the same do nothing
                // no change
                if(oldStatus.equals(newStatus))
                    return;

                sourceContact.updatePresenceStatus(newStatus);

                ContactGroup parent
                    = ssContactList.findContactGroup(sourceContact);

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

    private class JabberSubscriptionListener
        implements PacketListener
    {
        AuthorizationHandler handler = null;
        public void processPacket(Packet packet)
        {
            Presence presence = (Presence) packet;
            if (presence == null)
                return;

            Presence.Type presenceType = presence.getType();

            if (presenceType == Presence.Type.subscribe)
            {
                logger.trace(presence.getFrom()
                                + " wants to add you to its contact list");
                // buddy want to add you to its roster
                String fromID = presence.getFrom();
                ContactJabberImpl srcContact
                              = ssContactList.findContactById(fromID);

                if(srcContact == null)
                    srcContact = createVolatileContact(fromID);

                AuthorizationRequest req = new AuthorizationRequest();
                AuthorizationResponse response
                    = handler.processAuthorisationRequest(req, srcContact);

                if(response != null
                   && response.getResponseCode()
                           .equals(AuthorizationResponse.ACCEPT))
                {
                    Presence responsePacket
                        = new Presence(Presence.Type.subscribed);
                    responsePacket.setTo(fromID);
                    logger.info("Sending Accepted Subscription");
                    parentProvider.getConnection().sendPacket(responsePacket);
                }
                else
                {
                    Presence responsePacket
                        = new Presence(Presence.Type.unsubscribed);
                    responsePacket.setTo(fromID);
                    logger.info("Sending Rejected Subscription");
                    parentProvider.getConnection().sendPacket(responsePacket);
                }

            }
            else if (presenceType == Presence.Type.unsubscribed)
            {
                logger.trace(presence.getFrom()
                                + " does not allow your subscription");
                ContactJabberImpl contact =
                    ssContactList.findContactById(presence.getFrom());

                if(contact != null)
                {
                    AuthorizationResponse response
                        = new AuthorizationResponse(
                                        AuthorizationResponse.REJECT, "");
                    handler.processAuthorizationResponse(response, contact);

                    ssContactList.removeContact(contact);
                }
            }
            else if (presenceType == Presence.Type.subscribed)
            {
                ContactJabberImpl contact =
                    ssContactList.findContactById(presence.getFrom());

                AuthorizationResponse response = new AuthorizationResponse(
                    AuthorizationResponse.ACCEPT, "");
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
}
