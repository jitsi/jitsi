/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.launcher;

import com.apple.eio.*;

import net.java.sip.communicator.util.*;

/**
 * A simple implementation of the BrowserLauncherService. Checks the operating
 * system and launches the appropriate browser.
 * 
 * @author Yana Stamcheva
 */
public class BrowserLauncher
{
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
                if (OSUtils.IS_MAC)
                {
                    FileManager.openURL(url);
                }
                else if (OSUtils.IS_WINDOWS)
                {
                   Runtime.getRuntime().exec(
                       "rundll32 url.dll,FileProtocolHandler " + url);
                }
                else
                {
                   /* Linux and other Unix systems */
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
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
