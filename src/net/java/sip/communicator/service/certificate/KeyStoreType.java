/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.certificate;

/**
 * Data object for KeyStore configurations. Primarily used during adding/
 * editing client certificate configurations.
 * 
 * @author Ingo Bauersachs
 */
public class KeyStoreType
{
    private String name;
    private String[] fileExtensions;
    private boolean hasKeyStorePassword;

    /**
     * Creates a new instance of this class.
     * @param name the display name of the keystore type.
     * @param fileExtensions known file name extensions (including the dot)
     * @param hasKeyStorePassword
     */
    public KeyStoreType(String name, String[] fileExtensions,
        boolean hasKeyStorePassword)
    {
        this.name = name;
        this.fileExtensions = fileExtensions;
        this.hasKeyStorePassword = hasKeyStorePassword;
    }

    @Override
    public String toString()
    {
        return name;
    }

    /**
     * Gets the display name.
     * @return the display name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Gets the known file name extensions.
     * @return Known file name extensions (including the dot).
     */
    public String[] getFileExtensions()
    {
        return fileExtensions;
    }

    /**
     * Flag that indicates if the keystore supports passwords.
     * @return <tt>true</tt> if the keystore supports passwords, <tt>false</tt>
     *         otherwise.
     */
    public boolean hasKeyStorePassword()
    {
        return hasKeyStorePassword;
    }
}