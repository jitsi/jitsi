/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.undo.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.chat.menus.*;
import net.java.sip.communicator.impl.gui.main.chat.toolBars.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>ChatWritePanel</tt> is the panel, where user writes her messages.
 * It is located at the bottom of the split in the <tt>ChatPanel</tt> and it
 * contains an editor, where user writes the text.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class ChatWritePanel
    extends TransparentPanel
    implements  ActionListener,
                KeyListener,
                MouseListener,
                UndoableEditListener,
                Skinnable
{
    private final Logger logger = Logger.getLogger(ChatWritePanel.class);

    private JEditorPane editorPane = new JEditorPane();

    private UndoManager undo = new UndoManager();

    private final ChatPanel chatPanel;

    private final Timer stoppedTypingTimer = new Timer(2 * 1000, this);

    private final Timer typingTimer = new Timer(5 * 1000, this);

    private int typingState = -1;

    private WritePanelRightButtonMenu rightButtonMenu;

    private EditTextToolBar editTextToolBar;

    private ArrayList<ChatMenuListener> menuListeners
        = new ArrayList<ChatMenuListener>();

    /**
     * Creates an instance of <tt>ChatWritePanel</tt>.
     *
     * @param panel The parent <tt>ChatPanel</tt>.
     */
    public ChatWritePanel(ChatPanel panel)
    {
        super(new BorderLayout());
        SCScrollPane scrollPane = new SCScrollPane();

        this.chatPanel = panel;

        this.editorPane.setContentType("text/html");

        this.editorPane.putClientProperty(
            JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        this.editorPane.setCaretPosition(0);
        this.editorPane.setEditorKit(new SIPCommHTMLEditorKit(this));
        this.editorPane.getDocument().addUndoableEditListener(this);
        this.editorPane.addKeyListener(this);
        this.editorPane.addMouseListener(this);
        this.editorPane.setCursor(
            Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        this.editorPane.setDragEnabled(true);
        this.editorPane.setTransferHandler(new ChatTransferHandler(chatPanel));

        this.editTextToolBar = new EditTextToolBar(this);
        this.editTextToolBar.setVisible(
            ConfigurationManager.isChatStylebarVisible());

        this.add(editTextToolBar, BorderLayout.NORTH);
        this.add(scrollPane, BorderLayout.CENTER);

        this.rightButtonMenu =
            new WritePanelRightButtonMenu(chatPanel.getChatContainer());

        scrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, Color.GRAY),
            BorderFactory.createEmptyBorder(0, 2, 0, 2)));

        scrollPane.setViewportView(editorPane);

        this.typingTimer.setRepeats(true);

        // initialize send command to Ctrl+Enter
        ConfigurationService configService =
            GuiActivator.getConfigurationService();

        String messageCommandProperty =
            "service.gui.SEND_MESSAGE_COMMAND";
        String messageCommand = configService.getString(messageCommandProperty);

        if(messageCommand == null)
            messageCommand =
                GuiActivator.getResources().
                    getSettingsString(messageCommandProperty);

        this.changeSendCommand((messageCommand == null || messageCommand
            .equalsIgnoreCase("enter")));
    }

    /**
     * Shows or hides the Stylebar depending on the value of parameter b.
     *
     * @param b if true, makes the Stylebar visible, otherwise hides the Stylebar
     */
    public void setStylebarVisible(boolean b)
    {
        this.editTextToolBar.setVisible(b);
    }

    /**
     * Runs clean-up for associated resources which need explicit disposal (e.g.
     * listeners keeping this instance alive because they were added to the
     * model which operationally outlives this instance).
     */
    public void dispose()
    {

        /*
         * Stop the Timers because they're implicitly globally referenced and
         * thus don't let them retain this instance.
         */
        typingTimer.stop();
        typingTimer.removeActionListener(this);
        stoppedTypingTimer.stop();
        stoppedTypingTimer.removeActionListener(this);
        if (typingState != OperationSetTypingNotifications.STATE_STOPPED)
        {
            stopTypingTimer();
        }
    }

    /**
     * Returns the editor panel, contained in this <tt>ChatWritePanel</tt>.
     *
     * @return The editor panel, contained in this <tt>ChatWritePanel</tt>.
     */
    public JEditorPane getEditorPane()
    {
        return editorPane;
    }

    /**
     * Replaces the Ctrl+Enter send command with simple Enter.
     *
     * @param isEnter indicates if the new send command is enter or cmd-enter
     */
    public void changeSendCommand(boolean isEnter)
    {
        ActionMap actionMap = editorPane.getActionMap();
        actionMap.put("send", new SendMessageAction());
        actionMap.put("newLine", new NewLineAction());

        InputMap im = this.editorPane.getInputMap();

        if (isEnter)
        {
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                KeyEvent.CTRL_DOWN_MASK), "newLine");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                KeyEvent.SHIFT_DOWN_MASK), "newLine");

            chatPanel.getChatSendPanel().getSendButton().setToolTipText(
                "<html>"
                    + GuiActivator.getResources()
                        .getI18NString("service.gui.SEND_MESSAGE")
                    + " - Enter <br> "
                    + "Use Ctrl-Enter or Shift-Enter to make a new line"
                    + "</html>");
        }
        else
        {
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                KeyEvent.CTRL_DOWN_MASK), "send");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "newLine");

            chatPanel.getChatSendPanel().getSendButton()
                .setToolTipText(
                    GuiActivator.getResources()
                        .getI18NString("service.gui.SEND_MESSAGE")
                        + " Ctrl-Enter");
        }
    }

    /**
     * The <tt>SendMessageAction</tt> is an <tt>AbstractAction</tt> that
     * sends the text that is currently in the write message area.
     */
    private class SendMessageAction
        extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            // chatPanel.stopTypingNotifications();
            chatPanel.sendButtonDoClick();
        }
    }

    /**
     * The <tt>NewLineAction</tt> is an <tt>AbstractAction</tt> that types
     * an enter in the write message area.
     */
    private class NewLineAction
        extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            int caretPosition = editorPane.getCaretPosition();
            HTMLDocument doc = (HTMLDocument) editorPane.getDocument();

            try
            {
                doc.insertString(caretPosition, "\n", null);
            }
            catch (BadLocationException e1)
            {
                logger.error("Could not insert <br> to the document.", e1);
            }

            editorPane.setCaretPosition(caretPosition + 1);
        }
    }

    /**
     * Handles the <tt>UndoableEditEvent</tt>, by adding the content edit to
     * the <tt>UndoManager</tt>.
     *
     * @param e The <tt>UndoableEditEvent</tt>.
     */
    public void undoableEditHappened(UndoableEditEvent e)
    {
        this.undo.addEdit(e.getEdit());
    }

    /**
     * Implements the undo operation.
     */
    private void undo()
    {
        try
        {
            undo.undo();
        }
        catch (CannotUndoException e)
        {
            logger.error("Unable to undo.", e);
        }
    }

    /**
     * Implements the redo operation.
     */
    private void redo()
    {
        try
        {
            undo.redo();
        }
        catch (CannotRedoException e)
        {
            logger.error("Unable to redo.", e);
        }
    }

    /**
     * Sends typing notifications when user types.
     *
     * @param e the event.
     */
    public void keyTyped(KeyEvent e)
    {
        if (ConfigurationManager.isSendTypingNotifications())
        {
            if (typingState != OperationSetTypingNotifications.STATE_TYPING)
            {
                stoppedTypingTimer.setDelay(2 * 1000);
                typingState = OperationSetTypingNotifications.STATE_TYPING;

                int result = chatPanel.getChatSession()
                    .getCurrentChatTransport()
                        .sendTypingNotification(typingState);

                if (result == ChatPanel.TYPING_NOTIFICATION_SUCCESSFULLY_SENT)
                    typingTimer.start();
            }

            if (!stoppedTypingTimer.isRunning())
                stoppedTypingTimer.start();
            else
                stoppedTypingTimer.restart();
        }
    }

    /**
     * When CTRL+Z is pressed invokes the <code>ChatWritePanel.undo()</code>
     * method, when CTRL+R is pressed invokes the
     * <code>ChatWritePanel.redo()</code> method.
     *
     * @param e the <tt>KeyEvent</tt> that notified us
     */
    public void keyPressed(KeyEvent e)
    {
        if ((e.getModifiers() & KeyEvent.CTRL_MASK) == KeyEvent.CTRL_MASK
            && (e.getKeyCode() == KeyEvent.VK_Z))
        {
            if (undo.canUndo())
                undo();
        }
        else if ((e.getModifiers() & KeyEvent.CTRL_MASK) == KeyEvent.CTRL_MASK
            && (e.getKeyCode() == KeyEvent.VK_R))
        {
            if (undo.canRedo())
                redo();
        }
    }

    public void keyReleased(KeyEvent e) {}

    /**
     * Performs actions when typing timer has expired.
     *
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();

        if (typingTimer.equals(source))
        {
            if (typingState == OperationSetTypingNotifications.STATE_TYPING)
            {
                chatPanel.getChatSession().getCurrentChatTransport()
                    .sendTypingNotification(
                        OperationSetTypingNotifications.STATE_TYPING);
            }
        }
        else if (stoppedTypingTimer.equals(source))
        {
            typingTimer.stop();
            if (typingState == OperationSetTypingNotifications.STATE_TYPING)
            {
                try
                {
                    typingState = OperationSetTypingNotifications.STATE_PAUSED;

                    int result = chatPanel.getChatSession()
                        .getCurrentChatTransport().
                            sendTypingNotification(typingState);

                    if (result
                            == ChatPanel.TYPING_NOTIFICATION_SUCCESSFULLY_SENT)
                        stoppedTypingTimer.setDelay(3 * 1000);
                }
                catch (Exception ex)
                {
                    logger.error("Failed to send typing notifications.", ex);
                }
            }
            else if (typingState
                        == OperationSetTypingNotifications.STATE_PAUSED)
            {
                stopTypingTimer();
            }
        }
    }

    /**
     * Stops the timer and sends a notification message.
     */
    public void stopTypingTimer()
    {
        typingState = OperationSetTypingNotifications.STATE_STOPPED;

        int result = chatPanel.getChatSession().getCurrentChatTransport()
            .sendTypingNotification(typingState);

        if (result == ChatPanel.TYPING_NOTIFICATION_SUCCESSFULLY_SENT)
            stoppedTypingTimer.stop();
    }

    /**
     * Opens the <tt>WritePanelRightButtonMenu</tt> when user clicks with the
     * right mouse button on the editor area.
     *
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    public void mouseClicked(MouseEvent e)
    {
        if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
            || (e.isControlDown() && !e.isMetaDown()))
        {
            Point p = e.getPoint();
            SwingUtilities.convertPointToScreen(p, e.getComponent());

            rightButtonMenu.setInvoker(editorPane);
            rightButtonMenu.setLocation(p.x, p.y);
            rightButtonMenu.setVisible(true);
        }
    }

    public void mousePressed(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    /**
     * Returns the <tt>WritePanelRightButtonMenu</tt> opened in this panel.
     * Used by the <tt>ChatWindow</tt>, when the ESC button is pressed, to
     * check if there is an open menu, which should be closed.
     *
     * @return the <tt>WritePanelRightButtonMenu</tt> opened in this panel
     */
    public WritePanelRightButtonMenu getRightButtonMenu()
    {
        return rightButtonMenu;
    }

    /**
     * Returns the write area text as an html text.
     *
     * @return the write area text as an html text.
     */
    public String getTextAsHtml()
    {
        String msgText = editorPane.getText();

        String formattedString = msgText.replaceAll(
            "<html>|<head>|<body>|</html>|</head>|</body>", "");

        formattedString = extractFormattedText(formattedString);

        if (formattedString.endsWith("<BR>"))
            formattedString = formattedString
            .substring(0, formattedString.lastIndexOf("<BR>"));

        return formattedString;
    }

    /**
     * Returns the write area text as a plain text without any formatting.
     *
     * @return the write area text as a plain text without any formatting.
     */
    public String getText()
    {
        try
        {
            Document doc = editorPane.getDocument();

            return doc.getText(0, doc.getLength());
        }
        catch (BadLocationException e)
        {
            logger.error("Could not obtain write area text.", e);
        }

        return null;
    }


    /**
     * Clears write message area.
     */
    public void clearWriteArea()
    {
        try
        {
            this.editorPane.getDocument()
                .remove(0, editorPane.getDocument().getLength());
        }
        catch (BadLocationException e)
        {
            logger.error("Failed to obtain write panel document content.", e);
        }
    }

    /**
     * Appends the given text to the end of the contained HTML document. This
     * method is used to insert smileys when user selects a smiley from the
     * menu.
     *
     * @param text the text to append.
     */
    public void appendText(String text)
    {
        HTMLDocument doc = (HTMLDocument) editorPane.getDocument();

        Element currentElement
            = doc.getCharacterElement(editorPane.getCaretPosition());

        try
        {
            doc.insertAfterEnd(currentElement, text);
        }
        catch (BadLocationException e)
        {
            logger.error("Insert in the HTMLDocument failed.", e);
        }
        catch (IOException e)
        {
            logger.error("Insert in the HTMLDocument failed.", e);
        }

        this.editorPane.setCaretPosition(doc.getLength());
    }

    /**
     * Return all html paragraph content separated by <BR/> tags.
     *
     * @param msgText the html text.
     * @return the string containing only paragraph content.
     */
    private String extractFormattedText(String msgText)
    {
        String resultString = msgText.replaceAll("<p\\b[^>]*>", "");

        return resultString.replaceAll("<\\/p>", "<BR>");
    }

    /**
     * Returns the toolbar above the chat write area.
     *
     * @return the toolbar above the chat write area.
     */
    public EditTextToolBar getEditTextToolBar()
    {
        return editTextToolBar;
    }

    /**
     * Adds the given {@link ChatMenuListener} to this <tt>Chat</tt>.
     * The <tt>ChatMenuListener</tt> is used to determine menu elements
     * that should be added on right clicks.
     *
     * @param l the <tt>ChatMenuListener</tt> to add
     */
    public void addChatEditorMenuListener(ChatMenuListener l)
    {
        this.menuListeners.add(l);
    }

    /**
     * Removes the given {@link ChatMenuListener} to this <tt>Chat</tt>.
     * The <tt>ChatMenuListener</tt> is used to determine menu elements
     * that should be added on right clicks.
     *
     * @param l the <tt>ChatMenuListener</tt> to add
     */
    public void removeChatEditorMenuListener(ChatMenuListener l)
    {
        this.menuListeners.remove(l);
    }

    /**
     * Reloads menu.
     */
    public void loadSkin()
    {
        getRightButtonMenu().loadSkin();
    }
}
