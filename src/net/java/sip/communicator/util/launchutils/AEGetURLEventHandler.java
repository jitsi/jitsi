/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.launchutils;

/**
 * Registers as listener for kAEGetURL AppleScript events.
 * And will handle any url coming from the OS by passing it to LaunchArgHandler.
 *
 * @author Lubomir Marinov
 * @author Damian Minkov
 */
public class AEGetURLEventHandler
{
    private LaunchArgHandler launchArgHandler;

    /**
     * The interface for the used callback.
     */
    public interface IAEGetURLListener
    {
        /**
         * Handle the URL event.
         *
         * @param url the URL
         */
        void handleAEGetURLEvent (String url);
    }

    AEGetURLEventHandler(LaunchArgHandler launchArgHandler)
    {
        this.launchArgHandler = launchArgHandler;

        try
        {
            setAEGetURLListener (new IAEGetURLListener ()
            {
                public void handleAEGetURLEvent (final String url)
                {
                    new Thread()
                    {
                        @Override
                        public void run()
                        {
                            AEGetURLEventHandler.this.launchArgHandler.
                                handleArgs(new String[]{url});
                        }
                    }.start();
                }
            });
        }
        catch(Throwable err)
        {
            //we don't have logging here so dump to stderr
            System.err.println(
                    "Warning: Failed to register our command line argument"
                        + " handler. We won't be able to handle command line"
                        + " arguments.");
            err.printStackTrace();

        }
    }

    /**
     * Sets the (global) listener for kAEGetURL AppleScript events.
     * <p>
     * The listener should be prepared to handle any pending events before this
     * method returns because such events may have already been sent by the
     * operating system (e.g. when the application wasn't running and was
     * started in order to handle such an event).
     * </p>
     *
     * @param listener the {@link IAEGetURLListener} to be set as the (global)
     *                 listener for kAEGetURL AppleScript events
     */
    private static native void setAEGetURLListener (IAEGetURLListener listener);
}
