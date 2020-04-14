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
package net.java.sip.communicator.plugin.otr.authdialog;

import java.awt.*;

import javax.swing.*;

/**
     * A special {@link JTextArea} for use in the OTR authentication panels.
     * It is meant to be used for fingerprint representation and general
     * information display.
     *
     * @author George Politis
     */
    public class CustomTextArea
        extends JTextArea
    {
        public CustomTextArea()
        {
            this.setBackground(new Color(0,0,0,0));
            this.setOpaque(false);
            this.setColumns(20);
            this.setEditable(false);
            this.setLineWrap(true);
            this.setWrapStyleWord(true);
        }
    }
