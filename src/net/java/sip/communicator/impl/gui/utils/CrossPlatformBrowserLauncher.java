/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import java.net.MalformedURLException;
import java.net.URL;

import net.java.sip.communicator.impl.gui.main.message.ChatWindow;
import net.java.sip.communicator.util.Logger;
import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.BrowserLauncherRunner;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;
import edu.stanford.ejalbert.exceptionhandler.BrowserLauncherErrorHandler;

/**
 * Launches a browser, depending on the operation system and the browsers
 * available.
 *  
 * @author Yana Stamcheva
 */
public class CrossPlatformBrowserLauncher {

    private static Logger logger = Logger.getLogger(ChatWindow.class.getName());
    
	private static BrowserLauncher launcher;
	
    /**
     * Launches a browser for the given url, depending on the operation system
     * and the browsers available.
     * 
     * @param url The url to open in the browser.
     */
    public static void openURL(String urlString) {
        
		try {
            launcher = new BrowserLauncher(null);
                
    		if (urlString == null || urlString.trim().length() == 0) {
                throw new MalformedURLException("You must specify a url.");
            }
            new URL(urlString); // may throw MalformedURLException
            BrowserLauncherErrorHandler errorHandler = new BrowserErrorHandler();
            String targetBrowser = launcher.getBrowserList().get(0).toString();
            
            BrowserLauncherRunner runner = new BrowserLauncherRunner(
                    launcher,
                    targetBrowser,
                    urlString,
                    errorHandler);
            Thread launcherThread = new Thread(runner);
            launcherThread.start();
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
