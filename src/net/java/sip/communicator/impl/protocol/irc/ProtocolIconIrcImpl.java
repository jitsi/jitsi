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
package net.java.sip.communicator.impl.protocol.irc;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Represents the IRC protocol icon. Implements the <tt>ProtocolIcon</tt>
 * interface in order to provide an IRC icon image in two different sizes.
 *
 * @author Stephane Remy
 * @author Loic Kempf
 * @author Lubomir Marinov
 */
public class ProtocolIconIrcImpl
    implements ProtocolIcon
{
    /**
     * The <tt>Logger</tt> used by the <tt>ProtocolIconIrcImpl</tt> class and
     * its instances for logging output.
     */
    private static final Logger LOGGER
        = Logger.getLogger(ProtocolIconIrcImpl.class);

    /**
     * A hash table containing the protocol icon in different sizes.
     */
    private static final Map<String, byte[]> ICONS_TABLE
        = new Hashtable<String, byte[]>();
    static
    {
        ICONS_TABLE.put(ProtocolIcon.ICON_SIZE_16x16,
            getImageInBytes("service.protocol.irc.IRC_16x16"));

        ICONS_TABLE.put(ProtocolIcon.ICON_SIZE_32x32,
            getImageInBytes("service.protocol.irc.IRC_32x32"));

        ICONS_TABLE.put(ProtocolIcon.ICON_SIZE_48x48,
            getImageInBytes("service.protocol.irc.IRC_48x48"));

        ICONS_TABLE.put(ProtocolIcon.ICON_SIZE_64x64,
            getImageInBytes("service.protocol.irc.IRC_64x64"));
    }

    /**
     * A hash table containing the path to the protocol icon in different sizes.
     */
    private static final Map<String, String> ICONPATHS_TABLE
        = new Hashtable<String, String>();
    static
    {
        ICONPATHS_TABLE.put(ProtocolIcon.ICON_SIZE_16x16,
            IrcActivator.getResources().getImagePath(
                "service.protocol.irc.IRC_16x16"));

        ICONPATHS_TABLE.put(ProtocolIcon.ICON_SIZE_32x32,
            IrcActivator.getResources().getImagePath(
                "service.protocol.irc.IRC_32x32"));

        ICONPATHS_TABLE.put(ProtocolIcon.ICON_SIZE_48x48,
            IrcActivator.getResources().getImagePath(
                "service.protocol.irc.IRC_48x48"));

        ICONPATHS_TABLE.put(ProtocolIcon.ICON_SIZE_64x64,
            IrcActivator.getResources().getImagePath(
                "service.protocol.irc.IRC_64x64"));
    }

    /**
     * Implements the <tt>ProtocolIcon.getSupportedSizes()</tt> method. Returns
     * an iterator to a set containing the supported icon sizes.
     *
     * @return an iterator to a set containing the supported icon sizes
     */
    public Iterator<String> getSupportedSizes()
    {
        return ICONS_TABLE.keySet().iterator();
    }

    /**
     * Returns TRUE if a icon with the given size is supported, FALSE-otherwise.
     *
     * @param iconSize the icon size; one of ICON_SIZE_XXX constants
     * @return returns <tt>true</tt> if size is supported or <tt>false</tt> if
     *         not.
     */
    public boolean isSizeSupported(final String iconSize)
    {
        return ICONS_TABLE.containsKey(iconSize);
    }

    /**
     * Returns the icon image in the given size.
     *
     * @param iconSize the icon size; one of ICON_SIZE_XXX constants
     * @return returns icon image
     */
    public byte[] getIcon(final String iconSize)
    {
        return ICONS_TABLE.get(iconSize);
    }

    /**
     * Returns a path to the icon with the given size.
     *
     * @param iconSize the icon size; one of ICON_SIZE_XXX constants
     * @return the path to the icon with the given size
     */
    public String getIconPath(final String iconSize)
    {
        return ICONPATHS_TABLE.get(iconSize);
    }

    /**
     * Returns the icon image used to represent the protocol connecting state.
     *
     * @return the icon image used to represent the protocol connecting state
     */
    public byte[] getConnectingIcon()
    {
        return getImageInBytes("ircConnectingIcon");
    }

    /**
     * Returns the byte representation of the image corresponding to the given
     * identifier.
     *
     * @param imageID the identifier of the image
     * @return the byte representation of the image corresponding to the given
     *         identifier.
     */
    static byte[] getImageInBytes(final String imageID)
    {
        InputStream in
            = IrcActivator.getResources().getImageInputStream(imageID);
        byte[] image = null;

        if (in != null)
        {
            try
            {
                image = new byte[in.available()];

                in.read(image);
            }
            catch (IOException e)
            {
                LOGGER.error("Failed to load image:" + imageID, e);
            }
        }
        return image;
    }
}
