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
package net.java.sip.communicator.slick.protocol.sip;

import javax.xml.namespace.*;

import junit.framework.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.prescontent.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.presrules.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.resourcelists.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.xcapcaps.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror.*;

import org.jitsi.util.xml.*;
import org.w3c.dom.*;

/**
 * Contains tests of parsing xcap-caps, resource-lists, pres-content,
 * pres-rules, xcap-error.
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
     * The pres-rules for the tests.
     */
    private static String PRES_RULES_XML =
       "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
       "<cr:ruleset xmlns=\"urn:ietf:params:xml:ns:pres-rules\"" +
       "            xmlns:pr=\"urn:ietf:params:xml:ns:pres-rules\"" +
       "            xmlns:cr=\"urn:ietf:params:xml:ns:common-policy\">" +
       "  <cr:rule id=\"rule1\">" +
       "    <cr:conditions>" +
       "      <cr:identity>" +
       "        <cr:one id=\"sip:entry@example.com\"/>" +
       "      </cr:identity>" +
       "    </cr:conditions>" +
       "    <cr:actions>" +
       "      <pr:sub-handling>allow</pr:sub-handling>" +
       "    </cr:actions>" +
       "    <cr:transformations>" +
       "      <pr:provide-services>" +
       "        <pr:service-uri-scheme>sip</pr:service-uri-scheme>" +
       "        <pr:service-uri-scheme>mailto</pr:service-uri-scheme>" +
       "      </pr:provide-services>" +
       "      <pr:provide-persons>" +
       "        <pr:all-persons/>" +
       "     </pr:provide-persons>" +
       "     <pr:provide-activities>true</pr:provide-activities>" +
       "     <pr:provide-user-input>bare</pr:provide-user-input>" +
       "     <pr:provide-unknown-attribute" +
       "        ns=\"urn:vendor-specific:foo-namespace\"" +
       "        name=\"foo\">true</pr:provide-unknown-attribute>" +
       "    </cr:transformations>" +
       "  </cr:rule>" +
       "</cr:ruleset>";

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
     * Tests xcap-error parsing.
     *
     * @throws Exception if there is some error during test.
     */
    public static void testPresRulesParse() throws Exception
    {
        RulesetType originalRuleset =
                CommonPolicyParser.fromXml(PRES_RULES_XML);
        validatePresRules(originalRuleset);
        String xml = CommonPolicyParser.toXml(originalRuleset);
        RulesetType storedRuleset = CommonPolicyParser.fromXml(xml);
        validatePresRules(storedRuleset);
    }

    private static void validatePresRules(RulesetType ruleset)
    {
        assertNotNull("pres-rules cannot be null", ruleset);
        assertTrue("The rules we set is not read properly",
                ruleset.getRules().size() == 1);
        RuleType rule = ruleset.getRules().get(0);
        assertNotNull(
                "The rules[0] we set is not read properly",
                rule.getConditions());
        assertNotNull(
                "The rules[0] we set is not read properly",
                rule.getActions());
        assertNotNull(
                "The rules[0] we set is not read properly",
                rule.getTransformations());
        assertEquals(
                "The rules[0] id we set is not read properly",
                rule.getId(), "rule1");
        // conditions
        ConditionsType conditions = rule.getConditions();
        assertTrue(
                "The conditions we set is not read properly",
                conditions.getIdentities().size() == 1);
        assertTrue(
                "The conditions we set is not read properly",
                conditions.getSpheres().size() == 0);
        assertTrue(
                "The conditions we set is not read properly",
                conditions.getValidities().size() == 0);
        IdentityType identity = conditions.getIdentities().get(0);
        assertTrue(
                "The identity we set is not read properly",
                identity.getOneList().size() == 1);
        assertTrue(
                "The identity we set is not read properly",
                identity.getManyList().size() == 0);
        assertTrue(
                "The identity we set is not read properly",
                identity.getAny().size() == 0);
        OneType one = identity.getOneList().get(0);
        assertEquals(
                "The one we set is not read properly",
                one.getId(), "sip:entry@example.com");
        assertNull(
                "The one we set is not read properly",
                one.getAny());

        // actions
        ActionsType actions = rule.getActions();
        assertNotNull(
                "The actions sub-handling we set is not read properly",
                actions.getSubHandling());
        assertEquals(
                "The actions sub-handling we set is not read properly",
                actions.getSubHandling().value(), "allow");
        assertTrue(
                "The actions we set is not read properly",
                actions.getAny().size() == 0);
        // transformations
        TransformationsType transfomations = rule.getTransformations();
        assertNull(
                "The transfomations we set is not read properly",
                transfomations.getDevicePermission());
        assertNotNull(
                "The transfomations we set is not read properly",
                transfomations.getServicePermission());

        // service-permission
        ProvideServicePermissionType servicePermission =
                transfomations.getServicePermission();
        assertNull(
                "The servicePermission we set is not read properly",
                servicePermission.getAllServices());
        assertTrue(
                "The servicePermission we set is not read properly",
                servicePermission.getOccurrences().size() == 0);
        assertTrue(
                "The servicePermission we set is not read properly",
                servicePermission.getClasses().size() == 0);
        assertTrue(
                "The servicePermission we set is not read properly",
                servicePermission.getServiceUriList().size() == 0);
        assertTrue(
                "The servicePermission we set is not read properly",
                servicePermission.getServiceUriSchemeList().size() == 2);
        assertEquals(
                "The getServiceUriSchemeList[0] we set is not read properly",
                servicePermission.getServiceUriSchemeList().get(0).getValue(),
                "sip");
        assertEquals(
                "The getServiceUriSchemeList[1] we set is not read properly",
                servicePermission.getServiceUriSchemeList().get(1).getValue(),
                "mailto");
        assertTrue(
                "The servicePermission we set is not read properly",
                servicePermission.getAny().size() == 0);
        // person-permission
        ProvidePersonPermissionType personPermission =
                transfomations.getPersonPermission();
        assertNotNull(
                "The servicePermission we set is not read properly",
                personPermission.getAllPersons());
        assertTrue(
                "The servicePermission we set is not read properly",
                personPermission.getOccurrences().size() == 0);
        assertTrue(
                "The servicePermission we set is not read properly",
                personPermission.getClasses().size() == 0);
        assertTrue(
                "The personPermission we set is not read properly",
                personPermission.getAny().size() == 0);

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
                XMLUtils.getNamespaceUri(list1ExtElement), "extension");
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
                XMLUtils.getNamespaceUri(enty1ExtElement), "extension");
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
