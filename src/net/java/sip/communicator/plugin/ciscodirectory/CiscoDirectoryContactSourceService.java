/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.ciscodirectory;

import static net.java.sip.communicator.plugin.ciscodirectory
        .CiscoDirectoryActivator.*;

import java.util.regex.*;

import net.java.sip.communicator.service.contactsource.*;

/**
 * This ContactSourceService runs contact queries against a Cisco directory.
 *
 * @author Fabien Cortina <fabien.cortina@gmail.com>
 */
final class CiscoDirectoryContactSourceService
        implements ExtendedContactSourceService, PrefixedContactSourceService
{
    private final DirectorySettings directorySettings;

    /**
     * Creates a ContactSourceService for a Cisco directory.
     *
     * @param directorySettings the configuration of the directory.
     */
    CiscoDirectoryContactSourceService(DirectorySettings directorySettings)
    {
        this.directorySettings = directorySettings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getType()
    {
        return DEFAULT_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName()
    {
        return _txt("plugin.ciscodirectory.TITLE");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContactQuery createContactQuery(String queryString)
    {
        return createContactQuery(queryString, 10);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContactQuery createContactQuery(String queryString, int contactCount)
    {
        Pattern pattern;
        try
        {
            pattern = Pattern.compile(queryString);
        }
        catch (PatternSyntaxException pse)
        {
            pattern = Pattern.compile(Pattern.quote(queryString));
        }

        if (pattern != null)
        {
            return new CiscoDirectoryContactQuery(this, pattern);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContactQuery createContactQuery(Pattern queryString)
    {
        return new CiscoDirectoryContactQuery(this, queryString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIndex()
    {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPhoneNumberPrefix()
    {
        return getDirectorySettings().getPrefix();
    }

    /**
     * @return the read/write directory settings.
     */
    DirectorySettings getDirectorySettings()
    {
        return directorySettings;
    }
}
