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
package net.java.sip.communicator.launcher;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import net.java.sip.communicator.launchutils.*;
import org.jitsi.impl.osgi.framework.*;
import org.jitsi.impl.osgi.framework.launch.*;
import org.jitsi.osgi.framework.*;
import org.osgi.framework.*;
import org.osgi.framework.launch.*;
import org.osgi.framework.startlevel.*;
import org.reflections.*;
import org.reflections.util.*;
import org.slf4j.*;
import org.slf4j.bridge.*;

/**
 * Starts Jitsi.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Emil Ivov
 * @author Sebastien Vincent
 */
public class Jitsi
{
    /**
     * Legacy home directory names that we can use if current dir name is the
     * currently active name (overridableDirName).
     */
    private static final String[] LEGACY_DIR_NAMES
        = { ".sip-communicator", "SIP Communicator" };

    /**
     * The name of the property that stores the home dir for cache data, such
     * as avatars and spelling dictionaries.
     */
    public static final String PNAME_SC_CACHE_DIR_LOCATION =
            "net.java.sip.communicator.SC_CACHE_DIR_LOCATION";

    /**
     * The name of the property that stores the home dir for application log
     * files (not history).
     */
    public static final String PNAME_SC_LOG_DIR_LOCATION =
            "net.java.sip.communicator.SC_LOG_DIR_LOCATION";

    /**
     * Name of the possible configuration file names (used under macosx).
     */
    private static final String[] LEGACY_CONFIGURATION_FILE_NAMES
        = {
            "sip-communicator.properties",
            "jitsi.properties"
        };

    /**
     * The currently active name.
     */
    private static final String OVERRIDABLE_DIR_NAME = "Jitsi";

    /**
     * The name of the property that stores our home dir location.
     */
    public static final String PNAME_SC_HOME_DIR_LOCATION
        = "net.java.sip.communicator.SC_HOME_DIR_LOCATION";

    /**
     * The name of the property that stores our home dir name.
     */
    public static final String PNAME_SC_HOME_DIR_NAME
        = "net.java.sip.communicator.SC_HOME_DIR_NAME";

    /**
     * Starts Jitsi.
     *
     * @param args command line args if any
     * @throws Exception whenever it makes sense.
     */
    public static void main(String[] args)
        throws Exception
    {
        try (var cl = new BundleClassLoader(Jitsi.class.getClassLoader()))
        {
            var c = cl.loadClass(Jitsi.class.getName());
            var m = c.getDeclaredMethod("mainWithCl", String[].class);
            m.invoke(null, (Object) args);
        }
    }

    public static void mainWithCl(String[] args)
        throws Exception
    {
        init();
        handleArguments(args);
        var fw = startCustomOsgi();
        fw.waitForStop(0);
    }

    private static Framework startCustomOsgi() throws BundleException
    {
        var options = Map.of(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "3");
        Framework fw = new FrameworkImpl(options, Jitsi.class.getClassLoader());
        fw.init();
        var bundleContext = fw.getBundleContext();
        var reflections = new Reflections(new ConfigurationBuilder()
            .addClassLoaders(Jitsi.class.getClassLoader())
            .forPackages("org.jitsi", "net.java.sip"));

        for (final var activator : reflections.getSubTypesOf(BundleActivator.class))
        {
            if ((activator.getModifiers() & Modifier.ABSTRACT) == Modifier.ABSTRACT)
            {
                continue;
            }

            var url = activator.getProtectionDomain().getCodeSource().getLocation().toString();
            var bundle = bundleContext.installBundle(url);
            var startLevel = bundle.adapt(BundleStartLevel.class);
            startLevel.setStartLevel(2);
            var bundleActivator = bundle.adapt(BundleActivatorHolder.class);
            bundleActivator.addBundleActivator(activator);
        }

        new SplashScreenUpdater(bundleContext.getBundles().length, bundleContext);
        fw.start();
        return fw;
    }

