/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.util;

import java.io.*;
import javax.swing.text.html.*;
import javax.swing.text.*;

/**
 * A utility class that allows to extract the text content of an html page 
 * stripped from all formatting tags.
 * 
 * @author Emil Ivov <emcho at sip-communicator.org>
 */
public class Html2Text 
{
    private static final Logger logger
        = Logger.getLogger(Html2Text.class);
    /**
     * The editor kit we use for conversions.
     */
    private HTMLEditorKit htmlEditorKit = new HTMLEditorKit(); 
    
    /**
     * A utility class that allows to extract the text content of an html page 
     * stripped from all formatting tags. Method is synchronized to avoid 
     * concurrent access to the underlying html editor kit.
     * 
     * @param html the html string that we will extract the text from.
     * @return the text content of the <tt>html</tt> parameter.
     */
    public synchronized String extractText(String html)
    {
        Document doc = htmlEditorKit.createDefaultDocument();
        
        try
        {
            htmlEditorKit.read(new StringReader(html), doc, 0);
            return doc.getText(1, doc.getLength() - 1);
        } 
        catch (Exception exc)
        {
            logger.info("Failed to extract plain text from html="+html, exc);
            return html;
        } 
    }
}
