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
package net.java.sip.communicator.plugin.notificationconfiguration;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.audionotifier.*;

import org.jitsi.util.*;

/**
 * The UI of <tt>ConfigurationForm</tt> that would be added in the user
 * interface configuration window. It contains a list of all installed
 * notifications.
 *
 * @author Alexandre Maillard
 * @author Yana Stamcheva
 */
public class NotificationConfigurationPanel
    extends TransparentPanel
    implements ActionListener,
               DocumentListener
{
    private static final long serialVersionUID = 5784331951722787598L;

    private final Logger logger
            = Logger.getLogger(NotificationConfigurationPanel.class);

    private NotificationsTable notificationList;

    private final JTextField soundFileTextField = new JTextField();

    private final JButton soundFileButton
        = new JButton(new ImageIcon(Resources.getImageInBytes(
            "plugin.notificationconfig.FOLDER_ICON")));

    private final JTextField programFileTextField = new JTextField();

    private final JButton programFileButton
        = new JButton(new ImageIcon(Resources.getImageInBytes(
            "plugin.notificationconfig.FOLDER_ICON")));

    private final JButton playSoundButton
        = new JButton(new ImageIcon(Resources.getImageInBytes(
            "plugin.notificationconfig.PLAY_ICON")));

    private final JButton restoreButton
        = new JButton(Resources.getString("plugin.notificationconfig.RESTORE"));

    /**
     * The program file chooser component.
     */
    private SipCommFileChooser programFileChooser;

    /**
     * The sound file chooser component.
     */
    private SipCommFileChooser soundFileChooser;

    /**
     * The property for the last stored path from program file chooser.
     */
    private static final String PROGRAM_LAST_PATH_PROP
        = "net.java.sip.communicator.plugin.notificationconfiguration."
            + "PROGRAM_LAST_PATH";

    /**
     * The property for the last stored path from sound file chooser.
     */
    private static final String SOUND_LAST_PATH_PROP
        = "net.java.sip.communicator.plugin.notificationconfiguration."
            + "SOUND_LAST_PATH";

    /**
     * The last stored path from program file chooser.
     */
    private String programLastFilePath;

    /**
     * The last stored path from sound file chooser.
     */
    private String soundLastFilePath;

    /**
     * Used to suppress saving entry values while filling
     * programFileTextField and soundFileTextField.
     */
    private boolean isCurrentlyChangeEntryInTable = false;

    /**
     * Creates an instance of <tt>NotificationConfigurationPanel</tt>.
     */
    public NotificationConfigurationPanel()
    {
        super(new BorderLayout());

        JPanel labelsPanel = new TransparentPanel(new GridLayout(2, 1));

        JLabel soundFileLabel = new JLabel(
                Resources.getString("plugin.notificationconfig.SOUND_FILE"));
        JLabel programFileLabel = new JLabel(
                Resources.getString("plugin.notificationconfig.PROGRAM_FILE"));

        labelsPanel.add(soundFileLabel);
        labelsPanel.add(programFileLabel);

        JPanel soundFilePanel
            = new TransparentPanel(new FlowLayout(FlowLayout.LEFT));

        playSoundButton.setMinimumSize(new Dimension(30,30));
        playSoundButton.setPreferredSize(new Dimension(30,30));
        playSoundButton.setOpaque(false);
        playSoundButton.addActionListener(this);
        soundFilePanel.add(playSoundButton);

        soundFileTextField.setPreferredSize(new Dimension(200, 30));
        soundFileTextField.getDocument().addDocumentListener(this);

        soundFilePanel.add(soundFileTextField);

        soundFileButton.setMinimumSize(new Dimension(30,30));
        soundFileButton.setPreferredSize(new Dimension(30,30));
        soundFileButton.addActionListener(this);
        soundFilePanel.add(soundFileButton);

        JPanel programFilePanel
            = new TransparentPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel emptyLabel = new JLabel();
        emptyLabel.setPreferredSize(new Dimension(30, 30));
        programFilePanel.add(emptyLabel);

        programFileTextField.setPreferredSize(new Dimension(200, 30));
        programFileTextField.getDocument().addDocumentListener(this);

        programFilePanel.add(programFileTextField);

        programFileButton.setMinimumSize(new Dimension(30,30));
        programFileButton.setPreferredSize(new Dimension(30,30));
        programFileButton.addActionListener(this);

        programFilePanel.add(programFileButton);

        JPanel valuesPanel = new TransparentPanel(new GridLayout(2, 1));
        valuesPanel.add(soundFilePanel);
        valuesPanel.add(programFilePanel);

        JPanel southPanel = new TransparentPanel(new BorderLayout());
        southPanel.add(labelsPanel, BorderLayout.WEST);
        southPanel.add(valuesPanel, BorderLayout.CENTER);

        restoreButton.addActionListener(this);
        JPanel restorePanel
            = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        restorePanel.add(restoreButton);

        southPanel.add(restorePanel, BorderLayout.SOUTH);

        add(southPanel, BorderLayout.SOUTH);

        soundFileChooser =
            GenericFileDialog.create(null,
                Resources.getString("plugin.notificationconfig.BROWSE_SOUND"),
                SipCommFileChooser.LOAD_FILE_OPERATION);
        programFileChooser =
            GenericFileDialog.create(null,
                Resources.getString("plugin.notificationconfig.BROWSE_PROGRAM"),
                SipCommFileChooser.LOAD_FILE_OPERATION);
        String[] soundFormats = {SoundFileUtils.wav};
        soundFileChooser.setFileFilter(new SoundFilter(soundFormats));

        initNotificationsList();
    }

    /**
     * Initializes the notifications list component.
     */
    private void initNotificationsList()
    {
        String[] columnToolTips = {
            "plugin.notificationconfig.tableheader.ENABLE",
            "plugin.notificationconfig.tableheader.EXECUTE",
            "plugin.notificationconfig.tableheader.POPUP",
            "plugin.notificationconfig.tableheader.SOUND",
            "plugin.notificationconfig.tableheader.PLAYBACK_SOUND",
            "plugin.notificationconfig.tableheader.PCSPEAKER_SOUND",
            "plugin.notificationconfig.tableheader.DESCRIPTION"
        };

        JLabel icon1
            = new JLabel(new ImageIcon(Resources.getImageInBytes(
                            "plugin.notificationconfig.PROG_ICON")));
        JLabel icon2
            = new JLabel(new ImageIcon(Resources.getImageInBytes(
                            "plugin.notificationconfig.POPUP_ICON")));
        JLabel icon3
            = new JLabel(new ImageIcon(Resources.getImageInBytes(
                            "plugin.notificationconfig.SOUND_ICON_NOTIFY")));
        JLabel icon4
            = new JLabel(new ImageIcon(Resources.getImageInBytes(
                            "plugin.notificationconfig.SOUND_ICON_PLAYBACK")));
        JLabel icon5
            = new JLabel(new ImageIcon(Resources.getImageInBytes(
                            "plugin.notificationconfig.SOUND_ICON")));
        Object column[] =
            {   "",
                icon1, icon2, icon3, icon4, icon5,
                Resources.getString("plugin.notificationconfig.DESCRIPTION") };

        notificationList = new NotificationsTable(column, columnToolTips, this);

        notificationList.setPreferredSize(new Dimension(500, 300));
        this.add(notificationList, BorderLayout.CENTER);

        if (notificationList.getRowCount() > 0)
            notificationList.setSelectedRow(0);
    }

    /**
     * Sets <tt>entry</tt> configurations.
     * @param entry the entry to set
     */
    public void setNotificationEntry(NotificationEntry entry)
    {
        isCurrentlyChangeEntryInTable = true;

        programFileButton.setEnabled(entry.getProgram());
        programFileTextField.setEnabled(entry.getProgram());

        String programFile = entry.getProgramFile();
        programFileTextField.setText(
            (programFile != null && programFile.length() > 0)
                ? programFile
                : "");
        programFileChooser.setStartPath(
            (programFile != null && programFile.length() > 0)
                ? programFile
                : getLastProgramPath());

        soundFileButton.setEnabled(entry.getSoundNotification()
            || entry.getSoundPlayback());
        soundFileTextField.setEnabled(entry.getSoundNotification()
            || entry.getSoundPlayback());

        String soundFile = entry.getSoundFile();

        soundFileTextField.setText(
            (soundFile != null && soundFile.length() > 0)
                ? soundFile
                : "");

        soundFileChooser.setStartPath(
            (soundFile != null && soundFile.length() > 0)
            ? soundFile
            : getLastSoundPath());

        isCurrentlyChangeEntryInTable = false;
    }

    /**
     * Indicates that one of the contained in this panel buttons has been
     * clicked.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        int row = notificationList.getSelectedRow();

        if(e.getSource() == restoreButton)
        {
            notificationList.clear();
            NotificationConfigurationActivator.getNotificationService()
                .restoreDefaults();
        }
        else if(e.getSource() == soundFileButton)
        {
            if (row < 0)
                return;

            NotificationEntry entry
                = notificationList.getNotificationEntry(row);

            File file = soundFileChooser.getFileFromDialog();

            if (file != null)
            {
                try
                {
                    // Store the last program file path.
                    setLastSoundPath(file.getParent());

                    String fileUri = file.toURI().toURL().toExternalForm();
                    //This is where a real application would open the file.
                    if (logger.isDebugEnabled())
                        logger.debug("Opening: " + fileUri);

                    entry.setSoundFile(fileUri);
                    soundFileTextField.setText(fileUri);
                }
                catch (MalformedURLException ex)
                {
                    logger.error("Error file path parsing", ex);
                }
            }
            else
            {
                if (logger.isDebugEnabled())
                    logger.debug("Open command cancelled by user.");
            }
        }
        else if(e.getSource() == programFileButton)
        {
            if (row < 0)
                return;

            NotificationEntry entry
                = notificationList.getNotificationEntry(row);

            File file = programFileChooser.getFileFromDialog();

            if (file != null)
            {
                // Store the last program file path.
                setLastProgramPath(file.getParent());

                //This is where a real application would open the file.
                if (logger.isDebugEnabled())
                    logger.debug("Opening: " +file.getAbsolutePath());

                entry.setProgramFile(file.getAbsolutePath());
                programFileTextField.setText(file.getAbsolutePath());
            }
            else
            {
                if (logger.isDebugEnabled())
                    logger.debug("Open command cancelled by user.");
            }
        }
        else if(e.getSource() == playSoundButton)
        {
            String soundFile = soundFileTextField.getText();

            if (logger.isDebugEnabled())
                logger.debug("****"+soundFile+"****"+soundFile.length());

            if(soundFile.length() != 0)
            {
                AudioNotifierService audioNotifServ
                        = NotificationConfigurationActivator
                        .getAudioNotifierService();
                SCAudioClip sound = audioNotifServ.createAudio(soundFile);
                sound.play();
                //audioNotifServ.destroyAudio(sound);
            }
            else
            {
                if (logger.isDebugEnabled())
                    logger.debug("No file specified");
            }
        }
    }

    /**
     * Indicates that text is inserted in one of the text fields.
     * @param event the <tt>DocumentEvent</tt> that notified us
     */
    public void insertUpdate(DocumentEvent event)
    {
        textFieldUpdated(event);
    }

    /**
     * Indicates that text is removed in one of the text fields.
     * @param event the <tt>DocumentEvent</tt> that notified us
     */
    public void removeUpdate(DocumentEvent event)
    {
        textFieldUpdated(event);
    }

    public void changedUpdate(DocumentEvent de) {}

    /**
     * Indicates that text is inserted in one of the text fields.
     * @param event the <tt>DocumentEvent</tt> that notified us
     */
    public void textFieldUpdated(DocumentEvent event)
    {
        // we are just changing display values, no real change in data
        // to save it
        if(isCurrentlyChangeEntryInTable)
            return;

        NotificationEntry entry = notificationList.getNotificationEntry(
            notificationList.getSelectedRow());

        if(event.getDocument().equals(programFileTextField.getDocument()))
        {
            entry.setProgramFile(programFileTextField.getText());

            NotificationConfigurationActivator.getNotificationService()
                    .registerNotificationForEvent(
                            entry.getEvent(),
                            NotificationAction.ACTION_COMMAND,
                            entry.getProgramFile(),
                            ""
                    );
        }
        if(event.getDocument().equals(soundFileTextField.getDocument()))
        {
            entry.setSoundFile(soundFileTextField.getText());

            NotificationService notificationService =
                NotificationConfigurationActivator.getNotificationService();
            SoundNotificationAction origSoundAction
                = (SoundNotificationAction)
                notificationService.getEventNotificationAction(
                        entry.getEvent(), NotificationAction.ACTION_SOUND);

            NotificationConfigurationActivator.getNotificationService()
                .registerNotificationForEvent(
                    entry.getEvent(),
                    new SoundNotificationAction(
                            entry.getSoundFile(),
                            origSoundAction.getLoopInterval(),
                            origSoundAction.isSoundNotificationEnabled(),
                            origSoundAction.isSoundPlaybackEnabled(),
                            origSoundAction.isSoundPCSpeakerEnabled()));
        }
    }

    /**
     * Returns the last opened sound path.
     *
     * @return the last opened sound path
     */
    private String getLastSoundPath()
    {
        return NotificationConfigurationActivator.getConfigurationService()
            .getString(SOUND_LAST_PATH_PROP, "");
    }

    /**
     * Sets the last opened sound path.
     *
     * @param the last opened sound path
     */
    private void setLastSoundPath(String path)
    {
        NotificationConfigurationActivator.getConfigurationService()
            .setProperty(SOUND_LAST_PATH_PROP, path);
    }

    /**
     * Returns the last opened program path.
     *
     * @return the last opened program path
     */
    private String getLastProgramPath()
    {
        return NotificationConfigurationActivator.getConfigurationService()
            .getString(PROGRAM_LAST_PATH_PROP, "");
    }

    /**
     * Sets the last opened sound path.
     *
     * @param path the last opened sound path
     */
    private void setLastProgramPath(String path)
    {
        NotificationConfigurationActivator.getConfigurationService()
            .setProperty(PROGRAM_LAST_PATH_PROP, path);
    }
}
