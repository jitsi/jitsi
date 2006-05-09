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

import org.w3c.dom.*;
import org.xml.sax.*;
import org.osgi.framework.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.xml.*;
import net.java.sip.communicator.service.fileaccess.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The class handles read / write operations over the file where a persistent
 * copy of the meta contact list is stored.
 * <p>
 * The load / resolve strategy that we use when storing contact lists is
 * roughly the following:
 * <p>
 * 1) The MetaContactListService is started. <br>
 * 2) If no file exists for the meta contact list, create one.
 * 2) We fill in the Contact list from what we have in the local file, by first
 *    creating empty <tt>MetaContact</tt> instances (with no proto
 *    <tt>Contact</tt>).<br>
 * 3) paralelno s loadvaneto se syzdava index koito mapva
 * contactaddress:accountid na meta contact<br>
 * 4) We receive an OSGI event telling us that a new ProtocolProviderService
 *    or we simply retrieve one that was in the bundle context, before us. This
 *    This gives us two possible cases:<br>
 * A) The newly registered PP supports persistent presence (has
 *    OperationSetPersistentPresence).<br>
 *
 *    - In this case all <tt>Contact</tt> instances retrieved from the protocol
 *      provider are first being matched to all existing meta contacts, looking
 *      for one that already contained the contact in question and we add it
 *      into it. If the contact is a new on, we simply create a new MetaContact
 *      to encapsulate it.<br>
 * B) The newly registered PP does NOT support persistent presence.
 *    - If this is the case, then we simply go through all contacts that have
 *      been registered for this provider and create subscriptions for every
 *      one of them.<br>
 * <p>
 * After completing the above procedure we would have two kinds of contacts that
 * remain unresolved.<br>
 * 1) contacts whose corresponding provider was not found.<br>
 * 2) Those who first  originated out of a persistent operation set but were not
 *    resolved during this run. <br>
 * <p>
 * @todo We should most probably delete the first and ask the user what to do
 * with the second type.
 *
 * @author Emil Ivov
 */
