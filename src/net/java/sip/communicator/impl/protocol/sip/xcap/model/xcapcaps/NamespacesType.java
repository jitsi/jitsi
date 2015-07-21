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
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcapcaps;

import java.util.*;

/**
 * The XCAP-CAPS namespaces element.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public class NamespacesType
{
    /**
     * The list of the namespace elements.
     */
    protected List<String> namespace;

    /**
     * Gets the value of the auid property.
     *
     * @return the namespace property.
     */
    public List<String> getNamespace()
    {
        if (namespace == null)
        {
            namespace = new ArrayList<String>();
        }
        return this.namespace;
    }
}
