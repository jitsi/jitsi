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
package net.java.sip.communicator.plugin.generalconfig;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.generalconfig.autoaway.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.util.*;
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
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by this <tt>GeneralConfigurationPanel</tt> for
     * logging output.
     */
    private final Logger logger
        = Logger.getLogger(GeneralConfigurationPanel.class);

     /**
      * Indicates if the Startup configuration panel should be disabled, i.e.
      * not visible to the user.
      */
    private static final String STARTUP_CONFIG_DISABLED_PROP
        =
        "net.java.sip.communicator.plugin.generalconfig.startupconfig.DISABLED";

     /**
      * Indicates if the Message configuration panel should be disabled, i.e.
      * not visible to the user.
      */
    private static final String MESSAGE_CONFIG_DISABLED_PROP
        =
        "net.java.sip.communicator.plugin.generalconfig.messageconfig.DISABLED";

     /**
      * Indicates if the AutoAway configuration panel should be disabled, i.e.
      * not visible to the user.
      */
    private static final String AUTO_AWAY_CONFIG_DISABLED_PROP
        =
        "net.java.sip.communicator.plugin.generalconfig.autoawayconfig.DISABLED";

     /**
      * Indicates if the Notification configuration panel should be disabled,
      * i.e.  not visible to the user.
      */
    private static final String NOTIFICATION_CONFIG_DISABLED_PROP
        =
        "net.java.sip.communicator.plugin.generalconfig.notificationconfig.DISABLED";

     /**
      * Indicates if the Locale configuration panel should be disabled, i.e.
      * not visible to the user.
      */
    private static final String LOCALE_CONFIG_DISABLED_PROP
        =
        "net.java.sip.communicator.plugin.generalconfig.localeconfig.DISABLED";

     /**
      * Indicates if the Call configuration panel should be disabled, i.e.
      * not visible to the user.
      */
    private static final String CALL_CONFIG_DISABLED_PROP
        =
        "net.java.sip.communicator.plugin.generalconfig.callconfig.DISABLED";

    /**
     * Creates the general configuration panel.
     */
    public GeneralConfigurationPanel()
    {
        super(new BorderLayout());

        TransparentPanel mainPanel = new TransparentPanel();
        BoxLayout boxLayout = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);
        mainPanel.setLayout(boxLayout);
        mainPanel.setBorder(new EmptyBorder(0, 0, 0, 10));

        final JScrollPane scroller = new JScrollPane(mainPanel);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setPreferredSize(new Dimension(500, 420));
        scroller.setBorder(new EmptyBorder(0, 0, 0, 0));
        this.add(scroller, BorderLayout.CENTER);

        if(!GeneralConfigPluginActivator.getConfigurationService()
                .getBoolean(STARTUP_CONFIG_DISABLED_PROP, false))
        {
            Component startupConfigPanel = createStartupConfigPanel();
            if (startupConfigPanel != null)
            {
                mainPanel.add(startupConfigPanel);
                mainPanel.add(Box.createVerticalStrut(10));
            }
        }

        if(!GeneralConfigPluginActivator.getConfigurationService()
                .getBoolean(MESSAGE_CONFIG_DISABLED_PROP, false))
        {
            mainPanel.add(createMessageConfigPanel());
            mainPanel.add(Box.createVerticalStrut(10));
        }

        if(!GeneralConfigPluginActivator.getConfigurationService()
                .getBoolean(AUTO_AWAY_CONFIG_DISABLED_PROP, false))
        {
            mainPanel.add(new AutoAwayConfigurationPanel());
            mainPanel.add(Box.createVerticalStrut(10));
        }

        if(!GeneralConfigPluginActivator.getConfigurationService()
                .getBoolean(NOTIFICATION_CONFIG_DISABLED_PROP, false))
        {
            Component notifConfigPanel = createNotificationConfigPanel();
            if (notifConfigPanel != null)
            {
                mainPanel.add(notifConfigPanel);
                mainPanel.add(Box.createVerticalStrut(10));
            }
        }

        if(!GeneralConfigPluginActivator.getConfigurationService()
                .getBoolean(LOCALE_CONFIG_DISABLED_PROP, false))
        {
            mainPanel.add(createLocaleConfigPanel());
            mainPanel.add(Box.createVerticalStrut(10));
        }

        if(!GeneralConfigPluginActivator.getConfigurationService()
                .getBoolean(CALL_CONFIG_DISABLED_PROP, false))
        {
            mainPanel.add(createCallConfigPanel());
            mainPanel.add(Box.createVerticalStrut(10));
        }

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                scroller.getVerticalScrollBar().setValue(0);
                scroller.revalidate();
                scroller.repaint();
            }
        });
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
            autoStartCheckBox.setSelected(
                WindowsStartup.isStartupEnabled(getApplicationName()));
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
        JPanel configPanel
            = GeneralConfigPluginActivator.createConfigSectionComponent(
                Resources.getString("service.gui.MESSAGE"));

        configPanel.add(createGroupMessagesCheckbox());
        configPanel.add(createHistoryPanel());
        configPanel.add(createSendMessagePanel());
        configPanel.add(createTypingNitificationsCheckBox());
        configPanel.add(createBringToFrontCheckBox());
        configPanel.add(createChatAlertsOnMessageCheckbox());
        configPanel.add(createMultichatCheckbox());
        configPanel.add(createRecentMessagesCheckbox());

        return configPanel;
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
            ConfigurationUtils.isMultiChatWindowEnabled());

        groupMessagesCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ConfigurationUtils.setMultiChatWindowEnabled(
                    groupMessagesCheckBox.isSelected());
            }
        });

        return groupMessagesCheckBox;
    }

    /**
     * Initializes the window alert on message check box.
     * @return the created check box
     */
    private Component createChatAlertsOnMessageCheckbox()
    {
        final JCheckBox chatAlertOnMessageCheckBox = new SIPCommCheckBox();
        chatAlertOnMessageCheckBox.setText(
            Resources.getString(
                "plugin.generalconfig.CHATALERTS_ON_MESSAGE"));

        chatAlertOnMessageCheckBox.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);
        chatAlertOnMessageCheckBox.setSelected(
            ConfigurationUtils.isAlerterEnabled());

        chatAlertOnMessageCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ConfigurationUtils.setAlerterEnabled(
                    chatAlertOnMessageCheckBox.isSelected());
            }
        });

        return chatAlertOnMessageCheckBox;
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
            ConfigurationUtils.isLeaveChatRoomOnWindowCloseEnabled());

        leaveChatroomCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ConfigurationUtils.setLeaveChatRoomOnWindowClose(
                    leaveChatroomCheckBox.isSelected());
            }
        });

        return leaveChatroomCheckBox;
    }

    /**
     * Initializes the recent messages check box.
     * @return the created check box
     */
    private Component createRecentMessagesCheckbox()
    {
        final JCheckBox recentMessagesCheckBox = new SIPCommCheckBox();
        recentMessagesCheckBox.setText(
            Resources.getString(
                "plugin.generalconfig.SHOW_RECENT_MESSAGES"));

        recentMessagesCheckBox.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);
        recentMessagesCheckBox.setSelected(
            ConfigurationUtils.isRecentMessagesShown());

        recentMessagesCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ConfigurationUtils.setRecentMessagesShown(
                    recentMessagesCheckBox.isSelected());
            }
        });

        return recentMessagesCheckBox;
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
        final MessageHistoryService mhs
            = GeneralConfigPluginActivator.getMessageHistoryService();
        logHistoryCheckBox.setText(
            Resources.getString("plugin.generalconfig.LOG_HISTORY"));
        logHistoryCheckBox.setSelected(mhs.isHistoryLoggingEnabled());

        logHistoryCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                mhs.setHistoryLoggingEnabled(logHistoryCheckBox.isSelected());
            }
        });
        GeneralConfigPluginActivator.getConfigurationService()
            .addPropertyChangeListener(
                MessageHistoryService.PNAME_IS_MESSAGE_HISTORY_ENABLED,
                new PropertyChangeListener()
                {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt)
                    {
                        logHistoryCheckBox.setSelected(
                            mhs.isHistoryLoggingEnabled());
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
            ConfigurationUtils.isHistoryShown());

        showHistoryCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ConfigurationUtils.setHistoryShown(
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
            ConfigurationUtils.getChatHistorySize());

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
                    ConfigurationUtils.setChatHistorySize(
                        ((Integer) historySizeSpinner
                            .getValue()).intValue());
                }
            });

        JLabel historySizeLabel = new JLabel();
        showHistoryPanel.add(historySizeLabel);
        historySizeLabel.setText(
            Resources.getString("plugin.generalconfig.HISTORY_SIZE"));

        if (!mhs.isHistoryLoggingEnabled())
        {
            showHistoryCheckBox.setEnabled(false);
            historySizeSpinner.setEnabled(false);
        }

        if (!ConfigurationUtils.isHistoryShown())
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

        ComboBoxModel sendMessageComboBoxModel
            = new DefaultComboBoxModel(
                    new String[]
                            {
                                ConfigurationUtils.ENTER_COMMAND,
                                ConfigurationUtils.CTRL_ENTER_COMMAND
                            });
        final JComboBox sendMessageComboBox = new JComboBox();
        sendMessagePanel.add(sendMessageComboBox);
        sendMessageComboBox.setModel(sendMessageComboBoxModel);
        sendMessageComboBox.setSelectedItem(
            ConfigurationUtils.getSendMessageCommand());

        sendMessageComboBox.addItemListener(
                new ItemListener()
                {
                    public void itemStateChanged(ItemEvent ev)
                    {
                        ConfigurationUtils.setSendMessageCommand(
                                (String) sendMessageComboBox.getSelectedItem());
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
            ConfigurationUtils.isSendTypingNotifications());

        enableTypingNotifiCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ConfigurationUtils.setSendTypingNotifications(
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
            ConfigurationUtils.isAutoPopupNewMessage());

        bringToFrontCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ConfigurationUtils.setAutoPopupNewMessage(
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

        JPanel notifConfigPanel = GeneralConfigPluginActivator.
            createConfigSectionComponent(
                Resources.getString(
                    "plugin.notificationconfig.POPUP_NOTIF_HANDLER"));

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
                    ConfigurationUtils.setPopupHandlerConfig(null);
                    GeneralConfigPluginActivator.getSystrayService()
                        .selectBestPopupMessageHandler();

                } else
                {
                    PopupMessageHandler handler =
                        (PopupMessageHandler)
                        notifConfigComboBox.getSelectedItem();

                    ConfigurationUtils.setPopupHandlerConfig(
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
     * Model for the language combobox.
     */
    private static class LocaleItem
        implements Comparable<LocaleItem>
    {
        private Locale locale;
        private int translated;

        public LocaleItem(Locale locale, int translated)
        {
            this.locale = locale;
            this.translated = translated;
        }

        @Override
        public int compareTo(LocaleItem o)
        {
            return locale.getDisplayLanguage().compareTo(
                o.locale.getDisplayLanguage());
        }
    }

    /**
     * 3-column layout to show the language in the current locale, the
     * locale of the language itself and the percentage of translation.
     */
    @SuppressWarnings("serial")
    private static class LanguageDropDownRenderer
        extends JPanel
        implements ListCellRenderer
    {
        JLabel[] labels = new JLabel[3];

        public LanguageDropDownRenderer()
        {
            setLayout(new GridLayout(0, 3));
            for (int i = 0; i < labels.length; i++)
            {
                labels[i] = new JLabel();
                add(labels[i]);
            }

            labels[2].setHorizontalAlignment(JLabel.RIGHT);
        }

        public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
        {
            LocaleItem lm = (LocaleItem)value;
            if (value != null)
            {
                labels[0].setText(lm.locale.getDisplayName());
                labels[1].setText(lm.locale.getDisplayName(lm.locale));
                labels[2].setText(Resources.getString(
                    "plugin.generalconfig.DEFAULT_LANGUAGE_TRANSLATED",
                    new String[]{
                        Integer.toString(lm.translated)
                    }));
            }
            else
            {
                labels[0].setText("");
                labels[1].setText("");
                labels[2].setText("");
            }

            this.setBackground(isSelected
                ? list.getSelectionBackground()
                : list.getBackground());

            return this;
        }
    } 

    /**
     * Initializes the local configuration panel.
     * @return the created component
     */
    private Component createLocaleConfigPanel()
    {
        JPanel localeConfigPanel = GeneralConfigPluginActivator.
            createConfigSectionComponent(
                Resources.getString("plugin.generalconfig.DEFAULT_LANGUAGE"));

        LanguagePack lp = ServiceUtils.getService(
            GeneralConfigPluginActivator.bundleContext,
            LanguagePack.class);
        Map<String, String> defaultRes = lp.getResources(Locale.ENGLISH);

        Locale currentLocale = ConfigurationUtils.getCurrentLanguage();
        LocaleItem currentLocaleItem = null;
        java.util.List<LocaleItem> languages = new ArrayList<LocaleItem>();
        Iterator<Locale> iter = Resources.getResources().getAvailableLocales();
        while (iter.hasNext())
        {
            Locale locale = iter.next();

            // count the number of translated strings
            Set<String> res = lp.getResourceKeys(locale);
            int count = 0;
            for (String key : defaultRes.keySet())
            {
                if (res.contains(key))
                {
                    count++;
                }
            }

            LocaleItem li = new LocaleItem(
                locale,
                count * 100 / defaultRes.size());
            languages.add(li);
            if (locale.equals(currentLocale))
            {
                currentLocaleItem = li;
            }
        }

        Collections.sort(languages);
        final JComboBox localesConfigComboBox = new JComboBox();
        localesConfigComboBox.setRenderer(new LanguageDropDownRenderer());
        for (LocaleItem li : languages)
        {
            localesConfigComboBox.addItem(li);
        }

        localesConfigComboBox.setSelectedItem(currentLocaleItem);
        localesConfigComboBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                GeneralConfigPluginActivator.getUIService().getPopupDialog().
                    showMessagePopupDialog(Resources.getString(
                    "plugin.generalconfig.DEFAULT_LANGUAGE_RESTART_WARN"));

                LocaleItem li =
                        (LocaleItem)localesConfigComboBox.getSelectedItem();
                ConfigurationUtils.setLanguage(li.locale);
            }
        });
        localeConfigPanel.add(localesConfigComboBox);

        String label = "* " +
                Resources.getString(
                        "plugin.generalconfig.DEFAULT_LANGUAGE_RESTART_WARN");
        JLabel warnLabel = new JLabel(label);
        warnLabel.setToolTipText(label);
        warnLabel.setForeground(Color.GRAY);
        warnLabel.setFont(warnLabel.getFont().deriveFont(8));
        warnLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        warnLabel.setHorizontalAlignment(JLabel.RIGHT);
        localeConfigPanel.add(warnLabel);

        return localeConfigPanel;
    }

    /**
     * Creates the call configuration panel.
     *
     * @return the call configuration panel
     */
    private Component createCallConfigPanel()
    {
        JPanel callConfigPanel = GeneralConfigPluginActivator.
            createConfigSectionComponent(
                Resources.getString("service.gui.CALL"));

        callConfigPanel.add(createNormalizeNumberCheckBox());
        callConfigPanel.add(createAcceptPhoneNumberWithAlphaCharCheckBox());

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
            ConfigurationUtils.isNormalizePhoneNumber());

        formatPhoneNumber.setAlignmentY(Component.TOP_ALIGNMENT);

        formatPhoneNumber.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ConfigurationUtils.setNormalizePhoneNumber(
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
     * Creates the accept phone number with alphabetical character check box.
     *
     * @return the created component
     */
    private Component createAcceptPhoneNumberWithAlphaCharCheckBox()
    {
        JPanel checkBoxPanel = new TransparentPanel(new BorderLayout());

        // Checkbox to accept string with alphabetical characters as potential
        // phone numbers.
        SIPCommCheckBox acceptPhoneNumberWithAlphaChars
            = new SIPCommCheckBox("",
                ConfigurationUtils.acceptPhoneNumberWithAlphaChars());

        acceptPhoneNumberWithAlphaChars.setAlignmentY(Component.TOP_ALIGNMENT);

        acceptPhoneNumberWithAlphaChars.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ConfigurationUtils.setAcceptPhoneNumberWithAlphaChars(
                        ((JCheckBox)e.getSource()).isSelected());
            }
        });

        StyledHTMLEditorPane acceptPhoneNumberWithAlphaCharsTextLabel
                = new StyledHTMLEditorPane();

        acceptPhoneNumberWithAlphaCharsTextLabel.setContentType("text/html");
        acceptPhoneNumberWithAlphaCharsTextLabel.appendToEnd(
                "<html>"
                + GeneralConfigPluginActivator.getResources().getI18NString(
                    "plugin.generalconfig.ACCEPT_PHONE_NUMBER_WITH_ALPHA_CHARS")
                + "</html>");

        acceptPhoneNumberWithAlphaCharsTextLabel.setBorder(
                BorderFactory.createEmptyBorder(3, 0, 0, 0));
        acceptPhoneNumberWithAlphaCharsTextLabel.setOpaque(false);
        acceptPhoneNumberWithAlphaCharsTextLabel.setEditable(false);

        // The example of changing letters to numbers in a phone number.
        String label = "* " + Resources.getString(
            "plugin.generalconfig.ACCEPT_PHONE_NUMBER_WITH_ALPHA_CHARS_EXAMPLE");
        JLabel exampleLabel = new JLabel(label);
        exampleLabel.setToolTipText(label);
        exampleLabel.setForeground(Color.GRAY);
        exampleLabel.setFont(exampleLabel.getFont().deriveFont(8));
        exampleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        exampleLabel.setHorizontalAlignment(JLabel.LEFT);

        // Adds the components to the current panel.
        checkBoxPanel.add(acceptPhoneNumberWithAlphaChars, BorderLayout.WEST);
        checkBoxPanel.add(
                acceptPhoneNumberWithAlphaCharsTextLabel,
                BorderLayout.CENTER);
        checkBoxPanel.add(
                exampleLabel,
                BorderLayout.SOUTH);

        return checkBoxPanel;
    }

    /**
     * Initializes the startup config panel.
     * @return the created component
     */
    public Component createStartupConfigPanel()
    {
        if (!OSUtils.IS_WINDOWS)
            return null;

        JPanel updateConfigPanel = GeneralConfigPluginActivator.
            createConfigSectionComponent(
                Resources.getString("plugin.generalconfig.STARTUP_CONFIG"));

        updateConfigPanel.add(createAutoStartCheckBox());
        updateConfigPanel.add(createUpdateCheckBox());
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
