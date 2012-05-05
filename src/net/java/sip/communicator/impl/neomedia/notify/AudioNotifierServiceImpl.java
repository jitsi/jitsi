/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.notify;

import java.net.*;
import java.util.*;
import java.beans.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.service.audionotifier.*;

/**
 * The implementation of the AudioNotifierService.
 *
 * @author Yana Stamcheva
 */
public class AudioNotifierServiceImpl
    implements AudioNotifierService,
               PropertyChangeListener
{
    /**
     * Map of differents audio clips.
     */
    private static final Map<String, SCAudioClipImpl> audioClips =
        new HashMap<String, SCAudioClipImpl>();

    /**
     * If the sound is currently disabled.
     */
    private boolean isMute;

    /**
     * Device config to look for notify device.
     */
    private DeviceConfiguration deviceConfiguration;

    /**
     * Creates audio notify service.
     * @param deviceConfiguration the device configuration.
     */
    public AudioNotifierServiceImpl(DeviceConfiguration deviceConfiguration)
    {
        this.deviceConfiguration = deviceConfiguration;
        deviceConfiguration.addPropertyChangeListener(this);
    }

    /**
     * Creates an SCAudioClip from the given URI and adds it to the list of
     * available audio-s.
     *
     * @param uri the path where the audio file could be found
     * @return a newly created <tt>SCAudioClip</tt> from <tt>uri</tt>
     */
    public SCAudioClipImpl createAudio(String uri)
    {
        SCAudioClipImpl audioClip;

        synchronized (audioClips)
        {
            if(audioClips.containsKey(uri))
            {
                audioClip = audioClips.get(uri);
            }
            else
            {
                URL url =
                    NeomediaActivator.getResources().getSoundURLForPath(uri);

                if (url == null)
                {
                    // Not found by the class loader. Perhaps it's a local file.
                    try
                    {
                        url = new URL(uri);
                    }
                    catch (MalformedURLException e)
                    {
                        //logger.error("The given uri could not be parsed.", e);
                        return null;
                    }
                }

                try
                {
                    AudioSystem audioSystem
                        = getDeviceConfiguration().getAudioSystem();

                    if (audioSystem == null)
                        audioClip = new JavaSoundClipImpl(url, this);
                    else if (NoneAudioSystem.LOCATOR_PROTOCOL.equalsIgnoreCase(
                            audioSystem.getLocatorProtocol()))
                        audioClip = null;
                    else
                    {
                        audioClip
                            = new AudioSystemClipImpl(url, this, audioSystem);
                    }
                }
                catch (Throwable t)
                {
                    if (t instanceof ThreadDeath)
                        throw (ThreadDeath) t;
                    // Cannot create audio to play
                    return null;
                }

                audioClips.put(uri, audioClip);
            }
        }

        return audioClip;
    }

    /**
     * Removes the given audio from the list of available audio clips.
     *
     * @param audioClip the audio to destroy
     */
    public void destroyAudio(SCAudioClip audioClip)
    {
        synchronized (audioClips)
        {
            audioClips.remove(audioClip);
        }
    }

    /**
     * Enables or disables the sound in the application. If FALSE, we try to
     * restore all looping sounds if any.
     *
     * @param isMute when TRUE disables the sound, otherwise enables the sound.
     */
    public void setMute(boolean isMute)
    {
        this.isMute = isMute;

        for (SCAudioClipImpl audioClip : audioClips.values())
        {
            if (isMute)
            {
                audioClip.internalStop();
            }
            else if (audioClip.isLooping())
            {
                audioClip.playInLoop(audioClip.getLoopInterval());
            }
        }
    }

    /**
     * Returns TRUE if the sound is currently disabled, FALSE otherwise.
     * @return TRUE if the sound is currently disabled, FALSE otherwise
     */
    public boolean isMute()
    {
        return isMute;
    }

    /**
     * The device configuration.
     *
     * @return the deviceConfiguration
     */
    public DeviceConfiguration getDeviceConfiguration()
    {
        return deviceConfiguration;
    }

    /**
     * Listens for changes in notify device
     * @param event the event that notify device has changed.
     */
    public void propertyChange(PropertyChangeEvent event)
    {
        if (DeviceConfiguration.AUDIO_NOTIFY_DEVICE.equals(
                event.getPropertyName()))
        {
            audioClips.clear();
        }
    }
}
