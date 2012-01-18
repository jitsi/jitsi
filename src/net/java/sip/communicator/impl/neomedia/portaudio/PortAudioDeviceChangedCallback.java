/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.portaudio;

/**
 * Interface to be notified by PortAudio device changed callback.
 * 
 * @author Sebastien Vincent
 */
public interface PortAudioDeviceChangedCallback
{
    /**
     * Callback when PortAudio device changed.
     */
    public void deviceChanged();
}
