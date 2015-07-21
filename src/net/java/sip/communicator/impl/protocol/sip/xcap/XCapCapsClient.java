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

import net.java.sip.communicator.impl.protocol.sip.xcap.model.xcapcaps.*;

/**
 * XCAP xcap-caps client interface.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public interface XCapCapsClient
{
    /**
     * Xcap-caps uri format
     */
    public static String DOCUMENT_FORMAT = "xcap-caps/global/index";

    /**
     * Xcap-caps content type
     */
    public static String CONTENT_TYPE = "application/xcap-caps+xml";

    /**
     * Gets the xcap-caps from the server.
     *
     * @return the xcap-caps.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    public XCapCapsType getXCapCaps()
            throws XCapException;
}
