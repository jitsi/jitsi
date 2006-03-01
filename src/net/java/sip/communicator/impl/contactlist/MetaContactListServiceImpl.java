/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.contactlist;

import java.util.Iterator;
import java.util.Vector;

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListException;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.contactlist.event.MetaContactEvent;
import net.java.sip.communicator.service.contactlist.event.MetaContactGroupEvent;
import net.java.sip.communicator.service.contactlist.event.MetaContactListListener;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ContactGroup;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.ServerStoredGroupEvent;
import net.java.sip.communicator.service.protocol.event.ServerStoredGroupListener;
import net.java.sip.communicator.service.protocol.event.SubscriptionEvent;
import net.java.sip.communicator.service.protocol.event.SubscriptionListener;
import net.java.sip.communicator.util.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * An almost dummy implementation of the MetaContactListService that would only
 * connect to protocol service providers and build its contact list accordingly
 * only basing itself on the contact list stored by the icq service.
 * <p>
 * In its current form, the purpose of this implementation is to provide a tool
 * for retrieving any contact list so that other modules such as the user
 * interface may use it.
 * <p>
 * Because of its experimental-patch nature, the implementa would only function
 * properly if the underlying service providers have already been loaded at the
 * time this one gets started.
 *
 * @todo might be a good idea to say that the implementation does not support
 *       subgroups.
 *
 * @author Emil Ivov
 */
