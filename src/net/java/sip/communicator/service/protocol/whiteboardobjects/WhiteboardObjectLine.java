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

package net.java.sip.communicator.service.protocol.whiteboardobjects;

import net.java.sip.communicator.service.protocol.*;

/**
 * Used to access the content of instant whiteboard objects that are sent or
 * received via the WhiteboardOperationSet.
 *
 * @author Julien Waechter
 */
public interface WhiteboardObjectLine extends WhiteboardObject
{
    /**
     * A type string constant indicating that an object is of type line.
     */
    public static final String NAME = "WHITEBOARDOBJECTLINE";

    /**
     * Returns the coordinates of  start point for the line
     *
     * @return the start coordinates of this line.
     */
    public WhiteboardPoint getWhiteboardPointStart();

    /**
     * Returns the coordinates of  end point for the line
     *
     * @return the end coordinates of this line.
     */
    public WhiteboardPoint getWhiteboardPointEnd();

    /**
     * Sets the coordinates of start point for the line
     *
     * @param whiteboardPointStart the new start coordinates for this line.
     */
    public void setWhiteboardPointStart(WhiteboardPoint whiteboardPointStart);

    /**
     * Sets the coordinates of end point for the line
     *
     * @param whiteboardPointEnd the new end coordinates for this line.
     */
    public void setWhiteboardPointEnd(WhiteboardPoint whiteboardPointEnd);
}
