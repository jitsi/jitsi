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
package net.java.sip.communicator.impl.swingnotification;

import net.java.sip.communicator.service.systray.*;

/**
 * Empty popup message handler. Used when we want to disable popup messages.
 * @author Damian Minkov
 */
public class NonePopupMessageHandlerImpl
    extends AbstractPopupMessageHandler
{
    /**
     * Implements <tt>PopupMessageHandler#showPopupMessage()</tt>
     * Doing nothing.
     *
     * @param popupMessage the message we will show
     */
    @Override
    public void showPopupMessage(PopupMessage popupMessage)
    {}

    /**
     * Implements <tt>getPreferenceIndex</tt> from
     * <tt>NonePopupMessageHandlerImpl</tt>.
     * This handler is a empty one, thus the preference index is 0.
     * @return a preference index
     */
    @Override
    public int getPreferenceIndex()
    {
        return 0;
    }

    /**
     * Implements <tt>toString</tt> from <tt>PopupMessageHandler</tt>
     * @return a description of this handler
     */
    @Override
    public String toString()
    {
        return SwingNotificationActivator.getResources()
            .getI18NString("service.gui.NONE");
    }
}
