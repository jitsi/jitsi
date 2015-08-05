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
package net.java.sip.communicator.impl.protocol.sip;

import gov.nist.javax.sip.header.*;

/**
 * Custom header to insert. Custom name and value.
 *
 * @author Damian Minkov
 */
public class CustomHeader
    extends SIPHeader
{
    /**
     * The header value.
     */
    private String value;

    /**
     * Constructs header.
     * @param name header name
     * @param value header value
     */
    public CustomHeader(String name, String value)
    {
        super(name);
        this.value = value;
    }

    /**
     * Just the encoded body of the header.
     * @param buffer the insert result
     * @return the string encoded header body.
     */
    @Override
    protected StringBuilder encodeBody(StringBuilder buffer)
    {
        return value != null ? buffer.append(value) : buffer.append("");
    }

    /**
     * Clones this object.
     */
    public Object clone()
    {
        return new CustomHeader(headerName, value);
    }
}
