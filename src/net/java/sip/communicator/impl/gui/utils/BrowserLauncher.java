/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import edu.stanford.ejalbert.BrowserLauncherRunner;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

import net.java.sip.communicator.impl.gui.i18n.Messages;

/**
 * Launches a browser, depending on the operation system and the browsers
 * available.
 *  
 * @author Yana Stamcheva
 */
public class BrowserLauncher {

    //private static final String errMsg 
      //  = Messages.getString("launchBrowserError");

    /**
     * Launches a browser for the given url, depending on the operation system
     * and the browsers available.
     * 
     * @param url The url to open in the browser.
     */
    public static void openURL(String url) {
        
        edu.stanford.ejalbert.BrowserLauncher launcher;
        try {
            launcher = new edu.stanford.ejalbert.BrowserLauncher(null);
            
            BrowserLauncherRunner runner = new BrowserLauncherRunner(launcher, url, null);
            Thread launcherThread = new Thread(runner);
            launcherThread.start();            
        }
        catch (BrowserLaunchingInitializingException e) {
            e.printStackTrace();
        }
        catch (UnsupportedOperatingSystemException e) {
            e.printStackTrace();
        }
        
        /*
        String osName = System.getProperty("os.name");
        try {
            if (osName.startsWith("Mac OS")) {
                Class fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL",
                        new Class[] { String.class });
                openURL.invoke(null, new Object[] { url });
            }
            else if (osName.startsWith("Windows"))
                Runtime.getRuntime().exec(
                        "rundll32 url.dll,FileProtocolHandler " + url);
            else { // assume Unix or Linux
                String[] browsers = { "firefox", "opera", "konqueror",
                        "epiphany", "mozilla", "netscape" };
                String browser = null;
                for (int count = 0; count < browsers.length 
                                    && browser == null; count++) {
                    if (Runtime.getRuntime().exec(new String[] {
                            "which", browsers[count] }).waitFor() == 0)
                        browser = browsers[count];
                }
                if (browser == null)
                    throw new Exception("Could not find web browser");
                else
                    Runtime.getRuntime().exec(new String[] { browser, url });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, errMsg + ":\n"
                    + e.getLocalizedMessage());
        }
        */
    }
}
