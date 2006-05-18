/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import net.java.sip.communicator.util.Logger;

public class ImageLoader {

	private static Logger log = Logger.getLogger(ImageLoader.class);
    
    private static Hashtable   loadedImages = new Hashtable();

    /*------------------------------------------------------------------------
     * =========================LOOK AND FEEL IMAGES========================== 
     * -----------------------------------------------------------------------
     */
    public static final ImageID BUTTON = new ImageID("BUTTON");
    
    public static final ImageID BUTTON_ROLLOVER
        = new ImageID("BUTTON_ROLLOVER");
    
    public static final ImageID SPLITPANE_HORIZONTAL
        = new ImageID("SPLITPANE_HORIZONTAL");
    
    public static final ImageID SPLITPANE_VERTICAL
        = new ImageID("SPLITPANE_VERTICAL");

    public static final ImageID SCROLLBAR_THUMB_VERTICAL
        = new ImageID("SCROLLBAR_VERTICAL");
    
    public static final ImageID SCROLLBAR_THUMB_HORIZONTAL
        = new ImageID("SCROLLBAR_HORIZONTAL");
    
    public static final ImageID SCROLLBAR_THUMB_HANDLE_HORIZONTAL
        = new ImageID("SCROLLBAR_THUMB_HORIZONTAL");
    
    public static final ImageID SCROLLBAR_THUMB_HANDLE_VERTICAL
        = new ImageID("SCROLLBAR_THUMB_VERTICAL");
    
    /*------------------------------------------------------------------------
     * ============================APPLICATION ICONS =========================
     * -----------------------------------------------------------------------
     */
	public static final ImageID EMPTY_16x16_ICON
											= new ImageID("EMPTY_16x16_ICON");

	public static final ImageID QUICK_MENU_ADD_ICON
											= new ImageID("QUICK_MENU_ADD_ICON");

	public static final ImageID QUICK_MENU_CONFIGURE_ICON
											= new ImageID("QUICK_MENU_CONFIGURE_ICON");

	public static final ImageID QUICK_MENU_SEARCH_ICON
											= new ImageID("QUICK_MENU_SEARCH_ICON");

	public static final ImageID QUICK_MENU_INFO_ICON
											= new ImageID("QUICK_MENU_INFO_ICON");

	public static final ImageID QUICK_MENU_BUTTON_BG
											= new ImageID("QUICK_MENU_BUTTON_BG");

	public static final ImageID QUICK_MENU_BUTTON_ROLLOVER_BG
											= new ImageID("QUICK_MENU_BUTTON_ROLLOVER_BG");

	public static final ImageID CALL_BUTTON_BG
											= new ImageID("CALL_BUTTON_BG");

	public static final ImageID HANGUP_BUTTON_BG
											= new ImageID("HANGUP_BUTTON_BG");

	public static final ImageID CALL_ROLLOVER_BUTTON_BG
											= new ImageID("CALL_ROLLOVER_BUTTON_BG");

	public static final ImageID HANGUP_ROLLOVER_BUTTON_BG
											= new ImageID("HANGUP_ROLLOVER_BUTTON_BG");

	public static final ImageID STATUS_SELECTOR_BOX
											= new ImageID("STATUS_SELECTOR_BOX");

	public static final ImageID BUTTON_BG 	= new ImageID("BUTTON_BG");

	public static final ImageID BUTTON_ROLLOVER_BG
											= new ImageID("BUTTON_ROLLOVER_BG");

	public static final ImageID ONE_DIAL_BUTTON
											= new ImageID("ONE_DIAL_BUTTON");

	public static final ImageID TWO_DIAL_BUTTON
											= new ImageID("TWO_DIAL_BUTTON");

	public static final ImageID THREE_DIAL_BUTTON
											= new ImageID("THREE_DIAL_BUTTON");

	public static final ImageID FOUR_DIAL_BUTTON
											= new ImageID("FOUR_DIAL_BUTTON");

	public static final ImageID FIVE_DIAL_BUTTON
											= new ImageID("FIVE_DIAL_BUTTON");

	public static final ImageID SIX_DIAL_BUTTON
											= new ImageID("SIX_DIAL_BUTTON");

	public static final ImageID SEVEN_DIAL_BUTTON
											= new ImageID("SEVEN_DIAL_BUTTON");

	public static final ImageID EIGHT_DIAL_BUTTON
											= new ImageID("EIGHT_DIAL_BUTTON");

	public static final ImageID NINE_DIAL_BUTTON
											= new ImageID("NINE_DIAL_BUTTON");

	public static final ImageID STAR_DIAL_BUTTON
											= new ImageID("STAR_DIAL_BUTTON");

