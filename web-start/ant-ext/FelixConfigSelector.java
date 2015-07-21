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
import java.io.*;
import java.util.*;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.selectors.*;

/**
 * Selector which lets those files through that are referenced in the felix
 * configuration file.
 */
public class FelixConfigSelector
    implements FileSelector
{
    private Set<File> referencedBundleCache;

    private File felixConfig;

    public void setFelixConfig(File felixConifg)
    {
        if (!felixConifg.isFile())
            throw new BuildException("No felix configuration file provided.");

        this.felixConfig = felixConifg;
    }

    @Override
    public boolean isSelected(File basedir, String filename, File file)
    {
        cacheConfigEntries(basedir);
        return referencedBundleCache.contains(file);
    }

    private void cacheConfigEntries(File basedir)
    {
        if (referencedBundleCache != null)
            return;

        // cache files referenced in felix config
        referencedBundleCache = new HashSet<File>();

        // load felix config
        Properties pIn = new Properties();
        try
        {
            pIn.load(new FileInputStream(felixConfig));
        }
        catch (FileNotFoundException e)
        {
            throw new BuildException(e);
        }
        catch (IOException e)
        {
            throw new BuildException(e);
        }

        for (Map.Entry<Object, Object> e : pIn.entrySet())
        {
            if (((String) e.getKey()).startsWith("felix.auto.start."))
            {
                String[] refs = ((String) e.getValue()).split("\\s");
                for (String jar : refs)
                {
                    if (jar.startsWith("reference:file:"))
                    {
                        String relPath =
                            jar.substring("reference:file:".length());
                        String absPath =
                            basedir.getPath() + File.separator + relPath;
                        File f = new File(absPath);
                        if (f.isFile())
                        {
                            referencedBundleCache.add(f);
                        }
                        else
                        {
                            System.out.println(
                                "WARNING: Referenced file does not exist: "
                                    + f.getPath());
                        }
                    }
                }
            }
        }

    }

}
