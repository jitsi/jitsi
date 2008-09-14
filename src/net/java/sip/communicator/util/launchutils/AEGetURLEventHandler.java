/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.util.launchutils;

/**
 * Registers as listener for Apple Event GURL.
 * And will handle any url comming from the os by passing it to LaunchArgHandler.
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
        void handleAEGetURLEvent (String url);
    }

    AEGetURLEventHandler(LaunchArgHandler launchArgHandler)
    {
        this.launchArgHandler = launchArgHandler;

        setAEGetURLListener (new IAEGetURLListener ()
        {
            public void handleAEGetURLEvent (final String url)
            {
                new Thread()
                {
                    public void run()
                    {
                        AEGetURLEventHandler.this.launchArgHandler.
                            handleArgs(new String[]{url});
                    }                    
                }.start();
            }
        });
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
