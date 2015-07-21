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
package net.java.sip.communicator.plugin.loggingutils;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.httputil.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.packetlogging.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;

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
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Our Logger.
     */
    private static final Logger logger
            = Logger.getLogger(LoggingConfigForm.class);

    /**
     * Upload location property.
     */
    private static final String UPLOAD_LOCATION_PROPETY =
            "plugin.loggingutils.uploadlocation";

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
    private JButton archiveButton;

    /**
     * Archive logs button.
     */
    private JButton uploadLogsButton;

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
                    NotificationAction.ACTION_POPUP_MESSAGE,
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

        archiveButton = new JButton(
            resources.getI18NString("plugin.loggingutils.ARCHIVE_BUTTON"));
        archiveButton.addActionListener(this);

        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 4;
        mainPanel.add(archiveButton, c);

        if(!StringUtils.isNullOrEmpty(getUploadLocation()))
        {
            uploadLogsButton = new JButton(
                resources.getI18NString("plugin.loggingutils.UPLOAD_LOGS_BUTTON"));
            uploadLogsButton.addActionListener(this);

            c.insets = new Insets(10, 0, 0, 0);
            c.gridy = 5;
            mainPanel.add(uploadLogsButton, c);
        }
    }

    /**
     * Checks the property in configuration service and if missing there get it
     * from default settings.
     * @return the upload location.
     */
    static String getUploadLocation()
    {
        // check first in configuration it can be manually set
        // or by provisioning
        String uploadLocation =
            LoggingUtilsActivator.getConfigurationService()
                .getString(UPLOAD_LOCATION_PROPETY);
        // if missing check default settings
        if(uploadLocation == null || uploadLocation.length() == 0)
        {
            uploadLocation = LoggingUtilsActivator.getResourceService()
                .getSettingsString(UPLOAD_LOCATION_PROPETY);
        }

        return uploadLocation;
    }

    /**
     * Loading the values stored into configuration form
     */
    private void loadValues()
    {
        PacketLoggingService packetLogging
            = LoggingUtilsActivator.getPacketLoggingService();
        PacketLoggingConfiguration cfg = packetLogging.getConfiguration();

        enableCheckBox.setSelected(cfg.isGlobalLoggingEnabled());

        sipProtocolCheckBox.setSelected(cfg.isSipLoggingEnabled());
        jabberProtocolCheckBox.setSelected(cfg.isJabberLoggingEnabled());
        rtpProtocolCheckBox.setSelected(cfg.isRTPLoggingEnabled());
        ice4jProtocolCheckBox.setSelected(cfg.isIce4JLoggingEnabled());
        fileCountField.setText(String.valueOf(cfg.getLogfileCount()));
        fileSizeField.setText(String.valueOf(cfg.getLimit() / 1000));

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
            // don't block the UI thread
            new Thread(new Runnable()
            {
                public void run()
                {
                    collectLogs();
                }
            }).start();
        }
        else if(source.equals(uploadLogsButton))
        {
            // don't block the UI thread
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
                    "plugin.loggingutils.ARCHIVE_FILECHOOSE_TITLE"),
            SipCommFileChooser.SAVE_FILE_OPERATION);
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

        dest = LogsCollector.collectLogs(dest, null);

        NotificationService notificationService
            = LoggingUtilsActivator.getNotificationService();

        if(notificationService != null)
        {
            String bodyMsgKey
                = (dest == null)
                    ? "plugin.loggingutils.ARCHIVE_MESSAGE_NOTOK"
                    : "plugin.loggingutils.ARCHIVE_MESSAGE_OK";

            notificationService.fireNotification(
                    LOGFILES_ARCHIVED,
                    resources.getI18NString(
                            "plugin.loggingutils.ARCHIVE_BUTTON"),
                    resources.getI18NString(
                            bodyMsgKey,
                            new String[]{dest.getAbsolutePath()}),
                    null);
        }
    }

    /**
     * Shows a dialog with input for logs description.
     */
    private void uploadLogs()
    {
        ResourceManagementService resources =
            LoggingUtilsActivator.getResourceService();

        final SIPCommDialog dialog = new SIPCommDialog(false)
        {
            /**
             * Serial version UID.
             */
            private static final long serialVersionUID = 0L;

            /**
             * Dialog is closed. Do nothing.
             * @param escaped <tt>true</tt> if this dialog has been
             * closed by pressing
             */
            @Override
            protected void close(boolean escaped)
            {}
        };

        dialog.setModal(true);
        dialog.setTitle(resources.getI18NString(
            "plugin.loggingutils.UPLOAD_LOGS_BUTTON"));

        Container container = dialog.getContentPane();
        container.setLayout(new GridBagLayout());

        JLabel descriptionLabel = new JLabel("Add a comment:");
        final JTextArea commentTextArea = new JTextArea();
        commentTextArea.setRows(4);
        final JButton uploadButton = new JButton(
            resources.getI18NString("plugin.loggingutils.UPLOAD_BUTTON"));
        final SIPCommTextField emailField = new SIPCommTextField(resources
            .getI18NString("plugin.loggingutils.ARCHIVE_UPREPORT_EMAIL"));
        final JCheckBox emailCheckBox = new SIPCommCheckBox(
            "Email me when more information is available");
        emailCheckBox.setSelected(true);
        emailCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if(!emailCheckBox.isSelected())
                {
                    uploadButton.setEnabled(true);
                    emailField.setEnabled(false);
                }
                else
                {
                    emailField.setEnabled(true);

                    if(emailField.getText() != null
                        && emailField.getText().trim().length() > 0)
                        uploadButton.setEnabled(true);
                    else
                        uploadButton.setEnabled(false);
                }
            }
        });

        emailField.getDocument().addDocumentListener(new DocumentListener()
        {
            public void insertUpdate(DocumentEvent e)
            {
                updateButtonsState();
            }

            public void removeUpdate(DocumentEvent e)
            {
                updateButtonsState();
            }

            public void changedUpdate(DocumentEvent e){}

            /**
             * Check whether we should enable upload button.
             */
            private void updateButtonsState()
            {
                if(emailCheckBox.isSelected() && emailField.getText() != null
                    && emailField.getText().trim().length() > 0)
                    uploadButton.setEnabled(true);
                else
                    uploadButton.setEnabled(false);
            }
        });

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 10, 3, 10);
        c.weightx = 1.0;
        c.gridx = 0;
        c.gridy = 0;

        container.add(descriptionLabel, c);

        c.insets = new Insets(0, 10, 10, 10);
        c.gridy = 1;
        container.add(new JScrollPane(commentTextArea), c);

        c.insets = new Insets(0, 10, 0, 10);
        c.gridy = 2;
        container.add(emailCheckBox, c);

        c.insets = new Insets(0, 10, 10, 10);
        c.gridy = 3;
        container.add(emailField, c);

        JButton cancelButton = new JButton(
            resources.getI18NString("service.gui.CANCEL"));
        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dialog.dispose();
            }
        });

        uploadButton.setEnabled(false);
        uploadButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    final ArrayList<String> paramNames = new ArrayList<String>();
                    final ArrayList<String> paramValues = new ArrayList<String>();

                    if(emailCheckBox.isSelected())
                    {
                        paramNames.add("Email");
                        paramValues.add(emailField.getText());
                    }

                    paramNames.add("Description");
                    paramValues.add(commentTextArea.getText());

                    // don't block the UI thread we may need to show
                    // some ui for password input if protected area on the way
                    new Thread(new Runnable()
                    {
                        public void run()
                        {
                            uploadLogs(
                                getUploadLocation(),
                                LogsCollector.getDefaultFileName(),
                                paramNames.toArray(new String[]{}),
                                paramValues.toArray(new String[]{}));
                        }
                    }).start();
                }
                finally
                {
                    dialog.dispose();
                }
            }
        });
        JPanel buttonsPanel = new TransparentPanel(
            new FlowLayout(FlowLayout.RIGHT));
        buttonsPanel.add(uploadButton);
        buttonsPanel.add(cancelButton);

        c.anchor = GridBagConstraints.LINE_END;
        c.weightx = 0;
        c.gridy = 4;
        container.add(buttonsPanel, c);

        dialog.setVisible(true);
    }

    /**
     * Upload files to pre-configured url.
     * @param uploadLocation the location we are uploading to.
     * @param fileName the filename we use for the resulting filename we upload.
     * @param params the optional parameter names.
     * @param values the optional parameter values.
     */
    static void uploadLogs(
        String uploadLocation,
        String fileName,
        String[] params,
        String[] values)
    {
        try
        {
            File tempDir = LoggingUtilsActivator.getFileAccessService()
                .getTemporaryDirectory();
            File newDest = new File(tempDir, fileName);

            File optionalFile = null;

            // if we have some description params
            // save them to file and add it to archive
            if(params != null)
            {
                optionalFile = new File(
                    LoggingUtilsActivator.getFileAccessService().
                        getTemporaryDirectory(),
                    "description.txt");
                OutputStream out = new FileOutputStream(optionalFile);
                for(int i = 0; i < params.length; i++)
                {
                    out.write((params[i] + " : "
                        + values[i] + "\r\n").getBytes("UTF-8"));
                }
                out.flush();
                out.close();
            }

            newDest = LogsCollector.collectLogs(newDest, optionalFile);

            // don't leave any unneeded information
            if(optionalFile != null)
                optionalFile.delete();

            if(uploadLocation == null)
                return;

            if(HttpUtils.postFile(uploadLocation, "logs", newDest) != null)
            {
                NotificationService notificationService
                    = LoggingUtilsActivator.getNotificationService();

                if(notificationService != null)
                {
                    ResourceManagementService resources
                        = LoggingUtilsActivator.getResourceService();
                    String bodyMsgKey = "plugin.loggingutils.ARCHIVE_MESSAGE_OK";

                    notificationService.fireNotification(
                            LOGFILES_ARCHIVED,
                            resources.getI18NString(
                                    "plugin.loggingutils.ARCHIVE_BUTTON"),
                            resources.getI18NString(
                                    bodyMsgKey,
                                    new String[]{uploadLocation}),
                            null);
                }
            }
        }
        catch(Throwable e)
        {
            logger.error("Cannot upload file", e);
        }
    }
}
