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

import java.awt.*;
import java.awt.Desktop.*;
import java.net.*;
import net.java.sip.communicator.service.browserlauncher.*;
import org.apache.commons.lang3.SystemUtils;
import org.jitsi.service.configuration.*;

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
    private static final String LINUX_BROWSERS_PROP_NAME
            = "net.java.sip.communicator.impl.browserlauncher.LINUX_BROWSERS";
    /**
     * The <tt>Logger</tt> instance used by the <tt>BrowserLauncherImpl</tt>
     * class and its instances for logging output.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BrowserLauncherImpl.class);

    /**
     * The name of the browser executable to use on linux
     */
    private static String linuxBrowser = null;

    private final ConfigurationService configService;

    public BrowserLauncherImpl(ConfigurationService configService)
    {
        this.configService = configService;
    }

    /**
     * Opens the specified URL in an OS-specific associated browser.
     *
     * @param url a <tt>String</tt> value which represents the URL to be opened
     * in a browser
     * @throws Exception if no associated browser was found for the specified
     * URL or there was an error during the instruction of the found associated
     * browser to open the specified URL
     */
    private void launchBrowser(String url)
        throws Exception
    {
        if (Desktop.isDesktopSupported())
        {
            var desktop = Desktop.getDesktop();
            if (desktop != null && desktop.isSupported(Action.BROWSE))
            {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
        }

        if (SystemUtils.IS_OS_WINDOWS)
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
            String browsers= configService.getString(LINUX_BROWSERS_PROP_NAME);
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
        if (url == null || !url.startsWith("http"))
        {
            logger.warn("Not a valid URL to open:" + url);
            return;
        }
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
