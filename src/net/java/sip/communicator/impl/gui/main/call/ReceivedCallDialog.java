/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

import javax.swing.*;

import com.explodingpixels.macwidgets.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The dialog created when an incoming call is received.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class ReceivedCallDialog
    implements  ActionListener,
                CallListener,
                Skinnable
{
    /**
     * The call button name.
     */
    private static final String CALL_BUTTON = "CallButton";

    /**
     * The hangup button name.
     */
    private static final String HANGUP_BUTTON = "HangupButton";

    /**
     * The horizontal gap between buttons.
     */
    private static final int HGAP = 5;

    /**
     * The incoming call to render.
     */
    private final Call incomingCall;

    /**
     * Call button.
     */
    private SIPCommButton callButton;

    /**
     * HandUp button.
     */
    private SIPCommButton hangupButton;

    /**
     * Call label.
     */
    private JLabel callLabel;

    /**
     * The window handling received calls.
     */
    private Window receivedCallWindow;

    /**
     * Indicates that the received call image search has been canceled.
     */
    private boolean imageSeachCanceled = false;

    /**
     * Creates a <tt>ReceivedCallDialog</tt> by specifying the associated call.
     *
     * @param call The associated with this dialog incoming call.
     */
    public ReceivedCallDialog(Call call)
    {
        this.incomingCall = call;

        receivedCallWindow = createWindow();

        this.initComponents();

        OperationSetBasicTelephony<?> telephonyOpSet
            = call.getProtocolProvider()
                .getOperationSet(OperationSetBasicTelephony.class);

        telephonyOpSet.addCallListener(this);
    }

    /**
     * Packs the content of this dialog.
     */
    public void pack()
    {
        receivedCallWindow.pack();
    }

    /**
     * Shows/hides this dialog.
     *
     * @param isVisible indicates if this dialog should be shown or hidden
     */
    public void setVisible(boolean isVisible)
    {
        if (isVisible)
            receivedCallWindow.setLocationRelativeTo(null);

        receivedCallWindow.setVisible(isVisible);
    }

    /**
     * Indicates if this dialog is currently visible.
     *
     * @return <tt>true</tt> if this dialog is currently visible, <tt>false</tt>
     * otherwise
     */
    public boolean isVisible()
    {
        return receivedCallWindow.isVisible();
    }

    /**
     * Creates this received call window.
     *
     * @return the created window
     */
    private Window createWindow()
    {
        Window receivedCallWindow = null;

        if (OSUtils.IS_MAC)
        {
            HudWindow window = new HudWindow();
            window.hideCloseButton();

            JDialog dialog = window.getJDialog();
            dialog.setUndecorated(true);
            dialog.setTitle(
                GuiActivator.getResources()
                    .getSettingsString("service.gui.APPLICATION_NAME")
                + " "
                + GuiActivator.getResources()
                    .getI18NString("service.gui.INCOMING_CALL_STATUS")
                        .toLowerCase());

            receivedCallWindow = window.getJDialog();

            callLabel = HudWidgetFactory.createHudLabel("");
        }
        else
        {
            SIPCommFrame frame = new SIPCommFrame(false);

            frame.setUndecorated(true);

            receivedCallWindow = frame;

            callLabel = new JLabel();
        }

        receivedCallWindow.setAlwaysOnTop(true);

        // prevents dialog window to get unwanted key events and when going
        // on top on linux, it steals focus and if we are accedently
        // writing something and pressing enter a call get answered
        receivedCallWindow.setFocusableWindowState(false);

        return receivedCallWindow;
    }

    /**
     * Initializes all components in this panel.
     */
    private void initComponents()
    {
        JPanel mainPanel = new JPanel(new GridBagLayout());

        // disable html rendering
        callLabel.putClientProperty("html.disable", Boolean.TRUE);

        JPanel buttonsPanel = new TransparentPanel(new GridBagLayout());

        callButton = new SIPCommButton(
            ImageLoader.getImage(ImageLoader.CALL_BUTTON_BG));

        hangupButton = new SIPCommButton(
            ImageLoader.getImage(ImageLoader.HANGUP_BUTTON_BG));

        mainPanel.setPreferredSize(new Dimension(400, 90));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        callButton.setName(CALL_BUTTON);
        hangupButton.setName(HANGUP_BUTTON);

        callButton.addActionListener(this);
        hangupButton.addActionListener(this);

        this.initCallLabel(callLabel);

        receivedCallWindow.add(mainPanel);

        GridBagConstraints mainConstraints = new GridBagConstraints();
        mainConstraints.anchor = GridBagConstraints.WEST;
        mainConstraints.gridx = 0;
        mainConstraints.gridy = 0;
        mainConstraints.weightx = 1;
        mainPanel.add(callLabel, mainConstraints);
        mainConstraints.anchor = GridBagConstraints.CENTER;
        mainConstraints.gridx = 1;
        mainConstraints.weightx = 0;
        mainPanel.add(Box.createHorizontalStrut(HGAP), mainConstraints);
        mainConstraints.anchor = GridBagConstraints.CENTER;
        mainConstraints.gridx = 2;
        mainConstraints.weightx = 0;
        mainPanel.add(buttonsPanel, mainConstraints);

        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridx = 0;
        buttonConstraints.gridy = 0;
        buttonsPanel.add(callButton, buttonConstraints);
        buttonConstraints.gridx = 1;
        buttonsPanel.add(Box.createHorizontalStrut(HGAP));
        buttonConstraints.gridx = 2;
        buttonsPanel.add(hangupButton, buttonConstraints);
    }

    /**
     * Initializes the label of the received call.
     *
     * @param callLabel The label to initialize.
     */
    private void initCallLabel(JLabel callLabel)
    {
        Iterator<? extends CallPeer> peersIter = incomingCall.getCallPeers();

        boolean hasMorePeers = false;
        String text = "";

        ImageIcon imageIcon =
            ImageUtils.scaleIconWithinBounds(ImageLoader
                .getImage(ImageLoader.DEFAULT_USER_PHOTO), 40, 45);

        while (peersIter.hasNext())
        {
            final CallPeer peer = peersIter.next();

            // More peers.
            if (peersIter.hasNext())
            {
                text = callLabel.getText()
                    + peer.getDisplayName() + ", ";

                hasMorePeers = true;
            }
            // Only one peer.
            else
            {
                text = callLabel.getText()
                    + peer.getDisplayName()
                    + " "
                    + GuiActivator.getResources()
                        .getI18NString("service.gui.IS_CALLING");

                byte[] image = CallManager.getPeerImage(peer);

                if (image != null && image.length > 0)
                    imageIcon = ImageUtils.getScaledRoundedIcon(image, 50, 50);
                else
                    new Thread(new Runnable(){

                        public void run()
                        {
                            querySourceContactImage(peer.getAddress());
                        }
                    }).start();
            }
        }

        if (hasMorePeers)
            text += GuiActivator.getResources()
                .getI18NString("service.gui.ARE_CALLING");

        callLabel.setIcon(imageIcon);
        callLabel.setText(text);
    }

    /**
     * Handles <tt>ActionEvent</tt>s triggered by pressing the call or the
     * hangup buttons.
     * @param e The <tt>ActionEvent</tt> to handle.
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton) e.getSource();
        String buttonName = button.getName();

        if (buttonName.equals(CALL_BUTTON))
        {
            CallManager.answerCall(incomingCall);
        }
        else if (buttonName.equals(HANGUP_BUTTON))
        {
            NotificationManager.stopSound(NotificationManager.INCOMING_CALL);

            CallManager.hangupCall(incomingCall);
        }

        receivedCallWindow.dispose();
    }

    /**
     * When call is remotely ended we close this dialog.
     * @param event the <tt>CallEvent</tt> that has been triggered
     */
    public void callEnded(CallEvent event)
    {
        Call sourceCall = event.getSourceCall();

        if (sourceCall.equals(incomingCall))
        {
            receivedCallWindow.dispose();
        }
    }

    /**
     * Indicates that an incoming call has been received.
     */
    public void incomingCallReceived(CallEvent event) {}

    /**
     * Indicates that an outgoing call has been created.
     */
    public void outgoingCallCreated(CallEvent event) {}

    /**
     * Invoked when this dialog is closed.
     */
    protected void close(boolean isEscaped) {}

    /**
     * Searches for a source contact image for the given peer string.
     *
     * @param peerString the address of the peer to search for
     */
    private void querySourceContactImage(String peerString)
    {
        Pattern filterPattern = Pattern.compile(
            "^" + Pattern.quote(peerString) + "$", Pattern.UNICODE_CASE);

        Iterator<ExternalContactSource> contactSources
            = TreeContactList.getContactSources().iterator();

        final Vector<ContactQuery> loadedQueries = new Vector<ContactQuery>();

        // Then we apply the filter on all contact sources.
        while (contactSources.hasNext())
        {
            if (imageSeachCanceled)
                return;

            final ExternalContactSource contactSource = contactSources.next();

            if (contactSource instanceof ExtendedContactSourceService)
            {
                ContactQuery query
                    = ((ExtendedContactSourceService) contactSource)
                        .queryContactSource(filterPattern);

                loadedQueries.add(query);

                List<SourceContact> results = query.getQueryResults();

                if (results != null && !results.isEmpty())
                {
                    Iterator<SourceContact> resultsIter = results.iterator();

                    while (resultsIter.hasNext())
                    {
                        byte[] image = resultsIter.next().getImage();

                        if (image != null && image.length > 0)
                        {
                            setCallImage(image);

                            cancelImageQueries(loadedQueries);
                            return;
                        }
                    }
                }

                query.addContactQueryListener(new ContactQueryListener()
                {
                    public void queryStatusChanged(ContactQueryStatusEvent event)
                    {}

                    public void contactReceived(ContactReceivedEvent event)
                    {
                        SourceContact sourceContact = event.getContact();

                        byte[] image = sourceContact.getImage();

                        if (image != null && image.length > 0)
                        {
                            setCallImage(image);

                            cancelImageQueries(loadedQueries);

                            imageSeachCanceled = true;
                        }
                    }
                });
            }
        }
    }

    /**
     * Cancels the list of loaded <tt>ContactQuery</tt>s.
     *
     * @param loadedQueries the list of queries to cancel
     */
    private void cancelImageQueries(Collection<ContactQuery> loadedQueries)
    {
        Iterator<ContactQuery> queriesIter = loadedQueries.iterator();

        while (queriesIter.hasNext())
        {
            queriesIter.next().cancel();
        }
    }

    /**
     * Sets the image of the incoming call notification.
     *
     * @param image the image to set
     */
    private void setCallImage(byte[] image)
    {
        callLabel.setIcon(
            ImageUtils.getScaledRoundedIcon(image, 50, 50));

        callLabel.repaint();
    }

    /**
     * Reloads icons.
     */
    public void loadSkin()
    {
        callButton.setBackgroundImage(
            ImageLoader.getImage(ImageLoader.CALL_BUTTON_BG));

        hangupButton.setBackgroundImage(
            ImageLoader.getImage(ImageLoader.HANGUP_BUTTON_BG));

        this.initCallLabel(callLabel);
    }
}
