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
package net.java.sip.communicator.impl.gui.customcontrols.wizard;

import java.util.*;

public class WizardEvent
    extends EventObject
{
    private final int eventCode;

    /**
     * Indicates that the wizard triggering this event has finished
     * successfully.
     */
    public static final int SUCCESS = 1;

    /**
     * Indicates that the wizard was canceled.
     */
    public static final int CANCEL = 2;

    /**
     * Indicates that an error occured and the wizard hasn't been able to
     * finish.
     */
    public static final int ERROR = 3;

    /**
     * Creates a new WizardEvent according to the given source and event code.
     *
     * @param source the source where this event occurred
     * @param eventCode the event code : SUCCESS or ERROR
     */
    public WizardEvent(Object source, int eventCode) {
        super(source);

        this.eventCode = eventCode;
    }

    /**
     * Returns the event code of this event : SUCCESS or ERRROR.
     * @return the event code of this event : SUCCESS or ERRROR
     */
    public int getEventCode()
    {
        return this.eventCode;
    }
}
