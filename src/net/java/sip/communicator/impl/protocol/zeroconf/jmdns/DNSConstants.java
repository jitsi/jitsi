/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright 2003-2005 Arthur van Hoff, Rick Blair
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

/**
 * DNS constants.
 *
 * @version %I%, %G%
 * @author  Arthur van Hoff, Jeff Sonstein,
 *          Werner Randelshofer, Pierre Frisch, Rick Blair
 */
public final class DNSConstants
{

    // changed to final class - jeffs
    final static String MDNS_GROUP = "224.0.0.251";
    final static String MDNS_GROUP_IPV6 = "FF02::FB";
    final static int MDNS_PORT = 5353;
    final static int DNS_PORT = 53;
    // default one hour TTL
    final static int DNS_TTL = 60 * 60;
    // two hour TTL (draft-cheshire-dnsext-multicastdns.txt ch 13)
    // final static int DNS_TTL = 120 * 60;

    final static int MAX_MSG_TYPICAL = 1460;
    final static int MAX_MSG_ABSOLUTE = 8972;

    final static int FLAGS_QR_MASK = 0x8000;    // Query response mask
    final static int FLAGS_QR_QUERY = 0x0000;   // Query
    final static int FLAGS_QR_RESPONSE = 0x8000;// Response

    public final static int FLAGS_AA = 0x0400;  // Authorative answer
    final static int FLAGS_TC = 0x0200;         // Truncated
    final static int FLAGS_RD = 0x0100;         // Recursion desired
    public final static int FLAGS_RA = 0x8000;  // Recursion available

    final static int FLAGS_Z = 0x0040;          // Zero
    final static int FLAGS_AD = 0x0020;         // Authentic data
    final static int FLAGS_CD = 0x0010;         // Checking disabled

    // Final Static Internet
    public final static int CLASS_IN = 1;
    // CSNET
    final static int CLASS_CS = 2;
    // CHAOS
    final static int CLASS_CH = 3;
    // Hesiod
    final static int CLASS_HS = 4;
    // Used in DNS UPDATE [RFC 2136]
    final static int CLASS_NONE = 254;
    // Not a DNS class, but a DNS query class, meaning "all classes"
    final static int CLASS_ANY = 255;
    // Multicast DNS uses the bottom 15 bits to identify the record class...
    final static int CLASS_MASK = 0x7FFF;
    // ... and the top bit indicates that all other cached records are now invalid
    public final static int CLASS_UNIQUE = 0x8000;

