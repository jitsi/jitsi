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
package net.java.sip.communicator.impl.protocol.dict;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Represents the Dict protocol icon. Implements the <tt>ProtocolIcon</tt>
 * interface in order to provide a Dict logo image in two different sizes.
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
    private static Hashtable<String,byte[]> iconsTable
        = new Hashtable<String,byte[]>();
    static
    {
        iconsTable.put(ProtocolIcon.ICON_SIZE_16x16,
            DictActivator.getResources()
                .getImageInBytes("service.protocol.dict.DICT_16x16"));

        iconsTable.put(ProtocolIcon.ICON_SIZE_32x32,
            DictActivator.getResources()
                .getImageInBytes("service.protocol.dict.DICT_32x32"));

        iconsTable.put(ProtocolIcon.ICON_SIZE_48x48,
            DictActivator.getResources()
                .getImageInBytes("service.protocol.dict.DICT_48x48"));

        iconsTable.put(ProtocolIcon.ICON_SIZE_64x64,
            DictActivator.getResources()
                .getImageInBytes("service.protocol.dict.DICT_64x64"));
    }

    /**
     * A hash table containing the path to the protocol icon in different sizes.
     */
    private static Hashtable<String, String> iconPathsTable
        = new Hashtable<String, String>();
    static
    {
        iconPathsTable.put(ProtocolIcon.ICON_SIZE_16x16,
            DictActivator.getResources()
                .getImagePath("service.protocol.dict.DICT_16x16"));

        iconPathsTable.put(ProtocolIcon.ICON_SIZE_32x32,
            DictActivator.getResources()
                .getImagePath("service.protocol.dict.DICT_32x32"));

        iconPathsTable.put(ProtocolIcon.ICON_SIZE_48x48,
            DictActivator.getResources()
                .getImagePath("service.protocol.dict.DICT_48x48"));

        iconPathsTable.put(ProtocolIcon.ICON_SIZE_64x64,
            DictActivator.getResources()
                .getImagePath("service.protocol.dict.DICT_64x64"));
    }

    /**
     * Implements the <tt>ProtocolIcon.getSupportedSizes()</tt> method. Returns
     * an iterator to a set containing the supported icon sizes.
     * @return Returns an iterator to a set containing the supported icon sizes
     */
    public Iterator<String> getSupportedSizes()
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
     * @return Returns a byte[] containing the pixels of the icon for the given
     * size.
     */
    public byte[] getIcon(String iconSize)
    {
        return iconsTable.get(iconSize);
    }

    /**
     * Returns a path to the icon with the given size.
     * @param iconSize the size of the icon we're looking for
     * @return the path to the icon with the given size
     */
    public String getIconPath(String iconSize)
    {
        return iconPathsTable.get(iconSize);
    }

    /**
     * Returns the icon image used to represent the protocol connecting state.
     * @return Returns the icon image used to represent the protocol connecting
     * state.
     */
    public byte[] getConnectingIcon()
    {
        return iconsTable.get(ProtocolIcon.ICON_SIZE_16x16);
    }
}
