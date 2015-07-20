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
package net.java.sip.communicator.service.gui;

import java.awt.event.*;
import java.util.*;

import javax.swing.event.*;
import javax.swing.text.*;

import net.java.sip.communicator.service.gui.event.*;

/**
 * The <tt>Chat</tt> interface is meant to be implemented by the GUI component
 * class representing a chat. Through the <i>isChatFocused</i> method the other
 * bundles could check the visibility of the chat component. The
 * <tt>ChatFocusListener</tt> is used to inform other bundles when a chat has
 * changed its focus state.
 *
 * @author Yana Stamcheva
 */
public interface Chat
{
    /**
     * The message type representing outgoing messages.
     */
    public static final String OUTGOING_MESSAGE = "OutgoingMessage";
    /**
     * The message type representing incoming messages.
     */
    public static final String INCOMING_MESSAGE = "IncomingMessage";
    /**
     * The message type representing status messages.
     */
    public static final String STATUS_MESSAGE = "StatusMessage";
    /**
     * The message type representing action messages. These are message specific
     * for IRC, but could be used in other protocols also.
     */
    public static final String ACTION_MESSAGE = "ActionMessage";
    /**
     * The message type representing system messages.
     */
    public static final String SYSTEM_MESSAGE = "SystemMessage";
    /**
     * The message type representing sms messages.
     */
    public static final String SMS_MESSAGE = "SmsMessage";
    /**
     * The message type representing error messages.
     */
    public static final String ERROR_MESSAGE = "ErrorMessage";
    /**
     * The history incoming message type.
     */
    public static final String HISTORY_INCOMING_MESSAGE = "HistoryIncomingMessage";
    /**
     * The history outgoing message type.
     */
    public static final String HISTORY_OUTGOING_MESSAGE = "HistoryOutgoingMessage";
    /**
     * The size of the buffer that indicates how many messages will be stored
     * in the conversation area in the chat window.
     */
    public static final int CHAT_BUFFER_SIZE = 50000;

    /**
     * Checks if this <tt>Chat</tt> is currently focused.
     *
     * @return TRUE if the chat is focused, FALSE - otherwise
     */
    public boolean isChatFocused();

    /**
     * Returns the message written by user in the chat write area.
     *
     * @return the message written by user in the chat write area
     */
    public String getMessage();

    /**
     * Bring this chat to front if <tt>b</tt> is true, hide it otherwise.
     *
     * @param isVisible tells if the chat will be made visible or not.
     */
    public void setChatVisible(boolean isVisible);

    /**
     * Sets the given message as a message in the chat write area.
     *
     * @param message the text that would be set to the chat write area
     */
    public void setMessage(String message);

    /**
     * Adds the given <tt>ChatFocusListener</tt> to this <tt>Chat</tt>.
     * The <tt>ChatFocusListener</tt> is used to inform other bundles when a
     * chat has changed its focus state.
     *
     * @param l the <tt>ChatFocusListener</tt> to add
     */
    public void addChatFocusListener(ChatFocusListener l);

    /**
     * Removes the given <tt>ChatFocusListener</tt> from this <tt>Chat</tt>.
     * The <tt>ChatFocusListener</tt> is used to inform other bundles when a
     * chat has changed its focus state.
     *
     * @param l the <tt>ChatFocusListener</tt> to remove
     */
    public void removeChatFocusListener(ChatFocusListener l);

    /**
     * Adds the given {@link KeyListener} to this <tt>Chat</tt>.
     * The <tt>KeyListener</tt> is used to inform other bundles when a user has
     * typed in the chat editor area.
     *
     * @param l the <tt>KeyListener</tt> to add
     */
    public void addChatEditorKeyListener(KeyListener l);

    /**
     * Removes the given {@link KeyListener} from this <tt>Chat</tt>.
     * The <tt>KeyListener</tt> is used to inform other bundles when a user has
     * typed in the chat editor area.
     *
     * @param l the <tt>ChatFocusListener</tt> to remove
     */
    public void removeChatEditorKeyListener(KeyListener l);

    /**
     * Adds the given {@link ChatMenuListener} to this <tt>Chat</tt>.
     * The <tt>ChatMenuListener</tt> is used to determine menu elements
     * that should be added on right clicks.
     *
     * @param l the <tt>ChatMenuListener</tt> to add
     */
    public void addChatEditorMenuListener(ChatMenuListener l);

    /**
     * Adds the given {@link CaretListener} to this <tt>Chat</tt>.
     * The <tt>CaretListener</tt> is used to inform other bundles when a user has
     * moved the caret in the chat editor area.
     *
     * @param l the <tt>CaretListener</tt> to add
     */
    public void addChatEditorCaretListener(CaretListener l);

    /**
     * Adds the given {@link DocumentListener} to this <tt>Chat</tt>.
     * The <tt>DocumentListener</tt> is used to inform other bundles when a user has
     * modified the document in the chat editor area.
     *
     * @param l the <tt>DocumentListener</tt> to add
     */
    public void addChatEditorDocumentListener(DocumentListener l);

     /**
     * Removes the given {@link ChatMenuListener} to this <tt>Chat</tt>.
     * The <tt>ChatMenuListener</tt> is used to determine menu elements
     * that should be added on right clicks.
     *
     * @param l the <tt>ChatMenuListener</tt> to add
     */
    public void removeChatEditorMenuListener(ChatMenuListener l);

    /**
     * Removes the given {@link CaretListener} from this <tt>Chat</tt>.
     * The <tt>CaretListener</tt> is used to inform other bundles when a user has
     * moved the caret in the chat editor area.
     *
     * @param l the <tt>CaretListener</tt> to remove
     */
    public void removeChatEditorCaretListener(CaretListener l);

     /**
     * Removes the given {@link DocumentListener} from this <tt>Chat</tt>.
     * The <tt>DocumentListener</tt> is used to inform other bundles when a user has
     * modified the document in the chat editor area.
     *
     * @param l the <tt>DocumentListener</tt> to remove
     */
    public void removeChatEditorDocumentListener(DocumentListener l);

    /**
     * Adds a message to this <tt>Chat</tt>.
     *
     * @param contactName the name of the contact sending the message
     * @param date the time at which the message is sent or received
     * @param messageType the type of the message
     * @param message the message text
     * @param contentType the content type
     */
    public void addMessage(String contactName, Date date, String messageType,
        String message, String contentType);

    /**
     * Adds a new ChatLinkClickedListener. The callback is called for every
     * link whose scheme is <tt>jitsi</tt>. It is the callback's responsibility
     * to filter the action based on the URI.
     *
     * Example:<br>
     * <tt>jitsi://classname/action?query</tt><br>
     * Use the name of the registering class as the host, the action to execute
     * as the path and any parameters as the query.
     *
     * @param listener callback that is notified when a link was clicked.
     */
    public void addChatLinkClickedListener(ChatLinkClickedListener listener);

    /**
     * Removes an existing ChatLinkClickedListener
     *
     * @param listener the already registered listener to remove.
     */
    public void removeChatLinkClickedListener(ChatLinkClickedListener listener);

    /**
     * Provides the {@link Highlighter} used in rendering the chat editor.
     *
     * @return highlighter used to render message being composed
     */
    public Highlighter getHighlighter();

    /**
     * Gets the caret position in the chat editor.
     * @return index of caret in message being composed
     */
    public int getCaretPosition();

    /**
     * Causes the chat to validate its appearance (suggests a repaint operation
     * may be necessary).
     */
    public void promptRepaint();
}
