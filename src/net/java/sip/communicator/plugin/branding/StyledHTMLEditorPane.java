/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.branding;

import java.io.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.util.*;

public class StyledHTMLEditorPane
    extends JEditorPane
{
    private final Logger logger = Logger.getLogger(StyledHTMLEditorPane.class);

    private final HTMLDocument document;
    
    public StyledHTMLEditorPane()
    {
        this.setContentType("text/html");

        this.document
            = (HTMLDocument) this.getDocument();

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
