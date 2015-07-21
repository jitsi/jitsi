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
package net.java.sip.communicator.impl.netaddr;

import java.io.*;
import java.net.*;
import java.util.*;

import com.sun.jna.*;
import com.sun.jna.ptr.*;

/**
 * Utility class to lookup the local source address of a UDP socket on BSD-like
 * platforms (i.e. @{link sockaddr_in} has the <tt>sin_len</tt> member) using
 * JNA. Other platforms (including Windows) could be used by introducing new
 * {@link sockaddr} subclasses without the len member.
 *
 * @author Ingo Bauersachs
 */
public class BsdLocalhostRetriever
{
    /**
     * JNA interface to LibC.
     */
    public static interface LibC
        extends Library
    {
        static final LibC INSTANCE = (LibC) Native.loadLibrary("c", LibC.class);

        public static final int AF_INET = 2;
        public static final int AF_INET6 = 30;

        public static final int SOCK_DGRAM = 2;

        // see the man pages for the mapped C functions
        int socket(int domain, int type, int protocol);
        int connect(int s, sockaddr name, int namelen);
        int getsockname(int fd, sockaddr addr, IntByReference len);
        int close(int fd);
        String strerror(int error);

        public static class CException extends IOException
        {
            private static final long serialVersionUID = 2988769925077172885L;

            public CException()
            {
                super(LibC.INSTANCE.strerror(Native.getLastError()));
            }
        }
    }

    /**
     * Corresponds to the C structure sockaddr with some utility functions.
     */
    public abstract static class sockaddr
        extends Structure
    {
        /**
         * Creates a C sockaddr_in or sockaddr_in6 based on the type of the
         * passed socket address.
         * 
         * @param socket The socket address to map.
         * @return The mapped socket address.
         */
        public static sockaddr create(InetSocketAddress socket)
        {
            InetAddress address = socket.getAddress();
            if (address instanceof Inet4Address)
            {
                sockaddr_in v4 = new sockaddr_in();
                v4.sin_addr = address.getAddress();
                v4.sin_port = (short) socket.getPort();
                v4.sin_len = (byte) Native.getNativeSize(v4.getClass(), v4);
                return v4;
            }
            else if (address instanceof Inet6Address)
            {
                sockaddr_in6 v6 = new sockaddr_in6();
                v6.sin6_addr = address.getAddress();
                v6.sin6_port = (short) socket.getPort();
                v6.sin6_len = (byte) Native.getNativeSize(v6.getClass(), v6);
                return v6;
            }
            else
            {
                throw new IllegalArgumentException("Unsupported address type");
            }
        }

        /**
         * Gets the socket type.
         * @return {@link LibC#AF_INET} or {@link LibC#AF_INET6}.
         */
        public abstract int getFamily();

        /**
         * Creates a new sockaddr instance of the same type.
         * @return {@link sockaddr_in} or {@link sockaddr_in6}
         */
        public abstract sockaddr createEmpty();

        /**
         * Gets an {@link InetAddress} from the sin[6]_addr member.
         * 
         * @return an {@link InetAddress} with only the address set.
         * @throws UnknownHostException When the data in the structure does not
         *             represent a valid IPv4 or IPv6 address.
         */
        public abstract InetAddress getAddress() throws UnknownHostException;

        /**
         * Gets the native size of this structure.
         * @return the native size of this structure.
         */
        public int getSize()
        {
            return Native.getNativeSize(this.getClass(), this);
        }
    }

    /**
     * JNA mapping of <tt>sockaddr_in</tt>.
     */
    public final static class sockaddr_in
        extends sockaddr
    {
        public byte sin_len;
        public byte sin_family;
        public short sin_port;
        public byte[] sin_addr;
        public byte[] sin_zero;

        public sockaddr_in()
        {
            sin_family = LibC.AF_INET;
            sin_addr = new byte[4];
            sin_zero = new byte[8];
        }

        @Override
        public sockaddr createEmpty()
        {
            sockaddr_in v4 = new sockaddr_in();
            v4.sin_len = (byte) Native.getNativeSize(v4.getClass(), v4);
            return v4;
        }

        @Override
        public InetAddress getAddress() throws UnknownHostException
        {
            return InetAddress.getByAddress(sin_addr);
        }

        @Override
        public int getFamily()
        {
            return LibC.AF_INET;
        }

        @Override
        protected List getFieldOrder()
        {
            return
                Arrays.asList(
                        new String[]
                        {
                            "sin_len",
                            "sin_family",
                            "sin_port",
                            "sin_addr",
                            "sin_zero"
                        });
        }
    }

    /**
     * JNA mapping of <tt>sockaddr_in6</tt>
     */
    public final static class sockaddr_in6
        extends sockaddr
    {
        public byte sin6_len;
        public byte sin6_family;
        public short sin6_port;
        public int sin6_flowinfo;
        public byte[] sin6_addr;
        public int sin6_scope_id;

        public sockaddr_in6()
        {
            sin6_family = LibC.AF_INET6;
            sin6_addr = new byte[16];
        }

        @Override
        public sockaddr createEmpty()
        {
            sockaddr_in6 v6 = new sockaddr_in6();
            v6.sin6_len = (byte) Native.getNativeSize(v6.getClass(), v6);
            return v6;
        }

        @Override
        public InetAddress getAddress() throws UnknownHostException
        {
            return InetAddress.getByAddress(sin6_addr);
        }

        @Override
        public int getFamily()
        {
            return LibC.AF_INET6;
        }

        @Override
        protected List getFieldOrder()
        {
            return
                Arrays.asList(
                        new String[]
                        {
                            "sin6_len",
                            "sin6_family",
                            "sin6_port",
                            "sin6_flowinfo",
                            "sin6_addr",
                            "sin6_scope_id"
                        });
        }
    }

    /**
     * Gets the local address of a UDP socket that connects to the given
     * address.
     * 
     * @param dest The destination to which the socket connects.
     * @return The local address of the connecting socket.
     * @throws IOException When the socket cannot be created, connected or
     *             returns an invalid local address.
     */
    public static InetAddress getLocalSocketAddress(InetSocketAddress dest)
        throws IOException
    {
        sockaddr addr = sockaddr.create(dest);
        int fd = LibC.INSTANCE.socket(addr.getFamily(), LibC.SOCK_DGRAM, 0);
        if (fd == -1)
        {
            throw new LibC.CException();
        }

        try
        {
            int err = LibC.INSTANCE.connect(fd, addr, addr.getSize());
            if (err != 0)
            {
                throw new LibC.CException();
            }

            sockaddr local = addr.createEmpty();
            IntByReference size = new IntByReference(local.getSize());
            err = LibC.INSTANCE.getsockname(fd, local, size);
            if (err != 0)
            {
                throw new LibC.CException();
            }

            return local.getAddress();
        }
        finally
        {
            LibC.INSTANCE.close(fd);
        }
    }
}
