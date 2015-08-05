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

import net.java.sip.communicator.impl.protocol.sip.dtmf.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.protocol.*;

/**
 * Class responsible for sending a DTMF Tone using SIP INFO or using rfc4733.
 *
 * @author JM HEITZ
 * @author Vincent Lucas
 */
public class OperationSetDTMFSipImpl
    extends AbstractOperationSetDTMF
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(OperationSetDTMFSipImpl.class);

    /**
     * DTMF mode sending DTMF as sip info.
     */
    private final DTMFInfo dtmfModeInfo;

    /**
     * Constructor.
     *
     * @param pps the SIP Protocol provider service
     */
    public OperationSetDTMFSipImpl(ProtocolProviderServiceSipImpl pps)
    {
        super(pps);

        dtmfModeInfo = new DTMFInfo(pps);
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

        // If this account is configured to use SIP-INFO DTMF.
        if(this.dtmfMethod == DTMFMethod.SIP_INFO_DTMF)
        {
            dtmfModeInfo.startSendingDTMF(cp, tone);
        }
        // Else sends DTMF (dtmfMethod defined as RTP or INBAND) via the
        // AudioMediaStream interface.
        else
        {
            DTMFMethod cpDTMFMethod = dtmfMethod;

            // If "auto" DTMF mode selected, automatically selects RTP DTMF is
            // telephon-event is available. Otherwise selects INBAND DMTF.
            if(this.dtmfMethod == DTMFMethod.AUTO_DTMF)
            {
                if(isRFC4733Active(cp))
                {
                    cpDTMFMethod =  DTMFMethod.RTP_DTMF;
                }
                else
                {
                    cpDTMFMethod = DTMFMethod.INBAND_DTMF;
                }
            }

            // If the account is configured to use RTP DTMF method and the call
            // does not manage telephone events. Then, we log it for future
            // debugging.
            if(this.dtmfMethod == DTMFMethod.RTP_DTMF && !isRFC4733Active(cp))
            {
                logger.debug("RTP DTMF used without telephone-event capacity");
            }

            ((AudioMediaStream)cp.getMediaHandler().getStream(MediaType.AUDIO))
                .startSendingDTMF(
                        tone,
                        cpDTMFMethod,
                        minimalToneDuration,
                        maximalToneDuration,
                        volume);
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

        // If this account is configured to use SIP-INFO DTMF.
        if(this.dtmfMethod == DTMFMethod.SIP_INFO_DTMF)
        {
            dtmfModeInfo.stopSendingDTMF(cp);
        }
        // Else sends DTMF (dtmfMethod defined as RTP or INBAND) via the
        // AudioMediaStream interface.
        else
        {
            DTMFMethod cpDTMFMethod = dtmfMethod;

            // If "auto" DTMF mode selected, automatically selects RTP DTMF is
            // telephon-event is available. Otherwise selects INBAND DMTF.
            if(this.dtmfMethod == DTMFMethod.AUTO_DTMF)
            {
                if(isRFC4733Active(cp))
                {
                    cpDTMFMethod =  DTMFMethod.RTP_DTMF;
                }
                else
                {
                    cpDTMFMethod = DTMFMethod.INBAND_DTMF;
                }
            }

            // If the account is configured to use RTP DTMF method and the call
            // does not manage telephone events. Then, we log it for futur
            // debugging.
            if(this.dtmfMethod == DTMFMethod.RTP_DTMF && !isRFC4733Active(cp))
            {
                logger.debug("RTP DTMF used without telephon-event capacities");
            }

            ((AudioMediaStream)cp.getMediaHandler().getStream(MediaType.AUDIO))
                .stopSendingDTMF(cpDTMFMethod);
        }
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
