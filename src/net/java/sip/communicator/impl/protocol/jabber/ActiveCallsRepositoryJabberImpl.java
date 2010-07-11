/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Keeps a list of all calls currently active and maintained by this protocol
 * provider. Offers methods for finding a call by its ID, peer session
 * and others.
 *
 * @author Emil Ivov
 * @author Symphorien Wanko
 */
public class ActiveCallsRepositoryJabberImpl
    extends ActiveCallsRepository<CallJabberImpl,
                                  OperationSetBasicTelephonyJabberImpl>
{
    /**
     * logger of this class
     */
    private static final Logger logger
        = Logger.getLogger(ActiveCallsRepositoryJabberImpl.class);

    /**
     * It's where we store all active calls
     *
     * @param opSet the <tt>OperationSetBasicTelphony</tt> instance which has
     * been used to create calls in this repository
     */
    public ActiveCallsRepositoryJabberImpl(
                                    OperationSetBasicTelephonyJabberImpl opSet)
    {
        super(opSet);
    }

    /**
     * Returns the {@link CallJabberImpl} containing a {@link
     * CallPeerJabberImpl} whose corresponding jingle session has the specified
     * jingle <tt>sid</tt>.
     *
     * @param sid the jingle <tt>sid</tt> we're looking for.
     *
     * @return the {@link CallJabberImpl} containing the peer with the
     * specified <tt>sid</tt> or  tt>null</tt> if we couldn't find one matching
     * it.
     */
    public CallJabberImpl findJingleSID(String sid)
    {
        Iterator<CallJabberImpl> calls = getActiveCalls();

        while (calls.hasNext())
        {
            CallJabberImpl call = calls.next();
            if (call.containsJingleSID(sid))
                return call;
        }

        return null;
    }
}
