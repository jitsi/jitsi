/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.smiley;

import java.util.*;

import net.java.sip.communicator.service.replacement.smilies.*;

/**
 * The <tt>Resources</tt> is used to access smiley icons.
 *
 * @author Yana Stamcheva
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
            new String[] {"(angel)" }, "Angel"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY3",
            new String[] {":-*", ":*", "(kiss)"}, "Kiss"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY4",
            new String[] {":-0", "(shocked)"}, "Shocked"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY5",
            new String[] {  ";-((", ";((", ";-(", ";(", ":'(", ":'-(",
                            ":~-(", ":~(", "(upset)" }, "Upset"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY6",
            new String[] {"(L)" , "(l)", "(H)", "(h)"}, "In love"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY7",
            new String[] {"(blush)"}, "Blushing"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY8",
            new String[] {":-P", ":P", ":-p", ":p" }, "Tongue out"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY9",
            new String[] {":-))", ":))", ";-))", ";))", "(lol)"},
            "Laughing"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY10",
            new String[] {"(y)", "(Y)", "(ok)"}, "Ok"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY11",
            new String[] {";-)", ";)", ":-)", ":)"}, "Smile"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY12",
            new String[] {"(sick)"}, "Sick"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY13",
            new String[] {"(n)", "(N)" }, "No"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY14",
            new String[] {"(chuckle)" }, "Chuckle"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY15",
            new String[] {"(wave)" }, "Waving"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY16",
            new String[] {"(clap)"}, "Clapping"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY17",
            new String[] {"(angry)"}, "Angry"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY18",
            new String[] {"(bomb)"}, "Explosing"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY19",
            new String[] {"(search)"}, "Searching"));

        defaultSmileyList.add(new SmileyImpl("service.gui.smileys.SMILEY20",
            new String[] {"(oops)"}, "Oops"));

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
}
