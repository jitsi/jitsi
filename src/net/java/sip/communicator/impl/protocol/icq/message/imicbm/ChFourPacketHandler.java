package net.java.sip.communicator.impl.protocol.icq.message.imicbm;

import net.kano.joscar.flapcmd.*;

/**
 * @author Damian Minkov
 */
public interface ChFourPacketHandler
{
    SnacCommand handle(IcbmChannelFourCommand cmd);
}
