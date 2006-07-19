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
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.StyledEditorKit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import net.java.sip.communicator.impl.gui.main.message.menu.WritePanelRightButtonMenu;
import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications;
import net.java.sip.communicator.util.Logger;
/**
 * The <tt>ChatWritePanel</tt> is the panel, where user writes her messages.
 * It is located at the bottom of the split in the <tt>ChatPanel</tt> and
 * it contains an editor, where user writes the text.
 * 
 * @author Yana Stamcheva
 */
public class ChatWritePanel extends JScrollPane implements
        UndoableEditListener, KeyListener, MouseListener {

    private Logger logger = Logger.getLogger(ChatWritePanel.class);

    private JEditorPane editorPane = new JEditorPane();

    private UndoManager undo = new UndoManager();

    private ChatPanel chatPanel;

    private Timer stoppedTypingTimer = new Timer(2 * 1000,
            new Timer1ActionListener());

    private Timer typingTimer = new Timer(5 * 1000, new Timer2ActionListener());

    private int typingState = -1;

    private StyledEditorKit styledEditor = new StyledEditorKit();
    
    private WritePanelRightButtonMenu rightButtonMenu;
    
    /**
     * Creates an instance of <tt>ChatWritePanel</tt>.
     * @param chatPanel The parent <tt>ChatPanel</tt>.
     */
    public ChatWritePanel(ChatPanel chatPanel) {

        super();

        this.chatPanel = chatPanel;
        
        this.rightButtonMenu 
            = new WritePanelRightButtonMenu(chatPanel.getChatWindow());

        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        this.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.editorPane.setFont(Constants.FONT);
        
        this.editorPane.setEditorKit(styledEditor);
        this.editorPane.getDocument().addUndoableEditListener(this);
        this.editorPane.addKeyListener(this);
        this.editorPane.addMouseListener(this);

        this.getViewport().add(editorPane, BorderLayout.CENTER);

        this.getVerticalScrollBar().setUnitIncrement(30);

        this.typingTimer.setRepeats(true);
    }

    /**
     * Overrides the <code>javax.swing.JComponent.paint()</code> in order
     * to privide a round border.
     * @param g The Graphics object.
     */
    public void paint(Graphics g) {

        AntialiasingManager.activateAntialiasing(g);

        super.paint(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Constants.BLUE_GRAY_BORDER_COLOR);
        g2.setStroke(new BasicStroke(1.5f));

        g2.drawRoundRect(3, 3, this.getWidth() - 4, this.getHeight() - 4, 8, 8);
    }

    /**
     * Returns the editor panel, contained in this <tt>ChatWritePanel</tt>.
     * @return The editor panel, contained in this <tt>ChatWritePanel</tt>.
     */
    public JEditorPane getEditorPane() {
        return editorPane;
    }

    /**
     * Handles the <tt>UndoableEditEvent</tt>, by adding the content edit
     * to the <tt>UndoManager</tt>.
     * @param e The <tt>UndoableEditEvent</tt>.
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

    /**
     * When CTRL+Z is pressed invokes the <code>ChatWritePanel.undo()</code>
     * method, when CTRL+R is pressed invokes the
     * <code>ChatWritePanel.redo()</code> method.
     * <p>
     * Sends typing notifications when user types.
     */
    public void keyPressed(KeyEvent e) {
        if ((e.getModifiers() & KeyEvent.CTRL_MASK) == KeyEvent.CTRL_MASK
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
        else if (chatPanel.getChatWindow().isTypingNotificationEnabled()) {
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

    /**
     * Listens for <code>stoppedTypingTimer</tt> events.
     */
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

    /**
     * Listens for <code>typingTimer</tt> events.
     */
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

    /**
     * Opens the <tt>WritePanelRightButtonMenu</tt> whe user clicks with the
     * right mouse button on the editor area.
     */
    public void mouseClicked(MouseEvent e) {
        if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {            
            Point p = e.getPoint();
            SwingUtilities.convertPointToScreen(p, e.getComponent());
            
            rightButtonMenu.setInvoker(editorPane);
            rightButtonMenu.setLocation(p.x, p.y);
            rightButtonMenu.setVisible(true);
        }
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }   
}
