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
package net.java.sip.communicator.slick.protocol.jabber;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * Test for account installation.
 *
 * @author Damian Minkov
 * @author Valentin Martinet
 */
public class TestAccountInstallation
    extends TestCase
{
    /**
     * Creates the test with the specified method name.
     * @param name the name of the method to execute.
     */
    public TestAccountInstallation(String name)
    {
        super(name);
    }

    /**
     * JUnit setup method.
     * @throws Exception in case anything goes wrong.
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /**
     * JUnit teardown method.
     * @throws Exception in case anything goes wrong.
     */
    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * Installs an account and verifies whether the installation has gone well.
     */
    public void testInstallAccount()
    {
        // first obtain a reference to the provider factory
        ServiceReference[] serRefs = null;
        String osgiFilter = "(" + ProtocolProviderFactory.PROTOCOL
                            + "="+ProtocolNames.JABBER+")";
        try{
            serRefs = JabberSlickFixture.bc.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            //this really shouldn't occur as the filter expression is static.
            fail(osgiFilter + " is not a valid osgi filter");
        }

        assertTrue(
            "Failed to find a provider factory service for protocol Jabber",
            serRefs != null && serRefs.length >  0);

        // Enable always trust mode for testing tls jabber connections
        ServiceReference confReference
            = JabberSlickFixture.bc.getServiceReference(
                ConfigurationService.class.getName());
        ConfigurationService configurationService
            = (ConfigurationService) JabberSlickFixture.
                bc.getService(confReference);

        configurationService.setProperty(
            CertificateService.PNAME_ALWAYS_TRUST,
            Boolean.TRUE);

        //Keep the reference for later usage.
        ProtocolProviderFactory jabberProviderFactory = (ProtocolProviderFactory)
            JabberSlickFixture.bc.getService(serRefs[0]);

        //make sure the account is empty
        assertTrue("There was an account registered with the account manager "
                   +"before we've installed any",
                   jabberProviderFactory.getRegisteredAccounts().size() == 0);


        //Prepare the properties of the first jabber account.

        Hashtable<String, String> jabberAccount1Properties
            = getAccountProperties(
                JabberProtocolProviderServiceLick.ACCOUNT_1_PREFIX);
        Hashtable<String, String> jabberAccount2Properties
            = getAccountProperties(
                JabberProtocolProviderServiceLick.ACCOUNT_2_PREFIX);
        Hashtable<String, String> jabberAccount3Properties
            = getAccountProperties(
                JabberProtocolProviderServiceLick.ACCOUNT_3_PREFIX);

        //try to install an account with a null account id
        try{
            jabberProviderFactory.installAccount(
                null, jabberAccount1Properties);
            fail("installing an account with a null account id must result "
                 +"in a NullPointerException");
        }catch(NullPointerException exc)
        {
            //that's what had to happen
        }

        //now really install the accounts
        jabberProviderFactory.installAccount(
            jabberAccount1Properties.get(ProtocolProviderFactory.USER_ID)
            , jabberAccount1Properties);
        jabberProviderFactory.installAccount(
            jabberAccount2Properties.get(ProtocolProviderFactory.USER_ID)
            , jabberAccount2Properties);
        jabberProviderFactory.installAccount(
            jabberAccount3Properties.get(ProtocolProviderFactory.USER_ID)
            , jabberAccount3Properties);


        //try to install one of the accounts one more time and verify that an
        //exception is thrown.
        try{
            jabberProviderFactory.installAccount(
                jabberAccount1Properties.get(ProtocolProviderFactory.USER_ID)
                , jabberAccount1Properties);

            fail("An IllegalStateException must be thrown when trying to "+
                 "install a duplicate account");

        }catch(IllegalStateException exc)
        {
            //that's what supposed to happen.
        }

        //Verify that the provider factory is aware of our installation
        assertTrue(
            "The newly installed account was not in the acc man's "
            +"registered accounts!",
            jabberProviderFactory.getRegisteredAccounts().size() == 3);

        //Verify protocol providers corresponding to the new account have
        //been properly registered with the osgi framework.

        osgiFilter =
            "(&("+ProtocolProviderFactory.PROTOCOL +"="+ProtocolNames.JABBER+")"
             +"(" + ProtocolProviderFactory.USER_ID
             + "=" + jabberAccount1Properties.get(
                            ProtocolProviderFactory.USER_ID)
             + "))";

        try
        {
            serRefs = JabberSlickFixture.bc.getServiceReferences(
                    ProtocolProviderService.class.getName(),
                    osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            //this really shouldn't occur as the filter expression is static.
            fail(osgiFilter + "is not a valid osgi filter");
        }

        assertTrue("An protocol provider was apparently not installed as "
                + "requested."
                , serRefs != null && serRefs.length > 0);

        Object jabberProtocolProvider
            = JabberSlickFixture.bc.getService(serRefs[0]);

        assertTrue("The installed protocol provider does not implement "
                  + "the protocol provider service."
                  ,jabberProtocolProvider instanceof ProtocolProviderService);
    }

    /**
     * Returns all properties necessary for the initialization of the account
     * with <tt>accountPrefix</tt>.
     * @param accountPrefix the prefix contained by all property names for the
     * the account we'd like to initialized
     * @return a Hashtable that can be used when creating the account in a
     * protocol provider factory.
     */
    private Hashtable<String, String> getAccountProperties(String accountPrefix)
    {
        Hashtable<String, String> table = new Hashtable<String, String>();

        String userID = System.getProperty(
            accountPrefix + ProtocolProviderFactory.USER_ID, null);

        assertNotNull(
            "The system property named "
            + accountPrefix + ProtocolProviderFactory.USER_ID
            +" has to contain a valid Jabber address that could be used during "
            +"SIP Communicator's tests."
            , userID);

        table.put(ProtocolProviderFactory.USER_ID, userID);

        String passwd = System.getProperty(
            accountPrefix + ProtocolProviderFactory.PASSWORD, null );

        assertNotNull(
            "The system property named "
            + accountPrefix + ProtocolProviderFactory.PASSWORD
            +" has to contain the password corresponding to the account "
            + "specified in "
            + accountPrefix + ProtocolProviderFactory.USER_ID
            , passwd);

        table.put(ProtocolProviderFactory.PASSWORD, passwd);

        String serverAddress
            = System.getProperty(accountPrefix
                + ProtocolProviderFactory.SERVER_ADDRESS, null);

        // optional
        if(serverAddress != null)
            table.put(ProtocolProviderFactory.SERVER_ADDRESS, serverAddress);

        String serverPort = System.getProperty(
                    accountPrefix + ProtocolProviderFactory.SERVER_PORT, null);

        // optional
        if(serverPort != null)
            table.put(ProtocolProviderFactory.SERVER_PORT, serverPort);

        return table;
    }
}
