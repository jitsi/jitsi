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
 * A <tt>WhiteboardParticipantListener</tt> receives events notifying of changes
 * that have occurred within a <tt>WhiteboardParticipant</tt>. Such changes may
 * pertain to current whiteboard participant state, their display name,
 * address...
 *
 * @author Julien Waechter
 * @author Emil Ivov
 */
public interface WhiteboardParticipantListener
    extends EventListener
{
    /**
     * Indicates that a change has occurred in the status of the source
     * WhiteboardParticipant.
     *
     * @param evt The <tt>WhiteboardParticipantChangeEvent</tt>
     * instance containing the source event
     * as well as its previous and its new status.
     */
    public void participantStateChanged(WhiteboardParticipantChangeEvent evt);

    /**
     * Indicates that a change has occurred in the display name of the source
     * WhiteboardParticipant.
     *
     * @param evt The <tt>WhiteboardParticipantChangeEvent</tt>
     * instance containing the source event
     * as well as its previous and its new display names.
     */
    public void participantDisplayNameChanged(
            WhiteboardParticipantChangeEvent evt);

    /**
     * Indicates that a change has occurred in the image of the source
     * WhiteboardParticipant.
     *
     * @param evt The <tt>WhiteboardParticipantChangeEvent</tt>
     * instance containing the source event
     * as well as its previous and its new image.
     */
    public void participantImageChanged(WhiteboardParticipantChangeEvent evt);
}
