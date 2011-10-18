/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.whiteboard.gui;

import java.io.File;

import net.java.sip.communicator.util.swing.*;

/**
 * A simple file filter manager
 *
 * @author Julien Waechter
 */
public class WhiteboardFileFilter extends SipCommFileFilter {
    
    /**
     * file extension
     */
    private String ext;
    /**
     * file description
     */
    private String description;
    
    /**
     * WhiteboardFileFilter constructor
     * @param ext extension
     * @param description description
     */
    public WhiteboardFileFilter (String ext, String description) {
        this.ext = ext;
        this.description = description;
    }
    
    /**
     * Tests the specified file,
     * returning true if the file is accepted, false otherwise.
     * True is returned if the extension matches one of
     * the file name extensions of this FileFilter,
     * or the file is a directory.
     * @param f file
     * @return true if file is accepted
     */
    public boolean accept (File f) {
        if (f != null) {
            if (f.isDirectory ()) {
                return true;
            }
            String e = getExtension (f);
            if (e != null && e.equals (ext)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * The description of this filter. For example: "JPG and GIF Images"
     * @return description
     */
    public String getDescription () {
        return description;
    }
    /**
     * The description of this filter. For example: "JPG and GIF Images"
     * @return description
     */
    public String getExtension () {
        return ext;
    }
    
    /**
     * The extension of the file"
     * @param f File
     * @return file extension
     */
    public String getExtension (File f) {
        if (f != null) {
            String filename = f.getName ();
            int i = filename.lastIndexOf ('.');
            if (i > 0 && i < filename.length () - 1) {
                return filename.substring (i + 1).toLowerCase ();
            }
        }
        return null;
    }
}