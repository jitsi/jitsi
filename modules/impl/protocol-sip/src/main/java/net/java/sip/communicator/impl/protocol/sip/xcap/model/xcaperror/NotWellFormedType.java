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
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror;

/**
 * The XCAP not-well-formed element. Indicates that the body of the request was
 * not a well-formed XML document.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public class NotWellFormedType extends BaseXCapError
{
    /**
     * Creates the XCAP not-well-formed error with phrase attribute.
     *
     * @param phrase the phrase to set.
     */
    public NotWellFormedType(String phrase)
    {
        super(phrase);
    }
}
