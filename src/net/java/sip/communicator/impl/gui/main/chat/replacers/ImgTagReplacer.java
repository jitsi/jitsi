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

/**
 * Formats HTML tags &lt;img ... /&gt; to &lt; img ... &gt;&lt;/img&gt; or
 * &lt;IMG ... /&gt; to &lt;IMG&gt;&lt;/IMG&gt;. The reason of this
 * {@link Replacer} is that the ChatPanel does not support &lt;img /&gt; tags
 * (XHTML syntax). Thus, we remove every slash from each &lt;img /&gt; and close
 * it with a separate closing tag.
 *
 * @author Danny van Heumen
 */
public class ImgTagReplacer
    implements Replacer
{

    /**
     * Img tag replacer expects HTML content.
     *
     * @return returns false for HTML content
     */
    @Override
    public boolean expectsPlainText()
    {
        return false;
    }

    /**
     * Replace operation that replaces img tags that immediately close.
     *
     * @param target destination to write the result to
     * @param piece the piece of content to process
     */
    @Override
    public void replace(final StringBuilder target, final String piece)
    {
        // Compile the regex to match something like <img ... /> or
        // <IMG ... />. This regex is case sensitive and keeps the style,
        // src or other attributes of the <img> tag.
        final Pattern p = Pattern.compile("<\\s*[iI][mM][gG](.*?)(/\\s*>)");
        final Matcher m = p.matcher(piece);
        int slashIndex;
        int start = 0;

        // while we find some <img /> self-closing tags with a slash inside.
        while (m.find())
        {
            // First, we have to copy all the message preceding the <img>
            // tag.
            target.append(piece.substring(start, m.start()));
            // Then, we find the position of the slash inside the tag.
            slashIndex = m.group().lastIndexOf("/");
            // We copy the <img> tag till the slash exclude.
            target.append(m.group().substring(0, slashIndex));
            // We copy all the end of the tag following the slash exclude.
            target.append(m.group().substring(slashIndex + 1));
            // We close the tag with a separate closing tag.
            target.append("</img>");
            start = m.end();
        }
        // Finally, we have to add the end of the message following the last
        // <img> tag, or the whole message if there is no <img> tag.
        target.append(piece.substring(start));
    }
}
