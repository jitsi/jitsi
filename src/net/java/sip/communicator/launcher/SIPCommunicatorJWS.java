/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.launcher;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.logging.*;

import net.java.sip.communicator.util.FileHandler;
import net.java.sip.communicator.util.*;

public class SIPCommunicatorJWS
{
    public static void main(String[] args) throws Exception
    {
        // allow access to everything
        System.setSecurityManager(null);

        // optional: the name of another class with a main method
        // that should be started in the same JVM:
        String chainMain = System.getProperty("chain.main.class");
        if(chainMain != null)
        {
            // optional: a space-separated list of arguments to be passed
            // to the chained main() method:
            String chainArgs = System.getProperty("chain.main.args");
            if(chainArgs == null)
            {
                chainArgs = "";
            }
            final String[] _chainArgs = chainArgs.split("\\s");
            try
            {
                Class<?> c = Class.forName(chainMain);
                final Method m = c.getMethod("main", _chainArgs.getClass());
                new Thread()
                {
                    public void run()
                    {
                        try
                        {
                            m.invoke(null, new Object[]{_chainArgs});
                        }
                        catch (Exception ex)
                        {
                            ex.printStackTrace();
                            System.err.println("Exception running the " +
                               "chained main class, will continue anyway.");
                        }
                    }
                }.start();
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                System.err.println("Exception finding the chained main " +
                    "class, will continue anyway.");
            }
        }

        // prepare the logger
        // needed by the FileHandler-Logger
        SIPCommunicator.setScHomeDir(System.getProperty("os.name"));
        LogManager.getLogManager()
            .readConfiguration(
                SIPCommunicatorJWS.class
                    .getResourceAsStream("/logging.properties"));

        for (Handler h : LogManager.getLogManager().getLogger("").getHandlers())
            LogManager.getLogManager().getLogger("").removeHandler(h);
        LogManager.getLogManager().getLogger("").addHandler(new FileHandler());
        LogManager.getLogManager().getLogger("")
            .addHandler(new ConsoleHandler());
        for (Handler h : LogManager.getLogManager().getLogger("").getHandlers())
            h.setFormatter(new ScLogFormatter());

        // be evil :-)
        // find the path of the nativelibs under webstart (findLibrary is
        // protected and therefore at least documented api)
        Method findLibrary =
            SIPCommunicatorJWS.class.getClassLoader().getClass()
                .getDeclaredMethod("findLibrary", String.class);
        findLibrary.setAccessible(true);
        File path =
            new File((String) findLibrary.invoke(
                SIPCommunicatorJWS.class.getClassLoader(), "hid"))
                .getParentFile();
        System.setProperty(
            "java.library.path",
            System.getProperty("java.library.path") + File.pathSeparator
                + path.getAbsolutePath());

        // reset sys_paths to re-read usr_paths (runtime-dependent and therefore
        // very very ugly :()
        Field sys_paths = ClassLoader.class.getDeclaredField("sys_paths");
        sys_paths.setAccessible(true);
        sys_paths.set(null, null);

        // prepare the felix-config with the absolute paths
        Properties pIn = new Properties();
        Properties pOut = new Properties();
        pIn.load(SIPCommunicatorJWS.class.getResourceAsStream(System
            .getProperty("felix.config.properties")));

        String baseServerUrl =
            System.getProperty("net.java.sip.communicator.SC_JWS_BASEDIR");
        ClassLoader cl = SIPCommunicatorJWS.class.getClassLoader();
        Method getJarFile =
            cl.getClass().getDeclaredMethod("getJarFile", URL.class);
        getJarFile.setAccessible(true);
        for (Map.Entry<Object, Object> e : pIn.entrySet())
        {
            if (((String) e.getKey()).startsWith("felix.auto.start."))
            {
                String[] refs = ((String) e.getValue()).split("\\s");
                StringBuilder sb = new StringBuilder();
                for (String ref : refs)
                {
                    JarFile localFile =
                        (JarFile) getJarFile.invoke(cl, new URL(baseServerUrl
                            + ref.replace("@URL@", "")));
                    if (localFile != null)
                    {
                        String localFileName =
                            new File(localFile.getName()).toURI().toString();
                        sb.append("reference:");
                        sb.append(localFileName);
                        sb.append(" ");
                    }
                    else
                    {
                        throw new Exception("ref <" + ref
                            + "> not found in cache");
                    }
                }
                pOut.put(e.getKey(), sb.toString());
            }
            else
            {
                pOut.put(e.getKey(), e.getValue());
            }
        }
        File jwsFelixConfig = File.createTempFile("jws", ".properties");
        jwsFelixConfig.deleteOnExit();
        pOut.store(new FileOutputStream(jwsFelixConfig),
            "--- autogenerated, do not edit! ---");
        System.setProperty("felix.config.properties", jwsFelixConfig.toURI()
            .toString());

        // Workaround broken desktop shortcut in ubuntu linux:
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6957030
        try
        {
            if (System.getProperty("os.name").equals("Linux"))
            {
                File desktop =
                    new File(System.getProperty("user.home") + "/Desktop");
                File[] files = desktop.listFiles();
                for (File file : files)
                {
                    if (file.getName().contains("jws_app_shortcut_"))
                    {
                        file.setExecutable(true, false);
                    }
                }
            }
        }
        catch (Exception e)
        {
        }

        // Handle the "-open" argument from the javaws command line
        Vector<String> _args = new Vector<String>();
        for(int i = 0; i < args.length ; i++)
        {
            String arg = args[i];
            if(arg.equalsIgnoreCase("-open"))
            {
                // are we at the last argument or is the next value
                // some other option flag?
                if(i == (args.length - 1) ||
                    (args[i+1].length() > 0 &&
                        "-/".indexOf(args[i+1].charAt(0))>=0))
                {
                    // invalid, can't use "-open" as final argument
                    System.err.println("Command line argument '-open'"
                        + " requires a parameter, usually a URI");
                    System.exit(1);
                }
            }
            else
            {
                _args.add(arg);
            }
        }

        // launch the original app
        SIPCommunicator.main(_args.toArray(new String[] {}));
    }
}
