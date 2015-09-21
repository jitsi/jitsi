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
package net.java.sip.communicator.slick.protocol.icq;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Tests basic provider factory functionalitites
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

    ProtocolProviderFactory icqProviderFactory  = null;

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

    public static Test suite()
    {
        TestSuite suite = new TestSuite();

        if (!IcqSlickFixture.onlineTestingDisabled)
        {
            suite.addTest(
                new TestAccountInstallation("testRegisterWrongUsername"));
            suite.addTest(
                new TestAccountInstallation("testRegisterWrongPassword"));
        }

        suite.addTest(
            new TestAccountInstallation("testInstallAccount"));

        return suite;
    }

    /**
     * We try to register with wrong uin which must fire event with status
     * AUTHENTICATION_FAILED. As the uin is new (not existing and not
     * registered)
     * we first install this account, then try to register and wait for the
     * supposed event. After all we unregister this account
     *
     * @throws OperationFailedException if register () fails for a provider.
     */
    public void testRegisterWrongUsername()
        throws OperationFailedException
    {
        ServiceReference[] serRefs = null;
        String osgiFilter = "(" + ProtocolProviderFactory.PROTOCOL
                            + "="+ProtocolNames.ICQ+")";
        try{
            serRefs = IcqSlickFixture.bc.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            //this really shouldhn't occur as the filter expression is static.
            fail(osgiFilter + " is not a valid osgi filter");
        }

        assertTrue(
            "Failed to find a provider factory service for protocol ICQ",
            (serRefs != null) && (serRefs.length >  0));

        //Keep the reference for later usage.
        ProtocolProviderFactory icqProviderFactory =
            (ProtocolProviderFactory)IcqSlickFixture.bc.getService(serRefs[0]);

        //Prepare the properties of the icq account.

        String passwd = System.getProperty( IcqProtocolProviderSlick
                                            .TESTED_IMPL_PWD_PROP_NAME, null );
        String uin = System.getProperty( IcqProtocolProviderSlick
                                         .TESTED_IMPL_USER_ID_PROP_NAME, null);
        // make this uin an invalid one
        uin = uin + "1234";


        Hashtable<String, String> icqAccountProperties = new Hashtable<String, String>();
        icqAccountProperties.put(ProtocolProviderFactory.PASSWORD, passwd);

        AccountID icqAccountID = icqProviderFactory.installAccount(
            uin, icqAccountProperties);

        //find the protocol provider service
        ServiceReference[] icqProviderRefs = null;
        try
        {
            icqProviderRefs
                = IcqSlickFixture.bc.getServiceReferences(
                    ProtocolProviderService.class.getName(),
                    "(&"
                    + "(" + ProtocolProviderFactory.PROTOCOL + "=" +
                    ProtocolNames.ICQ + ")"
                    + "(" + ProtocolProviderFactory.USER_ID + "="
                    + uin + ")"
                    + ")");
        }
        catch(InvalidSyntaxException ex)
        {
            logger.debug("Our filter seems to be messed up.", ex);
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

        provider.register(new SecurityAuthorityImpl(passwd.toCharArray()));

        //give it enough time to register. We won't really have to wait all this
        //time since the registration event collector would notify us the moment
        //we get signed on.
        try{
            synchronized(registrationLock)
            {
                logger.debug("Waiting for registration to complete ...");
                registrationLock.wait(40000);
                logger.debug("Registration was completed or we lost patience.");
            }
        }
        catch (InterruptedException ex)
        {
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
            "Registration status must be auth failed as we are logging in "
            +"with wrong uin",
            regFailedEvtCollector.failedCode,
                RegistrationStateChangeEvent.REASON_NON_EXISTING_USER_ID);

        assertNotNull("We must have reason for auth failed",
                      regFailedEvtCollector.failedReason);

        provider.removeRegistrationStateChangeListener(regFailedEvtCollector);

        icqProviderFactory.uninstallAccount(icqAccountID);
    }


    /**
     * Will try to register with wrong password and wait for triggered event
     * with status AUTHENTICATION_FAILED.
     * We get the already installed account. Change the password and try to
     * register. After all tests we must return the original password so we
     * don't break the other tests
     *
     * @throws OperationFailedException if register fails for a provider.
     */
    public void testRegisterWrongPassword()
        throws OperationFailedException
    {
        ServiceReference[] serRefs = null;
        String osgiFilter = "(" + ProtocolProviderFactory.PROTOCOL
                            + "="+ProtocolNames.ICQ+")";
        try{
            serRefs = IcqSlickFixture.bc.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            //this really shouldhn't occur as the filter expression is static.
            fail(osgiFilter + " is not a valid osgi filter");
        }

        assertTrue(
            "Failed to find a provider factory service for protocol ICQ",
            (serRefs != null) && (serRefs.length >  0));

        //Keep the reference for later usage.
        ProtocolProviderFactory icqProviderFactory =
            (ProtocolProviderFactory)IcqSlickFixture.bc.getService(serRefs[0]);

        //Prepare the properties of the icq account.

        String passwd = System.getProperty( IcqProtocolProviderSlick
                                            .TESTED_IMPL_PWD_PROP_NAME, null );
        String uin = System.getProperty( IcqProtocolProviderSlick
                                         .TESTED_IMPL_USER_ID_PROP_NAME, null);

        passwd = "dam1234";

        Hashtable<String, String> icqAccountProperties = new Hashtable<String, String>();
        icqAccountProperties.put(ProtocolProviderFactory.PASSWORD, passwd);

        AccountID icqAccountID = icqProviderFactory.installAccount(
            uin, icqAccountProperties);

        //find the protocol provider service
        ServiceReference[] icqProviderRefs = null;
        try
        {
            icqProviderRefs
                = IcqSlickFixture.bc.getServiceReferences(
                    ProtocolProviderService.class.getName(),
                    "(&"
                    + "(" + ProtocolProviderFactory.PROTOCOL + "=" +
                    ProtocolNames.ICQ + ")"
                    + "(" + ProtocolProviderFactory.USER_ID + "="
                    + icqAccountID.getUserID() + ")"
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

        provider.register(new SecurityAuthorityImpl(passwd.toCharArray()));

        //give it enough time to register. We won't really have to wait all this
        //time since the registration event collector would notify us the moment
        //we get signed on.
        try{
            synchronized(registrationLock)
            {
                logger.debug("Waiting for registration to complete ...");
                registrationLock.wait(40000);
                logger.debug("Registration was completed or we lost patience.");
            }
        }
        catch (InterruptedException ex)
        {
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

        icqProviderFactory.uninstallAccount(icqAccountID);
    }

    /**
     * Installs an account and verifies whether the installation has gone well.
     */
    public void testInstallAccount()
    {
        // first obtain a reference to the provider factory
        ServiceReference[] serRefs = null;
        String osgiFilter = "(" + ProtocolProviderFactory.PROTOCOL
                            + "="+ProtocolNames.ICQ+")";
        try{
            serRefs = IcqSlickFixture.bc.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            //this really shouldhn't occur as the filter expression is static.
            fail(osgiFilter + " is not a valid osgi filter");
        }

        assertTrue(
            "Failed to find a provider factory service for protocol ICQ",
            (serRefs != null) && (serRefs.length >  0));

        //Keep the reference for later usage.
        icqProviderFactory = (ProtocolProviderFactory)
            IcqSlickFixture.bc.getService(serRefs[0]);

        //make sure the account is empty
        assertTrue("There was an account registered with the account mananger "
                   +"before we've installed any",
                   icqProviderFactory.getRegisteredAccounts().size() == 0);


        //Prepare the properties of the icq account.

        String passwd = System.getProperty( IcqProtocolProviderSlick
                                            .TESTED_IMPL_PWD_PROP_NAME, null );
        String uin = System.getProperty( IcqProtocolProviderSlick
                                         .TESTED_IMPL_USER_ID_PROP_NAME, null);

        assertNotNull(
            "In the " + IcqProtocolProviderSlick.TESTED_IMPL_USER_ID_PROP_NAME
            +" system property, you need to provide a valid icq UIN for the "
            +" slick to use when signing on icq. It's passwd must be set in "

           + IcqProtocolProviderSlick.TESTED_IMPL_PWD_PROP_NAME,
            uin);
        assertNotNull(
            "In the " + IcqProtocolProviderSlick.TESTED_IMPL_PWD_PROP_NAME
            +" system property, you need to provide a password for the "
            + uin +" account.",
            passwd);


        Hashtable<String, String> icqAccountProperties = new Hashtable<String, String>();
        icqAccountProperties.put(ProtocolProviderFactory.PASSWORD, passwd);

        //try to install an account with a null account id
        try{
            icqProviderFactory.installAccount(
                null, icqAccountProperties);
            fail("installing an account with a null account id must result "
                 +"in a NullPointerException");
        }catch(NullPointerException exc)
        {
            //that's what had to happen
        }

        //now really install the account
        IcqSlickFixture.icqAccountID = icqProviderFactory.installAccount(
            uin, icqAccountProperties);

        //try to install the account one more time and verify that an excepion
        //is thrown.
        try{
            IcqSlickFixture.icqAccountID = icqProviderFactory.installAccount(
                uin, icqAccountProperties);
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
            icqProviderFactory.getRegisteredAccounts().size() == 1);

        //Verify that the protocol provider corresponding to the new account has
        //been properly registered with the osgi framework.

        osgiFilter =
            "(&("+ProtocolProviderFactory.PROTOCOL +"="+ProtocolNames.ICQ+")"
             +"(" + ProtocolProviderFactory.USER_ID
             + "=" + IcqSlickFixture.icqAccountID.getUserID() + "))";

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

    /**
     * A blocking registration listener that would allow us to wait until
     * registration is completed.
     */
    public class RegistrationFailedEventCollector
        implements RegistrationStateChangeListener
    {
        public List<RegistrationState> collectedNewStates = new LinkedList<RegistrationState>();

        public int failedCode;
        public String failedReason = null;

        /**
         * Notifies the registration lock once a new
         * RegistrationStateChangeEvent is received.
         * @param evt the RegistrationStateChangeEvent that we'll have to store.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            collectedNewStates.add(evt.getNewState());

            if(evt.getNewState().equals( RegistrationState.AUTHENTICATION_FAILED))
            {
                failedCode = evt.getReasonCode();
                failedReason = evt.getReason();

                logger.debug("Our registration failed - " + failedCode + " = " + failedReason);
                synchronized(registrationLock)
                {
                    logger.debug(".");
                    registrationLock.notifyAll();
                    logger.debug(".");
                }
            }
        }
    }

    /**
     * A very simple straight forward implementation of a security authority
     * that would always return the same password (the one specified upon
     * construction) when asked for credentials.
     */
    public class SecurityAuthorityImpl implements SecurityAuthority
    {
        /**
         * The password to return when asked for credentials
         */
        private char[] passwd = null;

        private boolean isUserNameEditable = false;

        /**
         * Creates an instance of this class that would always return "passwd"
         * when asked for credentials.
         *
         * @param passwd the password that this class should return when
         * asked for credentials.
         */
        public SecurityAuthorityImpl(char[] passwd)
        {
            this.passwd = passwd;
        }

        /**
         * Returns a Credentials object associated with the specified realm.
         * <p>
         * @param realm The realm that the credentials are needed for.
         * @param defaultValues the values to propose the user by default
         * @param reasonCode the reason for which we're obtaining the
         * credentials.
         * @return The credentials associated with the specified realm or null if
         * none could be obtained.
         */
        public UserCredentials obtainCredentials(String realm,
                                                 UserCredentials defaultValues,
                                                 int reasonCode)
        {
            return obtainCredentials(realm, defaultValues);
        }

        /**
         * Returns a Credentials object associated with the specified realm.
         * <p>
         * @param realm The realm that the credentials are needed for.
         * @param defaultValues the values to propose the user by default
         *
         * @return The credentials associated with the specified realm or null if
         * none could be obtained.
         */
        public UserCredentials obtainCredentials(String realm,
                                                 UserCredentials defaultValues)
        {
            defaultValues.setPassword(passwd);
            return defaultValues;
        }

        /**
         * Sets the userNameEditable property, which should indicate if the
         * user name could be changed by user or not.
         *
         * @param isUserNameEditable indicates if the user name could be changed
         */
        public void setUserNameEditable(boolean isUserNameEditable)
        {
            this.isUserNameEditable = isUserNameEditable;
        }

        /**
         * Indicates if the user name is currently editable, i.e. could be changed
         * by user or not.
         *
         * @return <code>true</code> if the user name could be changed,
         * <code>false</code> - otherwise.
         */
        public boolean isUserNameEditable()
        {
            return isUserNameEditable;
        }
    }
}
