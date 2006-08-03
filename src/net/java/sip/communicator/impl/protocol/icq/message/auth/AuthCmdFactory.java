/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq.message.auth;

import java.util.*;

import net.java.sip.communicator.impl.protocol.icq.*;
import net.java.sip.communicator.impl.protocol.icq.message.imicbm.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.kano.joscar.flapcmd.*;
import net.kano.joscar.snac.*;
import net.kano.joscar.snaccmd.ssi.*;
import net.kano.joustsim.oscar.*;
import net.kano.joustsim.oscar.oscar.service.ssi.*;

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
    implements SnacResponseListener, ChFourPacketHandler
{
    private static final Logger logger =
        Logger.getLogger(AuthCmdFactory.class);

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

        this.operationSetPresence =
            (OperationSetPersistentPresenceIcqImpl)
            icqProvider.getSupportedOperationSets().
            get(OperationSetPresence.class.getName());
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
                        operationSetPresence.findContactByID(uinToAskForAuth);

                    if(srcContact == null)
                        srcContact =
                            operationSetPresence.createUnresolvedContact(
                                uinToAskForAuth,
                                null,
                                getGroupByID(buddyItem.getParentId()));

                    AuthorizationRequest authRequest =
                        authorizationHandler.createAuthorizationRequest(
                        srcContact);

                    if (authRequest != null)
                    {
                        //SNAC(13,14)     send future authorization grant to client
//                        aimConnection.getSsiService().sendSnac(
//                            new AuthFutureCmd(
//                                uinToAskForAuth,
//                                authRequest.getReason()));

                        Vector buddiesToBeAdded = new Vector();

                        BuddyAwaitingAuth newBuddy = new BuddyAwaitingAuth(
                            buddyItem);
                        buddiesToBeAdded.add(newBuddy);

                        CreateItemsCmd addCMD = new CreateItemsCmd(buddiesToBeAdded);

                        logger.trace("Adding buddy as awaiting authorization");

                        aimConnection.getSsiService().sendSnac(addCMD);

                        //SNAC(13,18)     send authorization request
                        aimConnection.getSsiService().sendSnac(
                          new RequestAuthCmd(
                              uinToAskForAuth,
                              authRequest.getReason()));

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

    public SnacCommand handle(IcbmChannelFourCommand messageCommand)
    {
        int messageType = messageCommand.getMessageType();

        String uin = String.valueOf(messageCommand.getSender());
        Contact srcContact = operationSetPresence.findContactByID(uin);

        // Contact may be not in the contact list
        // as we added it as Volatile stopped the application
        // and after that received authorization response
        if(srcContact == null)
            srcContact = operationSetPresence.createVolatileContact(uin);

        if (messageType == IcbmChannelFourCommand.MTYPE_AUTHREQ)
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
                                       getUserID()),
                        authResponse.getReason(),
                        true));
            }
            else if (authResponse.getResponseCode() ==
                     AuthorizationResponse.REJECT)
            {
                aimConnection.getInfoService().sendSnac(
                    new AuthReplyCmd(
                        String.valueOf(icqProvider.getAccountID().
                                       getUserID()),
                        authResponse.getReason(),
                        false));
            }
            // all other is ignored
        }
        else
        if (messageType == IcbmChannelFourCommand.MTYPE_AUTHDENY)
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
        if (messageType == IcbmChannelFourCommand.MTYPE_AUTHOK)
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

        return messageCommand;
    }

    private ContactGroup getGroupByID(int id)
    {
        String groupName = SSIItemInfo.getGroupName(aimConnection, id);

        if(groupName == null) return null;

        return operationSetPresence.
            getServerStoredContactListRoot().getGroup(groupName);
    }
}
