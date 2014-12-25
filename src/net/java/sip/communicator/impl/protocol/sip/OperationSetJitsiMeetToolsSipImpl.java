/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.packet.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * The SIP implementation of {@link OperationSetJitsiMeetTools}.
 *
 * @author Pawel Domas
 */
public class OperationSetJitsiMeetToolsSipImpl
    implements OperationSetJitsiMeetTools
{
    /**
     * The logger used by this class.
     */
    private final static Logger logger
        = Logger.getLogger(OperationSetJitsiMeetToolsSipImpl.class);

    /**
     * The list of {@link JitsiMeetRequestListener}.
     */
    private final List<JitsiMeetRequestListener> requestHandlers
        = new CopyOnWriteArrayList<JitsiMeetRequestListener>();

    /*private ProtocolProviderServiceSipImpl parentProvider;

    public OperationSetJitsiMeetToolsSipImpl(
        ProtocolProviderServiceSipImpl parentProvider)
    {
        this.parentProvider = parentProvider;
    }*/

    //@Override
    //public Call createGatewayCall(String uri, String roomName)
    //{
        /*OperationSetBasicTelephonySipImpl sipTelephony
            = (OperationSetBasicTelephonySipImpl)
                    parentProvider.getOperationSet(
                            OperationSetBasicTelephony.class);

        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put(CallSipImpl.JITSI_MEET_ROOM_HEADER, roomName);

        return sipTelephony.createCall(uri, parameters);*/
    //}

    /**
     * {@inheritDoc}
     */
    @Override
    public void addRequestListener(JitsiMeetRequestListener requestHandler)
    {
        this.requestHandlers.add(requestHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeRequestListener(JitsiMeetRequestListener requestHandler)
    {
        this.requestHandlers.remove(requestHandler);
    }

    /**
     * Notifies all registered {@link JitsiMeetRequestListener} about incoming
     * call that contains name of the MUC room which is hosting Jitsi Meet
     * conference.
     * @param call the incoming {@link Call} instance.
     * @param jitsiMeetRoom the name of the chat room of Jitsi Meet conference
     *                      to be joined.
     */
    public void notifyJoinJitsiMeetRoom(Call call, String jitsiMeetRoom)
    {
        boolean handled = false;
        for (JitsiMeetRequestListener l : requestHandlers)
        {
            l.onJoinJitsiMeetRequest(call, jitsiMeetRoom);
            handled = true;
        }
        if (!handled)
        {
            logger.warn(
                "Unhandled join Jitsi Meet request R:" + jitsiMeetRoom
                    + " C: " + call);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addSupportedFeature(String featureName)
    {
        throw new RuntimeException("Not implemented for SIP");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendPresenceExtension(ChatRoom chatRoom,
                                      PacketExtension extension)
    {
        throw new RuntimeException("Not implemented for SIP");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPresenceStatus(ChatRoom chatRoom, String statusMessage)
    {
        throw new RuntimeException("Not implemented for SIP");
    }
}
