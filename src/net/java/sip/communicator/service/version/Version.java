/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.version;

/**
 * Contains version information of the SIP Communicator instance that we're
 * currently running.
 *
 * @author Emil Ivov
 */
public interface Version extends Comparable<Version>
{
    /**
     * Returns the version major of the current SIP Communicator version. In an
     * example 2.3.1 version string 2 is the version major. The version major
     * number changes when a relatively extensive set of new features and
     * possibly rearchitecturing have been applied to the SIP Communicator.
     *
     * @return the version major integer.
     */
    public int getVersionMajor();

    /**
     * Returns the version minor of the current SIP Communicator version. In an
     * example 2.3.1 version string 3 is the version minor. The version minor
     * number changes after adding enhancements and possibly new features to a
     * given SIP Communicator version.
     *
     * @return the version minor integer.
     */
    public int getVersionMinor();

    /**
     * Returns the version revision number of the current SIP Communicator
     * version. In an example 2.3.1 version string 1 is the revision number.
     * The version revision number number changes after applying bug fixes and
     * possible some small enhancements to a given SIP Communicator version.
     *
     * @return the version revision number.
     */
    public int getVersionRevision();

    /**
     * Indicates if this SIP Communicator version corresponds to a nightly build
     * of a repository snapshot or to an official SIP Communicator release.
     *
     * @return true if this is a build of a nightly repository snapshot and
     * false if this is an official SIP Communicator release.
     */
    public boolean isNightly();

    /**
     * If this is a nightly build, returns the build identifies (e.g.
     * nightly-2007.12.07-06.45.17). If this is not a nightly build SIP
     * Communicator version, the method returns null.
     *
     * @return a String containing a nightly build identifier or null if
     */
    public String getNightlyBuildID();

    /**
     * Indicates whether this version represents a prerelease (i.e. a
     * non-complete release like an alpha, beta or release candidate version).
     * @return true if this version represents a prerelease and false otherwise.
     */
    public boolean isPreRelease();

    /**
     * Returns the version prerelease ID of the current SIP Communicator version
     * and null if this version is not a prerelease. Version pre-release id-s
     * and version revisions are exclusive, so in case this version is a pre-
     * release the revision will bereturn null
     *
     * @return a String containing the version prerelease ID.
     */
    public String getPreReleaseID();

    /**
     * Compares another <tt>Version</tt> object to this one and returns a
     * negative, zero or a positive integer if this version instance represents
     * respectively an earlier, same, or later version as the one indicated
     * by the <tt>version</tt> parameter.
     *
     * @param version the <tt>Version</tt> instance that we'd like to compare
     * to this one.
     *
     * @return a negative integer, zero, or a positive integer as this object
     * represents a version that is earlier, same, or more recent than the one
     * referenced by the <tt>version</tt> parameter.
     */
    public int compareTo(Version version);

    /**
     * Compares the <tt>version</tt> parameter to this version and returns true
     * if and only if both reference the same SIP Communicator version and
     * false otherwise.
     *
     * @param version the version instance that we'd like to compare with this
     * one.
     * @return true if and only the version param references the same
     * SIP Communicator version as this Version instance and false otherwise.
     */
    public boolean equals(Object version);

    /**
     * Returns the name of the application that we're currently running. Default
     * MUST be SIP Communicator.
     *
     * @return the name of the application that we're currently running. Default
     * MUST be SIP Communicator.
     */
    public String getApplicationName();

    /**
     * Returns a String representation of this Version instance. If you'd just
     * like to obtain the version of SIP Communicator so that you could display
     * it (e.g. in a Help->About dialog) then all you need is calling this
     * method.
     *
     * @return a major.minor.revision[.build] String containing the complete
     * SIP Communicator version.
     */
    public String toString();
}