	public static final ImageID ZERO_DIAL_BUTTON
											= new ImageID("ZERO_DIAL_BUTTON");

	public static final ImageID DIEZ_DIAL_BUTTON
											= new ImageID("DIEZ_DIAL_BUTTON");

	public static final ImageID DEFAULT_USER_PHOTO
											= new ImageID("DEFAULT_USER_PHOTO");

	public static final ImageID DEFAULT_CHAT_USER_PHOTO
											= new ImageID("DEFAULT_CHAT_USER_PHOTO");

	public static final ImageID CALL_PANEL_MINIMIZE_BUTTON
											= new ImageID("CALL_PANEL_MINIMIZE_BUTTON");

	public static final ImageID CALL_PANEL_RESTORE_BUTTON
											= new ImageID("CALL_PANEL_RESTORE_BUTTON");

	public static final ImageID CALL_PANEL_MINIMIZE_ROLLOVER_BUTTON
											= new ImageID("CALL_PANEL_MINIMIZE_ROLLOVER_BUTTON");

	public static final ImageID CALL_PANEL_RESTORE_ROLLOVER_BUTTON
											= new ImageID("CALL_PANEL_RESTORE_ROLLOVER_BUTTON");


	public static final ImageID ADD_TO_CHAT_BUTTON
											= new ImageID("ADD_TO_CHAT_BUTTON");

	public static final ImageID ADD_TO_CHAT_ROLLOVER_BUTTON
											= new ImageID("ADD_TO_CHAT_ROLLOVER_BUTTON");

	public static final ImageID ADD_TO_CHAT_ICON
											= new ImageID("ADD_TO_CHAT_ICON");

	public static final ImageID TOOLBAR_DIVIDER
											= new ImageID("TOOLBAR_DIVIDER");

	public static final ImageID RIGHT_ARROW_ICON
											= new ImageID("RIGHT_ARROW_ICON");

	public static final ImageID RIGHT_ARROW_ROLLOVER_ICON
											= new ImageID("RIGHT_ARROW_ROLLOVER_ICON");

	public static final ImageID BOTTOM_ARROW_ICON
											= new ImageID("BOTTOM_ARROW_ICON");

	public static final ImageID BOTTOM_ARROW_ROLLOVER_ICON
											= new ImageID("BOTTOM_ARROW_ROLLOVER_ICON");
    
    public static final ImageID TAB_BG 
                                            = new ImageID("TAB_BG"); 
    
    public static final ImageID SELECTED_TAB_BG 
                                            = new ImageID("SELECTED_TAB_BG");
    
    public static final ImageID CLOSABLE_TAB_BG
                                            = new ImageID("CLOSABLE_TAB_BG");

    public static final ImageID SELECTED_CLOSABLE_TAB_BG
                                            = new ImageID("SELECTED_CLOSABLE_TAB_BG");
    
	/////////////////////// Edit Text Toolbar icons //////////////////////////

	public static final ImageID ALIGN_LEFT_BUTTON
											= new ImageID("ALIGN_LEFT_BUTTON");

	public static final ImageID ALIGN_RIGHT_BUTTON
											= new ImageID("ALIGN_RIGHT_BUTTON");

	public static final ImageID ALIGN_CENTER_BUTTON
											= new ImageID("ALIGN_RIGHT_BUTTON");

	public static final ImageID ALIGN_LEFT_ROLLOVER_BUTTON
											= new ImageID("ALIGN_LEFT_ROLLOVER_BUTTON");

	public static final ImageID ALIGN_RIGHT_ROLLOVER_BUTTON
											= new ImageID("ALIGN_RIGHT_ROLLOVER_BUTTON");

	public static final ImageID ALIGN_CENTER_ROLLOVER_BUTTON
											= new ImageID("ALIGN_CENTER_ROLLOVER_BUTTON");

	public static final ImageID TEXT_BOLD_BUTTON
											= new ImageID("TEXT_BOLD_BUTTON");

	public static final ImageID TEXT_ITALIC_BUTTON
											= new ImageID("TEXT_ITALIC_BUTTON");

	public static final ImageID TEXT_UNDERLINED_BUTTON
											= new ImageID("TEXT_ITALIC_BUTTON");

	public static final ImageID TEXT_BOLD_ROLLOVER_BUTTON
											= new ImageID("TEXT_BOLD_ROLLOVER_BUTTON");

	public static final ImageID TEXT_ITALIC_ROLLOVER_BUTTON
											= new ImageID("TEXT_ITALIC_ROLLOVER_BUTTON");

