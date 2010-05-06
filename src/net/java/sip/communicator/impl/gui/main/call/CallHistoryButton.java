/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>CallHistoryButton</tt> is the button shown on the top of the contact
 * list.
 * @author Yana Stamcheva
 */
public class CallHistoryButton
    extends SIPCommTextButton
    implements MissedCallsListener
{
    /**
     * The history icon.
     */
    private final Image historyImage
        = ImageLoader.getImage(ImageLoader.CALL_HISTORY_BUTTON);

    /**
     * The history pressed icon.
     */
    private final Image pressedImage
        = ImageLoader.getImage(ImageLoader.CALL_HISTORY_BUTTON_PRESSED);

    /**
     * Indicates if the history is visible.
     */
    private boolean isHistoryVisible = false;

    /**
     * Indicates if this button currently shows the number of missed calls or
     * the just the history icon.
     */
    private boolean isMissedCallView = false;

    /**
     * The tool tip shown when there are missed calls.
     */
    private final static String missedCallsToolTip
        = GuiActivator.getResources().getI18NString(
            "service.gui.MISSED_CALLS_TOOL_TIP");

    /**
     * The tool tip shown by default over the history button. 
     */
    private final static String callHistoryToolTip
        = GuiActivator.getResources().getI18NString(
            "service.gui.CALL_HISTORY_TOOL_TIP");

    /**
     * The tool tip shown when we're in history view.
     */
    private final static String showContactListToolTip
        = GuiActivator.getResources().getI18NString(
            "service.gui.SHOW_CONTACT_LIST_TOOL_TIP");

    /**
     * Creates a <tt>CallHistoryButton</tt>.
     */
    public CallHistoryButton()
    {
        super("");

        this.setBgImage(historyImage);

        CallManager.setMissedCallsListener(this);

        this.setPreferredSize(new Dimension(29, 22));
        this.setForeground(Color.WHITE);
        this.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        this.setFont(getFont().deriveFont(Font.BOLD, 10f));
        this.setToolTipText(callHistoryToolTip);
        this.setBackground(new Color(255, 255, 255, 160));

        this.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (isHistoryVisible && !isMissedCallView)
                {
                    TreeContactList.searchFilter
                        .setSearchSourceType(SearchFilter.DEFAULT_SOURCE);
                    GuiActivator.getContactList()
                        .setDefaultFilter(TreeContactList.presenceFilter);
                    GuiActivator.getContactList().applyDefaultFilter();

                    isHistoryVisible = false;
                }
                else
                {
                    TreeContactList.searchFilter
                        .setSearchSourceType(SearchFilter.HISTORY_SOURCE);
                    GuiActivator.getContactList()
                        .setDefaultFilter(TreeContactList.historyFilter);
                    GuiActivator.getContactList().applyDefaultFilter();

                    CallManager.clearMissedCalls();

                    isHistoryVisible = true;
                }
                setHistoryView();

                GuiActivator.getContactList().requestFocusInWindow();
                repaint();
            }
        });
    }

    /**
     * Indicates that missed calls count has changed.
     * @param newCallCount the new call count
     */
    public void missedCallCountChanged(int newCallCount)
    {
        if (newCallCount > 0)
        {
            setMissedCallsView(newCallCount);
        }
        else if (newCallCount <= 0)
        {
            setHistoryView();
        }

        this.revalidate();
        this.repaint();
    }

    /**
     * Sets the history view.
     */
    private void setHistoryView()
    {
        isMissedCallView = false;

        if (isHistoryVisible)
        {
            setBgImage(pressedImage);
            setToolTipText(showContactListToolTip);
        }
        else
        {
            setBgImage(historyImage);
            setToolTipText(callHistoryToolTip);
        }
        setText("");
    }

    /**
     * Sets the missed calls view of this button.
     * @param callCount the missed calls count
     */
    private void setMissedCallsView(int callCount)
    {
        isMissedCallView = true;
        this.setBgImage(null);
        this.setToolTipText(missedCallsToolTip);
        this.setBackground(new Color(200, 0, 0));
        this.setText(new Integer(callCount).toString());
    }
}