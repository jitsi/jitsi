/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.launcher;

import java.awt.*;
import java.io.*;

import org.apache.felix.main.*;

/**
 * Starts the SIP Communicator.
 * 
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class SIPCommunicator
{
    private static final String PNAME_SC_HOME_DIR_LOCATION =
            "net.java.sip.communicator.SC_HOME_DIR_LOCATION";

    private static final String PNAME_SC_HOME_DIR_NAME =
            "net.java.sip.communicator.SC_HOME_DIR_NAME";

    /**
     * Starts the SIP Communicator.
     * 
     * @param args
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

        if (version.startsWith("1.4") || vmVendor.startsWith("Gnu"))
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

        Main.main(args);
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
             * releases.
             */
            if ((new File(location, name).isDirectory() == false)
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
