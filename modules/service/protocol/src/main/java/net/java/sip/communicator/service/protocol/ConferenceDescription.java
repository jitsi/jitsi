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
     * The name of the conference.
     */
    private String displayName;
    /**
     * Whether the conference is available or not.
     */
    private boolean available = true;

    /**
     * The transport methods supported for calling into the conference.
     *
     * If the set is empty, the intended interpretation is that it is up to the
     * caller to chose an appropriate transport.
     */
    private Set<String> transports = new HashSet<String>();

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
     * Creates a new instance.
     */
    public ConferenceDescription()
    {
        this(null, null, null);
    }

    /**
     * Returns the display name of the conference.
     * @return the display name
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Sets the display name of the conference.
     * @param displayName the display name to set
     */
    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    /**
     * Gets the uri of this <tt>ConferenceDescription</tt>.
     * @return the uri of this <tt>ConferenceDescription</tt>.
     */
    public String getUri()
    {
        return uri;
    }

    /**
     * Sets the uri of this <tt>ConferenceDescription</tt>.
     * @param uri the value to set
     */
    public void setUri(String uri)
    {
        this.uri = uri;
    }

    /**
     * Gets the subject of this <tt>ConferenceDescription</tt>.
     * @return the subject of this <tt>ConferenceDescription</tt>.
     */
    public String getSubject()
    {
        return subject;
    }

    /**
     * Sets the subject of this <tt>ConferenceDescription</tt>.
     * @param subject the value to set
     */
    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    /**
     * Gets the call ID of this <tt>ConferenceDescription</tt>
     * @return the call ID of this <tt>ConferenceDescription</tt>
     */
    public String getCallId()
    {
        return callId;
    }

    /**
     * Sets the call ID of this <tt>ConferenceDescription</tt>.
     * @param callId the value to set
     */
    public void setCallId(String callId)
    {
        this.callId = callId;
    }

    /**
     * Gets the password of this <tt>ConferenceDescription</tt>
     * @return the password of this <tt>ConferenceDescription</tt>
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * Sets the auth of this <tt>ConferenceDescription</tt>.
     * @param password the value to set
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * Checks if the conference is available.
     * @return <tt>true</tt> iff the conference is available.
     */
    public boolean isAvailable()
    {
        return available;
    }

    /**
     * Sets the availability of this <tt>ConferenceDescription</tt>.
     * @param available the value to set
     */
    public void setAvailable(boolean available)
    {
        this.available = available;
    }

    /**
     * Adds a <tt>Transport</tt> to the set of <tt>Transport</tt>s supported
     * by the conference.
     * @param transport the <tt>Transport</tt> to add.
     */
    public void addTransport(String transport)
    {
        transports.add(transport);
    }

    /**
     * Checks whether <tt>transport</tt> is supported by this
     * <tt>ConferenceDescription</tt>. If the set of transports for this
     * <tt>ConferenceDescription</tt> is empty, always returns true.
     * @param transport the <tt>Transport</tt> to check.
     * @return <tt>true</tt> if <tt>transport</tt> is supported by this
     * <tt>ConferenceDescription</tt>
     */
    public boolean supportsTransport(String transport)
    {
        /*
         * An empty list means that all transports are supported.
         */
        if (transports.isEmpty())
            return true;
        return transports.contains(transport);
    }

    /**
     * Returns the transports supported by this <tt>ConferenceDescription</tt>
     * @return the supported by this <tt>ConferenceDescription</tt>
     */
    public Set<String> getSupportedTransports()
    {
        return new HashSet<String>(transports);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "ConferenceDescription(uri="+uri+"; callid="+callId+")";
    }
    
    
    /**
     * Checks if two <tt>ConferenceDescription</tt> instances have the same 
     * call id, URI and supported transports.
     * 
     * @param cd1 the first <tt>ConferenceDescription</tt> instance.
     * @param cd2 the second <tt>ConferenceDescription</tt> instance.
     * @return <tt>true</tt> if the <tt>ConferenceDescription</tt> instances 
     * have the same call id, URI and supported transports. Otherwise 
     * <tt>false</tt> is returned.
     */
    public boolean compareConferenceDescription(ConferenceDescription cd)
    {
        return (getCallId().equals(cd.getCallId())
            && getUri().equals(cd.getUri())
            && getSupportedTransports().equals(
                cd.getSupportedTransports()));
    }
}
