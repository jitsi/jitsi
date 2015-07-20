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
import java.util.logging.*;

/**
 * DNS record
 *
 * @version %I%, %G%
 * @author  Arthur van Hoff, Rick Blair, Werner Randelshofer, Pierre Frisch
 */
public abstract class DNSRecord extends DNSEntry
{
    private static Logger logger =
        Logger.getLogger(DNSRecord.class.toString());
    int ttl;
    private long created;

    /**
     * Create a DNSRecord with a name, type, clazz, and ttl.
     */
    DNSRecord(String name, int type, int clazz, int ttl)
    {
        super(name, type, clazz);
        this.ttl = ttl;
        this.created = System.currentTimeMillis();

        String SLevel = System.getProperty("jmdns.debug");
        if (SLevel == null) SLevel = "INFO";
        logger.setLevel(Level.parse(SLevel));
    }

    /**
     * True if this record is the same as some other record.
     * @param other obj to be compared to.
     */
    @Override
    public boolean equals(Object other)
    {
        return (other instanceof DNSRecord) && sameAs((DNSRecord) other);
    }

    /**
     * True if this record is the same as some other record.
     */
    boolean sameAs(DNSRecord other)
    {
        return super.equals(other) && sameValue(other);
    }

    /**
     * True if this record has the same value as some other record.
     */
    abstract boolean sameValue(DNSRecord other);

    /**
     * True if this record has the same type as some other record.
     */
    boolean sameType(DNSRecord other)
    {
        return type == other.type;
    }

    /**
     * Handles a query represented by this record.
     *
     * @return Returns true if a conflict with one of the services registered
     *         with JmDNS or with the hostname occured.
     */
    abstract boolean handleQuery(JmDNS dns, long expirationTime);

    /**
     * Handles a responserepresented by this record.
     *
     * @return Returns true if a conflict with one of the services registered
     *         with JmDNS or with the hostname occured.
     */
    abstract boolean handleResponse(JmDNS dns);

    /**
     * Adds this as an answer to the provided outgoing datagram.
     */
    abstract DNSOutgoing addAnswer(JmDNS dns, DNSIncoming in, InetAddress addr,
                                   int port, DNSOutgoing out)
        throws IOException;

    /**
     * True if this record is suppressed by the answers in a message.
     */
    boolean suppressedBy(DNSIncoming msg)
    {
        try
        {
            for (int i = msg.numAnswers; i-- > 0;)
            {
                if (suppressedBy(msg.answers.get(i)))
                {
                    return true;
                }
            }
            return false;
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            logger.log(Level.WARNING,
                "suppressedBy() message " + msg + " exception ", e);
            // msg.print(true);
            return false;
        }
    }

    /**
     * True if this record would be supressed by an answer.
     * This is the case if this record would not have a
     * significantly longer TTL.
     */
    boolean suppressedBy(DNSRecord other)
    {
        if (sameAs(other) && (other.ttl > ttl / 2))
        {
            return true;
        }
        return false;
    }

    /**
     * Get the expiration time of this record.
     */
    long getExpirationTime(int percent)
    {
        return created + (percent * ttl * 10L);
    }

    /**
     * Get the remaining TTL for this record.
     */
    int getRemainingTTL(long now)
    {
        return (int) Math.max(0, (getExpirationTime(100) - now) / 1000);
    }

    /**
     * Check if the record is expired.
     */
    boolean isExpired(long now)
    {
        return getExpirationTime(100) <= now;
    }

    /**
     * Check if the record is stale, ie it has outlived
     * more than half of its TTL.
     */
    boolean isStale(long now)
    {
        return getExpirationTime(50) <= now;
    }

    /**
     * Reset the TTL of a record. This avoids having to
     * update the entire record in the cache.
     */
    void resetTTL(DNSRecord other)
    {
        created = other.created;
        ttl = other.ttl;
    }

    /**
     * Write this record into an outgoing message.
     */
    abstract void write(DNSOutgoing out) throws IOException;

    /**
     * Address record.
     */
    static class Address extends DNSRecord
    {
        private static Logger logger =
            Logger.getLogger(Address.class.toString());
        InetAddress addr;

        Address(String name, int type, int clazz, int ttl, InetAddress addr)
        {
            super(name, type, clazz, ttl);
            this.addr = addr;

            String SLevel = System.getProperty("jmdns.debug");
            if (SLevel == null) SLevel = "INFO";
            logger.setLevel(Level.parse(SLevel));
        }

        Address(String name, int type, int clazz, int ttl, byte[] rawAddress)
        {
            super(name, type, clazz, ttl);
            try
            {
                this.addr = InetAddress.getByAddress(rawAddress);
            }
            catch (UnknownHostException exception)
            {
                logger.log(Level.WARNING, "Address() exception ", exception);
            }
        }

