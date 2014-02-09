/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

/**
 * IRC color codes that can be specified in the color control code.
 * 
 * @author Danny van Heumen
 */
public enum Color
{
    WHITE("White"),
    BLACK("Black"),
    BLUE("Navy"),
    GREEN("Green"),
    RED("Red"),
    BROWN("Maroon"),
    PURPLE("Purple"),
    ORANGE("Orange"),
    YELLOW("Yellow"),
    LIGHT_GREEN("Lime"),
    TEAL("Teal"),
    LIGHT_CYAN("Cyan"),
    LIGHT_BLUE("RoyalBlue"),
    PINK("Fuchsia"),
    GREY("Grey"),
    LIGHT_GREY("Silver");

    /**
     * Instance containing the html representation of this color.
     */
    private String html;

    /**
     * Constructor for enum entries.
     * 
     * @param html HTML representation for color
     */
    private Color(String html)
    {
        this.html = html;
    }

    /**
     * Get the HTML representation of this color.
     * 
     * @return returns html representation or null if none exist
     */
    public String getHtml()
    {
        return this.html;
    }
}
