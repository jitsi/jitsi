/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
