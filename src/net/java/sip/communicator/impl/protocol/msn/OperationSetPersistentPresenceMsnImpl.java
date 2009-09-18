/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.msnconstants.*;
import net.java.sip.communicator.util.*;
import net.sf.jml.*;
import net.sf.jml.event.*;

/**
 * The Msn implementation of a Persistent Presence Operation set. This class
 * manages our own presence status as well as subscriptions for the presence
 * status of our buddies. It also offers methods for retrieving and modifying
 * the buddy contact list and adding listeners for changes in its layout.
 *
 * @author Damian Minkov
 */
public class OperationSetPersistentPresenceMsnImpl
    extends AbstractOperationSetPersistentPresence<ProtocolProviderServiceMsnImpl>
{
    private static final Logger logger =
        Logger.getLogger(OperationSetPersistentPresenceMsnImpl.class);

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
    private PresenceStatus currentStatus = MsnStatusEnum.OFFLINE;

    /**
     * Sometimes status changes are received before the contact list is inited
     * here we store such events so we can show them correctly
     */
    private Hashtable<String, MsnUserStatus> earlyStatusChange
        = new Hashtable<String, MsnUserStatus>();

    private AuthorizationHandler authorizationHandler = null;

    /**
     * The array list we use when returning from the getSupportedStatusSet()
     * method.
     */
    private static final List<PresenceStatus> supportedPresenceStatusSet
        = new ArrayList<PresenceStatus>();
    static{
        supportedPresenceStatusSet.add(MsnStatusEnum.AWAY);
        supportedPresenceStatusSet.add(MsnStatusEnum.BE_RIGHT_BACK);
        supportedPresenceStatusSet.add(MsnStatusEnum.BUSY);
        supportedPresenceStatusSet.add(MsnStatusEnum.HIDE);
        supportedPresenceStatusSet.add(MsnStatusEnum.IDLE);
        supportedPresenceStatusSet.add(MsnStatusEnum.OFFLINE);
        supportedPresenceStatusSet.add(MsnStatusEnum.ONLINE);
        supportedPresenceStatusSet.add(MsnStatusEnum.ON_THE_PHONE);
        supportedPresenceStatusSet.add(MsnStatusEnum.OUT_TO_LUNCH);
    }

    /**
     * A map containing bindings between SIP Communicator's msn presence status
     * instances and Msn status codes
     */
    private static Map<MsnStatusEnum, MsnUserStatus> scToMsnModesMappings
        = new Hashtable<MsnStatusEnum, MsnUserStatus>();
    static{
        scToMsnModesMappings.put(MsnStatusEnum.AWAY,
                                 MsnUserStatus.AWAY);
        scToMsnModesMappings.put(MsnStatusEnum.BE_RIGHT_BACK,
                                 MsnUserStatus.BE_RIGHT_BACK);
        scToMsnModesMappings.put(MsnStatusEnum.BUSY,
                                 MsnUserStatus.BUSY);
        scToMsnModesMappings.put(MsnStatusEnum.HIDE,
                                 MsnUserStatus.HIDE);
        scToMsnModesMappings.put(MsnStatusEnum.IDLE,
                                 MsnUserStatus.IDLE);
        scToMsnModesMappings.put(MsnStatusEnum.OFFLINE,
                                 MsnUserStatus.OFFLINE);
        scToMsnModesMappings.put(MsnStatusEnum.ONLINE,
                                 MsnUserStatus.ONLINE);
        scToMsnModesMappings.put(MsnStatusEnum.ON_THE_PHONE,
                                 MsnUserStatus.ON_THE_PHONE);
        scToMsnModesMappings.put(MsnStatusEnum.OUT_TO_LUNCH,
                                 MsnUserStatus.OUT_TO_LUNCH);
    }

    /**
     * The server stored contact list that will be encapsulating msn's
     * buddy list.
     */
    private ServerStoredContactListMsnImpl ssContactList = null;

    public OperationSetPersistentPresenceMsnImpl(
        ProtocolProviderServiceMsnImpl provider)
    {
        super(provider);

        ssContactList = new ServerStoredContactListMsnImpl( this , provider);

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
     * @param contact the msn contact.
     * @return the newly created volatile <tt>ContactImpl</tt>
     */
    public ContactMsnImpl createVolatileContact(MsnContact contact)
    {
        return ssContactList.createVolatileContact(contact);
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
        if(! (parentGroup instanceof ContactGroupMsnImpl ||
              parentGroup instanceof RootContactGroupMsnImpl) )
            throw new IllegalArgumentException(
                "Argument is not an msn contact group (group="
                + parentGroup + ")");

        ContactMsnImpl contact =
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
     * Returns the status message that was confirmed by the server
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
        return supportedPresenceStatusSet.iterator();
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

        if( !(contactToMove instanceof ContactMsnImpl) )
            throw new IllegalArgumentException(
                "The specified contact is not an msn contact." + contactToMove);
        if( !(newParent instanceof ContactGroupMsnImpl) )
            throw new IllegalArgumentException(
                "The specified group is not an msn contact group."
                + newParent);

        ssContactList.moveContact((ContactMsnImpl)contactToMove,
                                  (ContactGroupMsnImpl)newParent);
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
                                      String statusMessage) throws
        IllegalArgumentException, IllegalStateException,
        OperationFailedException
    {
        assertConnected();

        if (!(status instanceof MsnStatusEnum))
            throw new IllegalArgumentException(
                            status + " is not a valid Msn status");

        if(status.equals(MsnStatusEnum.OFFLINE))
        {
            parentProvider.unregister();
            return;
        }

        // if the contact list is inited set the state
        // otherwise just set the init status
        //(as if set the status too early the server does not provide
        // any status information about the contacts in our list)
        if(ssContactList.isInitialized())
            parentProvider.getMessenger().getOwner().
                setStatus(scToMsnModesMappings.get(status));
        else
            parentProvider.getMessenger().getOwner().
                setInitStatus(scToMsnModesMappings.get(status));
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
        ContactMsnImpl contact = ssContactList.findContactById(contactIdentifier);
        if(contact == null)
        {
            logger.info("Contact not found id :" + contactIdentifier);
            return null;
        }
        else
            return msnStatusToPresenceStatus(contact.getSourceContact().getStatus());
    }

    /**
     * Removes the specified group from the server stored contact list.
     *
     * @param group the group to remove.
     */
    public void removeServerStoredContactGroup(ContactGroup group)
    {
        assertConnected();

        if( !(group instanceof ContactGroupMsnImpl) )
            throw new IllegalArgumentException(
                "The specified group is not an msn contact group: " + group);

        ssContactList.removeGroup(((ContactGroupMsnImpl)group));
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

        if( !(group instanceof ContactGroupMsnImpl) )
            throw new IllegalArgumentException(
                "The specified group is not an msn contact group: " + group);

        ssContactList.renameGroup((ContactGroupMsnImpl)group, newName);
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
        this.authorizationHandler = handler;
    }

    /**
     * Returns the AuthorizationHandler.
     * @return AuthorizationHandler
     */
    AuthorizationHandler getAuthorizationHandler()
    {
        return this.authorizationHandler;
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

        if(! (parent instanceof ContactGroupMsnImpl) )
            throw new IllegalArgumentException(
                "Argument is not an msn contact group (group=" + parent + ")");

        ssContactList.addContact((ContactGroupMsnImpl)parent, contactIdentifier);
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

        if(! (contact instanceof ContactMsnImpl) )
            throw new IllegalArgumentException(
                "Argument is not an msn contact (contact=" + contact + ")");

        ssContactList.removeContact((ContactMsnImpl)contact);
    }

    /**
     * Converts the specified msn status to one of the status fields of the
     * MsnStatusEnum class.
     *
     * @param status the msn Status
     * @return a PresenceStatus instance representation of the Msn Status
     * parameter. The returned result is one of the MsnStatusEnum fields.
     */
    MsnStatusEnum msnStatusToPresenceStatus(MsnUserStatus status)
    {
        if(status.equals(MsnUserStatus.ONLINE))
            return MsnStatusEnum.ONLINE;
        else if(status.equals(MsnUserStatus.AWAY))
            return MsnStatusEnum.AWAY;
        else if(status.equals(MsnUserStatus.BE_RIGHT_BACK))
            return MsnStatusEnum.BE_RIGHT_BACK;
        else if(status.equals(MsnUserStatus.BUSY))
            return MsnStatusEnum.BUSY;
        else if(status.equals(MsnUserStatus.HIDE))
            return MsnStatusEnum.HIDE;
        else if(status.equals(MsnUserStatus.IDLE))
            return MsnStatusEnum.IDLE;
        else if(status.equals(MsnUserStatus.ON_THE_PHONE))
            return MsnStatusEnum.ON_THE_PHONE;
        else if(status.equals(MsnUserStatus.OUT_TO_LUNCH))
            return MsnStatusEnum.OUT_TO_LUNCH;
        else return MsnStatusEnum.OFFLINE;
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
                "The provider must be non-null and signed on the msn "
                +"service before being able to communicate.");
        if (!parentProvider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the msn service before "
                +"being able to communicate.");
    }

    /**
     * Notify all provider presence listeners of the corresponding event change
     * 
     * @param oldStatus
     *            the status our stack had so far
     * @param newStatus
     *            the status we have from now on
     */
    protected void fireProviderStatusChangeEvent(
        PresenceStatus oldStatus,
        PresenceStatus newStatus)
    {
        if (!oldStatus.equals(newStatus)) {
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
         * The method is called by a ProtocolProvider implementation whenever
         * a change in the registration state of the corresponding provider had
         * occurred.
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            logger.debug("The msn provider changed state from: "
                         + evt.getOldState()
                         + " to: " + evt.getNewState());

            if(evt.getNewState() == RegistrationState.REGISTERED)
            {
                parentProvider.getMessenger().
                    addContactListListener(new StatusChangedListener());
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
                currentStatus = MsnStatusEnum.OFFLINE;

                fireProviderStatusChangeEvent(oldStatus, currentStatus);

                //send event notifications saying that all our buddies are
                //offline.
                Iterator rootContactsIter =
                    getServerStoredContactListRoot().contacts();
                while(rootContactsIter.hasNext())
                {
                    ContactMsnImpl contact
                        = (ContactMsnImpl)rootContactsIter.next();

                    PresenceStatus oldContactStatus
                        = contact.getPresenceStatus();

                    if(!oldContactStatus.isOnline())
                        continue;

                    contact.updatePresenceStatus(MsnStatusEnum.OFFLINE);

                    fireContactPresenceStatusChangeEvent(
                          contact
                        , contact.getParentContactGroup()
                        , oldContactStatus, MsnStatusEnum.OFFLINE);
                }

                Iterator<ContactGroup> groupsIter
                    = getServerStoredContactListRoot().subgroups();
                while(groupsIter.hasNext())
                {
                    ContactGroupMsnImpl group
                        = (ContactGroupMsnImpl)groupsIter.next();

                    Iterator<Contact> contactsIter = group.contacts();

                    while(contactsIter.hasNext())
                    {
                        ContactMsnImpl contact
                            = (ContactMsnImpl)contactsIter.next();

                        PresenceStatus oldContactStatus
                            = contact.getPresenceStatus();

                        if(!oldContactStatus.isOnline())
                            continue;

                        contact.updatePresenceStatus(MsnStatusEnum.OFFLINE);

                        fireContactPresenceStatusChangeEvent(
                              contact
                            , contact.getParentContactGroup()
                            , oldContactStatus, MsnStatusEnum.OFFLINE);
                    }
                }
            }
        }
    }

    /**
     * Sets the messenger instance impl of the lib
     * which communicates with the server
     * @param messenger MsnMessenger
     */
    void setMessenger(MsnMessenger messenger)
    {
        ssContactList.setMessenger(messenger);
    }

    /**
     * Fires all the saved statuses which were received before
     * contact list init
     */
    void earlyStatusesDispatch()
    {
        Iterator<String> iter = earlyStatusChange.keySet().iterator();
        while (iter.hasNext())
        {
            String contactEmail = iter.next();

            ContactMsnImpl sourceContact
                = ssContactList.findContactById(contactEmail);

            if (sourceContact == null)
            {
                return;
            }

            PresenceStatus oldStatus
                = sourceContact.getPresenceStatus();

            PresenceStatus newStatus
                = msnStatusToPresenceStatus(earlyStatusChange.get(contactEmail));

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
        earlyStatusChange.clear();
    }

    /**
     * Returns the server stored contact list registered in this operation set.
     * @return the server stored contact list registered in this operation set.
     */
    ServerStoredContactListMsnImpl getServerStoredContactList()
    {
        return ssContactList;
    }

    /**
     * Waits for status changes from the contacts in the list
     * or own account
     */
    private class StatusChangedListener
        extends MsnContactListAdapter
    {
        /**
         * Indicates that owner status changed
         * @param messenger the messenger changing the status
         */
        public void ownerStatusChanged(MsnMessenger messenger)
        {
            logger.trace("Own status changed to " + messenger.getOwner().getStatus());
            PresenceStatus oldStatus = currentStatus;
            currentStatus =
                msnStatusToPresenceStatus(messenger.getOwner().getStatus());
            fireProviderStatusChangeEvent(oldStatus, currentStatus);
        }

        /**
         * Called from the lib when a contact status changes
         * @param messenger MsnMessenger
         * @param contact MsnContact
         */
        public void contactStatusChanged(   MsnMessenger messenger,
                                            MsnContact contact)
        {
            logger.debug("Received a status update for contact=" + contact);

            ContactMsnImpl sourceContact
                = ssContactList
                    .findContactById(contact.getEmail().getEmailAddress());

            if (sourceContact == null)
            {
                logger.debug("No source contact found for msncontact=" + contact);

                // maybe list is not inited yet will store till init
                earlyStatusChange.put(contact.getEmail().getEmailAddress(),
                    contact.getStatus());

                return;
            }

            PresenceStatus oldStatus
                = sourceContact.getPresenceStatus();

            PresenceStatus newStatus
                = msnStatusToPresenceStatus(contact.getStatus());

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
    }
}
