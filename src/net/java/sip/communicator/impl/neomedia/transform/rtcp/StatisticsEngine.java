/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform.rtcp;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.transform.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.util.*;
import net.sf.fmj.media.rtp.*;

/**
 * Engine which don't transform packets, just listens for outgoing
 * RTCP Packets and logs and stores statistical data for the stream.
 *
 * @author Damian Minkov
 */
public class StatisticsEngine
    implements TransformEngine,
               PacketTransformer
{
    /**
     * The <tt>Logger</tt> used by the <tt>StatisticsEngine</tt> class
     * and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(StatisticsEngine.class);

    /**
     * The rtp statistics prefix we use for every log.
     * Simplifies parsing and searching for statistics info in log files.
     */
    public static final String RTP_STAT_PREFIX = "rtpstat:";

    /**
     * Number of sender reports send.
     * Used only for logging and debug purposes.
     */
    private long numberOfSenderReports = 0;

    /**
     * The minimum inter arrival jitter value we have reported.
     */
    private long maxInterArrivalJitter = 0;

    /**
     * The minimum inter arrival jitter value we have reported.
     */
    private long minInterArrivalJitter = -1;

    /**
     * Number of lost packets reported.
     */
    private long lost = 0;

    /**
     * The stream created us.
     */
    private MediaStreamImpl mediaStream;

    /**
     * Creates Statistic engine.
     * @param stream the stream creating us.
     */
    public StatisticsEngine(MediaStreamImpl stream)
    {
        this.mediaStream = stream;
    }

    /**
     * Close the transformer and underlying transform engine.
     * 
     * Nothing to do here. 
     */
    public void close() 
    {
    }

    /**
     * Transfers RTCP sender report feedback as new information about the
     * download stream for the MediaStreamStats.
     * Finds the info needed for statistics in the packet and stores it.
     * Then returns the same packet as we are not modifying it.
     *
     * @param pkt the packet
     * @return the packet
     */
    public RawPacket transform(RawPacket pkt)
    {
        try
        {
            numberOfSenderReports++;

            byte[] data = pkt.getBuffer();
            int offset = pkt.getOffset();
            int length = pkt.getLength();

            RTCPHeader header = new RTCPHeader(
                    data, offset, length);
            if (header.getPacketType() == RTCPPacket.SR)
            {
                RTCPSenderReport report = new RTCPSenderReport(
                        data, offset, length);

                if(report.getFeedbackReports().size() > 0)
                {
                    RTCPFeedback feedback =
                            (RTCPFeedback)report.getFeedbackReports().get(0);

                    this.mediaStream.getMediaStreamStats()
                        .updateNewSentFeedback(feedback);

                    // The rest of this function is only used for logging
                    // purpose. Thus, it is useless to continue if the
                    // logger is not at least in INFO mode.
                    if(!logger.isInfoEnabled())
                        return pkt;

                    long jitter = feedback.getJitter();

                    if(jitter < getMinInterArrivalJitter()
                        || getMinInterArrivalJitter() == -1)
                        minInterArrivalJitter = jitter;

                    if(getMaxInterArrivalJitter() < jitter)
                        maxInterArrivalJitter = jitter;

                    lost = feedback.getNumLost();

                    // As sender reports are sent on every 5 seconds, print
                    // every 4th packet, on every 20 seconds.
                    if(numberOfSenderReports % 4 != 1)
                        return pkt;

                    StringBuilder buff = new StringBuilder(RTP_STAT_PREFIX);
                    MediaFormat mediaStreamFormat = mediaStream.getFormat();

                    buff.append("Sending a report for ")
                        .append(mediaStreamFormat != null
                                    ? mediaStreamFormat.getMediaType()
                                    : "")
                        .append(" stream SSRC:")
                        .append(feedback.getSSRC())
                        .append(" [packet count:")
                        .append(report.getSenderPacketCount())
                        .append(", bytes:").append(report.getSenderByteCount())
                        .append(", interarrival jitter:")
                                .append(jitter)
                        .append(", lost packets:").append(feedback.getNumLost())
                        .append(", time since previous report:")
                                .append((int) (feedback.getDLSR() / 65.536))
                                .append("ms ]");
                    logger.info(buff);
                }
            }
        }
        catch(Throwable t)
        {
            t.printStackTrace();
        }

        return pkt;
    }

    /**
     * Transfers RTCP sender report feedback as new information about the upload
     * stream for the MediaStreamStats.
     * Returns the packet as we are listening just for sending packages.
     *
     * @param pkt the packet without any change.
     * @return the packet without any change.
     */
    public RawPacket reverseTransform(RawPacket pkt)
    {
        try
        {
            byte[] data = pkt.getBuffer();
            int offset = pkt.getOffset();
            int length = pkt.getLength();

            RTCPHeader header = new RTCPHeader(data, offset, length);
            if (header.getPacketType() == RTCPPacket.SR)
            {
                RTCPSenderReport report = new RTCPSenderReport(
                        data, offset, length);

                if(report.getFeedbackReports().size() > 0)
                {
                    RTCPFeedback feedback =
                            (RTCPFeedback)report.getFeedbackReports().get(0);

                    this.mediaStream.getMediaStreamStats()
                        .updateNewReceivedFeedback(feedback);
                }
            }
        }
        catch(Throwable t)
        {
            t.printStackTrace();
        }
        return pkt;
    }

    /**
     * Always returns <tt>null</tt> since this engine does not require any
     * RTP transformations.
     *
     * @return <tt>null</tt> since this engine does not require any
     * RTP transformations.
     */
    public PacketTransformer getRTPTransformer()
    {
        return null;
    }

    /**
     * Returns a reference to this class since it is performing RTP
     * transformations in here.
     *
     * @return a reference to <tt>this</tt> instance of the
     * <tt>StatisticsEngine</tt>.
     */
    public PacketTransformer getRTCPTransformer()
    {
        return this;
    }

    /**
     * The minimum inter arrival jitter value we have reported.
     * @return minimum inter arrival jitter value we have reported.
     */
    public long getMaxInterArrivalJitter()
    {
        return maxInterArrivalJitter;
    }

    /**
     * The maximum inter arrival jitter value we have reported.
     * @return maximum inter arrival jitter value we have reported.
     */
    public long getMinInterArrivalJitter()
    {
        return minInterArrivalJitter;
    }

    /**
     * Number of lost packets reported.
     * @return number of lost packets reported.
     */
    public long getLost()
    {
        return lost;
    }
}
