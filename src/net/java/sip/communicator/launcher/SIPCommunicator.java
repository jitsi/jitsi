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

import java.awt.*;
import java.io.*;

import net.java.sip.communicator.impl.version.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.launchutils.*;

import org.apache.felix.main.*;

/**
 * Starts the SIP Communicator.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Emil Ivov
 * @author Sebastien Vincent
 */
public class SIPCommunicator
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
            "jitsi.properties",
            "sip-communicator.xml",
            "jitsi.xml"
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
     * Starts the SIP Communicator.
     *
     * @param args command line args if any
     * @throws Exception whenever it makes sense.
     */
    public static void main(String[] args)
        throws Exception
    {
        String version = System.getProperty("java.version");
        String vmVendor = System.getProperty("java.vendor");
        String osName = System.getProperty("os.name");

        setSystemProperties(osName);

        /*
         * SC_HOME_DIR_* are specific to the OS so make sure they're configured
         * accordingly before any other application-specific logic depending on
         * them starts (e.g. Felix).
         */
        setScHomeDir(osName);

        // this needs to be set before any DNS lookup is run
        File f
            = new File(
                    System.getProperty(PNAME_SC_HOME_DIR_LOCATION),
                    System.getProperty(PNAME_SC_HOME_DIR_NAME)
                        + File.separator
                        + ".usednsjava");
        if(f.exists())
        {
            System.setProperty(
                    "sun.net.spi.nameservice.provider.1",
                    "dns,dnsjava");
        }

        if (version.startsWith("1.5") || vmVendor.startsWith("Gnu") ||
                vmVendor.startsWith("Free"))
        {
            String os = "";

            if (osName.startsWith("Mac"))
                os = ChangeJVMFrame.MAC_OSX;
            else if (osName.startsWith("Linux"))
                os = ChangeJVMFrame.LINUX;
            else if (osName.startsWith("Windows"))
                os = ChangeJVMFrame.WINDOWS;

            ChangeJVMFrame changeJVMFrame = new ChangeJVMFrame(os);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            changeJVMFrame.setLocation(
                screenSize.width/2 - changeJVMFrame.getWidth()/2,
                screenSize.height/2 - changeJVMFrame.getHeight()/2);
            changeJVMFrame.setVisible(true);

            return;
        }

        //first - pass the arguments to our arg handler
        LaunchArgHandler argHandler = LaunchArgHandler.getInstance();
        int argHandlerRes = argHandler.handleArgs(args);

        if ( argHandlerRes == LaunchArgHandler.ACTION_EXIT
             || argHandlerRes == LaunchArgHandler.ACTION_ERROR)
        {
            System.exit(argHandler.getErrorCode());
        }

        //lock our config dir so that we would only have a single instance of
        //sip communicator, no matter how many times we start it (use mainly
        //for handling sip: uris after starting the application)
        if ( argHandlerRes != LaunchArgHandler.ACTION_CONTINUE_LOCK_DISABLED )
        {
            switch (new SipCommunicatorLock().tryLock(args))
            {
            case SipCommunicatorLock.LOCK_ERROR:
                System.err.println("Failed to lock Jitsi's "
                                +"configuration directory.\n"
                                +"Try launching with the --multiple param.");
                System.exit(SipCommunicatorLock.LOCK_ERROR);
                break;
            case SipCommunicatorLock.ALREADY_STARTED:
                System.out.println(
                    "Jitsi is already running and will "
                    +"handle your parameters (if any).\n"
                    +"Launch with the --multiple param to override this "
                    +"behaviour.");

                //we exit with success because for the user that's what it is.
                System.exit(SipCommunicatorLock.SUCCESS);
                break;
            case SipCommunicatorLock.SUCCESS:
                //Successfully locked, continue as normal.
                break;
            }
        }

        String currentVersion =
            VersionImpl.VERSION_MAJOR + "." + VersionImpl.VERSION_MINOR;
        File jitsiVersion
            = new File(new File(
                System.getProperty(PNAME_SC_CACHE_DIR_LOCATION),
                System.getProperty(PNAME_SC_HOME_DIR_NAME)),
                    ".lastversion");
        if (jitsiVersion.exists())
        {
            BufferedReader r = new BufferedReader(new FileReader(jitsiVersion));
            String lastVersion = r.readLine();
            r.close();

            if (!currentVersion.equals(lastVersion))
            {
                File felixCache =
                    new File(new File(
                        System.getProperty(PNAME_SC_CACHE_DIR_LOCATION),
                        System.getProperty(PNAME_SC_HOME_DIR_NAME)),
                        "sip-communicator.bin");
                if (felixCache.exists())
                {
                    deleteRecursive(felixCache);
                }
            }
        }

        FileWriter fw = new FileWriter(jitsiVersion);
        fw.write(currentVersion);
        fw.close();

        //there was no error, continue;
        System.setOut(new ScStdOut(System.out));
        Main.main(new String[0]);
    }

    /**
     * Recursively delete a directory.
     * @param f The directory to the delete.
     * @throws IOException
     */
    private static void deleteRecursive(File f) throws IOException
    {
        if (f.isDirectory())
        {
            for (File c : f.listFiles())
            {
                deleteRecursive(c);
            }
        }

        f.delete();
    }

    /**
     * Sets the system properties net.java.sip.communicator.SC_HOME_DIR_LOCATION
     * and net.java.sip.communicator.SC_HOME_DIR_NAME (if they aren't already
     * set) in accord with the OS conventions specified by the name of the OS.
     *
     * Please leave the access modifier as package (default) to allow launch-
     * wrappers to call it.
     *
     * @param osName the name of the OS according to which the SC_HOME_DIR_*
     * properties are to be set
     */
    static void setScHomeDir(String osName)
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

            if (osName.startsWith("Mac"))
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
                    name = "Jitsi";
            }
            else if (osName.startsWith("Windows"))
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
                    name = "Jitsi";
            }

            /* If there're no OS specifics, use the defaults. */
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
                && (new File(profileLocation, name).isDirectory() == false)
                && new File(defaultLocation, defaultName).isDirectory())
            {
                profileLocation = defaultLocation;
                name = defaultName;
            }

            // if we need to check legacy names and there is no current home dir
            // already created
            if(chekLegacyDirNames
                    && !checkHomeFolderExist(profileLocation, name, osName))
            {
                // now check whether a legacy dir name exists and use it
                for(int i = 0; i < LEGACY_DIR_NAMES.length; i++)
                {
                    String dir = LEGACY_DIR_NAMES[i];

                    // check the platform specific directory
                    if(checkHomeFolderExist(profileLocation, dir, osName))
                    {
                        name = dir;
                        break;
                    }

                    // now check it and in the default location
                    if(checkHomeFolderExist(defaultLocation, dir, osName))
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
     * @param osName OS name
     * @return whether folder exists.
     */
    static boolean checkHomeFolderExist(
            String parent, String name, String osName)
    {
        if(osName.startsWith("Mac"))
        {
            for(int i = 0; i < LEGACY_CONFIGURATION_FILE_NAMES.length; i++)
            {
                String f = LEGACY_CONFIGURATION_FILE_NAMES[i];

                if(new File(new File(parent, name), f).exists())
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
     *
     * @param osName OS name
     */
    private static void setSystemProperties(String osName)
    {
        // setup here all system properties that need to be initialized at
        // the very beginning of an application
        if(osName.startsWith("Windows"))
        {
            // disable Direct 3D pipeline (used for fullscreen) before
            // displaying anything (frame, ...)
            System.setProperty("sun.java2d.d3d", "false");
        }
        else if(osName.startsWith("Mac"))
        {
            // On Mac OS X when switch in fullscreen, all the monitors goes
            // fullscreen (turns black) and only one monitors has images
            // displayed. So disable this behavior because somebody may want
            // to use one monitor to do other stuff while having other ones with
            // fullscreen stuff.
            System.setProperty("apple.awt.fullscreencapturealldisplays",
                "false");
        }
    }
}
