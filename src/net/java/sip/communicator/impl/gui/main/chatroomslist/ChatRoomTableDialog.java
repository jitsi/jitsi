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
package net.java.sip.communicator.impl.gui.main.chatroomslist;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;


/**
 * The dialog containing a list of all chat rooms of the user and
 * also interface for create a new chat room, join a chat room, search all
 * chat rooms, etc.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Damian Minkov
 */
public class ChatRoomTableDialog
    extends SIPCommDialog
    implements ActionListener
{
    /**
     * Whether we should remove the room if it fails to join when adding it.
     */
    private static final String REMOVE_ROOM_ON_FIRST_JOIN_FAILED
         = "net.java.sip.communicator.impl.gui.main.chatroomslist." +
                "REMOVE_ROOM_ON_FIRST_JOIN_FAILED";

    /**
     * The global/shared <code>ChatRoomTableDialog</code> currently showing.
     */
    private static ChatRoomTableDialog chatRoomTableDialog;

    /**
     * A JComboBox which will allow to select an account for joining a room.
     */
    private JComboBox providersCombo;

    /**
     * The ok button.
     */
    private final JButton okButton
        = new JButton(
                GuiActivator.getResources().getI18NString("service.gui.JOIN"));

    /**
     * The cancel button.
     */
    private final JButton cancelButton
        = new JButton(
                GuiActivator.getResources().getI18NString(
                        "service.gui.CANCEL"));
    /**
     * The list button. This button lists the existing chat rooms on the server.
     */
    private final JButton listButton
        = new JButton(GuiActivator.getResources().getI18NString(
                        "service.gui.LIST"));

    /**
     * The editor for the chat room name.
     */
    private JTextField chatRoomNameField = null;

    /**
     * Label that hides and shows the more fields panel on click.
     */
    private JLabel cmdExpandMoreFields;

    /**
     * Panel that holds the subject field and the nickname field.
     */
    private JPanel moreFieldsPannel = new JPanel(new BorderLayout(5, 5));

    /**
     * The field for the nickname.
     */
    private JTextField nicknameField = new JTextField();

    /**
     * Text field for the subject.
     */
    private SIPCommTextField subject = new SIPCommTextField(DesktopUtilActivator
        .getResources().getI18NString("service.gui.SUBJECT"));

    /**
     * The dialog for the existing chat rooms on the server.
     */
    private ServerChatRoomsChoiceDialog serverChatRoomsChoiceDialog = null;

    /**
     * The <tt>ChatRoomList.ChatRoomProviderWrapperListener</tt> instance which
     * has been registered with {@link #chatRoomList} and which is to be
     * unregistered when this instance is disposed in order to prevent this
     * instance from leaking.
     */
    private final ChatRoomProviderWrapperListener
        chatRoomProviderWrapperListener
            = new ChatRoomProviderWrapperListener()
            {
                public void chatRoomProviderWrapperAdded(
                    ChatRoomProviderWrapper provider)
                {
                    providersCombo.addItem(provider);
                }

                public void chatRoomProviderWrapperRemoved(
                    ChatRoomProviderWrapper provider)
                {
                    providersCombo.removeItem(provider);
                }
            };

    /**
     * Shows a <code>ChatRoomTableDialog</code> creating it first if necessary.
     * The shown instance is shared in order to prevent displaying multiple
     * instances of one and the same <code>ChatRoomTableDialog</code>.
     */
    public static void showChatRoomTableDialog()
    {
        if (chatRoomTableDialog == null)
        {
            chatRoomTableDialog
                = new ChatRoomTableDialog(
                        GuiActivator.getUIService().getMainFrame());

            /*
             * When the global/shared ChatRoomTableDialog closes, don't keep a
             * reference to it and let it be garbage-collected.
             */
            chatRoomTableDialog.addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosed(WindowEvent e)
                {
                    if (chatRoomTableDialog == e.getWindow())
                        chatRoomTableDialog = null;
                }
            });
        }
        chatRoomTableDialog.setVisible(true);
        chatRoomTableDialog.pack();
    }

    /**
     * Creates an instance of <tt>MyChatRoomsDialog</tt> by specifying the
     * parent window.
     *
     * @param parentWindow the parent window of this dialog
     */
    public ChatRoomTableDialog(MainFrame parentWindow)
    {
        super(parentWindow);

        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        this.setTitle(GuiActivator.getResources()
            .getI18NString("service.gui.MY_CHAT_ROOMS_TITLE"));

        this.init();
    }

    /**
     * Initializes this chat room dialog.
     */
    private void init()
    {
        this.getContentPane().setLayout(new BorderLayout(5,5));

        JPanel northPanel = new TransparentPanel(new BorderLayout(5, 5));
        northPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));

        JPanel labels = new TransparentPanel(new GridLayout(2, 2, 5, 5));

        labels.add(new JLabel(GuiActivator.getResources()
            .getI18NString("service.gui.ACCOUNT")));
        labels.add(new JLabel(GuiActivator.getResources()
            .getI18NString("service.gui.ROOM_NAME")));


        JPanel valuesPanel = new TransparentPanel(new GridLayout(2, 2, 5, 5));
        providersCombo = createProvidersCombobox();

        chatRoomNameField = new JTextField();

        valuesPanel.add(providersCombo);
        valuesPanel.add(chatRoomNameField);

        northPanel.add(labels, BorderLayout.WEST);
        northPanel.add(valuesPanel, BorderLayout.CENTER);
        northPanel.setPreferredSize(new Dimension(600, 80));
        JPanel buttonPanel = new TransparentPanel(new BorderLayout(5, 5));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        JPanel eastButtonPanel = new TransparentPanel();
        JPanel westButtonPanel = new TransparentPanel();

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
        listButton.addActionListener(this);

        okButton.setToolTipText(GuiActivator.getResources()
            .getI18NString("service.gui.JOIN_CHAT_ROOM"));
        updateOKButtonEnableState();


        eastButtonPanel.add(cancelButton);
        eastButtonPanel.add(okButton);
        westButtonPanel.add(listButton);

        buttonPanel.add(eastButtonPanel, BorderLayout.EAST);
        buttonPanel.add(westButtonPanel, BorderLayout.WEST);
        this.getContentPane().add(northPanel, BorderLayout.NORTH);
        this.getContentPane().add(initMoreFields(), BorderLayout.CENTER);
        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        KeyListener keyListener = new KeyListener() {

            public void keyTyped(KeyEvent e)
            {}

            public void keyPressed(KeyEvent e)
            {}

            public void keyReleased(KeyEvent e)
            {
                updateOKButtonEnableState();
                if (e.getKeyCode() == KeyEvent.VK_ENTER && okButton.isEnabled())
                {
                    okButton.doClick();
                }
            }
        };
        // when we are typing we clear any selection in the available and saved
        // rooms
        chatRoomNameField.addKeyListener(keyListener);
        nicknameField.addKeyListener(keyListener);

        providersCombo.addItemListener(new ItemListener()
        {

            @Override
            public void itemStateChanged(ItemEvent event)
            {
                setNickname(
                    (ChatRoomProviderWrapper)providersCombo.getSelectedItem());
                if(serverChatRoomsChoiceDialog != null)
                    serverChatRoomsChoiceDialog.changeProtocolProvider(
                        getSelectedProvider());
            }
        });
        //register listener to listen for newly added chat room providers
        // and for removed ones
        GuiActivator.getMUCService().addChatRoomProviderWrapperListener(
                chatRoomProviderWrapperListener);
    }

    /**
     * Updates the enable/disable state of the OK button.
     */
    private void updateOKButtonEnableState()
    {
        okButton.setEnabled(
            (chatRoomNameField.getText() != null
                && chatRoomNameField.getText().trim().length() > 0)
            && (nicknameField.getText() != null
                && nicknameField.getText().trim().length() > 0));
    }
    /**
     * Sets the default value in the nickname field based on chat room provider.
     * @param provider the provider
     */
    private void setNickname(ChatRoomProviderWrapper provider)
    {
        if (provider == null)
        {
            return;
        }

        nicknameField.setText(
            GuiActivator.getGlobalDisplayDetailsService().getDisplayName(
                provider.getProtocolProvider()));
        updateOKButtonEnableState();
    }

    /**
     * Constructs the more label and the fields related to the label and returns
     * them.
     * @return the more label and the fields related to the label
     */
    private Component initMoreFields()
    {
        JPanel morePanel = new TransparentPanel(new BorderLayout());
        morePanel.setOpaque(false);
        morePanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 5, 15));
        moreFieldsPannel.setBorder(
            BorderFactory.createEmptyBorder(10, 30, 0, 0));
        moreFieldsPannel.setOpaque(false);
        moreFieldsPannel.setVisible(false);
        JPanel subjectPanel = new TransparentPanel(new BorderLayout());
        subject.setFont(getFont().deriveFont(12f));
        subjectPanel.add(subject,BorderLayout.NORTH);
        moreFieldsPannel.add(subjectPanel, BorderLayout.CENTER);
        JPanel nicknamePanel = new TransparentPanel(new BorderLayout(5, 5));
        setNickname((ChatRoomProviderWrapper)providersCombo.getSelectedItem());
        nicknamePanel.add(nicknameField, BorderLayout.CENTER);
        nicknamePanel.add(new JLabel(
            GuiActivator.getResources().getI18NString("service.gui.NICKNAME")),
            BorderLayout.WEST);
        moreFieldsPannel.add(nicknamePanel,BorderLayout.NORTH);
        cmdExpandMoreFields = new JLabel();
        cmdExpandMoreFields.setBorder(new EmptyBorder(0, 5, 0, 0));
        cmdExpandMoreFields.setIcon(DesktopUtilActivator.getResources()
            .getImage("service.gui.icons.RIGHT_ARROW_ICON"));
        cmdExpandMoreFields.setText(DesktopUtilActivator
            .getResources().getI18NString("service.gui.MORE_LABEL"));
        cmdExpandMoreFields.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                cmdExpandMoreFields.setIcon(
                        GuiActivator.getResources().getImage(
                            moreFieldsPannel.isVisible()
                                    ? "service.gui.icons.RIGHT_ARROW_ICON"
                                    : "service.gui.icons.DOWN_ARROW_ICON"));

                moreFieldsPannel.setVisible(
                    !moreFieldsPannel.isVisible());

                pack();
            }
        });
        morePanel.add(cmdExpandMoreFields,BorderLayout.NORTH);
        morePanel.add(moreFieldsPannel,BorderLayout.CENTER);
        return morePanel;
    }

    /**
     * Creates the providers combobox and filling its content.
     * @return
     */
    private JComboBox createProvidersCombobox()
    {
        Iterator<ChatRoomProviderWrapper> providers
            = GuiActivator.getMUCService().getChatRoomProviders();
        JComboBox chatRoomProvidersCombobox = new JComboBox();

        while (providers.hasNext())
            chatRoomProvidersCombobox.addItem(providers.next());

        chatRoomProvidersCombobox.setRenderer(new ChatRoomProviderRenderer());

        return chatRoomProvidersCombobox;
    }

    /**
     * Handles <tt>ActionEvent</tt>s triggered by a button click.
     * @param e the action event.
     */
    public void actionPerformed(ActionEvent e)
    {
        String subject = null;
        JButton sourceButton = (JButton) e.getSource();
        if(sourceButton.equals(okButton))
        {

            if((chatRoomNameField.getText() != null
                    && chatRoomNameField.getText().trim().length() > 0)
                && (nicknameField.getText() != null
                    && nicknameField.getText().trim().length() > 0))
            {
                final ChatRoomWrapper chatRoomWrapper =
                    GuiActivator.getMUCService().createChatRoom(
                        chatRoomNameField.getText().trim(),
                        getSelectedProvider().getProtocolProvider(),
                        new ArrayList<String>(),
                        "",
                        false,
                        false,
                        false);

                if (chatRoomWrapper == null)
                {
                    // In case the protocol failed to create a chat room, null
                    // is returned, so we can stop preparing the UI to open the
                    // (null) chat room.
                    return;
                }

                if(!chatRoomWrapper.isPersistent())
                {
                    chatRoomWrapper.setPersistent(true);

                    ConfigurationUtils.saveChatRoom(
                        chatRoomWrapper.getParentProvider()
                            .getProtocolProvider(),
                        chatRoomWrapper.getChatRoomID(),
                        chatRoomWrapper.getChatRoomID(),
                        chatRoomWrapper.getChatRoomName());
                }

                String nickName = nicknameField.getText().trim();

                ConfigurationUtils.updateChatRoomProperty(
                    chatRoomWrapper.getParentProvider().getProtocolProvider(),
                    chatRoomWrapper.getChatRoomID(), "userNickName", nickName);
                subject = this.subject.getText();
                if(nickName == null)
                    return;

                if(GuiActivator.getConfigurationService()
                    .getBoolean(REMOVE_ROOM_ON_FIRST_JOIN_FAILED, false))
                {
                    chatRoomWrapper.addPropertyChangeListener(
                        new PropertyChangeListener()
                        {
                            @Override
                            public void propertyChange(PropertyChangeEvent evt)
                            {
                                if(evt.getPropertyName()
                                    .equals(ChatRoomWrapper.JOIN_SUCCESS_PROP))
                                    return;

                                // if we failed for some reason we want to
                                // remove the room

                                // close the room
                                GuiActivator.getUIService()
                                    .closeChatRoomWindow(chatRoomWrapper);

                                // remove it
                                GuiActivator.getMUCService()
                                    .removeChatRoom(chatRoomWrapper);
                            }
                        }
                    );
                }

                GuiActivator.getMUCService()
                    .joinChatRoom(chatRoomWrapper, nickName, null, subject);

                ChatWindowManager chatWindowManager =
                    GuiActivator.getUIService().getChatWindowManager();
                ChatPanel chatPanel =
                    chatWindowManager.getMultiChat(chatRoomWrapper, true);

                chatWindowManager.openChat(chatPanel, true);
            }

            // in all cases we dispose this dialog
            dispose();
        }
        else if(sourceButton.equals(cancelButton))
        {
            dispose();
        }
        else if(sourceButton.equals(listButton))
        {
            if(serverChatRoomsChoiceDialog == null)
            {
                serverChatRoomsChoiceDialog = new ServerChatRoomsChoiceDialog(
                    getTitle(), getSelectedProvider());
            }
            serverChatRoomsChoiceDialog.setVisible(true);
        }
    }

    @Override
    protected void close(boolean isEscaped)
    {
        dispose();
    }

    /**
     * Releases the resources allocated by this instance throughout its
     * lifetime.
     */
    @Override
    public void dispose()
    {
        if (chatRoomTableDialog == this)
            chatRoomTableDialog = null;

        GuiActivator.getMUCService().removeChatRoomProviderWrapperListener(
                chatRoomProviderWrapperListener);
        if(serverChatRoomsChoiceDialog != null)
        {
            serverChatRoomsChoiceDialog.dispose();
            serverChatRoomsChoiceDialog = null;
        }

        super.dispose();
    }


    /**
     * Returns the selected provider in the providers combo box.
     *
     * @return the selected provider
     */
    public ChatRoomProviderWrapper getSelectedProvider()
    {
        return (ChatRoomProviderWrapper)providersCombo.getSelectedItem();
    }

    /**
     * Sets the value of chat room name field.
     * @param chatRoom the chat room name.
     */
    public void setChatRoomNameField(String chatRoom)
    {
        this.chatRoomNameField.setText(chatRoom);
        updateOKButtonEnableState();
    }

    /**
     * Sets the value of chat room name field in the current
     * <tt>ChatRoomTableDialog</tt> instance.
     * @param chatRoom the chat room name.
     */
    public static void setChatRoomField(String chatRoom)
    {
        if(chatRoomTableDialog != null)
        {
            chatRoomTableDialog.setChatRoomNameField(chatRoom);
        }
    }

    /**
     * Cell renderer for the providers combo box: displays the protocol name
     * with its associated icon.
     */
    class ChatRoomProviderRenderer
        extends JLabel
        implements ListCellRenderer
    {
        /**
         * The renderer.
         */
        public ChatRoomProviderRenderer()
        {
            setOpaque(true);
            setHorizontalAlignment(LEFT);
            setVerticalAlignment(CENTER);
            this.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        }

        public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
        {
            ChatRoomProviderWrapper provider = (ChatRoomProviderWrapper)value;

            if(provider == null)
                return this;

            if (isSelected)
            {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else
            {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            setText(provider.getProtocolProvider()
                .getAccountID().getDisplayName());

            setIcon(new ImageIcon(provider.getProtocolProvider()
                    .getProtocolIcon().getIcon(ProtocolIcon.ICON_SIZE_16x16)));

            return this;
        }
    }
}
