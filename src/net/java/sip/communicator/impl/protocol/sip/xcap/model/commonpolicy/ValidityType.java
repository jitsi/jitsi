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

/**
 * The Authorization Rules validity element.
 * <p/>
 * Compliant with rfc5025
 *
 * @author Grigorii Balutsel
 */
public class ValidityType
{
    /**
     * The list of from elements.
     */
    private List<String> fromList;

    /**
     * The list of until elements.
     */
    private List<String> untilList;

    /**
     * Gets the value of the fromList property.
     *
     * @return the fromList property.
     */
    public List<String> getFromList()
    {
        if (fromList == null)
        {
            fromList = new ArrayList<String>();
        }
        return fromList;
    }

    /**
     * Gets the value of the untilList property.
     *
     * @return the untilList property.
     */
    public List<String> getUntilList()
    {
        if (untilList == null)
        {
            untilList = new ArrayList<String>();
        }
        return untilList;
    }
}
