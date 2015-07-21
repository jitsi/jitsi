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
package net.java.sip.communicator.impl.gui.customcontrols.events;

/*
 * The following code is borrowed from David Bismut, davidou@mageos.com Intern,
 * SETLabs, Infosys Technologies Ltd. May 2004 - Jul 2004 Ecole des Mines de
 * Nantes, France
 */

import java.awt.*;
import java.awt.event.*;

/**
 * @author Yana Stamcheva
 */
public class CloseTabbedPaneEvent
    extends Event
{
    private final String description;

    private final MouseEvent e;

    private final int overTabIndex;

    public CloseTabbedPaneEvent(MouseEvent e, String description,
            int overTabIndex) {
        super(null, 0, null);
        this.e = e;
        this.description = description;
        this.overTabIndex = overTabIndex;
    }

    public String getDescription() {
        return description;
    }

    public MouseEvent getMouseEvent() {
        return e;
    }

    public int getOverTabIndex() {
        return overTabIndex;
    }
}
