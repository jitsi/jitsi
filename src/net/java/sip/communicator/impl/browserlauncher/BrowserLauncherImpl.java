/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.browserlauncher;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.service.configuration.*;
import org.jitsi.util.*;

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
     * The name of the property which holds the colon-separated list of browsers
     * to try on linux.
     */
    private static String LINUX_BROWSERS_PROP_NAME
            = "net.java.sip.communicator.impl.browserlauncher.LINUX_BROWSERS";
    /**
     * The <tt>Logger</tt> instance used by the <tt>BrowserLauncherImpl</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(BrowserLauncherImpl.class);

    /**
     * The name of the browser executable to use on linux
     */
    private static String linuxBrowser = null;

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
            String browser = getLinuxBrowser();

            if (browser == null)
                logger.error("Could not find web browser");
            else
                Runtime.getRuntime().exec(new String[]{browser, url});
        }
    }

    /**
     * Gets the name (or absolute path) to the executable to use as a browser
     * on linux.
     *
     * @return the name (or absolute path) to the executable to use as a browser
     * on linux.
     *
     * @throws Exception on failure from <tt>Runtime.exec()</tt>
     */
    private String getLinuxBrowser()
            throws Exception
    {
        if (linuxBrowser == null)
        {
            ConfigurationService cfg
                    = BrowserLauncherActivator.getConfigurationService();
            if (cfg != null)
            {
                String browsers= cfg.getString(LINUX_BROWSERS_PROP_NAME);
                if (browsers== null)
                {
                    logger.error("Required property not set: " +
                            LINUX_BROWSERS_PROP_NAME);
                    return null;
                }

                Runtime runtime = Runtime.getRuntime();
                for (String b : browsers.split(":"))
                {
                    if (runtime.exec(new String[] { "which", b }).waitFor() == 0)
                    {
                        linuxBrowser = b;
                        break;
                    }
                }
            }
        }

        return linuxBrowser;
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
