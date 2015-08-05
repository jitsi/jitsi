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
package net.java.sip.communicator.util;

import java.io.*;

import org.jitsi.service.fileaccess.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>AvatarCacheUtils</tt> allows to cache an avatar or to obtain the
 * image of a cached avatar by specifying a contact or an account address.
 *
 * @author Yana Stamcheva
 */
public class AvatarCacheUtils
{
    /**
     * The logger for this class.
     */
    private final static Logger logger
        = Logger.getLogger(AvatarCacheUtils.class);

    /**
     * The name (i.e. not the whole path) of the directory in which the avatar
     * files are to be cached for later reuse.
     */
    private final static String AVATAR_DIR = "avatarcache";

    /**
     *  Characters and their replacement in created folder names
     */
    private final static String[][] ESCAPE_SEQUENCES = new String[][]
    {
        {"&", "&_amp"},
        {"/", "&_sl"},
        {"\\\\", "&_bs"},   // the char \
        {":", "&_co"},
        {"\\*", "&_as"},    // the char *
        {"\\?", "&_qm"},    // the char ?
        {"\"", "&_pa"},     // the char "
        {"<", "&_lt"},
        {">", "&_gt"},
        {"\\|", "&_pp"}     // the char |
    };

    /**
     * Returns the bytes of the avatar image stored for the account
     * corresponding to the given protocol provider.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * account avatar image we're looking for
     * @return the bytes of the avatar image stored for the account
     * corresponding to the given protocol provider
     */
    public static byte[] getCachedAvatar(
                                    ProtocolProviderService protocolProvider)
    {
        String avatarPath = getCachedAvatarPath(protocolProvider);

        byte[] cachedAvatar = getLocallyStoredAvatar(avatarPath);

        /*
         * Caching a zero-length avatar happens but such an avatar isn't
         * very useful.
         */
        if ((cachedAvatar != null) && (cachedAvatar.length > 0))
            return cachedAvatar;

        return null;
    }

    /**
     * Returns the bytes of the avatar image stored for the account
     * corresponding to the given protocol provider.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * account avatar image we're looking for
     * @return the bytes of the avatar image stored for the account
     * corresponding to the given protocol provider
     */
    public static byte[] getCachedAvatar(Contact protocolContact)
    {
        String avatarPath = getCachedAvatarPath(protocolContact);

        byte[] cachedAvatar = getLocallyStoredAvatar(avatarPath);

        /*
         * Caching a zero-length avatar happens but such an avatar isn't
         * very useful.
         */
        if ((cachedAvatar != null) && (cachedAvatar.length > 0))
            return cachedAvatar;

        return null;
    }

    /**
     * Returns the bytes of the avatar image stored for the account
     * corresponding to the given protocol provider.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * account avatar image we're looking for
     * @return the bytes of the avatar image stored for the account
     * corresponding to the given protocol provider
     */
    public static String getCachedAvatarPath(
                                    ProtocolProviderService protocolProvider)
    {
        return AVATAR_DIR
            + File.separator
            + escapeSpecialCharacters(
                    protocolProvider.getAccountID().getAccountUniqueID())
            + File.separator
            + escapeSpecialCharacters(
                    protocolProvider.getAccountID().getAccountUniqueID());
    }

    /**
     * Returns the bytes of the avatar image stored for the account
     * corresponding to the given protocol provider.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * account avatar image we're looking for
     * @return the bytes of the avatar image stored for the account
     * corresponding to the given protocol provider
     */
    public static String getCachedAvatarPath(Contact protocolContact)
    {
        return AVATAR_DIR
            + File.separator
            + escapeSpecialCharacters(
                protocolContact
                    .getProtocolProvider()
                        .getAccountID().getAccountUniqueID())
            + File.separator
            + escapeSpecialCharacters(protocolContact.getAddress());
    }

    /**
     * Returns the bytes of the avatar image stored for the account
     * corresponding to the given protocol provider.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * account avatar image we're looking for
     * @return the bytes of the avatar image stored for the account
     * corresponding to the given protocol provider
     */
    public static String getCachedAvatarPath(
                                    ProtocolProviderService protocolProvider,
                                    String contactAddress)
    {
        return AVATAR_DIR
            + File.separator
            + escapeSpecialCharacters(
                protocolProvider.getAccountID().getAccountUniqueID())
            + File.separator
            + escapeSpecialCharacters(contactAddress);
    }

