/*
* This file is part of jfxTorrent, an open-source BitTorrent client written in JavaFX.
* Copyright (C) 2015 Vedran Matic
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
*
*/

package org.matic.torrent.gui.image;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javafx.geometry.Rectangle2D;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.ColorInput;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

import org.matic.torrent.gui.window.ApplicationWindow;

public final class ImageUtils {
	
	//Custom icon/image colors
	public static final Color INACTIVE_TAB_COLOR = Color.rgb(162, 170, 156);
	public static final Color BUTTON_COLOR_DELETE = Color.rgb(165,57,57);	
	public static final Color ICON_COLOR = Color.rgb(102, 162, 54);	
	
	//Side bar specific icons
	public static final String SIDEBAR_ARROW_UP_DOWN_ICON_LOCATION = "/icons/sidebar_downloading.png";
	public static final String SIDEBAR_LABEL_ICON_LOCATION = "/icons/sidebar_label.png";
	public static final String SIDEBAR_RSS_ICON_LOCATION = "/icons/sidebar_rss.png";
	
	public static final String FOLDER_OPEN_ICON_LOCATION = "/icons/toolbar_open.png";
	public static final String FOLDER_CLOSED_ICON_LOCATION = "/icons/folder_closed.png";
	public static final String PAUSE_ICON_LOCATION = "/icons/toolbar_pause.png";
	public static final String LOCK_ICON_LOCATION = "/icons/toolbar_unlock.png";
	public static final String SETTINGS_ICON_LOCATION = "/icons/toolbar_settings.png";
	public static final String STOP_ICON_LOCATION = "/icons/toolbar_stop.png";	
	public static final String DOWNLOAD_ICON_LOCATION = "/icons/toolbar_download.png";
	public static final String DOWN_ICON_LOCATION = "/icons/toolbar_down.png";
	public static final String REMOTE_ICON_LOCATION = "/icons/toolbar_remote.png";	
	public static final String UP_ICON_LOCATION = "/icons/toolbar_up.png";
	public static final String DELETE_ICON_LOCATION = "/icons/toolbar_delete.png";	
	public static final String NEW_ICON_LOCATION = "/icons/toolbar_new.png";
	public static final String LINK_ICON_LOCATION = "/icons/toolbar_link.png";
	public static final String ADD_ICON_LOCATION = "/icons/toolbar_open.png";
	public static final String RSS_ICON_LOCATION = "/icons/toolbar_rss.png";
		
	public static final int ICON_SIZE_CATEGORY_LIST = 25;	
	public static final int ICON_SIZE_FILE_TYPE = 15;
	public static final int ICON_SIZE_TOOLBAR = 18;
	public static final int ICON_SIZE_TAB = 14;
	
	//Detailed info tab's icon paths
	public static final String TAB_FILES_ICON_LOCATION = "/icons/tab_files.png";
	public static final String TAB_INFO_ICON_LOCATION = "/icons/tab_information.png";
	public static final String TAB_PEERS_ICON_LOCATION = "/icons/tab_peers.png";
	public static final String TAB_TRACKERS_ICON_LOCATION = "/icons/tab_trackers.png";
	public static final String TAB_PIECES_ICON_LOCATION = "/icons/tab_pieces.png";
	public static final String TAB_SPEED_ICON_LOCATION = "/icons/tab_speed.png";
	public static final String TAB_LOGGER_ICON_LOCATION = "/icons/tab_logger.png";
	
	//Image area where the margins not containing the image itself have been cropped
	public static final Rectangle2D CROPPED_MARGINS_IMAGE_VIEW = new Rectangle2D(16, 12, 44, 46);

	//Folder opened/closed images
	public static final Image FOLDER_CLOSED_IMAGE = new Image(
			ApplicationWindow.class.getResourceAsStream("/themes/dark" + FOLDER_CLOSED_ICON_LOCATION));
	public static final Image FOLDER_OPENED_IMAGE = new Image(
			ApplicationWindow.class.getResourceAsStream("/themes/dark" + FOLDER_OPEN_ICON_LOCATION));
	
