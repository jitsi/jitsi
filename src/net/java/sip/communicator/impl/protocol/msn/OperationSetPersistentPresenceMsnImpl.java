/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import java.beans.*;
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
    implements OperationSetPersistentPresence
{
    private static final Logger logger =
        Logger.getLogger(OperationSetPersistentPresenceMsnImpl.class);

    /**
     * A callback to the Msn provider that created us.
     */
    private ProtocolProviderServiceMsnImpl msnProvider = null;

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
    private PresenceStatus currentStatus = MsnStatusEnum.OFFLINE;

    /**
     * The list of listeners interested in receiving changes in our local
     * presencestatus.
     */
    private Vector providerPresenceStatusListeners = new Vector();

    /**
     * The list of subscription listeners interested in receiving  notifications
     * whenever .
     */
    private Vector subscriptionListeners = new Vector();

    /**
     * The list of presence status listeners interested in receiving presence
     * notifications of changes in status of contacts in our contact list.
     */
    private Vector contactPresenceStatusListeners = new Vector();

    /**
     * The array list we use when returning from the getSupportedStatusSet()
     * method.
     */
    private static final ArrayList supportedPresenceStatusSet = new ArrayList();
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
    private static Map scToMsnModesMappings = new Hashtable();
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
     * The server stored contact list that will be encapsulating smack's
     * buddy list.
     */
    private ServerStoredContactListMsnImpl ssContactList = null;

    public OperationSetPersistentPresenceMsnImpl(
        ProtocolProviderServiceMsnImpl provider)
    {
        this.msnProvider = provider;

        ssContactList = new ServerStoredContactListMsnImpl( this , provider);

        this.msnProvider.addRegistrationStateChangeListener(
            new RegistrationStateListener());
    }

    /**
     * Registers a listener that would receive a presence status change event
     * every time a contact, whose status we're subscribed for, changes her
     * status.
     *
     * @param listener the listener that would received presence status
     *   updates for contacts.
     */
    public void addContactPresenceStatusListener(ContactPresenceStatusListener
                                                 listener)
    {
        synchronized(contactPresenceStatusListeners){
                    this.contactPresenceStatusListeners.add(listener);
        }
    }

    /**
     * Adds a listener that would receive events upon changes of the provider
     * presence status.
     *
     * @param listener the listener to register for changes in our
     *   PresenceStatus.
     */
    public void addProviderPresenceStatusListener(
        ProviderPresenceStatusListener listener)
    {
        synchronized(providerPresenceStatusListeners){
            providerPresenceStatusListeners.add(listener);
        }
    }

    /**
     * Registers a listener that would receive events upong changes in server
     * stored groups.
     *
     * @param listener a ServerStoredGroupChangeListener impl that would
     *   receive events upong group changes.
     */
    public void addServerStoredGroupChangeListener(ServerStoredGroupListener
        listener)
    {
        ssContactList.addGroupListener(listener);
    }

    /**
     * Registers a listener that would get notifications any time a new
     * subscription was succesfully added, has failed or was removed.
     *
     * @param listener the SubscriptionListener to register
     */
    public void addSubsciptionListener(SubscriptionListener listener)
    {
        synchronized(subscriptionListeners){
           subscriptionListeners.add(listener);
       }
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
    public ContactMsnImpl createVolatileContact(String id)
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
    public Iterator getSupportedStatusSet()
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

        if (!(status instanceof MsnStatusEnum))
            throw new IllegalArgumentException(
                            status + " is not a valid Msn status");

        if(status.equals(MsnStatusEnum.OFFLINE))
        {
            msnProvider.unregister();
            return;
        }

        // if the contact list is inited set the state
        // otherwise just set the init status
        //(as if set the status too early the server does not provide
        // any status information about the contacts in our list)
        if(ssContactList.isInitialized())
            msnProvider.getMessenger().getOwner().
                setStatus((MsnUserStatus)scToMsnModesMappings.get(status));
        else
            msnProvider.getMessenger().getOwner().
                setInitStatus((MsnUserStatus)scToMsnModesMappings.get(status));
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
     * Removes the specified listener so that it won't receive any further
     * updates on contact presence status changes
     *
     * @param listener the listener to remove.
     */
    public void removeContactPresenceStatusListener(
        ContactPresenceStatusListener listener)
    {
        synchronized(contactPresenceStatusListeners){
            contactPresenceStatusListeners.remove(listener);
        }
    }

    /**
     * Unregisters the specified listener so that it does not receive further
     * events upon changes in local presence status.
     *
     * @param listener ProviderPresenceStatusListener
     */
    public void removeProviderPresenceStatusListener(
        ProviderPresenceStatusListener listener)
    {
        synchronized(providerPresenceStatusListeners){
            providerPresenceStatusListeners.remove(listener);
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
     * Removes the specified subscription listener.
     *
     * @param listener the listener to remove.
     */
    public void removeSubscriptionListener(SubscriptionListener listener)
    {
        synchronized(subscriptionListeners){
            subscriptionListeners.remove(listener);
        }
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
     * @param mode the msn Status
     * @return a PresenceStatus instance representation of the Msn Status
     * parameter. The returned result is one of the MsnStatusEnum fields.
     */
    private MsnStatusEnum msnStatusToPresenceStatus(MsnUserStatus status)
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
        if (msnProvider == null)
            throw new IllegalStateException(
                "The provider must be non-null and signed on the msn "
                +"service before being able to communicate.");
        if (!msnProvider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the msn service before "
                +"being able to communicate.");
    }

    /**
     * Notify all provider presence listeners of the corresponding event change
     * @param oldStatus the status our stack had so far
     * @param newStatus the status we have from now on
     */
    void fireProviderPresenceStatusChangeEvent(
        PresenceStatus oldStatus, PresenceStatus newStatus)
    {
        if(oldStatus.equals(newStatus)){
            logger.debug("Ignored prov stat. change evt. old==new = "
                         + oldStatus);
            return;
        }

        ProviderPresenceStatusChangeEvent evt =
            new ProviderPresenceStatusChangeEvent(
                msnProvider, oldStatus, newStatus);

        currentStatus = newStatus;


        logger.debug("Dispatching Provider Status Change. Listeners="
                     + providerPresenceStatusListeners.size()
                     + " evt=" + evt);

        Iterator listeners = null;
        synchronized (providerPresenceStatusListeners)
        {
            listeners = new ArrayList(providerPresenceStatusListeners).iterator();
        }

        while (listeners.hasNext())
        {
            ProviderPresenceStatusListener listener
                = (ProviderPresenceStatusListener) listeners.next();

            listener.providerStatusChanged(evt);
        }
    }

    /**
     * Notify all provider presence listeners that a new status message has
     * been set.
     * @param oldStatusMessage the status message our stack had so far
     * @param newStatusMessage the status message we have from now on
     */
    private void fireProviderStatusMessageChangeEvent(
                        String oldStatusMessage, String newStatusMessage)
    {

        PropertyChangeEvent evt = new PropertyChangeEvent(
                msnProvider, ProviderPresenceStatusListener.STATUS_MESSAGE,
                oldStatusMessage, newStatusMessage);

        logger.debug("Dispatching  stat. msg change. Listeners="
                     + providerPresenceStatusListeners.size()
                     + " evt=" + evt);

        Iterator listeners = null;
        synchronized (providerPresenceStatusListeners)
        {
            listeners = new ArrayList(providerPresenceStatusListeners).iterator();
        }

        while (listeners.hasNext())
        {
            ProviderPresenceStatusListener listener =
                (ProviderPresenceStatusListener) listeners.next();

            listener.providerStatusMessageChanged(evt);
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
            logger.debug("The msn provider changed state from: "
                         + evt.getOldState()
                         + " to: " + evt.getNewState());

            if(evt.getNewState() == RegistrationState.REGISTERED)
            {
                msnProvider.getMessenger().
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

                fireProviderPresenceStatusChangeEvent(oldStatus,
                    currentStatus);

                //send event notifications saying that all our buddies are
                //offline. The protocol does not implement top level buddies
                //nor subgroups for top level groups so a simple nested loop
                //would be enough.
                Iterator groupsIter =
                    getServerStoredContactListRoot().subgroups();
                while(groupsIter.hasNext())
                {
                    ContactGroupMsnImpl group
                        = (ContactGroupMsnImpl)groupsIter.next();

                    Iterator contactsIter = group.contacts();

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
     * Notify all subscription listeners of the corresponding event.
     *
     * @param eventID the int ID of the event to dispatch
     * @param sourceContact the ContactMsnImpl instance that this event is
     * pertaining to.
     * @param parentGroup the ContactGroupMsnImpl under which the corresponding
     * subscription is located.
     */
    void fireSubscriptionEvent( int eventID,
                                ContactMsnImpl sourceContact,
                                ContactGroup parentGroup)
    {
        SubscriptionEvent evt =
            new SubscriptionEvent(sourceContact, msnProvider, parentGroup,
                                  eventID);

        logger.debug("Dispatching a Subscription Event to"
                     +subscriptionListeners.size() + " listeners. Evt="+evt);

        Iterator listeners = null;
        synchronized (subscriptionListeners)
        {
            listeners = new ArrayList(subscriptionListeners).iterator();
        }

        while (listeners.hasNext())
        {
            SubscriptionListener listener
                = (SubscriptionListener) listeners.next();

            if (evt.getEventID() == SubscriptionEvent.SUBSCRIPTION_CREATED)
                listener.subscriptionCreated(evt);
            else if (evt.getEventID() == SubscriptionEvent.SUBSCRIPTION_REMOVED)
                listener.subscriptionRemoved(evt);
            else if (evt.getEventID() == SubscriptionEvent.SUBSCRIPTION_FAILED)
                listener.subscriptionFailed(evt);
        }
    }

    /**
     * Notify all subscription listeners of the corresponding event.
     *
     * @param sourceContact the ContactMsnImpl instance that this event is
     * pertaining to.
     * @param oldParentGroup the group that was previously a parent of the
     * source contact.
     * @param newParentGroup the group under which the corresponding
     * subscription is currently located.
     */
    void fireSubscriptionMovedEvent( ContactMsnImpl sourceContact,
                                     ContactGroup oldParentGroup,
                                     ContactGroup newParentGroup)
    {
        SubscriptionMovedEvent evt =
            new SubscriptionMovedEvent(sourceContact, msnProvider
                                       , oldParentGroup, newParentGroup);

        logger.debug("Dispatching a Subscription Event to"
                     +subscriptionListeners.size() + " listeners. Evt="+evt);

        Iterator listeners = null;
        synchronized (subscriptionListeners)
        {
            listeners = new ArrayList(subscriptionListeners).iterator();
        }

        while (listeners.hasNext())
        {
            SubscriptionListener listener
                = (SubscriptionListener) listeners.next();

            listener.subscriptionMoved(evt);
        }
    }

    /**
     * Notify all contact presence listeners of the corresponding event change
     * @param contact the contact that changed its status
     * @param oldStatus the status that the specified contact had so far
     * @param newStatus the status that the specified contact is currently in.
     * @param parentGroup the group containing the contact which caused the event
     */
    private void fireContactPresenceStatusChangeEvent(
                        Contact contact,
                        ContactGroup parentGroup,
                        PresenceStatus oldStatus,
                        PresenceStatus newStatus)
    {
        ContactPresenceStatusChangeEvent evt =
            new ContactPresenceStatusChangeEvent(
                contact, msnProvider, parentGroup, oldStatus, newStatus);


        logger.debug("Dispatching Contact Status Change. Listeners="
                     + contactPresenceStatusListeners.size()
                     + " evt=" + evt);

        Iterator listeners = null;
        synchronized (contactPresenceStatusListeners)
        {
            listeners = new ArrayList(contactPresenceStatusListeners).iterator();
        }

        while (listeners.hasNext())
        {
            ContactPresenceStatusListener listener
                = (ContactPresenceStatusListener) listeners.next();

            listener.contactPresenceStatusChanged(evt);
        }
    }

    /**
     * Sets the messenger instance impl of the lib
     * which comunicates with the server
     * @param messenger MsnMessenger
     */
    void setMessenger(MsnMessenger messenger)
    {
        ssContactList.setMessenger(messenger);
    }

    /**
     * Waits for status changes from the contacts in the list
     * or ouw own account
     */
    private class StatusChangedListener
        extends MsnContactListAdapter
    {
        /**
         * Indicates that ower status changed
         * @param newStatus MsnUserStatus the new status
         */
        public void ownerStatusChanged(MsnMessenger messenger)
        {
            logger.trace("Own status changed to " + messenger.getOwner().getStatus());
            PresenceStatus oldStatus = currentStatus;
            currentStatus =
                msnStatusToPresenceStatus(messenger.getOwner().getStatus());
            fireProviderPresenceStatusChangeEvent(oldStatus, currentStatus);
        }

        /**
         * Called from the lib when a contact status changes
         * @param messenger MsnMessenger
         * @param contact MsnContact
         */
        public void contactStatusChanged(MsnMessenger messenger, MsnContact contact)
        {
            logger.debug("Received a status update for contact=" + contact);

            ContactMsnImpl sourceContact
                = ssContactList.findContactById(contact.getEmail().getEmailAddress());

            if (sourceContact == null)
            {
                logger.warn("No source contact found for msncontact=" + contact);
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