public class MclStorageManager
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
     * The name of the system property that stores the name of the contact list
     * file.
     */
    private static final String DEFAULT_FILE_NAME = "contactlist.xml";

    /**
     * The name of the node that represents the contact list root.
     */
    private static String META_CONTACT_LIST_ROOT_NAME = "meta-contactlist";

    /**
     * The XML Document containing the contact list file.
     */
    private Document contactListDocument = null;

    /**
     * A reference to the file containing the locally stored meta contact list.
     */
    private File contactlistFile = null;

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
     * Initializes the storage manager and makes it do initial load and parsing
     * of the contact list file.
     *
     * @param bc a reference to the currently valid OSGI <tt>BundleContext</tt>
     *
     * @throws IOException if the contact lsit file specified file does not
     * exist and could not be created.
     * @throws XMLException if there is a problem with the file syntax.
     */
    void start(BundleContext bc) throws IOException, XMLException
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
            fileName = DEFAULT_FILE_NAME;

        //get a reference to the contact list file.
        try{
            contactlistFile  = faService.getPrivatePersistentFile(fileName);
        }
        catch (Exception ex)
        {
            /** @todo throw an exception */
            logger.error("Failed to get a reference to the contact list file."
                         , ex);
            return;
        }

        try
        {
            //load the contact list
            DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            contactListDocument = builder.parse(contactlistFile);
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

    }

    /**
     * Parses the contact list
     * file and calls corresponding "add" methods
     * belonging to <tt>mclServiceImpl</tt> for every meta contact and meta
     * contact group stored in the (conteactlist.xml) file that correspond to a
     * provider carying the specified <tt>accountID</tt>.
     *
     * @param mclServiceImpl a reference to the currently valid instance of the
     * <tt>MetaContactListServiceImpl</tt> that we could use to pass parsed
     * contacts and contact groups.
     * @param accountID the identifier of the account whose contacts we're
     * interested in.
     */
    void extractContactsForAccount(MetaContactListServiceImpl mclServiceImpl,
                                    String accountID)
    {
        if(!isStarted())
            return;

        Node root = contactListDocument.getFirstChild();
        Element metaContactListRoot = XMLUtils.findChild( (Element) root
            , META_CONTACT_LIST_ROOT_NAME);

        NodeList children = metaContactListRoot.getChildNodes();

        for (int i = 0; i < children.getLength(); i++)
        {
            Node currentNode = children.item(i);

            if (currentNode.getNodeType() != Node.ELEMENT_NODE
                || !currentNode.getNodeName().equals( GROUP_NODE_NAME))
                continue;

            //parse the group node and extract all its child groups and contacts
            processGroupXmlNode(mclServiceImpl, accountID, (Element)currentNode
                                , mclServiceImpl.getRoot(), null);

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
        //first resolve the group itself.
        String groupMetaUID = XMLUtils.getAttribute(
             groupNode, GROUP_UID_ATTR_NAME);
        String groupDisplayName = XMLUtils.getAttribute(
            groupNode, GROUP_NAME_ATTR_NAME);

        //create the meta group
        MetaContactGroupImpl currentMetaGroup = mclServiceImpl
            .loadStoredMetaContactGroup(   parentGroup
                                         , groupMetaUID
                                         , groupDisplayName);

        //extract and load one by one all proto groups in this meta group.
        Node protoGroupsNode = XMLUtils.findChild(
                                        groupNode, PROTO_GROUPS_NODE_NAME);

        NodeList protoGroups = protoGroupsNode.getChildNodes();

        //store all found proto groups in order to pass them as parent references
        //to any subgroups.
        Map protoGroupsMap = new Hashtable();

        for (int i = 0; i < protoGroups.getLength(); i++)
        {
            Node currentProtoGroupNode = protoGroups.item(i);

            if (currentProtoGroupNode.getNodeType() != Node.ELEMENT_NODE)
                continue;

            String groupAccountID = XMLUtils.getAttribute(
                currentProtoGroupNode, ACCOUNT_ID_ATTR_NAME);

            if(!accountID.equals(groupAccountID))
                continue;

            String protoGroupUID = XMLUtils.getAttribute(
                currentProtoGroupNode, UID_ATTR_NAME);

            String parentProtoGroupUID = XMLUtils.getAttribute(
                currentProtoGroupNode, PARENT_PROTO_GROUP_UID_ATTR_NAME);

            Element persistentDataNode = XMLUtils.findChild(
                                                (Element)currentProtoGroupNode
                                                , PERSISTENT_DATA_NODE_NAME);

            String persistentData = "";

            if(persistentDataNode != null){
                persistentData = XMLUtils.getText(persistentDataNode);
            }

            //try to find the parent proto group for the one we're currently
            //parsing.
            ContactGroup parentProtoGroup = null;
            if(parentProtoGroups != null && parentProtoGroups.size() > 0)
                parentProtoGroup = (ContactGroup)parentProtoGroups
                                                    .get(parentProtoGroupUID);

            //create the proto group
            ContactGroup newProtoGroup = mclServiceImpl.loadStoredContactGroup
                ( currentMetaGroup, protoGroupUID, parentProtoGroup
                  , persistentData, accountID);

            protoGroupsMap.put(protoGroupUID, newProtoGroup);
        }

        if ( protoGroupsMap.size() == 0)
            return;

        //we have parsed groups now go over the children
        Node childContactsNode = XMLUtils.findChild(
                                        groupNode, CHILD_CONTACTS_NODE_NAME);

        NodeList childContacts = childContactsNode.getChildNodes();

        //go over every meta contact, extract its details and its encapsulated
        //proto contacts
        for(int i = 0; i < childContacts.getLength(); i++)
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

            //pass the parsed proto contacts to the mcl service
            mclServiceImpl.loadStoredMetaContact(
                currentMetaGroup, uid, displayName, protoContacts, accountID);
        }

        //now, last thing that's left to do - go over all subgroups if any
        Node subgroupsNode = XMLUtils.findChild(
                                               groupNode, SUBGROUPS_NODE_NAME);

        if(subgroupsNode == null)
            return;

        //recurse for every sub meta group
        for(int i = 0; i < childContacts.getLength(); i++)
        {
            Node currentGroupNode = childContacts.item(i);

            if (currentGroupNode.getNodeType() != Node.ELEMENT_NODE
                || !currentGroupNode.getNodeName().equals(GROUP_NODE_NAME))
                continue;

            processGroupXmlNode(mclServiceImpl
                                , accountID
                                , (Element)currentGroupNode
                                , parentGroup
                                , protoGroupsMap);
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
     * the method could use in order to fill in the corresponding field in the
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
