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

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.protocol.*;

import net.java.sip.communicator.service.protocol.sip.*;
import net.java.sip.communicator.util.*;
import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

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
        ServiceUtils.getService(SipSlickFixture.bc, ConfigurationService.class)
            .setProperty(CertificateService.PNAME_ALWAYS_TRUST, true);

        // first obtain a reference to the provider factory
        ServiceReference[] serRefs = null;
        String osgiFilter = "(" + ProtocolProviderFactory.PROTOCOL
                            + "="+ProtocolNames.SIP+")";
        try{
            serRefs = SipSlickFixture.bc.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            //this really shouldhn't occur as the filter expression is static.
            fail(osgiFilter + " is not a valid osgi filter");
        }

        assertTrue(
            "Failed to find a provider factory service for protocol SIP",
            serRefs != null && serRefs.length >  0);

        //Keep the reference for later usage.
        ProtocolProviderFactory sipProviderFactory = (ProtocolProviderFactory)
            SipSlickFixture.bc.getService(serRefs[0]);

        //make sure the account is empty
        assertTrue("There was an account registered with the account mananger "
                   +"before we've installed any",
                   sipProviderFactory.getRegisteredAccounts().size() == 0);


        //Prepare the properties of the first sip account.

        Hashtable<String,String> sipAccount1Properties = getAccountProperties(
            SipProtocolProviderServiceLick.ACCOUNT_1_PREFIX);
        Hashtable<String, String> sipAccount2Properties = getAccountProperties(
            SipProtocolProviderServiceLick.ACCOUNT_2_PREFIX);

        //try to install an account with a null account id
        try{
            sipProviderFactory.installAccount(
                null, sipAccount1Properties);
            fail("installing an account with a null account id must result "
                 +"in a NullPointerException");
        }catch(NullPointerException exc)
        {
            //that's what had to happen
        }

        //now really install the accounts
        sipProviderFactory.installAccount(
            sipAccount1Properties.get(ProtocolProviderFactory.USER_ID)
            , sipAccount1Properties);
        sipProviderFactory.installAccount(
            sipAccount2Properties.get(ProtocolProviderFactory.USER_ID)
            , sipAccount2Properties);

        //try to install one of the accounts one more time and verify that an
        //excepion is thrown.
        try{
            sipProviderFactory.installAccount(
                sipAccount1Properties.get(ProtocolProviderFactory.USER_ID)
                , sipAccount1Properties);

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
            sipProviderFactory.getRegisteredAccounts().size() == 2);

        //Verify protocol providers corresponding to the new account have
        //been properly registered with the osgi framework.
        osgiFilter =
            "(&("+ProtocolProviderFactory.PROTOCOL +"="+ProtocolNames.SIP+")"
             +"(" + ProtocolProviderFactory.USER_ID
             + "=" + sipAccount1Properties.get(
                            ProtocolProviderFactory.USER_ID)
             + "))";

        try
        {
            serRefs = SipSlickFixture.bc.getServiceReferences(
                    ProtocolProviderService.class.getName(),
                    osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            //this really shouldhn't occur as the filter expression is static.
            fail(osgiFilter + "is not a valid osgi filter");
        }

        assertTrue("A SIP protocol provider was apparently not installed as "
                + "requested."
                , serRefs != null && serRefs.length > 0);

        Object icqProtocolProvider
            = SipSlickFixture.bc.getService(serRefs[0]);

        assertTrue("The installed protocol provider does not implement "
                  + "the protocol provider service."
                  ,icqProtocolProvider instanceof ProtocolProviderService);
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
            +" has to tontain a valid SIP address that could be used during "
            +"SIP Communicator's tests."
            , userID);

        table.put(ProtocolProviderFactory.USER_ID, userID);

        String displayName = System.getProperty(
            accountPrefix + ProtocolProviderFactory.DISPLAY_NAME, null);

        assertNotNull(
            "The system property named "
            + accountPrefix + ProtocolProviderFactory.DISPLAY_NAME
            + " has to contain a valid name string that could be used during "
            + "SIP Communicator's tests."
            , displayName);

        table.put(ProtocolProviderFactory.DISPLAY_NAME, displayName);


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
            accountPrefix + ProtocolProviderFactory.SERVER_ADDRESS, null );

        assertNotNull(
            "The system property named "
            + accountPrefix + ProtocolProviderFactory.SERVER_ADDRESS
            +" has to contain a valid server address to use for testing."
            , serverAddress);

        table.put(ProtocolProviderFactory.SERVER_ADDRESS, serverAddress);

        String serverPort = System.getProperty(
            accountPrefix + ProtocolProviderFactory.SERVER_PORT, null );

        if(serverPort != null)
        {
            table.put(ProtocolProviderFactory.SERVER_PORT, serverPort);
        }

        String proxyAddress = System.getProperty(
            accountPrefix + ProtocolProviderFactory.PROXY_ADDRESS, null );

        if(serverPort != null)
        {
            table.put(ProtocolProviderFactory.PROXY_ADDRESS, proxyAddress);

            String proxyPort = System.getProperty(
            accountPrefix + ProtocolProviderFactory.PROXY_PORT, null );

            if(proxyPort != null)
            {
                table.put(ProtocolProviderFactory.PROXY_PORT, proxyPort);
            }
        }

        String xCapServerUri = System.getProperty(accountPrefix +
                SipProtocolProviderServiceLick.XCAP_SERVER_PROPERTY_NAME, null);
        if (xCapServerUri != null)
        {
            table.put(SipAccountID.XCAP_ENABLE,
                    Boolean.TRUE.toString());
            table.put(SipAccountID.OPT_CLIST_USE_SIP_CREDETIALS,
                    Boolean.TRUE.toString());
            table.put(SipAccountID.OPT_CLIST_USER,
                    Boolean.TRUE.toString());
            table.put(SipAccountID.OPT_CLIST_SERVER_URI,
                    xCapServerUri);
        }

        table.put(ProtocolProviderFactory.FORCE_P2P_MODE,
            Boolean.FALSE.toString());

        return table;
    }
}
