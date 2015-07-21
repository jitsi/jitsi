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
package net.java.sip.communicator.plugin.securityconfig.masterpassword;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * Implements a Swing <tt>Component</tt> to represent the user interface of the
 * Passwords <tt>ConfigurationForm</tt>.
 *
 * @author Dmitri Melnikov
 * @author Lubomir Marinov
 */
public class ConfigurationPanel
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Initializes a new <tt>ConfigurationPanel</tt> instance.
     */
    public ConfigurationPanel()
    {
        add(new MasterPasswordPanel());
        add(Box.createVerticalStrut(10));
        add(new SavedPasswordsPanel());
    }
}
