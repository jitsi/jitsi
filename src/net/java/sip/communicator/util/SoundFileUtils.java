/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.io.*;

/**
 * Defines the different permit extension file.
 *
 * @author Alexandre Maillard
 * @author Dmitri Melnikov
 */
public class SoundFileUtils 
{
    /**
     * Different extension of a sound file
     */
    public final static String wav = "wav";
    public final static String mid = "midi";
    public final static String mp2 = "mp2";
    public final static String mp3 = "mp3";
    public final static String mod = "mod";
    public final static String ram = "ram";
    public final static String wma = "wma";
    public final static String ogg = "ogg";
    public final static String gsm = "gsm";
    public final static String aif = "aiff";
    public final static String au = "au";

    /**
     * The file extension and the format of call recording to be used by
     * default. 
     */
    public static final String DEFAULT_CALL_RECORDING_FORMAT = wav;

    /**
     * Checks whether this file is a sound file.
     * 
     * @param f <tt>File</tt> to check
     * @return <tt>true</tt> if it's a sound file, <tt>false</tt> otherwise
     */
    public static boolean isSoundFile(File f)
    {
        String ext = getExtension(f);

        if (ext != null)
        {
            return
                ext.equals(wma)
                    || ext.equals(wav)
                    || ext.equals(ram)
                    || ext.equals(ogg)
                    || ext.equals(mp3)
                    || ext.equals(mp2)
                    || ext.equals(mod)
                    || ext.equals(mid)
                    || ext.equals(gsm)
                    || ext.equals(au);
        }
        return false;
    }

    /**
     * Checks whether this file is a recorded call. Only some
     * sound file formats are used in call recording.
     *
     * @param f <tt>File</tt> to check
     * @return <tt>true</tt> if it's a call file, <tt>false</tt> otherwise
     */    
    public static boolean isRecordedCall(File f)
    {
        String ext = getExtension(f);

        if (ext != null)
        {
            return
                ext.equals(wav)
                    || ext.equals(gsm)
                    || ext.equals(au)
                    || ext.equals(aif);
        }
        return false;
    }

    /**
     * Gets the file extension.
     * TODO: There are at least 2 other methods like this scattered around
     * the SC code, we should move them all to util package.
     *
     * @param f which wants the extension
     * @return Return the extension as a String
     */  
    public static String getExtension(File f) 
    {
        String s = f.getName();
        int i = s.lastIndexOf('.');
        String ext = null;

        if ((i > 0) &&  (i < s.length() - 1))
            ext = s.substring(i+1).toLowerCase();
        return ext;
    }
}
