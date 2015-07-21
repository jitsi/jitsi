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

import javax.swing.*;

import org.jitsi.util.*;


/**
 * This class is the entry point for creating a file dialog regarding to the OS.
 *
 * If the current operating system is Apple Mac OS X, we create an AWT
 * FileDialog (user interface is more practical under Mac OS than a
 * JFileChooser), else, a Swing JFileChooser.
 *
 * @author Valentin Martinet
 */
public class GenericFileDialog
{
    /**
     * Creates a file dialog (AWT's FileDialog or Swing's JFileChooser)
     * regarding to user's operating system.
     *
     * @param parent the parent Frame/JFrame of this dialog
     * @param title dialog's title
     * @param fileOperation
     * @return a SipCommFileChooser instance
     */
    public static SipCommFileChooser create(
            Frame parent,
            String title,
            int fileOperation)
    {
        int operation;

        if(OSUtils.IS_MAC)
        {
            switch (fileOperation)
            {
            case SipCommFileChooser.LOAD_FILE_OPERATION:
                operation = FileDialog.LOAD;
                break;
            case SipCommFileChooser.SAVE_FILE_OPERATION:
                operation = FileDialog.SAVE;
                break;
            default:
                throw new IllegalArgumentException("fileOperation");
            }

            if (parent == null)
                parent = new Frame();

            return new SipCommFileDialogImpl(parent, title, operation);
        }
        else
        {
            switch (fileOperation)
            {
            case SipCommFileChooser.LOAD_FILE_OPERATION:
                operation = JFileChooser.OPEN_DIALOG;
                break;
            case SipCommFileChooser.SAVE_FILE_OPERATION:
                operation = JFileChooser.SAVE_DIALOG;
                break;
            default:
                throw new IllegalArgumentException("fileOperation");
            }

            return new SipCommFileChooserImpl(title, operation);
        }
    }

    /**
     * Creates a file dialog (AWT FileDialog or Swing JFileChooser) regarding to
     * user's operating system.
     *
     * @param parent the parent Frame/JFrame of this dialog
     * @param title dialog's title
     * @param fileOperation
     * @param path start path of this dialog
     * @return SipCommFileChooser an implementation of SipCommFileChooser
     */
    public static SipCommFileChooser create(
        Frame parent, String title, int fileOperation, String path)
    {
        SipCommFileChooser scfc
            = GenericFileDialog.create(parent, title, fileOperation);

        if(path != null)
            scfc.setStartPath(path);
        return scfc;
    }
}
