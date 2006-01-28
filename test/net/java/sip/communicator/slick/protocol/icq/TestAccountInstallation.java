/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.icq;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import java.util.*;
import org.osgi.framework.*;

/**
 * Tests basic account manager functionalitites
 * @author Emil Ivov
 */
public class TestAccountInstallation extends TestCase
{
    AccountManager icqAccountManager  = null;

    public TestAccountInstallation(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();

    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * Installs an account and verifies whether the installation has gone well.
     */
    public void testInstallAccount()
    {
        // first obtain a reference to the account manager
        ServiceReference[] serRefs = null;
        String osgiFilter = "(" + AccountManager.PROTOCOL_PROPERTY_NAME
                            + "="+ProtocolNames.ICQ+")";
        try{
            serRefs = IcqSlickFixture.bc.getServiceReferences(
                    AccountManager.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            //this really shouldhn't occur as the filter expression is static.
            fail(osgiFilter + " is not a valid osgi filter");
        }

        assertTrue(
            "Failed to find an account manager service for protocol ICQ",
            serRefs != null || serRefs.length >  0);

        //Keep the reference for later usage.
        icqAccountManager = (AccountManager)
            IcqSlickFixture.bc.getService(serRefs[0]);

        //make sure the account is empty
        assertTrue("There was an account registered with the account mananger "
                   +"before we've installed any",
                   icqAccountManager.getRegisteredAcounts().size() == 0);


        //Prepare the properties of the icq account.

        String passwd = System.getProperty( IcqProtocolProviderSlick
                                            .TESTED_IMPL_PWD_PROP_NAME, null );
        String uin = System.getProperty( IcqProtocolProviderSlick
                                         .TESTED_IMPL_ACCOUNT_ID_PROP_NAME, null);

        assertNotNull(
            "In the " + IcqProtocolProviderSlick.TESTED_IMPL_ACCOUNT_ID_PROP_NAME
            +" system property, you need to provide a valid icq UIN for the "
            +" slick to use when signing on icq. It's passwd must be set in "

           + IcqProtocolProviderSlick.TESTED_IMPL_PWD_PROP_NAME,
            uin);
        assertNotNull(
            "In the " + IcqProtocolProviderSlick.TESTED_IMPL_PWD_PROP_NAME
            +" system property, you need to provide a password for the "
            + uin +" account.",
            passwd);


        Hashtable icqAccountProperties = new Hashtable();
        icqAccountProperties.put(AccountProperties.PASSWORD, passwd);

        //try to install an account with a null bundle context
        try{
            icqAccountManager.installAccount( null, uin, icqAccountProperties);
            fail("installing an account with a null BundleContext must result "
                 +"in a NullPointerException");
        }catch(NullPointerException exc){
            //that's what had to happen
        }

        //try to install an account with a null account id
        try{
            icqAccountManager.installAccount(
                IcqSlickFixture.bc, null, icqAccountProperties);
            fail("installing an account with a null account id must result "
                 +"in a NullPointerException");
        }catch(NullPointerException exc){
            //that's what had to happen
        }

        //now really install the account
        IcqSlickFixture.icqAccountID = icqAccountManager.installAccount(
            IcqSlickFixture.bc, uin, icqAccountProperties);

        //try to install the account one more time and verify that an excepion
        //is thrown.
        try{
            IcqSlickFixture.icqAccountID = icqAccountManager.installAccount(
                IcqSlickFixture.bc, uin, icqAccountProperties);
            fail("An IllegalStateException must be thrown when trying to "+
                 "install a duplicate account");

        }catch(IllegalStateException exc)
        {
            //that's what supposed to happen.
        }

        //Verify that the account manager is aware of our installation
        assertTrue(
            "The newly installed account was not in the acc man's "
            +"registered accounts!",
            icqAccountManager.getRegisteredAcounts().size() == 1);

        //Verify that the protocol provider corresponding to the new account has
        //been properly registered with the osgi framework.

        osgiFilter =
            "(&("+AccountManager.PROTOCOL_PROPERTY_NAME +"="+ProtocolNames.ICQ+")"
             +"(" + AccountManager.ACCOUNT_ID_PROPERTY_NAME
             + "=" + IcqSlickFixture.icqAccountID.getAccountID() + "))";

        try
        {
            serRefs = IcqSlickFixture.bc.getServiceReferences(
                    ProtocolProviderService.class.getName(),
                    osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            //this really shouldhn't occur as the filter expression is static.
            fail(osgiFilter + "is not a valid osgi filter");
        }

        assertTrue("An ICQ protocol provider was apparently not installed as "
                + "requested."
                , serRefs != null && serRefs.length > 0);

        Object icqProtocolProvider
            = IcqSlickFixture.bc.getService(serRefs[0]);

        assertTrue("The installed protocol provider does not implement "
                  + "the protocol provider service."
                  ,icqProtocolProvider instanceof ProtocolProviderService);
    }
}
