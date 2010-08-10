/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.sip;

import junit.framework.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.xcapcaps.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.resourcelists.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.prescontent.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror.*;
import org.w3c.dom.*;

import javax.xml.namespace.*;

/**
 * Contains tests of parsing xcap-caps, resource-lists, pres-content.
 *
 * @author Grigorii Balutsel
 */
public class TestXCapParse extends TestCase
{
    /**
     * The resource-xml for the tests.
     */
    private static String RESOURCE_LISTS_XML =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
      "<resource-lists xmlns=\"urn:ietf:params:xml:ns:resource-lists\"" +
      "                xmlns:ext=\"extension\"" +
      "                xmlns:xsi=\"htt//www.w3.org/2001/XMLSchema-instance\">" +
      "                xsi:schemaLocation=\"resource-lists.xsd \">" +
      "  <list name=\"list1\" ext:name=\"ext list1\">" +
      "    <entry uri=\"sip:entry1@example.com\"" +
      "           ext:uri=\"sip:user1@example.com\">" +
      "      <display-name>Entry1</display-name>" +
      "      <ext:display-name>Ext:Entry1</ext:display-name>" +
      "    </entry>" +
      "    <entry uri=\"sip:entry2@example.com\">" +
      "      <display-name xml:lang=\"en-US\">Entry2</display-name>" +
      "    </entry>" +
      "    <list name=\"sub_group1\"/>" +
      "    <external anchor=\"anchor_uri\">" +
      "      <display-name xml:lang=\"en-US\">External</display-name>" +
      "    </external>" +
      "    <entry-ref ref=\"ref_uri\">" +
      "      <display-name xml:lang=\"en-US\">Entry1</display-name>" +
      "    </entry-ref>" +
      "    <ext:entry uri=\"sip:user1@example.com\" " +
      "               ext:uri=\"sip:user1@example.com\"/>" +
      "  </list>" +
      "  <list name=\"list2\">" +
      "    <display-name xml:lang=\"en-US\">List2</display-name>" +
      "  </list>" +
      "</resource-lists>";

    /**
     * The resource-xml for the tests.
     */
    private static String XCAP_CAPS_XML =
       "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
       "<xcap-caps xmlns=\"urn:ietf:params:xml:ns:xcap-caps\"" +
       "           xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
       "           xsi:schemaLocation=\"xcap-caps.xsd \">" +
       "  <auids>" +
       "    <auid>resource-lists</auid>" +
       "    <auid>rls-services</auid>" +
       "  </auids>" +
       "  <extensions>" +
       "    <!-- No extensions defined -->" +
       "  </extensions>" +
       "  <namespaces>" +
       "    <namespace>urn:ietf:params:xml:ns:xcap-caps</namespace>" +
       "    <namespace>urn:ietf:params:xml:ns:xcap-error</namespace>" +
       "    <namespace>urn:ietf:params:xml:ns:resource-lists</namespace>" +
       "  </namespaces>" +
       "</xcap-caps>";

    /**
     * The pres-content for the tests.
     */
    private static String PRES_CONTENT_XML =
       "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
       "<content xmlns=\"urn:oma:xml:prs:pres-content\">" +
       "  <mime-type>image/png</mime-type>" +
       "  <encoding>base64</encoding>" +
       "  <description>Description</description>" +
       "  <description xml:lang=\"en-US\">Description</description>" +
       "  <data>Data</data>" +
       "</content>";

    /**
     * The xcap-error uniqueness-failure for the tests.
     */
    private static String XCAP_ERROR_CANNOT_DELETE_XML =
       "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
       "<xcap-error xmlns=\"urn:ietf:params:xml:ns:xcap-error\">" +
       "  <cannot-delete phrase=\"Cannot Delete\"/>" +
       "</xcap-error>";

    /**
     * The xcap-error uniqueness-failure for the tests.
     */
    private static String XCAP_ERROR_UNIQUENESS_FAILURE_XML =
       "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
       "<xcap-error xmlns=\"urn:ietf:params:xml:ns:xcap-error\">" +
       "  <uniqueness-failure>" +
       "    <exists field=\"field\">" +
       "      <alt-value>sip:entry@example.com</alt-value>" +
       "  </exists>" +
       "  </uniqueness-failure>" +
       "</xcap-error>";

    /**
     * Creates a test suite containing tests of this class in a specific order.
     * We'll first execute tests beginning with the "test" prefix and then go to
     * ordered tests. We first execute tests for reading info, then writing.
     * Then the ordered tests - error handling and finaly for removing details
     *
     * @return Test a testsuite containing all tests to execute.
     */
    public static Test suite()
    {
        return new TestSuite(TestXCapParse.class);
    }

