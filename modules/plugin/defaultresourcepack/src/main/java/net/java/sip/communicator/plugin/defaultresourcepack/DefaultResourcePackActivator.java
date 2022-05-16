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

import java.net.*;
import java.util.*;
import net.java.sip.communicator.service.resources.*;
import org.osgi.framework.*;

/**
 * @author damencho
 */
public class DefaultResourcePackActivator
    implements BundleActivator
{
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultResourcePackActivator.class);

    static BundleContext bundleContext;

    // buffer for ressource files found
    private static final Map<String, List<String>> resourceFiles =
        new HashMap<>();

    public void start(BundleContext bc)
    {
        bundleContext = bc;

        Hashtable<String, String> props = new Hashtable<>();
        props.put(ResourcePack.RESOURCE_NAME,
            ColorPack.RESOURCE_NAME_DEFAULT_VALUE);

        bundleContext.registerService(ColorPack.class,
            new DefaultColorPackImpl(),
            props);

        Hashtable<String, String> imgProps = new Hashtable<>();
        imgProps.put(ResourcePack.RESOURCE_NAME,
            ImagePack.RESOURCE_NAME_DEFAULT_VALUE);

        bundleContext.registerService(ImagePack.class,
            new DefaultImagePackImpl(),
            imgProps);

        Hashtable<String, String> sndProps = new Hashtable<>();
        sndProps.put(ResourcePack.RESOURCE_NAME,
            SoundPack.RESOURCE_NAME_DEFAULT_VALUE);

        bundleContext.registerService(SoundPack.class,
            new DefaultSoundPackImpl(),
            sndProps);

        Hashtable<String, String> settingsProps = new Hashtable<>();
        settingsProps.put(ResourcePack.RESOURCE_NAME,
            SettingsPack.RESOURCE_NAME_DEFAULT_VALUE);

        bundleContext.registerService(SettingsPack.class,
            new DefaultSettingsPackImpl(),
            settingsProps);

        Hashtable<String, String> langProps = new Hashtable<>();
        langProps.put(ResourcePack.RESOURCE_NAME,
            LanguagePack.RESOURCE_NAME_DEFAULT_VALUE);

        bundleContext.registerService(LanguagePack.class,
            new DefaultLanguagePackImpl(),
            langProps);

        if (logger.isInfoEnabled())
        {
            logger.info("Default resources ... [REGISTERED]");
        }
    }

    public void stop(BundleContext bc)
    {
    }

    /**
     * Finds all properties files for the given path in this bundle.
     *
     * @param path the path pointing to the properties files.
     */
    protected static List<String> findResourcePaths(String path,
        String pattern)
    {
        List<String> cachedResult = resourceFiles.get(path + pattern);
        if (cachedResult != null)
        {
            return cachedResult;
        }

        ArrayList<String> propertiesList = new ArrayList<>();

        Enumeration<URL> propertiesUrls = bundleContext.getBundle()
            .findEntries(path, pattern, false);

        if (propertiesUrls != null)
        {
            for (URL propertyUrl : Collections.list(propertiesUrls))
            {
                // Remove the first slash, then replace all slashes with dots.
                propertiesList.add(
                    propertyUrl.getPath().substring(1).replaceAll("/", "."));
            }
        }

        resourceFiles.put(path + pattern, propertiesList);
        return propertiesList;
    }
}
