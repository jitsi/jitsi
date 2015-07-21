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
package net.java.sip.communicator.impl.gui.main.authorization;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.protocol.*;

import javax.swing.*;

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
 * @author Damian Minkov
 * @author Hristo Terezov
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
            AuthorizationRequest req, Contact sourceContact)
    {
        AuthorizationResponse response = null;
        AuthorizationRequestedDialog dialog = null;
        if(!SwingUtilities.isEventDispatchThread())
        {
            ProcessAuthorizationRequestRunnable runnable
                = new ProcessAuthorizationRequestRunnable(
                sourceContact, req);
            try
            {
                SwingUtilities.invokeAndWait(runnable);
            }
            catch(Throwable t)
            {
                // if we cannot init in event dispatch thread
                // execute on current thread
                if(dialog == null)
                {
                    runnable.run();
                }
            }

            dialog = runnable.getDialog();
        }
        else
        {
            dialog = createAndShowAuthorizationRequestDialog(
                sourceContact, req);
        }

        int result = dialog.getReturnCode();

        if(result == AuthorizationRequestedDialog.ACCEPT_CODE)
        {
            response
                = new AuthorizationResponse(AuthorizationResponse.ACCEPT, null);

            // If the add contact option has been selected then open the
            // add contact window.
            if (dialog.isAddContact())
            {
                if(!sourceContact.getAddress().equals(
                    sourceContact.getDisplayName()))
                    addRenameListener(sourceContact.getProtocolProvider(),
                        sourceContact.getAddress(),
                        sourceContact.getDisplayName());
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
     * Creates and shows the dialog.
     * @param contact the contact to pass to dialog.
     * @param request the request
     * @return the dialog.
     */
    private AuthorizationRequestedDialog
        createAndShowAuthorizationRequestDialog(Contact contact,
                                                AuthorizationRequest request)
    {
        AuthorizationRequestedDialog dialog = new AuthorizationRequestedDialog(
                        mainFrame, contact, request);
        dialog.showDialog();

        return dialog;
    }

    /**
     * Implements the <tt>AuthorizationHandler.createAuthorizationRequest</tt>
     * method.
     * <p>
     * The method is called when the user has tried to add a contact to the
     * contact list and this contact requires authorization.
     */
    public AuthorizationRequest createAuthorizationRequest(
        final Contact contact)
    {
        AuthorizationRequest request = new AuthorizationRequest();

        RequestAuthorizationDialog dialog = null;

        if(!SwingUtilities.isEventDispatchThread())
        {
            RequestAuthorizationRunnable runnable =
                new RequestAuthorizationRunnable(contact, request);
            try
            {
                SwingUtilities.invokeAndWait(runnable);
            }
            catch(Throwable t)
            {
                // if we cannot init in event dispatch thread
                if(dialog == null)
                {
                    runnable.run();
                }
            }

            dialog = runnable.getDialog();
        }
        else
        {
            dialog = createAndShowRequestAuthorizationDialog(contact, request);
        }

        int returnCode = dialog.getReturnCode();

        if(returnCode == RequestAuthorizationDialog.OK_RETURN_CODE) {
            request.setReason(dialog.getRequestReason());
        }
        else {
            request = null;
        }
        return request;
    }

    /**
     * Creates and shows the dialog.
     * @param contact the contact to pass to dialog.
     * @param request the request
     * @return the dialog.
     */
    private RequestAuthorizationDialog
        createAndShowRequestAuthorizationDialog(Contact contact,
                                                AuthorizationRequest request)
    {
        RequestAuthorizationDialog dialog = new RequestAuthorizationDialog(
                        mainFrame, contact, request);
        dialog.showDialog();

        return dialog;
    }

    /**
     * Implements the <tt>AuthorizationHandler.processAuthorizationResponse</tt>
     * method.
     * <p>
     * The method will be called any whenever someone acts upone an authorization
     * request that we have previously sent.
     */
    public void processAuthorizationResponse(final AuthorizationResponse response,
            final Contact sourceContact)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    processAuthorizationResponse(response, sourceContact);
                }
            });
            return;
        }

        AuthorizationResponseDialog dialog
            = new AuthorizationResponseDialog(mainFrame, sourceContact, response);

        dialog.setVisible(true);
    }

    /**
     * Creates and shows the dialog in new thread.
     * Keeps a reference to the dialog.
     */
    private class RequestAuthorizationRunnable
        implements Runnable
    {
        /**
         * The contact to use.
         */
        private final Contact contact;

        /**
         * The request to use.
         */
        private final AuthorizationRequest request;

        /**
         * The created and shown dialog.
         */
        private RequestAuthorizationDialog dialog;

        /**
         * Constructs.
         * @param contact
         * @param request
         */
        private RequestAuthorizationRunnable(
            Contact contact, AuthorizationRequest request)
        {
            this.contact = contact;
            this.request = request;
        }

        public void run()
        {
            dialog = createAndShowRequestAuthorizationDialog(contact, request);
        }

        /**
         * The dialog.
         * @return
         */
        public RequestAuthorizationDialog getDialog()
        {
            return dialog;
        }
    }

    /**
     * Creates and shows the dialog in new thread.
     * Keeps a reference to the dialog.
     */
    private class ProcessAuthorizationRequestRunnable
        implements Runnable
    {
        /**
         * The contact to use.
         */
        private final Contact contact;

        /**
         * The request to use.
         */
        private final AuthorizationRequest request;

        /**
         * The created and shown dialog.
         */
        private AuthorizationRequestedDialog dialog;

        /**
         * Constructs.
         * @param contact
         * @param request
         */
        private ProcessAuthorizationRequestRunnable(
            Contact contact, AuthorizationRequest request)
        {
            this.contact = contact;
            this.request = request;
        }

        public void run()
        {
            dialog = createAndShowAuthorizationRequestDialog(contact, request);
        }

        /**
         * Returns the dialog reference.
         * @return
         */
        public AuthorizationRequestedDialog getDialog()
        {
            return dialog;
        }
    }

    /**
     * Adds a rename listener.
     *
     * @param protocolProvider the protocol provider to which the contact was
     * added
     * @param contactAddress the address of the newly added contact
     * @param displayName the new display name
     */
    private void addRenameListener(
                                final ProtocolProviderService protocolProvider,
                                final String contactAddress,
                                final String displayName)
    {
        GuiActivator.getContactListService().addMetaContactListListener(
            new MetaContactListAdapter()
            {
                @Override
                public void metaContactAdded(final MetaContactEvent evt)
                {
                    if (evt.getSourceMetaContact().getContact(
                            contactAddress, protocolProvider) != null
                        && contactAddress.equals(
                            evt.getSourceMetaContact().getDisplayName()))
                    {
                        new Thread()
                        {
                            @Override
                            public void run()
                            {
                                GuiActivator.getContactListService()
                                    .renameMetaContact(
                                        evt.getSourceMetaContact(), displayName);
                            }
                        }.start();
                    }
                }
            });
    }

}
