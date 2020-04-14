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
package net.java.sip.communicator.impl.protocol.sip.xcap.model.prescontent;

import java.util.*;

import javax.xml.namespace.*;

/**
 * The PRES-CONTENT description element.
 * <p/>
 * Compliant with Presence Content XDM Specification v1.0
 *
 * @author Grigorii Balutsel
 */
public class DescriptionType
{
    /**
     * The element value.
     */
    private String value;

    /**
     * The lang attribute.
     */
    private String lang;

    /**
     * The map of any attributes.
     */
    private Map<QName, String> anyAttributes = new HashMap<QName, String>();

    /**
     * Creates description.
     */
    public DescriptionType()
    {
    }

    /**
     * Creates description with value.
     *
     * @param value the value property.
     */
    public DescriptionType(String value)
    {
        this(value, null);
    }

    /**
     * Creates description with value and lang properties.
     *
     * @param value the value property.
     * @param lang  the lang property.
     */
    public DescriptionType(String value, String lang)
    {
        this.value = value;
        this.lang = lang;
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

    /**
     * Gets the value of the lang property.
     *
     * @return the lang property.
     */
    public String getLang()
    {
        return lang;
    }

    /**
     * Sets the value of the lang property.
     *
     * @param lang the lang to set.
     */
    public void setLang(String lang)
    {
        this.lang = lang;
    }

    /**
     * Gets the value of the anyAttributes property.
     *
     * @return the anyAttributes property.
     */
    public Map<QName, String> getAnyAttributes()
    {
        return anyAttributes;
    }
}
