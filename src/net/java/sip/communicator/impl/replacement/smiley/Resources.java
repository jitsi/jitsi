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

import net.java.sip.communicator.service.replacement.smilies.*;

/**
 * The <tt>Resources</tt> is used to access smiley icons.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class Resources
{
    /**
     * The default pack of <tt>Smiley</tt>s.
     */
    private static Collection<Smiley> defaultSmileyPack;

    /**
     * Load default smileys pack.
     *
     * @return the ArrayList of all smileys.
     */
    public static Collection<Smiley> getDefaultSmileyPack()
    {
        if (defaultSmileyPack != null)
            return defaultSmileyPack;

        List<Smiley> defaultSmileyList = new ArrayList<Smiley>();

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY1",
            new String[] {":((", ":-((", ":((", ":(", ":-(", "(sad)"},
            "Sad"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY2",
            new String[] {"(angry)"}, "Angry"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY3",
            new String[] {"(n)", "(N)" }, "No"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY4",
            new String[] {":-))", ":))", ";-))", ";))", "(lol)", ":-D", ":D",
                        ";-D", ";D"}, "Laughing"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY5",
            new String[] {  ";-((", ";((", ";-(", ";(", ":'(", ":'-(",
                            ":~-(", ":~(", "(upset)" }, "Upset"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY6",
            new String[] {"&lt;3", "(L)" , "(l)", "(H)", "(h)"}, "In love"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY7",
            new String[] {"(angel)" }, "Angel"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY8",
            new String[] {"(bomb)"}, "Exploding"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY9",
            new String[] {"(chuckle)" }, "Chuckle"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY10",
            new String[] {"(y)", "(Y)", "(ok)"}, "Ok"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY11",
            new String[] {":-)", ":)"}, "Smile"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY12",
            new String[] {"(blush)"}, "Blushing"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY13",
            new String[] {":-*", ":*", "(kiss)"}, "Kiss"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY14",
            new String[] {"(search)"}, "Searching"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY15",
            new String[] {"(wave)" }, "Waving"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY16",
            new String[] {"(clap)"}, "Clapping"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY17",
            new String[] {"(sick)"}, "Sick"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY18",
            new String[] {":-P", ":P", ":-p", ":p" }, "Tongue out"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY19",
            new String[] {":-0", "(shocked)"}, "Shocked"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY20",
            new String[] {"(oops)"}, "Oops"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY21",
            new String[] {";-)", ";)"}, "Wink"));

        defaultSmileyPack
            = Collections.unmodifiableCollection(defaultSmileyList);

        return defaultSmileyPack;
    }

    /**
     * Returns a Smiley object for a given smiley string.
     * @param smileyString One of :-), ;-), etc.
     * @return A Smiley object for a given smiley string.
     */
    public static Smiley getSmiley(String smileyString)
    {
        for (Smiley smiley : getDefaultSmileyPack())
            for (String srcString : smiley.getSmileyStrings())
                if (srcString.equals(smileyString))
                    return smiley;
        return null;
    }

    /**
     * Reloads smilies.
     */
    public static void reloadResources()
    {
        defaultSmileyPack = null;
    }
}