	public static final ImageID TEXT_UNDERLINED_ROLLOVER_BUTTON
											= new ImageID("TEXT_UNDERLINED_ROLLOVER_BUTTON");

    public static final ImageID CLOSE_TAB_ICON
                                    = new ImageID("CLOSE_TAB_ICON");
    
    public static final ImageID CLOSE_TAB_SELECTED_ICON
                                    = new ImageID("CLOSE_TAB_SELECTED_ICON");
    
	// ///////////////////////// Main Toolbar icons ////////////////////////////

	public static final ImageID MSG_TOOLBAR_BUTTON_BG
											= new ImageID("MSG_TOOLBAR_BUTTON_BG");

	public static final ImageID MSG_TOOLBAR_ROLLOVER_BUTTON_BG
											= new ImageID("MSG_TOOLBAR_ROLLOVER_BUTTON_BG");

	public static final ImageID COPY_ICON 	= new ImageID("COPY_ICON");

	public static final ImageID CUT_ICON 	= new ImageID("CUT_ICON");

	public static final ImageID PASTE_ICON 	= new ImageID("PASTE_ICON");

	public static final ImageID SMILIES_ICON
											= new ImageID("SMILIES_ICON");

	public static final ImageID SAVE_ICON 	= new ImageID("SAVE_ICON");

	public static final ImageID PRINT_ICON 	= new ImageID("PRINT_ICON");

	public static final ImageID CLOSE_ICON 	= new ImageID("CLOSE_ICON");

	public static final ImageID QUIT_ICON 	= new ImageID("QUIT_ICON");

	public static final ImageID PREVIOUS_ICON
											= new ImageID("PREVIOUS_ICON");

	public static final ImageID NEXT_ICON 	= new ImageID("NEXT_ICON");

	public static final ImageID HISTORY_ICON
											= new ImageID("HISTORY_ICON");

	public static final ImageID SEND_FILE_ICON
											= new ImageID("SEND_FILE_ICON");


	public static final ImageID FONT_ICON	= new ImageID("FONT_ICON");

	// ///////////////////// Chat contact icons ////////////////////////////////

	public static final ImageID CHAT_CONTACT_INFO_BUTTON
		= new ImageID("CHAT_CONTACT_INFO_BUTTON");

	public static final ImageID CHAT_CONTACT_INFO_ROLLOVER_BUTTON
		= new ImageID("CHAT_CONTACT_INFO_ROLLOVER_BUTTON");

	public static final ImageID CHAT_CONTACT_CALL_BUTTON
		= new ImageID("CHAT_CONTACT_CALL_BUTTON");

	public static final ImageID CHAT_CONTACT_CALL_ROLLOVER_BUTTON
		= new ImageID("CHAT_CONTACT_CALL_ROLLOVER_BUTTON");

	public static final ImageID CHAT_CONTACT_SEND_FILE_BUTTON
		= new ImageID("CHAT_CONTACT_SEND_FILE_BUTTON");

	public static final ImageID CHAT_SEND_FILE_ROLLOVER_BUTTON
		= new ImageID("CHAT_SEND_FILE_ROLLOVER_BUTTON");

	// ///////////////////// Optionpane icons /////////////////////////////

	public static final ImageID WARNING_ICON
											= new ImageID("WARNING_ICON");

	// //////////////////// RightButton menu icons ////////////////////////

	public static final ImageID SEND_MESSAGE_16x16_ICON
	    = new ImageID("SEND_MESSAGE_16x16_ICON");

	public static final ImageID DELETE_16x16_ICON
		= new ImageID("DELETE_16x16_ICON");

	public static final ImageID HISTORY_16x16_ICON
		= new ImageID("HISTORY_16x16_ICON");

	public static final ImageID SEND_FILE_16x16_ICON
		= new ImageID("SEND_FILE_16x16_ICON");

	public static final ImageID GROUPS_16x16_ICON
		= new ImageID("GROUPS_16x16_ICON");

	public static final ImageID INFO_16x16_ICON
		= new ImageID("INFO_16x16_ICON");

	public static final ImageID ADD_CONTACT_16x16_ICON
	    = new ImageID("ADD_CONTACT_16x16_ICON");

	public static final ImageID RENAME_16x16_ICON
	    = new ImageID("RENAME_16x16_ICON");

	public static final ImageID MORE_INFO_ICON
	    = new ImageID("MORE_INFO_ICON");

	public static final ImageID TOOLBAR_DRAG_ICON
	    = new ImageID("TOOLBAR_DRAG_ICON");
    
    public static final ImageID LOGIN_WINDOW_LOGO
        = new ImageID("LOGIN_WINDOW_LOGO");
	
