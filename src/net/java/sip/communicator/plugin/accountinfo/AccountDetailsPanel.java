/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.accountinfo;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;

import javax.imageio.*;
import javax.swing.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The right side panel of AccountDetailsDialog. Shows one tab of a summary of
 * contact information for the selected subcontact, and has an extended tab
 * listing all of the details.
 * 
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class AccountDetailsPanel
    extends TransparentPanel
    implements Skinnable
{
    private static final long serialVersionUID = 5524135388175045624L;

    private Logger logger = Logger.getLogger(AccountDetailsPanel.class);

    /**
     * The operation set giving access to the server stored account details.
     */
    private OperationSetServerStoredAccountInfo accountInfoOpSet;

    private JTextField firstNameField = new JTextField();

    private JTextField middleNameField = new JTextField();

    private JTextField lastNameField = new JTextField();

    private JTextField genderField = new JTextField();

    private JTextField ageField = new JTextField();

    private JTextField birthdayField = new JTextField();

    private JTextField emailField = new JTextField();

    private JTextField phoneField = new JTextField();

    private JLabel avatarLabel = new JLabel();

    private JButton applyButton
        = new JButton(Resources.getString("service.gui.APPLY"));

    private JPanel buttonPanel =
        new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

    private JScrollPane mainScrollPane = new JScrollPane();

    private boolean isDataLoaded = false;

    private FirstNameDetail firstNameDetail;

    private MiddleNameDetail middleNameDetail;

    private LastNameDetail lastNameDetail;

    private GenderDetail genderDetail;

    private BirthDateDetail birthDateDetail;

    private EmailAddressDetail emailDetail;

    private PhoneNumberDetail phoneDetail;

    private BinaryDetail avatarDetail;

    private byte[] newAvatarImage;

    private ImageIcon avatarImageIcon = null;

    /**
     * The last avatar file directory open.
     */
    private File lastAvatarDir;

    /**
     * Construct a panel containing all account details for the given protocol
     * provider.
     * 
     * @param protocolProvider the protocol provider service
     */
    public AccountDetailsPanel(ProtocolProviderService protocolProvider)
    {
        super(new BorderLayout());

        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
//        this.setPreferredSize(new Dimension(500, 400));

        accountInfoOpSet
            = protocolProvider
                .getOperationSet(OperationSetServerStoredAccountInfo.class);

        if (accountInfoOpSet == null)
        {
            initUnsupportedPanel();
        }
        else
        {
            this.initSummaryPanel();

            if (protocolProvider.isRegistered())
            {
                loadDetails();
            }
        }
    }

    private void initSummaryPanel()
    {
        JPanel summaryPanel = new TransparentPanel(new BorderLayout(10, 10));

        summaryPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        summaryPanel.setSize(this.getWidth(), this.getHeight());

        // Create the avatar panel.
        JPanel leftPanel = new TransparentPanel(new BorderLayout());
        JPanel avatarPanel = new TransparentPanel(new BorderLayout());
        JButton changeAvatarButton = new JButton(Resources.getString("plugin.accountinfo.CHANGE"));
        JPanel changeButtonPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

        changeAvatarButton.addActionListener(new ChangeAvatarActionListener());

        changeButtonPanel.add(changeAvatarButton);

        avatarPanel.add(avatarLabel, BorderLayout.CENTER);
        avatarPanel.add(changeButtonPanel, BorderLayout.SOUTH);

        leftPanel.add(avatarPanel, BorderLayout.NORTH);

        summaryPanel.add(leftPanel, BorderLayout.WEST);

        // Create the summary details panel.
        JPanel detailsPanel = new TransparentPanel(new BorderLayout(10, 10));
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        summaryPanel.add(detailsPanel, BorderLayout.CENTER);

        // Labels panel.
        JPanel labelsPanel = new TransparentPanel(new GridLayout(0, 1, 5, 5));

        labelsPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.FIRST_NAME")));
//        labelsPanel.add(new JLabel(Resources.getString("plugin.accountinfo.MIDDLE_NAME")));
        labelsPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.LAST_NAME")));
        labelsPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.GENDER")));
        labelsPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.AGE")));
        labelsPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.BDAY")));
        labelsPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.EMAIL")));
        labelsPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.PHONE")));

        detailsPanel.add(labelsPanel, BorderLayout.WEST);

        // Values panel.
        JPanel valuesPanel = new TransparentPanel(new GridLayout(0, 1, 5, 5));

        valuesPanel.add(firstNameField);
//        valuesPanel.add(middleNameField);
        valuesPanel.add(lastNameField);
        valuesPanel.add(genderField);
        valuesPanel.add(ageField);
        valuesPanel.add(birthdayField);
        valuesPanel.add(emailField);
        valuesPanel.add(phoneField);

        detailsPanel.add(valuesPanel, BorderLayout.CENTER);

        this.mainScrollPane.getViewport().add(summaryPanel);

        this.add(mainScrollPane, BorderLayout.NORTH);

        this.applyButton.addActionListener(new SubmitActionListener());

        this.buttonPanel.add(applyButton);

        this.add(buttonPanel, BorderLayout.SOUTH);

        // All items are now instantiated and could safely load the skin.
        loadSkin();
    }

    /**
     * Loads details for 
     */
    public void loadDetails()
    {
        this.loadSummaryDetails();
        this.isDataLoaded = true;
    }

    /**
     * Creates the panel that indicates to the user that the currently selected
     * contact does not support server stored contact info.
     */
    private void initUnsupportedPanel()
    {
        JTextArea unsupportedTextArea =
            new JTextArea(Resources.getString(
                "plugin.accountinfo.NOT_SUPPORTED"));

        unsupportedTextArea.setEditable(false);
        unsupportedTextArea.setLineWrap(true);

        JPanel unsupportedPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

        unsupportedTextArea.setPreferredSize(new Dimension(200, 200));

        unsupportedPanel.setBorder(
            BorderFactory.createEmptyBorder(50, 20, 50, 20));

        unsupportedPanel.add(unsupportedTextArea);

        this.add(unsupportedPanel);
    }

    /**
     * Creates a panel that can be added as the summary tab that displays the
     * following details: -
     * <p>
     * Avatar(Contact image) - FirstNameDetail - MiddleNameDetail -
     * LastNameDetail - BirthdateDetail (and calculate age) - GenderDetail -
     * EmailAddressDetail - PhoneNumberDetail. All other details will be* added
     * to our list of extended details.
     */
    private void loadSummaryDetails()
    {
        Iterator<GenericDetail> contactDetails;

        // Avatar details.
        contactDetails
            = accountInfoOpSet.getDetails(BinaryDetail.class);

        byte[] avatarImage = null;
        if (contactDetails.hasNext())
        {
            avatarDetail = (BinaryDetail) contactDetails.next();

            avatarImage = avatarDetail.getBytes();
        }

        if (avatarImage != null && avatarImage.length > 0)
        {
            avatarImageIcon = new ImageIcon(
                getScaledImageInstance(avatarImage));
            avatarLabel.setIcon(avatarImageIcon);
        }

        // First name details.
        contactDetails =
            accountInfoOpSet.getDetails(FirstNameDetail.class);

        String firstNameDetailString = "";
        while (contactDetails.hasNext())
        {
            firstNameDetail = (FirstNameDetail) contactDetails.next();

            firstNameDetailString =
                firstNameDetailString + " " + firstNameDetail.getDetailValue();
        }

        firstNameField.setText(firstNameDetailString);

        // Middle name details.
        contactDetails =
            accountInfoOpSet.getDetails(MiddleNameDetail.class);

        String middleNameDetailString = "";
        while (contactDetails.hasNext())
        {
            middleNameDetail = (MiddleNameDetail) contactDetails.next();
            middleNameDetailString =
                middleNameDetailString + " " + middleNameDetail.getDetailValue();
        }

        middleNameField.setText(middleNameDetailString);

        // Last name details.
        contactDetails =
            accountInfoOpSet.getDetails(LastNameDetail.class);

        String lastNameDetailString = "";
        while (contactDetails.hasNext())
        {
            lastNameDetail = (LastNameDetail) contactDetails.next();

            lastNameDetailString =
                lastNameDetailString + " " + lastNameDetail.getDetailValue();
        }

        lastNameField.setText(lastNameDetailString);

        // Gender details.
        contactDetails =
            accountInfoOpSet.getDetails(GenderDetail.class);

        String genderDetailString = "";
        while (contactDetails.hasNext())
        {
            genderDetail = (GenderDetail) contactDetails.next();
            genderDetailString = genderDetailString + " "
                                    + genderDetail.getDetailValue();
        }

        genderField.setText(genderDetailString);

        // Birthday details.
        contactDetails =
            accountInfoOpSet.getDetails(BirthDateDetail.class);

        String birthDateDetailString = "";
        String ageDetail = "";
        if (contactDetails.hasNext())
        {
            birthDateDetail = (BirthDateDetail) contactDetails.next();

            Calendar calendarDetail =
                (Calendar) birthDateDetail.getDetailValue();

            Date birthDate = calendarDetail.getTime();
            DateFormat dateFormat = DateFormat.getDateInstance();

            birthDateDetailString = dateFormat.format(birthDate).trim();

            Calendar c = Calendar.getInstance();
            int age = c.get(Calendar.YEAR) - calendarDetail.get(Calendar.YEAR);

            if (c.get(Calendar.MONTH) < calendarDetail.get(Calendar.MONTH))
                age--;

            ageDetail = Integer.toString(age).trim();
        }

        birthdayField.setText(birthDateDetailString);
        ageField.setText(ageDetail);

        // Email details.
        contactDetails =
            accountInfoOpSet.getDetails(EmailAddressDetail.class);

        String emailDetailString = "";
        while (contactDetails.hasNext())
        {
            emailDetail = (EmailAddressDetail) contactDetails.next();
            emailDetailString = emailDetailString + " "
                + emailDetail.getDetailValue();
        }

        emailField.setText(emailDetailString);

        // Phone number details.
        contactDetails =
            accountInfoOpSet.getDetails(PhoneNumberDetail.class);

        String phoneNumberDetailString = "";
        while (contactDetails.hasNext())
        {
            phoneDetail = (PhoneNumberDetail) contactDetails.next();
            phoneNumberDetailString =
                phoneNumberDetailString + " " + phoneDetail.getDetailValue();
        }

        phoneField.setText(phoneNumberDetailString);
    }

    private class SubmitActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            String firstName = firstNameField.getText();
//            String middleName = middleNameField.getText();
            String lastName = lastNameField.getText();
            String gender = genderField.getText();
            String email = emailField.getText();
            String phoneNumber = phoneField.getText();

            Calendar birthDateCalendar = Calendar.getInstance();

            if(birthdayField.getText() != null
                && birthdayField.getText().length() > 0)
            {
                try
                {
                    DateFormat dateFormat
                        = DateFormat.getDateInstance(DateFormat.DEFAULT);

                    Date birthDate = dateFormat.parse(birthdayField.getText());

                    birthDateCalendar.setTime(birthDate);
                }
                catch (ParseException e2)
                {
                    logger.error("Failed to parse birth date.", e2);
                }
            }

            try
            {
                FirstNameDetail newFirstNameDetail
                    = new ServerStoredDetails.FirstNameDetail(firstName);

                if (firstNameDetail == null)
                    accountInfoOpSet.addDetail(newFirstNameDetail);
                else
                    accountInfoOpSet.replaceDetail( firstNameDetail,
                                                    newFirstNameDetail);

//                MiddleNameDetail newMiddleNameDetail
//                    = new ServerStoredDetails.MiddleNameDetail(middleName);
//
//                if (middleNameDetail == null)
//                    accountInfoOpSet.addDetail(newMiddleNameDetail);
//                else
//                    accountInfoOpSet.replaceDetail( middleNameDetail,
//                                                    newMiddleNameDetail);

                LastNameDetail newLastNameDetail
                    = new ServerStoredDetails.LastNameDetail(lastName);

                if (lastNameDetail == null)
                    accountInfoOpSet.addDetail(newLastNameDetail);
                else
                    accountInfoOpSet.replaceDetail( lastNameDetail,
                        newLastNameDetail);

                GenderDetail newGenderDetail
                    = new ServerStoredDetails.GenderDetail(gender);

                if (genderDetail == null)
                    accountInfoOpSet.addDetail(newGenderDetail);
                else
                    accountInfoOpSet.replaceDetail( genderDetail,
                                                    newGenderDetail);

                BirthDateDetail newBirthDateDetail
                    = new ServerStoredDetails.BirthDateDetail(birthDateCalendar);

                if (birthDateDetail == null)
                    accountInfoOpSet.addDetail(newBirthDateDetail);
                else
                    accountInfoOpSet.replaceDetail( birthDateDetail,
                                                    newBirthDateDetail);

                EmailAddressDetail newEmailDetail
                    = new ServerStoredDetails.EmailAddressDetail(email);

                if (emailDetail == null)
                    accountInfoOpSet.addDetail(newEmailDetail);
                else
                    accountInfoOpSet.replaceDetail( emailDetail,
                                                    newEmailDetail);

                PhoneNumberDetail newPhoneDetail
                    = new ServerStoredDetails.PhoneNumberDetail(phoneNumber);

                if (phoneDetail == null)
                    accountInfoOpSet.addDetail(newPhoneDetail);
                else
                    accountInfoOpSet.replaceDetail( phoneDetail,
                                                    newPhoneDetail);

                BinaryDetail newAvatarDetail
                    = new ServerStoredDetails.BinaryDetail(
                        "Avatar",
                        newAvatarImage);

                if (avatarDetail == null)
                    accountInfoOpSet.addDetail(newAvatarDetail);
                else
                    accountInfoOpSet.replaceDetail( avatarDetail,
                                                    newAvatarDetail);
            }
            catch (ClassCastException e1)
            {
                logger.error("Failed to update account details.", e1);
            }
            catch (OperationFailedException e1)
            {
                logger.error("Failed to update account details.", e1);
            }
        }
    }

    private class ChangeAvatarActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            SipCommFileChooser chooser = GenericFileDialog.create(
                null, "Change avatar...", 
                SipCommFileChooser.LOAD_FILE_OPERATION,
                lastAvatarDir.getAbsolutePath());
            chooser.addFilter(new ImageFilter());

            File file = chooser.getFileFromDialog();

            if (file != null)
            {
                try
                {
                    lastAvatarDir = file.getParentFile();

                    FileInputStream in = new FileInputStream(file);
                    byte buffer[] = new byte[in.available()];
                    in.read(buffer);

                    if (buffer == null || buffer.length <= 0)
                        return;

                    newAvatarImage = buffer;

                    avatarImageIcon = new ImageIcon(
                        getScaledImageInstance(newAvatarImage));
                    avatarLabel.setIcon(avatarImageIcon);
                }
                catch (IOException ex)
                {
                    logger.error("Failed to load image.", ex);
                }
            }
        }
    }

    /**
     * A custom filter that would accept only image files.
     */
    private static class ImageFilter extends SipCommFileFilter
    {
        /**
         * Accept all directories and all gif, jpg, tiff, or png files.
         * Method implemented from FileFilter abstract class.
         *
         * @param f a file to accept or not
         */
        @Override
        public boolean accept(File f)
        {
            if (f.isDirectory())
            {
                return true;
            }

            String extension = getExtension(f);
            if (extension != null)
            {
                if (extension.equals("tiff") ||
                    extension.equals("tif") ||
                    extension.equals("gif") ||
                    extension.equals("jpeg") ||
                    extension.equals("jpg") ||
                    extension.equals("png"))
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }

            return false;
        }

        /**
         * Get the extension of a file.
         */  
        public String getExtension(File f)
        {
            String ext = null;
            String s = f.getName();
            int i = s.lastIndexOf('.');

            if (i > 0 &&  i < s.length() - 1) {
                ext = s.substring(i+1).toLowerCase();
            }
            return ext;
        }

        /**
         * The description of this filter.
         */
        @Override
        public String getDescription()
        {
            return Resources.getString("plugin.accountinfo.ONLY_MESSAGE");
        }

    }

    /**
     * Returns a scaled <tt>Image</tt> instance of the given byte image.
     * 
     * @param image the image in bytes
     * @return a scaled <tt>Image</tt> instance of the given byte image.
     */
    private Image getScaledImageInstance(byte[] image)
    {
        Image resultImage = null;

        try
        {
            resultImage = ImageIO.read(
                    new ByteArrayInputStream(image));
        }
        catch (Exception e)
        {
            logger.error("Failed to convert bytes to image.", e);
        }

        if(resultImage == null)
            return null;

        return resultImage.getScaledInstance(
            avatarLabel.getWidth(),
            avatarLabel.getHeight(),
            Image.SCALE_SMOOTH);
    }

    /**
     * Returns <code>true</code> if the account details are loaded,
     * <code>false</code> - otherwise.
     * 
     * @return <code>true</code> if the account details are loaded,
     * <code>false</code> - otherwise
     */
    public boolean isDataLoaded()
    {
        return isDataLoaded;
    }

    /**
     * Loads the avatar default icon.
     */
    public void loadSkin()
    {
        avatarLabel.setIcon(Resources.getImage("accountInfoDefaultPersonIcon"));
    }
}
