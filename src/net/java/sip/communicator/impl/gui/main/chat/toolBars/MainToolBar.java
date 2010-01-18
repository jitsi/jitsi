/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.toolBars;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>MainToolBar</tt> is a <tt>JToolBar</tt> which contains buttons
 * for file operations, like save and print, for copy-paste operations, etc.
 * It's the main toolbar in the <tt>ChatWindow</tt>. It contains only
 * <tt>ChatToolbarButton</tt>s, which have a specific background icon and
 * rollover behaviour to differentiates them from normal buttons.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class MainToolBar
    extends TransparentPanel
    implements ActionListener,
               ChatChangeListener,
               ChatSessionChangeListener
{
    private static final long serialVersionUID = -5572510509556499465L;

    private final ChatToolbarButton inviteButton
        = new ChatToolbarButton(
                ImageLoader.getImage(ImageLoader.ADD_TO_CHAT_ICON));

    private final ChatToolbarButton historyButton
        = new ChatToolbarButton(
                ImageLoader.getImage(ImageLoader.HISTORY_ICON));

    private final ChatToolbarButton sendFileButton
        = new ChatToolbarButton(
                ImageLoader.getImage(ImageLoader.SEND_FILE_ICON));

    private final ChatToolbarButton previousButton
        = new ChatToolbarButton(
                ImageLoader.getImage(ImageLoader.PREVIOUS_ICON));

    private final ChatToolbarButton nextButton
        = new ChatToolbarButton(
                ImageLoader.getImage(ImageLoader.NEXT_ICON));

    /**
     * The current <tt>ChatSession</tt> made known to this instance by the last
     * call to its {@link #chatChanged(ChatPanel)}. 
     */
    private ChatSession chatSession;

    protected final ChatWindow messageWindow;

    private final PluginContainer pluginContainer;

    /**
     * Creates an instance and constructs the <tt>MainToolBar</tt>.
     *
     * @param messageWindow The parent <tt>ChatWindow</tt>.
     */
    public MainToolBar(ChatWindow messageWindow)
    {
        this.messageWindow = messageWindow;

        init();

        pluginContainer
            = new PluginContainer(this, Container.CONTAINER_CHAT_TOOL_BAR);
        
        this.messageWindow.addChatChangeListener(this);
    }

    protected void init()
    {
        ChatToolbarButton optionsButton
            = new ChatToolbarButton(
                    ImageLoader.getImage(ImageLoader.CHAT_CONFIGURE_ICON));

        this.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 0));
        this.setOpaque(false);

        this.add(inviteButton);
        this.add(historyButton);
        this.add(optionsButton);
        this.add(sendFileButton);

        this.add(previousButton);
        this.add(nextButton);

        this.inviteButton.setName("invite");
        this.inviteButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.INVITE"));

        this.historyButton.setName("history");
        this.historyButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.HISTORY")
            + " Ctrl-H");

        optionsButton.setName("options");
        optionsButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.OPTIONS"));

        this.sendFileButton.setName("sendFile");
        this.sendFileButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.SEND_FILE"));

        this.previousButton.setName("previous");
        this.previousButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.PREVIOUS"));

        this.nextButton.setName("next");
        this.nextButton.setToolTipText(
            GuiActivator.getResources().getI18NString("service.gui.NEXT"));

        this.inviteButton.addActionListener(this);
        this.historyButton.addActionListener(this);
        optionsButton.addActionListener(this);
        this.sendFileButton.addActionListener(this);
        this.previousButton.addActionListener(this);
        this.nextButton.addActionListener(this);
    }

    /**
     * Runs clean-up for associated resources which need explicit disposal (e.g.
     * listeners keeping this instance alive because they were added to the
     * model which operationally outlives this instance).
     */
    public void dispose()
    {
        pluginContainer.dispose();
    }

    /*
     * Implements ChatChangeListener#chatChanged(ChatPanel).
     */
    public void chatChanged(ChatPanel panel)
    {
        if (panel == null)
        {
            setChatSession(null);
        }
        else
        {
            MetaContact contact
                = GuiActivator.getUIService().getChatContact(panel);

            for (PluginComponent c : pluginContainer.getPluginComponents())
                c.setCurrentContact(contact);

            setChatSession(panel.chatSession);
        }
    }

    /*
     * Implements
     * ChatSessionChangeListener#currentChatTransportChanged(ChatSession).
     */
    public void currentChatTransportChanged(ChatSession chatSession)
    {
        if (chatSession == null)
            return;

        ChatTransport currentTransport = chatSession.getCurrentChatTransport();
        Object currentDescriptor = currentTransport.getDescriptor();

        if (currentDescriptor instanceof Contact)
        {
            Contact contact = (Contact) currentDescriptor;

            for (PluginComponent c : pluginContainer.getPluginComponents())
                c.setCurrentContact(contact);
        }
    }

    /**
     * Handles the <tt>ActionEvent</tt>, when one of the toolbar buttons is
     * clicked.
     */
    public void actionPerformed(ActionEvent e)
    {
        AbstractButton button = (AbstractButton) e.getSource();
        String buttonText = button.getName();

        ChatPanel chatPanel = messageWindow.getCurrentChatPanel();

        if (buttonText.equals("previous"))
        {
            chatPanel.loadPreviousPageFromHistory();
        }
        else if (buttonText.equals("next"))
        {
            chatPanel.loadNextPageFromHistory();
        }
        else if (buttonText.equals("sendFile"))
        {
            SipCommFileChooser scfc = GenericFileDialog.create(
                null, "Send file...", SipCommFileChooser.LOAD_FILE_OPERATION,
                    ConfigurationManager.getSendFileLastDir());
            File selectedFile = scfc.getFileFromDialog();
            if(selectedFile != null)
            {
                ConfigurationManager.setSendFileLastDir(
                    selectedFile.getParent());
                messageWindow.getCurrentChatPanel().sendFile(selectedFile);
            }
        }
        else if (buttonText.equals("history"))
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
        else if (buttonText.equals("invite"))
        {
            ChatInviteDialog inviteDialog = new ChatInviteDialog(chatPanel);

            inviteDialog.setVisible(true);
        }
        else if (buttonText.equals("options"))
        {
            GuiActivator.getUIService().setConfigurationWindowVisible(true);
        }
    }

    /**
     * Returns the button used to show the history window.
     *
     * @return the button used to show the history window.
     */
    public ChatToolbarButton getHistoryButton()
    {
        return historyButton;
    }

    /**
     * Disables/Enables history arrow buttons depending on whether the
     * current page is the first, the last page or a middle page.
     */
    public void changeHistoryButtonsState(ChatPanel chatPanel)
    {
        ChatConversationPanel convPanel = chatPanel.getChatConversationPanel();

        long firstMsgInHistory = chatPanel.getFirstHistoryMsgTimestamp();
        long lastMsgInHistory = chatPanel.getLastHistoryMsgTimestamp();
        Date firstMsgInPage = convPanel.getPageFirstMsgTimestamp();
        Date lastMsgInPage = convPanel.getPageLastMsgTimestamp();

        if(firstMsgInHistory == 0 || lastMsgInHistory == 0)
        {
            previousButton.setEnabled(false);
            nextButton.setEnabled(false);
            return;
        }

        previousButton.setEnabled(firstMsgInHistory < firstMsgInPage.getTime());

        nextButton
            .setEnabled(
                (lastMsgInPage.getTime() > 0)
                    && (lastMsgInHistory > lastMsgInPage.getTime()));
    }

    /**
     * Enables or disables the conference button in this tool bar.
     *
     * @param isEnabled <code>true</code> if the conference button should be
     * enabled; <code>false</code>, otherwise.
     */
    public void enableInviteButton(boolean isEnabled)
    {
        inviteButton.setEnabled(isEnabled);
    }

    /**
     * Enables or disables the send file button in this tool bar.
     *
     * @param isEnabled <code>true</code> if the send file button should be
     * enabled; <code>false</code>, otherwise.
     */
    public void enableSendFileButton(boolean isEnabled)
    {
        sendFileButton.setEnabled(isEnabled);
    }

    /**
     * Sets the current <tt>ChatSession</tt> made known to this instance by the
     * last call to its {@link #chatChanged(ChatPanel)}.
     * 
     * @param chatSession the <tt>ChatSession</tt> to become current for this
     * instance
     */
    private void setChatSession(ChatSession chatSession)
    {
        if (this.chatSession != chatSession)
        {
            if (this.chatSession instanceof MetaContactChatSession)
                this.chatSession.removeChatTransportChangeListener(this);

            this.chatSession = chatSession;

            if (this.chatSession instanceof MetaContactChatSession)
                this.chatSession.addChatTransportChangeListener(this);
        }
    }
}