    private static void init()
    {
        setSystemProperties();
        setScHomeDir();
        Logger logger = LoggerFactory.getLogger(Jitsi.class);
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        logger.info("home={}, cache={}, log={}, dir={}",
            System.getProperty(PNAME_SC_HOME_DIR_LOCATION),
            System.getProperty(PNAME_SC_CACHE_DIR_LOCATION),
            System.getProperty(PNAME_SC_LOG_DIR_LOCATION),
            System.getProperty(PNAME_SC_HOME_DIR_NAME));
    }

    private static void handleArguments(String[] args)
    {
        //first - pass the arguments to our arg handler
        LaunchArgHandler argHandler = LaunchArgHandler.getInstance();
        int argHandlerRes = argHandler.handleArgs(args);

        if ( argHandlerRes == LaunchArgHandler.ACTION_EXIT
            || argHandlerRes == LaunchArgHandler.ACTION_ERROR)
        {
            System.err.println("ArgHandler error: " + argHandler.getErrorCode());
            System.exit(argHandler.getErrorCode());
        }

        //lock our config dir so that we would only have a single instance of
        //sip communicator, no matter how many times we start it (use mainly
        //for handling sip: uris after starting the application)
        if ( argHandlerRes != LaunchArgHandler.ACTION_CONTINUE_LOCK_DISABLED )
        {
            switch (new JitsiLock().tryLock(args))
            {
            case JitsiLock.LOCK_ERROR:
                System.err.println("Failed to lock Jitsi's "
                    +"configuration directory.\n"
                    +"Try launching with the --multiple param.");
                System.exit(JitsiLock.LOCK_ERROR);
                break;
            case JitsiLock.ALREADY_STARTED:
                System.out.println(
                    "Jitsi is already running and will "
                        +"handle your parameters (if any).\n"
                        +"Launch with the --multiple param to override this "
                        +"behaviour.");

                //we exit with success because for the user that's what it is.
                System.exit(JitsiLock.SUCCESS);
                break;
            case JitsiLock.SUCCESS:
                //Successfully locked, continue as normal.
                break;
            }
        }
    }

