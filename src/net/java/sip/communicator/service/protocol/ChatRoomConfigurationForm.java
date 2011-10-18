/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

/**
 * The <tt>ChatRoomConfigurationForm</tt> contains the chat room configuration.
 * It's meant to be implemented by protocol providers in order to provide an
 * access to the administration properties of a chat room. The GUI should be
 * able to obtain this form from the chat room and provide the user with the
 * user interface representation and the possibility to change it.
 * <br>
 * The <tt>ChatRoomConfigurationForm</tt> contains a list of
 * <tt>ChatRoomConfigurationFormField</tt>s. Each field corresponds to a chat
 * room configuration property.
 * 
 * @author Yana Stamcheva
 */
public interface ChatRoomConfigurationForm
{   
    /**
     * Returns an Iterator over a set of <tt>ChatRoomConfigurationFormField</tt>s,
     * containing the current configuration of the chat room. This method is
     * meant to be used by bundles interested showing and changing the current
     * chat room configuration.
     * 
     * @return a list of <tt>ChatRoomConfigurationFormField</tt>s, containing
     * the current configuration of the chat room
     */
    public Iterator<ChatRoomConfigurationFormField> getConfigurationSet();
    
    /**
     * Submits the information in this configuration form to the server.
     * 
     * @throws OperationFailedException if the submit opeation do not succeed
     * for some reason (e.g. a wrong value is provided for a property)
     */
    public void submit() throws OperationFailedException;
}
