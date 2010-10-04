/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.fileaccess;

import java.io.*;

/**
 * A service used to provide the basic functionality required to access the
 * underlying file system.
 *
 * Note: Never store unencrypted sensitive information, such as passwords,
 * personal data, credit card numbers, etc..
 *
 * @author Alexander Pelov
 */
public interface FileAccessService {

    /**
     * The key of the configuration property containing the user home dir - if
     * it is not defined the system property is used
     */
    public static final String CONFPROPERTYKEY_USER_HOME
        = "net.java.sip.communicator.fileaccess.USER_HOME";

    /**
     * This method returns a created temporary file. After you close this file
     * it is not guaranteed that you will be able to open it again nor that it
     * will contain any information.
     *
     * Note: DO NOT store unencrypted sensitive information in this file
     *
     * @return The created temporary file
     * @throws IOException
     *             If the file cannot be created
     */
    File getTemporaryFile() throws IOException;

    /**
     * This method returns a created temporary directory. Any file you create in
     * it will be a temporary file.
     *
     * Note: If there is no opened file in this directory it may be deleted at
     * any time. Note: DO NOT store unencrypted sensitive information in this
     * directory
     *
     * @return The created directory
     * @throws IOException
     *             If the directory cannot be created
     */
    File getTemporaryDirectory() throws IOException;

    /**
     * This method returns a file specific to the current user. It may not
     * exist, but it is guaranteed that you will have the sufficient rights to
     * create it.
     *
     * This file should not be considered secure because the implementor may
     * return a file accesible to everyone. Generaly it will reside in current
     * user's homedir, but it may as well reside in a shared directory.
     *
     * Note: DO NOT store unencrypted sensitive information in this file
     *
     * @param fileName
     *            The name of the private file you wish to access
     * @return The file
     * @throws Exception
     *             Thrown if there is no suitable location for the persistent
     *             file
     */
    File getPrivatePersistentFile(String fileName) throws Exception;

    /**
     * This method creates a directory specific to the current user.
     *
     * This directory should not be considered secure because the implementor
     * may return a directory accessible to everyone. Generally, it will reside
     * in current user's homedir, but it may as well reside in a shared
     * directory.
     *
     * It is guaranteed that you will be able to create files in it.
     *
     * Note: DO NOT store unencrypted sensitive information in this file
     *
     * @param dirName
     *            The name of the private directory you wish to access.
     * @return The created directory.
     * @throws Exception
     *             Thrown if there is no suitable location for the persistent
     *             directory.
     */
    File getPrivatePersistentDirectory(String dirName) throws Exception;

    /**
     * This method creates a directory specific to the current user.
     *
     * {@link #getPrivatePersistentDirectory(String)}
     *
     * @param dirNames
     *            The name of the private directory you wish to access.
     * @return The created directory.
     * @throws Exception
     *             Thrown if there is no suitable location for the persistent
     *             directory.
     */
    File getPrivatePersistentDirectory(String[] dirNames) throws Exception;

    /**
     * Returns the default download directory depending on the operating system.
     *
     * @return the default download directory depending on the operating system
     * @throws IOException if it I/O error occurred
     */
    File getDefaultDownloadDirectory() throws IOException;

    /**
     * Creates a failsafe transaction which can be used to safely store
     * informations into a file.
     *
     * @param file The file concerned by the transaction, null if file is null.
     *
     * @return A new failsafe transaction related to the given file.
     */
    FailSafeTransaction createFailSafeTransaction(File file);
}
