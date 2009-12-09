/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

/**
 * Utility methods for OS detection.
 *
 * @author Sebastien Vincent
 * @author Lubomir Marinov
 */
public class OSUtils
{

    /**
     * Full OS name.
     */
    private static final String osName = System.getProperty("os.name");

    /**
     * OS Architecture (x86, x64, ...).
     */
    private static final String osArch
        = System.getProperty("sun.arch.data.model");

    /**
     * Check whether or not platform is Windows.
     *
     * @return <tt>true</tt> if OS is Windows, <tt>false</tt> otherwise
     */
    public static boolean isWindows()
    {
        return (osName != null) && osName.startsWith("Windows");
    }

    /**
     * Check whether or not platform is Windows.
     *
     * @param name OS version name (i.e. Vista) or <tt>null</tt> if OS version
     * name is not important
     * @return <tt>true</tt> if OS is Windows, <tt>false</tt> otherwise
     */
    public static boolean isWindows(String name)
    {
        return
            isWindows() && ((name == null) || (osName.indexOf(name) != -1));
    }

    /**
     * Check whether or not platform is Windows 64-bit.
     *
     * @return <tt>true</tt> if OS is Windows 64-bit, <tt>false</tt> otherwise
     */
    public static boolean isWindows64()
    {
        return isWindows() && isArch64();
    }

    /**
     * Check whether or not platform is Linux.
     *
     * @return <tt>true</tt> if OS is Linux, <tt>false</tt> otherwise
     */
    public static boolean isLinux()
    {
        return (osName != null) && osName.startsWith("Linux");
    }

    /**
     * Check whether or not platform is Linux 32-bit.
     *
     * @return <tt>true</tt> if OS is Linux 32-bit, <tt>false</tt> otherwise
     */
    public static boolean isLinux32()
    {
        return isLinux() && isArch32();
    }

    /**
     * Check whether or not platform is Linux 64-bit.
     *
     * @return <tt>true</tt> if OS is Linux 64-bit, <tt>false</tt> otherwise
     */
    public static boolean isLinux64()
    {
        return isLinux() && isArch64();
    }

    /**
     * Check whether or not platform is Mac.
     *
     * @return <tt>true</tt> if OS is Mac, <tt>false</tt> otherwise
     */
    public static boolean isMac()
    {
        return
            (osName != null)
                && (osName.startsWith("Mac")
                    || (osName.indexOf("Darwin") != -1));
    }

    /**
     * Check whether or not platform is MacOS 64-bit.
     * @return true if OS is Mac 64-bit, false otherwise
     */
    public static boolean isMac64()
    {
        return isMac() && isArch64();
    }

    /**
     * Check whether or not platform is 32-bit.
     * @return true if OS is 32-bit, false otherwise
     */
    public static boolean isArch32()
    {
        return (osArch.indexOf("32") != -1);
    }

    /**
     * Check whether or not platform is 64-bit.
     *
     * @return <tt>true</tt> if OS is 64-bit, <tt>false</tt> otherwise
     */
    public static boolean isArch64()
    {
        return (osArch.indexOf("64") != -1);
    }
}