    /**
     * Tests resource-lists parsing.
     *
     * @throws Exception if there is some error during test.
     */
    public static void testResourceListsParse() throws Exception
    {
        ResourceListsType originalResourceLists =
                ResourceListsParser.fromXml(RESOURCE_LISTS_XML);
        validateResourceLists(originalResourceLists);
        String xml = ResourceListsParser.toXml(originalResourceLists);
        ResourceListsType storedResourceLists =
                ResourceListsParser.fromXml(xml);
        validateResourceLists(storedResourceLists);
    }

    /**
     * Tests xcap-caps parsing.
     *
     * @throws Exception if there is some error during test.
     */
    public static void testXCapCapsParse() throws Exception
    {
        XCapCapsType originalXCapCaps =
                XCapCapsParser.fromXml(XCAP_CAPS_XML);
        validateXCapCaps(originalXCapCaps);
    }

    /**
     * Tests pres-content parsing.
     *
     * @throws Exception if there is some error during test.
     */
    public static void testPresContentParse() throws Exception
    {
        ContentType originalContent =
                PresContentParser.fromXml(PRES_CONTENT_XML);
        validatePresContent(originalContent);
        String xml = PresContentParser.toXml(originalContent);
        ContentType storedContent = PresContentParser.fromXml(xml);
        validatePresContent(storedContent);
    }

    /**
     * Tests xcap-error parsing.
     *
     * @throws Exception if there is some error during test.
     */
    public static void testXCapErrorParse() throws Exception
    {
        XCapErrorType cannotDelete =
                XCapErrorParser.fromXml(XCAP_ERROR_CANNOT_DELETE_XML);
        validateXCapErrorConnotDelete(cannotDelete);

        XCapErrorType uniquenessFailure =
                XCapErrorParser.fromXml(XCAP_ERROR_UNIQUENESS_FAILURE_XML);
        validateXCapErrorUniquenessFailure(uniquenessFailure);
    }

    /**
     * Validates uniqueness-failure with the original XML.
     *
     * @param xCapError the xcap-error to analyze.
     */
    private static void validateXCapErrorUniquenessFailure(
            XCapErrorType xCapError)
    {
        assertNotNull("xcap-error cannot be null", xCapError);
        assertNotNull("uniqueness-failure cannot be null",
                xCapError.getError());
        assertTrue(
                "The uniqueness-failure elements we set is not read properly",
                xCapError.getError() instanceof UniquenessFailureType);
        UniquenessFailureType uniquenessFailure = (UniquenessFailureType)
                xCapError.getError();
        assertNull("The phrase we set is not read properly",
                uniquenessFailure.getPhrase());
        assertTrue("The exists elements we set is not read properly",
                uniquenessFailure.getExists().size() == 1);
        UniquenessFailureType.ExistsType exists1 =
                uniquenessFailure.getExists().get(0);
        assertEquals("The exists[0] element we set is not read properly",
                exists1.getField(), "field");
        assertTrue("The exists[0]altValue we set is not read properly",
                exists1.getAltValue().size() == 1);
        assertEquals("The exists[0]altValue[0] we set is not read properly",
                exists1.getAltValue().get(0), "sip:entry@example.com");
    }

    /**
     * Validates cannot-delete with the original XML.
     *
     * @param xCapError the xcap-error to analyze.
     */
    private static void validateXCapErrorConnotDelete(
            XCapErrorType xCapError)
    {
        assertNotNull("xcap-error cannot be null", xCapError);
        assertNotNull("cannot-delete cannot be null",
                xCapError.getError());
        assertTrue(
                "The cannot-delete elements we set is not read properly",
                xCapError.getError() instanceof CannotDeleteType);
        CannotDeleteType cannotDelete = (CannotDeleteType)
                xCapError.getError();
        assertEquals("The phrase we set is not read properly",
                cannotDelete.getPhrase(), "Cannot Delete");
    }