        @Override
        void write(DNSOutgoing out) throws IOException
        {
            if (addr != null)
            {
                byte[] buffer = addr.getAddress();
                if (DNSConstants.TYPE_A == type)
                {
                    // If we have a type A records we should
                    // answer with a IPv4 address
                    if (addr instanceof Inet4Address)
                    {
                        // All is good
                    }
                    else
                    {
                        // Get the last four bytes
                        byte[] tempbuffer = buffer;
                        buffer = new byte[4];
                        System.arraycopy(tempbuffer, 12, buffer, 0, 4);
                    }
                }
                else
                {
                    // If we have a type AAAA records we should
                    // answer with a IPv6 address
                    if (addr instanceof Inet4Address)
                    {
                        byte[] tempbuffer = buffer;
                        buffer = new byte[16];
                        for (int i = 0; i < 16; i++)
                        {
                            if (i < 11)
                            {
                                buffer[i] = tempbuffer[i - 12];
                            }
                            else
                            {
                                buffer[i] = 0;
                            }
                        }
                    }
                }
                int length = buffer.length;
                out.writeBytes(buffer, 0, length);
            }
        }

        boolean same(DNSRecord other)
        {
            return ((sameName(other)) && ((sameValue(other))));
        }

        boolean sameName(DNSRecord other)
        {
            return name.equalsIgnoreCase(((Address) other).name);
        }

        @Override
        boolean sameValue(DNSRecord other)
        {
            return addr.equals(((Address) other).getAddress());
        }

        InetAddress getAddress()
        {
            return addr;
        }

        /**
         * Creates a byte array representation of this record.
         * This is needed for tie-break tests according to
         * draft-cheshire-dnsext-multicastdns-04.txt chapter 9.2.
         */
        private byte[] toByteArray()
        {
            try
            {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                DataOutputStream dout = new DataOutputStream(bout);
                dout.write(name.getBytes("UTF8"));
                dout.writeShort(type);
                dout.writeShort(clazz);
                //dout.writeInt(len);
                byte[] buffer = addr.getAddress();
                for (int i = 0; i < buffer.length; i++)
                {
                    dout.writeByte(buffer[i]);
                }
                dout.close();
                return bout.toByteArray();
            }
            catch (IOException e)
            {
                throw new InternalError();
            }
        }

        /**
         * Does a lexicographic comparison of the byte array representation
         * of this record and that record.
         * This is needed for tie-break tests according to
         * draft-cheshire-dnsext-multicastdns-04.txt chapter 9.2.
         */
        private int lexCompare(DNSRecord.Address that)
        {
            byte[] thisBytes = this.toByteArray();
            byte[] thatBytes = that.toByteArray();
            for (   int i = 0, n = Math.min(thisBytes.length, thatBytes.length);
                    i < n;
                    i++)
            {
                if (thisBytes[i] > thatBytes[i])
                {
                    return 1;
                }
                else
                {
                    if (thisBytes[i] < thatBytes[i])
                    {
                        return -1;
                    }
                }
            }
            return thisBytes.length - thatBytes.length;
        }

        /**
         * Does the necessary actions, when this as a query.
         */
        @Override
        boolean handleQuery(JmDNS dns, long expirationTime)
        {
            DNSRecord.Address dnsAddress =
                dns.getLocalHost().getDNSAddressRecord(this);
            if (dnsAddress != null)
            {
                if (dnsAddress.sameType(this) &&
                    dnsAddress.sameName(this) &&
                    (!dnsAddress.sameValue(this)))
                {
                    logger.finer(
                        "handleQuery() Conflicting probe detected. dns state " +
                        dns.getState() +
                        " lex compare " + lexCompare(dnsAddress));
                    // Tie-breaker test
                    if (dns.getState().isProbing() && lexCompare(dnsAddress) >= 0)
                    {
                        // We lost the tie-break. We have to choose a different name.
                        dns.getLocalHost().incrementHostName();
                        dns.getCache().clear();
                        for (ServiceInfo info : dns.services.values())
                            info.revertState();
                    }
                    dns.revertState();
                    return true;
                }
            }
            return false;
        }

