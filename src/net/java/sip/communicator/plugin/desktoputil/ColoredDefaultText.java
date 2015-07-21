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
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;

/**
 * The purpose of this interface is to allow UI components with a default
 * text value to give the default text its own colour, set independently of
 * the normal text colour.
 * @author Tom Denham
 */
public interface ColoredDefaultText
{
    /**
     * Sets the foreground color.
     *
     * @param c the color to set for the text field foreground
     */
    public void setForegroundColor(Color c);

    /**
     * Gets the foreground color.
     *
     * @return the color of the text
     */
    public Color getForegroundColor();

    /**
     * Sets the foreground color of the default text shown in this text field.
     *
     * @param c the color to set
     */
    public void setDefaultTextColor(Color c);

    /**
     * Gets the foreground color of the default text shown in this text field.
     *
     * @return the color of the default text
     */
    public Color getDefaultTextColor();
}