    /**
     * Validates resource-lists with the original XML.
     *
     * @param resourceLists the resource-lists to analyze.
     */
    private static void validateResourceLists(ResourceListsType resourceLists)
    {
        assertNotNull("resource-lists cannot be null", resourceLists);
        assertTrue("The first level lists we set is not read properly",
                resourceLists.getList().size() == 2);
        // list1
        ListType list1 = resourceLists.getList().get(0);
        assertEquals(
                "The lists[1] name we set is not read properly",
                list1.getName(), "list1");
        assertEquals(
                "The lists[1] name we set is not read properly",
                list1.getName(), "list1");
        assertNull("The lists[1] display-name we set is not read properly",
                list1.getDisplayName());
        assertTrue("The lists[1] entries we set is not read properly",
                list1.getEntries().size() == 2);
        assertTrue("The lists[1] lists we set is not read properly",
                list1.getLists().size() == 1);
        assertTrue("The lists[1] externals we set is not read properly",
                list1.getExternals().size() == 1);
        assertTrue("The lists[1] entryRefs we set is not read properly",
                list1.getEntryRefs().size() == 1);
        assertTrue("The lists[1] custom attriutes we set is not read properly",
                list1.getAnyAttributes().size() == 1);
        assertTrue("The lists[1] custom elements we set is not read properly",
                list1.getAnyAttributes().size() == 1);

        String list1ExtAttribute = list1.getAnyAttributes().get(
                new QName("extension", "name", "ext"));
        assertNotNull(
                "The lists[1]ext:display-name attribute we set is not read " +
                        "properly",
                list1ExtAttribute);
        assertEquals(
                "The lists[1]ext:display-name attribute we set is not read " +
                        "properly",
                list1ExtAttribute, "ext list1");

        Element list1ExtElement = list1.getAny().get(0);
        assertEquals(
                "The lists[1]ext:entry attribute we set is not read " +
                        "properly",
                list1ExtElement.getLocalName(), "entry");
        assertEquals(
                "The lists[1]ext:entry attribute we set is not read " +
                        "properly",
                XmlUtils.getNamespaceUri(list1ExtElement), "extension");
        assertEquals(
                "The lists[1]ext:entry attribute we set is not read " +
                        "properly",
                list1ExtElement.getPrefix(), "ext");

        EntryType lis1Entry1 = list1.getEntries().get(0);
        assertNotNull(
                "The lists[1]entry[1] name we set is not read properly",
                lis1Entry1.getUri());
        assertEquals(
                "The lists[1]entry[1] name we set is not read properly",
                lis1Entry1.getUri(), "sip:entry1@example.com");
        assertNotNull(
                "The lists[1]entry[1] display-name we set is not read properly",
                lis1Entry1.getDisplayName());
        assertEquals(
                "The lists[1]entry[1] display-name we set is not read properly",
                lis1Entry1.getDisplayName().getValue(), "Entry1");
        assertNull(
                "The lists[1]entry[1] display-name we set is not read properly",
                lis1Entry1.getDisplayName().getLang());
        String enty1ExtAttribute = lis1Entry1.getAnyAttributes().get(
                new QName("extension", "uri", "ext"));
        assertNotNull(
                "The lists[1]entry[1]ext:uri attribute we set is not read " +
                        "properly",
                enty1ExtAttribute);
        assertEquals(
                "The lists[1]entry[1]ext:uri attribute we set is not read " +
                        "properly",
                enty1ExtAttribute, "sip:user1@example.com");
        Element enty1ExtElement = lis1Entry1.getAny().get(0);
        assertEquals(
                "The lists[1]entry[1]ext:entry element we set is not read " +
                        "properly",
                enty1ExtElement.getLocalName(), "display-name");
        assertEquals(
                "The lists[1]entry[1]ext:dispaly-name element we set is not " +
                        "read properly",
                XmlUtils.getNamespaceUri(enty1ExtElement), "extension");
        assertEquals(
                "The lists[1]entry[1]ext:dispaly-name element we set is not " +
                        "read properly",
                enty1ExtElement.getPrefix(), "ext");
        assertEquals(
                "The lists[1]entry[1]ext:dispaly-name element we set is not " +
                        "read properly",
                enty1ExtElement.getTextContent(), "Ext:Entry1");

        EntryType lis1Entry2 = list1.getEntries().get(1);
        assertNotNull(
                "The lists[1]entry[2] display-name we set is not read properly",
                lis1Entry2.getDisplayName());
        assertNotNull(
                "The lists[1]entry[2] name we set is not read properly",
                lis1Entry2.getUri());
        assertEquals(
                "The lists[1]entry[2] name we set is not read properly",
                lis1Entry2.getUri(), "sip:entry2@example.com");
        assertEquals(
                "The lists[1]entry[2] display-name we set is not read properly",
                lis1Entry2.getDisplayName().getValue(), "Entry2");
        assertEquals(
                "The lists[1]entry[2] display-name we set is not read properly",
                lis1Entry2.getDisplayName().getLang(), "en-US");

        EntryRefType list1EntryRef = list1.getEntryRefs().get(0);
        assertEquals(
                "The lists[1]entryRef[1] name we set is not read properly",
                list1EntryRef.getRef(), "ref_uri");
        assertNotNull(
                "The lists[1]entryRef[1] display-name we set is not read " +
                        "properly",
                list1EntryRef.getDisplayName());
        assertEquals(
                "The llists[1]entryRef[1] display-name we set is not read " +
                        "properly",
                list1EntryRef.getDisplayName().getValue(), "Entry1");
        assertEquals(
                "The lists[1]entryRef[1]display-name we set is not read " +
                        "properly",
                list1EntryRef.getDisplayName().getLang(), "en-US");

        ExternalType list1External = list1.getExternals().get(0);
        assertEquals(
                "The lists[1]external[1] name we set is not read properly",
                list1External.getAnchor(), "anchor_uri");
        assertNotNull(
                "The lists[1]external[1] display-name we set is not read " +
                        "properly",
                list1External.getDisplayName());
        assertEquals(
                "The llists[1]external[1] display-name we set is not read " +
                        "properly",
                list1External.getDisplayName().getValue(), "External");
        assertEquals(
                "The lists[1]external[1]display-name we set is not read " +
                        "properly",
                list1External.getDisplayName().getLang(), "en-US");
        // list2
        ListType list2 = resourceLists.getList().get(1);
        assertEquals(
                "The lists[2] name we set is not read properly",
                list2.getName(), "list2");
        assertNotNull("The lists[2] display-name we set is not read properly",
                list2.getDisplayName());
        assertEquals("The lists[2] display-name we set is not read properly",
                list2.getDisplayName().getValue(), "List2");
        assertEquals("The lists[2] display-name we set is not read properly",
                list2.getDisplayName().getLang(), "en-US");
    }

