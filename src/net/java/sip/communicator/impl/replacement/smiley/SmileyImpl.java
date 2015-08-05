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
package net.java.sip.communicator.impl.replacement.smiley;

import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.replacement.smilies.*;

/**
 * The <tt>Smiley</tt> is used to store a smiley.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class SmileyImpl
    implements Smiley
{
    /**
     * The description of the smiley
     */
    private final String description;

    /**
     * The identifier of the smiley icon.
     */
    private final String imageID;

    /**
     * The strings corresponding to this smiley, e.g. :), :-), etc.
     */
    private final List<String> smileyStrings;

    /**
     * Creates an instance of <tt>Smiley</tt>, by specifying the smiley
     * image identifier and the strings corresponding to it.
     * @param imageID The image identifier of the smiley icon.
     * @param smileyStrings A set of strings corresponding to the smiley
     * icon.
     * @param description the description of the smiley
     */
    public SmileyImpl(String imageID, String[] smileyStrings, String description)
    {
        this.imageID = imageID;
        this.smileyStrings
                = Collections
                    .unmodifiableList(Arrays.asList(smileyStrings.clone()));
        this.description = description;
    }

    /**
     * Returns the set of Strings corresponding to this smiley.
     * @return the set of Strings corresponding to this smiley.
     */
    public List<String> getSmileyStrings()
    {
        return smileyStrings;
    }

    /**
     * Returns the description of this smiley.
     *
     * @return the description of this smiley.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Returns the default String corresponding for this smiley. For example
     * ":-)".
     * @return the default String corresponding for this smiley.
     */
    public String getDefaultString()
    {
        return smileyStrings.get(0);
    }

    /**
     * Returns the identifier of the image corresponding to this smiley.
     * @return the identifier of the image corresponding to this smiley.
     */
    public String getImageID()
    {
        return imageID;
    }

    /**
     * Returns the path of the image corresponding to this smiley.
     * @return the path of the image corresponding to this smiley.
     */
    public String getImagePath()
    {
        URL url = SmileyActivator.getResources().getImageURL(imageID);

        if(url == null)
            return null;

        return url.toString();
    }
}
