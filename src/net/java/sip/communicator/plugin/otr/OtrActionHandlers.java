/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.awt.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Class for storing OTR functions used by the menus, buttons, links, etc.
 * 
 * @author Daniel Perren
 */
class OtrActionHandlers
{
    /**
     * Opening the standard authorisation dialog for OTR fingerprints.
     * 
     * @param contact the contact you would like to authenticate.
     */
    static void openAuthDialog(Contact contact)
    {
        // Launch auth buddy dialog.
        OtrBuddyAuthenticationDialog authenticateBuddyDialog =
            new OtrBuddyAuthenticationDialog(contact);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        authenticateBuddyDialog.setLocation(screenSize.width / 2
            - authenticateBuddyDialog.getWidth() / 2, screenSize.height / 2
            - authenticateBuddyDialog.getHeight() / 2);
        authenticateBuddyDialog.setVisible(true);
    }
}
