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

import org.jitsi.util.*;

/**
 * @author Lubomir Marinov
 */
public class SIPCommCheckBox
    extends JCheckBox
{
    private static final long serialVersionUID = 0L;

    private static final boolean setContentAreaFilled = (OSUtils.IS_WINDOWS
            || OSUtils.IS_LINUX);

    public SIPCommCheckBox()
    {
        init();
    }

    public SIPCommCheckBox(String text)
    {
        super(text);

        init();
    }

    public SIPCommCheckBox(String text, boolean selected)
    {
        super(text, selected);

        init();
    }

    private void init()
    {
        if (setContentAreaFilled)
            setContentAreaFilled(false);
    }
}
