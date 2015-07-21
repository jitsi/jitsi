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
package net.java.sip.communicator.slick.protocol.gibberish;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 * Installs test accounts and verifies whether they are available after
 * installation.
 *
 * @author Emil Ivov
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
                            + "=Gibberish)";
        try{
            serRefs = GibberishSlickFixture.bc.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            //this really shouldhn't occur as the filter expression is static.
            fail(osgiFilter + " is not a valid osgi filter");
        }

        assertTrue(
            "Failed to find a provider factory service for protocol Gibberish",
            serRefs != null && serRefs.length >  0);

        //Keep the reference for later usage.
        ProtocolProviderFactory gibberishProviderFactory = (ProtocolProviderFactory)
            GibberishSlickFixture.bc.getService(serRefs[0]);

        //make sure the account is empty
        assertTrue("There was an account registered with the account mananger "
                   +"before we've installed any",
                   gibberishProviderFactory.getRegisteredAccounts().size()== 0);


        //Prepare the properties of the first gibberish account.

        Hashtable<String, String> gibberishAccount1Properties = getAccountProperties(
            GibberishProtocolProviderServiceLick.ACCOUNT_1_PREFIX);
        Hashtable<String, String> gibberishAccount2Properties = getAccountProperties(
            GibberishProtocolProviderServiceLick.ACCOUNT_2_PREFIX);

        //try to install an account with a null account id
        try{
            gibberishProviderFactory.installAccount(
                null, gibberishAccount1Properties);
            fail("installing an account with a null account id must result "
                 +"in a NullPointerException");
        }catch(NullPointerException exc)
        {
            //that's what had to happen
        }

        //now really install the accounts
        gibberishProviderFactory.installAccount(
            gibberishAccount1Properties.get(ProtocolProviderFactory.USER_ID)
            , gibberishAccount1Properties);
        gibberishProviderFactory.installAccount(
            gibberishAccount2Properties.get(ProtocolProviderFactory.USER_ID)
            , gibberishAccount2Properties);


        //try to install one of the accounts one more time and verify that an
        //excepion is thrown.
        try{
            gibberishProviderFactory.installAccount(
                gibberishAccount1Properties.get(ProtocolProviderFactory.USER_ID)
                , gibberishAccount1Properties);

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
            gibberishProviderFactory.getRegisteredAccounts().size() == 2);

        //Verify protocol providers corresponding to the new account have
        //been properly registered with the osgi framework.

        osgiFilter =
            "(&("+ProtocolProviderFactory.PROTOCOL +"=Gibberish)"
             +"(" + ProtocolProviderFactory.USER_ID
             + "=" + gibberishAccount1Properties.get(
                            ProtocolProviderFactory.USER_ID)
             + "))";

        try
        {
            serRefs = GibberishSlickFixture.bc.getServiceReferences(
                    ProtocolProviderService.class.getName(),
                    osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            //this really shouldhn't occur as the filter expression is static.
            fail(osgiFilter + "is not a valid osgi filter");
        }

        assertTrue("An protocol provider was apparently not installed as "
                + "requested."
                , serRefs != null && serRefs.length > 0);

        Object gibberishProtocolProvider
            = GibberishSlickFixture.bc.getService(serRefs[0]);

        assertTrue("The installed protocol provider does not implement "
                  + "the protocol provider service."
                  ,gibberishProtocolProvider instanceof ProtocolProviderService);
    }

    /**
     * Returns all properties necessary for the intialization of the account
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
            +" has to tontain a valid gibberish address that could be used "
            +"during SIP Communicator's tests."
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

        String serverAddress = System.getProperty(
                    accountPrefix + ProtocolProviderFactory.SERVER_ADDRESS, null);

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
