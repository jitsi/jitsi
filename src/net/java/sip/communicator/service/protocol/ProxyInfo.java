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
 * The supported proxy types and properties used to store the values
 * in the configuration service.
 *
 * @author Damian Minkov
 */
public class ProxyInfo
{
    /**
     * Enum which stores possible proxy types
     */
    public static enum ProxyType
    {
        /**
         * Proxy is not used.
         */
        NONE,
        /**
         * HTTP proxy type.
         */
        HTTP,
        /**
         * Proxy type socks4.
         */
        SOCKS4,
        /**
         * Proxy type socks5.
         */
        SOCKS5
    }

    /**
     * Stores in the configuration the connection proxy type.
     */
    public final static String CONNECTION_PROXY_TYPE_PROPERTY_NAME =
        "net.java.sip.communicator.service.connectionProxyType";

    /**
     * Stores in the configuration the connection proxy address.
     */
    public final static String CONNECTION_PROXY_ADDRESS_PROPERTY_NAME =
        "net.java.sip.communicator.service.connectionProxyAddress";

    /**
     * Stores in the configuration the connection proxy port.
     */
    public final static String CONNECTION_PROXY_PORT_PROPERTY_NAME =
        "net.java.sip.communicator.service.connectionProxyPort";

    /**
     * Stores in the configuration the connection proxy username.
     */
    public final static String CONNECTION_PROXY_USERNAME_PROPERTY_NAME =
        "net.java.sip.communicator.service.connectionProxyUsername";

    /**
     * Stores in the configuration the connection proxy password.
     */
    public final static String CONNECTION_PROXY_PASSWORD_PROPERTY_NAME =
        "net.java.sip.communicator.service.connectionProxyPassword";

    /**
     * Stores in the configuration the connection dns forwarding is it enabled.
     */
    public final static String CONNECTION_PROXY_FORWARD_DNS_PROPERTY_NAME =
        "net.java.sip.communicator.service.connectionProxyForwardDNS";

    /**
     * Stores in the configuration the connection dns forwarding address.
     */
    public final static String CONNECTION_PROXY_FORWARD_DNS_ADDRESS_PROPERTY_NAME
        = "net.java.sip.communicator.service.connectionProxyForwardDNSAddress";

    /**
     * Stores in the configuration the connection dns forwarding port.
     */
    public final static String CONNECTION_PROXY_FORWARD_DNS_PORT_PROPERTY_NAME
        = "net.java.sip.communicator.service.connectionProxyForwardDNSPort";
}
