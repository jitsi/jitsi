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
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

import java.util.*;

import net.java.sip.communicator.impl.protocol.sip.xcap.model.presrules.*;

import org.w3c.dom.*;

/**
 * The Authorization Rules actions element.
 * <p/>
 * Compliant with rfc5025
 *
 * @author Grigorii Balutsel
 */
public class ActionsType
{
    /**
     * The pres-rules sub-handling element.
     */
    private SubHandlingType subHandling;

    /**
     * The list of any elements.
     */
    private List<Element> any;

    /**
     * Gets the value of the subHandling property.
     *
     * @return the subHandling property.
     */
    public SubHandlingType getSubHandling()
    {
        return subHandling;
    }

    /**
     * Sets the value of the subHandling property.
     *
     * @param subHandling the subHandling to set.
     */
    public void setSubHandling(SubHandlingType subHandling)
    {
        this.subHandling = subHandling;
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
        return any;
    }
}
