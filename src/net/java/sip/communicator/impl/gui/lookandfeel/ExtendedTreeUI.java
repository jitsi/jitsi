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
package net.java.sip.communicator.impl.gui.lookandfeel;

import java.awt.event.*;

import javax.swing.tree.*;

import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.plugin.desktoputil.plaf.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>ExtendedTreeUI</tt> is an extended implementation of the
 * <tt>SIPCommTreeUI</tt> specific for the gui implementation.
 */
public class ExtendedTreeUI
    extends SIPCommTreeUI
{
    /**
     * Do not select the <tt>ShowMoreContact</tt>.
     *
     * @param path the <tt>TreePath</tt> to select
     * @param event the <tt>MouseEvent</tt> that provoked the select
     */
    @Override
    protected void selectPathForEvent(TreePath path, MouseEvent event)
    {
        Object lastComponent = path.getLastPathComponent();

        // Open right button menu when right mouse is pressed.
        if (lastComponent instanceof ContactNode)
        {
            UIContact uiContact
                = ((ContactNode) lastComponent).getContactDescriptor();

            if (!(uiContact instanceof ShowMoreContact))
            {
                super.selectPathForEvent(path, event);
            }
        }
        else
            super.selectPathForEvent(path, event);
    }
}
