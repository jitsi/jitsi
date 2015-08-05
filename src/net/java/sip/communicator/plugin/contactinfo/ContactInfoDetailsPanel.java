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
package net.java.sip.communicator.plugin.contactinfo;

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.BinaryDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.BirthDateDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.CalendarDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.EmailAddressDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.FirstNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenderDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.LastNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.LocaleDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.MiddleNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.PhoneNumberDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.TimeZoneDetail;

/**
 * The right side panel of ContactInfoDialog. Shows one tab of a summary of
 * contact information for the selected subcontact, and has an extended tab
 * listing all of the details.
 *
 * @author Adam Goldstein
 * @author Yana Stamcheva
 */
public class ContactInfoDetailsPanel
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The tabbed pane containing the two different tabs for details.
     */
    private final JTabbedPane tabbedPane = new SIPCommTabbedPane();

    /**
     * The operation set giving access to the server stored contact details.
     */
    private OperationSetServerStoredContactInfo contactInfoOpSet;

    /**
     * The currently selected sub-contact we are displaying information about.
     */
    private Contact contact;

    /**
     * The default width of hte avater area.
     */
    private static final int AVATAR_AREA_WIDTH = 105;

    /**
     * The default height of hte avater area.
     */
    private static final int AVATAR_AREA_HEIGHT = 130;

    /**
     * Construct a tabbed pane that will have one tab with a summary of info for
     * the selected subcontact and one tab for all of the extended details.
     */
    public ContactInfoDetailsPanel()
    {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.setPreferredSize(new Dimension(400, 300));

        this.tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    /**
     * Retrieve and display the information for the newly selected contact, c.
     *
     * @param c the sub-contact we are now focusing on.
     */
    public void loadContactDetails(Contact c)
    {
        this.contact = c;

        ProtocolProviderService pps = contact.getProtocolProvider();
        contactInfoOpSet
            = pps.getOperationSet(OperationSetServerStoredContactInfo.class);

        this.removeAll();

        if (contactInfoOpSet == null || !pps.isRegistered())
        {
            JPanel unsupportedPanel = createUnsupportedPanel();

            this.add(unsupportedPanel);

            this.revalidate();
            this.repaint();

            return;
        }

        this.tabbedPane.removeAll();

        ImageIcon icon =
            new ImageIcon(contact.getProtocolProvider().getProtocolIcon()
                .getIcon(ProtocolIcon.ICON_SIZE_16x16));

        JPanel summaryPanel = createSummaryInfoPanel();

        JPanel extendedPanel = createExtendedInfoPanel();

        JScrollPane extendedScrollPane = new JScrollPane(extendedPanel);

        this.tabbedPane.addTab(
            Resources.getString("service.gui.SUMMARY"), icon,
            summaryPanel,
            Resources.getString(
                "plugin.contactinfo.CONTACT_SUMMARY_DESCRIPTION")
                + contact.getDisplayName());

        this.tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

        this.tabbedPane.addTab(
            Resources.getString("plugin.accountinfo.EXTENDED"), icon,
            extendedScrollPane,
            Resources.getString(
                "plugin.contactinfo.CONTACT_EXTENDED_DESCRIPTION")
                + contact.getDisplayName());

        this.tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);

        this.add(tabbedPane);

        this.revalidate();
        this.repaint();
    }

    /**
     * Creates the panel that indicates to the user that the currently selected
     * contact does not support server stored contact info.
     *
     * @return the panel that is added and shows a message that the selected
     *         sub-contact does not have the operation set for server stored
     *         contact info supported.
     */
    private JPanel createUnsupportedPanel()
    {
        JTextArea unsupportedTextArea = new JTextArea(
            Resources.getString("service.gui.CONTACT_INFO_NOT_SUPPORTED"));

        unsupportedTextArea.setEditable(false);
        unsupportedTextArea.setLineWrap(true);

        JPanel unsupportedPanel = new TransparentPanel(new BorderLayout());

        unsupportedPanel.add(unsupportedTextArea);

        return unsupportedPanel;
    }

    /**
     * Creates a panel that can be added as the summary tab that displays the
     * following details: -
     * <p>
     * Avatar(Contact image) - FirstNameDetail - MiddleNameDetail -
     * LastNameDetail - BirthdateDetail (and calculate age) - GenderDetail -
     * EmailAddressDetail - PhoneNumberDetail. All other details will be* added
     * to our list of extended details.
     *
     * @return the panel that will be added as the summary tab.
     */
    private JPanel createSummaryInfoPanel()
    {
        JPanel summaryPanel = new TransparentPanel();

        summaryPanel.setLayout(new BorderLayout(10, 5));
        summaryPanel.setSize(this.getWidth(), this.getHeight());

        // Create the avatar panel.
        JPanel avatarPanel = new TransparentPanel();

        avatarPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        byte[] bytes = this.contact.getImage();

        ImageIcon scaledImage = null;
        // If the user has a contact image, let's use it. If not, add the
        // default
        if (bytes != null)
        {
            scaledImage = ImageUtils.getScaledRoundedIcon(
                bytes,
                AVATAR_AREA_WIDTH,
                AVATAR_AREA_HEIGHT
                );
        }
        else
            scaledImage =
                ImageUtils.getScaledRoundedIcon(Resources
                    .getImage("service.gui.DEFAULT_USER_PHOTO"),
                    AVATAR_AREA_WIDTH, AVATAR_AREA_HEIGHT);

        JLabel label = new JLabel(scaledImage);
        label.setVerticalAlignment(JLabel.CENTER);
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setPreferredSize(new Dimension(
                    AVATAR_AREA_WIDTH,
                    AVATAR_AREA_HEIGHT)
                );
        avatarPanel.add(label);
        summaryPanel.add(avatarPanel, BorderLayout.WEST);

        // Create the summary details panel.
        JPanel detailsPanel = new TransparentPanel();
        detailsPanel.setLayout(new BorderLayout());
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        summaryPanel.add(detailsPanel);

        // Labels panel.
        JPanel labelsPanel = new TransparentPanel(new GridLayout(0, 1, 5, 5));
        labelsPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.FIRST_NAME")));
        labelsPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.MIDDLE_NAME")));
        labelsPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.LAST_NAME")));
        labelsPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.GENDER")));
        labelsPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.BDAY")));
        labelsPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.AGE")));
        labelsPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.EMAIL")));
        labelsPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.PHONE")));

        detailsPanel.add(labelsPanel, BorderLayout.WEST);

        // Values panel.
        JPanel valuesPanel = new TransparentPanel(new GridLayout(0, 1, 5, 5));

        detailsPanel.add(valuesPanel, BorderLayout.CENTER);

        Iterator<GenericDetail> contactDetails;
        GenericDetail genericDetail;

        // First name details.
        contactDetails =
            contactInfoOpSet.getDetails(contact, FirstNameDetail.class);

        String firstNameDetail = "";
        while (contactDetails.hasNext())
        {
            genericDetail = contactDetails.next();

            firstNameDetail =
                firstNameDetail + " " + genericDetail.getDetailValue();
        }

        if (firstNameDetail.equals(""))
            firstNameDetail
                = Resources.getString("plugin.contactinfo.NOT_SPECIFIED");

        valuesPanel.add(new JLabel(firstNameDetail));

        // Middle name details.
        contactDetails =
            contactInfoOpSet.getDetails(contact, MiddleNameDetail.class);

        String middleNameDetail = "";
        while (contactDetails.hasNext())
        {
            genericDetail = contactDetails.next();
            middleNameDetail =
                middleNameDetail + " " + genericDetail.getDetailValue();
        }

        if (middleNameDetail.trim().equals(""))
            middleNameDetail
                = Resources.getString("plugin.contactinfo.NOT_SPECIFIED");

        valuesPanel.add(new JLabel(middleNameDetail));

        // Last name details.
        contactDetails =
            contactInfoOpSet.getDetails(contact, LastNameDetail.class);

        String lastNameDetail = "";
        while (contactDetails.hasNext())
        {
            genericDetail = contactDetails.next();

            lastNameDetail =
                lastNameDetail + " " + genericDetail.getDetailValue();
        }

        if (lastNameDetail.trim().equals(""))
            lastNameDetail
                = Resources.getString("plugin.contactinfo.NOT_SPECIFIED");

        valuesPanel.add(new JLabel(lastNameDetail));

        // Gender details.
        contactDetails =
            contactInfoOpSet.getDetails(contact, GenderDetail.class);

        String genderDetail = "";
        while (contactDetails.hasNext())
        {
            genericDetail = contactDetails.next();
            genderDetail = genderDetail + " " + genericDetail.getDetailValue();
        }

        if (genderDetail.trim().equals(""))
            genderDetail
                = Resources.getString("plugin.contactinfo.NOT_SPECIFIED");

        valuesPanel.add(new JLabel(genderDetail));

        // Birthday details.
        contactDetails =
            contactInfoOpSet.getDetails(contact, BirthDateDetail.class);

        String birthDateDetail = "";
        String ageDetail = "";
        if (contactDetails.hasNext())
        {
            genericDetail = contactDetails.next();

            Calendar calendarDetail =
                (Calendar) genericDetail.getDetailValue();

            Date birthDate = calendarDetail.getTime();
            DateFormat dateFormat = DateFormat.getDateInstance();

            birthDateDetail = dateFormat.format(birthDate).trim();

            Calendar c = Calendar.getInstance();
            int age = c.get(Calendar.YEAR) - calendarDetail.get(Calendar.YEAR);

            if (c.get(Calendar.MONTH) < calendarDetail.get(Calendar.MONTH))
                age--;

            ageDetail = Integer.toString(age).trim();
        }

        if (birthDateDetail.equals(""))
            birthDateDetail
                = Resources.getString("plugin.contactinfo.NOT_SPECIFIED");

        if (ageDetail.equals(""))
            ageDetail = Resources.getString("plugin.contactinfo.NOT_SPECIFIED");

        valuesPanel.add(new JLabel(birthDateDetail));
        valuesPanel.add(new JLabel(ageDetail));

        // Email details.
        contactDetails =
            contactInfoOpSet.getDetails(contact, EmailAddressDetail.class);

        String emailDetail = "";
        while (contactDetails.hasNext())
        {
            genericDetail = contactDetails.next();
            emailDetail = emailDetail + " " + genericDetail.getDetailValue();
        }

        if (emailDetail.trim().equals(""))
            emailDetail
                = Resources.getString("plugin.contactinfo.NOT_SPECIFIED");

        valuesPanel.add(new JLabel(emailDetail));

        // Phone number details.
        contactDetails =
            contactInfoOpSet.getDetails(contact, PhoneNumberDetail.class);

        String phoneNumberDetail = "";
        while (contactDetails.hasNext())
        {
            genericDetail = contactDetails.next();
            phoneNumberDetail =
                phoneNumberDetail + " " + genericDetail.getDetailValue();
        }

        if (phoneNumberDetail.trim().equals(""))
            phoneNumberDetail
                = Resources.getString("plugin.contactinfo.NOT_SPECIFIED");

        valuesPanel.add(new JLabel(phoneNumberDetail));

        return summaryPanel;
    }

    /**
     * A panel that displays all of the details retrieved from the opSet.
     *
     * @return a panel that will be added as the extended tab.
     */
    private JPanel createExtendedInfoPanel()
    {
        JPanel mainExtendedPanel = new TransparentPanel(new BorderLayout());

        JPanel extendedPanel = new TransparentPanel();
        extendedPanel.setLayout(new BoxLayout(extendedPanel, BoxLayout.Y_AXIS));

        JPanel imagePanel = new TransparentPanel();

        // The imagePanel will be used for any BinaryDetails and will be added at
        // the bottom so we don't disrupt the standard look of the other details
        imagePanel.setLayout(new BoxLayout(imagePanel, BoxLayout.LINE_AXIS));
        imagePanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory
            .createTitledBorder(
                Resources.getString("plugin.contactinfo.USER_PICTURES")),

            BorderFactory.createEmptyBorder(0, 5, 5, 5)));

        // Obtain all the details for a contact.
        Iterator<GenericDetail> iter
            = contactInfoOpSet.getAllDetailsForContact(contact);

        GenericDetail detail;
        JLabel detailLabel;
        JTextArea detailValueArea;
        JPanel detailPanel;

        while (iter.hasNext())
        {
            detail = iter.next();

            if (detail.getDetailValue().toString().equals(""))
                continue;

            detailLabel = new JLabel();
            detailValueArea = new JTextArea();
            detailPanel = new TransparentPanel(new BorderLayout(10, 10));

            detailValueArea.setAlignmentX(JTextArea.CENTER_ALIGNMENT);
            detailValueArea.setEditable(false);
            detailValueArea.setLineWrap(true);

            detailPanel.add(detailLabel, BorderLayout.WEST);
            detailPanel.add(detailValueArea, BorderLayout.CENTER);
            detailPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            extendedPanel.add(detailPanel);

            if (detail instanceof BinaryDetail)
            {
                JLabel imageLabel =
                    new JLabel(new ImageIcon((byte[]) detail
                        .getDetailValue()));

                imagePanel.add(imageLabel);
            }
            else if (detail instanceof CalendarDetail)
            {
                detailLabel.setText(detail.getDetailDisplayName() + ": ");

                Date detailDate =
                    ((Calendar) detail.getDetailValue()).getTime();
                DateFormat df = DateFormat.getDateInstance();

                detailValueArea.setText(df.format(detailDate).trim());
            }
            else if (detail instanceof LocaleDetail)
            {
                detailLabel.setText(detail.getDetailDisplayName() + ": ");

                Object value = detail.getDetailValue();
                String valueStr = "";

                if(value instanceof Locale)
                    valueStr = ((Locale) value).getDisplayName().trim();
                else if(value instanceof String)
                    valueStr = (String)value;

                detailValueArea.setText(valueStr);
            }
            else if (detail instanceof TimeZoneDetail)
            {
                detailLabel.setText(detail.getDetailDisplayName() + ": ");

                detailValueArea.setText(((TimeZone) detail.getDetailValue())
                        .getDisplayName().trim());
            }
            else
            {
                detailLabel.setText(detail.getDetailDisplayName() + ": ");

                detailValueArea.setText(
                    detail.getDetailValue().toString().trim());
            }
        }

        // Add users status message to extended details if it exists
        String statusMessage = contact.getStatusMessage();
        if(statusMessage != null && statusMessage.length() > 0)
        {
            detailLabel = new JLabel();
            HTMLTextPane detailValuePane = new HTMLTextPane();
            detailPanel = new TransparentPanel(new BorderLayout(10, 10));

            detailValuePane.setEditable(false);
            detailValuePane.setOpaque(false);

            detailPanel.add(detailLabel, BorderLayout.WEST);
            detailPanel.add(detailValuePane, BorderLayout.CENTER);
            detailPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            extendedPanel.add(detailPanel);

            detailLabel.setText(Resources.getString(
                "plugin.contactinfo.USER_STATUS_MESSAGE") + ": ");

            detailValuePane.setText(statusMessage);
        }

        // If the contact's protocol supports web info, give them a button to
        // get it
        OperationSetWebContactInfo webContactInfo
            = contact
                .getProtocolProvider()
                    .getOperationSet(OperationSetWebContactInfo.class);

        if (webContactInfo != null)
        {
            final String urlString
                = webContactInfo.getWebContactInfo(contact).toString();

            JLabel webInfoLabel = new JLabel("Click to see web info: ");
            JEditorPane webInfoValue = new JEditorPane();
            JPanel webInfoPanel = new TransparentPanel(new BorderLayout());

            webInfoPanel.add(webInfoLabel, BorderLayout.WEST);
            webInfoPanel.add(webInfoValue, BorderLayout.CENTER);

            extendedPanel.add(webInfoPanel);

            webInfoValue.setOpaque(false);
            webInfoValue.setContentType("text/html");
            webInfoValue.setEditable(false);
            webInfoValue.setText(   "<a href='"
                                    + urlString + "'>"
                                    + contact.getDisplayName()
                                    + " web info</a>");

            webInfoValue.addHyperlinkListener(new HyperlinkListener()
            {
                public void hyperlinkUpdate(HyperlinkEvent e)
                {
                    if (e.getEventType()
                            .equals(HyperlinkEvent.EventType.ACTIVATED))
                    {
                        ContactInfoActivator
                            .getBrowserLauncher().openURL(urlString);
                    }
                }
            });
        }

        if (imagePanel.getComponentCount() > 0)
            mainExtendedPanel.add(imagePanel, BorderLayout.CENTER);

        mainExtendedPanel.add(extendedPanel, BorderLayout.NORTH);

        return mainExtendedPanel;
    }

    /**
     * The <tt>HTMLTextPane</tt> is a pane that handles displaying HTML and
     * hyperlinking urls found in the text.
     */
    private class HTMLTextPane
        extends JTextPane
        implements HyperlinkListener
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        /**
         * The regular expression (in the form of compiled <tt>Pattern</tt>)
         * which matches URLs for the purposed of turning them into links.
         */
        private final Pattern URL_PATTERN = Pattern.compile("("
            + "(\\bwww\\.[^\\s<>\"]+\\.[^\\s<>\"]+/*[?#]*(\\w+[&=;?]\\w+)*\\b)" // wwwURL
            + "|" + "(\\b\\w+://[^\\s<>\"]+/*[?#]*(\\w+[&=;?]\\w+)*\\b)" // protocolURL
            + ")");

        private SIPCommHTMLEditorKit editorKit;
        private HTMLDocument document;

        /**
         * Creates and instance of <tt>HTMLTextPane</tt>
         */
        public HTMLTextPane()
        {
            editorKit = new SIPCommHTMLEditorKit(this);

            this.document = (HTMLDocument) editorKit.createDefaultDocument();

            this.addHyperlinkListener(this);

            this.setContentType("text/html");
            this.setEditorKitForContentType("text/html", editorKit);
            this.setEditorKit(editorKit);
            this.setDocument(document);

            putClientProperty(
                JTextPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        }


        /**
         * Override of parent <tt>setText(String)</tt> to search for URLs and
         * set as hyperlinks.
         * @param string <tt>String</tt> to display.
         */
        @Override
        public void setText(String string)
        {

            Matcher m = URL_PATTERN.matcher(string);
            StringBuffer msgBuffer = new StringBuffer();
            int prevEnd = 0;

            while (m.find())
            {
                String fromPrevEndToStart = string.substring(prevEnd, m.start());

                msgBuffer.append(fromPrevEndToStart);
                prevEnd = m.end();

                String url = m.group().trim();

                msgBuffer.append("<A href=\"");
                if (url.startsWith("www"))
                    msgBuffer.append("http://");
                msgBuffer.append(url);
                msgBuffer.append("\">");
                msgBuffer.append(url);
                msgBuffer.append("</A>");
            }

            String fromPrevEndToEnd = string.substring(prevEnd);

            msgBuffer.append(fromPrevEndToEnd);

            super.setText(msgBuffer.toString());

        }

        /**
         * Handles activations of hyperlinks
         * @param e <tt>HyperlinkEvent</tt> to handle.
         */
        public void hyperlinkUpdate(HyperlinkEvent e)
        {
            if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                ContactInfoActivator.getBrowserLauncher()
                    .openURL(e.getURL().toString());
        }
    }
}
