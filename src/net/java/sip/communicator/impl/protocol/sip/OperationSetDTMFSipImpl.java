/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.protocol.sip.dtmf.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.DTMFTone;

/**
 * Class responsible for sending a DTMF Tone using SIP INFO or using rfc4733.
 *
 * @author JM HEITZ
 */
public class OperationSetDTMFSipImpl
    implements OperationSetDTMF
{
    /**
     * DTMF mode sending DTMF as sip info.
     */
    private final DTMFInfo dtmfModeInfo;

    /**
     * DTMF sending rfc4733.
     */
    private final DTMF4733 dtmfModeRFC4733;

    /**
     * Constructor.
     *
     * @param pps the SIP Protocol provider service
     */
    public OperationSetDTMFSipImpl(ProtocolProviderServiceSipImpl pps)
    {
        dtmfModeInfo = new DTMFInfo(pps);
        dtmfModeRFC4733 = new DTMF4733();
    }

    /**
     * Sends the <tt>DTMFTone</tt> <tt>tone</tt> to <tt>callPeer</tt>.
     *
     * @param callPeer the  call peer to send <tt>tone</tt> to.
     * @param tone the DTMF tone to send to <tt>callPeer</tt>.
     *
     * @throws OperationFailedException with code OPERATION_NOT_SUPPORTED if
     * DTMF tones are not supported for <tt>callPeer</tt>.
     *
     * @throws NullPointerException if one of the arguments is null.
     *
     * @throws IllegalArgumentException in case the call peer does not
     * belong to the underlying implementation.
     */
    public synchronized void startSendingDTMF(CallPeer callPeer, DTMFTone tone)
        throws OperationFailedException
    {
        if (callPeer == null || tone == null)
        {
            throw new NullPointerException("Argument is null");
        }
        if (! (callPeer instanceof CallPeerSipImpl))
        {
            throw new IllegalArgumentException();
        }

        CallPeerSipImpl cp = (CallPeerSipImpl) (callPeer);

        if(isRFC4733Active(cp))
        {
            dtmfModeRFC4733.startSendingDTMF(
                ((AudioMediaStream)cp.getMediaHandler()
                    .getStream(MediaType.AUDIO)),
                tone);
        }
        else
        {
            dtmfModeInfo.startSendingDTMF(cp, tone);
        }
    }

    /**
     * Stops sending DTMF.
     *
     * @param callPeer the call peer that we'd like to stop sending DTMF to.
     */
    public synchronized void stopSendingDTMF(CallPeer callPeer)
    {
        if (callPeer == null)
        {
            throw new NullPointerException("Argument is null");
        }
        if (! (callPeer instanceof CallPeerSipImpl))
        {
            throw new IllegalArgumentException();
        }

        CallPeerSipImpl cp = (CallPeerSipImpl) (callPeer);

        if(isRFC4733Active(cp))
        {
            dtmfModeRFC4733.stopSendingDTMF(
                ((AudioMediaStream)cp.getMediaHandler()
                    .getStream(MediaType.AUDIO)));
        }
        else
        {
            dtmfModeInfo.stopSendingDTMF(cp);
        }
    }

    /**
     * Checks whether rfc4733 is negotiated for this call.
     * @param peer the call peer.
     * @return whether we can use rfc4733 in this call.
     */
    private boolean isRFC4733Active(CallPeerSipImpl peer)
    {
        Iterator<MediaFormat> iter =
            peer.getMediaHandler().getStream(MediaType.AUDIO)
                .getDynamicRTPPayloadTypes().values().iterator();
        while (iter.hasNext())
        {
            MediaFormat mediaFormat = iter.next();
            if(mediaFormat.getEncoding().equals(Constants.TELEPHONE_EVENT))
                return true;
        }

        return false;
    }
}
