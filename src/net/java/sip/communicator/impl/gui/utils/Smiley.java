/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import net.java.sip.communicator.impl.gui.utils.ImageLoader.*;

/**
 * The <tt>Smily</tt> is used to store a smily.
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
    public String getImagePath() {
        return Images.getString(this.getImageID().getId());
    }
}
