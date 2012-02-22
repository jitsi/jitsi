/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.swing.*;

import java.awt.*;
import java.text.*;
import javax.swing.*;

/**
 * The <tt>CallPeerInfoPanel</tt> is a label displaying information about the
 * callpeer streams (audio and video): ip address, port, codec name, codec clock
 * rate, bandwidth used and loss rate.
 *
 * @author Vincent Lucas
 */
public class CallPeerInfoPanel
    extends TransparentPanel
{
    /**
     * The instance of CallPeer monitored to get the stream information.
     */
    private CallPeer callPeer;

    /**
     * The label displaying the audio statistics.
     */
    private JLabel[] audioLabel;

    /**
     * The textfield displaying the audio statistics.
     */
    private JTextField[] audioTextField;

    /**
     * The label displaying the video statistics.
     */
    private JLabel[] videoLabel;

    /**
     * The textfield displaying the video statistics.
     */
    private JTextField[] videoTextField;

    /**
     * The label displayed when there is neither audio nor the video data
     * available.
     */
    private JLabel voidLabel;

    /**
     * Creates a new instance of a <tt>CallPeerInfoLabel</tt> linked to a
     * <tt>CallPeer</tt>.
     *
     * @param callPeer The instance of CallPeer monitored to get the stream
     * information.
     */
    public CallPeerInfoPanel(CallPeer callPeer)
    {
        this.callPeer = callPeer;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.add(new JLabel(
                    "Call stream for " + callPeer.getDisplayName() + ": "));

        this.voidLabel = new JLabel("No information available");
        this.add(voidLabel);

        this.audioLabel = new JLabel[12];
        this.audioTextField = new JTextField[12];
        for(int i = 0; i < audioLabel.length; ++i)
        {
            this.audioLabel[i] = new JLabel();
            this.audioLabel[i].setVisible(false);
            this.add(this.audioLabel[i]);
            this.audioTextField[i] = new JTextField();
            this.audioTextField[i].setEditable(false);
            this.audioTextField[i].setVisible(false);
            this.add(this.audioTextField[i]);
        }

        this.videoLabel = new JLabel[12];
        this.videoTextField = new JTextField[12];
        for(int i = 0; i < videoLabel.length; ++i)
        {
            this.videoLabel[i] = new JLabel();
            this.videoLabel[i].setVisible(false);
            this.add(this.videoLabel[i]);
            this.videoTextField[i] = new JTextField();
            this.videoTextField[i].setEditable(false);
            this.videoTextField[i].setVisible(false);
            this.add(this.videoTextField[i]);
        }

        this.updateInfos();
    }

    /**
     * Computes and updates information about this CallPeer streams.
     */
    public void updateInfos()
    {
        long startTime = callPeer.getCallDurationStartTime();
        long currentTime = System.currentTimeMillis();
        long callNbTimeMsSpent = currentTime - startTime;

        String peerAddress = callPeer.getAddress();

        if(callPeer instanceof MediaAwareCallPeer)
        {
            CallPeerMediaHandler callPeerMediaHandler =
                ((MediaAwareCallPeer) callPeer).getMediaHandler();
            if(callPeerMediaHandler != null)
            {
                MediaStream mediaStream;

                mediaStream = callPeerMediaHandler.getStream(MediaType.AUDIO);
                CallPeerInfoPanel.updateStats(
                        this.audioLabel,
                        this.audioTextField,
                        mediaStream);

                mediaStream = callPeerMediaHandler.getStream(MediaType.VIDEO);
                CallPeerInfoPanel.updateStats(
                        this.videoLabel,
                        this.videoTextField,
                        mediaStream);
            }

            // Set the void label invisible, if there is at least one stream
            // connected from which we have information to display.
            if((this.audioLabel[0] != null
                        && this.audioLabel[0].isVisible())
                    || (this.videoLabel[0] != null
                        && this.videoLabel[0].isVisible()))
            {
                this.voidLabel.setVisible(false);
            }
            // If there is nothing to display set this panel to not visible.
            else
            {
                this.voidLabel.setVisible(true);
            }
        }
    }

    /**
     * Updates the stats of a MediaStream and the label associated.
     *
     * @param label The labels to update with the new information.
     * @param textField The text fields to update with the new information.
     * @param mediaStream The MediaStream used to get the stats.
     */
    private static void updateStats(
            JLabel[] label,
            JTextField[] textField,
            MediaStream mediaStream)
    {
        boolean setVisible = false;

        // If there is an audio stream, then updates this stream
        // information.
        if(mediaStream != null)
        {
            MediaStreamStats mediaStreamStats =
                mediaStream.getMediaStreamStats();
            if(mediaStreamStats != null)
            {
                mediaStreamStats.updateStats();
                CallPeerInfoPanel.updateLabel(
                        label,
                        textField,
                        mediaStreamStats);
                setVisible = true;
            }
        }
        for(int i = 0; i < label.length; ++i)
        {
            label[i].setVisible(setVisible);
            textField[i].setVisible(setVisible);
        }
    }

    /**
     * Updates the JLabel with the information for a specific stream stats
     * (audio or video) of this CallPeer.
     *
     * @param label The labels to update with the new information.
     * @param textField The text fields to update with the new information.
     * @param lastStats The stats (the number of packet received,
     * the bandwidth used and the loss rate) compute at the last update.
     */
    private static void updateLabel(
            JLabel[] label,
            JTextField[] textField,
            MediaStreamStats lastStats)
    {
        // Gets this stream encoding.
        String encoding = lastStats.getEncoding();
        // Gets this stream encoding clock rate.
        String encodingClockRate = lastStats.getEncodingClockRate();

        // Gets this stream local IP address.
        String localIPAddress = lastStats.getLocalIPAddress();
        // Gets this stream local port.
        int localPort = lastStats.getLocalPort();

        // Gets this stream remote IP address.
        String remoteIPAddress = lastStats.getRemoteIPAddress();
        // Gets this stream remote port.
        int remotePort = lastStats.getRemotePort();

        // Computes the download loss rate for this stream.
        double downloadPercentLost = lastStats.getDownloadPercentLost();
        // Computes the doawnload bandwidth used by this stream.
        double downloadRateKiloBitPerSec =
            lastStats.getDownloadRateKiloBitPerSec();
        // Computes the download jitter used by this stream.
        double downloadJitterMs = lastStats.getDownloadJitterMs();

        // Computes the upload loss rate for this stream.
        double uploadPercentLost = lastStats.getUploadPercentLost();
        // Computes the doawnload bandwidth used by this stream.
        double uploadRateKiloBitPerSec =
            lastStats.getUploadRateKiloBitPerSec();
        // Computes the upload jitter used by this stream.
        double uploadJitterMs = lastStats.getUploadJitterMs();

        // Updates the label.
        label[0].setText("Codec: ");
        textField[0].setText(encoding);
        label[1].setText("Codec frequency (Hz): ");
        textField[1].setText(encodingClockRate);
        label[2].setText("Local IP: ");
        textField[2].setText(localIPAddress);
        label[3].setText("Local Port: ");
        textField[3].setText(localPort + "");
        label[4].setText("Remote IP: ");
        textField[4].setText(remoteIPAddress);
        label[5].setText("remote Port: ");
        textField[5].setText(remotePort + "");
        label[6].setText("Download rate (Kbps): ");
        textField[6].setText(((int) downloadRateKiloBitPerSec) + "");
        label[7].setText("Download lost rate (%): ");
        textField[7].setText(((int) downloadPercentLost) + "");
        label[8].setText("Download jitter (ms): ");
        textField[8].setText(downloadJitterMs + "");
        label[9].setText("Upload rate (Kbps): ");
        textField[9].setText(((int) uploadRateKiloBitPerSec) + "");
        label[10].setText("Upload lost rate (%): ");
        textField[10].setText(((int) uploadPercentLost) + "");
        label[11].setText("Upload jitter (ms): ");
        textField[11].setText(uploadJitterMs + "");
    }

    /**
     * Returns the call peer ID which information are displayed by this panel.
     *
     * @return the call peer ID which information are displayed by this panel.
     */
    public String getCallPeerID()
    {
        return this.callPeer.getPeerID();
    }
}
