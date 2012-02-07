/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
     * The DTMF method used to send tones.
     */
    private String dtmfMethod = null;

    /**
     * DTMF mode sending DTMF as sip info.
     */
    private final DTMFInfo dtmfModeInfo;

    /**
     * DTMF sending rfc4733.
     */
    private final DTMF4733 dtmfModeRFC4733;

    /**
     * DTMF sending inband tones into the audio stream.
     */
    private final DTMFInband dtmfModeInband;

    /**
     * Constructor.
     *
     * @param pps the SIP Protocol provider service
     */
    public OperationSetDTMFSipImpl(ProtocolProviderServiceSipImpl pps)
    {
        AccountID accountID = pps.getAccountID();
        this.dtmfMethod = accountID.getAccountPropertyString("DTMF_METHOD");
        if(dtmfMethod == null)
        {
            accountID.putAccountProperty("DTMF_METHOD", "RFC4733 / SIP-INFO");
        }

        dtmfModeInfo = new DTMFInfo(pps);
        dtmfModeRFC4733 = new DTMF4733();
        dtmfModeInband = new DTMFInband();
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

        if(this.dtmfMethod == null ||
                this.dtmfMethod.equals("RFC4733 / SIP-INFO"))
        {
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
        else
        {
            dtmfModeInband.addInbandDTMF(
                    ((AudioMediaStream)cp.getMediaHandler()
                        .getStream(MediaType.AUDIO)),
                    tone);
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

        if(this.dtmfMethod == null ||
                this.dtmfMethod.equals("RFC4733 / SIP-INFO"))
        {
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

    /**
     * Returns DTMFInfo mode implementation.
     * @return DTMFInfo mode implementation.
     */
    DTMFInfo getDtmfModeInfo()
    {
        return dtmfModeInfo;
    }
}
