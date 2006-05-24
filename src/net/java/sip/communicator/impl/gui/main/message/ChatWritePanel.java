/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications;
import net.java.sip.communicator.util.Logger;
/**
 * The ChatWritePanel is the panel, where user write his messages.
 * @author Yana Stamcheva
 */
public class ChatWritePanel extends JScrollPane implements
        UndoableEditListener, KeyListener {

    private Logger logger = Logger.getLogger(ChatWritePanel.class);

    private JEditorPane editorPane = new JEditorPane();

    private UndoManager undo = new UndoManager();

    private ChatPanel chatPanel;

    private Timer stoppedTypingTimer = new Timer(2 * 1000,
            new Timer1ActionListener());

    private Timer typingTimer = new Timer(5 * 1000, new Timer2ActionListener());

    private int typingState = -1;

    /**
     * Creates an instance of ChatWritePanel.
     * @param chatPanel The parent ChatPanel.
     */
    public ChatWritePanel(ChatPanel chatPanel) {

        super();

        this.chatPanel = chatPanel;

        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        this.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.editorPane.getDocument().addUndoableEditListener(this);
        this.editorPane.addKeyListener(this);

        this.getViewport().add(editorPane, BorderLayout.CENTER);

        this.getVerticalScrollBar().setUnitIncrement(30);

        this.typingTimer.setRepeats(true);
    }

    /**
     * Overrides the javax.swing.JComponent.paint() in order
     * to privide a round border.
     * @param g The Graphics object.
     */
    public void paint(Graphics g) {

        AntialiasingManager.activateAntialiasing(g);

        super.paint(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Constants.MSG_WINDOW_BORDER_COLOR);
        g2.setStroke(new BasicStroke(1.5f));

        g2.drawRoundRect(3, 3, this.getWidth() - 4, this.getHeight() - 4, 8, 8);
    }

    /**
     * Returns the editor panel in this panel.
     * @return The editor panel in this panel.
     */
    public JEditorPane getEditorPane() {
        return editorPane;
    }

    /**
     * @param e The UndoableEditEvent.
     */
    public void undoableEditHappened(UndoableEditEvent e) {
        this.undo.addEdit(e.getEdit());
    }

    /**
     * Implements the undo operation.
     */
    private void undo() {
        try {
            undo.undo();
        } catch (CannotUndoException e) {
            logger.error("Unable to undo.", e);
        }
    }

    /**
     * Implements the redo operation.
     */
    private void redo() {
        try {
            undo.redo();
        } catch (CannotRedoException e) {
            logger.error("Unable to redo.", e);
        }
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        if ((e.getModifiers() & KeyEvent.CTRL_MASK) == KeyEvent.CTRL_MASK
                && (e.getKeyCode() == KeyEvent.VK_ENTER)) {
            stopTypingTimer();

            chatPanel.sendMessage();
        }
        else if ((e.getModifiers() & KeyEvent.CTRL_MASK) == KeyEvent.CTRL_MASK
                && (e.getKeyCode() == KeyEvent.VK_Z)) {

            if (undo.canUndo()) {
                undo();
            }
        }
        else if ((e.getModifiers() & KeyEvent.CTRL_MASK) == KeyEvent.CTRL_MASK
                && (e.getKeyCode() == KeyEvent.VK_R)) {

            if (undo.canRedo()) {
                redo();
            }
        }
        else {
            if (typingState != OperationSetTypingNotifications.STATE_TYPING) {
                stoppedTypingTimer.setDelay(2 * 1000);
                typingState = OperationSetTypingNotifications.STATE_TYPING;
                chatPanel.getTnOperationSet().sendTypingNotification(
                        chatPanel.getProtocolContact(), typingState);
                typingTimer.start();
            }

            if (!stoppedTypingTimer.isRunning()) {
                stoppedTypingTimer.start();
            } else {
                stoppedTypingTimer.restart();
            }
        }
    }

    public void keyReleased(KeyEvent e) {
    }

    private class Timer1ActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            typingTimer.stop();
            if (typingState == OperationSetTypingNotifications.STATE_TYPING) {
                chatPanel.getTnOperationSet().sendTypingNotification(
                        chatPanel.getProtocolContact(),
                        OperationSetTypingNotifications.STATE_PAUSED);
                typingState = OperationSetTypingNotifications.STATE_PAUSED;
                stoppedTypingTimer.setDelay(3 * 1000);
            } else if (typingState 
                    == OperationSetTypingNotifications.STATE_PAUSED) {
                stopTypingTimer();
            }
        }
    }

    private class Timer2ActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (typingState == OperationSetTypingNotifications.STATE_TYPING) {
                chatPanel.getTnOperationSet().sendTypingNotification(
                        chatPanel.getProtocolContact(),
                        OperationSetTypingNotifications.STATE_TYPING);
            }
        }
    }

    /**
     * Stops the timer and sends a notification message.
     */
    public void stopTypingTimer() {
        chatPanel.getTnOperationSet().sendTypingNotification(
                chatPanel.getProtocolContact(),
                OperationSetTypingNotifications.STATE_STOPPED);
        typingState = OperationSetTypingNotifications.STATE_STOPPED;
        stoppedTypingTimer.stop();
    }   
}
