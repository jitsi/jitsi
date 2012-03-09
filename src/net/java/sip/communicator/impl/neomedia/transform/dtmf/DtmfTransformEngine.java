/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform.dtmf;

import javax.media.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.transform.*;
import net.java.sip.communicator.service.neomedia.*;

/**
 * The class is responsible for sending DTMF tones in an RTP audio stream as
 * described by RFC4733.
 *
 * @author Emil Ivov
 * @author Romain Philibert
 * @author Damian Minkov
 */
public class DtmfTransformEngine
    implements TransformEngine,
               PacketTransformer
{

    /**
     * The <tt>AudioMediaStreamImpl</tt> that this transform engine was created
     * by and that it's going to deliver DTMF packets for.
     */
    private final AudioMediaStreamImpl mediaStream;

    /**
     * The enumeration contains a set of states that reflect the progress of
     */
    private enum ToneTransmissionState
    {
        /**
         * Indicates that this engine is not currently sending DTMF.
         */
        IDLE,

        /**
         * Indicates that the user has just called the {@link
         * #startSending(DTMFRtpTone)} method and we haven't yet sent any of the
         * packets corresponding to that particular tone.
         */
        SEND_PENDING,

        /**
         * Indicates that we are currently in the process of sending a DTMF
         * tone, and we have already sent at least one packet.
         */
        SENDING,

        /**
         * Indicates that the user has requested that DTMF transmission be
         * stopped but we haven't acted upon that request yet (i.e. we have yet
         * to send a single retransmission)
         */
        END_REQUESTED,

        /**
         * Indicates that the user has requested that DTMF transmission be
         * stopped we have already sent a retransmission of the final packet.
         */
        END_SEQUENCE_INITIATED
    };

    /**
     * Array of all supported tones.
     */
    private static final DTMFRtpTone[] supportedTones =
        new DTMFRtpTone[]
        {DTMFRtpTone.DTMF_0, DTMFRtpTone.DTMF_1, DTMFRtpTone.DTMF_2,
            DTMFRtpTone.DTMF_3, DTMFRtpTone.DTMF_4, DTMFRtpTone.DTMF_5,
            DTMFRtpTone.DTMF_6, DTMFRtpTone.DTMF_7, DTMFRtpTone.DTMF_8,
            DTMFRtpTone.DTMF_9, DTMFRtpTone.DTMF_A, DTMFRtpTone.DTMF_B,
            DTMFRtpTone.DTMF_C, DTMFRtpTone.DTMF_D, DTMFRtpTone.DTMF_SHARP,
            DTMFRtpTone.DTMF_STAR};

    /**
     * The dispatcher that is delivering tones to the media steam.
     */
    private DTMFDispatcher dtmfDispatcher = null;

    /**
     * The status that this engine is currently in.
     */
    private ToneTransmissionState toneTransmissionState
                                                = ToneTransmissionState.IDLE;

    /**
     * The tone that we are supposed to be currently transmitting.
     */
    private DTMFRtpTone currentTone = null;

    /**
     * The duration (in timestamp units or in other words ms*8) that we have
     * transmitted the current tone for.
     */
    private int currentDuration = 0;

    /**
     * The current transmitting timestamp.
     */
    private long currentTimestamp = 0;

    /**
     * We send 3 end packets and this is the counter of remaining packets.
     */
    private int remainingsEndPackets = 0;

    /**
     * Current duration of every event we send.
     */
    private int currentSpacingDuration = Format.NOT_SPECIFIED;

    /**
     * Creates an engine instance that will be replacing audio packets
     * with DTMF ones upon request.
     *
     * @param stream the <tt>AudioMediaStream</tt> whose RTP packets we are
     * going to be replacing with DTMF.
     */
    public DtmfTransformEngine(AudioMediaStreamImpl stream)
    {
        this.mediaStream = stream;
    }

    /**
     * Gets the current duration of every event we send.
     *
     * @return the current duration of every event we send
     */
    private int getCurrentSpacingDuration()
    {
        if (currentSpacingDuration == Format.NOT_SPECIFIED)
        {
            // the default is 50 ms. RECOMMENDED in rfc4733.
            currentSpacingDuration
                = (int) mediaStream.getFormat().getClockRate()/50;
        }
        return currentSpacingDuration;
    }
    /**
     * Always returns <tt>null</tt> since this engine does not require any
     * RTCP transformations.
     *
     * @return <tt>null</tt> since this engine does not require any
     * RTCP transformations.
     */
    public PacketTransformer getRTCPTransformer()
    {
        return null;
    }

    /**
     * Returns a reference to this class since it is performing RTP
     * transformations in here.
     *
     * @return a reference to <tt>this</tt> instance of the
     * <tt>DtmfTransformEngine</tt>.
     */
    public PacketTransformer getRTPTransformer()
    {
        return this;
    }

    /**
     * A stub meant to handle incoming DTMF packets.
     *
     * @param pkt an incoming packet that we need to parse and handle in case
     * we determine it to be DTMF.
     *
     * @return the <tt>pkt</tt> if it is not a DTMF tone and <tt>null</tt>
     * otherwise since we will be handling the packet ourselves and their's
     * no point in feeding it to the application.
     */
    public RawPacket reverseTransform(RawPacket pkt)
    {
        byte currentDtmfPayload = mediaStream.getDynamicRTPPayloadType(
                        Constants.TELEPHONE_EVENT);

        if(currentDtmfPayload == pkt.getPayloadType())
        {
            DtmfRawPacket p = new DtmfRawPacket(pkt);

            if (dtmfDispatcher == null)
            {
                dtmfDispatcher = new DTMFDispatcher();
                new Thread(dtmfDispatcher).start();
            }
            dtmfDispatcher.addTonePacket(p);

            // ignore received dtmf packets
            // if jmf receive change in rtp payload stops reception
            return null;
        }

        return pkt;
    }

    /**
     * Replaces <tt>pkt</tt> with a DTMF packet if this engine is in a DTMF
     * transmission mode or returns it unchanged otherwise.
     *
     * @param pkt the audio packet that we may want to replace with a DTMF one.
     *
     * @return <tt>pkt</tt> with a DTMF packet if this engine is in a DTMF
     * transmission mode or returns it unchanged otherwise.
     */
    public RawPacket transform(RawPacket pkt)
    {
        if (this.toneTransmissionState.equals(ToneTransmissionState.IDLE)
             || currentTone == null)
        {
            return pkt;
        }

        byte currentDtmfPayload = mediaStream.getDynamicRTPPayloadType(
                        Constants.TELEPHONE_EVENT);

        if ( currentDtmfPayload == -1 )
            throw new IllegalStateException("Can't send DTMF when no payload "
                            +"type has been negotiated for DTMF events.");

        DtmfRawPacket dtmfPkt = new DtmfRawPacket(pkt.getBuffer(),
                        pkt.getOffset(), currentDtmfPayload);

        long audioPacketTimestamp = dtmfPkt.getTimestamp();
        boolean pktEnd = false;
        boolean pktMarker = false;
        int pktDuration = 0;

        if(toneTransmissionState == ToneTransmissionState.SEND_PENDING)
        {
            currentDuration = 0;
            currentDuration += getCurrentSpacingDuration();
            pktDuration = currentDuration;

            pktMarker = true;
            currentTimestamp = audioPacketTimestamp;

            toneTransmissionState = ToneTransmissionState.SENDING;
        }
        else if(toneTransmissionState == ToneTransmissionState.SENDING)
        {
            currentDuration += getCurrentSpacingDuration();
            pktDuration = currentDuration;
            // Check for long state event
            if (currentDuration > 0xFFFF)
            {
                 // When duration > 0xFFFF we first send a packet with
                 // duration = 0xFFFF. For the next packet, the duration
                 // start from begining but the audioPacketTimestamp is set to the
                 // time when the long duration event occurs.
                pktDuration = 0xFFFF;
                currentDuration = 0;
                currentTimestamp = audioPacketTimestamp;
            }
        }
        else if(toneTransmissionState == ToneTransmissionState.END_REQUESTED)
        {
            // The first ending packet do have the End flag set.
            // But the 2 next will have the End flag set.
            //
            // The audioPacketTimestamp and the duration field stay unchanged for
            // the 3 last packets
            currentDuration += getCurrentSpacingDuration();
            pktDuration = currentDuration;

            pktEnd = true;
            remainingsEndPackets = 2;

            toneTransmissionState = ToneTransmissionState.END_SEQUENCE_INITIATED;
        }
        else if(toneTransmissionState == ToneTransmissionState.END_SEQUENCE_INITIATED)
        {
            pktEnd = true;
            pktDuration = currentDuration;
            remainingsEndPackets--;

            if(remainingsEndPackets == 0)
                toneTransmissionState = ToneTransmissionState.IDLE;
        }

        dtmfPkt.init(
            currentTone.getCode(),
            pktEnd,
            pktMarker,
            pktDuration,
            currentTimestamp);
        pkt = dtmfPkt;

        return pkt;
    }


    /**
     * DTMF sending stub: this is where we should set the transformer in the
     * proper state so that it would start replacing packets with dtmf codes.
     *
     * @param tone the tone that we'd like to start sending.
     */
    public void startSending(DTMFRtpTone tone)
    {
        if(toneTransmissionState != ToneTransmissionState.IDLE)
            throw new IllegalStateException(
                "Calling start before stopping previous transmission");

        currentTone = tone;
        toneTransmissionState = ToneTransmissionState.SEND_PENDING;
    }

    /**
     * Interrupts transmission of a <tt>DTMFRtpTone</tt> started with the
     * <tt>startSendingDTMF()</tt> method. Has no effect if no tone is currently
     * being sent.
     *
     * @see AudioMediaStream#stopSendingDTMF(DTMFMethod dtmfMethod)
     */
    public void stopSendingDTMF()
    {
        toneTransmissionState = ToneTransmissionState.END_REQUESTED;
    }

    /**
     * Stops threads that this transform engine is using for even delivery.
     */
    public void stop()
    {
        if(dtmfDispatcher != null)
            dtmfDispatcher.stop();
    }

    /**
     * A simple thread that waits for new tones to be reported from incoming
     * RTP packets and then delivers them to the <tt>AudioMediaStream</tt>
     * associated with this engine. The reason we need to do this in a separate
     * thread is of course the time sensitive nature of incoming RTP packets.
     */
    private class DTMFDispatcher
        implements Runnable
    {
        /** Indicates whether this thread is supposed to be running */
        private boolean isRunning = false;

        /** The tone that we last received from the reverseTransform thread*/
        private DTMFRtpTone lastReceivedTone = null;

        /** The tone that we last received from the reverseTransform thread*/
        private DTMFRtpTone lastReportedTone = null;

        /**
         * Have we received end of the currently started tone.
         */
        private boolean toEnd = false;

        /**
         * Waits for new tone to be reported via the <tt>addTonePacket()</tt>
         * method and then delivers them to the <tt>AudioMediaStream</tt> that
         * we are associated with.
         */
        public void run()
        {
            isRunning = true;

            DTMFRtpTone temp = null;

            while(isRunning)
            {
                synchronized(this)
                {
                    if(lastReceivedTone == null)
                    {
                        try
                        {
                            wait();
                        }
                        catch (InterruptedException ie) {}
                    }

                    temp = lastReceivedTone;
                    // make lastReportedLevels to null
                    // so we will wait for the next tone on next iteration
                    lastReceivedTone = null;
                }

                if(temp != null
                    && ((lastReportedTone == null && !toEnd)
                        || (lastReportedTone != null && toEnd)))
                {
                    //now notify our listener
                    if (mediaStream != null)
                    {
                        mediaStream.fireDTMFEvent(temp, toEnd);
                        if(toEnd)
                            lastReportedTone = null;
                        else
                            lastReportedTone = temp;
                        toEnd = false;
                    }
                }
            }
        }

        /**
         * A packet that we should convert to tone and deliver
         * to our media stream and its listeners in a separate thread.
         *
         * @param p the packet we will convert and deliver.
         */
        public void addTonePacket(DtmfRawPacket p)
        {
            synchronized(this)
            {
                this.lastReceivedTone = getToneFromPacket(p);
                this.toEnd = p.isEnd();

                notifyAll();
            }
        }

        /**
         * Causes our run method to exit so that this thread would stop
         * handling levels.
         */
        public void stop()
        {
            synchronized(this)
            {
                this.lastReceivedTone = null;
                isRunning = false;

                notifyAll();
            }
        }

        /**
         * Maps DTMF packet codes to our DTMFRtpTone objects.
         * @param p the packet
         * @return the corresponding tone.
         */
        private DTMFRtpTone getToneFromPacket(DtmfRawPacket p)
        {
            for (int i = 0; i < supportedTones.length; i++)
            {
                DTMFRtpTone t = supportedTones[i];
                if(t.getCode() == p.getCode())
                    return t;
            }

            return null;
        }
    }
}
