/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.rtp.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.audiolevel.*;
import net.java.sip.communicator.impl.neomedia.audiolevel.event.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.util.*;

/**
 * Extends <tt>MediaDeviceSession</tt> to add audio-specific functionality.
 *
 * @author Emil Ivov
 * @author Damian Minkov
 */
public class AudioMediaDeviceSession extends MediaDeviceSession
{
    /**
     * Our class logger.
     */
    private Logger logger = Logger.getLogger(AudioMediaDeviceSession.class);

    /**
     * A list of listeners registered for local user sound level events.
     */
    private final List<SoundLevelListener> localUserAudioLevelListeners
        = new Vector<SoundLevelListener>();

    /**
     * A list of listeners registered for stream user sound level events.
     */
    private final List<SoundLevelListener> streamSoundLevelListeners
        = new Vector<SoundLevelListener>();

    /**
     * Mapping between threads dispatching events and received streams.
     * Those threads contain the listeners that are interested for sound level
     * changes of the particular received stream.
     */
    private final Map<ReceiveStream, AudioLevelEventDispatcher>
        streamAudioLevelListeners
            = new Hashtable<ReceiveStream, AudioLevelEventDispatcher>();

    /**
     * Initializes a new <tt>MediaDeviceSession</tt> instance which is to
     * represent the use of a specific <tt>MediaDevice</tt> by a
     * <tt>MediaStream</tt>.
     *
     * @param device the <tt>MediaDevice</tt> the use of which by a
     * <tt>MediaStream</tt> is to be represented by the new instance
     */
    protected AudioMediaDeviceSession(AbstractMediaDevice device)
    {
        super(device);
    }

    /**
     * Adds a specific <tt>SoundLevelListener</tt> to the list of
     * listeners interested in and notified about changes in local sound level
     * related information.
     * @param l the <tt>SoundLevelListener</tt> to add
     */
    public void addLocalUserAudioLevelListener(SoundLevelListener l)
    {
        synchronized(localUserAudioLevelListeners)
        {
            if (!localUserAudioLevelListeners.contains(l))
                localUserAudioLevelListeners.add(l);
        }
    }

    /**
     * Removes a specific <tt>SoundLevelListener</tt> of the list of
     * listeners interested in and notified about changes in local sound level
     * related information.
     * @param l the <tt>SoundLevelListener</tt> to remove
     */
    public void removeLocalUserAudioLevelListener(SoundLevelListener l)
    {
        synchronized(localUserAudioLevelListeners)
        {
            localUserAudioLevelListeners.remove(l);
        }
    }

    /**
     * Creates and dispatches a <tt>SoundLevelEvent</tt> notifying
     * registered listeners that the local user sound level has changed.
     *
     * @param level the new level
     */
    protected void fireLocalUserAudioLevelEvent(int level)
    {
        // If no local ssrc give up
        if(parentStream.getLocalSourceID() == 0)
            return;

        Map<Long,Integer> lev = new HashMap<Long, Integer>();
        lev.put(parentStream.getLocalSourceID(), level);
        SoundLevelChangeEvent soundLevelEvent
            = new SoundLevelChangeEvent(parentStream,lev);

        List<SoundLevelListener> listeners;

        synchronized (localUserAudioLevelListeners)
        {
            listeners =
                new ArrayList<SoundLevelListener>(localUserAudioLevelListeners);
        }

        for (Iterator<SoundLevelListener> listenerIter
                = listeners.iterator(); listenerIter.hasNext();)
        {
            SoundLevelListener listener = listenerIter.next();

            listener.soundLevelChanged(soundLevelEvent);
        }
    }

    /**
     * Creates sound level indicator effect and add it to the codec chain of the
     * <tt>TrackControl</tt> and assumes there is only one audio track.
     *
     * @param tc the track control.
     * @throws UnsupportedPlugInException if we fail to register a sound level
     * effect
     */
    private void registerLocalAudioLevelJMFEffect(TrackControl tc)
            throws UnsupportedPlugInException
    {
        AudioLevelEffect slie = new AudioLevelEffect(
            new SimpleAudioLevelListener()
            {
                public void audioLevelChanged(int level)
                {
                    fireLocalUserAudioLevelEvent(level);
                }
            });
        // Assume there is only one audio track
        tc.setCodecChain(new Codec[]{slie});
    }

    /**
     * Adds sound level indicator to the track and fire events to the listeners
     * from the <tt>MediaStream</tt>.
     *
     * @param mediaStream the media stream
     * @param tc the TrackControl
     *
     * @throws UnsupportedPlugInException if we fail to add our sound level
     * effect to the track control of <tt>mediaStream</tt>'s processor.
     */
    private void registerStreamAudioLevelJMFEffect(MediaStream  mediaStream,
                                                   TrackControl tc)
        throws UnsupportedPlugInException
    {
        if(mediaStream == null
           || !(mediaStream instanceof AudioMediaStreamImpl))
            return;

        final AudioMediaStreamImpl aStream = (AudioMediaStreamImpl)mediaStream;

        if(streamAudioLevelListeners.size() == 0)
            return;

        AudioLevelEffect audioLevelEffect = new AudioLevelEffect(
            new SimpleAudioLevelListener()
            {
                public void audioLevelChanged(int level)
                {
                    fireStreamAudioLevelEvent(level);
                }
            });
        // Assume there is only one audio track
        tc.setCodecChain(new Codec[]{audioLevelEffect});
    }

