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
package net.java.sip.communicator.service.protocol.event;

/**
 * The <tt>ContactResourceListener</tt> listens for events related to
 * <tt>ContactResource</tt>-s. It is notified each time a
 * <tt>ContactResource</tt> has been added, removed or modified.
 *
 * @author Yana Stamcheva
 */
public interface ContactResourceListener
{
    /**
     * Called when a new <tt>ContactResource</tt> has been added to the list
     * of available <tt>Contact</tt> resources.
     *
     * @param event the <tt>ContactResourceEvent</tt> that notified us
     */
    public void contactResourceAdded(ContactResourceEvent event);

    /**
     * Called when a <tt>ContactResource</tt> has been removed to the list
     * of available <tt>Contact</tt> resources.
     *
     * @param event the <tt>ContactResourceEvent</tt> that notified us
     */
    public void contactResourceRemoved(ContactResourceEvent event);

    /**
     * Called when a <tt>ContactResource</tt> in the list of available
     * <tt>Contact</tt> resources has been modified.
     *
     * @param event the <tt>ContactResourceEvent</tt> that notified us
     */
    public void contactResourceModified(ContactResourceEvent event);
}