	/*
	 * =========================================================================
	 * --------------------- PROTOCOLS STATUS ICONS ---------------------------
	 * ========================================================================
	 */

	public static final ImageID ICQ_LOGO 	= new ImageID("ICQ_LOGO");

    public static final ImageID ICQ_CONNECTING 
                                            = new ImageID("ICQ_CONNECTING");
    
	public static final ImageID ICQ_FF_CHAT_ICON
											= new ImageID("ICQ_FF_CHAT_ICON");

	public static final ImageID ICQ_AWAY_ICON
											= new ImageID("ICQ_AWAY_ICON");

	public static final ImageID ICQ_NA_ICON = new ImageID("ICQ_NA_ICON");

	public static final ImageID ICQ_DND_ICON
											= new ImageID("ICQ_DND_ICON");

	public static final ImageID ICQ_OCCUPIED_ICON
											= new ImageID("ICQ_OCCUPIED_ICON");

	public static final ImageID ICQ_OFFLINE_ICON
											= new ImageID("ICQ_OFFLINE_ICON");

	public static final ImageID ICQ_INVISIBLE_ICON
											= new ImageID("ICQ_INVISIBLE_ICON");

	public static final ImageID MSN_LOGO	= new ImageID("MSN_LOGO");


	public static final ImageID	AIM_LOGO 	= new ImageID("AIM_LOGO");

	public static final ImageID YAHOO_LOGO 	= new ImageID("YAHOO_LOGO");

	public static final ImageID JABBER_LOGO = new ImageID("JABBER_LOGO");

	public static final ImageID SKYPE_LOGO 	= new ImageID("SKYPE_LOGO");

	public static final ImageID SIP_LOGO 	= new ImageID("SIP_LOGO");


	public static final ImageID SIP_ONLINE_ICON
											= new ImageID("SIP_ONLINE_ICON");

	public static final ImageID SIP_OFFLINE_ICON
											= new ImageID("SIP_OFFLINE_ICON");

	public static final ImageID SIP_INVISIBLE_ICON
											= new ImageID("SIP_INVISIBLE_ICON");

	public static final ImageID SIP_AWAY_ICON
											= new ImageID("SIP_AWAY_ICON");

	public static final ImageID SIP_NA_ICON	= new ImageID("SIP_NA_ICON");

	public static final ImageID SIP_DND_ICON
											= new ImageID("SIP_DND_ICON");

	public static final ImageID SIP_OCCUPIED_ICON
											= new ImageID("SIP_OCCUPIED_ICON");

	public static final ImageID SIP_CHAT_ICON
											= new ImageID("SIP_CHAT_ICON");

	/*
	 * =====================================================================
	 * ------------------------ USERS ICONS --------------------------------
	 * =====================================================================
	 */

	public static final ImageID USER_ONLINE_ICON
											= new ImageID("USER_ONLINE_ICON");

    public static final ImageID USER_OFFLINE_ICON
                                            = new ImageID("USER_OFFLINE_ICON");
    
    public static final ImageID USER_AWAY_ICON
                                            = new ImageID("USER_AWAY_ICON");
    
    public static final ImageID USER_NA_ICON
                                            = new ImageID("USER_NA_ICON");
    
    public static final ImageID USER_FFC_ICON
                                            = new ImageID("USER_FFC_ICON");
    
    public static final ImageID USER_DND_ICON
                                            = new ImageID("USER_DND_ICON");
    
    public static final ImageID USER_OCCUPIED_ICON
                                            = new ImageID("USER_OCCUPIED_ICON");

    /*
	 * =====================================================================
	 * ---------------------------- SMILIES --------------------------------
	 * =====================================================================
	 */

	public static final ImageID SMILEY1 = new ImageID("SMILEY1");

	public static final ImageID SMILEY2 = new ImageID("SMILEY2");

	public static final ImageID SMILEY3 = new ImageID("SMILEY3");

	public static final ImageID SMILEY4 = new ImageID("SMILEY4");

	public static final ImageID SMILEY5 = new ImageID("SMILEY5");

	public static final ImageID SMILEY6 = new ImageID("SMILEY6");

	public static final ImageID SMILEY7 = new ImageID("SMILEY7");

	public static final ImageID SMILEY8 = new ImageID("SMILEY8");

	public static final ImageID SMILEY9 = new ImageID("SMILEY9");

	public static final ImageID SMILEY10 = new ImageID("SMILEY10");

	public static final ImageID SMILEY11 = new ImageID("SMILEY11");

	public static final ImageID SMILEY12 = new ImageID("SMILEY12");

