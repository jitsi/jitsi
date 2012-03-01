/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import com.explodingpixels.macwidgets.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The frame displaying the statistical information for a call.
 *
 * @author Vincent Lucas
 * @author Yana Stamcheva
 */
public class CallInfoFrame
    implements CallTitleListener
{
    /**
     * The Call from which are computed the statisics displayed.
     */
    private Call call;

    /**
     * The call info window.
     */
    private final JDialog callInfoWindow;

    /**
     * The information text pane.
     */
    private final JEditorPane infoTextPane;

    /**
     * The font color.
     */
    private String fontColor;

    /**
     * The resource management service.
     */
    private final ResourceManagementService resources
        = GuiActivator.getResources();

    /**
     * Creates a new frame containing the statistical information for a call.
     *
     * @param call The call from which are computed the statistics displayed.
     */
    public CallInfoFrame(Call call)
    {
        this.call = call;

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        infoTextPane = createGeneralInfoPane();
        scrollPane.getViewport().add(infoTextPane);

        callInfoWindow
            = createCallInfoWindow(
                GuiActivator.getResources().getI18NString(
                    "service.gui.callinfo.TECHNICAL_CALL_INFO"));

        callInfoWindow.getContentPane().add(scrollPane);

        this.constructCallInfo();
    }

    /**
     * Creates different types of windows depending on the operating system.
     *
     * @param title the title of the created window
     */
    private JDialog createCallInfoWindow( String title)
    {
        JDialog callInfoWindow = null;

        if (OSUtils.IS_MAC)
        {
            HudWindow window = new HudWindow();

            JDialog dialog = window.getJDialog();
            dialog.setTitle(title);

            callInfoWindow = window.getJDialog();
            callInfoWindow.setResizable(true);

            fontColor = "FFFFFF";
        }
        else
        {
            SIPCommDialog dialog = new SIPCommDialog(false);

            callInfoWindow = dialog;
            callInfoWindow.setTitle(title);

            fontColor = "000000";
        }

        return callInfoWindow;
    }

    /**
     * Create call info text pane.
     *
     * @return the created call info text pane
     */
    private JEditorPane createGeneralInfoPane()
    {
        JEditorPane infoTextPane = new JEditorPane();

        /*
         * Make JEditorPane respect our default font because we will be using it
         * to just display text.
         */
        infoTextPane.putClientProperty(
                JEditorPane.HONOR_DISPLAY_PROPERTIES,
                true);

        infoTextPane.setOpaque(false);
        infoTextPane.setEditable(false);
        infoTextPane.setContentType("text/html");

        return infoTextPane;
    }

    /**
     * Returns an HTML string corresponding to the given labelText and infoText,
     * that could be easily added to the information text pane.
     *
     * @param labelText the label text that would be shown in bold
     * @param infoText the info text that would be shown in plain text
     * @return the newly constructed HTML string
     */
    private String getLineString(String labelText, String infoText)
    {
        return "<b>" + labelText + "</b> : " + infoText + "<br/>";
    }

    /**
     * Constructs the call info text.
     */
    private void constructCallInfo()
    {
        StringBuffer stringBuffer = new StringBuffer();

        stringBuffer.append(
            "<html><body><p align=\"left\">"
                + "<font color=\"" + fontColor + "\" size=\"3\">");

        stringBuffer.append(getLineString(resources.getI18NString(
            "service.gui.callinfo.CALL_INFORMATION"), ""));

        stringBuffer.append(getLineString(resources.getI18NString(
            "service.gui.callinfo.CALL_IDENTITY"),
            call.getProtocolProvider().getAccountID().getDisplayName()));

        int callPeerCount = call.getCallPeerCount();
        if (call.getCallPeerCount() > 1)
            stringBuffer.append(
                getLineString(resources.getI18NString(
                    "service.gui.callinfo.PEER_COUNT"),
                    String.valueOf(callPeerCount)));

        boolean isConfFocus = call.isConferenceFocus();

        if (isConfFocus)
            stringBuffer.append(getLineString(
                resources.getI18NString(
                    "service.gui.callinfo.IS_CONFERENCE_FOCUS"),
                String.valueOf(isConfFocus)));

        TransportProtocol preferredTransport =
            call.getProtocolProvider().getTransportProtocol();

        if (preferredTransport != TransportProtocol.UNKNOWN)
            stringBuffer.append(getLineString(
                resources.getI18NString("service.gui.callinfo.CALL_TRANSPORT"),
                preferredTransport.toString()));

        constructCallPeersInfo(stringBuffer);

        stringBuffer.append("</font></p></body></html>");

        infoTextPane.setText(stringBuffer.toString());
        infoTextPane.revalidate();
        infoTextPane.repaint();
    }

    /**
     * Constructs call peers' info.
     *
     * @param stringBuffer the <tt>StringBuffer</tt>, where call peer info will
     * be added
     */
    private void constructCallPeersInfo(StringBuffer stringBuffer)
    {
        Iterator<? extends CallPeer> callPeers = this.call.getCallPeers();

        while(callPeers.hasNext())
        {
            CallPeer callPeer = (CallPeer) callPeers.next();

            stringBuffer.append("<br/>");
            constructPeerInfo(callPeer, stringBuffer);
        }
    }

    /**
     * Constructs peer info.
     *
     * @param callPeer the <tt>CallPeer</tt>, for which we'll construct the info
     * @param stringBuffer the <tt>StringBuffer</tt>, where call peer info will
     * be added
     */
    private void constructPeerInfo(CallPeer callPeer, StringBuffer stringBuffer)
    {
        Date startTime = new Date(callPeer.getCallDurationStartTime());
        Date currentTime = new Date(System.currentTimeMillis());
        Date callNbTimeMsSpent = GuiUtils.substractDates(
                currentTime,
                startTime);

        String peerAddress = callPeer.getAddress();

        stringBuffer.append(getLineString(peerAddress, ""));
        stringBuffer.append(getLineString(
            resources.getI18NString("service.gui.callinfo.CALL_DURATION"),
            GuiUtils.formatTime(callNbTimeMsSpent)));

        if(callPeer instanceof MediaAwareCallPeer)
        {
            CallPeerMediaHandler callPeerMediaHandler
                = ((MediaAwareCallPeer) callPeer).getMediaHandler();

            if(callPeerMediaHandler != null)
            {
                MediaStream mediaStream;

                mediaStream = callPeerMediaHandler.getStream(MediaType.AUDIO);

                if (mediaStream != null)
                {
                    stringBuffer.append("<br/>");
                    stringBuffer.append(getLineString(resources.getI18NString(
                        "service.gui.callinfo.AUDIO_INFO"), ""));
                    constructAudioVideoInfo(mediaStream, stringBuffer);
                }

                mediaStream = callPeerMediaHandler.getStream(MediaType.VIDEO);

                if (mediaStream != null)
                {
                    stringBuffer.append("<br/>");
                    stringBuffer.append(getLineString(resources.getI18NString(
                        "service.gui.callinfo.VIDEO_INFO"), ""));
                    constructAudioVideoInfo(mediaStream, stringBuffer);
                }
            }
        }
    }

    /**
     * Constructs audio video peer info.
     *
     * @param mediaStream the <tt>MediaStream</tt> that gives us access to
     * audio video info
     * @param stringBuffer the <tt>StringBuffer</tt>, where call peer info will
     * be added
     */
    private void constructAudioVideoInfo(   MediaStream mediaStream,
                                            StringBuffer stringBuffer)
    {
        MediaStreamStats mediaStreamStats
            = mediaStream.getMediaStreamStats();

        if(mediaStreamStats == null)
            return;

        mediaStreamStats.updateStats();

        stringBuffer.append(
            getLineString(
                resources.getI18NString("service.gui.callinfo.CODEC"),
                mediaStreamStats.getEncoding()
                + " / " + mediaStreamStats.getEncodingClockRate() + " Hz"));

        stringBuffer.append(
            getLineString(
                resources.getI18NString("service.gui.callinfo.LOCAL_IP"),
                mediaStreamStats.getLocalIPAddress()
                + " / " + String.valueOf(mediaStreamStats.getLocalPort())));

        stringBuffer.append(
            getLineString(
                resources.getI18NString("service.gui.callinfo.REMOTE_IP"),
                mediaStreamStats.getRemoteIPAddress()
                + " / " + String.valueOf(mediaStreamStats.getRemotePort())));

        stringBuffer.append(
            getLineString(
                resources.getI18NString(
                    "service.gui.callinfo.BANDWITH"),
                    "&darr; "
                    + (int) mediaStreamStats.getDownloadRateKiloBitPerSec()
                        + " Kbps "
                    + " &uarr; "
                    + (int) mediaStreamStats.getUploadRateKiloBitPerSec()
                        + " Kbps"));

        stringBuffer.append(
            getLineString(
                resources.getI18NString("service.gui.callingo.LOSS_RATE"),
                    "&darr; " + (int) mediaStreamStats.getDownloadPercentLoss()
                    + "% &uarr; "
                    + (int) mediaStreamStats.getUploadPercentLoss()
                    + "%"));

        stringBuffer.append(
            getLineString(resources.getI18NString("service.gui.callingo.JITTER"),
                "&darr; " + mediaStreamStats.getDownloadJitterMs()
                + " ms &uarr; "
                + mediaStreamStats.getUploadJitterMs() + " ms"));
    }

    /**
     * Called when the title of the given CallPanel changes.
     *
     * @param callContainer the <tt>CallContainer</tt>, which title has changed
     */
    public void callTitleChanged(CallPanel callContainer)
    {
        String selectedText = infoTextPane.getSelectedText();

        // If there's a selection do not update call info, otherwise the user
        // would not be able to copy the selected text.
        if (selectedText != null && selectedText.length() > 0)
            return;

        constructCallInfo();
        callInfoWindow.pack();
    }

    /**
     * Shows/hides the corresponding window.
     *
     * @param isVisible <tt>true</tt> to show the window, <tt>false</tt> to
     * hide it
     */
    public void setVisible(boolean isVisible)
    {
        if (isVisible)
        {
            callInfoWindow.pack();
            callInfoWindow.setPreferredSize(new Dimension(300, 450));
            callInfoWindow.setSize(300, 450);
            callInfoWindow.setLocationRelativeTo(null);
        }

        callInfoWindow.setVisible(isVisible);
    }

    /**
     * Indicates if the corresponding window is visible.
     *
     * @return <tt>true</tt> if the window is visible, <tt>false</tt> -
     * otherwise
     */
    public boolean isVisible()
    {
        return callInfoWindow.isVisible();
    }

    /**
     * Disposes the corresponding window.
     */
    public void dispose()
    {
        callInfoWindow.dispose();
    }
}
