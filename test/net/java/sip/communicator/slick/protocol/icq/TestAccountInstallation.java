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
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.util.Logger;

/**
 * Tests basic account manager functionalitites
 * @author Emil Ivov
 * @author Damian Minkov
 */
public class TestAccountInstallation extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestAccountInstallation.class);
    /**
     * The lock that we wait on until registration is finalized.
     */
    private Object registrationLock = new Object();

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

    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(
            new TestAccountInstallation("testRegisterWrongUsername"));
        suite.addTest(
            new TestAccountInstallation("testRegisterWrongPassword"));
        suite.addTest(
            new TestAccountInstallation("testInstallAccount"));

        return suite;
    }

    /**
     * We try to register with wrong uin which must fire event with status
     * AUTHENTICATION_FAILED. As the uin is new (not existing and not registered)
     * we first install this account, then try to register and wait for the
     * supposed event. After all we unregister this account
     */
    public void testRegisterWrongUsername()
    {
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
        AccountManager icqAccountManager =
            (AccountManager)IcqSlickFixture.bc.getService(serRefs[0]);

        //Prepare the properties of the icq account.

        String passwd = System.getProperty( IcqProtocolProviderSlick
                                            .TESTED_IMPL_PWD_PROP_NAME, null );
        String uin = System.getProperty( IcqProtocolProviderSlick
                                         .TESTED_IMPL_ACCOUNT_ID_PROP_NAME, null);
        // make this uin an invalid one
        uin = uin + "1234";


        Hashtable icqAccountProperties = new Hashtable();
        icqAccountProperties.put(AccountProperties.PASSWORD, passwd);

        AccountID icqAccountID = icqAccountManager.installAccount(
            IcqSlickFixture.bc, uin, icqAccountProperties);

        //find the protocol provider service
        ServiceReference[] icqProviderRefs = null;
        try
        {
            icqProviderRefs
                = IcqSlickFixture.bc.getServiceReferences(
                    ProtocolProviderService.class.getName(),
                    "(&"
                    + "(" + AccountManager.PROTOCOL_PROPERTY_NAME + "=" +
                    ProtocolNames.ICQ + ")"
                    + "(" + AccountManager.ACCOUNT_ID_PROPERTY_NAME + "="
                    + uin + ")"
                    + ")");
        }
        catch(InvalidSyntaxException ex1)
        {
        }

        //make sure we found a service
        assertNotNull("No Protocol Provider was found for ICQ UIN:"+ icqAccountID,
                     icqProviderRefs);
        assertTrue("No Protocol Provider was found for ICQ UIN:"+ icqAccountID,
                     icqProviderRefs.length > 0);

        //save the service for other tests to use.
        ServiceReference icqServiceRef = icqProviderRefs[0];
        ProtocolProviderService provider =
            (ProtocolProviderService)IcqSlickFixture.bc.getService(icqServiceRef);


        RegistrationFailedEventCollector regFailedEvtCollector =
            new RegistrationFailedEventCollector();

        logger.debug("install " + regFailedEvtCollector);

        provider.addRegistrationStateChangeListener(regFailedEvtCollector);

        provider.register(null);

        //give it enough time to register. We won't really have to wait all this
        //time since the registration event collector would notify us the moment
        //we get signed on.
        try{
            synchronized(registrationLock){
                logger.debug("Waiting for registration to complete ...");
                registrationLock.wait(40000);
                logger.debug("Registration was completed or we lost patience.");
            }
        }
        catch (InterruptedException ex){
            logger.debug("Interrupted while waiting for registration", ex);
        }
        catch(Throwable t)
        {
            logger.debug("We got thrown out while waiting for registration", t);
        }

        assertTrue(
                    "No registration event notifying of registration has failed. "
                    +"All events were: " + regFailedEvtCollector.collectedNewStates
                    ,regFailedEvtCollector.collectedNewStates
                        .contains(RegistrationState.AUTHENTICATION_FAILED));

        assertEquals(
            "Registration status must be auth failed as we are logging in with wrong uin",
            regFailedEvtCollector.failedCode,
                RegistrationStateChangeEvent.REASON_NON_EXISTING_USER_ID);

        assertNotNull("We must have reason for auth failed",
                      regFailedEvtCollector.failedReason);

        provider.removeRegistrationStateChangeListener(regFailedEvtCollector);

        icqAccountManager.uninstallAccount(icqAccountID);
    }


    /**
     * Will try to register with wrong password and wait for triggered event
     * with status AUTHENTICATION_FAILED.
     * We get the already installed account. Change the password and try to register
     * After all tests we must return the original password so we don't break the other tests
     */
    public void testRegisterWrongPassword()
    {
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
        AccountManager icqAccountManager =
            (AccountManager)IcqSlickFixture.bc.getService(serRefs[0]);

        //Prepare the properties of the icq account.

        String passwd = System.getProperty( IcqProtocolProviderSlick
                                            .TESTED_IMPL_PWD_PROP_NAME, null );
        String uin = System.getProperty( IcqProtocolProviderSlick
                                         .TESTED_IMPL_ACCOUNT_ID_PROP_NAME, null);

        passwd += "1234";

        Hashtable icqAccountProperties = new Hashtable();
        icqAccountProperties.put(AccountProperties.PASSWORD, passwd);

        AccountID icqAccountID = icqAccountManager.installAccount(
            IcqSlickFixture.bc, uin, icqAccountProperties);

        //find the protocol provider service
        ServiceReference[] icqProviderRefs = null;
        try
        {
            icqProviderRefs
                = IcqSlickFixture.bc.getServiceReferences(
                    ProtocolProviderService.class.getName(),
                    "(&"
                    + "(" + AccountManager.PROTOCOL_PROPERTY_NAME + "=" +
                    ProtocolNames.ICQ + ")"
                    + "(" + AccountManager.ACCOUNT_ID_PROPERTY_NAME + "="
                    + icqAccountID.getAccountUserID() + ")"
                    + ")");
        }
        catch(InvalidSyntaxException ex1)
        {
            logger.error("", ex1);
        }

        //make sure we found a service
        assertNotNull("No Protocol Provider was found for ICQ UIN:" + icqAccountID,
                      icqProviderRefs);
        assertTrue("No Protocol Provider was found for ICQ UIN:" + icqAccountID,
                   icqProviderRefs.length > 0);

        //save the service for other tests to use.
        ServiceReference icqServiceRef = icqProviderRefs[0];
        ProtocolProviderService provider = (ProtocolProviderService)IcqSlickFixture.
            bc.getService(icqServiceRef);

        RegistrationFailedEventCollector regFailedEvtCollector =
            new RegistrationFailedEventCollector();

        logger.debug("install " + regFailedEvtCollector);

        provider.addRegistrationStateChangeListener(regFailedEvtCollector);

        provider.register(null);

        //give it enough time to register. We won't really have to wait all this
        //time since the registration event collector would notify us the moment
        //we get signed on.
        try{
            synchronized(registrationLock){
                logger.debug("Waiting for registration to complete ...");
                registrationLock.wait(40000);
                logger.debug("Registration was completed or we lost patience.");
            }
        }
        catch (InterruptedException ex){
            logger.debug("Interrupted while waiting for registration", ex);
        }
        catch(Throwable t)
        {
            logger.debug("We got thrown out while waiting for registration", t);
        }

        assertTrue(
                    "No registration event notifying of registration has failed. "
                    +"All events were: " + regFailedEvtCollector.collectedNewStates
                    ,regFailedEvtCollector.collectedNewStates
                        .contains(RegistrationState.AUTHENTICATION_FAILED));

        assertEquals(
            "Registration status must be auth failed as we are logging in with wrong pass",
            regFailedEvtCollector.failedCode,
            RegistrationStateChangeEvent.REASON_AUTHENTICATION_FAILED);

        assertNotNull("We must have reason for auth failed", regFailedEvtCollector.failedReason);

        provider.removeRegistrationStateChangeListener(regFailedEvtCollector);

        icqAccountManager.uninstallAccount(icqAccountID);
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
                   icqAccountManager.getRegisteredAccounts().size() == 0);


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
            icqAccountManager.getRegisteredAccounts().size() == 1);

        //Verify that the protocol provider corresponding to the new account has
        //been properly registered with the osgi framework.

        osgiFilter =
            "(&("+AccountManager.PROTOCOL_PROPERTY_NAME +"="+ProtocolNames.ICQ+")"
             +"(" + AccountManager.ACCOUNT_ID_PROPERTY_NAME
             + "=" + IcqSlickFixture.icqAccountID.getAccountUserID() + "))";

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

    public class RegistrationFailedEventCollector
        implements RegistrationStateChangeListener
    {
        public List collectedNewStates = new LinkedList();

        public int failedCode;
        public String failedReason = null;

        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            collectedNewStates.add(evt.getNewState());

            if(evt.getNewState().equals( RegistrationState.AUTHENTICATION_FAILED))
            {
                failedCode = evt.getReasonCode();
                failedReason = evt.getReason();

                logger.debug("Our registration failed - " + failedCode + " = " + failedReason);
                synchronized(registrationLock){
                    logger.debug(".");
                    registrationLock.notifyAll();
                    logger.debug(".");
                }
            }
        }
    }
}
