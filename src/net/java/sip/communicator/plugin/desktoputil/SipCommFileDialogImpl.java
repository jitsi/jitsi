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

import java.awt.*;
import java.io.*;
import java.net.*;

import org.jitsi.util.*;

/**
 * Implements <tt>SipCommFileChooser</tt> for AWT's <tt>FileDialog</tt>.
 *
 * @author Valentin Martinet
 */
public class SipCommFileDialogImpl
    extends FileDialog
    implements SipCommFileChooser
{
    /**
     * The serialization-related version of the <tt>SipCommFileDialogImpl</tt>
     * class explicitly defined to silence a related warning (e.g. in Eclipse
     * IDE) since the <tt>SipCommFileDialogImpl</tt> class does not add instance
     * fields.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The selection mode, the default is files only, can be changed to
     * DIRECTORIES_ONLY.
     */
    private int selectionMode = FILES_ONLY;

    /**
     * Constructor
     *
     * @param parent the parent frame of this dialog
     * @param title the title for this dialog
     */
    public SipCommFileDialogImpl(Frame parent, String title)
    {
        super(parent, title);
    }

    /**
     * Constructor
     *
     * @param parent the parent frame of this dialog
     * @param title the title for this dialog
     * @param fileOperation request a 'load file' or 'save file' dialog
     */
    public SipCommFileDialogImpl(Frame parent, String title, int fileOperation)
    {
        super(parent, title, fileOperation);
    }

    /**
     * Returns the selected file by the user from the dialog.
     *
     * @return File the selected file from the dialog
     */
    public File getApprovedFile()
    {
        String file = getFile();

        return (file != null) ? new File(getDirectory(), file) : null;
    }

    /**
     * Sets the default path to be considered for browsing among files.
     *
     * @param path the default start path for this dialog
     */
    public void setStartPath(String path)
    {
        // If the path is null, we have nothing more to do here.
        if (path == null)
            return;

        // If the path is an URL extract the path from the URL in order to
        // remove the "file:" part, which doesn't work with methods provided
        // by the file chooser.
        try
        {
            URL url = new URL(path);

            path = url.getPath();
        }
        catch (MalformedURLException e) {}

        File file = new File(path);

        if ((file != null) && !file.isDirectory())
        {
            setDirectory(file.getParent());
            setFile(file.getName());
        }
        else
        {
            setDirectory(path);
            setFile(null);
        }
    }

    /**
     * Shows the dialog and returns the selected file.
     *
     * @return File the selected file in this dialog
     */
    public File getFileFromDialog()
    {
        this.setVisible(true);

        return this.getApprovedFile();
    }

    /**
     * Adds a file filter to this dialog.
     *
     * @param filter the filter to add
     */
    public void addFilter(SipCommFileFilter filter)
    {
        this.setFilenameFilter(filter);
    }

    /**
     * Sets a file filter to this dialog.
     *
     * @param filter the filter to add
     */
    public void setFileFilter(SipCommFileFilter filter)
    {
        this.setFilenameFilter(filter);
    }

    /**
     * Returns the filter the user has chosen for saving a file.
     *
     * @return SipCommFileFilter the used filter when saving a file
     */
    public SipCommFileFilter getUsedFilter()
    {
        return (SipCommFileFilter)this.getFilenameFilter();
    }

    /**
     * Change the selection mode for the file choose.
     * Possible values are DIRECTORIES_ONLY or FILES_ONLY, default is
     * FILES_ONLY.
     *
     * @param mode the mode to use.
     */
    public void setSelectionMode(int mode)
    {
        this.selectionMode = mode;
    }

    /**
     * Shows or hides the file chooser dialog.
     * @param b  if <code>true</code>, shows the dialog;
     * otherwise, hides it
     */
    @Override
    public void setVisible(boolean b)
    {
        // workaround to make sure we choose only folders on macosx
        if(OSUtils.IS_MAC && selectionMode == DIRECTORIES_ONLY)
            System.setProperty(
                "apple.awt.fileDialogForDirectories", "true");

        super.setVisible(b);

        if(OSUtils.IS_MAC && selectionMode == DIRECTORIES_ONLY)
            System.setProperty(
                "apple.awt.fileDialogForDirectories", "false");
    }
}
