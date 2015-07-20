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
package net.java.sip.communicator.impl.protocol.sip.xcap;

/**
 * XCAP client interface.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public interface XCapClient extends HttpXCapClient,
        XCapCapsClient, ResourceListsClient,
        PresRulesClient, PresContentClient
{
    /**
     * Gets information about XCAP resource-lists support information.
     *
     * @return true if resource-lists is supported.
     */
    public boolean isResourceListsSupported();

    /**
     * Gets information about XCAP pres-rules support information.
     *
     * @return true if pres-rules is supported.
     */
    public boolean isPresRulesSupported();

    /**
     * Gets information about XCAP pres-content support information.
     *
     * @return true if pres-content is supported.
     */
    public boolean isPresContentSupported();

}
