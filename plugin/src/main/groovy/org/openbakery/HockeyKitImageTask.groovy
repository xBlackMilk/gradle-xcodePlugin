/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openbakery


import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import javax.imageio.ImageIO
import org.apache.commons.io.FileUtils
import java.awt.image.BufferedImage
import java.awt.RenderingHints
import org.gradle.api.tasks.TaskAction

class HockeyKitImageTask extends AbstractHockeykitTask {

	private static final int IMAGE_WIDTH = 114

	public HockeyKitImageTask() {
		super()
		dependsOn("hockeykit-archive")
		this.description = "Creates the image that is used on the HockeyKit Server"
	}

	def resizeImage(File fromImage, toImage) {
		def image = ImageIO.read( fromImage)

		new BufferedImage( IMAGE_WIDTH, IMAGE_WIDTH, image.type ).with { i ->
			createGraphics().with {
				setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC )
				drawImage( image, 0, 0, HockeyKitImageTask.IMAGE_WIDTH, HockeyKitImageTask.IMAGE_WIDTH, null )
				dispose()
			}
			ImageIO.write( i, 'png', new File(toImage) )
		}
	}

	@TaskAction
	def imageCreate() {
		def infoplist = getAppBundleInfoPlist()
		logger.debug("infoplist: {}", infoplist)
		XMLPropertyListConfiguration config = new XMLPropertyListConfiguration(new File(infoplist))
		def list = config.getList("CFBundleIconFiles")
		if (list.isEmpty()) {
			list = config.getList("CFBundleIcons.CFBundlePrimaryIcon.CFBundleIconFiles")
		}
		TreeMap<Integer, String> iconMap = new TreeMap<Integer, String>()
		list.each {
			item ->
				try {
					def image
					File iconFile;

					// get fileName of info.plist in project / target
					def infoPlist = getInfoPlist()
					def infoPlistFile = new File(infoPlist)

					// get path from info.plist
					//String absolutePath = infoPlistFile.getAbsolutePath();
					//String appPath = absolutePath.substring(0,absolutePath.lastIndexOf(File.separator));


					if (project.infoplist.iconPath) {

						// appPath + additional iconPath + name of iconFile
						iconFile = new File(appPath + File.separator + project.infoplist.iconPath + File.separator + item)

					} else {

						// appPath + additional iconPath + name of iconFile
						iconFile = new File(item)
					}
					logger.debug("try to read iconFile: {}", iconFile)
					image = ImageIO.read(iconFile)

					iconMap.put(image.width, iconFile)
				} catch (Exception ex) {
					logger.error("Cannot read image {}. (Will be ignored), ", item)
				}
		}
		logger.debug("Images to choose from: {}", iconMap)
		def outputDirectory = new File(getOutputDirectory()).getParent()

		def selectedImage = iconMap.get(114)

		def outputImageFile = new File(outputDirectory, "Icon.png")
		if (selectedImage != null) {
			logger.debug("Copy file {} to {}", selectedImage, outputImageFile)
			FileUtils.copyFile(selectedImage, outputImageFile)
		} else {
			if (iconMap.size() > 0) {
				selectedImage = iconMap.lastEntry().value
				logger.debug("Resize file {} to {}", selectedImage, outputImageFile)
				resizeImage(selectedImage, outputImageFile.absolutePath)
			}
		}

	}

}
