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

import java.util.*;

import javax.sip.*;
import javax.sip.header.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Keeps a list of all calls currently active and maintained by this protocol
 * povider. Offers methods for finding a call by its ID, peer dialog
 * and others.
 *
 * @author Emil Ivov
 */
public class ActiveCallsRepositorySipImpl
    extends ActiveCallsRepository<CallSipImpl,
                                  OperationSetBasicTelephonySipImpl>
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(ActiveCallsRepositorySipImpl.class);

    /**
     * Creates a new instance of this repository.
     *
     * @param opSet a reference to the
     * <tt>OperationSetBasicTelephonySipImpl</tt> that craeted us.
     */
    public ActiveCallsRepositorySipImpl(OperationSetBasicTelephonySipImpl opSet)
    {
        super(opSet);
    }

    /**
     * Returns the call that contains the specified dialog (i.e. it is
     * established  between us and one of the other call peers).
     * <p>
     * @param dialog the jain sip <tt>Dialog</tt> whose containing call we're
     * looking for.
     * @return the <tt>CallSipImpl</tt> containing <tt>dialog</tt> or null
     * if no call contains the specified dialog.
     */
    public CallSipImpl findCall(Dialog dialog)
    {
        Iterator<CallSipImpl> activeCalls = getActiveCalls();

        if(dialog == null)
        {
            if (logger.isDebugEnabled())
                logger.debug("Cannot find a peer with a null dialog. "
                        + "Returning null");
            return null;
        }

        if(logger.isTraceEnabled())
        {
            logger.trace("Looking for peer with dialog: " + dialog
                        + " among " + getActiveCallCount() + " calls");
        }


        while(activeCalls.hasNext())
        {
            CallSipImpl call = activeCalls.next();
            if(call.contains(dialog))
                return call;
        }

        return null;
    }

    /**
     * Returns the call peer whose associated jain sip dialog matches
     * <tt>dialog</tt>.
     *
     * @param dialog the jain sip dialog whose corresponding peer we're
     * looking for.
     * @return the call peer whose jain sip dialog is the same as the
     * specified or null if no such call peer was found.
     */
    public CallPeerSipImpl findCallPeer(Dialog dialog)
    {
        if(dialog == null)
        {
            if (logger.isDebugEnabled())
                logger.debug("Cannot find a peer with a null dialog. "
                        + "Returning null");
            return null;
        }

        if(logger.isTraceEnabled())
        {
            logger.trace("Looking for peer with dialog: " + dialog
                        + " among " + getActiveCallCount() + " calls");
        }

        for (Iterator<CallSipImpl> activeCalls = getActiveCalls();
                activeCalls.hasNext();)
        {
            CallSipImpl call = activeCalls.next();
            CallPeerSipImpl callPeer
                = call.findCallPeer(dialog);
            if(callPeer != null)
            {
                if (logger.isTraceEnabled())
                    logger.trace("Returning peer " + callPeer);
                return callPeer;
            }
        }

        return null;
    }

    /**
     * Returns the <tt>CallPeerSipImpl</tt> instance with a <tt>Dialog</tt>
     * matching CallID, local and remote tags.
     *
     * @param callID the <tt>Call-ID</tt> of the dialog we are looking for.
     * @param localTag the local tag of the dialog we are looking for.
     * @param remoteTag the remote tag of the dialog we are looking for.
     *
     * @return the <tt>CallPeerSipImpl</tt> matching specified dialog ID or
     * <tt>null</tt> if no such peer is known to this repository.
     */
    public CallPeerSipImpl findCallPeer(String callID,
            String localTag, String remoteTag)
    {
        if (logger.isTraceEnabled())
        {
            logger.trace("Looking for call peer with callID " + callID
                + ", localTag " + localTag + ", and remoteTag " + remoteTag
                + " among " + getActiveCallCount() + " calls.");
        }

        for (Iterator<CallSipImpl> activeCalls = getActiveCalls();
                activeCalls.hasNext();)
        {
            CallSipImpl call = activeCalls.next();

            for (Iterator<? extends CallPeer> callPeerIter
                            = call.getCallPeers();
                    callPeerIter.hasNext();)
            {
                CallPeerSipImpl callPeer =
                    (CallPeerSipImpl) callPeerIter.next();
                Dialog dialog = callPeer.getDialog();

                if (dialog != null)
                {
                    if (!callID.equals(dialog.getCallId().getCallId()))
                        continue;

                    String dialogLocalTag = dialog.getLocalTag();

                    if (((localTag == null) || "0".equals(localTag))
                            ? ((dialogLocalTag == null)
                                            || "0".equals(dialogLocalTag))
                            : localTag.equals(dialogLocalTag))
                    {
                        String dialogRemoteTag = dialog.getRemoteTag();

                        if (((remoteTag == null) || "0".equals(remoteTag))
                                ? ((dialogRemoteTag == null)
                                || "0".equals(dialogRemoteTag))
                                : remoteTag.equals(dialogRemoteTag))
                        {
                            return callPeer;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the <tt>CallPeerSipImpl</tt> whose INVITE transaction has the
     * specified <tt>branchID</tt> and whose corresponding INVITE request
     * contains the specified <tt>callID</tt>.
     *
     * @param callID the <tt>Call-ID</tt> of the dialog we are looking for.
     * @param branchID a <tt>String</tt> corresponding to the branch id of the
     * latest INVITE transaction that was associated with the peer we are
     * looking for.
     *
     * @return the <tt>CallPeerSipImpl</tt> matching specified call and branch
     * id-s or <tt>null</tt> if no such peer is known to this repository.
     */
    public CallPeerSipImpl findCallPeer(String branchID, String callID)
    {
        Iterator<CallSipImpl> activeCallsIter = getActiveCalls();

        while (activeCallsIter.hasNext())
        {
            CallSipImpl activeCall = activeCallsIter.next();
            Iterator<CallPeerSipImpl> callPeersIter = activeCall.getCallPeers();

            while (callPeersIter.hasNext())
            {
                CallPeerSipImpl cp = callPeersIter.next();
                Dialog cpDialog = cp.getDialog();
                Transaction cpTran = cp.getLatestInviteTransaction();

                if( cpDialog == null
                    || cpDialog.getCallId() == null
                    || cpTran == null)
                    continue;

                if ( cp.getLatestInviteTransaction() != null
                      && cpDialog.getCallId().getCallId().equals(callID)
                      && branchID.equals(cpTran.getBranchId()))
                {
                    return cp;
                }
            }
        }

        return null;
    }

    /**
     * Returns the <tt>CallPeerSipImpl</tt> whose INVITE transaction has the
     * specified <tt>branchID</tt> and whose corresponding INVITE request
     * contains the specified <tt>callID</tt>.
     *
     * @param cidHeader the <tt>Call-ID</tt> of the dialog we are looking for.
     * @param branchID a <tt>String</tt> corresponding to the branch id of the
     * latest INVITE transaction that was associated with the peer we are
     * looking for.
     *
     * @return the <tt>CallPeerSipImpl</tt> matching specified call and branch
     * id-s or <tt>null</tt> if no such peer is known to this repository.
     */
    public CallPeerSipImpl findCallPeer(String branchID, Header cidHeader)
    {
        if(cidHeader == null || ! (cidHeader instanceof CallIdHeader))
            return null;

        return findCallPeer(branchID, (((CallIdHeader)cidHeader).getCallId()));
    }



    /**
     * Returns the <tt>CallSipImpl</tt> instance with a <tt>Dialog</tt>
     * matching the specified <tt>Call-ID</tt>, local and remote tags.
     *
     * @param callID the <tt>Call-ID</tt> of the dialog we are looking for.
     * @param localTag the local tag of the dialog we are looking for.
     * @param remoteTag the remote tag of the dialog we are looking for.
     *
     * @return the <tt>CallSipImpl</tt> responsible for handling the
     * <tt>Dialog</tt> with the matching ID or <tt>null</tt> if no such call was
     * found.
     */
    public CallSipImpl findCall(String callID,
                                String localTag,
                                String remoteTag)
    {
        CallPeerSipImpl peer = findCallPeer(callID, localTag, remoteTag);

        return (peer == null)? null : peer.getCall();
    }

    /**
     * Creates and dispatches a <tt>CallEvent</tt> notifying registered
     * listeners that an event with id <tt>eventID</tt> has occurred on
     * <tt>sourceCall</tt>.
     *
     * @param eventID the ID of the event to dispatch
     * @param sourceCall the call on which the event has occurred
     * @param cause the <tt>CallChangeEvent</tt>, if any, which is the cause
     * that necessitated a new <tt>CallEvent</tt> to be fired
     * @see ActiveCallsRepository#fireCallEvent(int, Call, CallChangeEvent)
     */
    @Override
    protected void fireCallEvent(
            int eventID,
            Call sourceCall,
            CallChangeEvent cause)
    {
        parentOperationSet.fireCallEvent(eventID, sourceCall);
    }
}
