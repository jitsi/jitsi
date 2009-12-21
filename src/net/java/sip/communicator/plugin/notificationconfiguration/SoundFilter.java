/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.notificationconfiguration;

import java.io.File;

import net.java.sip.communicator.util.swing.SipCommFileFilter;

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
        String extension = Utils.getExtension(f);
        if (extension != null)
        {
            if (extension.equals(Utils.wav) ||
                    extension.equals(Utils.mid) ||
                    extension.equals(Utils.mp2) ||
                    extension.equals(Utils.mp3) ||
                    extension.equals(Utils.mod) ||
                    extension.equals(Utils.ogg) ||
                    extension.equals(Utils.wma) ||
                    extension.equals(Utils.au) ||
                    extension.equals(Utils.ram))
            {
                return true;
            }
            else
            {
                return false;
            }
        }

        return false;
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

/**
 * class which defines the different permit extension file
 * @author Alexandre Maillard
 */
class Utils 
{
    /*
     * Differents extension of a sound file
     */
    public final static String wav = "wav";
    public final static String mid = "mid";
    public final static String mp2 = "mp2";
    public final static String mp3 = "mp3";
    public final static String mod = "mod";
    public final static String ram = "ram";
    public final static String wma = "wma";
    public final static String ogg = "ogg";
    public final static String au = "au";

    /*
     * Gets the file extension.
     * @param File which wants the extension
     * @return Return the extension as a String
     */  
    public static String getExtension(File f) 
    {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) 
            ext = s.substring(i+1).toLowerCase();

        return ext;
    }
}
