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
package net.java.sip.communicator.impl.resources.util;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import net.java.sip.communicator.service.resources.*;

/**
 * Class for building of skin bundles from zip files.
 * @author Adam Netocny
 */
public class SkinJarBuilder
{
    /**
     * Creates bundle from zip file.
     * @param srv <tt>ResourcePack</tt> containing class files and manifest
     *            for the SkinResourcePack.
     * @param zip Zip file with skin contents.
     * @return Jar <tt>File</tt>.
     * @throws Exception When something goes wrong.
     */
    public static File createBundleFromZip(File zip, ResourcePack srv)
        throws Exception
    {
        File tmpDir = unzipIntoTmp(zip);
        File tmpDir2 = findBase(tmpDir);

        if (tmpDir2 == null)
        {
            tmpDir2 = tmpDir;
        }

        if (!test(tmpDir2))
        {
            deleteDir(tmpDir);
            throw new Exception(
                "Zip file doesn't contain all necessary files and folders.");
        }
        cpTmp(tmpDir2, srv);
        File jar = insertIntoZip(tmpDir2);
        deleteDir(tmpDir);

        return jar;
    }

    /**
     * Creates a copy of skinresources.jar in temp folder.
     *
     * @param unzippedBase Base dir where files should appear.
     * @param srv <tt>ResourcePack</tt> containing class files and manifest
     *            for the SkinResourcePack.
     * @throws IOException Is thrown if the jar cannot be located or if a file
     * operation goes wrong.
     */
    private static void cpTmp(File unzippedBase, ResourcePack srv)
        throws IOException
    {
        InputStream in = srv.getClass().getClassLoader()
            .getResourceAsStream(
                "resources/skinresourcepack/SkinResourcePack.class");

        File dest = new File(unzippedBase, "net" + File.separatorChar + "java"
            + File.separatorChar + "sip" + File.separatorChar
            + "communicator" + File.separatorChar + "plugin"
            + File.separatorChar + "skinresourcepack");

        if(!dest.mkdirs())
        {
            throw new IOException("Unable to build resource pack.");
        }

        OutputStream out = new FileOutputStream(
            new File(dest, "SkinResourcePack.class"));

        copy(in, out);

        in = srv.getClass().getClassLoader()
            .getResourceAsStream(
                "resources/skinresourcepack/skinresourcepack.manifest.mf");

        dest = new File(unzippedBase, "META-INF");

        if(!dest.mkdirs()) {
            throw new IOException("Unable to build resource pack.");
        }

        out = new FileOutputStream(new File(dest, "MANIFEST.MF"));

        copy(in, out);
    }

    /**
     * Simple file copy operation.
     * @param in <tt>InputStream</tt> for the source.
     * @param out <tt>OutputStream</tt> for the destination file.
     * @throws IOException Is thrown if the jar cannot be located or if a file
     * operation goes wrong.
     */
    private static void copy(InputStream in, OutputStream out) throws IOException
    {
        byte[] buf = new byte[1024];
        int len;

        while ((len = in.read(buf)) > 0)
        {
            out.write(buf, 0, len);
        }

        in.close();
        out.close();
    }

    /**
     * Unzips a specified <tt>File</tt> to temp folder.
     *
     * @param zip ZIP <tt>File</tt> to be unzipped.
     * @return temporary directory with the content of the ZIP file.
     * @throws IOException Is thrown if a file operation goes wrong.
     */
    private static File unzipIntoTmp(File zip)
        throws IOException
    {
        File dest = File.createTempFile("zip", null);

        if (!dest.delete())
            throw new IOException("Cannot unzip given zip file");
        if (!dest.mkdirs())
            throw new IOException("Cannot unzip given zip file");

        ZipFile archive = new ZipFile(zip);

        try
        {
            Enumeration<? extends ZipEntry> e = archive.entries();

            if (e.hasMoreElements())
            {
                byte[] buffer = new byte[8192];

                while (e.hasMoreElements())
                {
                    ZipEntry entry = e.nextElement();
                    File file = new File(dest, entry.getName());

                    if (entry.isDirectory() && !file.exists())
                    {
                        file.mkdirs();
                    }
                    else
                    {
                        File parentFile = file.getParentFile();

                        if (!parentFile.exists())
                            parentFile.mkdirs();

                        InputStream in = archive.getInputStream(entry);

                        try
                        {
                            BufferedOutputStream out
                                = new BufferedOutputStream(
                                        new FileOutputStream(file));

                            try
                            {
                                int read;
    
                                while (-1 != (read = in.read(buffer)))
                                    out.write(buffer, 0, read);
                            }
                            finally
                            {
                                out.close();
                            }
                        }
                        finally
                        {
                            in.close();
                        }
                    }
                }
            }
        }
        finally
        {
            archive.close();
        }
        return dest;
    }

