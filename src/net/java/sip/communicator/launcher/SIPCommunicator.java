/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.launcher;

import java.awt.*;

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
