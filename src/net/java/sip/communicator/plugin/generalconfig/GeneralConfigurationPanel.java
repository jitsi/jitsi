/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.generalconfig;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import com.izforge.izpack.util.os.*;

import org.osgi.framework.*;

/**
 * @author Yana Stamcheva
 */
public class GeneralConfigurationPanel
    extends JPanel
    implements ActionListener
{
    private final Logger logger = Logger.getLogger(GeneralConfigurationPanel.class);

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
    private JPanel notifConfigPanel;
    private JLabel notifConfigLabel;
    private JComboBox notifConfigComboBox;

    private JComboBox localesConfigComboBox;
    private JCheckBox updateCheckBox;

    public GeneralConfigurationPanel()
    {
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

            BoxLayout boxLayout = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);
            mainPanel.setLayout(boxLayout);
            this.add(mainPanel, BorderLayout.NORTH);

            if (OSUtils.IS_WINDOWS)
            {
                autoStartCheckBox = new SIPCommCheckBox();
                mainPanel.add(autoStartCheckBox);
                mainPanel.add(new JSeparator());
                mainPanel.add(Box.createVerticalStrut(10));

                autoStartCheckBox.setText(
                    Resources.getString(
                        "plugin.generalconfig.AUTO_START",
                        new String[]{getApplicationName()}));

                initAutoStartCheckBox();
                autoStartCheckBox.addActionListener(this);
            }
            {
                groupMessagesCheckBox = new SIPCommCheckBox();
                mainPanel.add(groupMessagesCheckBox);
                mainPanel.add(Box.createVerticalStrut(10));
                groupMessagesCheckBox.setText(
                    Resources.getString(
                        "plugin.generalconfig.GROUP_CHAT_MESSAGES"));
                groupMessagesCheckBox.addActionListener(this);
            }
            {
                logHistoryPanel = new JPanel();
                logHistoryPanel.setOpaque(false);

                mainPanel.add(logHistoryPanel);
                mainPanel.add(Box.createVerticalStrut(10));
                logHistoryPanel.setLayout(null);
                logHistoryPanel.setPreferredSize(new Dimension(380, 57));
                logHistoryPanel.setAlignmentX(0.0f);
                {
                    logHistoryCheckBox = new SIPCommCheckBox();
                    logHistoryPanel.add(logHistoryCheckBox);
                    logHistoryCheckBox.setText(
                        Resources.getString("plugin.generalconfig.LOG_HISTORY"));
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
                    showHistoryCheckBox = new SIPCommCheckBox();
                    logHistoryPanel.add(showHistoryCheckBox);
                    showHistoryCheckBox.setText(
                        Resources.getString("plugin.generalconfig.SHOW_HISTORY"));
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
                        Resources.getString("plugin.generalconfig.HISTORY_SIZE"));
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
                sendMessagePanel.setPreferredSize(new Dimension(380, 22));
                {
                    sendMessageLabel = new JLabel();
                    sendMessagePanel.add(
                        sendMessageLabel, BorderLayout.WEST);
                    sendMessageLabel.setText(
                        Resources.getString("plugin.generalconfig.SEND_MESSAGES_WITH"));
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
                enableTypingNotifiCheckBox = new SIPCommCheckBox();
                enableTypingNotifiCheckBox.setLayout(null);
                mainPanel.add(enableTypingNotifiCheckBox);
                mainPanel.add(Box.createVerticalStrut(10));
                enableTypingNotifiCheckBox.setText(
                    Resources.getString("service.gui.ENABLE_TYPING_NOTIFICATIONS"));
                enableTypingNotifiCheckBox.setPreferredSize(
                    new Dimension(253, 20));
                enableTypingNotifiCheckBox.setAlignmentY(0.0f);
                enableTypingNotifiCheckBox.addActionListener(this);
            }
            {
                bringToFrontCheckBox = new SIPCommCheckBox();
                mainPanel.add(bringToFrontCheckBox);
                mainPanel.add(new JSeparator());
                mainPanel.add(Box.createVerticalStrut(10));
                bringToFrontCheckBox.setText(
                    Resources.getString("plugin.generalconfig.BRING_WINDOW_TO_FRONT"));
                bringToFrontCheckBox.addActionListener(this);
            }
            {
                ServiceReference[] handlerRefs = null;
                BundleContext bc = GeneralConfigPluginActivator.bundleContext;
                try
                {
                    handlerRefs = bc.getServiceReferences(
                        PopupMessageHandler.class.getName(),
                        null);
                }
                catch (InvalidSyntaxException ex)
                {
                    logger.warn("Error while retrieving service refs", ex);
                }
                if (handlerRefs != null)
                {
                    notifConfigPanel = new JPanel();
                    notifConfigPanel.setOpaque(false);
                    notifConfigPanel.setLayout(new BorderLayout(10, 10));
                    notifConfigPanel.setAlignmentX(0.0f);
                    notifConfigPanel.setPreferredSize(new Dimension(380, 22));

                    mainPanel.add(notifConfigPanel);
                    mainPanel.add(Box.createVerticalStrut(4));
                    mainPanel.add(new JSeparator());
                    mainPanel.add(Box.createVerticalStrut(10));
                    {
                        notifConfigLabel = new JLabel(
                            Resources.getString(
                            "plugin.notificationconfig.POPUP_NOTIF_HANDLER"));
                        notifConfigPanel.add(
                            notifConfigLabel, BorderLayout.WEST);
                    }
                    {
                        notifConfigComboBox = new JComboBox();

                        String configuredHandler = (String) GeneralConfigPluginActivator
                            .getConfigurationService().getProperty("systray.POPUP_HANDLER");
                        
                        for (ServiceReference ref : handlerRefs)
                        {
                            PopupMessageHandler handler =
                                (PopupMessageHandler) bc.getService(ref);

                            notifConfigComboBox.addItem(handler);

                            if (configuredHandler != null && 
                                configuredHandler.equals(handler.getClass().getName()))
                            {
                                notifConfigComboBox.setSelectedItem(handler);
                            }
                        }
                        
                        // We need an entry in combo box that represents automatic
                        // popup handler selection in systray service. It is selected
                        // only if there is no user preference regarding which popup 
                        // handler to use.
                        String auto = "Auto";
                        notifConfigComboBox.addItem(auto);
                        if (configuredHandler == null)
                        {
                            notifConfigComboBox.setSelectedItem(auto);
                        }

                        notifConfigComboBox.addItemListener(new ItemListener()
                        {
                            public void itemStateChanged(ItemEvent evt)
                            {
                                if (notifConfigComboBox.getSelectedItem() instanceof String)
                                {
                                    // "Auto" selected. Delete the user's preference and
                                    // select the best available handler.
                                    ConfigurationManager.setPopupHandlerConfig(null);
                                    GeneralConfigPluginActivator.getSystrayService()
                                        .selectBestPopupMessageHandler();
                                    
                                } else
                                {
                                    PopupMessageHandler handler =
                                        (PopupMessageHandler)
                                        notifConfigComboBox.getSelectedItem();

                                    ConfigurationManager.setPopupHandlerConfig(
                                        handler.getClass().getName());

                                    GeneralConfigPluginActivator.getSystrayService()
                                        .setActivePopupMessageHandler(handler);
                                }
                            }
                        });
                        notifConfigPanel.add(
                            notifConfigComboBox, BorderLayout.CENTER);
                    }
                }

            }
            {
                JPanel localeConfigPanel = new JPanel();
                localeConfigPanel.setOpaque(false);
                localeConfigPanel.setLayout(new BorderLayout(10, 10));
                localeConfigPanel.setAlignmentX(0.0f);
                localeConfigPanel.setPreferredSize(new Dimension(380, 22));

                mainPanel.add(localeConfigPanel);
                mainPanel.add(Box.createVerticalStrut(4));
                mainPanel.add(new JSeparator());
                mainPanel.add(Box.createVerticalStrut(10));
                {
                    localeConfigPanel.add(
                        new JLabel(
                            Resources.getString(
                            "plugin.generalconfig.DEFAULT_LANGUAGE")),
                        BorderLayout.WEST);
                }
                {
                    localesConfigComboBox = new JComboBox();

                    Iterator<Locale> iter =
                            Resources.getResources().getAvailableLocales();
                    while (iter.hasNext())
                    {
                        Locale locale = iter.next();
                        localesConfigComboBox.addItem(
                            locale.getDisplayLanguage(locale));
                    }
                    Locale currLocale =
                        ConfigurationManager.getCurrentLanguage();
                    localesConfigComboBox.setSelectedItem(currLocale
                        .getDisplayLanguage(currLocale));

                    localesConfigComboBox.addActionListener(new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            GeneralConfigPluginActivator.getUIService().getPopupDialog().
                                showMessagePopupDialog(Resources.getString(
                                "plugin.generalconfig.DEFAULT_LANGUAGE_RESTART_WARN"));

                            String language =
                                    (String)localesConfigComboBox.getSelectedItem();
                            Iterator<Locale> iter =
                                Resources.getResources().getAvailableLocales();
                            while (iter.hasNext())
                            {
                                Locale locale = iter.next();
                                if(locale.getDisplayLanguage(locale)
                                    .equals(language))
                                {
                                    ConfigurationManager.setLanguage(locale);
                                    break;
                                }
                            }
                        }
                    });
                    localeConfigPanel.add(
                        localesConfigComboBox, BorderLayout.CENTER);
                }
            }
            {
                JPanel sipClientPortConfigPanel = new JPanel();
                sipClientPortConfigPanel.setOpaque(false);
                sipClientPortConfigPanel.setLayout(new BorderLayout(10, 10));
                sipClientPortConfigPanel.setAlignmentX(0.0f);
                sipClientPortConfigPanel.setPreferredSize(new Dimension(380, 72));

                JPanel labelPanel = new JPanel(new GridLayout(0, 1, 2, 2));
                labelPanel.setOpaque(false);
                JPanel valuePanel = new JPanel(new GridLayout(0, 1, 2, 2));
                valuePanel.setOpaque(false);

                mainPanel.add(sipClientPortConfigPanel);
                mainPanel.add(new JSeparator());
                mainPanel.add(Box.createVerticalStrut(10));

                mainPanel.add(Box.createVerticalStrut(10));
                {
                    sipClientPortConfigPanel.add(labelPanel,
                        BorderLayout.WEST);
                    sipClientPortConfigPanel.add(valuePanel,
                        BorderLayout.CENTER);
                }
                {
                    labelPanel.add(new JLabel(
                        Resources.getString(
                            "plugin.generalconfig.SIP_CLIENT_PORT")));
                    labelPanel.add(new JLabel(
                        Resources.getString(
                            "plugin.generalconfig.SIP_CLIENT_SECURE_PORT")));

                    JPanel emptyPanel = new JPanel();
                    emptyPanel.setOpaque(false);
                    emptyPanel.setMaximumSize(new Dimension(40, 35));
                    labelPanel.add(emptyPanel);
                    
                    final JTextField clientPortField = new JTextField(6);
                    clientPortField.setText(
                        String.valueOf(ConfigurationManager.getClientPort()));
                    valuePanel.add(clientPortField);
                    clientPortField.addFocusListener(new FocusListener()
                    {
                        private String oldValue = null;

                        public void focusLost(FocusEvent e)
                        {
                            try
                            {
                                int port =
                                    Integer.valueOf(clientPortField.getText());

                                if(port <= 0 || port > 65535)
                                    throw new NumberFormatException(
                                        "Not a port number");

                                ConfigurationManager.setClientPort(port);
                            }
                            catch (NumberFormatException ex)
                            {
                                // not a number for port
                                String error =
                                    Resources.getString(
                                        "plugin.generalconfig.ERROR_PORT_NUMBER");
                                GeneralConfigPluginActivator.getUIService().
                                getPopupDialog().showMessagePopupDialog(
                                    error,
                                    error,
                                    PopupDialog.ERROR_MESSAGE);
                                clientPortField.setText(oldValue);
                            }


                        }

                        public void focusGained(FocusEvent e)
                        {
                            oldValue = clientPortField.getText();
                        }
                    });
                    final JTextField clientSecurePortField = new JTextField(6);
                    clientSecurePortField.setText(
                        String.valueOf(ConfigurationManager.getClientSecurePort()));
                    valuePanel.add(clientSecurePortField);
                    clientSecurePortField.addFocusListener(new FocusListener()
                    {
                        private String oldValue = null;

                        public void focusLost(FocusEvent e)
                        {
                            try
                            {
                                int port =
                                    Integer.valueOf(clientSecurePortField.getText());

                                if(port <= 0 || port > 65535)
                                    throw new NumberFormatException(
                                        "Not a port number");

                                ConfigurationManager.setClientSecurePort(port);
                            }
                            catch (NumberFormatException ex)
                            {
                                // not a number for port
                                String error =
                                    Resources.getString(
                                        "plugin.generalconfig.ERROR_PORT_NUMBER");
                                GeneralConfigPluginActivator.getUIService().
                                getPopupDialog().showMessagePopupDialog(
                                    error,
                                    error,
                                    PopupDialog.ERROR_MESSAGE); 
                                clientSecurePortField.setText(oldValue);
                            }
                        }

                        public void focusGained(FocusEvent e)
                        {
                            oldValue = clientSecurePortField.getText();
                        }
                    });
                    JLabel warnLabel = new JLabel("* " + 
                        Resources.getString(
                            "plugin.generalconfig.DEFAULT_LANGUAGE_RESTART_WARN"));
                    warnLabel.setForeground(Color.GRAY);
                    warnLabel.setFont(warnLabel.getFont().deriveFont(8));
                    warnLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
                    valuePanel.add(warnLabel);
                }
            }
            if(!OSUtils.IS_MAC)// if we are not running mac
            {
                updateCheckBox = new SIPCommCheckBox();
                mainPanel.add(updateCheckBox);
                mainPanel.add(new JSeparator());
                mainPanel.add(Box.createVerticalStrut(10));
                updateCheckBox.setText(
                    Resources.getString("plugin.generalconfig.CHECK_FOR_UPDATES"));
                updateCheckBox.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e)
                    {
                        GeneralConfigPluginActivator.getConfigurationService()
                            .setProperty(
                                "net.java.sip.communicator.plugin.updatechecker.ENABLED",
                            Boolean.toString(
                                ((JCheckBox)e.getSource()).isSelected()));
                    }
                });
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
//                            Resources.getString("plugin.generalconfig.ENABLE_TRANSPARENCY"),
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
//                        Resources.getString("plugin.generalconfig.TRANSPARENCY"));
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

    /**
     * Init default values.
     */
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

        if(!OSUtils.IS_MAC)// if we are not running mac
        {
            updateCheckBox.setSelected(
            GeneralConfigPluginActivator.getConfigurationService().getBoolean((
                "net.java.sip.communicator.plugin.updatechecker.ENABLED"), true));
        }
    }

    /**
     * Returns the application name.
     * @return
     */
    private String getApplicationName()
    {
        return Resources.getSettingsString("service.gui.APPLICATION_NAME");
    }

    public void actionPerformed(ActionEvent event)
    {
        Object sourceObject = event.getSource();

        if (sourceObject.equals(autoStartCheckBox))
        {
            try
            {
                String workingDir = new File(".").getCanonicalPath();

                String appName = getApplicationName();

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
                                    Resources.getString(
                                        "plugin.generalconfig.ERROR_PERMISSION"),
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

    /**
     * Init auto start checkbox.
     */
    private void initAutoStartCheckBox()
    {
        try
        {
            String appName = getApplicationName();

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
        }
        catch (Exception e)
        {
            logger.error(e);
        }
    }

}
