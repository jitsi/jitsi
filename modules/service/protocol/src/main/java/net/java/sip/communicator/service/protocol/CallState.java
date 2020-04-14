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
package net.java.sip.communicator.service.protocol;

/**
 * The CallState class reflects the current state of a call. Compared to the
 * state of a single call peer, a call itself has a more limited amount
 * of sets which follow the following cycle:
 * <p>
 * CALL_INITIALIZATION -> CALL_IN_PROGRESS -> CALL_ENDED
 * <p>
 * When you start calling someone or receive a call alert, the call that is
 * automatically created is in the CALL_INITIALIZATION_PHASE. As soon as one of
 * the peers passes into a CONNECTED call peer state, the call
 * would enter the CALL_IN_PROGRESS state. When the last call peer enters
 * a DISCONNECTED state the call itself would go into the CALL_ENDED state and
 * will be ready for garbage collection.
 *
 * @author Emil Ivov
 */
public class CallState
{
    /**
     * This constant containing a String representation of the
     * CALL_INITIALIZATION state.
     * <p>
     * This constant has the String value "Initializing".
     */
    public static final String _CALL_INITIALIZATION = "Initializing";

    /**
     * This constant value indicates that the associated call is currently in an
     * initialization state.
     */
    public static final CallState CALL_INITIALIZATION =
        new CallState(_CALL_INITIALIZATION);

    /**
     * This constant containing a String representation of the CALL_IN_PROGRESS
     * state.
     * <p>
     * This constant has the String value "In Progress".
     */
    public static final String _CALL_IN_PROGRESS = "In Progress";

    /**
     * This constant value indicates that the associated call is currently in an
     * active state.
     */
    public static final CallState CALL_IN_PROGRESS =
        new CallState(_CALL_IN_PROGRESS);

    /**
     * This constant containing a String representation of the CALL_ENDED state.
     * <p>
     * This constant has the String value "Ended".
     */
    public static final String _CALL_ENDED = "Ended";

    /**
     * This constant value indicates that the associated call is currently in a
     * terminated phase.
     */
    public static final CallState CALL_ENDED = new CallState(_CALL_ENDED);

    /**
     * This constant containing a String representation of the CALL_REFERED
     * state.
     * <p>
     * This constant has the String value "Referred".
     */
    public static final String _CALL_REFERRED = "Refered";

    /**
     * This constant value indicates that the associated call is currently
     * referred.
     */
    public static final CallState CALL_REFERRED = new CallState(_CALL_REFERRED);

    /**
     * A string representation of this Call State. Could be
     * _CALL_INITIALIZATION, _CALL_IN_PROGRESS, _CALL_ENDED.
     */
    private String callStateStr;

    /**
     * Create a call state object with a value corresponding to the specified
     * string.
     *
     * @param callState a string representation of the state.
     */
    private CallState(String callState)
    {
        this.callStateStr = callState;
    }

    /**
     * Returns a String representation of this CallState.
     *
     * @return a string value (one of the _CALL_XXX constants) representing this
     *         call state).
     */
    public String getStateString()
    {
        return callStateStr;
    }

    /**
     * Returns a string representation of this call state. Strings returned by
     * this method have the following format: "CallState:<STATE_STRING>" and are
     * meant to be used for logging/debugging purposes.
     *
     * @return a string representation of this object.
     */
    @Override
    public String toString()
    {
        return getClass().getName() + ":" + getStateString();
    }
}
