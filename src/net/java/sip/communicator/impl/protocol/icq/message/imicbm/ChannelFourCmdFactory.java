package net.java.sip.communicator.impl.protocol.icq.message.imicbm;

import java.util.*;

import net.kano.joscar.*;
import net.kano.joscar.flapcmd.*;
import net.kano.joscar.snac.*;
import net.kano.joscar.snaccmd.*;
import net.kano.joscar.snaccmd.icbm.*;

/**
 * Extending the normal messages factory as its not handling the channel 4
 * for the messages.
 *
 * channel 1 -  plain-text messages
 * channel 2 -  rtf messages, rendezvous
 * channel 4 -  typed old-style messages ,
 *              which holds very different data in it like
 *                  Authorization denied message,
 *                  Authorization given message,
 *                  File request / file ok message,
 *                  URL message ...
 *
 * @author Damian Minkov
 */
public class ChannelFourCmdFactory
    extends ClientIcbmCmdFactory
{
    protected static List SUPPORTED_TYPES = null;

    public static final int CHANNEL = 0x0004;

    private Hashtable commandHandlers = new Hashtable();

    public ChannelFourCmdFactory()
    {
        List types = super.getSupportedTypes();
        ArrayList tempTypes = new ArrayList(types);
        tempTypes.add(new CmdType(4, 7));

        this.SUPPORTED_TYPES = DefensiveTools.getUnmodifiable(tempTypes);
    }

    /**
     * Attempts to convert the given SNAC packet to a
     * <code>SnacCommand</code>.
     *
     * @param packet the packet to use for generation of a
     *   <code>SnacCommand</code>
     * @return an appropriate <code>SnacCommand</code> for representing the
     *   given <code>SnacPacket</code>, or <code>null</code> if no such
     *   object can be created
     */
    public SnacCommand genSnacCommand(SnacPacket packet)
    {
        if (AbstractIcbm.getIcbmChannel(packet) == CHANNEL)
        {
            IcbmChannelFourCommand messageCommand = new IcbmChannelFourCommand(packet);

            int messageType = messageCommand.getMessageType();

            ChFourPacketHandler handler =
                (ChFourPacketHandler)commandHandlers.get(new Integer(messageType));

            if(handler != null)
                return handler.handle(messageCommand);

            return messageCommand;
        }

        return super.genSnacCommand(packet);
    }

    /**
     * Returns a list of the SNAC command types this factory can possibly
     * convert to <code>SnacCommand</code>s.
     *
     * @return a list of command types that can be passed to
     *   <code>genSnacCommand</code>
     */
    public List getSupportedTypes()
    {
        return SUPPORTED_TYPES;
    }

    public void addCommandHandler(int commmand, ChFourPacketHandler handler)
    {
        commandHandlers.put(new Integer(commmand), handler);
    }
}
