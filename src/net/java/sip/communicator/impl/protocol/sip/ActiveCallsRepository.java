/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import net.java.sip.communicator.util.*;
import java.util.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.*;
import javax.sip.*;

/**
 * Keeps a list of all calls currently active and maintained by this protocol
 * povider. Offers methods for finding a call by its ID, participant dialog
 * and others.
 *
 * @author Emil Ivov
 */
public class ActiveCallsRepository
    implements CallChangeListener
{
    private static final Logger logger
        = Logger.getLogger(ActiveCallsRepository.class);

    /**
     * The operation set that created us. Instance is mainly used for firing
     * events when necessary.
     */
    private OperationSetBasicTelephonySipImpl parentOperationSet = null;

    /**
     * A table mapping call ids against call instances.
     */
    private Hashtable activeCalls = new Hashtable();

    public ActiveCallsRepository(OperationSetBasicTelephonySipImpl opSet)
    {
        this.parentOperationSet = opSet;
    }

    /**
     * Adds the specified call to the list of calls tracked by this repository.
     * @param call CallSipImpl
     */
    public void addCall(CallSipImpl call)
    {
        activeCalls.put(call.getCallID(), call);
        call.addCallChangeListener(this);
    }

    /**
     * A dummy implementation of the CallChangeListener method that we don't
     * use.
     * @param evt unused.
     */
    public void callParticipantAdded(CallParticipantEvent evt)
    {}

    /**
     * A dummy implementation of the CallChangeListener method that we don't
     * use.
     * @param evt unused.
     */
    public void callParticipantRemoved(CallParticipantEvent evt)
    {}

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
            CallSipImpl sourceCall = (CallSipImpl)this.activeCalls
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
     * Returns the call that contains the specified dialog (i.e. it is
     * established  between us and one of the other call participants).
     * <p>
     * @param dialog the jain sip <tt>Dialog</tt> whose containing call we're
     * looking for.
     * @return the <tt>CallSipImpl</tt> containing <tt>dialog</tt> or null
     * if no call contains the specified dialog.
     */
    public CallSipImpl findCall(Dialog dialog)
    {
        Iterator activeCalls = getActiveCalls();

        if(dialog == null)
        {
            logger.debug("Cannot find a participant with a null dialog. "
                         +"Returning null");
            return null;
        }

        if(logger.isTraceEnabled())
        {
            logger.trace("Looking for participant with dialog: " + dialog
                         + " among " + this.activeCalls.size() + " calls");
        }


        while(activeCalls.hasNext())
        {
            CallSipImpl call = (CallSipImpl)activeCalls.next();
            if(call.contains(dialog))
                return call;
        }

        return null;
    }

    /**
     * Returns the call participant whose associated jain sip dialog matches
     * <tt>dialog</tt>.
     *
     * @param dialog the jain sip dialog whose corresponding participant we're
     * looking for.
     * @return the call participant whose jain sip dialog is the same as the
     * specified or null if no such call participant was found.
     */
    public CallParticipantSipImpl findCallParticipant(Dialog dialog)
    {
        Iterator activeCalls = getActiveCalls();

        if(dialog == null)
        {
            logger.debug("Cannot find a participant with a null dialog. "
                         +"Returning null");
            return null;
        }

        if(logger.isTraceEnabled())
        {
            logger.trace("Looking for participant with dialog: " + dialog
                         + " among " + this.activeCalls.size() + " calls");
        }

        while(activeCalls.hasNext())
        {
            CallSipImpl call = (CallSipImpl)activeCalls.next();
            CallParticipantSipImpl callParticipant
                = call.findCallParticipant(dialog);
            if(callParticipant != null)
            {
                logger.trace("Returning participant " + callParticipant);
                return callParticipant;
            }
        }

        return null;
    }
}
