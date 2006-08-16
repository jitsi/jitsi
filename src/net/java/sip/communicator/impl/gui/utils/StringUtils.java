/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;
import javax.swing.*;
/**
 * The <tt>StringUtils</tt> class is used through this ui implementation for
 * some special operations with strings.
 * 
 * @author Yana Stamcheva
 */
public class StringUtils {

    /**
     * Replaces some chars that are special in a regular expression.
     * @param text The initial text.
     * @return the formatted text
     */
    public static String replaceSpecialRegExpChars(String text) {
        return text.replaceAll("([.()^&$*|])", "\\\\$1");
    }

    /**
     * Returns the width in pixels of a text.
     * @param c the component where the text is contained
     * @param text the text to measure
     * @return the width in pixels of a text.
     */
    public static int getStringWidth(Component c, String text) {
        return SwingUtilities.computeStringWidth(c
                .getFontMetrics(Constants.FONT), text);
    }
}
