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
package net.java.sip.communicator.service.desktop;

import java.io.*;
import java.net.*;

/**
 * The <tt>DesktopService</tt> manages the .
 *
 * @author Yana Stamcheva
 */
public interface DesktopService
{
    /**
     * Launches the associated application to open the file.
     *
     * @param file the file to be opened
     *
     * @throws NullPointerException if file is null
     * @throws IllegalArgumentException if the specified file dosen't exist
     * @throws UnsupportedOperationException if the current platform does not
     * support the Desktop.Action.OPEN action
     * @throws IOException if the specified file has no associated application
     * or the associated application fails to be launched
     * @throws SecurityException if a security manager exists and its
     * SecurityManager.checkRead(java.lang.String) method denies read access to
     * the file, or it denies the AWTPermission("showWindowWithoutWarningBanner")
     * permission, or the calling thread is not allowed to create a subprocess
     */
    public void open(File file)
        throws  NullPointerException,
                IllegalArgumentException,
                UnsupportedOperationException,
                IOException,
                SecurityException;

    /**
     * Prints a file with the native desktop printing facility, using the
     * associated application's print command.
     *
     * @param file the file to be opened
     *
     * @throws NullPointerException if file is null
     * @throws IllegalArgumentException if the specified file dosen't exist
     * @throws UnsupportedOperationException if the current platform does not
     * support the Desktop.Action.OPEN action
     * @throws IOException if the specified file has no associated application
     * or the associated application fails to be launched
     * @throws SecurityException if a security manager exists and its
     * SecurityManager.checkRead(java.lang.String) method denies read access to
     * the file, or it denies the AWTPermission("showWindowWithoutWarningBanner")
     * permission, or the calling thread is not allowed to create a subprocess
     */
    public void print(File file)
        throws  NullPointerException,
                IllegalArgumentException,
                UnsupportedOperationException,
                IOException,
                SecurityException;

    /**
     * Launches the associated editor application and opens a file for editing.
     *
     * @param file the file to open for editing
     *
     * @throws NullPointerException if file is null
     * @throws IllegalArgumentException if the specified file dosen't exist
     * @throws UnsupportedOperationException if the current platform does not
     * support the Desktop.Action.OPEN action
     * @throws IOException if the specified file has no associated application
     * or the associated application fails to be launched
     * @throws SecurityException if a security manager exists and its
     * SecurityManager.checkRead(java.lang.String) method denies read access to
     * the file, or it denies the AWTPermission("showWindowWithoutWarningBanner")
     * permission, or the calling thread is not allowed to create a subprocess
     */
    public void edit(File file)
        throws  NullPointerException,
                IllegalArgumentException,
                UnsupportedOperationException,
                IOException,
                SecurityException;

    /**
     * Launches the default browser to display a URI.
     *
     * @param uri the URI to be displayed in the user default browser
     *
     * @throws NullPointerException if file is null
     * @throws IllegalArgumentException if the specified file dosen't exist
     * @throws UnsupportedOperationException if the current platform does not
     * support the Desktop.Action.OPEN action
     * @throws IOException if the specified file has no associated application
     * or the associated application fails to be launched
     * @throws SecurityException if a security manager exists and its
     * SecurityManager.checkRead(java.lang.String) method denies read access to
     * the file, or it denies the AWTPermission("showWindowWithoutWarningBanner")
     * permission, or the calling thread is not allowed to create a subprocess
     */
    public void browse(URI uri)
        throws  NullPointerException,
                IllegalArgumentException,
                UnsupportedOperationException,
                IOException,
                SecurityException;
}