	/**
	 * Load default smilies pack
	 *
	 * @return the ArrayList of all smilies.
	 *
	 */
	public static ArrayList getDefaultSmiliesPack() {

		ArrayList defaultPackList = new ArrayList();

		defaultPackList.add(new Smiley(ImageLoader.SMILEY1,
							new String[] { "$-)", "$)" }));

		defaultPackList.add(new Smiley(ImageLoader.SMILEY2,
							new String[] { "8-)", "8)" }));

		defaultPackList.add(new Smiley(ImageLoader.SMILEY3,
							new String[] { ":-*", ":*" }));

		defaultPackList.add(new Smiley(ImageLoader.SMILEY4,
							new String[] { ":-0", ":0" }));

		defaultPackList.add(new Smiley(ImageLoader.SMILEY5,
							new String[] { ":-((", ":((" }));

		defaultPackList.add(new Smiley(ImageLoader.SMILEY6,
							new String[] { ":-~", ":~" }));

		defaultPackList.add(new Smiley(ImageLoader.SMILEY7,
							new String[] { ":-|", ":|" }));

		defaultPackList.add(new Smiley(ImageLoader.SMILEY8,
							new String[] { ":-P", ":P", ":-p", ":p" }));

		defaultPackList.add(new Smiley(ImageLoader.SMILEY9,
							new String[] { ":-))", ":))" }));

		defaultPackList.add(new Smiley(ImageLoader.SMILEY10,
							new String[] { ":-(", ":(" }));

		defaultPackList.add(new Smiley(ImageLoader.SMILEY11,
							new String[] { ":-)", ":)" }));

		defaultPackList.add(new Smiley(ImageLoader.SMILEY12,
							new String[] { ";-)", ";)" }));

		return defaultPackList;
	}

    public static Smiley getSmiley(String smileyString){
        ArrayList smiliesList = getDefaultSmiliesPack();
        
        for (int i = 0; i < smiliesList.size(); i++) {

            Smiley smiley = (Smiley) smiliesList.get(i);

            String[] smileyStrings = smiley.getSmileyStrings();

            for (int j = 0; j < smileyStrings.length; j++) {
                
                String srcString = smileyStrings[j];
                
                if(srcString.equals(smileyString))
                    return smiley;
            }
        }
        return null;
    }
    
	/**
	 * Loads an image from a given image identifier.
	 */

	public static BufferedImage getImage(ImageID imageID) {

        BufferedImage image = null;
        
        if(loadedImages.containsKey(imageID)){
            
            image = (BufferedImage)loadedImages.get(imageID);
        }
        else {            
            String path = Images.getString(imageID.getId());
            try {
                image = ImageIO.read(ImageLoader.class.getClassLoader().getResource(path));
                
                loadedImages.put(imageID, image);
                
            } catch (IOException e) {
                log.error("Failed to load image:" + path, e);
            }
        }
		
        return image;
	}

    /**
     * Loads animated gif image.
     */
    
    public static BufferedImage[] getAnimatedImage(ImageID imageID){
        
        String path = Images.getString(imageID.getId());
        
        URL  url = ImageLoader.class.getClassLoader()
                        .getResource(path);
        
        Iterator readers = ImageIO.getImageReadersBySuffix("gif");
        
        ImageReader reader = (ImageReader) readers.next();
              
        ImageInputStream iis;
        
        BufferedImage[] images = null;
        
        try {
            iis = ImageIO.createImageInputStream(url.openStream());
            
            reader.setInput(iis);
            
            final int numImages;
            
            numImages = reader.getNumImages(true);
            
            images = new BufferedImage[numImages];
                       
            for(int i=0; i<numImages; ++i) {
                images[i] =  reader.read(i);            
            }
            
        } catch (IOException e) {
            log.error("Failed to load image:" + path, e);
        } finally {
            log.logExit();
        }

        return images;
    }
    
	/**
	 *  Represents the Image Identifier.
	 */
	public static class ImageID {

		private String id;

		private ImageID(String id){

			this.id = id;
		}

		public String getId() {
			return id;
		}
	}
	
	/**
	 * Returns the path string of an already loaded image, otherwise null.
	 *  
	 * @param image The image wich path to return.
	 * @return The path string of an already loaded image, otherwise null.
	 */
	public static String getImagePath(Image image){
		
		String path = null;
		
		Iterator i = ImageLoader.loadedImages.entrySet().iterator();
		
		while(i.hasNext()){
			Map.Entry entry = (Map.Entry)i.next();
			
			if (entry.getValue().equals(image)){
				String imageID = ((ImageID)entry.getKey()).getId();
				
				path = Images.getString(imageID);
			}
		}
		
		return path;
	}
}
