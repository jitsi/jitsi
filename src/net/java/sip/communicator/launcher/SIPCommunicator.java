/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.launcher;

import java.awt.*;
import java.io.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.launchutils.*;

import org.apache.felix.main.*;

/**
 * Starts the SIP Communicator.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Emil Ivov
 */
public class SIPCommunicator
{
    /**
     * The name of the property that stores our home dir location.
     */
    public static final String PNAME_SC_HOME_DIR_LOCATION =
            "net.java.sip.communicator.SC_HOME_DIR_LOCATION";

    /**
     * The name of the property that stores our home dir name.
     */
    public static final String PNAME_SC_HOME_DIR_NAME =
            "net.java.sip.communicator.SC_HOME_DIR_NAME";

    /**
     * Starts the SIP Communicator.
     *
     * @param args command line args if any
     *
     * @throws Exception whenever it makes sense.
     */
    public static void main(String[] args)
        throws Exception
    {
        String version = System.getProperty("java.version");
        String vmVendor = System.getProperty("java.vendor");
        String osName = System.getProperty("os.name");

        /*
         * SC_HOME_DIR_* are specific to the OS so make sure they're configured
         * accordingly before any other application-specific logic depending on
         * them starts (e.g. Felix).
         */
        setScHomeDir(osName);

        if (version.startsWith("1.4") || vmVendor.startsWith("Gnu") ||
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

            changeJVMFrame.setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width/2
                    - changeJVMFrame.getWidth()/2,
                Toolkit.getDefaultToolkit().getScreenSize().height/2
                    - changeJVMFrame.getHeight()/2
                );
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
            SipCommunicatorLock lock = new SipCommunicatorLock();

            int lockResult = lock.tryLock(args);

            if( lockResult == SipCommunicatorLock.LOCK_ERROR )
            {
                System.err.println("Failed to lock SIP Communicator's "
                                +"configuration directory.\n"
                                +"Try launching with the --multiple param.");
                System.exit(SipCommunicatorLock.LOCK_ERROR);

            }
            else if(lockResult == SipCommunicatorLock.ALREADY_STARTED)
            {
                System.out.println(
                    "SIP Communicator is already running and will "
                    +"handle your parameters (if any).\n"
                    +"Launch with the --multiple param to override this "
                    +"behaviour.");

                //we exit with succes because for the user that's what it is.
                System.exit(SipCommunicatorLock.SUCCESS);
            }
            else if(lockResult == SipCommunicatorLock.SUCCESS)
            {
                //Successfully locked, continue as normal.
            }
        }

        //there was no error, continue;
        System.setOut(new ScStdOut(System.out));
        Main.main(new String[0]);
    }

    /**
     * Sets the system properties net.java.sip.communicator.SC_HOME_DIR_LOCATION
     * and net.java.sip.communicator.SC_HOME_DIR_NAME (if they aren't already
     * set) in accord with the OS conventions specified by the name of the OS.
     *
     * @param osName the name of the OS according to which the SC_HOME_DIR_*
     *            properties are to be set
     */
    private static void setScHomeDir(String osName)
    {
        /*
         * Though we'll be setting the SC_HOME_DIR_* property values depending
         * on the OS running the application, we have to make sure we are
         * compatible with earlier releases i.e. use
         * ${user.home}/.sip-communicator if it exists (and the new path isn't
         * already in use).
         */
        String location = System.getProperty(PNAME_SC_HOME_DIR_LOCATION);
        String name = System.getProperty(PNAME_SC_HOME_DIR_NAME);

        boolean isHomeDirnameForced = name != null;

        if ((location == null) || (name == null))
        {
            String defaultLocation = System.getProperty("user.home");
            String defaultName = ".sip-communicator";

            if (osName.startsWith("Mac"))
            {
                if (location == null)
                    location =
                            System.getProperty("user.home") + File.separator
                            + "Library" + File.separator
                            + "Application Support";
                if (name == null)
                    name = "SIP Communicator";
            }
            else if (osName.startsWith("Windows"))
            {
                /*
                 * Primarily important on Vista because Windows Explorer opens
                 * in %USERPROFILE% so .sip-communicator is always visible. But
                 * it may be a good idea to follow the OS recommendations and
                 * use APPDATA on pre-Vista systems as well.
                 */
                if (location == null)
                    location = System.getenv("APPDATA");
                if (name == null)
                    name = "SIP Communicator";
            }

            /* If there're no OS specifics, use the defaults. */
            if (location == null)
                location = defaultLocation;
            if (name == null)
                name = defaultName;

            /*
             * As it was noted earlier, make sure we're compatible with previous
             * releases. If the home dir name is forced (set as system property)
             * doesn't look for the default dir.
             */
            if (!isHomeDirnameForced
                && (new File(location, name).isDirectory() == false)
                && new File(defaultLocation, defaultName).isDirectory())
            {
                location = defaultLocation;
                name = defaultName;
            }

            System.setProperty(PNAME_SC_HOME_DIR_LOCATION, location);
            System.setProperty(PNAME_SC_HOME_DIR_NAME, name);
        }
    }
}
