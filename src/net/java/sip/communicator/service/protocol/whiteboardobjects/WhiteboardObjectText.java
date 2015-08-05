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
public interface WhiteboardObjectText extends WhiteboardObject
{
    /**
     * A type string constant indicating that an object is of type text.
     */
    public static final String NAME = "WHITEBOARDOBJECTTEXT";

    /**
     * Returns the coordinates of this whiteboard object.
     *
     * @return the coordinates of this object.
     */
    public WhiteboardPoint getWhiteboardPoint ();

    /**
     * Sets the coordinates of this whiteboard object.
     *
     * @param whiteboardPoint the coordinates of this object.
     */
    public void setWhiteboardPoint (WhiteboardPoint whiteboardPoint);

    /**
     * Returns the WhiteboardObjectText's text.
     *
     * @return the WhiteboardObjectText's text.
     */
    public String getText();

    /**
     * Sets the WhiteboardObjectText's text.
     *
     * @param text the new WhiteboardObjectText's text.
     */
    public void setText(String text);

    /**
     * Returns the WhiteboardObjectText's font size.
     *
     * @return the WhiteboardObjectText's font size.
     */
    public int getFontSize();

    /**
     * Sets the WhiteboardObjectText's font size.
     *
     * @param fontSize the new WhiteboardObjectText's font size.
     */
    public void setFontSize(int fontSize);

    /**
     * Returns the WhiteboardObjectText's font name.
     * (By default Dialog)
     *
     * @return the new WhiteboardObjectText's font name.
     */
    public String getFontName();

    /**
     * Sets the WhiteboardObjectText's font name.
     *
     * @param fontName the new WhiteboardObjectText's font name.
     */
    public void setFontName(String fontName);
}
