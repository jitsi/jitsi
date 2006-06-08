/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.Dimension;

import javax.swing.JTextArea;

/**
 * The SIPCommMsgTextArea is a text area defined specially for warning
 * messages.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommMsgTextArea extends JTextArea {

    public SIPCommMsgTextArea(String text){
        super(text);
        
        this.setLineWrap(true);
        this.setWrapStyleWord(true);
        
        int col = 40;
        this.setColumns(col);
        int docLen = this.getDocument().getLength();
        this.setRows((int)Math.ceil(docLen/col));
    }
}