    final static int TYPE_IGNORE = 0;   // This is a hack to stop further processing
    public final static int TYPE_A = 1; // Address
    final static int TYPE_NS = 2;       // Name Server
    final static int TYPE_MD = 3;       // Mail Destination
    final static int TYPE_MF = 4;       // Mail Forwarder
    final static int TYPE_CNAME = 5;    // Canonical Name
    final static int TYPE_SOA = 6;      // Start of Authority
    final static int TYPE_MB = 7;       // Mailbox
    final static int TYPE_MG = 8;       // Mail Group
    final static int TYPE_MR = 9;       // Mail Rename
    final static int TYPE_NULL = 10;    // NULL RR
    final static int TYPE_WKS = 11;     // Well-known-service
    final static int TYPE_PTR = 12;     // Domain Name pofinal static inter
    final static int TYPE_HINFO = 13;   // Host information
    final static int TYPE_MINFO = 14;   // Mailbox information
    final static int TYPE_MX = 15;      // Mail exchanger
    public final static int TYPE_TXT = 16;// Arbitrary text string
    final static int TYPE_RP = 17;      // for Responsible Person                 [RFC1183]
    final static int TYPE_AFSDB = 18;   // for AFS Data Base location             [RFC1183]
    final static int TYPE_X25 = 19;     // for X.25 PSDN address                  [RFC1183]
    final static int TYPE_ISDN = 20;    // for ISDN address                       [RFC1183]
    final static int TYPE_RT = 21;      // for Route Through                      [RFC1183]
    final static int TYPE_NSAP = 22;    // for NSAP address, NSAP style A record  [RFC1706]
    final static int TYPE_NSAP_PTR = 23;//
    final static int TYPE_SIG = 24;     // for security signature                 [RFC2931]
    final static int TYPE_KEY = 25;     // for security key                       [RFC2535]
    final static int TYPE_PX = 26;      // X.400 mail mapping information         [RFC2163]
    final static int TYPE_GPOS = 27;    // Geographical Position                  [RFC1712]
    final static int TYPE_AAAA = 28;    // IP6 Address                            [Thomson]
    final static int TYPE_LOC = 29;     // Location Information                   [Vixie]
    final static int TYPE_NXT = 30;     // Next Domain - OBSOLETE                 [RFC2535, RFC3755]
    final static int TYPE_EID = 31;     // Endpoint Identifier                    [Patton]
    final static int TYPE_NIMLOC = 32;  // Nimrod Locator                         [Patton]
    public final static int TYPE_SRV = 33;// Server Selection                     [RFC2782]
    final static int TYPE_ATMA = 34;    // ATM Address                            [Dobrowski]
    final static int TYPE_NAPTR = 35;   // Naming Authority Pointer               [RFC2168, RFC2915]
    final static int TYPE_KX = 36;      // Key Exchanger                          [RFC2230]
    final static int TYPE_CERT = 37;    // CERT                                   [RFC2538]
    final static int TYPE_A6 = 38;      // A6                                     [RFC2874]
    final static int TYPE_DNAME = 39;   // DNAME                                  [RFC2672]
    final static int TYPE_SINK = 40;    // SINK                                   [Eastlake]
    final static int TYPE_OPT = 41;     // OPT                                    [RFC2671]
    final static int TYPE_APL = 42;     // APL                                    [RFC3123]
    final static int TYPE_DS = 43;      // Delegation Signer                      [RFC3658]
    final static int TYPE_SSHFP = 44;   // SSH Key Fingerprint                    [RFC-ietf-secsh-dns-05.txt]
    final static int TYPE_RRSIG = 46;   // RRSIG                                  [RFC3755]
    final static int TYPE_NSEC = 47;    // NSEC                                   [RFC3755]
    final static int TYPE_DNSKEY = 48;  // DNSKEY                                 [RFC3755]
    final static int TYPE_UINFO = 100;  //                                        [IANA-Reserved]
    final static int TYPE_UID = 101;    //                                        [IANA-Reserved]
    final static int TYPE_GID = 102;    //                                        [IANA-Reserved]
    final static int TYPE_UNSPEC = 103; //                                        [IANA-Reserved]
    final static int TYPE_TKEY = 249;   // Transaction Key                        [RFC2930]
    final static int TYPE_TSIG = 250;   // Transaction Signature                  [RFC2845]
    final static int TYPE_IXFR = 251;   // Incremental transfer                   [RFC1995]
    final static int TYPE_AXFR = 252;   // Transfer of an entire zone             [RFC1035]
    final static int TYPE_MAILA = 253;  // Mailbox-related records (MB, MG or MR) [RFC1035]
    final static int TYPE_MAILB = 254;  // Mail agent RRs (Obsolete - see MX)     [RFC1035]
    final static int TYPE_ANY = 255;    // Request for all records                [RFC1035]

    //Time Intervals for various functions

    //milliseconds before send shared query
    final static int SHARED_QUERY_TIME = 20;
    //milliseconds between query loops.
    final static int QUERY_WAIT_INTERVAL = 225;
    //milliseconds between probe loops.
    final static int PROBE_WAIT_INTERVAL = 250;
    //minimal wait interval for response.
    final static int RESPONSE_MIN_WAIT_INTERVAL = 20;
    //maximal wait interval for response
    final static int RESPONSE_MAX_WAIT_INTERVAL = 115;
    //milliseconds to wait after conflict.
    final static int PROBE_CONFLICT_INTERVAL = 1000;
    //After x tries go 1 time a sec. on probes.
    final static int PROBE_THROTTLE_COUNT = 10;
    //We only increment the throttle count, if
    // the previous increment is inside this interval.
    final static int PROBE_THROTTLE_COUNT_INTERVAL = 5000;
    //milliseconds between Announce loops.
    final static int ANNOUNCE_WAIT_INTERVAL = 1000;
    //milliseconds between cache cleanups.
    final static int RECORD_REAPER_INTERVAL = 10000;

    final static int KNOWN_ANSWER_TTL = 120;
    // 50% of the TTL in milliseconds
    final static int ANNOUNCED_RENEWAL_TTL_INTERVAL = DNS_TTL * 500;
}
