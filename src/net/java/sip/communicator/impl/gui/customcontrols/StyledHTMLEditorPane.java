/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.io.IOException;

import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.SIPCommHTMLEditorKit;
import net.java.sip.communicator.util.Logger;

public class StyledHTMLEditorPane
    extends JEditorPane
{
    private Logger logger = Logger.getLogger(StyledHTMLEditorPane.class);
    
    private HTMLEditorKit editorKit = new SIPCommHTMLEditorKit();
    
    private HTMLDocument document;
    
    public StyledHTMLEditorPane()
    {
        this.document = (HTMLDocument) editorKit.createDefaultDocument();
        
        this.setContentType("text/html");
        this.setEditorKitForContentType("text/html", editorKit);
        this.setEditorKit(editorKit);
        this.setDocument(document);
        
        Constants.loadSimpleStyle(document.getStyleSheet());
    }
    
    
    public void appendToEnd(String text)
    {
        Element root = document.getDefaultRootElement();
        try
        {   
            document.insertAfterEnd(root
                .getElement(root.getElementCount() - 1), text);
        }
        catch (BadLocationException e)
        {
            logger.error("Insert in the HTMLDocument failed.", e);
        }
        catch (IOException e)
        {
            logger.error("Insert in the HTMLDocument failed.", e);
        }
    }
    
    public void insertAfterStart(String text)
    {
        Element root = this.document.getDefaultRootElement();
        
        try {
            this.document.insertBeforeStart(root
                    .getElement(0), text);            
        } catch (BadLocationException e) {
            logger.error("Insert in the HTMLDocument failed.", e);
        } catch (IOException e) {
            logger.error("Insert in the HTMLDocument failed.", e);
        }
    }
}
