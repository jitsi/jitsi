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
 */
public class SIPCommunicator
{
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

        /**
         * Check whether default config folder exists.
         * If it exists we use it. Otherwise use the settings coming 
         * from system properties. And set SC_HOME_DIR_LOCATION 
         * as we cannot set it when building dmg packet.
         * This is done cause moving the config folder and preventing
         * not using existing data for users already using default folder. 
         */
        if (osName.startsWith("Mac"))
        {
            String scDefultDirName = ".sip-communicator";
            
            String scHomeDirLocation = 
                System.getProperty("net.java.sip.communicator.SC_HOME_DIR_LOCATION");
            
            if(scHomeDirLocation == null)
            {
                String defaultAppDirName = 
                    System.getProperty("user.home") + 
                    File.separator + 
                    scDefultDirName;

                if(new File(defaultAppDirName).exists())
                {
                    System.setProperty("net.java.sip.communicator.SC_HOME_DIR_LOCATION", 
                            System.getProperty("user.home"));
                    System.setProperty("net.java.sip.communicator.SC_HOME_DIR_NAME", 
                            scDefultDirName);
                }
                else
                {
                    System.setProperty("net.java.sip.communicator.SC_HOME_DIR_LOCATION", 
                            System.getProperty("user.home") + File.separator +
                            "Library" + File.separator + "Application Support");
                }
            }
        }
        
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
}
