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
package net.java.sip.communicator.impl.protocol.ssh;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * A SSH implementation of a persistent presence operation set. In order
 * to simulate server persistence, this operation set would simply accept all
 * unresolved contacts and resolve them immediately. A real world protocol
 * implementation would save it on a server using methods provided by the
 * protocol stack.
 *
 * @author Shobhit Jindal
 */
public class OperationSetPersistentPresenceSSHImpl
    extends AbstractOperationSetPersistentPresence<ProtocolProviderServiceSSHImpl>
{
    private static final Logger logger =
            Logger.getLogger(OperationSetPersistentPresenceSSHImpl.class);

    /**
     * The root of the ssh contact list.
     */
    private ContactGroupSSHImpl contactListRoot = null;

    /**
     * The currently active status message.
     */
    private String statusMessage = "Online";

    /**
     * Our default presence status.
     */
    private PresenceStatus presenceStatus = SSHStatusEnum.ONLINE;

    /**
     * Creates an instance of this operation set keeping a reference to the
     * specified parent <tt>provider</tt>.
     * @param provider the ProtocolProviderServiceSSHImpl instance that
     * created us.
     */
    public OperationSetPersistentPresenceSSHImpl(
            ProtocolProviderServiceSSHImpl        provider)
    {
        super(provider);

        contactListRoot = new ContactGroupSSHImpl("RootGroup", provider);

        //add our unregistration listener
        parentProvider.addRegistrationStateChangeListener(
                new UnregistrationListener());
    }

    /**
     * This function changes the status of contact as well as that of the
     * provider
     *
     * @param sshContact the contact of the remote machine
     * @param newStatus new status of the contact
     */
    public void changeContactPresenceStatus(
            ContactSSH sshContact,
            PresenceStatus newStatus)
    {
        PresenceStatus oldStatus = sshContact.getPresenceStatus();
        sshContact.setPresenceStatus(newStatus);
        fireContactPresenceStatusChangeEvent(
                sshContact
                , sshContact.getParentContactGroup()
                , oldStatus);
        fireProviderStatusChangeEvent(oldStatus);
    }

    /**
     * Creates a group with the specified name and parent in the server
     * stored contact list.
     *
     * @param parent the group where the new group should be created
     * @param groupName the name of the new group to create.
     */
    public void createServerStoredContactGroup(
            ContactGroup parent,
            String groupName)
    {
        ContactGroupSSHImpl newGroup
                = new ContactGroupSSHImpl(groupName, parentProvider);

        ((ContactGroupSSHImpl)parent).addSubgroup(newGroup);

        this.fireServerStoredGroupEvent(
                newGroup, ServerStoredGroupEvent.GROUP_CREATED_EVENT);
    }

    /**
     * A SSH Provider method to use for fast filling of a contact list.
     *
     * @param contactGroup the group to add
     */
    public void addSSHGroup(ContactGroupSSHImpl contactGroup)
    {
        contactListRoot.addSubgroup(contactGroup);
    }

    /**
     * A SSH Provider method to use for fast filling of a contact list.
     * This method would add both the group and fire an event.
     *
     * @param parent the group where <tt>contactGroup</tt> should be added.
     * @param contactGroup the group to add
     */
    public void addSSHGroupAndFireEvent(
            ContactGroupSSHImpl parent,
            ContactGroupSSHImpl contactGroup)
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
        return SSHStatusEnum.supportedStatusSet();
    }

    /**
     * Removes the specified contact from its current parent and places it
     * under <tt>newParent</tt>.
     *
     * @param contactToMove the <tt>Contact</tt> to move
     * @param newParent the <tt>ContactGroup</tt> where <tt>Contact</tt>
     *   would be placed.
     */
    public void moveContactToGroup(
            Contact contactToMove,
            ContactGroup newParent)
    {
        ContactSSHImpl sshContact
                = (ContactSSHImpl)contactToMove;

        ContactGroupSSHImpl parentSSHGroup
                = findContactParent(sshContact);

        parentSSHGroup.removeContact(sshContact);

        //if this is a volatile contact then we haven't really subscribed to
        //them so we'd need to do so here
        if(!sshContact.isPersistent())
        {
            //first tell everyone that the volatile contact was removed
            fireSubscriptionEvent(sshContact
                    , parentSSHGroup
                    , SubscriptionEvent.SUBSCRIPTION_REMOVED);

            try
            {
                //now subscribe
                this.subscribe(newParent, contactToMove.getAddress());

                //now tell everyone that we've added the contact
                fireSubscriptionEvent(sshContact
                        , newParent
                        , SubscriptionEvent.SUBSCRIPTION_CREATED);
            }
            catch (Exception ex)
            {
                logger.error("Failed to move contact "
                        + sshContact.getAddress()
                        , ex);
            }
        }
        else
        {
            ( (ContactGroupSSHImpl) newParent)
            .addContact(sshContact);

            fireSubscriptionMovedEvent(contactToMove
                    , parentSSHGroup
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
     */
    public void publishPresenceStatus(
            PresenceStatus status,
            String statusMessage)
            throws IllegalArgumentException,
                   IllegalStateException
    {
        PresenceStatus oldPresenceStatus = this.presenceStatus;
        this.presenceStatus = status;
        this.statusMessage = statusMessage;

        this.fireProviderStatusChangeEvent(oldPresenceStatus);


//        //since we are not a real protocol, we set the contact presence status
//        //ourselves and make them have the same status as ours.
//        changePresenceStatusForAllContacts( getServerStoredContactListRoot()
//        , getPresenceStatus());
//
//        //now check whether we are in someone else's contact list and modify
//        //our status there
//        List contacts = findContactsPointingToUs();
//
//        Iterator contactsIter = contacts.iterator();
//        while (contactsIter.hasNext())
//        {
//            ContactSSHImpl contact
//                    = (ContactSSHImpl) contactsIter.next();
//
//            PresenceStatus oldStatus = contact.getPresenceStatus();
//            contact.setPresenceStatus(status);
//            contact.getParentPresenceOperationSet()
//            .fireContactPresenceStatusChangeEvent(
//                    contact
//                    , contact.getParentContactGroup()
//                    , oldStatus);
//
//        }
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
     * @param contact the <tt>ContactSSHImpl</tt> whose status we'd like
     * to set.
     * @param newStatus the new status we'd like to set to <tt>contact</tt>.
     */
    private void changePresenceStatusForContact(
            ContactSSH contact,
            PresenceStatus newStatus)
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
    private void changePresenceStatusForAllContacts(
            ContactGroup   parent,
            PresenceStatus newStatus)
    {
        //first set the status for contacts in this group
        Iterator<Contact> childContacts = parent.contacts();

        while(childContacts.hasNext())
        {
            ContactSSHImpl contact
                    = (ContactSSHImpl)childContacts.next();

            if(findProviderForSSHUserID(contact.getAddress()) != null)
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
     * Returns the group that is parent of the specified sshGroup  or null
     * if no parent was found.
     * @param sshGroup the group whose parent we're looking for.
     * @return the ContactGroupSSHImpl instance that sshGroup
     * belongs to or null if no parent was found.
     */
    public ContactGroupSSHImpl findGroupParent(
            ContactGroupSSHImpl sshGroup)
    {
        return contactListRoot.findGroupParent(sshGroup);
    }

    /**
     * Returns the group that is parent of the specified sshContact  or
     * null if no parent was found.
     * @param sshContact the contact whose parent we're looking for.
     * @return the ContactGroupSSHImpl instance that sshContact
     * belongs to or null if no parent was found.
     */
    public ContactGroupSSHImpl findContactParent(
            ContactSSH sshContact)
    {
        return (ContactGroupSSHImpl)sshContact
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
        ContactGroupSSHImpl sshGroup
                = (ContactGroupSSHImpl)group;

        ContactGroupSSHImpl parent = findGroupParent(sshGroup);

        if(parent == null)
        {
            throw new IllegalArgumentException(
                    "group " + group
                    + " does not seem to belong to this protocol's contact "
                    + "list.");
        }

        parent.removeSubGroup(sshGroup);

        this.fireServerStoredGroupEvent(
                sshGroup, ServerStoredGroupEvent.GROUP_REMOVED_EVENT);
    }

    /**
     * Renames the specified group from the server stored contact list.
     *
     * @param group the group to rename.
     * @param newName the new name of the group.
     */
    public void renameServerStoredContactGroup(
            ContactGroup group,
            String newName)
    {
        ((ContactGroupSSHImpl)group).setGroupName(newName);

        this.fireServerStoredGroupEvent(
                group, ServerStoredGroupEvent
                    .GROUP_RENAMED_EVENT);
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
    public void subscribe(
            ContactGroup parent,
            String contactIdentifier)
            throws IllegalArgumentException,
                   IllegalStateException,
                   OperationFailedException
    {
        ContactSSH sshContact = new ContactSSHImpl(contactIdentifier,
                parentProvider);

/*        ProtocolProviderServiceSSHImpl.getUIService().getConfigurationWindow()
                                                        .setVisible(true);
*/
        sshContact.setParentGroup((ContactGroupSSHImpl)parent);
        sshContact.getSSHConfigurationForm().setVisible(true);



/*        Gets the domain name or IP address of the sshContact machine via the
 *        UI Service Interface
          sshContact.setPersistentData(ProtocolProviderServiceSSHImpl
              .getUIService().getPopupDialog()
              .showInputPopupDialog("Enter Domain Name or IP Address of "
              + sshContact.getDisplayName()));

        // contact is added to list later after the user has provided
        // details in SSHConfigurationForm

        // addContactToList method is called
*/
    }

    /**
     * Add a contact to the specified group
     *
     * @param parent the group
     * @param sshContact the contact
     */
    public void addContactToList(
            ContactGroup parent,
            ContactSSH sshContact)
    {
        // Adds the sshContact to the sshContact list

        ((ContactGroupSSHImpl)parent).addContact(sshContact);

        fireSubscriptionEvent(sshContact,
                parent,
                SubscriptionEvent.SUBSCRIPTION_CREATED);

        //notify presence listeners for the status change.
        fireContactPresenceStatusChangeEvent(sshContact
                , parent
                , SSHStatusEnum.NOT_AVAILABLE);

        sshContact.startTimerTask();
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
            IllegalArgumentException,
            IllegalStateException,
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
    public void unsubscribe(Contact contact) throws
            IllegalArgumentException,
            IllegalStateException,
            OperationFailedException
    {
        ContactGroupSSHImpl parentGroup
                = (ContactGroupSSHImpl)((ContactSSHImpl)contact)
                .getParentContactGroup();

        parentGroup.removeContact((ContactSSHImpl)contact);

        fireSubscriptionEvent(contact,
                ((ContactSSHImpl)contact).getParentContactGroup()
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
    public Contact createUnresolvedContact(
            String address,
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
    public Contact createUnresolvedContact(
            String address,
            String persistentData,
            ContactGroup parent)
    {
        ContactSSH contact = new ContactSSHImpl(
                address,
                parentProvider);

        contact.setPersistentData(persistentData);
        contact.startTimerTask();

        // SSH Contacts are resolved by default
        contact.setResolved(true);

        ( (ContactGroupSSHImpl) parent).addContact(contact);

        fireSubscriptionEvent(contact,
                parent,
                SubscriptionEvent.SUBSCRIPTION_CREATED);

        //since we don't have any server, we'll simply resolve the contact
        //ourselves as if we've just received an event from the server telling
        //us that it has been resolved.
        fireSubscriptionEvent(
                contact, parent, SubscriptionEvent.SUBSCRIPTION_RESOLVED);

        return contact;
    }

    /**
     * Looks for a ssh protocol provider registered for a user id matching
     * <tt>sshUserID</tt>.
     *
     * @param sshUserID the ID of the SSH user whose corresponding
     * protocol provider we'd like to find.
     * @return ProtocolProviderServiceSSHImpl a ssh protocol
     * provider registered for a user with id <tt>sshUserID</tt> or null
     * if there is no such protocol provider.
     */
    public ProtocolProviderServiceSSHImpl
            findProviderForSSHUserID(String sshUserID)
    {
        BundleContext bc = SSHActivator.getBundleContext();

        String osgiQuery = "(&"
                + "(" + ProtocolProviderFactory.PROTOCOL
                + "=" + ProtocolNames.SSH + ")"
                + "(" + ProtocolProviderFactory.USER_ID
                + "=" + sshUserID + ")"
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
            return (ProtocolProviderServiceSSHImpl)bc.getService(refs[0]);
        }

        return null;
    }

    /**
     * Looks for ssh protocol providers that have added us to their
     * contact list and returns list of all contacts representing us in these
     * providers.
     *
     * @return a list of all contacts in other providers' contact lists that
     * point to us.
     */
    public List<Contact> findContactsPointingToUs()
    {
        List<Contact> contacts = new LinkedList<Contact>();
        BundleContext bc = SSHActivator.getBundleContext();

        String osgiQuery =
                "(" + ProtocolProviderFactory.PROTOCOL
                + "=SSH)";

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
            ProtocolProviderServiceSSHImpl gibProvider
                    = (ProtocolProviderServiceSSHImpl)bc.getService(refs[i]);

            OperationSetPersistentPresenceSSHImpl opSetPersPresence
                    = (OperationSetPersistentPresenceSSHImpl)gibProvider
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
    public ContactGroup createUnresolvedContactGroup(
            String groupUID,
            String persistentData,
            ContactGroup parentGroup)
    {
        ContactGroupSSHImpl newGroup
                = new ContactGroupSSHImpl(
                ContactGroupSSHImpl.createNameFromUID(groupUID)
                , parentProvider);
        newGroup.setResolved(false);

        //if parent is null then we're adding under root.
        if(parentGroup == null)
            parentGroup = getServerStoredContactListRoot();

        ((ContactGroupSSHImpl)parentGroup).addSubgroup(newGroup);

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
         * that the ssh provider has unregistered so that it would fire
         * status change events for all contacts in our buddy list.
         *
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if (! evt.getNewState().equals(RegistrationState.UNREGISTERED)
            && !evt.getNewState().equals(RegistrationState
                                                        .AUTHENTICATION_FAILED)
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
                    ContactSSHImpl contact
                            = (ContactSSHImpl) contactsIter.next();

                    PresenceStatus oldContactStatus
                            = contact.getPresenceStatus();

                    if (!oldContactStatus.isOnline())
                        continue;

                    contact.setPresenceStatus(SSHStatusEnum.OFFLINE);

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
    private ContactGroupSSHImpl getNonPersistentGroup()
    {
        for (int i = 0
                ; i < getServerStoredContactListRoot().countSubgroups()
                ; i++)
        {
            ContactGroupSSHImpl gr =
                    (ContactGroupSSHImpl)getServerStoredContactListRoot()
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
    public ContactSSHImpl createVolatileContact(String contactAddress)
    {
        //First create the new volatile contact;
        ContactSSHImpl newVolatileContact = new ContactSSHImpl(
                contactAddress,
                this.parentProvider);

        newVolatileContact.setPersistent(false);


        //Check whether a volatile group already exists and if not create
        //one
        ContactGroupSSHImpl theVolatileGroup = getNonPersistentGroup();


        //if the parent volatile group is null then we create it
        if (theVolatileGroup == null)
        {
            theVolatileGroup = new ContactGroupSSHImpl(
                    SSHActivator.getResources().getI18NString(
                        "service.gui.NOT_IN_CONTACT_LIST_GROUP_NAME")
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

    /**
     * DUMMY METHOD
     * Handler for incoming authorization requests.
     *
     * @param handler an instance of an AuthorizationHandler for
     *   authorization requests coming from other users requesting
     *   permission add us to their contact list.
     */
    public void setAuthorizationHandler(AuthorizationHandler handler)
    {
    }

}
