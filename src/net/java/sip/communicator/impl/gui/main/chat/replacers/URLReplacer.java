/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.replacers;

import java.util.regex.*;

import net.java.sip.communicator.impl.gui.main.chat.*;

import org.apache.commons.lang3.*;

/**
 * The URL replacer used for replacing identified URL's with variations that are
 * surrounded by A-tags (anchor tags).
 *
 * @author Danny van Heumen
 */
public class URLReplacer
    implements Replacer
{
    /**
     * The URL pattern to be used in matching.
     */
    private final Pattern pattern;

    /**
     * The URL Replacer.
     *
     * @param urlPattern the exact URL pattern to be applied
     */
    public URLReplacer(final Pattern urlPattern)
    {
        if (urlPattern == null)
        {
            throw new IllegalArgumentException("urlPattern cannot be null");
        }
        this.pattern = urlPattern;
    }

    /**
     * Plain text is expected.
     *
     * @return returns true for plain text expectation
     */
    @Override
    public boolean expectsPlainText()
    {
        return true;
    }

    /**
     * Replace operation for replacing URL's with a hyperlinked version.
     *
     * @param target destination to write the replacement result to
     * @param piece the piece of content to be processed
     */
    @Override
    public void replace(final StringBuilder target, final String piece)
    {
        final Matcher m = this.pattern.matcher(piece);
        int prevEnd = 0;

        while (m.find())
        {
            target.append(StringEscapeUtils.escapeHtml4(piece.substring(
                prevEnd, m.start())));
            prevEnd = m.end();

            String url = m.group().trim();
            target.append("<A href=\"");
            if (url.startsWith("www"))
            {
                target.append("http://");
            }
            target.append(url);
            target.append("\">");
            target.append(StringEscapeUtils.escapeHtml4(url));
            target.append("</A>");
        }
        target.append(StringEscapeUtils.escapeHtml4(piece.substring(prevEnd)));
    }
}
