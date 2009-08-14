/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.shutdown;

/**
 * Abstracts the shutdown-related procedures of the application so that they
 * can be used throughout various bundles.
 *
 * @author Linus Wallgren
 */
public interface ShutdownService
{

    /**
     * Invokes the UI action commonly associated with the "File &gt; Quit" menu
     * item which begins the application shutdown procedure.
     * <p>
     * The method avoids duplication since the "File &gt; Quit" functionality
     * may be invoked not only from the main application menu but also from the
     * systray, for example.
     * </p>
     */
    public void beginShutdown();
}
