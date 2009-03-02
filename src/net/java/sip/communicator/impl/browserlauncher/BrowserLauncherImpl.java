/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.browserlauncher;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.util.*;

import com.apple.eio.*;

/**
 * A simple implementation of the BrowserLauncherService. Checks the operating
 * system and launches the appropriate browser.
 * 
 * @author Yana Stamcheva
 */
public class BrowserLauncherImpl
    implements BrowserLauncherService
{
    private static Logger logger = Logger.getLogger(
        BrowserLauncherImpl.class.getName());

    public void openURL(String url)
    {
        new LaunchBrowser(url).start();
    }
    
    /**
     * Launch browser in a separate thread.
     */
    private static class LaunchBrowser extends Thread
    {
        private final String url;
        
        public LaunchBrowser(String url)
        {
            this.url = url;
        }
        
        public void run()
        {
            try
            {
                String osName = System.getProperty("os.name");
                
                if (osName.startsWith("Mac OS"))
                {
                    FileManager.openURL(url);
                }
                else if (osName.startsWith("Windows"))
                {
                   Runtime.getRuntime().exec(
                       "rundll32 url.dll,FileProtocolHandler " + url);
                }
                else
                {
                   String[] browsers = {"firefox", "iceweasel", "opera", "konqueror",
                       "epiphany", "mozilla", "netscape" };
                   
                   String browser = null;
                   
                   for (int i = 0; i < browsers.length && browser == null; i ++)
                   {
                      if (Runtime.getRuntime().exec(
                            new String[] {"which", browsers[i]}).waitFor() == 0)
                         browser = browsers[i];
                   }
                   if (browser == null)
                      throw new Exception("Could not find web browser");
                   else
                      Runtime.getRuntime().exec(new String[] {browser, url});
                }
            }
            catch (Exception e) {
               logger.error("Failed to launch browser: " + e);
            }
        }
    }
}
