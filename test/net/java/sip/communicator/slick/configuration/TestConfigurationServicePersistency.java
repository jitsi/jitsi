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
package net.java.sip.communicator.slick.configuration;

import java.io.*;

import javax.xml.parsers.*;

import junit.framework.*;

import org.jitsi.service.configuration.*;
import org.jitsi.util.xml.*;
import org.osgi.framework.*;
import org.w3c.dom.*;

/**
 * Tests ConfiguratioService persistency, which means that it makes the
 * ConfigurationService store and load its properties from a file and checks
 * whether everything is going well.
 *
 * @author Emil Ivov
 */
public class TestConfigurationServicePersistency extends TestCase
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

    /** the contents of our properties file.*/
    private static final String confFileContent =
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
        "</sip-communicator>\n";

    /** the contents of our second properties file that we use to test reload.*/
    private static final String confFileContent2 =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<sip-communicator>\n" +
        "    <parent>" +"\n"+
        "       <"+property1+" value=\""+property1Value2+"\"/>" +"\n"+
        "       <"+systemProperty
                 +" value=\""+systemPropertyValue2+"\" system=\"true\"/>" +"\n"+
        "       <innerprops>" +"\n"+
        "           <"+innerProperty+" value=\""+innerPropertyValue2+"\"/>" +"\n"+
        "       </innerprops>" +"\n"+
        "    </parent>" +"\n"+
        "</sip-communicator>\n";

    private static final String ourConfFileName
        = "test.persistency.sip-communicator.xml";

    private String originalConfFileName = null;

    /** the configuration file itself (created and deleted for every test)*/
    private File confFile = null;

    /**
     * The ConfigurationService that we will be testing.
     */
    private ConfigurationService configurationService = null;

    /**
     * Generic JUnit Constructor.
     * @param name the name of the test
     */
    public TestConfigurationServicePersistency(String name)
    {
        super(name);
        BundleContext context = ConfigurationServiceLick.bc;
        ServiceReference ref = context.getServiceReference(
            ConfigurationService.class.getName());
        configurationService = (ConfigurationService)context.getService(ref);
    }

    /**
     * Generic JUnit setUp method.
     * @throws Exception if anything goes wrong.
     */
    @Override
    protected void setUp() throws Exception
    {
        String confDirLocation
            = System.getProperty(
                    ConfigurationService.PNAME_SC_HOME_DIR_LOCATION,
                    System.getProperty("user.home"));
        String confDirName
            = System.getProperty(
                    ConfigurationService.PNAME_SC_HOME_DIR_NAME,
                    ".sip-communicator");

        confFile
            = new File(
                    confDirLocation + File.separator + confDirName,
                    ourConfFileName);

        configurationService.purgeStoredConfiguration();
        originalConfFileName
            = System.getProperty(
                    ConfigurationService.PNAME_CONFIGURATION_FILE_NAME);
        System.setProperty(
            ConfigurationService.PNAME_CONFIGURATION_FILE_NAME,
            ourConfFileName);

        /*
         * We're going to (attempt to) create a new file so (try to) make sure
         * the parent directory exists.
         */
        confFile.getParentFile().mkdir();
        confFile.createNewFile();

        FileOutputStream out = new FileOutputStream(confFile);
        out.write(confFileContent.getBytes());
        out.flush();
        out.close();

        configurationService.reloadConfiguration();

        super.setUp();
    }

    /**
     * Generic JUnit tearDown method.
     * @throws Exception if anything goes wrong.
     */
    @Override
    protected void tearDown() throws Exception
    {
        //delete the temp conf file
        confFile.delete();

        configurationService.purgeStoredConfiguration();

        //reset the fileNameProperty
        System.setProperty("net.java.sip.communicator.CONFIGURATION_FILE_NAME",
                            originalConfFileName);

        super.tearDown();
    }

    /**
     * Tests whether the load method has properly loaded our conf file during
     * setup.
     */
    public void testLoadConfiguration()
    {
        Object returnedValueObj =
            configurationService.getProperty(property1Path
                                             + property1);
        assertNotNull("configuration not properly loaded", returnedValueObj);
        assertTrue("returned prop is not a String",
                   returnedValueObj instanceof String);
        String returnedValue = returnedValueObj.toString();

        assertEquals("configuration not properly loaded",
                     property1Value, returnedValue);


        returnedValueObj =
            configurationService.getProperty(systemPropertyPath
                                             + systemProperty);
        assertNotNull("configuration not properly loaded", returnedValueObj);
        assertTrue("returned prop is not a String",
                   returnedValueObj instanceof String);

        //check whether this property was resolved in System.properties
        returnedValue = System.getProperty(systemPropertyPath + systemProperty);
        assertNotNull("A system property was not resolved", returnedValue);
        assertEquals("A system property was not resolved",
                     systemPropertyValue, returnedValue);

        returnedValue = returnedValueObj.toString();
        assertEquals("configuration not properly loaded",
                     systemPropertyValue, returnedValue);

        //check whether inner properties are properly loaded
        returnedValueObj =
            configurationService.getProperty(innerPropertyPath
                                             + innerProperty).toString();
        assertNotNull("configuration not properly loaded", returnedValueObj);
        assertTrue("returned prop is not a String",
                   returnedValueObj instanceof String);
        returnedValue = returnedValueObj.toString();
        assertEquals("configuration not properly loaded",
                     innerPropertyValue, returnedValue);
    }

    /**
     * Tests whether a configuration is properly reloaded (i.e. values of
     * the properties are updated to match those present in the file, and
     * any additional properties added since the previous load are clreared).
     *
     * @throws java.lang.Exception if an error occurs during testing.
     */
    public void testReLoadConfiguration() throws Exception
    {
        //set a new property so that we could see whether its deleted after
        //the file contents is reloaded.
        configurationService.setProperty(addedPropertyPath + addedProperty,
                                         addedPropertyValue);

        //write the new file
        FileOutputStream out = new FileOutputStream(confFile);
        out.write(confFileContent2.getBytes());
        out.flush();
        out.close();

        configurationService.reloadConfiguration();

        //check whether normal properties are properly reloaded
        Object returnedValueObj =
            configurationService.getProperty(property1Path
                                             + property1);
        assertNotNull("configuration not properly loaded", returnedValueObj);
        assertTrue("returned prop is not a String",
                   returnedValueObj instanceof String);
        String returnedValue = returnedValueObj.toString();

        assertEquals("configuration not properly reloaded",
                     property1Value2, returnedValue);

        //check whether systemproperties are properly reresolved
        returnedValueObj =
            configurationService.getProperty(systemPropertyPath
                                             + systemProperty);
        assertNotNull("configuration not properly reloaded", returnedValueObj);
        assertTrue("returned prop is not a String",
                   returnedValueObj instanceof String);
        returnedValue = returnedValueObj.toString();
        assertEquals("configuration not properly reloaded",
                     systemPropertyValue2, returnedValue);

        //make sure that the property was re-resolved in System.properties
        returnedValue = System.getProperty(systemPropertyPath + systemProperty);
        assertNotNull("A system property was not resolved", returnedValue);
        assertEquals("A system property was not resolved",
                     systemPropertyValue2, returnedValue);

        //verify that the inner property is also reloaded
        returnedValueObj =
            configurationService.getProperty(innerPropertyPath
                                             + innerProperty);
        assertNotNull("configuration not properly reloaded", returnedValueObj);
        assertTrue("returned prop is not a String",
                   returnedValueObj instanceof String);
        returnedValue = returnedValueObj.toString();
        assertEquals("configuration not properly reloaded",
                     innerPropertyValue2, returnedValue);

        //make sure the property we added in the beginning is not there anymore.
        returnedValueObj =
            configurationService.getProperty(addedPropertyPath
                                             + addedProperty);
        assertNull("reload didn't remove newly added properties",
                      returnedValueObj);
    }

    /**
     * Test whether a configuration is properly stored in the configuration
     * file.
     *
     * @throws java.lang.Exception if an error occurs during testing.
     */
    public void testStoreConfiguration() throws Exception
    {
        //add a new property that will have to be added to the xml conf file.
        configurationService.setProperty(addedPropertyPath + addedProperty,
                                         addedPropertyValue2);

        //then give new values to existing properties
        configurationService.setProperty(property1Path + property1,
                                         property1Value2);
        configurationService.setProperty(systemPropertyPath + systemProperty,
                                         systemPropertyValue2);
        configurationService.setProperty(innerPropertyPath + innerProperty,
                                         innerPropertyValue2);

        configurationService.storeConfiguration();

        //reload the conf
        configurationService.reloadConfiguration();

        //Now reload the file and make sure it containts the updated values.
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(confFile);

        Node root = document.getFirstChild();

        Node parent = XMLUtils.findChild( (Element) root, "parent");

        Node property1Node = XMLUtils.findChild( (Element) parent, property1);

        Node systemPropertyNode =
            XMLUtils.findChild( (Element) parent, systemProperty);

        Node addedPropertyNode =
            XMLUtils.findChild( (Element) parent, addedProperty);

        Node innerpropNode =
            XMLUtils.findChild( (Element) parent, "innerprops");

        Node innerPropertyNode =
            XMLUtils.findChild((Element)innerpropNode, innerProperty);

        String xmlProp1Value = XMLUtils.getAttribute(property1Node, "value");
        String xmlProp2Value =
            XMLUtils.getAttribute(systemPropertyNode, "value");
        String xmlAddedPropertyValue =
            XMLUtils.getAttribute(addedPropertyNode, "value");
        String xmlInnerPropertyValue =
            XMLUtils.getAttribute(innerPropertyNode, "value");


        assertEquals("property1 was incorrectly stored",
            property1Value2, xmlProp1Value);
        assertEquals("System property was incorrectly stored",
            systemPropertyValue2, xmlProp2Value);
        assertEquals("The added property was incorrectly stored",
            addedPropertyValue2, xmlAddedPropertyValue);
        assertEquals("The inner property was incorrectly stored",
            innerPropertyValue2, xmlInnerPropertyValue);
    }
}
