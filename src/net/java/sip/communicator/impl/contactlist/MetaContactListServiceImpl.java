/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.contactlist;

import java.util.*;

import org.osgi.framework.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * An implementation of the MetaContactListService that would connect to
 * protocol service providers and build its contact list accordingly
 * basing itself on the contact list stored by the various protocol provider
 * services and the contact list instance saved on the hard disk.
 * <p>
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
     * The BundleContext that we got from the OSGI bus.
     */
    private BundleContext bundleContext = null;

    /**
     * The list of protocol providers that we're currently aware of.
     */
    private Vector currentlyInstalledProviders = new Vector();

    /**
     * The root of the meta contact list.
     */
    MetaContactGroupImpl rootMetaGroup
        = new MetaContactGroupImpl("RootMetaContactGroup");

    /**
     * The number of miliseconds to wait for confirmations of account
     * modifications before deciding to drop.
     */
    public static int CONTACT_LIST_MODIFICATION_TIMEOUT = 10000;

    /**
     * Listeners interested in events dispatched upond modification of the meta
     * contact list.
     */
    private Vector metaContactListListeners = new Vector();

    /**
     * Contains (as keys) <tt>MetaContactGroup</tt> names that are currently
     * being resolved against a given protocol and that this class's
     * <tt>ContactGroupListener</tt> should ignore as corresponding events will
     * be handled by the corresponding methods. The table maps the meta contact
     * group names against lists of protocol providers. An incoming group event
     * would therefore be ignored by the class group listener if and only if it
     * carries a name present in this table and is issued by one of the
     * providers mapped against this groupName.
     */
    private Hashtable groupEventIgnoreList = new Hashtable();

    /**
     * Contains (as keys) <tt>Contact</tt> addressess that are currently
     * being resolved against a given protocol and that this class's
     * <tt>ContactListener</tt> should ignore as corresponding events will
     * be handled by the corresponding methods. The table maps the meta contact
     * addresses against lists of protocol providers. An incoming group event
     * would therefore be ignored by the class group listener if and only if it
     * carries a name present in this table and is issued by one of the
     * providers mapped against this groupName.
     */
    private Hashtable contactEventIgnoreList = new Hashtable();


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
     * @param bc the currently valid osgi bundle context.
     */
    public void start(BundleContext bc)
    {
        logger.debug("Starting the meta contact list implementation.");
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
     * @param l the listener to add
     */
    public void addContactListListener(MetaContactListListener l)
    {
        synchronized (metaContactListListeners)
        {
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
            throws MetaContactListException
    {
        addNewContactToMetaContact(provider, metaContact, contactID
            , MetaContactEvent.PROTO_CONTACT_ADDED);
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
     * @param eventID
     *            the event type that should be generated. Used when this method
     * is called upon creation of a new meta contact and not only a new contact.
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    public void addNewContactToMetaContact(
            ProtocolProviderService provider,
            MetaContact metaContact, String contactID, int eventID)
            throws MetaContactListException
    {
        OperationSetPersistentPresence opSetPersPresence
            = (OperationSetPersistentPresence)provider
                .getSupportedOperationSets().get(
                    OperationSetPersistentPresence.class.getName());
        if( opSetPersPresence == null)
        {
            /** @todo handle non-persistent presence operation sets as well */
            return;
        }

        if( ! (metaContact instanceof MetaContactImpl) )
            throw new IllegalArgumentException(metaContact
                + " is not an instance of MetaContactImpl");

        //find the parent group in the corresponding protocol.
        MetaContactGroup parentMetaGroup
            = findParentMetaContactGroup(metaContact);

        if (parentMetaGroup == null)
            throw new MetaContactListException(
                            "orphan Contact: " + metaContact
                            , null
                            , MetaContactListException.CODE_NETWORK_ERROR);

        ContactGroup parentProtoGroup
            = resolveProtoPath(provider, (MetaContactGroupImpl)parentMetaGroup);

        if( parentProtoGroup == null)
            throw new MetaContactListException(
                "Could not obtain proto group parent for " + metaContact
                , null
                , MetaContactListException.CODE_NETWORK_ERROR);

        BlockingSubscriptionEventRetriever evtRetriever
            = new BlockingSubscriptionEventRetriever(contactID);

        addContactToEventIgnoreList(contactID, provider);

        opSetPersPresence.addSubsciptionListener(evtRetriever);

        try
        {
            //create the contact in the group
            opSetPersPresence.subscribe(parentProtoGroup, contactID);

            //wait for a confirmation event
            evtRetriever.waitForEvent(CONTACT_LIST_MODIFICATION_TIMEOUT);
        }
        catch (Exception ex)
        {
            throw new MetaContactListException(
                "failed to create contact" + contactID
                , ex
                , MetaContactListException.CODE_NETWORK_ERROR);
        }
        finally
        {
            //whatever happens we need to remove the event collector
            //end the ignore filter.
            removeContactFromEventIgnoreList(contactID, provider);
            opSetPersPresence.removeSubscriptionListener(evtRetriever);
        }

        //attach the newly created contact to a meta contact
        if(evtRetriever.evt == null)
            throw new MetaContactListException(
                "Failed to create a contact with address: "
                + contactID
                , null
                , MetaContactListException.CODE_NETWORK_ERROR);

        //now finally - add the contact to the meta contact
        ((MetaContactImpl)metaContact).addProtoContact(
            evtRetriever.evt.getSourceContact());


        //no need to fire an event here since the meta contact listener will
        //sdo that for us
        this.fireMetaContactEvent(metaContact
                                  , provider
                                  , parentMetaGroup
                                  , eventID);
    }

    /**
     * Makes sure that directories in the whole path from the root to the
     * specified group have corresponding directories in the protocol indicated
     * by <tt>protoProvider</tt>. The method does not return before creating
     * all groups has completed.
     *
     * @param protoProvider a reference to the protoco provider where the groups
     * should be created.
     * @param metaGroup a ref to the last group of the path that should be
     * created in the specified <tt>protoProvider</tt>
     *
     * @return e reference to the newly created <tt>ContactGroup</tt>
     */
    private ContactGroup resolveProtoPath( ProtocolProviderService protoProvider,
                                          MetaContactGroupImpl    metaGroup)
    {
        Iterator contactGroupsForProv = metaGroup
            .getContactGroupsForProvider(protoProvider);

        if(contactGroupsForProv.hasNext())
        {
            //we already have at least one group corresponding to the meta group
            return (ContactGroup)contactGroupsForProv.next();
        }
        //we don't have a proto group here. obtain a ref to the parent
        //proto group (which may be created along the way) and create it.
        MetaContactGroupImpl parentMetaGroup = (MetaContactGroupImpl)
            findParentMetaContactGroup(metaGroup);
        if (parentMetaGroup == null)
            throw new NullPointerException("Internal Error. Orphan group.");

        ContactGroup parentProtoGroup
            = resolveProtoPath(protoProvider, parentMetaGroup);

        OperationSetPersistentPresence opSetPersPresence
            = (OperationSetPersistentPresence)protoProvider
                .getSupportedOperationSets().get(OperationSetPersistentPresence
                                                 .class.getName());
        //if persistent presence is not supported - just bail
        //we should have verified this earlier anyway
        if(opSetPersPresence == null)
            return null;

        //create the proto group
        BlockingGroupEventRetriever evtRetriever
            = new BlockingGroupEventRetriever(metaGroup.getGroupName());

        opSetPersPresence.addServerStoredGroupChangeListener(evtRetriever);

        addGroupToEventIgnoreList( metaGroup.getGroupName(), protoProvider);

        try
        {
            //create the group
            opSetPersPresence.createServerStoredContactGroup(
                parentProtoGroup, metaGroup.getGroupName());

            //wait for a confirmation event
            evtRetriever.waitForEvent(CONTACT_LIST_MODIFICATION_TIMEOUT);
        }
        catch (Exception ex)
        {
            throw new MetaContactListException(
                "failed to create contact group " + metaGroup.getGroupName()
                , ex
                , MetaContactListException.CODE_NETWORK_ERROR);
        }
        finally
        {
            //whatever happens we need to remove the event collector
            //and the ignore filter.
            removeGroupFromEventIgnoreList(metaGroup.getGroupName()
                                           , protoProvider);
            opSetPersPresence.removeServerStoredGroupChangeListener(
                evtRetriever);
        }

        removeGroupFromEventIgnoreList(
            metaGroup.getGroupName(), protoProvider);

        //sth went wrong.
        if(evtRetriever.evt == null)
            throw new MetaContactListException(
                "Failed to create a proto group named: "
                + metaGroup.getGroupName()
                , null
                , MetaContactListException.CODE_NETWORK_ERROR);

        //now add the proto group to the meta group.
        metaGroup.addProtoGroup(evtRetriever.evt.getSrouceGroup());

        return evtRetriever.evt.getSrouceGroup();
    }

    /**
     * Returns the meta contact group that is a direct parent of the specified
     * <tt>child</tt>. If no parent is found <tt>null</tt> is returned.
     * @param child the <tt>MetaContactGroup</tt> whose paret group we're
     * looking for. If no parent is found <tt>null</tt> is returned.
     *
     * @return the <tt>MetaContactGroup</tt> that contains <tt>child</tt> or
     * null if no parent was found.
     */
    public MetaContactGroup findParentMetaContactGroup(MetaContactGroup child)
    {
        return findParentMetaContactGroup(rootMetaGroup, child);
    }

    /**
     * Returns the meta contact group that is a direct parent of the specified
     * <tt>child</tt>, beginning the search at the specified root. If
     * no parent is found <tt>null</tt> is returned.
     * @param child the <tt>MetaContactGroup</tt> whose paret group we're
     * looking for.
     * @param root the parent where the search should start.
     * @return the <tt>MetaContactGroup</tt> that contains <tt>child</tt> or
     * null if no parent was found.
     */
    private MetaContactGroup findParentMetaContactGroup(
        MetaContactGroupImpl root, MetaContactGroup child)
    {
        if(root.contains(child))
            return root;

        Iterator subgroups = root.getSubgroups();

        while(subgroups.hasNext())
        {
            MetaContactGroup contactGroup
                = findParentMetaContactGroup((MetaContactGroupImpl)subgroups
                                             .next() , child);
            if(contactGroup != null)
                return contactGroup;

        }

        return null;
    }


    /**
     * Returns the meta contact group that is a direct parent of the specified
     * <tt>child</tt>.
     * @param child the <tt>MetaContact</tt> whose paret group we're looking
     * for.
     *
     * @return the <tt>MetaContactGroup</tt>
     */
    public MetaContactGroup findParentMetaContactGroup(MetaContact child)
    {
        return findParentMetaContactGroup(rootMetaGroup, child);
    }

    /**
     * Returns the meta contact group that is a direct parent of the specified
     * <tt>child</tt>, beginning the search at the specified root. If
     * no parent is found <tt>null</tt> is returned.
     * @param child the <tt>MetaContact</tt> whose paret group we're
     * looking for.
     * @param root the parent where the search should start.
     * @return the <tt>MetaContactGroup</tt> that contains <tt>child</tt> or
     * null if no parent was found.
     */
    private MetaContactGroup findParentMetaContactGroup(
        MetaContactGroupImpl root, MetaContact child)
    {
        if(root.contains(child))
            return root;

        Iterator subgroups = root.getSubgroups();

        while(subgroups.hasNext())
        {
            MetaContactGroup contactGroup
                = findParentMetaContactGroup((MetaContactGroupImpl)subgroups
                                             .next() , child);
            if(contactGroup != null)
                return contactGroup;

        }

        return null;
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
     * @param metaContactGroup
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
            MetaContactGroup metaContactGroup, String contactID)
            throws MetaContactListException
    {
        if (!(metaContactGroup instanceof MetaContactGroupImpl))
            throw new IllegalArgumentException(metaContactGroup
                + " is not an instance of MetaContactGroupImpl");

        MetaContactImpl newMetaContact = new MetaContactImpl();

        ((MetaContactGroupImpl)metaContactGroup).addMetaContact(newMetaContact);

        this.addNewContactToMetaContact(provider, newMetaContact, contactID
            , MetaContactEvent.META_CONTACT_ADDED);
    }

    /**
     * Creates a <tt>MetaContactGroup</tt> with the specified group name.
     * The meta contact group would only be created locally and resolved
     * against the different server stored protocol contact lists upon the
     * creation of the first protocol specific child contact in the respective
     * group.
     *
     * * @param parentGroup the <tt>MetaContactGroup</tt> that should be the
     * parent of the newly created group.
     * @param parent
     *            the meta contact group inside which the new child group must
     *            be created.
     * @param groupName
     *            the name of the <tt>MetaContactGroup</tt> to create.
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    public void createMetaContactGroup(MetaContactGroup parent, String groupName)
            throws MetaContactListException
    {
        if (!(parent instanceof MetaContactGroupImpl))
            throw new IllegalArgumentException(parent
                + " is not an instance of MetaContactGroupImpl");

        // we only have to create the meta contact group here.
        // we don't care about protocol specific groups.
        MetaContactGroupImpl newMetaGroup = new MetaContactGroupImpl(groupName);

        ((MetaContactGroupImpl)parent).addSubgroup(newMetaGroup);

        //fire the event
        fireMetaContactGroupEvent(newMetaGroup, null
            , MetaContactGroupEvent.META_CONTACT_GROUP_ADDED);
    }

    /**
     * Renames the specified <tt>MetaContactGroup</tt> as indicated by the
     * <tt>newName</tt> param.
     * The operation would only affect the local meta group and would not
     * "touch" any encapsulated protocol specific group.
     * <p>
     * @param group the group to rename.
     * @param newGroupName the new name of the <tt>MetaContactGroup</tt> to
     * rename.
     */
    public void renameMetaContactGroup(MetaContactGroup group,
                                       String newGroupName)
    {
        ((MetaContactGroupImpl)group).setGroupName(newGroupName);

        fireMetaContactGroupEvent(group, null
            , MetaContactGroupEvent.META_CONTACT_GROUP_RENAMED);
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
     * @param newParentMetaContact
     *            the MetaContact where we'd like contact to be moved.
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    public void moveContact(Contact contact,
            MetaContact newParentMetaContact)
            throws MetaContactListException
    {
        if( !(newParentMetaContact instanceof MetaContactImpl))
            throw new IllegalArgumentException(newParentMetaContact
                            + " is not a MetaContactImpl instance.");

        MetaContactImpl currentParentMetaContact
            = (MetaContactImpl)this.findMetaContactByContact(contact);

        currentParentMetaContact.removeProtoContact(contact);

        //fire an event telling everyone that contact has left its current
        //parent
        MetaContactGroup currentParentMetaGroup
            = this.findParentMetaContactGroup(currentParentMetaContact);


        //get a persistent  presence operation set
        OperationSetPersistentPresence opSetPresence
            = (OperationSetPersistentPresence)contact
                .getProtocolProvider().getSupportedOperationSets()
                    .get(OperationSetPersistentPresence.class.getName());

        if(opSetPresence == null)
        {
            /** @todo handle non persistent presence operation sets */
        }

        MetaContactGroup newParentGroup
            = findParentMetaContactGroup(newParentMetaContact);

        ContactGroup parentProtoGroup = resolveProtoPath(contact
            .getProtocolProvider(), (MetaContactGroupImpl)newParentGroup);

        opSetPresence.moveContactToGroup(contact, parentProtoGroup);

        ((MetaContactImpl)newParentMetaContact).addProtoContact(contact);

        //fire an event telling everyone that contact has been added to its new
        //parent.
        MetaContactGroup newParentMetaGroup
            = this.findParentMetaContactGroup(newParentMetaContact);
        fireMetaContactEvent(newParentMetaContact, contact.getProtocolProvider()
            , newParentMetaGroup, MetaContactEvent.PROTO_CONTACT_MOVED);
    }

    /**
     * Moves the specified <tt>MetaContact</tt> to <tt>newGroup</tt>.
     *
     * @param metaContact
     *            the <tt>MetaContact</tt> to move.
     * @param newMetaGroup
     *            the <tt>MetaContactGroup</tt> that should be the new parent
     *            of <tt>contact</tt>.
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     * @throws IllegalArgumentException if <tt>newMetaGroup</tt> or
     * <tt>metaCOntact</tt> do not come from this implementation.
     */
    public void moveMetaContact(MetaContact metaContact,
            MetaContactGroup newMetaGroup)
            throws MetaContactListException, IllegalArgumentException
    {
        if( !(newMetaGroup instanceof MetaContactGroupImpl) )
            throw new IllegalArgumentException(newMetaGroup
                + " is not a MetaContactGroupImpl instance");

        if( !(metaContact instanceof MetaContactImpl) )
            throw new IllegalArgumentException(metaContact
                + " is not a MetaContactImpl instance");

        //first remove the meta contact from its current parent:
        MetaContactGroupImpl currentParent
            = (MetaContactGroupImpl)findParentMetaContactGroup(metaContact);
        currentParent.removeMetaContact(metaContact);
        fireMetaContactEvent(metaContact, null, currentParent
                             , MetaContactEvent.META_CONTACT_REMOVED);

        ((MetaContactGroupImpl)newMetaGroup).addMetaContact(metaContact);

        //first make sure that the new meta contact group path is resolved
        //against all protocols that the MetaContact requires. then move
        //the meta contact in there and move all prot contacts inside it.
        Iterator contacts = metaContact.getContacts();

        while ( contacts.hasNext() )
        {
            Contact protoContact = (Contact)contacts.next();

            ContactGroup protoGroup = resolveProtoPath(protoContact
                .getProtocolProvider(), (MetaContactGroupImpl)newMetaGroup);

            //get a persistent or non persistent presence operation set
            OperationSetPersistentPresence opSetPresence
                = (OperationSetPersistentPresence)protoContact
                    .getProtocolProvider().getSupportedOperationSets()
                        .get(OperationSetPersistentPresence.class.getName());

            if(opSetPresence == null)
            {
                /** @todo handle non persistent presence operation sets */
            }

            opSetPresence.moveContactToGroup(protoContact, protoGroup);
        }

        fireMetaContactEvent(metaContact, null, newMetaGroup
                             , MetaContactEvent.META_CONTACT_ADDED);
    }

    /**
     * Deletes the specified contact from both the local contact list and (if
     * applicable) the server stored contact list if supported by the
     * corresponding protocol.
     *
     * @param contact the contact to remove.
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    public void removeContact(Contact contact)
            throws MetaContactListException
    {
        //remove the contact from the provider and do nothing else
        //updating and/or removing the corresponding meta contact would happen
        //once a confirmation event is received from the underlying protocol
        //provider
        OperationSetPresence opSetPresence =
            (OperationSetPresence)contact.getProtocolProvider()
                .getSupportedOperationSets().get(OperationSetPresence.class
                                                 .getName());

        //in case the provider only hase a persistent operation set:
        if(opSetPresence == null)
        {
            opSetPresence = (OperationSetPresence)contact.getProtocolProvider()
                .getSupportedOperationSets().get(
                    OperationSetPersistentPresence.class.getName());

            if (opSetPresence == null)
                throw new IllegalStateException(
                    "Cannot remove a contact from a provider with no presence "
                    +"operation set.");
        }

        try
        {
            opSetPresence.unsubscribe(contact);
        }
        catch (Exception ex)
        {
            throw new MetaContactListException("Failed to remove "
                + contact + " from its protocol provider.", ex
                , MetaContactListException.CODE_NETWORK_ERROR);
        }
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
            throws MetaContactListException
    {
        Iterator protoContactsIter = metaContact.getContacts();

        while(protoContactsIter.hasNext())
        {
            removeContact((Contact)protoContactsIter.next());
        }

        //do not fire events. that will be done by the contact listener as soon
        //as it gets confirmation events of proto contact removal

        //the removal of the last contact would also generate an even for the
        //removal of the meta contact itself.
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
            throws MetaContactListException
    {
        if( !(groupToRemove instanceof MetaContactGroupImpl) )
            throw new IllegalArgumentException(groupToRemove
                + " is not an instance of MetaContactGroupImpl" );

        //remove all proto groups and then remove the meta group as well.
        Iterator protoGroups
            = ((MetaContactGroupImpl)groupToRemove).getContactGroups();

        while( protoGroups.hasNext() )
        {
            ContactGroup protoGroup = (ContactGroup)protoGroups.next();

            OperationSetPersistentPresence opSetPersPresence
                = (OperationSetPersistentPresence)protoGroup
                    .getProtocolProvider().getSupportedOperationSets().get(
                        OperationSetPersistentPresence.class.getName());

            if(opSetPersPresence == null)
            {
                /** @todo handle removal of non persistent proto groups */
                return;
            }

            opSetPersPresence.removeServerStoredContactGroup(protoGroup);
        }

        MetaContactGroupImpl parentMetaGroup = (MetaContactGroupImpl)
            findParentMetaContactGroup((MetaContactGroupImpl)groupToRemove);

        parentMetaGroup.removeSubgroup(groupToRemove);

        fireMetaContactGroupEvent( groupToRemove, null,
            MetaContactGroupEvent.META_CONTACT_GROUP_REMOVED);
    }

    /**
     * Removes the protocol specific group from the specified meta contact group
     * and removes from meta contacts all proto contacts that belong to the
     * same provider as the group which is being removed.
     * @param metaContainer the MetaContactGroup that we'd like to remove a
     * contact group from.
     * @param groupToRemove the ContactGroup that we'd like removed.
     * @param sourceProvider the ProtocolProvider that the contact group belongs
     * to.
     */
    public void removeContactGroupFromMetaContactGroup(
                    MetaContactGroupImpl    metaContainer,
                    ContactGroup            groupToRemove,
                    ProtocolProviderService sourceProvider)
    {
        metaContainer.removeProtoGroup(groupToRemove);

        //go through all meta contacts and remove all contats that belong to the
        //same provider and are therefore children of the group that is being
        //removed
        locallyRemoveAllContactsForProvider(metaContainer, sourceProvider);

        fireMetaContactGroupEvent( metaContainer, sourceProvider,
                MetaContactGroupEvent.CONTACT_GROUP_REMOVED_FROM_META_GROUP);

    }

    /**
     * Goes through the specified group and removes from all meta contacts,
     * protocol specific contacts belonging to the specified provider. Note
     * that this method won't undertake any calls to the protocol itself as
     * it is used only to update the local contact list as a result of a
     * server generated event.
     *
     * @param parent  the MetaContactGroup whose children we should go through
     * @param sourceProvider the ProtocolProviderService whose contacts we'd
     * like deleted from <tt>parent</tt>.
     */
    private void locallyRemoveAllContactsForProvider(
        MetaContactGroupImpl    parent, ProtocolProviderService sourceProvider)
    {
        Iterator childrenContactsIter = parent.getChildContacts();

        //first go through all direct children.
        while(childrenContactsIter.hasNext())
        {
            MetaContactImpl child = (MetaContactImpl)childrenContactsIter.next();

            child.removeContactsForProvider(sourceProvider);

            //if this was the last proto contact inside this meta contact,
            //then remove the meta contact as well.
            int eventID = MetaContactEvent.PROTO_CONTACT_REMOVED;
            if (child.getContactCount() == 0){
                parent.removeMetaContact(child);
                eventID = MetaContactEvent.META_CONTACT_REMOVED;
            }

            fireMetaContactEvent(child, sourceProvider, parent, eventID);
        }

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
        (ContactGroup contactGroup)
    {
        return rootMetaGroup.findMetaContactGroupByContactGroup(contactGroup);
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
     * @param contact the protocol specific <tt>contact</tt> that we're looking
     *  for.
     */
    public MetaContact findMetaContactByContact(Contact contact) {
        return rootMetaGroup.findMetaContactByContact(contact);
    }

    /**
     * Returns the MetaContact that corresponds to the specified metaContactID.
     *
     * @param metaContactID
     *            a String identifier of a meta contact.
     * @return the MetaContact with the speicified string identifier or null if
     *         no such meta contact was found.
     */
    public MetaContact findMetaContactByMetaUID(String metaContactID) {

        return rootMetaGroup.findMetaContactByMetaUID(metaContactID);
    }

    /**
     * Goes through the server stored ContactList of the specified operation
     * set, retrieves all protocol specific contacts it contains and makes sure
     * they are all present in the local contact list.
     *
     * @param presenceOpSet
     *            the presence operation set whose contact list we'd like to
     *            synchronize with the local contact list.
     */
    private void synchronizeOpSetWithLocalContactList(
                    OperationSetPersistentPresence presenceOpSet)
    {
        ContactGroup rootProtoGroup = presenceOpSet
                .getServerStoredContactListRoot();
        
        if(rootProtoGroup != null){
        	
        	logger.trace("subgroups: "
                    + rootProtoGroup.countSubgroups());
            logger.trace("child contacts: "
                    + rootProtoGroup.countContacts());

            addContactGroupToMetaGroup(rootProtoGroup, rootMetaGroup, true);
        }

        presenceOpSet
                .addSubsciptionListener(new ContactListSubscriptionListener());

        presenceOpSet
                .addServerStoredGroupChangeListener(new ContactListGroupListener());
    }

    /**
     * Creates meta contacts and meta contact groups for all childredn of the
     * specified <tt>contactGroup</tt> and adds them to <tt>metaGroup</tt>
     * @param protoGroup the <tt>ContactGroup</tt> to add.
     * <p>
     * @param metaGroup the <tt>MetaContactGroup</tt> where <tt>ContactGroup</tt>
     * should be added.
     * @param fireEvents indicates whether or not events are to be fired upon
     * adding subcontacts and subgroups. When this method is called recursively,
     * the parameter should will be false in order to generate a minimal number
     * of events for the whole addition and not an event per every subgroup
     * and child contact.
     */
    private void addContactGroupToMetaGroup( ContactGroup protoGroup,
                                             MetaContactGroupImpl metaGroup,
                                             boolean fireEvents)
    {
        // first register the root group
        metaGroup.addProtoGroup(protoGroup);

        // register subgroups and contacts
        Iterator subgroupsIter = protoGroup.subGroups();

        while (subgroupsIter.hasNext()) {
            ContactGroup group = (ContactGroup) subgroupsIter
                    .next();

            // right now we simply map this group to an existing one
            // without being cautious and verify whether we already have it
            // registered
            MetaContactGroupImpl newMetaGroup = new MetaContactGroupImpl(
                    group.getGroupName());

            metaGroup.addSubgroup(newMetaGroup);

            addContactGroupToMetaGroup(group, newMetaGroup, false);

            if(fireEvents)
                this.fireMetaContactGroupEvent(newMetaGroup,
                        group.getProtocolProvider(),
                        MetaContactGroupEvent.META_CONTACT_GROUP_ADDED);
        }

        // now add all contacts, located in this group
        Iterator contactsIter = protoGroup.contacts();
        while (contactsIter.hasNext()) {
            Contact contact = (Contact) contactsIter.next();
            MetaContactImpl newMetaContact = new MetaContactImpl();

            newMetaContact.addProtoContact(contact);

            metaGroup.addMetaContact(newMetaContact);

            if( fireEvents )
                this.fireMetaContactEvent(newMetaContact,
                    protoGroup.getProtocolProvider(), metaGroup,
                    MetaContactEvent.META_CONTACT_ADDED);

        }
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
        OperationSetPersistentPresence opSetPersPresence
            = (OperationSetPersistentPresence) provider
                .getSupportedOperationSets().get(
                        OperationSetPersistentPresence.class
                                .getName());

        //If we have a persistent presence op set - then retrieve its contat
        //list and merge it with the local one.
        if( opSetPersPresence != null ){
            synchronizeOpSetWithLocalContactList(opSetPersPresence);
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
     * Registers <tt>group</tt> to the event ignore list. This would make the
     * method that is normally handling events for newly created groups ignore
     * any events for that particular group and leave the responsibility to the
     * method that added the group to the ignore list.
     *
     * @param group the name of the group that we'd like to
     * register.
     * @param ownerProvider the protocol provider that we expect the addition
     * to come from.
     */
    private void addGroupToEventIgnoreList(
        String                  group,
        ProtocolProviderService ownerProvider)
    {
        //first check whether registrations in the ignore list already
        //exist for this group.

        if (isGroupInEventIgnoreList(group, ownerProvider))
            return;

        List existingProvList = (List)this.groupEventIgnoreList.get(group);

        if (existingProvList == null)
            existingProvList = new LinkedList();

        existingProvList.add(ownerProvider);
        groupEventIgnoreList.put(group, existingProvList);
    }

    /**
     * Verifies whether the specified group is in the group event ignore list.
     * @return true if the group is in the group event ignore list and false
     * otherwise.
     * @param group the group whose presence in the ignore list we'd like to
     * verify.
     * @param ownerProvider the provider that <tt>group</tt> belongs to.
     */
    private boolean isGroupInEventIgnoreList(
        String group, ProtocolProviderService ownerProvider)
    {
        List existingProvList = (List)this.groupEventIgnoreList.get(group);

        return     existingProvList != null
                && existingProvList.contains(ownerProvider);
    }

    /**
     * Removes the <tt>group</tt> from the group event ignore list so that
     * events concerning this group get treated.
     *
     * @param group the group whose that we'd want out of the ignore list.
     * @param ownerProvider the provider that <tt>group</tt> belongs to.
     */
    private void removeGroupFromEventIgnoreList(
        String group, ProtocolProviderService ownerProvider)
    {
        //first check whether the registration actually exists.
        if (!isGroupInEventIgnoreList(group, ownerProvider))
            return;

        List existingProvList = (List)this.groupEventIgnoreList.get(group);

        if(existingProvList.size() <  1)
            groupEventIgnoreList.remove(group);
        else
            existingProvList.remove(ownerProvider);
    }

    /**
     * Registers <tt>contact</tt> to the event ignore list. This would make the
     * method that is normally handling events for newly created contacts ignore
     * any events for that particular contact and leave the responsibility to
     * the method that added the contact to the ignore list.
     *
     * @param contact the address of the contact that we'd like to ignore.
     * @param ownerProvider the protocol provider that we expect the addition
     * to come from.
     */
    private void addContactToEventIgnoreList(
        String                  contact,
        ProtocolProviderService ownerProvider)
    {
        //first check whether registrations in the ignore list already
        //exist for this contact.

        if (isContactInEventIgnoreList(contact, ownerProvider))
            return;

        List existingProvList = (List)this.contactEventIgnoreList.get(contact);

        if (existingProvList == null)
            existingProvList = new LinkedList();

        existingProvList.add(ownerProvider);
        contactEventIgnoreList.put(contact, existingProvList);
    }

    /**
     * Verifies whether the specified contact is in the contact event ignore
     * list.
     * @return true if the contact is in the contact event ignore list and false
     * otherwise.
     * @param contact the contact whose presence in the ignore list we'd like to
     * verify.
     * @param ownerProvider the provider that <tt>contact</tt> belongs to.
     */
    private boolean isContactInEventIgnoreList(
        String contact, ProtocolProviderService ownerProvider)
    {
        List existingProvList = (List)this.contactEventIgnoreList.get(contact);

        return     existingProvList != null
                && existingProvList.contains(ownerProvider);
    }

    /**
     * Removes the <tt>contact</tt> from the group event ignore list so that
     * events concerning this group get treated.
     *
     * @param contact the contact whose that we'd want out of the ignore list.
     * @param ownerProvider the provider that <tt>group</tt> belongs to.
     */
    private void removeContactFromEventIgnoreList(
        String contact, ProtocolProviderService ownerProvider)
    {
        //first check whether the registration actually exists.
        if (!isContactInEventIgnoreList(contact, ownerProvider))
            return;

        List existingProvList = (List)this.contactEventIgnoreList.get(contact);

        if(existingProvList.size() <  1)
            groupEventIgnoreList.remove(contact);
        else
            existingProvList.remove(ownerProvider);
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

        /**
         * Creates a meta contact for the source contact indicated by the
         * specified SubscriptionEvent, or updates an existing one if there
         * is one. The method would also generate the corresponding
         * <tt>MetaContactEvent</tt>.
         *
         * @param evt the SubscriptionEvent that we'll be handling.
         */
        public void subscriptionCreated(SubscriptionEvent evt)
        {
            logger.trace("Subscription created: " + evt);

            //ignore the event if the source group is in the ignore list
            if( isContactInEventIgnoreList(
                    evt.getSourceContact().getAddress()
                    , evt.getSourceProvider()) )
                return;

            MetaContactGroupImpl parentGroup = (MetaContactGroupImpl)
                    findMetaContactGroupByContactGroup( evt.getParentGroup() );

            if(parentGroup == null)
            {
                logger.error("Received a subscription for a group that we "
                             +"hadn't seen before! " + evt);
                return;
            }

            MetaContactImpl newMetaContact = new MetaContactImpl();

            newMetaContact.addProtoContact(evt.getSourceContact());

            newMetaContact.setDisplayName(evt
                    .getSourceContact().getDisplayName());

            parentGroup.addMetaContact(newMetaContact);

            fireMetaContactEvent(newMetaContact,
                    evt.getSourceProvider(),
                    parentGroup,
                    MetaContactEvent.META_CONTACT_ADDED);
        }


        public void subscriptionFailed(SubscriptionEvent evt) {
            logger.trace("Subscription failed: " + evt);
        }

        /**
         * Locates the <tt>MetaContact</tt> corresponding to the contact
         * that has been removed and updates it. If the removed proto contact
         * was the last one in it, then the <tt>MetaContact</tt> is also
         * removed.
         *
         * @param evt the <tt>SubscriptionEvent</tt> containing the contact
         * that has been removed.
         */
        public void subscriptionRemoved(SubscriptionEvent evt) {

            logger.trace("Subscription removed: " + evt);

            MetaContactImpl metaContact = (MetaContactImpl)
                findMetaContactByContact(evt.getSourceContact());

            MetaContactGroupImpl metaContactGroup = (MetaContactGroupImpl)
                    findMetaContactGroupByContactGroup(evt.getParentGroup());

            metaContact.removeProtoContact(evt.getSourceContact());

            //if this was the last protocol specific contact in this meta
            //contact then remove the meta contact as well.
            if(metaContact.getContactCount() == 0)
            {
                metaContactGroup.removeMetaContact(metaContact);

                fireMetaContactEvent(metaContact,
                                     evt.getSourceProvider(),
                                     metaContactGroup,
                                     MetaContactEvent.META_CONTACT_REMOVED);
            }
            else
            {
                //this was not the las proto contact so only generate the
                //corresponding event.
                fireMetaContactEvent(metaContact,
                                     evt.getSourceProvider(),
                                     metaContactGroup,
                                     MetaContactEvent.PROTO_CONTACT_REMOVED);

            }
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

            //ignore the event if the source group is in the ignore list
            if( isGroupInEventIgnoreList(evt.getSrouceGroup().getGroupName()
                                         , evt.getSourceProvider()) )
                return;

            MetaContactGroupImpl newMetaGroup = new MetaContactGroupImpl(
                    evt.getSrouceGroup().getGroupName());

            newMetaGroup.addProtoGroup(evt.getSrouceGroup());

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
                    MetaContactGroupEvent.META_CONTACT_GROUP_ADDED);
        }

        /**
         * Updates the local contact list by removing the meta contact group
         * corresponding to the group indicated by the delivered <tt>evt</tt>
         * @param evt the ServerStoredGroupEvent contining the group that has
         * been removed.
         */
        public void groupRemoved(ServerStoredGroupEvent evt) {

            logger.trace("ContactGroup removed: " + evt);

            MetaContactGroupImpl metaContactGroup = (MetaContactGroupImpl)
                findMetaContactGroupByContactGroup(evt.getSrouceGroup());

            if (metaContactGroup == null) {
                logger.error("Received a RemovedGroup event for an orphan grp: "
                             + evt.getSrouceGroup());
                return;
            }


            removeContactGroupFromMetaContactGroup(metaContactGroup,
                evt.getSrouceGroup(), evt.getSourceProvider());

            //do not remove the meta contact group even if this is the las
            //protocol specific contact group. Contrary to contacts, meta
            //contact groups are to only be remove upon user indication or
            //otherwise it would be difficult for a user to create a new grp.


        }

        /**
         * Nothing to do here really. Oh yes .... we should actually trigger
         * a MetaContactGroup event indicating the change for interested parties
         * but that's all.
         * @param evt the ServerStoredGroupEvent containing the source group.
         */
        public void groupNameChanged(ServerStoredGroupEvent evt) {

            logger.trace("ContactGroup renamed: " + evt);

            MetaContactGroup metaContactGroup
                = findMetaContactGroupByContactGroup(evt.getSrouceGroup());

            fireMetaContactGroupEvent(metaContactGroup, evt.getSourceProvider(),
                MetaContactGroupEvent.CONTACT_GROUP_RENAMED_IN_META_GROUP);
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
                switch (eventID)
                {
                    case MetaContactEvent.META_CONTACT_ADDED:
                        l.metaContactAdded(evt);break;
                    case MetaContactEvent.META_CONTACT_REMOVED:
                        l.metaContactRemoved(evt);break;
                    case MetaContactEvent.PROTO_CONTACT_REMOVED:
                    case MetaContactEvent.PROTO_CONTACT_ADDED:
                    case MetaContactEvent.PROTO_CONTACT_MOVED:
                        l.metaContactModified(evt);break;
                    default:
                        logger.error("Unknown event type " + eventID);
                }
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

                switch (eventID)
                {
                    case MetaContactGroupEvent.META_CONTACT_GROUP_ADDED:
                        l.metaContactGroupAdded(evt);break;
                    case MetaContactGroupEvent.META_CONTACT_GROUP_REMOVED:
                        l.metaContactGroupRemoved(evt);break;
                    case MetaContactGroupEvent
                            .CONTACT_GROUP_REMOVED_FROM_META_GROUP:
                        l.metaContactGroupModified(evt);break;
                    default:
                        logger.error("Unknown event type ("+eventID
                                     +") for event: " + evt);
                }
            }
        }
    }

    /**
     * Utility class used for blocking the current thread until an event
     * is delivered confirming the creation of a particular group.
     */
    private class BlockingGroupEventRetriever
        implements ServerStoredGroupListener
    {
        private String groupName = null;
        public ServerStoredGroupEvent evt = null;

        /**
         * Creates an instance of the retriever that will wait for events
         * confirming the creation of the group with the specified name.
         * @param groupName the name of the group whose birth we're waiting for.
         */
        BlockingGroupEventRetriever(String groupName)
        {
            this.groupName = groupName;
        }
        /**
         * Called whnever an indication is received that a new server stored
         * group is created.
         * @param evt a ServerStoredGroupChangeEvent containing a reference to
         * the newly created group.
         */
        public void groupCreated(ServerStoredGroupEvent evt)
        {
            synchronized(this)
            {
                if(evt.getSrouceGroup().getGroupName().equals(groupName))
                {
                    this.evt = evt;
                    this.notifyAll();
                }
            }
        }

        /**
         * Evens delivered through this method are ignored
         * @param evt param ignored
         */
        public void groupRemoved(ServerStoredGroupEvent evt){}

        /**
         * Evens delivered through this method are ignored
         * @param evt param ignored
         */
        public void groupNameChanged(ServerStoredGroupEvent evt){}

        /**
         * Block the execution of the current thread until either a group
         * created event is received or milis miliseconds pass.
         * @param millis the number of millis that we should wait before we
         * determine failure.
         */
        public void waitForEvent(long millis)
        {
            synchronized(this)
            {
                //no need to wait if an event is already there.
                if(evt != null)
                    return;

                try{
                    this.wait(millis);
                }
                catch (InterruptedException ex){
                    logger.error("Interrupted while waiting for group creation"
                        , ex);
                }
            }
        }
    }


    /**
     * Utility class used for blocking the current thread until an event
     * is delivered confirming the creation of a particular contact.
     */
    private class BlockingSubscriptionEventRetriever
        implements SubscriptionListener
    {
        private String subscriptionAddress = null;
        public SubscriptionEvent evt = null;

        /**
         * Creates an instance of the retriever that will wait for events
         * confirming the creation of the subscription with the specified
         * address.
         * @param subscriptionAddress the name of the group whose birth we're waiting for.
         */
        BlockingSubscriptionEventRetriever(String subscriptionAddress)
        {
            this.subscriptionAddress = subscriptionAddress;
        }

        /**
         * Called whnever an indication is received that a subscription is
         * created.
         * @param evt a <tt>SubscriptionEvent</tt> containing a reference to
         * the newly created contact.
         */
        public void subscriptionCreated(SubscriptionEvent evt)
        {
            synchronized (this)
            {
                if (evt.getSourceContact().getAddress()
                        .equals(subscriptionAddress))
                {
                    this.evt = evt;
                    this.notifyAll();
                }
            }
        }

        /**
         * Evens delivered through this method are ignored
         * @param evt param ignored
         */
        public void subscriptionRemoved(SubscriptionEvent evt)
        {}

        /**
         * Evens delivered through this method are ignored
         * @param evt param ignored
         */
        public void subscriptionFailed(SubscriptionEvent evt)
        {}

        /**
         * Block the execution of the current thread until either a contact
         * created event is received or milis miliseconds pass.
         * @param millis the number of milis to wait upon determining a failure.
         */
        public void waitForEvent(long millis)
        {
            synchronized (this)
            {
                //no need to wait if an event is already there.
                if(evt != null)
                    return;

                try
                {
                    this.wait(millis);
                }
                catch (InterruptedException ex)
                {
                    logger.error("Interrupted while waiting for contact creation"
                                 , ex);
                }
            }
        }
    }


}
