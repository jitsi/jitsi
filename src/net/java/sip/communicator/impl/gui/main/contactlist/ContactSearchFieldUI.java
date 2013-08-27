/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.plugin.desktoputil.plaf.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

/**
 *
 * @author Marin Dzhigarov
 */
public class ContactSearchFieldUI
    extends SearchFieldUI
    implements Skinnable
{
    /**
     * Creates a UI for a SearchFieldUI.
     * 
     * @param c the text field
     * @return the UI
     */
    public static ComponentUI createUI(JComponent c)
    {
        return new ContactSearchFieldUI();
    }

    public ContactSearchFieldUI()
    {
        // Indicates if the big call button outside the search is enabled.
        String callButtonEnabledString
            = UtilActivator.getResources().getSettingsString(
                    "impl.gui.CALL_BUTTON_ENABLED");

        if ((callButtonEnabledString != null)
                && (callButtonEnabledString.length() > 0))
        {
            // If the outside call button is enabled the call button in this
            // search field is disabled.
            isCallButtonEnabled
                = !Boolean.parseBoolean(callButtonEnabledString);
        }

        loadSkin();
    }

    /**
     * Paints the background of the associated component.
     * 
     * @param g the <tt>Graphics</tt> object used for painting
     */
    @Override
    protected void customPaintBackground(Graphics g)
    {
        isCallButtonEnabled = CallManager.getTelephonyProviders().size() > 0;
        super.customPaintBackground(g);
    }

    /**
     * Creates a call when the mouse is clicked on the call icon.
     * 
     * @param ev the mouse event that has prompted us to create the call.
     */
    @Override
    protected void updateCallIcon(MouseEvent ev)
    {
        super.updateCallIcon(ev);


        if ((ev.getID() == MouseEvent.MOUSE_CLICKED) && isCallIconVisible)
        {
            Rectangle callButtonRect = getCallButtonRect();
            int x = ev.getX();
            int y = ev.getY();

            if (callButtonRect.contains(x, y))
            {
                JTextComponent c = getComponent();
                String searchText = c.getText();

                if (searchText != null)
                    CallManager.createCall(searchText, c);
            }
        }
    }
}
