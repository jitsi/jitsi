/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;

/**
 * The purpose of this interface is to allow UI components with a default
 * text value to give the default text its own colour, set independently of
 * the normal text colour.
 * @author Tom Denham
 */
public interface ColoredDefaultText
{
    /**
     * Sets the foreground color.
     *
     * @param c the color to set for the text field foreground
     */
    public void setForegroundColor(Color c);

    /**
     * Gets the foreground color.
     *
     * @return the color of the text
     */
    public Color getForegroundColor();

    /**
     * Sets the foreground color of the default text shown in this text field.
     *
     * @param c the color to set
     */
    public void setDefaultTextColor(Color c);

    /**
     * Gets the foreground color of the default text shown in this text field.
     *
     * @return the color of the default text
     */
    public Color getDefaultTextColor();
}