	//Filter list category images
	public static final Image DOWNLOADS_IMAGE = new Image(
			ApplicationWindow.class.getResourceAsStream(SIDEBAR_ARROW_UP_DOWN_ICON_LOCATION),
			ICON_SIZE_CATEGORY_LIST, ICON_SIZE_CATEGORY_LIST, true, true);
	public static final Image LABEL_IMAGE = new Image(
			ApplicationWindow.class.getResourceAsStream(SIDEBAR_LABEL_ICON_LOCATION),
			ICON_SIZE_CATEGORY_LIST, ICON_SIZE_CATEGORY_LIST, true, true);
	public static final Image RSS_IMAGE = new Image(
			ApplicationWindow.class.getResourceAsStream(SIDEBAR_RSS_ICON_LOCATION),
			ICON_SIZE_CATEGORY_LIST, ICON_SIZE_CATEGORY_LIST, true, true);	
	
	//File type mapping names
	private static final String FILE_TYPE_IMAGE = "file_type_image";
	private static final String FILE_TYPE_VIDEO = "file_type_video";
	private static final String FILE_TYPE_SUBTITLE = "file_type_subtitle";
	private static final String FILE_TYPE_MUSIC = "file_type_music";
	private static final String FILE_TYPE_PDF = "file_type_pdf";
	private static final String FILE_TYPE_PACKAGE = "file_type_package";
	private static final String FILE_TYPE_EBOOK = "file_type_ebook";
	private static final String FILE_TYPE_EXECUTABLE = "file_type_executable";
	private static final String FILE_TYPE_GENERIC = "file_type_generic";
	
	//Image and file type mappings
	private static final Map<String, Set<String>> FILE_EXTENSION_MAPPINGS = new HashMap<>();
	private static final Map<String, Image> FILE_IMAGE_MAPPINGS = new HashMap<>();
	
	static {
		FILE_EXTENSION_MAPPINGS.put(FILE_TYPE_IMAGE, new HashSet<String>(Arrays.asList(
				"jpg", "jpeg", "gif", "png", "bmp", "exif", "tiff", "tif", "raw", "webp", "bpg", "img", "svg")));
		FILE_EXTENSION_MAPPINGS.put(FILE_TYPE_VIDEO, new HashSet<String>(Arrays.asList(
				"mp4", "m4p", "mpg", "mpeg", "vob", "mkv", "avi", "mov", "webm", "flv", "ogv", "ogg", "qt",
				"wmv", "yuv", "rm", "asf", "m4v", "mp2", "mpe", "mpv", "m2v", "m4v", "3gp", "3g2")));
		FILE_EXTENSION_MAPPINGS.put(FILE_TYPE_SUBTITLE, new HashSet<String>(Arrays.asList(
				"aqt", "gsub", "jss", "srt", "sub", "ssa", "smi", "pjs", "psb", "rt", "stl", "ssf", "ass",
				"usf", "idx")));
		FILE_EXTENSION_MAPPINGS.put(FILE_TYPE_MUSIC, new HashSet<String>(Arrays.asList(
				"mp3", "wav", "ac3", "aac", "act", "aiff", "amr", "au", "awb", "dvf", "flac", "gsm", "ivs",
				"m4a", "mmf", "mpc", "msv", "oga", "opus", "ra", "sln", "tta", "vox", "wma", "wv")));
		FILE_EXTENSION_MAPPINGS.put(FILE_TYPE_PDF, new HashSet<String>(Arrays.asList("pdf")));
		FILE_EXTENSION_MAPPINGS.put(FILE_TYPE_PACKAGE, new HashSet<String>(Arrays.asList("a", "ar", "lbr",
				"iso", "mar", "tar", "bz2", "gz", "lz", "lzma", "lzo", "rz", "xz", "z", "7z", "s7z",
				"ace", "apk", "cab", "dar", "dgc", "dmg", "ear", "ice", "jar", "lzh", "lha", "lzx",
				"pea", "pim", "rar", "tgz", "z", "bz2", "tbz2", "lzma", "tlz", "war", "wim", "zip",
				"zipx", "deb", "rpm")));		
		FILE_EXTENSION_MAPPINGS.put(FILE_TYPE_EBOOK, new HashSet<String>(Arrays.asList("epub", "fb2",
				"azw", "lit", "prc", "mobi", "pdb")));
		FILE_EXTENSION_MAPPINGS.put(FILE_TYPE_EXECUTABLE, new HashSet<String>(Arrays.asList("sh", "cmd",
				"com", "exe")));
		
		FILE_IMAGE_MAPPINGS.put(FILE_TYPE_IMAGE, new Image(
				ApplicationWindow.class.getResourceAsStream("/icons/filetype_image.png")));
		FILE_IMAGE_MAPPINGS.put(FILE_TYPE_VIDEO, new Image(
				ApplicationWindow.class.getResourceAsStream("/icons/filetype_video.png")));
		FILE_IMAGE_MAPPINGS.put(FILE_TYPE_SUBTITLE, new Image(
				ApplicationWindow.class.getResourceAsStream("/icons/filetype_subtitle.png")));
		FILE_IMAGE_MAPPINGS.put(FILE_TYPE_MUSIC, new Image(
				ApplicationWindow.class.getResourceAsStream("/icons/filetype_music.png")));
		FILE_IMAGE_MAPPINGS.put(FILE_TYPE_PDF, new Image(
				ApplicationWindow.class.getResourceAsStream("/icons/filetype_pdf.png")));
		FILE_IMAGE_MAPPINGS.put(FILE_TYPE_PACKAGE, new Image(
				ApplicationWindow.class.getResourceAsStream("/icons/filetype_package.png")));
		FILE_IMAGE_MAPPINGS.put(FILE_TYPE_EBOOK, new Image(
				ApplicationWindow.class.getResourceAsStream("/icons/filetype_ebook.png")));
		FILE_IMAGE_MAPPINGS.put(FILE_TYPE_EXECUTABLE, new Image(
				ApplicationWindow.class.getResourceAsStream("/icons/filetype_executable.png")));
		
		FILE_IMAGE_MAPPINGS.put(FILE_TYPE_GENERIC, new Image(
				ApplicationWindow.class.getResourceAsStream("/icons/filetype_generic.png")));
	}
	
