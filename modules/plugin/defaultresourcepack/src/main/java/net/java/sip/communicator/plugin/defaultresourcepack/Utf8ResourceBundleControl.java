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
package net.java.sip.communicator.plugin.defaultresourcepack;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.ResourceBundle.*;

class Utf8ResourceBundleControl
    extends Control
{
    @Override
    public ResourceBundle newBundle(String baseName,
        Locale locale, String format, ClassLoader loader,
        boolean reload) throws
        IOException
    {
        try (InputStream is = loader.getResourceAsStream(
            toResourceName(toBundleName(baseName, locale),
                "properties")))
        {
            if (is != null)
            {
                try (var isr = new InputStreamReader(is, StandardCharsets.UTF_8))
                {
                    return new JitsiResourceBundle(isr);
                }
            }
            else
            {
                return null;
            }
        }
    }

    public static class JitsiResourceBundle
        extends PropertyResourceBundle
    {
        public JitsiResourceBundle(Reader reader) throws IOException
        {
            super(reader);
        }

        @Override
        public Set<String> handleKeySet()
        {
            return super.handleKeySet();
        }
    }

    // work around Java's backwards compatibility
    @Override
    public String toBundleName(String baseName, Locale locale)
    {
        if (locale.equals(new Locale("he")))
        {
            return baseName + "_he";
        }
        else if (locale.equals(new Locale("yi")))
        {
            return baseName + "_yi";
        }
        else if (locale.equals(new Locale("id")))
        {
            return baseName + "_id";
        }

        return super.toBundleName(baseName, locale);
    }
}
