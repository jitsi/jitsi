/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.beans.*;
import java.util.*;
import javax.media.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.systray.event.*;

import org.jitsi.impl.neomedia.device.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * A listener to the click on the popup message concerning audio
 * device configuration changes.
 *
 * @author Vincent Lucas
 */
public class AudioDeviceConfigurationListener
    extends AbstractDeviceConfigurationListener
{
    /**
     * Creates a listener to the click on the popup message concerning audio
     * device configuration changes.
     *
     * @param configurationForm The audio or video configuration form.
     */
    public AudioDeviceConfigurationListener(
            ConfigurationForm configurationForm)
    {
        super(configurationForm);
    }

    /**
     * Function called when an audio device is plugged or unplugged.
     *
     * @param event The property change event which may concern the audio
     * device.
     */
    public void propertyChange(PropertyChangeEvent event)
    {
        String popUpEvent = null;
        String title = null;
        CaptureDeviceInfo device = null;
        ResourceManagementService resources
            = NeomediaActivator.getResources();

        // If the device configuration has changed: a device has been
        // plugged or un-plugged.
        if(DeviceConfiguration.PROP_AUDIO_SYSTEM_DEVICES
                .equals(event.getPropertyName()))
        {
            popUpEvent = NeomediaActivator.DEVICE_CONFIGURATION_HAS_CHANGED;
            // A device has been connected.
            if(event.getNewValue() != null)
            {
                title = resources.getI18NString(
                        "impl.media.configform"
                        + ".AUDIO_DEVICE_CONNECTED");
                device = (CaptureDeviceInfo) event.getNewValue();
            }
            // A device has been disconnected.
            else if(event.getOldValue() != null)
            {
                title = resources.getI18NString(
                        "impl.media.configform"
                        + ".AUDIO_DEVICE_DISCONNECTED");
                device = (CaptureDeviceInfo) event.getOldValue();
            }
        }
        // If a new capture device has been selected.
        else if(CaptureDevices.PROP_DEVICE.equals(event.getPropertyName()))
        {
            if(event.getNewValue() != null)
            {
                popUpEvent = NeomediaActivator.NEW_SELECTED_DEVICE;
                title = resources.getI18NString(
                        "impl.media.configform"
                        + ".AUDIO_DEVICE_SELECTED_AUDIO_IN");
                device = (CaptureDeviceInfo) event.getNewValue();
            }
        }
        // If a new playback device has been selected.
        else if(PlaybackDevices.PROP_DEVICE.equals(event.getPropertyName()))
        {
            if(event.getNewValue() != null)
            {
                popUpEvent = NeomediaActivator.NEW_SELECTED_DEVICE;
                title = resources.getI18NString(
                        "impl.media.configform"
                        + ".AUDIO_DEVICE_SELECTED_AUDIO_OUT");
                device = (CaptureDeviceInfo) event.getNewValue();
            }
        }
        // If a new notify device has been selected.
        else if(NotifyDevices.PROP_DEVICE.equals(event.getPropertyName()))
        {
            if(event.getNewValue() != null)
            {
                popUpEvent = NeomediaActivator.NEW_SELECTED_DEVICE;
                title = resources.getI18NString(
                        "impl.media.configform"
                        + ".AUDIO_DEVICE_SELECTED_AUDIO_NOTIFICATIONS");
                device = (CaptureDeviceInfo) event.getNewValue();
            }
        }

        // Shows the pop-up notification.
        super.showPopUpNotification(title, device, popUpEvent);
    }
}
