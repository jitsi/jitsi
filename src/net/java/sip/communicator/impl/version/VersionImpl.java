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
package net.java.sip.communicator.impl.version;

import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.jitsi.service.version.util.*;

/**
 * A static implementation of the Version interface.
 */
public class VersionImpl
    extends AbstractVersion
{
    /**
     * The version major of the current Jitsi version. In an
     * example 2.3.1 version string 2 is the version major. The version major
     * number changes when a relatively extensive set of new features and
     * possibly rearchitecturing have been applied to the Jitsi.
     */
    public static final int VERSION_MAJOR = 2;

    /**
     * The version major of the current Jitsi version. In an
     * example 2.3.1 version string 2 is the version major. The version major
     * number changes when a relatively extensive set of new features and
     * possibly rearchitecturing have been applied to the Jitsi.
     */
    public static final int VERSION_MINOR = 9;

    /**
     * Indicates whether this version represents a prerelease (i.e. a
     * non-complete release like an alpha, beta or release candidate version).
     */
    public static final boolean IS_PRE_RELEASE_VERSION  = false;

    /**
     * Returns the version prerelease ID of the current Jitsi version
     * and null if this version is not a prerelease.
     */
    public static final String PRE_RELEASE_ID = "beta1";

    /**
     * Indicates if this Jitsi version corresponds to a nightly build
     * of a repository snapshot or to an official Jitsi release.
     */
    public static final boolean IS_NIGHTLY_BUILD = true;

    /**
     * The default name of this application.
     */
    public static final String DEFAULT_APPLICATION_NAME = "Jitsi";

    /**
     * The name of this application.
     */
    private static String applicationName = null;

    /**
     * Returns the VersionImpl instance describing the current version of
     * Jitsi.
     */
    public static final VersionImpl CURRENT_VERSION = new VersionImpl();

    /**
     * Creates version object with default (current) values.
     */
    private VersionImpl()
    {
        super(VERSION_MAJOR, VERSION_MINOR, NightlyBuildID.BUILD_ID);
    }

    /**
     * Creates version object with custom major, minor and nightly build id.
     *
     * @param majorVersion the major version to use.
     * @param minorVersion the minor version to use.
     * @param nightlyBuildID the nightly build id value for new version object.
     */
    private VersionImpl(int majorVersion,
                        int minorVersion,
                        String nightlyBuildID)
    {
        super(majorVersion, minorVersion, nightlyBuildID);
    }

    /**
     * Indicates if this Jitsi version corresponds to a nightly build
     * of a repository snapshot or to an official Jitsi release.
     *
     * @return true if this is a build of a nightly repository snapshot and
     * false if this is an official Jitsi release.
     */
    public boolean isNightly()
    {
        return IS_NIGHTLY_BUILD;
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
     * Returns the version prerelease ID of the current Jitsi version
     * and null if this version is not a prerelease.
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
     * Returns the VersionImpl instance describing the current version of
     * Jitsi.
     *
     * @return the VersionImpl instance describing the current version of
     * Jitsi.
     */
    public static final VersionImpl currentVersion()
    {
        return CURRENT_VERSION;
    }

    /**
     * Returns the VersionImpl instance describing the version with the
     * parameters supplied.
     *
     * @param majorVersion the major version to use.
     * @param minorVersion the minor version to use.
     * @param nightlyBuildID the nightly build id value for new version object.
     * @return the VersionImpl instance describing the version with parameters
     * supplied.
     */
    public static final VersionImpl customVersion(
        int majorVersion,
        int minorVersion,
        String nightlyBuildID)
    {
        return new VersionImpl(majorVersion, minorVersion, nightlyBuildID);
    }

    /**
     * Returns the name of the application that we're currently running. Default
     * MUST be Jitsi.
     *
     * @return the name of the application that we're currently running. Default
     * MUST be Jitsi.
     */
    public String getApplicationName()
    {
        if (applicationName == null)
        {
            try
            {
                /*
                 * XXX There is no need to have the ResourceManagementService
                 * instance as a static field of the VersionImpl class because
                 * it will be used once only anyway.
                 */
                ResourceManagementService resources
                    = ServiceUtils.getService(
                            VersionActivator.getBundleContext(),
                            ResourceManagementService.class);

                if (resources != null)
                {
                    applicationName
                        = resources.getSettingsString(
                                "service.gui.APPLICATION_NAME");
                }
            }
            catch (Exception e)
            {
                // if resource bundle is not found or the key is missing
                // return the default name
            }
            finally
            {
                if (applicationName == null)
                    applicationName = DEFAULT_APPLICATION_NAME;
            }
        }
        return applicationName;
    }
}
