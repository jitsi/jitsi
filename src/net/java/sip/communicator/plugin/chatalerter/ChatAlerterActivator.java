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
package net.java.sip.communicator.plugin.chatalerter;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;

import org.jdesktop.jdic.misc.*;
import org.jitsi.service.configuration.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

import java.beans.*;
import java.util.*;

/**
 * Chat Alerter plugin.
 *
 * Sends alerts to the user when new message arrives and the application is not
 * in the foreground. On Mac OS X this will bounce the dock icon until the user
 * selects the chat windows. On Windows, Gnome and KDE this will flash the
 * taskbar button/icon until the user selects the chat window.
 *
 * @author Damian Minkov
 */
public class ChatAlerterActivator
    implements  BundleActivator,
                ServiceListener,
                MessageListener,
                ChatRoomMessageListener,
                AdHocChatRoomMessageListener,
                LocalUserChatRoomPresenceListener,
                LocalUserAdHocChatRoomPresenceListener,
                PropertyChangeListener,
                CallListener
{
    /**
     * The logger for this class.
     */
    private static Logger logger = Logger.getLogger(ChatAlerterActivator.class);

    /**
     * The BundleContext that we got from the OSGI bus.
     */
    private BundleContext bundleContext = null;

    /**
     * UIService reference.
     */
    private UIService uiService;

    /**
     * Whether we are started.
     */
    private boolean started = false;

    /**
     * Starts this bundle.
     * @param bc bundle context.
     * @throws Exception
     */
    public void start(BundleContext bc) throws Exception
    {
        this.bundleContext = bc;

        ServiceUtils.getService(bundleContext, ConfigurationService.class)
                    .addPropertyChangeListener(
                        ConfigurationUtils.ALERTER_ENABLED_PROP, this);

        try
        {
            if(!ConfigurationUtils.isAlerterEnabled())
            {
                return;
            }

            // try to load native libs, if it fails don't do anything
            if(!OSUtils.IS_MAC)
                Alerter.newInstance();
        }
        catch (Exception exception)
        {
            if (logger.isInfoEnabled())
                logger.info("The Alerter not supported or problem loading it!",
                exception);
            return;
        }

        startInternal(bc);
    }

    /**
     * Starts the impl and adds necessary listeners.
     * @param bc the current bundle context.
     */
    private void startInternal(BundleContext bc)
    {
        // start listening for newly register or removed protocol providers
        bc.addServiceListener(this);

        ServiceReference[] protocolProviderRefs;
        try
        {
            protocolProviderRefs = bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error(
                "Error while retrieving service refs", ex);
            return;
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {
            if (logger.isDebugEnabled())
                logger.debug("Found "
                         + protocolProviderRefs.length
                         + " already installed providers.");
            for (ServiceReference protocolProviderRef : protocolProviderRefs)
            {
                ProtocolProviderService provider
                    = (ProtocolProviderService)
                        bc.getService(protocolProviderRef);

                this.handleProviderAdded(provider);
            }
        }

        this.started = true;
    }

    /**
     * Stops bundle.
     * @param bc context.
     * @throws Exception
     */
    public void stop(BundleContext bc) throws Exception
    {
        stopInternal(bc);

        ServiceUtils.getService(bundleContext, ConfigurationService.class)
            .removePropertyChangeListener(
                ConfigurationUtils.ALERTER_ENABLED_PROP, this);
    }

    /**
     * Stops the impl and removes necessary listeners.
     * @param bc the current bundle context.
     */
    private void stopInternal(BundleContext bc)
    {
        // start listening for newly register or removed protocol providers
        bc.removeServiceListener(this);

        ServiceReference[] protocolProviderRefs;
        try
        {
            protocolProviderRefs = bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error(
                "Error while retrieving service refs", ex);
            return;
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {
            for (ServiceReference protocolProviderRef : protocolProviderRefs)
            {
                ProtocolProviderService provider
                    = (ProtocolProviderService)
                        bc.getService(protocolProviderRef);

                this.handleProviderRemoved(provider);
            }
        }

        this.started = false;
    }

    /**
     * Used to attach the Alerter plugin to existing or
     * just registered protocol provider. Checks if the provider has implementation
     * of OperationSetBasicInstantMessaging
     *
     * @param provider ProtocolProviderService
     */
    private void handleProviderAdded(ProtocolProviderService provider)
    {
        if (logger.isDebugEnabled())
            logger.debug("Adding protocol provider " + provider.getProtocolName());

        // check whether the provider has a basic im operation set
        OperationSetBasicInstantMessaging opSetIm
            = provider
                .getOperationSet(OperationSetBasicInstantMessaging.class);

        if (opSetIm != null)
        {
            opSetIm.addMessageListener(this);
        }
        else
        {
            if (logger.isTraceEnabled())
                logger.trace("Service did not have a im op. set.");
        }

        // check whether the provider has a sms operation set
        OperationSetSmsMessaging opSetSms
            = provider.getOperationSet(OperationSetSmsMessaging.class);

        if (opSetSms != null)
        {
            opSetSms.addMessageListener(this);
        }
        else
        {
            if (logger.isTraceEnabled())
                logger.trace("Service did not have a sms op. set.");
        }

        OperationSetMultiUserChat opSetMultiUChat
            = provider.getOperationSet(OperationSetMultiUserChat.class);

        if (opSetMultiUChat != null)
        {
            for (ChatRoom room : opSetMultiUChat.getCurrentlyJoinedChatRooms())
                room.addMessageListener(this);

            opSetMultiUChat.addPresenceListener(this);
        }
        else
        {
            if (logger.isTraceEnabled())
                logger.trace("Service did not have a multi im op. set.");
        }

        OperationSetBasicTelephony<?> basicTelephonyOpSet
            = provider.getOperationSet(OperationSetBasicTelephony.class);

        if (basicTelephonyOpSet != null)
        {
            basicTelephonyOpSet.addCallListener(this);
        }
    }

    /**
     * Removes the specified provider from the list of currently known providers
     * and ignores all the messages exchanged by it
     *
     * @param provider the ProtocolProviderService that has been unregistered.
     */
    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        OperationSetBasicInstantMessaging opSetIm
            = provider.getOperationSet(OperationSetBasicInstantMessaging.class);

        if (opSetIm != null)
        {
            opSetIm.removeMessageListener(this);
        }

        OperationSetSmsMessaging opSetSms
            = provider.getOperationSet(OperationSetSmsMessaging.class);

        if (opSetSms != null)
        {
            opSetSms.removeMessageListener(this);
        }

        OperationSetMultiUserChat opSetMultiUChat
            = provider.getOperationSet(OperationSetMultiUserChat.class);

        if (opSetMultiUChat != null)
        {
            for (ChatRoom room : opSetMultiUChat.getCurrentlyJoinedChatRooms())
                room.removeMessageListener(this);
        }

        OperationSetBasicTelephony<?> basicTelephonyOpSet
            = provider.getOperationSet(OperationSetBasicTelephony.class);

        if (basicTelephonyOpSet != null)
        {
            basicTelephonyOpSet.removeCallListener(this);
        }
    }

    /**
     * Called to notify interested parties that a change in our presence in
     * a chat room has occurred. Changes may include us being kicked, join,
     * left.
     * @param ev the <tt>LocalUserChatRoomPresenceChangeEvent</tt> instance
     * containing the chat room and the type, and reason of the change
     */
    public void localUserPresenceChanged(LocalUserChatRoomPresenceChangeEvent ev)
    {
        ChatRoom chatRoom = ev.getChatRoom();

        if(LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED.equals(
                ev.getEventType()))
        {
            if (!chatRoom.isSystem())
                chatRoom.addMessageListener(this);
        }
        else
        {
            chatRoom.removeMessageListener(this);
        }
    }

    public void messageReceived(MessageReceivedEvent evt)
    {
        alertChatWindow();
    }

    public void messageDelivered(MessageDeliveredEvent evt)
    {
        // do nothing
    }

    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
    {
        // do nothing
    }

    public void messageReceived(ChatRoomMessageReceivedEvent evt)
    {
        alertChatWindow();
    }

    public void messageDelivered(ChatRoomMessageDeliveredEvent evt)
    {
        // do nothing
    }

    public void messageDeliveryFailed(ChatRoomMessageDeliveryFailedEvent evt)
    {
        // do nothing
    }

    /**
     * Alerts that a message has been received in
     * <code>ExportedWindow.CHAT_WINDOW</code> by using a platform-dependent
     * visual clue such as flashing it in the task bar on Windows and Linux.
     */
    private void alertChatWindow()
    {
        alertWindow(ExportedWindow.CHAT_WINDOW);
    }

    /**
     * Alerts the <code>windowID</code> by using a platform-dependent
     * visual clue such as flashing it in the task bar on Windows and Linux,
     * or the bouncing the dock icon under macosx.
     */
    private void alertWindow(WindowID windowID)
    {
        try
        {
            ExportedWindow win = getUIService().getExportedWindow(windowID);
            if (win == null)
                return;

            Object winSource = win.getSource();
            if (!(winSource instanceof JFrame))
                return;

            JFrame fr = (JFrame) winSource;

            if(OSUtils.IS_MAC)
                com.apple.eawt.Application.getApplication()
                    .requestUserAttention(true);
            else
                Alerter.newInstance().alert(fr);
        }
        catch (Throwable ex)
        {
            logger.error("Cannot alert chat window!", ex);
        }
    }

    /**
     * When new protocol provider is registered we check
     * does it supports needed Op. Sets and if so add a listener to it
     *
     * @param serviceEvent ServiceEvent
     */
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        Object sService
            = bundleContext.getService(serviceEvent.getServiceReference());

        if (logger.isTraceEnabled())
            logger.trace("Received a service event for: " +
            sService.getClass().getName());

        // we don't care if the source service is not a protocol provider
        if (!(sService instanceof ProtocolProviderService))
            return;

        if (logger.isDebugEnabled())
            logger.debug("Service is a protocol provider.");
        switch (serviceEvent.getType())
        {
        case ServiceEvent.REGISTERED:
            this.handleProviderAdded((ProtocolProviderService)sService);
            break;

        case ServiceEvent.UNREGISTERING:
            this.handleProviderRemoved( (ProtocolProviderService) sService);
            break;
        }
    }

    public void messageDelivered(AdHocChatRoomMessageDeliveredEvent evt)
    {
        // do nothing
    }

    public void messageDeliveryFailed(
            AdHocChatRoomMessageDeliveryFailedEvent evt)
    {
        // do nothing
    }

    public void messageReceived(AdHocChatRoomMessageReceivedEvent evt)
    {
        alertChatWindow();
    }

    /**
     * Called to notify interested parties that a change in our presence in
     * an ad-hoc chat room has occurred. Changes may include us being join,
     * left.
     * @param ev the <tt>LocalUserAdHocChatRoomPresenceChangeEvent</tt>
     * instance containing the ad-hoc chat room and the type, and reason of the
     * change
     */
    public void localUserAdHocPresenceChanged(
            LocalUserAdHocChatRoomPresenceChangeEvent ev)
    {
        AdHocChatRoom adHocChatRoom = ev.getAdHocChatRoom();

        if(LocalUserAdHocChatRoomPresenceChangeEvent.LOCAL_USER_JOINED.equals(
                ev.getEventType()))
        {
            adHocChatRoom.addMessageListener(this);
        }
        else
        {
            ev.getAdHocChatRoom().removeMessageListener(this);
        }
    }

    /**
     * Returns the <tt>UIService</tt> obtained from the bundle
     * context.
     * @return the <tt>UIService</tt> obtained from the bundle
     * context
     */
    public UIService getUIService()
    {
        if(uiService == null)
        {
            ServiceReference serviceRef = bundleContext
                .getServiceReference(UIService.class.getName());

            if (serviceRef != null)
                uiService = (UIService) bundleContext.getService(serviceRef);
        }

        return uiService;
    }

    /**
     * Waits for enable/disable property change.
     * @param evt the event of change
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        if(!evt.getPropertyName()
                    .equals(ConfigurationUtils.ALERTER_ENABLED_PROP))
            return;

        try
        {
            if(ConfigurationUtils.isAlerterEnabled() && !started)
            {
                startInternal(bundleContext);
            }
            else if(!ConfigurationUtils.isAlerterEnabled() && started)
            {
                stopInternal(bundleContext);
            }
        }
        catch(Throwable t)
        {
            logger.error("Error starting/stopping on configuration change");
        }
    }

    /**
     * This method is called by a protocol provider whenever an incoming call is
     * received.
     *
     * @param event a CallEvent instance describing the new incoming call
     */
    public void incomingCallReceived(CallEvent event)
    {
        Call call = event.getSourceCall();

        /*
         * INCOMING_CALL should be dispatched for a Call
         * only while there is a CallPeer in the
         * INCOMING_CALL state.
         */
        Iterator<? extends CallPeer> peerIter = call.getCallPeers();
        boolean alert = false;
        while (peerIter.hasNext())
        {
            CallPeer peer = peerIter.next();
            if (CallPeerState.INCOMING_CALL.equals(peer.getState()))
            {
                alert = true;
                break;
            }
        }

        if(alert)
            alertWindow(ExportedWindow.MAIN_WINDOW);
    }

    /**
     * Not used.
     * @param event a CalldEvent instance describing the new outgoing call.
     */
    public void outgoingCallCreated(CallEvent event)
    {}

    /**
     * Not used
     * @param event the <tt>CallEvent</tt> containing the source call.
     */
    public void callEnded(CallEvent event)
    {}
}
