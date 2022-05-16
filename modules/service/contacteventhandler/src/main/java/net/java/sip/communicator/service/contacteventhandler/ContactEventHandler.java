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
package net.java.sip.communicator.service.contacteventhandler;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ContactEventHandler</tt> is meant to be used from other bundles in
 * order to change the default behavior of events generated when clicking
 * a contact. The GUI implementation should take in consideration all registered
 * <tt>ContactEventHandler</tt>s when managing contact list events.
 *
 * @author Yana Stamcheva
 */
public interface ContactEventHandler
{
    /**
     * Indicates that a contact in the contact list was clicked.
     *
     * @param contact the selected <tt>Contact</tt>
     * @param clickCount the count of clicks
     */
    public void contactClicked(Contact contact, int clickCount);
}
