/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import org.jivesoftware.smackx.jingle.*;

/**
 * Keeps a list of all calls currently active and maintained by this protocol
 * provider. Offers methods for finding a call by its ID, participant session
 * and others.
 *
 * @author Emil Ivov
 * @author Symphorien Wanko
 */
public class ActiveCallsRepository
    extends CallChangeAdapter
{
    /**
     * logger of this class
     */
    private static final Logger logger
        = Logger.getLogger(ActiveCallsRepository.class);

    /**
     * The operation set that created us. Instance is mainly used for firing
     * events when necessary.
     */
    private OperationSetBasicTelephonyJabberImpl parentOperationSet = null;

    /**
     * A table mapping call ids against call instances.
     */
    private Hashtable activeCalls = new Hashtable();

    /**
     * It's where we store all active calls
     * @param opSet the <tt>OperationSetBasicTelphony</tt> instance which has 
     * been used to create calls in this repository
     */
    public ActiveCallsRepository(OperationSetBasicTelephonyJabberImpl opSet)
    {
        this.parentOperationSet = opSet;
    }

    /**
     * Adds the specified call to the list of calls tracked by this repository.
     * @param call CallJabberImpl
     */
    public void addCall(CallJabberImpl call)
    {
        activeCalls.put(call.getCallID(), call);
        call.addCallChangeListener(this);
    }

    /**
     * If <tt>evt</tt> indicates that the call has been ended we remove it from
     * the repository.
     * @param evt the <tt>CallChangeEvent</tt> instance containing the source
     * calls and its old and new state.
     */
    public void callStateChanged(CallChangeEvent evt)
    {
        if(evt.getEventType().equals(CallChangeEvent.CALL_STATE_CHANGE)
           && ((CallState)evt.getNewValue()).equals(CallState.CALL_ENDED))
        {
            CallJabberImpl sourceCall = (CallJabberImpl)this.activeCalls
                .remove(evt.getSourceCall().getCallID());

            logger.trace(  "Removing call " + sourceCall + " from the list of "
                         + "active calls because it entered an ENDED state");

            this.parentOperationSet.fireCallEvent(
                CallEvent.CALL_ENDED, sourceCall);
        }
    }

    /**
     * Returns an iterator over all currently active (non-ended) calls.
     *
     * @return an iterator over all currently active (non-ended) calls.
     */
    public Iterator getActiveCalls()
    {
        return new LinkedList(activeCalls.values()).iterator();
    }

    /**
     * Returns the call that contains the specified session (i.e. it is
     * established  between us and one of the other call participants).
     * <p>
     * @param session the <tt>jingleSession</tt> whose containing call we're
     * looking for.
     * @return the <tt>CallJabberImpl</tt> containing <tt>session</tt> or null
     * if no call contains the specified session.
     */
    public CallJabberImpl findCall(JingleSession session)
    {
        Iterator activeCalls = getActiveCalls();

        if(session == null)
        {
            logger.debug("Cannot find a participant with a null session. "
                         +"Returning null");
            return null;
        }

        if(logger.isTraceEnabled())
        {
            logger.trace("Looking for participant with session: " + session
                         + " among " + this.activeCalls.size() + " calls");
        }


        while(activeCalls.hasNext())
        {
            CallJabberImpl call = (CallJabberImpl)activeCalls.next();
            if(call.contains(session))
                return call;
        }

        return null;
    }

    /**
     * Returns the call participant whose associated jingle session matches
     * <tt>session</tt>.
     *
     * @param session the jingle session whose corresponding participant we're
     * looking for.
     * @return the call participant whose jingle session is the same as the
     * specified or null if no such call participant was found.
     */
    public CallPeerJabberImpl findCallPeer(JingleSession session)
    {
        Iterator activeCalls = getActiveCalls();

        if(session == null)
        {
            logger.debug("Cannot find a participant with a null session. "
                         + "Returning null");
            return null;
        }

        if(logger.isTraceEnabled())
        {
            logger.trace("Looking for participant with session: " + session
                         + " among " + this.activeCalls.size() + " calls");
        }

        while(activeCalls.hasNext())
        {
            CallJabberImpl call = (CallJabberImpl)activeCalls.next();
            CallPeerJabberImpl callParticipant
                = call.findCallPeer(session);
            if(callParticipant != null)
            {
                logger.trace("Returning participant " + callParticipant);
                return callParticipant;
            }
        }

        return null;
    }
}