        /**
         * Does the necessary actions, when this as a response.
         */
        @Override
        boolean handleResponse(JmDNS dns)
        {
            DNSRecord.Address dnsAddress =
                dns.getLocalHost().getDNSAddressRecord(this);
            if (dnsAddress != null)
            {
                if (dnsAddress.sameType(this) &&
                    dnsAddress.sameName(this) &&
                    (!dnsAddress.sameValue(this)))
                {
                    logger.finer("handleResponse() Denial detected");

                    if (dns.getState().isProbing())
                    {
                        dns.getLocalHost().incrementHostName();
                        dns.getCache().clear();
                        for (ServiceInfo info : dns.services.values())
                            info.revertState();
                    }
                    dns.revertState();
                    return true;
                }
            }
            return false;
        }

        @Override
        DNSOutgoing addAnswer(JmDNS dns,
                              DNSIncoming in,
                              InetAddress addr,
                              int port,
                              DNSOutgoing out)
            throws IOException
        {
            return out;
        }

        @Override
        public String toString()
        {
            return toString(" address '" +
                (addr != null ? addr.getHostAddress() : "null") + "'");
        }

    }

    /**
     * Pointer record.
     */
    static class Pointer extends DNSRecord
    {
        private static Logger logger =
            Logger.getLogger(Pointer.class.toString());
        String alias;

        Pointer(String name, int type, int clazz, int ttl, String alias)
        {
            super(name, type, clazz, ttl);
            this.alias = alias;

            String SLevel = System.getProperty("jmdns.debug");
            if (SLevel == null) SLevel = "INFO";
            logger.setLevel(Level.parse(SLevel));
        }

        @Override
        void write(DNSOutgoing out) throws IOException
        {
            out.writeName(alias);
        }

        @Override
        boolean sameValue(DNSRecord other)
        {
            return alias.equals(((Pointer) other).alias);
        }

        @Override
        boolean handleQuery(JmDNS dns, long expirationTime)
        {
            // Nothing to do (?)
            // I think there is no possibility
            // for conflicts for this record type?
            return false;
        }

        @Override
        boolean handleResponse(JmDNS dns)
        {
            // Nothing to do (?)
            // I think there is no possibility for conflicts for this record type?
            return false;
        }

        String getAlias()
        {
            return alias;
        }

        @Override
        DNSOutgoing addAnswer(JmDNS dns,
                            DNSIncoming in,
                            InetAddress addr,
                            int port,
                            DNSOutgoing out)
            throws IOException
        {
            return out;
        }

        @Override
        public String toString()
        {
            return toString(alias);
        }
    }

    static class Text extends DNSRecord
    {
        private static Logger logger =
            Logger.getLogger(Text.class.toString());
        byte text[];

        Text(String name, int type, int clazz, int ttl, byte text[])
        {
            super(name, type, clazz, ttl);
            this.text = text;

            String SLevel = System.getProperty("jmdns.debug");
            if (SLevel == null) SLevel = "INFO";
            logger.setLevel(Level.parse(SLevel));
        }

        @Override
        void write(DNSOutgoing out) throws IOException
        {
            out.writeBytes(text, 0, text.length);
        }

        @Override
        boolean sameValue(DNSRecord other)
        {
            Text txt = (Text) other;
            if (txt.text.length != text.length)
            {
                return false;
            }
            for (int i = text.length; i-- > 0;)
            {
                if (txt.text[i] != text[i])
                {
                    return false;
                }
            }
            return true;
        }

        @Override
        boolean handleQuery(JmDNS dns, long expirationTime)
        {
            // Nothing to do (?)
            // I think there is no possibility for conflicts for this record type?
            return false;
        }

        @Override
        boolean handleResponse(JmDNS dns)
        {
            // Nothing to do (?)
            // Shouldn't we care if we get a conflict at this level?
            /*
                ServiceInfo info = (ServiceInfo) dns.services.get(name.toLowerCase());
                if (info != null)
                {
                    if (! Arrays.equals(text,info.text))
                    {
                        info.revertState();
                        return true;
                    }
                }
             */
            return false;
        }

        @Override
        DNSOutgoing addAnswer(JmDNS dns,
                              DNSIncoming in,
                              InetAddress addr,
                              int port,
                              DNSOutgoing out)
            throws IOException
        {
            return out;
        }

        @Override
        public String toString()
        {
            return toString((text.length > 10) ?
                    new String(text, 0, 7) + "..." :
                    new String(text));
        }
    }

    /**
     * Service record.
     */
    static class Service extends DNSRecord
    {
        private static Logger logger =
            Logger.getLogger(Service.class.toString());
        int priority;
        int weight;
        int port;
        String server;

        Service(String name,
                int type,
                int clazz,
                int ttl,
                int priority,
                int weight,
                int port,
                String server)
        {
            super(name, type, clazz, ttl);
            this.priority = priority;
            this.weight = weight;
            this.port = port;
            this.server = server;

            String SLevel = System.getProperty("jmdns.debug");
            if (SLevel == null) SLevel = "INFO";
            logger.setLevel(Level.parse(SLevel));
        }

