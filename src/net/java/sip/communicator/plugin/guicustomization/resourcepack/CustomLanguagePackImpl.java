/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.guicustomization.resourcepack;

import java.util.*;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import net.java.sip.communicator.service.resources.*;

/**
 *
 * @author Damian Minkov
 */
public class CustomLanguagePackImpl
    implements LanguagePack
{
    private ArrayList localeList = new ArrayList();
    
    public CustomLanguagePackImpl()
    {
        try
        {
            JarFile jf = new JarFile(getJarfileName());

            Enumeration resources = jf.entries();
            while (resources.hasMoreElements())
            {
                JarEntry je = (JarEntry) resources.nextElement();

                Locale locale;
                String entryName = je.getName();
                if (entryName.matches("resources/languages/.*\\.properties"))
                {
                    int localeIndex = entryName.indexOf('_');

                    if (localeIndex == -1)
                        locale = new Locale("EN");
                    else
                    {
                        String localeName =
                            entryName.substring(localeIndex + 1,
                                                entryName.indexOf('.'));

                        locale = new Locale(localeName);
                    }

                    localeList.add(locale);
                }
            }
        }
        catch (java.io.IOException e)
        {
//            logger.error("Cannot load locales.", e);
        }
    }
    
    public String getResourcePackBaseName()
    {
        return "resources.languages.resources";
    }

    public String getName()
    {
        return "Language Resources";
    }

    public String getDescription()
    {
        return "Provide SIP Communicator Language resource pack.";
    }

    public Iterator getAvailableLocales()
    {
        return localeList.iterator();
    }
    
    private String getJarfileName()
    {
        // Get the location of the jar file and the jar file name
        java.net.URL outputURL =
            CustomLanguagePackImpl.class.getProtectionDomain().getCodeSource()
                .getLocation();

        String outputString = outputURL.toString();

        String[] parseString;
        parseString = outputString.split("file:");

        String jarFilename = parseString[1];
        return jarFilename;
    }
}
