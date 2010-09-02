package net.java.sip.communicator.impl.gui.main.presence.avatar.imagepicker;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;

import javax.imageio.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

public class ImagePickerDialog
    extends SIPCommDialog
    implements ActionListener
{
    private TransparentPanel fullEditPanel;
    
    private EditPanel editPanel;
    
    private JButton okButton, cancelButton;
    private JButton resetButton, selectFileButton, webcamButton;
    
    private boolean editCanceled = false;
    
    public ImagePickerDialog(int clipperZoneWidth, int clipperZoneHeight)
    {
        super();
        this.initComponents(clipperZoneWidth, clipperZoneHeight);
        this.initDialog();
        
        this.pack();
        this.setLocationRelativeTo(null);
    }

    private void initDialog()
    {
        this.setTitle(GuiActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.IMAGE_PICKER"));
        this.setModal(true);
        this.setResizable(true);
        
        
        this.setLayout(new BorderLayout());
        
        TransparentPanel editButtonsPanel = new TransparentPanel();
        editButtonsPanel.setLayout(new GridLayout(1, 3, 5, 0));
        editButtonsPanel.add(this.selectFileButton);
        editButtonsPanel.add(this.webcamButton);
        editButtonsPanel.add(this.resetButton);
        
        this.fullEditPanel = new TransparentPanel();
        this.fullEditPanel.setLayout(new BorderLayout());
        this.fullEditPanel.add(this.editPanel, BorderLayout.CENTER);
        this.fullEditPanel.add(editButtonsPanel, BorderLayout.SOUTH);
        
        TransparentPanel okCancelPanel = new TransparentPanel();
        okCancelPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        okCancelPanel.add(cancelButton);
        okCancelPanel.add(okButton);
        
        this.add(fullEditPanel, BorderLayout.CENTER);
        this.add(okCancelPanel, BorderLayout.SOUTH);
        
        this.pack();

    }
    
    private void initComponents(int clipperZoneWidth, int clipperZoneHeight)
    {        
        // Edit panel
        this.editPanel = new EditPanel(clipperZoneWidth, clipperZoneHeight);
        
        // Buttons
        this.okButton = new JButton(GuiActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.SET"));
        this.okButton.addActionListener(this);
        this.okButton.setName("okButton");
        
        this.cancelButton = new JButton(GuiActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.CANCEL"));
        this.cancelButton.addActionListener(this);
        this.cancelButton.setName("cancelButton");
        
        this.resetButton = new JButton(GuiActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.RESET"));
        this.resetButton.addActionListener(this);
        this.resetButton.setName("resetButton");
        
        this.selectFileButton = new JButton(GuiActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.CHOOSE_FILE"));
        this.selectFileButton.addActionListener(this);
        this.selectFileButton.setName("selectFileButton");
        
        this.webcamButton = new JButton(GuiActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.TAKE_PHOTO"));
        // disable it till we support it
        this.webcamButton.setEnabled(false);

        this.webcamButton.addActionListener(this);
        this.webcamButton.setName("webcamButton");
    }
    
    public byte[] showDialog(Image image)
    {
        if(image != null)
            this.editPanel.setImage(ImageUtils.getBufferedImage(image));

        this.setVisible(true);
        
        if (this.editCanceled)
            return null;
        else
            return this.editPanel.getClippedImage();
    }

    public void actionPerformed(ActionEvent e)
    {
        String name = ((JButton) e.getSource()).getName();
        if (name.equals("cancelButton"))
        {
            editCanceled = true;
            this.setVisible(false);
        }
        else if (name.equals("selectFileButton"))
        {
            SipCommFileChooser chooser = GenericFileDialog.create(
                GuiActivator.getUIService().getMainFrame(),
                GuiActivator.getResources().getI18NString(
                        "service.gui.avatar.imagepicker.CHOOSE_FILE"),
                SipCommFileChooser.LOAD_FILE_OPERATION);

            chooser.addFilter(new ImageFileFilter());

            File selectedFile = chooser.getFileFromDialog();
            if(selectedFile != null)
            {
                try
                {
                    BufferedImage image = ImageIO.read(selectedFile);
                    this.editPanel.setImage(image);
                }
                catch (IOException ioe)
                {
                    // TODO Auto-generated catch block
                    ioe.printStackTrace();
                }
            }
        }
        else if (name.equals("okButton"))
        {
            editCanceled = false;
            this.setVisible(false);
        }
        else if (name.equals("resetButton"))
        {
            this.editPanel.reset();
        }
        else
        {
//            WebcamDialog dialog = new WebcamDialog(this);
//            dialog.setVisible(true);
//            byte[] bimage = dialog.getGrabbedImage();
//
//            if (bimage != null)
//            {
//                Image i = new ImageIcon(bimage).getImage();
//                editPanel.setImage(ImageUtils.getBufferedImage(i));
//            }
        }
    }
    
    protected void close(boolean isEscaped)
    {
        dispose();
    }
    
    class ImageFileFilter extends SipCommFileFilter
    {
        public boolean accept(File f)
        {
            String path = f.getAbsolutePath().toLowerCase();
            if (path.matches("(.*)\\.(jpg|jpeg|png|bmp)$") ||
                    f.isDirectory())
                return true;

            else
                return false;
        }

        public String getDescription()
        {
            return GuiActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.IMAGE_FILES");
        }
    }
}
