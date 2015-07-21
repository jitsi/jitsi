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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Represents the <tt>parameter</tt> elements described in XEP-0167.
 *
 * @author Emil Ivov
 */
public class ParameterPacketExtension extends AbstractPacketExtension
{
    /**
     * The name of the "parameter" element.
     */
    public static final String ELEMENT_NAME = "parameter";

    /**
     * The name of the <tt>name</tt> parameter in the <tt>parameter</tt>
     * element.
     */
    public static final String NAME_ATTR_NAME = "name";

    /**
     * The name of the <tt>value</tt> parameter in the <tt>parameter</tt>
     * element.
     */
    public static final String VALUE_ATTR_NAME = "value";

    /**
     * Creates a new {@link ParameterPacketExtension} instance.
     */
    public ParameterPacketExtension()
    {
        super(null, ELEMENT_NAME);
    }

    /**
     * Creates a new {@link ParameterPacketExtension} instance and sets the
     * given name and value.
     */
    public ParameterPacketExtension(String name, String value)
    {
        super(null, ELEMENT_NAME);

        setName(name);
        setValue(value);
    }

    /**
     * Sets the name of the format parameter we are representing here.
     *
     * @param name the name of the format parameter we are representing here.
     */
    public void setName(String name)
    {
        super.setAttribute(NAME_ATTR_NAME, name);
    }

    /**
     * Returns the name of the format parameter we are representing here.
     *
     * @return the name of the format parameter we are representing here.
     */
    public String getName()
    {
        return super.getAttributeAsString(NAME_ATTR_NAME);
    }

    /**
     * Sets that value of the format parameter we are representing here.
     *
     * @param value the value of the format parameter we are representing here.
     */
    public void setValue(String value)
    {
        super.setAttribute(VALUE_ATTR_NAME, value);
    }

    /**
     * Returns the value of the format parameter we are representing here.
     *
     * @return the value of the format parameter we are representing here.
     */
    public String getValue()
    {
        return super.getAttributeAsString(VALUE_ATTR_NAME);
    }
}
