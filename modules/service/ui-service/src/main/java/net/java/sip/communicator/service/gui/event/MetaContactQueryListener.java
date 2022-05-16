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

/**
 * The <tt>MetaContactQueryListener</tt> listens for events coming from a
 * <tt>MetaContactListService</tt> filtering.
 *
 * @author Yana Stamcheva
 */
public interface MetaContactQueryListener
{
    /**
     * Indicates that a <tt>MetaContact</tt> has been received for a search in
     * the <tt>MetaContactListService</tt>.
     * @param event the received <tt>MetaContactQueryEvent</tt>
     */
    public void metaContactReceived(MetaContactQueryEvent event);

    /**
     * Indicates that a <tt>MetaGroup</tt> has been received from a search in
     * the <tt>MetaContactListService</tt>.
     * @param event the <tt>MetaGroupQueryEvent</tt> that has been received
     */
    public void metaGroupReceived(MetaGroupQueryEvent event);

    /**
     * Indicates that a query has changed its status.
     * @param event the <tt>MetaContactQueryStatusEvent</tt> that notified us
     */
    public void metaContactQueryStatusChanged(MetaContactQueryStatusEvent event);
}
