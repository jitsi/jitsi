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
package net.java.sip.communicator.impl.gui.main.chat.toolBars;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.net.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>HistorySelectorBox</tt> is the component where user could choose a
 * history action like enable/disable and erase history for contact or all
 * history.
 *
 * @author Damian Minkov
 */
public class HistorySelectorBox
    extends SIPCommMenu
    implements ActionListener,
               PropertyChangeListener,
               ChatChangeListener,
               ChatLinkClickedListener,
               Skinnable
{
    /**
     * The logger.
     */
    private Logger logger = Logger.getLogger(HistorySelectorBox.class);

    /**
     * PopupMenu
     */
    private JPopupMenu popupMenu;

    /**
     * The menu item to turn off/on the history logging for a contact.
     */
    private JCheckBoxMenuItem toggleHistoryPerContact;

    /**
     * The menu item to turn off/on the history logging for all contacts.
     */
    private JCheckBoxMenuItem toggleAllHistory;

    /**
     * The menu item to erase history for contact or chat room.
     */
    private JMenuItem eraseHistoryPerContact;

    /**
     * The chat container, where this tool bar is added.
     */
    private ChatContainer chatContainer;

    public HistorySelectorBox(ChatContainer chatContainer)
    {
        this.chatContainer = chatContainer;

        this.setOpaque(false);

        popupMenu = this.getPopupMenu();
        popupMenu.setBackground(Color.WHITE);

        init();

        loadSkin();
    }

    /**
     * Writes the symbol corresponding to a chosen smiley icon to the write
     * message area at the end of the current text.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        String menuItemName = ((JMenuItem) e.getSource()).getName();
        ChatPanel chatPanel = chatContainer.getCurrentChat();

        if (menuItemName.equals("history"))
        {
            HistoryWindow history;

            HistoryWindowManager historyWindowManager
                = GuiActivator.getUIService().getHistoryWindowManager();

            ChatSession chatSession = chatPanel.getChatSession();

            if(historyWindowManager
                .containsHistoryWindowForContact(chatSession.getDescriptor()))
            {
                history = historyWindowManager
                    .getHistoryWindowForContact(chatSession.getDescriptor());

                if(history.getState() == JFrame.ICONIFIED)
                    history.setState(JFrame.NORMAL);

                history.toFront();
            }
            else
            {
                history = new HistoryWindow(
                    chatPanel.getChatSession().getDescriptor());

                history.setVisible(true);

                historyWindowManager
                    .addHistoryWindowForContact(chatSession.getDescriptor(),
                                                    history);
            }
        }
        else if (menuItemName.equals("toggleAllHistory"))
        {
            boolean isHistoryEnabled =
                GuiActivator.getMessageHistoryService()
                    .isHistoryLoggingEnabled();
            GuiActivator.getMessageHistoryService()
                .setHistoryLoggingEnabled(!isHistoryEnabled);
        }
        else if (menuItemName.equals("toggleHistoryPerContact"))
        {
            Object desc = chatPanel.getChatSession().getDescriptor();
            if(desc instanceof MetaContact)
            {
                MetaContact currentContact = (MetaContact)desc;
                boolean isHistoryEnabled =
                    GuiActivator.getMessageHistoryService()
                        .isHistoryLoggingEnabled(currentContact.getMetaUID());
                GuiActivator.getMessageHistoryService()
                    .setHistoryLoggingEnabled(
                    !isHistoryEnabled, currentContact.getMetaUID());
            }
            else if(desc instanceof ChatRoomWrapper)
            {
                ChatRoom currentChatRoom = ((ChatRoomWrapper)desc).getChatRoom();
                boolean isHistoryEnabled =
                    GuiActivator.getMessageHistoryService()
                        .isHistoryLoggingEnabled(
                            currentChatRoom.getIdentifier());
                GuiActivator.getMessageHistoryService()
                    .setHistoryLoggingEnabled(
                        !isHistoryEnabled, currentChatRoom.getIdentifier());
            }
        }
        else if (menuItemName.equals("eraseHistoryPerContact"))
        {
            Object desc = chatPanel.getChatSession().getDescriptor();

            String destination;
            if(desc instanceof MetaContact)
                destination = ((MetaContact)desc).getDisplayName();
            else if(desc instanceof ChatRoomWrapper)
                destination = ((ChatRoomWrapper)desc).getChatRoomName();
            else
                return;

            MessageDialog dialog =
                new MessageDialog(null,
                GuiActivator.getResources().getI18NString(
                    "service.gui.WARNING"),
                GuiActivator.getResources().getI18NString(
                    "service.gui.HISTORY_REMOVE_PER_CONTACT_WARNING",
                    new String[]{destination}),
                GuiActivator.getResources().getI18NString("service.gui.OK"),
                false);

            if (dialog.showDialog() == MessageDialog.OK_RETURN_CODE)
            {
                try
                {
                    if(desc instanceof MetaContact)
                    {
                        ServiceUtils.getService(GuiActivator.bundleContext,
                            MessageHistoryService.class)
                                .eraseLocallyStoredHistory(
                                    (MetaContact)desc);
                        ServiceUtils.getService(GuiActivator.bundleContext,
                            FileHistoryService.class)
                                .eraseLocallyStoredHistory(
                                    (MetaContact)desc);
                    }
                    else if(desc instanceof ChatRoomWrapper)
                    {
                        ServiceUtils.getService(GuiActivator.bundleContext,
                            MessageHistoryService.class)
                                .eraseLocallyStoredHistory(
                                    ((ChatRoomWrapper)desc).getChatRoom());
                    }
                }
                catch(IOException ex)
                {
                    logger.error("Error removing history", ex);

                    chatPanel.addErrorMessage(destination,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.HISTORY_REMOVE_ERROR"),
                        ex.getLocalizedMessage());
                }
            }
        }
        else if (menuItemName.equals("eraseAllHistory"))
        {
            MessageDialog dialog =
                new MessageDialog(null,
                GuiActivator.getResources().getI18NString(
                    "service.gui.WARNING"),
                GuiActivator.getResources().getI18NString(
                    "service.gui.HISTORY_REMOVE_ALL_WARNING"),
                GuiActivator.getResources().getI18NString("service.gui.OK"),
                false);

            if (dialog.showDialog() == MessageDialog.OK_RETURN_CODE)
            {
                try
                {
                    ServiceUtils.getService(GuiActivator.bundleContext,
                        MessageHistoryService.class).eraseLocallyStoredHistory();

                    ServiceUtils.getService(GuiActivator.bundleContext,
                        FileHistoryService.class).eraseLocallyStoredHistory();
                }
                catch(IOException ex)
                {
                    logger.error("Error removing history", ex);

                    chatPanel.addErrorMessage("all",
                        GuiActivator.getResources().getI18NString(
                            "service.gui.HISTORY_REMOVE_ERROR"),
                        ex.getLocalizedMessage());
                }
            }
        }
    }

    /**
     * Initialize the ui components.
     */
    private void init()
    {
        ResourceManagementService R = GuiActivator.getResources();
        JMenuItem historyButton =
            new JMenuItem(R.getI18NString("service.gui.HISTORY"));
        historyButton.setName("history");
        historyButton.setToolTipText(
            R.getI18NString("service.gui.HISTORY") + " Ctrl-H");
        historyButton.addActionListener(this);
        popupMenu.add(historyButton);

        popupMenu.addSeparator();

        toggleHistoryPerContact = new JCheckBoxMenuItem();
        toggleHistoryPerContact.setName("toggleHistoryPerContact");
        toggleHistoryPerContact.addActionListener(this);
        popupMenu.add(toggleHistoryPerContact);

        toggleAllHistory = new JCheckBoxMenuItem(
            R.getI18NString("service.gui.HISTORY_TOGGLE_ALL"));
        toggleAllHistory.setName("toggleAllHistory");
        toggleAllHistory.addActionListener(this);
        popupMenu.add(toggleAllHistory);

        popupMenu.addSeparator();

        eraseHistoryPerContact = new JMenuItem();
        eraseHistoryPerContact.setName("eraseHistoryPerContact");
        eraseHistoryPerContact.addActionListener(this);
        popupMenu.add(eraseHistoryPerContact);

        JMenuItem eraseAllHistory = new JMenuItem(
            R.getI18NString(
                "service.gui.HISTORY_ERASE_ALL",
                new String[]
                    {R.getSettingsString("service.gui.APPLICATION_NAME")}));
        eraseAllHistory.setName("eraseAllHistory");
        eraseAllHistory.addActionListener(this);
        popupMenu.add(eraseAllHistory);

        GuiActivator.getConfigurationService().addPropertyChangeListener(this);

        chatContainer.addChatChangeListener(this);
    }

    /**
     * Listens for changes in the properties to change the icon.
     * @param evt the event of the change
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if(evt.getPropertyName().equals(
            MessageHistoryService.PNAME_IS_MESSAGE_HISTORY_ENABLED))
        {
            updateMenus();
        }
        else if(evt.getPropertyName().startsWith(MessageHistoryService
            .PNAME_IS_MESSAGE_HISTORY_PER_CONTACT_ENABLED_PREFIX))
        {
            Object desc = chatContainer.getCurrentChat()
                .getChatSession().getDescriptor();
            if(desc instanceof MetaContact)
            {
                MetaContact contact = (MetaContact)desc;

                if(evt.getPropertyName().endsWith(contact.getMetaUID()))
                    updateMenus();
            }
            else if(desc instanceof ChatRoomWrapper)
            {
                if(evt.getPropertyName().endsWith(
                        ((ChatRoomWrapper)desc).getChatRoom().getIdentifier()))
                    updateMenus();
            }
        }
    }

    /**
     * Reloads icons in this menu.
     */
    public void loadSkin()
    {
        updateMenus();
    }

    /**
     * Changes currently used icon on or off, depending on the current settings.
     * Updates selected menu items, depends on current configuration.
     * Updates the text depending on the current chat session, is it chat room
     * or a metacontact.
     */
    private void updateMenus()
    {
        toggleAllHistory.setSelected(false);
        toggleHistoryPerContact.setSelected(false);

        MessageHistoryService mhs = GuiActivator.getMessageHistoryService();
        if(!mhs.isHistoryLoggingEnabled())
            toggleAllHistory.setSelected(true);

        if(chatContainer.getCurrentChat() != null)
        {
            ResourceManagementService R = GuiActivator.getResources();

            Object desc = chatContainer.getCurrentChat()
                .getChatSession().getDescriptor();
            if(desc instanceof MetaContact)
            {
                MetaContact contact = (MetaContact)desc;

                if(!mhs.isHistoryLoggingEnabled(
                        contact.getMetaUID()))
                {
                    toggleHistoryPerContact.setSelected(true);
                }

                toggleHistoryPerContact.setText(
                    R.getI18NString("service.gui.HISTORY_TOGGLE_PER_CONTACT"));
                eraseHistoryPerContact.setText(
                    R.getI18NString("service.gui.HISTORY_ERASE_PER_CONTACT"));
            }
            else if(desc instanceof ChatRoomWrapper)
            {
                if(!mhs.isHistoryLoggingEnabled(
                        ((ChatRoomWrapper)desc).getChatRoom().getIdentifier()))
                {
                    toggleHistoryPerContact.setSelected(true);
                }

                toggleHistoryPerContact.setText(
                    R.getI18NString("service.gui.HISTORY_TOGGLE_PER_CHATROOM"));
                eraseHistoryPerContact.setText(
                    R.getI18NString("service.gui.HISTORY_ERASE_PER_CHATROOM"));
            }
        }

        if(toggleAllHistory.isSelected()
            || toggleHistoryPerContact.isSelected())
        {
            this.setIcon(new ImageIcon(ImageLoader
                    .getImage(ImageLoader.HISTORY_ICON_OFF)));
        }
        else
            this.setIcon(new ImageIcon(ImageLoader
                    .getImage(ImageLoader.HISTORY_ICON_ON)));
    }

    /**
     * Clears the listener.
     */
    public void dispose()
    {
        GuiActivator.getConfigurationService()
            .removePropertyChangeListener(this);
        chatContainer.removeChatChangeListener(this);
    }

    /**
     * Listens for changes in the current chat so we can change
     * the icon to on/off.
     * @param panel the current visible chat panel
     */
    @Override
    public void chatChanged(ChatPanel panel)
    {
        updateMenus();

        panel.addChatLinkClickedListener(this);
    }

    /**
     * If a link is clicked with certain action to open the history popup menu.
     * @param url The URI of the link that was clicked.
     */
    @Override
    public void chatLinkClicked(URI url)
    {
        if(url.getPath().equals("/showHistoryPopupMenu"))
        {
            this.doClick();
        }
    }
}
