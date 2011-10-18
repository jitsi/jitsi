/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.resources;

import java.util.*;

/**
 * @author Damian Minkov
 */
public interface LanguagePack
    extends ResourcePack
{
    public static final String RESOURCE_NAME_DEFAULT_VALUE
        = "DefaultLanguagePack";

    /**
     * Returns a <tt>Map</tt>, containing all [key, value] pairs for the given
     * locale.
     *
     * @param locale The <tt>Locale</tt> we're looking for.
     * @return a <tt>Map</tt>, containing all [key, value] pairs for the given
     * locale.
     */
    public Map<String, String> getResources(Locale locale);

    /**
     * All the locales in the language pack.
     * @return all the locales this Language pack contains.
     */
    public Iterator<Locale> getAvailableLocales();
}
