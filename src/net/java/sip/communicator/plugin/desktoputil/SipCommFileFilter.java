/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.desktoputil;

import java.io.*;

import javax.swing.filechooser.FileFilter;


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
