/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.authorization;

import javax.swing.JDialog;

import net.java.sip.communicator.service.protocol.AuthorizationHandler;
import net.java.sip.communicator.service.protocol.AuthorizationRequest;
import net.java.sip.communicator.service.protocol.AuthorizationResponse;
import net.java.sip.communicator.service.protocol.Contact;

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
public class AuthorizationHandlerImpl extends JDialog 
    implements AuthorizationHandler {

    public AuthorizationHandlerImpl() {
        
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
            = new AuthorizationRequestedDialog(sourceContact, req);
        
        int result = dialog.showDialog();
        
        if(result == AuthorizationRequestedDialog.ACCEPT_CODE) {
            response = new AuthorizationResponse(AuthorizationResponse.ACCEPT,
                    dialog.getResponseReason());
        }
        else if(result == AuthorizationRequestedDialog.REJECT_CODE) {
            response = new AuthorizationResponse(AuthorizationResponse.REJECT,
                    dialog.getResponseReason());
        }
        else if(result == AuthorizationRequestedDialog.IGNORE_CODE) {
            response = new AuthorizationResponse(AuthorizationResponse.IGNORE,
                    dialog.getResponseReason());
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
            = new RequestAuthorizationDialog(contact, request);
        
        dialog.setVisible(true);
        
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
            Contact sourceContact) {
               
        AuthorizationResponseDialog dialog 
            = new AuthorizationResponseDialog(sourceContact, response);
        
        dialog.setVisible(true);
    }

}
