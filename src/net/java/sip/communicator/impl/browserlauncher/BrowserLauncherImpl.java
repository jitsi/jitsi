/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.browserlauncher;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.util.*;

import com.apple.eio.*;

/**
 * Implements a <tt>BrowserLauncherService</tt> which opens a specified URL in
 * an OS-specific associated browser.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class BrowserLauncherImpl
    implements BrowserLauncherService
{

    /**
     * The <tt>Logger</tt> instance used by the <tt>BrowserLauncherImpl</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(BrowserLauncherImpl.class);

    /**
     * Opens the specified URL in an OS-specific associated browser.
     *
     * @param url a <tt>String</tt> value which represents the URL to be opened
     * in a browser
     * @throws Exception if no associated browser was found for the specified
     * URL or there was an error during the instruction of the found associated
     * browser to open the specified URL
     */
    @SuppressWarnings("deprecation")
    private void launchBrowser(String url)
        throws Exception
    {
        if (OSUtils.IS_MAC)
        {
            FileManager.openURL(url);
        }
        else if (OSUtils.IS_WINDOWS)
        {
            Runtime
                .getRuntime()
                    .exec("rundll32 url.dll,FileProtocolHandler " + url);
        }
        else
        {
            String[] browsers
                = new String[]
                        {
                            "google-chrome",
                            "firefox",
                            "iceweasel",
                            "opera",
                            "konqueror",
                            "epiphany",
                            "mozilla",
                            "netscape",
                            "gnome-open"
                        };

            Runtime runtime = Runtime.getRuntime();
            String browser = null;

            for (String b : browsers)
                if (runtime.exec(new String[] { "which", b }).waitFor() == 0)
                    browser = b;

            if (browser == null)
                throw new Exception("Could not find web browser");
            else
                runtime.exec(new String[] { browser, url });
        }
    }

    /**
     * Tries to open the specified URL in a browser. The attempt is asynchronously
     * executed and does not wait for possible errors related to the launching
     * of the associated browser and the opening of the specified URL in it i.e.
     * the method returns immediately and does not report the success or the
     * failure of the opening.
     *
     * @param url a <tt>String</tt> value which represents the URL to be opened
     * in a browser
     * @see BrowserLauncherService#openURL(java.lang.String)
     */
    public void openURL(final String url)
    {
        Thread launchBrowserThread
            = new Thread(getClass().getName())
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                launchBrowser(url);
                            }
                            catch (Exception e)
                            {
                                logger.error("Failed to launch browser", e);
                            }
                        }
                    };

        launchBrowserThread.start();
    }
}
