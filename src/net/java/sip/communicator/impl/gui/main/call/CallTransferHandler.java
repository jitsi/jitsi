/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.datatransfer.*;
import java.awt.im.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * A TransferHandler that we use to handle dropping of <tt>UIContact</tt>s or
 * simple string addresses to an existing <tt>Call</tt>. Dropping of a such data
 * in the <tt>CallDialog</tt> would result in the creation of a call conference.
 *
 * @author Yana Stamcheva
 */
public class CallTransferHandler
    extends ExtendedTransferHandler
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The data flavor used when transferring <tt>UIContact</tt>s.
     */
    protected static final DataFlavor uiContactDataFlavor
        = new DataFlavor(UIContact.class, "UIContact");

    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(CallTransferHandler.class);

    /**
     * The call corresponding to the transfer.
     */
    private final Call call;

    /**
     * Creates an instance of <tt>CallTransferHandler</tt> by specifying the
     * <tt>call</tt>, to which dragged callees will be added.
     * @param call the call to which the dragged callees will be added
     */
    public CallTransferHandler(Call call)
    {
        this.call = call;
    }

    /**
     * Indicates whether a component will accept an import of the given
     * set of data flavors prior to actually attempting to import it. We return
     * <tt>true</tt> to indicate that the transfer with at least one of the
     * given flavors would work and <tt>false</tt> to reject the transfer.
     * <p>
     * @param comp component
     * @param flavor the data formats available
     * @return  true if the data can be inserted into the component, false
     * otherwise
     * @throws NullPointerException if <code>support</code> is {@code null}
     */
    public boolean canImport(JComponent comp, DataFlavor flavor[])
    {
        for (int i = 0, n = flavor.length; i < n; i++)
        {
            if (flavor[i].equals(DataFlavor.stringFlavor)
                || flavor[i].equals(uiContactDataFlavor))
            {
                if (comp instanceof JPanel)
                {
                    return true;
                }

                return false;
            }
        }

        return false;
    }

    /**
     * Handles transfers to the chat panel from the clip board or a
     * DND drop operation. The <tt>Transferable</tt> parameter contains the
     * data that needs to be imported.
     * <p>
     * @param comp  the component to receive the transfer;
     * @param t the data to import
     * @return  true if the data was inserted into the component and false
     * otherwise
     */
    public boolean importData(JComponent comp, Transferable t)
    {
        if (t.isDataFlavorSupported(uiContactDataFlavor))
        {
            Object o = null;

            try
            {
                o = t.getTransferData(uiContactDataFlavor);
            }
            catch (UnsupportedFlavorException e)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Failed to drop meta contact.", e);
            }
            catch (IOException e)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Failed to drop meta contact.", e);
            }

            if (o instanceof ContactNode)
            {
                UIContact uiContact
                    = ((ContactNode) o).getContactDescriptor();

                ProtocolProviderService callProvider
                    = call.getProtocolProvider();

                Iterator<UIContactDetail> contactDetails
                    = uiContact.getContactDetailsForOperationSet(
                        OperationSetBasicTelephony.class).iterator();

                String callee = null;
                ProtocolProviderService provider = null;

                while (contactDetails.hasNext())
                {
                    UIContactDetail detail = contactDetails.next();

                    ProtocolProviderService detailProvider
                        = detail.getPreferredProtocolProvider(
                            OperationSetBasicTelephony.class);

                    if (detailProvider != null
                        /*&& detailProvider.equals(callProvider)*/)
                    {
                        callee = detail.getAddress();
                        provider = detailProvider;
                        break;
                    }
                }

                if (callee != null)
                {
                    Map<ProtocolProviderService, List<String>> callees =
                        new HashMap<ProtocolProviderService, List<String>>();
                    List<String> lst = new ArrayList<String>();

                    lst.add(callee);
                    callees.put(provider, lst);
                    CallManager.inviteToCrossProtocolConferenceCall(
                        callees, call);

                    return true;
                }
                else
                    new ErrorDialog(null,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.ERROR"),
                        GuiActivator.getResources().getI18NString(
                            "service.gui.CALL_NOT_SUPPORTING_PARTICIPANT",
                            new String[]{
                                callProvider.getAccountID().getService(),
                                callProvider.getAccountID().getUserID(),
                                uiContact.getDisplayName()}))
                    .showDialog();
            }
        }
        else if (t.isDataFlavorSupported(DataFlavor.stringFlavor))
        {
            InputContext inputContext = comp.getInputContext();
            if (inputContext != null)
            {
                inputContext.endComposition();
            }

            try
            {
                BufferedReader reader = new BufferedReader(
                    DataFlavor.stringFlavor.getReaderForText(t));

                final StringBuffer buffToCall = new StringBuffer();

                String str;
                while ((str = reader.readLine()) != null)
                    buffToCall.append(str);

                CallManager.inviteToConferenceCall(
                    new String[]{buffToCall.toString()}, call);

                return true;
            }
            catch (UnsupportedFlavorException e)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Failed to drop string.", e);
            }
            catch (IOException e)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Failed to drop string.", e);
            }
        }
        return false;
    }
}
