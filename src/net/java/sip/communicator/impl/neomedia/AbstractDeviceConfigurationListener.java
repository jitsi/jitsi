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
package net.java.sip.communicator.impl.neomedia;

import java.beans.*;
import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.*;

/**
 * An abstract listener to the click on the popup message concerning
 * device configuration changes.
 *
 * @author Vincent Lucas
 */
public abstract class AbstractDeviceConfigurationListener
    implements PropertyChangeListener,
               SystrayPopupMessageListener
{
    /**
     *  The audio or video configuration form.
     */
    private final ConfigurationForm configurationForm;

    /**
     * A boolean used to verify that this listener registers only once to
     * the popup message notification handler.
     */
    private boolean isRegisteredToPopupMessageListener = false;

    /**
     * Creates an abstract listener to the click on the popup message concerning
     * device configuration changes.
     *
     * @param configurationForm The audio or video configuration form.
     */
    public AbstractDeviceConfigurationListener(
            ConfigurationForm configurationForm)
    {
        this.configurationForm = configurationForm;
    }

    /**
     * Adds/removes this instance as a <tt>PopupMessageListener</tt> to/from the
     * <tt>NotificationService</tt> in order to be able to detect when the user
     * clicks on a pop-up notification displayed by this instance.
     *
     * @param add <tt>true</tt> to add this instance as a
     * <tt>PopupMessageListener</tt> to the <tt>NotificationService</tt> or
     * <tt>false</tt> to remove it
     */
    private void addOrRemovePopupMessageListener(boolean add)
    {
        Iterator<NotificationHandler> notificationHandlers
            = NeomediaActivator.getNotificationService()
                    .getActionHandlers(NotificationAction.ACTION_POPUP_MESSAGE)
                        .iterator();

        while(notificationHandlers.hasNext())
        {
            NotificationHandler notificationHandler
                = notificationHandlers.next();

            if(notificationHandler instanceof PopupMessageNotificationHandler)
            {
                PopupMessageNotificationHandler popupMessageNotificationHandler
                    = (PopupMessageNotificationHandler) notificationHandler;

                if(add)
                {
                    popupMessageNotificationHandler.addPopupMessageListener(
                            this);
                }
                else
                {
                    popupMessageNotificationHandler.removePopupMessageListener(
                            this);
                }
            }
        }
    }

    /**
     * Releases the resources acquired by this instance throughout its lifetime,
     * uninstalls the listeners it has installed and, generally, prepares it for
     * garbage collection.
     */
    public void dispose()
    {
        addOrRemovePopupMessageListener(false);
    }

    /**
     * Indicates that user has clicked on the systray popup message.
     *
     * @param ev the event triggered when user clicks on the systray popup
     * message
     */
    public void popupMessageClicked(SystrayPopupMessageEvent ev)
    {
        // Checks if this event is fired from one click on one of our popup
        // message.
        if(ev.getTag() == this)
        {
            // Get the UI service
            UIService uiService
                = ServiceUtils.getService(
                        NeomediaActivator.getBundleContext(),
                        UIService.class);

            if(uiService != null)
            {
                // Shows the audio configuration window.
                ConfigurationContainer configurationContainer
                    = uiService.getConfigurationContainer();

                configurationContainer.setSelected(configurationForm);
                configurationContainer.setVisible(true);
            }
        }
    }

    /**
     * Function called when an audio device is plugged or unplugged.
     *
     * @param ev The property change event which may concern the audio device
     */
    public abstract void propertyChange(PropertyChangeEvent ev);

    /**
     * Shows a pop-up notification corresponding to a device configuration
     * change.
     *
     * @param title The title of the pop-up notification.
     * @param body A body text describing the device modifications.
     * @param popUpEvent The event for a device which has fired this
     * notification: connected, disconnected or selected.
     */
    public void showPopUpNotification(
            String title,
            String body,
            String popUpEvent)
    {
        // Shows the pop-up notification.
        if(title != null && body != null && popUpEvent != null)
        {
            NotificationService notificationService
                = NeomediaActivator.getNotificationService();

            if(notificationService != null)
            {
                // Registers only once to the popup message notification
                // handler.
                if(!isRegisteredToPopupMessageListener)
                {
                    isRegisteredToPopupMessageListener = true;
                    addOrRemovePopupMessageListener(true);
                }

                // Fires the popup notification.
                Map<String,Object> extras = new HashMap<String,Object>();

                extras.put(
                        NotificationData.POPUP_MESSAGE_HANDLER_TAG_EXTRA,
                        this);
                notificationService.fireNotification(
                        popUpEvent,
                        title,
                        body
                        + "\r\n\r\n"
                        + NeomediaActivator.getResources().getI18NString(
                                "impl.media.configform"
                                    + ".AUDIO_DEVICE_CONFIG_MANAGMENT_CLICK"),
                        null,
                        extras);
            }
        }
    }
}
