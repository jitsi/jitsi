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
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.protocol.icqconstants.*;

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

    public TestProtocolProviderServiceIcqImpl(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();
    }

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
        suite.addTest(
            new TestProtocolProviderServiceIcqImpl("testRegister"));
        suite.addTest(
            new TestProtocolProviderServiceIcqImpl("testIsRegistered"));
        suite.addTest(
            new TestProtocolProviderServiceIcqImpl("testGetRegistrationState"));
        suite.addTest(
            new TestProtocolProviderServiceIcqImpl("testOperationSetTypes"));

        return suite;
    }

    /**
     * Makes sure that the instance of the ICQ protocol provider that we're
     * going to use for testing is properly initialized and signed on ICQ. This
     * method only makes sense if called before any other icq testing.
     *
     * The method also verifies that a registration event is fired upond
     * succesful registration and collected by our event collector.
     */
    public void testRegister()
    {
        //add an event collector that will collect all events during the
        //registration and allows us to later inspect them and make sure
        //they were properly dispatched.
        fixture.provider.addRegistrationStateChangeListener(regEvtCollector);

        fixture.provider.register(null);

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
        Map supportedOperationSets =
            fixture.provider.getSupportedOperationSets();
        OperationSetBasicInstantMessaging opSetBasicIM =
                    (OperationSetBasicInstantMessaging)supportedOperationSets.get(
                OperationSetBasicInstantMessaging.class.getName());
        fixture.offlineMsgCollector.register(opSetBasicIM);

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
                fixture.testerAgent.getBuddyStatus(fixture.ourAccountID))
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
                fixture.testerAgent.getBuddyStatus(fixture.ourAccountID)))
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
                fixture.testerAgent.getBuddyStatus(fixture.ourAccountID)))
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
        Map supportedOperationSets
            = fixture.provider.getSupportedOperationSets();

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
     * @author Emil Ivov
     */
    public class RegistrationEventCollector implements RegistrationStateChangeListener
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

            if(evt.getNewState().equals( RegistrationState.REGISTERED))
            {
                logger.debug("We're registered and will notify those who wait");
                synchronized(registrationLock){
                    logger.debug(".");
                    registrationLock.notifyAll();
                    logger.debug(".");
                }
            }
        }

    }
}
