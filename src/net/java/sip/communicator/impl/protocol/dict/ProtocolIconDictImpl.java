/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.dict;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Reperesents the Dict protocol icon. Implements the <tt>ProtocolIcon</tt>
 * interface in order to provide a dict logo image in two different sizes.
 * 
 * @author ROTH Damien
 * @author LITZELMANN Cedric
 */
public class ProtocolIconDictImpl
    implements ProtocolIcon
{
    /**
     * A hash table containing the protocol icon in different sizes.
     */
    private static Hashtable<String,byte[]> iconsTable = new Hashtable<String,byte[]>();
    static {
        iconsTable.put(ProtocolIcon.ICON_SIZE_16x16,
            DictActivator.getResources().getImageInBytes("dictProtocolIcon"));

        iconsTable.put(ProtocolIcon.ICON_SIZE_64x64,
                DictActivator.getResources().getImageInBytes("dict64x64Icon"));
    }

    /**
     * Implements the <tt>ProtocolIcon.getSupportedSizes()</tt> method. Returns
     * an iterator to a set containing the supported icon sizes.
     * @return          Returns an iterator to a set containing the supported icon sizes
     */
    public Iterator getSupportedSizes()
    {
        return iconsTable.keySet().iterator();
    }

    /**
     * Returns TRUE if an icon with the given size is supported, FALSE otherwise.
     * @param iconSize  The size of the icon, that we want to know if it is
     * supported.
     * @return          Returns true if the size is supported. False otherwise.
     */
    public boolean isSizeSupported(String iconSize)
    {
        return iconsTable.containsKey(iconSize);
    }
    
    /**
     * Returns the icon image in the given size.
     * @param iconSize  The icon size one of ICON_SIZE_XXX constants
     * @return          Returns a byte[] containing the pixels of the icon for the given
     * size.
     */
    public byte[] getIcon(String iconSize)
    {
        return iconsTable.get(iconSize);
    }
    
    /**
     * Returns the icon image used to represent the protocol connecting state.
     * @return          Returns the icon image used to represent the protocol connecting state.
     */
    public byte[] getConnectingIcon()
    {
        return iconsTable.get(ProtocolIcon.ICON_SIZE_16x16);
    }
}
