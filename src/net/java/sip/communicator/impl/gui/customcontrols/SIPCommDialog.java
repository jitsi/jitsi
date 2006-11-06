/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public abstract class SIPCommDialog extends JDialog
{
    ActionMap amap;
    InputMap imap;

    public SIPCommDialog(Dialog owner)
    {
        super(owner);
        
        this.initInputMap();
    }
 
    public SIPCommDialog(Frame owner)
    {
        super(owner);
        
        this.initInputMap(); 
    }
    
    public SIPCommDialog()
    {
        this.initInputMap(); 
    }
    
    private void initInputMap()
    {
        amap = this.getRootPane().getActionMap();

        amap.put("close", new CloseAction());
        
        imap = this.getRootPane().getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");  
    }
    
    private class CloseAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            close();
        }
    }
    
    protected void addKeyBinding(KeyStroke keyStroke, Action action)
    {
        String actionID = action.getClass().getName();
        
        amap.put(actionID, action);
        
        imap.put(keyStroke, actionID);
    }
    
    protected abstract void close();
}
