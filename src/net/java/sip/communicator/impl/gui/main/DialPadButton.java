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
package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The dial pad button in the contact list.
 *
 * @author Yana Stamcheva
 */
public class DialPadButton
    extends SIPCommTextButton
{
    /**
     * The dial pad dialog that this button opens.
     */
    GeneralDialPadDialog dialPad;

    /**
     * Creates an instance of <tt>DialPadButton</tt>.
     */
    public DialPadButton()
    {
        super("");

        loadSkin();
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Load the defaults (registers with notification service)
        DTMFHandler.loadDefaults();

        addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (dialPad == null)
                    dialPad = new GeneralDialPadDialog();

                dialPad.clear();
                dialPad.setVisible(true);
            }
        });
    }

    /**
     * Loads images and sets history view.
     */
    public void loadSkin()
    {
        Image image = ImageLoader.getImage(ImageLoader.CONTACT_LIST_DIAL_BUTTON);

        setBgImage(image);

        this.setPreferredSize(new Dimension(image.getWidth(this),
                                            image.getHeight(this)));
    }
}
