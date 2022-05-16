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
package net.java.sip.communicator.impl.protocol.irc;

/**
 * Available IRC control characters.
 *
 * @author Danny van Heumen
 */
public abstract class ControlChar
{
    /**
     * HTML tag that expresses the specific formatting requirement.
     */
    private final String tag;

    /**
     * Constructor.
     *
     * @param htmlTag the control code
     */
    private ControlChar(final String htmlTag)
    {
        this.tag = htmlTag;
    }

    /**
     * The specified HTML tag.
     *
     * @return returns the HTML tag.
     */
    public String getTag()
    {
        return this.tag;
    }

    /**
     * Get the HTML start tag.
     *
     * @return returns HTML start tag.
     */
    public String getHtmlStart()
    {
        return "<" + this.tag + ">";
    }

    /**
     * Get the HTML end tag.
     *
     * @return returns the HTML end tag
     */
    public String getHtmlEnd()
    {
        return "</" + this.tag + ">";
    }

    /**
     * Control char representation for 'bold' formatting.
     *
     * @author Danny van Heumen
     */
    static class Bold extends ControlChar
    {
        /**
         * IRC control code.
         */
        public static final char CODE = '\u0002';

        /**
         * Constructor.
         */
        Bold()
        {
            super("b");
        }
    }

    /**
     * Control char representation for 'italics' formatting.
     *
     * @author Danny van Heumen
     */
    static class Italics extends ControlChar
    {
        /**
         * IRC control code.
         */
        public static final char CODE = '\u0016';

        /**
         * Constructor.
         */
        Italics()
        {
            super("i");
        }
    }

    /**
     * Control char representation for underlining.
     *
     * @author Danny van Heumen
     */
    static class Underline extends ControlChar
    {
        /**
         * IRC control code.
         */
        public static final char CODE = '\u001F';

        /**
         * Constructor.
         */
        Underline()
        {
            super("u");
        }
    }

    /**
     * Control char representation for colored text.
     *
     * @author Danny van Heumen
     */
    static class ColorFormat extends ControlChar
    {
        /**
         * IRC control code.
         */
        public static final char CODE = '\u0003';

        /**
         * Foreground color.
         */
        private final Color foreground;

        /**
         * Background color.
         */
        private final Color background;

        /**
         * Constructor.
         *
         * @param foreground foreground color
         * @param background background color
         */
        ColorFormat(final Color foreground, final Color background)
        {
            super("font");
            this.foreground = foreground;
            this.background = background;
        }

        /**
         * Get HTML start tag with foreground and background color codes
         * embedded.
         *
         * @return Returns string containing html start tag including foreground
         *         and background colors.
         */
        public String getHtmlStart()
        {
            StringBuilder result = new StringBuilder("<");
            result.append(getTag());
            if (this.foreground != null)
            {
                result.append(" color=\"");
                result.append(this.foreground.getHtml());
                result.append("\"");
            }
            if (this.background != null)
            {
                result.append(" bgcolor=\"");
                result.append(this.background.getHtml());
                result.append("\"");
            }
            result.append('>');
            return result.toString();
        }
    }

    /**
     * Control char representation for the cancellation of all active formatting
     * options, i.e. return to normal.
     *
     * In the current implementation, you cannot instantiate this control char.
     * Its use is purely symbolic, since it doesn't actually translate in
     * additional HTML formatting tags.
     *
     * @author Danny van Heumen
     */
    abstract static class Normal extends ControlChar
    {
        /**
         * IRC control code.
         */
        public static final char CODE = '\u000F';

        /**
         * Private constructor since it should not be instantiated.
         */
        private Normal()
        {
            super(null);
        }
    }
}
