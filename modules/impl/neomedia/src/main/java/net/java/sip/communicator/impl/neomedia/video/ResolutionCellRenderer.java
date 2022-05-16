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
package net.java.sip.communicator.impl.neomedia.video;

import java.awt.*;
import javax.swing.*;

/**
 * Renders the available resolutions in the combo box.
 */
public class ResolutionCellRenderer
    extends DefaultListCellRenderer
{
    /**
     * Sets readable text describing the resolution if the selected value is
     * null we return the string "Auto".
     */
    @Override
    public Component getListCellRendererComponent(
        JList list,
        Object value,
        int index,
        boolean isSelected,
        boolean cellHasFocus)
    {
        // call super to set backgrounds and fonts
        super.getListCellRendererComponent(
            list,
            value,
            index,
            isSelected,
            cellHasFocus);

        // now just change the text
        if (value == null)
        {
            setText("Auto");
        }
        else if (value instanceof Dimension)
        {
            Dimension d = (Dimension) value;

            setText((int) d.getWidth() + "x" + (int) d.getHeight());
        }
        return this;
    }
}
