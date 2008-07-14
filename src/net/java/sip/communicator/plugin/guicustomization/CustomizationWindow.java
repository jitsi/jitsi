/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
/*
 * This code was edited or generated using CloudGarden's Jigloo SWT/Swing GUI
 * Builder, which is free for non-commercial use. If Jigloo is being used
 * commercially (ie, by a corporation, company or business for any purpose
 * whatever) then you should purchase a license for each developer using Jigloo.
 * Please visit www.cloudgarden.com for details. Use of Jigloo implies
 * acceptance of these licensing terms. A COMMERCIAL LICENSE HAS NOT BEEN
 * PURCHASED FOR THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED LEGALLY FOR
 * ANY CORPORATE OR COMMERCIAL PURPOSE.
 */
package net.java.sip.communicator.plugin.guicustomization;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import javax.swing.*;

import net.java.sip.communicator.plugin.guicustomization.resourcepack.*;
import net.java.sip.communicator.util.*;
import org.osgi.framework.*;

public class CustomizationWindow
    extends JFrame
{
    private JTabbedPane tabbedPane;

    private ImagesPanel imagesPanel;

    private SoundsPanel soundsPanel;

    private ColorsPanel colorsPanel;

    private I18nStringsPanel stringsPanel;

    private SettingsPanel settingsPanel;

    private Logger logger = Logger.getLogger(CustomizationWindow.class);

    private JButton saveProjectButton = new JButton("Create Skin");

    private JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private final String srcPackagePath = 
        "/net/java/sip/communicator/plugin/guicustomization/resourcepack";
    private final String manifestFileName = "customresourcepack.manifest.mf";
    
    private final String[] customClassFiles = new String[]{
        "CustomColorPackImpl",
        "CustomImagePackImpl",
        "CustomLanguagePackImpl",
        "CustomResourcePackActivator",
        "CustomSettingsPackImpl",
        "CustomSoundPackImpl"
    };
    
    private final String dstPackagePath = 
        "net/java/sip/communicator/plugin/guicustomization/resourcepack";
    
    public CustomizationWindow()
    {
        super("SIP Communicator Branding Studio");

        this.initGUI();
    }

    private void initGUI()
    {
        this.imagesPanel = new ImagesPanel(this);
        this.colorsPanel = new ColorsPanel();
        this.soundsPanel = new SoundsPanel(this);
        this.stringsPanel = new I18nStringsPanel();
        this.settingsPanel = new SettingsPanel();

        this.tabbedPane = new JTabbedPane();
        this.getContentPane().add(tabbedPane, BorderLayout.CENTER);
        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        this.tabbedPane.addTab("Images", imagesPanel);
        this.tabbedPane.addTab("Colors", colorsPanel);
        this.tabbedPane.addTab("Sounds", soundsPanel);
        this.tabbedPane.addTab("Strings", stringsPanel);
        this.tabbedPane.addTab("Settings", settingsPanel);

        this.buttonPanel.add(saveProjectButton);

        this.saveProjectButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                //TODO: Implement jar creation here.
                performSaveProject();
            }
        });

        this.setSize(570, 337);
    }
    
    private void performSaveProject()
    {
        try
        {
            File file = null;
            
            JFileChooser fileChooser = new JFileChooser();

            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setDialogTitle("Select Destination");
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));

            int result
                = fileChooser.showOpenDialog(CustomizationWindow.this);

            if (result == JFileChooser.APPROVE_OPTION)
            {
                file = fileChooser.getSelectedFile();
                file = new File(file, "customresources.jar");
            }
            else 
                return;
            
            
            InputStream manifestIs = 
                CustomizationWindow.class.
                    getResource(srcPackagePath + "/" + manifestFileName).
                    openStream();

            JarOutputStream outFile = 
                new JarOutputStream(
                    new FileOutputStream(file),
                    new Manifest(manifestIs));
            
            for (int i = 0; i < customClassFiles.length; i++)
            {
                String cf = customClassFiles[i];

                InputStream clIs = 
                    CustomizationWindow.class.
                    getResourceAsStream(srcPackagePath + "/" + cf + ".class");
                byte[] bs = new byte[clIs.available()];
                clIs.read(bs);
                clIs.close();

                addNewZipEntry(dstPackagePath + "/" + cf + ".class", outFile, bs);
            }
            
            saveColorPack(outFile);
            saveImagePack(outFile);
            saveLanguagePack(outFile);
            saveSettingsPack(outFile);
            saveSoundsPack(outFile);
            
            outFile.flush();
            outFile.close();
            manifestIs.close();
        }
        catch (Exception e)
        {

            logger.error("",e);
        }
    }
    
    private void saveColorPack(JarOutputStream outFile)
        throws Exception
    {
        String resources = 
            new CustomColorPackImpl().getResourcePackBaseName();
  
        Properties props = new Properties();
        
        Hashtable h = colorsPanel.getColors();
        Iterator<String> iter = h.keySet().iterator();
        while (iter.hasNext())
        {
            String k = iter.next();
            props.put(k, h.get(k));
        }

        ByteArrayOutputStream outB = new ByteArrayOutputStream();
        props.store(outB, "Custom Color resources");
        
        addNewZipEntry(resources.replaceAll("\\.", "/") + ".properties", 
            outFile, outB.toByteArray());
    }
    
    private void saveImagePack(JarOutputStream outFile)
        throws Exception
    {
        String resources = 
            new CustomImagePackImpl().getResourcePackBaseName();

        Properties props = new Properties();
        String imagePathPrefix = "resources/images/";
        int imageName = 0;
        
        Hashtable<String, byte[]> h = imagesPanel.getImages();
        Iterator<String> iter = h.keySet().iterator();
        while (iter.hasNext())
        {
            String k = iter.next();
            String fileName = imagePathPrefix + String.valueOf(imageName++) + ".png";
            byte[] bs = h.get(k);
            
            if(bs.length > 0)
            {
                props.put(k, fileName);

                addNewZipEntry(fileName, 
                    outFile, bs);
            }
            else
                props.put(k, "");
        }
        ByteArrayOutputStream outB = new ByteArrayOutputStream();
        props.store(outB, "Custom Color resources");
        
        addNewZipEntry(resources.replaceAll("\\.", "/") + ".properties", 
                    outFile, outB.toByteArray());
    }
    
    private void saveLanguagePack(JarOutputStream outFile)
        throws Exception
    {
        String resources = 
            new CustomLanguagePackImpl().getResourcePackBaseName();
  
        Hashtable languages = stringsPanel.getLanguages();
        Iterator<String> iter = languages.keySet().iterator();
        while (iter.hasNext())
        {
            String l = iter.next();
            Properties props = new Properties();
            
            Hashtable<String,String> strings = (Hashtable<String,String>)languages.get(l);
                
            Iterator<String> stringsIter = strings.keySet().iterator();
            while (stringsIter.hasNext())
            {
                String k = stringsIter.next();
                String v = strings.get(k);
                
                props.put(k, v);
            }
            
            String filename = null;
            if(l.equals("en"))
                filename = 
                    resources.replaceAll("\\.", "/") + ".properties";
            else
                filename = 
                    resources.replaceAll("\\.", "/") + "_" + l + ".properties";
            
            ByteArrayOutputStream outB = new ByteArrayOutputStream();
            props.store(outB, "Custom Color resources");
            
            addNewZipEntry(filename, 
                    outFile, outB.toByteArray());
        }
    }
    
    private void saveSettingsPack(JarOutputStream outFile)
        throws Exception
    {
        String resources = 
            new CustomSettingsPackImpl().getResourcePackBaseName();
  
        Properties props = new Properties();
        
        Hashtable h = settingsPanel.getSettings();
        Iterator<String> iter = h.keySet().iterator();
        while (iter.hasNext())
        {
            String k = iter.next();
            props.put(k, h.get(k));
        }

        ByteArrayOutputStream outB = new ByteArrayOutputStream();
        props.store(outB, "Custom Color resources");
        
        addNewZipEntry(resources.replaceAll("\\.", "/") + ".properties", 
                    outFile, outB.toByteArray());
        
        // fix for missing styles, must also add the styles css
        String entryName = 
            GuiCustomizationActivator.getResources().getSettingsString("textStyle");
        InputStream in = 
            GuiCustomizationActivator.getResources().getSettingsInputStream("textStyle");
        byte[] bs = new byte[in.available()];
        in.read(bs);
        in.close();
        
        addNewZipEntry(entryName, outFile, bs);
    }
    
    private void saveSoundsPack(JarOutputStream outFile)
        throws Exception
    {
        String resources = 
            new CustomSoundPackImpl().getResourcePackBaseName();
  
        Properties props = new Properties();
        
        String sndPathPrefix = "resources/sounds/";
        int sndName = 0;
        
        Hashtable<String,URL> h = soundsPanel.getSounds();
        Iterator<String> iter = h.keySet().iterator();
        while (iter.hasNext())
        {
            String k = iter.next();
            URL u = h.get(k);
            
            String fileName = sndPathPrefix + String.valueOf(sndName++);
            
            props.put(k, fileName);
            
            InputStream in = u.openStream();
            byte[] bs = new byte[in.available()];
            in.read(bs);
            
            addNewZipEntry(fileName, 
                    outFile, bs);
        }
        
        ByteArrayOutputStream outB = new ByteArrayOutputStream();
        props.store(outB, "Custom Color resources");
        
        addNewZipEntry(resources.replaceAll("\\.", "/") + ".properties", 
                    outFile, outB.toByteArray());
    }
    
    private void addNewZipEntry(String name, JarOutputStream outFile, byte[] data)
        throws Exception
    {
        ZipEntry z = new ZipEntry(name);
        outFile.putNextEntry(z);
        outFile.write(data);
    }
}
