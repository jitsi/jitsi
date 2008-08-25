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
import net.java.sip.communicator.util.xml.*;

/**
 * An implementation of the MetaContactListService that would connect to
 * protocol service providers and build it  s contact list accordingly
 * basing itself on the contact list stored by the various protocol provider
 * services and the contact list instance saved on the hard disk.
 * <p>
 *
 * @author Emil Ivov
 */
public class MetaContactListServiceImpl
    implements MetaContactListService,
               ServiceListener,
               ContactPresenceStatusListener
{
    private static final Logger logger = Logger
        .getLogger(MetaContactListServiceImpl.class);

    /**
     * The BundleContext that we got from the OSGI bus.
     */
    private BundleContext bundleContext = null;

    /**
     * The list of protocol providers that we're currently aware of.
     */
    private Map currentlyInstalledProviders = new Hashtable();

    /**
     * The root of the meta contact list.
     */
    MetaContactGroupImpl rootMetaGroup
        = new MetaContactGroupImpl("RootMetaContactGroup",
                                   "RootMetaContactGroup");

    /**
     * The event handler that will be handling our subscription events.
     */
    ContactListSubscriptionListener clSubscriptionEventHandler
        = new ContactListSubscriptionListener();

    /**
     * The event handler that will be handling group events.
     */
    ContactListGroupListener clGroupEventHandler
        = new ContactListGroupListener();

    /**
     * The number of milliseconds to wait for confirmations of account
     * modifications before deciding to drop.
     */
    public static int CONTACT_LIST_MODIFICATION_TIMEOUT = 10000;

    /**
     * Listeners interested in events dispatched upon modification of the meta
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
     * Contains (as keys) <tt>Contact</tt> addresses that are currently
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
     * The instance of the storage manager which is handling the local copy of
     * our contact list.
     */
    private MclStorageManager storageManager = new MclStorageManager();

    /**
     * Creates an instance of this class.
     */
    public MetaContactListServiceImpl()
    {
    }

    /**
     * Starts this implementation of the MetaContactListService. The
     * implementation would first restore a default contact list from what has
     * been stored in a file. It would then connect to OSGI and retrieve any
     * existing protocol providers and if <br>
     * 1) They provide implementations of OperationSetPersistentPresence, it
     * would synchronize their contact lists with the local one (adding
     * subscriptions for contacts that do not exist in the server stored contact
     * list and adding locally contacts that were found on the server but not in
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
     * @param bc the currently valid OSGI bundle context.
     */
    public void start(BundleContext bc)
    {
        logger.debug("Starting the meta contact list implementation.");
        this.bundleContext = bc;

        //initializne the meta contact list from what has been stored locally.
        try
        {
            storageManager.start(bundleContext, this);
        }
        catch (Exception exc)
        {
            logger.error("Failed loading the stored contact list.", exc);
        }

        // start listening for newly register or removed protocol providers
        bc.addServiceListener(this);

        // first discover the icq service
        // then find the protocol provider service
        ServiceReference[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs = bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error(
                "Error while retrieving service refs", ex);
            return;
        }

        // in case we found any, retrieve the root groups for all protocol
        // providers and create the meta contact list
        if (protocolProviderRefs != null)
        {
            logger.debug("Found "
                         + protocolProviderRefs.length
                         + " already installed providers.");
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider = (ProtocolProviderService) bc
                    .getService(protocolProviderRefs[i]);

                this.handleProviderAdded(provider);
            }
        }
    }

    /**
     * Prepares the meta contact list service for shutdown.
     *
     * @param bc the currently active bundle context.
     */
    public void stop(BundleContext bc)
    {
        storageManager.storeContactListAndStopStorageManager();
        bc.removeServiceListener(this);

        //stop listening to all currently installed providers
        Iterator providers
            = this.currentlyInstalledProviders.values().iterator();

        while (providers.hasNext())
        {
            ProtocolProviderService pp
                = (ProtocolProviderService)providers.next();

            OperationSetPersistentPresence opSetPersPresence
                = (OperationSetPersistentPresence)pp
                    .getOperationSet(OperationSetPersistentPresence.class);

            if(opSetPersPresence !=null)
            {
                opSetPersPresence
                    .removeSubscriptionListener(clSubscriptionEventHandler);
                opSetPersPresence
                    .removeServerStoredGroupChangeListener(clGroupEventHandler);
            }
            else
            {
                //check if a non persistent presence operation set exists.
                OperationSetPresence opSetPresence = (OperationSetPresence)pp
                        .getOperationSet(OperationSetPresence.class);

                if(opSetPresence != null)
                {
                    opSetPresence
                        .removeSubscriptionListener(clSubscriptionEventHandler);
                }
            }
        }
        currentlyInstalledProviders.clear();
        if(storageManager != null)
        {
            storageManager.stop();
        }
    }

    /**
     * Adds a listener for <tt>MetaContactListChangeEvent</tt>s posted after
     * the tree changes.
     *
     * @param listener the listener to add
     */
    public void addMetaContactListListener(MetaContactListListener listener)
    {
        synchronized (metaContactListListeners)
        {
            if(!metaContactListListeners.contains(listener))
                this.metaContactListListeners.add(listener);
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
        MetaContact metaContact, String contactID) throws
        MetaContactListException
    {
        addNewContactToMetaContact(provider, metaContact, contactID, true);
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
     * @param fireEvent
     *            specifies whether or not an even is to be fire at
     * the end of the method.Used when this method is called upon creation of a
     * new meta contact and not only a new contact.
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    public void addNewContactToMetaContact( ProtocolProviderService provider,
                                            MetaContact metaContact,
                                            String contactID,
                                            boolean fireEvent)
        throws MetaContactListException
    {
        //find the parent group in the corresponding protocol.
        MetaContactGroup parentMetaGroup
            = findParentMetaContactGroup(metaContact);

        if (parentMetaGroup == null)
        {
            throw new MetaContactListException(
                "orphan Contact: " + metaContact
                , null
                , MetaContactListException.CODE_NETWORK_ERROR);
        }

        addNewContactToMetaContact(provider, parentMetaGroup, metaContact,
                contactID, fireEvent);
    }

    /**
     * First makes the specified protocol provider create the contact as
     * indicated by <tt>contactID</tt>, and then associates it to the
     * _existing_ <tt>metaContact</tt> given as an argument.
     *
     * @param provider
     *            the ProtocolProviderService that should create the contact
     *            indicated by <tt>contactID</tt>.
     * @param parentMetaGroup
     *            the meta contact group which is the parent group of the newly
     *            created contact
     * @param metaContact
     *            the meta contact where that the newly created contact should
     *            be associated to.
     * @param contactID
     *            the identifier of the contact that the specified provider
     * @param fireEvent
     *            specifies whether or not an even is to be fired at
     * the end of the method.Used when this method is called upon creation of a
     * new meta contact and not only a new contact.
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    private void addNewContactToMetaContact( ProtocolProviderService provider,
                                            MetaContactGroup parentMetaGroup,
                                            MetaContact metaContact,
                                            String contactID,
                                            boolean fireEvent)
        throws MetaContactListException
    {
        OperationSetPersistentPresence opSetPersPresence
            = (OperationSetPersistentPresence) provider
            .getSupportedOperationSets().get(
                OperationSetPersistentPresence.class.getName());
        if (opSetPersPresence == null)
        {
            /** @todo handle non-persistent presence operation sets as well */
            return;
        }

        if (! (metaContact instanceof MetaContactImpl))
        {
            throw new IllegalArgumentException(
                    metaContact
                    + " is not an instance of MetaContactImpl");
        }

        ContactGroup parentProtoGroup
            = resolveProtoPath(provider, (MetaContactGroupImpl) parentMetaGroup);

        if (parentProtoGroup == null)
        {
            throw new MetaContactListException(
                "Could not obtain proto group parent for " + metaContact
                , null
                , MetaContactListException.CODE_NETWORK_ERROR);
        }

        BlockingSubscriptionEventRetriever evtRetriever
            = new BlockingSubscriptionEventRetriever(contactID);

        addContactToEventIgnoreList(contactID, provider);

        opSetPersPresence.addSubsciptionListener(evtRetriever);
        opSetPersPresence.addServerStoredGroupChangeListener(evtRetriever);

        try
        {
            //create the contact in the group
            opSetPersPresence.subscribe(parentProtoGroup, contactID);

            //wait for a confirmation event
            evtRetriever.waitForEvent(CONTACT_LIST_MODIFICATION_TIMEOUT);
        }
        catch(OperationFailedException ex)
        {
            if(ex.getErrorCode()
               == OperationFailedException.SUBSCRIPTION_ALREADY_EXISTS)
            {
                throw new MetaContactListException(
                "failed to create contact" + contactID
                , ex
                , MetaContactListException.CODE_CONTACT_ALREADY_EXISTS_ERROR);
            }

            throw new MetaContactListException(
                "failed to create contact" + contactID
                , ex
                , MetaContactListException.CODE_NETWORK_ERROR);

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
        if (evtRetriever.evt == null)
        {
            throw new MetaContactListException(
                "Failed to create a contact with address: "
                + contactID
                , null
                , MetaContactListException.CODE_NETWORK_ERROR);
        }
        
        if (evtRetriever.evt instanceof SubscriptionEvent &&
            ((SubscriptionEvent)evtRetriever.evt).getEventID() == 
            SubscriptionEvent.SUBSCRIPTION_FAILED)
        {
            throw new MetaContactListException(
                "Failed to create a contact with address: "
                + contactID + " " 
                + ((SubscriptionEvent)evtRetriever.evt).getErrorReason()
                , null
                , MetaContactListException.CODE_UNKNOWN_ERROR);
        }

        //now finally - add the contact to the meta contact
        ( (MetaContactImpl) metaContact).addProtoContact(
            evtRetriever.sourceContact);

        //only fire an event here if the calling method wants us to. in case
        //this is the creation of a new contact and not only addition of a
        //proto contact we should remain silent and the calling method will
        //do the eventing.
        if(fireEvent)
        {
            this.fireProtoContactEvent(evtRetriever.sourceContact,
                                       ProtoContactEvent.PROTO_CONTACT_ADDED,
                                       null,
                                       metaContact);
        }
        ((MetaContactGroupImpl) parentMetaGroup).addMetaContact(
                (MetaContactImpl)metaContact);
    }

    /**
     * Makes sure that directories in the whole path from the root to the
     * specified group have corresponding directories in the protocol indicated
     * by <tt>protoProvider</tt>. The method does not return before creating
     * all groups has completed.
     *
     * @param protoProvider a reference to the protocol provider where the
     * groups should be created.
     * @param metaGroup a ref to the last group of the path that should be
     * created in the specified <tt>protoProvider</tt>
     *
     * @return e reference to the newly created <tt>ContactGroup</tt>
     */
    private ContactGroup resolveProtoPath(ProtocolProviderService protoProvider,
                                          MetaContactGroupImpl metaGroup)
    {
        Iterator contactGroupsForProv = metaGroup
            .getContactGroupsForProvider(protoProvider);

        if (contactGroupsForProv.hasNext())
        {
            //we already have at least one group corresponding to the meta group
            return (ContactGroup) contactGroupsForProv.next();
        }
        //we don't have a proto group here. obtain a ref to the parent
        //proto group (which may be created along the way) and create it.
        MetaContactGroupImpl parentMetaGroup = (MetaContactGroupImpl)
            findParentMetaContactGroup(metaGroup);
        if (parentMetaGroup == null)
        {
            throw new NullPointerException("Internal Error. Orphan group.");
        }

        ContactGroup parentProtoGroup
            = resolveProtoPath(protoProvider, parentMetaGroup);

        OperationSetPersistentPresence opSetPersPresence
            = (OperationSetPersistentPresence) protoProvider
            .getSupportedOperationSets().get(OperationSetPersistentPresence
                                             .class.getName());

        //if persistent presence is not supported - just bail
        //we should have verified this earlier anyway
        if (opSetPersPresence == null)
        {
            return null;
        }

        //create the proto group
        BlockingGroupEventRetriever evtRetriever
            = new BlockingGroupEventRetriever(metaGroup.getGroupName());

        opSetPersPresence.addServerStoredGroupChangeListener(evtRetriever);

        addGroupToEventIgnoreList(metaGroup.getGroupName(), protoProvider);

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

        //sth went wrong.
        if (evtRetriever.evt == null)
        {
            throw new MetaContactListException(
                "Failed to create a proto group named: "
                + metaGroup.getGroupName()
                , null
                , MetaContactListException.CODE_NETWORK_ERROR);
        }

        //now add the proto group to the meta group.
        metaGroup.addProtoGroup(evtRetriever.evt.getSourceGroup());

        fireMetaContactGroupEvent(
            metaGroup
            , evtRetriever.evt.getSourceProvider()
            , evtRetriever.evt.getSourceGroup()
            , MetaContactGroupEvent.CONTACT_GROUP_ADDED_TO_META_GROUP);

        return evtRetriever.evt.getSourceGroup();
    }

    /**
     * Returns the meta contact group that is a direct parent of the specified
     * <tt>child</tt>. If no parent is found <tt>null</tt> is returned.
     * @param child the <tt>MetaContactGroup</tt> whose parent group we're
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
     * @param child the <tt>MetaContactGroup</tt> whose parent group we're
     * looking for.
     * @param root the parent where the search should start.
     * @return the <tt>MetaContactGroup</tt> that contains <tt>child</tt> or
     * null if no parent was found.
     */
    private MetaContactGroup findParentMetaContactGroup(
        MetaContactGroupImpl root, MetaContactGroup child)
    {
        return child.getParentMetaContactGroup();
    }

    /**
     * Returns the meta contact group that is a direct parent of the specified
     * <tt>child</tt>.
     * @param child the <tt>MetaContact</tt> whose parent group we're looking
     * for.
     *
     * @return the <tt>MetaContactGroup</tt>
     * @throws IllegalArgumentException if <tt>child</tt> is not an instance of
     * MetaContactImpl
     */
    public MetaContactGroup findParentMetaContactGroup(MetaContact child)
    {
        if (! (child instanceof MetaContactImpl))
        {
            throw new IllegalArgumentException(child
                                       + " is not a MetaContactImpl instance.");
        }
        return ( (MetaContactImpl) child).getParentGroup();
    }

    /**
     * Returns the meta contact group that is a direct parent of the specified
     * <tt>child</tt>, beginning the search at the specified root. If
     * no parent is found <tt>null</tt> is returned.
     * @param child the <tt>MetaContact</tt> whose parent group we're
     * looking for.
     * @param root the parent where the search should start.
     * @return the <tt>MetaContactGroup</tt> that contains <tt>child</tt> or
     * null if no parent was found.
     */
    private MetaContactGroup findParentMetaContactGroup(
        MetaContactGroupImpl root, MetaContact child)
    {
        if (root.contains(child))
        {
            return root;
        }

        Iterator subgroups = root.getSubgroups();

        while (subgroups.hasNext())
        {
            MetaContactGroup contactGroup
                = findParentMetaContactGroup( (MetaContactGroupImpl) subgroups
                                             .next(), child);
            if (contactGroup != null)
            {
                return contactGroup;
            }

        }

        return null;
    }

    /**
     * First makes the specified protocol provider create a contact
     * corresponding to the specified <tt>contactID</tt>, then creates a new
     * MetaContact which will encapsulate the newly created protocol specific
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
     *            the protocol provider should create.
     * @throws MetaContactListException
     *             with an appropriate code if the operation fails for some
     *             reason.
     */
    public void createMetaContact(
        ProtocolProviderService provider,
        MetaContactGroup metaContactGroup, String contactID) throws
        MetaContactListException
    {
        if (! (metaContactGroup instanceof MetaContactGroupImpl))
        {
            throw new IllegalArgumentException(metaContactGroup
                + " is not an instance of MetaContactGroupImpl");
        }

        MetaContactImpl newMetaContact = new MetaContactImpl(this);

        this.addNewContactToMetaContact(provider, metaContactGroup, newMetaContact,
                contactID, false);  //don't fire a PROTO_CONT_ADDED event we'll
                                    //fire our own event here.

        fireMetaContactEvent(  newMetaContact
                             , findParentMetaContactGroup(newMetaContact)
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
    public void createMetaContactGroup(MetaContactGroup parent,
                                       String groupName)
        throws MetaContactListException
    {
        if (! (parent instanceof MetaContactGroupImpl))
        {
            throw new IllegalArgumentException(
                parent
                + " is not an instance of MetaContactGroupImpl");
        }

        //make sure that "parent" does not already contain a subgroup called
        //"groupName"
        Iterator subgroups = parent.getSubgroups();

        while(subgroups.hasNext())
        {
            MetaContactGroup group = (MetaContactGroup)subgroups.next();

            if(group.getGroupName().equals(groupName))
            {
                throw new MetaContactListException(
                    "Parent " + parent.getGroupName() + " already contains a "
                    + "group called " + groupName,
                    new CloneNotSupportedException("just testing nested exc-s"),
                    MetaContactListException.CODE_GROUP_ALREADY_EXISTS_ERROR);
            }
        }

        // we only have to create the meta contact group here.
        // we don't care about protocol specific groups.
        MetaContactGroupImpl newMetaGroup = new MetaContactGroupImpl(groupName);

        ( (MetaContactGroupImpl) parent).addSubgroup(newMetaGroup);

        //fire the event
        fireMetaContactGroupEvent(newMetaGroup
            , null, null, MetaContactGroupEvent. META_CONTACT_GROUP_ADDED);
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
        ( (MetaContactGroupImpl) group).setGroupName(newGroupName);

        fireMetaContactGroupEvent(group, null, null
            , MetaContactGroupEvent.META_CONTACT_GROUP_RENAMED);
    }

    /**
     * Returns the root <tt>MetaContactGroup</tt> in this contact list.
     *
     * @return the root <tt>MetaContactGroup</tt> for this contact list.
     */
    public MetaContactGroup getRoot()
    {
        return rootMetaGroup;
    }

    /**
     * Sets the display name for <tt>metaContact</tt> to be <tt>newName</tt>.
     * <p>
     * @param metaContact the <tt>MetaContact</tt> that we are renaming
     * @param newDisplayName a <tt>String</tt> containing the new display name
     * for <tt>metaContact</tt>.
     * @throws IllegalArgumentException if <tt>metaContact</tt> is not an
     * instance that belongs to the underlying implementation.
     */
    public void renameMetaContact(MetaContact metaContact, String newDisplayName)
        throws IllegalArgumentException
    {
        if (! (metaContact instanceof MetaContactImpl))
        {
            throw new IllegalArgumentException(
                metaContact + " is not a MetaContactImpl instance.");
        }

        String oldDisplayName = metaContact.getDisplayName();

        ((MetaContactImpl)metaContact).setDisplayName(newDisplayName);

        fireMetaContactPropertyChangeEvent(new MetaContactRenamedEvent(
            metaContact, oldDisplayName, newDisplayName));

        //changing the display name has surely brought a change in the order as
        //well so let's tell the others
        fireMetaContactGroupEvent(
                    findParentMetaContactGroup( metaContact )
                    , null
                    , null
                    , MetaContactGroupEvent.CHILD_CONTACTS_REORDERED);

    }

    /**
     * Makes the specified <tt>contact</tt> a child of the
     * <tt>newParentMetaGroup</tt> MetaContactGroup. If <tt>contact</tt> was
     * previously a child of a meta contact, it will be removed from its
     * old parent and to a newly created one even if they both are in the same
     * group. If the specified contact was the only child of its previous
     * parent, then the meta contact will also be moved.
     *
     *
     * @param contact the <tt>Contact</tt> to move to the
     * @param newParentMetaGroup the MetaContactGroup where we'd like contact to be moved.
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    public void moveContact(Contact contact,
                            MetaContactGroup newParentMetaGroup)
        throws MetaContactListException
    {
        /** first create the new meta contact */
        MetaContactImpl metaContactImpl = new MetaContactImpl(this);

        MetaContactGroupImpl newParentMetaGroupImpl
            = (MetaContactGroupImpl)newParentMetaGroup;

        newParentMetaGroupImpl.addMetaContact(metaContactImpl);

        fireMetaContactEvent(metaContactImpl
                             , newParentMetaGroupImpl
                             , MetaContactEvent.META_CONTACT_ADDED);

        /** then move the sub contactact to the new metacontact container */
        moveContact(contact, metaContactImpl);
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
                            MetaContact newParentMetaContact) throws
        MetaContactListException
    {
        if (! (newParentMetaContact instanceof MetaContactImpl))
        {
            throw new IllegalArgumentException(
                newParentMetaContact + " is not a MetaContactImpl instance.");
        }

        MetaContactImpl currentParentMetaContact
            = (MetaContactImpl)this.findMetaContactByContact(contact);

        currentParentMetaContact.removeProtoContact(contact);

        //fire an event telling everyone that contact has left its current
        //parent
        MetaContactGroup currentParentMetaGroup
            = this.findParentMetaContactGroup(currentParentMetaContact);

        //get a persistent  presence operation set
        OperationSetPersistentPresence opSetPresence
            = (OperationSetPersistentPresence) contact
            .getProtocolProvider().getSupportedOperationSets()
            .get(OperationSetPersistentPresence.class.getName());

        if (opSetPresence == null)
        {
            /** @todo handle non persistent presence operation sets */
        }

        MetaContactGroup newParentGroup
            = findParentMetaContactGroup(newParentMetaContact);

        ContactGroup parentProtoGroup = resolveProtoPath(contact
            .getProtocolProvider(), (MetaContactGroupImpl) newParentGroup);

        //if the contact is not currently in the proto group corresponding to
        //its new metacontact group parent then move it
        if(contact.getParentContactGroup() != parentProtoGroup)
            opSetPresence.moveContactToGroup(contact, parentProtoGroup);

        ( (MetaContactImpl) newParentMetaContact).addProtoContact(contact);

        //fire an event telling everyone that contact has been added to its new
        //parent.
        fireProtoContactEvent(contact, ProtoContactEvent.PROTO_CONTACT_MOVED
            , currentParentMetaContact , newParentMetaContact);

        //if this was the last contact in the meta contact - remove it.
        //it is true that in some cases the move would be followed by some kind
        //of protocol provider events indicating the change which on its turn
        //may trigger the removal of empty meta contacts. Yet in many cases
        //particularly if parent groups were not changed in the protocol contact
        //list no event would come and the meta contact will remain empty
        //that's why we delete it here and if an event follows it would simply
        //be ignored.
        if (currentParentMetaContact.getContactCount() == 0)
        {
            MetaContactGroupImpl parentMetaGroup =
                currentParentMetaContact.getParentGroup();
            parentMetaGroup.removeMetaContact(currentParentMetaContact);

            fireMetaContactEvent(currentParentMetaContact, parentMetaGroup
                                     , MetaContactEvent.META_CONTACT_REMOVED);
        }
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
                                MetaContactGroup newMetaGroup) throws
        MetaContactListException, IllegalArgumentException
    {
        if (! (newMetaGroup instanceof MetaContactGroupImpl))
        {
            throw new IllegalArgumentException(newMetaGroup
                                               +
                                               " is not a MetaContactGroupImpl instance");
        }

        if (! (metaContact instanceof MetaContactImpl))
        {
            throw new IllegalArgumentException(metaContact
                                               +
                                               " is not a MetaContactImpl instance");
        }

        //first remove the meta contact from its current parent:
        MetaContactGroupImpl currentParent
            = (MetaContactGroupImpl) findParentMetaContactGroup(metaContact);
        currentParent.removeMetaContact( (MetaContactImpl) metaContact);

        ( (MetaContactGroupImpl) newMetaGroup).addMetaContact(
            (MetaContactImpl) metaContact);

        try
        {
            //first make sure that the new meta contact group path is resolved
            //against all protocols that the MetaContact requires. then move
            //the meta contact in there and move all prot contacts inside it.
            Iterator contacts = metaContact.getContacts();

            while (contacts.hasNext())
            {
                Contact protoContact = (Contact) contacts.next();

                ContactGroup protoGroup = resolveProtoPath(protoContact
                    .getProtocolProvider(), (MetaContactGroupImpl) newMetaGroup);

                //get a persistent or non persistent presence operation set
                OperationSetPersistentPresence opSetPresence
                    = (OperationSetPersistentPresence) protoContact
                    .getProtocolProvider().getSupportedOperationSets()
                    .get(OperationSetPersistentPresence.class.getName());

                if (opSetPresence == null)
                {
                    /** @todo handle non persistent presence operation sets */
                }

                opSetPresence.moveContactToGroup(protoContact, protoGroup);
            }
        }
        catch (Exception ex)
        {
            logger.error("Cannot move contact", ex);
            
            // now move the contact to prevoius parent
            ((MetaContactGroupImpl)newMetaGroup).
                removeMetaContact( (MetaContactImpl) metaContact);

            currentParent.addMetaContact((MetaContactImpl) metaContact);
            
            throw new MetaContactListException(ex.getMessage(),
                MetaContactListException.CODE_MOVE_CONTACT_ERROR);
        }

        //fire the mved event.
        fireMetaContactEvent(new MetaContactMovedEvent(
                                    metaContact, currentParent, newMetaGroup));
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
    public void removeContact(Contact contact) throws MetaContactListException
    {
        //remove the contact from the provider and do nothing else
        //updating and/or removing the corresponding meta contact would happen
        //once a confirmation event is received from the underlying protocol
        //provider
        OperationSetPresence opSetPresence =
            (OperationSetPresence) contact.getProtocolProvider()
            .getSupportedOperationSets().get(OperationSetPresence.class
                                             .getName());

        //in case the provider only hase a persistent operation set:
        if (opSetPresence == null)
        {
            opSetPresence = (OperationSetPresence) contact.getProtocolProvider()
                .getSupportedOperationSets().get(
                    OperationSetPersistentPresence.class.getName());

            if (opSetPresence == null)
            {
                throw new IllegalStateException(
                    "Cannot remove a contact from a provider with no presence "
                    + "operation set.");
            }
        }

        try
        {
            opSetPresence.unsubscribe(contact);
        }
        catch (Exception ex)
        {
            throw new MetaContactListException("Failed to remove "
                                               + contact +
                                               " from its protocol provider.",
                                               ex
                                               ,
                                               MetaContactListException.
                                               CODE_NETWORK_ERROR);
        }
    }

    /**
     * Removes a listener previously added with <tt>addContactListListener</tt>.
     *
     * @param listener the listener to remove
     */
    public void removeMetaContactListListener(
        MetaContactListListener listener)
    {
        synchronized (metaContactListListeners)
        {
            this.metaContactListListeners.remove(listener);
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
    public void removeMetaContact(MetaContact metaContact) throws
        MetaContactListException
    {
        Iterator protoContactsIter = metaContact.getContacts();

        while (protoContactsIter.hasNext())
        {
            removeContact( (Contact) protoContactsIter.next());
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
        MetaContactGroup groupToRemove) throws MetaContactListException
    {
        if (! (groupToRemove instanceof MetaContactGroupImpl))
        {
            throw new IllegalArgumentException(groupToRemove
                                               +
                                               " is not an instance of MetaContactGroupImpl");
        }

        try
        {
            //remove all proto groups and then remove the meta group as well.
            Iterator protoGroups
                = ( (MetaContactGroupImpl) groupToRemove).getContactGroups();

            while (protoGroups.hasNext())
            {
                ContactGroup protoGroup = (ContactGroup) protoGroups.next();

                OperationSetPersistentPresence opSetPersPresence
                    = (OperationSetPersistentPresence) protoGroup
                    .getProtocolProvider().getSupportedOperationSets().get(
                        OperationSetPersistentPresence.class.getName());

                if (opSetPersPresence == null)
                {
                    /** @todo handle removal of non persistent proto groups */
                    return;
                }

                opSetPersPresence.removeServerStoredContactGroup(protoGroup);
            }
        }catch(Exception ex)
        {
            throw new MetaContactListException(ex.getMessage(), 
                MetaContactListException.CODE_REMOVE_GROUP_ERROR);
        }
        

        MetaContactGroupImpl parentMetaGroup = (MetaContactGroupImpl)
            findParentMetaContactGroup( (MetaContactGroupImpl) groupToRemove);

        parentMetaGroup.removeSubgroup(groupToRemove);

        fireMetaContactGroupEvent( groupToRemove, null, null
            , MetaContactGroupEvent.META_CONTACT_GROUP_REMOVED);
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
        MetaContactGroupImpl metaContainer,
        ContactGroup groupToRemove,
        ProtocolProviderService sourceProvider)
    {


        //go through all meta contacts and remove all contats that belong to the
        //same provider and are therefore children of the group that is being
        //removed
        locallyRemoveAllContactsForProvider(metaContainer
                                            , groupToRemove);
        
        fireMetaContactGroupEvent(metaContainer, sourceProvider, groupToRemove
            , MetaContactGroupEvent.CONTACT_GROUP_REMOVED_FROM_META_GROUP);

    }

    /**
     * Removes local resources storing copies of the meta contact list. This
     * method is meant primarily to aid automated testing which may depend on
     * beginning the tests with an empty local contact list.
     */
    public void purgeLocallyStoredContactListCopy()
    {
        this.storageManager.storeContactListAndStopStorageManager();
        this.storageManager.removeContactListFile();
        logger.trace("Removed meta contact list storage file.");
    }

    /**
     * Goes through the specified group and removes from all meta contacts,
     * protocol specific contacts belonging to the specified
     * <tt>groupToRemove</tt>. Note that this method won't undertake any calls
     * to the protocol itself as it is used only to update the local contact
     * list as a result of a server generated event.
     *
     * @param parentMetaGroup  the MetaContactGroup whose children we should go
     * through
     * @param groupToRemove the proto group that we want removed together with
     * its children.
     */
    private void locallyRemoveAllContactsForProvider(
                        MetaContactGroupImpl parentMetaGroup,
                        ContactGroup         groupToRemove)
    {
        Iterator childrenContactsIter = parentMetaGroup.getChildContacts();

        //first go through all direct children.
        while (childrenContactsIter.hasNext())
        {
            MetaContactImpl child
                = (MetaContactImpl) childrenContactsIter.next();

            //Get references to all contacts that will be removed in case we
            //need to fire an event.
            Iterator contactsToRemove
                = child.getContactsForContactGroup(groupToRemove);

            child.removeContactsForGroup(groupToRemove);

            //if this was the last proto contact inside this meta contact,
            //then remove the meta contact as well. Otherwise only fire an
            //event.
            if (child.getContactCount() == 0)
            {
                parentMetaGroup.removeMetaContact(child);
                fireMetaContactEvent(child, parentMetaGroup
                                     , MetaContactEvent.META_CONTACT_REMOVED);
            }
            else
            {
                // there are other proto contacts left in the contact child
                //meta contact so we'll have to send an event for each of the
                //removed contacts and not only a single event for the whole
                //meta contact.
                while (contactsToRemove.hasNext())
                {
                    fireProtoContactEvent(
                          (Contact)contactsToRemove.next()
                        , ProtoContactEvent.PROTO_CONTACT_REMOVED
                        , child
                        , null);
                }
            }
        }

        Iterator subgroupsIter = parentMetaGroup.getSubgroups();

        //then go through all subgroups.
        while (subgroupsIter.hasNext())
        {
            MetaContactGroupImpl subMetaGroup
                = (MetaContactGroupImpl)subgroupsIter.next();

            Iterator contactGroups = subMetaGroup.getContactGroups();

            ContactGroup protoGroup = null;
            while(contactGroups.hasNext())
            {
                protoGroup = (ContactGroup)contactGroups.next();
                if(groupToRemove == protoGroup.getParentContactGroup())
                    this.locallyRemoveAllContactsForProvider(
                            subMetaGroup, protoGroup);
            }

            //remove the group if there are no children left.
            if(subMetaGroup.countSubgroups() == 0
               && subMetaGroup.countChildContacts() == 0)
            {
                parentMetaGroup.removeSubgroup(subMetaGroup);
                fireMetaContactGroupEvent(
                    subMetaGroup
                    , groupToRemove.getProtocolProvider()
                    , protoGroup
                    , MetaContactGroupEvent.META_CONTACT_GROUP_REMOVED);

            }
        }

        parentMetaGroup.removeProtoGroup(groupToRemove);
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
     * @param contact the protocol specific <tt>contact</tt> that we're looking
     *  for.
     *
     * @return the MetaContact containing the specified contact or null if no
     *         such contact is present in this contact list.
     */
    public MetaContact findMetaContactByContact(Contact contact)
    {
        return rootMetaGroup.findMetaContactByContact(contact);
    }

    /**
     * Returns the MetaContact containing a contact with an address equal to
     * <tt>contactAddress</tt> and with a source provider matching
     * <tt>accountID</tt>, or null if no such MetaContact was found. The method
     * can be used when for example we
     * need to find the MetaContact that is the author of an incoming message
     * and the corresponding ProtocolProviderService has only provided a
     * <tt>Contact</tt> as its author.
     *
     * @param contactAddress the address of the  protocol specific
     * <tt>contact</tt> that we're looking for.
     * @param accountID the ID of the account that the contact we're looking for
     * must belong to.
     *
     * @return the MetaContact containing the specified contact or null if no
     *         such contact is present in this contact list.
     */
    public MetaContact findMetaContactByContact(String contactAddress,
                                                String accountID)
    {
        return rootMetaGroup.findMetaContactByContact(contactAddress
                                                      , accountID);
    }

    /**
     * Returns the MetaContact that corresponds to the specified metaContactID.
     *
     * @param metaContactID
     *            a String identifier of a meta contact.
     * @return the MetaContact with the specified string identifier or null if
     *         no such meta contact was found.
     */
    public MetaContact findMetaContactByMetaUID(String metaContactID)
    {

        return rootMetaGroup.findMetaContactByMetaUID(metaContactID);
    }

    /**
     * Returns the MetaContactGroup that corresponds to the specified
     * metaGroupID.
     *
     * @param metaGroupID
     *            a String identifier of a meta contact group.
     * @return the MetaContactGroup with the specified string identifier or null
     *          if no such meta contact was found.
     */
    public MetaContactGroup findMetaContactGroupByMetaUID(String metaGroupID)
    {
        return rootMetaGroup.findMetaContactGroupByMetaUID(metaGroupID);
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

        if (rootProtoGroup != null)
        {

            logger.trace("subgroups: "
                         + rootProtoGroup.countSubgroups());
            logger.trace("child contacts: "
                         + rootProtoGroup.countContacts());

            addContactGroupToMetaGroup(rootProtoGroup, rootMetaGroup, true);
        }

        presenceOpSet
            .addSubsciptionListener(clSubscriptionEventHandler);

        presenceOpSet
            .addServerStoredGroupChangeListener(clGroupEventHandler);
    }

    /**
     * Creates meta contacts and meta contact groups for all children of the
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
    private void addContactGroupToMetaGroup(ContactGroup protoGroup,
                                            MetaContactGroupImpl metaGroup,
                                            boolean fireEvents)
    {
        // first register the root group
        metaGroup.addProtoGroup(protoGroup);

        // register subgroups and contacts
        Iterator subgroupsIter = protoGroup.subgroups();

        while (subgroupsIter.hasNext())
        {
            ContactGroup group = (ContactGroup) subgroupsIter
                .next();

            //continue if we have already loaded this group from the locally
            //stored contact list.
            if(metaGroup.findMetaContactGroupByContactGroup(group) != null)
                continue;

            // right now we simply map this group to an existing one
            // without being cautious and verify whether we already have it
            // registered
            MetaContactGroupImpl newMetaGroup = new MetaContactGroupImpl(
                group.getGroupName());

            metaGroup.addSubgroup(newMetaGroup);

            addContactGroupToMetaGroup(group, newMetaGroup, false);

            if (fireEvents)
            {
                this.fireMetaContactGroupEvent(
                        newMetaGroup
                        , group.getProtocolProvider()
                        , group
                        , MetaContactGroupEvent.
                        META_CONTACT_GROUP_ADDED);
            }
        }

        // now add all contacts, located in this group
        Iterator contactsIter = protoGroup.contacts();
        while (contactsIter.hasNext())
        {
            Contact contact = (Contact) contactsIter.next();

            //continue if we have already loaded this contact from the locally
            //stored contact list.
            if(metaGroup.findMetaContactByContact(contact) != null)
                continue;


            MetaContactImpl newMetaContact = new MetaContactImpl(this);

            newMetaContact.addProtoContact(contact);

            metaGroup.addMetaContact(newMetaContact);

            if (fireEvents)
            {
                this.fireMetaContactEvent(newMetaContact,
                                          metaGroup,
                                          MetaContactEvent.META_CONTACT_ADDED);
            }

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
        ProtocolProviderService provider)
    {
        logger.debug("Adding protocol provider "
                     + provider.getProtocolName());

        // check whether the provider has a persistent presence op set
        OperationSetPersistentPresence opSetPersPresence
            = (OperationSetPersistentPresence) provider
            .getSupportedOperationSets().get(
                OperationSetPersistentPresence.class
                .getName());

        this.currentlyInstalledProviders.put(
                           provider.getAccountID().getAccountUniqueID(), provider);

        //If we have a persistent presence op set - then retrieve its contat
        //list and merge it with the local one.
        if (opSetPersPresence != null)
        {
            //load contacts, stored in the local contact list and corresponding to
            //this provider.
            try
            {
                storageManager.extractContactsForAccount(
                    provider.getAccountID().getAccountUniqueID());
            }
            catch (XMLException exc)
            {
                logger.error("Failed to load contacts for account "
                             + provider.getAccountID().getAccountUniqueID(), exc);
            }

            synchronizeOpSetWithLocalContactList(opSetPersPresence);
        }
        else
        {
            logger.debug("Service did not have a pers. pres. op. set.");
        }

        /** @todo implement handling non persistent presence operation sets */

        //add a presence status listener so that we could reorder contacts upon
        //status change. NOTE that we MUST NOT add the presence listener before
        //extracting the locally stored contact list or  otherwise we'll get
        //events for all contacts that we have already extracted
        if(opSetPersPresence != null)
            opSetPersPresence.addContactPresenceStatusListener(this);
    }

    /**
     * Removes the specified provider from the list of currently known providers
     * and ignores all the contacts that it has registered locally.
     *
     * @param provider
     *            the ProtocolProviderService that has been unregistered.
     */
    private void handleProviderRemoved(
        ProtocolProviderService provider)
    {
        logger.debug("Removing protocol provider "
                     + provider.getProtocolName());

        this.currentlyInstalledProviders.
            remove(provider.getAccountID().getAccountUniqueID());

        //get the root group for the provider so that we could remove it.
        OperationSetPersistentPresence persPresOpSet
            = (OperationSetPersistentPresence)provider
                .getOperationSet(OperationSetPersistentPresence.class);

        //ignore if persistent presence is not supported.
        if(persPresOpSet == null)
            return;

        ContactGroup rootGroup
            = persPresOpSet.getServerStoredContactListRoot();

        //iterate all sub groups and remove them one by one
        //(we dont simply remove the root group because the mcl storage manager
        //is stupid (i wrote it) and doesn't know root groups exist. that's why
        //it needs to hear an event for every single group.)
        Iterator subgroups = rootGroup.subgroups();

        while(subgroups.hasNext())
        {
            ContactGroup group = (ContactGroup)subgroups.next();
            //remove the group
            this.removeContactGroupFromMetaContactGroup(
                (MetaContactGroupImpl)findMetaContactGroupByContactGroup(group), 
                group, 
                provider);
        }

        //remove the root group
        this.removeContactGroupFromMetaContactGroup(
            this.rootMetaGroup, rootGroup, provider);

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
        String group,
        ProtocolProviderService ownerProvider)
    {
        //first check whether registrations in the ignore list already
        //exist for this group.

        if (isGroupInEventIgnoreList(group, ownerProvider))
        {
            return;
        }

        List existingProvList = (List)this.groupEventIgnoreList.get(group);

        if (existingProvList == null)
        {
            existingProvList = new LinkedList();
        }

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

        return existingProvList != null
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
        {
            return;
        }

        List existingProvList = (List)this.groupEventIgnoreList.get(group);

        if (existingProvList.size() < 1)
        {
            groupEventIgnoreList.remove(group);
        }
        else
        {
            existingProvList.remove(ownerProvider);
        }
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
        String contact,
        ProtocolProviderService ownerProvider)
    {
        //first check whether registrations in the ignore list already
        //exist for this contact.

        if (isContactInEventIgnoreList(contact, ownerProvider))
        {
            return;
        }

        List existingProvList = (List)this.contactEventIgnoreList.get(contact);

        if (existingProvList == null)
        {
            existingProvList = new LinkedList();
        }

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

        return existingProvList != null
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
        {
            return;
        }

        List existingProvList = (List)this.contactEventIgnoreList.get(contact);

        if (existingProvList.size() < 1)
        {
            groupEventIgnoreList.remove(contact);
        }
        else
        {
            existingProvList.remove(ownerProvider);
        }
    }

    /**
     * Implements the <tt>ServiceListener</tt> method. Verifies whether the
     * passed event concerns a <tt>ProtocolProviderService</tt> and modifies
     * the list of registered protocol providers accordingly.
     *
     * @param event
     *            The <tt>ServiceEvent</tt> object.
     */
    public void serviceChanged(ServiceEvent event)
    {
        Object sService = bundleContext.getService(event
            .getServiceReference());

        logger.trace("Received a service event for: "
                     + sService.getClass().getName());

        // we don't care if the source service is not a protocol provider
        if (! (sService instanceof ProtocolProviderService))
        {
            return;
        }

        logger.debug("Service is a protocol provider.");

        ProtocolProviderService provider =
            (ProtocolProviderService)sService;
        //first check if the event really means that the accounts is
        //uninstalled/installed (or is it just stopped ... e.g. we could be
        // shutting down, or in the other case it could be just modified) ...
        // before that however, we'd need to get a reference to the service.
        ProtocolProviderFactory sourceFactory = null;

        ServiceReference[] allBundleServices
            = event.getServiceReference().getBundle()
                .getRegisteredServices();

        for (int i = 0; i < allBundleServices.length; i++)
        {
            Object service = bundleContext.getService(allBundleServices[i]);
            if(service instanceof ProtocolProviderFactory)
            {
                sourceFactory = (ProtocolProviderFactory) service;
                break;
            }
        }

        if (event.getType() == ServiceEvent.REGISTERED)
        {
            logger
                .debug("Handling registration of a new Protocol Provider.");
            // if we have the PROVIDER_MASK property set, make sure that this
            // provider has it and if not ignore it.
            String providerMask = System
                .getProperty(MetaContactListService.PROVIDER_MASK_PROPERTY);
            if (providerMask != null
                && providerMask.trim().length() > 0)
            {
                String servRefMask = (String) event
                    .getServiceReference()
                    .getProperty(
                        MetaContactListService.PROVIDER_MASK_PROPERTY);

                if (servRefMask == null
                    || !servRefMask.equals(providerMask))
                {
                    return;
                }
            }

            if(sourceFactory != null && sourceFactory.getRegisteredAccounts().contains(
                provider.getAccountID()))
            {
                // the account is already installed and this event is coming
                // from a modification. we don't need to do anything.
                return;
            }

            this
                .handleProviderAdded( (ProtocolProviderService) sService);
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            if(sourceFactory == null)
            {
                //strange ... we must be shutting down. just bail
                return;
            }

            if(sourceFactory.getRegisteredAccounts().contains(
                provider.getAccountID()))
            {
                //the account is still installed. we don't need to do anything.
                return;
            }

            logger.debug("Account uninstalled. acc.id="
                         +provider.getAccountID() +". Removing from meta "
                         +"contact list.");
            this
                .handleProviderRemoved( (ProtocolProviderService) sService);
        }
    }

    /**
     * The class would listen for events delivered to
     * <tt>SubscriptionListener</tt>s.
     */
    private class ContactListSubscriptionListener
        implements SubscriptionListener
    {

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

            //ignore the event if the source contact is in the ignore list
            if (isContactInEventIgnoreList(
                      evt.getSourceContact().getAddress()
                    , evt.getSourceProvider()))
            {
                return;
            }

            MetaContactGroupImpl parentGroup = (MetaContactGroupImpl)
                findMetaContactGroupByContactGroup(evt.getParentGroup());

            if (parentGroup == null)
            {
                logger.error("Received a subscription for a group that we "
                             + "hadn't seen before! ");
                return;
            }

            MetaContactImpl newMetaContact = new MetaContactImpl(
                    MetaContactListServiceImpl.this);

            newMetaContact.addProtoContact(evt.getSourceContact());

            newMetaContact.setDisplayName(evt
                                          .getSourceContact().getDisplayName());

            parentGroup.addMetaContact(newMetaContact);

            //fire the meta contact event.
            fireMetaContactEvent(newMetaContact,
                                 parentGroup,
                                 MetaContactEvent.META_CONTACT_ADDED);
        }

        /**
         * Indicates that a contact/subscription has been moved from one server
         * stored group to another. The way we handle the event depends on
         * whether the source contact/subscription is the only proto contact
         * found in its current MetaContact encapsulator or not.
         * <p>
         * If this is the case (the source contact has no siblings in its current
         * meta contact list encapsulator) then we will move the whole meta
         * contact to the meta contact group corresponding to the new parent
         * ContactGroup of the source contact. In this case we would only fire
         * a MetaContactMovedEvent containing the old and new parents of the
         * MetaContact in question.
         * <p>
         * If, however, the MetaContact that currently encapsulates the source
         * contact also encapsulates other proto contacts, then we will create
         * a new MetaContact instance, place it in the MetaContactGroup
         * corresponding to the new parent ContactGroup of the source contact
         * and add the source contact inside it. In this case we would first
         * fire a metacontact added event over the empty meta contact and then,
         * once the proto contact has been moved inside it, we would also fire
         * a ProtoContactEvent with event id PROTO_CONTACT_MOVED.
         * <p>
         * @param evt a reference to the SubscriptionMovedEvent containing previous
         * and new parents as well as a ref to the source contact.
         */
        public void subscriptionMoved(SubscriptionMovedEvent evt)
        {
            logger.trace("Subscription moved: " + evt);

            //ignore the event if the source contact is in the ignore list
            if (isContactInEventIgnoreList(
                     evt.getSourceContact().getAddress()
                   , evt.getSourceProvider()))
            {
                return;
            }

            MetaContactGroupImpl oldParentGroup = (MetaContactGroupImpl)
                findMetaContactGroupByContactGroup(evt.getOldParentGroup());
            MetaContactGroupImpl newParentGroup = (MetaContactGroupImpl)
                findMetaContactGroupByContactGroup(evt.getNewParentGroup());

            if (newParentGroup == null || oldParentGroup == null)
            {
                logger.error("Received a subscription for a group that we "
                             + "hadn't seen before! ");
                return;
            }

            MetaContactImpl currentMetaContact = (MetaContactImpl)
                               findMetaContactByContact(evt.getSourceContact());

            if(currentMetaContact == null)
            {
                logger.warn("Received a move event for a contact that is "
                            +"not in our contact list."
                            , new NullPointerException(
                                    "Received a move event for a contact that "
                                    +"is not in our contact list."));
                return;
            }

             //if the move was caused by us (when merging contacts) then chances
            //are that the contact is already in the right group
            MetaContactGroup currentParentGroup
                = currentMetaContact.getParentMetaContactGroup();

            if(currentParentGroup == newParentGroup)
            {
                return;
            }

            //if the meta contact does not have other children apart from the
            //contact that we're currently moving then move the whole meta
            //contact to the new parent group.
            if( currentMetaContact.getContactCount() == 1 )
            {
                oldParentGroup.removeMetaContact(currentMetaContact);
                newParentGroup.addMetaContact(currentMetaContact);
                fireMetaContactEvent(new MetaContactMovedEvent(
                    currentMetaContact, oldParentGroup, newParentGroup));
            }
            //if the source contact is not the only contact encapsulated by the
            //currentMetaContact, then create a new meta contact in the new
            //parent group and move the source contact to it.
            else
            {
                MetaContactImpl newMetaContact = new MetaContactImpl(
                        MetaContactListServiceImpl.this);
                newMetaContact.setDisplayName(evt
                                          .getSourceContact().getDisplayName());
                newParentGroup.addMetaContact(newMetaContact);

                //fire an event notifying that a new meta contact was added.
                fireMetaContactEvent(newMetaContact,
                                     newParentGroup,
                                     MetaContactEvent.META_CONTACT_ADDED);

                //move the proto contact and fire the corresponding event
                currentMetaContact.removeProtoContact(evt.getSourceContact());
                newMetaContact.addProtoContact(evt.getSourceContact());

                fireProtoContactEvent(evt.getSourceContact()
                                      , ProtoContactEvent.PROTO_CONTACT_MOVED
                                      , currentMetaContact
                                      , newMetaContact);
            }
        }

        public void subscriptionFailed(SubscriptionEvent evt)
        {
            logger.trace("Subscription failed: " + evt);
        }

        /**
         * Events delivered through this method are ignored as they are of no
         * interest to this implementation of the meta contact list service.
         * @param evt the SubscriptionEvent containing the source contact
         */
        public void subscriptionResolved(SubscriptionEvent evt)
        {
            //who cares?
        }

        /**
         * In the case where the event refers to a change in the display name
         * we compare the old value with the display name of the corresponding
         * meta contact. If they are equal this means that the user has not
         * specified their own display name for the meta contact and that the
         * display name was using this contact's display name for its own
         * display name. In this case we change the display name of the meta
         * contact to match the new display name of the proto contact.
         * <p>
         * @param evt the <tt>ContactPropertyChangeEvent</tt> containing the source
         * contact and the old and new values of the changed property.
         */
        public void contactModified(ContactPropertyChangeEvent evt)
        {
            MetaContactImpl mc
                = (MetaContactImpl)findMetaContactByContact(
                    evt.getSourceContact());

            if( evt.getPropertyName().equals(ContactPropertyChangeEvent
                                             .PROPERTY_DISPLAY_NAME)
                && evt.getOldValue() != null
                && ((String)evt.getOldValue()).equals(mc.getDisplayName()))
            {
                renameMetaContact(mc, (String)evt.getNewValue());
            }
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
        public void subscriptionRemoved(SubscriptionEvent evt)
        {

            logger.trace("Subscription removed: " + evt);

            MetaContactImpl metaContact = (MetaContactImpl)
                findMetaContactByContact(evt.getSourceContact());

            MetaContactGroupImpl metaContactGroup = (MetaContactGroupImpl)
                findMetaContactGroupByContactGroup(evt.getParentGroup());

            metaContact.removeProtoContact(evt.getSourceContact());

            //if this was the last protocol specific contact in this meta
            //contact then remove the meta contact as well.
            if (metaContact.getContactCount() == 0)
            {
                metaContactGroup.removeMetaContact(metaContact);

                fireMetaContactEvent(metaContact,
                                     metaContactGroup,
                                     MetaContactEvent.META_CONTACT_REMOVED);
            }
            else
            {
                //this was not the las proto contact so only generate the
                //corresponding event.
                fireProtoContactEvent(evt.getSourceContact(),
                    ProtoContactEvent.PROTO_CONTACT_REMOVED, metaContact, null);

            }
        }
    }

    /**
     * The class would listen for events delivered to
     * <tt>ServerStoredGroupListener</tt>s.
     */
    private class ContactListGroupListener
        implements
        ServerStoredGroupListener
    {

        /**
         * The method is called upon receiving notification that a new server
         * stored group has been created.
         * @param parent  a reference to the <tt>MetaContactGroupImpl</tt> where
         * <tt>group</tt>'s newly created <tt>MetaContactGroup</tt> wrapper
         * should be added as a subgroup.
         * @param group the newly added <tt>ContactGroup</tt>
         * @return the <tt>MetaContactGroup</tt> that now wraps the newly
         *  created <tt>ContactGroup</tt>.
         */
        private MetaContactGroup handleGroupCreatedEvent(
                                             MetaContactGroupImpl parent,
                                             ContactGroup group)
        {
            //if parent already contains a meta group with the same name, we'll
            //reuse it as the container for the new contact group.
            MetaContactGroupImpl newMetaGroup = (MetaContactGroupImpl)parent
                .getMetaContactSubgroup(group.getGroupName());

            //if there was no meta group with the specified name, create a new
            //one
            if(newMetaGroup == null)
            {
                newMetaGroup = new MetaContactGroupImpl(
                    group.getGroupName());
                newMetaGroup.addProtoGroup(group);
                parent.addSubgroup(newMetaGroup);
            }
            else
            {
                newMetaGroup.addProtoGroup(group);
            }

            //check if there were any subgroups
            Iterator subgroups = group.subgroups();

            while(subgroups.hasNext())
            {
                ContactGroup subgroup = (ContactGroup)subgroups.next();
                handleGroupCreatedEvent(newMetaGroup, subgroup);
            }

            Iterator contactsIter = group.contacts();

            while (contactsIter.hasNext())
            {
                Contact contact = (Contact) contactsIter.next();

                MetaContactImpl newMetaContact = new MetaContactImpl(
                        MetaContactListServiceImpl.this);

                newMetaContact.addProtoContact(contact);

                newMetaContact.setDisplayName(contact
                                              .getDisplayName());

                newMetaGroup.addMetaContact(newMetaContact);
            }

            return newMetaGroup;
        }

        /**
         * Adds the source group and its child contacts to the meta contact
         * list.
         * @param evt the ServerStoredGroupEvent containing the source group.
         */
        public void groupCreated(ServerStoredGroupEvent evt)
        {

            logger.trace("ContactGroup created: " + evt);

            //ignore the event if the source group is in the ignore list
            if (isGroupInEventIgnoreList(evt.getSourceGroup().getGroupName()
                                         , evt.getSourceProvider()))
            {
                return;
            }

            MetaContactGroupImpl parentMetaGroup = (MetaContactGroupImpl)
                findMetaContactGroupByContactGroup( evt.getParentGroup());

            if (parentMetaGroup == null)
            {
                logger.error("Failed to identify a parent where group "
                    + evt.getSourceGroup().getGroupName() + "should be placed.");
            }

            // add parent group to the ServerStoredGroupEvent
            MetaContactGroup newMetaGroup
                = handleGroupCreatedEvent(parentMetaGroup, evt.getSourceGroup());

            //if this was the first contact group in the meta group fire an
            //ADDED event. otherwise fire a modification event.
            if(newMetaGroup.countContactGroups() > 1)
            {
                fireMetaContactGroupEvent(
                    newMetaGroup
                    , evt.getSourceProvider()
                    , evt.getSourceGroup()
                    , MetaContactGroupEvent.CONTACT_GROUP_ADDED_TO_META_GROUP);
            }
            else
            {
                fireMetaContactGroupEvent(
                    newMetaGroup
                    , evt.getSourceProvider()
                    , evt.getSourceGroup()
                    , MetaContactGroupEvent.META_CONTACT_GROUP_ADDED);
            }
        }

        /**
         * Dummy implementation.
         * <p>
         * @param evt a ServerStoredGroupEvent containing the source group.
         */
        public void groupResolved(ServerStoredGroupEvent evt)
        {
            //we couldn't care less :)
        }

        /**
         * Updates the local contact list by removing the meta contact group
         * corresponding to the group indicated by the delivered <tt>evt</tt>
         * @param evt the ServerStoredGroupEvent confining the group that has
         * been removed.
         */
        public void groupRemoved(ServerStoredGroupEvent evt)
        {

            logger.trace("ContactGroup removed: " + evt);

            MetaContactGroupImpl metaContactGroup = (MetaContactGroupImpl)
                findMetaContactGroupByContactGroup(evt.getSourceGroup());

            if (metaContactGroup == null)
            {
                logger.error(
                    "Received a RemovedGroup event for an orphan grp: "
                    + evt.getSourceGroup());
                return;
            }

            removeContactGroupFromMetaContactGroup(metaContactGroup,
                evt.getSourceGroup(), evt.getSourceProvider());

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
        public void groupNameChanged(ServerStoredGroupEvent evt)
        {

            logger.trace("ContactGroup renamed: " + evt);

            MetaContactGroup metaContactGroup
                = findMetaContactGroupByContactGroup(evt.getSourceGroup());

            fireMetaContactGroupEvent(
                metaContactGroup
                , evt.getSourceProvider()
                , evt.getSourceGroup()
                , MetaContactGroupEvent.CONTACT_GROUP_RENAMED_IN_META_GROUP);
        }
    }

    /**
     * Creates the corresponding MetaContact event and notifies all
     * <tt>MetaContactListListener</tt>s that a MetaContact is added or
     * removed from the MetaContactList.
     *
     * @param sourceContact the contact that this event is about.
     * @param parentGroup the group that the source contact belongs or belonged
     * to.
     * @param eventID the id indicating the exavt type of the event to fire.
     */
    private void fireMetaContactEvent(MetaContact sourceContact,
                                      MetaContactGroup parentGroup,
                                      int eventID)
    {
        MetaContactEvent evt
            = new MetaContactEvent(sourceContact, parentGroup, eventID);
        logger.trace("Will dispatch the following mcl event: "
                     + evt);

        Iterator listeners = null;
        synchronized (metaContactListListeners)
        {
            listeners = new ArrayList(metaContactListListeners).iterator();
        }

        while (listeners.hasNext())
        {
            MetaContactListListener listener
                = (MetaContactListListener) listeners.next();

            switch (evt.getEventID())
            {
                case MetaContactEvent.META_CONTACT_ADDED:
                    listener.metaContactAdded(evt);
                    break;
                case MetaContactEvent.META_CONTACT_REMOVED:
                    listener.metaContactRemoved(evt);
                    break;
                default:
                    logger.error("Unknown event type " + evt.getEventID());
            }
        }
    }

    /**
     * Creates the corresponding <tt>MetaContactPropertyChangeEvent</tt>
     * instance and notifies all <tt>MetaContactListListener</tt>s that a
     * MetaContact has been modified.
     *
     * @param event the event to dispatch.
     */
    void fireMetaContactEvent(MetaContactPropertyChangeEvent event)
    {
        logger.trace("Will dispatch the following mcl property change event: "
                     + event);

        Iterator listeners = null;
        synchronized (metaContactListListeners)
        {
            listeners = new ArrayList(metaContactListListeners).iterator();
        }

        while (listeners.hasNext())
        {
            MetaContactListListener listener
                = (MetaContactListListener) listeners.next();

            if (event instanceof MetaContactMovedEvent)
            {
                listener.metaContactMoved( (MetaContactMovedEvent) event);
            }
            else if (event instanceof MetaContactRenamedEvent)
            {
                listener.metaContactRenamed( (MetaContactRenamedEvent) event);
            }
            else if (event instanceof MetaContactModifiedEvent)
            {
                listener.metaContactModified( (MetaContactModifiedEvent) event);
            }
        }
    }

    /**
     * Creates the corresponding <tt>MetaContactPropertyChangeEvent</tt>
     * instance and notifies all <tt>MetaContactListListener</tt>s that a
     * MetaContact has been modified.
     *
     * @param event the event to dispatch.
     */
    private void fireMetaContactPropertyChangeEvent(
                                    MetaContactPropertyChangeEvent event)
    {
        logger.trace("Will dispatch the following mcl property change event: "
                     + event);

        Iterator listeners = null;
        synchronized (metaContactListListeners)
        {
            listeners = new ArrayList(metaContactListListeners).iterator();
        }

        while (listeners.hasNext())
        {
            MetaContactListListener listener
                = (MetaContactListListener) listeners.next();

            if (event instanceof MetaContactMovedEvent)
            {
                listener.metaContactMoved( (MetaContactMovedEvent) event);
            }
            else if (event instanceof MetaContactRenamedEvent)
            {
                listener.metaContactRenamed( (MetaContactRenamedEvent) event);
            }
        }
    }

    /**
     * Creates the corresponding <tt>ProtoContactEvent</tt> instance and
     * notifies all <tt>MetaContactListListener</tt>s that a protocol specific
     * <tt>Contact</tt> has been added moved or removed.
     *
     * @param source the contact that has caused the event.
     * @param eventName One of the ProtoContactEvent.PROTO_CONTACT_XXX fields
     * indicating the exact type of the event.
     * @param oldParent the <tt>MetaContact</tt> that was wrapping the source
     * <tt>Contact</tt> before the event occurred or <tt>null</tt> if the event
     * is caused by adding a new <tt>Contact</tt>
     * @param newParent the <tt>MetaContact</tt> that is wrapping the source
     * <tt>Contact</tt> after the event occurred or <tt>null</tt> if the event
     * is caused by removing a <tt>Contact</tt>
     */
    private void fireProtoContactEvent(Contact     source,
                                       String      eventName,
                                       MetaContact oldParent,
                                       MetaContact newParent)
    {
        ProtoContactEvent event
            = new ProtoContactEvent(source, eventName, oldParent, newParent );

        logger.trace("Will dispatch the following mcl property change event: "
                     + event);

        Iterator listeners = null;
        synchronized (metaContactListListeners)
        {
            listeners = new ArrayList(metaContactListListeners).iterator();
        }

        while (listeners.hasNext())
        {
            MetaContactListListener listener
                = (MetaContactListListener) listeners.next();

            if (eventName.equals(ProtoContactEvent.PROTO_CONTACT_ADDED))
            {
                listener.protoContactAdded(event);
            }
            else if (eventName.equals(ProtoContactEvent
                                      .PROTO_CONTACT_MOVED))
            {
                listener.protoContactMoved(event);
            }
            else if (eventName.equals(ProtoContactEvent
                                      .PROTO_CONTACT_REMOVED))
            {
                listener.protoContactRemoved(event);
            }
        }
    }

    /**
     * Upon each status notification this method finds the corresponding meta
     * contact and updates the ordering in its parent group.
     * <p>
     * @param evt the ContactPresenceStatusChangeEvent describing the status
     * change.
     */
    public void contactPresenceStatusChanged(
        ContactPresenceStatusChangeEvent evt)
    {
        MetaContactImpl metaContactImpl =
            (MetaContactImpl) findMetaContactByContact(evt.getSourceContact());

        //ignore if we have no meta contact.
        if(metaContactImpl == null)
            return;

        int oldContactIndex = metaContactImpl.getParentGroup()
            .indexOf(metaContactImpl);

        int newContactIndex = metaContactImpl.reevalContact();

        if(oldContactIndex != newContactIndex)
        {
            fireMetaContactGroupEvent(
                findParentMetaContactGroup(metaContactImpl)
                , evt.getSourceProvider()
                , null
                , MetaContactGroupEvent.CHILD_CONTACTS_REORDERED);
        }
    }


    /**
     * The method is called from the storage manager whenever a new contact
     * group has been parsed and it has to be created.
     * @param parentGroup the group that contains the meta contact group we're
     * about to load.
     * @param metaContactGroupUID the unique identifier of the meta contact
     * group.
     * @param displayName the name of the meta contact group.
     *
     * @return the newly created meta contact group.
     */
    MetaContactGroupImpl loadStoredMetaContactGroup(
        MetaContactGroup parentGroup,
        String metaContactGroupUID,
        String displayName)
    {
        //first check if the group exists already.
        MetaContactGroupImpl newMetaGroup = ((MetaContactGroupImpl)parentGroup)
            .getMetaContactSubgroupByUID(metaContactGroupUID);

        //if the group exists then we have already loaded it for another
        //account and we should reuse the same instance.
        if(newMetaGroup != null)
            return newMetaGroup;

        newMetaGroup
            = new MetaContactGroupImpl(displayName, metaContactGroupUID);

        ((MetaContactGroupImpl)parentGroup).addSubgroup(newMetaGroup);

        //I don't think this method needs to produce events since it is
        //currently only called upon initialization ... but it doesn't hurt
        //trying
        fireMetaContactGroupEvent(newMetaGroup, null, null
            , MetaContactGroupEvent.META_CONTACT_GROUP_ADDED);

        return newMetaGroup;
    }

    /**
     * Creates a unresolved instance of the proto specific contact group
     * according to the specified arguments and adds it to
     * <tt>containingMetaContactGroup</tt>
     *
     * @param containingMetaGroup the <tt>MetaContactGroupImpl</tt> where the
     * restored contact group should be added.
     * @param contactGroupUID the unique identifier of the group.
     * @param parentProtoGroup the identifier of the parent proto group.
     * @param persistentData the persistent data last returned by the contact
     * group.
     * @param accountID the ID of the account that the proto group belongs to.
     *
     * @return a reference to the newly created (unresolved) contact group.
     */
    ContactGroup loadStoredContactGroup(MetaContactGroupImpl containingMetaGroup,
                                        String               contactGroupUID,
                                        ContactGroup         parentProtoGroup,
                                        String               persistentData,
                                        String               accountID)
    {
        //get the presence op set
        ProtocolProviderService sourceProvider = (ProtocolProviderService)
            currentlyInstalledProviders.get(accountID);
        OperationSetPersistentPresence presenceOpSet =
            (OperationSetPersistentPresence)sourceProvider
                .getSupportedOperationSets().get(OperationSetPersistentPresence
                                                    .class.getName());

        ContactGroup newProtoGroup = presenceOpSet.createUnresolvedContactGroup(
            contactGroupUID, persistentData,
                (parentProtoGroup == null)
                    ? presenceOpSet.getServerStoredContactListRoot()
                    : parentProtoGroup);

        containingMetaGroup.addProtoGroup(newProtoGroup);

        return newProtoGroup;

    }

    /**
     * The method is called from the storage manager whenever a new contact
     * has been parsed and it has to be created.
     * @param parentGroup the group that contains the meta contact we're about
     * to load.
     * @param metaUID the unique identifier of the meta contact.
     * @param displayName the display name of the meta contact.
     * @param details the details for the contact to create.
     * @param protoContacts a list containing descriptors of proto contacts
     * encapsulated by the meta contact that we're about to create.
     * @param accountID the identifier of the account that the contacts
     * originate from.
     */
    void loadStoredMetaContact(MetaContactGroupImpl parentGroup,
                               String metaUID,
                               String displayName,
                               Hashtable    details,
                               List    protoContacts,
                               String accountID)
    {
        //first check if the meta contact exists already.
        MetaContactImpl newMetaContact
            = (MetaContactImpl)findMetaContactByMetaUID(metaUID);

        if(newMetaContact == null)
        {
            newMetaContact = new MetaContactImpl(this, metaUID, details);
            newMetaContact.setDisplayName(displayName);
        }

        //create unresolved contacts for the protocontacts associated with this
        //mc
        ProtocolProviderService sourceProvider = (ProtocolProviderService)
            currentlyInstalledProviders.get(accountID);
        OperationSetPersistentPresence presenceOpSet =
            (OperationSetPersistentPresence)sourceProvider
                .getSupportedOperationSets().get(OperationSetPersistentPresence
                                                    .class.getName());

        Iterator contactsIter = protoContacts.iterator();
        while (contactsIter.hasNext())
        {
            MclStorageManager.StoredProtoContactDescriptor contactDescriptor
                = (MclStorageManager.StoredProtoContactDescriptor)contactsIter
                    .next();

            if(contactDescriptor.contactAddress.indexOf("238431632") > -1)
                logger.debug("asdfasdfasdfasdfasdfasdfasdf");


            //this contact has already been registered by another meta contact
            //so we'll ignore it. If this is the only contact in the meta
            //contact, we'll throw an exception at the end of the method and
            //cause the mcl storage manager to remove it.
            MetaContact mc = findMetaContactByContact(
                contactDescriptor.contactAddress, accountID);

            if(mc != null)
            {
                logger.warn("Ignoring duplicate proto contact "
                            + contactDescriptor
                            + " accountID=" + accountID
                            + ". The contact was also present in the "
                            + "folloing meta contact:" + mc);
                continue;
            }


            Contact protoContact = presenceOpSet.createUnresolvedContact(
                contactDescriptor.contactAddress,
                contactDescriptor.persistentData,
                ( contactDescriptor.parentProtoGroup == null )
                    ? presenceOpSet.getServerStoredContactListRoot()
                    : contactDescriptor.parentProtoGroup);

            newMetaContact.addProtoContact(protoContact);
        }

        if(newMetaContact.getContactCount() == 0)
        {
            logger.error("Found an empty meta contact. Throwing an exception "
                + "so that the storage manager would remove it.");
            throw new IllegalArgumentException("MetaContact["
                + newMetaContact
                +"] contains no non-duplicating child contacts.");
        }

        parentGroup.addMetaContact(newMetaContact);

        logger.trace("Created meta contact: " + newMetaContact);
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
     * @param sourceProtoGroup the proto group associated with this event or
     *            null if the event does not concern a particular source group.
     * @param eventID
     *            one of the METACONTACT_GROUP_XXX static fields indicating the
     *            nature of the event.
     */
    private void fireMetaContactGroupEvent( MetaContactGroup source,
                                            ProtocolProviderService provider,
                                            ContactGroup sourceProtoGroup,
                                            int eventID)
    {
        MetaContactGroupEvent evt = new MetaContactGroupEvent(
            source, provider, sourceProtoGroup, eventID);

        logger.trace("Will dispatch the following mcl event: "
                     + evt);



        Iterator listeners = null;
        synchronized (metaContactListListeners)
        {
            listeners = new ArrayList(metaContactListListeners).iterator();
        }

        while (listeners.hasNext())
        {
            MetaContactListListener listener
                = (MetaContactListListener) listeners.next();

            switch (eventID)
            {
                case MetaContactGroupEvent.META_CONTACT_GROUP_ADDED:
                    listener.metaContactGroupAdded(evt);
                    break;
                case MetaContactGroupEvent.META_CONTACT_GROUP_REMOVED:
                    listener.metaContactGroupRemoved(evt);
                    break;
                case MetaContactGroupEvent.CHILD_CONTACTS_REORDERED:
                    listener.childContactsReordered(evt);
                    break;
                case MetaContactGroupEvent
                    .META_CONTACT_GROUP_RENAMED:
                case MetaContactGroupEvent
                    .CONTACT_GROUP_RENAMED_IN_META_GROUP:
                case MetaContactGroupEvent
                    .CONTACT_GROUP_REMOVED_FROM_META_GROUP:
                case MetaContactGroupEvent
                    .CONTACT_GROUP_ADDED_TO_META_GROUP:
                    listener.metaContactGroupModified(evt);
                    break;
                default:
                    logger.error("Unknown event type (" + eventID
                                 + ") for event: " + evt);
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
         * Called whoever an indication is received that a new server stored
         * group is created.
         * @param evt a ServerStoredGroupChangeEvent containing a reference to
         * the newly created group.
         */
        public void groupCreated(ServerStoredGroupEvent evt)
        {
            synchronized (this)
            {
                if (evt.getSourceGroup().getGroupName().equals(groupName))
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
        public void groupRemoved(ServerStoredGroupEvent evt)
        {}

        /**
         * Evens delivered through this method are ignored
         * @param evt param ignored
         */
        public void groupNameChanged(ServerStoredGroupEvent evt)
        {}

        /**
         * Evens delivered through this method are ignored
         * @param evt param ignored
         */
        public void groupResolved(ServerStoredGroupEvent evt)
        {}

        /**
         * Block the execution of the current thread until either a group
         * created event is received or milis miliseconds pass.
         * @param millis the number of millis that we should wait before we
         * determine failure.
         */
        public void waitForEvent(long millis)
        {
            synchronized (this)
            {
                //no need to wait if an event is already there.
                if (evt != null)
                {
                    return;
                }

                try
                {
                    this.wait(millis);
                }
                catch (InterruptedException ex)
                {
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
        implements SubscriptionListener,
                   ServerStoredGroupListener
    {
        private String      subscriptionAddress = null;
        public  Contact     sourceContact = null;
        public  EventObject evt = null;

        /**
         * Events delivered through this method are ignored
         * @param evt param ignored
         */
        public void groupResolved(ServerStoredGroupEvent evt)
        {}

        /**
         * Events delivered through this method are ignored
         * @param evt param ignored
         */
        public void groupRemoved(ServerStoredGroupEvent evt)
        {}

        /**
         * Events delivered through this method are ignored
         * @param evt param ignored
         */
        public void groupNameChanged(ServerStoredGroupEvent evt)
        {}

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
         * Called whenever an indication is received that a new server stored group
         * is created.
         * @param evt a ServerStoredGroupEvent containing a reference to the
         * newly created group.
         */
        public void groupCreated(ServerStoredGroupEvent evt)
        {
            synchronized (this)
            {
                Contact contact
                    = evt.getSourceGroup().getContact(subscriptionAddress);
                if ( contact != null)
                {
                    this.evt = evt;
                    this.sourceContact = contact;
                    this.notifyAll();
                }
            }

        }

        /**
         * Called whenever an indication is received that a subscription is
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
                    this.sourceContact = evt.getSourceContact();
                    this.notifyAll();
                }
            }
        }

        /**
         * Events delivered through this method are ignored
         * @param evt param ignored
         */
        public void subscriptionRemoved(SubscriptionEvent evt)
        {}

        /**
         * Called whenever an indication is received that a subscription 
         * creation has failed.
         * @param evt a <tt>SubscriptionEvent</tt> containing a reference to
         * the contact we are trying to subscribe.
         */
        public void subscriptionFailed(SubscriptionEvent evt)
        {
            synchronized (this)
            {
                if (evt.getSourceContact().getAddress()
                    .equals(subscriptionAddress))
                {
                    this.evt = evt;
                    this.sourceContact = evt.getSourceContact();
                    this.notifyAll();
                }
            }
        }

        /**
         * Events delivered through this method are ignored
         * @param evt param ignored
         */
        public void subscriptionMoved(SubscriptionMovedEvent evt)
        {}

        /**
         * Events delivered through this method are ignored
         * @param evt param ignored
         */
        public void subscriptionResolved(SubscriptionEvent evt)
        {}

        /**
         * Events delivered through this method are ignored
         * @param evt param ignored
         */
        public void contactModified(ContactPropertyChangeEvent evt)
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
                if (evt != null)
                {
                    return;
                }

                try
                {
                    this.wait(millis);
                }
                catch (InterruptedException ex)
                {
                    logger.error(
                        "Interrupted while waiting for contact creation"
                        , ex);
                }
            }
        }
    }

}
