/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

/**
 * Utility fields for OS detection.
 *
 * @author Sebastien Vincent
 * @author Lubomir Marinov
 */
public class OSUtils
{

    /** <tt>true</tt> if architecture is 32 bit. */
    public static final boolean IS_32_BIT;

    /** <tt>true</tt> if architecture is 64 bit. */
    public static final boolean IS_64_BIT;

    /** <tt>true</tt> if OS is Linux. */
    public static final boolean IS_LINUX;

    /** <tt>true</tt> if OS is Linux 32-bit. */
    public static final boolean IS_LINUX32;

    /** <tt>true</tt> if OS is Linux 64-bit. */
    public static final boolean IS_LINUX64;

    /** <tt>true</tt> if OS is MacOSX. */
    public static final boolean IS_MAC;

    /** <tt>true</tt> if OS is MacOSX 32-bit. */
    public static final boolean IS_MAC32;

    /** <tt>true</tt> if OS is MacOSX 64-bit. */
    public static final boolean IS_MAC64;
    
    /** <tt>true</tt> if OS is Windows. */
    public static final boolean IS_WINDOWS;

    /** <tt>true</tt> if OS is Windows 32-bit. */
    public static final boolean IS_WINDOWS32;

    /** <tt>true</tt> if OS is Windows 64-bit. */
    public static final boolean IS_WINDOWS64;

    /** <tt>true</tt> if OS is Windows Vista. */
    public static final boolean IS_WINDOWS_VISTA;

    /** <tt>true</tt> if OS is FreeBSD. */
    public static final boolean IS_FREEBSD;

    static
    {
        // OS
        String osName = System.getProperty("os.name");

        if (osName == null)
        {
            IS_LINUX = false;
            IS_MAC = false;
            IS_WINDOWS = false;
            IS_WINDOWS_VISTA = false;
            IS_FREEBSD = false;
        }
        else if (osName.startsWith("Linux"))
        {
            IS_LINUX = true;
            IS_MAC = false;
            IS_WINDOWS = false;
            IS_WINDOWS_VISTA = false;
            IS_FREEBSD = false;
        }
        else if (osName.startsWith("Mac"))
        {
            IS_LINUX = false;
            IS_MAC = true;
            IS_WINDOWS = false;
            IS_WINDOWS_VISTA = false;
            IS_FREEBSD = false;
        }
        else if (osName.startsWith("Windows"))
        {
            IS_LINUX = false;
            IS_MAC = false;
            IS_WINDOWS = true;
            IS_WINDOWS_VISTA = (osName.indexOf("Vista") != -1);
            IS_FREEBSD = false;
        }
        else if (osName.startsWith("FreeBSD"))
        {
            IS_LINUX = false;
            IS_MAC = false;
            IS_WINDOWS = false; 
            IS_WINDOWS_VISTA = (osName.indexOf("Vista") != -1);
            IS_FREEBSD = true;
        }
        else
        {
            IS_LINUX = false;
            IS_MAC = false;
            IS_WINDOWS = false;
            IS_WINDOWS_VISTA = false;
            IS_FREEBSD = false;
        }

        // arch i.e. x86, amd64
        String osArch = System.getProperty("sun.arch.data.model");

        if(osArch == null)
        {
            IS_32_BIT = true;
            IS_64_BIT = false;
        }
        else if (osArch.indexOf("32") != -1)
        {
            IS_32_BIT = true;
            IS_64_BIT = false;
        }
        else if (osArch.indexOf("64") != -1)
        {
            IS_32_BIT = false;
            IS_64_BIT = true;
        }
        else
        {
            IS_32_BIT = false;
            IS_64_BIT = false;
        }

        // OS && arch
        IS_LINUX32 = IS_LINUX && IS_32_BIT;
        IS_LINUX64 = IS_LINUX && IS_64_BIT;
        IS_MAC32 = IS_MAC && IS_32_BIT;
        IS_MAC64 = IS_MAC && IS_64_BIT;
        IS_WINDOWS32 = IS_WINDOWS && IS_32_BIT;
        IS_WINDOWS64 = IS_WINDOWS && IS_64_BIT;
    }
}
