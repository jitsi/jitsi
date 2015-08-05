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
package net.java.sip.communicator.plugin.accountinfo;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.net.*;
import java.util.*;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.plugin.desktoputil.SwingWorker;
import org.jitsi.util.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.presence.avatar.*;
import net.java.sip.communicator.service.globaldisplaydetails.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.plugin.accountinfo.AccountInfoMenuItemComponent.*;

import net.java.sip.communicator.util.Logger;

import com.toedter.calendar.*;

/**
 * The main panel that allows users to view and edit their account information.
 * Different instances of this class are created for every registered
 * <tt>ProtocolProviderService</tt>.
 * Currently, supported account details are first/middle/last names, nickname,
 * street/city/region/country address, postal code, birth date, gender,
 * organization name, job title, about me, home/work email, home/work phone.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 * @author Marin Dzhigarov
 */
public class AccountDetailsPanel
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(AccountDetailsPanel.class);

    /**
     * Mapping between all supported by this plugin <tt>ServerStoredDetails</tt>
     * and their respective <tt>JTextField</tt> that are used for modifying
     * the details.
     */
    private final Map<Class<? extends GenericDetail>, JTextField>
        detailToTextField
            = new HashMap<Class<? extends GenericDetail>, JTextField>();

    /**
     * The <tt>ProtocolProviderService</tt> that this panel is associated with.
     */
    ProtocolProviderService protocolProvider;

    /**
     * The operation set giving access to the server stored account details.
     */
    private OperationSetServerStoredAccountInfo accountInfoOpSet;

    private JTextField displayNameField;

    private JTextField firstNameField;

    private JTextField middleNameField;

    private JTextField lastNameField;

    private JTextField nicknameField;

    private JTextField urlField;

    private JTextField streetAddressField;

    private JTextField cityField;

    private JTextField regionField;

    private JTextField postalCodeField;

    private JTextField countryField;

    private JTextField phoneField;

    private JTextField workPhoneField;

    private JTextField mobilePhoneField;

    private JTextField emailField;

    private JTextField workEmailField;

    private JTextField organizationField;

    private JTextField jobTitleField;

    private JTextArea aboutMeArea;

    private JTextField genderField;

    private JTextField ageField;

    private JDateChooser birthDayCalendar;

    private JRadioButton globalIcon;

    private JRadioButton localIcon;

    private FramedImageWithMenu imageWithMenu;
    /**
     * The "apply" button.
     */
    private JButton applyButton
        = new JButton(Resources.getString("service.gui.APPLY"));

    /**
     * The panel containing all buttons.
     */
    private JPanel buttonPanel =
        new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

    private DisplayNameDetail displayNameDetail;

    private FirstNameDetail firstNameDetail;

    private MiddleNameDetail middleNameDetail;

    private LastNameDetail lastNameDetail;

    private NicknameDetail nicknameDetail;

    private URLDetail urlDetail;

    private AddressDetail streetAddressDetail;

    private CityDetail cityDetail;

    private ProvinceDetail regionDetail;

    private PostalCodeDetail postalCodeDetail;

    private CountryDetail countryDetail;

    private PhoneNumberDetail phoneDetail;
    
    private WorkPhoneDetail workPhoneDetail;

    private MobilePhoneDetail mobilePhoneDetail;

    private EmailAddressDetail emailDetail;

    private WorkEmailAddressDetail workEmailDetail;

    private WorkOrganizationNameDetail organizationDetail;

    private JobTitleDetail jobTitleDetail;

    private AboutMeDetail aboutMeDetail;

    private GenderDetail genderDetail;

    private BirthDateDetail birthDateDetail;

    private ImageDetail avatarDetail;

    /**
     * The panel that contains description labels and text fields
     * for every account detail.
     */
    private JPanel valuesPanel;

    private JScrollPane mainScrollPane;

    /**
     * The parent dialog.
     */
    private AccountInfoDialog dialog;

    /**
     * Construct a panel containing all account details for the given protocol
     * provider.
     *
     * @param protocolProvider the protocol provider service
     */
    public AccountDetailsPanel(AccountInfoDialog dialog,
                               ProtocolProviderService protocolProvider)
    {
        this.dialog = dialog;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        this.setPreferredSize(new Dimension(600, 400));
        this.protocolProvider = protocolProvider;
        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

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
        unsupportedTextArea.setOpaque(false);

        JPanel unsupportedPanel
            = new TransparentPanel();

        unsupportedPanel.setBorder(
            BorderFactory.createEmptyBorder(50, 20, 50, 20));

        unsupportedPanel.add(unsupportedTextArea);

        this.add(unsupportedPanel, BorderLayout.NORTH);
    }

    /**
     * Initialized the main panel that contains all <tt>ServerStoredDetails</tt>
     */
    private void initSummaryPanel()
    {
        JPanel summaryPanel = new TransparentPanel(new BorderLayout(10, 10));

        summaryPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Create the avatar panel.
        JPanel leftPanel = new TransparentPanel(new BorderLayout());
        JPanel avatarPanel = new TransparentPanel(new BorderLayout());

        JPanel radioButtonPanel = new TransparentPanel(new GridLayout(2, 1));
        globalIcon =
            new JRadioButton(
                Resources.getString("plugin.accountinfo.GLOBAL_ICON"));
        globalIcon.setSelected(true);
        globalIcon.setOpaque(false);
        globalIcon.setEnabled(false);
        localIcon =
            new JRadioButton(
                Resources.getString("plugin.accountinfo.LOCAL_ICON"));
        localIcon.setOpaque(false);
        localIcon.setEnabled(false);
        ButtonGroup group = new ButtonGroup();
        group.add(globalIcon);
        group.add(localIcon);
        radioButtonPanel.add(globalIcon);
        radioButtonPanel.add(localIcon);
        avatarPanel.add(radioButtonPanel, BorderLayout.NORTH);

        leftPanel.add(avatarPanel, BorderLayout.NORTH);
        summaryPanel.add(leftPanel, BorderLayout.WEST);
        detailToTextField.put(ImageDetail.class, new JTextField());

        imageWithMenu
            = new FramedImageWithMenu(
                    Resources.getImage(
                        "service.gui.DEFAULT_USER_PHOTO"),
                    Resources.getImage(
                        "service.gui.DEFAULT_USER_PHOTO").getIconWidth(),
                    Resources.getImage(
                        "service.gui.DEFAULT_USER_PHOTO").getIconHeight());
        SelectAvatarMenu selectAvatarMenu = new SelectAvatarMenu(imageWithMenu);
        selectAvatarMenu.setAccountID(protocolProvider.getAccountID());
        imageWithMenu.setPopupMenu(selectAvatarMenu);
        avatarPanel.add(imageWithMenu, BorderLayout.SOUTH);

        globalIcon.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                imageWithMenu.setEnabled(false);
            }
        });
        localIcon.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                imageWithMenu.setEnabled(true);
            }
        });
        imageWithMenu.setEnabled(false);

        valuesPanel = new TransparentPanel(new GridBagLayout());
        valuesPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        GridBagConstraints first = new GridBagConstraints();
        first.gridx = 0;
        first.gridy = 0;
        first.weightx = 0;
        first.anchor = GridBagConstraints.LINE_START;
        first.gridwidth = 1;
        first.insets = new Insets(4, 4, 4, 4);
        first.fill = GridBagConstraints.HORIZONTAL;
        GridBagConstraints second = new GridBagConstraints();
        second.gridx = 1;
        second.gridy = 0;
        second.weightx = 2;
        second.anchor = GridBagConstraints.LINE_START;
        second.gridwidth = 1; // GridBagConstraints.REMAINDER;
        second.insets = first.insets;
        second.fill = GridBagConstraints.HORIZONTAL;

        if (accountInfoOpSet.isDetailClassSupported(DisplayNameDetail.class))
        {
            displayNameField = new JTextField();
            valuesPanel.add(new JLabel(
                Resources.getString("plugin.accountinfo.DISPLAY_NAME"))
                , first);
            valuesPanel.add(displayNameField, second);
            first.gridy = ++second.gridy;
            detailToTextField.put(DisplayNameDetail.class, displayNameField);
        }

        firstNameField = new JTextField();
        valuesPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.FIRST_NAME"))
            , first);
        valuesPanel.add(firstNameField, second);
        first.gridy = ++second.gridy;
        detailToTextField.put(FirstNameDetail.class, firstNameField);

        middleNameField = new JTextField();
        valuesPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.MIDDLE_NAME"))
            , first);
        valuesPanel.add(middleNameField, second);
        first.gridy = ++second.gridy;
        detailToTextField.put(MiddleNameDetail.class, middleNameField);
 
        lastNameField = new JTextField();
        valuesPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.LAST_NAME"))
            , first);
        valuesPanel.add(lastNameField, second);
        first.gridy = ++second.gridy;
        detailToTextField.put(LastNameDetail.class, lastNameField);

        if (accountInfoOpSet.isDetailClassSupported(NicknameDetail.class))
        {
            nicknameField = new JTextField();
            valuesPanel.add(new JLabel(
                Resources.getString("plugin.accountinfo.NICKNAME"))
                , first);
            valuesPanel.add(nicknameField, second);
            first.gridy = ++second.gridy;
            detailToTextField.put(NicknameDetail.class, nicknameField);
        }
        if (accountInfoOpSet.isDetailClassSupported(URLDetail.class))
        {
            urlField = new JTextField();
            valuesPanel.add(new JLabel(
                Resources.getString("plugin.accountinfo.URL"))
                , first);
            valuesPanel.add(urlField, second);
            first.gridy = ++second.gridy;
            detailToTextField.put(URLDetail.class, urlField);
        }
        if (accountInfoOpSet.isDetailClassSupported(GenderDetail.class))
        {
            genderField = new JTextField();
            valuesPanel.add(new JLabel(
                Resources.getString("plugin.accountinfo.GENDER"))
                , first);
            valuesPanel.add(genderField, second);
            first.gridy = ++second.gridy;
            detailToTextField.put(GenderDetail.class, genderField);
        }

        birthDayCalendar = new JDateChooser();
        valuesPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.BDAY"))
            , first);
        valuesPanel.add(birthDayCalendar, second);
        birthDayCalendar.setDateFormatString(
            Resources.getString("plugin.accountinfo.BDAY_FORMAT"));
        birthDayCalendar.addPropertyChangeListener(
            new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt)
                {
                    if (evt.getPropertyName().equals("date"))
                    {
                        Date date = (Date) evt.getNewValue();
                        if (date != null)
                        {
                            Calendar currentDate = Calendar.getInstance();
                            Calendar c = Calendar.getInstance();
                            c.setTime(date);
                            int age =
                                currentDate.get(Calendar.YEAR) -
                                c.get(Calendar.YEAR);

                            if (currentDate.get(Calendar.MONTH) <
                                c.get(Calendar.MONTH))
                                age--;
                            if ((currentDate.get(Calendar.MONTH) ==
                                    c.get(Calendar.MONTH))
                                &&
                                (currentDate.get(Calendar.DAY_OF_MONTH) <
                                    c.get(Calendar.DAY_OF_MONTH)))
                                age--;
                            String ageDetail = Integer.toString(age).trim();
                            ageField.setText(ageDetail);
                        }
                    }
                }
        });
        first.gridy = ++second.gridy;
        ageField = new JTextField();
        valuesPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.AGE")), first);
        valuesPanel.add(ageField, second);
        first.gridy = ++second.gridy;
        ageField.setEditable(false);
        detailToTextField.put(BirthDateDetail.class, new JTextField());

        if (accountInfoOpSet.isDetailClassSupported(AddressDetail.class))
        {
            streetAddressField = new JTextField();
            valuesPanel.add(new JLabel(
                Resources.getString("plugin.accountinfo.STREET"))
                , first);
            valuesPanel.add(streetAddressField, second);
            first.gridy = ++second.gridy;
            detailToTextField.put(AddressDetail.class, streetAddressField);
        }
        if (accountInfoOpSet.isDetailClassSupported(CityDetail.class))
        {
            cityField = new JTextField();
            valuesPanel.add(new JLabel(
                Resources.getString("plugin.accountinfo.CITY")), first);
            valuesPanel.add(cityField, second);
            first.gridy = ++second.gridy;
            detailToTextField.put(CityDetail.class, cityField);
        }
        if (accountInfoOpSet.isDetailClassSupported(ProvinceDetail.class))
        {
            regionField = new JTextField();
            valuesPanel.add(new JLabel(
                Resources.getString("plugin.accountinfo.REGION"))
                , first);
            valuesPanel.add(regionField, second);
            first.gridy = ++second.gridy;
            detailToTextField.put(ProvinceDetail.class, regionField);
        }
        if (accountInfoOpSet.isDetailClassSupported(PostalCodeDetail.class))
        {
            postalCodeField = new JTextField();
            valuesPanel.add(new JLabel(
                Resources.getString("plugin.accountinfo.POST"))
                , first);
            valuesPanel.add(postalCodeField, second);
            first.gridy = ++second.gridy;
            detailToTextField.put(PostalCodeDetail.class, postalCodeField);
        }
        if (accountInfoOpSet.isDetailClassSupported(CountryDetail.class))
        {
            countryField = new JTextField();
            valuesPanel.add(new JLabel(
                Resources.getString("plugin.accountinfo.COUNTRY"))
                , first);
            valuesPanel.add(countryField, second);
            first.gridy = ++second.gridy;
            detailToTextField.put(CountryDetail.class, countryField);
        }

        emailField = new JTextField();
        valuesPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.EMAIL"))
            , first);
        valuesPanel.add(emailField, second);
        first.gridy = ++second.gridy;
        detailToTextField.put(EmailAddressDetail.class, emailField);

        if (accountInfoOpSet.isDetailClassSupported(
            WorkEmailAddressDetail.class))
        {
            workEmailField = new JTextField();
            valuesPanel.add(new JLabel(
                Resources.getString("plugin.accountinfo.WORK_EMAIL"))
                , first);
            valuesPanel.add(workEmailField, second);
            first.gridy = ++second.gridy;
            detailToTextField.put(WorkEmailAddressDetail.class, workEmailField);
        }

        phoneField = new JTextField();
        valuesPanel.add(new JLabel(
            Resources.getString("plugin.accountinfo.PHONE"))
            , first);
        valuesPanel.add(phoneField, second);
        first.gridy = ++second.gridy;
        detailToTextField.put(PhoneNumberDetail.class, phoneField);

        if (accountInfoOpSet.isDetailClassSupported(WorkPhoneDetail.class))
        {
            workPhoneField = new JTextField();
            valuesPanel.add(new JLabel(
                Resources.getString("plugin.accountinfo.WORK_PHONE"))
                , first);
            valuesPanel.add(workPhoneField, second);
            first.gridy = ++second.gridy;
            detailToTextField.put(
                WorkPhoneDetail.class, workPhoneField);
        }
        if (accountInfoOpSet.isDetailClassSupported(MobilePhoneDetail.class))
        {
            mobilePhoneField = new JTextField();
            valuesPanel.add(new JLabel(
                Resources.getString("plugin.accountinfo.MOBILE_PHONE"))
                , first);
            valuesPanel.add(mobilePhoneField, second);
            first.gridy = ++second.gridy;
            detailToTextField.put(
                MobilePhoneDetail.class, mobilePhoneField);
        }
        if (accountInfoOpSet.isDetailClassSupported(
            WorkOrganizationNameDetail.class))
        {
            organizationField = new JTextField();
            valuesPanel.add(new JLabel(
                Resources.getString("plugin.accountinfo.ORGANIZATION"))
                , first);
            valuesPanel.add(organizationField, second);
            first.gridy = ++second.gridy;
            detailToTextField.put(
                WorkOrganizationNameDetail.class, organizationField);
        }
        if (accountInfoOpSet.isDetailClassSupported(JobTitleDetail.class))
        {
            jobTitleField = new JTextField();
            valuesPanel.add(new JLabel(
                Resources.getString("plugin.accountinfo.JOB_TITLE"))
                , first);
            valuesPanel.add(jobTitleField, second);
            first.gridy = ++second.gridy;
            detailToTextField.put(
                JobTitleDetail.class, jobTitleField);
        }
        if (accountInfoOpSet.isDetailClassSupported(AboutMeDetail.class))
        {
            aboutMeArea = new JTextArea(3, 0);
            valuesPanel.add(new JLabel(
                Resources.getString("plugin.accountinfo.ABOUT_ME"))
                , first);
            second.gridheight = 3;
            JScrollPane areaScrollPane = new JScrollPane(aboutMeArea);
            areaScrollPane.setOpaque(false);
            areaScrollPane.setBorder(BorderFactory.createEmptyBorder());
            areaScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            valuesPanel.add(areaScrollPane, second);
            first.gridy = ++second.gridy;

            DefaultStyledDocument doc = new DefaultStyledDocument();
            doc.setDocumentFilter(new DocumentFilter() {

                private final int MAX_CHARACTERS =
                    Integer.valueOf(Resources.getString(
                        "plugin.accountinfo.ABOUT_ME_MAX_CHARACTERS"));

                public void insertString(FilterBypass fb, int offs,
                    String str, AttributeSet a)
                    throws BadLocationException {
                    //This rejects the entire insertion if it would make
                    //the contents too long. Another option would be
                    //to truncate the inserted string so the contents
                    //would be exactly maxCharacters in length.
                    if ((fb.getDocument().getLength() + str.length())
                        <= MAX_CHARACTERS)
                       super.insertString(fb, offs, str, a);
                    else
                       Toolkit.getDefaultToolkit().beep();
                    }

                    public void replace(FilterBypass fb, int offs,
                                   int length, 
                                   String str, AttributeSet a)
                    throws BadLocationException {
                    //This rejects the entire replacement if it would make
                    //the contents too long. Another option would be
                    //to truncate the replacement string so the contents
                    //would be exactly maxCharacters in length.
                    if ((fb.getDocument().getLength() + str.length()
                        - length) <= MAX_CHARACTERS)
                       super.replace(fb, offs, length, str, a);
                    else
                       Toolkit.getDefaultToolkit().beep();
                    }
            });

            aboutMeArea.setDocument(doc);
            aboutMeArea.setBorder(firstNameField.getBorder());
            aboutMeArea.setLineWrap(true);
            aboutMeArea.setWrapStyleWord(true);
            aboutMeArea.setPreferredSize(new Dimension(50, 100));
            aboutMeArea.setMinimumSize(new Dimension(50, 100));
            detailToTextField.put(AboutMeDetail.class, new JTextField());
        }

        mainScrollPane = new JScrollPane(summaryPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainScrollPane.getViewport().setOpaque(false);
        mainScrollPane.setOpaque(false);
        mainScrollPane.setBorder(BorderFactory.createEmptyBorder());

        summaryPanel.add(valuesPanel, BorderLayout.CENTER);

        this.add(mainScrollPane);

        this.applyButton.addActionListener(new SubmitActionListener());

        JButton cancelButton =
            new JButton(Resources.getString("service.gui.CANCEL"));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt)
            {
                dialog.close(false);
                mainScrollPane.getVerticalScrollBar().setValue(0);
            }
        });
        this.buttonPanel.add(applyButton);
        this.buttonPanel.add(cancelButton);

        this.add(buttonPanel);

        for (Component component : valuesPanel.getComponents())
            component.setEnabled(false);
        if (aboutMeArea != null)
            aboutMeArea.setEnabled(false);
        applyButton.setEnabled(false);
    }

    /**
     * Loads all <tt>ServerStoredDetails</tt> which are currently supported by
     * this plugin. Note that some <tt>OperationSetServerStoredAccountInfo</tt>
     * implementations may support details that are not supported by this plugin.
     * In this case they will not be loaded.
     */
    public void loadDetails()
    {
        if (accountInfoOpSet != null)
        {
            new DetailsLoadWorker().start();
        }
    }

    /**
     * Loads details in separate thread.
     */
    private class DetailsLoadWorker
        extends SwingWorker
    {

        @Override
        protected Object construct()
            throws
            Exception
        {
            return accountInfoOpSet.getAllAvailableDetails();
        }

        /**
         * Called on the event dispatching thread (not on the worker thread)
         * after the <code>construct</code> method has returned.
         */
        @Override
        protected void finished()
        {
            Iterator<GenericDetail> allDetails = (Iterator<GenericDetail>)get();

            if(allDetails == null)
                return;

            while (allDetails.hasNext())
            {
                GenericDetail detail = allDetails.next();
                loadDetail(detail);
            }

            boolean isAnyDetailEditable = false;
            for (Class<? extends GenericDetail> editable :
                detailToTextField.keySet())
            {
                if (accountInfoOpSet.isDetailClassEditable(editable))
                {
                    isAnyDetailEditable = true;
                    if (editable.equals(AboutMeDetail.class))
                        aboutMeArea.setEnabled(true);
                    else if (editable.equals(BirthDateDetail.class))
                        birthDayCalendar.setEnabled(true);
                    else if (editable.equals(ImageDetail.class))
                    {
                        globalIcon.setEnabled(true);
                        localIcon.setEnabled(true);
                        imageWithMenu.setEnabled(true);
                    }
                    else
                    {
                        JTextField field = detailToTextField.get(editable);
                        field.setEnabled(true);
                    }
                }
            }
            if (isAnyDetailEditable)
                applyButton.setEnabled(true);
        }
    }

    /**
     * Loads a single <tt>GenericDetail</tt> obtained from the
     * <tt>OperationSetServerStoredAccountInfo</tt> into this plugin.
     * @param detail to be loaded.
     */
    private void loadDetail(GenericDetail detail)
    {
        if (detail.getClass().equals(AboutMeDetail.class))
        {
            aboutMeDetail = (AboutMeDetail) detail;
            aboutMeArea.setText((String) detail.getDetailValue());
            return;
        }
        if (detail instanceof BirthDateDetail)
        {
            birthDateDetail = (BirthDateDetail) detail;

            Calendar calendarDetail =
                (Calendar) birthDateDetail.getDetailValue();

            birthDayCalendar.setCalendar(calendarDetail);
            return;
        }

        JTextField field = detailToTextField.get(detail.getClass());
        if (field != null)
        {
            if (detail instanceof ImageDetail)
            {
                localIcon.setSelected(true);
                avatarDetail = (ImageDetail) detail;
                byte[] avatarImage = avatarDetail.getBytes();
                imageWithMenu.setImageIcon(avatarImage);
            }
            else if (detail instanceof URLDetail)
            {
                urlDetail = (URLDetail) detail;
                urlField.setText(urlDetail.getURL().toString());
            }
            else
            {
                Object obj = detail.getDetailValue();
                if(obj instanceof String)
                    field.setText((String) obj);
                else if(obj != null)
                    field.setText(obj.toString());

                if (detail.getClass().equals(DisplayNameDetail.class))
                    displayNameDetail = (DisplayNameDetail) detail;
                else if (detail.getClass().equals(FirstNameDetail.class))
                    firstNameDetail = (FirstNameDetail) detail;
                else if (detail.getClass().equals(MiddleNameDetail.class))
                    middleNameDetail = (MiddleNameDetail) detail;
                else if (detail.getClass().equals(LastNameDetail.class))
                    lastNameDetail = (LastNameDetail) detail;
                else if (detail.getClass().equals(NicknameDetail.class))
                    nicknameDetail = (NicknameDetail) detail;
                else if (detail.getClass().equals(URLDetail.class))
                    urlDetail = (URLDetail) detail;
                else if (detail.getClass().equals(GenderDetail.class))
                    genderDetail = (GenderDetail) detail;
                else if (detail.getClass().equals(AddressDetail.class))
                    streetAddressDetail = (AddressDetail) detail;
                else if (detail.getClass().equals(CityDetail.class))
                    cityDetail = (CityDetail) detail;
                else if (detail.getClass().equals(ProvinceDetail.class))
                    regionDetail = (ProvinceDetail) detail;
                else if (detail.getClass().equals(PostalCodeDetail.class))
                    postalCodeDetail = (PostalCodeDetail) detail;
                else if (detail.getClass().equals(CountryDetail.class))
                    countryDetail = (CountryDetail) detail;
                else if (detail.getClass().equals(PhoneNumberDetail.class))
                    phoneDetail = (PhoneNumberDetail) detail;
                else if (detail.getClass().equals(WorkPhoneDetail.class))
                    workPhoneDetail = (WorkPhoneDetail) detail;
                else if (detail.getClass().equals(MobilePhoneDetail.class))
                    mobilePhoneDetail = (MobilePhoneDetail) detail;
                else if (detail.getClass().equals(EmailAddressDetail.class))
                    emailDetail = (EmailAddressDetail) detail;
                else if (detail.getClass().equals(WorkEmailAddressDetail.class))
                    workEmailDetail = (WorkEmailAddressDetail) detail;
                else if (detail.getClass().equals(
                    WorkOrganizationNameDetail.class))
                    organizationDetail = (WorkOrganizationNameDetail) detail;
                else if (detail.getClass().equals(JobTitleDetail.class))
                    jobTitleDetail = (JobTitleDetail) detail;
                else if (detail.getClass().equals(AboutMeDetail.class))
                    aboutMeDetail = (AboutMeDetail) detail;
            }
        }
    }

    /**
     * Returns the provider we represent.
     * @return
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return protocolProvider;
    }

    /**
     * Attempts to upload all <tt>ServerStoredDetails</tt> on the server using
     * <tt>OperationSetServerStoredAccountInfo</tt>
     *
     */
    private class SubmitActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            if (accountInfoOpSet.isDetailClassSupported(ImageDetail.class))
            {
                if (globalIcon.isSelected())
                {
                    GlobalDisplayDetailsService serv =
                        AccountInfoActivator.getGlobalDisplayDetailsService();

                    byte[] newIcon = serv.getGlobalDisplayAvatar();

                    ImageDetail newDetail = null;
                    if (newIcon != null)
                        newDetail =
                            new ImageDetail(
                                "avatar", serv.getGlobalDisplayAvatar());

                    if (avatarDetail != null || newDetail != null)
                        changeDetail(avatarDetail, newDetail);
                }
            }
            if (accountInfoOpSet.isDetailClassSupported
                (DisplayNameDetail.class))
            {
                String text =
                    detailToTextField.get(DisplayNameDetail.class).getText();

                DisplayNameDetail newDetail = null;
                if (!StringUtils.isNullOrEmpty(text, true))
                    newDetail = new DisplayNameDetail(text);

                if (displayNameDetail != null || newDetail != null)
                    changeDetail(displayNameDetail, newDetail);
            }
            if (accountInfoOpSet.isDetailClassSupported(
                FirstNameDetail.class))
            {
                String text =
                    detailToTextField.get(FirstNameDetail.class).getText();

                FirstNameDetail newDetail = null;
                if (!StringUtils.isNullOrEmpty(text, true))
                    newDetail = new FirstNameDetail(text);

                if (firstNameDetail != null || newDetail != null)
                    changeDetail(firstNameDetail, newDetail);
            }
            if (accountInfoOpSet.isDetailClassSupported(
                MiddleNameDetail.class))
            {
                String text =
                    detailToTextField.get(MiddleNameDetail.class).getText();

                MiddleNameDetail newDetail = null;
                if (!StringUtils.isNullOrEmpty(text, true))
                    newDetail = new MiddleNameDetail(text);

                if (middleNameDetail != null || newDetail != null)
                    changeDetail(middleNameDetail, newDetail);
            }
            if (accountInfoOpSet.isDetailClassSupported(
                LastNameDetail.class))
            {
                String text =
                    detailToTextField.get(LastNameDetail.class).getText();
                LastNameDetail newDetail = null;

                if (!StringUtils.isNullOrEmpty(text, true))
                    newDetail = new LastNameDetail(text);

                if (lastNameDetail != null || newDetail != null)
                    changeDetail(lastNameDetail, newDetail);
            }
            if (accountInfoOpSet.isDetailClassSupported(NicknameDetail.class))
            {
                String text =
                    detailToTextField.get(NicknameDetail.class).getText();

                NicknameDetail newDetail = null;
                if (!StringUtils.isNullOrEmpty(text, true))
                    newDetail = new NicknameDetail(text);

                if (nicknameDetail != null || newDetail != null)
                    changeDetail(nicknameDetail, newDetail);
            }

            if (accountInfoOpSet.isDetailClassSupported(URLDetail.class))
            {
                String text
                    = detailToTextField.get(URLDetail.class).getText();

                URL url = null;
                if (!StringUtils.isNullOrEmpty(text, true))
                    try
                    {
                        url = new URL(text);
                    }
                    catch (MalformedURLException e1)
                    {
                        logger.debug("Failed to update URL detail due to " +
                                        "malformed URL.");
                    }

                URLDetail newDetail = null;

                if (url != null)
                    newDetail = new URLDetail("URL", url);

                if (urlDetail != null || newDetail != null)
                    changeDetail(urlDetail, newDetail);
            }

            if (accountInfoOpSet.isDetailClassSupported(
                GenderDetail.class))
            {
                String text =
                    detailToTextField.get(GenderDetail.class).getText();

                GenderDetail newDetail = null;

                if (!StringUtils.isNullOrEmpty(text, true))
                    newDetail = new GenderDetail(text);

                if (genderDetail != null || newDetail != null)
                    changeDetail(genderDetail, newDetail);
            }

            if (accountInfoOpSet.isDetailClassSupported(
                BirthDateDetail.class))
            {
                BirthDateDetail newDetail = null;

                if (birthDayCalendar.getDate() != null)
                {
                    newDetail =
                        new BirthDateDetail(birthDayCalendar.getCalendar());
                }

                if (birthDateDetail != null || newDetail != null)
                    changeDetail(birthDateDetail, newDetail);
            }

            if (accountInfoOpSet.isDetailClassSupported(
                AddressDetail.class))
            {
                String text =
                    detailToTextField.get(AddressDetail.class).getText();

                AddressDetail newDetail = null;
                if (!StringUtils.isNullOrEmpty(text, true))
                    newDetail = new AddressDetail(text);

                if (streetAddressDetail != null || newDetail != null)
                    changeDetail(streetAddressDetail, newDetail);
            }

            if (accountInfoOpSet.isDetailClassSupported(
                CityDetail.class))
            {
                String text =
                    detailToTextField.get(CityDetail.class).getText();

                CityDetail newDetail = null;
                if (!StringUtils.isNullOrEmpty(text, true))
                    newDetail = new CityDetail(text);

                if (cityDetail != null || newDetail != null)
                    changeDetail(cityDetail, newDetail);
            }

            if (accountInfoOpSet.isDetailClassSupported(
                ProvinceDetail.class))
            {
                String text =
                    detailToTextField.get(ProvinceDetail.class).getText();

                ProvinceDetail newDetail = null;
                if (!StringUtils.isNullOrEmpty(text, true))
                newDetail = new ProvinceDetail(text);

                if (regionDetail != null || newDetail != null)
                    changeDetail(regionDetail, newDetail);
            }

            if (accountInfoOpSet.isDetailClassSupported(
                PostalCodeDetail.class))
            {
                String text =
                    detailToTextField.get(PostalCodeDetail.class).getText();

                PostalCodeDetail newDetail = null;
                if (!StringUtils.isNullOrEmpty(text, true))
                    newDetail = new PostalCodeDetail(text);

                if (postalCodeDetail != null || newDetail != null)
                    changeDetail(postalCodeDetail, newDetail);
            }

            if (accountInfoOpSet.isDetailClassSupported(CountryDetail.class))
            {
                String text = detailToTextField.get(CountryDetail.class).getText();

                CountryDetail newDetail = null;
                if (!StringUtils.isNullOrEmpty(text, true))
                    newDetail = new CountryDetail(text);

                if (countryDetail != null || newDetail != null)
                    changeDetail(countryDetail, newDetail);
            }

            if (accountInfoOpSet.isDetailClassSupported(
                EmailAddressDetail.class))
            {
                String text =
                    detailToTextField.get(EmailAddressDetail.class).getText();

                EmailAddressDetail newDetail = null;
                if (!StringUtils.isNullOrEmpty(text, true))
                    newDetail = new EmailAddressDetail(text);

                if (emailDetail != null || newDetail != null)
                    changeDetail(emailDetail, newDetail);
            }

            if (accountInfoOpSet.isDetailClassSupported(
                WorkEmailAddressDetail.class))
            {
                String text =
                    detailToTextField.get(WorkEmailAddressDetail.class)
                        .getText();

                WorkEmailAddressDetail newDetail = null;
                if (!StringUtils.isNullOrEmpty(text, true))
                    newDetail = new WorkEmailAddressDetail(text);

                if (workEmailDetail != null || newDetail != null)
                    changeDetail(workEmailDetail, newDetail);
            }

            if (accountInfoOpSet.isDetailClassSupported(
                PhoneNumberDetail.class))
            {
                String text =
                    detailToTextField.get(PhoneNumberDetail.class).getText();

                PhoneNumberDetail newDetail = null;
                if (!StringUtils.isNullOrEmpty(text, true))
                    newDetail = new PhoneNumberDetail(text);

                if (phoneDetail != null || newDetail != null)
                    changeDetail(phoneDetail, newDetail);
            }
            if (accountInfoOpSet.isDetailClassSupported(
                WorkPhoneDetail.class))
            {
                String text =
                    detailToTextField.get(WorkPhoneDetail.class).getText();

                WorkPhoneDetail newDetail = null;
                if (!StringUtils.isNullOrEmpty(text, true))
                    newDetail = new WorkPhoneDetail(text);

                if (workPhoneDetail != null || newDetail != null)
                    changeDetail(workPhoneDetail, newDetail);
            }
            if (accountInfoOpSet.isDetailClassSupported(
                MobilePhoneDetail.class))
            {
                String text =
                    detailToTextField.get(MobilePhoneDetail.class).getText();

                MobilePhoneDetail newDetail = null;
                if (!StringUtils.isNullOrEmpty(text, true))
                    newDetail = new MobilePhoneDetail(text);

                if (mobilePhoneDetail != null || newDetail != null)
                    changeDetail(mobilePhoneDetail, newDetail);
            }
            if (accountInfoOpSet.isDetailClassSupported(
                WorkOrganizationNameDetail.class))
            {
                String text =
                    detailToTextField.get(WorkOrganizationNameDetail.class)
                        .getText();

                WorkOrganizationNameDetail newDetail = null;
                if (!StringUtils.isNullOrEmpty(text, true))
                    newDetail = new WorkOrganizationNameDetail(text);

                if (organizationDetail != null || newDetail != null)
                    changeDetail(organizationDetail, newDetail);
            }

            if (accountInfoOpSet.isDetailClassSupported(JobTitleDetail.class))
            {
                String text =
                    detailToTextField.get(JobTitleDetail.class)
                        .getText();

                JobTitleDetail newDetail = null;
                if (!StringUtils.isNullOrEmpty(text, true))
                    newDetail = new JobTitleDetail(text);

                if (jobTitleDetail != null || newDetail != null)
                    changeDetail(jobTitleDetail, newDetail);
            }
            if (accountInfoOpSet.isDetailClassSupported(AboutMeDetail.class))
            {
                String text =
                    aboutMeArea.getText();

                AboutMeDetail newDetail = null;
                if (!StringUtils.isNullOrEmpty(text, true))
                    newDetail = new AboutMeDetail(text);

                if (aboutMeDetail != null || newDetail != null)
                    changeDetail(aboutMeDetail, newDetail);
            }

            try
            {
                dialog.close(false);
                //mainScrollPane.getVerticalScrollBar().setValue(0);
                accountInfoOpSet.save();
            }
            catch (OperationFailedException e1)
            {
                logger.debug("Failed to update account details.", e1);
            }
        }

        /**
         * A helper method to decide whether to add new
         * <tt>ServerStoredDetails</tt> or to replace an old one.
         * @param oldDetail the detail to be replaced.
         * @param newDetail the replacement.
         */
        private void changeDetail(  GenericDetail oldDetail,
                                    GenericDetail newDetail)
        {
            try
            {
                if (newDetail == null)
                {
                    accountInfoOpSet.removeDetail(oldDetail);
                }
                else if (oldDetail == null)
                {
                    accountInfoOpSet.addDetail(newDetail);
                }
                else
                {
                    accountInfoOpSet.replaceDetail(oldDetail, newDetail);
                }
            }
            catch (ArrayIndexOutOfBoundsException e1)
            {
                logger.debug("Failed to update account details. " +
                            newDetail.getDetailDisplayName() + " " + e1);
            }
            catch (OperationFailedException e1)
            {
                logger.debug("Failed to update account details. " +
                            newDetail.getDetailDisplayName() + " " + e1);
            }
        }
    }
}
