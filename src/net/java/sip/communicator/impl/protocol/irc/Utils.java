/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * Some IRC-related utility methods.
 * 
 * @author Danny van Heumen
 */
public final class Utils
{
    /**
     * Logger
     */
    private static final Logger LOGGER = Logger.getLogger(Utils.class);

    /**
     * Private constructor since we do not need to construct anything.
     */
    private Utils()
    {
    }

    /**
     * Parse IRC text message and process possible control codes.
     * 
     * TODO Support reverse (0x16) control code?
     * 
     * @param text the message
     * @return returns the processed message or null if text message was null,
     *         since there is nothing to modify there
     */
    public static String parse(String text)
    {
        if (text == null)
            return null;

        FormattedTextBuilder builder = new FormattedTextBuilder();
        for (int i = 0; i < text.length(); i++)
        {
            char val = text.charAt(i);
            ControlChar control = ControlChar.byCode(val);
            if (control != null)
            {
                // value is a control character, so do something special
                switch (control)
                {
                case ITALICS:
                case UNDERLINE:
                case BOLD:
                case NORMAL:
                    builder.apply(control);
                    break;
                case COLOR:
                    if (builder.isActive(control))
                    {
                        builder.apply(control);
                    }
                    else
                    {
                        final List<String> adds = new LinkedList<String>();
                        try
                        {
                            // parse foreground color code
                            final Color foreground =
                                parseForegroundColor(text.substring(i + 1));
                            adds.add("color=\"" + foreground.getHtml() + "\"");
                            i += 2;

                            // parse background color code
                            final Color background =
                                parseBackgroundColor(text.substring(i + 1));
                            adds.add(
                                "bgcolor=\"" + background.getHtml() + "\"");
                            i += 3;
                        }
                        catch (IllegalArgumentException e)
                        {
                            LOGGER.debug(
                                "Invalid color code: "
                                    + text.substring(i + 1), e);
                        }
                        catch (ArrayIndexOutOfBoundsException e)
                        {
                            LOGGER.debug("Unknown color code referenced: "
                                + text.substring(i + 1), e);
                        }
                        builder.apply(control,
                            adds.toArray(new String[adds.size()]));
                    }
                    break;
                default:
                    LOGGER.warn("Unsupported IRC control code encountered: "
                        + control);
                    break;
                }
            }
            else
            {
                // value is a normal character, just append
                builder.append(val);
            }
        }

        return builder.done();
    }

    /**
     * Parse background color code starting with the separator.
     * 
     * @param text the text starting with the background color (separator)
     * @return returns the background color
     */
    private static Color parseBackgroundColor(String text)
    {
        try
        {
            if (text.charAt(0) == ',')
            {
                // if available, also parse background color
                int color =
                    Integer.parseInt("" + text.charAt(1) + text.charAt(2));
                return Color.values()[color];
            }
            throw new IllegalArgumentException(
                "no color separator present, hence no background color present");
        }
        catch (StringIndexOutOfBoundsException e)
        {
            // Abort parsing background color. Assume only
            // foreground color available.
            throw new IllegalArgumentException(
                "text stopped before the background color code was finished");
        }
        catch (NumberFormatException e)
        {
            // No background color defined, ignoring ...
            throw new IllegalArgumentException(
                "value isn't a background color code: " + text.substring(1, 3));
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            LOGGER.info("Specified IRC color is not a known color code.", e);
            throw new IllegalArgumentException("background color value "
                + text.substring(1, 3) + " is not a known color");
        }
    }

    /**
     * Parse foreground color and return corresponding Color instance.
     * 
     * @param text the text to parse, starting with color code
     * @return returns Color instance
     */
    private static Color parseForegroundColor(String text)
    {
        try
        {
            int color = Integer.parseInt("" + text.charAt(0) + text.charAt(1));
            return Color.values()[color];
        }
        catch (StringIndexOutOfBoundsException e)
        {
            // Invalid control code, since text has ended.
            LOGGER
                .trace("ArrayIndexOutOfBounds during foreground color control code parsing.");
            throw new IllegalArgumentException("missing foreground color code");
        }
        catch (NumberFormatException e)
        {
            // Invalid text color value
            LOGGER.trace("Invalid foreground color code encountered.");
            throw new IllegalArgumentException(
                "invalid foreground color code: " + text.substring(0, 2));
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            LOGGER.info("Specified IRC color is not a known color code.", e);
            throw new IllegalArgumentException("foreground color value "
                + text.substring(0, 2) + " is not a known color");
        }
    }
}
