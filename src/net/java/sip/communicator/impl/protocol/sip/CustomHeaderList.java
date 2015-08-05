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

import java.util.*;

/**
 * Custom header to insert. Custom name and value.
 *
 * @author Damian Minkov
 */
public class CustomHeaderList
    extends SIPHeaderList
{
    /**
     * Constructs header.
     * @param name header name
     * @param value header value
     */
    public CustomHeaderList(String name, String value)
    {
        super(CustomHeader.class, name);
        add(new CustomHeader(name, value));
    }

    /**
     * Constructs header list by name.
     * @param hName the name of the header.
     */
    public CustomHeaderList(String hName) {
        super( CustomHeader.class, hName);
    }

    /**
     * Constructs header list.
     */
    public CustomHeaderList() {
        super(CustomHeader.class,null);
    }

    /**
     * Clones this list.
     * @return the cloned list.
     */
    public Object clone() {
        CustomHeaderList retval = new CustomHeaderList(headerName);
        retval.clonehlist(this.hlist);
        return retval;
    }

    /**
     * Encodes the headers every header on separate line. Skip combining of
     * headers.
     * @param buffer the current message.
     * @return the message with added headers.
     */
    @Override
    public StringBuilder encode(StringBuilder buffer)
    {
        ListIterator<SIPHeader> li = hlist.listIterator();
        while (li.hasNext())
        {
            li.next().encode(buffer);
        }

        return buffer;
    }
}
