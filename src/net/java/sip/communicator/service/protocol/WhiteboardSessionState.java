/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * The WhiteboardSessionState class reflects the current state of a whiteboard
 * session.
 *
 * @author Julien Waechter
 * @author Emil Ivov
 */
public class WhiteboardSessionState
{
    /**
     * This constant containing a String representation of the
     * WHITEBOARD_INITIALIZATION state.
     * <p>
     * This constant has the String value "Initializing".
     */
    public static final String _WHITEBOARD_INITIALIZATION  = "Initializing";

    /**
     * This constant value indicates that the associated whiteboard
     * is currently in an initialization state.
     */
    public static final WhiteboardSessionState WHITEBOARD_INITIALIZATION
        = new WhiteboardSessionState(_WHITEBOARD_INITIALIZATION);

    /**
     * This constant containing a String representation
     * of the WHITEBOARD_IN_PROGRESS state.
     * <p>
     * This constant has the String value "In Progress".
     */
    public static final String _WHITEBOARD_IN_PROGRESS = "In Progress";

    /**
     * This constant value indicates that the associated whiteboard
     * is currently in an active state.
     */
    public static final WhiteboardSessionState WHITEBOARD_IN_PROGRESS
        = new WhiteboardSessionState(_WHITEBOARD_IN_PROGRESS);

    /**
     * This constant containing a String representation of the
     * WHITEBOARD_ENDED state.
     * <p>
     * This constant has the String value "Ended".
     */
    public static final String _WHITEBOARD_ENDED = "Ended";

    /**
     * This constant value indicates that the associated whiteboard
     * is currently in a terminated phase.
     */
    public static final WhiteboardSessionState WHITEBOARD_ENDED =
            new WhiteboardSessionState(_WHITEBOARD_ENDED);

    /**
     * A string representationf this Whiteboard State. Could be
     * _WHITEBOARD_INITIALIZATION, _WHITEBOARD_IN_PROGRESS, _WHITEBOARD_ENDED.
     */
    private String whiteboardStateStr;

    /**
     * Create a whiteboard state object with a value corresponding
     * to the specified string.
     * @param whiteboardState a string representation of the state.
     */
    private WhiteboardSessionState(String whiteboardState)
    {
        this.whiteboardStateStr = whiteboardState;
    }

    /**
     * Returns a String representation of tha WhiteboardSte.
     *
     * @return a string value (one of the _WHITEBOARD_XXX constants)
     * representing this whiteboard state).
     */
    public String getStateString()
    {
        return whiteboardStateStr;
    }

    /**
     * Returns a string represenation of this whiteboard state.
     * Strings returned by this method have the following format:
     * "WhiteboardState:<STATE_STRING>" and are meant to be used
     * for loggin/debugging purposes.
     * @return a string representation of this object.
     */
    public String toString()
    {
        return getClass().getName()+":"+getStateString();
    }
}
