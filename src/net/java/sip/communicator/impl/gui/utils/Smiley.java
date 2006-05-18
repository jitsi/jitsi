/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import net.java.sip.communicator.impl.gui.utils.ImageLoader.ImageID;

public class Smiley {

    private ImageID imageID;

    private String[] smileyStrings;

    public Smiley(ImageID imageID, String[] smileyStrings) {

        this.imageID = imageID;

        this.setSmileyStrings(smileyStrings);
    }

    public String[] getSmileyStrings() {

        return smileyStrings;
    }

    public void setSmileyStrings(String[] smileyStrings) {

        this.smileyStrings = smileyStrings;
    }

    public String getDefaultString() {

        return this.smileyStrings[0];
    }

    public ImageID getImageID() {

        return this.imageID;
    }

    public String getImagePath() {
        return Images.getString(this.getImageID().getId());
    }
}
