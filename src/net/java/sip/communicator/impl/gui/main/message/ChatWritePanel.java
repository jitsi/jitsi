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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import net.java.sip.communicator.impl.gui.main.customcontrols.AntialiasedEditorPane;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.Constants;

public class ChatWritePanel extends JScrollPane
	implements UndoableEditListener {

	private AntialiasedEditorPane editorPane = new AntialiasedEditorPane();
	
	private UndoManager undo = new UndoManager();
	
	private ChatPanel chatPanel;	
	
	
	public ChatWritePanel (ChatPanel chatPanel){		
		
		super();		
		
		this.chatPanel = chatPanel;
		
		this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
		this.setHorizontalScrollBarPolicy(
		                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		this.editorPane.getDocument().addUndoableEditListener(this);
		
		this.enableKeyboardEvents();
		
		this.getViewport().add(editorPane, BorderLayout.CENTER);		
	}
		
	public void paint(Graphics g){
	
		AntialiasingManager.activateAntialiasing(g);
		
		super.paint(g);
	
		Graphics2D g2 = (Graphics2D)g;
		
		g2.setColor(Constants.MSG_WINDOW_BORDER_COLOR);
		g2.setStroke(new BasicStroke(1.5f));
		
		g2.drawRoundRect(3, 3, this.getWidth() - 4, this.getHeight() - 4, 8, 8);
	
	}

	public JEditorPane getEditorPane() {
		return editorPane;
	}
	
	private void enableKeyboardEvents(){
		
		this.editorPane.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e){
				if ((e.getModifiers() & KeyEvent.CTRL_MASK) == KeyEvent.CTRL_MASK && 
							(e.getKeyCode() == KeyEvent.VK_ENTER)){
                   
					JButton sendButton = chatPanel.getSendPanel().getSendButton();
					
                    sendButton.requestFocus();
					sendButton.doClick();									
				}
				else if ((e.getModifiers() & KeyEvent.CTRL_MASK) == KeyEvent.CTRL_MASK && 
						(e.getKeyCode() == KeyEvent.VK_Z)){					
					
					if(undo.canUndo()){
						undo();
					}
				}
				else if ((e.getModifiers() & KeyEvent.CTRL_MASK) == KeyEvent.CTRL_MASK && 
						(e.getKeyCode() == KeyEvent.VK_R)){
					
					if(undo.canRedo()){
						redo();
					}					
				}				
			}
		});		
	}

	public void undoableEditHappened(UndoableEditEvent e) {
		//Remember the edit		
        this.undo.addEdit(e.getEdit());        
	}
	
	private void undo() {
        try {
            undo.undo();
        } catch (CannotUndoException ex) {
            System.out.println("Unable to undo: " + ex);
            ex.printStackTrace();
        }       
    }   

    private void redo() {
        try {
            undo.redo();
        } catch (CannotRedoException ex) {
            System.out.println("Unable to redo: " + ex);
            ex.printStackTrace();
        }
    }   
}
