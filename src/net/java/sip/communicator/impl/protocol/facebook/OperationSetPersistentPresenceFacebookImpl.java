/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * A Facebook implementation of a persistent presence operation set. In order to
 * simulate server persistence, this operation set would simply accept all
 * unresolved contacts and resolve them immediately. A real world protocol
 * implementation would save it on a server using methods provided by the
 * protocol stack.
 *
 * @author Dai Zhiwei
 * @author Edgar Poce
 */
public class OperationSetPersistentPresenceFacebookImpl
    extends AbstractOperationSetPersistentPresence<ProtocolProviderServiceFacebookImpl>
{
    private static final Logger logger
        = Logger.getLogger(OperationSetPersistentPresenceFacebookImpl.class);

    /**
     * The root of the facebook contact list.
     */
    private ContactGroupFacebookImpl contactListRoot = null;

    /**
     * The currently active status message.
     */
    private String statusMessage = "Default Status Message";

    /**
     * Our presence status.
     */
    private PresenceStatus presenceStatus = FacebookStatusEnum.OFFLINE;

    /**
     * The <tt>AuthorizationHandler</tt> instance that we'd have to transmit
     * authorization requests to for approval.
     */
    private AuthorizationHandler authorizationHandler = null;

    /**
     * Creates an instance of this operation set keeping a reference to the
     * specified parent <tt>provider</tt>.
     *
     * @param provider the ProtocolProviderServiceFacebookImpl instance that
     *            created us.
     */
    public OperationSetPersistentPresenceFacebookImpl(
        ProtocolProviderServiceFacebookImpl provider)
    {
        super(provider);

        contactListRoot = new ContactGroupFacebookImpl("RootGroup", provider);

        // add our unregistration listener
        parentProvider
            .addRegistrationStateChangeListener(new UnregistrationListener());
    }

    /**
     * Creates a group with the specified name and parent in the server stored
     * contact list.
     *
     * @param parent the group where the new group should be created
     * @param groupName the name of the new group to create.
     */
    public void createServerStoredContactGroup(ContactGroup parent,
        String groupName)
    {
        ContactGroupFacebookImpl newGroup =
            new ContactGroupFacebookImpl(groupName, parentProvider);

        ((ContactGroupFacebookImpl) parent).addSubgroup(newGroup);

        this.fireServerStoredGroupEvent(newGroup,
            ServerStoredGroupEvent.GROUP_CREATED_EVENT);
    }

    /**
     * A Facebook Provider method to use for fast filling of a contact list.
     *
     * @param contactGroup the group to add
     */
    public void addFacebookGroup(ContactGroupFacebookImpl contactGroup)
    {
        contactListRoot.addSubgroup(contactGroup);
    }

    /**
     * A Facebook Provider method to use for fast filling of a contact list.
     * This method would add both the group and fire an event.
     *
     * @param parent the group where <tt>contactGroup</tt> should be added.
     * @param contactGroup the group to add
     */
    public void addFacebookGroupAndFireEvent(ContactGroupFacebookImpl parent,
        ContactGroupFacebookImpl contactGroup)
    {
        parent.addSubgroup(contactGroup);

        this.fireServerStoredGroupEvent(contactGroup,
            ServerStoredGroupEvent.GROUP_CREATED_EVENT);
    }

    /**
     * Returns a reference to the contact with the specified ID in case we have
     * a subscription for it and null otherwise/
     *
     * @param contactID a String identifier of the contact which we're seeking a
     *            reference of.
     * @return a reference to the Contact with the specified <tt>contactID</tt>
     *         or null if we don't have a subscription for the that identifier.
     */
    public Contact findContactByID(String contactID)
    {
        return contactListRoot.findContactByID(contactID);
    }

    /**
     * Sets the specified status message.
     *
     * @deprecated I don't find any method invoke this method.
     *  We should use publishPresenceStatus() to set status message.
     *
     * @param statusMessage a String containing the new status message.
     */
    @Deprecated
    public void setStatusMessage(String statusMessage)
    {
        this.statusMessage = statusMessage;
    }

    /**
     * Returns the status message that was last set through
     * setCurrentStatusMessage.
     *
     * @return the last status message that we have requested and the aim server
     *         has confirmed.
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
     *         implementation is communicating on behalf of.
     */
    public Contact getLocalContact()
    {
        return null;
    }

    /**
     * Returns a PresenceStatus instance representing the state this provider is
     * currently in.
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
     * @return the root ContactGroup for the ContactList stored by this service.
     */
    public ContactGroup getServerStoredContactListRoot()
    {
        return contactListRoot;
    }

    /**
     * Returns the set of PresenceStatus objects that a user of this service may
     * request the provider to enter.
     *
     * @return Iterator a PresenceStatus array containing "enterable" status
     *         instances.
     */
    public Iterator<PresenceStatus> getSupportedStatusSet()
    {
        return FacebookStatusEnum.supportedStatusSet();
    }

    /**
     * Removes the specified contact from its current parent and places it under
     * <tt>newParent</tt>.
     *
     * @param contactToMove the <tt>Contact</tt> to move
     * @param newParent the <tt>ContactGroup</tt> where <tt>Contact</tt>
     *            would be placed.
     */
    public void moveContactToGroup(Contact contactToMove, ContactGroup newParent)
    {
        ContactFacebookImpl facebookContact =
            (ContactFacebookImpl) contactToMove;

        ContactGroupFacebookImpl parentFacebookGroup =
            findContactParent(facebookContact);

        parentFacebookGroup.removeContact(facebookContact);

        // if this is a volatile contact then we haven't really subscribed to
        // them so we'd need to do so here
        if (!facebookContact.isPersistent())
        {
            // first tell everyone that the volatile contact was removed
            fireSubscriptionEvent(facebookContact, parentFacebookGroup,
                SubscriptionEvent.SUBSCRIPTION_REMOVED);

            try
            {
                // now subscribe
                this.subscribe(newParent, contactToMove.getAddress());

                // now tell everyone that we've added the contact
                fireSubscriptionEvent(facebookContact, newParent,
                    SubscriptionEvent.SUBSCRIPTION_CREATED);
            }
            catch (Exception ex)
            {
                logger.error("Failed to move contact "
                    + facebookContact.getAddress(), ex);
            }
        }
        else
        {
            try
            {
                ((ContactGroupFacebookImpl) newParent)
                    .addContact(facebookContact);

                fireSubscriptionMovedEvent(contactToMove, parentFacebookGroup,
                    newParent);
            }
            catch (OperationFailedException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Requests the provider to enter into a status corresponding to the
     * specified paramters.
     *
     * @param status the PresenceStatus as returned by getRequestableStatusSet
     * @param statusMessage the message that should be set as the reason to
     *            enter that status
     * @throws IllegalArgumentException if the status requested is not a valid
     *             PresenceStatus supported by this provider.
     * @throws IllegalStateException if the provider is not currently
     *             registered.
     * @throws OperationFailedException with code NETWORK_FAILURE if publishing
     *             the status fails due to a network error.
     */
    public void publishPresenceStatus(PresenceStatus status,
        String statusMessage)
        throws IllegalArgumentException,
        IllegalStateException,
        OperationFailedException
    {
        PresenceStatus oldPresenceStatus = this.presenceStatus;
        this.presenceStatus = status;

        //OK, now post the new status message!
        if(statusMessage != null && !statusMessage.equals(""))
        {
            if(this.statusMessage == null || !this.statusMessage.equals(statusMessage))
            {
                FacebookAdapter adapter = parentProvider.getAdapter();
                if(parentProvider != null && adapter != null)
                    adapter.setStatusMessage(statusMessage);
            }
        }

        this.statusMessage = statusMessage;

        this.fireProviderStatusChangeEvent(oldPresenceStatus);

        try
        {
            if(this.presenceStatus == FacebookStatusEnum.OFFLINE)
            {
                parentProvider.getAdapter().shutdown();
                changePresenceStatusForAllContactsWithoutFiringEvent(
                    getServerStoredContactListRoot(),
                    getPresenceStatus());
            }
            else if(this.presenceStatus == FacebookStatusEnum.ONLINE)
            {
                //parentProvider.getAdapter().initialize(email, pass)
                if(oldPresenceStatus == FacebookStatusEnum.INVISIBLE)
                    parentProvider.getAdapter().getSession().setVisibility(true);
            }
            else if(this.presenceStatus == FacebookStatusEnum.INVISIBLE)
            {
                parentProvider.getAdapter().getSession().setVisibility(false);
            }
        }
        catch (IOException e)
        {
            throw new OperationFailedException(
                    "unable to change facebook visibility", -1, e);
        }
        catch (BrokenFacebookProtocolException e)
        {
            throw new OperationFailedException(
                    "unable to change facebook visibility", -1, e);
        }


        /*// since we are not a real protocol, we set the contact presence status
        // ourselves and make them have the same status as ours.
        changePresenceStatusForAllContacts(getServerStoredContactListRoot(),
            getPresenceStatus());

        // now check whether we are in someone else's contact list and modify
        // our status there
        List contacts = findContactsPointingToUs();

        Iterator contactsIter = contacts.iterator();
        while (contactsIter.hasNext())
        {
            ContactFacebookImpl contact =
                (ContactFacebookImpl) contactsIter.next();

            PresenceStatus oldStatus = contact.getPresenceStatus();
            contact.setPresenceStatus(status);
            contact.getParentPresenceOperationSet()
                .fireContactPresenceStatusChangeEvent(contact,
                    contact.getParentContactGroup(), oldStatus);

        }*/
    }

    /**
     * Get the PresenceStatus for a particular contact.
     *
     * @param contactIdentifier the identifier of the contact whose status we're
     *            interested in.
     * @return PresenceStatus the <tt>PresenceStatus</tt> of the specified
     *         <tt>contact</tt>
     * @throws IllegalArgumentException if <tt>contact</tt> is not a contact
     *             known to the underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is not
     *             registered/signed on a public service.
     * @throws OperationFailedException with code NETWORK_FAILURE if retrieving
     *             the status fails due to errors experienced during network
     *             communication
     */
    public PresenceStatus queryContactStatus(String contactIdentifier)
        throws IllegalArgumentException,
        IllegalStateException,
        OperationFailedException
    {
        return findContactByID(contactIdentifier).getPresenceStatus();
    }

    /**
     * Sets the presence status of <tt>contact</tt> to <tt>newStatus</tt>.
     *
     * @param contact the <tt>ContactFacebookImpl</tt> whose status we'd like
     *            to set.
     * @param newStatus the new status we'd like to set to <tt>contact</tt>.
     */
    private void changePresenceStatusForContact(ContactFacebookImpl contact,
        PresenceStatus newStatus)
    {
        if (contact == null)
            return;
        PresenceStatus oldStatus = contact.getPresenceStatus();
        contact.setPresenceStatus(newStatus);

        fireContactPresenceStatusChangeEvent(contact,
            findContactParent(contact), oldStatus);
    }

    /**
     * Sets the presence status of the contact who has the <tt>adress</tt> to <tt>newStatus</tt>.
     * If we can't find the contact, we subscribe him/her.
     * @param address the contact's address
     * @param newStatus the new status we'd like to set to <tt>contact</tt>.
     */
    public void setPresenceStatusForContact(String address,
            PresenceStatus newStatus)
    {
        ContactFacebookImpl contact = (ContactFacebookImpl) findContactByID(address);
        if(contact == null)
        {
            try
            {
                subscribe(address);
                contact = (ContactFacebookImpl) findContactByID(address);
            }
            catch (IllegalArgumentException e)
            {
                logger.warn(e.getMessage());
            }
            catch (IllegalStateException e)
            {
                logger.warn(e.getMessage());
            }
            catch (OperationFailedException e)
            {
                logger.warn(e.getMessage());
            }
        }
        changePresenceStatusForContact(contact, newStatus);
    }

    /**
     * The same as changePresenceStatusForContact, but is public
     * @param contact the <tt>ContactFacebookImpl</tt> whose status we'd like
     *            to set.
     * @param newStatus the new status we'd like to set to <tt>contact</tt>.
     */
    public void setPresenceStatusForContact(ContactFacebookImpl contact,
        PresenceStatus newStatus)
    {
        changePresenceStatusForContact(contact, newStatus);
    }

    /**
     * Sets the presence status of all <tt>contact</tt>s in our contact list
     * (except those that correspond to the other providers registered with SC) to
     * <tt>newStatus</tt>.
     *
     * @param newStatus the new status we'd like to set to <tt>contact</tt>.
     * @param parent the group in which we'd have to update the status of all
     *            direct and indirect child contacts.
     */
    private void changePresenceStatusForAllContacts(ContactGroup parent,
        PresenceStatus newStatus)
    {
        // first set the status for contacts in this group
        Iterator<Contact> childContacts = parent.contacts();

        while (childContacts.hasNext())
        {
            ContactFacebookImpl contact =
                (ContactFacebookImpl) childContacts.next();

            if (findProviderForFacebookUserID(contact.getAddress()) != null)
            {
                // this is a contact corresponding to another SIP Communicator
                // provider so we won't change it's status here.
                continue;
            }
            PresenceStatus oldStatus = contact.getPresenceStatus();
            contact.setPresenceStatus(newStatus);

            fireContactPresenceStatusChangeEvent(contact, parent, oldStatus);
        }

        // now call this method recursively for all subgroups
        Iterator<ContactGroup> subgroups = parent.subgroups();

        while (subgroups.hasNext())
        {
            ContactGroup subgroup = subgroups.next();
            changePresenceStatusForAllContacts(subgroup, newStatus);
        }
    }

    /**
     * Sets the presence status of all <tt>contact</tt>s in our contact list
     * (except those that correspond to another provider registered with SC) to
     * <tt>newStatus</tt>.
     *
     * @param newStatus the new status we'd like to set to <tt>contact</tt>.
     * @param parent the group in which we'd have to update the status of all
     *            direct and indirect child contacts.
     */
    private void changePresenceStatusForAllContactsWithoutFiringEvent(
        ContactGroup parent, PresenceStatus newStatus)
    {
        // first set the status for contacts in this group
        Iterator<Contact> childContacts = parent.contacts();

        while (childContacts.hasNext())
        {
            ContactFacebookImpl contact =
                (ContactFacebookImpl) childContacts.next();

            if (findProviderForFacebookUserID(contact.getAddress()) != null)
            {
                // this is a contact corresponding to another SIP Communicator
                // provider so we won't change it's status here.
                continue;
            }
            //PresenceStatus oldStatus = contact.getPresenceStatus();
            contact.setPresenceStatus(newStatus);

            /*
             * fireContactPresenceStatusChangeEvent( contact, parent,
             * oldStatus);
             */
        }

        // now call this method recursively for all subgroups
        Iterator<ContactGroup> subgroups = parent.subgroups();

        while (subgroups.hasNext())
        {
            ContactGroup subgroup = subgroups.next();
            changePresenceStatusForAllContactsWithoutFiringEvent(subgroup,
                newStatus);
        }
    }

    public void setPresenceStatusForAllContacts(PresenceStatus newStatus)
    {
        changePresenceStatusForAllContactsWithoutFiringEvent(
            getServerStoredContactListRoot(), newStatus);
    }

    /**
     * Returns the group that is parent of the specified facebookGroup or null
     * if no parent was found.
     *
     * @param facebookGroup the group whose parent we're looking for.
     * @return the ContactGroupFacebookImpl instance that facebookGroup belongs
     *         to or null if no parent was found.
     */
    public ContactGroupFacebookImpl findGroupParent(
        ContactGroupFacebookImpl facebookGroup)
    {
        return contactListRoot.findGroupParent(facebookGroup);
    }

    /**
     * Returns the group that is parent of the specified facebookContact or null
     * if no parent was found.
     *
     * @param facebookContact the contact whose parent we're looking for.
     * @return the ContactGroupFacebookImpl instance that facebookContact
     *         belongs to or null if no parent was found.
     */
    public ContactGroupFacebookImpl findContactParent(
        ContactFacebookImpl facebookContact)
    {
        return (ContactGroupFacebookImpl) facebookContact
            .getParentContactGroup();
    }

    /**
     * Removes the specified group from the server stored contact list.
     *
     * @param group the group to remove.
     *
     * @throws IllegalArgumentException if <tt>group</tt> was not found in
     *             this protocol's contact list.
     */
    public void removeServerStoredContactGroup(ContactGroup group)
        throws IllegalArgumentException
    {
        ContactGroupFacebookImpl facebookGroup =
            (ContactGroupFacebookImpl) group;

        ContactGroupFacebookImpl parent = findGroupParent(facebookGroup);

        if (parent == null)
        {
            throw new IllegalArgumentException("group " + group
                + " does not seem to belong to this protocol's contact list.");
        }

        parent.removeSubGroup(facebookGroup);

        this.fireServerStoredGroupEvent(facebookGroup,
            ServerStoredGroupEvent.GROUP_REMOVED_EVENT);
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
        ((ContactGroupFacebookImpl) group).setGroupName(newName);

        this.fireServerStoredGroupEvent((ContactGroupFacebookImpl) group,
            ServerStoredGroupEvent.GROUP_RENAMED_EVENT);
    }

    /**
     * Handler for incoming authorization requests.
     *
     * @param handler an instance of an AuthorizationHandler for authorization
     *            requests coming from other users requesting permission add us
     *            to their contact list.
     */
    public void setAuthorizationHandler(AuthorizationHandler handler)
    {
        this.authorizationHandler = handler;
    }

    /**
     * Persistently adds a subscription for the presence status of the contact
     * corresponding to the specified contactIdentifier and indicates that it
     * should be added to the specified group of the server stored contact list.
     *
     * @param parent the parent group of the server stored contact list where
     *            the contact should be added.
     *            <p>
     * @param contactIdentifier the contact whose status updates we are
     *            subscribing for.
     * @throws IllegalArgumentException if <tt>contact</tt> or <tt>parent</tt>
     *             are not a contact known to the underlying protocol provider.
     * @throws IllegalStateException if the underlying protocol provider is not
     *             registered/signed on a public service.
     * @throws OperationFailedException with code NETWORK_FAILURE if subscribing
     *             fails due to errors experienced during network communication
     */
    public void subscribe(ContactGroup parent, String contactIdentifier)
        throws IllegalArgumentException,
        IllegalStateException,
        OperationFailedException
    {
        ContactFacebookImpl contact =
            new ContactFacebookImpl(contactIdentifier, parentProvider);

        ((ContactGroupFacebookImpl) parent).addContact(contact);

        fireSubscriptionEvent(contact, parent,
            SubscriptionEvent.SUBSCRIPTION_CREATED);
        // if the newly added contact corresponds to another provider - set
        // their
        // status accordingly
        ProtocolProviderServiceFacebookImpl fbProvider =
            findProviderForFacebookUserID(contactIdentifier);
        if (fbProvider != null)
        {
            OperationSetPersistentPresence opSetPresence
                = (OperationSetPersistentPresence)
                    fbProvider
                        .getOperationSet(OperationSetPersistentPresence.class);

            changePresenceStatusForContact(contact,
                (FacebookStatusEnum) opSetPresence.getPresenceStatus());
        }
        else
        {
            // otherwise - since we are not a real protocol, we set the contact
            // presence status ourselves
            changePresenceStatusForContact(contact, getPresenceStatus());
        }

        // notify presence listeners for the status change.
        fireContactPresenceStatusChangeEvent(contact, parent,
            FacebookStatusEnum.OFFLINE);
    }

    /**
     * Depending on whether <tt>contact</tt> corresponds to another protocol
     * provider installed in sip-communicator, this method would either deliver
     * it to that provider or simulate a corresponding request from the
     * destination contact and make return a response after it has received one
     * If the destination contact matches us, then we'll ask the user to act
     * upon the request, and return the response.
     *
     * @param request the authorization request that we'd like to deliver to the
     *            desination <tt>contact</tt>.
     * @param contact the <tt>Contact</tt> to notify
     *
     * @return the <tt>AuthorizationResponse</tt> that has been given or
     *         generated in response to <tt>request</tt>.
     */
    private AuthorizationResponse deliverAuthorizationRequest(
        AuthorizationRequest request, Contact contact)
    {
        String userID = contact.getAddress();

        // if the user id is our own id, then this request is being routed to us
        // from another instance of the facebook provider.
        if (userID.equals(this.parentProvider.getAccountID().getUserID()))
        {
            // check who is the provider sending the message
            String sourceUserID =
                contact.getProtocolProvider().getAccountID().getUserID();

            // check whether they are in our contact list
            Contact from = findContactByID(sourceUserID);

            // and if not - add them there as volatile.
            if (from == null)
            {
                from = createVolatileContact(sourceUserID);
            }

            // and now handle the request.
            return authorizationHandler.processAuthorisationRequest(request,
                from);
        }
        else
        {
            // if userID is not our own, try a check whether another provider
            // has that id and if yes - deliver the request to them.
            ProtocolProviderServiceFacebookImpl facebookProvider
                = this.findProviderForFacebookUserID(userID);
            if (facebookProvider != null)
            {
                OperationSetPersistentPresenceFacebookImpl opSetPersPresence
                    = (OperationSetPersistentPresenceFacebookImpl)
                        facebookProvider
                            .getOperationSet(
                                OperationSetPersistentPresence.class);
                return opSetPersPresence.deliverAuthorizationRequest(request,
                    contact);
            }
            else
            {
                // if we got here then "to" is simply someone in our contact
                // list so let's just simulate a reciproce request and generate
                // a response accordingly.

                // pretend that the remote contact is asking for authorization
                authorizationHandler.processAuthorisationRequest(request,
                    contact);

                // and now pretend that the remote contact has granted us
                // authorization
                return new AuthorizationResponse(AuthorizationResponse.ACCEPT,
                    "You are welcome!");
            }
        }
    }

    /**
     * Adds a subscription for the presence status of the contact corresponding
     * to the specified contactIdentifier.
     *
     * @param contactIdentifier the identifier of the contact whose status
     *            updates we are subscribing for.
     *            <p>
     * @throws IllegalArgumentException if <tt>contact</tt> is not a contact
     *             known to the underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is not
     *             registered/signed on a public service.
     * @throws OperationFailedException with code NETWORK_FAILURE if subscribing
     *             fails due to errors experienced during network communication
     */
    public void subscribe(String contactIdentifier)
        throws IllegalArgumentException,
        IllegalStateException,
        OperationFailedException
    {
        subscribe(contactListRoot, contactIdentifier);
    }

    /**
     * Removes a subscription for the presence status of the specified contact.
     *
     * @param contact the contact whose status updates we are unsubscribing
     *            from.
     * @throws IllegalArgumentException if <tt>contact</tt> is not a contact
     *             known to the underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is not
     *             registered/signed on a public service.
     * @throws OperationFailedException with code NETWORK_FAILURE if
     *             unsubscribing fails due to errors experienced during network
     *             communication
     */
    public void unsubscribe(Contact contact)
        throws IllegalArgumentException,
        IllegalStateException,
        OperationFailedException
    {
        ContactGroupFacebookImpl parentGroup =
            (ContactGroupFacebookImpl) ((ContactFacebookImpl) contact)
                .getParentContactGroup();

        parentGroup.removeContact((ContactFacebookImpl) contact);

        fireSubscriptionEvent((ContactFacebookImpl) contact,
            parentGroup,
            SubscriptionEvent.SUBSCRIPTION_REMOVED);
    }

    /**
     * Creates and returns a unresolved contact from the specified
     * <tt>address</tt> and <tt>persistentData</tt>. The method will not
     * try to establish a network connection and resolve the newly created
     * Contact against the server. The protocol provider may will later try and
     * resolve the contact. When this happens the corresponding event would
     * notify interested subscription listeners.
     *
     * @param address an identifier of the contact that we'll be creating.
     * @param persistentData a String returned Contact's getPersistentData()
     *            method during a previous run and that has been persistently
     *            stored locally.
     * @return the unresolved <tt>Contact</tt> created from the specified
     *         <tt>address</tt> and <tt>persistentData</tt>
     */
    public Contact createUnresolvedContact(String address, String persistentData)
    {
        return createUnresolvedContact(address, persistentData,
            getServerStoredContactListRoot());
    }

    /**
     * Creates and returns a unresolved contact from the specified
     * <tt>address</tt> and <tt>persistentData</tt>. The method will not
     * try to establish a network connection and resolve the newly created
     * Contact against the server. The protocol provider may will later try and
     * resolve the contact. When this happens the corresponding event would
     * notify interested subscription listeners.
     *
     * @param address an identifier of the contact that we'll be creating.
     * @param persistentData a String returned Contact's getPersistentData()
     *            method during a previous run and that has been persistently
     *            stored locally.
     * @param parent the group where the unresolved contact is supposed to
     *            belong to.
     *
     * @return the unresolved <tt>Contact</tt> created from the specified
     *         <tt>address</tt> and <tt>persistentData</tt>
     */
    public Contact createUnresolvedContact(String address,
        String persistentData, ContactGroup parent)
    {
        ContactFacebookImpl contact =
            new ContactFacebookImpl(address, parentProvider);
        contact.setResolved(false);

        try
        {
            ((ContactGroupFacebookImpl) parent).addContact(contact);

            fireSubscriptionEvent(contact, parent,
                SubscriptionEvent.SUBSCRIPTION_CREATED);

            // since we don't have any server, we'll simply resolve the contact
            // ourselves as if we've just received an event from the server
            // telling
            // us that it has been resolved.
            fireSubscriptionEvent(contact, parent,
                SubscriptionEvent.SUBSCRIPTION_RESOLVED);

            // since we are not a real protocol, we set the contact presence
            // status
            // ourselves
            changePresenceStatusForContact(contact, getPresenceStatus());
        }
        catch (OperationFailedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return contact;
    }

    /**
     * Looks for a facebook protocol provider registered for a user id matching
     * <tt>facebookUserID</tt>.
     *
     * @param facebookUserID the ID of the Facebook user whose corresponding
     *            protocol provider we'd like to find.
     * @return ProtocolProviderServiceFacebookImpl a facebook protocol provider
     *         registered for a user with id <tt>facebookUserID</tt> or null
     *         if there is no such protocol provider.
     */
    public ProtocolProviderServiceFacebookImpl findProviderForFacebookUserID(
        String facebookUserID)
    {
        BundleContext bc = FacebookActivator.getBundleContext();

        String osgiQuery =
            "(&" + "(" + ProtocolProviderFactory.PROTOCOL + "=Facebook)" + "("
                + ProtocolProviderFactory.USER_ID + "=" + facebookUserID + ")"
                + ")";

        ServiceReference[] refs = null;
        try
        {
            refs =
                bc.getServiceReferences(
                    ProtocolProviderService.class.getName(), osgiQuery);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("Failed to execute the following osgi query: "
                + osgiQuery, ex);
        }

        if (refs != null && refs.length > 0)
        {
            return (ProtocolProviderServiceFacebookImpl) bc.getService(refs[0]);
        }

        return null;
    }

    /**
     * Looks for facebook protocol providers that have added us to their contact
     * list and returns list of all contacts representing us in these providers.
     *
     * @return a list of all contacts in other providers' contact lists that
     *         point to us.
     */
    public List<Contact> findContactsPointingToUs()
    {
        List<Contact> contacts = new LinkedList<Contact>();
        BundleContext bc = FacebookActivator.getBundleContext();

        String osgiQuery =
            "(" + ProtocolProviderFactory.PROTOCOL + "=Facebook)";

        ServiceReference[] refs = null;
        try
        {
            refs =
                bc.getServiceReferences(
                    ProtocolProviderService.class.getName(), osgiQuery);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("Failed to execute the following osgi query: "
                + osgiQuery, ex);
        }

        if (refs != null)
            for (ServiceReference ref : refs)
            {
                ProtocolProviderServiceFacebookImpl gibProvider
                    = (ProtocolProviderServiceFacebookImpl) bc.getService(ref);
                OperationSetPersistentPresenceFacebookImpl opSetPersPresence
                    = (OperationSetPersistentPresenceFacebookImpl)
                        gibProvider
                            .getOperationSet(
                                OperationSetPersistentPresence.class);
                Contact contact
                    = opSetPersPresence
                        .findContactByID(
                            parentProvider.getAccountID().getUserID());

                if (contact != null)
                    contacts.add(contact);
            }

        return contacts;
    }

    /**
     * Creates and returns a unresolved contact group from the specified
     * <tt>address</tt> and <tt>persistentData</tt>. The method will not
     * try to establish a network connection and resolve the newly created
     * <tt>ContactGroup</tt> against the server or the contact itself. The
     * protocol provider will later resolve the contact group. When this happens
     * the corresponding event would notify interested subscription listeners.
     *
     * @param groupUID an identifier, returned by ContactGroup's getGroupUID,
     *            that the protocol provider may use in order to create the
     *            group.
     * @param persistentData a String returned ContactGroups's
     *            getPersistentData() method during a previous run and that has
     *            been persistently stored locally.
     * @param parentGroup the group under which the new group is to be created
     *            or null if this is group directly underneath the root.
     * @return the unresolved <tt>ContactGroup</tt> created from the specified
     *         <tt>uid</tt> and <tt>persistentData</tt>
     */
    public ContactGroup createUnresolvedContactGroup(String groupUID,
        String persistentData, ContactGroup parentGroup)
    {
        ContactGroupFacebookImpl newGroup =
            new ContactGroupFacebookImpl(ContactGroupFacebookImpl
                .createNameFromUID(groupUID), parentProvider);
        newGroup.setResolved(false);

        // if parent is null then we're adding under root.
        if (parentGroup == null)
            parentGroup = getServerStoredContactListRoot();

        ((ContactGroupFacebookImpl) parentGroup).addSubgroup(newGroup);

        this.fireServerStoredGroupEvent(newGroup,
            ServerStoredGroupEvent.GROUP_CREATED_EVENT);

        return newGroup;
    }

    private class UnregistrationListener
        implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenver a
         * change in the registration state of the corresponding provider had
         * occurred. The method is particularly interested in events stating
         * that the facebook provider has unregistered so that it would fire
         * status change events for all contacts in our buddy list.
         *
         * @param evt ProviderStatusChangeEvent the event describing the status
         *            change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if (!evt.getNewState().equals(RegistrationState.UNREGISTERED)
                && !evt.getNewState().equals(
                    RegistrationState.AUTHENTICATION_FAILED)
                && !evt.getNewState().equals(
                    RegistrationState.CONNECTION_FAILED))
            {
                return;
            }

            // send event notifications saying that all our buddies are
            // offline. The icq protocol does not implement top level buddies
            // nor subgroups for top level groups so a simple nested loop
            // would be enough.
            Iterator<ContactGroup> groupsIter
                = getServerStoredContactListRoot().subgroups();
            while (groupsIter.hasNext())
            {
                ContactGroupFacebookImpl group =
                    (ContactGroupFacebookImpl) groupsIter.next();

                Iterator<Contact> contactsIter = group.contacts();

                while (contactsIter.hasNext())
                {
                    ContactFacebookImpl contact =
                        (ContactFacebookImpl) contactsIter.next();

                    PresenceStatus oldContactStatus =
                        contact.getPresenceStatus();

                    if (!oldContactStatus.isOnline())
                        continue;

                    contact.setPresenceStatus(FacebookStatusEnum.OFFLINE);

                    fireContactPresenceStatusChangeEvent(contact, contact
                        .getParentContactGroup(), oldContactStatus);
                }
            }
        }
    }

    /**
     * Returns the volatile group or null if this group has not yet been
     * created.
     *
     * @return a volatile group existing in our contact list or <tt>null</tt>
     *         if such a group has not yet been created.
     */
    private ContactGroupFacebookImpl getNonPersistentGroup()
    {
        for (int i = 0; i < getServerStoredContactListRoot().countSubgroups(); i++)
        {
            ContactGroupFacebookImpl gr =
                (ContactGroupFacebookImpl) getServerStoredContactListRoot()
                    .getGroup(i);

            if (!gr.isPersistent())
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
     *            create.
     * @return the newly created volatile contact.
     */
    public ContactFacebookImpl createVolatileContact(String contactAddress)
    {
        // First create the new volatile contact;
        ContactFacebookImpl newVolatileContact =
            new ContactFacebookImpl(contactAddress, this.parentProvider);
        newVolatileContact.setPersistent(false);

        // Check whether a volatile group already exists and if not create
        // one
        ContactGroupFacebookImpl theVolatileGroup = getNonPersistentGroup();

        // if the parent volatile group is null then we create it
        if (theVolatileGroup == null)
        {
            theVolatileGroup =
                new ContactGroupFacebookImpl("NotInContactList", parentProvider);
            theVolatileGroup.setResolved(false);
            theVolatileGroup.setPersistent(false);

            this.contactListRoot.addSubgroup(theVolatileGroup);

            fireServerStoredGroupEvent(theVolatileGroup,
                ServerStoredGroupEvent.GROUP_CREATED_EVENT);
        }

        // now add the volatile contact instide it
        try
        {
            theVolatileGroup.addContact(newVolatileContact);

            fireSubscriptionEvent(newVolatileContact, theVolatileGroup,
                SubscriptionEvent.SUBSCRIPTION_CREATED);
        }
        catch (OperationFailedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return newVolatileContact;
    }
}
