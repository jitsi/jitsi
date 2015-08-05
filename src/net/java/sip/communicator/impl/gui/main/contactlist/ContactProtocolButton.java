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
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The ContactProtocolButton is a button behind a "meta contact" in the
 * contactlist, which corresponds to a specific protocol contact. This
 * button allows opening a chat for a given "meta contact" by specifing
 * the exact protocol contact to use for the chat.
 *
 * @author Yana Stamcheva
 */
public class ContactProtocolButton
    extends SIPCommButton
{

    private Contact protocolContact;

    /**
     * Creates an instance of ContactProtocolButton.
     * @param bgImage The background image of the button.
     */
    public ContactProtocolButton(Image bgImage) {
        super(bgImage, bgImage);
    }

    /**
     * Returns the specific protocol contact corresponding to this button.
     * @return The specific protocol contact corresponding to this button.
     */
    public Contact getProtocolContact() {
        return protocolContact;
    }

    /**
     * Sets the specific protocol contact corresponding to this button.
     * @param protocolContact The specific protocol contact.
     */
    public void setProtocolContact(Contact protocolContact) {
        this.protocolContact = protocolContact;
    }
}
