/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright 2003-2005 Arthur van Hoff Rick Blair
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
package net.java.sip.communicator.impl.protocol.zeroconf.jmdns;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

/**
 * JmDNS service information.
 *
 * @version %I%, %G%
 * @author  Arthur van Hoff, Jeff Sonstein, Werner Randelshofer
 * @author Christian Vincenot
 */
public class ServiceInfo implements DNSListener
{
    private static Logger logger =
        Logger.getLogger(ServiceInfo.class.toString());
    public final static byte[] NO_VALUE = new byte[0];
    JmDNS dns;

    // State machine
    /**
     * The state of this service info.
     * This is used only for services announced by JmDNS.
     * <p/>
     * For proper handling of concurrency, this variable must be
     * changed only using methods advanceState(), revertState() and cancel().
     */
    private DNSState state = DNSState.PROBING_1;

    /**
     * Task associated to this service info.
     * Possible tasks are JmDNS.Prober, JmDNS.Announcer, JmDNS.Responder,
     * JmDNS.Canceler.
     */
    TimerTask task;

    String type;
    private String name;
    String server;
    int port;
    int weight;
    int priority;
    byte text[];
    private Map<String, Object> props;
    InetAddress addr;


    /**
     * Construct a service description for registrating with JmDNS.
     *
     * @param type fully qualified service type name,
     *      such as <code>_http._tcp.local.</code>.
     * @param name unqualified service instance name,
     *      such as <code>foobar</code>
     * @param port the local port on which the service runs
     * @param text string describing the service
     */
    public ServiceInfo(String type, String name, int port, String text)
    {
        this(type, name, port, 0, 0, text);
    }

