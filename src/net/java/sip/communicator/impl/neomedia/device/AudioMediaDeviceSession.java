/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;


import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.rtp.*;

import net.java.sip.communicator.impl.neomedia.audiolevel.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.util.*;

/**
 * Extends <tt>MediaDeviceSession</tt> to add audio-specific functionality.
 *
 * @author Emil Ivov
 * @author Damian Minkov
 */
public class AudioMediaDeviceSession
    extends MediaDeviceSession
{
    /**
     * Our class logger.
     */
    private Logger logger = Logger.getLogger(AudioMediaDeviceSession.class);

    /**
     * The listener (possibly the stream that created this session) that we
     * should be notifying every time we detect a change in the level of the
     * audio that the local user is generating.
     */
    private SimpleAudioLevelListener localUserAudioLevelListener = null;

    /**
     * The listener (possibly the stream that created this session) that we
     * should be notifying every time we detect a change in the level of the
     * audio that the remote user is sending to us.
     */
    private SimpleAudioLevelListener streamAudioLevelListener = null;

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
     * Sets the  <tt>SimpleAudioLevelListener</tt> that this session should be
     * notifying about changes in local audio level related information. This
     * class only supports a single listener for audio changes per source
     * (i.e. stream or data source). Audio changes are generally quite time
     * intensive (~ 50 per second) so we are doing this in order to reduce the
     * number of objects associated with the process (such as event instances
     * listener list iterators and sync copies).
     *
     * @param l the <tt>SimpleAudioLevelListener</tt> to add
     */
    public void setLocalUserAudioLevelListener(SimpleAudioLevelListener l)
    {
        this.localUserAudioLevelListener = l;
    }

    /**
     * Returns the  <tt>SimpleAudioLevelListener</tt> that this session is
     * notifying about changes in local audio level related information. This
     * class only supports a single listener for audio changes per source
     * (i.e. stream or data source). Audio changes are generally quite time
     * intensive (~ 50 per second) so we are doing this in order to reduce the
     * number of objects associated with the process (such as event instances
     * listener list iterators and sync copies).
     *
     * @return the <tt>SimpleAudioLevelListener</tt> that this session is
     * currently notifying for changes in the audio level of the local user.
     */
    public SimpleAudioLevelListener getLocalUserAudioLevelListener()
    {
        return localUserAudioLevelListener;
    }

    /**
     * Delivers <tt>level</tt> to the <tt>SimpleAudioLevelListener</tt> handling
     * local user levels for this class or ignores it if that listener has
     * not been set yet.
     *
     * @param level the new level that we need to pass to our local user level
     * listener if it exists
     */
    protected void fireLocalUserAudioLevelEvent(int level)
    {
        if (localUserAudioLevelListener != null)
        {
            localUserAudioLevelListener.audioLevelChanged(level);
        }
    }

    /**
     * Creates an audio level effect and add its to the codec chain of the
     * <tt>TrackControl</tt> assuming that it only contains a single track.
     *
     * @param tc the track control that we need to register a level effect with.
     * @throws UnsupportedPlugInException if we <tt>tc</tt> does not support
     * effects.
     */
    private void registerLocalAudioLevelJMFEffect(TrackControl tc)
            throws UnsupportedPlugInException
    {
        AudioLevelEffect levelEffect = new AudioLevelEffect(
            new SimpleAudioLevelListener()
            {
                public void audioLevelChanged(int level)
                {
                    fireLocalUserAudioLevelEvent(level);
                }
            });
        // Assume there is only one audio track
        tc.setCodecChain(new Codec[]{levelEffect});
    }

    /**
     * Sets <tt>listener</tt> as the <tt>SimpleAudioLevelListener</tt> that we
     * are going to notify every time a change occurs in the audio level of
     * the media that this device session is receiving from the remote party.
     * This class only supports a single listener for audio changes per source
     * (i.e. stream or data source). Audio changes are generally quite time
     * intensive (~ 50 per second) so we are doing this in order to reduce the
     * number of objects associated with the process (such as event instances
     * listener list iterators and sync copies).
     *
     * @param listener the <tt>SimpleAudioLevelListener</tt> that we want
     * notified for audio level changes in the remote participant's media.
     */
    public void setStreamAudioLevelListener(SimpleAudioLevelListener listener)
    {
        this.streamAudioLevelListener = listener;
    }

    /**
     * Returns the  <tt>SimpleAudioLevelListener</tt> that this session is
     * notifying about changes in the audio level of media we receive from the
     * stream associated with this session. This class only supports a single
     * listener for audio changes per source (i.e. stream or data source).
     * Audio changes are generally quite time intensive (~ 50 per second) so we
     * are doing this in order to reduce the number of objects associated with
     * the process (such as event instances listener list iterators and sync
     * copies).
     *
     * @return the <tt>SimpleAudioLevelListener</tt> that this session is
     * currently notifying for changes in the audio level of the stream
     * associated with this session.
     */
    public SimpleAudioLevelListener getStreamAudioLevelListener()
    {
        return streamAudioLevelListener;
    }

    /**
     * Notifies all registered listeners that the audio level of the stream
     * that this device session is receiving has changed.
     *
     * @param level the new sound level
     */
    protected void fireStreamAudioLevelEvent(int level)
    {
        this.streamAudioLevelListener.audioLevelChanged(level);
    }

    /**
     * Adds an audio level effect to the tracks of the specified
     * <tt>trackControl</tt> and so that we would notify interested listeners
     * of audio level changes.
     *
     * @param trackControl the <tt>TrackControl</tt> where we need to register
     * a level effect that would measure the audio levels of the
     * <tt>ReceiveStream</tt> associated with this class.
     *
     * @throws UnsupportedPlugInException if we fail to add our sound level
     * effect to the track control of <tt>mediaStream</tt>'s processor.
     */
    private void registerStreamAudioLevelJMFEffect(TrackControl trackControl)
        throws UnsupportedPlugInException
    {
        AudioLevelEffect audioLevelEffect = new AudioLevelEffect(
            new SimpleAudioLevelListener()
            {
                public void audioLevelChanged(int level)
                {
                    fireStreamAudioLevelEvent(level);
                }
            });
        // Assume there is only one audio track
        trackControl.setCodecChain(new Codec[]{audioLevelEffect});
    }

    /**
     * Adds a <tt>ReceiveStream</tt> to this <tt>AudioMediaDeviceSession</tt>
     * so that it would be played back on the associated <tt>MediaDevice</tt>
     * and registers an <tt>AudioLevelEffect</tt> so that we would be able to
     * measure its audio level and report changes.
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
                        registerStreamAudioLevelJMFEffect(tc);
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

    /**
     * Gets notified about <tt>ControllerEvent</tt>s generated by the
     * processor reading our capture data source, calls the corresponding
     * method from the parent class so that it would initialize the processor
     * and then adds the level effect for the local user audio levels.
     *
     * @param event the <tt>ControllerEvent</tt> specifying the
     * <tt>Controller</tt> which is the source of the event and the very type of
     * the event
     */
    protected void processorControllerUpdate(ControllerEvent event)
    {
        super.processorControllerUpdate(event);

        if (event instanceof ConfigureCompleteEvent)
        {
            Processor processor = (Processor) event.getSourceController();

            if (processor != null)
            {
                if (localUserAudioLevelListener == null)
                    return;
                // here we add sound level indicator for captured media
                // from the microphone if there are interested listeners
                try
                {
                    TrackControl tcs[] = processor.getTrackControls();

                    if (tcs != null)
                    {
                        for (TrackControl tc : tcs)
                        {
                            if (tc.getFormat() instanceof AudioFormat)
                            {
                                registerLocalAudioLevelJMFEffect(tc);
                                // we assume a single track
                                break;
                            }
                        }
                    }
                }
                catch (UnsupportedPlugInException ex)
                {
                    logger.error(
                        "Effects are not supported by the datasource.", ex);
                }
            }
        }
    }
}
