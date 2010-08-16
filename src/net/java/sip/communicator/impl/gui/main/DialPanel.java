/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.CallManager;
import net.java.sip.communicator.impl.gui.main.call.ChooseCallAccountPopupMenu;
import net.java.sip.communicator.impl.gui.main.contactlist.SearchField;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>DialPanel</tt> is the panel that contains the buttons to dial a
 * phone number.
 *
 * @author Werner Dittmann
 * @author Yana Stamcheva
 */
public class DialPanel
    extends JPanel
    implements MouseListener
{
    /**
     * The dial panel.
     */
    private final JPanel dialPadPanel =
        new JPanel(new GridLayout(4, 3,
            GuiActivator.getResources()
                .getSettingsInt("impl.gui.DIAL_PAD_HORIZONTAL_GAP"),
            GuiActivator.getResources()
                .getSettingsInt("impl.gui.DIAL_PAD_VERTICAL_GAP")));
    
    private static final String CALL = "CALL";
    private static final String BACK = "BACK";
    private static final String DELETE = "DELETE";

    static final DialButtonInfo[] availableTones = new DialButtonInfo[] {
            new DialButtonInfo("1", ImageLoader.ONE_DIAL_BUTTON),
            new DialButtonInfo("2", ImageLoader.TWO_DIAL_BUTTON),
            new DialButtonInfo("3", ImageLoader.THREE_DIAL_BUTTON),
            new DialButtonInfo("4", ImageLoader.FOUR_DIAL_BUTTON),
            new DialButtonInfo("5", ImageLoader.FIVE_DIAL_BUTTON),
            new DialButtonInfo("6", ImageLoader.SIX_DIAL_BUTTON),
            new DialButtonInfo("7", ImageLoader.SEVEN_DIAL_BUTTON),
            new DialButtonInfo("8", ImageLoader.EIGHT_DIAL_BUTTON),
            new DialButtonInfo("9", ImageLoader.NINE_DIAL_BUTTON),
//            new DialButtonInfo("a", null), new DialButtonInfo("b", null),
//            new DialButtonInfo("c", null), new DialButtonInfo("d", null),
            new DialButtonInfo("*", ImageLoader.STAR_DIAL_BUTTON),
            new DialButtonInfo("0", ImageLoader.ZERO_DIAL_BUTTON),
            new DialButtonInfo("#", ImageLoader.DIEZ_DIAL_BUTTON) };

    private final MainFrameTouch mainFrameTouch;
    private final SearchField searchField;
    
    private final SIPCommButton callButton = new SIPCommButton(
            ImageLoader.getImage(ImageLoader.CALL_BUTTON_BG));

    private final SIPCommButton deleteButton = new SIPCommButton(
            ImageLoader.getImage(ImageLoader.DIAL_BUTTON_BG),
            ImageLoader.getImage(ImageLoader.DELETE_TEXT_ICON));
    
    private final SIPCommButton backButton = new SIPCommButton(
            ImageLoader.getImage(ImageLoader.DIAL_BUTTON_BG),
            ImageLoader.getImage(ImageLoader.PREVIOUS_ICON));
    
    private final JButton callContact = new JButton(
            GuiActivator.getResources().getI18NString("service.gui.CALL_CONTACT"),
            GuiActivator.getResources()
                .getImage("service.gui.icons.CALL_16x16_ICON"));

    
    /**
     * Creates an instance of <tt>DialPanel</tt>
     *
     * @param mfTouch The MainFrameTouch window that contains the tabs and 
     * other elements.
     * @param sField The search field that receives the input of the DialPanel.
     */
    public DialPanel(MainFrameTouch mfTouch, SearchField sField)
    {
        super(new BorderLayout());
        this.mainFrameTouch = mfTouch;
        this.searchField = sField;

        this.init();
    }

    /**
     * Initializes this panel by adding all dial buttons to it.
     */
    public void init()
    {
        this.dialPadPanel.setOpaque(false);

        this.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        int width = GuiActivator.getResources()
            .getSettingsInt("impl.gui.DIAL_PAD_WIDTH");

        int height = GuiActivator.getResources()
            .getSettingsInt("impl.gui.DIAL_PAD_HEIGHT");

        this.dialPadPanel.setPreferredSize(new Dimension(width, height));

        Image bgImage = ImageLoader.getImage(ImageLoader.DIAL_BUTTON_BG);

        for (int i = 0; i < availableTones.length; i++)
        {
            DialButtonInfo info = availableTones[i];
            // we add only buttons having image
            if(info.imageID == null)
                continue;

            dialPadPanel.add(
                createDialButton(bgImage, info.imageID, info.keyChar));
        }

        TransparentPanel centerPanel = new TransparentPanel();
        centerPanel.add(dialPadPanel, BorderLayout.CENTER);
        
        TransparentPanel southPanel = new TransparentPanel(new GridLayout(1, 4));
        
        deleteButton.setName(DELETE);
        deleteButton.addMouseListener(this);
        southPanel.add(deleteButton);
        
        backButton.setName(BACK);
        backButton.addMouseListener(this);
        southPanel.add(backButton);
        
        southPanel.add(Box.createHorizontalStrut(10));
        
        callButton.setName(CALL);
        callButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                String searchText = searchField.getText();

                if (searchText == null || searchText.length() == 0)
                    return;

                List<ProtocolProviderService> telephonyProviders
                    = CallManager.getTelephonyProviders();

                if (telephonyProviders.size() == 1)
                {
                    CallManager.createCall(
                        telephonyProviders.get(0), searchText);
                }
                else if (telephonyProviders.size() > 1)
                {
                    ChooseCallAccountPopupMenu chooseAccountDialog
                        = new ChooseCallAccountPopupMenu(
                            callContact,
                            searchText,
                            telephonyProviders);

                    chooseAccountDialog.showPopupMenu();
                }
            }
        });
        southPanel.add(callButton);
        
        this.add(centerPanel, BorderLayout.CENTER);
        this.add(southPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates DTMF button.
     * @param bgImage
     * @param iconImage
     * @param name
     * @return
     */
    private JButton createDialButton(Image bgImage, ImageID iconImage,
        String name)
    {
        JButton button =
            new SIPCommButton(bgImage, ImageLoader.getImage(iconImage));

        button.setAlignmentY(JButton.LEFT_ALIGNMENT);
        button.setName(name);
        button.setOpaque(false);
        button.addMouseListener(this);
        return button;
    }

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    /**
     * Handles the <tt>MouseEvent</tt> triggered when user presses one of the
     * dial buttons.
     * @param e the event
     */
    public void mousePressed(MouseEvent e)
    {
        JButton button = (JButton) e.getSource();
        String name = button.getName();
        String inText = searchField.getText();
        
        if (BACK.equals(name)) {
            if (inText == null || inText.length() == 0)
                return;
            if (inText.length() == 1) {
                searchField.setText(null);
                return;
            }
            String newText = inText.substring(0, inText.length() - 1);
            searchField.setText(newText);
            return;
        }
        if (DELETE.equals(name)) {
            searchField.setText(null);
            return;            
        }
        if (inText == null || inText.length() == 0) {
            searchField.setText(name);
            return;
        }
        searchField.setText(inText + name);        
    }

    /**
     * Handles the <tt>MouseEvent</tt> triggered when user releases one of the
     * dial buttons.
     * @param e the event
     */
    public void mouseReleased(MouseEvent e)
    {
    }

    /**
     * Paints the main background image to the background of this dial panel.
     */
    public void paintComponent(Graphics g)
    {
     // do the superclass behavior first
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        boolean isTextureBackground = Boolean.parseBoolean(GuiActivator.getResources()
            .getSettingsString("impl.gui.IS_CONTACT_LIST_TEXTURE_BG_ENABLED"));

        BufferedImage bgImage
            = ImageLoader.getImage(ImageLoader.MAIN_WINDOW_BACKGROUND);

        // paint the image
        if (bgImage != null)
        {
            if (isTextureBackground)
            {
                Rectangle rect
                    = new Rectangle(0, 0,
                            bgImage.getWidth(null),
                            bgImage.getHeight(null));

                TexturePaint texture = new TexturePaint(bgImage, rect);

                g2.setPaint(texture);

                g2.fillRect(0, 0, this.getWidth(), this.getHeight());
            }
            else
            {
                g.setColor(new Color(
                    GuiActivator.getResources()
                        .getColor("contactListBackground")));

                // paint the background with the choosen color
                g.fillRect(0, 0, getWidth(), getHeight());

                g2.drawImage(bgImage,
                        this.getWidth() - bgImage.getWidth(),
                        this.getHeight() - bgImage.getHeight(),
                        this);
            }
        }
    }
    /**
     * Dial button information
     */
    static class DialButtonInfo
    {
        /**
         * The char associated with this dial button.
         */
        String keyChar;

        /**
         * The image to display in buttons sending DTMFs.
         */
        ImageID imageID;

        /**
         * Creates button info.
         * @param keyChar the char associated with the DTMF
         * @param imageID the image if any.
         */
        public DialButtonInfo(
            String keyChar,
            ImageID imageID)
        {
            this.keyChar = keyChar;
            this.imageID = imageID;
        }
    }

}
