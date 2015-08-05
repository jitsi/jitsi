/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.connectioninfo;

import java.awt.*;
import java.security.cert.*;

import javax.swing.*;
import javax.swing.event.*;
import net.java.sip.communicator.plugin.connectioninfo.ConnectionInfoMenuItemComponent.*;

import net.java.sip.communicator.plugin.desktoputil.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.resources.*;

/**
 * The main panel that allows users to view and edit their account information.
 * Different instances of this class are created for every registered
 * <tt>ProtocolProviderService</tt>.
 * Currently, supported account details are first/middle/last names, nickname,
 * street/city/region/country address, postal code, birth date, gender,
 * organization name, job title, about me, home/work email, home/work phone.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 * @author Marin Dzhigarov
 * @author Markus Kilas
 */
public class ConnectionDetailsPanel
    extends TransparentPanel
    implements HyperlinkListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The logger
     */
    private final Logger logger = Logger.getLogger(
            ConnectionDetailsPanel.class);

    private final ResourceManagementService R;

    /**
     * The <tt>ProtocolProviderService</tt> that this panel is associated with.
     */
    final ProtocolProviderService protocolProvider;

    /**
     * The parent dialog.
     */
    private final ConnectionInfoDialog dialog;

    /**
     * The information text pane.
     */
    private final JEditorPane infoTextPane;

    /**
     * Dummy URL to indicate that the certificate should be displayed.
     */
    private final String CERTIFICATE_URL = "jitsi://viewCertificate";

    /**
     * Construct a panel containing all account details for the given protocol
     * provider.
     *
     * @param dialog the parent dialog
     * @param protocolProvider the protocol provider service
     */
    public ConnectionDetailsPanel(ConnectionInfoDialog dialog,
            ProtocolProviderService protocolProvider)
    {
        this.dialog = dialog;
        this.R = ConnectionInfoActivator.R;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        this.setPreferredSize(new Dimension(600, 400));
        this.protocolProvider = protocolProvider;
        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        infoTextPane = new JEditorPane();

        // Make JEditorPane respect our default font because we will be using it
        // to just display text.
        infoTextPane.putClientProperty(
                JEditorPane.HONOR_DISPLAY_PROPERTIES,
                true);

        infoTextPane.setOpaque(false);
        infoTextPane.setEditable(false);
        infoTextPane.setContentType("text/html");
        infoTextPane.addHyperlinkListener(this);

        if (protocolProvider.isRegistered())
        {
            loadDetails();
        }

        this.add(infoTextPane);
    }

    /**
     * Constructs the connection info text.
     */
    public void loadDetails()
    {
        final StringBuilder buff = new StringBuilder();

        buff.append(
            "<html><body><p align=\"left\">"
                + "<font size=\"3\">");

        // Protocol name
        buff.append(getLineString(R.getI18NString(
                "service.gui.PROTOCOL"), protocolProvider.getProtocolName()));

        // Server address and port
        final OperationSetConnectionInfo opSetConnectionInfo = protocolProvider
                .getOperationSet(OperationSetConnectionInfo.class);
        if (opSetConnectionInfo != null)
        {
            buff.append(getLineString(R.getI18NString(
                    "service.gui.ADDRESS"),
                    opSetConnectionInfo.getServerAddress() == null ?
                            "" : opSetConnectionInfo.getServerAddress()
                                    .getHostName()));
            buff.append(getLineString(R.getI18NString(
                    "service.gui.PORT"),
                    opSetConnectionInfo.getServerAddress() == null ?
                            "" : String.valueOf(opSetConnectionInfo
                                    .getServerAddress().getPort())));
        }

        // Transport protocol
        TransportProtocol preferredTransport
            = protocolProvider.getTransportProtocol();

        if (preferredTransport != TransportProtocol.UNKNOWN)
            buff.append(getLineString(
                R.getI18NString("service.gui.callinfo.CALL_TRANSPORT"),
                preferredTransport.toString()));

        // TLS information
        final OperationSetTLS opSetTls = protocolProvider
                .getOperationSet(OperationSetTLS.class);
        if (opSetTls != null)
        {
            buff.append(getLineString(
                    R.getI18NString(
                    "service.gui.callinfo.TLS_PROTOCOL"),
                    opSetTls.getProtocol()));
            buff.append(getLineString(
                    R.getI18NString(
                    "service.gui.callinfo.TLS_CIPHER_SUITE"),
                    opSetTls.getCipherSuite()));

            buff.append("<b><a href=\"")
                .append(CERTIFICATE_URL)
                .append("\">")
                .append(R.getI18NString(
                        "service.gui.callinfo.VIEW_CERTIFICATE"))
                .append("</a></b><br/>");
        }

        buff.append("</font></p></body></html>");

        infoTextPane.setText(buff.toString());
        infoTextPane.revalidate();
        infoTextPane.repaint();
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
     * Invoked when user clicks a link in the editor pane.
     * @param e the event
     */
    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        // Handle "View certificate" link
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED
                        && CERTIFICATE_URL.equals(e.getDescription()))
        {
            Certificate[] chain = protocolProvider
                    .getOperationSet(OperationSetTLS.class)
                    .getServerCertificates();

            ViewCertificateFrame certFrame =
                    new ViewCertificateFrame(chain, null, R.getI18NString(
                            "service.gui.callinfo.TLS_CERTIFICATE_CONTENT"));
            certFrame.setVisible(true);
        }
    }

    /**
     * Returns the provider we represent.
     * @return
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return protocolProvider;
    }

}
