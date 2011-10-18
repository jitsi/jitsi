/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;

import javax.swing.*;

/**
 *
 * @author Yana Stamcheva
 */
public interface ChatContainer
{
    /**
     * Sets the title of this chat container.
     *
     * @param title the title to set
     */
    public void setTitle(String title);

    /**
     * Adds a given <tt>ChatPanel</tt> to this chat window.
     * 
     * @param chatPanel The <tt>ChatPanel</tt> to add.
     */
    public void addChat(ChatPanel chatPanel);

    /**
     * Removes a given <tt>ChatPanel</tt> from this chat window.
     * 
     * @param chatPanel The <tt>ChatPanel</tt> to remove.
     */
    public void removeChat(ChatPanel chatPanel);

    /**
     * Removes all chats this <tt>ChatContainer</tt> contains.
     */
    public void removeAllChats();

    /**
     * Returns the number of all open chats.
     * 
     * @return the number of all open chats
     */
    public int getChatCount();

    /**
     * Returns all chats this <tt>ChatContainer</tt> contains.
     *
     * @return a list of all chats this <tt>ChatContainer</tt> contains.
     */
    public java.util.List<ChatPanel> getChats();

    /**
     * Opens the specified <tt>ChatPanel</tt> and optinally brings it to the
     * front.
     *
     * @param chatPanel the <tt>ChatPanel</tt> to be opened
     * @param setSelected <tt>true</tt> if <tt>chatPanel</tt> (and respectively
     * this <tt>ChatContainer</tt>) should be brought to the front; otherwise,
     * <tt>false</tt>
     */
    public void openChat(ChatPanel chatPanel, final boolean setSelected);

    /**
     * Returns the currently selected chat panel.
     * 
     * @return the currently selected chat panel.
     */
    public ChatPanel getCurrentChat();

    /**
     * Selects the chat tab which corresponds to the given <tt>MetaContact</tt>.
     * 
     * @param chatPanel The <tt>ChatPanel</tt> to select.
     */
    public void setCurrentChat(ChatPanel chatPanel);

    /**
     * Sets the title of this chat container.
     *
     * @param chatPanel the chat, for which we set the title
     * @param title the title to set
     */
    public void setChatTitle(ChatPanel chatPanel, String title);

    /**
     * Sets the icon for the given chat.
     *
     * @param chatPanel the chat, for which we want to set an icon
     * @param icon the icon to set
     */
    public void setChatIcon(ChatPanel chatPanel, Icon icon);

    /**
     * Shows or hides the Toolbar depending on the value of parameter b. 
     * 
     * @param b if true, makes the Toolbar visible, otherwise hides the Toolbar
     */
    public void setToolbarVisible(boolean b);

    /**
     * Returns the frame to which this container belongs.
     *
     * @return the frame to which this container belongs
     */
    public Frame getFrame();

    /**
     * Adds the given <tt>ChatChangeListener</tt>.
     *
     * @param listener the listener to add
     */
    public void addChatChangeListener(ChatChangeListener listener);

    /**
     * Removes the given <tt>ChatChangeListener</tt>.
     *
     * @param listener the listener to remove
     */
    public void removeChatChangeListener(ChatChangeListener listener);
}
