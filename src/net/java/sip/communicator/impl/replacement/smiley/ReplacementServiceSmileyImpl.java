/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.smiley;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

/**
 * Implements the {@link ReplacementService} to provide smiley as replacement
 * source.
 *
 * @author Yana Stamcheva
 * @author Purvesh Sahoo
 */
public class ReplacementServiceSmileyImpl
    implements ReplacementService
{
    /**
     * The compiled <tt>Pattern</tt> which matches {@link #smileyStrings}.
     */
    private static Pattern smileyPattern;

    /**
     * The <tt>List</tt> of smiley strings which are matched by
     * {@link #smileyPattern}.
     */
    private static final java.util.List<String> smileyStrings =
        new ArrayList<String>();

    /**
     * The closing tag of the <code>PLAINTEXT</code> HTML element.
     */
    private static final String END_PLAINTEXT_TAG = "</PLAINTEXT>";

    /**
     * The opening tag of the <code>PLAINTEXT</code> HTML element.
     */
    private static final String START_PLAINTEXT_TAG = "<PLAINTEXT>";

    /**
     * Configuration label property name. The label is saved in the languages
     * file under this property.
     */
    public static final String SMILEY_SOURCE = "SMILEY";

    /**
     * Replaces the smiley strings in the chat message with their
     * corresponding smiley image.
     * 
     * @param chatString the original chat message.
     * @return replaced chat message with the smiley images; the original
     *         message in case of no match.
     */
    public String getReplacedMessage(final String chatString)
    {
        String startPlainTextTag = START_PLAINTEXT_TAG;
        String endPlainTextTag = END_PLAINTEXT_TAG;
        Collection<Smiley> smileys = ImageLoader.getDefaultSmileyPack();

        Matcher m = getSmileyPattern(smileys).matcher(chatString);
        StringBuffer msgBuffer = new StringBuffer();

        int prevEnd = 0;

        while (m.find())
        {
            msgBuffer.append(chatString.substring(prevEnd, m.start()));
            prevEnd = m.end();

            String smileyString = m.group().trim();

            msgBuffer.append(endPlainTextTag);
            msgBuffer.append("<IMG SRC=\"");
            try
            {
                msgBuffer.append(ImageLoader.getSmiley(smileyString)
                    .getImagePath(SmileyActivator.getResources()));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            msgBuffer.append("\" ALT=\"");
            msgBuffer.append(smileyString);
            msgBuffer.append("\"></IMG>");
            msgBuffer.append(startPlainTextTag);

        }
        msgBuffer.append(chatString.substring(prevEnd));

        return msgBuffer.toString();
    }

    /**
     * Gets a compiled <tt>Pattern</tt> which matches the smiley strings of the
     * specified <tt>Collection</tt> of <tt>Smiley</tt>s.
     *
     * @param smileys the <tt>Collection</tt> of <tt>Smiley</tt>s for which to
     *            get a compiled <tt>Pattern</tt> which matches its smiley
     *            strings
     * @return a compiled <tt>Pattern</tt> which matches the smiley strings of
     *         the specified <tt>Collection</tt> of <tt>Smiley</tt>s
     */
    private static Pattern getSmileyPattern(Collection<Smiley> smileys)
    {
        synchronized (smileyStrings)
        {
            boolean smileyStringsIsEqual;

            if (smileyPattern == null)
                smileyStringsIsEqual = false;
            else
            {
                smileyStringsIsEqual = true;

                int smileyStringIndex = 0;
                int smileyStringCount = smileyStrings.size();

                smileyLoop: for (Smiley smiley : smileys)
                    for (String smileyString : smiley.getSmileyStrings())
                        if ((smileyStringIndex < smileyStringCount)
                            && smileyString.equals(smileyStrings
                                .get(smileyStringIndex)))
                            smileyStringIndex++;
                        else
                        {
                            smileyStringsIsEqual = false;
                            break smileyLoop;
                        }
                if (smileyStringsIsEqual
                    && (smileyStringIndex != smileyStringCount))
                    smileyStringsIsEqual = false;
            }

            if (!smileyStringsIsEqual)
            {
                smileyStrings.clear();

                StringBuffer regex = new StringBuffer();

                regex.append("(?<!(alt='|alt=\"))(");
                for (Smiley smiley : smileys)
                    for (String smileyString : smiley.getSmileyStrings())
                    {
                        smileyStrings.add(smileyString);

                        regex.append(
                            GuiUtils.replaceSpecialRegExpChars(smileyString))
                            .append("|");
                    }
                regex = regex.deleteCharAt(regex.length() - 1);
                regex.append(')');

                smileyPattern = Pattern.compile(regex.toString());
            }
            return smileyPattern;
        }
    }
}