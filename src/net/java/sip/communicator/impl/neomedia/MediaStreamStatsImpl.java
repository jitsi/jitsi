/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;
import java.net.*;

import javax.media.format.*;
import javax.media.rtp.*;

import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.*;
import net.sf.fmj.media.rtp.*;

/**
 * Class used to compute stats concerning a MediaStream.
 *
 * @author Vincent Lucas
 */
public class MediaStreamStatsImpl
    implements MediaStreamStats
{

    /**
     * Enumeation of the direction (DOWNLOAD or UPLOAD) used for the stats.
     */ 
    private enum StreamDirection
    {
        DOWNLOAD,
        UPLOAD
    }

    /**
     * The MediaStream used to copte the stats.
     */
    private MediaStreamImpl mediaStreamImpl;

    /**
     * The last time these stats have been updated.
     */
    private long updateTimeMs;

    /**
     * The last number of received/sent packets.
     */
    private long[] nbPackets = {0, 0};

    /**
     * The last number of sent packets when the last feedback has been received.
     * This counter is used to compute the upload loss rate.
     */
    private long uploadFeedbackNbPackets = 0;

    /**
     * The last number of download/upload lost packets.
     */
    private long[] nbLost = {0, 0};

    /**
     * The last number of received/sent Bytes.
     */
    private long[] nbByte = {0, 0};

    /**
     * The last download/upload loss rate computed (in %).
     */
    private double[] percentLoss = {0, 0};

    /**
     * The last used bandwidth computed in download/upload (in Kbit/s).
     */
    private double[] rateKiloBitPerSec = {0, 0};

    /**
     * The last jitter received/sent in a RTCP feedback (in RTP timestamp
     * units).
     */
    private double[] jitterRTPTimestampUnits = {0, 0};

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

        // UPdates stats for the download stream.
        this.updateStreamDirectionStats(
                StreamDirection.DOWNLOAD,
                currentTimeMs);
        // UPdates stats for the upload stream.
        this.updateStreamDirectionStats(
                StreamDirection.UPLOAD,
                currentTimeMs);

        // Saves the last update values.
        this.updateTimeMs = currentTimeMs;
    }

    /**
     * Computes and updates information for a specific stream.
     *
     * @param streamDirection The stream direction (DOWNLOAD or UPLOAD) of the
     * stream from which this function updates the stats.
     * @param currentTime The current time in ms.
     */
    private void updateStreamDirectionStats(
            StreamDirection streamDirection,
            long currentTimeMs)
    {
        int streamDirectionIndex = streamDirection.ordinal();

        // Gets the current number of packets correctly received since the
        // beginning of this stream.
        long newNbRecv = this.getNbPDU(streamDirection);
        // Gets the number of byte received/sent since the beginning of this
        // stream.
        long newNbByte = this.getNbBytes(streamDirection);

        // Computes the number of update steps which has not been done since
        // last update.
        long nbSteps = newNbRecv - this.nbPackets[streamDirectionIndex];
        // Even if the remote peer does not send any packets (i.e. is
        // microphone is muted), Jitsi must updates it stats. Thus, Jitsi
        // computes a number of steps equivalent as if Jitsi receives a packet
        // each 20ms (default value).
        if(nbSteps == 0)
        {
            nbSteps = (currentTimeMs - this.updateTimeMs) / 20;
        }

        // The upload percentLoss is only computed when a new RTCP feedback is
        // received. This is not the case for the download percentLoss which is
        // updated for each new RTP packet received.
        // Computes the loss rate for this stream.
        if(streamDirection == StreamDirection.DOWNLOAD)
        {
            // Gets the current number of losses in download since the beginning
            // of this stream.
            long newNbLost =
                this.getDownloadNbPDULost() - this.nbLost[streamDirectionIndex];

            updateNbLoss(streamDirection, newNbLost, nbSteps + newNbLost);
        }

        // Computes the bandwidth used by this stream.
        double newRateKiloBitPerSec =
            MediaStreamStatsImpl.computeRateKiloBitPerSec(
                    newNbByte - this.nbByte[streamDirectionIndex],
                    currentTimeMs - this.updateTimeMs);
        this.rateKiloBitPerSec[streamDirectionIndex] =
            MediaStreamStatsImpl.computeEWMA(
                    nbSteps,
                    this.rateKiloBitPerSec[streamDirectionIndex],
                    newRateKiloBitPerSec);

        // Saves the last update values.
        this.nbPackets[streamDirectionIndex] = newNbRecv;
        this.nbByte[streamDirectionIndex] = newNbByte;
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
        return mediaStreamImpl.getFormat().getRealUsedClockRateString();
    }

    /**
     * Returns the upload video format if this stream uploads a video, or null
     * if not.
     *
     * @return the upload video format if this stream uploads a video, or null
     * if not.
     */
    private VideoFormat getUploadVideoFormat()
    {
        MediaDeviceSession mediaDeviceSession
            = mediaStreamImpl.getDeviceSession();

        return
            (mediaDeviceSession instanceof VideoMediaDeviceSession)
                ? ((VideoMediaDeviceSession) mediaDeviceSession)
                    .getSentVideoFormat()
                : null;
    }

    /**
     * Returns the download video format if this stream downloads a video, or
     * null if not.
     *
     * @return the download video format if this stream downloads a video, or
     * null if not.
     */
    private VideoFormat getDownloadVideoFormat()
    {
        MediaDeviceSession mediaDeviceSession
            = mediaStreamImpl.getDeviceSession();

        return
            (mediaDeviceSession instanceof VideoMediaDeviceSession)
                ? ((VideoMediaDeviceSession) mediaDeviceSession)
                    .getReceivedVideoFormat()
                : null;
    }

    /**
     * Returns the upload video size if this stream uploads a video, or null if
     * not.
     *
     * @return the upload video size if this stream uploads a video, or null if
     * not.
     */
    public Dimension getUploadVideoSize()
    {
        Dimension videoSize = null;
        VideoFormat format = this.getUploadVideoFormat();
        if(format != null)
        {
            videoSize = format.getSize();
        }
        return videoSize;
    }

    /**
     * Returns the download video size if this stream downloads a video, or
     * null if not.
     *
     * @return the download video size if this stream downloads a video, or null
     * if not.
     */
    public Dimension getDownloadVideoSize()
    {
        Dimension videoSize = null;
        VideoFormat format = this.getDownloadVideoFormat();
        if(format != null)
        {
            videoSize = format.getSize();
        }
        return videoSize;
    }

    /**
     * Returns the percent loss of the download stream.
     *
     * @return the last loss rate computed (in %).
     */
    public double getDownloadPercentLoss()
    {
        return this.percentLoss[StreamDirection.DOWNLOAD.ordinal()];
    }

    /**
     * Returns the percent loss of the upload stream.
     *
     * @return the last loss rate computed (in %).
     */
    public double getUploadPercentLoss()
    {
        return this.percentLoss[StreamDirection.UPLOAD.ordinal()];
    }

    /**
     * Returns the bandwidth used by this download stream.
     *
     * @return the last used download bandwidth computed (in Kbit/s).
     */
    public double getDownloadRateKiloBitPerSec()
    {
        return this.rateKiloBitPerSec[StreamDirection.DOWNLOAD.ordinal()];
    }

    /**
     * Returns the bandwidth used by this download stream.
     *
     * @return the last used upload bandwidth computed (in Kbit/s).
     */
    public double getUploadRateKiloBitPerSec()
    {
        return this.rateKiloBitPerSec[StreamDirection.UPLOAD.ordinal()];
    }

    /**
     * Returns the jitter average of this download stream.
     *
     * @return the last jitter average computed (in ms).
     */
    public double getDownloadJitterMs()
    {
        return this.getJitterMs(StreamDirection.DOWNLOAD);
    }

    /**
     * Returns the jitter average of this upload stream.
     *
     * @return the last jitter average computed (in ms).
     */
    public double getUploadJitterMs()
    {
        return this.getJitterMs(StreamDirection.UPLOAD);
    }

    /**
     * Returns the jitter average of this upload/download stream.
     *
     * @param streamDirection The stream direction (DOWNLOAD or UPLOAD) of the
     * stream from which this function retrieve the jitter.
     *
     * @return the last jitter average computed (in ms).
     */
    private double getJitterMs(StreamDirection streamDirection)
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
        return (this.jitterRTPTimestampUnits[streamDirection.ordinal()]
                / mediaFormatClockRate) * 1000.0;
    }

    /**
     * Updates the jitter stream stats with the new feedback sent.
     *
     * @param feedback The last RTCP feedback sent by the MediaStream.
     * @param streamDirection The stream direction (DOWNLOAD or UPLOAD) of the
     * stream from which this function retrieve the jitter.
     */
    private void updateJitterRTPTimestampUnits(
            RTCPFeedback feedback,
            StreamDirection streamDirection)
    {
        // Updates the download jitter in RTP timestamp units.
        // There is no need to compute a jitter average, since (cf. RFC3550,
        // section 6.4.1 SR: Sender Report RTCP Packet, subsection interarrival
        // jitter: 32 bits) the value contained in the RTCP sender report packet
        // contains a mean deviation of the jitter.
        this.jitterRTPTimestampUnits[streamDirection.ordinal()] =
            feedback.getJitter();
    }

    /**
     * Updates this stream stats with the new feedback sent.
     *
     * @param feedback The last RTCP feedback sent by the MediaStream.
     */
    public void updateNewSentFeedback(RTCPFeedback feedback)
    {
        updateJitterRTPTimestampUnits(feedback, StreamDirection.DOWNLOAD);

        // No need to update the download loss has we have a more accurate value
        // in the global reception stats, which are updated for each new packet
        // received.
    }

    /**
     * Updates this stream stats with the new feedback received.
     *
     * @param feedback The last RTCP feedback received by the MediaStream.
     */
    public void updateNewReceivedFeedback(RTCPFeedback feedback)
    {
        StreamDirection streamDirection = StreamDirection.UPLOAD;

        updateJitterRTPTimestampUnits(feedback, streamDirection);

        // Updates the loss rate with the RTCP sender report feedback, since
        // this is the only information source available for the upalod stream.
        long uploadNewNbRecv = feedback.getXtndSeqNum();
        long newNbLost =
            feedback.getNumLost() - this.nbLost[streamDirection.ordinal()];
        long nbSteps = uploadNewNbRecv - this.uploadFeedbackNbPackets;

        updateNbLoss(streamDirection, newNbLost, nbSteps);

        // Updates the upload loss counters.
        this.uploadFeedbackNbPackets = uploadNewNbRecv;
    }

    /**
     * Updates the number of loss for a given stream.
     *
     * @param streamDirection The stream direction (DOWNLOAD or UPLOAD) of the
     * stream from which this function updates the stats.
     * @param newNbLost The last update of the number of lost.
     * @param nbSteps The number of elasped steps since the last number of loss
     * update.
     */
    private void updateNbLoss(
            StreamDirection streamDirection,
            long newNbLost,
            long nbSteps)
    {
        int streamDirectionIndex = streamDirection.ordinal();

        double newPercentLoss = MediaStreamStatsImpl.computePercentLoss(
                nbSteps,
                newNbLost);
        this.percentLoss[streamDirectionIndex] =
            MediaStreamStatsImpl.computeEWMA(
                    nbSteps,
                    this.percentLoss[streamDirectionIndex],
                    newPercentLoss);

        // Saves the last update number download lost value.
        this.nbLost[streamDirectionIndex] += newNbLost;
    }

    /**
     * Computes the loss rate.
     *
     * @param nbLostAndRecv The number of lost and received packets.
     * @param nbLost The number of lost packets.
     *
     * @return The loss rate in percent.
     */
    private static double computePercentLoss(long nbLostAndRecv, long nbLost)
    {
        if(nbLostAndRecv == 0)
        {
            return 0;
        }
        return ((double) 100 * nbLost) / ((double)(nbLostAndRecv));
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
     * Returns the number of Protocol Data Units (PDU) sent/received since the
     * beginning of the session.
     *
     * @param streamDirection The stream direction (DOWNLOAD or UPLOAD) of the
     * stream from which this function retrieve the number of sent/received
     * packets.
     *
     * @return the number of packets sent/received for this stream.
     */
    private long getNbPDU(StreamDirection streamDirection)
    {
        long nbPDU = 0;
        StreamRTPManager rtpManager = this.mediaStreamImpl.getRTPManager();

        if(rtpManager != null)
        {
            switch(streamDirection)
            {
                case UPLOAD:
                    nbPDU =
                        rtpManager.getGlobalTransmissionStats().getRTPSent();
                    break;
                case DOWNLOAD:
                    GlobalReceptionStats globalReceptionStats =
                        rtpManager.getGlobalReceptionStats();
                    nbPDU =
                        globalReceptionStats.getPacketsRecd()
                        - globalReceptionStats.getRTCPRecd();
                    break;
            }
        }
        return nbPDU;
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
     * Returns the number of sent/received bytes since the beginning of the
     * session.
     *
     * @param streamDirection The stream direction (DOWNLOAD or UPLOAD) of the
     * stream from which this function retrieve the number of sent/received
     * bytes.
     *
     * @return the number of sent/received bytes for this stream.
     */
    private long getNbBytes(StreamDirection streamDirection)
    {
        long nbBytes = 0;
        StreamRTPManager rtpManager = this.mediaStreamImpl.getRTPManager();

        if(rtpManager != null)
        {
            switch(streamDirection)
            {
                case DOWNLOAD:
                    nbBytes =
                        rtpManager.getGlobalReceptionStats().getBytesRecd();
                    break;
                case UPLOAD:
                    nbBytes =
                        rtpManager.getGlobalTransmissionStats().getBytesSent();
                    break;
            }
        }
        return nbBytes;
    }
}
