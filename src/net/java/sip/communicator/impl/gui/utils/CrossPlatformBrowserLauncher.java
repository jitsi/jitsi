/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JOptionPane;

import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.BrowserLauncherRunner;
import edu.stanford.ejalbert.exceptionhandler.BrowserLauncherErrorHandler;

/**
 * Launches a browser, depending on the operation system and the browsers
 * available.
 *  
 * @author Yana Stamcheva
 */
public class CrossPlatformBrowserLauncher {

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
            BrowserLauncherErrorHandler errorHandler = new TestAppErrorHandler();
            String targetBrowser = launcher.getBrowserList().get(0).toString();
            
            BrowserLauncherRunner runner = new BrowserLauncherRunner(
                    launcher,
                    targetBrowser,
                    urlString,
                    errorHandler);
            Thread launcherThread = new Thread(runner);
            launcherThread.start();
        }
        catch (Exception ex) {
            // show message to user
            JOptionPane.showMessageDialog(null,
                                          ex.getMessage(),
                                          "Error Message",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static class TestAppErrorHandler
        implements BrowserLauncherErrorHandler {
        
        public void handleException(Exception ex) {
            // show message to user
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(),
                                          ex.getMessage(),
                                          "Error Message",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
}
