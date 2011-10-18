/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.io.File;
import javax.swing.filechooser.FileFilter;
import java.io.FilenameFilter;


/**
 * The purpose of this interface is to provide an generic file filter type for 
 * the SipCommFileChooser, which is used either as an AWT FileDialog, either as
 * a Swing JFileChooser. 
 * 
 * Both of these dialogs use their own filter type, FileFilter (class) for 
 * JFileChooser and FilenameFilter (interface) for FileDialog.
 * 
 * SipCommFileFilter acts as both an implementation and an heritage from these
 * two filters. To use a your own file filter with a SipCommFileChooser, you
 * just have to extend from SipCommFileFilter and redefine at least the method
 * 'public boolean accept(File f)' which is described in the Java FileFilter 
 * class.
 * 
 * You won't have to redefine 'public boolean accept(File dir, String name)' 
 * from the Java FilenameFilter interface since it's done here: the method is 
 * transfered toward the accept method of Java FileFilter class.
 * 
 * @author Valentin Martinet
 */
public abstract class SipCommFileFilter 
    extends FileFilter 
    implements FilenameFilter
{

    /**
     * Avoid to be obliged to implement 
     * 'public boolean accept(File dir, String name)'
     * in your own file filter.
     * 
     * @param dir file's parent directory
     * @param name file's name
     * @return boolean if the file is accepted or not
     */
    public boolean accept(File dir, String name) 
    {
        return accept(new File(dir.getAbsolutePath(), name));
    }
}
