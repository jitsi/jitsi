/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import javax.swing.*;

import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.plaf.*;
/**
 * The SIPCommMenuBar is a <tt>JMenuBar</tt> without border decoration that can
 * be used as a container for other components, like selector boxes that won't
 * need a menu decoration.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SIPCommMenuBar
    extends JMenuBar
    implements Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Class id key used in UIDefaults.
     */
    private static final String UIClassID = "SIPCommMenuBarUI";

    /**
     * Adds the ui class to UIDefaults.
     */
    static
    {
        UIManager.getDefaults().put(UIClassID,
            SIPCommMenuBarUI.class.getName());
    }

    /**
     * Creates an instance of <tt>SIPCommMenuBar</tt>.
     */
    public SIPCommMenuBar()
    {
        loadSkin();
    }

    /**
     * Reload UI defs.
     */
    public void loadSkin()
    {
        this.setBorder(BorderFactory.createEmptyBorder());
    }

    /**
    * Returns the name of the L&F class that renders this component.
    *
    * @return the string "TreeUI"
    * @see JComponent#getUIClassID
    * @see UIDefaults#getUI
    */
   public String getUIClassID()
   {
       return UIClassID;
   }
}