    /**
     * Construct a service description for registrating with JmDNS.
     *
     * @param type     fully qualified service type name,
     *      such as <code>_http._tcp.local.</code>.
     * @param name     unqualified service instance name,
     *      such as <code>foobar</code>
     * @param port     the local port on which the service runs
     * @param weight   weight of the service
     * @param priority priority of the service
     * @param text     string describing the service
     */
    public ServiceInfo(String type, String name,
                       int port, int weight,
                       int priority, String text)
    {
        this(type, name, port, weight, priority, (byte[]) null);
        try
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream(text.length());
            writeUTF(out, text);
            this.text = out.toByteArray();
        }
        catch (IOException e)
        {
            throw new RuntimeException("unexpected exception: " + e);
        }
    }

    /**
     * Construct a service description for registrating with JmDNS. The properties hashtable must
     * map property names to either Strings or byte arrays describing the property values.
     *
     * @param type     fully qualified service type name, such as <code>_http._tcp.local.</code>.
     * @param name     unqualified service instance name, such as <code>foobar</code>
     * @param port     the local port on which the service runs
     * @param weight   weight of the service
     * @param priority priority of the service
     * @param props    properties describing the service
     */
    public ServiceInfo(String type, String name,
                       int port, int weight,
                       int priority, Map<String, Object> props)
    {
        this(type, name, port, weight, priority, new byte[0]);
        if (props != null)
        {
            try
            {
                ByteArrayOutputStream out = new ByteArrayOutputStream(256);
                for (Map.Entry<String, Object> prop : props.entrySet())
                {
                    String key = prop.getKey();
                    Object val = prop.getValue();
                    ByteArrayOutputStream out2 = new ByteArrayOutputStream(100);
                    writeUTF(out2, key);
                    if (val instanceof String)
                    {
                        out2.write('=');
                        writeUTF(out2, (String) val);
                    }
                    else
                    {
                        if (val instanceof byte[])
                        {
                            out2.write('=');
                            byte[] bval = (byte[]) val;
                            out2.write(bval, 0, bval.length);
                        }
                        else
                        {
                            if (val != NO_VALUE)
                            {
                                throw new IllegalArgumentException(
                                    "invalid property value: " + val);
                            }
                        }
                    }
                    byte data[] = out2.toByteArray();
                    out.write(data.length);
                    out.write(data, 0, data.length);
                }
                this.text = out.toByteArray();
            }
            catch (IOException e)
            {
                throw new RuntimeException("unexpected exception: " + e);
            }
        }
    }

    /**
     * Construct a service description for registrating with JmDNS.
     *
     * @param type     fully qualified service type name,
     *      such as <code>_http._tcp.local.</code>.
     * @param name     unqualified service instance name,
     *      such as <code>foobar</code>
     * @param port     the local port on which the service runs
     * @param weight   weight of the service
     * @param priority priority of the service
     * @param text     bytes describing the service
     */
    public ServiceInfo(String type, String name,
                       int port, int weight,
                       int priority, byte text[])
    {
        this.type = type;
        this.name = name;
        this.port = port;
        this.weight = weight;
        this.priority = priority;
        this.text = text;

        String SLevel = System.getProperty("jmdns.debug");
        if (SLevel == null) SLevel = "INFO";
        logger.setLevel(Level.parse(SLevel));
    }

    /**
     * Construct a service record during service discovery.
     */
    ServiceInfo(String type, String name)
    {
        if (!type.endsWith("."))
        {
            throw new IllegalArgumentException(
                "type must be fully qualified DNS name ending in '.': " + type);
        }

        this.type = type;
        this.name = name;
    }

    /**
     * During recovery we need to duplicate service info to reregister them
     */
    ServiceInfo(ServiceInfo info)
    {
        if (info != null)
        {
            this.type = info.type;
            this.name = info.name;
            this.port = info.port;
            this.weight = info.weight;
            this.priority = info.priority;
            this.text = info.text;
        }
    }

    /**
     * Fully qualified service type name,
     * such as <code>_http._tcp.local.</code> .
     * @return Returns fully qualified service type name.
     */
    public String getType()
    {
        return type;
    }

    /**
     * Unqualified service instance name,
     * such as <code>foobar</code> .
     * @return Returns unqualified service instance name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the service instance name.
     *
     * @param name unqualified service instance name,
     * such as <code>foobar</code>
     */
    void setName(String name)
    {
        this.name = name;
    }

    /**
     * Fully qualified service name,
     * such as <code>foobar._http._tcp.local.</code> .
     * @return Returns fully qualified service name.
     */
    public String getQualifiedName()
    {
        return name + "." + type;
    }

    /**
     * Get the name of the server.
     * @return Returns name of the server.
     */
    public String getServer()
    {
        return server;
    }

    /**
     * Get the host address of the service (ie X.X.X.X).
     * @return Returns host address of the service.
     */
    public String getHostAddress()
    {
        return (addr != null ? addr.getHostAddress() : "");
    }

    public InetAddress getAddress()
    {
        return addr;
    }

    /**
     * Get the InetAddress of the service.
     * @return Returns the InetAddress of the service.
     */
    public InetAddress getInetAddress()
    {
        return addr;
    }

    /**
     * Get the port for the service.
     * @return Returns port for the service.
     */
    public int getPort()
    {
        return port;
    }

    /**
     * Get the priority of the service.
     * @return Returns the priority of the service.
     */
    public int getPriority()
    {
        return priority;
    }

    /**
     * Get the weight of the service.
     * @return Returns the weight of the service.
     */
    public int getWeight()
    {
        return weight;
    }

    /**
     * Get the text for the serivce as raw bytes.
     * @return Returns the text for the serivce as raw bytes.
     */
    public byte[] getTextBytes()
    {
        return text;
    }

    /**
     * Get the text for the service. This will interpret the text bytes
     * as a UTF8 encoded string. Will return null if the bytes are not
     * a valid UTF8 encoded string.
     * @return Returns the text for the service.
     */
    public String getTextString()
    {
        if ((text == null) ||
            (text.length == 0) ||
            ((text.length == 1) && (text[0] == 0)))
        {
            return null;
        }
        return readUTF(text, 0, text.length);
    }

    /**
     * Get the URL for this service. An http URL is created by
     * combining the address, port, and path properties.
     * @return Returns the URL for this service.
     */
    public String getURL()
    {
        return getURL("http");
    }

    /**
     * Get the URL for this service. An URL is created by
     * combining the protocol, address, port, and path properties.
     * @param protocol
     * @return Returns URL for this service.
     */
    public String getURL(String protocol)
    {
        String url = protocol + "://" + getHostAddress() + ":" + getPort();
        String path = getPropertyString("path");
        if (path != null)
        {
            if (path.indexOf("://") >= 0)
            {
                url = path;
            }
            else
            {
                url += path.startsWith("/") ? path : "/" + path;
            }
        }
        return url;
    }

    /**
     * Get a property of the service. This involves decoding the
     * text bytes into a property list. Returns null if the property
     * is not found or the text data could not be decoded correctly.
     * @param name
     * @return Returns property of the service as bytes.
     */
    public synchronized byte[] getPropertyBytes(String name)
    {
        return (byte[]) getProperties().get(name);
    }

    /**
     * Get a property of the service. This involves decoding the
     * text bytes into a property list. Returns null if the property
     * is not found, the text data could not be decoded correctly, or
     * the resulting bytes are not a valid UTF8 string.
     * @param name
     * @return Returns property of the service as string.
     */
    public synchronized String getPropertyString(String name)
    {
        byte data[] = (byte[]) getProperties().get(name);

        if (data == null)
        {
            return null;
        }
        if (data == NO_VALUE)
        {
            return "true";
        }
        String res = readUTF(data, 0, data.length);

        return res;
    }

    /**
     * Iterator<String> of the property names.
     * @return Iterator<String> of the property names.
     */
    public Iterator<String> getPropertyNames()
    {
        Map<String, Object> properties = getProperties();
        Iterable<String> propertyNames
            = (properties != null) ? properties.keySet() : new Vector<String>();
        return propertyNames.iterator();
    }

    /**
     * Write a UTF string with a length to a stream.
     */
    void writeUTF(OutputStream out, String str) throws IOException
    {
        for (int i = 0, len = str.length(); i < len; i++)
        {
            int c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F))
            {
                out.write(c);
            }
            else
            {
                if (c > 0x07FF)
                {
                    out.write(0xE0 | ((c >> 12) & 0x0F));
                    out.write(0x80 | ((c >> 6) & 0x3F));
                    out.write(0x80 | ((c >> 0) & 0x3F));
                }
                else
                {
                    out.write(0xC0 | ((c >> 6) & 0x1F));
                    out.write(0x80 | ((c >> 0) & 0x3F));
                }
            }
        }
    }

    /**
     * Read data bytes as a UTF stream.
     */
    String readUTF(byte data[], int off, int len)
    {
        StringBuffer buf = new StringBuffer();
        for (int end = off + len; off < end;)
        {
            int ch = data[off++] & 0xFF;
            switch (ch >> 4)
            {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    // 0xxxxxxx
                    break;
                case 12:
                case 13:
                    if (off >= len)
                    {
                        return null;
                    }
                    // 110x xxxx   10xx xxxx
                    ch = ((ch & 0x1F) << 6) | (data[off++] & 0x3F);
                    break;
                case 14:
                    if (off + 2 >= len)
                    {
                        return null;
                    }
                    // 1110 xxxx  10xx xxxx  10xx xxxx
                    ch = ((ch & 0x0f) << 12) |
                        ((data[off++] & 0x3F) << 6) |
                        (data[off++] & 0x3F);
                    break;
                default:
                    if (off + 1 >= len)
                    {
                        return null;
                    }
                    // 10xx xxxx,  1111 xxxx
                    ch = ((ch & 0x3F) << 4) | (data[off++] & 0x0f);
                    break;
            }
            buf.append((char) ch);
        }
        return buf.toString();
    }

    synchronized Map<String, Object> getProperties()
    {
        if ((props == null) && (text != null))
        {
            Map<String, Object> props = new Hashtable<String, Object>();
            int off = 0;
            while (off < text.length)
            {
                // length of the next key value pair
                int len = text[off++] & 0xFF;
                if ((len == 0) || (off + len > text.length))
                {
                    props.clear();
                    break;
                }
                // look for the '='
                int i = 0;
                for (; (i < len) && (text[off + i] != '='); i++)
                {
                    ;
                }

                // get the property name
                String name = readUTF(text, off, i);
                if (name == null)
                {
                    props.clear();
                    break;
                }
                if (i == len)
                {
                    props.put(name, NO_VALUE);
                }
                else
                {
                    byte value[] = new byte[len - ++i];
                    System.arraycopy(text, off + i, value, 0, len - i);
                    props.put(name, value);
                    off += len;
                }
            }
            this.props = props;
        }
        return props;
    }


    /**
     * JmDNS callback to update a DNS record.
     * @param rec
     */
    public void updateRecord(JmDNS jmdns, long now, DNSRecord rec)
    {
        if ((rec != null) && !rec.isExpired(now))
        {
            switch (rec.type)
            {
                case DNSConstants.TYPE_A:       // IPv4
                case DNSConstants.TYPE_AAAA:    // IPv6 FIXME [PJYF Oct 14 2004] This has not been tested
                    if (rec.name.equals(server))
                    {
                        addr = ((DNSRecord.Address) rec).getAddress();

                    }
                    break;
                case DNSConstants.TYPE_SRV:
                    if (rec.name.equals(getQualifiedName()))
                    {
                        DNSRecord.Service srv = (DNSRecord.Service) rec;
                        server = srv.server;
                        port = srv.port;
                        weight = srv.weight;
                        priority = srv.priority;
                        addr = null;
                        // changed to use getCache() instead - jeffs
                        // updateRecord(jmdns, now, (DNSRecord)jmdns.cache.get(server, TYPE_A, CLASS_IN));
                        updateRecord(jmdns,
                                     now,
                                     (DNSRecord) jmdns.getCache().get(
                                        server,
                                        DNSConstants.TYPE_A,
                                        DNSConstants.CLASS_IN));
                    }
                    break;
                case DNSConstants.TYPE_TXT:
                    if (rec.name.equals(getQualifiedName()))
                    {
                        DNSRecord.Text txt = (DNSRecord.Text) rec;
                        text = txt.text;
                    }
                    break;
            }
            // Future Design Pattern
            // This is done, to notify the wait loop in method
            // JmDNS.getServiceInfo(type, name, timeout);
            if (hasData() && dns != null)
            {
                dns.handleServiceResolved(this);
                dns = null;
            }
            synchronized (this)
            {
                notifyAll();
            }
        }
    }

    /**
     * Returns true if the service info is filled with data.
     */
    boolean hasData()
    {
        return server != null && addr != null && text != null;
    }


    // State machine
    /**
     * Sets the state and notifies all objects that wait on the ServiceInfo.
     */
    synchronized void advanceState()
    {
        state = state.advance();
        notifyAll();
    }

    /**
     * Sets the state and notifies all objects that wait on the ServiceInfo.
     */
    synchronized void revertState()
    {
        state = state.revert();
        notifyAll();
    }

    /**
     * Sets the state and notifies all objects that wait on the ServiceInfo.
     */
    synchronized void cancel()
    {
        state = DNSState.CANCELED;
        notifyAll();
    }

    /**
     * Returns the current state of this info.
     */
    DNSState getState()
    {
        return state;
    }


    @Override
    public int hashCode()
    {
        return getQualifiedName().hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return (obj instanceof ServiceInfo) &&
            getQualifiedName().equals(((ServiceInfo) obj).getQualifiedName());
    }

    public String getNiceTextString()
    {
        StringBuffer buf = new StringBuffer();
        for (int i = 0, len = text.length; i < len; i++)
        {
            if (i >= 20)
            {
                buf.append("...");
                break;
            }
            int ch = text[i] & 0xFF;
            if ((ch < ' ') || (ch > 127))
            {
                buf.append("\\0");
                buf.append(Integer.toString(ch, 8));
            }
            else
            {
                buf.append((char) ch);
            }
        }
        return buf.toString();
    }

    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("service[");
        buf.append(getQualifiedName());
        buf.append(',');
        buf.append(getAddress());
        buf.append(':');
        buf.append(port);
        buf.append(',');
        buf.append(getNiceTextString());
        buf.append(']');
        return buf.toString();
    }

    /**
     * SC-Bonjour Implementation: Method used to set the properties of an existing ServiceInfo.
     * This is used in the implementation of Bonjour in SIP Communicator to be able to replace
     * old properties of the service we've declared to announce the local user with new properties
     * (for example in case of a status change).
     * @param props Hashtable containing all the new properties to set
     */
    public void setProps(Map<String, Object> props)
    {
        if (props != null)
        {
            try
            {
                ByteArrayOutputStream out = new ByteArrayOutputStream(256);
                for (Map.Entry<String, Object> prop : props.entrySet())
                {
                    String key = prop.getKey();
                    Object val = prop.getValue();

                    ByteArrayOutputStream out2 = new ByteArrayOutputStream(100);
                    writeUTF(out2, key);
                    if (val instanceof String)
                    {
                        out2.write('=');
                        writeUTF(out2, (String) val);
                    }
                    else
                    {
                        if (val instanceof byte[])
                        {
                            out2.write('=');
                            byte[] bval = (byte[]) val;
                            out2.write(bval, 0, bval.length);
                        }
                        else
                        {
                            if (val != NO_VALUE)
                            {
                                throw new IllegalArgumentException(
                                    "invalid property value: " + val);
                            }
                        }
                    }
                    byte data[] = out2.toByteArray();
                    out.write(data.length);
                    out.write(data, 0, data.length);
                }
                this.text = out.toByteArray();
            }
            catch (IOException e)
            {
                throw new RuntimeException("unexpected exception: " + e);
            }
        }
    }
}
