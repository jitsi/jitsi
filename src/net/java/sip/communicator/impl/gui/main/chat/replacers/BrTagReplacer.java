/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.replacers;

import java.util.regex.*;

import net.java.sip.communicator.impl.gui.main.chat.*;

/**
 * Formats HTML tags &lt;br/&gt; to &lt;br&gt; or &lt;BR/&gt; to &lt;BR&gt;.
 * The reason of this {@link Replacer} is that the ChatPanel does not support
 * &lt;br /&gt; closing tags (XHTML syntax), thus we have to remove every
 * slash from each &lt;br /&gt; tags.
 *
 * @author Danny van Heumen
 */
public class BrTagReplacer
    implements Replacer
{

    /**
     * BrTagReplacer expects HTML content.
     *
     * @return false for HTML content
     */
    @Override
    public boolean expectsPlainText()
    {
        return false;
    }

    /**
     * Replace operation. "New-style" br-tags are processed and the result
     * written to <tt>target</tt>.
     *
     * @param target destination of the replacer result
     * @param piece piece of HTML content with properly formatted &lt;br&gt;
     *            tags.
     */
    @Override
    public void replace(final StringBuilder target, final String piece)
    {
        // Compile the regex to match something like <br .. /> or <BR .. />.
        // This regex is case sensitive and keeps the style or other
        // attributes of the <br> tag.
        Matcher m =
            Pattern.compile("<\\s*[bB][rR](.*?)(/\\s*>)").matcher(piece);
        int start = 0;

        // while we find some <br /> closing tags with a slash inside.
        while (m.find())
        {
            // First, we have to copy all the message preceding the <br>
            // tag.
            target.append(piece.substring(start, m.start()));
            // Then, we find the position of the slash inside the tag.
            final int slashIndex = m.group().lastIndexOf("/");
            // We copy the <br> tag till the slash exclude.
            target.append(m.group().substring(0, slashIndex));
            // We copy all the end of the tag following the slash exclude.
            target.append(m.group().substring(slashIndex + 1));
            start = m.end();
        }
        // Finally, we have to add the end of the message following the last
        // <br> tag, or the whole message if there is no <br> tag.
        target.append(piece.substring(start));
    }
}
