/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.launcher;

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
        WelcomeWindow welcomeWindow = new WelcomeWindow();
        
        welcomeWindow.pack();
        welcomeWindow.setVisible(true);
        
        Main.main(args);        
    }
}
