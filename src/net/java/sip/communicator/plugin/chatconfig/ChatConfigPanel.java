/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.chatconfig;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.util.swing.*;

/**
 * 
 * @author Yana Stamcheva
 */
public class ChatConfigPanel
    extends TransparentPanel
{
    public ChatConfigPanel()
    {
        super(new BorderLayout());

        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        TransparentPanel mainPanel = new TransparentPanel();

        BoxLayout boxLayout = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);
        mainPanel.setLayout(boxLayout);
        this.add(mainPanel, BorderLayout.NORTH);

//        mainPanel.add(createGroupMessagesCheckbox());
//        mainPanel.add(Box.createVerticalStrut(10));
//
//        mainPanel.add(createHistoryPanel());
//        mainPanel.add(Box.createVerticalStrut(10));
//
//        mainPanel.add(createSendMessagePanel());
//        mainPanel.add(Box.createVerticalStrut(10));
    }

    /**
     * Initializes the group messages check box.
     * @return the created check box
     */
//    private Component createGroupMessagesCheckbox()
//    {
//        final JCheckBox groupMessagesCheckBox = new SIPCommCheckBox();
//        groupMessagesCheckBox.setText(
//            ChatConfigActivator.getResources().getI18NString(
//                "plugin.generalconfig.GROUP_CHAT_MESSAGES"));
//
//        groupMessagesCheckBox.setSelected(
//            ConfigurationManager.isMultiChatWindowEnabled());
//
//        groupMessagesCheckBox.setAlignmentX(JCheckBox.CENTER_ALIGNMENT);
//        groupMessagesCheckBox.addActionListener(new ActionListener()
//        {
//            public void actionPerformed(ActionEvent e)
//            {
//                ConfigurationManager.setMultiChatWindowEnabled(
//                    groupMessagesCheckBox.isSelected());
//            }
//        });
//
//        return groupMessagesCheckBox;
//    }
//
//    /**
//     * Initializes the history panel.
//     * @return the created history panel
//     */
//    private Component createHistoryPanel()
//    {
//        TransparentPanel logHistoryPanel = new TransparentPanel();
//
//        logHistoryPanel.setLayout(null);
//        logHistoryPanel.setPreferredSize(new Dimension(250, 57));
//        logHistoryPanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
//
//        // Log history check box.
//        final JCheckBox logHistoryCheckBox = new SIPCommCheckBox();
//        logHistoryPanel.add(logHistoryCheckBox);
//        logHistoryCheckBox.setText(
//            ChatConfigActivator.getResources()
//                .getI18NString("plugin.generalconfig.LOG_HISTORY"));
//        logHistoryCheckBox.setBounds(0, 0, 200, 19);
//        logHistoryCheckBox.setSelected(
//            ConfigurationManager.isHistoryLoggingEnabled());
//
//        logHistoryCheckBox.addActionListener(new ActionListener()
//        {
//            public void actionPerformed(ActionEvent e)
//            {
//                ConfigurationManager.setHistoryLoggingEnabled(
//                    logHistoryCheckBox.isSelected());
//            }
//        });
//
//        // Show history check box.
//        final JCheckBox showHistoryCheckBox = new SIPCommCheckBox();
//        logHistoryPanel.add(showHistoryCheckBox);
//        showHistoryCheckBox.setText(
//            ChatConfigActivator.getResources()
//                .getI18NString("plugin.generalconfig.SHOW_HISTORY"));
//        showHistoryCheckBox.setBounds(17, 25, 200, 19);
//        showHistoryCheckBox.setSelected(
//            ConfigurationManager.isHistoryShown());
//
//        showHistoryCheckBox.addActionListener(new ActionListener()
//        {
//            public void actionPerformed(ActionEvent e)
//            {
//                ConfigurationManager.setHistoryShown(
//                    showHistoryCheckBox.isSelected());
//            }
//        });
//
//        // History size.
//        SpinnerNumberModel historySizeSpinnerModel =
//            new SpinnerNumberModel(0, 0, 140, 1);
//        final JSpinner historySizeSpinner = new JSpinner();
//        logHistoryPanel.add(historySizeSpinner);
//        historySizeSpinner.setModel(historySizeSpinnerModel);
//        historySizeSpinner.setBounds(150, 23, 47, 22);
//        historySizeSpinner.setValue(
//            ConfigurationManager.getChatHistorySize());
//
//        logHistoryCheckBox.addChangeListener(new ChangeListener()
//        {
//            public void stateChanged(ChangeEvent e)
//            {
//                showHistoryCheckBox.setEnabled(
//                    logHistoryCheckBox.isSelected());
//                historySizeSpinner.setEnabled(
//                    logHistoryCheckBox.isSelected());
//            }
//        });
//
//        showHistoryCheckBox.addChangeListener(new ChangeListener()
//        {
//            public void stateChanged(ChangeEvent e)
//            {
//                historySizeSpinner.setEnabled(
//                    showHistoryCheckBox.isSelected());
//            }
//        });
//
//        historySizeSpinnerModel.addChangeListener(
//            new ChangeListener()
//            {
//                public void stateChanged(ChangeEvent e)
//                {
//                    ConfigurationManager.setChatHistorySize(
//                        ((Integer) historySizeSpinner
//                            .getValue()).intValue());
//                }
//            });
//
//        JLabel historySizeLabel = new JLabel();
//        logHistoryPanel.add(historySizeLabel);
//        historySizeLabel.setText(
//            ChatConfigActivator.getResources()
//                .getI18NString("plugin.generalconfig.HISTORY_SIZE"));
//        historySizeLabel.setBounds(205, 27, 220, 15);
//
//        if (!ConfigurationManager.isHistoryLoggingEnabled())
//        {
//            showHistoryCheckBox.setEnabled(false);
//            historySizeSpinner.setEnabled(false);
//        }
//
//        if (!ConfigurationManager.isHistoryShown())
//        {
//            historySizeSpinner.setEnabled(false);
//        }
//
//        return logHistoryPanel;
//    }
//
//    /**
//     * Initializes the send message configuration panel.
//     * @return the created message config panel
//     */
//    private Component createSendMessagePanel()
//    {
//        TransparentPanel sendMessagePanel
//            = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));
//
//        JLabel sendMessageLabel = new JLabel();
//        sendMessagePanel.add(sendMessageLabel);
//        sendMessageLabel.setText(
//            ChatConfigActivator.getResources()
//                .getI18NString("plugin.generalconfig.SEND_MESSAGES_WITH"));
//
//        ComboBoxModel sendMessageComboBoxModel =
//            new DefaultComboBoxModel(
//                new String[] {
//                    ConfigurationManager.ENTER_COMMAND,
//                    ConfigurationManager.CTRL_ENTER_COMMAND });
//        final JComboBox sendMessageComboBox = new JComboBox();
//        sendMessagePanel.add(sendMessageComboBox);
//        sendMessageComboBox.setModel(sendMessageComboBoxModel);
//        sendMessageComboBox.setSelectedItem(
//            ConfigurationManager.getSendMessageCommand());
//
//        sendMessageComboBox.addItemListener(new ItemListener()
//        {
//            public void itemStateChanged(ItemEvent arg0)
//            {
//                ConfigurationManager.setSendMessageCommand(
//                    (String)sendMessageComboBox.getSelectedItem());
//            }
//        });
//
//        return sendMessagePanel;
//    }
}
