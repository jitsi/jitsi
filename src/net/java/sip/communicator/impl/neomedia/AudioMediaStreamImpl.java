/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import javax.media.*;
import javax.media.format.*;
import javax.media.rtp.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
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
    private static final Format[] CUSTOM_CODEC_FORMATS
        = new Format[]
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

    /*
     * Implements AudioMediaStream#addDTMFListener(DTMFListener).
     */
    public void addDTMFListener(DTMFListener listener)
    {
        // TODO Auto-generated method stub
    }

    /*
     * Implements AudioMediaStream#addSoundLevelListener(SoundLevelListener).
     */
    public void addSoundLevelListener(SoundLevelListener listener)
    {
        // TODO Auto-generated method stub
    }

    /*
     * Overrides MediaStreamImpl#registerCustomCodecFormats(RTPManager) in order
     * to register CUSTOM_CODEC_FORMATS.
     */
    @Override
    protected void registerCustomCodecFormats(RTPManager rtpManager)
    {
        // if we have already registered custom formats and we are running JMF
        // we bail out.
        if (!FMJConditionals.REGISTER_FORMATS_WITH_EVERY_RTP_MANAGER
                && formatsRegisteredOnce)
            return;

        for (Format format : CUSTOM_CODEC_FORMATS)
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
                    MediaUtils.jmfToSdpEncoding(format.getEncoding()));
        }

        formatsRegisteredOnce = true;
    }

    /*
     * Implements AudioMediaStream#removeDTMFListener(DTMFListener).
     */
    public void removeDTMFListener(DTMFListener listener)
    {
        // TODO Auto-generated method stub
    }

    /*
     * Implements AudioMediaStream#removeSoundLevelListener(SoundLevelListener).
     */
    public void removeSoundLevelListener(SoundLevelListener listener)
    {
        // TODO Auto-generated method stub
    }

    /*
     * Implements AudioMediaStream#setMute(boolean).
     */
    public void setMute(boolean mute)
    {
        // TODO Auto-generated method stub
    }

    /*
     * Implements AudioMediaStream#startSendingDTMF(DTMFTone).
     */
    public void startSendingDTMF(DTMFTone tone)
    {
        // TODO Auto-generated method stub
    }

    /*
     * Implements AudioMediaStream#stopSendingDTMF().
     */
    public void stopSendingDTMF()
    {
        // TODO Auto-generated method stub
    }
}
