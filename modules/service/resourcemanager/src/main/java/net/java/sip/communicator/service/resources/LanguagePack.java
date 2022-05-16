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
     * Returns a Set of the keys contained only in the ResourceBundle for
     * locale.
     * @param locale the locale for which the keys are requested
     * @return a Set of the keys contained only in the ResourceBundle for
     * locale
     */
    public Set<String> getResourceKeys(Locale locale);

    /**
     * All the locales in the language pack.
     * @return all the locales this Language pack contains.
     */
    public Iterator<Locale> getAvailableLocales();
}
