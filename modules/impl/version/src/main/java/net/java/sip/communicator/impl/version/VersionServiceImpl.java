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

import java.io.*;
import java.util.*;
import lombok.extern.slf4j.*;
import org.jitsi.utils.version.*;

/**
 * The version service keeps track of the Jitsi version that we are currently
 * running. Other modules (such as a Help->About dialog) query and use this
 * service in order to show the current application version.
 * <p>
 * This version service implementation is based around the VersionImpl class
 * where all details of the version are statically defined.
 *
 * @author Emil Ivov
 */
@Slf4j
public class VersionServiceImpl
    extends AbstractVersionService
{
    private final Version currentVersion;

    private String applicationName = "Jitsi";

    public VersionServiceImpl()
    {
        int versionMajor = 0;
        int versionMinor = 0;
        String versionBuildId = "unknown";
        String prerelease = "unknown";
        Properties p = new Properties();
        try (InputStream is = VersionServiceImpl.class
            .getResourceAsStream("version.properties"))
        {
            p.load(is);
            applicationName = Objects.requireNonNullElse(
                p.getProperty("version.name"), applicationName);
            versionMajor = Integer.parseInt(p.getProperty("version.major"));
            versionMinor = Integer.parseInt(p.getProperty("version.minor"));
            versionBuildId = p.getProperty("version.buildId");
            prerelease = p.getProperty("version.prerelease");
        }
        catch (Exception e)
        {
            logger.error("Could not read version properties", e);
        }

        currentVersion = new VersionImpl(
            applicationName,
            versionMajor,
            versionMinor,
            versionBuildId,
            prerelease
        );
    }

    /**
     * Returns a <tt>Version</tt> object containing version details of the Jitsi
     * version that we're currently running.
     *
     * @return a <tt>Version</tt> object containing version details of the Jitsi
     * version that we're currently running.
     */
    public Version getCurrentVersion()
    {
        return currentVersion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Version createVersionImpl(int majorVersion,
        int minorVersion,
        String nightlyBuildId)
    {
        return new VersionImpl(
            applicationName, majorVersion, minorVersion, nightlyBuildId);
    }
}
