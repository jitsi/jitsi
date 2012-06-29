/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.dnsconfig;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.table.*;

import org.jitsi.service.configuration.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.dns.*;

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
    public int getRowCount()
    {
        if(data == null)
            return 0;
        return data.size();
    }

    /**
     * {@inheritDoc}
     */
    public int getColumnCount()
    {
        return 2;
    }

    /**
     * {@inheritDoc}
     */
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
    public Class<?> getColumnClass(int columnIndex)
    {
        if(columnIndex < 1)
            return String.class;
        return Component.class;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return columnIndex == 1;
    }

    /**
     * {@inheritDoc}
     */
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
