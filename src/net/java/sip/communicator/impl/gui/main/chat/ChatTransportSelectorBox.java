/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

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

    private final Map<ChatTransport, JMenuItem> transportMenuItems =
        new Hashtable<ChatTransport, JMenuItem>();

    private final SIPCommMenu menu = new SIPCommMenu();

    private final ChatSession chatSession;

    /**
     * Creates an instance of <tt>ChatTransportSelectorBox</tt>.
     *
     * @param chatSession the corresponding chat session
     * @param selectedChatTransport the chat transport to select by default
     */
    public ChatTransportSelectorBox(ChatSession chatSession,
                                    ChatTransport selectedChatTransport)
    {
        this.chatSession = chatSession;

        this.menu.setPreferredSize(new Dimension(28, 24));
        this.menu.setUI(new SIPCommSelectorMenuUI());

        this.add(menu);

        // as a default disable the menu, it will be enabled as soon as we add
        // a valid menu item
        this.menu.setEnabled(false);

        Iterator<ChatTransport> chatTransports = chatSession.getChatTransports();
        while (chatTransports.hasNext())
            this.addChatTransport(chatTransports.next());

        if (this.menu.getItemCount() > 0 &&
            selectedChatTransport.allowsInstantMessage())
        {
            this.setSelected(selectedChatTransport);
        }
    }

    /*
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
        if (chatTransport.allowsInstantMessage())
        {
            Image img = createTransportStatusImage(chatTransport);

            JMenuItem menuItem = new JMenuItem(
                        "From: " + chatTransport.getProtocolProvider()
                            .getAccountID().getAccountAddress()
                        + " To: " + chatTransport.getName(),
                        new ImageIcon(img));

            menuItem.addActionListener(this);
            this.transportMenuItems.put(chatTransport, menuItem);

            this.menu.add(menuItem);

            updateEnableStatus();
        }
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
        this.menu.remove(transportMenuItems.get(chatTransport));
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
        JMenuItem menuItem = (JMenuItem) e.getSource();

        for (Map.Entry<ChatTransport, JMenuItem> transportMenuItem
                : transportMenuItems.entrySet())
        {
            ChatTransport chatTransport = transportMenuItem.getKey();

            if (transportMenuItem.getValue().equals(menuItem))
            {
                this.setSelected(chatTransport, (ImageIcon) menuItem.getIcon());

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
            ImageLoader.badgeImageWithProtocolIndex(
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
                = chatSession.getCurrentChatTransport();

            if(newChatTransport.getStatus().isOnline())
                this.setSelected(newChatTransport);
        }

        if (!containsOtherOnlineContacts(chatTransport)
            && chatTransport.getStatus().isOnline())
        {
            this.setSelected(chatTransport);
        }

        menuItem = transportMenuItems.get(chatTransport);

        // sometimes it may happen that menuItem is null
        // it was removed for some reason, this maybe due to other bug
        // anyway detect it to avoid NPE
        if(menuItem == null)
            return;

        icon = new ImageIcon(createTransportStatusImage(chatTransport));

        menuItem.setIcon(icon);
        if(menu.getSelectedObject().equals(chatTransport))
        {
            this.menu.setIcon(icon);
        }
    }

    /**
     * In the "send via" menu selects the given contact and sets the given icon
     * to the "send via" menu button.
     *
     * @param chatTransport
     * @param icon
     */
    private void setSelected(ChatTransport chatTransport, ImageIcon icon)
    {
        this.chatSession.setCurrentChatTransport(chatTransport);

        SelectedObject selectedObject = new SelectedObject(icon, chatTransport);

        this.menu.setSelected(selectedObject);

        String tooltipText = "From: " + chatTransport.getProtocolProvider()
            .getAccountID().getAccountAddress() + " To: ";

        if(!chatTransport.getDisplayName()
                .equals(chatTransport.getName()))
            tooltipText += chatTransport.getDisplayName()
                + " (" + chatTransport.getName() + ")";
        else
            tooltipText += chatTransport.getDisplayName();

        this.menu.setToolTipText(tooltipText);
    }

    /**
     * Sets the selected contact to the given proto contact.
     * @param chatTransport the proto contact to select
     */
    public void setSelected(ChatTransport chatTransport)
    {
        this.setSelected(chatTransport,
                new ImageIcon(createTransportStatusImage(chatTransport)));
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
    private boolean containsOtherOnlineContacts(ChatTransport chatTransport)
    {
        for (ChatTransport comboChatTransport : transportMenuItems.keySet())
        {
            if(!comboChatTransport.equals(chatTransport)
                && comboChatTransport.getStatus().isOnline())
                return true;
        }
        return false;
    }

    /**
     * Reloads UI defs.
     */
    public void loadSkin()
    {
        super.loadSkin();

        if(menu != null && menu.getUI() != null
                && menu.getUI() instanceof SIPCommSelectorMenuUI)
            ((SIPCommSelectorMenuUI) menu.getUI()).loadSkin();
    }
}
