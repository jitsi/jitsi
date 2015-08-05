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
package net.java.sip.communicator.service.dns;

import org.xbill.DNS.*;

/**
 * The purpose of this class is to help avoid the significant delays that occur
 * in networks where DNS servers would ignore SRV, NAPTR, and sometimes even
 * A/AAAA queries (i.e. without even sending an error response). We also try to
 * handle cases where DNS servers may return empty responses to some records.
 * <p>
 * We achieve this by entering a redundant mode whenever we detect an abnormal
 * delay (longer than <tt>DNS_PATIENCE</tt>)  while waiting for a DNS resonse,
 * or when that response is not considered satisfying.
 * <p>
 * Once we enter redundant mode, we start duplicating all queries and sending
 * them to both our primary and backup resolvers (in case we have any). We then
 * always return the first response we get, regardless of who sent it.
 * <p>
 * We exit redundant mode after receiving <tt>DNS_REDEMPTION</tt> consecutive
 * timely and correct responses from our primary resolver.
 *
 * @author Emil Ivov
 */
public interface CustomResolver
    extends Resolver
{
    /**
     * The default number of milliseconds it takes us to get into redundant
     * mode while waiting for a DNS query response.
     */
    public static final int DNS_PATIENCE = 1500;

    /**
     * The name of the property that allows us to override the default
     * <tt>DNS_PATIENCE</tt> value.
     */
    public static final String PNAME_DNS_PATIENCE
        = "net.java.sip.communicator.util.dns.DNS_PATIENCE";

    /**
     * The default number of times that the primary DNS would have to provide a
     * faster response than the backup resolver before we consider it safe
     * enough to exit redundant mode.
     */
    public static final int DNS_REDEMPTION = 3;

    /**
     * The name of the property that allows us to override the default
     * <tt>DNS_REDEMPTION</tt> value.
     */
    public static final String PNAME_DNS_REDEMPTION
        = "net.java.sip.communicator.util.dns.DNS_REDEMPTION";

    /**
     * The currently configured number of times that the primary DNS would have
     * to provide a faster response than the backup resolver before we consider
     * it safe enough to exit redundant mode.
     */
    public static int currentDnsRedemption = DNS_REDEMPTION;

    /**
     * The name of the property that enables or disables the DNSSEC resolver
     * (instead of a normal, non-validating local resolver).
     */
    public static final String PNAME_DNSSEC_RESOLVER_ENABLED
        = "net.java.sip.communicator.util.dns.DNSSEC_ENABLED";

    /**
     * Default value of @see PNAME_DNSSEC_RESOLVER_ENABLED.
     */
    public static final boolean PDEFAULT_DNSSEC_RESOLVER_ENABLED = false;

    /**
     * Resets resolver configuration and populate our default resolver
     * with the newly configured servers.
     */
    public void reset();
}
