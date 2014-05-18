/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.security.cert.*;
import javax.swing.*;
import org.jitsi.service.resources.*;

/**
 * Frame for showing information about a certificate.
 */
public class ViewCertificateFrame
    extends SIPCommFrame
{

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The resource service.
     */
    private final ResourceManagementService R = DesktopUtilActivator.getResources();

    /**
     * The maximum width that we allow message dialogs to have.
     */
    private static final int MAX_MSG_PANE_WIDTH = 600;

    /**
     * The maximum height that we allow message dialogs to have.
     */
    private static final int MAX_MSG_PANE_HEIGHT = 800;

    /**
     * The certificate to show.
     */
    Certificate cert;

    /**
     * A text that describes why the verification failed.
     */
    String message;

    /**
     * The certificate panel.
     */
    TransparentPanel certPanel;

    /**
     * This dialog content pane.
     */
    TransparentPanel contentPane;

    /**
     * Creates the dialog.
     *
     * @param certs the certificates list
     * @param title The title of the dialog; when null the resource
     * <tt>service.gui.CERT_DIALOG_TITLE</tt> is loaded.
     * @param message A text that describes why the verification failed.
     */
    public ViewCertificateFrame(Certificate[] certs,
                                       String title, String message)
    {
        super(false);

        setTitle(title != null ? title :
            R.getI18NString("service.gui.CERT_DIALOG_TITLE"));

        // for now shows only the first certificate from the chain
        this.cert = certs[0];
        this.message = message;

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        init();

        setLocationRelativeTo(getParent());
    }

    /**
     * Inits the dialog initial display.
     */
    private void init()
    {
        this.getContentPane().setLayout(new BorderLayout());

        contentPane =
            new TransparentPanel(new BorderLayout(5, 5));

        TransparentPanel northPanel =
            new TransparentPanel(new BorderLayout(5, 5));
        northPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));

        JLabel imgLabel = new JLabel(
            R.getImage("service.gui.icons.CERTIFICATE_WARNING"));
        imgLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        northPanel.add(imgLabel, BorderLayout.WEST);

        StyledHTMLEditorPane descriptionPane = new StyledHTMLEditorPane();
        descriptionPane.setOpaque(false);
        descriptionPane.setEditable(false);
        descriptionPane.setContentType("text/html");
        descriptionPane.setText(message);
        descriptionPane.setSize(
                    new Dimension(MAX_MSG_PANE_WIDTH, MAX_MSG_PANE_HEIGHT));
        int height = descriptionPane.getPreferredSize().height;
        descriptionPane.setPreferredSize(
                    new Dimension(MAX_MSG_PANE_WIDTH, height));

        northPanel.add(descriptionPane, BorderLayout.CENTER);
        contentPane.add(northPanel, BorderLayout.NORTH);

        certPanel = new TransparentPanel(new BorderLayout());
        contentPane.add(certPanel, BorderLayout.CENTER);

        this.getContentPane().add(contentPane, BorderLayout.CENTER);

        Component certInfoPane;
        if(cert instanceof X509Certificate)
        {
            certInfoPane = new X509CertificatePanel((X509Certificate)cert);
        }
        else
        {
            JTextArea textArea = new JTextArea();
            textArea.setOpaque(false);
            textArea.setEditable(false);
            textArea.setText(cert.toString());
            certInfoPane = textArea;
        }

        final JScrollPane certScroll = new JScrollPane(certInfoPane);
        certScroll.setPreferredSize(new Dimension(300, 600));
        certPanel.add(certScroll, BorderLayout.CENTER);

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                certScroll.getVerticalScrollBar().setValue(0);
            }
        });
        setPreferredSize(null);
        
        pack();
    }

}
