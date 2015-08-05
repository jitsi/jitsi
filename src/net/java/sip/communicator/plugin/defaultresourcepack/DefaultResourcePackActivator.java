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
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 *
 * @author damencho
 */
public class DefaultResourcePackActivator
    implements BundleActivator
{
    private Logger logger =
        Logger.getLogger(DefaultResourcePackActivator.class);

    static BundleContext bundleContext;

    // buffer for ressource files found
    private static Hashtable<String, Iterator<String>> ressourcesFiles =
        new Hashtable<String, Iterator<String>>();

    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        DefaultColorPackImpl colPackImpl =
            new DefaultColorPackImpl();

        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(ResourcePack.RESOURCE_NAME,
                  ColorPack.RESOURCE_NAME_DEFAULT_VALUE);

        bundleContext.registerService(  ColorPack.class.getName(),
                                        colPackImpl,
                                        props);

        DefaultImagePackImpl imgPackImpl =
            new DefaultImagePackImpl();

        Hashtable<String, String> imgProps = new Hashtable<String, String>();
        imgProps.put(ResourcePack.RESOURCE_NAME,
                    ImagePack.RESOURCE_NAME_DEFAULT_VALUE);

        bundleContext.registerService(  ImagePack.class.getName(),
                                        imgPackImpl,
                                        imgProps);

        DefaultLanguagePackImpl langPackImpl =
            new DefaultLanguagePackImpl();

        Hashtable<String, String> langProps = new Hashtable<String, String>();
        langProps.put(ResourcePack.RESOURCE_NAME,
                    LanguagePack.RESOURCE_NAME_DEFAULT_VALUE);

        bundleContext.registerService(  LanguagePack.class.getName(),
                                        langPackImpl,
                                        langProps);

        DefaultSettingsPackImpl setPackImpl =
            new DefaultSettingsPackImpl();

        Hashtable<String, String> setProps = new Hashtable<String, String>();
        setProps.put(ResourcePack.RESOURCE_NAME,
                      SettingsPack.RESOURCE_NAME_DEFAULT_VALUE);

        bundleContext.registerService(  SettingsPack.class.getName(),
                                        setPackImpl,
                                        setProps);

        DefaultSoundPackImpl sndPackImpl =
            new DefaultSoundPackImpl();

        Hashtable<String, String> sndProps = new Hashtable<String, String>();
        sndProps.put(ResourcePack.RESOURCE_NAME,
                      SoundPack.RESOURCE_NAME_DEFAULT_VALUE);

        bundleContext.registerService(  SoundPack.class.getName(),
                                        sndPackImpl,
                                        sndProps);

        if (logger.isInfoEnabled())
            logger.info("Default resources ... [REGISTERED]");
    }

    public void stop(BundleContext bc) throws Exception
    {

    }

    /**
     * Finds all properties files for the given path in this bundle.
     *
     * @param path the path pointing to the properties files.
     */
    protected static Iterator<String> findResourcePaths(  String path,
                                                            String pattern)
    {
        Iterator<String> bufferedResult = ressourcesFiles.get(path + pattern);
        if (bufferedResult != null) {
            return bufferedResult;
        }

        ArrayList<String> propertiesList = new ArrayList<String>();

        @SuppressWarnings ("unchecked")
        Enumeration<URL> propertiesUrls = bundleContext.getBundle()
            .findEntries(path,
                        pattern,
                        false);

        if (propertiesUrls != null)
        {
            while (propertiesUrls.hasMoreElements())
            {
                URL propertyUrl = propertiesUrls.nextElement();

                // Remove the first slash.
                String propertyFilePath
                    = propertyUrl.getPath().substring(1);

                // Replace all slashes with dots.
                propertyFilePath = propertyFilePath.replaceAll("/", ".");

                propertiesList.add(propertyFilePath);
            }
        }

        Iterator<String> result = propertiesList.iterator();
        ressourcesFiles.put(path + pattern, result);

        return result;
    }
}
