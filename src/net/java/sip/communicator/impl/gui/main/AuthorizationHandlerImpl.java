/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main;

import javax.swing.JDialog;

import net.java.sip.communicator.service.protocol.AuthorizationHandler;
import net.java.sip.communicator.service.protocol.AuthorizationRequest;
import net.java.sip.communicator.service.protocol.AuthorizationResponse;
import net.java.sip.communicator.service.protocol.Contact;

public class AuthorizationHandlerImpl extends JDialog 
    implements AuthorizationHandler {

    public AuthorizationHandlerImpl() {
        
    }
    
    public AuthorizationResponse processAuthorisationRequest(
            AuthorizationRequest req, Contact sourceContact) {
        // TODO Auto-generated method stub
        return null;
    }

    public AuthorizationRequest createAuthorizationRequest(Contact contact) {
        
        AuthorizationRequest request = new AuthorizationRequest();
        
        RequestAuthorisationDialog dialog 
            = new RequestAuthorisationDialog(contact, request);
        
        dialog.setVisible(true);
        
        return request;
    }

    public void processAuthorizationResponse(AuthorizationResponse response,
            Contact sourceContact) {
        // TODO Auto-generated method stub

    }

}
