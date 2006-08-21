/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.sip;

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
public class TestProtocolProviderServiceSipImpl
    extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestProtocolProviderServiceSipImpl.class);

    private SipSlickFixture fixture = new SipSlickFixture();

    /**
     * The lock that we wait on until registration is finalized.
     */
    private Object registrationLock = new Object();

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
    public TestProtocolProviderServiceSipImpl(String name)
    {
        super(name);
    }

    /**
     * Initializes the fixture.
     * @throws Exception if super.setUp() throws one.
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();
    }

    /**
     * Tears the fixture down.
     * @throws Exception if fixture.tearDown() fails.
     */
    protected void tearDown() throws Exception
    {
        fixture.tearDown();
        super.tearDown();
    }

    /**
     * Makes sure that the instance of the SIP protocol provider that we're
     * going to use for testing is properly initialized and registered with
     * a SIP registrar. This MUST be called before any other online testing
     * of the SIP provider so that we won't have to reregister for every single
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
        if(true)
            return;
        //add an event collector that will collect all events during the
        //registration and allow us to later inspect them and make sure
        //they were properly dispatched.
        fixture.provider1.addRegistrationStateChangeListener(regEvtCollector1);
        fixture.provider2.addRegistrationStateChangeListener(regEvtCollector2);

        //register both our providers
        fixture.provider1.register(new SecurityAuthorityImpl(
            System.getProperty(SipProtocolProviderServiceLick.ACCOUNT_1_PREFIX
                           + ProtocolProviderFactory.PASSWORD).toCharArray()));
        fixture.provider2.register(new SecurityAuthorityImpl(
            System.getProperty(SipProtocolProviderServiceLick.ACCOUNT_2_PREFIX
                           + ProtocolProviderFactory.PASSWORD).toCharArray()));


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

        fixture.provider1
            .removeRegistrationStateChangeListener(regEvtCollector1);
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
        Map supportedOperationSets
            = fixture.provider1.getSupportedOperationSets();

        //make sure that keys (which are supposed to be class names) correspond
        //what the class of the values recorded against them.
        Iterator setNames = supportedOperationSets.keySet().iterator();
        while (setNames.hasNext())
        {
            String setName = (String) setNames.next();
            Object opSet = supportedOperationSets.get(setName);

            assertTrue(opSet + " was not an instance of "
                       + setName + " as declared"
                       , Class.forName(setName).isInstance(opSet));
        }
    }

    /**
     * A class that would plugin as a registration listener to a protocol
     * provider and simply record all events that it sees and notify the
     * registrationLock if it sees an event that notifies us of a completed
     * registration.
     */
    public class RegistrationEventCollector
        implements RegistrationStateChangeListener
    {
        public List collectedNewStates = new LinkedList();

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

            if (evt.getNewState().equals(RegistrationState.REGISTERED))
            {
                logger.debug("We're registered and will notify those who wait");
                synchronized (registrationLock)
                {
                    registrationLock.notifyAll();
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
         * @return The credentials associated with the specified realm or null
         * if none could be obtained.
         */
        public UserCredentials obtainCredentials(String          realm,
                                                 UserCredentials defaultValues)
        {
            defaultValues.setPassword(passwd);
            return defaultValues;
        }

    }

}
