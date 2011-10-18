/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import javax.swing.*;

/**
 * The <tt>SIPCommMsgTextArea</tt> is a text area defined specially for warning
 * messages. It defines an area with a fixed number of columns and wraps the 
 * text within it.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommMsgTextArea
    extends JTextArea
{
    private static final long serialVersionUID = 0L;

    public SIPCommMsgTextArea()
    {
        init();
    }
    
    /** 
     * Creates a text area with a fixed number of columns and wraps the 
     * text within it.
     * @param text The text to insert in this text area.
     */
    public SIPCommMsgTextArea(String text){
        super(text);

        init();
    }

    private void init()
    {
        this.setEditable(false);
        this.setLineWrap(true);
        this.setWrapStyleWord(true);
        this.setOpaque(false);
        
        int col = 40;
        this.setColumns(col);
        int docLen = this.getDocument().getLength();

        /*
         * FIXME The original code was "(int)Math.ceil(docLen/col)". But it was
         * unnecessary because both docLen and col are integers and,
         * consequently, docLen/col gives an integer. Was the intention to have
         * the division produce a real number?
         */
        this.setRows(docLen/col);
    }
}
