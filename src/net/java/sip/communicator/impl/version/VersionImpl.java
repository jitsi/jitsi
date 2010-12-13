/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.version;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.version.*;

import org.osgi.framework.ServiceReference;

/**
 * A static implementation of the Version interface.
 */
public class VersionImpl
    implements Version
{
    /**
     * The version major of the current SIP Communicator version. In an
     * example 2.3.1 version string 2 is the version major. The version major
     * number changes when a relatively extensive set of new features and
     * possibly rearchitecturing have been applied to the SIP Communicator.
     */
    public static final int VERSION_MAJOR = 1;

    /**
     * The version major of the current SIP Communicator version. In an
     * example 2.3.1 version string 2 is the version major. The version major
     * number changes when a relatively extensive set of new features and
     * possibly rearchitecturing have been applied to the SIP Communicator.
     */
    public static final int VERSION_MINOR = 0;

    /**
     * Indicates whether this version represents a prerelease (i.e. a
     * non-complete release like an alpha, beta or release candidate version).
     */
    public static final boolean IS_PRE_RELEASE_VERSION  = true;

    /**
     * Returns the version prerelease ID of the current SIP Communicator version
     * and null if this version is not a prerelease. Version pre-release id-s
     * and version revisions are exclusive, so in case this version is a pre-
     * release the revision will bereturn null.
     */
    public static final String PRE_RELEASE_ID = "alpha6";

    /**
     * Indicates if this SIP Communicator version corresponds to a nightly build
     * of a repository snapshot or to an official SIP Communicator release.
     */
    public static final boolean IS_NIGHTLY_BUILD = true;

    /**
     * The default name of this application.
     */
    public static final String DEFAULT_APPLICATION_NAME = "SIP Communicator";

    /**
     * The name of this application.
     */
    public static String applicationName = null;

    /**
     * Returns the VersionImpl instance describing the current version of
     * SIP Communicator.
     */
    public static final VersionImpl CURRENT_VERSION = new VersionImpl();

    /**
     * The resource management service.
     */
    private static ResourceManagementService resourcesService;

    /**
     * Returns the version major of the current SIP Communicator version. In an
     * example 2.3.1 version string 2 is the version major. The version major
     * number changes when a relatively extensive set of new features and
     * possibly rearchitecturing have been applied to the SIP Communicator.
     *
     * @return the version major String.
     */
    public int getVersionMajor()
    {
        return VERSION_MAJOR;
    }

    /**
     * Returns the version minor of the current SIP Communicator version. In an
     * example 2.3.1 version string 3 is the version minor. The version minor
     * number changes after adding enhancements and possibly new features to a
     * given SIP Communicator version.
     *
     * @return the version minor integer.
     */
    public int getVersionMinor()
    {
        return VERSION_MINOR;
    }

    /**
     * Returns the version revision number of the current SIP Communicator
     * version. In an example 2.3.1 version string 1 is the revision number.
     * The version revision number number changes after applying bug fixes and
     * possible some small enhancements to a given SIP Communicator version.
     *
     * @return the version revision number or -1 if this version of
     * SIP Communicator corresponds to a pre-release.
     */
    public int getVersionRevision()
    {
        if(isPreRelease())
            return -1;

        try
        {
            return Integer.valueOf(RevisionID.REVISION_ID);
        } catch (NumberFormatException numberFormatException)
        {
            // if we cannot parse the revision number return -1, so we skip it
            return -1;
        }
    }

    /**
     * Indicates if this SIP Communicator version corresponds to a nightly build
     * of a repository snapshot or to an official SIP Communicator release.
     *
     * @return true if this is a build of a nightly repository snapshot and
     * false if this is an official SIP Communicator release.
     */
    public boolean isNightly()
    {
        return IS_NIGHTLY_BUILD;
    }

    /**
     * If this is a nightly build, returns the build identifies (e.g.
     * nightly-2007.12.07-06.45.17). If this is not a nightly build SIP
     * Communicator version, the method returns null.
     *
     * @return a String containing a nightly build identifier or null if this is
     * a release version and therefore not a nightly build
     */
    public String getNightlyBuildID()
    {
        if(!isNightly())
            return null;

        return NightlyBuildID.BUILD_ID;
    }

    /**
     * Indicates whether this version represents a prerelease (i.e. a
     * non-complete release like an alpha, beta or release candidate version).
     * @return true if this version represents a prerelease and false otherwise.
     */
    public boolean isPreRelease()
    {
        return IS_PRE_RELEASE_VERSION;
    }

    /**
     * Returns the version prerelease ID of the current SIP Communicator version
     * and null if this version is not a prerelease. Version pre-release id-s
     * and version revisions are exclusive, so in case this version is a pre-
     * release the revision will bereturn null
     *
     * @return a String containing the version prerelease ID.
     */
    public String getPreReleaseID()
    {
        if(!isPreRelease())
            return null;

        return PRE_RELEASE_ID;
    }

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
    public int compareTo(Version version)
    {
        //our versioning system is built to produce lexicographically ordered
        //names so we could simply return the comparison of the version strings.
        return toString().compareTo( (version == null)
                                        ? "null"
                                        : version.toString());
    }

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
    public boolean equals(Object version)
    {
        //simply compare the version strings
        return toString().equals( (version == null)
                                        ? "null"
                                        : version.toString());

    }

    /**
     * Returns a String representation of this Version instance in the generic
     * form of major.minor.revision[.nightly.build.id]. If you'd just
     * like to obtain the version of SIP Communicator so that you could display
     * it (e.g. in a Help->About dialog) then all you need is calling this
     * method.
     *
     * @return a major.minor.revision[.build] String containing the complete
     * SIP Communicator version.
     */
    public String toString()
    {
        StringBuffer versionStringBuff = new StringBuffer();

        versionStringBuff.append(Integer.toString(getVersionMajor()));
        versionStringBuff.append(".");
        versionStringBuff.append(Integer.toString(getVersionMinor()));

        if(isPreRelease())
        {
            versionStringBuff.append("-");
            versionStringBuff.append(getPreReleaseID());
        }
        else
        {
            int rev = getVersionRevision();
            if(rev >= 0)
            {
                versionStringBuff.append(".");
                versionStringBuff.append(Integer.toString(rev));
            }
        }

        if(isNightly())
        {
            versionStringBuff.append("-");
            versionStringBuff.append(getNightlyBuildID());
        }

        return versionStringBuff.toString();
    }

    /**
     * Returns the VersionImpl instance describing the current version of
     * SIP Communicator.
     *
     * @return the VersionImpl instance describing the current version of
     * SIP Communicator.
     */
    public static final VersionImpl currentVersion()
    {
        return CURRENT_VERSION;
    }

    /**
     * Returns the name of the application that we're currently running. Default
     * MUST be SIP Communicator.
     *
     * @return the name of the application that we're currently running. Default
     * MUST be SIP Communicator.
     */
    public String getApplicationName()
    {
        if(applicationName == null)
        {
            try
            {
                if (resourcesService == null)
                {
                    ServiceReference serviceReference =
                        VersionActivator.bundleContext
                        .getServiceReference(
                                ResourceManagementService.class.getName());

                    if(serviceReference == null)
                        return null;

                    resourcesService =
                        (ResourceManagementService)VersionActivator.
                            bundleContext.getService(serviceReference);
                }

                applicationName =
                    resourcesService.getSettingsString(
                        "service.gui.APPLICATION_NAME");
            }
            catch (Exception e)
            {
                // if resource bundle is not found or the key is missing
                // return the defautl name
                applicationName = DEFAULT_APPLICATION_NAME;
            }
        }

        return applicationName;
    }
}
