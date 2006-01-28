package net.java.sip.communicator.service.protocol;

/**
 * Specifies the names of some of the most popular instant messageing protocols.
 * These names should be used when registering an implementation of a protocol
 * with the osgi framwke. These names must be set in the properties dictionary
 * that one specifies when registering an OSGI service. When setting one of
 * these names a protocol implementor must map it against the
 * ProtocolAccountManager.PROTOCOL_PROPERTY_NAME key.
 * @author Emil Ivov
 */
public interface ProtocolNames
{
    /**
     * The SIP (and SIMPLE) protcools.
     */
    public static final String SIP  = "SIP";

    /**
     * The Jabber protcool.
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
     * The MSN messenger protocol.
     */
    public static final String MSN = "MSN";

    /**
     * The Yahoo! messenger protcool.
     */
    public static final String YAHOO  = "Yahoo!";

    /**
     * The Skype protcool.
     */
    public static final String SKYPE  = "Skype";
}
