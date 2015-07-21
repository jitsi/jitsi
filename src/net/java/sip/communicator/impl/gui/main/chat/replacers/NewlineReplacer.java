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

import net.java.sip.communicator.impl.gui.main.chat.*;

/**
 * NewlineReplacer for replacing newlines with a combined version of a
 * &lt;br&gt; tag and a <tt>\n</tt> character. This special treatment is
 * necessary because copy-paste operations from the chat window do not recognize
 * the &lt;br&gt; tags as line breaks.
 *
 * @author Danny van Heumen
 */
public class NewlineReplacer
    implements Replacer
{

    /**
     * The NewlineReplacer expects HTML content.
     *
     * @return returns false
     */
    @Override
    public boolean expectsPlainText()
    {
        return false;
    }

    /**
     * New line characters are searched and replaced with a combination of
     * newline and br tag.
     *
     * @param target the destination for to write the replacement result to
     * @param piece the piece of content to process
     */
    @Override
    public void replace(final StringBuilder target, final String piece)
    {
        /*
         * <br> tags are needed to visualize a new line in the html format, but
         * when copied to the clipboard they are exported to the plain text
         * format as ' ' and not as '\n'.
         *
         * See bug N4988885:
         * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4988885
         *
         * To fix this we need "&#10;" - the HTML-Code for ASCII-Character No.10
         * (Line feed).
         */
        target.append(piece.replaceAll("\n", "<BR/>&#10;"));
    }
}