    /**
     * Validates xcap-caps with the original XML.
     *
     * @param xCapCaps the xcap-caps to analyze.
     */
    private static void validateXCapCaps(XCapCapsType xCapCaps)
    {
        assertNotNull("xcap-caps cannot be null", xCapCaps);

        AuidsType auids = xCapCaps.getAuids();
        NamespacesType namespaces = xCapCaps.getNamespaces();
        ExtensionsType extensions = xCapCaps.getExtensions();

        assertNotNull("The auids we set is not read properly", auids);
        assertTrue("The auids we set is not read properly",
                auids.getAuid().size() == 2);
        assertNotNull("The namespaces we set is not read properly", namespaces);
        assertTrue("The namespaces we set is not read properly",
                namespaces.getNamespace().size() == 3);
        assertNotNull("The auids we set is not read properly", extensions);
        assertTrue("The extensions we set is not read properly",
                extensions.getExtension().size() == 0);
        // auids
        assertEquals(
                "The auids[0] name we set is not read properly",
                auids.getAuid().get(0), "resource-lists");
        assertEquals(
                "The auids[1] name we set is not read properly",
                auids.getAuid().get(1), "rls-services");
        // namespaces
        assertEquals(
                "The namespaces[0] name we set is not read properly",
                namespaces.getNamespace().get(0),
                "urn:ietf:params:xml:ns:xcap-caps");
        assertEquals(
                "The namespaces[1] name we set is not read properly",
                namespaces.getNamespace().get(1),
                "urn:ietf:params:xml:ns:xcap-error");
        assertEquals(
                "The namespaces[2] name we set is not read properly",
                namespaces.getNamespace().get(2),
                "urn:ietf:params:xml:ns:resource-lists");
    }

    /**
     * Validates pres-content with the original XML.
     *
     * @param presContent the pres-content to analyze.
     */
    private static void validatePresContent(ContentType presContent)
    {
        assertNotNull("pres-content cannot be null", presContent);
        DataType data = presContent.getData();
        MimeType mimeType = presContent.getMimeType();
        EncodingType encoding = presContent.getEncoding();
        // data
        assertNotNull("The data we set is not read properly", data);
        assertEquals("The data we set is not read properly",
                data.getValue(), "Data");
        assertTrue("The data custom elements we set is not read properly",
                data.getAnyAttributes().size() == 0);
        // mime-type
        assertNotNull("The mime-type we set is not read properly", mimeType);
        assertEquals("The mime-type we set is not read properly",
                mimeType.getValue(), "image/png");
        assertTrue("The mime-type custom elements we set is not read properly",
                mimeType.getAnyAttributes().size() == 0);
        // encoding
        assertNotNull("The encoding we set is not read properly", encoding);
        assertEquals("The encoding we set is not read properly",
                encoding.getValue(), "base64");
        assertTrue("The encoding custom elements we set is not read properly",
                encoding.getAnyAttributes().size() == 0);
        // description
        assertNotNull("The description we set is not read properly",
                presContent.getDescription());
        assertTrue("The description we set is not read properly",
                presContent.getDescription().size() == 2);
        DescriptionType description1 = presContent.getDescription().get(0);
        assertEquals(
                "The description[0] we set is not read properly",
               description1.getValue(), "Description");
        assertNull(
                "The description[0] display-name we set is not read properly",
                description1.getLang());
        assertTrue(
                "The description[0] custom elements we set is not read properly",
                description1.getAnyAttributes().size() == 0);

        DescriptionType description2 = presContent.getDescription().get(1);
        assertEquals(
                "The description[1] we set is not read properly",
               description2.getValue(), "Description");
       assertEquals(
                "The description[1] we set is not read properly",
               description2.getLang(), "en-US");
        assertTrue(
                "The description[1] custom elements we set is not read properly",
                description2.getAnyAttributes().size() == 0);
    }
}
