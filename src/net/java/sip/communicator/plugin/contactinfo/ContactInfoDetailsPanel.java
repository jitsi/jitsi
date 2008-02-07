package net.java.sip.communicator.plugin.contactinfo;

/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

import java.awt.*;
import java.awt.event.*;

import java.util.*;
import java.text.DateFormat;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;

/**
 * The right side panel of ContactInfoDialog. Shows one tab of a summary of
 * contact information for the selected subcontact, and has an extended tab
 * listing all of the details.
 * 
 * @author Adam Goldstein
 * @author Yana Stamcheva
 */
public class ContactInfoDetailsPanel
    extends JPanel
{
    /**
     * The tabbed pane containing the two different tabs for details.
     */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /**
     * The operation set giving access to the server stored contact details.
     */
    private OperationSetServerStoredContactInfo contactInfoOpSet;

    /**
     * The currently selected sub-contact we are displaying information about.
     */
    private Contact contact;

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
        contactInfoOpSet =
            (OperationSetServerStoredContactInfo) pps
                .getOperationSet(OperationSetServerStoredContactInfo.class);

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

        this.tabbedPane.addTab(Resources.getString("summary"), icon,
            summaryPanel, Resources.getString("summaryDesc")
                + contact.getDisplayName());

        this.tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

        this.tabbedPane.addTab(Resources.getString("extended"), icon,
            extendedScrollPane, Resources.getString("extendedDesc")
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
        JTextArea unsupportedTextArea =
            new JTextArea(Resources.getString("notSupported"));

        unsupportedTextArea.setEditable(false);
        unsupportedTextArea.setLineWrap(true);

        JPanel unsupportedPanel = new JPanel(new BorderLayout());

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
        JPanel summaryPanel = new JPanel();

        summaryPanel.setLayout(new BorderLayout(10, 5));
        summaryPanel.setSize(this.getWidth(), this.getHeight());

        // Create the avatar panel.
        JPanel avatarPanel = new JPanel();

        avatarPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        byte[] bytes = this.contact.getImage();

        ImageIcon avatarImage = null;
        // If the user has a contact image, let's use it. If not, add the
        // default
        if (bytes != null)
            avatarImage = new ImageIcon(bytes);
        else
            avatarImage = Resources.getImage("defaultPersonIcon");

        ImageIcon scaledImage =
            new ImageIcon(avatarImage.getImage().getScaledInstance(105, 130,
                Image.SCALE_SMOOTH));

        JLabel label = new JLabel(scaledImage);
        avatarPanel.add(label);
        summaryPanel.add(avatarPanel, BorderLayout.WEST);

        // Create the summary details panel.
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BorderLayout());
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        summaryPanel.add(detailsPanel);

        // Labels panel.
        JPanel labelsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        labelsPanel.add(new JLabel(Resources.getString("firstNameNS")));
        labelsPanel.add(new JLabel(Resources.getString("middleNameNS")));
        labelsPanel.add(new JLabel(Resources.getString("lastNameNS")));
        labelsPanel.add(new JLabel(Resources.getString("genderNS")));
        labelsPanel.add(new JLabel(Resources.getString("bdayNS")));
        labelsPanel.add(new JLabel(Resources.getString("ageNS")));
        labelsPanel.add(new JLabel(Resources.getString("emailNS")));
        labelsPanel.add(new JLabel(Resources.getString("phoneNS")));

        detailsPanel.add(labelsPanel, BorderLayout.WEST);

        // Values panel.
        JPanel valuesPanel = new JPanel(new GridLayout(0, 1, 5, 5));

        detailsPanel.add(valuesPanel, BorderLayout.CENTER);

        Iterator contactDetails;
        GenericDetail genericDetail;

        // First name details.
        contactDetails =
            contactInfoOpSet.getDetails(contact, FirstNameDetail.class);

        String firstNameDetail = "";
        while (contactDetails.hasNext())
        {
            genericDetail = (FirstNameDetail) contactDetails.next();

            firstNameDetail =
                firstNameDetail + " " + genericDetail.getDetailValue();
        }

        if (firstNameDetail.equals(""))
            firstNameDetail = Resources.getString("notSpecified");

        valuesPanel.add(new JLabel(firstNameDetail));

        // Middle name details.
        contactDetails =
            contactInfoOpSet.getDetails(contact, MiddleNameDetail.class);

        String middleNameDetail = "";
        while (contactDetails.hasNext())
        {
            genericDetail = (MiddleNameDetail) contactDetails.next();
            middleNameDetail =
                middleNameDetail + " " + genericDetail.getDetailValue();
        }

        if (middleNameDetail.trim().equals(""))
            middleNameDetail = Resources.getString("notSpecified");

        valuesPanel.add(new JLabel(middleNameDetail));

        // Last name details.
        contactDetails =
            contactInfoOpSet.getDetails(contact, LastNameDetail.class);

        String lastNameDetail = "";
        while (contactDetails.hasNext())
        {
            genericDetail = (LastNameDetail) contactDetails.next();

            lastNameDetail =
                lastNameDetail + " " + genericDetail.getDetailValue();
        }

        if (lastNameDetail.trim().equals(""))
            lastNameDetail = Resources.getString("notSpecified");

        valuesPanel.add(new JLabel(lastNameDetail));

        // Gender details.
        contactDetails =
            contactInfoOpSet.getDetails(contact, GenderDetail.class);

        String genderDetail = "";
        while (contactDetails.hasNext())
        {
            genericDetail = (GenderDetail) contactDetails.next();
            genderDetail = genderDetail + " " + genericDetail.getDetailValue();
        }

        if (genderDetail.trim().equals(""))
            genderDetail = Resources.getString("notSpecified");

        valuesPanel.add(new JLabel(genderDetail));

        // Birthday details.
        contactDetails =
            contactInfoOpSet.getDetails(contact, BirthDateDetail.class);

        String birthDateDetail = "";
        String ageDetail = "";
        if (contactDetails.hasNext())
        {
            genericDetail = (BirthDateDetail) contactDetails.next();

            Calendar calendarDetail =
                (Calendar) genericDetail.getDetailValue();

            Date birthDate = calendarDetail.getTime();
            DateFormat dateFormat = DateFormat.getDateInstance();

            birthDateDetail = dateFormat.format(birthDate).trim();

            Calendar c = Calendar.getInstance();
            int age = c.get(Calendar.YEAR) - calendarDetail.get(Calendar.YEAR);

            if (c.get(Calendar.MONTH) < calendarDetail.get(Calendar.MONTH))
                age--;

            ageDetail = new Integer(age).toString().trim();
        }

        if (birthDateDetail.equals(""))
            birthDateDetail = Resources.getString("notSpecified");

        if (ageDetail.equals(""))
            ageDetail = Resources.getString("notSpecified");

        valuesPanel.add(new JLabel(birthDateDetail));
        valuesPanel.add(new JLabel(ageDetail));

        // Email details.
        contactDetails =
            contactInfoOpSet.getDetails(contact, EmailAddressDetail.class);

        String emailDetail = "";
        while (contactDetails.hasNext())
        {
            genericDetail = (EmailAddressDetail) contactDetails.next();
            emailDetail = emailDetail + " " + genericDetail.getDetailValue();
        }

        if (emailDetail.trim().equals(""))
            emailDetail = Resources.getString("notSpecified");

        valuesPanel.add(new JLabel(emailDetail));

        // Phone number details.
        contactDetails =
            contactInfoOpSet.getDetails(contact, PhoneNumberDetail.class);

        String phoneNumberDetail = "";
        while (contactDetails.hasNext())
        {
            genericDetail = (PhoneNumberDetail) contactDetails.next();
            phoneNumberDetail =
                phoneNumberDetail + " " + genericDetail.getDetailValue();
        }

        if (phoneNumberDetail.trim().equals(""))
            phoneNumberDetail = Resources.getString("notSpecified");

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
        JPanel mainExtendedPanel = new JPanel(new BorderLayout());

        JPanel extendedPanel = new JPanel();
        extendedPanel.setLayout(new BoxLayout(extendedPanel, BoxLayout.Y_AXIS));

        JPanel imagePanel = new JPanel();

        // The imagePanel will be used for any BinaryDetails and will be added at
        // the bottom so we don't disrupt the standard look of the other details
        imagePanel.setLayout(new BoxLayout(imagePanel, BoxLayout.LINE_AXIS));
        imagePanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory
            .createTitledBorder(Resources.getString("userPictures")),
            BorderFactory.createEmptyBorder(0, 5, 5, 5)));

        // Obtain all the details for a contact.
        Iterator iter = contactInfoOpSet.getAllDetailsForContact(contact);

        GenericDetail detail;
        JLabel detailLabel;
        JTextArea detailValueArea;
        JPanel detailPanel;

        while (iter.hasNext())
        {
            detail = (GenericDetail) iter.next();

            if (detail.getDetailValue().toString().equals(""))
                continue;

            detailLabel = new JLabel();
            detailValueArea = new JTextArea();
            detailPanel = new JPanel(new BorderLayout(10, 10));

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

                detailValueArea.setText(((Locale) detail.getDetailValue())
                        .getDisplayName().trim());
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

        // If the contact's protocol supports web info, give them a button to
        // get it
        if (contact.getProtocolProvider().getOperationSet(
                OperationSetWebContactInfo.class) != null)
        {
            final String urlString = ((OperationSetWebContactInfo) contact
                .getProtocolProvider().getOperationSet(
                    OperationSetWebContactInfo.class))
                    .getWebContactInfo(contact).toString();

            JLabel webInfoLabel = new JLabel("Click to see web info: ");
            JEditorPane webInfoValue = new JEditorPane();
            JPanel webInfoPanel = new JPanel(new BorderLayout());

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
}