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
package net.java.sip.communicator.plugin.certconfig;

import java.beans.*;
import java.util.*;

import javax.swing.table.*;

import net.java.sip.communicator.service.certificate.*;

import org.jitsi.service.resources.*;

/**
 * Backing data model for a JTable that displays the client certificate
 * configuration entries.
 *
 * @author Ingo Bauersachs
 */
public class CertConfigTableModel
    extends AbstractTableModel
    implements PropertyChangeListener
{
    private static final long serialVersionUID = -6369348252411082340L;
    private CertificateService cvs;
    private List<CertificateConfigEntry> model;
    private ResourceManagementService R = CertConfigActivator.R;

    /**
     * Constructor.
     */
    public CertConfigTableModel()
    {
        CertConfigActivator.getConfigService().addPropertyChangeListener(this);
        cvs = CertConfigActivator.getCertService();
        model = cvs.getClientAuthCertificateConfigs();
    }

    public int getRowCount()
    {
        return model.size();
    }

    public int getColumnCount()
    {
        return 3;
    }

    public Object getValueAt(int rowIndex, int columnIndex)
    {
        switch(columnIndex)
        {
        case 0:
            return model.get(rowIndex).getDisplayName();
        case 1:
            return model.get(rowIndex).getAlias();
        case 2:
            return model.get(rowIndex).getKeyStoreType();
        }
        return null;
    }

    /**
     * Get <tt>CertificateConfigEntry</tt> located at <tt>rowIndex</tt>.
     *
     * @param rowIndex row index
     * @return <tt>CertificateConfigEntry</tt>
     */
    public CertificateConfigEntry getItem(int rowIndex)
    {
        return model.get(rowIndex);
    }

    @Override
    public String getColumnName(int column)
    {
        switch(column)
        {
        case 0:
            return R.getI18NString("service.gui.DISPLAY_NAME");
        case 1:
            return R.getI18NString("plugin.certconfig.ALIAS");
        case 2:
            return R.getI18NString("plugin.certconfig.KEYSTORE_TYPE");
        }
        return super.getColumnName(column);
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getPropertyName().startsWith(
            CertificateService.PNAME_CLIENTAUTH_CERTCONFIG_BASE))
        {
            model = cvs.getClientAuthCertificateConfigs();
            super.fireTableDataChanged();
        }
    }
}
