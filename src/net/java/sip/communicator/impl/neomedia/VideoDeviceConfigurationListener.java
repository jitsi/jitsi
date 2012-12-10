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
 * A listener to the click on the popup message concerning video
 * device configuration changes.
 *
 * @author Vincent Lucas
 */
public class VideoDeviceConfigurationListener
    extends AbstractDeviceConfigurationListener
{
    /**
     * Creates a listener to the click on the popup message concerning video
     * device configuration changes.
     *
     * @param configurationForm The audio or video configuration form.
     */
    public VideoDeviceConfigurationListener(
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

        // If a new video device has been selected.
        if(DeviceConfiguration.VIDEO_CAPTURE_DEVICE
                .equals(event.getPropertyName()))
        {
            if(event.getNewValue() != null)
            {
                popUpEvent = NeomediaActivator.NEW_SELECTED_DEVICE;
                title = resources.getI18NString(
                        "impl.media.configform"
                        + ".VIDEO_DEVICE_SELECTED");
                device = (CaptureDeviceInfo) event.getNewValue();
            }
        }

        String body = null;
        if(device != null)
        {
            body = device.getName();
        }

        // Shows the pop-up notification.
        this.showPopUpNotification(title, body, popUpEvent);
    }
}
