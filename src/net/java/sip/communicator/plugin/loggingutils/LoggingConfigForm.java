/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.loggingutils;

import com.sun.jndi.toolkit.url.*;
import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.packetlogging.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import javax.net.ssl.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * The Logging configuration form.
 * @author Damian Minkov
 */
public class LoggingConfigForm
    extends TransparentPanel
    implements ActionListener,
        DocumentListener
{
    /**
     * Our Logger.
     */
    private static final Logger logger
            = Logger.getLogger(LoggingConfigForm.class);

    /**
     * The enable packet logging check box.
     */
    private JCheckBox enableCheckBox;

    /**
     * Check box to enable/disable packet debug of sip protocol.
     */
    private JCheckBox sipProtocolCheckBox;

    /**
     * Check box to enable/disable packet debug of jabber protocol.
     */
    private JCheckBox jabberProtocolCheckBox;

    /**
     * Check box to enable/disable packet debug of media protocol/RTP.
     */
    private JCheckBox rtpProtocolCheckBox;

    /**
     * Check box to enable/disable packet debug of Ice4J.
     */
    private JCheckBox ice4jProtocolCheckBox;

    /**
     * The file count label.
     */
    private JLabel fileCountLabel;

    /**
     * The filed for file count value.
     */
    private JTextField fileCountField = new JTextField();

    /**
     * The file size label.
     */
    private JLabel fileSizeLabel;

    /**
     * The filed for file size value.
     */
    private JTextField fileSizeField = new JTextField();

    /**
     * Notification event.
     */
    private static final String LOGFILES_ARCHIVED = "LogFilesArchived";

    /**
     * Archive logs button.
     */
    private SIPCommTextButton archiveButton;

    /**
     * Archive logs button.
     */
    private SIPCommTextButton uploadLogsButton;

    /**
     * Creates Packet Logging Config form.
     */
    public LoggingConfigForm()
    {
        super(new BorderLayout());

        init();
        loadValues();

        // Register notification for saved calls.
        if(LoggingUtilsActivator.getNotificationService() != null)
            LoggingUtilsActivator.getNotificationService()
                .registerDefaultNotificationForEvent(
                    LOGFILES_ARCHIVED,
                    NotificationService.ACTION_POPUP_MESSAGE,
                    null,
                    null);
    }

    /**
     * Creating the configuration form
     */
    private void init()
    {
        ResourceManagementService resources =
                LoggingUtilsActivator.getResourceService();

        enableCheckBox = new SIPCommCheckBox(
            resources.getI18NString("plugin.loggingutils.ENABLE_DISABLE"));
        enableCheckBox.addActionListener(this);

        sipProtocolCheckBox = new SIPCommCheckBox(
            resources.getI18NString("plugin.sipaccregwizz.PROTOCOL_NAME"));
        sipProtocolCheckBox.addActionListener(this);

        jabberProtocolCheckBox = new SIPCommCheckBox(
            resources.getI18NString("plugin.jabberaccregwizz.PROTOCOL_NAME"));
        jabberProtocolCheckBox.addActionListener(this);

        String rtpDescription = resources.getI18NString(
            "plugin.loggingutils.PACKET_LOGGING_RTP_DESCRIPTION");
        rtpProtocolCheckBox = new SIPCommCheckBox(
            resources.getI18NString("plugin.loggingutils.PACKET_LOGGING_RTP")
            + " " + rtpDescription);
        rtpProtocolCheckBox.addActionListener(this);
        rtpProtocolCheckBox.setToolTipText(rtpDescription);

        ice4jProtocolCheckBox = new SIPCommCheckBox(
            resources.getI18NString("plugin.loggingutils.PACKET_LOGGING_ICE4J"));
        ice4jProtocolCheckBox.addActionListener(this);

        JPanel mainPanel = new TransparentPanel();

        add(mainPanel, BorderLayout.NORTH);

        mainPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        enableCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridx = 0;
        c.gridy = 0;
        mainPanel.add(enableCheckBox, c);

        String label = resources.getI18NString(
                "plugin.loggingutils.PACKET_LOGGING_DESCRIPTION");
        JLabel descriptionLabel = new JLabel(label);
        descriptionLabel.setToolTipText(label);
        enableCheckBox.setToolTipText(label);
        descriptionLabel.setForeground(Color.GRAY);
        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(8));
        c.gridy = 1;
        c.insets = new Insets(0, 25, 10, 0);
        mainPanel.add(descriptionLabel, c);

        final JPanel loggersButtonPanel
            = new TransparentPanel(new GridLayout(0, 1));

        loggersButtonPanel.setBorder(BorderFactory.createTitledBorder(
            resources.getI18NString("service.gui.PROTOCOL")));

        loggersButtonPanel.add(sipProtocolCheckBox);
        loggersButtonPanel.add(jabberProtocolCheckBox);
        loggersButtonPanel.add(rtpProtocolCheckBox);
        loggersButtonPanel.add(ice4jProtocolCheckBox);

        c.insets = new Insets(0, 20, 10, 0);
        c.gridy = 2;
        mainPanel.add(loggersButtonPanel, c);

        final JPanel advancedPanel
            = new TransparentPanel(new GridLayout(0, 2));

        advancedPanel.setBorder(BorderFactory.createTitledBorder(
            resources.getI18NString("service.gui.ADVANCED")));

        fileCountField.getDocument().addDocumentListener(this);
        fileSizeField.getDocument().addDocumentListener(this);

        fileCountLabel = new JLabel(resources.getI18NString(
                "plugin.loggingutils.PACKET_LOGGING_FILE_COUNT"));
        advancedPanel.add(fileCountLabel);
        advancedPanel.add(fileCountField);
        fileSizeLabel = new JLabel(resources.getI18NString(
                "plugin.loggingutils.PACKET_LOGGING_FILE_SIZE"));
        advancedPanel.add(fileSizeLabel);
        advancedPanel.add(fileSizeField);

        c.gridy = 3;
        mainPanel.add(advancedPanel, c);

        archiveButton = new SIPCommTextButton(
            resources.getI18NString("plugin.loggingutils.ARCHIVE_BUTTON"));
        archiveButton.addActionListener(this);

        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 4;
        mainPanel.add(archiveButton, c);

        String uploadLocation =
            LoggingUtilsActivator.getResourceService()
                .getSettingsString("plugin.loggingutils.uploadlocation");

        if(!StringUtils.isNullOrEmpty(uploadLocation))
        {
            uploadLogsButton = new SIPCommTextButton(
                resources.getI18NString("plugin.loggingutils.UPLOAD_LOGS_BUTTON"));
            uploadLogsButton.addActionListener(this);

            c.insets = new Insets(10, 0, 0, 0);
            c.gridy = 5;
            mainPanel.add(uploadLogsButton, c);
        }
    }

    /**
     * Loading the values stored into configuration form
     */
    private void loadValues()
    {
        PacketLoggingService packetLogging =
                LoggingUtilsActivator.getPacketLoggingService();

        enableCheckBox.setSelected(
                packetLogging.getConfiguration().isGlobalLoggingEnabled());

        sipProtocolCheckBox.setSelected(
                packetLogging.getConfiguration().isSipLoggingEnabled());
        jabberProtocolCheckBox.setSelected(
                packetLogging.getConfiguration().isJabberLoggingEnabled());
        rtpProtocolCheckBox.setSelected(
                packetLogging.getConfiguration().isRTPLoggingEnabled());
        ice4jProtocolCheckBox.setSelected(
                packetLogging.getConfiguration().isIce4JLoggingEnabled());
        fileCountField.setText(String.valueOf(
                packetLogging.getConfiguration().getLogfileCount()));
        fileSizeField.setText(String.valueOf(
                packetLogging.getConfiguration().getLimit()/1000));

        updateButtonsState();
    }

    /**
     * Update button enable/disable state according enableCheckBox.
     */
    private void updateButtonsState()
    {
        sipProtocolCheckBox.setEnabled(enableCheckBox.isSelected());
        jabberProtocolCheckBox.setEnabled(enableCheckBox.isSelected());
        rtpProtocolCheckBox.setEnabled(enableCheckBox.isSelected());
        ice4jProtocolCheckBox.setEnabled(enableCheckBox.isSelected());
        fileCountField.setEnabled(enableCheckBox.isSelected());
        fileSizeField.setEnabled(enableCheckBox.isSelected());
        fileSizeLabel.setEnabled(enableCheckBox.isSelected());
        fileCountLabel.setEnabled(enableCheckBox.isSelected());
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();

        PacketLoggingService packetLogging =
                LoggingUtilsActivator.getPacketLoggingService();

        if(source.equals(enableCheckBox))
        {
            // turn it on/off in activator
            packetLogging.getConfiguration().setGlobalLoggingEnabled(
                    enableCheckBox.isSelected());
            updateButtonsState();
        }
        else if(source.equals(sipProtocolCheckBox))
        {
            packetLogging.getConfiguration().setSipLoggingEnabled(
                    sipProtocolCheckBox.isSelected());
        }
        else if(source.equals(jabberProtocolCheckBox))
        {
            packetLogging.getConfiguration().setJabberLoggingEnabled(
                    jabberProtocolCheckBox.isSelected());
        }
        else if(source.equals(rtpProtocolCheckBox))
        {
            packetLogging.getConfiguration().setRTPLoggingEnabled(
                    rtpProtocolCheckBox.isSelected());
        }
        else if(source.equals(ice4jProtocolCheckBox))
        {
            packetLogging.getConfiguration().setIce4JLoggingEnabled(
                    ice4jProtocolCheckBox.isSelected());
        }
        else if(source.equals(archiveButton))
        {
            collectLogs();
        }
        else if(source.equals(uploadLogsButton))
        {
            new Thread(new Runnable()
            {
                public void run()
                {
                    uploadLogs();
                }
            }).start();

        }
    }

    /**
     * Gives notification that there was an insert into the document.  The
     * range given by the DocumentEvent bounds the freshly inserted region.
     *
     * @param e the document event
     */
    public void insertUpdate(DocumentEvent e)
    {
        documentChanged(e);
    }

    /**
     * Gives notification that a portion of the document has been
     * removed.  The range is given in terms of what the view last
     * saw (that is, before updating sticky positions).
     *
     * @param e the document event
     */
    public void removeUpdate(DocumentEvent e)
    {
        documentChanged(e);
    }

    /**
     * Not used.
     *
     * @param e the document event
     */
    public void changedUpdate(DocumentEvent e)
    {}

    /**
     * A change in the text fields.
     * @param e the document event.
     */
    private void documentChanged(DocumentEvent e)
    {
        if(e.getDocument().equals(fileCountField.getDocument()))
        {
            // set file count only if its un integer
            try
            {
                int newFileCount = Integer.valueOf(fileCountField.getText());
                fileCountField.setForeground(Color.black);
                LoggingUtilsActivator.getPacketLoggingService()
                    .getConfiguration().setLogfileCount(newFileCount);
            }
            catch(Throwable t)
            {
                fileCountField.setForeground(Color.red);
            }
        }
        else if(e.getDocument().equals(fileSizeField.getDocument()))
        {
            // set file size only if its un integer
            try
            {
                int newFileSize = Integer.valueOf(fileSizeField.getText());
                fileSizeField.setForeground(Color.black);
                LoggingUtilsActivator.getPacketLoggingService()
                    .getConfiguration().setLimit(newFileSize * 1000);
            }
            catch(Throwable t)
            {
                fileSizeField.setForeground(Color.red);
            }
        }
    }

    /**
     * Asks user for a location to save logs by poping up a file chooser.
     * and archiving logs and saving them on the specified location.
     */
    private void collectLogs()
    {
        ResourceManagementService resources =
                LoggingUtilsActivator.getResourceService();

        SipCommFileChooser fileChooser = GenericFileDialog.create(
            null,
            resources.getI18NString(
                    "plugin.callrecordingconfig.CHOOSE_DIR"),
            SipCommFileChooser.LOAD_FILE_OPERATION);
        fileChooser.setSelectionMode(
                SipCommFileChooser.SAVE_FILE_OPERATION);

        String defaultDir = "";
        try
        {
            defaultDir = LoggingUtilsActivator.getFileAccessService()
                .getDefaultDownloadDirectory().getAbsolutePath()
                + File.separator;
        }
        catch(IOException ex){}
        fileChooser.setStartPath(
                 defaultDir + LogsCollector.getDefaultFileName());

        File dest = fileChooser.getFileFromDialog();

        if(dest == null)
            return;

        dest = LogsCollector.collectLogs(dest);

        NotificationService notificationService
            = LoggingUtilsActivator.getNotificationService();

        if(notificationService != null)
        {
            String bodyMsgKey = null;

            if(dest != null)
                bodyMsgKey = "plugin.loggingutils.ARCHIVE_MESSAGE_OK";
            else
                bodyMsgKey = "plugin.loggingutils.ARCHIVE_MESSAGE_NOTOK";

            notificationService.fireNotification(
                LOGFILES_ARCHIVED,
                resources.getI18NString(
                        "plugin.loggingutils.ARCHIVE_BUTTON"),
                resources.getI18NString(
                        bodyMsgKey,
                        new String[]{dest.getAbsolutePath()}),
                null,
                null);
        }
    }

    /**
     * Upload files to pre-configured url.
     */
    private void uploadLogs()
    {
        try
        {
            File tempDir = LoggingUtilsActivator.getFileAccessService()
                .getTemporaryDirectory();
            File newDest = new File(
                    tempDir, LogsCollector.getDefaultFileName());

            newDest = LogsCollector.collectLogs(newDest);

            String uploadLocation =
                LoggingUtilsActivator.getResourceService()
                    .getSettingsString("plugin.loggingutils.uploadlocation");

            if(uploadLocation == null)
                return;

            URL url = new URL(uploadLocation);
            URLConnection urlConn = url.openConnection();

            if (!(urlConn instanceof HttpURLConnection))
                return;

            HttpURLConnection conn = (HttpURLConnection)urlConn;

            if(urlConn instanceof HttpsURLConnection)
            {
                CertificateVerificationService vs =
                    LoggingUtilsActivator.getCertificateVerificationService();

                int port = url.getPort();

                /* if we do not specify port in the URL
                 * (http://domain.org:port) we have to set up the default
                 * port of HTTP (80) or
                 * HTTPS (443).
                 */
                if(port == -1)
                {
                    if(url.getProtocol().equals("http"))
                    {
                        port = 80;
                    }
                    else if(url.getProtocol().equals("https"))
                    {
                        port = 443;
                    }
                }

                ((HttpsURLConnection)urlConn).setSSLSocketFactory(
                        vs.getSSLContext(
                        url.getHost(), port).getSocketFactory());
            }

            Random random = new Random();

            String boundary = "---------------------------" +
                Long.toString(random.nextLong(), 36) +
                Long.toString(random.nextLong(), 36) +
                Long.toString(random.nextLong(), 36);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type",
                              "multipart/form-data; boundary=" + boundary);

            OutputStream out = conn.getOutputStream();

            out.write("--".getBytes());
            out.write(boundary.getBytes());

            out.write("\r\n".getBytes());
            out.write("Content-Disposition: form-data; name=\"".getBytes());
            out.write("logs".getBytes());
            out.write('"');

            out.write("; filename=\"".getBytes());
            out.write(newDest.getPath().getBytes());
            out.write('"');

            out.write("\r\n".getBytes());
            out.write("Content-Type: ".getBytes());
            String type = conn.guessContentTypeFromName(newDest.getPath());
            if (type == null)
                type = "application/octet-stream";
            out.write(type.getBytes());
            out.write("\r\n".getBytes());
            out.write("\r\n".getBytes());

            byte[] buf = new byte[4096];
            int nread;
            FileInputStream in = new FileInputStream(newDest);
            while((nread = in.read(buf, 0, buf.length)) >= 0)
            {
                out.write(buf, 0, nread);
            }
            out.flush();
            buf = null;
            out.write("\r\n".getBytes());

            out.write("--".getBytes());
            out.write(boundary.getBytes());
            out.write("--".getBytes());
            out.write("\r\n".getBytes());
            out.close();
            InputStream serverInput = conn.getInputStream();

            // Get response data.
            BufferedReader input =
                new BufferedReader(new InputStreamReader(serverInput));

            if(logger.isDebugEnabled())
            {
                logger.debug("Log files uploaded result:");
                String str;
                while((str = input.readLine()) != null)
                {
                    logger.debug(str);
                }
            }
            input.close ();

            NotificationService notificationService
                = LoggingUtilsActivator.getNotificationService();

            if(notificationService != null)
            {
                String bodyMsgKey = "plugin.loggingutils.ARCHIVE_MESSAGE_OK";

                ResourceManagementService resources =
                    LoggingUtilsActivator.getResourceService();

                notificationService.fireNotification(
                    LOGFILES_ARCHIVED,
                    resources.getI18NString(
                            "plugin.loggingutils.ARCHIVE_BUTTON"),
                    resources.getI18NString(
                            bodyMsgKey,
                            new String[]{uploadLocation}),
                    null,
                    null);
            }
        }
        catch(Throwable e)
        {
            logger.error("Cannot upload file", e);
        }
    }
}