public class MetaContactListServiceImpl
        implements
            MetaContactListService,
            ServiceListener {
    private static final Logger logger = Logger
            .getLogger(MetaContactListServiceImpl.class);

    /**
     * Listeners interested in events dispatched upond modification of the meta
     * contact list.
     */
    private Vector contactListListeners = new Vector();

    /**
     * The BundleContext that we got from the OSGI bus.
     */
    private BundleContext bundleContext = null;

    /**
     * The list of protocol providers that we're currently aware of.
     */
    Vector currentlyInstalledProviders = new Vector();

    /**
     * The root of the meta contact list.
     */
    RootMetaContactGroupImpl rootMetaGroup = new RootMetaContactGroupImpl();

    private Vector metaContactListListeners = new Vector();

    /**
     * Creates an instance of this class.
     */
    public MetaContactListServiceImpl() {
    }

    /**
     * Starts this implementation of the MetaContactListService. The
     * implementation would first restore a default contact list from what has
     * been stored in a file. It would then connect to OSGI and retrieve any
     * existing protocol providers and if <br>
     * 1) They provide implementations of OperationSetPersistentPresence, it
     * would synchronize their contact lists with the local one (adding
     * subscriptions for contacts that do not exist in the server stored contact
     * list and ading locally contacts that were found on the server but not in
     * the local file).
     * <p>
     * 2) The only provide non persistent implementations of
     * OperationSetPresence, the meta contact list impl would create
     * subscriptions for all local contacts in the corresponding protocol
     * provider.
     * <p>
     * This implementation would also start listening for any newly registered
     * protocol provider implementations and perform the same algorithm with
     * them.
     * <p>
     *
     * @param bc
     *            the currently valid osgi bundle context.
     */
    public void start(BundleContext bc) {
        logger
                .debug("Starting the meta contact list implementation.");
        this.bundleContext = bc;

        // start listening for newly register or removed protocol providers
        bc.addServiceListener(this);

        // first discover the icq service
        // find the protocol provider service
        ServiceReference[] protocolProviderRefs = null;
        try {
            protocolProviderRefs = bc.getServiceReferences(
                    ProtocolProviderService.class.getName(),
                    null);
        } catch (InvalidSyntaxException ex) {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error(
                    "Error while retrieving service refs", ex);
            return;
        }

        // in case we found any, retrieve the root groups for all protocol
        // providers and create the meta contact list
        if (protocolProviderRefs != null) {
            logger.debug("Found "
                    + protocolProviderRefs.length
                    + " already installed providers.");
            for (int i = 0; i < protocolProviderRefs.length; i++) {
                ProtocolProviderService provider = (ProtocolProviderService) bc
                        .getService(protocolProviderRefs[i]);

                this.handleProviderAdded(provider);
            }
        }
    }

    /**
     * Adds a listener for <tt>MetaContactListChangeEvent</tt>s posted after
     * the tree changes.
     *
     * @param l
     *            the listener to add
     */
    public void addContactListListener(
            MetaContactListListener l) {
        synchronized (metaContactListListeners) {
            this.metaContactListListeners.add(l);
        }
    }

    /**
     * First makes the specified protocol provider create the contact as
     * indicated by <tt>contactID</tt>, and then associates it to the
     * _existing_ <tt>metaContact</tt> given as an argument.
     *
     * @param provider
     *            the ProtocolProviderService that should create the contact
     *            indicated by <tt>contactID</tt>.
     * @param metaContact
     *            the meta contact where that the newly created contact should
     *            be associated to.
     * @param contactID
     *            the identifier of the contact that the specified provider
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    public void addNewContactToMetaContact(
            ProtocolProviderService provider,
            MetaContact metaContact, String contactID)
            throws MetaContactListException {
        /** @todo implement addNewContactToMetaContact() */
        System.out
                .println("@todo implement addNewContactToMetaContact()");
    }

    /**
     * First makes the specified protocol provider create a contact
     * corresponding to the specified <tt>contactID</tt>, then creates a new
     * MetaContact which will encapsulate the newly crated protocol specific
     * contact.
     *
     * @param provider
     *            a ref to <tt>ProtocolProviderService</tt> instance which
     *            will create the actual protocol specific contact.
     * @param contactGroup
     *            the MetaContactGroup where the newly created meta contact
     *            should be stored.
     * @param contactID
     *            a protocol specific string identifier indicating the contact
     *            the prtocol provider should create.
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    public void createMetaContact(
            ProtocolProviderService provider,
            MetaContactGroup contactGroup, String contactID)
            throws MetaContactListException {
        /** @todo implement createMetaContact() */
        System.out
                .println("@todo implement createMetaContact()");
    }

    /**
     * Creates a <tt>MetaContactGroup</tt> with the specified group name.
     *
     * @param groupName
     *            the name of the <tt>MetaContactGroup</tt> to create.
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    public void createMetaContactGroup(String groupName)
            throws MetaContactListException {
        /** @todo implement createMetaContactGroup() */
        System.out
                .println("@todo implement createMetaContactGroup()");
    }

    /**
     * Returns the root <tt>MetaContactGroup</tt> in this contact list.
     *
     * @return the root <tt>MetaContactGroup</tt> for this contact list.
     */
    public MetaContactGroup getRoot() {
        return rootMetaGroup;
    }

    /**
     * Makes the specified <tt>contact</tt> a child of the <tt>newParent</tt>
     * MetaContact.
     *
     * @param contact
     *            the <tt>Contact</tt> to move to the
     * @param newParent
     *            the MetaContact where we'd like contact to be moved.
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    public void moveContact(Contact contact,
            MetaContact newParent)
            throws MetaContactListException {
        /** @todo implement moveContact() */
        System.out.println("@todo implement moveContact()");
    }

    /**
     * Moves the specified <tt>MetaContact</tt> to <tt>newGroup</tt>.
     *
     * @param metaContact
     *            the <tt>MetaContact</tt> to move.
     * @param newGroup
     *            the <tt>MetaContactGroup</tt> that should be the new parent
     *            of <tt>contact</tt>.
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    public void moveMetaContact(MetaContact metaContact,
            MetaContactGroup newGroup)
            throws MetaContactListException {
        /** @todo implement moveMetaContact() */
        System.out
                .println("@todo implement moveMetaContact()");
    }

    /**
     * Deletes the specified contact from both the local contact list and (if
     * applicable) the server stored contact list if supported by the
     * corresponding protocol.
     *
     * @param contact
     *            the contact to remove.
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    public void removeContact(Contact contact)
            throws MetaContactListException {
        /** @todo implement removeContact() */
        System.out.println("@todo implement removeContact()");
    }

    /**
     * Removes a listener previously added with <tt>addContactListListener</tt>.
     *
     * @param l
     *            the listener to remove
     */
    public void removeContactListListener(
            MetaContactListListener l) {
        synchronized (metaContactListListeners) {
            this.metaContactListListeners.remove(l);
        }
    }

    /**
     * Removes the specified <tt>metaContact</tt> as well as all of its
     * underlying contacts.
     *
     * @param metaContact
     *            the metaContact to remove.
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    public void removeMetaContact(MetaContact metaContact)
            throws MetaContactListException {
        /** @todo implement removeMetaContact() */
        System.out
                .println("@todo implement removeMetaContact()");
    }

    /**
     * Removes the specified meta contact group, all its corresponding protocol
     * specific groups and all their children.
     *
     * @param groupToRemove
     *            the <tt>MetaContactGroup</tt> to have removed.
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    public void removeMetaContactGroup(
            MetaContactGroup groupToRemove)
            throws MetaContactListException {
        /** @todo implement removeMetaContactGroup() */
        System.out
                .println("@todo implement removeMetaContactGroup()");
    }

    /**
     * Returns the MetaContactGroup corresponding to the specified contactGroup
     * or null if no such MetaContactGroup was found.
     * @return the MetaContactGroup corresponding to the specified contactGroup
     * or null if no such MetaContactGroup was found.
     * @param contactGroup
     *            the protocol specific <tt>contactGroup</tt> that we're looking
     *            for.
     */
    public MetaContactGroup findMetaContactGroupByContactGroup
        (ContactGroup contactGroup) {
        /** @todo implement findMetaContactByContact() */
        return null;
    }

    /**
     * Returns the MetaContact containing the specified contact or null if no
     * such MetaContact was found. The method can be used when for example we
     * need to find the MetaContact that is the author of an incoming message
     * and the corresponding ProtocolProviderService has only provided a
     * <tt>Contact</tt> as its author.
     *
     * @return the MetaContact containing the speicified contact or null if no
     *         such contact is present in this contact list.
     * @param contact
     *            the protocol specific <tt>contact</tt> that we're looking
     *            for.
     */
    public MetaContact findMetaContactByContact(Contact contact) {
        /** @todo implement findMetaContactByContact() */
        return null;
    }

    /**
     * Returns the MetaContact that corresponds to the specified metaContactID.
     *
     * @param metaContactID
     *            a String identifier of a meta contact.
     * @return the MetaContact with the speicified string identifier or null if
     *         no such meta contact was found.
     */
    public MetaContact findMetaContactByID(String metaContactID) {
        /** @todo implement findMetaContactByID() */
        return null;
    }

    /**
     * Goes through the server stored ContactList of the specified operation
     * set, retrieves all protocol specific contacts it contains and makes sure
     * they are all present in the local contact list.
     *
     * @param presenceOpSet
     *            the presence operation set whose contact list we'd like to
     *            synchronize with the local contact list.
     * @param provider
     *            the provider that the operation set belongs to.
     */
    private void synchronizeOpSetWithLocalContactList(
                    ProtocolProviderService provider,
                    OperationSetPersistentPresence presenceOpSet)
    {
        ContactGroup rootProtoGroup = presenceOpSet
                .getServerStoredContactListRoot();

        logger.trace("subgroups: "
                + rootProtoGroup.countSubGroups());
        logger.trace("child contacts: "
                + rootProtoGroup.countContacts());

        // first register the root group
        rootMetaGroup.addProtoGroup(provider, presenceOpSet
                .getServerStoredContactListRoot());

        // register subgroups and contacts
        // this implementation only supports one level of groups apart from
        // the root group - so let's keep this simple and do it in a nested loop
        Iterator subgroupsIter = rootProtoGroup.subGroups();

        while (subgroupsIter.hasNext()) {
            ContactGroup group = (ContactGroup) subgroupsIter
                    .next();

            // right now we simply map this group to an existing one
            // without being cautious and verify whether we already have it
            // registered
            MetaContactGroupImpl newMetaGroup = new MetaContactGroupImpl(
                    group.getGroupName());

            newMetaGroup.addProtoGroup(provider, group);

            Iterator contactsIter = group.contacts();
            while (contactsIter.hasNext()) {
                Contact contact = (Contact) contactsIter
                        .next();
                MetaContactImpl newMetaContact = new MetaContactImpl();

                newMetaContact.addProtoContact(contact);

                /** @todo this shouldn't be that simple */
                newMetaContact.setDisplayName(contact
                        .getDisplayName());

                newMetaGroup.addMetaContact(newMetaContact);
            }

            rootMetaGroup.addSubgroup(newMetaGroup);

            this.fireMetaContactGroupEvent(newMetaGroup,
                    provider,
                    MetaContactEvent.METACONTACT_ADDED);
        }

        // now add all contacts, located in the root group
        Iterator contactsIter = rootProtoGroup.contacts();
        while (contactsIter.hasNext()) {
            Contact contact = (Contact) contactsIter.next();
            MetaContactImpl newMetaContact = new MetaContactImpl();

            newMetaContact.addProtoContact(contact);

            /** @todo this shouldn't be that simple */
            newMetaContact.setDisplayName(contact.getDisplayName());

            rootMetaGroup.addMetaContact(newMetaContact);

            this.fireMetaContactEvent(newMetaContact,
                    provider, rootMetaGroup,
                    MetaContactEvent.METACONTACT_ADDED);
        }

        presenceOpSet
                .addSubsciptionListener(new ContactListSubscriptionListener());

        presenceOpSet
                .addServerStoredGroupChangeListener(new ContactListGroupListener());
    }

    /**
     * Adds the specified provider to the list of currently known providers. In
     * case the provider supports persistent presence the method would also
     * extract all contacts and synchronize them with the local contact list.
     * Otherwise it would start a process where local contacts would be added on
     * the server.
     *
     * @param provider
     *            the ProtocolProviderService that we've just detected.
     */
    private void handleProviderAdded(
            ProtocolProviderService provider) {
        logger.debug("Adding protocol provider "
                + provider.getProtocolName());

        // first check whether the provider has a persistent presence op set
        OperationSetPersistentPresence opSetPersPresence = (OperationSetPersistentPresence) provider
                .getSupportedOperationSets().get(
                        OperationSetPersistentPresence.class
                                .getName());

        //If we have a persistent presence op set - then retrieve its contat
        //list and merge it with the local one.
        if( opSetPersPresence != null ){
            synchronizeOpSetWithLocalContactList(provider, opSetPersPresence);
        }
        else
            logger.debug("Service did not have a pers. pres. op. set.");

        /** @todo implement handling non persistent presence operation sets */
        this.currentlyInstalledProviders.add(provider);
    }

    /**
     * Removes the specified provider from the list of currently known providers
     * and ignores all the contacts that it has registered locally.
     *
     * @param provider
     *            the ProtocolProviderService that has been unregistered.
     */
    private void handleProviderRemoved(
            ProtocolProviderService provider) {
        logger.debug("Removing protocol provider "
                + provider.getProtocolName());

        this.currentlyInstalledProviders.remove(provider);
    }

    /**
     * Implements the <tt>ServiceListener</tt> method. Verifies whether the
     * passed event concerns a <tt>ProtocolProviderService</tt> and modifies
     * the list of registered protocol providers accordingly.
     *
     * @param event
     *            The <tt>ServiceEvent</tt> object.
     */
    public void serviceChanged(ServiceEvent event) {
        Object sService = bundleContext.getService(event
                .getServiceReference());

        logger.trace("Received a service event for: "
                + sService.getClass().getName());

        // we don't care if the source service is not a protocol provider
        if (!(sService instanceof ProtocolProviderService))
            return;

        logger.debug("Service is a protocol provider.");
        if (event.getType() == ServiceEvent.REGISTERED) {
            logger
                    .debug("Handling registration of a new Protocol Provider.");
            // if we have the PROVIDER_MASK property set, make sure that this
            // provider has it and if not ignore it.
            String providerMask = System
                    .getProperty(MetaContactListService.PROVIDER_MASK_PROPERTY);
            if (providerMask != null
                    && providerMask.trim().length() > 0) {
                String servRefMask = (String) event
                        .getServiceReference()
                        .getProperty(
                                MetaContactListService.PROVIDER_MASK_PROPERTY);

                if (servRefMask == null
                        || !servRefMask.equals(providerMask)) {
                    return;
                }
            }
            this
                    .handleProviderAdded((ProtocolProviderService) sService);
        } else if (event.getType() == ServiceEvent.UNREGISTERING) {
            this
                    .handleProviderRemoved((ProtocolProviderService) sService);
        }
    }

    /**
     * The class would listen for events delivered to
     * <tt>SubscriptionListener</tt>s.
     */
    private class ContactListSubscriptionListener
            implements
                SubscriptionListener {

        public void subscriptionCreated(SubscriptionEvent evt) {

            logger.trace("Subscription created: " + evt);

            MetaContactGroupImpl parentGroup =
                (MetaContactGroupImpl)rootMetaGroup.getMetaContactSubgroup(evt
                            .getParentGroup().getGroupName());

            MetaContactImpl newMetaContact = new MetaContactImpl();

            newMetaContact.addProtoContact(evt
                    .getSourceContact());

            newMetaContact.setDisplayName(evt
                    .getSourceContact().getDisplayName());

            parentGroup.addMetaContact(newMetaContact);

            fireMetaContactEvent(newMetaContact,
                    evt.getSourceProvider(),
                    parentGroup,
                    MetaContactEvent.METACONTACT_ADDED);
        }

        public void subscriptionFailed(SubscriptionEvent evt) {

            logger.trace("Subscription failed: " + evt);
        }

        public void subscriptionRemoved(SubscriptionEvent evt) {

            logger.trace("Subscription removed: " + evt);

            MetaContact metaContact
                = findMetaContactByContact(evt.getSourceContact());

            MetaContactGroup metaContactGroup
                = findMetaContactGroupByContactGroup(evt.getParentGroup());

            //TODO: remove contact from metacontact

            fireMetaContactEvent(metaContact,
                    evt.getSourceProvider(),
                    metaContactGroup,
                    MetaContactEvent.METACONTACT_REMOVED);
        }
    }

    /**
     * The class would listen for events delivered to
     * <tt>ServerStoredGroupListener</tt>s.
     */
    private class ContactListGroupListener
            implements
                ServerStoredGroupListener {

        public void groupCreated(ServerStoredGroupEvent evt) {

            logger.trace("ContactGroup created: " + evt);

            MetaContactGroupImpl newMetaGroup = new MetaContactGroupImpl(
                    evt.getSrouceGroup().getGroupName());

            newMetaGroup.addProtoGroup(
                    evt.getSourceProvider(), evt
                            .getSrouceGroup());

            Iterator contactsIter = evt.getSrouceGroup()
                    .contacts();
            while (contactsIter.hasNext()) {
                Contact contact = (Contact) contactsIter
                        .next();

                MetaContactImpl newMetaContact = new MetaContactImpl();

                newMetaContact.addProtoContact(contact);

                newMetaContact.setDisplayName(contact
                        .getDisplayName());

                newMetaGroup.addMetaContact(newMetaContact);
            }

            rootMetaGroup.addSubgroup(newMetaGroup);

            fireMetaContactGroupEvent(newMetaGroup,
                    evt.getSourceProvider(),
                    MetaContactGroupEvent.METACONTACT_GROUP_ADDED);
        }

        public void groupRemoved(ServerStoredGroupEvent evt) {

            logger.trace("ContactGroup removed: " + evt);

            MetaContactGroup metaContactGroup
                = findMetaContactGroupByContactGroup(evt.getSrouceGroup());

            if (metaContactGroup != null) {

                removeMetaContactGroup(metaContactGroup);

                fireMetaContactGroupEvent(metaContactGroup,
                        evt.getSourceProvider(),
                        MetaContactGroupEvent.METACONTACT_GROUP_REMOVED);
            }
        }

        public void groupNameChanged(ServerStoredGroupEvent evt) {

            logger.trace("ContactGroup renamed: " + evt);

            MetaContactGroup metaContactGroup
                = findMetaContactGroupByContactGroup(evt.getSrouceGroup());

            //TODO: change the name of the MetaContactGroup
        }
    }

    /**
     * Creates the corresponding MetaContact event and notifies all
     * <tt>MetaContactListListener</tt>s that a MetaContact is added or
     * removed from the MetaContactList.
     *
     * @param source
     *            the MetaContact instance that is added to the MetaContactList
     * @param provider
     *            the ProtocolProviderService instance where this event occurred
     * @param parentGroup
     *            the MetaContactGroup underwhich the corresponding MetaContact
     *            is located
     * @param eventID
     *            one of the METACONTACT_XXX static fields indicating the nature
     *            of the event.
     */
    private void fireMetaContactEvent(MetaContact source,
            ProtocolProviderService provider,
            MetaContactGroup parentGroup, int eventID) {
        MetaContactEvent evt = new MetaContactEvent(source,
                provider, parentGroup, eventID);

        logger.trace("Will dispatch the following mcl event: "
                + evt);

        synchronized (metaContactListListeners) {
            Iterator listeners = this.metaContactListListeners
                    .iterator();

            while (listeners.hasNext()) {
                MetaContactListListener l = (MetaContactListListener) listeners
                        .next();
                if (eventID == MetaContactEvent.METACONTACT_ADDED)
                    l.metaContactAdded(evt);
                else if (eventID == MetaContactEvent.METACONTACT_REMOVED)
                    l.metaContactRemoved(evt);
            }
        }
    }

    /**
     * Creates the corresponding MetaContactGroup event and notifies all
     * <tt>MetaContactListListener</tt>s that a MetaContactGroup is added or
     * removed from the MetaContactList.
     *
     * @param source
     *            the MetaContactGroup instance that is added to the
     *            MetaContactList
     * @param provider
     *            the ProtocolProviderService instance where this event occurred
     * @param eventID
     *            one of the METACONTACT_GROUP_XXX static fields indicating the
     *            nature of the event.
     */
    private void fireMetaContactGroupEvent(
            MetaContactGroup source,
            ProtocolProviderService provider, int eventID) {
        MetaContactGroupEvent evt = new MetaContactGroupEvent(
                source, provider, eventID);

        logger.trace("Will dispatch the following mcl event: "
                + evt);

        synchronized (metaContactListListeners) {
            Iterator listeners = this.metaContactListListeners
                    .iterator();

            while (listeners.hasNext()) {
                MetaContactListListener l = (MetaContactListListener) listeners
                        .next();
                if (eventID == MetaContactGroupEvent.METACONTACT_GROUP_ADDED)
                    l.metaContactGroupAdded(evt);
                else if (eventID == MetaContactGroupEvent.METACONTACT_GROUP_REMOVED)
                    l.metaContactGroupRemoved(evt);
            }
        }
    }

}
