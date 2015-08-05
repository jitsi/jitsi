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
import org.w3c.dom.*;

/**
 * The Authorization Rules one element.
 * <p/>
 * Compliant with rfc5025
 *
 * @author Grigorii Balutsel
 */
public class OneType
{
    /**
     * The id attribute
     */
    private String id;

    /**
     * The any element.
     */
    private Element any;

    /**
     * Create the one element.
     */
    public OneType()
    {
    }

    /**
     * Create the one element with the id attribute.
     *
     * @param id the id attribute.
     * @throws IllegalArgumentException if uri attribute is null or empty.
     */
    public OneType(String id)
    {
        if (StringUtils.isNullOrEmpty(id))
        {
            throw new IllegalArgumentException("id cannot be null or empty");
        }
        this.id = id;
    }

    /**
     * Gets the value of the id property.
     *
     * @return the id property.
     */
    public String getId()
    {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param id the id to set.
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * Gets the value of the any property.
     *
     * @return the any property.
     */
    public Element getAny()
    {
        return any;
    }

    /**
     * Sets the value of the any property.
     *
     * @param any the name to set.
     */
    public void setAny(Element any)
    {
        this.any = any;
    }
}
