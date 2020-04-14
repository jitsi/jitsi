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
package net.java.sip.communicator.service.gui.event;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;

/**
 * Listens for the chat's right click menu becoming visible so menu items can
 * be offered.
 *
 * @author Damian Johnson
 */
public interface ChatMenuListener
{
    /**
     * Provides menu items that should be contributed.
     * @param source chat to which the menu belongs
     * @param event mouse event triggering menu
     * @return elements that should be added to the menu
     */
    List<JMenuItem> getMenuElements(Chat source, MouseEvent event);
}
