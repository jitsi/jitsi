/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.rss;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import java.util.*;

/**
 * This class performs testing on protocol provider methods. It verifies that
 * the protocol provider service implements the declared operation sets and also
 * tests the process of registering an account.
 *
 * @author Mihai Balan
 */
public class TestProtocolProviderServiceRssImpl
    extends TestCase
{
    /**
     * Time in miliseconds to wait for registration to complete. Is subject to
     * further fine-tuning.
     */
    public static final int WAIT_DELAY = 10000;

    public static final Logger logger =
        Logger.getLogger(TestProtocolProviderServiceRssImpl.class);

    /**
     * Test's fixture.
     */
    private RssSlickFixture fixture = new RssSlickFixture();

    public RegistrationEventCollector eventCollector =
        new RegistrationEventCollector();

    /**
     * Creates a test case that runs the specified test.
     *
     * @param testName the name of the method the test should run.
     */
    public TestProtocolProviderServiceRssImpl(String testName)
    {
        super(testName);
    }

    /**
     * Initializes the fixture.
     *
     * @throws Exception if <code>super.setUp()</code> throws one.
     */
    public void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();
    }

    /**
     * Cleans up after the test.
     *
     * @throws Exception if <code>fixture.tearDown()</code> fails.
     */
    public void tearDown() throws Exception
    {
        fixture.tearDown();
        super.tearDown();
    }

    /**
     * This methods makes sure that the instance of the RSS protocol provider
     * that we are going to use is properly initialized and registered. It also
     * verifies that a registration event is fired upon successful registration
     * and collected by our event collector.
     *
     * @throws OperationFailedException if <code>provider.register()</code>
     * fails.
     */
    public void testRegister() throws OperationFailedException
    {
        //registering as a listener & starting registration with a
        //null SecurityAuthority
        fixture.provider.addRegistrationStateChangeListener(
                this.eventCollector);
        fixture.provider.register(new  NullSecurityAuthority());

        //give it a little time to do his magic ;)
        logger.debug("Waiting for registration to complete...");
        eventCollector.waitForEvent(WAIT_DELAY);

        //make sure we received events...
        assertTrue("No events were dispatched during the registration process.",
            eventCollector.collectedStates.size() > 0);
        //...and they were the right ones
        assertTrue("No registration event notifying of registration was"
            + " dispatched."
            + " All events were:" + eventCollector.collectedStates,
            eventCollector.collectedStates.contains(
                RegistrationState.REGISTERED));

        //if everything is ok, we leave it alone
        fixture.provider.removeRegistrationStateChangeListener(eventCollector);
    }

    /**
     * This method verifies that all operation sets have the type they are
     * declared to have.
     *
     * @throws ClassNotFoundException if a class name found in the keys for the
     * supported operation sets doesn't correspond to a class that the
     * class loader can find.
     */
    public void testOperationsSets() throws ClassNotFoundException
    {
        Map<String, OperationSet> supportedOperationSets =
            fixture.provider.getSupportedOperationSets();

        // get the keys for the supported operation set. The keys are strings
        // corresponding to class names.
        for (Map.Entry<String, OperationSet> entry : supportedOperationSets
            .entrySet())
        {
            String key = entry.getKey();
            Object opSet = entry.getValue();

            assertTrue(
                opSet + " was not an instance of " + key + "as declared", Class
                    .forName(key).isInstance(opSet));
        }
    }

    /**
     * This class acts as a very simple registration listener for the protocol
     * provider and simply records all events it receives and notifies
     * (<code>notifyAll()</code>) all objects waiting for an instance of this
     * class upon receiving an event that signals a completed registration.
     *
     * TODO: This class is just the same as the one in
     * TestProtocolProviderServiceGibberishImpl.java . Is there any reason for
     * duplicating code this way?
     *
     * @author Emil Ivov
     * @author Mihai Balan
     */
    public class RegistrationEventCollector
        implements RegistrationStateChangeListener
    {
        /**
         * We store all the received events in this list. It's made public to
         * ease later inspection by unit tests.
         */
        public List<RegistrationState> collectedStates = new LinkedList<RegistrationState>();

        /**
         * This method simply records all received events in a <code>List</code>
         * that can be easily inspected by unit tests. In case we receive a
         * registration event notifying us of a completed registration, the
         * method calls <code>notifyAll()</code>.
         *
         * @param e a <code>RegistrationStateChangeEvent</code> decribing the
         * status change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent e)
        {
            logger.debug("Received a RegistrationChangeEvent: " + e.toString());

            collectedStates.add(e.getNewState());

            if (e.getNewState().equals(RegistrationState.REGISTERED))
            {
            logger.debug("We're registered. Notifying waiting threads");

            synchronized(this)
            {
                notifyAll();
            }
            }
        }

        /**
         * Blocks until an event notifying us of the awaited status change is
         * received or until <code>waitFor</code> miliseconds pass, whichever
         * comes first.
         *
         * @param waitFor the number of seconds to wait for an event. If no
         * event is received, we simply return.
         */
        public void waitForEvent(long waitFor)
        {
            logger.trace("Waiting for a RegistrationChangeEvent");

            synchronized(this)
            {
                if (collectedStates.contains(RegistrationState.REGISTERED))
                {
                    logger.trace("Event already received" + collectedStates);
                    return;
                }

                try {
                    wait(waitFor);

                    if (collectedStates.size() > 0)
                    logger.trace("Received a RegistrationStateChangeEvent.");
                    else
                    logger.trace("No registrationStateChangeEvent received"
                        + " for" + waitFor + "ms.");
                } catch(InterruptedException ie)
                {
                    logger.debug("Interrupted while waiting for "
                        + "a RegistrationStateChangeEvent", ie);
                }
            }
        }
    }

    /**
     * Dummy implementation of a <code>SecurityAuthority</code>. It simply
     * returns null credentials as the RSS "authentication" protocol requires
     * none.
     *
     * @author Mihai Balan
     */
    public class NullSecurityAuthority
        implements SecurityAuthority
    {
        public UserCredentials obtainCredentials(String realm,
            UserCredentials defaultValues,
            int reasonCode)
        {
            return null;
        }

        public UserCredentials obtainCredentials(String realm,
            UserCredentials defaultValues)
        {
            return null;
        }

        public void setUserNameEditable(boolean isUserNameEditable)
        {
        }

        public boolean isUserNameEditable()
        {
            return false;
        }
    }
}
