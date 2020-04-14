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
package net.java.sip.communicator.service.contactsource;

/**
 * The <tt>DemuxContactSourceService</tt> provides a de-multiplexed copy of
 * the given <tt>ContactSourceService</tt>, where each contact detail like
 * telephone number or protocol contact address is represented as a single entry
 * in the query result set.
 *
 * @author Yana Stamcheva
 */
public abstract class DemuxContactSourceService
{
    /**
     * Creates a demultiplexed copy of the given <tt>ContactSourceService</tt>,
     * where each contact detail like telephone number or protocol contact
     * address is represented as a single entry in the query result set.
     *
     * @param contactSourceService the original <tt>ContactSourceService</tt> to
     * be demultiplexed
     * @return a demultiplexed copy of the given <tt>ContactSourceService</tt>
     */
    public abstract ContactSourceService createDemuxContactSource(
        ContactSourceService contactSourceService);
}
