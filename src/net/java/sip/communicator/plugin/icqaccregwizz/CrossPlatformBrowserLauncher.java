/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.icqaccregwizz;

import java.net.*;

import net.java.sip.communicator.util.*;
import edu.stanford.ejalbert.*;
import edu.stanford.ejalbert.exception.*;
import edu.stanford.ejalbert.exceptionhandler.*;

/**
 * Launches a browser, depending on the operation system and the browsers
 * available.
 *
 * @author Yana Stamcheva
 */
public class CrossPlatformBrowserLauncher {

    private static Logger logger = Logger.getLogger(
            CrossPlatformBrowserLauncher.class.getName());

	private static BrowserLauncher launcher;

    /**
     * Launches a browser for the given url, depending on the operation system
     * and the browsers available.
     *
     * @param urlString The url to open in the browser.
     */
    public static void openURL(String urlString) {

		try {
            launcher = new BrowserLauncher(null);

    		if (urlString == null || urlString.trim().length() == 0) {
                throw new MalformedURLException("You must specify a url.");
            }
            new URL(urlString); // may throw MalformedURLException
            launcher.openURLinBrowser(urlString);
        }
        catch (BrowserLaunchingInitializingException e) {
            logger.error("Failed to initialize browser launcher : " + e);
        }
        catch (UnsupportedOperatingSystemException e) {
            logger.error("The operating system is not supported "
                    + "by browser launcher implementation : " + e);
        }
        catch (MalformedURLException e) {
            logger.error("The URL string could not be parsed : " + e);
        }
    }

    /**
     * The error handler to be passed to the browser launcher runner.
     */
    private static class BrowserErrorHandler
        implements BrowserLauncherErrorHandler {

        public void handleException(Exception ex) {
            logger.error(ex);
        }
    }
}
