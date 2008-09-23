/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.net.*;
import java.text.*;
import java.util.*;
import java.util.logging.Level;
import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

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
    private static final Logger logger =
        Logger.getLogger(SipRegistrarlessConnection.class);

    /**
     * A reference to the sip provider that created us.
     */
    private ProtocolProviderServiceSipImpl sipProvider = null;

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
     *
     */
    public SipRegistrarlessConnection(
                    ProtocolProviderServiceSipImpl sipProviderCallback)
    {
        this.sipProvider = sipProviderCallback;
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
     * Returns the address of this connection's registrar.
     *
     * @return the InetAddress of our registrar server.
     */
    @Override
    public InetAddress getRegistrarAddress()
    {
        try
        {
            return InetAddress.getByAddress("2001:1890:1112:1::20",
                new byte[]{(byte) 20, (byte) 01, (byte) 18, (byte) 90,
                           (byte) 11, (byte) 11, (byte) 12, (byte) 00,
                           (byte) 01, (byte) 00, (byte) 00, (byte) 00,
                           (byte) 00, (byte) 00, (byte) 00, (byte) 20});
        } catch (UnknownHostException ex)
        {
            logger.error("Failed to generate a dummy registrar addr", ex);
            return null;
        }
    }

    /**
     * Returns the listening point that should be used for communication with our
     * current registrar.
     *
     * @return the listening point that should be used for communication with our
     * current registrar.
     */
    @Override
    public ListeningPoint getListeningPoint()
    {
        return sipProvider.getDefaultListeningPoint();
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
               +" addr="+sipProvider.getOurSipAddress() + "]";
    }
}