	/**
	 * Try to find an image for a given file name/type. Return a generic file type image if none applies.
	 * 
	 * @param fileName Target file name
	 * @return Matching file type image
	 */
	public static Image getFileTypeImage(final String fileName) {
		final String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
		final String fileType = FILE_EXTENSION_MAPPINGS.keySet().stream().filter(type -> {
			return FILE_EXTENSION_MAPPINGS.get(type).contains(fileExtension);
		}).findAny().orElse(FILE_TYPE_GENERIC);
		
		return FILE_IMAGE_MAPPINGS.get(fileType);
	}
	
	/**
	 * Create a view of an image
	 * 
	 * @param image Target image
	 * @param viewPort Part of the image to use
	 * @param imageWidth Resulting image width
	 * @param imageHeight Resulting image height
	 * @return Targeted view of the image
	 */
	public static ImageView createImageView(final Image image, final Rectangle2D viewPort,
			final int imageWidth, final int imageHeight) {
		final ImageView monochromeImageView = new ImageView(image);
		
		monochromeImageView.setFitWidth(imageWidth);
    	monochromeImageView.setFitHeight(imageHeight);
		
		final Rectangle clip = new Rectangle(0, 0, imageWidth, imageHeight);
		monochromeImageView.setClip(clip);    	
        monochromeImageView.setViewport(viewPort);
    	monochromeImageView.setSmooth(true);
    	
		return monochromeImageView;
	}
	
	/**
	 * Given a monochrome image, apply a color to it
	 * 
	 * @param imageView A view to the target monochrome image
	 * @param color Target new image color
	 */
	public static void colorize(final ImageView imageView, final Paint color) {		
		final ColorAdjust monochrome = new ColorAdjust();
        monochrome.setSaturation(-1.0);
        monochrome.setBrightness(0.75);

		final Blend selectionColorBlend = new Blend(BlendMode.SRC_ATOP,
				monochrome, new ColorInput(0, 0, imageView.getFitWidth(),
						imageView.getFitHeight(), color));	        
		imageView.setEffect(selectionColorBlend);			
	}
}
