/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media;

import java.util.*;

import javax.media.*;
import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.media.device.*;

/**
 * @author Lubomir Marinov
 */
public class DeviceConfigurationComboBoxModel
    implements ComboBoxModel
{
    public static class CaptureDevice
    {
        public static boolean equals(CaptureDeviceInfo a, CaptureDeviceInfo b)
        {
            return (a == null) ? (b == null) : a.equals(b);
        }

        public final CaptureDeviceInfo info;

        public CaptureDevice(CaptureDeviceInfo info)
        {
            this.info = info;
        }

        public String toString()
        {
            if (info == null)
                return MediaActivator.getResources().getI18NString(
                    "impl.media.configform.NO_DEVICE");
            return info.getName();
        }
    }

    public static final int AUDIO = 1;

    public static final int VIDEO = 2;

    private final DeviceConfiguration deviceConfiguration;

    private CaptureDevice[] devices;

    private final List<ListDataListener> listeners =
        new ArrayList<ListDataListener>();

    private final int type;

    public DeviceConfigurationComboBoxModel(
        DeviceConfiguration deviceConfiguration, int type)
    {
        if (deviceConfiguration == null)
            throw new IllegalArgumentException("deviceConfiguration");
        if ((type != AUDIO) && (type != VIDEO))
            throw new IllegalArgumentException("type");

        this.deviceConfiguration = deviceConfiguration;
        this.type = type;
    }

    public void addListDataListener(ListDataListener listener)
    {
        if (listener == null)
            throw new IllegalArgumentException("listener");

        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    protected void fireContentsChanged(int index0, int index1)
    {
        ListDataListener[] listeners =
            this.listeners.toArray(new ListDataListener[this.listeners.size()]);
        ListDataEvent event =
            new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index0,
                index1);

        for (ListDataListener listener : listeners)
        {
            listener.contentsChanged(event);
        }
    }

    private DeviceConfiguration getDeviceConfiguration()
    {
        return deviceConfiguration;
    }

    private CaptureDevice[] getDevices()
    {
        if (devices != null)
            return devices;

        DeviceConfiguration deviceConfiguration = getDeviceConfiguration();
        CaptureDeviceInfo[] infos;
        switch (type)
        {
        case AUDIO:
            infos = deviceConfiguration.getAvailableAudioCaptureDevices();
            break;
        case VIDEO:
            infos = deviceConfiguration.getAvailableVideoCaptureDevices();
            break;
        default:
            throw new IllegalStateException("type");
        }

        final int deviceCount = infos.length;
        devices = new CaptureDevice[deviceCount + 1];
        for (int i = 0; i < deviceCount; i++)
        {
            devices[i] = new CaptureDevice(infos[i]);
        }
        devices[deviceCount] = new CaptureDevice(null);
        return devices;
    }

    public Object getElementAt(int index)
    {
        return getDevices()[index];
    }

    private CaptureDevice getSelectedDevice()
    {
        DeviceConfiguration deviceConfiguration = getDeviceConfiguration();
        CaptureDeviceInfo info;
        switch (type)
        {
        case AUDIO:
            info = deviceConfiguration.getAudioCaptureDevice();
            break;
        case VIDEO:
            info = deviceConfiguration.getVideoCaptureDevice();
            break;
        default:
            throw new IllegalStateException("type");
        }

        for (CaptureDevice device : getDevices())
        {
            if (CaptureDevice.equals(device.info, info))
                return device;
        }
        return null;
    }

    public Object getSelectedItem()
    {
        return getSelectedDevice();
    }

    public int getSize()
    {
        return getDevices().length;
    }

    public void removeListDataListener(ListDataListener listener)
    {
        if (listener == null)
            throw new IllegalArgumentException("listener");

        listeners.remove(listener);
    }

    private void setSelectedDevice(CaptureDevice device)
    {
        // We cannot clear the selection of DeviceConfiguration.
        if (device == null)
            return;

        CaptureDevice selectedDevice = getSelectedDevice();
        if (selectedDevice != device)
        {
            DeviceConfiguration deviceConfiguration = getDeviceConfiguration();
            switch (type)
            {
            case AUDIO:
                deviceConfiguration.setAudioCaptureDevice(device.info);
                break;
            case VIDEO:
                deviceConfiguration.setVideoCaptureDevice(device.info);
                break;
            }

            fireContentsChanged(-1, -1);
        }
    }

    public void setSelectedItem(Object item)
    {
        setSelectedDevice((CaptureDevice) item);
    }
}
