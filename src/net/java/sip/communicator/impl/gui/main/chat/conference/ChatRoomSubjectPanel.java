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
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

import org.jitsi.service.resources.*;

/**
 * The panel containing the subject of the chat room and the configuration
 * button.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class ChatRoomSubjectPanel
    extends TransparentPanel
    implements Skinnable,
               ChatRoomLocalUserRoleListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>ChatRoomSubjectPanel</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ChatRoomSubjectPanel.class);

    /**
     * The corresponding chat session.
     */
    private final ConferenceChatSession chatSession;

    /**
     * The field containing the subject of the chat room.
     */
    private final JTextField subjectField = new JTextField();

    /**
     * Config button.
     */
    private JButton configButton;

    /**
     * Members list button.
     */
    private JButton membersListButton;

    /**
     * Configuration buttons.
     */
    private final JPanel configButtonsPanel =
        new TransparentPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));

    /**
     * Creates the panel containing the chat room subject.
     *
     * @param chatSession the chat session
     * chat room subject and the configuration information.
     */
    public ChatRoomSubjectPanel(ConferenceChatSession chatSession)
    {
        super(new BorderLayout(0, 5));

        this.chatSession = chatSession;

        JLabel subjectLabel
            = new JLabel(
                    GuiActivator.getResources().getI18NString(
                            "service.gui.SUBJECT") + ": ");

        subjectField.setText(chatSession.getChatSubject());
        // TODO Implement the editing of the chat room subject.
        subjectField.setEditable(false);

        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 0));
        add(subjectLabel, BorderLayout.WEST);
        add(subjectField, BorderLayout.CENTER);
        add(configButtonsPanel, BorderLayout.EAST);

        chatSession.addLocalUserRoleListener(this);
        updateConfigButtons();
    }

    /**
     * Updates the config button state add or remove depending on the
     * user role.
     */
    private synchronized void updateConfigButtons()
    {
        ChatRoom room = ((ChatRoomWrapper)chatSession.getDescriptor())
            .getChatRoom();
        ChatRoomMemberRole role = room.getUserRole();

        if(!ConfigurationUtils.isChatRoomConfigDisabled()
            && (role.equals(ChatRoomMemberRole.ADMINISTRATOR)
            || role.equals(ChatRoomMemberRole.OWNER)))
        {
            if(membersListButton == null)
            {
                membersListButton
                    = new JButton(new ImageIcon(ImageLoader.getImage(
                                ImageLoader.CHAT_ROOM_MEMBERS_LIST_CONFIG)));
                membersListButton.setToolTipText(
                    GuiActivator.getResources().getI18NString(
                    "service.gui.CHAT_ROOM_CONFIGURATION_MEMBERS_EDIT_TITLE"));
                membersListButton.setPreferredSize(new Dimension(26, 26));
                membersListButton.addActionListener(
                    new MembersListButtonActionListener());

                configButtonsPanel.add(membersListButton);

                revalidate();
                repaint();
            }
        }
        else if(membersListButton != null)
        {
            remove(membersListButton);
            membersListButton = null;

            revalidate();
            repaint();
        }

        if(!ConfigurationUtils.isChatRoomConfigDisabled()
            && role.equals(ChatRoomMemberRole.OWNER))
        {
            if(configButton == null)
            {
                configButton
                    = new JButton(new ImageIcon(ImageLoader.getImage(
                                ImageLoader.CHAT_ROOM_CONFIG)));
                configButton.setToolTipText(
                    GuiActivator.getResources().getI18NString(
                        "service.gui.CHAT_ROOM_OPTIONS"));
                configButton.setPreferredSize(new Dimension(26, 26));
                configButton.addActionListener(
                    new ConfigButtonActionListener());

                configButtonsPanel.add(configButton);

                revalidate();
                repaint();
            }
        }
        else if(configButton != null)
        {
            remove(configButton);
            configButton = null;

            revalidate();
            repaint();
        }
    }

    /**
     * Gets the (chat room) subject displayed in this
     * <tt>ChatRoomSubjectPanel</tt>.
     *
     * @return the (chat room) subject displayed in this
     * <tt>ChatRoomSubjectPanel</tt>
     */
    public String getSubject()
    {
        return subjectField.getText();
    }

    /**
     * Sets the (chat room) subject to be displayed in this
     * <tt>ChatRoomSubjectPanel</tt>.
     *
     * @param subject the (chat room) subject to be displayed in this
     * <tt>ChatRoomSubjectPanel</tt>
     */
    public void setSubject(String subject)
    {
        subjectField.setText(subject);
    }

    /**
     * Fired when local user role has changed.
     * @param evt the <tt>ChatRoomLocalUserRoleChangeEvent</tt> instance
     */
    @Override
    public void localUserRoleChanged(final ChatRoomLocalUserRoleChangeEvent evt)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    localUserRoleChanged(evt);
                }
            });
            return;
        }

        updateConfigButtons();
    }

    /**
     * Reload config button if exists.
     */
    public void loadSkin()
    {
        if(configButton != null)
            configButton.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.CHAT_ROOM_CONFIG)));
        if(membersListButton != null)
            membersListButton.setIcon(new ImageIcon(
                ImageLoader.getImage(
                    ImageLoader.CHAT_ROOM_MEMBERS_LIST_CONFIG)));
    }

    /**
     * Runs clean-up.
     */
    public void dispose()
    {
        chatSession.removeLocalUserRoleListener(this);
    }

    /**
     * Opens the configuration dialog when the configure buttons is pressed.
     */
    private class ConfigButtonActionListener
        implements ActionListener
    {
        /**
         * Obtains and opens the configuration form of the corresponding chat
         * room when user clicks on the configuration button.
         * @param evt the <tt>ActionEvent</tt> that notified us
         */
        public void actionPerformed(ActionEvent evt)
        {
            try
            {
                ChatRoomConfigurationForm configForm
                    = chatSession.getChatConfigurationForm();
                ChatRoomConfigurationWindow configWindow
                    = new ChatRoomConfigurationWindow(
                        chatSession.getChatName(), configForm);

                configWindow.pack();
                configWindow.setVisible(true);
            }
            catch (OperationFailedException e)
            {
                logger.error(
                    "Failed to obtain the chat room configuration form.", e);

                ResourceManagementService resources
                    = GuiActivator.getResources();

                if(e.getErrorCode()
                    == OperationFailedException.NOT_ENOUGH_PRIVILEGES)
                {
                    new ErrorDialog(
                        null,
                        resources.getI18NString("service.gui.WARNING"),
                        resources.getI18NString(
                                "service.gui.CHAT_ROOM_CONFIGURATION_FORBIDDEN",
                                new String[]{chatSession.getChatName()}),
                        ErrorDialog.WARNING)
                    .showDialog();
                }
                else
                {
                    new ErrorDialog(
                        null,
                        resources.getI18NString("service.gui.ERROR"),
                        resources.getI18NString(
                                "service.gui.CHAT_ROOM_CONFIGURATION_FAILED",
                                new String[]{chatSession.getChatName()}),
                        e)
                    .showDialog();
                }
            }
        }
    }

    /**
     * Opens the configuration dialog for members list when
     * the button is pressed.
     */
    private class MembersListButtonActionListener
        implements ActionListener
    {
        /**
         * Just opens the MembersListDialog.
         * @param evt the <tt>ActionEvent</tt> that notified us
         */
        public void actionPerformed(ActionEvent evt)
        {
            MembersListDialog dialog = new MembersListDialog(
                (ChatRoomWrapper)chatSession.getDescriptor(),
                GuiActivator.getResources().getI18NString(
                    "service.gui.CHAT_ROOM_CONFIGURATION_MEMBERS_EDIT_TITLE"),
                false);
            dialog.setVisible(true);
        }
    }
}
