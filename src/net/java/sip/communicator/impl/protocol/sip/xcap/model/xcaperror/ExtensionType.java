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

import java.util.*;

import org.w3c.dom.*;

/**
 * The XCAP extension element. Indicates an error condition that is defined by
 * an extension to XCAP. Clients that do not understand the content of the
 * extension element MUST discard the xcap-error document and treat the error
 * as an unqualified 409.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public class ExtensionType extends BaseXCapError
{
    /**
     * The list of any elements.
     */
    private List<Element> any;

    /**
     * Creates the XCAP extension error.
     */
    public ExtensionType()
    {
        super(null);
    }

    /**
     * Gets the value of the any property.
     *
     * @return the any property.
     */
    public List<Element> getAny()
    {
        if (any == null)
        {
            any = new ArrayList<Element>();
        }
        return this.any;
    }
}
