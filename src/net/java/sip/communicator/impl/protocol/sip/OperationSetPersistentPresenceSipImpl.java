/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import org.osgi.framework.*;

/**
 * A very basic and non-realistic SIP implementation of a persistent presence
 * operation set. In order to simulate server persistence, this operation set
 * would simply accept all unresolved contacts, resolve them immediately and
 * show them as having an offline status.
 *
 * @author Emil Ivov
 */
public class OperationSetPersistentPresenceSipImpl
    implements OperationSetPersistentPresence
{
    private static final Logger logger =
        Logger.getLogger(OperationSetPersistentPresenceSipImpl.class);
    /**
     * A list of listeners registered for <tt>SubscriptionEvent</tt>s.
     */
    private Vector subscriptionListeners = new Vector();

    /**
     * A list of listeners registered for <tt>ServerStoredGroupChangeEvent</tt>s.
     */
    private Vector serverStoredGroupListeners = new Vector();

    /**
     * A list of listeners registered for
     *  <tt>ProviderPresenceStatusChangeEvent</tt>s.
     */
    private Vector providerPresenceStatusListeners = new Vector();

    /**
     * A list of listeneres registered for
     * <tt>ContactPresenceStatusChangeEvent</tt>s.
     */
    private Vector contactPresenceStatusListeners = new Vector();

    /**
     * The root of the SIP contact list.
     */
    private ContactGroupSipImpl contactListRoot = null;

    /**
     * The provider that created us.
     */
    private ProtocolProviderServiceSipImpl parentProvider = null;

    /**
     * The currently active status message.
     */
    private String statusMessage = "Default Status Message";

    /**
     * Our default presence status.
     */
    private PresenceStatus presenceStatus = SipStatusEnum.OFFLINE;

    /**
     * The <tt>AuthorizationHandler</tt> instance that we'd have to transmit
     * authorization requests to for approval.
     */
    private AuthorizationHandler authorizationHandler = null;

    /**
     * Creates an instance of this operation set keeping a reference to the
     * specified parent <tt>provider</tt>.
     * @param provider the ProtocolProviderServiceSipImpl instance that
     * created us.
     */
    public OperationSetPersistentPresenceSipImpl(
            ProtocolProviderServiceSipImpl        provider)
    {
        this.parentProvider = provider;
        contactListRoot = new ContactGroupSipImpl("RootGroup", provider);

        //add our unregistration listener
        parentProvider.addRegistrationStateChangeListener(
            new UnregistrationListener());
    }

    /**
     * SIP implementation of the corresponding ProtocolProviderService
     * method.
     *
     * @param listener a presence status listener.
     */
    public void addContactPresenceStatusListener(
                        ContactPresenceStatusListener listener)
    {
        synchronized(contactPresenceStatusListeners)
        {
            if (!contactPresenceStatusListeners.contains(listener))
                contactPresenceStatusListeners.add(listener);
        }
    }

    /**
     * Notifies all registered listeners of the new event.
     *
     * @param source the contact that has caused the event.
     * @param parentGroup the group that contains the source contact.
     * @param oldValue the status that the source contact detained before
     * changing it.
     */
    public void fireContactPresenceStatusChangeEvent(ContactSipImpl  source,
                                                     ContactGroup parentGroup,
                                                     PresenceStatus oldValue)
    {
        ContactPresenceStatusChangeEvent evt
            = new ContactPresenceStatusChangeEvent(source, parentProvider
                        , parentGroup, oldValue, source.getPresenceStatus());

        Iterator listeners = null;
        synchronized(contactPresenceStatusListeners)
        {
            listeners = new ArrayList(contactPresenceStatusListeners).iterator();
        }


        while(listeners.hasNext())
        {
            ContactPresenceStatusListener listener
                = (ContactPresenceStatusListener)listeners.next();

            listener.contactPresenceStatusChanged(evt);
        }
    }


    /**
     * Notifies all registered listeners of the new event.
     *
     * @param source the contact that has caused the event.
     * @param parentGroup the group that contains the source contact.
     * @param eventID an identifier of the event to dispatch.
     */
    public void fireSubscriptionEvent(ContactSipImpl  source,
                                      ContactGroup parentGroup,
                                      int          eventID)
    {
        SubscriptionEvent evt  = new SubscriptionEvent(source
            , this.parentProvider
            , parentGroup
            , eventID);

        Iterator listeners = null;
        synchronized (subscriptionListeners)
        {
            listeners = new ArrayList(subscriptionListeners).iterator();
        }

        while (listeners.hasNext())
        {
            SubscriptionListener listener
                = (SubscriptionListener) listeners.next();

            if(eventID == SubscriptionEvent.SUBSCRIPTION_CREATED)
            {
                listener.subscriptionCreated(evt);
            }
            else if (eventID == SubscriptionEvent.SUBSCRIPTION_FAILED)
            {
                listener.subscriptionFailed(evt);
            }
            else if (eventID == SubscriptionEvent.SUBSCRIPTION_REMOVED)
            {
                listener.subscriptionRemoved(evt);
            }
        }
    }

    /**
     * Notifies all registered listeners of the new event.
     *
     * @param source the contact that has been moved..
     * @param oldParent the group where the contact was located before being
     * moved.
     * @param newParent the group where the contact has been moved.
     */
    public void fireSubscriptionMovedEvent(Contact      source,
                                           ContactGroup oldParent,
                                           ContactGroup newParent)
    {
        SubscriptionMovedEvent evt  = new SubscriptionMovedEvent(source
            , this.parentProvider
            , oldParent
            , newParent);

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
     * Notifies all registered listeners of the new event.
     *
     * @param source the contact that has caused the event.
     * @param eventID an identifier of the event to dispatch.
     */
    public void fireServerStoredGroupEvent(ContactGroupSipImpl  source,
                                           int                        eventID)
    {
        ServerStoredGroupEvent evt  = new ServerStoredGroupEvent(
            source, eventID,  (ContactGroupSipImpl)source.getParentContactGroup()
           , this.parentProvider, this);

        Iterator listeners = null;
        synchronized (serverStoredGroupListeners)
        {
            listeners = new ArrayList(serverStoredGroupListeners).iterator();
        }

        while (listeners.hasNext())
        {
            ServerStoredGroupListener listener
                = (ServerStoredGroupListener) listeners.next();

            if(eventID == ServerStoredGroupEvent.GROUP_CREATED_EVENT)
            {
                listener.groupCreated(evt);
            }
            else if(eventID == ServerStoredGroupEvent.GROUP_RENAMED_EVENT)
            {
                listener.groupNameChanged(evt);
            }
            else if(eventID == ServerStoredGroupEvent.GROUP_REMOVED_EVENT)
            {
                listener.groupRemoved(evt);
            }
        }
    }

    /**
     * Notifies all registered listeners of the new event.
     *
     * @param oldValue the presence status we were in before the change.
     */
    public void fireProviderStatusChangeEvent(PresenceStatus oldValue)
    {
        ProviderPresenceStatusChangeEvent evt
            = new ProviderPresenceStatusChangeEvent(this.parentProvider,
                                        oldValue, this.getPresenceStatus());

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
     * SIP implementation of the corresponding ProtocolProviderService
     * method.
     *
     * @param listener a dummy param.
     */
    public void addProviderPresenceStatusListener(
        ProviderPresenceStatusListener listener)
    {
        synchronized(providerPresenceStatusListeners)
        {
            if (!providerPresenceStatusListeners.contains(listener))
                this.providerPresenceStatusListeners.add(listener);
        }
    }

    /**
     * Registers a listener that would receive events upon changes in server
     * stored groups.
     *
     * @param listener a ServerStoredGroupChangeListener impl that would
     *   receive events upong group changes.
     */
    public void addServerStoredGroupChangeListener(ServerStoredGroupListener
                                                        listener)
    {
        synchronized(serverStoredGroupListeners)
        {
            if (!serverStoredGroupListeners.contains(listener))
                serverStoredGroupListeners.add(listener);
        }
    }

    /**
     * SIP implementation of the corresponding ProtocolProviderService
     * method.
     *
     * @param listener the SubscriptionListener to register
     */
    public void addSubsciptionListener(SubscriptionListener listener)
    {
        synchronized(subscriptionListeners)
        {
            if (!subscriptionListeners.contains(listener))
                this.subscriptionListeners.add(listener);
        }
    }

    /**
     * Creates a group with the specified name and parent in the server
     * stored contact list.
     *
     * @param parent the group where the new group should be created
     * @param groupName the name of the new group to create.
     */
    public void createServerStoredContactGroup(ContactGroup parent,
                                               String groupName)
    {
        ContactGroupSipImpl newGroup
            = new ContactGroupSipImpl(groupName, parentProvider);

        ((ContactGroupSipImpl)parent).addSubgroup(newGroup);

        this.fireServerStoredGroupEvent(
            newGroup, ServerStoredGroupEvent.GROUP_CREATED_EVENT);
    }

    /**
     * A SIP Provider method to use for fast filling of a contact list.
     *
     * @param contactGroup the group to add
     */
    public void addSipGroup(ContactGroupSipImpl contactGroup)
    {
        contactListRoot.addSubgroup(contactGroup);
    }

    /**
     * A SIP Provider method to use for fast filling of a contact list.
     * This method would add both the group and fire an event.
     *
     * @param parent the group where <tt>contactGroup</tt> should be added.
     * @param contactGroup the group to add
     */
    public void addSipGroupAndFireEvent(
                                  ContactGroupSipImpl parent
                                , ContactGroupSipImpl contactGroup)
    {
        parent.addSubgroup(contactGroup);

        this.fireServerStoredGroupEvent(
            contactGroup, ServerStoredGroupEvent.GROUP_CREATED_EVENT);
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
        return contactListRoot.findContactByID(contactID);
    }

    /**
     * Sets the specified status message.
     * @param statusMessage a String containing the new status message.
     */
    public void setStatusMessage(String statusMessage)
    {
        this.statusMessage = statusMessage;
    }

    /**
     * Returns the status message that was last set through
     * setCurrentStatusMessage.
     *
     * @return the last status message that we have requested and the aim
     *   server has confirmed.
     */
    public String getCurrentStatusMessage()
    {
        return statusMessage;
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
        return presenceStatus;
    }

    /**
     * Returns the root group of the server stored contact list.
     *
     * @return the root ContactGroup for the ContactList stored by this
     *   service.
     */
    public ContactGroup getServerStoredContactListRoot()
    {
        return contactListRoot;
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
        return SipStatusEnum.supportedStatusSet();
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
        ContactSipImpl sipContact
            = (ContactSipImpl)contactToMove;

        ContactGroupSipImpl parentSipGroup
            = findContactParent(sipContact);

        parentSipGroup.removeContact(sipContact);

        //if this is a volatile contact then we haven't really subscribed to
        //them so we'd need to do so here
        if(!sipContact.isPersistent())
        {
            //first tell everyone that the volatile contact was removed
            fireSubscriptionEvent(sipContact
                                  , parentSipGroup
                                  , SubscriptionEvent.SUBSCRIPTION_REMOVED);

            try
            {
                //now subscribe
                this.subscribe(newParent, contactToMove.getAddress());

                //now tell everyone that we've added the contact
                fireSubscriptionEvent(sipContact
                                      , newParent
                                      , SubscriptionEvent.SUBSCRIPTION_CREATED);
            }
            catch (Exception ex)
            {
                logger.error("Failed to move contact "
                             + sipContact.getAddress()
                             , ex);
            }
        }
        else
        {
            ( (ContactGroupSipImpl) newParent)
                    .addContact(sipContact);

            fireSubscriptionMovedEvent(contactToMove
                                      , parentSipGroup
                                       , newParent);
        }
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
        PresenceStatus oldPresenceStatus = this.presenceStatus;
        this.presenceStatus = status;
        this.statusMessage = statusMessage;

        this.fireProviderStatusChangeEvent(oldPresenceStatus);

        //since we are not a real protocol, we set the contact presence status
        //ourselves and make them have the same status as ours.
        changePresenceStatusForAllContacts( getServerStoredContactListRoot()
                                            , getPresenceStatus());
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
        return findContactByID(contactIdentifier).getPresenceStatus();
    }

    /**
     * Sets the presence status of <tt>contact</tt> to <tt>newStatus</tt>.
     *
     * @param contact the <tt>ContactSipImpl</tt> whose status we'd like
     * to set.
     * @param newStatus the new status we'd like to set to <tt>contact</tt>.
     */
    private void changePresenceStatusForContact(
                                            ContactSipImpl contact
                                         ,  PresenceStatus       newStatus)
    {
        PresenceStatus oldStatus = contact.getPresenceStatus();
        contact.setPresenceStatus(newStatus);

        fireContactPresenceStatusChangeEvent(
                contact, findContactParent(contact), oldStatus);
    }

    /**
     * Sets the presence status of all <tt>contact</tt>s in our contact list
     * (except those that  correspond to another provider registered with SC)
     * to <tt>newStatus</tt>.
     *
     * @param newStatus the new status we'd like to set to <tt>contact</tt>.
     * @param parent the group in which we'd have to update the status of all
     * direct and indirect child contacts.
     */
    private void changePresenceStatusForAllContacts(ContactGroup   parent,
                                                    PresenceStatus newStatus)
    {
        //first set the status for contacts in this group
        Iterator childContacts = parent.contacts();

        while(childContacts.hasNext())
        {
            ContactSipImpl contact
                = (ContactSipImpl)childContacts.next();

            PresenceStatus oldStatus = contact.getPresenceStatus();
            contact.setPresenceStatus(newStatus.isOnline()
                                        ? SipStatusEnum.ONLINE
                                        : SipStatusEnum.OFFLINE);

            fireContactPresenceStatusChangeEvent(
                contact, parent, oldStatus);
        }

        //now call this method recursively for all subgroups
        Iterator subgroups = parent.subgroups();

        while(subgroups.hasNext())
        {
            ContactGroup subgroup = (ContactGroup)subgroups.next();
            changePresenceStatusForAllContacts(subgroup, newStatus);
        }
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
        synchronized(contactPresenceStatusListeners)
        {
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
        synchronized(providerPresenceStatusListeners)
        {
            this.providerPresenceStatusListeners.remove(listener);
        }
    }

    /**
     * Returns the group that is parent of the specified sipGroup  or null
     * if no parent was found.
     * @param sipGroup the group whose parent we're looking for.
     * @return the ContactGroupSipImpl instance that sipGroup
     * belongs to or null if no parent was found.
     */
    public ContactGroupSipImpl findGroupParent(
                                    ContactGroupSipImpl sipGroup)
    {
        return contactListRoot.findGroupParent(sipGroup);
    }

    /**
     * Returns the group that is parent of the specified sipContact  or
     * null if no parent was found.
     * @param sipContact the contact whose parent we're looking for.
     * @return the ContactGroupSipImpl instance that sipContact
     * belongs to or null if no parent was found.
     */
    public ContactGroupSipImpl findContactParent(
                                        ContactSipImpl sipContact)
    {
        return (ContactGroupSipImpl)sipContact
                                                .getParentContactGroup();
    }


    /**
     * Removes the specified group from the server stored contact list.
     *
     * @param group the group to remove.
     *
     * @throws IllegalArgumentException if <tt>group</tt> was not found in this
     * protocol's contact list.
     */
    public void removeServerStoredContactGroup(ContactGroup group)
        throws IllegalArgumentException
    {
        ContactGroupSipImpl sipGroup
                                        = (ContactGroupSipImpl)group;

        ContactGroupSipImpl parent = findGroupParent(sipGroup);

        if(parent == null){
            throw new IllegalArgumentException(
                "group " + group
                + " does not seem to belong to this protocol's contact list.");
        }

        parent.removeSubGroup(sipGroup);

        this.fireServerStoredGroupEvent(
            sipGroup, ServerStoredGroupEvent.GROUP_REMOVED_EVENT);
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
        synchronized(serverStoredGroupListeners)
        {
            serverStoredGroupListeners.remove(listener);
        }
    }

    /**
     * Removes the specified subscription listener.
     *
     * @param listener the listener to remove.
     */
    public void removeSubscriptionListener(SubscriptionListener listener)
    {
        synchronized(subscriptionListeners)
        {
            this.subscriptionListeners.remove(listener);
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
        ((ContactGroupSipImpl)group).setGroupName(newName);

        this.fireServerStoredGroupEvent(
            (ContactGroupSipImpl)group, ServerStoredGroupEvent.GROUP_RENAMED_EVENT);
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
        ContactSipImpl contact = new ContactSipImpl(
            contactIdentifier
            , parentProvider);

        if(authorizationHandler != null)
        {
            //we require authorizations in SIP
            AuthorizationRequest request
                = authorizationHandler.createAuthorizationRequest(contact);


            //and finally pretend that the remote contact has granted us
            //authorization
            AuthorizationResponse response
                = deliverAuthorizationRequest(request, contact);

            authorizationHandler.processAuthorizationResponse(
                response, contact);

            //if the request was not accepted - return the contact.
            if(response.getResponseCode() == AuthorizationResponse.REJECT)
                return;
        }
        ((ContactGroupSipImpl)parent).addContact(contact);

        fireSubscriptionEvent(contact,
                              parent,
                              SubscriptionEvent.SUBSCRIPTION_CREATED);

        //otherwise - since we are not a real protocol, we set the contact
        //presence status ourselves
        changePresenceStatusForContact(contact, getPresenceStatus());

        //notify presence listeners for the status change.
        fireContactPresenceStatusChangeEvent(contact
                                             , parent
                                             , SipStatusEnum.OFFLINE);
    }


    /**
     * Depending on whether <tt>contact</tt> corresponds to another protocol
     * provider installed in sip-communicator, this method would either deliver
     * it to that provider or simulate a corresponding request from the
     * destination contact and make return a response after it has received
     * one If the destination contact matches us, then we'll ask the user to
     * act upon the request, and return the response.
     *
     * @param request the authorization request that we'd like to deliver to the
     * desination <tt>contact</tt>.
     * @param contact the <tt>Contact</tt> to notify
     *
     * @return the <tt>AuthorizationResponse</tt> that has been given or
     * generated in response to <tt>request</tt>.
     */
    private AuthorizationResponse deliverAuthorizationRequest(
                AuthorizationRequest request,
                Contact contact)
    {
        //pretend that the remote contact is asking for authorization
        authorizationHandler.processAuthorisationRequest(
            request, contact);

        //and now pretend that the remote contact has granted us
        //authorization
        return new AuthorizationResponse(AuthorizationResponse.ACCEPT
                                         , "You are welcome!");
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
        subscribe(contactListRoot, contactIdentifier);

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
        ContactGroupSipImpl parentGroup
            = (ContactGroupSipImpl)((ContactSipImpl)contact)
            .getParentContactGroup();

        parentGroup.removeContact((ContactSipImpl)contact);

        fireSubscriptionEvent((ContactSipImpl)contact,
             ((ContactSipImpl)contact).getParentContactGroup()
           , SubscriptionEvent.SUBSCRIPTION_REMOVED);
    }

    /**
     * Creates and returns a unresolved contact from the specified
     * <tt>address</tt> and <tt>persistentData</tt>. The method will not try
     * to establish a network connection and resolve the newly created Contact
     * against the server. The protocol provider may will later try and resolve
     * the contact. When this happens the corresponding event would notify
     * interested subscription listeners.
     *
     * @param address an identifier of the contact that we'll be creating.
     * @param persistentData a String returned Contact's getPersistentData()
     * method during a previous run and that has been persistently stored
     * locally.
     * @return the unresolved <tt>Contact</tt> created from the specified
     * <tt>address</tt> and <tt>persistentData</tt>
     */
    public Contact createUnresolvedContact(String address,
                                           String persistentData)
    {
        return createUnresolvedContact(address
                                      , persistentData
                                      , getServerStoredContactListRoot());
    }

    /**
     * Creates and returns a unresolved contact from the specified
     * <tt>address</tt> and <tt>persistentData</tt>. The method will not try
     * to establish a network connection and resolve the newly created Contact
     * against the server. The protocol provider may will later try and resolve
     * the contact. When this happens the corresponding event would notify
     * interested subscription listeners.
     *
     * @param address an identifier of the contact that we'll be creating.
     * @param persistentData a String returned Contact's getPersistentData()
     * method during a previous run and that has been persistently stored
     * locally.
     * @param parent the group where the unresolved contact is
     * supposed to belong to.
     *
     * @return the unresolved <tt>Contact</tt> created from the specified
     * <tt>address</tt> and <tt>persistentData</tt>
     */
    public Contact createUnresolvedContact(String address,
                                           String persistentData,
                                           ContactGroup parent)
    {
        ContactSipImpl contact = new ContactSipImpl(
            address
            , parentProvider);
        contact.setResolved(false);

        ( (ContactGroupSipImpl) parent).addContact(contact);

        fireSubscriptionEvent(contact,
                              parent,
                              SubscriptionEvent.SUBSCRIPTION_CREATED);

        //since we don't have any server, we'll simply resolve the contact
        //ourselves as if we've just received an event from the server telling
        //us that it has been resolved.
        fireSubscriptionEvent(
            contact, parent, SubscriptionEvent.SUBSCRIPTION_RESOLVED);

        //since we are not a real protocol, we set the contact presence status
        //ourselves
        changePresenceStatusForContact( contact, getPresenceStatus());

        return contact;
    }

    /**
     * Creates and returns a unresolved contact group from the specified
     * <tt>address</tt> and <tt>persistentData</tt>. The method will not try
     * to establish a network connection and resolve the newly created
     * <tt>ContactGroup</tt> against the server or the contact itself. The
     * protocol provider will later resolve the contact group. When this happens
     * the corresponding event would notify interested subscription listeners.
     *
     * @param groupUID an identifier, returned by ContactGroup's getGroupUID,
     * that the protocol provider may use in order to create the group.
     * @param persistentData a String returned ContactGroups's
     * getPersistentData() method during a previous run and that has been
     * persistently stored locally.
     * @param parentGroup the group under which the new group is to be created
     * or null if this is group directly underneath the root.
     * @return the unresolved <tt>ContactGroup</tt> created from the specified
     * <tt>uid</tt> and <tt>persistentData</tt>
     */
    public ContactGroup createUnresolvedContactGroup(String groupUID,
        String persistentData, ContactGroup parentGroup)
    {
        ContactGroupSipImpl newGroup
            = new ContactGroupSipImpl(
                ContactGroupSipImpl.createNameFromUID(groupUID)
                , parentProvider);
        newGroup.setResolved(false);

        //if parent is null then we're adding under root.
        if(parentGroup == null)
            parentGroup = getServerStoredContactListRoot();

        ((ContactGroupSipImpl)parentGroup).addSubgroup(newGroup);

        this.fireServerStoredGroupEvent(
            newGroup, ServerStoredGroupEvent.GROUP_CREATED_EVENT);

        return newGroup;
    }

    private class UnregistrationListener
        implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenver
         * a change in the registration state of the corresponding provider had
         * occurred. The method is particularly interested in events stating
         * that the SIP provider has unregistered so that it would fire
         * status change events for all contacts in our buddy list.
         *
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            //send event notifications saying that all our buddies are
            //offline. The icq protocol does not implement top level buddies
            //nor subgroups for top level groups so a simple nested loop
            //would be enough.
            Iterator groupsIter = getServerStoredContactListRoot()
                .subgroups();
            while (groupsIter.hasNext())
            {
                ContactGroupSipImpl group
                    = (ContactGroupSipImpl) groupsIter.next();

                Iterator contactsIter = group.contacts();

                while (contactsIter.hasNext())
                {
                    ContactSipImpl contact
                        = (ContactSipImpl) contactsIter.next();

                    PresenceStatus oldContactStatus
                        = contact.getPresenceStatus();

                    if (!oldContactStatus.isOnline())
                        continue;

                    contact.setPresenceStatus(SipStatusEnum.OFFLINE);

                    fireContactPresenceStatusChangeEvent(
                        contact
                        , contact.getParentContactGroup()
                        , oldContactStatus);
                }
            }
        }
    }

    /**
     * Returns the volatile group or null if this group has not yet been
     * created.
     *
     * @return a volatile group existing in our contact list or <tt>null</tt>
     * if such a group has not yet been created.
     */
    private ContactGroupSipImpl getNonPersistentGroup()
    {
        for (int i = 0
             ; i < getServerStoredContactListRoot().countSubgroups()
             ; i++)
        {
            ContactGroupSipImpl gr =
                (ContactGroupSipImpl)getServerStoredContactListRoot()
                    .getGroup(i);

            if(!gr.isPersistent())
                return gr;
        }

        return null;
    }


    /**
     * Creates a non persistent contact for the specified address. This would
     * also create (if necessary) a group for volatile contacts that would not
     * be added to the server stored contact list. This method would have no
     * effect on the server stored contact list.
     *
     * @param contactAddress the address of the volatile contact we'd like to
     * create.
     * @return the newly created volatile contact.
     */
    public ContactSipImpl createVolatileContact(String contactAddress)
    {
        //First create the new volatile contact;
        ContactSipImpl newVolatileContact
            = new ContactSipImpl(contactAddress
                                       , this.parentProvider);
        newVolatileContact.setPersistent(false);


        //Check whether a volatile group already exists and if not create
        //one
        ContactGroupSipImpl theVolatileGroup = getNonPersistentGroup();


        //if the parent volatile group is null then we create it
        if (theVolatileGroup == null)
        {
            List emptyBuddies = new LinkedList();
            theVolatileGroup = new ContactGroupSipImpl(
                "NotInContactList"
                , parentProvider);
            theVolatileGroup.setResolved(false);
            theVolatileGroup.setPersistent(false);
            theVolatileGroup.addContact(newVolatileContact);

            this.contactListRoot.addSubgroup(theVolatileGroup);

            fireServerStoredGroupEvent(theVolatileGroup
                           , ServerStoredGroupEvent.GROUP_CREATED_EVENT);
        }

        //now add the volatile contact instide it
        theVolatileGroup.addContact(newVolatileContact);
        fireSubscriptionEvent(newVolatileContact
                         , theVolatileGroup
                         , SubscriptionEvent.SUBSCRIPTION_CREATED);

        return newVolatileContact;
    }

}
