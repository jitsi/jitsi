/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq.message.auth;

import java.util.*;

import net.java.sip.communicator.impl.protocol.icq.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.kano.joscar.*;
import net.kano.joscar.flapcmd.*;
import net.kano.joscar.snac.*;
import net.kano.joscar.snaccmd.*;
import net.kano.joscar.snaccmd.icbm.*;
import net.kano.joscar.snaccmd.ssi.*;
import net.kano.joustsim.oscar.*;
import net.kano.joustsim.Screenname;

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
public class AuthCmdFactory
    extends ClientIcbmCmdFactory implements SnacResponseListener
{
    private static final Logger logger =
        Logger.getLogger(AuthCmdFactory.class);

    protected static List SUPPORTED_TYPES = null;

    public static final int CHANNEL_AUTH = 0x0004;

    private ProtocolProviderServiceIcqImpl icqProvider;
    private AuthorizationHandler authorizationHandler;
    private OperationSetPersistentPresenceIcqImpl operationSetPresence;
    private AimConnection aimConnection = null;

    public AuthCmdFactory(ProtocolProviderServiceIcqImpl icqProvider,
                          AimConnection aimConnection,
                          AuthorizationHandler authorizationHandler)
    {
        this.icqProvider = icqProvider;
        this.authorizationHandler = authorizationHandler;
        this.aimConnection = aimConnection;

        List types = super.getSupportedTypes();
        ArrayList tempTypes = new ArrayList(types);
        tempTypes.add(new CmdType(4, 7));

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
        if (AbstractIcbm.getIcbmChannel(packet) == CHANNEL_AUTH)
        {
            AuthOldMsgCmd messageCommand = new AuthOldMsgCmd(packet);

            int messageType = messageCommand.getMessageType();

            String uin = String.valueOf(messageCommand.getSender());
            Contact srcContact = operationSetPresence.findContactByID(uin);

            // Contact my be not in the contact list
            // as we added it as Volatile stopped the application
            // and after that received authorization response
            if(srcContact == null)
                srcContact = operationSetPresence.createVolatileContact(
                    new Screenname(uin));


            if (messageType == AuthOldMsgCmd.MTYPE_AUTHREQ)
            {
                // this is a authorisation request with or without reason
                AuthorizationRequest authRequest = new AuthorizationRequest();
                authRequest.setReason(messageCommand.getReason());

                AuthorizationResponse authResponse =
                    authorizationHandler.processAuthorisationRequest(
                        authRequest,
                        srcContact
                    );

                if (authResponse.getResponseCode() ==
                    AuthorizationResponse.ACCEPT)
                {
                    aimConnection.getInfoService().sendSnac(
                        new AuthReplyCmd(
                            String.valueOf(icqProvider.getAccountID().
                                           getAccountUserID()),
                            authResponse.getReason(),
                            true));
                }
                else if (authResponse.getResponseCode() ==
                         AuthorizationResponse.REJECT)
                {
                    aimConnection.getInfoService().sendSnac(
                        new AuthReplyCmd(
                            String.valueOf(icqProvider.getAccountID().
                                           getAccountUserID()),
                            authResponse.getReason(),
                            false));
                }
                // all other is ignored
            }
            else
            if (messageType == AuthOldMsgCmd.MTYPE_AUTHDENY)
            {
                // this is authorisation reply deny
                // with or without reason
                AuthorizationResponse authResponse =
                    new AuthorizationResponse(
                        AuthorizationResponse.REJECT,
                        messageCommand.getReason());

                authorizationHandler.processAuthorizationResponse(
                    authResponse,
                    srcContact);
            }
            else
            if (messageType == AuthOldMsgCmd.MTYPE_AUTHOK)
            {
                // this is authorization reply with accept
                // with reason == null
                AuthorizationResponse authResponse =
                    new AuthorizationResponse(
                        AuthorizationResponse.ACCEPT,
                        messageCommand.getReason());

                authorizationHandler.processAuthorizationResponse(
                    authResponse,
                    srcContact);
            }
            else
            if (messageType == AuthOldMsgCmd.MTYPE_ADDED)
            {
                /** Info that user has added us to their contact list */
                logger.trace("User (" + messageCommand.getSender() +
                             ") has added us to his contact list!");
            }

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

    /**
     * Listen for errors comming from the server
     * when somebody tries to add a buddy which requires
     * an authorization - error is the response from the server
     * So our job here is to triger the authorization process to begin.
     *
     * @param snacResponseEvent SnacResponseEvent incoming event
     */
    public void handleResponse(SnacResponseEvent snacResponseEvent)
    {
        if (snacResponseEvent.getSnacCommand() instanceof SsiDataModResponse)
        {
            SsiDataModResponse dataModResponse =
                (SsiDataModResponse) snacResponseEvent.getSnacCommand();

            int[] results = dataModResponse.getResults();
            List items = ( (ItemsCmd) snacResponseEvent.getRequest().getCommand()).
                getItems();
            items = new LinkedList(items);

            for (int i = 0; i < results.length; i++)
            {
                int result = results[i];
                if (result ==
                    SsiDataModResponse.RESULT_ICQ_AUTH_REQUIRED)
                {

                    // authorisation required for user
                    SsiItem buddyItem = (SsiItem) items.get(i);

                    String uinToAskForAuth = buddyItem.getName();

                    logger.trace("finding buddy : " + uinToAskForAuth);
                    Contact srcContact =
//                        new VolatileBuddy();
                        operationSetPresence.findContactByID(uinToAskForAuth);
                    AuthorizationRequest authRequest =
                        authorizationHandler.createAuthorizationRequest(
                        srcContact);

                    if (authRequest != null)
                    {
                        //SNAC(13,14)     send future authorization grant to client
                        aimConnection.getIcbmService().sendSnac(
                            new AuthFutureCmd(
                                uinToAskForAuth,
                                authRequest.getReason()));

                        //SNAC(13,18)     send authorization request
                        aimConnection.getIcbmService().sendSnac(
                            new RequestAuthCmd(
                                uinToAskForAuth,
                                authRequest.getReason()));

                        Vector buddiesToBeAdded = new Vector();

                        BuddyAwaitingAuth newBuddy = new BuddyAwaitingAuth(
                            buddyItem);
                        items.add(newBuddy);

                        CreateItemsCmd addCMD = new CreateItemsCmd(
                            buddiesToBeAdded);

                        logger.trace("Adding buddy as awaiting authorization");
                        aimConnection.getIcbmService().sendSnac(addCMD);

                        return;
                    }
                    else
                    {
                        logger.trace(
                            "AuthorizationRequest is NULL after calling " +
                            "-> createAuthorizationRequest");
                    }
                }
            }
        }

    }
}
