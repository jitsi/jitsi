/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.notificationconfiguration;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.audionotifier.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.notification.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The UI of <tt>ConfigurationForm</tt> that would be added in the user
 * interface configuration window. It contains a list of all installed
 * notifications.
 *
 * @author Alexandre Maillard
 */
public class NotificationConfigurationPanel
    extends TransparentPanel
    implements ActionListener,
               ItemListener,
               DocumentListener,
               NotificationChangeListener
{
    private static final long serialVersionUID = 5784331951722787598L;

    private final Logger logger
            = Logger.getLogger(NotificationConfigurationPanel.class);

    // Declaration of variables on the table notifications
    private Vector<NotificationsTableEntry> dataVector = null;

    private ListMulti notificationList;

    public static final String[] columnToolTips
            = {"Enable or disable this feature",
            "Execute a program",
            "Display a messagebox",
            "Play a sound",
            "Description of event" };

    private JButton activate;
    private JButton deactivate;
    private JCheckBox playSoundCheckBox;
    private JButton playSoundButton;
    private JTextField soundFileTextField;
    private JButton soundFileChooser;
    private JCheckBox programCheckBox;
    private JTextField programFileTextField;
    private JButton programFileChooser;
    private JCheckBox popupCheckBox;

    private JButton turnOnAll;
    private JButton turnOffAll;
    private JComboBox comboBoxTurnOn;
    private JComboBox comboBoxTurnOff;

    private JButton apply;
    private JButton restore;

    private SipCommFileChooser fileChooserProgram;
    private SipCommFileChooser fileChooserSound;

    private int index = -1;
    private boolean turnAll = false;

    private NotificationService notificationService = null;

    private boolean noListener = false;

    public NotificationConfigurationPanel()
    {
        JPanel actions = new TransparentPanel();
        GridBagLayout actionsLayout = new GridBagLayout();
        JPanel applyPanel = new TransparentPanel();
        GridBagLayout gridLayoutGlobal = new GridBagLayout();
        JPanel quickControl = new TransparentPanel();
        String[] textComboBox =
            { "Sounds", "Program Execution", "Messages Popup" };

        // constraints on the table
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;

        JLabel icon1 =
            new JLabel(new ImageIcon(Resources.getImageInBytes(
                            "plugin.notificationconfig.PROG_ICON")));
        JLabel icon2 =
            new JLabel(new ImageIcon(Resources.getImageInBytes(
                            "plugin.notificationconfig.POPUP_ICON")));
        JLabel icon3 =
            new JLabel(new ImageIcon(Resources.getImageInBytes(
                            "plugin.notificationconfig.SOUND_ICON")));
        Object column[] =
            { "Status", icon1, icon2, icon3, "Event" };
        notificationList = new ListMulti(column, columnToolTips);

        dataVector = new Vector<NotificationsTableEntry>();

        gridLayoutGlobal.setConstraints(notificationList, constraints);
        this.add(notificationList);

        // Initializing variable part of the "actions"
        TitledBorder title1 = BorderFactory.createTitledBorder(
                Resources.getString("plugin.notificationconfig.ACTIONS"));
        actions.setBorder(title1);

        JPanel activateDescactivatePanel = new TransparentPanel();
        FlowLayout layoutADP = new FlowLayout(FlowLayout.CENTER);
        layoutADP.setHgap(75);
        activateDescactivatePanel.setLayout(layoutADP);

        activate = new JButton(Resources.getString("service.gui.ACTIVATE"));
        activate.setMinimumSize(new Dimension(150,30));
        activate.setPreferredSize(new Dimension(150,30));
        activate.setOpaque(false);
        activate.addActionListener(this);
        deactivate = new JButton(
            Resources.getString("service.gui.DEACTIVATE"));
        deactivate.setMinimumSize(new Dimension(150,30));
        deactivate.setPreferredSize(new Dimension(150,30));
        deactivate.setOpaque(false);
        deactivate.addActionListener(this);

        playSoundCheckBox = new SIPCommCheckBox(
            Resources.getString("plugin.notificationconfig.PLAY_SOUND"));

        playSoundCheckBox.addItemListener(this);
        playSoundButton = new JButton(
                new ImageIcon(Resources.getImageInBytes(
                    "plugin.notificationconfig.PLAY_ICON")));
        playSoundButton.setMinimumSize(new Dimension(50,30));
        playSoundButton.setPreferredSize(new Dimension(50,30));
        playSoundButton.setOpaque(false);
        playSoundButton.addActionListener(this);
        soundFileTextField = new JTextField();
        soundFileTextField.setMinimumSize(new Dimension(250,30));
        soundFileTextField.setPreferredSize(new Dimension(250,30));
        soundFileTextField.getDocument().addDocumentListener(this);
        soundFileChooser = new JButton(
                new ImageIcon(Resources.getImageInBytes(
                    "plugin.notificationconfig.FOLDER_ICON")));
        soundFileChooser.setMinimumSize(new Dimension(30,30));
        soundFileChooser.setPreferredSize(new Dimension(30,30));
        soundFileChooser.addActionListener(this);

        programCheckBox = new SIPCommCheckBox(
            Resources.getString("plugin.notificationconfig.EXEC_PROG"));

        programCheckBox.addItemListener(this);
        programFileTextField = new JTextField();
        programFileTextField.setMinimumSize(new Dimension(250,30));
        programFileTextField.setPreferredSize(new Dimension(250,30));
        programFileTextField.getDocument().addDocumentListener(this);
        programFileChooser = new JButton(
                new ImageIcon(Resources.getImageInBytes(
                    "plugin.notificationconfig.FOLDER_ICON")));
        programFileChooser.setMinimumSize(new Dimension(30,30));
        programFileChooser.setPreferredSize(new Dimension(30,30));
        programFileChooser.addActionListener(this);

        popupCheckBox = new SIPCommCheckBox(
            Resources.getString("plugin.notificationconfig.DISPLAY_POPUP"));

        popupCheckBox.addItemListener(this);


        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 4;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0,0,10,0);

        activateDescactivatePanel.add(activate);
        activateDescactivatePanel.add(deactivate);
        actionsLayout.setConstraints(activateDescactivatePanel, constraints);
        actions.add(activateDescactivatePanel);


        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.insets = new Insets(0,0,2,0);
        actionsLayout.setConstraints(playSoundCheckBox, constraints);
        actions.add(playSoundCheckBox);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.insets = new Insets(0,0,2,5);
        constraints.anchor = GridBagConstraints.EAST;
        actionsLayout.setConstraints(playSoundButton, constraints);
        actions.add(playSoundButton);

        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.insets = new Insets(0,0,2,0);
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        actionsLayout.setConstraints(soundFileTextField, constraints);
        actions.add(soundFileTextField);

        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0,5,2,0);
        actionsLayout.setConstraints(soundFileChooser, constraints);
        actions.add(soundFileChooser);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0,0,2,0);
        actionsLayout.setConstraints(programCheckBox, constraints);
        actions.add(programCheckBox);

        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0,0,2,0);
        actionsLayout.setConstraints(programFileTextField, constraints);
        actions.add(programFileTextField);

        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.insets = new Insets(0,5,2,0);
        constraints.anchor = GridBagConstraints.WEST;
        actionsLayout.setConstraints(programFileChooser, constraints);
        actions.add(programFileChooser);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 4;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        actionsLayout.setConstraints(popupCheckBox, constraints);
        actions.add(popupCheckBox);

        actions.setLayout(actionsLayout);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        //constraints.weighty = 40.0;
        constraints.fill = GridBagConstraints.BOTH;
        gridLayoutGlobal.setConstraints(actions, constraints);
        this.add(actions);



        // Initializing variables of the "quickControl"
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        //constraints.weighty = 10.0;
        constraints.fill = GridBagConstraints.BOTH;

        comboBoxTurnOn = new JComboBox(textComboBox);
        comboBoxTurnOn.addActionListener(this);
        comboBoxTurnOff = new JComboBox(textComboBox);
        comboBoxTurnOff.addActionListener(this);
        turnOnAll = new JButton(
            Resources.getString("plugin.notificationconfig.TURN_ON_ALL"));
        turnOnAll.addActionListener(this);
        turnOffAll = new JButton(
            Resources.getString("plugin.notificationconfig.TURN_OFF_ALL"));
        turnOffAll.addActionListener(this);


        TitledBorder title2 = BorderFactory.createTitledBorder(
                Resources.getString("plugin.notificationconfig.QUICK_CONTROLS"));
        quickControl.setLayout(new FlowLayout(FlowLayout.CENTER,2,2));
        quickControl.setBorder(title2);
        quickControl.add(turnOnAll);
        quickControl.add(comboBoxTurnOn);
        quickControl.add(turnOffAll);
        quickControl.add(comboBoxTurnOff);
        gridLayoutGlobal.setConstraints(quickControl, constraints);
