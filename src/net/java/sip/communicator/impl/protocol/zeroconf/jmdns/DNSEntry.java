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

import java.util.logging.*;

/**
 * DNS entry with a name, type, and class. This is the base
 * class for questions and records.
 *
 * @version %I%, %G%
 * @author  Arthur van Hoff, Pierre Frisch, Rick Blair
 * @author Christian Vincenot
 */
public class DNSEntry
{
    private static Logger logger = Logger.getLogger(DNSEntry.class.toString());
    String key;
    String name;
    int type;
    int clazz;
    boolean unique;

    /**
     * Create an entry.
     */
    DNSEntry(String name, int type, int clazz)
    {
        this.key = name.toLowerCase();
        this.name = name;
        this.type = type;
        this.clazz = clazz & DNSConstants.CLASS_MASK;
        this.unique = (clazz & DNSConstants.CLASS_UNIQUE) != 0;

        String SLevel = System.getProperty("jmdns.debug");
        if (SLevel == null) SLevel = "INFO";
        logger.setLevel(Level.parse(SLevel));
    }

    /**
     * Check if two entries have exactly the same name, type, and class.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof DNSEntry)
        {
            DNSEntry other = (DNSEntry) obj;
            return name.equals(other.name) &&
                   type == other.type &&
                   clazz == other.clazz;
        }
        return false;
    }

    public String getName()
    {
        return name;
    }

    public int getType()
    {
        return type;
    }

    public int getClazz()
    {
        return clazz;
    }


    public boolean isUnique()
    {
        return unique;
    }

    /**
     * Overriden, to return a value which is consistent with the value returned
     * by equals(Object).
     */
    @Override
    public int hashCode()
    {
        return name.hashCode() + type + clazz;
    }

    /**
     * Get a string given a clazz.
     */
    static String getClazz(int clazz)
    {
        switch (clazz & DNSConstants.CLASS_MASK)
        {
            case DNSConstants.CLASS_IN:
                return "in";
            case DNSConstants.CLASS_CS:
                return "cs";
            case DNSConstants.CLASS_CH:
                return "ch";
            case DNSConstants.CLASS_HS:
                return "hs";
            case DNSConstants.CLASS_NONE:
                return "none";
            case DNSConstants.CLASS_ANY:
                return "any";
            default:
                return "?";
        }
    }

    /**
     * Get a string given a type.
     */
    static String getType(int type)
    {
        switch (type)
        {
            case DNSConstants.TYPE_A:
                return "a";
            case DNSConstants.TYPE_AAAA:
                return "aaaa";
            case DNSConstants.TYPE_NS:
                return "ns";
            case DNSConstants.TYPE_MD:
                return "md";
            case DNSConstants.TYPE_MF:
                return "mf";
            case DNSConstants.TYPE_CNAME:
                return "cname";
            case DNSConstants.TYPE_SOA:
                return "soa";
            case DNSConstants.TYPE_MB:
                return "mb";
            case DNSConstants.TYPE_MG:
                return "mg";
            case DNSConstants.TYPE_MR:
                return "mr";
            case DNSConstants.TYPE_NULL:
                return "null";
            case DNSConstants.TYPE_WKS:
                return "wks";
            case DNSConstants.TYPE_PTR:
                return "ptr";
            case DNSConstants.TYPE_HINFO:
                return "hinfo";
            case DNSConstants.TYPE_MINFO:
                return "minfo";
            case DNSConstants.TYPE_MX:
                return "mx";
            case DNSConstants.TYPE_TXT:
                return "txt";
            case DNSConstants.TYPE_SRV:
                return "srv";
            case DNSConstants.TYPE_ANY:
                return "any";
            default:
                return "?";
        }
    }

    public String toString(String hdr, String other)
    {
        return hdr + "[" + getType(type) + "," +
                            getClazz(clazz) + (unique ? "-unique," : ",") +
                            name + ((other != null) ? "," +
                            other + "]" : "]");
    }
}
