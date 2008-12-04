/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.swing;

import java.net.*;

import net.java.sip.communicator.service.resources.*;

/**
 * The <tt>Smiley</tt> is used to store a smiley.
 * 
 * @author Yana Stamcheva
 */
public class Smiley {

    private ImageID imageID;

    private String[] smileyStrings;

    /**
     * Creates an instance of <tt>Smily</tt>, by specifying the smily
     * image identifier and the strings corresponding to it.
     * @param imageID The image identifier of the smily icon.
     * @param smileyStrings A set of strings corresponding to the smily
     * icon.
     */
    public Smiley(ImageID imageID, String[] smileyStrings) {

        this.imageID = imageID;

        this.setSmileyStrings(smileyStrings);
    }

    /**
     * Returns the set of Strings corresponding to this smily.
     * @return the set of Strings corresponding to this smily.
     */
    public String[] getSmileyStrings() {

        return smileyStrings;
    }

    /**
     * Sets the set of Strings corresponding to this smily. They could be
     * ":-)", ":)", ":))" for example.
     * @param smileyStrings the set of Strings corresponding to this smily.
     */
    public void setSmileyStrings(String[] smileyStrings) {

        this.smileyStrings = smileyStrings;
    }

    /**
     * Returns the default String corresponding for this smily. For example
     * ":-)".
     * @return the default String corresponding for this smily.
     */
    public String getDefaultString() {

        return this.smileyStrings[0];
    }

    /**
     * Returns the identifier of the image corresponding to this smily. 
     * @return the identifier of the image corresponding to this smily.
     */
    public ImageID getImageID() {

        return this.imageID;
    }

    /**
     * Returns the path of the image corresponding to this smily.
     * @return the path of the image corresponding to this smily.
     */
    public String getImagePath() 
    {
        URL url = SwingCommonActivator.getResources().
            getImageURL(this.getImageID().getId());
        
        if(url == null)
            return null;
        
        return url.toString();
    }
}
