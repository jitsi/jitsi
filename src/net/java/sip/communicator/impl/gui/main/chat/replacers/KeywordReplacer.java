/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
     * Index of the optional prefix group in the regex.
     */
    private static final int INDEX_OPTIONAL_PREFIX_GROUP = 1;

    /**
     * Index of the keyword match group in the regex.
     */
    private static final int INDEX_KEYWORD_MATCH_GROUP = 2;

    /**
     * Index of the optional suffix group in the regex.
     */
    private static final int INDEX_OPTIONAL_SUFFIX_GROUP = 3;

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
            Pattern.compile("(^|\\W)(" + Pattern.quote(keyword) + ")(\\W|$)",
                Pattern.CASE_INSENSITIVE).matcher(piece);
        int prevEnd = 0;
        while (m.find())
        {
            target.append(StringEscapeUtils.escapeHtml4(piece.substring(
                prevEnd, m.start()
                    + m.group(INDEX_OPTIONAL_PREFIX_GROUP).length())));
            prevEnd = m.end() - m.group(INDEX_OPTIONAL_SUFFIX_GROUP).length();
            final String keywordMatch =
                m.group(INDEX_KEYWORD_MATCH_GROUP).trim();
            target.append("<b>");
            target.append(StringEscapeUtils.escapeHtml4(keywordMatch));
            target.append("</b>");
        }
        target.append(StringEscapeUtils.escapeHtml4(piece.substring(prevEnd)));
    }
}
