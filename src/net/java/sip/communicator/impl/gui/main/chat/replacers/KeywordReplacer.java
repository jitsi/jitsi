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
 * The keyword replacer used for highlighting keywords.
 *
 * @author Danny van Heumen
 */
public class KeywordReplacer
    implements Replacer
{
    /**
     * The keyword to highlight.
     */
    private final String keyword;

    /**
     * The keyword replacer with parameter for providing the keyword to
     * highlight.
     *
     * @param keyword the keyword to highlight when replacing
     */
    public KeywordReplacer(final String keyword)
    {
        this.keyword = keyword;
    }

    /**
     * Type of content expected by the replacer.
     *
     * @return returns true for HTML content
     */
    @Override
    public boolean expectsPlainText()
    {
        return true;
    }

    /**
     * Replace operation. Searches for the keyword in the provided piece of
     * content and replaces it with the piece of content surrounded by &lt;b&gt;
     * tags.
     *
     * @param target the destination to write the result to
     * @param piece the piece of content to process
     */
    @Override
    public void replace(final StringBuilder target, final String piece)
    {
        if (this.keyword == null || this.keyword.isEmpty())
        {
            target.append(StringEscapeUtils.escapeHtml4(piece));
            return;
        }

        final Matcher m =
            Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE)
                .matcher(piece);
        int prevEnd = 0;
        while (m.find())
        {
            target.append(StringEscapeUtils.escapeHtml4(piece.substring(
                prevEnd, m.start())));
            prevEnd = m.end();
            final String keywordMatch = m.group().trim();
            target.append("<b>");
            target.append(StringEscapeUtils.escapeHtml4(keywordMatch));
            target.append("</b>");
        }
        target.append(StringEscapeUtils.escapeHtml4(piece.substring(prevEnd)));
    }
}
