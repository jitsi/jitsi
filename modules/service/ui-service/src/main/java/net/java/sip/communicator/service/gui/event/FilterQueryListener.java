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

import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>FilterQueryListener</tt> is notified when a filter query finishes.
 *
 * @author Yana Stamcheva
 */
public interface FilterQueryListener
{
    /**
     * Indicates that the given <tt>query</tt> has finished with success, i.e.
     * the filter has returned results.
     * @param query the <tt>FilterQuery</tt>, where this listener is registered
     */
    public void filterQuerySucceeded(FilterQuery query);

    /**
     * Indicates that the given <tt>query</tt> has finished with failure, i.e.
     * no results for the filter were found.
     * @param query the <tt>FilterQuery</tt>, where this listener is registered
     */
    public void filterQueryFailed(FilterQuery query);
}
