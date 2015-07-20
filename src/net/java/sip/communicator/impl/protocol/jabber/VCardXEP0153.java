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
package net.java.sip.communicator.impl.protocol.jabber;

import org.jivesoftware.smack.util.*;
import org.jivesoftware.smackx.packet.*;

/**
 * Extends the smack vCard in order to manage the possibility to remove the
 * current avatar. In the setAvatar() functions, if the avatar image "bytes"
 * length is 0, then sets a "photo" tag with an empty body (corresponding to
 * the XEP0153 definition).
 * This class may be removed from Jitsi if the corresponding patch is merged
 * into smack (cf code between "&lt;BEGIN&gt; patch submitted to smack." and
 * "&lt;END\&gt; patch submitted to smack."). Thereafter, all references to the
 * VCardXEP0153 class must be replaced by the VCard smak class.
 *
 * @author Vincent Lucas
 */
public class VCardXEP0153
    extends VCard
{
    /**
     * Specify the bytes for the avatar to use.
     *
     * @param bytes the bytes of the avatar.
     */
    @Override
    public void setAvatar(byte[] bytes)
    {
        this.setAvatar(bytes, "image/jpeg");
    }

    /**
     * Specify the bytes for the avatar to use as well as the mime type.
     *
     * @param bytes the bytes of the avatar.
     * @param mimeType the mime type of the avatar.
     */
    @Override
    public void setAvatar(byte[] bytes, String mimeType)
    {
        // Remove avatar (if any) from mappings
        if (bytes == null)
        {
            super.setAvatar(bytes, mimeType);
            //otherUnescapableFields.remove("PHOTO");
        }
        // Otherwise, add to mappings.
        else
        {
            // <BEGIN> patch submitted to smack.
            // Sets an empty avatar (used to removes old pictures).
            if(bytes.length == 0)
            {
                setEncodedImage("");
                setField("PHOTO", "", true);
            }
            // <END> patch submitted to smack.
            // Sets the avatar image.
            else
            {
                String encodedImage = StringUtils.encodeBase64(bytes);
                setEncodedImage(encodedImage);

                setField(
                        "PHOTO",
                        "<TYPE>" + mimeType + "</TYPE>"
                        + "<BINVAL>" + encodedImage + "</BINVAL>",
                        true);
            }
        }
    }
}
