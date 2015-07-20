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

import org.jitsi.util.*;

/**
 * The Authorization Rules sphere element.
 * <p/>
 * Compliant with rfc5025
 *
 * @author Grigorii Balutsel
 */
public class SphereType
{
    /**
     * The value attribute.
     */
    private String value;

    /**
     * Create the sphere element.
     */
    public SphereType()
    {
    }

    /**
     * Create the sphere element with the value attribute.
     *
     * @param value the value attribute.
     * @throws IllegalArgumentException if uri attribute is null or empty.
     */
    public SphereType(String value)
    {
        if (StringUtils.isNullOrEmpty(value))
        {
            throw new IllegalArgumentException("value cannot be null or empty");
        }
        this.value = value;
    }

    /**
     * Gets the value of the value property.
     *
     * @return the value property.
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value the value to set.
     */
    public void setValue(String value)
    {
        this.value = value;
    }
}
