/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.beans.*;
import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.systray.event.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

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
    private ConfigurationForm configurationForm;

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
     * Registers or unregister as a popup message listener to detect when a
     * user click on notification saying that the device configuration has
     * changed.
     *
     * @param enable True to register to the popup message notifcation
     * handler.  False to unregister.
     */
    public void managePopupMessageListenerRegistration(boolean enable)
    {
        Iterator<NotificationHandler> notificationHandlers = NeomediaActivator
            .getNotificationService()
            .getActionHandlers(
                    net.java.sip.communicator.service.notification.NotificationAction.ACTION_POPUP_MESSAGE)
            .iterator();
        NotificationHandler notificationHandler;
        while(notificationHandlers.hasNext())
        {
            notificationHandler = notificationHandlers.next();
            if(notificationHandler
                    instanceof PopupMessageNotificationHandler)
            {
                // Register.
                if(enable)
                {
                    ((PopupMessageNotificationHandler) notificationHandler)
                        .addPopupMessageListener(this);
                }
                // Unregister.
                else
                {
                    ((PopupMessageNotificationHandler) notificationHandler)
                        .removePopupMessageListener(this);
                }
            }
        }
    }

    /**
     * Function called when an audio device is plugged or unplugged.
     *
     * @param event The property change event which may concern the audio
     * device.
     */
    public abstract void propertyChange(PropertyChangeEvent event);

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
                // Registers only once to the  popup message notification
                // handler.
                if(!isRegisteredToPopupMessageListener)
                {
                    isRegisteredToPopupMessageListener = true;
                    managePopupMessageListenerRegistration(true);
                }

                // Fires the popup notification.
                Map<String,Object> extras = new HashMap<String,Object>();
                extras.put(
                        NotificationData.POPUP_MESSAGE_HANDLER_TAG_EXTRA,
                        this);

                ResourceManagementService resources
                    = NeomediaActivator.getResources();

                notificationService.fireNotification(
                        popUpEvent,
                        title,
                        body
                        + "\r\n\r\n"
                        + resources.getI18NString(
                            "impl.media.configform"
                            + ".AUDIO_DEVICE_CONFIG_MANAGMENT_CLICK"),
                        null,
                        extras);
            }
        }
    }

    /**
     * Indicates that user has clicked on the systray popup message.
     * 
     * @param evt the event triggered when user clicks on the systray popup
     * message
     */
    public void popupMessageClicked(SystrayPopupMessageEvent evt)
    {
        // Checks if this event is fired from one click on one of our popup
        // message.
        //if(evt.getTag() == audioDeviceConfigurationPropertyChangeListener)
        if(evt.getTag() == this)
        {
            // Get the UI service
            BundleContext bundleContext = NeomediaActivator.getBundleContext();
            ServiceReference uiReference = bundleContext
                .getServiceReference(UIService.class.getName());

            UIService uiService = (UIService) bundleContext
                .getService(uiReference);

            if(uiService != null)
            {
                // Shows the audio configuration window.
                ConfigurationContainer configurationContainer
                    = uiService.getConfigurationContainer();
                configurationContainer.setSelected(
                        this.configurationForm);
                        //NeomediaActivator.getAudioConfigurationForm());
                configurationContainer.setVisible(true);
            }
        }
    }
}
