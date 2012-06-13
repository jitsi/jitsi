/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.notificationconfiguration;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.audionotifier.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

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
    private final JButton soundFileChooser
        = new JButton(new ImageIcon(Resources.getImageInBytes(
            "plugin.notificationconfig.FOLDER_ICON")));
    private final JTextField programFileTextField = new JTextField();
    private final JButton programFileChooser
        = new JButton(new ImageIcon(Resources.getImageInBytes(
            "plugin.notificationconfig.FOLDER_ICON")));
    private final JButton playSoundButton
        = new JButton(new ImageIcon(Resources.getImageInBytes(
            "plugin.notificationconfig.PLAY_ICON")));
    private final JButton restoreButton
        = new JButton(Resources.getString("plugin.notificationconfig.RESTORE"));

    private SipCommFileChooser fileChooserProgram;
    private SipCommFileChooser fileChooserSound;

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

        initNotificationsList();

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

        soundFileChooser.setMinimumSize(new Dimension(30,30));
        soundFileChooser.setPreferredSize(new Dimension(30,30));
        soundFileChooser.addActionListener(this);
        soundFilePanel.add(soundFileChooser);

        JPanel programFilePanel
            = new TransparentPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel emptyLabel = new JLabel();
        emptyLabel.setPreferredSize(new Dimension(30, 30));
        programFilePanel.add(emptyLabel);

        programFileTextField.setPreferredSize(new Dimension(200, 30));
        programFileTextField.getDocument().addDocumentListener(this);

        programFilePanel.add(programFileTextField);

        programFileChooser.setMinimumSize(new Dimension(30,30));
        programFileChooser.setPreferredSize(new Dimension(30,30));
        programFileChooser.addActionListener(this);

        programFilePanel.add(programFileChooser);

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

        fileChooserSound =
            GenericFileDialog.create(null,
                Resources.getString("plugin.notificationconfig.BROWSE_SOUND"),
                SipCommFileChooser.LOAD_FILE_OPERATION);
        fileChooserProgram =
            GenericFileDialog.create(null,
                Resources.getString("plugin.notificationconfig.BROWSE_PROGRAM"),
                SipCommFileChooser.LOAD_FILE_OPERATION);
        String[] soundFormats = {SoundFileUtils.wav};
        fileChooserSound.setFileFilter(new SoundFilter(soundFormats));
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
                            "plugin.notificationconfig.SOUND_ICON")));
        Object column[] =
            {   "",
                icon1, icon2, icon3,
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

        programFileChooser.setEnabled(entry.getProgram());
        programFileTextField.setEnabled(entry.getProgram());

        String programFile = entry.getProgramFile();
        programFileTextField.setText(
            (programFile != null && programFile.length() > 0) ? programFile : "");

        soundFileChooser.setEnabled(entry.getSound());
        soundFileTextField.setEnabled(entry.getSound());

        String soundFile = entry.getSoundFile();
        soundFileTextField.setText(
            (soundFile != null && soundFile.length() > 0) ? soundFile : "");

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
        else if(e.getSource() == soundFileChooser)
        {
            if (row < 0)
                return;

            NotificationEntry entry
                = notificationList.getNotificationEntry(row);

            File file = fileChooserSound.getFileFromDialog();

            if (file != null)
            {
                try
                {
                    //This is where a real application would open the file.
                    if (logger.isDebugEnabled())
                        logger.debug("Opening: "
                            + file.toURI().toURL().toExternalForm());

                    entry.setSoundFile(file.toURI().toURL().toExternalForm());
                    soundFileTextField.setText(
                        file.toURI().toURL().toExternalForm());
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
        else if(e.getSource() == programFileChooser)
        {
            if (row < 0)
                return;

            NotificationEntry entry
                = notificationList.getNotificationEntry(row);

            File file = fileChooserProgram.getFileFromDialog();

            if (file != null)
            {
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

            NotificationConfigurationActivator.getNotificationService()
                    .registerNotificationForEvent(
                            entry.getEvent(),
                            NotificationAction.ACTION_SOUND,
                            entry.getSoundFile(),
                            ""
                    );
        }
    }

    /**
     * Indicates that text is removed in one of the text fields.
     * @param event the <tt>DocumentEvent</tt> that notified us
     */
    public void removeUpdate(DocumentEvent event)
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

            NotificationConfigurationActivator.getNotificationService()
                    .registerNotificationForEvent(
                            entry.getEvent(),
                            NotificationAction.ACTION_SOUND,
                            entry.getSoundFile(),
                            ""
                    );
        }
    }

    public void changedUpdate(DocumentEvent de) {}
}
