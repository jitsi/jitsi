/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import net.java.sip.communicator.service.protocol.*;

import java.awt.*;
import java.util.*;

/**
 * Default OtrActionHandler implementation that opens SWING buddy authenticate
 * dialog.
 *
 * @author Daniel Perren
 * @author Pawel Domas
 */
public class SwingOtrActionHandler
    implements OtrActionHandler
{
    public void onAuthenticateLinkClicked(UUID uuid)
    {
        Contact contact = ScOtrEngineImpl.getContact(
                    ScOtrEngineImpl.getScSessionForGuid(uuid).getSessionID());

        openAuthDialog(contact);
    }

    public static void openAuthDialog(Contact contact)
    {
        // Launch auth buddy dialog.
        OtrBuddyAuthenticationDialog authenticateBuddyDialog
                = new OtrBuddyAuthenticationDialog(contact);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        authenticateBuddyDialog.setLocation(
            screenSize.width / 2
                    - authenticateBuddyDialog.getWidth() / 2,
            screenSize.height / 2
                    - authenticateBuddyDialog.getHeight() / 2);

        authenticateBuddyDialog.setVisible(true);
    }
}
