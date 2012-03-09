/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import net.java.sip.communicator.service.neomedia.*;

import net.sf.fmj.media.rtp.*;
import java.net.*;
import javax.media.rtp.*;

/**
 * Class used to compute stats concerning a MediaStream.
 *
 * @author Vincent Lucas
 */
public class MediaStreamStatsImpl
    implements MediaStreamStats
{
    /**
     * The MediaStream used to copte the stats.
     */
    private MediaStreamImpl mediaStreamImpl;

    /**
     * The last time these stats have been updated.
     */
    private long updateTimeMs;

    /**
     * The last number of received packets.
     */
    private long downloadNbPackets = 0;

    /**
     * The last number of sent packets.
     */
    private long uploadNbPackets = 0;

    /**
     * The last number of sent packets when the last feedback has been received.
     * This counter is used to compute the upload loss rate.
     */
    private long uploadFeedbackNbPackets = 0;

    /**
     * The last number of download lost packets.
     */
    private long downloadNbLost = 0;

    /**
     * The last number of upload lost packets.
     */
    private long uploadNbLost = 0;

    /**
     * The last number of received Bytes.
     */
    private long downloadNbByte = 0;

    /**
     * The last number of sent Bytes.
     */
    private long uploadNbByte = 0;

    /**
     * The last download loss rate computed (in %).
     */
    private double downloadPercentLoss = 0;

    /**
     * The last upload loss rate computed (in %).
     */
    private double uploadPercentLoss = 0;

    /**
     * The last used bandwidth computed in download (in Kbit/s).
     */
    private double downloadRateKiloBitPerSec = 0;

    /**
     * The last used bandwidth computed in upload (in Kbit/s).
     */
    private double uploadRateKiloBitPerSec = 0;

    /**
     * The last jitter sent in a RTCP feedback (in RTP timestamp units).
     */
    private double downloadJitterRTPTimestampUnits = 0;

    /**
     * The last jitter received in a RTCP feedback (in RTP timestamp units).
     */
    private double uploadJitterRTPTimestampUnits = 0;

    /**
     * Creates a new instance of stats concerning a MediaStream.
     *
     * @param mediaStreamImpl The MediaStreamImpl used to compute the stats.
     */
    public MediaStreamStatsImpl(MediaStreamImpl mediaStreamImpl)
    {
        this.updateTimeMs = System.currentTimeMillis();
        this.mediaStreamImpl = mediaStreamImpl;
    }

    /**
     * Computes and updates information for a specific stream.
     */
    public void updateStats()
    {
        // Gets the current time.
        long currentTimeMs = System.currentTimeMillis();
        // Gets the current number of losses in download since the beginning of
        // this stream.
        long downloadNewNbLost = this.getDownloadNbPDULost();
        // Gets the current number of packets correctly received since the
        // beginning of this stream.
        long downloadNewNbRecv = this.getDownloadNbPDUProcessed();
        long uploadNewNbRecv = this.getUploadNbPDUProcessed();
        // Gets the number of byte received/sent since the beginning of this
        // stream.
        long downloadNewNbByte = this.getDownloadNbByte();
        long uploadNewNbByte = this.getDownloadNbByte();

        // Computes the number of update steps which has not been done since
        // last update.
        long downloadNbSteps = downloadNewNbRecv - this.downloadNbPackets;
        long uploadNbSteps = uploadNewNbRecv - this.uploadNbPackets;

        // The uploadPercentLoss is only computed when a new RTCP feedback is
        // received. This is not the case for the downloadPercentLoss which is
        // updated for each new RTP packet received.
        // Computes the loss rate for this stream.
        double downloadNewPercentLoss = MediaStreamStatsImpl.computePercentLoss(
                downloadNewNbRecv - this.downloadNbPackets,
                downloadNewNbLost - this.downloadNbLost);
        this.downloadPercentLoss = MediaStreamStatsImpl.computeEWMA(
                downloadNbSteps,
                this.downloadPercentLoss,
                downloadNewPercentLoss);

        // Computes the bandwidth used by this stream.
        double downloadNewRateKiloBitPerSec =
            MediaStreamStatsImpl.computeRateKiloBitPerSec(
                downloadNewNbByte - this.downloadNbByte,
                currentTimeMs - this.updateTimeMs);
        this.downloadRateKiloBitPerSec = MediaStreamStatsImpl.computeEWMA(
                downloadNbSteps,
                this.downloadRateKiloBitPerSec,
                downloadNewRateKiloBitPerSec);

        double uploadNewRateKiloBitPerSec =
            MediaStreamStatsImpl.computeRateKiloBitPerSec(
                uploadNewNbByte - this.uploadNbByte,
                currentTimeMs - this.updateTimeMs);
        this.uploadRateKiloBitPerSec = MediaStreamStatsImpl.computeEWMA(
                uploadNbSteps,
                this.uploadRateKiloBitPerSec,
                uploadNewRateKiloBitPerSec);

        // Saves the last update values.
        this.updateTimeMs = currentTimeMs;
        this.downloadNbLost = downloadNewNbLost;
        this.downloadNbPackets = downloadNewNbRecv;
        this.uploadNbPackets = uploadNewNbRecv;
        this.downloadNbByte = downloadNewNbByte;
        this.uploadNbByte = uploadNewNbByte;
    }

    /**
     * Returns the local IP address of the MediaStream.
     *
     * @return the local IP address of the stream.
     */
    public String getLocalIPAddress()
    {
        InetSocketAddress mediaStreamLocalDataAddress =
            mediaStreamImpl.getLocalDataAddress();
        if(mediaStreamLocalDataAddress == null)
        {
            return null;
        }
        return mediaStreamLocalDataAddress.getAddress().getHostAddress();
    }

    /**
     * Returns the local port of the MediaStream.
     *
     * @return the local port of the stream.
     */
    public int getLocalPort()
    {
        InetSocketAddress mediaStreamLocalDataAddress =
            mediaStreamImpl.getLocalDataAddress();
        if(mediaStreamLocalDataAddress == null)
        {
            return -1;
        }
        return mediaStreamLocalDataAddress.getPort();
    }

    /**
     * Returns the remote IP address of the MediaStream.
     *
     * @return the remote IP address of the stream.
     */
    public String getRemoteIPAddress()
    {
        MediaStreamTarget mediaStreamTarget = mediaStreamImpl.getTarget();
        // Stops if the endpoint is disconnected.
        if(mediaStreamTarget == null)
        {
            return null;
        }
        // Gets this stream IP address endpoint.
        return mediaStreamTarget.getDataAddress().getAddress().getHostAddress();
    }

    /**
     * Returns the remote port of the MediaStream.
     *
     * @return the remote port of the stream.
     */
    public int getRemotePort()
    {
        MediaStreamTarget mediaStreamTarget = mediaStreamImpl.getTarget();
        // Stops if the endpoint is disconnected.
        if(mediaStreamTarget == null)
        {
            return -1;
        }
        // Gets this stream port endpoint.
        return mediaStreamTarget.getDataAddress().getPort();
    }

    /**
     * Returns the MediaStream enconding.
     *
     * @return the encoding used by the stream.
     */
    public String getEncoding()
    {
        // Gets this stream encoding.
        return mediaStreamImpl.getFormat().getEncoding();
    }

    /**
     * Returns the MediaStream enconding rate (in Hz)..
     *
     * @return the encoding rate used by the stream.
     */
    public String getEncodingClockRate()
    {
        // Gets this stream encoding clock rate.
        return mediaStreamImpl.getFormat().getClockRateString();
    }

    /**
     * Returns the percent loss of the download stream.
     *
     * @return the last loss rate computed (in %).
     */
    public double getDownloadPercentLoss()
    {
        return this.downloadPercentLoss;
    }

    /**
     * Returns the percent loss of the upload stream.
     *
     * @return the last loss rate computed (in %).
     */
    public double getUploadPercentLoss()
    {
        return this.uploadPercentLoss;
    }

    /**
     * Returns the bandwidth used by this download stream.
     *
     * @return the last used download bandwidth computed (in Kbit/s).
     */
    public double getDownloadRateKiloBitPerSec()
    {
        return this.downloadRateKiloBitPerSec;
    }

    /**
     * Returns the bandwidth used by this download stream.
     *
     * @return the last used upload bandwidth computed (in Kbit/s).
     */
    public double getUploadRateKiloBitPerSec()
    {
        return this.uploadRateKiloBitPerSec;
    }

    /**
     * Returns the jitter average of this download stream.
     *
     * @return the last jitter average computed (in ms).
     */
    public double getDownloadJitterMs()
    {
        double mediaFormatClockRate =
            this.mediaStreamImpl.getFormat().getClockRate();

        // RFC3550 says that concerning the RTP timestamp unit (cf. section 5.1
        // RTP Fixed Header Fields, subsection timestamp: 32 bits):
        // As an example, for fixed-rate audio the timestamp clock would likely
        // increment by one for each sampling period.
        //
        // Thus we take the jitter (in RTP timestamp units), converts it to
        // seconds (deivision by the codec clock rate) and finally converts it
        // in Ms (* 1000).
        return (this.downloadJitterRTPTimestampUnits / mediaFormatClockRate)
            * 1000.0;
    }

    /**
     * Returns the jitter average of this upload stream.
     *
     * @return the last jitter average computed (in ms).
     */
    public double getUploadJitterMs()
    {
        double mediaFormatClockRate =
            this.mediaStreamImpl.getFormat().getClockRate();

        // RFC3550 says that concerning the RTP timestamp unit (cf. section 5.1
        // RTP Fixed Header Fields, subsection timestamp: 32 bits):
        // As an example, for fixed-rate audio the timestamp clock would likely
        // increment by one for each sampling period.
        //
        // Thus we take the jitter (in RTP timestamp units), converts it to
        // seconds (deivision by the codec clock rate) and finally converts it
        // in Ms (* 1000).
        return (this.uploadJitterRTPTimestampUnits / mediaFormatClockRate)
            * 1000.0;
    }

    /**
     * Updates this stream stats with the new feedback sent.
     *
     * @param feedback The last RTCP feedback sent by the MediaStream.
     */
    public void updateNewSentFeedback(RTCPFeedback feedback)
    {
        // No need to update the download loss has we have a more accurate value
        // in the global reception stats, which are updated for each new packet
        // received.

        // Updates the download jitter in RTP timestamp units.
        // There is no need to compute a jitter average, since (cf. RFC3550,
        // section 6.4.1 SR: Sender Report RTCP Packet, subsection interarrival
        // jitter: 32 bits) the value contained in the RTCP sender report packet
        // contains a mean deviation of the jitter.
        this.downloadJitterRTPTimestampUnits = feedback.getJitter();
    }

    /**
     * Updates this stream stats with the new feedback received.
     *
     * @param feedback The last RTCP feedback received by the MediaStream.
     */
    public void updateNewReceivedFeedback(RTCPFeedback feedback)
    {
        // Updates the loss rate with the RTCP sender report feedback, since
        // this is the only information source available for the upalod stream.
        long uploadNewNbRecv = this.getUploadNbPDUProcessed();
        long uploadNewNbLost = feedback.getNumLost();
        long uploadNbSteps = uploadNewNbRecv - this.uploadFeedbackNbPackets;

        double uploadNewPercentLoss = MediaStreamStatsImpl.computePercentLoss(
                uploadNewNbRecv - this.uploadFeedbackNbPackets,
                uploadNewNbLost - this.uploadNbLost);
        this.uploadPercentLoss = MediaStreamStatsImpl.computeEWMA(
                uploadNbSteps,
                this.uploadPercentLoss,
                uploadNewPercentLoss);

        // Updates the upload loss counters.
        this.uploadFeedbackNbPackets = uploadNewNbRecv;
        this.uploadNbLost = uploadNewNbLost;

        // Updates the download jitter in RTP timestamp units.
        // There is no need to compute a jitter average, since (cf. RFC3550,
        // section 6.4.1 SR: Sender Report RTCP Packet, subsection interarrival
        // jitter: 32 bits) the value contained in the RTCP sender report packet
        // contains a mean deviation of the jitter.
        this.uploadJitterRTPTimestampUnits = feedback.getJitter();
    }

    /**
     * Computes the loss rate.
     *
     * @param nbRecv The number of received packets.
     * @param nbLost The number of lost packets.
     *
     * @return The loss rate in percent.
     */
    private static double computePercentLoss(long nbRecv, long nbLost)
    {
        if(nbRecv == 0)
        {
            return 0;
        }
        return ((double) 100 * nbLost) / ((double)(nbLost + nbRecv));
    }

    /**
     * Computes the bandwidth usage in Kilo bits per secondes.
     *
     * @param nbByteRecv The number of Byte received.
     * @param callNbTimeMsSpent The time spent since the mediaStreamImpl is
     * connected to the endpoint.
     *
     * @return the bandwidth rate computed in Kilo bits per secondes.
     */
    private static double computeRateKiloBitPerSec(
            long nbByteRecv,
            long callNbTimeMsSpent)
    {
        if(nbByteRecv == 0)
        {
            return 0;
        }
        return (nbByteRecv * 8.0 / 1000.0) / (callNbTimeMsSpent / 1000.0);
    }

    /**
     * Computes an Exponentially Weighted Moving Average (EWMA). Thus, the most
     * recent history has a more preponderant importance in the average
     * computed.
     *
     * @param nbStepSinceLastUpdate The number of step which has not been
     * computed since last update. In our case the number of packets received
     * since the last computation.
     * @param lastValue The value computed during the last update.
     * @param newValue The value newly computed.
     *
     * @return The EWMA average computed.
     */
    private static double computeEWMA(
            long nbStepSinceLastUpdate,
            double lastValue,
            double newValue)
    {
        // For each new packet received the EWMA moves by a 0.1 coefficient.
        double EWMACoeff = 0.01 * nbStepSinceLastUpdate;
        // EWMA must be <= 1.
        if(EWMACoeff > 1)
        {
            EWMACoeff = 1.0;
        }
        return lastValue * (1.0 - EWMACoeff) + newValue * EWMACoeff;

    }

    /**
     * Returns the number of Protocol Data Units (PDU) received since the
     * beginning of the session.
     *
     * @return the number of packets received for this stream.
     */
    private long getDownloadNbPDUProcessed()
    {
        int nbReceived = 0;
        java.util.List<ReceiveStream> listReceiveStream =
            this.mediaStreamImpl.getDeviceSession().getReceiveStreams();

        for(int i = 0; i < listReceiveStream.size(); ++i)
        {
            ReceiveStream receiveStream = listReceiveStream.get(i);
            nbReceived +=
                receiveStream.getSourceReceptionStats().getPDUProcessed();
        }

        return nbReceived;
    }

    /**
     * Returns the number of Protocol Data Units (PDU) sent since the
     * beginning of the session.
     *
     * @return the number of packets sent for this stream.
     */
    private long getUploadNbPDUProcessed()
    {
        StreamRTPManager rtpManager = this.mediaStreamImpl.getRTPManager();

        if(rtpManager == null)
        {
            return 0;
        }
        return rtpManager.getGlobalTransmissionStats().getRTPSent();
    }

    /**
     * Returns the number of Protocol Data Units (PDU) lost in download since
     * the beginning of the session.
     *
     * @return the number of packets lost for this stream.
     */
    private long getDownloadNbPDULost()
    {
        int nbLost = 0;
        java.util.List<ReceiveStream> listReceiveStream =
            this.mediaStreamImpl.getDeviceSession().getReceiveStreams();

        for(int i = 0; i < listReceiveStream.size(); ++i)
        {
            ReceiveStream receiveStream = listReceiveStream.get(i);
            nbLost += receiveStream.getSourceReceptionStats().getPDUlost();
        }

        return nbLost;
    }

    /**
     * Returns the number of byte received since the beginning of the session.
     *
     * @return the number of byte received for this stream.
     */
    private long getDownloadNbByte()
    {
        StreamRTPManager rtpManager = this.mediaStreamImpl.getRTPManager();

        if(rtpManager == null)
        {
            return 0;
        }
        return rtpManager.getGlobalReceptionStats().getBytesRecd();
    }

    /**
     * Returns the number of byte received since the beginning of the session.
     *
     * @return the number of byte received for this stream.
     */
    private long getUploadNbByte()
    {
        StreamRTPManager rtpManager = this.mediaStreamImpl.getRTPManager();

        if(rtpManager == null)
        {
            return 0;
        }
        return rtpManager.getGlobalTransmissionStats().getBytesSent();
    }
}
