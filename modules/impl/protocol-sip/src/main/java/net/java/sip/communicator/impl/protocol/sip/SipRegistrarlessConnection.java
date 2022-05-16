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
package net.java.sip.communicator.impl.protocol.sip;

import javax.sip.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * Allows SIP communicator to create SIP accounts without a registrar. We use
 * this class as a replacement of the SipRegistrarConnection for accounts that
 * do not have a configured registrar.
 *
 * @author Emil Ivov
 */
public class SipRegistrarlessConnection
        extends SipRegistrarConnection
{

    /**
     * A reference to the sip provider that created us.
     */
    private ProtocolProviderServiceSipImpl sipProvider = null;

    /**
     * The transport that we should claim to be using in case some of the other
     * components of the sip package would try to use it as the default transort
     * to connect with.
     */
    private String defaultTransport = null;

    /**
     * Keeps our current registration state.
     */
    private RegistrationState currentRegistrationState
        = RegistrationState.UNREGISTERED;

    /**
     * Creates a new instance of this class.
     *
     * @param sipProviderCallback a reference to the
     * ProtocolProviderServiceSipImpl instance that created us.
     * @param defaultTransport the transport that we should fake to be using
     * in case some of the other components in the sip package wants to use it
     * as a default.
     */
    public SipRegistrarlessConnection(
                    ProtocolProviderServiceSipImpl sipProviderCallback,
                    String defaultTransport)
    {
        this.sipProvider = sipProviderCallback;
        this.defaultTransport = defaultTransport;
    }

    /**
     * Simply sets the state of the connection to REGISTERED without doing
     * anything else.
     *
     * @throws OperationFailedException never thrown
     */
    @Override
    void register()
        throws OperationFailedException
    {
        setRegistrationState(RegistrationState.REGISTERED,
                             RegistrationStateChangeEvent.REASON_USER_REQUEST,
                             null);
    }

    /**
     * Simply sets the state of the connection to UNREGISTERED without doing
     * anything else.
     *
     * @throws OperationFailedException never thrown.
     */
    @Override
    public void unregister() throws OperationFailedException
    {
        // using and transition states, cause some op.sets like
        // OpSetPresence use it
        setRegistrationState(RegistrationState.UNREGISTERING,
                RegistrationStateChangeEvent.REASON_USER_REQUEST, "");

        setRegistrationState(RegistrationState.UNREGISTERED,
                             RegistrationStateChangeEvent.REASON_USER_REQUEST,
                             null);
    }

    /**
     * Returns the state of this connection.
     *
     * @return a RegistrationState instance indicating the state of our
     * registration with the corresponding registrar.
     */
    @Override
    public RegistrationState getRegistrationState()
    {
        return currentRegistrationState;
    }

    /**
     * Sets our registration state to <tt>newState</tt> and dispatches an event
     * through the protocol provider service impl.
     * <p>
     * @param newState a reference to the RegistrationState that we're currently
     * detaining.
     * @param reasonCode one of the REASON_XXX error codes specified in
     * {@link RegistrationStateChangeEvent}.
     * @param reason a reason String further explaining the reasonCode.
     */
    @Override
    public void setRegistrationState(RegistrationState newState,
                                     int               reasonCode,
                                     String            reason)
    {
        if( currentRegistrationState.equals(newState) )
        {
            return;
        }

        RegistrationState oldState = currentRegistrationState;
        this.currentRegistrationState = newState;

        sipProvider.fireRegistrationStateChanged(
            oldState, newState, reasonCode, reason);
    }

    /**
     * Returns the default jain-sip provider for our parent provider.
     *
     * @return the default jain-sip provider for our parent provider.
     */
    @Override
    public SipProvider getJainSipProvider()
    {
        return sipProvider.getJainSipProvider(getTransport());
    }

    /**
     * Returns the default transport for our parent provider.
     *
     * @return the default transport for our parent provider.
     */
    @Override
    public String getTransport()
    {
        return defaultTransport;
    }


    /**
     * Returns a string representation of this connection instance
     * instance including information that would permit to distinguish it among
     * other sip listeners when reading a log file.
     * <p>
     * @return  a string representation of this operation set.
     */
    @Override
    public String toString()
    {
        String className = getClass().getName();
        try
        {
            className = className.substring(className.lastIndexOf('.') + 1);
        }
        catch (Exception ex)
        {
            // we don't want to fail in this method because we've messed up
            //something with indexes, so just ignore.
        }
        return className + "-[dn=" + sipProvider.getOurDisplayName()
               +" addr="+sipProvider.getAccountID().getUserID() + "]";
    }

    /**
     * Returns true if this is a fake connection that is not actually using
     * a registrar. This method should be overridden in
     * <tt>SipRegistrarlessConnection</tt> and return <tt>true</tt> in there.
     *
     * @return true if this connection is really using a registrar and
     * false if it is a fake connection that doesn't really use a registrar.
     */
    @Override
    public boolean isRegistrarless()
    {
        return true;
    }
}
