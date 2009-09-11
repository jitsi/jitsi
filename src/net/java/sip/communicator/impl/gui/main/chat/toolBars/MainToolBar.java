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
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

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
    implements  ActionListener,
                PluginComponentListener
{
    private Logger logger = Logger.getLogger(MainToolBar.class);

    private ChatToolbarButton inviteButton = new ChatToolbarButton(
                ImageLoader.getImage(ImageLoader.ADD_TO_CHAT_ICON));

    private ChatToolbarButton historyButton = new ChatToolbarButton(
        ImageLoader.getImage(ImageLoader.HISTORY_ICON));

    private ChatToolbarButton optionsButton = new ChatToolbarButton(
        ImageLoader.getImage(ImageLoader.CHAT_CONFIGURE_ICON));

    private ChatToolbarButton sendFileButton
        = new ChatToolbarButton(
                ImageLoader.getImage(ImageLoader.SEND_FILE_ICON));

    private ChatToolbarButton previousButton = new ChatToolbarButton(
        ImageLoader.getImage(ImageLoader.PREVIOUS_ICON));

    private ChatToolbarButton nextButton = new ChatToolbarButton(
        ImageLoader.getImage(ImageLoader.NEXT_ICON));

    private ChatWindow messageWindow;

    /**
     * Empty constructor to be used from inheritors.
     */
    public MainToolBar()
    {
    }

    /**
     * Creates an instance and constructs the <tt>MainToolBar</tt>.
     *
     * @param messageWindow The parent <tt>ChatWindow</tt>.
     */
    public MainToolBar(ChatWindow messageWindow)
    {
        this.messageWindow = messageWindow;

        this.setOpaque(false);

        this.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 0));

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

        this.optionsButton.setName("options");
        this.optionsButton.setToolTipText(
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
        this.optionsButton.addActionListener(this);
        this.sendFileButton.addActionListener(this);
        this.previousButton.addActionListener(this);
        this.nextButton.addActionListener(this);

        this.initPluginComponents();
        
        this.messageWindow.addChatChangeListener(new ChatChangeListener()
        {
            public void chatChanged(ChatPanel panel)
            {
                if (panel == null)
                    return;
                
                MetaContact contact =
                    GuiActivator.getUIService().getChatContact(panel);

                for (Component c : getComponents())
                {
                    if (!(c instanceof PluginComponent))
                        continue;
                    
                    ((PluginComponent)c).setCurrentContact(contact);
                }
                
                if (panel.chatSession != null)
                {
                    panel.chatSession
                        .addChatTransportChangeListener(new ChatSessionChangeListener()
                        {
                            public void currentChatTransportChanged(
                                ChatSession chatSession)
                            {
                                if (chatSession == null)
                                    return;

                                ChatTransport currentTransport =
                                    chatSession.getCurrentChatTransport();
                                if (currentTransport.getDescriptor() instanceof Contact)
                                {
                                    Contact contact =
                                        (Contact) currentTransport
                                            .getDescriptor();
                                    for (Component c : getComponents())
                                    {
                                        if (!(c instanceof PluginComponent))
                                            continue;

                                        ((PluginComponent) c)
                                            .setCurrentContact(contact);
                                    }
                                }
                            }
                        });
                }
            }
        });
    }

    /**
     * Runs clean-up for associated resources which need explicit disposal (e.g.
     * listeners keeping this instance alive because they were added to the
     * model which operationally outlives this instance).
     */
    public void dispose()
    {
        GuiActivator.getUIService().removePluginComponentListener(this);
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
            JFileChooser fileChooser = new JFileChooser(
                ConfigurationManager.getSendFileLastDir());

            int result = fileChooser.showOpenDialog(this);

            if (result == JFileChooser.APPROVE_OPTION)
            {
                File selectedFile = fileChooser.getSelectedFile();

                ConfigurationManager
                    .setSendFileLastDir(selectedFile.getParent());
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
            ChatInviteDialog inviteDialog
                = new ChatInviteDialog(chatPanel);

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

        if(firstMsgInHistory < firstMsgInPage.getTime())
            previousButton.setEnabled(true);
        else
            previousButton.setEnabled(false);

        if(lastMsgInPage.getTime() > 0
                && (lastMsgInHistory > lastMsgInPage.getTime()))
        {
            nextButton.setEnabled(true);
        }
        else
        {
            nextButton.setEnabled(false);
        }
    }

    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + Container.CONTAINER_ID
            + "="+Container.CONTAINER_CHAT_TOOL_BAR.getID()+")";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                PluginComponent.class.getName(),
                osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            logger.error("Could not obtain plugin reference.", exc);
        }

        if (serRefs != null)
        {
            for (int i = 0; i < serRefs.length; i ++)
            {
                PluginComponent component = (PluginComponent) GuiActivator
                    .bundleContext.getService(serRefs[i]);

                this.add((Component)component.getComponent());

                this.revalidate();
                this.repaint();
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Implements the <code>PluginComponentListener.pluginComponentAdded</code>
     * method.
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if(c.getContainer().equals(Container.CONTAINER_CHAT_TOOL_BAR))
        {
            this.add((Component) c.getComponent());

            this.revalidate();
            this.repaint();
        }
    }

    /**
     * Implements the <code>PluginComponentListener.pluginComponentRemoved</code>
     * method.
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if(c.getContainer().equals(Container.CONTAINER_CHAT_TOOL_BAR))
        {
            this.remove((Component) c.getComponent());
        }
    }

    /**
     * Enables or disables the conference button in this tool bar.
     *
     * @param isEnabled <code>true</code> if the conference button should be
     * enabled, <code>false</code> - otherwise.
     */
    public void enableInviteButton(boolean isEnabled)
    {
        inviteButton.setEnabled(isEnabled);
    }

    /**
     * Enables or disables the send file button in this tool bar.
     *
     * @param isEnabled <code>true</code> if the send file button should be
     * enabled, <code>false</code> - otherwise.
     */
    public void enableSendFileButton(boolean isEnabled)
    {
        sendFileButton.setEnabled(isEnabled);
    }
}