        @Override
        void write(DNSOutgoing out) throws IOException
        {
            out.writeShort(priority);
            out.writeShort(weight);
            out.writeShort(port);
            out.writeName(server);
        }

        private byte[] toByteArray()
        {
            try
            {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                DataOutputStream dout = new DataOutputStream(bout);
                dout.write(name.getBytes("UTF8"));
                dout.writeShort(type);
                dout.writeShort(clazz);
                //dout.writeInt(len);
                dout.writeShort(priority);
                dout.writeShort(weight);
                dout.writeShort(port);
                dout.write(server.getBytes("UTF8"));
                dout.close();
                return bout.toByteArray();
            }
            catch (IOException e)
            {
                throw new InternalError();
            }
        }

        private int lexCompare(DNSRecord.Service that)
        {
            byte[] thisBytes = this.toByteArray();
            byte[] thatBytes = that.toByteArray();
            for (int i = 0, n = Math.min(thisBytes.length, thatBytes.length);
                i < n;
                i++)
            {
                if (thisBytes[i] > thatBytes[i])
                {
                    return 1;
                }
                else
                {
                    if (thisBytes[i] < thatBytes[i])
                    {
                        return -1;
                    }
                }
            }
            return thisBytes.length - thatBytes.length;
        }

        @Override
        boolean sameValue(DNSRecord other)
        {
            Service s = (Service) other;
            return (priority == s.priority) &&
                (weight == s.weight) &&
                (port == s.port) &&
                server.equals(s.server);
        }

        @Override
        boolean handleQuery(JmDNS dns, long expirationTime)
        {
            ServiceInfo info = dns.services.get(name.toLowerCase());
            if (info != null &&
                (port != info.port ||
                !server.equalsIgnoreCase(dns.getLocalHost().getName())))
            {
                logger.finer("handleQuery() Conflicting probe detected");

                // Tie breaker test
                if (info.getState().isProbing() &&
                    lexCompare(new DNSRecord.Service(
                        info.getQualifiedName(),
                        DNSConstants.TYPE_SRV,
                        DNSConstants.CLASS_IN | DNSConstants.CLASS_UNIQUE,
                        DNSConstants.DNS_TTL,
                        info.priority,
                        info.weight,
                        info.port,
                        dns.getLocalHost().getName())) >= 0)
                {
                    // We lost the tie break
                    String oldName = info.getQualifiedName().toLowerCase();
                    info.setName(dns.incrementName(info.getName()));
                    dns.services.remove(oldName);
                    dns.services.put(info.getQualifiedName().toLowerCase(), info);
                    logger.finer(
                        "handleQuery() Lost tie break: new unique name chosen:" + info.getName());

                }
                info.revertState();
                return true;

            }
            return false;
        }

        @Override
        boolean handleResponse(JmDNS dns)
        {
            ServiceInfo info = dns.services.get(name.toLowerCase());
            if (info != null &&
                (port != info.port || !server.equalsIgnoreCase(dns.getLocalHost().getName())))
            {
                logger.finer("handleResponse() Denial detected");

                if (info.getState().isProbing())
                {
                    String oldName = info.getQualifiedName().toLowerCase();
                    info.setName(dns.incrementName(info.getName()));
                    dns.services.remove(oldName);
                    dns.services.put(info.getQualifiedName().toLowerCase(), info);
                    logger.finer(
                        "handleResponse() New unique name chose:" + info.getName());

                }
                info.revertState();
                return true;
            }
            return false;
        }

        @Override
        DNSOutgoing addAnswer(JmDNS dns,
                              DNSIncoming in,
                              InetAddress addr,
                              int port,
                              DNSOutgoing out)
            throws IOException
        {
            ServiceInfo info = dns.services.get(name.toLowerCase());
            if (info != null)
            {
                if (this.port == info.port != server.equals(dns.getLocalHost().getName()))
                {
                    return dns.addAnswer(in, addr, port, out,
                        new DNSRecord.Service(
                                info.getQualifiedName(),
                                DNSConstants.TYPE_SRV,
                                DNSConstants.CLASS_IN | DNSConstants.CLASS_UNIQUE,
                                DNSConstants.DNS_TTL,
                                info.priority,
                                info.weight,
                                info.port,
                                dns.getLocalHost().getName()));
                }
            }
            return out;
        }

        @Override
        public String toString()
        {
            return toString(server + ":" + port);
        }
    }

    public String toString(String other)
    {
        return toString("record", ttl + "/" +
            getRemainingTTL(System.currentTimeMillis())
//            + "," + other
            );
    }
}

