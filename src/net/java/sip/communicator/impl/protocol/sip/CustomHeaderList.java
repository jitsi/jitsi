/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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