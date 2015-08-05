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
package net.java.sip.communicator.impl.protocol.gibberish;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * A Gibberish implementation of a persistent presence operation set. In order
 * to simulate server persistence, this operation set would simply accept all
 * unresolved contacts and resolve them immediately. A real world protocol
 * implementation would save it on a server using methods provided by the
 * protocol stack.
 *
 * @author Emil Ivov
 */
public class OperationSetPersistentPresenceGibberishImpl
    extends AbstractOperationSetPersistentPresence<ProtocolProviderServiceGibberishImpl>
{
    private static final Logger logger =
        Logger.getLogger(OperationSetPersistentPresenceGibberishImpl.class);

    /**
     * The root of the gibberish contact list.
     */
    private ContactGroupGibberishImpl contactListRoot = null;

    /**
     * The currently active status message.
     */
    private String statusMessage = "Default Status Message";

    /**
     * Our presence status.
     */
    private PresenceStatus presenceStatus = GibberishStatusEnum.OFFLINE;

    /**
     * The <tt>AuthorizationHandler</tt> instance that we'd have to transmit
     * authorization requests to for approval.
     */
    private AuthorizationHandler authorizationHandler = null;

    /**
     * Creates an instance of this operation set keeping a reference to the
     * specified parent <tt>provider</tt>.
     * @param provider the ProtocolProviderServiceGibberishImpl instance that
     * created us.
     */
    public OperationSetPersistentPresenceGibberishImpl(
            ProtocolProviderServiceGibberishImpl        provider)
    {
        super(provider);

        contactListRoot = new ContactGroupGibberishImpl("RootGroup", provider);

        //add our unregistration listener
        parentProvider.addRegistrationStateChangeListener(
            new UnregistrationListener());
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
        ContactGroupGibberishImpl newGroup
            = new ContactGroupGibberishImpl(groupName, parentProvider);

        ((ContactGroupGibberishImpl)parent).addSubgroup(newGroup);

        this.fireServerStoredGroupEvent(
            newGroup, ServerStoredGroupEvent.GROUP_CREATED_EVENT);
    }

    /**
     * A Gibberish Provider method to use for fast filling of a contact list.
     *
     * @param contactGroup the group to add
     */
    public void addGibberishGroup(ContactGroupGibberishImpl contactGroup)
    {
        contactListRoot.addSubgroup(contactGroup);
    }

    /**
     * A Gibberish Provider method to use for fast filling of a contact list.
     * This method would add both the group and fire an event.
     *
     * @param parent the group where <tt>contactGroup</tt> should be added.
     * @param contactGroup the group to add
     */
    public void addGibberishGroupAndFireEvent(
                                  ContactGroupGibberishImpl parent
                                , ContactGroupGibberishImpl contactGroup)
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
    public Iterator<PresenceStatus> getSupportedStatusSet()
    {
        return GibberishStatusEnum.supportedStatusSet();
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
        ContactGibberishImpl gibberishContact
            = (ContactGibberishImpl)contactToMove;

        ContactGroupGibberishImpl parentGibberishGroup
            = findContactParent(gibberishContact);

        parentGibberishGroup.removeContact(gibberishContact);

        //if this is a volatile contact then we haven't really subscribed to
        //them so we'd need to do so here
        if(!gibberishContact.isPersistent())
        {
            //first tell everyone that the volatile contact was removed
            fireSubscriptionEvent(gibberishContact
                                  , parentGibberishGroup
                                  , SubscriptionEvent.SUBSCRIPTION_REMOVED);

            try
            {
                //now subscribe
                this.subscribe(newParent, contactToMove.getAddress());

                //now tell everyone that we've added the contact
                fireSubscriptionEvent(gibberishContact
                                      , newParent
                                      , SubscriptionEvent.SUBSCRIPTION_CREATED);
            }
            catch (Exception ex)
            {
                logger.error("Failed to move contact "
                             + gibberishContact.getAddress()
                             , ex);
            }
        }
        else
        {
            ( (ContactGroupGibberishImpl) newParent)
                    .addContact(gibberishContact);

            fireSubscriptionMovedEvent(contactToMove
                                      , parentGibberishGroup
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

        //now check whether we are in someone else's contact list and modify
        //our status there
        List<Contact> contacts = findContactsPointingToUs();

        Iterator<Contact> contactsIter = contacts.iterator();
        while (contactsIter.hasNext())
        {
            ContactGibberishImpl contact = (ContactGibberishImpl)contactsIter.next();

            PresenceStatus oldStatus = contact.getPresenceStatus();
            contact.setPresenceStatus(status);
            contact.getParentPresenceOperationSet()
                .fireContactPresenceStatusChangeEvent(
                    contact
                    , contact.getParentContactGroup()
                    , oldStatus);

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
        return findContactByID(contactIdentifier).getPresenceStatus();
    }

    /**
     * Sets the presence status of <tt>contact</tt> to <tt>newStatus</tt>.
     *
     * @param contact the <tt>ContactGibberishImpl</tt> whose status we'd like
     * to set.
     * @param newStatus the new status we'd like to set to <tt>contact</tt>.
     */
    private void changePresenceStatusForContact(
                                            ContactGibberishImpl contact
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
        Iterator<Contact> childContacts = parent.contacts();

        while(childContacts.hasNext())
        {
            ContactGibberishImpl contact
                = (ContactGibberishImpl)childContacts.next();

            if(findProviderForGibberishUserID(contact.getAddress()) != null)
            {
                //this is a contact corresponding to another SIP Communicator
                //provider so we won't change it's status here.
                continue;
            }
            PresenceStatus oldStatus = contact.getPresenceStatus();
            contact.setPresenceStatus(newStatus);

            fireContactPresenceStatusChangeEvent(
                contact, parent, oldStatus);
        }

        //now call this method recursively for all subgroups
        Iterator<ContactGroup> subgroups = parent.subgroups();

        while(subgroups.hasNext())
        {
            ContactGroup subgroup = subgroups.next();
            changePresenceStatusForAllContacts(subgroup, newStatus);
        }
    }

    /**
     * Returns the group that is parent of the specified gibberishGroup  or null
     * if no parent was found.
     * @param gibberishGroup the group whose parent we're looking for.
     * @return the ContactGroupGibberishImpl instance that gibberishGroup
     * belongs to or null if no parent was found.
     */
    public ContactGroupGibberishImpl findGroupParent(
                                    ContactGroupGibberishImpl gibberishGroup)
    {
        return contactListRoot.findGroupParent(gibberishGroup);
    }

    /**
     * Returns the group that is parent of the specified gibberishContact  or
     * null if no parent was found.
     * @param gibberishContact the contact whose parent we're looking for.
     * @return the ContactGroupGibberishImpl instance that gibberishContact
     * belongs to or null if no parent was found.
     */
    public ContactGroupGibberishImpl findContactParent(
                                        ContactGibberishImpl gibberishContact)
    {
        return (ContactGroupGibberishImpl)gibberishContact
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
        ContactGroupGibberishImpl gibberishGroup
                                        = (ContactGroupGibberishImpl)group;

        ContactGroupGibberishImpl parent = findGroupParent(gibberishGroup);

        if(parent == null){
            throw new IllegalArgumentException(
                "group " + group
                + " does not seem to belong to this protocol's contact list.");
        }

        parent.removeSubGroup(gibberishGroup);

        this.fireServerStoredGroupEvent(
            gibberishGroup, ServerStoredGroupEvent.GROUP_REMOVED_EVENT);
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
        ((ContactGroupGibberishImpl)group).setGroupName(newName);

        this.fireServerStoredGroupEvent(
                group,
                ServerStoredGroupEvent.GROUP_RENAMED_EVENT);
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
        ContactGibberishImpl contact = new ContactGibberishImpl(
            contactIdentifier
            , parentProvider);

        if(authorizationHandler != null)
        {
            //we require authorizations in gibberish
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
        ((ContactGroupGibberishImpl)parent).addContact(contact);

        fireSubscriptionEvent(contact,
                              parent,
                              SubscriptionEvent.SUBSCRIPTION_CREATED);
        //if the newly added contact corresponds to another provider - set their
        //status accordingly
        ProtocolProviderServiceGibberishImpl gibProvider
            = findProviderForGibberishUserID(contactIdentifier);
        if(gibProvider != null)
        {
            OperationSetPersistentPresence opSetPresence
                = gibProvider
                    .getOperationSet(OperationSetPersistentPresence.class);

            changePresenceStatusForContact(
                contact,
                opSetPresence.getPresenceStatus());
        }
        else
        {
            //otherwise - since we are not a real protocol, we set the contact
            //presence status ourselves
            changePresenceStatusForContact(contact, getPresenceStatus());
        }

        //notify presence listeners for the status change.
        fireContactPresenceStatusChangeEvent(contact
                                             , parent
                                             , GibberishStatusEnum.OFFLINE);
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
        String userID = contact.getAddress();

        //if the user id is our own id, then this request is being routed to us
        //from another instance of the gibberish provider.
        if (userID.equals(this.parentProvider.getAccountID().getUserID()))
        {
            //check who is the provider sending the message
            String sourceUserID = contact.getProtocolProvider()
                .getAccountID().getUserID();

            //check whether they are in our contact list
            Contact from = findContactByID(sourceUserID);

            //and if not - add them there as volatile.
            if (from == null)
            {
                from = createVolatileContact(sourceUserID);
            }

            //and now handle the request.
            return authorizationHandler.processAuthorisationRequest(
                request, from);
        }
        else
        {
            //if userID is not our own, try a check whether another provider
            //has that id and if yes - deliver the request to them.
            ProtocolProviderServiceGibberishImpl gibberishProvider
                = this.findProviderForGibberishUserID(userID);
            if (gibberishProvider != null)
            {
                OperationSetPersistentPresenceGibberishImpl opSetPersPresence
                    = (OperationSetPersistentPresenceGibberishImpl)
                    gibberishProvider.getOperationSet(
                        OperationSetPersistentPresence.class);
                return opSetPersPresence
                    .deliverAuthorizationRequest(request, contact);
            }
            else
            {
                //if we got here then "to" is simply someone in our contact
                //list so let's just simulate a reciproce request and generate
                //a response accordingly.

                //pretend that the remote contact is asking for authorization
                authorizationHandler.processAuthorisationRequest(
                    request, contact);

                //and now pretend that the remote contact has granted us
                //authorization
                return new AuthorizationResponse(AuthorizationResponse.ACCEPT
                                                , "You are welcome!");
            }
        }
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
        ContactGroupGibberishImpl parentGroup
            = (ContactGroupGibberishImpl)((ContactGibberishImpl)contact)
            .getParentContactGroup();

        parentGroup.removeContact((ContactGibberishImpl)contact);

        fireSubscriptionEvent(
            contact,
            contact.getParentContactGroup(),
            SubscriptionEvent.SUBSCRIPTION_REMOVED);
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
        ContactGibberishImpl contact = new ContactGibberishImpl(
            address
            , parentProvider);
        contact.setResolved(false);

        ( (ContactGroupGibberishImpl) parent).addContact(contact);

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
     * Looks for a gibberish protocol provider registered for a user id matching
     * <tt>gibberishUserID</tt>.
     *
     * @param gibberishUserID the ID of the Gibberish user whose corresponding
     * protocol provider we'd like to find.
     * @return ProtocolProviderServiceGibberishImpl a gibberish protocol
     * provider registered for a user with id <tt>gibberishUserID</tt> or null
     * if there is no such protocol provider.
     */
    public ProtocolProviderServiceGibberishImpl
                        findProviderForGibberishUserID(String gibberishUserID)
    {
        BundleContext bc = GibberishActivator.getBundleContext();

        String osgiQuery = "(&"
                + "(" + ProtocolProviderFactory.PROTOCOL
                + "=Gibberish)"
                + "(" + ProtocolProviderFactory.USER_ID
                + "=" + gibberishUserID + ")"
                + ")";

        ServiceReference[] refs = null;
        try
        {
            refs = bc.getServiceReferences(
                ProtocolProviderService.class.getName()
                ,osgiQuery);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("Failed to execute the following osgi query: "
                         + osgiQuery
                         , ex);
        }

        if(refs != null && refs.length > 0)
        {
            return (ProtocolProviderServiceGibberishImpl)bc.getService(refs[0]);
        }

        return null;
    }

    /**
     * Looks for gibberish protocol providers that have added us to their
     * contact list and returns list of all contacts representing us in these
     * providers.
     *
     * @return a list of all contacts in other providers' contact lists that
     * point to us.
     */
    public List<Contact> findContactsPointingToUs()
    {
        List<Contact> contacts = new LinkedList<Contact>();
        BundleContext bc = GibberishActivator.getBundleContext();

        String osgiQuery =
                "(" + ProtocolProviderFactory.PROTOCOL
                + "=Gibberish)";

        ServiceReference[] refs = null;
        try
        {
            refs = bc.getServiceReferences(
                ProtocolProviderService.class.getName()
                ,osgiQuery);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("Failed to execute the following osgi query: "
                         + osgiQuery
                         , ex);
        }

        for (int i =0; refs != null && i < refs.length; i++)
        {
            ProtocolProviderServiceGibberishImpl gibProvider
               = (ProtocolProviderServiceGibberishImpl)bc.getService(refs[i]);

           OperationSetPersistentPresenceGibberishImpl opSetPersPresence
               = (OperationSetPersistentPresenceGibberishImpl)gibProvider
                    .getOperationSet(OperationSetPersistentPresence.class);

            Contact contact = opSetPersPresence.findContactByID(
                parentProvider.getAccountID().getUserID());

            if (contact != null)
                contacts.add(contact);
        }

        return contacts;
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
        ContactGroupGibberishImpl newGroup
            = new ContactGroupGibberishImpl(
                ContactGroupGibberishImpl.createNameFromUID(groupUID)
                , parentProvider);
        newGroup.setResolved(false);

        //if parent is null then we're adding under root.
        if(parentGroup == null)
            parentGroup = getServerStoredContactListRoot();

        ((ContactGroupGibberishImpl)parentGroup).addSubgroup(newGroup);

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
         * that the gibberish provider has unregistered so that it would fire
         * status change events for all contacts in our buddy list.
         *
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if (! evt.getNewState().equals(RegistrationState.UNREGISTERED)
                && !evt.getNewState().equals(RegistrationState.AUTHENTICATION_FAILED)
                && !evt.getNewState().equals(RegistrationState.CONNECTION_FAILED))
            {
                return;
            }

            //send event notifications saying that all our buddies are
            //offline. The icq protocol does not implement top level buddies
            //nor subgroups for top level groups so a simple nested loop
            //would be enough.
            Iterator<ContactGroup> groupsIter
                = getServerStoredContactListRoot().subgroups();
            while (groupsIter.hasNext())
            {
                ContactGroup group = groupsIter.next();
                Iterator<Contact> contactsIter = group.contacts();

                while (contactsIter.hasNext())
                {
                    ContactGibberishImpl contact
                        = (ContactGibberishImpl) contactsIter.next();
                    PresenceStatus oldContactStatus
                        = contact.getPresenceStatus();

                    if (!oldContactStatus.isOnline())
                        continue;

                    contact.setPresenceStatus(GibberishStatusEnum.OFFLINE);

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
    private ContactGroupGibberishImpl getNonPersistentGroup()
    {
        for (int i = 0
             ; i < getServerStoredContactListRoot().countSubgroups()
             ; i++)
        {
            ContactGroupGibberishImpl gr =
                (ContactGroupGibberishImpl)getServerStoredContactListRoot()
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
    public ContactGibberishImpl createVolatileContact(String contactAddress)
    {
        //First create the new volatile contact;
        ContactGibberishImpl newVolatileContact
            = new ContactGibberishImpl(contactAddress
                                       , this.parentProvider);
        newVolatileContact.setPersistent(false);


        //Check whether a volatile group already exists and if not create
        //one
        ContactGroupGibberishImpl theVolatileGroup = getNonPersistentGroup();


        //if the parent volatile group is null then we create it
        if (theVolatileGroup == null)
        {
            theVolatileGroup = new ContactGroupGibberishImpl(
                GibberishActivator.getResources().getI18NString(
                    "service.gui.NOT_IN_CONTACT_LIST_GROUP_NAME")
                , parentProvider);
            theVolatileGroup.setResolved(false);
            theVolatileGroup.setPersistent(false);

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
