package net.java.sip.communicator.slick.protocol.icq;

import net.java.sip.communicator.service.protocol.*;
import org.osgi.framework.*;
import junit.framework.*;
import java.util.*;
import net.java.sip.communicator.service.protocol.event.MessageListener;
import net.java.sip.communicator.service.protocol.event.MessageReceivedEvent;
import net.java.sip.communicator.service.protocol.event.MessageDeliveredEvent;
import net.java.sip.communicator.service.protocol.event.MessageDeliveryFailedEvent;

/**
 * Provides utility code, such as locating and obtaining references towards
 * base services that anyother service would need.
 *
 * @author Emil Ivov
 */
public class IcqSlickFixture extends TestCase
{
    /**
     * To be set by the slick itself upon activation.
     */
    public static BundleContext bc = null;

    /**
     * The tested account id obtained during installation.
     */
    public static AccountID icqAccountID = null;

    /**
     * The agent that we use to verify whether the tested implementation is
     * being honest with us. The icq tester agent is instantiated and registered
     * by the icq slick activator. If it is still null when a test is running,
     * it means there was something seriously wrong with the test account
     * properties file.
     */
    static IcqTesterAgent testerAgent = null;

    /**
     * A Hashtable containing group names mapped against array lists of buddy
     * screen names. This is a snapshot of the server stored buddy list for
     * the icq account that is going to be used by the tested implementation.
     * It is filled in by the icq tester agent who'd login with that account
     * and initialise the ss contact list before the tested implementation has
     * actually done so.
     */
    public static Hashtable preInstalledBuddyList = null;

    public ServiceReference        icqServiceRef = null;
    public ProtocolProviderService provider      = null;
    public ProtocolProviderFactory          accManager    = null;
    public String                  ourAccountID  = null;

    public static OfflineMsgCollector offlineMsgCollector = null;

    public void setUp() throws Exception
    {
        // first obtain a reference to the provider factory
        ServiceReference[] serRefs = null;
        String osgiFilter = "(" + ProtocolProviderFactory.PROTOCOL_PROPERTY_NAME
                            + "="+ProtocolNames.ICQ+")";
        try{
            serRefs = IcqSlickFixture.bc.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            //this really shouldhn't occur as the filter expression is static.
            fail(osgiFilter + " is not a valid osgi filter");
        }

        assertTrue(
            "Failed to find an provider factory service for protocol ICQ",
            serRefs != null || serRefs.length >  0);

        //Keep the reference for later usage.
        accManager = (ProtocolProviderFactory)
            IcqSlickFixture.bc.getService(serRefs[0]);

        ourAccountID =
            System.getProperty(
                IcqProtocolProviderSlick.TESTED_IMPL_ACCOUNT_ID_PROP_NAME);


        //find the protocol provider service
        ServiceReference[] icqProviderRefs
            = bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(&"
                +"("+ProtocolProviderFactory.PROTOCOL_PROPERTY_NAME+"="+ProtocolNames.ICQ+")"
                +"("+ProtocolProviderFactory.ACCOUNT_ID_PROPERTY_NAME+"="
                + ourAccountID +")"
                +")");

        //make sure we found a service
        assertNotNull("No Protocol Provider was found for ICQ UIN:"+ ourAccountID,
                     icqProviderRefs);
        assertTrue("No Protocol Provider was found for ICQ UIN:"+ ourAccountID,
                     icqProviderRefs.length > 0);

        //save the service for other tests to use.
        icqServiceRef = icqProviderRefs[0];
        provider = (ProtocolProviderService)bc.getService(icqServiceRef);
    }

    public void tearDown()
    {
        bc.ungetService(icqServiceRef);
    }

    //used in Offline Message receive test
    //this MessageReceiver is created in IcqProtocolProviderSlick
    //registered as listener in TestProtocolProviderServiceIcqImpl
    // as soon tested account has been registed
    //There is only one offline message send. And this message is the first message
    // received after the successful regitration, so this listener is removed
    // after receiving one message. This message is tested in TestOperationSetBasicInstantMessaging
    // whether it is the one that has been send
    static class OfflineMsgCollector implements MessageListener
    {
        private String offlineMessageToBeDelivered = null;
        private OperationSetBasicInstantMessaging imOper = null;
        private Message receivedMessage = null;

        public void messageReceived(MessageReceivedEvent evt)
        {
            receivedMessage = evt.getSourceMessage();

            imOper.removeMessageListener(this);
        }

        public void messageDelivered(MessageDeliveredEvent evt)
        {
        }

        public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
        {
        }

        public void setMessageText(String txt)
        {
            this.offlineMessageToBeDelivered = txt;
        }

        public String getMessageText()
        {
            return offlineMessageToBeDelivered;
        }

        public void register(OperationSetBasicInstantMessaging imOper)
        {
            this.imOper = imOper;
            imOper.addMessageListener(this);
        }

        public Message getReceivedMessage()
        {
            return receivedMessage;
        }
    }
}