    /**
     * Returns the avatar image corresponding to the given avatar path.
     *
     * @param avatarPath The path to the lovally stored avatar.
     * @return the avatar image corresponding to the given avatar path.
     */
    private static byte[] getLocallyStoredAvatar(String avatarPath)
    {
        try
        {
            File avatarFile
                = UtilActivator
                    .getFileAccessService()
                        .getPrivatePersistentFile(avatarPath,
                            FileCategory.CACHE);

            if(avatarFile.exists())
            {
                FileInputStream avatarInputStream
                    = new FileInputStream(avatarFile);
                byte[] bs = null;

                try
                {
                    int available = avatarInputStream.available();

                    if (available > 0)
                    {
                        bs = new byte[available];
                        avatarInputStream.read(bs);
                    }
                }
                finally
                {
                    avatarInputStream.close();
                }
                if (bs != null)
                    return bs;
            }
        }
        catch (Exception ex)
        {
            logger.error(
                    "Could not read avatar image from file " + avatarPath,
                    ex);
        }
        return null;
    }

    /**
     * Replaces the characters that we must escape used for the created
     * filename.
     *
     * @param id the <tt>String</tt> which is to have its characters escaped
     * @return a <tt>String</tt> derived from the specified <tt>id</tt> by
     * escaping characters
     */
    private static String escapeSpecialCharacters(String id)
    {
        String resultId = id;

        for (int j = 0; j < ESCAPE_SEQUENCES.length; j++)
        {
            resultId = resultId.
                replaceAll(ESCAPE_SEQUENCES[j][0], ESCAPE_SEQUENCES[j][1]);
        }
        return resultId;
    }

    /**
     * Stores avatar bytes in the given <tt>Contact</tt>.
     *
     * @param protoContact The contact in which we store the avatar.
     * @param avatarBytes The avatar image bytes.
     */
    public static void cacheAvatar( Contact protoContact,
                                    byte[] avatarBytes)
    {
        String avatarDirPath
            = AVATAR_DIR
                + File.separator
                + escapeSpecialCharacters(
                        protoContact
                            .getProtocolProvider()
                                .getAccountID().getAccountUniqueID());
        String avatarFileName
            = escapeSpecialCharacters(protoContact.getAddress());

        cacheAvatar(avatarDirPath, avatarFileName, avatarBytes);
    }
    /**
     * Stores avatar bytes for the account corresponding to the given
     * <tt>protocolProvider</tt>.
     *
     * @param protocolProvider the protocol provider corresponding to the
     * account, which avatar we're storing
     * @param avatarBytes the avatar image bytes
     */
    public static void cacheAvatar( ProtocolProviderService protocolProvider,
                                    byte[] avatarBytes)
    {
        String accountUID
            = protocolProvider.getAccountID().getAccountUniqueID();

        String avatarDirPath
            = AVATAR_DIR
                + File.separator
                + escapeSpecialCharacters(accountUID);

        String avatarFileName = escapeSpecialCharacters(accountUID);

        cacheAvatar(avatarDirPath, avatarFileName, avatarBytes);
    }

    /**
     * Stores avatar bytes for the account corresponding to the given
     * <tt>protocolProvider</tt>.
     *
     * @param avatarDirPath the directory in which the file will be stored
     * @param avatarFileName the name of the avatar file
     * @param avatarBytes the avatar image bytes
     */
    private static void cacheAvatar(String avatarDirPath,
                                    String avatarFileName,
                                    byte[] avatarBytes)
    {
        File avatarDir = null;
        File avatarFile = null;
        try
        {
            FileAccessService fileAccessService
                = UtilActivator.getFileAccessService();

            avatarDir
                = fileAccessService.getPrivatePersistentDirectory(
                        avatarDirPath, FileCategory.CACHE);
            avatarFile
                = fileAccessService.getPrivatePersistentFile(
                        new File(avatarDirPath, avatarFileName).toString(),
                        FileCategory.CACHE);

            if(!avatarFile.exists())
            {
                if (!avatarDir.exists() && !avatarDir.mkdirs())
                {
                    throw
                        new IOException(
                                "Failed to create directory: "
                                    + avatarDir.getAbsolutePath());
                }

                if (!avatarFile.createNewFile())
                {
                    throw
                        new IOException(
                                "Failed to create file"
                                    + avatarFile.getAbsolutePath());
                }
            }

            FileOutputStream fileOutStream = new FileOutputStream(avatarFile);

            try
            {
                fileOutStream.write(avatarBytes);
                fileOutStream.flush();
            }
            finally
            {
                fileOutStream.close();
            }
        }
        catch (Exception ex)
        {
            logger.error(
                    "Failed to store avatar. dir =" + avatarDir
                        + " file=" + avatarFile,
                    ex);
        }
    }
}
