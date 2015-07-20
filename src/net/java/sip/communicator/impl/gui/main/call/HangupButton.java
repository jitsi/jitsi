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
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The hangup button shown in the call window.
 *
 * @author Yana Stamcheva
 */
public class HangupButton
    extends CallToolBarButton
{
    /**
     * Creates an instance of <tt>HangupButton</tt>, by specifying the parent
     * call panel.
     *
     * @param callPanel the parent call panel
     */
    public HangupButton(final CallPanel callPanel)
    {
        super(  ImageLoader.getImage(ImageLoader.HANGUP_BUTTON_BG),
                GuiActivator.getResources()
                    .getI18NString("service.gui.HANG_UP"));

        addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                callPanel.actionPerformedOnHangupButton(false);
            }
        });
    }
}
