/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.gtalk;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;

/**
 * Google Talk version of Candidate packet extension.
 *
 * @author Sebastien Vincent
 */
public class GTalkCandidatePacketExtension
    extends CandidatePacketExtension
{
    /**
     * Address attribute name.
     */
    public static final String ADDRESS_ATTR_NAME = "address";

    /**
     * The name of the "preference" element.
     */
    public static final String PREFERENCE_ATTR_NAME = "preference";

    /**
     * Name attribute name.
     */
    public static final String NAME_ATTR_NAME = "name";

    /**
     * Username attribute name.
     */
    public static final String USERNAME_ATTR_NAME = "username";

    /**
     * Password attribute name.
     */
    public static final String PASSWORD_ATTR_NAME = "password";

    /**
     * Name for candidate audio RTP.
     */
    public static final String AUDIO_RTP_NAME = "rtp";

    /**
     * Name for candidate audio RTCP.
     */
    public static final String AUDIO_RTCP_NAME = "rtcp";

    /**
     * Name for candidate video RTP.
     */
    public static final String VIDEO_RTP_NAME = "video_rtp";

    /**
     * Name for candidate video RTCP.
     */
    public static final String VIDEO_RTCP_NAME = "video_rtcp";

    /**
     * Constructs a new <tt>GTalkCandidatePacketExtension</tt>.
     */
    public GTalkCandidatePacketExtension()
    {
    }

    /**
     * Sets a Candidate Type as defined in ICE-CORE.
     *
     * @param type this candidates' type as per ICE's Google dialect.
     */
    public void setType(String type)
    {
        super.setAttribute(TYPE_ATTR_NAME, type);
    }

    /**
     * Sets this candidate's name.
     *
     * @param name this candidate's name
     */
    public void setName(String name)
    {
        super.setAttribute(NAME_ATTR_NAME, name);
    }

    /**
     * Returns this candidate's name.
     *
     * @return this candidate's name
     */
    public String getName()
    {
        return super.getAttributeAsString(NAME_ATTR_NAME);
    }

    /**
     * Sets this candidate's preference.
     *
     * @param preference this candidate's preference
     */
    public void setPreference(double preference)
    {
        super.setAttribute(PREFERENCE_ATTR_NAME, Double.toString((preference)));
    }

    /**
     * Returns this candidate's preference.
     *
     * @return this candidate's preference
     */
    public double getPreference()
    {
        return Float.parseFloat(
                super.getAttributeAsString(PREFERENCE_ATTR_NAME));
    }

    /**
     * Sets this candidate's username.
     *
     * @param username this candidate's username
     */
    public void setUsername(String username)
    {
        super.setAttribute(USERNAME_ATTR_NAME, username);
    }

    /**
     * Returns this candidate's username.
     *
     * @return this candidate's username
     */
    public String getUsername()
    {
        return super.getAttributeAsString(USERNAME_ATTR_NAME);
    }

    /**
     * Sets this candidate's password.
     *
     * @param password this candidate's password
     */
    public void setPassword(String password)
    {
        super.setAttribute(PASSWORD_ATTR_NAME, password);
    }

    /**
     * Returns this candidate's password.
     *
     * @return this candidate's password
     */
    public String getPassword()
    {
        return super.getAttributeAsString(PASSWORD_ATTR_NAME);
    }

    /**
     * Sets this candidate's Internet Protocol (IP) address; this can be either
     * an IPv4 address or an IPv6 address.
     *
     * @param ip this candidate's IPv4 or IPv6 address.
     */
    public void setAddress(String ip)
    {
        super.setAttribute(ADDRESS_ATTR_NAME, ip);
    }

    /**
     * Returns this candidate's Internet Protocol (IP) address; this can be
     * either an IPv4 address or an IPv6 address.
     *
     * @return this candidate's IPv4 or IPv6 address.
     */
    public String getAddress()
    {
        return super.getAttributeAsString(ADDRESS_ATTR_NAME);
    }
}
