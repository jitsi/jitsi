/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import net.java.sip.communicator.service.neomedia.*;

import java.net.*;
import java.util.*;
import javax.media.rtp.*;
import javax.media.rtp.rtcp.*;

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
    private int downloadNbPackets = 0;

    /**
     * The last number of download lost packets.
     */
    private int downloadNbLost = 0;

    /**
     * The last number of received Bytes.
     */
    private long downloadNbByte = 0;

    /**
     * The last download loss rate computed (in %).
     */
    private double downloadPercentLost = 0;

    /**
     * The last used bandwidth computed in download (in Kbit/s).
     */
    private double downloadRateKiloBitPerSec = 0;

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
        int downloadNewNbLost = this.getDownloadNbPDULost();
        // Gets the current number of packets correctly received since the
        // beginning of this stream.
        int downloadNewNbRecv = this.getDownloadNbPDUProcessed();
        // Gets the number of byte received since the beginning of this
        // stream.
        long downloadNewNbByteRecv = this.getNbByteReceived();
        
        // Computes the number of update steps which has not been done since
        // last update.
        int downloadNbSteps = downloadNewNbRecv - this.downloadNbPackets;

        // Computes the loss rate for this stream.
        double newPercentLost = MediaStreamStatsImpl.computePercentLost(
                downloadNewNbRecv - this.downloadNbPackets,
                downloadNewNbLost - this.downloadNbLost);
        this.downloadPercentLost = MediaStreamStatsImpl.computeEWMA(
                downloadNbSteps,
                this.downloadPercentLost,
                newPercentLost);

        // Computes the bandwidth used by this stream.
        double newRateKiloBitPerSec =
            MediaStreamStatsImpl.computeRateKiloBitPerSec(
                downloadNewNbByteRecv - this.downloadNbByte,
                currentTimeMs - this.updateTimeMs);
        this.downloadRateKiloBitPerSec = MediaStreamStatsImpl.computeEWMA(
                downloadNbSteps,
                this.downloadRateKiloBitPerSec,
                newRateKiloBitPerSec);

        // Saves the last update values.
        this.updateTimeMs = currentTimeMs;
        this.downloadNbLost = downloadNewNbLost;
        this.downloadNbPackets = downloadNewNbRecv;
        this.downloadNbByte = downloadNewNbByteRecv;
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
     * Returns the percent lost of the download stream.
     *
     * @return the last loss rate computed (in %).
     */
    public double getDownloadPercentLost()
    {
        return this.downloadPercentLost;
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
     * Computes the loss rate.
     *
     * @param nbRecv The number of received packets.
     * @param nbLost The number of lost packets.
     *
     * @return The loss rate in percent.
     */
    private static double computePercentLost(int nbRecv, int nbLost)
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
    private int getDownloadNbPDUProcessed()
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
     * Returns the number of Protocol Data Units (PDU) lost in download since
     * the beginning of the session.
     *
     * @return the number of packets lost for this stream.
     */
    private int getDownloadNbPDULost()
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
    private long getNbByteReceived()
    {
        StreamRTPManager rtpManager = this.mediaStreamImpl.getRTPManager();

        if(rtpManager == null)
        {
            return 0;
        }
        return rtpManager.getGlobalReceptionStats().getBytesRecd();
    }

    /**
     * Returns the percent lost of the upload stream.
     *
     * @return the last loss rate computed (in %).
     */
    public double getUploadPercentLost()
    {
        // TODO: compute this stat.
        return -1;
    }

    /**
     * Returns the bandwidth used by this download stream.
     *
     * @return the last used upload bandwidth computed (in Kbit/s).
     */
    public double getUploadRateKiloBitPerSec()
    {
        // TODO: compute this stat.
        return -1;
    }

    /**
     * Returns the jitter average of this download stream.
     *
     * @return the last jitter average computed (in ms).
     */
    public double getDownloadJitterMs()
    {
        // TODO: compute this stat.
        return -1;
    }

    /**
     * Returns the jitter average of this upload stream.
     *
     * @return the last jitter average computed (in ms).
     */
    public double getUploadJitterMs()
    {
        // TODO: compute this stat.
        return -1;
    }
}
