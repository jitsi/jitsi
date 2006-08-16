package net.java.sip.communicator.impl.protocol.icq.message.auth;

import java.util.*;

import net.java.sip.communicator.impl.protocol.icq.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.AuthorizationResponse.*;
import net.java.sip.communicator.util.*;
import net.kano.joscar.*;
import net.kano.joscar.flapcmd.*;
import net.kano.joscar.snac.*;
import net.kano.joscar.snaccmd.ssi.*;
import net.kano.joustsim.oscar.*;

/**
 * @author Damian Minkov
 */
public class SsiCmdFactory
    extends ServerSsiCmdFactory
{
    private static final Logger logger =
        Logger.getLogger(SsiCmdFactory.class);

    protected static List SUPPORTED_TYPES = null;

    private ProtocolProviderServiceIcqImpl icqProvider;
    private AuthorizationHandler authorizationHandler;
    private OperationSetPersistentPresenceIcqImpl operationSetPresence;
    private AimConnection aimConnection = null;

    public SsiCmdFactory(ProtocolProviderServiceIcqImpl icqProvider,
                          AimConnection aimConnection,
                          AuthorizationHandler authorizationHandler)
    {
        this.icqProvider = icqProvider;
        this.authorizationHandler = authorizationHandler;
        this.aimConnection = aimConnection;

        List types = super.getSupportedTypes();
        ArrayList tempTypes = new ArrayList(types);
        tempTypes.add(new CmdType(
            SsiCommand.FAMILY_SSI, AbstractAuthCommand.CMD_AUTH_REPLY_RECV)); // 1b auth request reply
        tempTypes.add(new CmdType(
            SsiCommand.FAMILY_SSI, AbstractAuthCommand.CMD_AUTH_REQUEST_RECV)); // 19 auth request
//        tempTypes.add(new CmdType(
//            SsiCommand.FAMILY_SSI, AbstractAuthCommand.CMD_YOU_WERE_ADDED_RECV)); // 1c you were added

        this.SUPPORTED_TYPES = DefensiveTools.getUnmodifiable(tempTypes);

        this.operationSetPresence =
            (OperationSetPersistentPresenceIcqImpl)
            icqProvider.getSupportedOperationSets().
            get(OperationSetPresence.class.getName());
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
        int command = packet.getCommand();

        if(command == AbstractAuthCommand.CMD_AUTH_REPLY_RECV)
        {
            logger.trace("Received Authorization Replay!");

            AuthReplyCmd cmd = new AuthReplyCmd(packet);

            Contact srcContact = getContact(cmd.getSender());

            AuthorizationResponseCode authResponseCode = null;

            if(cmd.isAccepted())
                authResponseCode = AuthorizationResponse.ACCEPT;
            else
                authResponseCode = AuthorizationResponse.REJECT;

            AuthorizationResponse authResponse =
                new AuthorizationResponse(authResponseCode, cmd.getReason());

//            try
//            {
//                // the contact must be subscribed again so the 0x66 -
//                // awaiting authorization is removed
//                if (cmd.isAccepted())
//                {
//                    operationSetPresence.subscribe(cmd.getSender());
//                }
//            }
//            catch (OperationFailedException ex)
//            {}

            authorizationHandler.processAuthorizationResponse(
                authResponse, srcContact);

            return cmd;
        }
        else
            if(command == AbstractAuthCommand.CMD_AUTH_REQUEST_RECV)
            {
                logger.trace("Received Authorization Request!");

                RequestAuthCmd cmd = new RequestAuthCmd(packet);

                AuthorizationRequest authRequest = new AuthorizationRequest();
                authRequest.setReason(cmd.getReason());

                Contact srcContact = getContact(cmd.getSender());

                AuthorizationResponse authResponse =
                    authorizationHandler.processAuthorisationRequest(
                        authRequest,srcContact);

                if (authResponse.getResponseCode() == AuthorizationResponse.IGNORE)
                {
                    return cmd;
                }

                aimConnection.getInfoService().sendSnac(
                    new AuthReplyCmd(
                        cmd.getSender(),
                        authResponse.getReason(),
                        authResponse.getResponseCode() == AuthorizationResponse.ACCEPT));

                return cmd;
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

    private Contact getContact(String uin)
    {
        Contact contact =
                operationSetPresence.findContactByID(uin);

        if(contact == null)
            contact = operationSetPresence.createVolatileContact(uin);

        return contact;
    }

}
