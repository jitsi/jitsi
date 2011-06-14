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
     * The <tt>Logger</tt> used by this <tt>GeneralConfigurationPanel</tt> for
     * logging output.
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

        Component startupConfigPanel = createStartupConfigPanel();
        if (startupConfigPanel != null)
        {
            mainPanel.add(startupConfigPanel);
            mainPanel.add(Box.createVerticalStrut(10));
            mainPanel.add(new JSeparator());
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

        mainPanel.add(createCallConfigPanel());
        mainPanel.add(Box.createVerticalStrut(10));
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
     * Initializes the auto start checkbox. Used only on windows.
     * @return the created auto start check box
     */
    private Component createAutoStartCheckBox()
    {
        final JCheckBox autoStartCheckBox = new SIPCommCheckBox();

        autoStartCheckBox.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);

        String label = Resources.getString(
                "plugin.generalconfig.AUTO_START",
                new String[]{getApplicationName()});
        autoStartCheckBox.setText(label);
        autoStartCheckBox.setToolTipText(label);
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
            if(WindowsStartup.isStartupEnabled(getApplicationName()))
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

        configPanel.add(createMultichatCheckbox());
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
     * Initializes the group messages check box.
     * @return the created check box
     */
    private Component createMultichatCheckbox()
    {
        final JCheckBox leaveChatroomCheckBox = new SIPCommCheckBox();
        leaveChatroomCheckBox.setText(
            Resources.getString(
                "plugin.generalconfig.LEAVE_CHATROOM_ON_WINDOW_CLOSE"));

        leaveChatroomCheckBox.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);
        leaveChatroomCheckBox.setSelected(
            ConfigurationManager.isLeaveChatRoomOnWindowCloseEnabled());

        leaveChatroomCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ConfigurationManager.setLeaveChatRoomOnWindowClose(
                    leaveChatroomCheckBox.isSelected());
            }
        });

        return leaveChatroomCheckBox;
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
        localeConfigPanel.add(localesConfigComboBox, BorderLayout.CENTER);

        String label = "* " +
                Resources.getString(
                        "plugin.generalconfig.DEFAULT_LANGUAGE_RESTART_WARN");
        JLabel warnLabel = new JLabel(label);
        warnLabel.setToolTipText(label);
        warnLabel.setForeground(Color.GRAY);
        warnLabel.setFont(warnLabel.getFont().deriveFont(8));
        warnLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        warnLabel.setHorizontalAlignment(JLabel.RIGHT);
        localeConfigPanel.add(warnLabel, BorderLayout.SOUTH);

        return localeConfigPanel;
    }

    /**
     * Creates the call configuration panel.
     *
     * @return the call configuration panel
     */
    private Component createCallConfigPanel()
    {
        JPanel callConfigPanel = new TransparentPanel(new BorderLayout());

        callConfigPanel.add(
            GeneralConfigPluginActivator.createConfigSectionComponent(
                Resources.getString("service.gui.CALL") + ":"),
            BorderLayout.WEST);

        callConfigPanel.add(createNormalizeNumberCheckBox());

        return callConfigPanel;
    }

    /**
     * Creates the normalized phone number check box.
     *
     * @return the created component
     */
    private Component createNormalizeNumberCheckBox()
    {
        JPanel checkBoxPanel = new TransparentPanel(new BorderLayout());

        SIPCommCheckBox formatPhoneNumber = new SIPCommCheckBox("",
            ConfigurationManager.isNormalizePhoneNumber());

        formatPhoneNumber.setAlignmentY(Component.TOP_ALIGNMENT);

        formatPhoneNumber.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ConfigurationManager.setNormalizePhoneNumber(
                        ((JCheckBox)e.getSource()).isSelected());
            }
        });

        StyledHTMLEditorPane checkBoxTextLabel = new StyledHTMLEditorPane();

        checkBoxTextLabel.setContentType("text/html");
        checkBoxTextLabel.appendToEnd(
            "<html>" + GeneralConfigPluginActivator.getResources().getI18NString(
                "plugin.generalconfig.REMOVE_SPECIAL_PHONE_SYMBOLS") + "</html>");

        checkBoxTextLabel.setBorder(
            BorderFactory.createEmptyBorder(3, 0, 0, 0));
        checkBoxTextLabel.setOpaque(false);
        checkBoxTextLabel.setEditable(false);

        checkBoxPanel.add(formatPhoneNumber, BorderLayout.WEST);
        checkBoxPanel.add(checkBoxTextLabel, BorderLayout.CENTER);

        return checkBoxPanel;
    }
    /**
     * Initializes the startup config panel.
     * @return the created component
     */
    public Component createStartupConfigPanel()
    {
        Component updateCheckBox = null;
        Component autoStartCheckBox = null;

        if (OSUtils.IS_WINDOWS)
        {
            autoStartCheckBox = createAutoStartCheckBox();
            updateCheckBox = createUpdateCheckBox();
        }

        JPanel updateConfigPanel = null;

        if ((updateCheckBox != null) || (autoStartCheckBox != null))
        {
            updateConfigPanel = new TransparentPanel(new BorderLayout());
            updateConfigPanel.add(
                    GeneralConfigPluginActivator.createConfigSectionComponent(
                            Resources.getString(
                                    "plugin.generalconfig.STARTUP_CONFIG")
                                + ":"),
                    BorderLayout.WEST);

            if ((updateCheckBox != null) && (autoStartCheckBox != null))
            {
                JPanel checkBoxPanel
                    = new TransparentPanel(new GridLayout(0, 1));

                checkBoxPanel.add(autoStartCheckBox);
                checkBoxPanel.add(updateCheckBox);
                updateConfigPanel.add(checkBoxPanel);
            }
            else if (updateCheckBox != null)
                updateConfigPanel.add(updateCheckBox);
            else if (autoStartCheckBox != null)
                updateConfigPanel.add(autoStartCheckBox);
        }
        return updateConfigPanel;
    }

    /**
     * Initializes the update check panel.
     * @return the created component
     */
    public Component createUpdateCheckBox()
    {
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

        WindowsStartup.setAutostart(
                getApplicationName(), workingDir, isAutoStart);
    } 
}
