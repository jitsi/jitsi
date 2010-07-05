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

import net.java.sip.communicator.plugin.generalconfig.autoaway.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import com.izforge.izpack.util.os.*;

import org.osgi.framework.*;

/**
 * The general configuration form.
 *
 * @author Yana Stamcheva
 */
public class GeneralConfigurationPanel
    extends TransparentPanel
{
    /**
     * The logger.
     */
    private final Logger logger
        = Logger.getLogger(GeneralConfigurationPanel.class);

    /**
     * Creates the general configuration panel.
     */
    public GeneralConfigurationPanel()
    {
        super(new BorderLayout());

        TransparentPanel mainPanel = new TransparentPanel();

        BoxLayout boxLayout = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);
        mainPanel.setLayout(boxLayout);
        this.add(mainPanel, BorderLayout.NORTH);

        Component autoStartCheckBox = createAutoStartCheckBox();
        if (autoStartCheckBox != null)
        {
            mainPanel.add(autoStartCheckBox);
            mainPanel.add(new JSeparator());
            mainPanel.add(Box.createVerticalStrut(10));
        }

        mainPanel.add(createMessageConfigPanel());
        mainPanel.add(new JSeparator());

        mainPanel.add(new AutoAwayConfigurationPanel());
        mainPanel.add(new JSeparator());
        mainPanel.add(Box.createVerticalStrut(10));

        Component notifConfigPanel = createNotificationConfigPanel();
        if (notifConfigPanel != null)
        {
            mainPanel.add(notifConfigPanel);
            mainPanel.add(Box.createVerticalStrut(4));
            mainPanel.add(new JSeparator());
            mainPanel.add(Box.createVerticalStrut(10));
        }

        mainPanel.add(createLocaleConfigPanel());
        mainPanel.add(Box.createVerticalStrut(4));
        mainPanel.add(new JSeparator());
        mainPanel.add(Box.createVerticalStrut(10));

        mainPanel.add(createSipConfigPanel());
        mainPanel.add(Box.createVerticalStrut(10));

        Component updateCheckBox = createUpdateCheckPanel();
        if (updateCheckBox != null)
        {
            mainPanel.add(new JSeparator());
            mainPanel.add(updateCheckBox);
            mainPanel.add(Box.createVerticalStrut(10));
        }
    }

    /**
     * Returns the application name.
     * @return the application name
     */
    private String getApplicationName()
    {
        return Resources.getSettingsString("service.gui.APPLICATION_NAME");
    }

    /**
     * Initializes the auto start checkbox.
     * @return the created auto start check box
     */
    private Component createAutoStartCheckBox()
    {
        if (!OSUtils.IS_WINDOWS)
            return null;

        final JCheckBox autoStartCheckBox = new SIPCommCheckBox();

        autoStartCheckBox.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);

        autoStartCheckBox.setText(
            Resources.getString(
                "plugin.generalconfig.AUTO_START",
                new String[]{getApplicationName()}));

        autoStartCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    setAutostart(autoStartCheckBox.isSelected());
                }
                catch (Exception ex)
                {
                    logger.error("Cannot create/delete startup shortcut", ex);
                }
            }
        });

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

        return autoStartCheckBox;
    }

    /**
     * Creates the message configuration panel.
     * @return the created panel
     */
    private Component createMessageConfigPanel()
    {
        JPanel messagePanel = new TransparentPanel(new BorderLayout());
        Component messageLabel
            = GeneralConfigPluginActivator.createConfigSectionComponent(
                Resources.getString("service.gui.MESSAGE"));

        JPanel configPanel = new TransparentPanel();
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));

        configPanel.add(createGroupMessagesCheckbox());
        configPanel.add(Box.createVerticalStrut(10));

        configPanel.add(createHistoryPanel());
        configPanel.add(Box.createVerticalStrut(10));

        configPanel.add(createSendMessagePanel());
        configPanel.add(Box.createVerticalStrut(10));

        configPanel.add(createTypingNitificationsCheckBox());
        configPanel.add(Box.createVerticalStrut(10));

        configPanel.add(createBringToFrontCheckBox());
        configPanel.add(Box.createVerticalStrut(10));

        messagePanel.add(messageLabel, BorderLayout.WEST);
        messagePanel.add(configPanel);

        return messagePanel;
    }
    /**
     * Initializes the group messages check box.
     * @return the created check box
     */
    private Component createGroupMessagesCheckbox()
    {
        final JCheckBox groupMessagesCheckBox = new SIPCommCheckBox();
        groupMessagesCheckBox.setText(
            Resources.getString(
                "plugin.generalconfig.GROUP_CHAT_MESSAGES"));

        groupMessagesCheckBox.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);
        groupMessagesCheckBox.setSelected(
            ConfigurationManager.isMultiChatWindowEnabled());

        groupMessagesCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ConfigurationManager.setMultiChatWindowEnabled(
                    groupMessagesCheckBox.isSelected());
            }
        });

        return groupMessagesCheckBox;
    }

    /**
     * Initializes the history panel.
     * @return the created history panel
     */
    private Component createHistoryPanel()
    {
        JPanel logHistoryPanel = new TransparentPanel();

        logHistoryPanel.setLayout(new BorderLayout());
        logHistoryPanel.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);

        // Log history check box.
        final JCheckBox logHistoryCheckBox = new SIPCommCheckBox();
        logHistoryPanel.add(logHistoryCheckBox, BorderLayout.NORTH);
        logHistoryCheckBox.setText(
            Resources.getString("plugin.generalconfig.LOG_HISTORY"));
        logHistoryCheckBox.setSelected(
            ConfigurationManager.isHistoryLoggingEnabled());

        logHistoryCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ConfigurationManager.setHistoryLoggingEnabled(
                    logHistoryCheckBox.isSelected());
            }
        });

        // Show history check box.
        JPanel showHistoryPanel = new TransparentPanel();
        showHistoryPanel.setBorder(
            BorderFactory.createEmptyBorder(0, 10, 0, 0));
        logHistoryPanel.add(showHistoryPanel, BorderLayout.SOUTH);

        final JCheckBox showHistoryCheckBox = new SIPCommCheckBox();
        showHistoryPanel.add(showHistoryCheckBox);
        showHistoryCheckBox.setText(
            Resources.getString("plugin.generalconfig.SHOW_HISTORY"));
        showHistoryCheckBox.setSelected(
            ConfigurationManager.isHistoryShown());

        showHistoryCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ConfigurationManager.setHistoryShown(
                    showHistoryCheckBox.isSelected());
            }
        });

        // History size.
        SpinnerNumberModel historySizeSpinnerModel =
            new SpinnerNumberModel(0, 0, 140, 1);
        final JSpinner historySizeSpinner = new JSpinner();
        showHistoryPanel.add(historySizeSpinner);
        historySizeSpinner.setModel(historySizeSpinnerModel);
        historySizeSpinner.setValue(
            ConfigurationManager.getChatHistorySize());

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

        showHistoryCheckBox.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                historySizeSpinner.setEnabled(
                    showHistoryCheckBox.isSelected());
            }
        });

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

        JLabel historySizeLabel = new JLabel();
        showHistoryPanel.add(historySizeLabel);
        historySizeLabel.setText(
            Resources.getString("plugin.generalconfig.HISTORY_SIZE"));

        if (!ConfigurationManager.isHistoryLoggingEnabled())
        {
            showHistoryCheckBox.setEnabled(false);
            historySizeSpinner.setEnabled(false);
        }

        if (!ConfigurationManager.isHistoryShown())
        {
            historySizeSpinner.setEnabled(false);
        }

        return logHistoryPanel;
    }

    /**
     * Initializes the send message configuration panel.
     * @return the created message config panel
     */
    private Component createSendMessagePanel()
    {
        TransparentPanel sendMessagePanel
            = new TransparentPanel(new BorderLayout(5, 5));
        sendMessagePanel.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);

        JLabel sendMessageLabel = new JLabel();
        sendMessagePanel.add(sendMessageLabel, BorderLayout.WEST);
        sendMessageLabel.setText(
            Resources.getString("plugin.generalconfig.SEND_MESSAGES_WITH"));

        ComboBoxModel sendMessageComboBoxModel =
            new DefaultComboBoxModel(
                new String[] {
                    ConfigurationManager.ENTER_COMMAND,
                    ConfigurationManager.CTRL_ENTER_COMMAND });
        final JComboBox sendMessageComboBox = new JComboBox();
        sendMessagePanel.add(sendMessageComboBox);
        sendMessageComboBox.setModel(sendMessageComboBoxModel);
        sendMessageComboBox.setSelectedItem(
            ConfigurationManager.getSendMessageCommand());

        sendMessageComboBox.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent arg0)
            {
                ConfigurationManager.setSendMessageCommand(
                    (String)sendMessageComboBox.getSelectedItem());
            }
        });

        return sendMessagePanel;
    }

    /**
     * Initializes typing notifications panel.
     * @return the created check box
     */
    private Component createTypingNitificationsCheckBox()
    {
        final JCheckBox enableTypingNotifiCheckBox = new SIPCommCheckBox();

        enableTypingNotifiCheckBox.setLayout(null);
        enableTypingNotifiCheckBox.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);

        enableTypingNotifiCheckBox.setText(
            Resources.getString("service.gui.ENABLE_TYPING_NOTIFICATIONS"));
        enableTypingNotifiCheckBox.setPreferredSize(
            new Dimension(253, 20));

        enableTypingNotifiCheckBox.setSelected(
            ConfigurationManager.isSendTypingNotifications());

        enableTypingNotifiCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ConfigurationManager.setSendTypingNotifications(
                    enableTypingNotifiCheckBox.isSelected());
            }
        });

        return enableTypingNotifiCheckBox;
    }

    /**
     * Initializes the bring to front check box.
     * @return the created check box
     */
    private Component createBringToFrontCheckBox()
    {
        final JCheckBox bringToFrontCheckBox = new SIPCommCheckBox();

        bringToFrontCheckBox.setText(
            Resources.getString("plugin.generalconfig.BRING_WINDOW_TO_FRONT"));

        bringToFrontCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        bringToFrontCheckBox.setSelected(
            ConfigurationManager.isAutoPopupNewMessage());

        bringToFrontCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ConfigurationManager.setAutoPopupNewMessage(
                    bringToFrontCheckBox.isSelected());
            }
        });

        return bringToFrontCheckBox;
    }

    /**
     * Initializes the notification configuration panel.
     * @return the created panel
     */
    private Component createNotificationConfigPanel()
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

        if (handlerRefs == null)
            return null;

        JPanel notifConfigPanel = new TransparentPanel(new BorderLayout());

        notifConfigPanel.add(
            GeneralConfigPluginActivator.createConfigSectionComponent(
                Resources.getString(
                    "plugin.notificationconfig.POPUP_NOTIF_HANDLER")),
            BorderLayout.WEST);

        final JComboBox notifConfigComboBox = new JComboBox();

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
        notifConfigPanel.add(notifConfigComboBox);

        return notifConfigPanel;
    }

    /**
     * Initializes the local configuration panel.
     * @return the created component
     */
    private Component createLocaleConfigPanel()
    {
        JPanel localeConfigPanel = new TransparentPanel(new BorderLayout());

        localeConfigPanel.add(
            GeneralConfigPluginActivator.createConfigSectionComponent(
                Resources.getString(
                "plugin.generalconfig.DEFAULT_LANGUAGE") + ":"),
                BorderLayout.WEST);

        final JComboBox localesConfigComboBox = new JComboBox();

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
        localeConfigPanel.add(localesConfigComboBox);

        return localeConfigPanel;
    }

    /**
     * Initializes the sip port configuration panel.
     * @return the created panel
     */
    private Component createSipConfigPanel()
    {
        JPanel callConfigPanel = new TransparentPanel(new BorderLayout());

        callConfigPanel.add(
            GeneralConfigPluginActivator.createConfigSectionComponent(
                Resources.getString("service.gui.CALL") + ":"),
            BorderLayout.WEST);

        TransparentPanel sipClientPortConfigPanel = new TransparentPanel();
        sipClientPortConfigPanel.setLayout(new BorderLayout(10, 10));
        sipClientPortConfigPanel.setPreferredSize(new Dimension(250, 72));

        callConfigPanel.add(sipClientPortConfigPanel);

        TransparentPanel labelPanel
            = new TransparentPanel(new GridLayout(0, 1, 2, 2));
        TransparentPanel valuePanel
            = new TransparentPanel(new GridLayout(0, 1, 2, 2));

        sipClientPortConfigPanel.add(labelPanel,
            BorderLayout.WEST);
        sipClientPortConfigPanel.add(valuePanel,
            BorderLayout.CENTER);

        labelPanel.add(new JLabel(
            Resources.getString(
                "plugin.generalconfig.SIP_CLIENT_PORT")));
        labelPanel.add(new JLabel(
            Resources.getString(
                "plugin.generalconfig.SIP_CLIENT_SECURE_PORT")));

        TransparentPanel emptyPanel = new TransparentPanel();
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

        return callConfigPanel;
    }

    /**
     * Initializes the update check panel.
     * @return the created component
     */
    public Component createUpdateCheckPanel()
    {
        if(OSUtils.IS_MAC)// if we are not running mac
            return null;

        JCheckBox updateCheckBox = new SIPCommCheckBox();

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

        updateCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        updateCheckBox.setSelected(
            GeneralConfigPluginActivator.getConfigurationService().getBoolean((
                "net.java.sip.communicator.plugin.updatechecker.ENABLED"), true));

        return updateCheckBox;
    }

    /**
     * Sets the auto start.
     * @param isAutoStart indicates if the auto start property is set to true or
     * false
     * @throws Exception if something goes wrong when obtaining the canonical
     * path or when creating or saving the shortcut
     */
    private void setAutostart(boolean isAutoStart)
        throws Exception
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

        if(isAutoStart)
        {
            if(!new File(f1).exists() &&
               !new File(f2).exists())
                shortcut.save();
        }
        else
        {
            boolean isFileDeleted = false;
            try
            {
                isFileDeleted = new File(f1).delete();
            }
            catch (Exception ex)
            {
                if (logger.isTraceEnabled())
                    logger.trace("Unable to delete file. ", ex);
            }

            try
            {
                new File(f2).delete();
            }
            catch (Exception ex)
            {
                if(!isFileDeleted)
                    GeneralConfigPluginActivator.getUIService().
                        getPopupDialog().showMessagePopupDialog(
                            ex.getMessage(),
                            Resources.getString(
                                "plugin.generalconfig.ERROR_PERMISSION"),
                            PopupDialog.ERROR_MESSAGE);
                // cannot delete no permissions
            }
        }
    } 
}