//        this.add(quickControl);

        this.setLayout(gridLayoutGlobal);


        fileChooserSound = GenericFileDialog.create(null, "Choose a sound...",
            SipCommFileChooser.LOAD_FILE_OPERATION);
        fileChooserProgram = GenericFileDialog.create(null, 
            "Choose a program...", SipCommFileChooser.LOAD_FILE_OPERATION);
        //fileChooserSound.setMultiSelectionEnabled(false);
        //fileChooserProgram.setMultiSelectionEnabled(false);
        fileChooserSound.addFilter(new SoundFilter());

        notificationList.addMouseListener(new MyMouseAdapter());


        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.BOTH;

        restore = new JButton(
            Resources.getString("plugin.notificationconfig.RESTORE"));
        restore.addActionListener(this);
        apply = new JButton(
            Resources.getString("service.gui.APPLY"));
        apply.addActionListener(this);
        applyPanel.setLayout(new BorderLayout(5,2));
        applyPanel.add(apply, BorderLayout.EAST);
        applyPanel.add(restore, BorderLayout.WEST);
        gridLayoutGlobal.setConstraints(applyPanel, constraints);
        this.add(applyPanel);

        notificationService
                = NotificationConfigurationActivator.getNotificationService();
        notificationService.addNotificationChangeListener(this);
        this.buildingVector();
        this.updateTable();

        if(dataVector.size() > 0)
        {
            NotificationsTableEntry tmpNTE = dataVector.elementAt(0);

            updatePanel(tmpNTE);
            notificationList.setRowSelectionInterval(0,0);
            index = 0;
        }
    }

    private void updatePanel(NotificationsTableEntry tmpNTE)
    {
        noListener = true;
        activate.setEnabled(!tmpNTE.getEnabled());
        deactivate.setEnabled(tmpNTE.getEnabled());
        programCheckBox.setSelected(tmpNTE.getProgram());
        programFileChooser.setEnabled(tmpNTE.getProgram());
        programFileTextField.setEnabled(tmpNTE.getProgram());
        programFileTextField.setText(tmpNTE.getProgramFile());
        playSoundCheckBox.setSelected(tmpNTE.getSound());
        playSoundButton.setEnabled(tmpNTE.getSound());
        soundFileChooser.setEnabled(tmpNTE.getSound());
        soundFileTextField.setEnabled(tmpNTE.getSound());
        soundFileTextField.setText(tmpNTE.getSoundFile());
        popupCheckBox.setSelected(tmpNTE.getPopup());
        noListener = false;
    }

    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == activate)
        {
            NotificationsTableEntry tmpNTE = dataVector.elementAt(index);
            tmpNTE.setEnabled(true);
            this.updateTableRow(tmpNTE, index);
            activate.setEnabled(false);
            deactivate.setEnabled(true);
            tmpNTE.setModify(true);
        }
        else if(e.getSource() == deactivate)
        {
            NotificationsTableEntry tmpNTE = dataVector.elementAt(index);
            tmpNTE.setEnabled(false);
            this.updateTableRow(tmpNTE, index);
            activate.setEnabled(true);
            deactivate.setEnabled(false);
            tmpNTE.setModify(true);
        }
        else if(e.getSource() == soundFileChooser)
        {
            File file = fileChooserSound.getFileFromDialog();

            noListener = true;
            if (file != null)
            {
                try
                {
                    NotificationsTableEntry tmpNTE = dataVector.elementAt(index);
                    
                    //This is where a real application would open the file.
                    logger.debug("Opening: "
                            + file.toURI().toURL().toExternalForm());
                    tmpNTE.setSoundFile(file.toURI().toURL().toExternalForm());
                    tmpNTE.setSound(true);
                    tmpNTE.setModify(true);
                    this.updateTableRow(tmpNTE, index);
                    notificationList.setLine(tmpNTE, index);
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
                logger.debug("Open command cancelled by user.");
            }
            noListener = false;
        }
        else if(e.getSource() == programFileChooser)
        {
            File file = fileChooserProgram.getFileFromDialog();
            noListener = true;

            if (file != null)
            {
                NotificationsTableEntry tmpNTE = dataVector.elementAt(index);
                
                //This is where a real application would open the file.
                logger.debug("Opening: " +file.getAbsolutePath());
                tmpNTE.setProgramFile(file.getAbsolutePath());
                tmpNTE.setProgram(true);
                tmpNTE.setModify(true);
                this.updateTableRow(tmpNTE,index);
                notificationList.setLine(tmpNTE,index);
                programFileTextField.setText(file.getAbsolutePath());
            }
            else
            {
                logger.debug("Open command cancelled by user.");
            }
            noListener = false;
        }
        else if(e.getSource() == playSoundButton)
        {
            if(playSoundCheckBox.isSelected() == true)
            {
                String soundFile = soundFileTextField.getText();

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
                    logger.debug("No file specified");
                }
            }
            else
            {
                logger.debug("Its non-active");
            }
        }
        else if(e.getSource() == turnOnAll)
        {
            Iterator<NotificationsTableEntry> it = dataVector.iterator();
            NotificationsTableEntry tmpNTE = null;
            int cpt = 0;

            if(!it.hasNext())
                return;

            turnAll = true;
            while(it.hasNext())
            {
                tmpNTE = it.next();
                if(((String)comboBoxTurnOn.getSelectedItem()).equals("Sounds"))
                {
                    if(tmpNTE.getSoundFile().trim().length() != 0)
                    {
                        tmpNTE.setSound(true);
                        tmpNTE.setModify(true);
                    }
                }
                if(((String)comboBoxTurnOn.getSelectedItem())
                        .equals("Program Execution"))
                {
                    if(tmpNTE.getProgramFile().trim().length() != 0)
                    {
                        tmpNTE.setProgram(true);
                        tmpNTE.setModify(true);
                    }
                }
                if(((String)comboBoxTurnOn.getSelectedItem())
                        .equals("Messages Popup"))
                {
                    tmpNTE.setPopup(true);
                    tmpNTE.setModify(true);
                }
                notificationList.setLine(tmpNTE, cpt);
                cpt ++;
            }
            notificationList.setRowSelectionInterval(index, index);
            tmpNTE = dataVector.elementAt(index);
            if(((String)comboBoxTurnOn.getSelectedItem()).equals("Sounds"))
            {
                playSoundCheckBox.setSelected(tmpNTE.getSound());
            }
            else if(((String)comboBoxTurnOn.getSelectedItem())
                    .equals("Program Execution"))
            {
                programCheckBox.setSelected(tmpNTE.getProgram());
            }
            else if(((String)comboBoxTurnOn.getSelectedItem())
                    .equals("Messages Popup"))
            {
                popupCheckBox.setSelected(tmpNTE.getPopup());
            }
            turnAll = false;
        }
        else if(e.getSource() == turnOffAll)
        {
            Iterator<NotificationsTableEntry> it = dataVector.iterator();
            NotificationsTableEntry tmpNTE = null;
            int cpt = 0;

            if(!it.hasNext())
                return;

            turnAll = true;
            while(it.hasNext())
            {
                tmpNTE = it.next();
                if(((String)comboBoxTurnOff.getSelectedItem()).equals("Sounds"))
                {
                    tmpNTE.setSound(false);
                    tmpNTE.setModify(true);
                }
                if(((String)comboBoxTurnOff.getSelectedItem())
                        .equals("Program Execution"))
                {
                    tmpNTE.setProgram(false);
                    tmpNTE.setModify(true);
                }
                if(((String)comboBoxTurnOff.getSelectedItem())
                        .equals("Messages Popup"))
                {
                    tmpNTE.setPopup(false);
                    tmpNTE.setModify(true);
                }
                notificationList.setLine(tmpNTE, cpt);
                cpt ++;
            }
            notificationList.setRowSelectionInterval(index, index);
            tmpNTE = dataVector.elementAt(index);
            if(((String)comboBoxTurnOn.getSelectedItem()).equals("Sounds"))
            {
                playSoundCheckBox.setSelected(tmpNTE.getSound());
            }
            else if(((String)comboBoxTurnOn.getSelectedItem())
                    .equals("Program Execution"))
            {
                programCheckBox.setSelected(tmpNTE.getProgram());
            }
            else if(((String)comboBoxTurnOn.getSelectedItem())
                    .equals("Messages Popup"))
            {
                popupCheckBox.setSelected(tmpNTE.getPopup());
            }
            turnAll = false;
        }
        else if(e.getSource() == apply)
        {
            Iterator<NotificationsTableEntry> it = dataVector.iterator();
            NotificationsTableEntry tmpNTE = null;
            while(it.hasNext())
            {
                tmpNTE = it.next();

                if(tmpNTE.isModified())
                {
                    logger.debug("Event modify : "+tmpNTE.getEvent());

                    notificationService.setActive(tmpNTE.getEvent(),
                            tmpNTE.getEnabled());
                    if(tmpNTE.getSound() == true)
                    {
                        notificationService.registerNotificationForEvent(
                                tmpNTE.getEvent(),
                                NotificationService.ACTION_SOUND,
                                tmpNTE.getSoundFile(),
                                "");
                        logger.debug("Adding Sound");
                    }
                    else
                    {
                        notificationService.removeEventNotificationAction(
                                tmpNTE.getEvent(),
                                NotificationService.ACTION_SOUND);
                        logger.debug("Deleting Sound");
                    }

                    if(tmpNTE.getProgram() == true)
                    {
                        notificationService.registerNotificationForEvent(
                                tmpNTE.getEvent(),
                                NotificationService.ACTION_COMMAND,
                                tmpNTE.getProgramFile(),
                                "");
                        logger.debug("Program");
                    }
                    else
                    {
                        notificationService.removeEventNotificationAction(
                                tmpNTE.getEvent(),
                                NotificationService.ACTION_COMMAND);
                        logger.debug("Deleting Program");
                    }

                    if(tmpNTE.getPopup())
                    {
                        notificationService.registerNotificationForEvent(
                                tmpNTE.getEvent(),
                                NotificationService.ACTION_POPUP_MESSAGE,
                                "",
                                "");
                        logger.debug("Popup");
                    }
                    else
                    {
                        notificationService.removeEventNotificationAction(
                                tmpNTE.getEvent(),
                                NotificationService.ACTION_POPUP_MESSAGE);
                        logger.debug("Deleting Popup");
                    }
                    tmpNTE.setModify(false);
                }
            }
        }
        else if(e.getSource() == restore)
        {
            notificationService.restoreDefaults();

            int ix = notificationList.getLine();

            if(ix >= 0)
            {
                NotificationsTableEntry tmpNTE = dataVector.elementAt(ix);
                updatePanel(tmpNTE);
            }
        }
    }

    /*
     * Listener of Checkbox
     */
    public void itemStateChanged(ItemEvent itev)
    {
        if(index == -1 || noListener == true)
            return;
        NotificationsTableEntry tmpNTE = dataVector.elementAt(index);
        if(itev.getSource() == playSoundCheckBox)
        {
            if(playSoundCheckBox.isSelected())
            {
                playSoundButton.setEnabled(true);
                soundFileTextField.setEnabled(true);
                soundFileChooser.setEnabled(true);
                tmpNTE.setSound(true);
            }
            else
            {
                playSoundButton.setEnabled(false);
                soundFileTextField.setEnabled(false);
                soundFileChooser.setEnabled(false);
                tmpNTE.setSound(false);
            }
        }
        else if(itev.getSource() == programCheckBox)
        {
            if(programCheckBox.isSelected())
            {
                programFileTextField.setEnabled(true);
                programFileChooser.setEnabled(true);
                tmpNTE.setProgram(true);
            }
            else
            {
                programFileTextField.setEnabled(false);
                programFileChooser.setEnabled(false);
                tmpNTE.setProgram(false);
            }
        }
        else if(itev.getSource() == popupCheckBox)
        {
            if(popupCheckBox.isSelected())
            {
                tmpNTE.setPopup(true);
            }
            else
            {
                tmpNTE.setPopup(false);
            }
        }
        tmpNTE.setModify(true);
        this.updateTableRow(tmpNTE, index);
    }

    /*
     * Listener for TextFields
     */

    public void insertUpdate(DocumentEvent de)
    {
        if(!turnAll)
        {
            if(index != -1 && noListener == false)
            {
                NotificationsTableEntry tmpNTE = dataVector.elementAt(index);
                if(de.getDocument().equals(programFileTextField.getDocument()))
                {
                    tmpNTE.setProgramFile(programFileTextField.getText());
                }
                if(de.getDocument().equals(soundFileTextField.getDocument()))
                {
                    tmpNTE.setSoundFile(soundFileTextField.getText());
                }
                tmpNTE.setModify(true);
                notificationList.setLine(tmpNTE, index);
            }
        }
    }

    public void removeUpdate(DocumentEvent de)
    {
        if(!turnAll)
        {
            if(index != -1 && noListener == false)
            {
                NotificationsTableEntry tmpNTE = dataVector.elementAt(index);
                if(de.getDocument().equals(programFileTextField.getDocument()))
                {
                    tmpNTE.setProgramFile(programFileTextField.getText());
                }
                if(de.getDocument().equals(soundFileTextField.getDocument()))
                {
                    tmpNTE.setSoundFile(soundFileTextField.getText());
                }
                tmpNTE.setModify(true);
                notificationList.setLine(tmpNTE, index);
            }
        }
    }
    public void changedUpdate(DocumentEvent de) {}

    /*
     * Action Listener Service Notifications
     */
    public void actionAdded(NotificationActionTypeEvent event)
    {
        logger.debug("Start action added");
        String eventName = event.getSourceEventType();
        Iterator<NotificationsTableEntry> it = null;
        int row = 0;
        NotificationsTableEntry tmpNTE = null;

        NotificationActionHandler handler = event.getActionHandler();
        boolean isActionEnabled = (handler != null && handler.isEnabled());

        if(dataVector.size() <= 0)
        {
            tmpNTE = new NotificationsTableEntry();
            tmpNTE.setEvent(eventName);

            if(event.getSourceActionType()
                    .equals(NotificationService.ACTION_POPUP_MESSAGE))
            {
                tmpNTE.setPopup(isActionEnabled);
            }
            else if(event.getSourceActionType()
                    .equals(NotificationService.ACTION_COMMAND))
            {
                tmpNTE.setProgram(isActionEnabled);

                tmpNTE.setProgramFile(((CommandNotificationHandler)event
                        .getActionHandler()).getDescriptor());
            }
            else if(event.getSourceActionType()
                    .equals(NotificationService.ACTION_SOUND))
            {
                tmpNTE.setSound(isActionEnabled);

                tmpNTE.setSoundFile(((SoundNotificationHandler)event
                        .getActionHandler()).getDescriptor());
            }
            tmpNTE.setEnabled(notificationService.isActive(eventName));
            this.addRowAtVector(tmpNTE);
            notificationList.setRowSelectionInterval(0, 0);
            updatePanel(tmpNTE);
            logger.debug("End action added");
            return;
        }
        /*
         * If the vector already contains events and the element that we want to
         * add the action to, already exists.
         */
        it = dataVector.iterator();
        while(it.hasNext())
        {
            tmpNTE = it.next();
            if(tmpNTE.getEvent().equals(eventName))
            {
                if(event.getSourceActionType()
                        .equals(NotificationService.ACTION_POPUP_MESSAGE))
                {
                    tmpNTE.setPopup(isActionEnabled);

                }
                else if(event.getSourceActionType()
                        .equals(NotificationService.ACTION_COMMAND))
                {
                    tmpNTE.setProgram(isActionEnabled);
                    tmpNTE.setProgramFile(((CommandNotificationHandler)event
                            .getActionHandler()).getDescriptor());
                }
                else if(event.getSourceActionType()
                        .equals(NotificationService.ACTION_SOUND))
                {
                    tmpNTE.setSound(isActionEnabled);
                    tmpNTE.setSoundFile(((SoundNotificationHandler)event
                            .getActionHandler()).getDescriptor());
                }
                tmpNTE.setEnabled(notificationService.isActive(eventName));
                this.updateTableRow(tmpNTE,row);
                updatePanel(tmpNTE);
                notificationList.setRowSelectionInterval(row, row);
                logger.debug("End action added");
                return;
            }
            row ++;
        }
        /*
         * The vector already contains events but not the one that we want
         * to attach the action to. We create it and add it to the vector.
         */
        tmpNTE = new NotificationsTableEntry();
        tmpNTE.setEvent(eventName);
        if(event.getSourceActionType()
                .equals(NotificationService.ACTION_POPUP_MESSAGE))
        {
            tmpNTE.setPopup(isActionEnabled);
        }
        else if(event.getSourceActionType()
                .equals(NotificationService.ACTION_COMMAND))
        {
            tmpNTE.setProgram(isActionEnabled);
            tmpNTE.setProgramFile(((CommandNotificationHandler)event
                    .getActionHandler()).getDescriptor());
        }
        else if(event.getSourceActionType()
                .equals(NotificationService.ACTION_SOUND))
        {
            tmpNTE.setSound(isActionEnabled);
            tmpNTE.setSoundFile(((SoundNotificationHandler)event
                    .getActionHandler()).getDescriptor());
        }
        tmpNTE.setEnabled(notificationService.isActive(eventName));
        this.addRowAtVector(tmpNTE);
        updatePanel(tmpNTE);
        notificationList.setRowSelectionInterval(
            notificationList.getRowCount() - 1,
            notificationList.getRowCount() - 1);

        logger.debug("End action added");
        return;
    }

    public void actionRemoved(NotificationActionTypeEvent event)
    {
        logger.debug("Start action remove");
        String eventName = event.getSourceEventType();
        Iterator<NotificationsTableEntry> it = null;
        NotificationsTableEntry tmpNTE = null;
        if(dataVector.size() == 0)
            return;

        it = dataVector.iterator();
        while(it.hasNext())
        {
            tmpNTE = it.next();
            if(tmpNTE.getEvent().equals(eventName))
            {
                if(event.getSourceActionType()
                        .equals(NotificationService.ACTION_POPUP_MESSAGE))
                {
                    tmpNTE.setPopup(false);
                }
                else if(event.getSourceActionType()
                        .equals(NotificationService.ACTION_COMMAND))
                {
                    tmpNTE.setProgram(false);
                    tmpNTE.setProgramFile("");
                }
                else if(event.getSourceActionType()
                        .equals(NotificationService.ACTION_SOUND))
                {
                    tmpNTE.setSound(false);
                    tmpNTE.setSoundFile("");
                }
                logger.debug("End action remove");
                return;
            }
        }
    }

    public void actionChanged(NotificationActionTypeEvent event)
    {
        logger.debug("Start action changed");
        String eventName = event.getSourceEventType();
        Iterator<NotificationsTableEntry> it = null;
        int row = 0;
        NotificationsTableEntry tmpNTE = null;

        it = dataVector.iterator();
        while(it.hasNext())
        {
            tmpNTE = it.next();
            if(tmpNTE.getEvent().equals(eventName))
            {
                if(event.getSourceActionType()
                        .equals(NotificationService.ACTION_COMMAND))
                {
                    tmpNTE.setProgramFile(((CommandNotificationHandler)event
                            .getActionHandler()).getDescriptor());
                }
                else if(event.getSourceActionType()
                                .equals(NotificationService.ACTION_SOUND))
                {
                    tmpNTE.setSoundFile(((SoundNotificationHandler)event
                            .getActionHandler()).getDescriptor());
                }
                this.updateTableRow(tmpNTE,row);
                logger.debug("End action changed");
                return;
            }
            row ++;
        }
    }

    public void eventTypeAdded(NotificationEventTypeEvent event)
    {
        String eventAdded = event.getSourceEventType();
        Iterator<NotificationsTableEntry> it = dataVector.iterator();
        NotificationsTableEntry tmpNTE = null;
        while(it.hasNext())
        {
            tmpNTE = it.next();
            if(tmpNTE.getEvent().equals(eventAdded))
                return;
        }
        tmpNTE = new NotificationsTableEntry();
        tmpNTE.setEvent(eventAdded);
        tmpNTE.setEnabled(notificationService.isActive(event.getSourceEventType()));
        this.addRowAtVector(tmpNTE);
    }

    public void eventTypeRemoved(NotificationEventTypeEvent event)
    {
        Iterator<NotificationsTableEntry> it = null;
        NotificationsTableEntry tmpNTE = null;
        int row = 0;

        if(dataVector.size() <= 0)
            return;
        it = dataVector.iterator();
        while(it.hasNext())
        {
            tmpNTE = it.next();
            if(tmpNTE.getEvent().equals(event.getSourceEventType()))
            {
                dataVector.remove(row);
                notificationList.removeLine(row);
                return;
            }
            row ++;
        }
    }


    public void updateTable()
    {
        Iterator<NotificationsTableEntry> it = dataVector.iterator();
        NotificationsTableEntry tmpNTE = null;
        int i = 0;
        int cpt = notificationList.getRowCount();
        /**
         * Emptying the list of notifications
         */
        for(i = 0; i < cpt; i ++)
        {
            notificationList.removeLine(0);
        }

        if(!it.hasNext())
        {
            activate.setEnabled(false);
            deactivate.setEnabled(false);
            programCheckBox.setSelected(false);
            programFileChooser.setEnabled(false);
            programFileTextField.setEnabled(false);
            playSoundCheckBox.setSelected(false);
            playSoundButton.setEnabled(false);
            soundFileChooser.setEnabled(false);
            soundFileTextField.setEnabled(false);
            popupCheckBox.setSelected(false);
            turnOnAll.setEnabled(false);
            comboBoxTurnOn.setEnabled(false);
            turnOffAll.setEnabled(false);
            comboBoxTurnOff.setEnabled(false);
            index = -1;

            return;
        }

        while(it.hasNext())
        {
            tmpNTE = it.next();
            notificationList.addLine(tmpNTE);
        }
    }

    public void buildingVector()
    {
        Iterator<String> it = notificationService.getRegisteredEvents();
        NotificationsTableEntry tmpNTE = null;
        String event = null;
        dataVector.removeAllElements();

        while(it.hasNext())
        {
            event = it.next();

            tmpNTE = new NotificationsTableEntry(
                    notificationService.isActive(event),
                    false,
                    "",
                    false,
                    false,
                    "",
                    event,
                    false);

            Map<String, NotificationActionHandler> actionsMap
                = notificationService.getEventNotifications(event);
            if(actionsMap != null)
            {
                for (Map.Entry<String, NotificationActionHandler> mEntry
                        : actionsMap.entrySet())
                {
                    String actionType = mEntry.getKey();
                    NotificationActionHandler handler = mEntry.getValue();
                    boolean isActionEnabled = (handler == null) ? false : handler.isEnabled();

                    if(actionType
                            .equals(NotificationService.ACTION_POPUP_MESSAGE))
                    {
                        tmpNTE.setPopup(isActionEnabled);
                    }
                    else if(actionType
                            .equals(NotificationService.ACTION_SOUND) &&
                            handler != null)
                    {
                        tmpNTE.setSound(isActionEnabled);
                        tmpNTE.setSoundFile(
                            ((SoundNotificationHandler) handler).getDescriptor());
                    }
                    else if(actionType
                            .equals(NotificationService.ACTION_COMMAND) &&
                            handler != null)
                    {
                        tmpNTE.setProgram(isActionEnabled);
                        tmpNTE.setProgramFile(
                                ((CommandNotificationHandler) handler)
                                .getDescriptor());
                    }
                }
            }
            dataVector.add(tmpNTE);
        }
    }

    public void addRowAtVector(NotificationsTableEntry tmpNTE)
    {
        dataVector.add(tmpNTE);
        notificationList.addLine(tmpNTE);
    }

    public void updateTableRow(NotificationsTableEntry entry, int index)
    {
        notificationList.setLine(entry, index);
    }

    class MyMouseAdapter implements MouseListener
    {
        public void mouseClicked(MouseEvent me)
        {
            index = notificationList.rowAtPoint(me.getPoint());

            noListener = true;

            if(index != -1)
            {
                NotificationsTableEntry tmpNTE = dataVector.elementAt(index);
                activate.setEnabled(!tmpNTE.getEnabled());
                deactivate.setEnabled(tmpNTE.getEnabled());
                if(tmpNTE.getProgram()
                    && tmpNTE.getProgramFile().trim().length() > 0)
                {
                    programCheckBox.setSelected(true);
                    programFileChooser.setEnabled(tmpNTE.getProgram());
                    programFileTextField.setEnabled(tmpNTE.getProgram());
                    programFileTextField.setText(tmpNTE.getProgramFile());
                }
                else
                {
                    programCheckBox.setSelected(false);
                    programFileChooser.setEnabled(false);
                    programFileTextField.setEnabled(false);
                    programFileTextField.setText(tmpNTE.getProgramFile());
                }
                if(tmpNTE.getSound()
                    && tmpNTE.getSoundFile().trim().length() > 0)
                {
                    playSoundCheckBox.setSelected(true);
                    playSoundButton.setEnabled(true);
                    soundFileChooser.setEnabled(true);
                    soundFileTextField.setEnabled(true);
                    soundFileTextField.setText(tmpNTE.getSoundFile());
                }
                else
                {
                    playSoundCheckBox.setSelected(false);
                    playSoundButton.setEnabled(false);
                    soundFileChooser.setEnabled(false);
                    soundFileTextField.setEnabled(false);
                    soundFileTextField.setText(tmpNTE.getSoundFile());
                }
                popupCheckBox.setSelected(tmpNTE.getPopup());
                notificationList.setRowSelectionInterval(index,index);
                noListener = false;
                return;
            }
        }

        public void mousePressed(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}
        public void mouseExited(MouseEvent e) {}
        public void mouseEntered(MouseEvent e) {}
    }
}
