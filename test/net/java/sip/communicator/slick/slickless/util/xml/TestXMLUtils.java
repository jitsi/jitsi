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
package net.java.sip.communicator.slick.slickless.util.xml;

import java.io.*;

import javax.xml.parsers.*;
import javax.xml.transform.stream.*;

import junit.framework.*;

import org.jitsi.util.xml.*;
import org.w3c.dom.*;


/**
 * JUnit tests for the TestXMLUtils package.
 *
 * @author Emil Ivov
 */
public class TestXMLUtils extends TestCase
{
        //property1 values
    private static final String property1 = "p1";
    private static final String property1Value =  "p1.value";
    private static final String property1Value2 =  "p1.value.2";
    private static final String property1Path =  "parent.";

    //property2 values
    private static final String systemProperty = "SYSTEM_PROPERTY";
    private static final String systemPropertyValue =  "I AM the SyS guy";
    private static final String systemPropertyValue2 =  "sys guy's new face";
    private static final String systemPropertyPath =  "parent.";

    //added_property values
    private static final String addedProperty = "ADDED_PROPERTY";
    private static final String addedPropertyValue =  "added";
    private static final String addedPropertyValue2 =  "and then re-aded";

    private static final String addedPropertyPath =  "parent.";

    //INNER_PROPERTY values
    private static final String innerProperty = "INNER_PROPERTY";
    private static final String innerPropertyValue =  "I am an insider";
    private static final String innerPropertyValue2 =  "I am a modified inner";
    private static final String innerPropertyPath =  "parent.innerprops.";

    //CDATA_NODE
    private static final String cdataNode = "CDATA_NODE";
    private static final String cdataNodeContent = "Thisis theCDATA nodeCOntent";
    private static final String cdataNodeContent2 = "The return of the CDATA";
    //TEXT_NODE
    private static final String textNode = "TEXT_NODE";
    private static final String textNodeContent = "Thisis the TeXt nodeCOntent";
    private static final String textNodeContent2 = "The text strikes back";

    /** the contents of our properties file.*/
    private static String xmlString =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<sip-communicator>\n" +
        "   <parent>" +"\n"+
        "      <"+property1+" value=\""+property1Value+"\"/>" +"\n"+
        "      <"+systemProperty
                 +" value=\""+systemPropertyValue+"\" system=\"true\"/>" +"\n"+
        "      <innerprops>" +"\n"+
        "          <"+innerProperty+" value=\""+innerPropertyValue+"\"/>" +"\n"+
        "      </innerprops>" +"\n"+
        "   </parent>" +"\n"+
        "   <"+cdataNode+"><![CDATA["+cdataNodeContent+"]]></"+cdataNode+">" +"\n"+
        "   <"+textNode+">"+textNodeContent+"</"+textNode+">" +"\n"+
        "</sip-communicator>\n";

    DocumentBuilderFactory factory  = null;
    DocumentBuilder        builder  = null;
    Document               document = null;
    Node                   rootNode     = null;

    public TestXMLUtils(String testName)
    {
        super(testName);
    }

    /**
     * Create a XML Document that will be used as a fixture in later testing.
     * @throws Exception if sth goes nuts
     */
    @Override
    protected void setUp() throws Exception
    {
        factory =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        InputStream in = new java.io.ByteArrayInputStream(xmlString.getBytes());

        document = builder.parse(in);

        rootNode = document.getFirstChild();

        super.setUp();
    }

    /**
     * Standard JUnit tear down
     * @throws Exception ... don't know when
     */
    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * Tests the find child method over a few nodes of the sample xml string.
     */
    public void testFindChild()
    {
        Element parent = (Element)rootNode;
        String tagName = "parent";

        Element actualReturn = XMLUtils.findChild(parent, tagName);

        //make sure it found the only "parent" child
        assertEquals("parent", actualReturn.getTagName());

        //let's now look for the inneroprs child
        parent = actualReturn;
        tagName = "innerprops";

        actualReturn = XMLUtils.findChild(parent, tagName);
        //make sure it found the innerprops child
        assertEquals("innerprops", actualReturn.getTagName());
    }

    /**
     * Tests the getAttribute method over property1
     */
    public void testGetAttribute()
    {
        Element parent = (Element)rootNode;
        String tagName = "parent";

        Element actualReturn = XMLUtils.findChild(parent, tagName);

        //make sure it found the only "parent" child
        assertEquals("parent", actualReturn.getTagName());

        //let's now look for the inneroprs child
        parent = actualReturn;

        actualReturn = XMLUtils.findChild(parent, property1);
        //make sure it found the innerprops child
        assertEquals(property1, actualReturn.getTagName());

        //make sure it found the innerprops child
        assertEquals(property1Value,
                     XMLUtils.getAttribute(actualReturn, "value"));
    }

    /**
     * Tests getCData over the cdataNode of the sample XML.
     */
    public void testGetSetCData()
    {
        Element parent = (Element)rootNode;

        Element returnedCdataNode = XMLUtils.findChild(parent, cdataNode);

        String actualReturn = XMLUtils.getCData(returnedCdataNode);

        //compare the returned data with the actual.
        assertEquals(cdataNodeContent, actualReturn);

        //set, with a new value
        XMLUtils.setCData(returnedCdataNode, cdataNodeContent2);

        //now get it again and re-assert
        returnedCdataNode = XMLUtils.findChild(parent, cdataNode);
        actualReturn = XMLUtils.getCData(returnedCdataNode);

        //compare the returned data with the actual.
        assertEquals(cdataNodeContent2, actualReturn);

    }

    /**
     * Tests getText over the textNode of the sample XML.
     */
    public void testGetSetText()
    {
        Element parent = (Element)rootNode;

        Element returnedTextNode = XMLUtils.findChild(parent, textNode);

        String actualReturn = XMLUtils.getText(returnedTextNode);

        //compare the returned data with the actual.
        assertEquals(textNodeContent, actualReturn);


        //set, with a new value
        XMLUtils.setCData(returnedTextNode, textNodeContent2);

        //now get it again and re-assert
        returnedTextNode = XMLUtils.findChild(parent, textNode);
        actualReturn = XMLUtils.getCData(returnedTextNode);

        //compare the returned data with the actual.
        assertEquals(textNodeContent2, actualReturn);
    }


    public void testWriteXML() throws Exception
    {
        StringWriter writer = new StringWriter();
        StreamResult streamResult = new StreamResult(writer);
        String doctypeSystem = null;
        String doctypePublic = null;
        XMLUtils.writeXML(document, streamResult, doctypeSystem, doctypePublic);

        String writtenString = writer.toString();

        //now run some of the previous tests to make sure they passe with the
        //newly written string
        xmlString = new StringBuffer(writtenString).toString();
        setUp();
        testFindChild();

        xmlString = new StringBuffer(writtenString).toString();
        setUp();
        testGetAttribute();

        xmlString = new StringBuffer(writtenString).toString();
        setUp();
        testGetSetCData();

        xmlString = new StringBuffer(writtenString).toString();
        setUp();
        testGetSetText();
    }
}
