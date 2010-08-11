/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.notificationconfiguration;

import java.io.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * Filter to display only the sound files in the filechooser
 * 
 * @author Alexandre Maillard
 */
public class SoundFilter 
    extends SipCommFileFilter
{
    /**
     * Method which describes differents permits extensions and defines which file or
     * directory will be displayed in the filechoser.
     * @param f file for the test
     * @return boolean true if the File is a Directory or a sound file. And
     * return false in the other cases.
     */
    public boolean accept(File f)
    {
        /*
         * Test if the file passed in argument is a directory.
         */
        if (f.isDirectory())
        {
            return true;
        }
        /*
         * Else, it tests if the exension is correct
         */
        return SoundFileUtils.isSoundFile(f);
    }
    /**
     * Method which describes, in the file chooser, the text representing the permit extension
     * files.
     * @return String which is displayed in the sound file chooser.
     */
    public String getDescription()
    {
        return "Sound File (*.au, *.mid, *.mod, *.mp2, *.mp3, *.ogg, *.ram," +
                "*.wav, *.wma)";
    }
}
