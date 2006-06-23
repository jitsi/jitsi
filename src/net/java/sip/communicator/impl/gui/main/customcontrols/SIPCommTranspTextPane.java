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
public class SIPCommTranspTextPane extends JTextArea {

    public SIPCommTranspTextPane(String text){
        super(text);
        
        this.setEditable(false);
        this.setLineWrap(true);
        this.setWrapStyleWord(true);
        this.setOpaque(false);
    }
}
