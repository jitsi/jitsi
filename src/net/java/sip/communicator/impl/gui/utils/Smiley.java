/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.net.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.resources.*;

/**
 * The <tt>Smiley</tt> is used to store a smiley.
 * 
 * @author Yana Stamcheva
 */
public class Smiley
{
    private ImageID imageID;

    private String[] smileyStrings;

    private String description;

    /**
     * Creates an instance of <tt>Smiley</tt>, by specifying the smiley
     * image identifier and the strings corresponding to it.
     * @param imageID The image identifier of the smiley icon.
     * @param smileyStrings A set of strings corresponding to the smiley
     * icon.
     */
    public Smiley(ImageID imageID, String[] smileyStrings, String description)
    {
        this.imageID = imageID;

        this.setSmileyStrings(smileyStrings);

        this.setDescription(description);
    }

    /**
     * Returns the set of Strings corresponding to this smiley.
     * @return the set of Strings corresponding to this smiley.
     */
    public String[] getSmileyStrings()
    {
        return smileyStrings;
    }

    /**
     * Sets the set of Strings corresponding to this smiley. They could be
     * ":-)", ":)", ":))" for example.
     * @param smileyStrings the set of Strings corresponding to this smiley.
     */
    public void setSmileyStrings(String[] smileyStrings)
    {
        this.smileyStrings = smileyStrings;
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
     * Sets the description of this smiley.
     * 
     * @param description the description of the smiley.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * Returns the default String corresponding for this smiley. For example
     * ":-)".
     * @return the default String corresponding for this smiley.
     */
    public String getDefaultString()
    {
        return this.smileyStrings[0];
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
}
