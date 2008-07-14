package net.java.sip.communicator.plugin.guicustomization;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

import javax.imageio.*;
import javax.swing.*;
import javax.swing.table.*;

import net.java.sip.communicator.util.*;

public class ImagesPanel
    extends JScrollPane
{
    private Logger logger = Logger.getLogger(ImagesPanel.class);

    private CustomTableModel imagesTableModel = new CustomTableModel();

    private JTable imagesTable = new JTable();

    private JFrame parentWindow;

    public ImagesPanel(JFrame parentWindow)
    {
        this.parentWindow = parentWindow;

        this.getViewport().add(imagesTable);

        imagesTableModel.addColumn("File path");
        imagesTableModel.addColumn("Image size");
        imagesTableModel.addColumn("Image");
        imagesTableModel.addColumn("Change image");

        imagesTable.setModel(imagesTableModel);

        TableColumn sizeColumn
            = imagesTable.getColumnModel().getColumn(1);

        sizeColumn.setCellRenderer(
            new LabelTableCellRenderer());
        sizeColumn.setMaxWidth(100);

        imagesTable.getColumnModel().getColumn(2).setCellRenderer(
            new LabelTableCellRenderer());

        TableColumn buttonColumn
            = imagesTable.getColumnModel().getColumn(3);
        buttonColumn.setCellRenderer(new ButtonTableCellRenderer());
        buttonColumn.setCellEditor(new ButtonTableEditor());

        this.initImageTable();
    }

    private void initImageTable()
    {
        Iterator imageKeys
            = GuiCustomizationActivator.getResources()
                .getCurrentImages();

        // we set an initial row height, to fit the button.
        int rowHeight = 40;

        while (imageKeys.hasNext())
        {
            String key = (String) imageKeys.next();
            ImageIcon image = getImage(key);
            final JLabel imageLabel = new JLabel();
            JLabel imageSizeLabel = new JLabel();
            int currentImageWidth = 0;
            int currentImageHeight = 0;

            if (image != null)
            {
                imageLabel.setIcon(image);
                currentImageWidth = image.getImage().getWidth(null);
                currentImageHeight = image.getImage().getHeight(null);

                imageSizeLabel
                    .setText(currentImageWidth + "x" + currentImageHeight);
                
                images.put(key, getImageBytes(key));
            }

            JButton fileChooserButton = new JButton();

            fileChooserButton.setAction(
                new ChangeImageAction(  key,
                                        imageLabel,
                                        imageSizeLabel,
                                        currentImageWidth,
                                        currentImageHeight));

            imagesTableModel.addRow(new Object[]{   key,
                                                    imageSizeLabel,
                                                    imageLabel,
                                                    fileChooserButton});

            fileChooserButton.setText("Change image");

            if (image != null && rowHeight < image.getIconHeight())
            {
                imagesTable.setRowHeight(   imagesTableModel.getRowCount() - 1,
                                            image.getIconHeight() );
            }
            else
            {
                imagesTable.setRowHeight(   imagesTableModel.getRowCount() - 1,
                                            rowHeight );
            }
        }
    }

    
    /**
     * Loads an image from a given image identifier.
     * 
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    private ImageIcon getImage(String imageID)
    {
        BufferedImage image = null;

        InputStream in = 
            GuiCustomizationActivator.getResources()
                .getImageInputStream(imageID);

        if(in == null)
            return null;

        try
        {
            image = ImageIO.read(in);
        }
        catch (IOException e)
        {
            logger.error("Failed to load image:" + imageID, e);
        }

        return new ImageIcon(image);
    }
    
    /**
     * Loads an image from a given image identifier.
     * 
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    private byte[] getImageBytes(String imageID)
    {
        InputStream in = 
            GuiCustomizationActivator.getResources()
                .getImageInputStream(imageID);

        if(in == null)
            return null;

        try
        {
            byte[] bs = new byte[in.available()];
            in.read(bs);

            return bs;
        }
        catch (IOException e)
        {
            logger.error("Failed to load image:" + imageID, e);
        }
        
        return null;
    }

    private class ChangeImageAction extends AbstractAction
    {
        private JLabel imageLabel;
        private JLabel imageSizeLabel;
        private int imageWidth;
        private int imageHeight;
        private String key;

        public ChangeImageAction(   String key,
                                    JLabel imageLabel,
                                    JLabel imageSizeLabel,
                                    int imageWidth,
                                    int imageHeight)
        {
            this.key = key;
            this.imageLabel = imageLabel;
            this.imageSizeLabel = imageSizeLabel;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
        }

        public void actionPerformed(ActionEvent evt)
        {
            JFileChooser fileChooser
                = new JFileChooser();

            int result
                = fileChooser.showOpenDialog(parentWindow);

            if (result == JFileChooser.APPROVE_OPTION)
            {
                File newImageFile = fileChooser.getSelectedFile();
                ImageIcon newImageIcon = new ImageIcon(newImageFile.getPath());

                try
                {
                    FileInputStream in = new FileInputStream(newImageFile);
                    byte[] bs = new byte[in.available()];
                    in.read(bs);
                    in.close();
                    
                    images.put(key, bs);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                
                imageLabel.setIcon(newImageIcon);

                if (newImageIcon.getIconWidth() > imageWidth
                    || newImageIcon.getIconHeight() > imageHeight)
                {
                    imageSizeLabel.setBackground(Color.RED);
                }
            }
        }
    }
    
//    public static byte[] convertImage(Image img) 
//    {
//        try 
//        {
//            int[] pix = new int[img.getWidth(null) * img.getHeight(null)];
//            PixelGrabber pg = 
//                new PixelGrabber(img, 0, 0, img.getWidth(null),
//                                img.getHeight(null), pix, 0, img.getWidth(null));
//            pg.grabPixels();
//
//            byte[] pixels = new byte[img.getWidth(null) * img.getHeight(null)];
//
//            for (int j = 0; j < pix.length; j++) 
//                pixels[j] = new Integer(pix[j]).byteValue();
//
//            return pixels;
//        }
//        catch (InterruptedException e) 
//        {
//            e.printStackTrace();
//        }
//        return null;
//    }
     
    Hashtable<String, byte[]> images = new Hashtable<String, byte[]>();
    
    Hashtable<String, byte[]> getImages()
    {
        return images;
//        Hashtable res = new Hashtable();
//        int rows = imagesTableModel.getRowCount();
//        for (int i = 0; i < rows; i++)
//        {
//            String key = (String)imagesTableModel.getValueAt(i, 0);
//            JLabel imageLabel = (JLabel)imagesTableModel.getValueAt(i, 2);
//            
//            Icon icon = imageLabel.getIcon();
//            if(icon != null && icon instanceof ImageIcon)
//                res.put(
//                    key,
//                    convertImage(((ImageIcon)icon).getImage()));
//            else
//                res.put(key,new byte[0]);
//        }
//
//        return res;
    }
}
