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

import lombok.extern.slf4j.*;
import net.java.sip.communicator.service.httputil.*;

import javax.swing.*;
import java.io.*;
import java.net.*;

/**
 * File utils that can download files and show UI if there is a problem with
 * the connection or it takes too much time.
 *
 * @author Damian Minkov
 */
@Slf4j
public class FileUtils
{
    /**
     * Downloads a remote file specified by its URL into a local file.
     *
     * @param url the URL of the remote file to download
     * @return the local <tt>File</tt> into which <tt>url</tt> has been
     * downloaded or <tt>null</tt> if there was no response from the
     * <tt>url</tt>
     * @param preferredFileName the file name we will use if it is not possible
     * to use the file name and extension from the URL provided
     * @param preferredExtension the extension name we will use if it is not
     * possible to use the file name and extension from the URL provided.
     * The extension should be in the form '.exe', '.bz2' etc.
     * @throws IOException if an I/O error occurs during the download
     */
    public static File download(
        String url,
        String preferredFileName,
        String preferredExtension)
        throws IOException
    {
        final File[] tempFile = new File[1];
        boolean deleteTempFile = true;

        var tempFileOutputStream = createTempFileOutputStream(
            new URL(url),
            /*
             * The default extension, possibly derived from url, is
             * fine. Besides, we do not really have information about
             * any preference.
             */
            null,
            /* Do create a FileOutputStream. */
            false,
            tempFile,
            preferredFileName,
            preferredExtension);
        try
        {
            HttpUtils.HTTPResponseResult res
                = HttpUtils.openURLConnection(url);

            if (res != null)
            {
                InputStream content = res.getContent();
                // Track the progress of the download.
                ProgressMonitorInputStream input
                    = new ProgressMonitorInputStream(null, url, content);

                /*
                 * Set the maximum value of the ProgressMonitor to the size of
                 * the file to download.
                 */

                try (input)
                {
                    input.getProgressMonitor().setMaximum(
                        (int) res.getContentLength());

                    try (BufferedOutputStream output = new BufferedOutputStream(
                        tempFileOutputStream))
                    {
                        int read = -1;
                        byte[] buff = new byte[1024];

                        while ((read = input.read(buff)) != -1)
                            output.write(buff, 0, read);
                    }
                    finally
                    {
                        tempFileOutputStream = null;
                    }
                    deleteTempFile = false;
                }
                /*
                 * Ignore it because we've already downloaded the setup
                 * and that's what matters most.
                 */
            }
        }
        finally
        {
            try
            {
                if (tempFileOutputStream != null)
                    tempFileOutputStream.close();
            }
            finally
            {
                if (deleteTempFile && (tempFile[0] != null))
                {
                    tempFile[0].delete();
                    tempFile[0] = null;
                }
            }
        }
        return tempFile[0];
    }

    /**
     * Tries to create a new <tt>FileOutputStream</tt> for a temporary file into
     * which a remote file is to be downloaded. Because temporary files
     * generally have random characters in their names and the name of the file
     * may be shown to the user, first tries to use the name of the URL to be
     * downloaded because it likely is prettier.
     *
     * @param url the <tt>URL</tt> of the file to be downloaded
     * @param extension the extension of the <tt>File</tt> to be created or
     * <tt>null</tt> for the default (which may be derived from <tt>url</tt>)
     * @param dryRun <tt>true</tt> to generate a <tt>File</tt> in
     * <tt>tempFile</tt> and not open it or <tt>false</tt> to generate a
     * <tt>File</tt> in <tt>tempFile</tt> and open it
     * @param tempFile a <tt>File</tt> array of at least one element which is to
     * receive the created <tt>File</tt> instance at index zero (if successful)
     * @param preferredFileName the file name we will use if it is not possible
     * to use the file name and extension from the URL provided
     * @param preferredExtension the extension name we will use if it is not
     * possible to use the file name and extension from the URL provided.
     * The extension should be in the form '.exe', '.bz2' etc.
     * @return the newly created <tt>FileOutputStream</tt>
     * @throws IOException if anything goes wrong while creating the new
     * <tt>FileOutputStream</tt>
     */
    public static FileOutputStream createTempFileOutputStream(
        URL url,
        String extension,
        boolean dryRun,
        File[] tempFile,
        String preferredFileName,
        String preferredExtension)
        throws IOException
    {
        /*
         * Try to use the name from the URL because it isn't a "randomly"
         * generated one.
         */
        String path = url.getPath();

        File tf = null;
        FileOutputStream tfos = null;

        if ((path != null) && (path.length() != 0))
        {
            int nameBeginIndex =path.lastIndexOf('/');
            String name;

            if (nameBeginIndex > 0)
            {
                name = path.substring(nameBeginIndex + 1);
                nameBeginIndex = name.lastIndexOf('\\');
                if (nameBeginIndex > 0)
                    name = name.substring(nameBeginIndex + 1);
            }
            else
                name = path;

            /*
             * Make sure the extension of the name is EXE so that we're able to
             * execute it later on.
             */
            int nameLength = name.length();

            if (nameLength != 0)
            {
                int baseNameEnd = name.lastIndexOf('.');

                if (extension == null)
                    extension = preferredExtension;
                if (baseNameEnd == -1)
                    name += extension;
                else if (baseNameEnd == 0)
                {
                    if (!extension.equalsIgnoreCase(name))
                        name += extension;
                }
                else
                    name = name.substring(0, baseNameEnd) + extension;

                try
                {
                    String tempDir = System.getProperty("java.io.tmpdir");

                    if ((tempDir != null) && (tempDir.length() != 0))
                    {
                        tf = new File(tempDir, name);
                        if (!dryRun)
                            tfos = new FileOutputStream(tf);
                    }
                }
                catch (FileNotFoundException | SecurityException e)
                {
                    // Ignore it because we'll try File#createTempFile().
                    logger.debug("Could not open output stream on {}", tf, e);
                }
            }
        }

        // Well, we couldn't use a pretty name so try File#createTempFile().
        if ((tfos == null) && !dryRun)
        {
            tf = File.createTempFile(preferredFileName, preferredExtension);
            tfos = new FileOutputStream(tf);
        }

        tempFile[0] = tf;
        return tfos;
    }
}
