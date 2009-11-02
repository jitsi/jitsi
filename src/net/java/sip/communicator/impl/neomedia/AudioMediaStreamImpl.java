/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import javax.media.format.*;
import javax.media.rtp.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.util.*;

/**
 * Extends <tt>MediaStreamImpl</tt> in order to provide an implementation of
 * <tt>AudioMediaStream</tt>.
 *
 * @author Lubomir Marinov
 */
public class AudioMediaStreamImpl
    extends MediaStreamImpl
    implements AudioMediaStream
{

    /**
     * The <tt>Logger</tt> used by the <tt>AudioMediaStreamImpl</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AudioMediaStreamImpl.class);

    /**
     * List of RTP format strings which are supported by SIP Communicator in
     * addition to the JMF standard formats.
     * 
     * @see #registerCustomCodecFormats(RTPManager)
     */
    private static final AudioFormat[] CUSTOM_CODEC_FORMATS
        = new AudioFormat[]
                {
                    /*
                     * these formats are specific, since RTP uses format numbers
                     * with no parameters.
                     */
                    new AudioFormat(
                            Constants.ILBC_RTP,
                            8000.0,
                            16,
                            1,
                            AudioFormat.LITTLE_ENDIAN,
                            AudioFormat.SIGNED),
                    new AudioFormat(
                            Constants.ALAW_RTP,
                            8000,
                            8,
                            1,
                            -1,
                            AudioFormat.SIGNED),
                    new AudioFormat(Constants.SPEEX_RTP,
                            8000,
                            8,
                            1,
                            -1,
                            AudioFormat.SIGNED)
                };

    /**
     * JMF stores <tt>CUSTOM_CODEC_FORMATS</tt> statically, so they only need to
     * be registered once. FMJ does this dynamically (per instance), so it needs
     * to be done for every time we instantiate an RTP manager.
     */
    private static boolean formatsRegisteredOnce = false;

    /**
     * Initializes a new <tt>AudioMediaStreamImpl</tt> instance which will use
     * the specified <tt>MediaDevice</tt> for both capture and playback of audio
     * exchanged via the specified <tt>StreamConnector</tt>.
     *
     * @param connector the <tt>StreamConnector</tt> the new instance is to use
     * for sending and receiving audio
     * @param device the <tt>MediaDevice</tt> the new instance is to use for
     * both capture and playback of audio exchanged via the specified
     * <tt>StreamConnector</tt>
     */
    public AudioMediaStreamImpl(StreamConnector connector, MediaDevice device)
    {
        super(connector, device);
    }

    /**
     * Adds a <tt>DTMFListener</tt> to this <tt>AudioMediaStream</tt> which is
     * to receive notifications when the remote party starts sending DTMF tones
     * to us.
     *
     * @param listener the <tt>DTMFListener</tt> to register for notifications
     * about the remote party starting sending of DTM tones to this
     * <tt>AudioMediaStream</tt>
     * @see AudioMediaStream#addDTMFListener(DTMFListener)
     */
    public void addDTMFListener(DTMFListener listener)
    {
        // TODO Auto-generated method stub
    }

    /**
     * Adds <tt>listener</tt> to the list of <tt>SoundLevelListener</tt>s
     * registered with this <tt>AudioMediaStream</tt> to receive notifications
     * about changes in the sound levels of the conference participants that the
     * remote party may be mixing.
     *
     * @param listener the <tt>SoundLevelListener</tt> to register with this
     * <tt>AudioMediaStream</tt>
     * @see AudioMediaStream#addSoundLevelListener(SoundLevelListener)
     */
    public void addSoundLevelListener(SoundLevelListener listener)
    {
        // TODO Auto-generated method stub
    }

    /**
     * Determines whether this <tt>AudioMediaStream</tt> is set to transmit
     * silence instead of the audio being fed from its <tt>MediaDevice</tt>.
     *
     * @return <tt>true</tt> if this <tt>AudioMediaStream</tt> is set to
     * transmit silence instead of the audio fed from its <tt>MediaDevice</tt>;
     * <tt>false</tt>, otherwise
     * @see AudioMediaStream#isMute()
     */
    public boolean isMute()
    {
        return ((AudioCaptureMediaDevice) getDevice()).isMute();
    }

    /**
     * Registers {@link #CUSTOM_CODEC_FORMATS} with a specific
     * <tt>RTPManager</tt>.
     *
     * @param rtpManager the <tt>RTPManager</tt> to register
     * {@link #CUSTOM_CODEC_FORMATS} with
     * @see MediaStreamImpl#registerCustomCodecFormats(RTPManager)
     */
    @Override
    protected void registerCustomCodecFormats(RTPManager rtpManager)
    {
        super.registerCustomCodecFormats(rtpManager);

        // if we have already registered custom formats and we are running JMF
        // we bail out.
        if (!FMJConditionals.REGISTER_FORMATS_WITH_EVERY_RTP_MANAGER
                && formatsRegisteredOnce)
            return;

        for (AudioFormat format : CUSTOM_CODEC_FORMATS)
        {
            logger.debug("registering format " + format + " with RTP manager");

            /*
             * NOTE (mkoch@rowa.de): com.sun.media.rtp.RtpSessionMgr.addFormat
             * leaks memory, since it stores the Format in a static Vector.
             * AFAIK there is no easy way around it, but the memory impact
             * should not be too bad.
             */
            rtpManager
                .addFormat(
                    format,
                    MediaUtils
                        .getRTPPayloadType(
                            format.getEncoding(),
                            format.getSampleRate()));
        }

        formatsRegisteredOnce = true;
    }

    /**
     * Removes <tt>listener</tt> from the list of <tt>DTMFListener</tt>s
     * registered with this <tt>AudioMediaStream</tt> to receive notifications
     * about incoming DTMF tones.
     *
     * @param listener the <tt>DTMFListener</tt> to no longer be notified by
     * this <tt>AudioMediaStream</tt> about incoming DTMF tones
     * @see AudioMediaStream#removeDTMFListener(DTMFListener)
     */
    public void removeDTMFListener(DTMFListener listener)
    {
        // TODO Auto-generated method stub
    }

    /**
     * Removes <tt>listener</tt> from the list of <tt>SoundLevelListener</tt>s
     * registered with this <tt>AudioMediaStream</tt> to receive notifications
     * about changes in the sound levels of the conference participants that the
     * remote party may be mixing.
     *
     * @param listener the <tt>SoundLevelListener</tt> to no longer be notified
     * by this <tt>AudioMediaStream</tt> about changes in the sound levels of
     * the conference participants that the remote party may be mixing
     * @see AudioMediaStream#removeSoundLevelListener(SoundLevelListener)
     */
    public void removeSoundLevelListener(SoundLevelListener listener)
    {
        // TODO Auto-generated method stub
    }

    /**
     * Sets the <tt>MediaDevice</tt> that this stream should use to play back
     * and capture media. Asserts that the specified <tt>device</tt> is an
     * <tt>AudioCaptureMediaDevice</tt> because the implementation of
     * <tt>AudioMediaStreamImpl</tt> depends on it.
     *
     * @param device the <tt>MediaDevice</tt> that this stream should use to
     * play back and capture media
     * @see MediaStreamImpl#setDevice(MediaDevice)
     */
    @Override
    public void setDevice(MediaDevice device)
    {
        super.setDevice((AudioCaptureMediaDevice) device);
    }

    /**
     * Causes this <tt>AudioMediaStream</tt> to stop transmitting the audio
     * being fed from this stream's <tt>MediaDevice</tt> and transmit silence
     * instead.
     *
     * @param mute <tt>true</tt> to have this <tt>AudioMediaStream</tt> transmit
     * silence instead of the actual audio data that it captures from its
     * <tt>MediaDevice</tt>; <tt>false</tt> to transmit actual audio data
     * captured from the <tt>MediaDevice</tt> of this <tt>AudioMediaStream</tt>
     * @see AudioMediaStream#setMute(boolean)
     */
    public void setMute(boolean mute)
    {
        ((AudioCaptureMediaDevice) getDevice()).setMute(mute);
    }

    /**
     * Starts sending the specified <tt>DTMFTone</tt> until the
     * <tt>stopSendingDTMF()</tt> method is called. Callers should keep in mind
     * the fact that calling this method would most likely interrupt all audio
     * transmission until the corresponding stop method is called. Also, calling
     * this method successively without invoking the corresponding stop method
     * between the calls will simply replace the <tt>DTMFTone</tt> from the
     * first call with that from the second.
     *
     * @param tone the <tt>DTMFTone</tt> to start sending
     * @see AudioMediaStream#startSendingDTMF(DTMFTone)
     */
    public void startSendingDTMF(DTMFTone tone)
    {
        // TODO Auto-generated method stub
    }

    /**
     * Interrupts transmission of a <tt>DTMFTone</tt> started with the
     * <tt>startSendingDTMF()</tt> method. Has no effect if no tone is currently
     * being sent.
     *
     * @see AudioMediaStream#stopSendingDTMF()
     */
    public void stopSendingDTMF()
    {
        // TODO Auto-generated method stub
    }
}
