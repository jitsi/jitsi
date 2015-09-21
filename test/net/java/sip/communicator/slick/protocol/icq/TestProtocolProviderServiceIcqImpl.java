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
import net.java.sip.communicator.service.protocol.icqconstants.*;
import net.java.sip.communicator.util.*;

/**
 * Test icq/aim specific behaviour for OSCAR (AIM/ICQ) implementations of the
 * protocol provider service. The class would basically test that registration
 * succeeds, that the provider registration state is updated accordingly and
 * that the corrsponding registration change events are generated and dispatched.
 *
 * In the case of a registration failure we try to remain consistent and do
 * assertions accordingly.
 *
 * It is important to note that it is critical to execute tests in this class
 * in a particular order because of protocol limitations (we cannot login and
 * logoff to most of the existing protocol services as often as we'd like to).
 * This is why we have overridden the suite() method which kind of solves the
 * problem but adds the limitation of making extending developers manually add
 * the tests they write to the suite method.
 *
 * @author Emil Ivov
 */
public class TestProtocolProviderServiceIcqImpl extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestProtocolProviderServiceIcqImpl.class);

    private IcqSlickFixture fixture = new IcqSlickFixture();

    /**
     * The lock that we wait on until registration is finalized.
     */
    private Object registrationLock = new Object();

    /**
     * An event adapter that would collec registation state change events
     */
    public RegistrationEventCollector regEvtCollector
        = new RegistrationEventCollector();

    /**
     * Creates a test encapsulator for the method with the specified name.
     * @param name the name of the method this test should run.
     */
    public TestProtocolProviderServiceIcqImpl(String name)
    {
        super(name);
    }

    /**
     * Initializes the fixture.
     * @throws Exception if super.setUp() throws one.
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();
    }

    /**
     * Tears the fixture down.
     * @throws Exception if fixture.tearDown() fails.
     */
    @Override
    protected void tearDown() throws Exception
    {
        fixture.tearDown();
        super.tearDown();
    }

    // -------------------------- IMPORTANT ----------------------
    /**
     * Since we cannot afford to log on and off to the icq service as many
     * times as we want we're obliged to do our testing in a predfined order.
     * That's why we explicitely difine a suite with the order that suits us ;).
     * @return a TestSuite with the tests of this class ordered for execution
     * the way we want them to be.
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite();

        if(!IcqSlickFixture.onlineTestingDisabled)
        {
            suite.addTest(
                new TestProtocolProviderServiceIcqImpl("testRegister"));
            suite.addTest(
                new TestProtocolProviderServiceIcqImpl("testIsRegistered"));
            suite.addTest(
                new TestProtocolProviderServiceIcqImpl("testGetRegistrationState"));
            suite.addTest(
                new TestProtocolProviderServiceIcqImpl("testOperationSetTypes"));
        }
        return suite;
    }

    /**
     * Makes sure that the instance of the ICQ protocol provider that we're
     * going to use for testing is properly initialized and signed on ICQ. This
     * method only makes sense if called before any other icq testing.
     *
     * The method also verifies that a registration event is fired upond
     * succesful registration and collected by our event collector.
     *
     * @throws OperationFailedException if provider.register() fails.
     */
    public void testRegister()
        throws OperationFailedException
    {
        //add an event collector that will collect all events during the
        //registration and allows us to later inspect them and make sure
        //they were properly dispatched.
        fixture.provider.addRegistrationStateChangeListener(regEvtCollector);

        fixture.provider.register(new SecurityAuthorityImpl());

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

        // Here is registered the listener which will receive the first message
        // This message is supposed to be offline message and as one is tested
        // in TestOperationSetBasicInstantMessaging.testReceiveOfflineMessages()
        OperationSetBasicInstantMessaging opSetBasicIM =
            fixture.provider.getOperationSet(OperationSetBasicInstantMessaging.class);
        IcqSlickFixture.offlineMsgCollector.register(opSetBasicIM);

        //give time for the AIM server to notify everyone of our arrival
        //simply waitinf is really not a reliable way of doing things but I
        //can't think of anything better
        Object lock = new Object();
        synchronized(lock)
        {
            try
            {
                logger.debug("Giving the aim server time to notify for our arrival!");
                lock.wait(5000);
            }
            catch (Exception ex)
            {}
        }
        //make sure the provider is on-line
        assertTrue(
            "The tested ICQ implementation on-line status was OFFLINE",
            !IcqStatusEnum.OFFLINE.equals(
                IcqSlickFixture.testerAgent.getBuddyStatus(fixture.ourUserID))
        );

        //make sure that the registration process trigerred the corresponding
        //events.
        assertTrue(
            "No events were dispatched during the registration process."
            ,regEvtCollector.collectedNewStates.size() > 0);

        assertTrue(
            "No registration event notifying of registration was dispatched. "
            +"All events were: " + regEvtCollector.collectedNewStates
            ,regEvtCollector.collectedNewStates
                .contains(RegistrationState.REGISTERED));

        fixture.provider.removeRegistrationStateChangeListener(regEvtCollector);
    }

    /**
     * Verifies whether the isRegistered method returns whatever it has to.
     */
    public void testIsRegistered()
    {
        if (!IcqStatusEnum.OFFLINE.equals(
                IcqSlickFixture.testerAgent.getBuddyStatus(fixture.ourUserID)))
            assertTrue(
                   "provider.isRegistered() returned false while registered"
                   ,fixture.provider.isRegistered());
        else
            //in case registration failed - the provider needs to know that.:
            assertFalse(
                   "provider.isRegistered() returned true while unregistered"
                   ,fixture.provider.isRegistered());

    }

    /**
     * Tests that getRegistrationState returns properly after registration
     * has completed (or failed)
     */
    public void testGetRegistrationState()
    {
        if (!IcqStatusEnum.OFFLINE.equals(
                IcqSlickFixture.testerAgent.getBuddyStatus(fixture.ourUserID)))
            assertEquals(
                "a provider was not in a REGISTERED state while registered."
                    ,RegistrationState.REGISTERED
                    ,fixture.provider.getRegistrationState());
        else
            assertFalse(
                   "a provider had a REGISTERED reg state while unregistered."
                   ,fixture
                        .provider.getRegistrationState()
                                .equals(RegistrationState.REGISTERED));
    }

    /**
     * Verifies that all operation sets have the type they are declarded to
     * have.
     *
     * @throws java.lang.Exception if a class indicated in one of the keys
     * could not be forName()ed.
     */
    public void testOperationSetTypes() throws Exception
    {
        Map<String, OperationSet> supportedOperationSets =
            fixture.provider.getSupportedOperationSets();

        // make sure that keys (which are supposed to be class names) correspond
        // what the class of the values recorded against them.
        for (Map.Entry<String, OperationSet> entry : supportedOperationSets
            .entrySet())
        {
            String setName = entry.getKey();
            Object opSet = entry.getValue();

            assertTrue(opSet + " was not an instance of " + setName
                + " as declared", Class.forName(setName).isInstance(opSet));
        }
    }

    /**
     * A class that would plugin as a registration listener to a protocol
     * provider and simply record all events that it sees and notify the
     * registrationLock if it sees an event that notifies us of a completed
     * registration.
     * @author Emil Ivov
     */
    public class RegistrationEventCollector implements RegistrationStateChangeListener
    {
        public List<RegistrationState> collectedNewStates = new LinkedList<RegistrationState>();

        /**
         * The method would simply register all received events so that they
         * could be available for later inspection by the unit tests. In the
         * case where a registraiton event notifying us of a completed
         * registration is seen, the method would call notifyAll() on the
         * registrationLock.
         *
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            logger.debug("Received a RegistrationStateChangeEvent: " + evt);

            collectedNewStates.add(evt.getNewState());

            if(evt.getNewState().equals( RegistrationState.REGISTERED))
            {
                logger.debug("We're registered and will notify those who wait");
                synchronized(registrationLock){
                    registrationLock.notifyAll();
                }
            }
        }

    }

    /**
     * A very simple straightforward implementation of a security authority
     * that would authentify our tested implementation if necessary, by
     * retrieving its password through the system properties.
     */
    public class SecurityAuthorityImpl implements SecurityAuthority
    {
        private boolean isUserNameEditable = false;

        /**
         * Creates an instance of this class that would would authentify our
         * tested implementation if necessary, by retrieving its password
         * through the system properties.
         */
        public SecurityAuthorityImpl()
        {}

        /**
         * Returns a Credentials object containing the password for our
         * tested implementation.
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
         * Returns a Credentials object containing the password for our
         * tested implementation.
         * <p>
         * @param realm The realm that the credentials are needed for.
         * @param defaultValues the values to propose the user by default
         * @return The credentials associated with the specified realm or null if
         * none could be obtained.
         */
        public UserCredentials obtainCredentials(String realm,
                                                 UserCredentials defaultValues)
        {
            String passwd = System.getProperty( IcqProtocolProviderSlick
                                            .TESTED_IMPL_PWD_PROP_NAME, null );
            defaultValues.setPassword(passwd.toCharArray());
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
