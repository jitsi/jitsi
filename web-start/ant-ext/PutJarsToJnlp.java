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
import org.apache.tools.ant.*;

import java.io.*;
import java.util.*;

public class PutJarsToJnlp
    extends Task
{
    private File bundledir;

    private File osbundles;

    public void setBundledir(File b)
    {
        bundledir = b;
    }

    public void setOsbundles(File f)
    {
        osbundles = f;
    }

    public void execute() throws BuildException
    {
        try
        {
            execute0();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new BuildException(e);
        }
    }

    public void execute0() throws BuildException
    {
        StringBuilder common = new StringBuilder();
        for (File jar : bundledir.listFiles())
        {
            if (jar.isFile())
            {
                common.append("<jar href=\"sc-bundles/");
                common.append(jar.getName());
                common.append("\"/>\n");
            }
        }
        getProject().setProperty("jnlp.jars.common", common.toString());

        for (File dir : osbundles.listFiles())
        {
            if (dir.isDirectory())
            {
                StringBuilder os = new StringBuilder();
                for (File jar : dir.listFiles())
                {
                    if (jar.isFile())
                    {
                        os.append("<jar href=\"sc-bundles/os-specific/");
                        os.append(dir.getName());
                        os.append("/");
                        os.append(jar.getName());
                        os.append("\"/>\n");
                    }
                }
                getProject().setProperty("jnlp.jars." + dir.getName(),
                    os.toString());
            }
        }
    }
}
