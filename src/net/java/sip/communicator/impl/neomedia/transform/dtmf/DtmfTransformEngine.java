/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform.dtmf;


import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.transform.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.util.*;

/**
 * The class is responsible for sending DTMF tones in an RTP audio stream as
 * described by RFC4733.
 *
 * @author Emil Ivov
 * @author Romain Philibert
 */
public class DtmfTransformEngine
    implements TransformEngine,
               PacketTransformer
{

    /**
     * Our class logger.
     */
    private Logger logger = Logger.getLogger(DtmfTransformEngine.class);

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
         * #startSending(DTMFTone)} method and we haven't yet sent any of the
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
     * The status that this engine is currently in.
     */
    private ToneTransmissionState toneTransmissionState
                                                = ToneTransmissionState.IDLE;

    /**
     * The tone that we are supposed to be currently transmitting.
     */
    private DTMFTone currentTone = null;

    /**
     * The duration (in timestamp units or in other words ms*8) that we have
     * transmitted the current tone for.
     */
    private int currentDuration = 0;

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
     * otherwise since we will be handling the packet ourselves and ther's
     * no point in feeding it to the application.
     */
    public RawPacket reverseTransform(RawPacket pkt)
    {
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
        if (this.toneTransmissionState == ToneTransmissionState.IDLE
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

        /*
        long pktTimestamp = engine.getCurrentTimestamp();

        if (STATUS_START_SENDING))
        {
                logger.trace("START_SENDING");
                 // The first packet is send with the RTP Marker set to 1.
                pktMarker=true;
                pktTimestamp = audioPacketTimestamp;

                 // Save the audioPacketTimestamp value. T
                 // This value will be used in the next dtmf packets.
                logger.trace("Timestamp read = "+audioPacketTimestamp);
                engine.setCurrentTimestamp(audioPacketTimestamp);
                engine.setSendingState(DtmfTransformEngine.SENDING_UPDATE);
            }
            else if (engine.isSendingStateEquals(DtmfTransformEngine.SENDING_UPDATE))
            {
                logger.trace("SENDING_UPDATE");
                int duration = (int)(audioPacketTimestamp-pktTimestamp);

                // Check for long state event
                if (duration>0xFFFF)
                {
                    logger.trace("LONG_DURATION_EVENT");

                     // When duration > 0xFFFF we first send a packet with
                     // duration = 0xFFFF. For the next packet, the duration
                     // start from begining but the audioPacketTimestamp is set to the
                     // time when the long duration event occurs.
                    pktDuration = 0xFFFF;
                    pktTimestamp = audioPacketTimestamp;
                    engine.setCurrentTimestamp(audioPacketTimestamp);
                }
                else
                {
                    pktDuration = duration;
                }
            }
            else if (engine.isSendingStateEquals(DtmfTransformEngine.STOP_SENDING))
            {
                logger.trace("STOP_SENDING");

                 // The first ending packet do have the End flag set.
                 // But the 2 next will have the End flag set.
                 //
                 // The audioPacketTimestamp and the duration field stay unchanged for
                 // the 3 last packets
                pktDuration = (int)(audioPacketTimestamp-pktTimestamp);

                engine.setEndTimestamp(audioPacketTimestamp);
                engine.remainingsEndPackets = 2;
                engine.setSendingState(DtmfTransformEngine.STOP_SENDING_REPEATING);
            }
            else if (engine.isSendingStateEquals(DtmfTransformEngine.STOP_SENDING_REPEATING))
            {
                logger.trace("STOP_SENDING_REPEATING");

                 // We set the End flag for the 2 last packets.
                 // The audioPacketTimestamp and the duration field stay unchanged for
                 // the 3 last packets.
                pktEnd=true;
                pktDuration=(int)(engine.getEndTimestamp()-pktTimestamp);

                engine.remainingsEndPackets --;
                if (engine.remainingsEndPackets<=0)
                {
                    engine.disableTransformation();
                }
            }
            dtmfPkt.fillRawPacket(pktCode, pktEnd, pktMarker, pktDuration, pktTimestamp);
            pkt = dtmfPkt;
        }
        */
        return pkt;
    }


    /**
     * DTMF sending stub: this is where we should set the transformer in the
     * proper state so that it would start replacing packets with dtmf codes.
     *
     * @param tone the tone that we'd like to start sending.
     */
    public void startSending(DTMFTone tone)
    {

    }

}
