/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
    /**
     * Creates a <tt>LaunchBrowser</tt> thread for the specified <tt>url</tt>.
     *
     * @param url the url we'd like to launch a browser for.
     */
    public void openURL(String url)
    {
        new LaunchBrowser(url).start();
    }

    /**
     * Launch browser in a separate thread.
     */
    private static class LaunchBrowser extends Thread
    {
        /**
         * The URL we'd be launching a browser for.
         */
        private final String url;

        /**
         * Creates a new instance.
         *
         * @param url the url we'd like to launch a browser for.
         */
        public LaunchBrowser(String url)
        {
            this.url = url;
        }

        /**
         * On mac, asks FileManager to open the the url, on Windows uses
         * FileProtocolHandler to do so, on Linux, loops through a list of
         * known browsers until we find one that seems to work.
         */
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
                   String[] browsers = {"firefox", "iceweasel", "opera",
                             "konqueror", "epiphany", "mozilla", "netscape" };

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
