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

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.plaf.*;
import net.java.sip.communicator.util.skin.*;
/**
 * The SIPCommMenuBar is a <tt>JMenuBar</tt> without border decoration that can
 * be used as a container for other components, like selector boxes that won't
 * need a menu decoration.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SIPCommMenuBar
    extends JMenuBar
    implements Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Class id key used in UIDefaults.
     */
    private static final String UIClassID = "SIPCommMenuBarUI";

    /**
     * Adds the ui class to UIDefaults.
     */
    static
    {
        UIManager.getDefaults().put(UIClassID,
            SIPCommMenuBarUI.class.getName());
    }

    /**
     * Creates an instance of <tt>SIPCommMenuBar</tt>.
     */
    public SIPCommMenuBar()
    {
        loadSkin();
    }

    /**
     * Reload UI defs.
     */
    public void loadSkin()
    {
        this.setBorder(BorderFactory.createEmptyBorder());
    }

    /**
    * Returns the name of the L&F class that renders this component.
    *
    * @return the string "TreeUI"
    * @see JComponent#getUIClassID
    * @see UIDefaults#getUI
    */
    @Override
    public String getUIClassID()
    {
        return UIClassID;
    }
}
