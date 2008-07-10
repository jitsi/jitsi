/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.notificationconfiguration;

import java.io.*;
import java.lang.String.*;
import java.net.MalformedURLException;
import java.util.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.ImageIcon.*;
import javax.swing.border.*;
import javax.swing.JPanel.*;
import javax.swing.BoxLayout.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.audionotifier.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.notification.event.*;

import net.java.sip.communicator.util.*;

/**
 * The <tt>ConfigurationForm</tt> that would be added in the user interface
 * configuration window. It contains a list of all installed notifications.
 * 
 * @author Alexandre Maillard
 */
public class NotificationConfigurationConfigForm
    extends JPanel
    implements ConfigurationForm,
               ActionListener,
               ItemListener,
               DocumentListener,
               NotificationChangeListener
{
    private Logger logger
            = Logger.getLogger(NotificationConfigurationConfigForm.class);
    
    // Declaration of variables concerning the whole of JPanel
    private GridBagConstraints constraints;
    private GridBagLayout gridLayoutGlobal = new GridBagLayout();
    
     
    // Declaration of variables on the table notifications
    private Vector dataVector = null;
    
    private ListMulti notificationList;

    private JLabel icon1
            = new JLabel(new ImageIcon(Resources.getImageInBytes("progIcon")));
    private JLabel icon2
            = new JLabel(new ImageIcon(Resources.getImageInBytes("popupIcon")));
    private JLabel icon3
            = new JLabel(new ImageIcon(Resources.getImageInBytes("soundIcon")));
    private Object column [] = {"Status", icon1, icon2, icon3, "Event"};
    public static final String[] columnToolTips
            = {"Enable or disable this feature",
            "Execute a program",
            "Display a messagebox",
            "Play a sound",
            "Description of event" };
        
    // Declaration of variables - actions
    private GridBagLayout actionsLayout = new GridBagLayout();
    private TitledBorder title1; 
    private JPanel actions = new JPanel();
    
    private JPanel activateDescactivatePanel;
    private FlowLayout layoutADP;
    
    private JButton activate;
    private JButton desactivate;
    private JCheckBox playSoundCheckBox;
    private JButton playSoundButton;
    private JTextField soundFileTextField;
    private JButton soundFileChooser;
    private JCheckBox programCheckBox;
    private JTextField programFileTextField;
    private JButton programFileChooser;
    private JCheckBox popupCheckBox;
    
    
    // Declaration of variables - quickControl
    private FlowLayout layoutButton = new FlowLayout(FlowLayout.CENTER,2,2);
    private TitledBorder title2;
    private JPanel quickControl = new JPanel();
    
    private String [] textComboBox
            = {"Sounds", "Program Execution", "Messages Popup"};
    private JButton turnOnAll;
    private JButton turnOffAll;
    private JComboBox comboBoxTurnOn;
    private JComboBox comboBoxTurnOff;
    
    private JPanel applyPanel = new JPanel();
//    private FlowLayout layoutApply = new FlowLayout(FlowLayout.RIGHT, 5,2);
    private BorderLayout layoutApply = new BorderLayout(5,2);
    private JButton apply;
    private JButton restore;
    
    
    private JFileChooser fileChooserProgram;
    private JFileChooser fileChooserSound;
    
    private int index = -1;
    private boolean turnAll = false;
        
    private NotificationService notificationService = null;
    
    private NotificationConfigurationConfigForm nC = null;
    
    private boolean noListener = false;
    
    public NotificationConfigurationConfigForm()
    {
        super();
        this.nC = this;
        // constraints on the table
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        
        notificationList = new ListMulti(column, columnToolTips);
        
        dataVector = new Vector();
        //dataVector.add(row1);
        //dataVector.add(row2);
        //dataVector.add(row3);
        
        gridLayoutGlobal.setConstraints(notificationList, constraints);
        this.add(notificationList);
                
        // Initializing variable part of the "actions"
        title1 = BorderFactory.createTitledBorder(
                Resources.getString("actions"));
        actions.setBorder(title1);
        
        activateDescactivatePanel = new JPanel();
        layoutADP = new FlowLayout(FlowLayout.CENTER);
        layoutADP.setHgap(75);
        activateDescactivatePanel.setLayout(layoutADP);
        
        activate = new JButton(Resources.getString("activate"));
        activate.setMinimumSize(new Dimension(150,30));
        activate.setPreferredSize(new Dimension(150,30));
        activate.addActionListener(this);
        desactivate = new JButton(Resources.getString("desactivate"));
        desactivate.setMinimumSize(new Dimension(150,30));
        desactivate.setPreferredSize(new Dimension(150,30));
        desactivate.addActionListener(this);
        playSoundCheckBox = new JCheckBox(Resources.getString("playsound"));
        playSoundCheckBox.addItemListener(this);
        playSoundButton = new JButton(
                new ImageIcon(Resources.getImageInBytes("playIcon")));
        playSoundButton.setMinimumSize(new Dimension(50,30));
        playSoundButton.setPreferredSize(new Dimension(50,30));
        playSoundButton.addActionListener(this);
        soundFileTextField = new JTextField();
        soundFileTextField.setMinimumSize(new Dimension(250,30));
        soundFileTextField.setPreferredSize(new Dimension(250,30));
        soundFileTextField.getDocument().addDocumentListener(this);
        soundFileChooser = new JButton(
                new ImageIcon(Resources.getImageInBytes("foldericon")));
        soundFileChooser.setMinimumSize(new Dimension(30,30));
        soundFileChooser.setPreferredSize(new Dimension(30,30));
        soundFileChooser.addActionListener(this);
        programCheckBox = new JCheckBox(Resources.getString("execprog"));
        programCheckBox.addItemListener(this);
        programFileTextField = new JTextField();
        programFileTextField.setMinimumSize(new Dimension(250,30));
        programFileTextField.setPreferredSize(new Dimension(250,30));
        programFileTextField.getDocument().addDocumentListener(this);
        programFileChooser = new JButton(
                new ImageIcon(Resources.getImageInBytes("foldericon")));
        programFileChooser.setMinimumSize(new Dimension(30,30));
        programFileChooser.setPreferredSize(new Dimension(30,30));
        programFileChooser.addActionListener(this);
        popupCheckBox = new JCheckBox(Resources.getString("displaypopup"));
        popupCheckBox.addItemListener(this);
        
       
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 4;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0,0,10,0);
        
        activateDescactivatePanel.add(activate);
        activateDescactivatePanel.add(desactivate);
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
        turnOnAll = new JButton(Resources.getString("turnonall"));
        turnOnAll.addActionListener(this);
        turnOffAll = new JButton(Resources.getString("turnoffall"));
        turnOffAll.addActionListener(this);
        
        
        title2 = BorderFactory.createTitledBorder(
                Resources.getString("quickcontrols"));
        quickControl.setLayout(layoutButton);
        quickControl.setBorder(title2);
        quickControl.add(turnOnAll);
        quickControl.add(comboBoxTurnOn);
        quickControl.add(turnOffAll);
        quickControl.add(comboBoxTurnOff);
        gridLayoutGlobal.setConstraints(quickControl, constraints);
//        this.add(quickControl);
        
        this.setLayout(gridLayoutGlobal);
        
        
        fileChooserSound = new JFileChooser();
        fileChooserProgram = new JFileChooser();
        fileChooserSound.setMultiSelectionEnabled(false);
        fileChooserProgram.setMultiSelectionEnabled(false);
        fileChooserSound.addChoosableFileFilter(new SoundFilter());
        
        notificationList.addMouseListener(new MyMouseAdapter());
        
        
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        
        restore = new JButton(Resources.getString("restore"));
        restore.addActionListener(this);
        apply = new JButton(Resources.getString("apply"));
        apply.addActionListener(this);
        applyPanel.setLayout(layoutApply);
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
            NotificationsTableEntry tmpNTE
                    = (NotificationsTableEntry) dataVector.elementAt(0);

            updatePanel(tmpNTE);
            notificationList.setRowSelectionInterval(0,0);
            index = 0;
        }
    }
    
    private void updatePanel(NotificationsTableEntry tmpNTE)
    {
        noListener = true;
        activate.setEnabled(!tmpNTE.getEnabled());
        desactivate.setEnabled(tmpNTE.getEnabled());
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
    
    /**
     * Implements the <tt>ConfigurationForm.getTitle()</tt> method. Returns the
     * title of this configuration form.
     */
    public String getTitle()
    {
        return Resources.getString("notification");
    }

    /**
     * Implements the <tt>ConfigurationForm.getIcon()</tt> method. Returns the
     * icon of this configuration form.
     */
    public byte[] getIcon()
    {
        return Resources.getImageInBytes("notificationIcon");
    }

    /**
     * Implements the <tt>ConfigurationForm.getForm()</tt> method. Returns the
     * component corresponding to this configuration form.
     */
    public Object getForm()
    {
        return this;
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == activate)
        {
            NotificationsTableEntry tmpNTE 
                    = (NotificationsTableEntry) dataVector.elementAt(index);
            tmpNTE.setEnabled(true);
            this.updateTableRow(tmpNTE, index);
            activate.setEnabled(false);
            desactivate.setEnabled(true);
            tmpNTE.setModify(true);
        }    
        else if(e.getSource() == desactivate)
        {
            NotificationsTableEntry tmpNTE 
                    = (NotificationsTableEntry) dataVector.elementAt(index);
            tmpNTE.setEnabled(false);
            this.updateTableRow(tmpNTE, index);
            activate.setEnabled(true);
            desactivate.setEnabled(false);
            tmpNTE.setModify(true);
        }
        else if(e.getSource() == soundFileChooser)
        {
            int returnVal = fileChooserSound.showOpenDialog(this);
            
            noListener = true;
            if (returnVal == JFileChooser.APPROVE_OPTION)
            {
                try
                {
                    NotificationsTableEntry tmpNTE = (NotificationsTableEntry) dataVector.elementAt(index);
                    File file = fileChooserSound.getSelectedFile();
                    //This is where a real application would open the file.
                    logger.debug("Opening: " + file.toURI().toURL().toExternalForm());
                    tmpNTE.setSoundFile(file.toURI().toURL().toExternalForm());
                    tmpNTE.setSound(true);
                    tmpNTE.setModify(true);
                    this.updateTableRow(tmpNTE, index);
                    notificationList.setLine(tmpNTE, index);
                    soundFileTextField.setText(file.toURI().toURL().toExternalForm());
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
            int returnVal = fileChooserProgram.showOpenDialog(this);
            noListener = true;
            
            if (returnVal == JFileChooser.APPROVE_OPTION)
            {
                NotificationsTableEntry tmpNTE
                        = (NotificationsTableEntry)dataVector.elementAt(index);
                File file = fileChooserProgram.getSelectedFile();
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
            Iterator it = dataVector.iterator();
            NotificationsTableEntry tmpNTE = null;
            int cpt = 0;
            
            if(!it.hasNext())
                return;
            
            turnAll = true;
            while(it.hasNext())
            {
                tmpNTE = (NotificationsTableEntry)it.next();
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
            tmpNTE = (NotificationsTableEntry) dataVector.elementAt(index);
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
            Iterator it = dataVector.iterator();
            NotificationsTableEntry tmpNTE = null;
            int cpt = 0;
            
            if(!it.hasNext())
                return;
            
            turnAll = true;
            while(it.hasNext())
            {
                tmpNTE = (NotificationsTableEntry)it.next();
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
            tmpNTE = (NotificationsTableEntry) dataVector.elementAt(index);
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
            Iterator it = dataVector.iterator();
            NotificationsTableEntry tmpNTE = null;
            while(it.hasNext())
            {
                tmpNTE = (NotificationsTableEntry) it.next();

                if(tmpNTE.isModified())
                {
                    logger.debug("Event modify : "+tmpNTE.getEvent());
                    
                    notificationService.setActive(tmpNTE.getEvent(),
                            tmpNTE.getEnabled());
                    if(tmpNTE.getSound() == true)
                    {
                        notificationService.registerNotificationForEvent(
                                tmpNTE.getEvent(),
                                notificationService.ACTION_SOUND,
                                tmpNTE.getSoundFile(),
                                "");
                        logger.debug("Adding Sound");
                    }
                    else
                    {
                        notificationService.removeEventNotificationAction(
                                tmpNTE.getEvent(),
                                notificationService.ACTION_SOUND);
                        logger.debug("Deleting Sound");
                    }
                    
                    if(tmpNTE.getProgram() == true)
                    {
                        notificationService.registerNotificationForEvent(
                                tmpNTE.getEvent(),
                                notificationService.ACTION_COMMAND,
                                tmpNTE.getProgramFile(),
                                "");
                        logger.debug("Program");
                    }
                    else
                    {
                        notificationService.removeEventNotificationAction(
                                tmpNTE.getEvent(),
                                notificationService.ACTION_COMMAND);
                        logger.debug("Deleting Program");
                    }
                    
                    if(tmpNTE.getPopup())
                    {
                        notificationService.registerNotificationForEvent(
                                tmpNTE.getEvent(),
                                notificationService.ACTION_POPUP_MESSAGE,
                                "",
                                "");
                        logger.debug("Popup");
                    }
                    else
                    {
                        notificationService.removeEventNotificationAction(
                                tmpNTE.getEvent(),
                                notificationService.ACTION_POPUP_MESSAGE);
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
                NotificationsTableEntry tmpNTE
                        = (NotificationsTableEntry) dataVector.elementAt(ix);
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
        NotificationsTableEntry tmpNTE
                = (NotificationsTableEntry) dataVector.elementAt(index);
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
                NotificationsTableEntry tmpNTE
                        = (NotificationsTableEntry) dataVector.elementAt(index);
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
                NotificationsTableEntry tmpNTE
                        = (NotificationsTableEntry) dataVector.elementAt(index);
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
        String eventName = (String) event.getSourceEventType();
        Iterator it = null;
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
                    .equals(notificationService.ACTION_COMMAND))
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
         * Si le Vecteur contient des évènements et que l'élément auquel on veut
         * rajouter l'action existe déjà.
         */
        it = dataVector.iterator();
        while(it.hasNext())
        {
            tmpNTE = (NotificationsTableEntry)it.next();
            if(tmpNTE.getEvent().equals(eventName))
            {
                if(event.getSourceActionType()
                        .equals(NotificationService.ACTION_POPUP_MESSAGE))
                {
                    tmpNTE.setPopup(isActionEnabled);
                     
                }
                else if(event.getSourceActionType()
                        .equals(notificationService.ACTION_COMMAND))
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
         * Le vecteur contient déjà des évènements mais pas l'évènement auquel
         * on veut rajouter l'action. On le créé et l'ajoute au vecteur.
         */
        tmpNTE = new NotificationsTableEntry();
        tmpNTE.setEvent(eventName);
        if(event.getSourceActionType()
                .equals(NotificationService.ACTION_POPUP_MESSAGE))
        {
            tmpNTE.setPopup(isActionEnabled);
        }
        else if(event.getSourceActionType()
                .equals(notificationService.ACTION_COMMAND))
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
        String eventName = (String) event.getSourceEventType();
        Iterator it = null;
        NotificationsTableEntry tmpNTE = null;
        if(dataVector.size() == 0)
            return;
        
        it = dataVector.iterator();
        while(it.hasNext())
        {
            tmpNTE = (NotificationsTableEntry) it.next();
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
        String eventName = (String) event.getSourceEventType();
        Iterator it = null;
        int row = 0;
        NotificationsTableEntry tmpNTE = null;

        it = dataVector.iterator();
        while(it.hasNext())
        {
            tmpNTE = (NotificationsTableEntry)it.next();
            if(tmpNTE.getEvent().equals(eventName))
            {
                if(event.getSourceActionType()
                        .equals(notificationService.ACTION_COMMAND))
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
        String eventAdded = (String) event.getSourceEventType();
        Iterator it = dataVector.iterator();
        NotificationsTableEntry tmpNTE = null;
        while(it.hasNext())
        {
            tmpNTE = (NotificationsTableEntry) it.next();
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
        Iterator it = null;
        NotificationsTableEntry tmpNTE = null;
        int row = 0;
        
        if(dataVector.size() <= 0)
            return;
        it = dataVector.iterator();
        while(it.hasNext())
        {
            tmpNTE = (NotificationsTableEntry) it.next();
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
        Iterator it = dataVector.iterator();
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
            desactivate.setEnabled(false);
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
            tmpNTE = (NotificationsTableEntry) it.next();
            notificationList.addLine(tmpNTE);
        }
    }
    
    public void buildingVector()
    {
        Iterator it = notificationService.getRegisteredEvents();
        NotificationsTableEntry tmpNTE = null;
        String event = null;
        Map actionsMap = null;
        int i;
        
        dataVector.removeAllElements();
        
        if(!it.hasNext())
            return;
        
        while(it.hasNext())
        {
            String actionType = null;
            event = (String) it.next();
            
            tmpNTE = new NotificationsTableEntry(
                    notificationService.isActive(event),
                    false,
                    "",
                    false,
                    false,
                    "",
                    event,
                    false);
            
            actionsMap = notificationService.getEventNotifications(event);
            if(actionsMap != null)
            {
                Set entry = actionsMap.entrySet();
                Iterator itEntry = entry.iterator();

                while(itEntry.hasNext())
                {
                    Map.Entry mEntry = (Map.Entry) itEntry.next();
                    actionType = (String) mEntry.getKey();

                    NotificationActionHandler handler = null;
                    
                    boolean isActionEnabled = false;
                    
                    if(mEntry.getValue() instanceof NotificationActionHandler)
                    {
                        handler = (NotificationActionHandler)mEntry.getValue();
                        isActionEnabled = handler.isEnabled();
                    }
                    
                    if(actionType
                            .equals(notificationService.ACTION_POPUP_MESSAGE))
                    {
                        tmpNTE.setPopup(isActionEnabled);
                    }
                    else if(actionType
                            .equals(notificationService.ACTION_SOUND) &&
                            handler != null)
                    {
                        tmpNTE.setSound(isActionEnabled);
                        tmpNTE.setSoundFile(
                            ((SoundNotificationHandler) handler).getDescriptor());
                    }
                    else if(actionType
                            .equals(notificationService.ACTION_COMMAND) &&
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
                NotificationsTableEntry tmpNTE
                        = (NotificationsTableEntry) dataVector.elementAt(index);
                activate.setEnabled(!tmpNTE.getEnabled());
                desactivate.setEnabled(tmpNTE.getEnabled());
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

    public int getIndex()
    {
        return -1;
    }
}
