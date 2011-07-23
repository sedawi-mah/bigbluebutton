/* BigBlueButton - http://www.bigbluebutton.org
 * 
 * 
 * Copyright (c) 2008-2009 by respective authors (see below). All rights reserved.
 * 
 * BigBlueButton is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 3 of the License, or (at your option) any later 
 * version. 
 * 
 * BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with BigBlueButton; if not, If not, see <http://www.gnu.org/licenses/>.
 *
 * Author: Alessandra Leonhardt <ale.lionheart@gmail.com>
 * 		
 * 
 * @version $Id: $
 */
package org.bigbluebutton.web.services


package org.bigbluebutton.presentation.imp;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.bigbluebutton.presentation.ImageSlideCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageSlideCreatorImp implements ImageSlideCreator {
	private static Logger log = LoggerFactory.getLogger(ImageSlideCreatorImp.class);
	
	private static final Pattern PAGE_NUMBER_PATTERN = Pattern.compile("(.+-image)-([0-9]+)(.png)");
	private String IMAGEMAGICK_DIR;
	private String BLANK_IMAGE_SLIDE;
	private static String TEMP_SLIDE_NAME = "temp-image";
	
	public boolean createImageSlides(File presentationFile, int pageCount){
		boolean success = false;
	 	File imageSlidesDir = determineImageSlidesDirectory(presentationFile);
	 	
	 	if (! imageSlidesDir.exists())
	 		imageSlidesDir.mkdir();
	 		
	 	cleanDirectory(imageSlidesDir);
	 	
		try {
			success = generateImageSlides(imageSlidesDir, presentationFile);
	    } catch (InterruptedException e) {
	        success = false;
	    }
	    
	    if (! success) createBlankImageSlides(imageSlidesDir, pageCount);
	    
	    renameImageSlides(imageSlidesDir);
	    
	    return true;
	
	}
	
	private boolean generateImageSlides(File imageSlidesDir, File presentationFile) throws InterruptedException {
	 	String source = presentationFile.getAbsolutePath();
	 	String dest = imageSlidesDir.getAbsolutePath() + File.separator + TEMP_SLIDE_NAME + ".png";
	 	
		String COMMAND = IMAGEMAGICK_DIR + "/convert -resize x600 " + source + " " + dest;
		
		Process p;
		try {
			p = Runtime.getRuntime().exec(COMMAND);
			
			int exitValue = p.waitFor();
			if (exitValue != 0) {
		    	log.warn("Exit Value != 0 while for " + COMMAND);
		    } else {
		    	
		    	return true;
		    }
		} catch (IOException e) {
			log.error("IOException while processing " + COMMAND);
		}       
		
		log.warn("Failed to create the image slide: " + COMMAND);
		return false;		
	}
	
	private File determineImageSlidesDirectory(File presentationFile) {
		return new File(presentationFile.getParent() + File.separatorChar + "images");
	}
	
	private void renameImageSlides(File dir) {
		/*
		 * If more than 1 file, filename like 'temp-image-X.png' else filename is 'temp-image.png'
		 */
		if (dir.list().length > 1) {
			File[] files = dir.listFiles();
			Matcher matcher;
			for (int i = 0; i < files.length; i++) {
				matcher = PAGE_NUMBER_PATTERN.matcher(files[i].getAbsolutePath());
	    		if (matcher.matches()) {
	    			// Path should be something like 'c:/temp/bigluebutton/presname/images/temp-image-1.png'
	    			// Extract the page number. There should be 4 matches.
	    			// 0. c:/temp/bigluebutton/presname/images/temp-image-1.png
					// 1. c:/temp/bigluebutton/presname/images/temp-image
					// 2. 1 ---> what we are interested in
					// 3. .png
	    			// We are interested in the second match.
				    int pageNum = Integer.valueOf(matcher.group(2).trim()).intValue();
				    String newFilename = "image-" + (pageNum + 1) + ".png";
				    File renamedFile = new File(dir.getAbsolutePath() + File.separator + newFilename);
				    files[i].renameTo(renamedFile);
	    		}
			}
		} else if (dir.list().length == 1) {
			File oldFilename = new File(dir.getAbsolutePath() + File.separator + dir.list()[0]);
			String newFilename = "image-1.png";
			File renamedFile = new File(oldFilename.getParent() + File.separator + newFilename);
			oldFilename.renameTo(renamedFile);
		}
	}
	
	private void createBlankImageSlides(File imageSlidesDir, int pageCount) {
		File[] imageSlides = imageSlidesDir.listFiles();
		
		if (imageSlides.length != pageCount) {
			for (int i = 0; i < pageCount; i++) {
				File image = new File(imageSlidesDir.getAbsolutePath() + File.separator + TEMP_SLIDE_NAME + "-" + i + ".png");
				if (! image.exists()) {
					copyBlankImageSlide(image);
				}
			}
		}
	}
	
	private void copyBlankImageSlide(File image) {
		try {
			FileUtils.copyFile(new File(BLANK_IMAGE_SLIDE), image);
		} catch (IOException e) {
			log.error("IOException while copying blank image.");
		}		
	}
	
	private void cleanDirectory(File directory) {	
		File[] files = directory.listFiles();				
		for (int i = 0; i < files.length; i++) {
			files[i].delete();
		}
	}

	public void setImageMagickDir(String imageMagickDir) {
		IMAGEMAGICK_DIR = imageMagickDir;
	}

	public void setBlankImageSlide(String blankImageSlide) {
		BLANK_IMAGE_SLIDE = blankImageSlide;
	}

}