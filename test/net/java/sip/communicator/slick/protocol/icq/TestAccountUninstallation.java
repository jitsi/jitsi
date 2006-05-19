/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.icq;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import org.osgi.framework.*;
import java.util.List;
import java.util.LinkedList;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.util.Logger;

/**
 * Tests whether accaounts are uninstalled properly. It is important that
 * tests from this class be called last since they will install the accounts
 * that have been used to test the implementations.
 *
 * @author Emil Ivov
 * @author Damian Minkov
 */
public class TestAccountUninstallation
    extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestAccountUninstallation.class);

    IcqSlickFixture fixture = new IcqSlickFixture();

    /**
     * An event adapter that would collec registation state change events
     */
    public RegistrationEventCollector regEvtCollector
        = new RegistrationEventCollector();

    /**
     * The lock that we wait on until registration is finalized.
     */
    private Object registrationLock = new Object();



    public TestAccountUninstallation(String name)
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

    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(
            new TestAccountUninstallation("testMultipleLogins"));
        suite.addTest(
            new TestAccountUninstallation("testUninstallAccount"));

        return suite;
    }


    /**
     * Before we uninstall the current account which is registered to the server
     * we add a registration change listener and wait for unregistered event
     * to be fired.
     * Then we use the tester agent to register to the servers with
     * the account info of the currently logged in account so we must
     * receive multiple logins event.
     */
    public void testMultipleLogins()
    {
        fixture.provider.addRegistrationStateChangeListener(regEvtCollector);

        String passwd = System.getProperty( IcqProtocolProviderSlick
                                            .TESTED_IMPL_PWD_PROP_NAME, null );
        String uin = System.getProperty( IcqProtocolProviderSlick
                                         .TESTED_IMPL_ACCOUNT_ID_PROP_NAME, null);

        IcqTesterAgent testerAgent = new IcqTesterAgent(uin);
        testerAgent.register(passwd);

        // give time to the register process
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

        testerAgent.unregister();

        assertNotNull(
                    "No event was dispatched"
                    ,regEvtCollector.stateRecieved);

        assertEquals(
                    "Event is not UNREGISTERED event"
                    , regEvtCollector.stateRecieved
                    , RegistrationState.UNREGISTERED);

        assertEquals(
            "No registration event notifying of Multiple logins dispatched "
            , regEvtCollector.eventReason
            , RegistrationStateChangeEvent.REASON_MULTIPLE_LOGINS);
    }

    /**
     * Uinstalls our test account and makes sure it really has been removed.
     */
    public void testUninstallAccount()
    {
        assertFalse("No installed accaounts found",
                    fixture.accManager.getRegisteredAcounts().isEmpty());

        assertNotNull(
            "Found no provider corresponding to account ID "
            + fixture.icqAccountID,
            fixture.accManager.getProviderForAccount(fixture.icqAccountID));

        assertTrue(
            "Failed to remove a provider corresponding to acc id "
            + fixture.icqAccountID,
            fixture.accManager.uninstallAccount(fixture.icqAccountID));

        ServiceReference[] icqProviderRefs = null;
        try
        {
            icqProviderRefs = fixture.bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(&"
                + "(" + AccountManager.PROTOCOL_PROPERTY_NAME + "=" +ProtocolNames.ICQ + ")"
                + "(" + AccountManager.ACCOUNT_ID_PROPERTY_NAME + "="+ fixture.icqAccountID + ")"
                + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            fail("We apparently got our filter wrhong");
        }

        //make sure we didn't see a service
        assertTrue("A Protocol Provider Service was still regged as an osgi service "
                      +"for ICQ UIN:" + fixture.icqAccountID
                      + "After it was explicitly uninstalled"
                      ,icqProviderRefs == null || icqProviderRefs.length == 0);

        //verify that the provider knows that we have uninstalled the service.
        assertTrue(
            "The ICQ account manager kept a reference to the provider we just "
            +"uninstalled (accID="+fixture.icqAccountID+")",
            fixture.accManager.getRegisteredAcounts().isEmpty()
            && fixture.accManager.getProviderForAccount(fixture.icqAccountID) == null
            );
    }

    /**
     * A class that would plugin as a registration listener to a protocol
     * provider and simply record all events that it sees and notify the
     * registrationLock if it sees an event that notifies us of a completed
     * registration.
     */
    public class RegistrationEventCollector implements RegistrationStateChangeListener
    {
        RegistrationState stateRecieved = null;
        int eventReason = -1;

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

            if(evt.getNewState().equals( RegistrationState.UNREGISTERED))
            {
                logger.debug("Connection FAILED!");
                stateRecieved = evt.getNewState();
                eventReason = evt.getReasonCode();

                synchronized(registrationLock){
                    logger.debug(".");
                    registrationLock.notifyAll();
                    logger.debug(".");
                }
            }
        }
    }
}
