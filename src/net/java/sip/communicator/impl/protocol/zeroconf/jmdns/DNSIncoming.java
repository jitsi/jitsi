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
 * Parse an incoming DNS message into its components.
 *
 * @version %I%, %G%
 * @author  Arthur van Hoff, Werner Randelshofer, Pierre Frisch, Daniel Bobbert
 */
final class DNSIncoming
{
    private static Logger logger = Logger.getLogger(DNSIncoming.class.toString());
    // Implementation note: This vector should be immutable.
    // If a client of DNSIncoming changes the contents of this vector,
    // we get undesired results. To fix this, we have to migrate to
    // the Collections API of Java 1.2. i.e we replace Vector by List.
    // final static Vector EMPTY = new Vector();

    private DatagramPacket packet;
    private int off;
    private int len;
    private byte data[];

    int id;
    private int flags;
    private int numQuestions;
    int numAnswers;
    private int numAuthorities;
    private int numAdditionals;
    private long receivedTime;

    List<DNSEntry> questions;
    List<DNSRecord> answers;

    /**
     * Parse a message from a datagram packet.
     */
    DNSIncoming(DatagramPacket packet) throws IOException
    {
        String SLevel = System.getProperty("jmdns.debug");
        if (SLevel == null) SLevel = "INFO";
        logger.setLevel(Level.parse(SLevel));

        this.packet = packet;
        this.data = packet.getData();
        this.len = packet.getLength();
        this.off = packet.getOffset();
        this.questions = new LinkedList<DNSEntry>();
        this.answers = new LinkedList<DNSRecord>();
        this.receivedTime = System.currentTimeMillis();

        try
        {
            id = readUnsignedShort();
            flags = readUnsignedShort();
            numQuestions = readUnsignedShort();
            numAnswers = readUnsignedShort();
            numAuthorities = readUnsignedShort();
            numAdditionals = readUnsignedShort();

            // parse questions
            if (numQuestions > 0)
            {
                questions =
                    Collections.synchronizedList(
                                    new ArrayList<DNSEntry>(numQuestions));
                for (int i = 0; i < numQuestions; i++)
                {
                    DNSQuestion question =
                        new DNSQuestion(
                            readName(),
                            readUnsignedShort(),
                            readUnsignedShort());

                    questions.add(question);
                }
            }

            // parse answers
            int n = numAnswers + numAuthorities + numAdditionals;
            if (n > 0)
            {
                //System.out.println("JMDNS received "+n+" answers!");
                answers = Collections.synchronizedList(
                                new ArrayList<DNSRecord>(n));
                for (int i = 0; i < n; i++)
                {
                    String domain = readName();
                    int type = readUnsignedShort();
                    int clazz = readUnsignedShort();
                    int ttl = readInt();
                    int len = readUnsignedShort();
                    int end = off + len;
                    DNSRecord rec = null;

                    switch (type)
                    {
                        case DNSConstants.TYPE_A:       // IPv4
                        case DNSConstants.TYPE_AAAA:    // IPv6 FIXME [PJYF Oct 14 2004] This has not been tested
                            rec = new DNSRecord.Address(
                                domain, type, clazz, ttl, readBytes(off, len));
                            break;
                        case DNSConstants.TYPE_CNAME:
                        case DNSConstants.TYPE_PTR:
                            rec = new DNSRecord.Pointer(
                                domain, type, clazz, ttl, readName());
                            break;
                        case DNSConstants.TYPE_TXT:
                            rec = new DNSRecord.Text(
                                domain, type, clazz, ttl, readBytes(off, len));
                            break;
                        case DNSConstants.TYPE_SRV:
                            //System.out.println("JMDNS: One is a SRV field!!");
                            rec = new DNSRecord.Service(    domain,
                                                            type,
                                                            clazz,
                                                            ttl,
                                                            readUnsignedShort(),
                                                            readUnsignedShort(),
                                                            readUnsignedShort(),
                                                            readName());
                            break;
                        case DNSConstants.TYPE_HINFO:
                            // Maybe we should do something with those
                            break;
                        default :
                            logger.finer("DNSIncoming() unknown type:" + type);
                            break;
                    }

                    if (rec != null)
                    {
                        // Add a record, if we were able to create one.
                        answers.add(rec);
                    }
                    else
                    {
                        // Addjust the numbers for the skipped record
                        if (answers.size() < numAnswers)
                        {
                            numAnswers--;
                        }
                        else
                        {
                            if (answers.size() < numAnswers + numAuthorities)
                            {
                                numAuthorities--;
                            }
                            else
                            {
                                if (answers.size() < numAnswers +
                                                     numAuthorities +
                                                     numAdditionals)
                                {
                                    numAdditionals--;
                                }
                            }
                        }
                    }
                    off = end;
                }
            }
        }
        catch (IOException e)
        {
            logger.log(Level.WARNING,
                "DNSIncoming() dump " + print(true) + "\n exception ", e);
            throw e;
        }
    }

