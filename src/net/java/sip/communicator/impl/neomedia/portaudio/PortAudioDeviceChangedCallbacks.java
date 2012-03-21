/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.portaudio;

import java.util.*;

/**
 *
 * @author Lyubomir Marinov
 * @author Sebastien Vincent
 */
public class PortAudioDeviceChangedCallbacks
{
    /**
     * List of device changed callbacks.
     */
    public static final List<PortAudioDeviceChangedCallback> callbacks =
        new Vector<PortAudioDeviceChangedCallback>();

    /**
     * Adds a device changed callback.
     *
     * @param cb callback that will be called if device are added/removed
     */
    public static void addDeviceChangedCallback(
        PortAudioDeviceChangedCallback cb)
    {
        if(!callbacks.contains(cb))
            callbacks.add(cb);
    }

    static void deviceChanged()
    {
        for(PortAudioDeviceChangedCallback callback : callbacks)
            callback.deviceChanged();
    }

    /**
     * Removes a device changed callback.
     *
     * @param cb callback that will be called if device are added/removed
     */
    public static void removeDeviceChangedCallback(
        PortAudioDeviceChangedCallback cb)
    {
        if(callbacks.contains(cb))
            callbacks.remove(cb);
    }
}