    /**
     * Sets the system properties net.java.sip.communicator.SC_HOME_DIR_LOCATION
     * and net.java.sip.communicator.SC_HOME_DIR_NAME (if they aren't already
     * set) in accord with the OS conventions specified by the name of the OS.
     * Please leave the access modifier as package (default) to allow launch-
     * wrappers to call it.
     *
     */
    static void setScHomeDir()
    {
        /*
         * Though we'll be setting the SC_HOME_DIR_* property values depending
         * on the OS running the application, we have to make sure we are
         * compatible with earlier releases i.e. use
         * ${user.home}/.sip-communicator if it exists (and the new path isn't
         * already in use).
         */
        String profileLocation = System.getProperty(PNAME_SC_HOME_DIR_LOCATION);
        String cacheLocation = System.getProperty(PNAME_SC_CACHE_DIR_LOCATION);
        String logLocation = System.getProperty(PNAME_SC_LOG_DIR_LOCATION);
        String name = System.getProperty(PNAME_SC_HOME_DIR_NAME);

        boolean isHomeDirnameForced = name != null;

        if (profileLocation == null
            || cacheLocation == null
            || logLocation == null
            || name == null)
        {
            String defaultLocation = System.getProperty("user.home");
            String defaultName = ".jitsi";

            // Whether we should check legacy names
            // 1) when such name is not forced we check
            // 2) if such is forced and is the overridableDirName check it
            //      (the later is the case with name transition SIP Communicator
            //      -> Jitsi, check them only for Jitsi)
            boolean chekLegacyDirNames
                = (name == null) || name.equals(OVERRIDABLE_DIR_NAME);

            if (System.getProperty("os.name", "unknown").contains("Mac"))
            {
                if (profileLocation == null)
                    profileLocation =
                            System.getProperty("user.home") + File.separator
                            + "Library" + File.separator
                            + "Application Support";
                if (cacheLocation == null)
                    cacheLocation =
                        System.getProperty("user.home") + File.separator
                        + "Library" + File.separator
                        + "Caches";
                if (logLocation == null)
                    logLocation =
                        System.getProperty("user.home") + File.separator
                        + "Library" + File.separator
                        + "Logs";

                if (name == null)
                    name = OVERRIDABLE_DIR_NAME;
            }
            else if (System.getProperty("os.name", "unknown").contains("Windows"))
            {
                /*
                 * Primarily important on Vista because Windows Explorer opens
                 * in %USERPROFILE% so .sip-communicator is always visible. But
                 * it may be a good idea to follow the OS recommendations and
                 * use APPDATA on pre-Vista systems as well.
                 */
                if (profileLocation == null)
                    profileLocation = System.getenv("APPDATA");
                if (cacheLocation == null)
                    cacheLocation = System.getenv("LOCALAPPDATA");
                if (logLocation == null)
                    logLocation = System.getenv("LOCALAPPDATA");
                if (name == null)
                    name = OVERRIDABLE_DIR_NAME;
            }

            // If there are no OS specifics, use the defaults
            if (profileLocation == null)
                profileLocation = defaultLocation;
            if (cacheLocation == null)
                cacheLocation = profileLocation;
            if (logLocation == null)
                logLocation = profileLocation;
            if (name == null)
                name = defaultName;

            /*
             * As it was noted earlier, make sure we're compatible with previous
             * releases. If the home dir name is forced (set as system property)
             * doesn't look for the default dir.
             */
            if (!isHomeDirnameForced
                && !new File(profileLocation, name).isDirectory()
                && new File(defaultLocation, defaultName).isDirectory())
            {
                profileLocation = defaultLocation;
                name = defaultName;
            }

            // if we need to check legacy names and there is no current home dir
            // already created
            if(chekLegacyDirNames
                    && !checkHomeFolderExist(profileLocation, name))
            {
                // now check whether a legacy dir name exists and use it
                for (String dir : LEGACY_DIR_NAMES)
                {
                    // check the platform specific directory
                    if (checkHomeFolderExist(profileLocation, dir))
                    {
                        name = dir;
                        break;
                    }

                    // now check it and in the default location
                    if (checkHomeFolderExist(defaultLocation, dir))
                    {
                        name = dir;
                        profileLocation = defaultLocation;
                        break;
                    }
                }
            }

            System.setProperty(PNAME_SC_HOME_DIR_LOCATION, profileLocation);
            System.setProperty(PNAME_SC_CACHE_DIR_LOCATION, cacheLocation);
            System.setProperty(PNAME_SC_LOG_DIR_LOCATION, logLocation);
            System.setProperty(PNAME_SC_HOME_DIR_NAME, name);
        }

        // when we end up with the home dirs, make sure we have log dir
        new File(new File(logLocation, name), "log").mkdirs();
    }

    /**
     * Checks whether home folder exists. Special situation checked under
     * macosx, due to created folder of the new version of the updater we may
     * end up with our settings in 'SIP Communicator' folder and having 'Jitsi'
     * folder created by the updater(its download location). So we check not
     * only the folder exist but whether it contains any of the known
     * configuration files in it.
     *
     * @param parent the parent folder
     * @param name the folder name to check.
     * @return whether folder exists.
     */
    static boolean checkHomeFolderExist(String parent, String name)
    {
        if(System.getProperty("os.name", "unknown").contains("Mac"))
        {
            for (String f : LEGACY_CONFIGURATION_FILE_NAMES)
            {
                if (new File(new File(parent, name), f).exists())
                    return true;
            }

            return false;
        }

        return new File(parent, name).isDirectory();
    }

    /**
     * Sets some system properties specific to the OS that needs to be set at
     * the very beginning of a program (typically for UI related properties,
     * before AWT is launched).
     */
    private static void setSystemProperties()
    {
        // disable Direct 3D pipeline (used for fullscreen) before
        // displaying anything (frame, ...)
        System.setProperty("sun.java2d.d3d", "false");

        // On Mac OS X when switch in fullscreen, all the monitors goes
        // fullscreen (turns black) and only one monitors has images
        // displayed. So disable this behavior because somebody may want
        // to use one monitor to do other stuff while having other ones with
        // fullscreen stuff.
        System.setProperty("apple.awt.fullscreencapturealldisplays", "false");
    }
}
