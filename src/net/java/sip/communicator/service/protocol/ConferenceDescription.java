/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

/**
 * A description of a conference call that can be dialed into. Contains an
 * URI and additional parameters to use.
 *
 * @author Boris Grozev
 */
public class ConferenceDescription
{
    /**
     * The URI of the conference.
     */
    private String uri;

    /**
     * The subject of the conference.
     */
    private String subject;

    /**
     * The call ID to use to call into the conference.
     */
    private String callId;

    /**
     * The password to use to call into the conference.
     */
    private String password;

    /**
     * The transport methods supported for calling into the conference.
     *
     * If the set is empty, the intended interpretation is that there are no
     * restrictions on the supported transports (e.g. that all transports are
     * supported).
     */
    private Set<Transport> transports = new HashSet<Transport>();

    /**
     * Creates a new instance with the specified <tt>uri</tt>, <tt>callId</tt>
     * and <tt>password</tt>.
     * @param uri the <tt>uri</tt> to set.
     * @param callId the <tt>callId</tt> to set.
     * @param password the <tt>auth</tt> to set.
     */
    public ConferenceDescription(String uri, String callId, String password)
    {
        this.uri = uri;
        this.callId = callId;
        this.password = password;
    }

    /**
     * Creates a new instance with the specified <tt>uri</tt> and <tt>callId</tt>
     * @param uri the <tt>uri</tt> to set.
     * @param callId the <tt>callId</tt> to set.
     */
    public ConferenceDescription(String uri, String callId)
    {
        this(uri, callId, null);
    }

    /**
     * Creates a new instance with the specified <tt>uri</tt>.
     * @param uri the <tt>uri</tt> to set.
     */
    public ConferenceDescription(String uri)
    {
        this(uri, null, null);
    }

    /**
     * Gets the uri of this <tt>ConferenceDescription</tt>.
     * @return the uri of this <tt>ConferenceDescription</tt>.
     */
    public String getUri() {
        return uri;
    }

    /**
     * Sets the uri of this <tt>ConferenceDescription</tt>.
     * @param uri the value to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Gets the subject of this <tt>ConferenceDescription</tt>.
     * @return the subject of this <tt>ConferenceDescription</tt>.
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Sets the subject of this <tt>ConferenceDescription</tt>.
     * @param subject the value to set
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Gets the call ID of this <tt>ConferenceDescription</tt>
     * @return the call ID of this <tt>ConferenceDescription</tt>
     */
    public String getCallId() {
        return callId;
    }

    /**
     * Sets the call ID of this <tt>ConferenceDescription</tt>.
     * @param callId the value to set
     */
    public void setCallId(String callId) {
        this.callId = callId;
    }

    /**
     * Gets the password of this <tt>ConferenceDescription</tt>
     * @return the password of this <tt>ConferenceDescription</tt>
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the auth of this <tt>ConferenceDescription</tt>.
     * @param password the value to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Adds a <tt>Transport</tt> to the set of <tt>Transport</tt>s supported
     * by the conference.
     * @param transport the <tt>Transport</tt> to add.
     */
    public void addTransport(Transport transport)
    {
        transports.add(transport);
    }

    /**
     * Checks whether <tt>transport</tt> is supported by this
     * <tt>ConferenceDescription</tt>.
     * @param transport the <tt>Transport</tt> to check.
     * @return <tt>true</tt> if <tt>transport</tt> is supported by this
     * <tt>ConferenceDescription</tt>
     */
    public boolean supportsTransport(Transport transport)
    {
        /*
         * An empty list means that all transports are supported.
         */
        if (transports.isEmpty())
            return true;
        return transports.contains(transport);
    }

    /**
     * Returns the set of <tt>Transport</tt>s supported by this
     * <tt>ConferenceDescription</tt>
     * @return the set of <tt>Transport</tt>s supported by this
     * <tt>ConferenceDescription</tt>
     */
    public Set<Transport> getSupportedTransports()
    {
        return new HashSet<Transport>(transports);
    }

    /**
     * A list of possible transport methods that could be supported by a
     * <tt>ConferenceDescription</tt>.
     */
    public static enum Transport
    {
        /**
         * ICE.
         */
        ICE("ice"),

        /**
         * RAW UDP.
         */
        RAW_UDP("raw-udp");

        /**
         * The name of this <tt>Transport</tt>
         */
        private String name;

        /**
         * Creates a new instance.
         *
         * @param name the name of the new instance.
         */
        private Transport(String name)
        {
            this.name = name;
        }

        /**
         * Returns the name of the instance.
         * @return the name of the instance.
         */
        @Override
        public String toString()
        {
            return name;
        }

        /**
         * Parses a <tt>String</tt> and returns one of the instances defined
         * in the enum, or <tt>null</tt> on failure to parse.
         * @param str the <tt>String</tt> to parse.
         * @return one of the instances of the enum or <tt>null</tt> on failure
         * to parse.
         */
        public static Transport parseString(String str)
        {
            if (str == null)
                return null;
            else if (str.equals(ICE.toString()))
                return ICE;
            else if (str.equals(RAW_UDP.toString()))
                return RAW_UDP;

            return null;
        }
    }
}
