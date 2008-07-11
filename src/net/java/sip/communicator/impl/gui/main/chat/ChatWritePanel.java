/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.undo.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.chat.menus.*;
import net.java.sip.communicator.impl.gui.main.chat.toolBars.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>ChatWritePanel</tt> is the panel, where user writes her messages.
 * It is located at the bottom of the split in the <tt>ChatPanel</tt> and it
 * contains an editor, where user writes the text.
 * 
 * @author Yana Stamcheva
 */
public class ChatWritePanel
    extends JPanel
    implements  UndoableEditListener,
                KeyListener,
                MouseListener
{
    private Logger logger = Logger.getLogger(ChatWritePanel.class);

    private JEditorPane editorPane = new JEditorPane();

    private UndoManager undo = new UndoManager();

    private ChatPanel chatPanel;

    private Timer stoppedTypingTimer =
        new Timer(2 * 1000, new Timer1ActionListener());

    private Timer typingTimer = new Timer(5 * 1000, new Timer2ActionListener());

    private int typingState = -1;

    private HTMLEditorKit htmlEditor = new HTMLEditorKit();

    private WritePanelRightButtonMenu rightButtonMenu;

    private EditTextToolBar editTextToolBar;
    
    private JScrollPane scrollPane = new JScrollPane();


    /**
     * Creates an instance of <tt>ChatWritePanel</tt>.
     * 
     * @param panel The parent <tt>ChatPanel</tt>.
     */
    public ChatWritePanel(ChatPanel panel)
    {

        super(new BorderLayout());

        this.chatPanel = panel;

        this.editTextToolBar = new EditTextToolBar(editorPane);

        this.add(editTextToolBar, BorderLayout.NORTH);
        this.add(scrollPane, BorderLayout.CENTER);

        this.rightButtonMenu =
            new WritePanelRightButtonMenu(chatPanel.getChatWindow());

        this.scrollPane
            .setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.editorPane.setContentType("text/html");
        this.editorPane.setFont(Constants.FONT);
        this.editorPane.setCaretPosition(0);
        this.editorPane.setEditorKit(htmlEditor);
        this.editorPane.getDocument().addUndoableEditListener(this);
        this.editorPane.addKeyListener(this);
        this.editorPane.addMouseListener(this);

        this.scrollPane.getViewport().add(editorPane, BorderLayout.CENTER);

        this.scrollPane.setBorder(BorderFactory
            .createCompoundBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3),
                SIPCommBorders.getBoldRoundBorder()));

        this.scrollPane.getVerticalScrollBar().setUnitIncrement(30);

        this.typingTimer.setRepeats(true);

        // initialize send command to Ctrl+Enter
        ConfigurationService configService =
            GuiActivator.getConfigurationService();

        String messageCommand =
            configService
                .getString("net.java.sip.communicator.impl.gui.sendMessageCommand");

        if (messageCommand == null || messageCommand.equalsIgnoreCase("enter"))
            this.changeSendCommand(true);
        else
            this.changeSendCommand(false);
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
     */
    public void changeSendCommand(boolean isEnter)
    {
        this.editorPane.getActionMap().put("send", new SendMessageAction());
        this.editorPane.getActionMap().put("newLine", new NewLineAction());

        InputMap im = this.editorPane.getInputMap();

        if (isEnter)
        {
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                KeyEvent.CTRL_DOWN_MASK), "newLine");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                KeyEvent.SHIFT_DOWN_MASK), "newLine");

            chatPanel.getChatSendPanel().getSendButton().setToolTipText(
                "<html>" + Messages.getI18NString("sendMessage").getText()
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
                    Messages.getI18NString("sendMessage").getText()
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

    public void keyTyped(KeyEvent e)
    {
    }

    /**
     * When CTRL+Z is pressed invokes the <code>ChatWritePanel.undo()</code>
     * method, when CTRL+R is pressed invokes the
     * <code>ChatWritePanel.redo()</code> method.
     * <p>
     * Sends typing notifications when user types.
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
        else if (ConfigurationManager.isSendTypingNotifications()
            && e.getKeyCode() != KeyEvent.VK_ESCAPE)
        {
            if (typingState != OperationSetTypingNotifications.STATE_TYPING)
            {
                stoppedTypingTimer.setDelay(2 * 1000);
                typingState = OperationSetTypingNotifications.STATE_TYPING;

                int result = chatPanel.sendTypingNotification(typingState);

                if (result == ChatPanel.TYPING_NOTIFICATION_SUCCESSFULLY_SENT)
                    typingTimer.start();
            }

            if (!stoppedTypingTimer.isRunning())
                stoppedTypingTimer.start();
            else
                stoppedTypingTimer.restart();
        }
    }

    public void keyReleased(KeyEvent e)
    {
    }

    /**
     * Listens for <code>stoppedTypingTimer</tt> events.
     */
    private class Timer1ActionListener
        implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            typingTimer.stop();
            if (typingState == OperationSetTypingNotifications.STATE_TYPING)
            {
                try
                {
                    typingState = OperationSetTypingNotifications.STATE_PAUSED;

                    int result = chatPanel.sendTypingNotification(typingState);

                    if (result == ChatPanel.TYPING_NOTIFICATION_SUCCESSFULLY_SENT)
                        stoppedTypingTimer.setDelay(3 * 1000);
                }
                catch (Exception ex)
                {
                    logger.error("Failed to send typing notifications.", ex);
                }
            }
            else if (typingState == OperationSetTypingNotifications.STATE_PAUSED)
            {
                stopTypingTimer();
            }
        }
    }

    /**
     * Listens for <code>typingTimer</tt> events.
     */
    private class Timer2ActionListener
        implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            if (typingState == OperationSetTypingNotifications.STATE_TYPING)
            {
                chatPanel
                    .sendTypingNotification(OperationSetTypingNotifications.STATE_TYPING);
            }
        }
    }

    /**
     * Stops the timer and sends a notification message.
     */
    public void stopTypingTimer()
    {
        typingState = OperationSetTypingNotifications.STATE_STOPPED;

        int result = chatPanel.sendTypingNotification(typingState);

        if (result == ChatPanel.TYPING_NOTIFICATION_SUCCESSFULLY_SENT)
            stoppedTypingTimer.stop();
    }

    /**
     * Opens the <tt>WritePanelRightButtonMenu</tt> whe user clicks with the
     * right mouse button on the editor area.
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

    public void mousePressed(MouseEvent e)
    {
    }

    public void mouseReleased(MouseEvent e)
    {
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

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

        String formattedString = extractFormattedText(msgText);

        return formattedString
            .substring(0, formattedString.lastIndexOf("\n"));
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
     * Return all html paragraph content separated by <BR/> tags.
     * 
     * @param msgText the html text.
     * @return the string containing only paragraph content.
     */
    private String extractFormattedText(String msgText)
    {
        int firstIndex = msgText.indexOf("<p");
        
        if (firstIndex != -1)
        {
            int lastIndex = msgText.indexOf("</p>", firstIndex);

            if (lastIndex < 0)
                lastIndex = msgText.length();

            int firstTagClosureIndex = msgText.indexOf('>', firstIndex);
            String pString = msgText
                .substring(firstTagClosureIndex+1, lastIndex).trim();

            return pString + "\n"
                + extractFormattedText(msgText.substring(lastIndex+1));
        }

        return "";
    }
}