    /**
     * Adds <tt>listener</tt> to the list of <tt>SoundLevelListener</tt>s
     * registered with this <tt>AudioMediaStream</tt> to receive notifications
     * about changes in the sound levels of the conference participants that the
     * remote party may be mixing.
     *
     * @param receiveStream the received stream for the listener
     * @param listener the <tt>SoundLevelListener</tt> to register with this
     * <tt>AudioMediaStream</tt>
     * @see AudioMediaStream#addSoundLevelListener(SoundLevelListener)
     */
    public void addStreamAudioLevelListener(
        ReceiveStream receiveStream, SimpleAudioLevelListener listener)
    {
        synchronized(streamAudioLevelListeners)
        {
            AudioLevelEventDispatcher audioLevelEventDispatcher =
                streamAudioLevelListeners.get(receiveStream);
            if(audioLevelEventDispatcher == null)
            {
                audioLevelEventDispatcher = new AudioLevelEventDispatcher();
                new Thread(audioLevelEventDispatcher).start();
                streamAudioLevelListeners.put(
                                receiveStream, audioLevelEventDispatcher);
            }

            audioLevelEventDispatcher.addAudioLevelListener(listener);
        }
    }

    /**
     * Removes <tt>listener</tt> from the list of <tt>SoundLevelListener</tt>s
     * registered with this <tt>AudioMediaStream</tt> to receive notifications
     * about changes in the sound levels of the conference participants that the
     * remote party may be mixing.
     *
     * @param receiveStream the received stream for the listener
     * @param listener the <tt>SoundLevelListener</tt> to no longer be notified
     * by this <tt>AudioMediaStream</tt> about changes in the sound levels of
     * the conference participants that the remote party may be mixing
     * @see AudioMediaStream#removeSoundLevelListener(SoundLevelListener)
     */
    public void removeStreamAudioLevelListener(
        ReceiveStream receiveStream, SimpleAudioLevelListener listener)
    {
        synchronized(streamAudioLevelListeners)
        {
            AudioLevelEventDispatcher dispatcher =
                streamAudioLevelListeners.get(receiveStream);
            if(dispatcher != null)
            {
                dispatcher.removeAudioLevelListener(listener);
            }
        }
    }

    /**
     * Fires a <tt>StreamSoundLevelEvent</tt> and notifies all registered
     * listeners.
     *
     * @param level the new sound level
     * @param stream the <tt>ReceiveStream</tt> whose level has just changed.
     */
    protected void fireStreamAudioLevelEvent(ReceiveStream stream, int level)
    {
        // If no remote ssrc give up
        if(stream.getSSRC() == 0)
            return;

        Map<Long,Integer> ssrcLevels = new HashMap<Long, Integer>();
        ssrcLevels.put(parentStream.getLocalSourceID(), level);
        SoundLevelChangeEvent event
            = new SoundLevelChangeEvent(parentStream, ssrcLevels);

        SoundLevelListener[] ls;

        synchronized(streamSoundLevelListeners)
        {
            ls = streamSoundLevelListeners.toArray(
                new SoundLevelListener[streamSoundLevelListeners.size()]);
        }

        for (SoundLevelListener listener : ls)
        {
            listener.soundLevelChanged(event);
        }
    }


    /**
     * Adds a <tt>ReceiveStream</tt> to this <tt>AudioMediaDeviceSession</tt>
     * so that it would be played back on the associated <tt>MediaDevice</tt>
     * and registers an <tt>AudioLevelEffect</tt> so that we would be able to
     * measure levels.
     *
     * @param receiveStream the <tt>ReceiveStream</tt> that is being added to
     * this <tt>AudioMediaDeviceSession</tt>.
     * @param receiveStreamDataSource the <tt>DataSource</tt> to be used for
     * accessing the media data of <tt>receiveStream</tt> during its playback
     */
    protected synchronized void addReceiveStream(
            ReceiveStream receiveStream,
            DataSource receiveStreamDataSource)
    {
        if (receiveStreamDataSource == null)
            return;

        super.addReceiveStream(receiveStream, receiveStreamDataSource);

        //at this point we should already have a processor for receiveStream
        //(unless something has gone wrong) so we can now register our audio
        //level effect.
        Processor player = getPlayer(receiveStreamDataSource);

        if (player == null || player.getState() < Processor.Configured)
        {
            //something must have gone wrong during processor creation in
            //the super ... guess it's not that super after all ;)
            return;
        }

        try
        {
            TrackControl tcs[] = player.getTrackControls();
            if (tcs != null)
            {
                for (TrackControl tc : tcs)
                {
                    if (tc.getFormat() instanceof AudioFormat)
                    {
                        // Assume there is only one audio track
                        registerStreamAudioLevelJMFEffect(parentStream, tc);
                        break;
                    }
                }
            }
        }
        catch (UnsupportedPlugInException ex)
        {
            logger.error("The processor does not support effects", ex);
        }
    }
}
