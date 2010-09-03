/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.resources.util;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Class for building of skin bundles from zip files.
 * @author Adam Netocny, CircleTech, s.r.o.
 */
public class SkinJarBuilder
{
    /**
     * Creates bundle from zip file.
     * @param zip Zip file with skin contents.
     * @return Jar <tt>File</tt>.
     * @throws Exception When something goes wrong.
     */
    public static File createBundleFromZip(File zip)
        throws Exception
    {
        File tmpDir = unzipIntoTmp(zip);

        if (!test(tmpDir))
        {
            deleteDir(tmpDir);
            throw new Exception(
                "Zip file doesn't contain all necessary files and folders.");
        }
        File jar = cpTmp();
        insertIntoZip(jar, tmpDir);
        deleteDir(tmpDir);
        return jar;
    }

    private static File cpTmp()
        throws IOException
    {
        File jar = new File(System.getProperty("user.dir"),
                            "sc-bundles/skinresources.jar");

        if (!jar.exists())
        {
            throw new IOException("Cannot find skinresources.jar file");
        }

        File tmp = File.createTempFile("skinresources", ".jar");

        InputStream in = new FileInputStream(jar);

        OutputStream out = new FileOutputStream(tmp);

        byte[] buf = new byte[1024];
        int len;

        while ((len = in.read(buf)) > 0)
        {
            out.write(buf, 0, len);
        }

        in.close();
        out.close();

        return tmp;
    }

    private static File unzipIntoTmp(File zip)
        throws Exception
    {
        File dest = File.createTempFile("zip", null);

        if (!dest.delete())
        {
            throw new IOException("Cannot unzip given zip file");
        }

        if (!dest.mkdirs())
        {
            throw new IOException("Cannot unzip given zip file");
        }

        ZipFile archive = new ZipFile(zip);
        Enumeration<? extends ZipEntry> e = archive.entries();
        while (e.hasMoreElements())
        {
            ZipEntry entry = (ZipEntry) e.nextElement();
            File file = new File(dest, entry.getName());
            if (entry.isDirectory() && !file.exists())
            {
                file.mkdirs();
            }
            else
            {
                if (!file.getParentFile().exists())
                {
                    file.getParentFile().mkdirs();
                }
                InputStream in = archive.getInputStream(entry);
                BufferedOutputStream out
                    = new BufferedOutputStream(new FileOutputStream(file));
                byte[] buffer = new byte[8192];
                int read;
                while (-1 != (read = in.read(buffer)))
                {
                    out.write(buffer, 0, read);
                }
                in.close();
                out.close();
            }
        }

        return dest;
    }

    private static void insertIntoZip(File jar, File tmpDir)
        throws IOException
    {
        File tempFile = File.createTempFile(jar.getName(), null);
        tempFile.delete();

        boolean renameOk = jar.renameTo(tempFile);
        if (!renameOk)
        {
            throw new IOException("Error moving file " + jar.getAbsolutePath()
                                    + " to " + tempFile.getAbsolutePath());
        }

        byte[] buf = new byte[8192];
        ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(jar));

        ZipEntry entry = zin.getNextEntry();
        while (entry != null)
        {
            String name = entry.getName();
            out.putNextEntry(new ZipEntry(name));

            int len;
            while ((len = zin.read(buf)) > 0)
            {
                out.write(buf, 0, len);
            }
            entry = zin.getNextEntry();
        }
        zin.close();

        tempFile.delete();

        zipDir(tmpDir.getAbsolutePath(), out);

        out.close();
    }

    private static void zipDir(String dir2zip, ZipOutputStream zos)
        throws IOException
    {
        File directory = new File(dir2zip);
        zip(directory, directory, zos);
    }

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
            else
            {
                return false;
            }
        }
        return styles || (colors || images);
    }
}
