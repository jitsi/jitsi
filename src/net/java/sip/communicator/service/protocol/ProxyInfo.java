/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
    public final static String CONNECTON_PROXY_TYPE_PROPERTY_NAME =
        "net.java.sip.communicator.service.connectionProxyType";

    /**
     * Stores in the configuration the connection proxy address.
     */
    public final static String CONNECTON_PROXY_ADDRESS_PROPERTY_NAME =
        "net.java.sip.communicator.service.connectionProxyAddress";

    /**
     * Stores in the configuration the connection proxy port.
     */
    public final static String CONNECTON_PROXY_PORT_PROPERTY_NAME =
        "net.java.sip.communicator.service.connectionProxyPort";

    /**
     * Stores in the configuration the connection proxy username.
     */
    public final static String CONNECTON_PROXY_USERNAME_PROPERTY_NAME =
        "net.java.sip.communicator.service.connectionProxyUsername";

    /**
     * Stores in the configuration the connection proxy password.
     */
    public final static String CONNECTON_PROXY_PASSWORD_PROPERTY_NAME =
        "net.java.sip.communicator.service.connectionProxyPassword";                            
}
