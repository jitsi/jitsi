/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.browserlauncher;

/**
 * Represents a service which is able to open an URL in a browser.
 *
 * @author Yana Stamcheva
 */
public interface BrowserLauncherService
{

    /**
     * Tries to open the specified URL in a browser. The attempt is asynchronously
     * executed and does not wait for possible errors related to the launching
     * of the associated browser and the opening of the specified URL in it i.e.
     * the method returns immediately and does not report the success or the
     * failure of the opening.
     *
     * @param url a <tt>String</tt> value which represents the URL to be opened
     * in a browser
     */
    public void openURL(String url);
}
