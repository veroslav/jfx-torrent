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
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

import org.matic.torrent.gui.window.ApplicationWindow;

public final class ImageUtils {
	
	public static final int TOOLBAR_BUTTON_IMAGE_SIZE = 18;
	public static final int CATEGORY_LIST_IMAGE_SIZE = 25;
	public static final int FILE_TYPE_IMAGE_SIZE = 15;
	
	//Image area where the margins not containing the image itself have been cropped
	public static final Rectangle2D CROPPED_MARGINS_IMAGE_VIEW = new Rectangle2D(16, 12, 44, 46);

	//Folder opened/closed images
	public static final Image FOLDER_CLOSED_IMAGE = new Image(
			ApplicationWindow.class.getResourceAsStream("/images/appbar.folder.png"));
	public static final Image FOLDER_OPENED_IMAGE = new Image(
			ApplicationWindow.class.getResourceAsStream("/images/appbar.folder.open.png"));
	
	//Filter list category images
	public static final Image DOWNLOADS_IMAGE = new Image(
			ApplicationWindow.class.getResourceAsStream("/images/appbar.arrow.down.up.png"), 25, 25, true, true);
	public static final Image LABEL_IMAGE = new Image(
			ApplicationWindow.class.getResourceAsStream("/images/appbar.tag.label.png"), 25, 25, true, true);
	public static final Image RSS_IMAGE = new Image(
			ApplicationWindow.class.getResourceAsStream("/images/appbar.rss.png"), 25, 25, true, true);	
	
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
				ApplicationWindow.class.getResourceAsStream("/images/appbar.image.png")));
		FILE_IMAGE_MAPPINGS.put(FILE_TYPE_VIDEO, new Image(
				ApplicationWindow.class.getResourceAsStream("/images/appbar.movie.clapper.png")));
		FILE_IMAGE_MAPPINGS.put(FILE_TYPE_SUBTITLE, new Image(
				ApplicationWindow.class.getResourceAsStream("/images/appbar.notification.png")));
		FILE_IMAGE_MAPPINGS.put(FILE_TYPE_MUSIC, new Image(
				ApplicationWindow.class.getResourceAsStream("/images/appbar.music.png")));
		FILE_IMAGE_MAPPINGS.put(FILE_TYPE_PDF, new Image(
				ApplicationWindow.class.getResourceAsStream("/images/appbar.adobe.acrobat.png")));
		FILE_IMAGE_MAPPINGS.put(FILE_TYPE_PACKAGE, new Image(
				ApplicationWindow.class.getResourceAsStream("/images/appbar.present.png")));
		FILE_IMAGE_MAPPINGS.put(FILE_TYPE_EBOOK, new Image(
				ApplicationWindow.class.getResourceAsStream("/images/appbar.book.open.png")));
		FILE_IMAGE_MAPPINGS.put(FILE_TYPE_EXECUTABLE, new Image(
				ApplicationWindow.class.getResourceAsStream("/images/appbar.cog.png")));
		
		FILE_IMAGE_MAPPINGS.put(FILE_TYPE_GENERIC, new Image(
				ApplicationWindow.class.getResourceAsStream("/images/appbar.page.small.png")));
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
	 * Given a monochrome image, apply a color to it
	 * 
	 * @param image Target monochrome image
	 * @param color Target new image color
	 * @param viewPort Part of the image to color
	 * @param imageWidth Resulting image width
	 * @param imageHeight Resulting image height
	 * 
	 * @return Colorized image
	 */
	public static ImageView colorImage(final Image image, final Paint color, 
			final Rectangle2D viewPort, final int imageWidth, final int imageHeight) {		
		final ImageView monochromeImageView = new ImageView(image);
		
		monochromeImageView.setFitWidth(imageWidth);
    	monochromeImageView.setFitHeight(imageHeight);
		
		final Rectangle clip = new Rectangle(0, 0, imageWidth, imageHeight);
		monochromeImageView.setClip(clip);
    	
    	final ColorAdjust monochrome = new ColorAdjust();
        monochrome.setSaturation(-1.0);
        monochrome.setBrightness(0.75);
        
        final Blend selectionColorBlend = new Blend(BlendMode.SRC_ATOP,
                monochrome, new ColorInput(0, 0, imageWidth, imageHeight, color));
        
    	monochromeImageView.setEffect(selectionColorBlend);    	
    	monochromeImageView.setViewport(viewPort);
    	monochromeImageView.setSmooth(true);
    	
		return monochromeImageView;
	}
}
