/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.util.*;
import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.impl.neomedia.transform.dtmf.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.util.*;

/**
 * Extends <tt>MediaStreamImpl</tt> in order to provide an implementation of
 * <tt>AudioMediaStream</tt>.
 *
 * @author Lyubomir Marinov
 * @author Emil Ivov
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
     * The transformer that we use for sending and receiving DTMF packets.
     */
    private DtmfTransformEngine dtmfTransfrmEngine ;

    /**
     * List of DTMF listeners;
     */
    private List<DTMFListener> dtmfListeners = new ArrayList<DTMFListener>();

    /**
     * List of RTP format strings which are supported by SIP Communicator in
     * addition to the JMF standard formats.
     *
     * @see #registerCustomCodecFormats(StreamRTPManager)
     */
    private static final AudioFormat[] CUSTOM_CODEC_FORMATS
        = new AudioFormat[]
                {
                    /*
                     * these formats are specific, since RTP uses format numbers
                     * with no parameters.
                     */
                    new AudioFormat(
                            Constants.ALAW_RTP,
                            8000,
                            8,
                            1,
                            Format.NOT_SPECIFIED,
                            AudioFormat.SIGNED),
                    new AudioFormat(
                            Constants.G722_RTP,
                            8000,
                            Format.NOT_SPECIFIED /* sampleSizeInBits */,
                            1)
                };

    /**
     * The listener that gets notified of changes in the audio level of
     * remote conference participants.
     */
    private CsrcAudioLevelListener csrcAudioLevelListener = null;

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
     * @param srtpControl a control which is already created, used to control
     * the srtp operations.
     */
    public AudioMediaStreamImpl(StreamConnector connector,
                                MediaDevice     device,
                                SrtpControl srtpControl)
    {
        super(connector, device, srtpControl);

        if(logger.isTraceEnabled())
            logger.trace("Created Audio Stream with hashCode " + hashCode());
    }

    /**
     * Performs any optional configuration on the <tt>BufferControl</tt> of the
     * specified <tt>RTPManager</tt> which is to be used as the
     * <tt>RTPManager</tt> of this <tt>MediaStreamImpl</tt>.
     *
     * @param rtpManager the <tt>RTPManager</tt> which is to be used by this
     * <tt>MediaStreamImpl</tt>
     * @param bufferControl the <tt>BufferControl</tt> of <tt>rtpManager</tt> on
     * which any optional configuration is to be performed
     */
    @Override
    protected void configureRTPManagerBufferControl(
            StreamRTPManager rtpManager,
            BufferControl bufferControl)
    {
        /*
         * It appears that if we don't do this managers don't play. You can try
         * some other buffer size to see if you can get better smoothness.
         */
        String bufferLengthStr
            = NeomediaActivator.getConfigurationService()
                    .getString(PROPERTY_NAME_RECEIVE_BUFFER_LENGTH);
        long bufferLength = 100;

        try
        {
            if ((bufferLengthStr != null) && (bufferLengthStr.length() > 0))
                bufferLength = Long.parseLong(bufferLengthStr);
        }
        catch (NumberFormatException nfe)
        {
            logger.warn(
                    bufferLengthStr
                        + " is not a valid receive buffer length/long value",
                    nfe);
        }

        bufferLength = bufferControl.setBufferLength(bufferLength);
        if (logger.isTraceEnabled())
            logger.trace("Set receiver buffer length to " + bufferLength);

        bufferControl.setEnabledThreshold(true);
        bufferControl.setMinimumThreshold(100);
    }

    /**
     * A stub that allows audio oriented streams to create and keep a reference
     * to a <tt>DtmfTransformEngine</tt>.
     *
     * @return a <tt>DtmfTransformEngine</tt> if this is an audio oriented
     * stream and <tt>null</tt> otherwise.
     */
    @Override
    protected DtmfTransformEngine createDtmfTransformEngine()
    {
        if(this.dtmfTransfrmEngine == null)
            this.dtmfTransfrmEngine = new DtmfTransformEngine(this);

        return this.dtmfTransfrmEngine;
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
        if(!dtmfListeners.contains(listener))
        {
            dtmfListeners.add(listener);
        }
    }

    /**
     * Sets <tt>listener</tt> as the <tt>SimpleAudioLevelListener</tt>
     * registered to receive notifications from our device session for changes
     * in the levels of the party that's at the other end of this stream.
     *
     * @param listener the <tt>SimpleAudioLevelListener</tt> that we'd like to
     * register or <tt>null</tt> if we want to stop stream audio level
     * measurements.
     */
    public void setStreamAudioLevelListener(SimpleAudioLevelListener listener)
    {
        getDeviceSession().setStreamAudioLevelListener(listener);
    }

    /**
     * Registers <tt>listener</tt> as the <tt>CsrcAudioLevelListener</tt> that
     * will receive notifications for changes in the levels of conference
     * participants that the remote party could be mixing.
     *
     * @param listener the <tt>CsrcAudioLevelListener</tt> that we'd like to
     * register or <tt>null</tt> if we'd like to stop receiving notifications.
     */
    public void setCsrcAudioLevelListener(CsrcAudioLevelListener listener)
    {
        this.csrcAudioLevelListener = listener;
    }

    /**
     * Registers {@link #CUSTOM_CODEC_FORMATS} with a specific
     * <tt>RTPManager</tt>.
     *
     * @param rtpManager the <tt>RTPManager</tt> to register
     * {@link #CUSTOM_CODEC_FORMATS} with
     * @see MediaStreamImpl#registerCustomCodecFormats(StreamRTPManager)
     */
    @Override
    protected void registerCustomCodecFormats(StreamRTPManager rtpManager)
    {
        super.registerCustomCodecFormats(rtpManager);

        for (AudioFormat format : CUSTOM_CODEC_FORMATS)
        {
            if (logger.isDebugEnabled())
                logger.debug("registering format " + format +
                        " with RTP manager");

            /*
             * NOTE (mkoch@rowa.de): com.sun.media.rtp.RtpSessionMgr.addFormat
             * leaks memory, since it stores the Format in a static Vector.
             * AFAIK there is no easy way around it, but the memory impact
             * should not be too bad.
             */
            rtpManager.addFormat( format,
                        MediaUtils.getRTPPayloadType(
                            format.getEncoding(), format.getSampleRate()));
        }
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
        dtmfListeners.remove(listener);
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
        if(dtmfTransfrmEngine == null)
            return;

        dtmfTransfrmEngine.startSending(tone);
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
        if(dtmfTransfrmEngine == null)
            return;

        dtmfTransfrmEngine.stopSendingDTMF();
    }

    /**
     * In addition to calling
     * {@link MediaStreamImpl#addRTPExtension(byte, RTPExtension)}
     * this method enables sending of CSRC audio levels. The reason we are
     * doing this here rather than in the super class is that CSRC levels only
     * make sense for audio streams so we don't want them enabled in any other
     * kind.
     *
     * @param extensionID the ID assigned to <tt>rtpExtension</tt> for the
     * lifetime of this stream.
     * @param rtpExtension the RTPExtension that is being added to this stream.
     */
    @Override
    public void addRTPExtension(byte extensionID, RTPExtension rtpExtension)
    {
        super.addRTPExtension(extensionID, rtpExtension);

        if ( RTPExtension.CSRC_AUDIO_LEVEL_URN
                        .equals(rtpExtension.getURI().toString()))
        {
            getCsrcEngine().setCsrcAudioLevelAudioLevelExtensionID(
                            extensionID, rtpExtension.getDirection());
        }
    }

    /**
     * Sets <tt>listener</tt> as the <tt>SimpleAudioLevelListener</tt>
     * registered to receive notifications from our device session for changes
     * in the levels of the audio that this stream is sending out.
     *
     * @param listener the <tt>SimpleAudioLevelListener</tt> that we'd like to
     * register or <tt>null</tt> if we want to stop local audio level
     * measurements.
     */
    public void setLocalUserAudioLevelListener(
                                            SimpleAudioLevelListener listener)
    {
        getDeviceSession().setLocalUserAudioLevelListener(listener);
    }

    /**
     * Returns the <tt>MediaDeviceSession</tt> associated with this stream
     * after first casting it to <tt>AudioMediaDeviceSession</tt> since this is,
     * after all, an <tt>AudioMediaStreamImpl</tt>.
     *
     * @return the <tt>AudioMediaDeviceSession</tt> associated with this stream.
     */
    @Override
    public AudioMediaDeviceSession getDeviceSession()
    {
        return (AudioMediaDeviceSession)super.getDeviceSession();
    }

    /**
     * Returns the last audio level that was measured by the underlying device
     * session for the specified <tt>ssrc</tt> (where <tt>ssrc</tt> could also
     * correspond to our local sync source identifier).
     *
     * @param ssrc the SSRC ID whose last measured audio level we'd like to
     * retrieve.
     *
     * @return the audio level that was last measured for the specified
     * <tt>ssrc</tt> or <tt>-1</tt> if no level has been cached for that ID.
     */
    public int getLastMeasuredAudioLevel(long ssrc)
    {
        AudioMediaDeviceSession devSession = getDeviceSession();

        if (devSession == null)
            return -1;

        if ( ssrc == getLocalSourceID() )
            return devSession.getLastMeasuredLocalUserAudioLevel();
        else
            return devSession.getLastMeasuredAudioLevel(ssrc);
    }

    /**
     * Delivers the <tt>audioLevels</tt> map to whoever's interested. This
     * method is meant for use primarily by the transform engine handling
     * incoming RTP packets (currently <tt>CsrcTransformEngine</tt>).
     *
     * @param audioLevels a array mapping CSRC IDs to audio levels in
     * consecutive elements.
     */
    public void fireConferenceAudioLevelEvent(final long[] audioLevels)
    {
        CsrcAudioLevelListener csrcAudioLevelListener
            = this.csrcAudioLevelListener;

        if (csrcAudioLevelListener != null)
            csrcAudioLevelListener.audioLevelsReceived(audioLevels);
    }

    /**
     * Delivers the <tt>DTMF</tt> tones. This
     * method is meant for use primarily by the transform engine handling
     * incoming RTP packets (currently <tt>DtmfTransformEngine</tt>).
     *
     * @param tone the new tone
     * @param end is end or start of tone.
     */
    public void fireDTMFEvent(DTMFTone tone, boolean end)
    {
        Iterator<DTMFListener> iter = dtmfListeners.iterator();
        DTMFToneEvent ev = new DTMFToneEvent(this, tone);
        while (iter.hasNext())
        {
            DTMFListener listener = iter.next();
            if(end)
                listener.dtmfToneReceptionEnded(ev);
            else
                listener.dtmfToneReceptionStarted(ev);
        }
    }

    /**
     * Releases the resources allocated by this instance in the course of its
     * execution and prepares it to be garbage collected.
     *
     * @see MediaStream#close()
     */
    @Override
    public void close()
    {
        super.close();

        if(dtmfTransfrmEngine != null)
        {
           dtmfTransfrmEngine.stop();
           dtmfTransfrmEngine = null;
        }
    }

    /**
     * The priority of the audio is 3, which is meant to be higher than
     * other threads and higher than the video one.
     * @return audio priority.
     */
    @Override
    protected int getPriority()
    {
        return 3;
    }
}
