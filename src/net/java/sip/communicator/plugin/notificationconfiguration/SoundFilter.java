/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.notificationconfiguration;

import java.io.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * Filter to display only the sound files in the filechooser.
 * 
 * @author Alexandre Maillard
 * @author Vincent Lucas
 */
public class SoundFilter 
    extends SipCommFileFilter
{
    /**
     * All acceptable sound formats. If null, then this sound filter will accept
     * all sound formats available in SoundFileUtils.
     */
    private String[] soundFormats = null;

    /**
     * Creates a new sound filter which accepts all sound format available in
     * SoundFileUtils.
     */
    public SoundFilter()
    {
        super();
    }

    /**
     * Creates a new sound filter which accepts only sound format corresponding
     * to the list given in parameter.
     *
     * @param soundFormats The list of sound format to accept.
     */
    public SoundFilter(String[] soundFormats)
    {
        super();
        if(soundFormats != null)
        {
            this.soundFormats = new String[soundFormats.length];
            System.arraycopy(
                    soundFormats,
                    0,
                    this.soundFormats,
                    0,
                    soundFormats.length);
        }
    }

    /**
     * Method which describes differents permits extensions and defines which
     * file or directory will be displayed in the filechoser.
     *
     * @param f file for the test
     *
     * @return boolean true if the File is a Directory or a sound file. And
     * return false in the other cases.
     */
    public boolean accept(File f)
    {
        // Tests if the file passed in argument is a directory.
        if (f.isDirectory())
        {
            return true;
        }
        // Else, tests if the exension is correct.
        else
        {
            return SoundFileUtils.isSoundFile(f, this.soundFormats);
        }
    }

    /**
     * Method which describes, in the file chooser, the text representing the
     * permit extension files.
     *
     * @return String which is displayed in the sound file chooser.
     */
    public String getDescription()
    {
        String desc = "Sound File (";
        if(this.soundFormats != null)
        {
            for(int i = 0; i < this.soundFormats.length; ++i)
            {
                if(i != 0)
                {
                    desc += ", ";
                }
                desc += "*." + this.soundFormats[i];
            }
        }
        else
        {
            desc += "*.au, *.mid, *.mod, *.mp2, *.mp3, *.ogg, *.ram, *.wav, "
                + "*.wma";
        }
        desc += ")";

        return desc;
    }
}
