/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.authorization;

import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>AuthorizationHandlerImpl</tt> is an implementation of the
 * <tt>AuthorizationHandler</tt> interface, which is used by the protocol
 * provider in order to make the user act upon requests coming from contacts
 * that would like to add us to their contact list or simply track our presence
 * status, or whenever a subscription request has failed for a particular
 * contact because we need to first generate an authorization request demanding
 * permission to subscibe.
 * 
 * @author Yana Stamcheva
 */
public class AuthorizationHandlerImpl
    implements AuthorizationHandler {

    private MainFrame mainFrame;
    
    public AuthorizationHandlerImpl(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }
    
    /**
     * Implements the <tt>AuthorizationHandler.processAuthorisationRequest</tt>
     * method.
     * <p>
     * Called by the protocol provider whenever someone would like to add us to
     * their contact list.
     */
    public AuthorizationResponse processAuthorisationRequest(
            AuthorizationRequest req, Contact sourceContact) {
        AuthorizationResponse response = null;

        AuthorizationRequestedDialog dialog 
            = new AuthorizationRequestedDialog(mainFrame, sourceContact, req);

        int result = dialog.showDialog();

        if(result == AuthorizationRequestedDialog.ACCEPT_CODE)
        {
            response
                = new AuthorizationResponse(AuthorizationResponse.ACCEPT, null);

            // If the add contact option has been selected then open the
            // add contact window.
            if (dialog.isAddContact())
            {
                ContactListUtils.addContact(sourceContact.getProtocolProvider(),
                                            dialog.getSelectedMetaContactGroup(),
                                            sourceContact.getAddress());
            }
        }
        else if(result == AuthorizationRequestedDialog.REJECT_CODE)
        {
            response = new AuthorizationResponse(AuthorizationResponse.REJECT,
                    null);
        }
        else if(result == AuthorizationRequestedDialog.IGNORE_CODE)
        {
            response = new AuthorizationResponse(AuthorizationResponse.IGNORE,
                    null);
        }
        return response;
    }

    /**
     * Implements the <tt>AuthorizationHandler.createAuthorizationRequest</tt>
     * method.
     * <p>
     * The method is called when the user has tried to add a contact to the
     * contact list and this contact requires authorization.
     */
    public AuthorizationRequest createAuthorizationRequest(Contact contact) {
        
        AuthorizationRequest request = new AuthorizationRequest();
        
        RequestAuthorizationDialog dialog 
            = new RequestAuthorizationDialog(mainFrame, contact, request);

        int returnCode = dialog.showDialog();
        
        if(returnCode == RequestAuthorizationDialog.OK_RETURN_CODE) {
            request.setReason(dialog.getRequestReason());
        }
        else {
            request = null;
        }
        return request;
    }

    /**
     * Implements the <tt>AuthorizationHandler.processAuthorizationResponse</tt>
     * method.
     * <p>
     * The method will be called any whenever someone acts upone an authorization
     * request that we have previously sent.
     */
    public void processAuthorizationResponse(AuthorizationResponse response,
            Contact sourceContact)
    {
        AuthorizationResponseDialog dialog 
            = new AuthorizationResponseDialog(mainFrame, sourceContact, response);
        
        dialog.setVisible(true);
    }
}
