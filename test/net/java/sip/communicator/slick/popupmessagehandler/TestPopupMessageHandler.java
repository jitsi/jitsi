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
package net.java.sip.communicator.slick.popupmessagehandler;

import junit.framework.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;

import org.osgi.framework.*;

/**
 * Test suite for the popup message handler interface.
 * @author Symphorien Wanko
 */
public class TestPopupMessageHandler
    extends TestCase
{
    /**
     * the <tt>SystrayService</tt> reference we will get from bundle
     * context to register ours handlers
     */
    private static SystrayService systrayService = null;

    /**
     * reference to services we will retrieve from bundle context
     */
    private static ServiceReference serviceReference = null;

    /**
     * the <tt>NotificationService</tt> reference we will get from bundle
     * context to send notifications to our handlers
     */
    private static NotificationService notificationService = null;

    /**
     * a trivial message we will send via the notification service.
     */
    private String messageStart = "Lorem ipsum dolor sit amet.";

    /**
     * the first handler we will use.
     */
    private PopupMessageHandler handler1 = new MockPopupMessageHandler();

    /**
     * the second handler we will use.
     */
    private PopupMessageHandler handler2 = new MockPopupMessageHandler();

    /**
     * A reference to the bundle context in which we are runing.
     */
    private BundleContext bc = PopupMessageHandlerSLick.bundleContext;

    /**
     * Create an instance to launch tests.
     * @param name name of the test case
     */
    public TestPopupMessageHandler(String name)
    {
        super(name);
    }

    /**
     * Creates the test suite
     * @return the <tt>TestSuite</tt> created
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite();

        suite.addTest(
            new TestPopupMessageHandler("testHandlerModification"));

        suite.addTest(
            new TestPopupMessageHandler("testNotificationHandling"));

        return suite;
    }

    /**
     * here we will test (get/set)ActivePopupMesssageHandler()
     */
    public void testHandlerModification()
    {
        serviceReference = bc.getServiceReference(
                SystrayService.class.getName());

        systrayService = (SystrayService) bc.getService(serviceReference);

        systrayService.setActivePopupMessageHandler(handler1);

        // do we have our handler as expected ?
        assertEquals(handler1, systrayService.getActivePopupMessageHandler());

        // was handler1 the previous handler as returned by the set method ?
        assertEquals(
                handler1,
                systrayService.setActivePopupMessageHandler(handler2));

        // and now handler2 is our curretn hander
        assertEquals(handler2, systrayService.getActivePopupMessageHandler());
    }

    /**
     * we will fire a notification then see if it was handled by the right handler
     * which received the right message.
     */
    public void testNotificationHandling()
    {
        serviceReference =  bc.getServiceReference(
                NotificationService.class.getName());
        notificationService
            = (NotificationService) bc.getService(serviceReference);

        notificationService.fireNotification(
                NotificationAction.ACTION_POPUP_MESSAGE,
                messageStart,
                messageStart,
                null);
    }

    /** A trivial handler implementing <tt>PopupMessageHandler</tt> */
    private class MockPopupMessageHandler implements PopupMessageHandler
    {
        /**
         * implements <tt>PopupMessageHandler.addPopupMessageListener()</tt>
         */
        public void addPopupMessageListener(SystrayPopupMessageListener listener)
        {}

        /**
         * implements <tt>PopupMessageHandler.removePopupMessageListener()</tt>
         */
        public void removePopupMessageListener(SystrayPopupMessageListener listener)
        {}

        /**
         * implements <tt>PopupMessageHandler#showPopupMessage()</tt>
         */
        public void showPopupMessage(PopupMessage popupMsg)
        {
            // is it the expected message and title ?
            assertEquals(messageStart, popupMsg.getMessage());
            assertEquals(messageStart, popupMsg.getMessageTitle());

            // is it the expected handler which is handling it ?
            assertEquals(handler2, this);
        }

        /**
         * implements <tt>getPreferenceIndex</tt> from <tt>PopupMessageHandler</tt>
         */
        public int getPreferenceIndex()
        {
            return 0;
        }
    }
}
