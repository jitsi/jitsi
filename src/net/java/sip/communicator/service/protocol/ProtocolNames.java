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
package net.java.sip.communicator.service.protocol;

/**
 * Specifies the names of some of the most popular instant messaging protocols.
 * These names should be used when registering an implementation of a protocol
 * with the osgi framework. These names must be set in the properties dictionary
 * that one specifies when registering an OSGI service. When setting one of
 * these names, a protocol implementor must map it against the
 * ProtocolProviderFactory.PROTOCOL_PROPERTY_NAME key.
 * @author Emil Ivov
 */
public interface ProtocolNames
{
    /**
     * The SIP (and SIMPLE) protocols.
     */
    public static final String SIP  = "SIP";

    /**
     * The Jabber protocol.
     */
    public static final String JABBER  = "Jabber";

    /**
     * The IRC protocol.
     */
    public static final String IRC = "IRC";

    /**
     * The Gadu-Gadu protocol.
     */
    public static final String GADU_GADU = "Gadu-Gadu";

    /**
     * The GroupWise protocol.
     */
    public static final String GROUP_WISE = "GroupWise";

    /**
     * The ICQ service protocol.
     */
    public static final String ICQ = "ICQ";

    /**
     * The AIM service protocol.
     */
    public static final String AIM = "AIM";

    /**
     * The Yahoo! messenger protocol.
     */
    public static final String YAHOO  = "Yahoo!";

    /**
     * The Skype protocol.
     */
    public static final String SKYPE  = "Skype";

    /**
     * The SIP Communicator MOCK protocol.
     */
    public static final String SIP_COMMUNICATOR_MOCK  = "sip-communicator-mock";

    /**
     * The Zeroconf protocol.
     */
    public static final String ZEROCONF  = "Zeroconf";

    /**
     * The SSH protocol.
     */
    public static final String SSH = "SSH";

    /**
     * The Gibberish protocol.
     */
    public static final String GIBBERISH = "Gibberish";

    /**
     * The Dict protocol.
     */
    public static final String DICT = "Dict";

    /**
     * The Facebook protocol.
     */
    public static final String FACEBOOK = "Facebook";
}
