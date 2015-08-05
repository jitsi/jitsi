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
package net.java.sip.communicator.plugin.dnsconfig;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.table.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.impl.dns.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * TableModel for selectively managing DNSSEC behavior for zones ever requested
 * by Jitsi.
 *
 * @author Ingo Bauersachs
 */
public class DnssecTableModel
    extends DefaultTableModel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private List<String> data = new LinkedList<String>();
    private ResourceManagementService R;
    private ConfigurationService config;

    /**
     * Creates a new instance of this class. Reads all zones from the
     * configuration store.
     */
    public DnssecTableModel()
    {
        BundleContext bc = DnsConfigActivator.bundleContext;
        R = ServiceUtils.getService(bc, ResourceManagementService.class);
        config = ServiceUtils.getService(bc, ConfigurationService.class);
        data = config.getPropertyNamesByPrefix(
            ConfigurableDnssecResolver.PNAME_BASE_DNSSEC_PIN, false);
        Collections.sort(data);
        if(data == null)
            data = new ArrayList<String>(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount()
    {
        if(data == null)
            return 0;
        return data.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getColumnCount()
    {
        return 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName(int columnIndex)
    {
        switch(columnIndex)
        {
            case 0:
                return R.getI18NString("plugin.dnsconfig.dnssec.DOMAIN_NAME");
            case 1:
                return R.getI18NString("plugin.dnsconfig.dnssec.MODE");
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        if(columnIndex < 1)
            return String.class;
        return Component.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return columnIndex == 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        switch(columnIndex)
        {
            case 0:
                return data.get(rowIndex).substring(
                    ConfigurableDnssecResolver.PNAME_BASE_DNSSEC_PIN.length()+1)
                    .split("\\.")[0].replaceAll("__", ".");
            case 1:
                return SecureResolveMode.valueOf(
                    config.getString(data.get(rowIndex)));
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        if(aValue == null)
            return;
        config.setProperty(
            data.get(rowIndex),
            ((SecureResolveMode)aValue).name()
        );
    }
}
