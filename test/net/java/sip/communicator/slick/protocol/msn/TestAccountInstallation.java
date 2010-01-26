/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.msn;

import java.util.*;

import org.osgi.framework.*;
import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;

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
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /**
     * JUnit teardown method.
     * @throws Exception in case anything goes wrong.
     */
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
                            + "="+ProtocolNames.MSN+")";
        try{
            serRefs = MsnSlickFixture.bc.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            //this really shouldhn't occur as the filter expression is static.
            fail(osgiFilter + " is not a valid osgi filter");
        }

        assertTrue(
            "Failed to find a provider factory service for protocol Msn",
            serRefs != null && serRefs.length >  0);

        //Keep the reference for later usage.
        ProtocolProviderFactory msnProviderFactory = (ProtocolProviderFactory)
            MsnSlickFixture.bc.getService(serRefs[0]);

        //make sure the account is empty
        assertTrue("There was an account registered with the account mananger "
                   +"before we've installed any",
                   msnProviderFactory.getRegisteredAccounts().size() == 0);


        //Prepare the properties of the first msn account.

        Hashtable<String, String> msnAccount1Properties = getAccountProperties(
            MsnProtocolProviderServiceLick.ACCOUNT_1_PREFIX);
        Hashtable<String, String> msnAccount2Properties = getAccountProperties(
            MsnProtocolProviderServiceLick.ACCOUNT_2_PREFIX);
        Hashtable<String, String> msnAccount3Properties = getAccountProperties(
                MsnProtocolProviderServiceLick.ACCOUNT_3_PREFIX);

        //try to install an account with a null account id
        try{
            msnProviderFactory.installAccount(
                null, msnAccount1Properties);
            fail("installing an account with a null account id must result "
                 +"in a NullPointerException");
        }catch(NullPointerException exc)
        {
            //that's what had to happen
        }

        //now really install the accounts
        msnProviderFactory.installAccount(
            msnAccount1Properties.get(ProtocolProviderFactory.USER_ID)
            , msnAccount1Properties);
        msnProviderFactory.installAccount(
            msnAccount2Properties.get(ProtocolProviderFactory.USER_ID)
            , msnAccount2Properties);
        msnProviderFactory.installAccount(
            msnAccount3Properties.get(ProtocolProviderFactory.USER_ID)
            , msnAccount3Properties);


        //try to install one of the accounts one more time and verify that an
        //exception is thrown.
        try{
            msnProviderFactory.installAccount(
                msnAccount1Properties.get(ProtocolProviderFactory.USER_ID)
                , msnAccount1Properties);

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
            msnProviderFactory.getRegisteredAccounts().size() == 3);

        //Verify protocol providers corresponding to the new account have
        //been properly registered with the osgi framework.

        osgiFilter =
            "(&("+ProtocolProviderFactory.PROTOCOL +"="+ProtocolNames.MSN+")"
             +"(" + ProtocolProviderFactory.USER_ID
             + "=" + msnAccount1Properties.get(
                            ProtocolProviderFactory.USER_ID)
             + "))";

        try
        {
            serRefs = MsnSlickFixture.bc.getServiceReferences(
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

        Object msnProtocolProvider
            = MsnSlickFixture.bc.getService(serRefs[0]);

        assertTrue("The installed protocol provider does not implement "
                  + "the protocol provider service."
                  ,msnProtocolProvider instanceof ProtocolProviderService);
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
            +" has to tontain a valid msn address that could be used during "
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
