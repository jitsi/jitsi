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
public enum ControlChar
{
    BOLD('\u0002', "b"),
    COLOR('\u0003', "font"),
    NORMAL('\u000F', null),
    ITALICS('\u0016', "i"),
    UNDERLINE('\u001F', "u");

    /**
     * The IRC control code.
     */
    private char code;

    /**
     * HTML tag that expresses the specific formatting requirement.
     */
    private String tag;

    /**
     * Constructor.
     * 
     * @param code the control code
     */
    private ControlChar(char code, String htmlTag)
    {
        this.code = code;
        this.tag = htmlTag;
    }

    /**
     * Find enum instance by IRC control code.
     * 
     * @param code IRC control code
     * @return returns enum instance or null if no instance was found
     */
    public static ControlChar byCode(char code)
    {
        for (ControlChar controlChar : values())
        {
            if (controlChar.getCode() == code)
                return controlChar;
        }
        return null;
    }

    /**
     * Get the IRC control code.
     * 
     * @return returns the IRC control code
     */
    public char getCode()
    {
        return this.code;
    }

    /**
     * Get the HTML start tag, optionally including extra parameters.
     * 
     * @param addition optional addition to be included before closing the start
     *            tag
     * @return returns HTML start tag.
     */
    public String getHtmlStart(String... addition)
    {
        StringBuilder tag = new StringBuilder("<" + this.tag);
        for (String add : addition)
        {
            tag.append(" ");
            tag.append(add);
        }
        tag.append('>');
        return tag.toString();
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
}