    /**
     * Inserts files into ZIP file.
     *
     * @param tmpDir Folder which contains the data.
     * @return <tt>File</tt> containing reference of the jar file.
     * @throws IOException Is thrown if a file operation goes wrong.
     */
    private static File insertIntoZip(File tmpDir)
        throws IOException
    {
        File jar = File.createTempFile("skinresourcepack", ".jar");

        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(jar));

        zipDir(tmpDir.getAbsolutePath(), out);

        out.close();

        return jar;
    }

    /**
     * Zips the content of a folder.
     * @param dir2zip Path to the directory with the data to be stored.
     * @param zos Opened <tt>ZipOutputStream</tt> in which will be information
     * stored.
     * @throws IOException Is thrown if a file operation goes wrong.
     */
    private static void zipDir(String dir2zip, ZipOutputStream zos)
        throws IOException
    {
        File directory = new File(dir2zip);
        zip(directory, directory, zos);
    }

    /**
     * Zips a file.
     * @param directory Path to the dir with the data to be stored.
     * @param base Base path for cutting paths into zip entries.
     * @param zos Opened <tt>ZipOutputStream</tt> in which will be information
     * stored.
     * @throws IOException Is thrown if a file operation goes wrong.
     */
    private static final void zip(File directory, File base, ZipOutputStream zos)
        throws IOException
    {
        File[] files = directory.listFiles();
        byte[] buffer = new byte[8192];
        int read = 0;
        for (int i = 0, n = files.length; i < n; i++)
        {
            if (files[i].isDirectory())
            {
                zip(files[i], base, zos);
            }
            else
            {
                FileInputStream in = new FileInputStream(files[i]);
                ZipEntry entry = new ZipEntry(files[i].getPath().substring(
                        base.getPath().length() + 1));
                zos.putNextEntry(entry);
                while (-1 != (read = in.read(buffer))) {
                    zos.write(buffer, 0, read);
                }
                in.close();
            }
        }
    }

    /**
     * Deletes a directory with all its sub-directories.
     *
     * @param tmp the directory to be deleted
     */
    private static void deleteDir(File tmp)
    {
        if (tmp.exists())
        {
            File[] files = tmp.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                if (files[i].isDirectory())
                {
                    deleteDir(files[i]);
                }
                else
                {
                    files[i].delete();
                }
            }
            tmp.delete();
        }
    }

    /**
     * Tests if the content of a folder has the same structure as the skin
     * content.
     *
     * @param tmpDir Directory to be tested.
     * @return <tt>true</tt> - if the directory contains valid skin, else
     * <tt>false</tt>.
     */
    private static boolean test(File tmpDir)
    {
        boolean colors = false;
        boolean images = false;
        boolean styles = false;

        File[] list = tmpDir.listFiles();

        if (list == null)
        {
            return false;
        }

        for (File f : list)
        {
            if (f.getName().equals("info.properties"))
            {
                if (!f.isFile())
                {
                    return false;
                }
            }
            else if (f.getName().equals("colors"))
            {
                if (f.isFile())
                {
                    return false;
                }
                File[] ff = f.listFiles();
                if (ff == null)
                {
                    return false;
                }

                for (File x : ff)
                {
                    if (x.getName().equals("colors.properties"))
                    {
                        colors = true;
                    }
                }
            }
            else if (f.getName().equals("images"))
            {
                if (f.isFile())
                {
                    return false;
                }
                File[] ff = f.listFiles();
                if (ff == null)
                {
                    return false;
                }

                for (File x : ff)
                {
                    if (x.getName().equals("images.properties"))
                    {
                        images = true;
                    }
                }
            }
            else if (f.getName().equals("styles"))
            {
                if (f.isFile())
                {
                    return false;
                }
                File[] ff = f.listFiles();
                if (ff == null)
                {
                    return false;
                }

                for (File x : ff)
                {
                    if (x.getName().equals("styles.properties"))
                    {
                        styles = true;
                    }
                }
            }
        }
        return styles || (colors || images);
    }

    /**
     * Moves to top level directory for unziped files. (e.g.
     * /dir/info.propreties will be changed to /info.properties.)
     * @param tmpDir Directory in which is the skin unzipped.
     * @return the top level directory
     */
    private static File findBase(File tmpDir)
    {
        File[] list = tmpDir.listFiles();

        if (list == null)
        {
            return null;
        }

        boolean test = false;

        for (File f : list)
        {
            if (f.getName().equals("info.properties"))
            {
                if (f.isFile())
                {
                    test = true;
                }
            }
        }

        if (!test)
        {
            if (list.length != 0)
            {
                File tmp = null;
                for (File f : list)
                {
                    if(f.isDirectory())
                    {
                        File tmp2 = findBase(f);
                        if(tmp2 != null && tmp == null)
                        {
                            tmp = tmp2;
                        }
                    }
                }
                return tmp;
            }
            else
            {
                return null;
            }
        }

        return tmpDir;
    }
}
