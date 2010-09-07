/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.net.*;
import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.resources.*;

/**
 * The <tt>Smiley</tt> is used to store a smiley.
 * 
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class Smiley
{
    private final String description;

    private final ImageID imageID;

    private final List<String> smileyStrings;

    /**
     * Creates an instance of <tt>Smiley</tt>, by specifying the smiley
     * image identifier and the strings corresponding to it.
     * @param imageID The image identifier of the smiley icon.
     * @param smileyStrings A set of strings corresponding to the smiley
     * icon.
     * @param description the description of the smiley
     */
    public Smiley(ImageID imageID, String[] smileyStrings, String description)
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
    public ImageID getImageID()
    {
        return this.imageID;
    }

    /**
     * Returns the path of the image corresponding to this smiley.
     * @return the path of the image corresponding to this smiley.
     */
    public String getImagePath() 
    {
        URL url
            = GuiActivator.getResources()
                .getImageURL(this.getImageID().getId());

        if(url == null)
            return null;

        return url.toString();
    }

    /**
     * Returns the path of the image corresponding to this smiley.
     * @param resourcesService The ResourceManagementService required to get the 
     *        image URL.
     * @return the path of the image corresponding to this smiley.
     */
    public String getImagePath(ResourceManagementService resourcesService) 
    {
        URL url
            = resourcesService
                .getImageURL(this.getImageID().getId());

        if(url == null)
            return null;

        return url.toString();
    }
}
