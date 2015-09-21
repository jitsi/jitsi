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

import org.osgi.framework.*;

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
    public static Hashtable<String, List<String>> preInstalledBuddyList  = null;

    public ServiceReference        icqServiceRef   = null;
    public ProtocolProviderService provider        = null;
    public ProtocolProviderFactory providerFactory = null;
    public String                  ourUserID       = null;

    /**
     * A reference to the bundle containing the tested pp implementation. This
     * reference is set during the account uninstallation testing and used during
     * the account uninstallation persistence testing.
     */
    public static Bundle           providerBundle  = null;

    public static OfflineMsgCollector offlineMsgCollector = null;

    /**
     * Indicates whether the user has requested for onlline tests not to be run.
     * (e.g. due to lack of network connectivity or ... time constraints ;)).
     */
    public static boolean onlineTestingDisabled = false;

    @Override
    public void setUp() throws Exception
    {
        // first obtain a reference to the provider factory
        ServiceReference[] serRefs = null;
        String osgiFilter = "(" + ProtocolProviderFactory.PROTOCOL
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
            "Failed to find a provider factory service for protocol ICQ",
            (serRefs != null) && (serRefs.length >  0));

        //Keep the reference for later usage.
        providerFactory = (ProtocolProviderFactory)
            IcqSlickFixture.bc.getService(serRefs[0]);

        ourUserID =
            System.getProperty(
                IcqProtocolProviderSlick.TESTED_IMPL_USER_ID_PROP_NAME);


        //find the protocol provider service
        ServiceReference[] icqProviderRefs
            = bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(&"
                +"("+ProtocolProviderFactory.PROTOCOL+"="+ProtocolNames.ICQ+")"
                +"("+ProtocolProviderFactory.USER_ID+"="
                + ourUserID +")"
                +")");

        //make sure we found a service
        assertNotNull("No Protocol Provider was found for ICQ UIN:"+ ourUserID,
                     icqProviderRefs);
        assertTrue("No Protocol Provider was found for ICQ UIN:"+ ourUserID,
                     icqProviderRefs.length > 0);

        //save the service for other tests to use.
        icqServiceRef = icqProviderRefs[0];
        provider = (ProtocolProviderService)bc.getService(icqServiceRef);
    }

    @Override
    public void tearDown()
    {
        bc.ungetService(icqServiceRef);
    }

    /**
     * Returns the bundle that has registered the protocol provider service
     * implementation that we're currently testing. The method would go through
     * all bundles currently installed in the framework and return the first
     * one that exports the same protocol provider instance as the one we test
     * in this slick.
     * @param provider the provider whose bundle we're looking for.
     * @return the Bundle that has registered the protocol provider service
     * we're testing in the slick.
     */
    public static Bundle findProtocolProviderBundle(
        ProtocolProviderService provider)
    {
        Bundle[] bundles = bc.getBundles();

        for (int i = 0; i < bundles.length; i++)
        {
            ServiceReference[] registeredServices
                = bundles[i].getRegisteredServices();

            if (registeredServices == null)
                continue;

            for (int j = 0; j < registeredServices.length; j++)
            {
                Object service
                    = bc.getService(registeredServices[j]);
                if (service == provider)
                    return bundles[i];
            }
        }

        return null;
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
