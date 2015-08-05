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
 * IRC color codes that can be specified in the color control code.
 *
 * @author Danny van Heumen
 */
public enum Color
{
    /**
     * White.
     */
    WHITE("White"),

    /**
     * Black.
     */
    BLACK("Black"),

    /**
     * Navy.
     */
    BLUE("Navy"),

    /**
     * Green.
     */
    GREEN("Green"),

    /**
     * Red.
     */
    RED("Red"),

    /**
     * Maroon.
     */
    BROWN("Maroon"),

    /**
     * Purple.
     */
    PURPLE("Purple"),

    /**
     * Orange.
     */
    ORANGE("Orange"),

    /**
     * Yellow.
     */
    YELLOW("Yellow"),

    /**
     * Lime.
     */
    LIGHT_GREEN("Lime"),

    /**
     * Teal.
     */
    TEAL("Teal"),

    /**
     * Cyan.
     */
    LIGHT_CYAN("Cyan"),

    /**
     * RoyalBlue.
     */
    LIGHT_BLUE("RoyalBlue"),

    /**
     * Fuchsia.
     */
    PINK("Fuchsia"),

    /**
     * Grey.
     */
    GREY("Grey"),

    /**
     * Silver.
     */
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
    private Color(final String html)
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
