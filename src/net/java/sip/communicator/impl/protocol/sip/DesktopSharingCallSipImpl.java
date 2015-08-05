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
import javax.sip.header.*;
import javax.sip.message.*;
import java.text.*;
import java.util.*;

/**
 * When enabled <tt>DesktopSharingCallSipImpl</tt> is used to be able to handle
 * desktop sharing control messages out of dialog.
 * A new header is generated and inserted in the INVITE and if present in
 * the incoming INVITE we add that header to the OK.
 * The header help us match the incoming notify and subscription requests to the
 * call.
 * @author Damian Minkov
 */
public class DesktopSharingCallSipImpl
    extends CallSipImpl
{
    /**
     * The property used to control enabling/disabling desktop control
     * handling out of dialog.
     */
    public static final String ENABLE_OUTOFDIALOG_DESKTOP_CONTROL_PROP
        = "net.java.sip.communicator.impl.protocol.sip" +
            ".ENABLE_OUTOFDIALOG_DESKTOP_CONTROL_PROP";

    public static final String DSSID_HEADER = "DSSID";

    /**
     * The value of the received or generated header for matching notify
     * and subscribe requests to the call.
     */
    private String desktopSharingSessionID = null;

    /**
     * Crates a DesktopSharingCallSipImpl instance belonging to
     * <tt>sourceProvider</tt> and initiated by <tt>CallCreator</tt>.
     *
     * @param parentOpSet a reference to the operation set that's creating us
     *                    and that we would be able to use for even dispatching.
     */
    protected DesktopSharingCallSipImpl(
        OperationSetBasicTelephonySipImpl parentOpSet)
    {
        super(parentOpSet);
    }

    /**
     * The value of the received or generated header for matching notify
     * and subscribe requests to the call.
     * @return the value used in DSSID header.
     */
    public String getDesktopSharingSessionID()
    {
        return desktopSharingSessionID;
    }

    /**
     * Creates a new call and sends a RINGING response. Reuses super
     * implementation.
     *
     * @param jainSipProvider the provider containing
     * <tt>sourceTransaction</tt>.
     * @param serverTran the transaction containing the received request.
     *
     * @return CallPeerSipImpl the newly created call peer (the one that sent
     * the INVITE).
     */
    @Override
    public CallPeerSipImpl processInvite(SipProvider jainSipProvider,
                                         ServerTransaction serverTran)
    {
        Request invite = serverTran.getRequest();

        Header dssidHeader = invite.getHeader(DSSID_HEADER);
        if(dssidHeader != null)
        {
            String dssid = dssidHeader.toString().replaceAll(
                dssidHeader.getName() + ":", "").trim();
            desktopSharingSessionID = dssid;

            setData(DSSID_HEADER, desktopSharingSessionID);
        }

        return super.processInvite(jainSipProvider, serverTran);
    }

    /**
     * A place where we can handle any headers we need for requests
     * and responses.
     * @param message the SIP <tt>Message</tt> in which a header change
     * is to be reflected
     * @throws java.text.ParseException if modifying the specified SIP
     * <tt>Message</tt> to reflect the header change fails
     */
    @Override
    protected void processExtraHeaders(javax.sip.message.Message message)
        throws ParseException
    {
        if(message instanceof Request)
        {
            if(desktopSharingSessionID == null)
            {
                desktopSharingSessionID = UUID.randomUUID().toString();
                setData(DSSID_HEADER, desktopSharingSessionID);
            }

            Header dssidHeader = getProtocolProvider().getHeaderFactory()
                .createHeader(DSSID_HEADER, desktopSharingSessionID);
            message.setHeader(dssidHeader);
        }
        else if(message instanceof Response)
        {
            if(desktopSharingSessionID != null)
            {
                Header dssidHeader = getProtocolProvider().getHeaderFactory()
                    .createHeader(DSSID_HEADER, desktopSharingSessionID);
                message.setHeader(dssidHeader);
            }
        }
    }
}