    /**
     * Check if the message is a query.
     */
    boolean isQuery()
    {
        return (flags & DNSConstants.FLAGS_QR_MASK) ==
                DNSConstants.FLAGS_QR_QUERY;
    }

    /**
     * Check if the message is truncated.
     */
    boolean isTruncated()
    {
        return (flags & DNSConstants.FLAGS_TC) != 0;
    }

    /**
     * Check if the message is a response.
     */
    boolean isResponse()
    {
        return (flags & DNSConstants.FLAGS_QR_MASK) ==
                DNSConstants.FLAGS_QR_RESPONSE;
    }

    private int get(int off) throws IOException
    {
        if ((off < 0) || (off >= len))
        {
            throw new IOException("parser error: offset=" + off);
        }
        return data[off] & 0xFF;
    }

    private int readUnsignedShort() throws IOException
    {
        return (get(off++) << 8) + get(off++);
    }

    private int readInt() throws IOException
    {
        return (readUnsignedShort() << 16) + readUnsignedShort();
    }

    private byte[] readBytes(int off, int len) throws IOException
    {
        byte bytes[] = new byte[len];
        System.arraycopy(data, off, bytes, 0, len);
        return bytes;
    }

    private void readUTF(StringBuffer buf, int off, int len) throws IOException
    {
        for (int end = off + len; off < end;)
        {
            int ch = get(off++);
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
                    // 110x xxxx   10xx xxxx
                    ch = ((ch & 0x1F) << 6) | (get(off++) & 0x3F);
                    break;
                case 14:
                    // 1110 xxxx  10xx xxxx  10xx xxxx
                    ch =    ((ch & 0x0f) << 12) |
                            ((get(off++) & 0x3F) << 6) |
                            (get(off++) & 0x3F);
                    break;
                default:
                    // 10xx xxxx,  1111 xxxx
                    ch = ((ch & 0x3F) << 4) | (get(off++) & 0x0f);
                    break;
            }
            buf.append((char) ch);
        }
    }

    private String readName() throws IOException
    {
        StringBuffer buf = new StringBuffer();
        int off = this.off;
        int next = -1;
        int first = off;

        while (true)
        {
            int len = get(off++);
            if (len == 0)
            {
                break;
            }
            switch (len & 0xC0)
            {
                case 0x00:
                    //buf.append("[" + off + "]");
                    readUTF(buf, off, len);
                    off += len;
                    buf.append('.');
                    break;
                case 0xC0:
                    //buf.append("<" + (off - 1) + ">");
                    if (next < 0)
                    {
                        next = off + 1;
                    }
                    off = ((len & 0x3F) << 8) | get(off++);
                    if (off >= first)
                    {
                        throw new IOException(
                            "bad domain name: possible circular name detected");
                    }
                    first = off;
                    break;
                default:
                    throw new IOException(
                        "bad domain name: '" + buf + "' at " + off);
            }
        }
        this.off = (next >= 0) ? next : off;
        return buf.toString();
    }

    /**
     * Debugging.
     */
    String print(boolean dump)
    {
        StringBuffer buf = new StringBuffer();
        buf.append(toString() + "\n");
        for (Iterator<DNSEntry> iterator = questions.iterator();
                iterator.hasNext();)
        {
            buf.append("    ques:" + iterator.next() + "\n");
        }
        int count = 0;
        for (Iterator<DNSRecord> iterator = answers.iterator();
                iterator.hasNext();
                count++)
        {
            if (count < numAnswers)
            {
                buf.append("    answ:");
            }
            else
            {
                if (count < numAnswers + numAuthorities)
                {
                    buf.append("    auth:");
                }
                else
                {
                    buf.append("    addi:");
                }
            }
            buf.append(iterator.next() + "\n");
        }
        if (dump)
        {
            for (int off = 0, len = packet.getLength(); off < len; off += 32)
            {
                int n = Math.min(32, len - off);
                if (off < 10)
                {
                    buf.append(' ');
                }
                if (off < 100)
                {
                    buf.append(' ');
                }
                buf.append(off);
                buf.append(':');
                for (int i = 0; i < n; i++)
                {
                    if ((i % 8) == 0)
                    {
                        buf.append(' ');
                    }
                    buf.append(Integer.toHexString((data[off + i] & 0xF0) >> 4));
                    buf.append(Integer.toHexString((data[off + i] & 0x0F) >> 0));
                }
                buf.append("\n");
                buf.append("    ");
                for (int i = 0; i < n; i++)
                {
                    if ((i % 8) == 0)
                    {
                        buf.append(' ');
                    }
                    buf.append(' ');
                    int ch = data[off + i] & 0xFF;
                    buf.append(((ch > ' ') && (ch < 127)) ? (char) ch : '.');
                }
                buf.append("\n");

                // limit message size
                if (off + 32 >= 256)
                {
                    buf.append("....\n");
                    break;
                }
            }
        }
        return buf.toString();
    }

    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(isQuery() ? "dns[query," : "dns[response,");
        if (packet.getAddress() != null)
        {
            buf.append(packet.getAddress().getHostAddress());
        }
        buf.append(':');
        buf.append(packet.getPort());
        buf.append(",len=");
        buf.append(packet.getLength());
        buf.append(",id=0x");
        buf.append(Integer.toHexString(id));
        if (flags != 0)
        {
            buf.append(",flags=0x");
            buf.append(Integer.toHexString(flags));
            if ((flags & DNSConstants.FLAGS_QR_RESPONSE) != 0)
            {
                buf.append(":r");
            }
            if ((flags & DNSConstants.FLAGS_AA) != 0)
            {
                buf.append(":aa");
            }
            if ((flags & DNSConstants.FLAGS_TC) != 0)
            {
                buf.append(":tc");
            }
        }
        if (numQuestions > 0)
        {
            buf.append(",questions=");
            buf.append(numQuestions);
        }
        if (numAnswers > 0)
        {
            buf.append(",answers=");
            buf.append(numAnswers);
        }
        if (numAuthorities > 0)
        {
            buf.append(",authorities=");
            buf.append(numAuthorities);
        }
        if (numAdditionals > 0)
        {
            buf.append(",additionals=");
            buf.append(numAdditionals);
        }
        buf.append("]");
        return buf.toString();
    }

    /**
     * Appends answers to this Incoming.
     *
     * @throws IllegalArgumentException If not a query or if Truncated.
     */
    void append(DNSIncoming that)
    {
        if (this.isQuery() && this.isTruncated() && that.isQuery())
        {
            if (that.numQuestions > 0) {
                if (Collections.EMPTY_LIST.equals(this.questions))
                    this.questions =
                        Collections.synchronizedList(
                            new ArrayList<DNSEntry>(that.numQuestions));

                this.questions.addAll(that.questions);
                this.numQuestions += that.numQuestions;
            }

            if (Collections.EMPTY_LIST.equals(answers))
            {
                answers = Collections.synchronizedList(
                                    new ArrayList<DNSRecord>());
            }

            if (that.numAnswers > 0)
            {
                this.answers.addAll(this.numAnswers,
                                    that.answers.subList(0, that.numAnswers));
                this.numAnswers += that.numAnswers;
            }
            if (that.numAuthorities > 0)
            {
                this.answers.addAll(this.numAnswers + this.numAuthorities,
                                    that.answers.subList(
                                        that.numAnswers,
                                        that.numAnswers + that.numAuthorities));
                this.numAuthorities += that.numAuthorities;
            }
            if (that.numAdditionals > 0)
            {
                this.answers.addAll(
                    that.answers.subList(
                        that.numAnswers + that.numAuthorities,
                        that.numAnswers + that.numAuthorities + that.numAdditionals));
                this.numAdditionals += that.numAdditionals;
            }
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }

    int elapseSinceArrival()
    {
        return (int) (System.currentTimeMillis() - receivedTime);
    }
}
