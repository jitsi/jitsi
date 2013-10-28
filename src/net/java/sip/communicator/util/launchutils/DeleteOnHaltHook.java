/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.launchutils;

import java.io.*;
import java.util.*;

/**
 * In the fashion of <tt>java.io.DeleteOnExitHook</tt>, provides a way to delete
 * files when <tt>Runtime.halt(int)</tt> is to be invoked.
 *
 * @author Lyubomir Marinov
 */
public class DeleteOnHaltHook
{
    /**
     * The set of files to be deleted when <tt>Runtime.halt(int)</tt> is to be
     * invoked.
     */
    private static Set<String> files = new LinkedHashSet<String>();

    /**
     * Adds a file to the set of files to be deleted when
     * <tt>Runtime.halt(int)</tt> is to be invoked.
     *
     * @param file the name of the file to be deleted when
     * <tt>Runtime.halt(int)</tt> is to be invoked
     */
    public static synchronized void add(String file)
    {
        if (files == null)
            throw new IllegalStateException("Shutdown in progress.");
        else
            files.add(file);
    }

    /**
     * Deletes the files which have been registered for deletion when
     * <tt>Runtime.halt(int)</tt> is to be invoked.
     */
    public static void runHooks()
    {
        Set<String> files;

        synchronized (DeleteOnHaltHook.class)
        {
            files = DeleteOnHaltHook.files;
            DeleteOnHaltHook.files = null;
        }

        if (files != null)
        {
            List<String> toBeDeleted = new ArrayList<String>(files);

            Collections.reverse(toBeDeleted);
            for (String filename : toBeDeleted)
                new File(filename).delete();
        }
    }

    /** Prevents the initialization of <tt>DeleteOnHaltHook</tt> instances. */
    private DeleteOnHaltHook() {}
}
