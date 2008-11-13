/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.generalconfig;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

import java.io.*;
import com.izforge.izpack.util.os.*;

/**
 * 
 * @author Yana Stamcheva
 */
public class GeneralConfigurationForm
    extends javax.swing.JPanel
    implements  ConfigurationForm,
                ActionListener
{
    private Logger logger = Logger.getLogger(GeneralConfigurationForm.class);

    private JPanel mainPanel;
    private JCheckBox bringToFrontCheckBox;
    private JCheckBox autoStartCheckBox;
    private JCheckBox groupMessagesCheckBox;
    private JCheckBox logHistoryCheckBox;
    private JPanel sendMessagePanel;
    private JLabel sendMessageLabel;
    private JComboBox sendMessageComboBox;
    private JLabel historySizeLabel;
    private JSpinner historySizeSpinner;
    private JCheckBox enableTypingNotifiCheckBox;
    private JCheckBox showHistoryCheckBox;
    private JPanel logHistoryPanel;

    public GeneralConfigurationForm()
    {
        super();
        initGUI();
        initDefaults();
    }

    private void initGUI()
    {
        BorderLayout borderLayout = new BorderLayout();

        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        this.setLayout(borderLayout);
        setPreferredSize(new Dimension(500, 300));
        {
            mainPanel = new JPanel();
            this.setOpaque(false);
            this.mainPanel.setOpaque(false);

            BoxLayout boxLayout = new BoxLayout(
                mainPanel, javax.swing.BoxLayout.Y_AXIS);
            mainPanel.setLayout(boxLayout);
            this.add(mainPanel, BorderLayout.NORTH);

            String osName = System.getProperty("os.name");
            if (osName.startsWith("Windows"))
            {
                autoStartCheckBox = new JCheckBox();
                mainPanel.add(autoStartCheckBox);
                mainPanel.add(Box.createVerticalStrut(10));
                autoStartCheckBox.setText(
                    Resources.getString("autoStartOption",
                        new String[]{
                            Resources.getSettingsString("applicationName")}));
                initAutoStartCheckBox();
                autoStartCheckBox.addActionListener(this);
            }
            {
                groupMessagesCheckBox = new JCheckBox();
                mainPanel.add(groupMessagesCheckBox);
                mainPanel.add(Box.createVerticalStrut(10));
                groupMessagesCheckBox.setText(
                    Resources.getString("groupChatMessages"));
                groupMessagesCheckBox.addActionListener(this);
            }
            {
                logHistoryPanel = new JPanel();
                logHistoryPanel.setOpaque(false);

                mainPanel.add(logHistoryPanel);
                mainPanel.add(Box.createVerticalStrut(10));
                logHistoryPanel.setLayout(null);
                logHistoryPanel.setPreferredSize(
                    new java.awt.Dimension(380, 57));
                logHistoryPanel.setAlignmentX(0.0f);
                {
                    logHistoryCheckBox = new JCheckBox();
                    logHistoryPanel.add(logHistoryCheckBox);
                    logHistoryCheckBox.setText(
                        Resources.getString("logHistory"));
                    logHistoryCheckBox.setBounds(0, 0, 200, 19);
                    logHistoryCheckBox.addActionListener(this);
                    logHistoryCheckBox.addChangeListener(new ChangeListener()
                    {
                        public void stateChanged(ChangeEvent e)
                        {
                            showHistoryCheckBox.setEnabled(
                                logHistoryCheckBox.isSelected());
                            historySizeSpinner.setEnabled(
                                logHistoryCheckBox.isSelected());
                        }
                    });
                }
                {
                    showHistoryCheckBox = new JCheckBox();
                    logHistoryPanel.add(showHistoryCheckBox);
                    showHistoryCheckBox.setText(
                        Resources.getString("showHistory"));
                    showHistoryCheckBox.setBounds(17, 25, 140, 19);
                    showHistoryCheckBox.addActionListener(this);
                    showHistoryCheckBox.addChangeListener(new ChangeListener()
                    {
                        public void stateChanged(ChangeEvent e)
                        {
                            historySizeSpinner.setEnabled(
                                showHistoryCheckBox.isSelected());
                        }
                    });
                }
                {
                    SpinnerNumberModel historySizeSpinnerModel = 
                        new SpinnerNumberModel(0, 0, 100, 1);
                    historySizeSpinner = new JSpinner();
                    logHistoryPanel.add(historySizeSpinner);
                    historySizeSpinner.setModel(historySizeSpinnerModel);
                    historySizeSpinner.setBounds(150, 23, 47, 22);
                    historySizeSpinnerModel.addChangeListener(
                        new ChangeListener()
                        {
                            public void stateChanged(ChangeEvent e)
                            {
                                ConfigurationManager.setChatHistorySize(
                                    ((Integer) historySizeSpinner
                                        .getValue()).intValue());
                            }
                        });
                }
                {
                    historySizeLabel = new JLabel();
                    logHistoryPanel.add(historySizeLabel);
                    historySizeLabel.setText(
                        Resources.getString("historySize"));
                    historySizeLabel.setBounds(205, 27, 220, 15);
                }
            }
            {
                sendMessagePanel = new JPanel();
                sendMessagePanel.setOpaque(false);

                BorderLayout sendMessagePanelLayout
                    = new BorderLayout(10, 10);
                sendMessagePanel.setLayout(sendMessagePanelLayout);
                mainPanel.add(sendMessagePanel);
                mainPanel.add(Box.createVerticalStrut(10));
                sendMessagePanel.setAlignmentX(0.0f);
                sendMessagePanel.setPreferredSize(
                    new java.awt.Dimension(380, 22));
                {
                    sendMessageLabel = new JLabel();
                    sendMessagePanel.add(
                        sendMessageLabel, BorderLayout.WEST);
                    sendMessageLabel.setText(
                        Resources.getString("sendMessagesWith"));
                }
                {
                    ComboBoxModel sendMessageComboBoxModel = 
                        new DefaultComboBoxModel(
                            new String[] {
                                ConfigurationManager.ENTER_COMMAND,
                                ConfigurationManager.CTRL_ENTER_COMMAND });
                    sendMessageComboBox = new JComboBox();
                    sendMessagePanel.add(
                        sendMessageComboBox, BorderLayout.CENTER);
                    sendMessageComboBox.setModel(sendMessageComboBoxModel);
                    sendMessageComboBox.addItemListener(new ItemListener()
                    {
                        public void itemStateChanged(ItemEvent arg0)
                        {
                            ConfigurationManager.setSendMessageCommand(
                                (String)sendMessageComboBox.getSelectedItem());
                        }
                    });
                }
            }
            {
                enableTypingNotifiCheckBox = new JCheckBox();
                enableTypingNotifiCheckBox.setLayout(null);
                mainPanel.add(enableTypingNotifiCheckBox);
                mainPanel.add(Box.createVerticalStrut(10));
                enableTypingNotifiCheckBox.setText(
                    Resources.getString("enableTypingNotifications"));
                enableTypingNotifiCheckBox.setPreferredSize(
                    new java.awt.Dimension(253, 20));
                enableTypingNotifiCheckBox.setAlignmentY(0.0f);
                enableTypingNotifiCheckBox.addActionListener(this);
            }
            {
                bringToFrontCheckBox = new JCheckBox();
                mainPanel.add(bringToFrontCheckBox);
                mainPanel.add(Box.createVerticalStrut(10));
                bringToFrontCheckBox.setText(
                    Resources.getString("bringWindowToFront"));
                bringToFrontCheckBox.addActionListener(this);
            }
//            {
//                JPanel transparencyPanel = new JPanel();
//                BorderLayout transparencyPanelLayout
//                    = new BorderLayout(10, 10);
//                transparencyPanel.setLayout(transparencyPanelLayout);
//                mainPanel.add(transparencyPanel);
//                mainPanel.add(Box.createVerticalStrut(10));
//                transparencyPanel.setAlignmentX(0.0f);
//                transparencyPanel.setPreferredSize(
//                    new java.awt.Dimension(380, 60));
//                {
//                    final JCheckBox enableTransparencyCheckBox
//                        = new JCheckBox(
//                            Resources.getString("enableTransparency"),
//                            ConfigurationManager.isTransparentWindowEnabled());
//                    transparencyPanel.add(
//                        enableTransparencyCheckBox, BorderLayout.NORTH);
//
//                    enableTransparencyCheckBox.addChangeListener(
//                        new ChangeListener()
//                    {
//                        public void stateChanged(ChangeEvent e)
//                        {
//                            ConfigurationManager.setTransparentWindowEnabled(
//                                enableTransparencyCheckBox.isSelected());
//                        }
//                    });
//
//                }
//                {
//                    JLabel transparencyLabel = new JLabel(
//                        Resources.getString("transparency"));
//
//                    transparencyPanel.add(  transparencyLabel,
//                                            BorderLayout.WEST);
//                }
//                {
//                    final JSlider transparencySlider
//                        = new JSlider(0, 255,
//                            ConfigurationManager.getWindowTransparency());
//
//                    transparencyPanel.add(  transparencySlider,
//                                            BorderLayout.CENTER);
//
//                    transparencySlider.addChangeListener(new ChangeListener()
//                    {
//                        public void stateChanged(ChangeEvent e)
//                        {
//                            int value = transparencySlider.getValue();
//                            ConfigurationManager.setWindowTransparency(value);
//                        }
//                    });
//                }
//            }
        }
    }

    private void initDefaults()
    {
        groupMessagesCheckBox.setSelected(
            ConfigurationManager.isMultiChatWindowEnabled());

        logHistoryCheckBox.setSelected(
            ConfigurationManager.isHistoryLoggingEnabled());

        showHistoryCheckBox.setSelected(
            ConfigurationManager.isHistoryShown());

        historySizeSpinner.setValue(
            ConfigurationManager.getChatHistorySize());

        if (!ConfigurationManager.isHistoryLoggingEnabled())
        {
            showHistoryCheckBox.setEnabled(false);
            historySizeSpinner.setEnabled(false);
        }

        if (!ConfigurationManager.isHistoryShown())
        {
            historySizeSpinner.setEnabled(false);
        }

        sendMessageComboBox.setSelectedItem(
            ConfigurationManager.getSendMessageCommand());

        enableTypingNotifiCheckBox.setSelected(
            ConfigurationManager.isSendTypingNotifications());

        bringToFrontCheckBox.setSelected(
            ConfigurationManager.isAutoPopupNewMessage());
    }

    public Object getForm()
    {
        return this;
    }

    public byte[] getIcon()
    {
        return Resources.getImage("pluginIconGeneralConfig");
    }

    public String getTitle()
    {
        return Resources.getString("general");
    }

    public int getIndex()
    {
        return 0;
    }

    public void actionPerformed(ActionEvent event)
    {
        Object sourceObject = event.getSource();
        
        if (sourceObject.equals(autoStartCheckBox))
        {
            try 
            {
                String workingDir = new File(".").getCanonicalPath();
                
                String appName = 
                        Resources.getSettingsString("applicationName");
                ShellLink shortcut = new ShellLink(ShellLink.STARTUP, appName);
                shortcut.setUserType(ShellLink.CURRENT_USER);
                shortcut.setDescription(
                        "This starts " + appName + " Application");
                shortcut.setIconLocation(
                        workingDir + File.separator + "sc-logo.ico", 0);
                shortcut.setShowCommand(ShellLink.MINNOACTIVE);
                shortcut.setTargetPath(workingDir + File.separator + "run.exe");
                shortcut.setWorkingDirectory(workingDir);
                
                String f1 = shortcut.getcurrentUserLinkPath() + 
                        File.separator + appName + ".lnk";
            
                String f2 = f1.replaceAll(
                        System.getProperty("user.name"), 
                        "All Users");

                if(autoStartCheckBox.isSelected())
                {
                    if(!new File(f1).exists() && 
                       !new File(f2).exists())
                    shortcut.save();
                }
                else
                {
                    boolean isFileDeleted = false;
                    try {
                        isFileDeleted = new File(f1).delete();
                    } catch (Exception e) {}
                    
                    try {
                        new File(f2).delete();
                    } catch (Exception e) 
                    {
                        if(!isFileDeleted)
                            GeneralConfigPluginActivator.getUIService().
                                getPopupDialog().showMessagePopupDialog(
                                    e.getMessage(),
                                    Resources.getString("errorPermission"),
                                    PopupDialog.ERROR_MESSAGE);
                        // cannot delete no permissions
                    }
                }
            } catch (Exception e) 
            {
                logger.error("Cannot create/delete startup shortcut", e);
            }
        }
        if (sourceObject.equals(groupMessagesCheckBox))
        {
            ConfigurationManager.setMultiChatWindowEnabled(
                groupMessagesCheckBox.isSelected());
        }
        else if (sourceObject.equals(logHistoryCheckBox))
        {
            ConfigurationManager.setHistoryLoggingEnabled(
                logHistoryCheckBox.isSelected());
        }
        else if (sourceObject.equals(showHistoryCheckBox))
        {
            ConfigurationManager.setHistoryShown(
                showHistoryCheckBox.isSelected());
        }
        else if (sourceObject.equals(enableTypingNotifiCheckBox))
        {
            ConfigurationManager.setSendTypingNotifications(
                enableTypingNotifiCheckBox.isSelected());
        }
        else if (sourceObject.equals(bringToFrontCheckBox))
        {
            ConfigurationManager.setAutoPopupNewMessage(
                bringToFrontCheckBox.isSelected());
        }
    }

    private void initAutoStartCheckBox()
    {
        try 
        {
            String appName = 
                        Resources.getSettingsString("applicationName");
            ShellLink shortcut = 
                new ShellLink(
                    ShellLink.STARTUP, 
                    appName);
            shortcut.setUserType(ShellLink.CURRENT_USER);
            
            String f1 = shortcut.getcurrentUserLinkPath() + 
                        File.separator + appName + ".lnk";
            
            String f2 = f1.replaceAll(
                    System.getProperty("user.name"), 
                    "All Users");

            if(new File(f1).exists() || new File(f2).exists())
                autoStartCheckBox.setSelected(true);
            else
                autoStartCheckBox.setSelected(false);
            
        } catch (Exception e) 
        {
            logger.error(e);
        }
    }
}
