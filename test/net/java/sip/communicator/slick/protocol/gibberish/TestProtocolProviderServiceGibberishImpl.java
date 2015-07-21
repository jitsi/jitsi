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
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Performs testing on protocol provider methods.
 * @todo add more detailed docs once the tests are written.
 * @author Emil Ivov
 */
public class TestProtocolProviderServiceGibberishImpl
    extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestProtocolProviderServiceGibberishImpl.class);

    private GibberishSlickFixture fixture = new GibberishSlickFixture();

    /**
     * An event adapter that would collec registation state change events
     */
    public RegistrationEventCollector regEvtCollector1
        = new RegistrationEventCollector();

    /**
     * An event adapter that would collec registation state change events
     */
    public RegistrationEventCollector regEvtCollector2
        = new RegistrationEventCollector();

    /**
     * Creates a test encapsulator for the method with the specified name.
     * @param name the name of the method this test should run.
     */
    public TestProtocolProviderServiceGibberishImpl(String name)
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

    /**
     * Makes sure that the instance of the Gibberish protocol provider that we're
     * going to use for testing is properly initialized and registered with
     * a Gibberish registrar. This MUST be called before any other online testing
     * of the Gibberish provider so that we won't have to reregister for every single
     * test.
     * <p>
     * The method also verifies that a registration event is fired upon
     * succesful registration and collected by our event collector.
     *
     * @throws OperationFailedException if provider.register() fails.
     */
    public void testRegister()
        throws OperationFailedException
    {
        //add an event collector that will collect all events during the
        //registration and allow us to later inspect them and make sure
        //they were properly dispatched.
        fixture.provider1.addRegistrationStateChangeListener(regEvtCollector1);
        fixture.provider2.addRegistrationStateChangeListener(regEvtCollector2);

        //register both our providers
        fixture.provider1.register(new SecurityAuthorityImpl(
            System.getProperty(GibberishProtocolProviderServiceLick.ACCOUNT_1_PREFIX
                           + ProtocolProviderFactory.PASSWORD).toCharArray()));
        fixture.provider2.register(new SecurityAuthorityImpl(
            System.getProperty(GibberishProtocolProviderServiceLick.ACCOUNT_2_PREFIX
                           + ProtocolProviderFactory.PASSWORD).toCharArray()));

        //give it enough time to register. We won't really have to wait all this
        //time since the registration event collector would notify us the moment
        //we get signed on.
        logger.debug("Waiting for registration to complete ...");

        regEvtCollector1.waitForEvent(15000);
        regEvtCollector2.waitForEvent(40000);

        //make sure that the registration process trigerred the corresponding
        //events.
        assertTrue(
            "No events were dispatched during the registration process."
            ,regEvtCollector1.collectedNewStates.size() > 0);

        assertTrue(
            "No registration event notifying of registration was dispatched. "
            +"All events were: " + regEvtCollector1.collectedNewStates
            ,regEvtCollector1.collectedNewStates
                .contains(RegistrationState.REGISTERED));

        //now the same for provider 2
        assertTrue(
            "No events were dispatched during the registration process "
            +"of provider2."
            ,regEvtCollector2.collectedNewStates.size() > 0);

        assertTrue(
            "No registration event notifying of registration was dispatched. "
            +"All events were: " + regEvtCollector2.collectedNewStates
            ,regEvtCollector2.collectedNewStates
                .contains(RegistrationState.REGISTERED));


        fixture.provider1
            .removeRegistrationStateChangeListener(regEvtCollector1);
        fixture.provider2
            .removeRegistrationStateChangeListener(regEvtCollector2);
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
            fixture.provider1.getSupportedOperationSets();

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
     * provider and simply record all events that it sees and notifyAll()
     *  if it sees an event that notifies us of a completed
     * registration.
     */
    public class RegistrationEventCollector
        implements RegistrationStateChangeListener
    {
        public List<RegistrationState> collectedNewStates = new LinkedList<RegistrationState>();

        /**
         * The method would simply register all received events so that they
         * could be available for later inspection by the unit tests. In the
         * case where a registraiton event notifying us of a completed
         * registration is seen, the method would call notifyAll().
         *
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            logger.debug("Received a RegistrationStateChangeEvent: " + evt);

            collectedNewStates.add(evt.getNewState());

            if (evt.getNewState().equals(RegistrationState.REGISTERED))
            {
                logger.debug("We're registered and will notify those who wait");
                synchronized (this)
                {
                    notifyAll();
                }
            }
        }

        /**
         * Blocks until an event notifying us of the awaited state change is
         * received or until waitFor miliseconds pass (whichever happens first).
         *
         * @param waitFor the number of miliseconds that we should be waiting
         * for an event before simply bailing out.
         */
        public void waitForEvent(long waitFor)
        {
            logger.trace("Waiting for a RegistrationStateChangeEvent ");

            synchronized (this)
            {
                if (collectedNewStates.contains(RegistrationState.REGISTERED))
                {
                    logger.trace("Event already received. "
                                 + collectedNewStates);
                    return;
                }

                try
                {
                    wait(waitFor);

                    if (collectedNewStates.size() > 0)
                        logger.trace(
                            "Received a RegistrationStateChangeEvent.");
                    else
                        logger.trace(
                            "No RegistrationStateChangeEvent received for "
                            + waitFor + "ms.");

                }
                catch (InterruptedException ex)
                {
                    logger.debug(
                        "Interrupted while waiting for a "
                        +"RegistrationStateChangeEvent"
                        , ex);
                }
            }
        }


    }

    /**
     * A very simple straight forward implementation of a security authority
     * that would always return the same password (the one specified upon
     * construction) when asked for credentials.
     */
    public class SecurityAuthorityImpl
        implements SecurityAuthority
    {
        private boolean isUserNameEditable = false;

        /**
         * The password to return when asked for credentials
         */
        private char[] passwd = null;

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
         * @return The credentials associated with the specified realm or null
         * if none could be obtained.
         */
        public UserCredentials obtainCredentials(String          realm,
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
         * @return The credentials associated with the specified realm or null
         * if none could be obtained.
         */
        public UserCredentials obtainCredentials(String          realm,
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
