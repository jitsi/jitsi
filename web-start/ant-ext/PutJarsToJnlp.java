/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
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
