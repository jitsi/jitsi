/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.toolBars;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import org.jitsi.service.resources.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;

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
                ConfigurationUtils.isHistoryLoggingEnabled();
            ConfigurationUtils.setHistoryLoggingEnabled(!isHistoryEnabled);
        }
        else if (menuItemName.equals("toggleHistoryPerContact"))
        {
            Object desc = chatPanel.getChatSession().getDescriptor();
            if(desc instanceof MetaContact)
            {
                MetaContact currentContact = (MetaContact)desc;
                boolean isHistoryEnabled =
                    ConfigurationUtils.isHistoryLoggingEnabled(currentContact);
                ConfigurationUtils.setHistoryLoggingEnabled(
                    !isHistoryEnabled, currentContact);
            }
        }
        else if (menuItemName.equals("eraseHistoryPerContact"))
        {
            Object desc = chatPanel.getChatSession().getDescriptor();

            if(!(desc instanceof MetaContact))
                return;

            MetaContact contact = (MetaContact)desc;

            MessageDialog dialog =
                new MessageDialog(null,
                GuiActivator.getResources().getI18NString(
                    "service.gui.WARNING"),
                GuiActivator.getResources().getI18NString(
                    "service.gui.HISTORY_REMOVE_PER_CONTACT_WARNING",
                    new String[]{contact.getDisplayName()}),
                GuiActivator.getResources().getI18NString("service.gui.OK"),
                false);

            if (dialog.showDialog() == MessageDialog.OK_RETURN_CODE)
            {
                try
                {
                    ServiceUtils.getService(GuiActivator.bundleContext,
                        MessageHistoryService.class).eraseLocallyStoredHistory(
                        contact);
                }
                catch(IOException ex)
                {
                    logger.error("Error removing history", ex);

                    chatPanel.addErrorMessage(contact.getDisplayName(),
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

        toggleHistoryPerContact = new JCheckBoxMenuItem(
            R.getI18NString("service.gui.HISTORY_TOGGLE_PER_CONTACT"));
        toggleHistoryPerContact.setName("toggleHistoryPerContact");
        toggleHistoryPerContact.addActionListener(this);
        popupMenu.add(toggleHistoryPerContact);

        toggleAllHistory = new JCheckBoxMenuItem(
            R.getI18NString("service.gui.HISTORY_TOGGLE_ALL"));
        toggleAllHistory.setName("toggleAllHistory");
        toggleAllHistory.addActionListener(this);
        popupMenu.add(toggleAllHistory);

        popupMenu.addSeparator();

        JMenuItem eraseHistoryPerContact = new JMenuItem(
            R.getI18NString("service.gui.HISTORY_ERASE_PER_CONTACT"));
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
            changeHistoryIcon();
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
                    changeHistoryIcon();
            }
        }
    }

    /**
     * Reloads icons in this menu.
     */
    public void loadSkin()
    {
        changeHistoryIcon();
    }

    /**
     * Changes currently used icon on or off, depending on the current settings.
     */
    private void changeHistoryIcon()
    {
        toggleAllHistory.setSelected(false);
        toggleHistoryPerContact.setSelected(false);

        if(!ConfigurationUtils.isHistoryLoggingEnabled())
            toggleAllHistory.setSelected(true);

        if(chatContainer.getCurrentChat() != null)
        {
            Object desc = chatContainer.getCurrentChat()
                .getChatSession().getDescriptor();
            if(desc instanceof MetaContact)
            {
                MetaContact contact = (MetaContact)desc;

                if(!ConfigurationUtils.isHistoryLoggingEnabled(contact))
                {
                    toggleHistoryPerContact.setSelected(true);
                }
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
        changeHistoryIcon();
    }
}
