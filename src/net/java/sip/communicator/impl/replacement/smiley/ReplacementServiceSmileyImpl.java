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
package net.java.sip.communicator.impl.replacement.smiley;

import java.util.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.service.replacement.smilies.*;
import net.java.sip.communicator.util.*;

/**
 * Implements the {@link ReplacementService} to provide smiley as replacement
 * source.
 *
 * @author Yana Stamcheva
 * @author Purvesh Sahoo
 * @author Adam Netocny
 */
public class ReplacementServiceSmileyImpl
    implements SmiliesReplacementService
{
    /**
     * The <tt>Logger</tt> used by the <tt>ReplacementServiceSmileyImpl</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ReplacementServiceSmileyImpl.class);

    /**
     * The <tt>List</tt> of smiley strings which are matched by
     * {@link #smileyRegex}.
     */
    private static final List<String> smileyStrings = new ArrayList<String>();

    /**
     * Configuration label shown in the config form.
     */
    public static final String SMILEY_SOURCE = "SMILEY";

    /**
     * The regex used to match the smilies in the message.
     */
    public static String smileyRegex;

    /**
     * Replaces the smiley strings with their corresponding smiley image.
     *
     * @param sourceString the original smiley string.
     * @return the smiley image replaced for the smiley string; the original
     *         smiley string in case of no match.
     */
    public String getReplacement(final String sourceString)
    {
        try
        {
            Smiley smiley = Resources.getSmiley(sourceString.trim());

            if (smiley != null)
                return smiley.getImagePath();
        }
        catch (Exception e)
        {
            logger.error(
                    "Failed to get smiley replacement for " + sourceString,
                    e);
        }
        return sourceString;
    }

    /**
     * Gets a regex string which matches the smiley strings of the specified
     * <tt>Collection</tt> of <tt>Smiley</tt>s.
     *
     * @param smileys the <tt>Collection</tt> of <tt>Smiley</tt>s for which to
     *            get a compiled <tt>Pattern</tt> which matches its smiley
     *            strings
     * @return a regex string which matches the smiley strings of the specified
     *         <tt>Collection</tt> of <tt>Smiley</tt>s
     */
    private static String getSmileyPattern(Collection<Smiley> smileys)
    {
        synchronized (smileyStrings)
        {
            boolean smileyStringsIsEqual;

            if (smileyRegex == null)
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

                smileyRegex = regex.toString();
            }
            return smileyRegex;
        }
    }

    /**
     * Returns the source name
     *
     * @return the source name
     */
    public String getSourceName()
    {
        return SMILEY_SOURCE;
    }

    /**
     * Returns the pattern of the source
     *
     * @return the source pattern
     */
    public String getPattern()
    {
        Collection<Smiley> smileys = Resources.getDefaultSmileyPack();
        return getSmileyPattern(smileys);
    }

    /**
     * Returns the smileys pack to use in the user interface.
     *
     * @return a collection of all smileys available
     */
    public Collection<Smiley> getSmiliesPack()
    {
        return Resources.getDefaultSmileyPack();
    }

    /**
     * Reloads all smilies.
     */
    public void reloadSmiliesPack()
    {
        Resources.reloadResources();
    }
}
