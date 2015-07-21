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
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * A whiteboard change listener receives events indicating that a whiteboard
 * has changed and a participant has either left or joined.
 *
 * @author Julien Waechter
 * @author Emil Ivov
 */
public interface WhiteboardChangeListener
    extends EventListener
{
    /**
     * Indicates that a new whiteboard participant has joined
     * the source whiteboard.
     *
     * @param evt the <tt>WhiteboardParticipantEvent</tt> containing the source
     * whiteboard and whiteboard participant.
     */
    public void whiteboardParticipantAdded(WhiteboardParticipantEvent evt);

    /**
     * Indicates that a whiteboard participant has left the source whiteboard.
     *
     * @param evt the <tt>WhiteboardParticipantEvent</tt> containing the source
     * whiteboard and whiteboard participant.
     */
    public void whiteboardParticipantRemoved(WhiteboardParticipantEvent evt);

    /**
     * Indicates that a change has occurred in the state of the source
     * whiteboard.
     *
     * @param evt the <tt>WhiteboardChangeEvent</tt> instance containing the
     * source whiteboards and its old and new state.
     */
    public void whiteboardStateChanged(WhiteboardChangeEvent evt);
}
