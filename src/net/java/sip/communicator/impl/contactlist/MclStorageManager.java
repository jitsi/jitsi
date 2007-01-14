/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.contactlist;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;

import org.osgi.framework.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.fileaccess.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.xml.*;
import net.java.sip.communicator.util.xml.XMLUtils;

/**
 * The class handles read / write operations over the file where a persistent
 * copy of the meta contact list is stored.
 * <p>
 * The load / resolve strategy that we use when storing contact lists is
 * roughly the following:
 * <p>
 * 1) The MetaContactListService is started. <br>
 * 2) If no file exists for the meta contact list, create one.
 * 3) We receive an OSGI event telling us that a new ProtocolProviderService
 *    is registered or we simply retrieve one that was already in the bundle
 * 4) We look through the contact list file and load groups and contacts
 *    belonging to this new provider. Unresolved proto groups and contacts
 *    will be created for every one of them.
 * <p>
 *
 * @author Emil Ivov
 */
public class MclStorageManager
        implements MetaContactListListener
{
    private static final Logger logger =
        Logger.getLogger(MclStorageManager.class.getName());

    /**
     * Indicates whether the storage manager has been properly started or in
     * other words that it has successfully found and read the xml contact list
     * file.
     */
    private boolean started = false;

    /**
     * Indicates whether there has been a change since the last time we stored
     * this contact list. Used by the storage methods.
     */
    private boolean isModified = false;

    /**
     * A currently valid reference to the OSGI bundle context,
     */
    private BundleContext     bundleContext = null;

    /**
     * The file acces service that we'll be using in order to obtain a reference
     * to the contact list file.
     */
    private FileAccessService faService = null;

    /**
     * A reference to the currently valid instance of the configuration service.
     */
    private ConfigurationService configurationService = null;

    /**
     * The name of the system property that stores the name of the contact list
     * file.
     */
    private static final String FILE_NAME_PROPERTY =
        "net.java.sip.communicator.CONTACTLIST_FILE_NAME";

    /**
     * The XML Document containing the contact list file.
     */
    private Document contactListDocument = null;

    /**
     * A reference to the file containing the locally stored meta contact list.
     */
    private File contactlistFile = null;

    /**
     * A regerence to the MetaContactListServiceImpl that created and started us.
     */
    private MetaContactListServiceImpl mclServiceImpl = null;

    /**
     * The name of the system property that stores the name of the contact list
     * file.
     */
    private static final String DEFAULT_FILE_NAME = "contactlist.xml";

    /**
     * The name of the node that represents the contact list root.
     */
    private static String DOCUMENT_ROOT_NAME = "sip-communicator";

    /**
     * The name of the XML node corresponding to a meta contact group.
     */
    private static final String GROUP_NODE_NAME = "group";

    /**
     * The name of the XML node corresponding to a collection of meta contact
     * subgroups.
     */
    private static final String SUBGROUPS_NODE_NAME = "subgroups";

    /**
     * The name of the XML attribute that contains group names.
     */
    private static final String GROUP_NAME_ATTR_NAME = "name";

    /**
     * The name of the XML attribute that contains group UIDs.
     */
    private static final String GROUP_UID_ATTR_NAME = "uid";

    /**
     * The name of the XML node that contains protocol specific group
     * descriptorS.
     */
    private static final String PROTO_GROUPS_NODE_NAME = "proto-groups";

    /**
     * The name of the XML node that contains A protocol specific group
     * descriptor.
     */
    private static final String PROTO_GROUP_NODE_NAME = "proto-group";

    /**
     * The name of the XML attribute that contains unique identifiers
     */
    private static final String UID_ATTR_NAME = "uid";

    /**
     * The name of the XML attribute that contains unique identifiers for
     * parent contact groups.
     */
    private static final String PARENT_PROTO_GROUP_UID_ATTR_NAME
        = "parent-proto-group-uid";

    /**
     * The name of the XML attribute that contains account identifers indicating
     * proto group's and proto contacts' owning providers.
     */
    private static final String ACCOUNT_ID_ATTR_NAME = "account-id";

    /**
     * The name of the XML node that contains meta contact details.
     */
    private static final String META_CONTACT_NODE_NAME = "meta-contact";

    /**
     * The name of the XML node that contains meta contact display names.
     */
    private static final String META_CONTACT_DISPLAY_NAME_NODE_NAME
        = "display-name";

    /**
     * The name of the XML node that contains information of a proto contact
     */
    private static final String PROTO_CONTACT_NODE_NAME = "contact";

    /**
     * The name of the XML node that contains information of a proto contact
     */
    private static final String PROTO_CONTACT_ADDRESS_ATTR_NAME = "address";

    /**
     * The name of the XML node that contains information that contacts or
     * groups returned as persistent and that should be used when restoring
     * a contact or a group.
     */
    private static final String PERSISTENT_DATA_NODE_NAME = "persistent-data";

    /**
     * The name of the XML node that contains all meta contact nodes inside
     * a group
     */
    private static final String CHILD_CONTACTS_NODE_NAME = "child-contacts";

    /**
     * A lock that we use when storing the contact list to avoid being exited
     * while in there.
     */
    private static final Object contactListRWLock = new Object();

    /**
     * Determines whether the storage manager has been properly started or in
     * other words that it has successfully found and read the xml contact list
     * file.
     *
     * @return true if the storage manager has been successfully initialialized
     * and false otherwise.
     */
    boolean isStarted()
    {
        return started;
    }

    /**
     * Prepares the storage manager for shutdown.
     */
    public void stop ()
    {
        logger.trace("Stopping the MCL XML storage manager.");
        this.started = false;
        synchronized(contactListRWLock)
        {
            this.contactListRWLock.notifyAll();
        }
    }


    /**
     * Initializes the storage manager and makes it do initial load and parsing
     * of the contact list file.
     *
     * @param bc a reference to the currently valid OSGI <tt>BundleContext</tt>
     * @param mclServiceImpl a reference to the currently valid instance of the
     * <tt>MetaContactListServiceImpl</tt> that we could use to pass parsed
     * contacts and contact groups.
     * @throws IOException if the contact lsit file specified file does not
     * exist and could not be created.
     * @throws XMLException if there is a problem with the file syntax.
     */
    void start(BundleContext bc,
               MetaContactListServiceImpl mclServiceImpl)
        throws IOException, XMLException
    {
        bundleContext = bc;

        //retrieve a reference to the file access service.
        ServiceReference faServiceReference = bundleContext.getServiceReference(
            FileAccessService.class.getName());

        faService = (FileAccessService) bundleContext
            .getService(faServiceReference);

        //retrieve a reference to the file access service.
        ServiceReference confServiceRefs = bundleContext.getServiceReference(
            ConfigurationService.class.getName());

        configurationService = (ConfigurationService) bundleContext
            .getService(confServiceRefs);

        String fileName = configurationService.getString(FILE_NAME_PROPERTY);

        if( fileName == null )
        {
            fileName = System.getProperty(FILE_NAME_PROPERTY);
            if( fileName == null )
                fileName = DEFAULT_FILE_NAME;
        }
        //get a reference to the contact list file.
        try{
            contactlistFile  = faService.getPrivatePersistentFile(fileName);

            if(!contactlistFile.exists())
                if(!contactlistFile.createNewFile())
                    throw new IOException("Failed to create file"
                                          + contactlistFile.getAbsolutePath());
        }
        catch (Exception ex)
        {
            logger.error("Failed to get a reference to the contact list file."
                         , ex);
            throw new IOException("Failed to get a reference to the contact "
                                  +"list file="+fileName
                                  +". error was:" + ex.getMessage());
        }

        try
        {
            //load the contact list
            DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            if(contactlistFile.length() == 0)
            {
                //if the contact list does not exist - create it.
                contactListDocument = builder.newDocument();
                initVirginDocument(mclServiceImpl, contactListDocument);

                //write the contact list so that it is there for the parser
                storeContactList0();
            }
            else
            {
                contactListDocument = builder.parse(contactlistFile);
            }
        }
        catch (SAXException ex)
        {
            logger.error("Error parsing configuration file", ex);
            throw new XMLException(ex.getMessage(), ex);
        }
        catch (ParserConfigurationException ex)
        {
            //it is not highly probable that this might happen - so lets just
            //log it.
            logger.error("Error finding configuration for default parsers", ex);
        }

        this.launchStorageThread();
        mclServiceImpl.addMetaContactListListener(this);
        this.mclServiceImpl = mclServiceImpl;
        started = true;
    }

    /**
     * Stores the contact list in its current state.
     * @throws IOException if writing fails.
     */
    private void scheduleContactListStorage() throws IOException
    {
        synchronized(this.contactListRWLock)
        {
            if (!isStarted())
                return;

            this.isModified = true;
            this.contactListRWLock.notifyAll();
        }
    }

    /**
     * Writes the contact list on the hard disk.
     * @throws IOException in case writing fails.
     */
    private void storeContactList0() throws IOException
    {
        logger.trace("storing contact list. because is started =="
                     + isStarted());
        logger.trace("storing contact list. because is modified =="
                     + isModified);
        if(isStarted())
        {
            XMLUtils.indentedWriteXML(contactListDocument
                                      , new FileOutputStream(contactlistFile));
        }
    }

    /**
     * Launches a separate thread that waits on the contact list rw lock and
     * when notified stores the contact list in case there have been
     * modifications since last time it saved.
     */
    private void launchStorageThread()
    {
        new Thread()
        {
            public void run()
            {
                try
                {
                    synchronized (contactListRWLock)
                    {
                        while(isStarted())
                        {
                            contactListRWLock.wait(5000);
                            if (isModified)
                            {
                                storeContactList0();
                                isModified = false;
                            }
                        }
                    }
                }
                catch (IOException ex)
                {
                    logger.error("Storing contact list failed", ex);
                    started = false;
                }
                catch (InterruptedException ex)
                {
                    logger.error("Storing contact list failed", ex);
                    started = false;
                }

            }
        }.start();
    }

    /**
     * Returns the object that we use to lock when writing the contact list.
     * @return the object that we use to lock when writing the contact list.
     */
    public Object getContactListRWLock()
    {
        return contactListRWLock;
    }

    /**
     * Stops the storage manager and performs a final write
     */
    public void storeContactListAndStopStorageManager()
    {

        synchronized(getContactListRWLock())
        {
            if (!isStarted())
                return;

            started = false;

            //make sure everyone gets released after we finish.
            getContactListRWLock().notifyAll();

            //write the contact list ourselves before we go out..
            try
            {
                storeContactList0();
            }
            catch (IOException ex)
            {
                logger.debug("Failed to store contact list before stopping", ex);
            }
        }
    }

    /**
     * update persistent data in the dom object model
     * for the given metacontact and its contacts
     *
     * @param metaContact MetaContact target meta contact
     */
    private void updatePersistentDataForMetaContact(MetaContact metaContact)
    {
        Element metaContactNode =
                    findMetaContactNode(metaContact.getMetaUID());

        Iterator iter = metaContact.getContacts();
        while (iter.hasNext())
        {
            Contact item = (Contact)iter.next();

            if(item.getPersistentData() != null)
            {
                Element currentNode = XMLUtils.locateElement(
                    metaContactNode
                    , PROTO_CONTACT_NODE_NAME
                    , PROTO_CONTACT_ADDRESS_ATTR_NAME
                    , item.getAddress());

                Element persistentDataNode = XMLUtils.findChild(
                    currentNode, PERSISTENT_DATA_NODE_NAME);

                // if node does not exist - create it
                if (persistentDataNode == null)
                {
                    persistentDataNode = contactListDocument.createElement(
                        PERSISTENT_DATA_NODE_NAME);

                    currentNode.appendChild(persistentDataNode);
                }

                XMLUtils.setText(persistentDataNode, item.getPersistentData());
            }
        }
    }

    /**
     * Fills the document with the tags necessary for it to be filled properly
     * as the meta contact list evolves.
     *
     * @param mclServiceImpl the meta contact list service to use when
     * initializing the document.
     * @param contactListDocument the document to init.
     */
    private void initVirginDocument(MetaContactListServiceImpl mclServiceImpl,
                                    Document contactListDocument)
    {
        Element root = contactListDocument.createElement(DOCUMENT_ROOT_NAME);

        contactListDocument.appendChild(root);

        //create the rootGroup
        Element rootGroup = createMetaContactGroupNode(mclServiceImpl.getRoot());

        root.appendChild(rootGroup);
    }

    /**
     * Parses the contact list
     * file and calls corresponding "add" methods
     * belonging to <tt>mclServiceImpl</tt> for every meta contact and meta
     * contact group stored in the (conteactlist.xml) file that correspond to a
     * provider carying the specified <tt>accountID</tt>.
     *
     * @param accountID the identifier of the account whose contacts we're
     * interested in.
     * @throws XMLException if a problem occurs while parsing contact lsit
     * contents.
     */
    void extractContactsForAccount(String accountID)
        throws XMLException
    {
        if(!isStarted())
            return;
        try
        {
            //we don't want to receive meta contact events triggerred by ourselves
            //so we stop listening. it is possible but very unlikely that other
            //events, not triggerred by us are received while we're off the channel
            //but that would be a very bizzare case ..... I guess we got to live
            //with the risk.
            this.mclServiceImpl.removeMetaContactListListener(this);

            Element root = findMetaContactGroupNode(
                mclServiceImpl.getRoot().getMetaUID());

            //parse the group node and extract all its child groups and contacts
            processGroupXmlNode(mclServiceImpl, accountID, root
                                , null, null);

            //now that we're done updating the contact list we can start listening
            //again
            this.mclServiceImpl.addMetaContactListListener(this);
        }catch(Throwable exc)
        {
            // catch everything because we MUST NOT disturb the thread
            //initializing the meta CL for a new provider with null point
            //exceptions or others of the sort
            throw new XMLException("Failed to extract contacts for account "
                                   +accountID, exc);
        }
    }

    /**
     * Parses <tt>groupNode</tt> and all of its subnodes, createing
     * corresponding instances through <tt>mclServiceImpl</tt> as children of
     * <tt>parentGroup</tt>
     *
     * @param mclServiceImpl the <tt>MetaContactListServiceImpl</tt> for
     * creating new contacts and groups.
     * @param accountID a String identifier of the account whose contacts we're
     * interested in.
     * @param groupNode teh XML <tt>Element</tt> that points to the group we're
     * currently parsing.
     * @param parentGroup the <tt>MetaContactGroupImpl</tt> where we should be
     * creating children.
     * @param parentProtoGroups a Map containing all proto groups that could
     * be parents of any groups parsed from the specified groupNode. The map
     * binds UIDs to group references and may be null for top level groups.
     */
    private void processGroupXmlNode(
        MetaContactListServiceImpl mclServiceImpl,
        String accountID,
        Element groupNode,
        MetaContactGroup parentGroup,
        Map parentProtoGroups)
    {
        //first resolve the group itself.(unless this is the meta contact list
        //root which is already resolved)
        MetaContactGroupImpl currentMetaGroup = null;

        //in this map we store all proto groups that we find in this meta group
        //(unless this is the MCL root)in order to pass them as parent
        //references to any subgroups.
        Map protoGroupsMap = new Hashtable();

        if(parentGroup == null)
        {
            currentMetaGroup = mclServiceImpl.rootMetaGroup;
        }
        else
        {
            String groupMetaUID = XMLUtils.getAttribute(
                groupNode, GROUP_UID_ATTR_NAME);
            String groupDisplayName = XMLUtils.getAttribute(
                groupNode, GROUP_NAME_ATTR_NAME);

            //create the meta group
            currentMetaGroup = mclServiceImpl
                .loadStoredMetaContactGroup(parentGroup
                                            , groupMetaUID
                                            , groupDisplayName);
            //extract and load one by one all proto groups in this meta group.
            Node protoGroupsNode = XMLUtils.findChild(
                groupNode, PROTO_GROUPS_NODE_NAME);

            NodeList protoGroups = protoGroupsNode.getChildNodes();

            for (int i = 0; i < protoGroups.getLength(); i++)
            {
                Node currentProtoGroupNode = protoGroups.item(i);

                if (currentProtoGroupNode.getNodeType() != Node.ELEMENT_NODE)
                    continue;

                String groupAccountID = XMLUtils.getAttribute(
                    currentProtoGroupNode, ACCOUNT_ID_ATTR_NAME);

                if (!accountID.equals(groupAccountID))
                    continue;

                String protoGroupUID = XMLUtils.getAttribute(
                    currentProtoGroupNode, UID_ATTR_NAME);

                String parentProtoGroupUID = XMLUtils.getAttribute(
                    currentProtoGroupNode, PARENT_PROTO_GROUP_UID_ATTR_NAME);

                Element persistentDataNode = XMLUtils.findChild(
                    (Element) currentProtoGroupNode
                    , PERSISTENT_DATA_NODE_NAME);

                String persistentData = "";

                if (persistentDataNode != null)
                {
                    persistentData = XMLUtils.getText(persistentDataNode);
                }

                //try to find the parent proto group for the one we're currently
                //parsing.
                ContactGroup parentProtoGroup = null;
                if (parentProtoGroups != null && parentProtoGroups.size() > 0)
                    parentProtoGroup = (ContactGroup) parentProtoGroups
                        .get(parentProtoGroupUID);

                //create the proto group
                ContactGroup newProtoGroup = mclServiceImpl.
                    loadStoredContactGroup
                    (currentMetaGroup, protoGroupUID, parentProtoGroup
                     , persistentData, accountID);

                protoGroupsMap.put(protoGroupUID, newProtoGroup);
            }

            //if this is not the meta contact list root and if it doesn't
            //contain proto groups of the account we're currently loading then
            //we don't need to recurese it since it cound contain any child
            //contacts of the same account.
            if (protoGroupsMap.size() == 0)
                return;
        }


        //we have parsed groups now go over the children
        Node childContactsNode = XMLUtils.findChild(
                                        groupNode, CHILD_CONTACTS_NODE_NAME);

        NodeList childContacts = (childContactsNode == null)
            ? null
            : childContactsNode.getChildNodes();

        //go over every meta contact, extract its details and its encapsulated
        //proto contacts
        for(int i = 0
            ; childContacts != null && i < childContacts.getLength()
            ; i++)
        {
            Node currentMetaContactNode = childContacts.item(i);

            if (currentMetaContactNode.getNodeType() != Node.ELEMENT_NODE)
                continue;

            String uid = XMLUtils.getAttribute(currentMetaContactNode
                                               , UID_ATTR_NAME);

            Element displayNameNode = XMLUtils.findChild(
                (Element)currentMetaContactNode
                , META_CONTACT_DISPLAY_NAME_NODE_NAME);

            String displayName = XMLUtils.getText(displayNameNode);

            //extract a map of all encapsulated proto contacts
            List protoContacts
                = extractProtoContacts((Element)currentMetaContactNode
                                       , accountID
                                       , protoGroupsMap);

            //if the size of the map is 0 then the meta contact does not contain
            //any contacts matching the currently parsed account id.
            if( protoContacts.size() < 1 )
                continue;

            //pass the parsed proto contacts to the mcl service
            mclServiceImpl.loadStoredMetaContact(
                currentMetaGroup, uid, displayName, protoContacts, accountID);
        }

        //now, last thing that's left to do - go over all subgroups if any
        Node subgroupsNode = XMLUtils.findChild(
                                               groupNode, SUBGROUPS_NODE_NAME);

        if(subgroupsNode == null)
            return;

        NodeList subgroups = subgroupsNode.getChildNodes();

        //recurse for every sub meta group
        for(int i = 0; i < subgroups.getLength(); i++)
        {
            Node currentGroupNode = subgroups.item(i);

            if (currentGroupNode.getNodeType() != Node.ELEMENT_NODE
                || !currentGroupNode.getNodeName().equals(GROUP_NODE_NAME))
                continue;

            try
            {
                processGroupXmlNode(mclServiceImpl
                                    , accountID
                                    , (Element) currentGroupNode
                                    , currentMetaGroup
                                    , protoGroupsMap);
            }
            catch(Throwable throwable)
            {
                //catch everything and bravely continue with remaining groups
                //and contacts
                logger.error("Failed to process group node " + currentGroupNode
                             , throwable);
            }
        }
    }

    /**
     * Returns all proto contacts that are encapsulated inside the meta contact
     * represented by <tt>metaContactNode</tt> and that originate from the
     * account with id - <tt>accountID</tt>. The returned list contains contact
     * contact descriptors as elements. In case the meta cotnact does
     * not contain proto contacts originating from the specified account, an
     * empty list is returned.
     * <p>
     * @param metaContactNode the Element whose proto contacts we'd likde to
     * extract.
     * @param accountID the id of the account whose contacts we're interested in.
     * @param protoGroups a map binding proto group UIDs to protogroups, that
     * the method could use i   n order to fill in the corresponding field in the
     * contact descriptors.
     * @return a java.util.List containing contact descriptors.
     */
    private List extractProtoContacts(Element metaContactNode,
                                      String accountID,
                                      Map protoGroups)
    {
        List protoContacts = new LinkedList();

        NodeList children = metaContactNode.getChildNodes();

        for (int i = 0; i < children.getLength(); i ++)
        {
            Node currentNode = children.item(i);

            if (currentNode.getNodeType() != Node.ELEMENT_NODE
                || !currentNode.getNodeName().equals(PROTO_CONTACT_NODE_NAME))
                continue;

            String contactAccountID = XMLUtils.getAttribute(
                currentNode, ACCOUNT_ID_ATTR_NAME);

            if (!accountID.equals(contactAccountID))
                continue;

            String contactAddress = XMLUtils.getAttribute(
                currentNode, PROTO_CONTACT_ADDRESS_ATTR_NAME);

            String protoGroupUID = XMLUtils.getAttribute(
                                       currentNode, PARENT_PROTO_GROUP_UID_ATTR_NAME);
            Element persistentDataNode = XMLUtils.findChild(
                (Element)currentNode, PERSISTENT_DATA_NODE_NAME);

            String persistentData = XMLUtils.getText(persistentDataNode);

            protoContacts.add( new StoredProtoContactDescriptor(
                contactAddress
                , persistentData
                , (ContactGroup)protoGroups.get(protoGroupUID)));
        }

        return protoContacts;
    }

    /**
     * Creates a node element corresponding to <tt>protoContact</tt>.
     *
     * @param protoContact the Contact whose element we'd like to create
     * @return a XML Element corresponding to <tt>protoContact</tt>.
     */
    private Element createProtoContactNode(Contact protoContact)
    {
        Element protoContactElement = contactListDocument.createElement(
                                            PROTO_CONTACT_NODE_NAME);

        //set attributes
        protoContactElement.setAttribute(PROTO_CONTACT_ADDRESS_ATTR_NAME
                                         , protoContact.getAddress());

        protoContactElement.setAttribute(
            ACCOUNT_ID_ATTR_NAME
            , protoContact.getProtocolProvider().getAccountID().getAccountUniqueID());

        protoContactElement.setAttribute(
            PARENT_PROTO_GROUP_UID_ATTR_NAME
            , protoContact.getParentContactGroup().getUID());

        //append persistent data child node
        Element persDataNode = contactListDocument.createElement(
                PERSISTENT_DATA_NODE_NAME);

        XMLUtils.setText(persDataNode, protoContact.getPersistentData());

        protoContactElement.appendChild(persDataNode);

        return protoContactElement;
    }

    /**
     * Creates a node element corresponding to <tt>protoGroup</tt>.
     *
     * @param protoGroup the ContactGroup whose element we'd like to create
     * @return a XML Element corresponding to <tt>protoGroup</tt>.
     */
    private Element createProtoContactGroupNode(ContactGroup protoGroup)
    {
        Element protoGroupElement = contactListDocument.createElement(
                                            PROTO_GROUP_NODE_NAME);

        //set attributes
        protoGroupElement.setAttribute(UID_ATTR_NAME, protoGroup.getUID());

        protoGroupElement.setAttribute(
            ACCOUNT_ID_ATTR_NAME
            , protoGroup.getProtocolProvider().getAccountID().getAccountUniqueID());

        protoGroupElement.setAttribute(
            PARENT_PROTO_GROUP_UID_ATTR_NAME
            , protoGroup.getParentContactGroup().getUID());

        //append persistent data child node
        Element persDataNode = contactListDocument.createElement(
                PERSISTENT_DATA_NODE_NAME);

        XMLUtils.setText(persDataNode, protoGroup.getPersistentData());

        protoGroupElement.appendChild(persDataNode);

        return protoGroupElement;
    }


    /**
     * Creates a meta contact node element corresponding to <tt>metaContact</tt>.
     *
     *
     * @param metaContact the MetaContact that the new node is about
     * @return the XML Element containing the persistent version of
     * <tt>metaContact</tt>
     */
    private Element createMetaContactNode(MetaContact metaContact)
    {
        Element metaContactElement
            = this.contactListDocument.createElement(META_CONTACT_NODE_NAME);

        metaContactElement.setAttribute(UID_ATTR_NAME
                                        , metaContact.getMetaUID());

        //create the display name node
        Element displayNameNode = contactListDocument.createElement(
                            META_CONTACT_DISPLAY_NAME_NODE_NAME);

        displayNameNode.appendChild(contactListDocument
                                    .createTextNode(
                                        metaContact.getDisplayName()));

        metaContactElement.appendChild(displayNameNode);

        Iterator contacts = metaContact.getContacts();

        while(contacts.hasNext())
        {
            Contact contact = (Contact)contacts.next();
            Element contactElement = createProtoContactNode(contact);
            metaContactElement.appendChild(contactElement);
        }

        return metaContactElement;
    }

    /**
     * Creates a meta contact group node element corresponding to
     * <tt>metaGroup</tt>.
     *
     * @param metaGroup the MetaContactGroup that the new node is about
     * @return the XML Element containing the persistent version of
     * <tt>metaGroup</tt>
     */
    private Element createMetaContactGroupNode(MetaContactGroup metaGroup)
    {
        Element metaGroupElement
            = this.contactListDocument.createElement(GROUP_NODE_NAME);

        metaGroupElement.setAttribute(    GROUP_NAME_ATTR_NAME
                                        , metaGroup.getGroupName());

        metaGroupElement.setAttribute(    UID_ATTR_NAME
                                        , metaGroup.getMetaUID());

        //create and fill the proto groups node
        Element protoGroupsElement
            = this.contactListDocument.createElement(PROTO_GROUPS_NODE_NAME);
        metaGroupElement.appendChild(protoGroupsElement);

        Iterator protoGroups = metaGroup.getContactGroups();

        while (protoGroups.hasNext())
        {
            ContactGroup group = (ContactGroup)protoGroups.next();

            //ignore if the proto group is not persistent:
            if(!group.isPersistent())
                continue;

            Element protoGroupEl = createProtoContactGroupNode(group);
            protoGroupsElement.appendChild(protoGroupEl);
        }

        //create and fill the sub groups node

        Element subgroupsElement
            = this.contactListDocument.createElement(SUBGROUPS_NODE_NAME);
        metaGroupElement.appendChild(subgroupsElement);

        Iterator subroups = metaGroup.getSubgroups();

        while (subroups.hasNext())
        {
            MetaContactGroup subgroup = (MetaContactGroup)subroups.next();
            Element subgroupEl = createMetaContactGroupNode(subgroup);
            subgroupsElement.appendChild(subgroupEl);
        }

        //create and fill child contacts node

        Element childContactsElement
            = this.contactListDocument.createElement(CHILD_CONTACTS_NODE_NAME);
        metaGroupElement.appendChild(childContactsElement);

        Iterator childContacts = metaGroup.getChildContacts();

        while (childContacts.hasNext())
        {
            MetaContact metaContact = (MetaContact)childContacts.next();
            Element metaContactEl = createMetaContactNode(metaContact);
            childContactsElement.appendChild(metaContactEl);
        }

        return metaGroupElement;
    }


    /**
     * Indicates that a MetaContact has been successfully added
     * to the MetaContact list.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactAdded(MetaContactEvent evt)
    {
        Element parentGroupNode = findMetaContactGroupNode(
            evt.getParentGroup().getMetaUID());

        //not sure what to do in case of null. we'll be logging an internal
        //err for now and that's all.
        if (parentGroupNode == null)
        {
            logger.error("Couldn't find parent of a newly added contact: "
                         + evt.getSourceMetaContact());
            return;
        }

        parentGroupNode = XMLUtils.findChild(
            parentGroupNode, CHILD_CONTACTS_NODE_NAME);

        Element metaContactElement
            = createMetaContactNode(evt.getSourceMetaContact());

        parentGroupNode.appendChild(metaContactElement);

        try{
            scheduleContactListStorage();
        }
        catch (IOException ex){
            /**given we're being invoked from an event dispatch thread that was
            proberly triggerred by a net operation - we could not do much.
            so ... log and @todo one day we'll have a global error  dispatcher */
            logger.error("Writing CL failed after adding contact "
                         + evt.getSourceMetaContact(), ex);
        }
    }

    /**
     * Creates XML nodes for the source metacontact group, its child meta
     * contacts and associated protogroups and adds them to the xml contact list.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactGroupAdded(MetaContactGroupEvent evt)
    {
        //if the group was created as an encapsulator of a non persistent proto
        //group then we'll ignore it.
        if (evt.getSourceProtoGroup() != null
            && !evt.getSourceProtoGroup().isPersistent())
            return;

        MetaContactGroup parentGroup = evt.getSourceMetaContactGroup()
            .getParentMetaContactGroup();

        Element parentGroupNode
            = findMetaContactGroupNode(parentGroup.getMetaUID());

        //not sure what to do in case of null. we'll be logging an internal
        //err for now and that's all.
        if (parentGroupNode == null)
        {
            logger.error("Couldn't find parent of a newly added group: "
                         + parentGroup);
            return;
        }

        Element newGroupElement
            = createMetaContactGroupNode(evt.getSourceMetaContactGroup());

        Element subgroupsNode
            = XMLUtils.findChild(parentGroupNode, SUBGROUPS_NODE_NAME);

        subgroupsNode.appendChild(newGroupElement);

        try
        {
            scheduleContactListStorage();
        }
        catch (IOException ex)
        {
            /**given we're being invoked from an event dispatch thread that was
                 proberly triggerred by a net operation - we could not do much.
             so ... log and @todo one day we'll have a global error  dispatcher */
            logger.error("Writing CL failed after adding contact "
                         + evt.getSourceMetaContactGroup(), ex);
        }
    }

    /**
     * Removes the corresponding node from the xml document.
     * @param evt the MetaContactGroupEvent containing the corresponding contact
     */
    public void metaContactGroupRemoved(MetaContactGroupEvent evt)
    {
        Element metaContactGroupNode = findMetaContactGroupNode(
            evt.getSourceMetaContactGroup().getMetaUID());

        //not sure what to do in case of null. we'll be loggin an internal err
        //for now and that's all.
        if(metaContactGroupNode == null)
        {
            logger.error("Save after removing an MN group. Groupt not found: "
                         + evt.getSourceMetaContactGroup());
            return;
        }

        //remove the meta contact node.
        metaContactGroupNode.getParentNode().removeChild(metaContactGroupNode);

        try{
            scheduleContactListStorage();
        }
        catch (IOException ex){
            /**given we're being invoked from an event dispatch thread that was
            probably triggerred by a net operation - we could not do much.
            so ... log and @todo one day we'll have a global error  dispatcher */
            logger.error("Writing CL failed after removing group "
                         + evt.getSourceMetaContactGroup(), ex);
        }
    }


    /**
     * Moves the corresponding node from its old parent to the node
     * corresponding to the new parent meta group.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactMoved(MetaContactMovedEvent evt)
    {
        Element metaContactNode
            = findMetaContactNode(evt.getSourceMetaContact().getMetaUID());
        Element newParentNode
            = findMetaContactGroupNode(evt.getNewParent().getMetaUID());

        //not sure what to do in case of null. we'll be logging an internal err
        //for now and that's all.
        if(metaContactNode == null)
        {
            logger.error("Save after metacontact moved. Contact not found: "
                         + evt.getSourceMetaContact());
            return;
        }
        if(newParentNode == null)
        {
            logger.error("Save after metacontact moved. new parent not found: "
                         + evt.getNewParent());
            return;
        }

        //move the meta contact
        metaContactNode.getParentNode().removeChild(metaContactNode);

        updateParentsForMetaContactNode(metaContactNode, evt.getNewParent());

        Element childContacts = XMLUtils.findChild(  newParentNode
                                                   , CHILD_CONTACTS_NODE_NAME);

        childContacts.appendChild(metaContactNode);

        try{
            scheduleContactListStorage();
        }
        catch (IOException ex){
            /**given we're being invoked from an event dispatch thread that was
            probably triggerred by a net operation - we could not do much.
            so ... log and @todo one day we'll have a global error  dispatcher */
            logger.error("Writing CL failed after moving "
                         + evt.getSourceMetaContact(), ex);
        }

    }

    /**
     * Traverses all contact elements of the metaContactNode argument and
     * updates theirs parent proto group uid-s to point to the first contact
     * group that is encapsulated by the newParent meta group and that belongs
     * to the same account as the contact itself.
     *
     * @param metaContactNode the meta contact node whose child contacts we're
     * to update.
     * @param newParent a reference to the <tt>MetaContactGroup</tt> where
     * metaContactNode was moved.
     */
    private void updateParentsForMetaContactNode(Element metaContactNode,
                                                 MetaContactGroup newParent)
    {
        NodeList children = metaContactNode.getChildNodes();

        for (int i = 0; i < children.getLength(); i++)
        {
            Node currentNode = children.item(i);

            if (currentNode.getNodeType() != Node.ELEMENT_NODE
                || !currentNode.getNodeName().equals(PROTO_CONTACT_NODE_NAME))
                continue;

            Element contactElement = (Element)currentNode;

            String attribute = contactElement
                .getAttribute(PARENT_PROTO_GROUP_UID_ATTR_NAME);

            if( attribute == null || attribute.trim().length() == 0)
                continue;

            String contactAccountID =
                contactElement.getAttribute(ACCOUNT_ID_ATTR_NAME);

            //find the first protogroup originating from the same account as
            //the one that the current contact belongs to.
            Iterator possibleParents = newParent
                .getContactGroupsForAccountID(contactAccountID);

            String newParentUID = ((ContactGroup)possibleParents.next())
                .getUID();

            contactElement.setAttribute(PARENT_PROTO_GROUP_UID_ATTR_NAME
                                        , newParentUID);
        }
    }

    /**
     * Removes the corresponding node from the xml document.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactRemoved(MetaContactEvent evt)
    {
        Element metaContactNode = findMetaContactNode(
            evt.getSourceMetaContact().getMetaUID());

        //not sure what to do in case of null. we'll be loggin an internal err
        //for now and that's all.
        if(metaContactNode == null)
        {
            logger.error("Save after metacontact removed. Contact not found: "
                         + evt.getSourceMetaContact());
            return;
        }

        //remove the meta contact node.
        metaContactNode.getParentNode().removeChild(metaContactNode);

        try{
            scheduleContactListStorage();
        }
        catch (IOException ex){
            /**given we're being invoked from an event dispatch thread that was
            probably triggerred by a net operation - we could not do much.
            so ... log and @todo one day we'll have a global error  dispatcher */
            logger.error("Writing CL failed after removing "
                         + evt.getSourceMetaContact(), ex);
        }
    }

    /**
     * Changes the display name attribute of the specified meta contact node.
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactRenamed(MetaContactRenamedEvent evt)
    {
        Element metaContactNode = findMetaContactNode(
            evt.getSourceMetaContact().getMetaUID());

        //not sure what to do in case of null. we'll be loggin an internal err
        //for now and that's all.
        if(metaContactNode == null)
        {
            logger.error("Save after renam failed. Contact not found: "
                         + evt.getSourceMetaContact());
            return;
        }

        Element displayNameNode = XMLUtils.findChild(
                metaContactNode
                , META_CONTACT_DISPLAY_NAME_NODE_NAME);

        XMLUtils.setText(displayNameNode, evt.getNewDisplayName());

        updatePersistentDataForMetaContact(evt.getSourceMetaContact());

        try{
            scheduleContactListStorage();
        }
        catch (IOException ex){
            /**given we're being invoked from an event dispatch thread that was
            probably triggerred by a net operation - we could not do much.
            so ... log and @todo one day we'll have a global error  dispatcher */
            logger.error("Writing CL failed after rename of "
                         + evt.getSourceMetaContact(), ex);
        }
    }


    /**
     * Removes the corresponding node from the xml contact list.
     *
     * @param evt a reference to the corresponding <tt>ProtoContactEvent</tt>
     */
    public void protoContactRemoved(ProtoContactEvent evt)
    {
        Element oldMcNode = findMetaContactNode(evt.getOldParent().getMetaUID());

        //not sure what to do in case of null. we'll be logging an internal err
        //for now and that's all.
        if (oldMcNode == null)
        {
            logger.error("Failed to find meta contact (old parent): "
                         + oldMcNode);
            return;
        }

        Element protoNode = XMLUtils.locateElement(
            oldMcNode
            , PROTO_CONTACT_NODE_NAME
            , PROTO_CONTACT_ADDRESS_ATTR_NAME
            , evt.getProtoContact().getAddress());

        protoNode.getParentNode().removeChild(protoNode);

        try
        {
            scheduleContactListStorage();
        }
        catch (IOException ex)
        {
            /**given we're being invoked from an event dispatch thread that was
                 probably triggerred by a net operation - we could not do much.
             so ... log and @todo one day we'll have a global error  dispatcher */
            logger.error("Writing CL failed after removing proto contact "
                         + evt.getProtoContact(), ex);
        }
    }


    /**
     * We simply ignore - we're not interested in this kind of events.
     * @param evt the <tt>MetaContactGroupEvent</tt> containind details of this
     * event.
     */
    public void childContactsReordered(MetaContactGroupEvent evt)
    {
        //ignore - not interested in such kind of events
    }

    /**
     * Determines the exact type of the change and acts accordingly by either
     * updating group name or .
     *
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactGroupModified(MetaContactGroupEvent evt)
    {
        Element mcGroupNode = findMetaContactGroupNode(
            evt.getSourceMetaContactGroup().getMetaUID());

        //not sure what to do in case of null. we'll be logging an internal err
        //for now and that's all.
        if (mcGroupNode == null)
        {
            logger.error("Failed to find meta contact group: "
                         + evt.getSourceMetaContactGroup()  );
            return;
        }

        switch (evt.getEventID())
        {
            case MetaContactGroupEvent.CONTACT_GROUP_REMOVED_FROM_META_GROUP:
                Element protoGroupNode
                    = XMLUtils.locateElement(
                        mcGroupNode
                        , PROTO_GROUP_NODE_NAME
                        , UID_ATTR_NAME, evt.getSourceProtoGroup().getUID());

                //remove the proto group from the node containing all proto
                //groups for the corresponding meta contact group.
                if(protoGroupNode != null)
                    protoGroupNode.getParentNode().removeChild(protoGroupNode);
                else
                    logger.error("Hm ... strange ...");
                break;
            case MetaContactGroupEvent.CONTACT_GROUP_ADDED_TO_META_GROUP:
                Element newProtoGroupNode
                    = createProtoContactGroupNode(evt.getSourceProtoGroup());

                Element protoGroupsNode = XMLUtils.findChild(
                    mcGroupNode, PROTO_GROUPS_NODE_NAME);
                //add the proto group to the node containing all proto
                //groups for the corresponding meta contact group.
                if(protoGroupsNode != null)
                    protoGroupsNode.appendChild(newProtoGroupNode);
                else
                    logger.error("Hm ... strange ...");
                break;
            case MetaContactGroupEvent.META_CONTACT_GROUP_RENAMED:
                mcGroupNode.setAttribute(
                    GROUP_NAME_ATTR_NAME
                    , evt.getSourceMetaContactGroup().getGroupName());
                break;
            case MetaContactGroupEvent.CONTACT_GROUP_RENAMED_IN_META_GROUP:
                //proto group names are not stored so ignore.
                break;
        }

        try
        {
            scheduleContactListStorage();
        }
        catch (IOException ex)
        {
            /**given we're being invoked from an event dispatch thread that was
                 probably triggerred by a net operation - we could not do much.
             so ... log and @todo one day we'll have a global error  dispatcher */
            logger.error("Writing CL failed after removing proto group "
                         + evt.getSourceProtoGroup().getGroupName(), ex);
        }

    }

    /**
     * Indicates that a protocol specific <tt>Contact</tt> instance has been
     * added to the list of protocol specific buddies in this
     * <tt>MetaContact</tt>
     * @param evt a reference to the corresponding
     * <tt>ProtoContactEvent</tt>
     */
    public void protoContactAdded(ProtoContactEvent evt)
    {
        Element mcNode = findMetaContactNode(evt.getParent().getMetaUID());

        //not sure what to do in case of null. we'll be logging an internal err
        //for now and that's all.
        if (mcNode == null)
        {
            logger.error("Failed to find meta contact: "
                         + evt.getParent());
            return;
        }

        Element protoNode = createProtoContactNode(evt.getProtoContact());

        mcNode.appendChild(protoNode);

        try
        {
            scheduleContactListStorage();
        }
        catch (IOException ex)
        {
            /**given we're being invoked from an event dispatch thread that was
                 probably triggerred by a net operation - we could not do much.
             so ... log and @todo one day we'll have a global error  dispatcher */
            logger.error("Writing CL failed after adding proto contact "
                         + evt.getProtoContact(), ex);
        }

    }

    /**
     * Indicates that a protocol specific <tt>Contact</tt> instance has been
     * moved from within one <tt>MetaContact</tt> to another.
     * @param evt a reference to the <tt>ProtoContactMovedEvent</tt> instance.
     */
    public void protoContactMoved(ProtoContactEvent evt)
    {
        Element newMcNode = findMetaContactNode(evt.getNewParent().getMetaUID());
        Element oldMcNode = findMetaContactNode(evt.getOldParent().getMetaUID());

        //not sure what to do in case of null. we'll be logging an internal err
        //for now and that's all.
        if (oldMcNode == null)
        {
            logger.error("Failed to find meta contact (old parent): "
                         + oldMcNode);
            return;
        }

        if (newMcNode == null)
        {
            logger.error("Failed to find meta contact (old parent): "
                         + newMcNode);
            return;
        }

        Element protoNode = XMLUtils.locateElement(
            oldMcNode
            , PROTO_CONTACT_NODE_NAME
            , PROTO_CONTACT_ADDRESS_ATTR_NAME
            , evt.getProtoContact().getAddress());

        protoNode.getParentNode().removeChild(protoNode);

        //update parent attr and append the contact to its new parent node.
        protoNode.setAttribute(PARENT_PROTO_GROUP_UID_ATTR_NAME
                              , evt.getProtoContact()
                                    .getParentContactGroup().getUID());
        newMcNode.appendChild(protoNode);

        try
        {
            scheduleContactListStorage();
        }
        catch (IOException ex)
        {
            /**given we're being invoked from an event dispatch thread that was
                 probably triggerred by a net operation - we could not do much.
             so ... log and @todo one day we'll have a global error  dispatcher */
            logger.error("Writing CL failed after moving proto contact "
                         + evt.getProtoContact(), ex);
        }
    }

    /**
     * Returns the node corresponding to the meta contact with the specified
     * uid or null if no such node was found.
     * @param metaContactUID the UID String of the meta contact whose node we
     * are looking for.
     * @return the node corresponding to the meta contact with the specified
     * UID or null if no such contact was found in the meta contact
     * list file.
     */
    private Element findMetaContactNode(String metaContactUID)
    {
        Element root = (Element)contactListDocument.getFirstChild();

        return XMLUtils.locateElement(  root
                                      , META_CONTACT_NODE_NAME
                                      , UID_ATTR_NAME
                                      , metaContactUID);
    }

    /**
     * Returns the node corresponding to the meta contact with the specified
     * uid or null if no such node was found.
     * @param metaContactGroupUID the UID String of the meta contact whose node
     * we are looking for.
     * @return the node corresponding to the meta contact group with the
     * specified UID or null if no such group was found in the meta contact
     * list file.
     */
    private Element findMetaContactGroupNode(String metaContactGroupUID)
    {
        Element root = (Element)contactListDocument.getFirstChild();

        return XMLUtils.locateElement(  root
                                      , GROUP_NODE_NAME
                                      , UID_ATTR_NAME
                                      , metaContactGroupUID);
    }

    /**
     * Removes the file where we store contact lists.
     */
    void removeContactListFile()
    {
        this.contactlistFile.delete();
    }

    /**
     * Contains details parsed out of the contact list xml file, necessary
     * for creating unresolved contacts.
     */
    class StoredProtoContactDescriptor
    {
        String contactAddress = null;
        String persistentData = null;
        ContactGroup parentProtoGroup = null;

        StoredProtoContactDescriptor( String contactAddress,
                                      String persistentData,
                                      ContactGroup parentProtoGroup)
        {
            this.contactAddress = contactAddress;
            this.persistentData = persistentData;
            this.parentProtoGroup = parentProtoGroup;
        }
    }
}
