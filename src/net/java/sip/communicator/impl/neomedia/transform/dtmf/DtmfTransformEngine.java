/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform.dtmf;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.transform.*;

/**
 * The class is responsible for sending DTMF tones in an RTP audio stream as
 * descirbed by RFC4733.
 *
 * @author Emil Ivov
 * @author Romain Philibert
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
     * we determine it to be dtmf.
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
        if (engine.isTransformationEnable())
            +        {
            +            byte[] buffer = new byte[16];
            +            System.arraycopy(pkt.getBuffer(), 0, buffer, 0, 12);
            +            DtmfRawPacket dtmfPkt = new DtmfRawPacket(buffer);
            +            long audioPacketTimestamp = dtmfPkt.getTimestamp();
            +            int pktCode = engine.getDtmfCode();
            +            boolean pktEnd = false;
            +            boolean pktMarker = false;
            +            int pktDuration = 0;
            +            long pktTimestamp = engine.getCurrentTimestamp();
            +            if (engine.isSendingStateEquals(DtmfTransformEngine.START_SENDING))
            +            {
            +                logger.trace("START_SENDING");
            +                /*
            +                 * The first packet is send with the RTP Marker set to 1.
            +                 */
            +                pktMarker=true;
            +                pktTimestamp = audioPacketTimestamp;
            +
            +                /*
            +                 * Save the audioPacketTimestamp value. T
            +                 * This value will be used in the next dtmf packets.
            +                 */
            +                logger.trace("Timestamp read = "+audioPacketTimestamp);
            +                engine.setCurrentTimestamp(audioPacketTimestamp);
            +                engine.setSendingState(DtmfTransformEngine.SENDING_UPDATE);
            +            }
            +
            +            else if (engine.isSendingStateEquals(DtmfTransformEngine.SENDING_UPDATE))
            +            {
            +                logger.trace("SENDING_UPDATE");
            +                int duration = (int)(audioPacketTimestamp-pktTimestamp);
            +
            +                // Check for long state event
            +                if (duration>0xFFFF)
            +                {
            +                    logger.trace("LONG_DURATION_EVENT");
            +                    /*
            +                     * When duration > 0xFFFF we first send a packet with
            +                     * duration = 0xFFFF. For the next packet, the duration
            +                     * start from begining but the audioPacketTimestamp is set to the
            +                     * time when the long duration event occurs.
            +                     */
            +                    pktDuration = 0xFFFF;
            +                    pktTimestamp = audioPacketTimestamp;
            +                    engine.setCurrentTimestamp(audioPacketTimestamp);
            +                }
            +                else
            +                {
            +                    pktDuration = duration;
            +                }
            +            }
            +            else if (engine.isSendingStateEquals(DtmfTransformEngine.STOP_SENDING))
            +            {
            +                logger.trace("STOP_SENDING");
            +                /**
            +                 * The first ending packet do have the End flag set.
            +                 * But the 2 next will have the End flag set.
            +                 *
            +                 * The audioPacketTimestamp and the duration field stay unchanged for
            +                 * the 3 last packets
            +                 */
            +                pktDuration = (int)(audioPacketTimestamp-pktTimestamp);
            +
            +                engine.setEndTimestamp(audioPacketTimestamp);
            +                engine.remainingsEndPackets = 2;
            +                engine.setSendingState(DtmfTransformEngine.STOP_SENDING_REPEATING);
            +            }
            +            else if (engine.isSendingStateEquals(DtmfTransformEngine.STOP_SENDING_REPEATING))
            +            {
            +                logger.trace("STOP_SENDING_REPEATING");
            +                /**
            +                 * We set the End flag for the 2 last packets.
            +                 * The audioPacketTimestamp and the duration field stay unchanged for
            +                 * the 3 last packets.
            +                 */
            +                pktEnd=true;
            +                pktDuration=(int)(engine.getEndTimestamp()-pktTimestamp);
            +
            +                engine.remainingsEndPackets --;
            +                if (engine.remainingsEndPackets<=0)
            +                {
            +                    engine.disableTransformation();
            +                }
            +            }
            +            dtmfPkt.fillRawPacket(pktCode, pktEnd, pktMarker, pktDuration, pktTimestamp);
            +            pkt = dtmfPkt;
            +        }
            +        return pkt;
        return pkt;
    }

}
