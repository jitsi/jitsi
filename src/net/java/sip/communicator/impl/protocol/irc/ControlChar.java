/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

/**
 * Enum with available IRC control characters.
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
     * @param code the control code
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
    
    static class Bold extends ControlChar
    {
        Bold()
        {
            super("b");
        }
    }
    
    static class Italics extends ControlChar
    {
        Italics()
        {
            super("i");
        }
    }
    
    static class Underline extends ControlChar
    {
        Underline()
        {
            super("u");
        }
    }
    
    static class ColorFormat extends ControlChar
    {
        private final Color foreground;

        private final Color background;

        ColorFormat(final Color foreground, final Color background)
        {
            super("font");
            this.foreground = foreground;
            this.background = background;
        }

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
}
