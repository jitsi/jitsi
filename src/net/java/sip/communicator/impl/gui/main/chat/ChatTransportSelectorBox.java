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
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.jitsi.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>ChatTransportSelectorBox</tt> represents the send via menu in the
 * chat window. The menu contains all protocol specific transports. In the case
 * of meta contact these would be all contacts for the currently selected meta
 * contact chat.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class ChatTransportSelectorBox
    extends SIPCommMenuBar
    implements  ActionListener,
                Skinnable
{
    private static final Logger logger
        = Logger.getLogger(ChatTransportSelectorBox.class);

    private static final long serialVersionUID = 0L;

    private final Map<ChatTransport, JCheckBoxMenuItem> transportMenuItems =
        new Hashtable<ChatTransport, JCheckBoxMenuItem>();

    private final SIPCommMenu menu = new SelectorMenu();

    private final ChatSession chatSession;

    private final ChatPanel chatPanel;

    /**
     * Take care for chat transport items, that only one is selected.
     */
    private ButtonGroup buttonGroup = new ButtonGroup();

    /**
     * Creates an instance of <tt>ChatTransportSelectorBox</tt>.
     *
     * @param chatPanel the chat panel
     * @param chatSession the corresponding chat session
     * @param selectedChatTransport the chat transport to select by default
     */
    public ChatTransportSelectorBox(ChatPanel chatPanel,
                                    ChatSession chatSession,
                                    ChatTransport selectedChatTransport)
    {
        this.chatPanel = chatPanel;
        this.chatSession = chatSession;

        setPreferredSize(new Dimension(30, 28));
        setMaximumSize(new Dimension(30, 28));
        setMinimumSize(new Dimension(30, 28));

        this.menu.setPreferredSize(new Dimension(30, 45));
        this.menu.setMaximumSize(new Dimension(30, 45));

        this.add(menu);

        this.setBorder(null);
        this.menu.setBorder(null);
        this.menu.setOpaque(false);
        this.setOpaque(false);

        // as a default disable the menu, it will be enabled as soon as we add
        // a valid menu item
        this.menu.setEnabled(false);

        Iterator<ChatTransport> chatTransports
            = chatSession.getChatTransports();

        while (chatTransports.hasNext())
            this.addChatTransport(chatTransports.next());

        if (this.menu.getItemCount() > 0)
        {
            if (selectedChatTransport != null
                && (selectedChatTransport.allowsInstantMessage()
                || selectedChatTransport.allowsSmsMessage()))
            {
                this.setSelected(selectedChatTransport);
            }
            else
                setSelected(menu.getItem(0));
        }
    }

    /**
     * Sets the menu to enabled or disabled. The menu is enabled, as soon as it
     * contains one or more items. If it is empty, it is disabled.
     */
    private void updateEnableStatus()
    {
        this.menu.setEnabled(this.menu.getItemCount() > 0);
    }

    /**
     * Adds the given chat transport to the "send via" menu.
     * Only add those that support IM.
     *
     * @param chatTransport The chat transport to add.
     */
    public void addChatTransport(ChatTransport chatTransport)
    {
        if (chatTransport.allowsInstantMessage()
            || chatTransport.allowsSmsMessage())
        {
            Image img = createTransportStatusImage(chatTransport);

            boolean isIndent = false;
            String toString = "";
            if (chatTransport.getResourceName() != null
                && chatTransport.isDisplayResourceOnly())
            {
                toString = chatTransport.getResourceName();
                isIndent = true;
            }
            else
                toString = "<b>" + chatTransport.getName() + "</b> "
                                    + ((chatTransport.getResourceName() == null)
                                        ? ""
                                        : chatTransport.getResourceName())
                                    + " <i>("
                                    + GuiActivator.getResources()
                                        .getI18NString("service.gui.VIA")
                                    + ": "
                                    + chatTransport.getProtocolProvider()
                                        .getAccountID().getDisplayName()
                                    + ")</i>";

            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(
                            "<html><font size=\"3\">"
                            + toString
                            + "</font></html>",
                            new ImageIcon(img));

            if (isIndent)
                menuItem.setBorder(
                    BorderFactory.createEmptyBorder(0, 20, 0, 0));

            menuItem.addActionListener(this);
            this.transportMenuItems.put(chatTransport, menuItem);

            buttonGroup.add(menuItem);
            this.menu.add(menuItem);

            updateEnableStatus();

            updateTransportStatus(chatTransport);
        }

        if(!allowsInstantMessage() && allowsSmsMessage())
            chatPanel.getChatWritePanel().setSmsSelected(true);
        else
            chatPanel.getChatWritePanel().setSmsSelected(false);
    }

    /**
     * Removes the given chat transport from the "send via" menu. This method is
     * used to update the "send via" menu when a protocol contact is moved or
     * removed from the contact list.
     *
     * @param chatTransport the chat transport to be removed
     */
    public void removeChatTransport(ChatTransport chatTransport)
    {
        JCheckBoxMenuItem menuItem = transportMenuItems.get(chatTransport);
        this.menu.remove(menuItem);
        this.buttonGroup.remove(menuItem);
        this.transportMenuItems.remove(chatTransport);

        updateEnableStatus();
    }

    /**
     * The listener of the chat transport selector box.
     *
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) e.getSource();

        for (Map.Entry<ChatTransport, JCheckBoxMenuItem> transportMenuItem
                : transportMenuItems.entrySet())
        {
            ChatTransport chatTransport = transportMenuItem.getKey();

            if (transportMenuItem.getValue().equals(menuItem))
            {
                this.setSelected(   menuItem,
                                    chatTransport,
                                    (ImageIcon) menuItem.getIcon());

                chatSession.getChatSessionRenderer()
                    .setChatIcon(new ImageIcon(
                        Constants.getStatusIcon(chatTransport.getStatus())));

                return;
            }
        }

        if (logger.isDebugEnabled())
            logger.debug( "Could not find contact for menu item "
                      + menuItem.getText() + ". contactsTable("
                      + transportMenuItems.size()+") is : "
                      + transportMenuItems);
    }

    /**
     * Obtains the status icon for the given chat transport and
     * adds to it the account index information.
     *
     * @param chatTransport The chat transport for which to create the image.
     * @return The indexed status image.
     */
    public Image createTransportStatusImage(ChatTransport chatTransport)
    {
        return
            ImageLoader.getIndexedProtocolImage(
                ImageUtils.getBytesInImage(
                    chatTransport.getStatus().getStatusIcon()),
                chatTransport.getProtocolProvider());
    }

    /**
     * Updates the chat transport presence status.
     *
     * @param chatTransport The chat transport to update.
     */
    public void updateTransportStatus(ChatTransport chatTransport)
    {
        JMenuItem menuItem;
        Icon icon;

        if (chatTransport.equals(chatSession.getCurrentChatTransport())
            && !chatTransport.getStatus().isOnline())
        {
            ChatTransport newChatTransport
                = getParentContactTransport(chatTransport);

            ChatTransport onlineTransport = getTransport(true);

            if(newChatTransport != null
                && newChatTransport.getStatus().isOnline())
                setSelected(newChatTransport);
            else if (onlineTransport != null)
                setSelected(onlineTransport);
            else
            {
                // update when going to offline
                ChatTransport offlineTransport = getTransport(false);

                if(offlineTransport != null)
                    setSelected(offlineTransport);
            }
        }

        menuItem = transportMenuItems.get(chatTransport);

        // sometimes it may happen that menuItem is null
        // it was removed for some reason, this maybe due to other bug
        // anyway detect it to avoid NPE
        if(menuItem == null)
            return;

        icon = new ImageIcon(createTransportStatusImage(chatTransport));

        menuItem.setIcon(icon);
        if( menu.getSelectedObject() != null
            && menu.getSelectedObject().equals(chatTransport))
        {
            this.menu.setIcon(icon);
            this.chatSession.fireCurrentChatTransportUpdated(
                    ChatSessionChangeListener.ICON_UPDATED);
        }
    }

    /**
     * In the "send via" menu selects the given contact and sets the given icon
     * to the "send via" menu button.
     *
     * @param menuItem the menu item that is selected
     * @param chatTransport the corresponding chat transport
     * @param icon
     */
    private void setSelected(   JCheckBoxMenuItem menuItem,
                                ChatTransport chatTransport,
                                ImageIcon icon)
    {
        menuItem.setSelected(true);

        this.chatSession.setCurrentChatTransport(chatTransport);

        SelectedObject selectedObject = new SelectedObject(icon, chatTransport);

        this.menu.setSelected(selectedObject);

        this.chatSession.fireCurrentChatTransportUpdated(
                ChatSessionChangeListener.ICON_UPDATED);

        String resourceName = (chatTransport.getResourceName() != null)
                                ? " (" + chatTransport.getResourceName() + ")"
                                : "";

        String displayName = (!chatTransport.getDisplayName()
                                    .equals(chatTransport.getName()))
                                ? chatTransport.getDisplayName()
                                    + " (" + chatTransport.getName() + ")"
                                : chatTransport.getDisplayName();

        String tooltipText = "<html><font size=\"3\"><b>"
                                + displayName + "</b>"
                                + resourceName
                                + "<br/><i>"
                                + GuiActivator.getResources()
                                    .getI18NString("service.gui.VIA")
                                + ": "
                                + chatTransport.getProtocolProvider()
                                    .getAccountID().getAccountAddress()
                                + "</i></font></html>";

        this.menu.setToolTipText(tooltipText);

        chatPanel.getChatWritePanel().setSmsLabelVisible(
            chatTransport.allowsSmsMessage());
    }

    /**
     * Sets the selected contact to the given proto contact.
     * @param chatTransport the proto contact to select
     */
    public void setSelected(ChatTransport chatTransport)
    {
        JCheckBoxMenuItem menuItem = transportMenuItems.get(chatTransport);

        if (menuItem == null)
            return;

        this.setSelected(
                menuItem,
                chatTransport,
                new ImageIcon(createTransportStatusImage(chatTransport)));
    }

    /**
     * Do we have a selected transport.
     * @return do we have a selected transport.
     */
    boolean hasSelectedTransport()
    {
        for(JCheckBoxMenuItem item : transportMenuItems.values())
        {
            if(item.isSelected())
                return true;
        }

        return false;
    }

    /**
     * Returns the protocol menu.
     *
     * @return the protocol menu
     */
    public SIPCommMenu getMenu()
    {
        return menu;
    }

    /**
     * Searches online contacts in the send via combo box.
     *
     * @param chatTransport the chat transport to check
     * @return TRUE if the send via combo box contains online contacts,
     * otherwise returns FALSE.
     */
    private ChatTransport getParentContactTransport(ChatTransport chatTransport)
    {
        for (ChatTransport comboChatTransport : transportMenuItems.keySet())
        {
            if(comboChatTransport.getDescriptor()
                .equals(chatTransport.getDescriptor())
                && StringUtils.isNullOrEmpty(
                    comboChatTransport.getResourceName()))
                return comboChatTransport;
        }
        return null;
    }

    /**
     * Searches online contacts in the send via combo box.
     *
     * @param online if <tt>TRUE</tt> will return online transport, otherwise
     *               will return offline one.
     * @return online or offline contact transport from combo box.
     */
    private ChatTransport getTransport(boolean online)
    {
        for (ChatTransport comboChatTransport : transportMenuItems.keySet())
        {
            if(online && comboChatTransport.getStatus().isOnline())
                return comboChatTransport;
            else if(!online && !comboChatTransport.getStatus().isOnline())
                return comboChatTransport;
        }
        return null;
    }

    /**
     * Returns <code>true</code> if this contains a chat transport that
     * supports instant messaging, otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this contains a chat transport that
     * supports instant messaging, otherwise returns <code>false</code>
     */
    private boolean allowsInstantMessage()
    {
        for(ChatTransport tr : transportMenuItems.keySet())
        {
            if(tr.allowsInstantMessage())
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns <code>true</code> if this contains a chat transport that
     * supports sms messaging, otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this contains a chat transport that
     * supports sms messaging, otherwise returns <code>false</code>
     */
    private boolean allowsSmsMessage()
    {
        for(ChatTransport tr : transportMenuItems.keySet())
        {
            if(tr.allowsSmsMessage())
                return true;
        }

        return false;
    }

    /**
     * A custom <tt>SIPCommMenu</tt> that adds an arrow icon to the right of
     * the menu image.
     */
    private class SelectorMenu
        extends SIPCommMenu
    {
        private static final long serialVersionUID = 0L;

        Image image = ImageLoader.getImage(ImageLoader.DOWN_ARROW_ICON);

        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            g.drawImage(image,
                getWidth() - image.getWidth(this) - 1,
                (getHeight() - image.getHeight(this) - 1)/2,
                this);
        }
    }
}
