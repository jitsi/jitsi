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

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.packetlogging.*;
import org.jitsi.service.resources.*;

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
            new Thread(this::collectLogs).start();
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
                int newFileCount = Integer.parseInt(fileCountField.getText());
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
                int newFileSize = Integer.parseInt(fileSizeField.getText());
                fileSizeField.setForeground(Color.black);
                LoggingUtilsActivator.getPacketLoggingService()
                    .getConfiguration().setLimit(newFileSize * 1000L);
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
}
