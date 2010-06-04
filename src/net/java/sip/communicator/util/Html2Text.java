/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.util;

import java.io.*;

import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;

/**
 * A utility class that allows to extract the text content of an html page 
 * stripped from all formatting tags.
 * 
 * @author Emil Ivov <emcho at sip-communicator.org>
 * @author Yana Stamcheva
 */
public class Html2Text
{
    private static final Logger logger
        = Logger.getLogger(Html2Text.class);

    private static HTMLParserCallBack parser;

    /**
     * A utility method that allows to extract the text content of an html page 
     * stripped from all formatting tags. Method is synchronized to avoid 
     * concurrent access to the underlying html editor kit.
     * 
     * @param html the html string that we will extract the text from.
     * @return the text content of the <tt>html</tt> parameter.
     */
    public static synchronized String extractText(String html)
    {
        if(html == null)
            return null;

        if (parser == null)
            parser = new HTMLParserCallBack();

        try
        {
            StringReader in = new StringReader(html);
            parser.parse(in);
            in.close();

            return parser.getText();
        }
        catch (Exception exc)
        {
            if (logger.isInfoEnabled())
                logger.info("Failed to extract plain text from html="+html, exc);
            return html;
        }
    }

    /**
     * The ParserCallback that will parse the html.
     */
    private static class HTMLParserCallBack extends HTMLEditorKit.ParserCallback
    {
        StringBuffer s;

        /**
         * Parses the text contained in the given reader.
         * 
         * @param in the reader to parse.
         * @throws IOException thrown if we fail to parse the reader.
         */
        public void parse(Reader in) throws IOException
        {
            s = new StringBuffer();
            ParserDelegator delegator = new ParserDelegator();
            // the third parameter is TRUE to ignore charset directive
            delegator.parse(in, this, Boolean.TRUE);
        }

        /**
         * Appends the given text to the string buffer.
         */
        public void handleText(char[] text, int pos)
        {
            s.append(text);
        }

        /**
         * Returns the parsed text.
         */
        public String getText()
        {
            return s.toString();
        }
    }
}
