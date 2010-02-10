/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.rss;

import java.io.*;
import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A RSS implementation of a persistent presence operation set. In order
 * to simulate server persistence, this operation set would simply accept all
 * unresolved contacts and resolve them immediately. A real world protocol
 * implementation would save it on a server using methods provided by the
 * protocol stack.
 *
 * @author Jean-Albert Vescovo
 * @author Emil Ivov
 */
public class OperationSetPersistentPresenceRssImpl
    extends AbstractOperationSetPersistentPresence<ProtocolProviderServiceRssImpl>
{
    private static final Logger logger =
        Logger.getLogger(OperationSetPersistentPresenceRssImpl.class);

    /**
     * The root of the RSS contact list.
     */
    private ContactGroupRssImpl contactListRoot = null;

    /**
     * The currently active status message.
     */
    private String statusMessage = "Default Status Message";

    /**
     * Our default presence status.
     */
    private PresenceStatus presenceStatus = RssStatusEnum.OFFLINE;

    /**
     * The <tt>AuthorizationHandler</tt> instance that we'd have to transmit
     * authorization requests to for approval.
     */
    private AuthorizationHandler authorizationHandler = null;

    /**
     * The image retriever that we use to retrieve rss contacts
     */
    private ImageRetriever imageRetriever = null;

    /**
     * Creates an instance of this operation set keeping a reference to the
     * specified parent <tt>provider</tt>.
     * @param provider the ProtocolProviderServiceRssImpl instance that
     * created us.
     */
    public OperationSetPersistentPresenceRssImpl(
            ProtocolProviderServiceRssImpl        provider)
    {
        super(provider);

        contactListRoot = new ContactGroupRssImpl("RootGroup", provider);

        // add our un-registration listener
        parentProvider.addRegistrationStateChangeListener(
            new UnregistrationListener());

        imageRetriever = new ImageRetriever(this);

        imageRetriever.start();
    }

    /*
     * Overrides
     * AbstractOperationSetPersistentPresence#fireProviderStatusChangeEvent
     * (PresenceStatus) to stop the firing of an event.
     */
    @Override
    protected void fireProviderStatusChangeEvent(PresenceStatus oldValue)
    {
        // Override the super implementation and stop the firing of an event.
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
        ContactGroupRssImpl newGroup
            = new ContactGroupRssImpl(groupName, parentProvider);

        ((ContactGroupRssImpl)parent).addSubgroup(newGroup);

        this.fireServerStoredGroupEvent(
            newGroup, ServerStoredGroupEvent.GROUP_CREATED_EVENT);
    }

    /**
     * A RSS Provider method to use for fast filling of a contact list.
     *
     * @param contactGroup the group to add
     */
    public void addRssGroup(ContactGroupRssImpl contactGroup)
    {
        contactListRoot.addSubgroup(contactGroup);
    }

    /**
     * A RSS Provider method to use for fast filling of a contact list.
     * This method would add both the group and fire an event.
     *
     * @param parent the group where <tt>contactGroup</tt> should be added.
     * @param contactGroup the group to add
     */
    public void addRssGroupAndFireEvent(
                                  ContactGroupRssImpl parent
                                , ContactGroupRssImpl contactGroup)
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
        return RssStatusEnum.supportedStatusSet();
    }

    /***
     * Return the <tt>ContactGroup</tt> that represents the root of the contacts
     * list.
     * @return ContactGroupRssImpl representing the root of the contacts list.
     */
    public ContactGroupRssImpl getContactListRoot()
    {
        return this.contactListRoot;
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
        ContactRssImpl rssContact
            = (ContactRssImpl)contactToMove;

        ContactGroupRssImpl parentRssGroup
            = findContactParent(rssContact);

        parentRssGroup.removeContact(rssContact);

        //if this is a volatile contact then we haven't really subscribed to
        //them so we'd need to do so here
        if(!rssContact.isPersistent())
        {
            //first tell everyone that the volatile contact was removed
            fireSubscriptionEvent(rssContact
                                  , parentRssGroup
                                  , SubscriptionEvent.SUBSCRIPTION_REMOVED);

            try
            {
                //now subscribe
                this.subscribe(newParent, contactToMove.getAddress());

                //now tell everyone that we've added the contact
                fireSubscriptionEvent(rssContact
                                      , newParent
                                      , SubscriptionEvent.SUBSCRIPTION_CREATED);
            }
            catch (Exception ex)
            {
                logger.error("Failed to move contact "
                             + rssContact.getAddress()
                             , ex);
            }
        }
        else
        {
            ( (ContactGroupRssImpl) newParent)
                    .addContact(rssContact);

            fireSubscriptionMovedEvent(contactToMove
                                      , parentRssGroup
                                       , newParent);
        }
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
     * @param contact the <tt>ContactRssImpl</tt> whose status we'd like
     * to set.
     * @param newStatus the new status we'd like to set to <tt>contact</tt>.
     */
    public void changePresenceStatusForContact(
                                            ContactRssImpl contact
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
            ContactRssImpl contact
                = (ContactRssImpl)childContacts.next();

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
     * Returns the group that is parent of the specified rssGroup  or null
     * if no parent was found.
     * @param rssGroup the group whose parent we're looking for.
     * @return the ContactGroupRssImpl instance that rssGroup
     * belongs to or null if no parent was found.
     */
    public ContactGroupRssImpl findGroupParent(ContactGroupRssImpl rssGroup)
    {
        return contactListRoot.findGroupParent(rssGroup);
    }

    /**
     * Returns the group that is parent of the specified rssContact  or
     * null if no parent was found.
     * @param rssContact the contact whose parent we're looking for.
     * @return the ContactGroupRssImpl instance that rssContact
     * belongs to or null if no parent was found.
     */
    public ContactGroupRssImpl findContactParent(
                                        ContactRssImpl rssContact)
    {
        return (ContactGroupRssImpl)rssContact.getParentContactGroup();
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
        ContactGroupRssImpl rssGroup = (ContactGroupRssImpl)group;

        ContactGroupRssImpl parent = findGroupParent(rssGroup);

        if(parent == null){
            throw new IllegalArgumentException(
                "group " + group
                + " does not seem to belong to this protocol's contact list.");
        }

        parent.removeSubGroup(rssGroup);

        this.fireServerStoredGroupEvent(
            rssGroup, ServerStoredGroupEvent.GROUP_REMOVED_EVENT);
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
        ((ContactGroupRssImpl)group).setGroupName(newName);

        this.fireServerStoredGroupEvent(
            (ContactGroupRssImpl)group,
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
    public void subscribe(ContactGroup parent, String contactIdentifier)
        throws  IllegalArgumentException,
                IllegalStateException,
                OperationFailedException
    {
        URL rssURL = null;

        String contactIdentifierURL = contactIdentifier;
        // in order to allow adding of URIs like feed://a.host.com/feed.xml
        // or like  feed:https://a.host.com/feed.xml
        if (contactIdentifierURL.startsWith("feed:https"))
        {
            contactIdentifierURL = contactIdentifierURL
                .replaceFirst("feed:https", "https");
        }
        else if (contactIdentifierURL.startsWith("feed"))
        {
            contactIdentifierURL = contactIdentifierURL
                .replaceFirst("feed", "http");
        }

        if(findContactByID(contactIdentifier) != null)
        {
            logger.debug(
                "contact with same id already exists - " + contactIdentifier);
            return;
        }

        try
        {
            rssURL = new URL(contactIdentifierURL);
        }
        catch (MalformedURLException ex)
        {
            throw new IllegalArgumentException(
                "failed to create a URL for address "
                + contactIdentifier
                + ". Error was: "
                + ex.getMessage());
        }

        //we instantiate a new RssFeedReader which will contain the feed
        //associated with the contact. It is important to try and connect here
        //in order to report failure if there is a problem with the feed.
        //RssFeedReader rssFeedReader = new RssFeedReader(rssURL);

        //we parse the feed/contact here so that we could be notified of any
        //failures
        try
        {
            ContactRssImpl contact = new ContactRssImpl(
                    contactIdentifier,
                    rssURL,
                    null,
                    parentProvider);
            ((ContactGroupRssImpl)parent).addContact(contact);


            fireSubscriptionEvent(contact,
                    parent,
                    SubscriptionEvent.SUBSCRIPTION_CREATED);

            //since we are not a real protocol, we set the contact
            //presence status ourselves
            changePresenceStatusForContact(contact, getPresenceStatus());

            //now update the flow for the first time.
            parentProvider.getBasicInstantMessaging()
                .threadedContactFeedUpdate(contact);
        }
        catch(FileNotFoundException ex)
        {
            //means the feed is no longer there.
            //ignore and subscribe the contact so that the exception would
            //occur while we try to refresh it. This way we would ask the
            //user whether they want it removed.
            logger.debug("failed to create a URL for address "
                + contactIdentifier
                + ". Error was: "
                + ex.getMessage()
                , ex);
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
        ContactGroupRssImpl parentGroup
            = (ContactGroupRssImpl)((ContactRssImpl)contact)
            .getParentContactGroup();

        parentGroup.removeContact((ContactRssImpl)contact);

        fireSubscriptionEvent((ContactRssImpl)contact,
             ((ContactRssImpl)contact).getParentContactGroup(),
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
        return createUnresolvedContact( address,
                                        persistentData,
                                        getServerStoredContactListRoot());
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
     * @throws IllegalArgumentException if the status requested is not a
     *   valid PresenceStatus supported by this provider.
     */
    public Contact createUnresolvedContact(String address,
                                           String persistentData,
                                           ContactGroup parent)
        throws IllegalArgumentException
    {
        URL rssURL = null;

        String contactIdentifierURL = address;
        // in order to allow adding of URIs like feed://a.host.com/feed.xml
        // or like  feed:https://a.host.com/feed.xml
        if (contactIdentifierURL.startsWith("feed:https"))
        {
            contactIdentifierURL = contactIdentifierURL
                .replaceFirst("feed:https", "https");
        }
        else if (contactIdentifierURL.startsWith("feed"))
        {
            contactIdentifierURL = contactIdentifierURL
                .replaceFirst("feed", "http");
        }

        try
        {
            rssURL = new URL(contactIdentifierURL);
        }
        catch (MalformedURLException ex)
        {
            throw new IllegalArgumentException(
                "failed to create a URL for address "
                + address
                + ". Error was: "
                + ex.getMessage());
        }

        try
        {
            ContactRssImpl contact = new ContactRssImpl(
                    address,
                    rssURL,
                    persistentData,
                    parentProvider);
            contact.setResolved(false);

            ( (ContactGroupRssImpl) parent).addContact(contact);

            fireSubscriptionEvent(contact,
                    parent,
                    SubscriptionEvent.SUBSCRIPTION_CREATED);

            //since we don't have any server, we'll simply resolve the contact
            //ourselves as if we've just received an event from the server telling
            //us that it has been resolved.
            contact.setResolved(true);
            fireSubscriptionEvent(
                    contact, parent, SubscriptionEvent.SUBSCRIPTION_RESOLVED);

            //since we are not a real protocol, we set the contact presence status
            //ourselves
            changePresenceStatusForContact( contact, getPresenceStatus());

            //we retrieve if exists the persistent data for this contact
            //which represents the date of the last item seen by the user
            //contact.setPersistentData(persistentData);

            //hack: make sure we launch the image cache thread here because
            //the meta cl service isn't listening for contact_resolved events yet.
            contact.getImage();

            return contact;
        }
        catch(FileNotFoundException ex)
        {
            //means the feed is no longer there.
            //ignore and subscribe the contact so that the exception would
            //occur while we try to refresh it. This way we would ask the
            //user whether they want it removed.
            logger.debug("failed to create a URL for address "
                    + rssURL
                    + ". Error was: "
                    + ex.getMessage()
                    , ex);
        }
        catch(OperationFailedException ex)
        {
            logger.debug("failed to create a URL for address "
                    + rssURL
                    + ". Error was: "
                    + ex.getMessage()
                    , ex);
        }
        return null;
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
     * <tt>UID</tt> and <tt>persistentData</tt>
     */
    public ContactGroup createUnresolvedContactGroup(String groupUID,
        String persistentData, ContactGroup parentGroup)
    {
        ContactGroupRssImpl newGroup
            = new ContactGroupRssImpl(
                ContactGroupRssImpl.createNameFromUID(groupUID)
                , parentProvider);
        newGroup.setResolved(true);

        //if parent is null then we're adding under root.
        if(parentGroup == null)
            parentGroup = getServerStoredContactListRoot();

        ((ContactGroupRssImpl)parentGroup).addSubgroup(newGroup);

        this.fireServerStoredGroupEvent(
            newGroup, ServerStoredGroupEvent.GROUP_CREATED_EVENT);

        return newGroup;
    }

    private class UnregistrationListener
        implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenever
         * a change in the registration state of the corresponding provider had
         * occurred. The method is particularly interested in events stating
         * that the RSS provider has unregistered so that it would fire
         * status change events for all contacts in our buddy list.
         *
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if(evt.getNewState() == RegistrationState.REGISTERED)
            {
                if(presenceStatus != RssStatusEnum.ONLINE)
                {
                    presenceStatus = RssStatusEnum.ONLINE;
                    changePresenceStatusForAllContacts(
                                    contactListRoot, presenceStatus);
                }
            }
            else if(evt.getNewState() == RegistrationState.UNREGISTERED
                 || evt.getNewState() == RegistrationState.AUTHENTICATION_FAILED
                 || evt.getNewState() == RegistrationState.CONNECTION_FAILED)
            {
                if(presenceStatus != RssStatusEnum.OFFLINE)
                {
                    presenceStatus = RssStatusEnum.OFFLINE;
                    changePresenceStatusForAllContacts(
                                    contactListRoot, presenceStatus);
                }
            }

        }
    }

    /**
     * Returns the image retriever that we are using in this opset to retrieve
     * avatars (favicons) for our icons.
     *
     * @return the <tt>ImageRetriever</tt> that we use to retrieve favicons for
     * our rss contacts.
     */
    public ImageRetriever getImageRetriever()
    {
        return imageRetriever;
    }
}
