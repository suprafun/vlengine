/*
 * Copyright (c) 2003-2007 jMonkeyEngine, 2008 VL Engine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 * 
 * * Neither the name of 'VL Engine' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.vlengine.resource;

import com.vlengine.resource.ParameterMap;
import com.vlengine.image.BitmapHeader;
import com.vlengine.image.Image;
import com.vlengine.image.Texture;
import com.vlengine.image.util.DDSLoader;
import com.vlengine.image.util.TGALoader;
import com.vlengine.math.Vector3f;
import com.vlengine.scene.state.lwjgl.LWJGLTextureState;
import com.vlengine.util.BufferInputStream;
import com.vlengine.util.geom.BufferUtils;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.FileCacheImageInputStream;

/**
 * A clone class of the TextureManager class, reworked to
 * better fit the ResourceFolder resource management class
 * 
 * @author vear (Arpad Vekas)
 */
public class TextureReader {
    private static final Logger logger = Logger.getLogger(TextureReader.class.getName());
    
    public static Image createImage( ByteBuffer data, String name, ParameterMap parameters) {
        int dot = name.lastIndexOf('.');
        String fileExt = dot >= 0 ? name.substring(dot) : "";
        
        boolean flipped = parameters.getBoolean("image_flipped", true);
        boolean addalpha = parameters.getBoolean("image_add_alpha", false);
        
        data.position(0);
        BufferInputStream stream = new BufferInputStream(data);
        
        com.vlengine.image.Image imageData = null;
        try {
            if( ".VLT".equalsIgnoreCase(fileExt)) {
                // our internal texture fromat
                imageData = new Image();
                if(!imageData.load(data))
                    imageData = null;
            } else if (".TGA".equalsIgnoreCase(fileExt)) { // TGA, direct to imageData
                imageData = TGALoader.loadImage(stream, flipped, addalpha);
            } else if (".DDS".equalsIgnoreCase(fileExt)) { // DDS, direct to
                // imageData
                imageData = DDSLoader.loadImage(stream, flipped);
            } else if (".BMP".equalsIgnoreCase(fileExt)) { // BMP, awtImage to
                // imageData
                java.awt.Image image = loadBMPImage(stream);
                imageData = loadImage(image, flipped);
            } else { // Anything else
                java.awt.Image image = readImage(fileExt, stream); //ImageIO.read(stream);
                imageData = loadImage(image, flipped);
            }
            if( imageData == null ) {
                logger.log(Level.WARNING, "Could not load Image. "+name);
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not load Image. "+name, e);
            return null;
        }
        
        // Use a tex state only to determine if S3TC is available.
        LWJGLTextureState.init();

        int imageType = parameters.getInt("image_type", Image.GUESS_FORMAT);
        
        // we've already guessed the format. override if given.
        if ( imageType != Image.GUESS_FORMAT_NO_S3TC
                && imageType != Image.GUESS_FORMAT) {
            imageData.setType( imageType );
        } else if ( imageType == Image.GUESS_FORMAT && LWJGLTextureState.isS3TCSupported()) {
            // Enable S3TC DXT1 compression if available and we're guessing
            // format.
            if (imageData.getType() == com.vlengine.image.Image.RGB888) {
                imageData.setType(com.vlengine.image.Image.RGB888_DXT1);
            } else if (imageData.getType() == com.vlengine.image.Image.RGBA8888) {
                imageData.setType(com.vlengine.image.Image.RGBA8888_DXT5);
            }
        }
        return imageData;
    }
    
    public static Texture createTexture( Image imageData, String name, ParameterMap parameters ) {
        Texture texture=null;

        TextureKey tkey = (TextureKey) parameters.get("tkey");
        if(tkey == null) {
            tkey = new TextureKey(name, parameters);
            parameters.put("tkey", tkey);
        }
        
        texture = new Texture();
        texture.setFilter(tkey.texture_mag_filter);
        texture.setImage(imageData);
        texture.setAnisoLevel(tkey.texture_aniso_level);
        texture.setMipmapState(tkey.texture_min_filter);
        texture.setWrap(tkey.texture_wrap);
        texture.setApply(tkey.texture_apply);

        // tranlation?
        Vector3f translation = tkey.texture_translation;
        if(translation != null) {
            texture.setTranslation(new Vector3f().set(translation));
        }
        Vector3f scale = tkey.texture_scale;
        if(scale!=null) {
            texture.setScale(new Vector3f().set(scale));
        }
        texture.setImageLocation(name);

        return texture;
    }
    
    private static java.awt.Image loadBMPImage(InputStream fs) {
        try {
            DataInputStream dis = new DataInputStream(fs);
            BitmapHeader bh = new BitmapHeader();
            byte[] data = new byte[dis.available()];
            dis.readFully(data);
            dis.close();
            bh.read(data);
            if (bh.bitcount == 24) {
                return (bh.readMap24(data));
            }
            if (bh.bitcount == 32) {
                return (bh.readMap32(data));
            }
            if (bh.bitcount == 8) {
                return (bh.readMap8(data));
            }
        } catch (IOException e) {
            logger.warning("Error while loading bitmap texture.");
        }
        return null;
    }
    
   /**
     * Load the image as either TYPE_3BYTE_BGR or TYPE_4BYTE_ABGR
     * 
     * @param fileExt
     * @param imageIn
     * @return
     * @throws java.io.IOException
     */
    private static BufferedImage readImage(String fileExt, InputStream imageIn)
            throws IOException {
        BufferedImage image;
        ImageTypeSpecifier imageType;
        int width;
        int height;

        if (imageIn == null)
            throw new IOException("Null Stream");

        String format = fileExt.substring(1); // Remove .
        ImageReader reader = (ImageReader) ImageIO.getImageReadersByFormatName(
                format).next();

        try {
            // Not ideal as we are creating a cache file, but as we
            // are processing
            // a stream we don't have access to the local file info
            reader.setInput(new FileCacheImageInputStream(imageIn, null));
            imageType = reader.getRawImageType(0);
            if (imageType == null) {
                // Workaround for Mac issue getting image type of JPEG images.
                // Look through the list to find the first type with
                // a non-null ColorModel
                for (Iterator<ImageTypeSpecifier> i = reader.getImageTypes(0); i
                        .hasNext();) {
                    ImageTypeSpecifier temp = i.next();
                    if (temp != null && temp.getColorModel() != null) {
                        imageType = temp;
                        break;
                    }
                }

                // if there is still no image type, throw an
                // exception
                if (imageType == null) {
                    throw new IOException("Cannot get image type for "
                            + fileExt);
                }
            }
            width = reader.getWidth(0);
            height = reader.getHeight(0);
        } catch (IndexOutOfBoundsException ioob) {
            logger.warning("Corrupt image file ");
            // The image file is corrupt
            throw new IOException("Image read failure");
        }

        if (imageType.getColorModel().getTransparency() == ColorModel.OPAQUE) {
            image = new BufferedImage(width, height,
                    BufferedImage.TYPE_3BYTE_BGR);
        } else {
            image = new BufferedImage(width, height,
                    BufferedImage.TYPE_4BYTE_ABGR);
        }
        ImageReadParam param = reader.getDefaultReadParam();
        param.setDestination(image);
        image = reader.read(0, param);

        reader.dispose();

        return image;
    }

    public static com.vlengine.image.Image loadImage(java.awt.Image image, boolean flipImage) {
        if (image == null) return null;
        boolean hasAlpha = hasAlpha(image);
        BufferedImage tex;
        if (flipImage || !(image instanceof BufferedImage) || (hasAlpha ? ((BufferedImage)image).getType() != BufferedImage.TYPE_4BYTE_ABGR : ((BufferedImage)image).getType() != BufferedImage.TYPE_3BYTE_BGR )) {
            // Obtain the image data.
            try {
                tex = new BufferedImage(image.getWidth(null),
                        image.getHeight(null),
                        hasAlpha ? BufferedImage.TYPE_4BYTE_ABGR
                                : BufferedImage.TYPE_3BYTE_BGR);
            } catch (IllegalArgumentException e) {
                logger.warning("Problem creating buffered Image: "
                        + e.getMessage());
                return null;
            }
            image.getWidth(null);
            image.getHeight(null);

            if(image instanceof BufferedImage) {
                int imageWidth = image.getWidth(null);
                int[] tmpData = new int[imageWidth];
                int row = 0;
                BufferedImage bufferedImage = ( (BufferedImage) image );
                for(int y=image.getHeight(null)-1; y>=0; y--) {
                    bufferedImage.getRGB(0, (flipImage ? row++ : y), imageWidth, 1, tmpData, 0, imageWidth);
                    tex.setRGB(0, y, imageWidth, 1, tmpData, 0, imageWidth);
                }
            } else {
                AffineTransform tx = null;
                if (flipImage) {
                    tx = AffineTransform.getScaleInstance(1, -1);
                    tx.translate(0, -image.getHeight(null));
                }
                Graphics2D g = (Graphics2D) tex.getGraphics();
                g.drawImage(image, tx, null);
                g.dispose();
            }

        } else {
            tex = (BufferedImage)image;
        }
        // Get a pointer to the image memory
        ByteBuffer scratch = BufferUtils.createByteBuffer(4 * tex.getWidth() * tex.getHeight());
        byte data[] = (byte[]) tex.getRaster().getDataElements(0, 0,
                tex.getWidth(), tex.getHeight(), null);
        scratch.clear();
        scratch.put(data);
        scratch.flip();
        com.vlengine.image.Image textureImage = new com.vlengine.image.Image();
        textureImage.setType(hasAlpha ? com.vlengine.image.Image.RGBA8888
                : com.vlengine.image.Image.RGB888);
        textureImage.setWidth(tex.getWidth());
        textureImage.setHeight(tex.getHeight());
        textureImage.setData(scratch);
        return textureImage;
    }
    
    public static boolean hasAlpha(java.awt.Image image) {
        if (null == image) {
            return false;
        }
        if (image instanceof BufferedImage) {
            BufferedImage bufferedImage = (BufferedImage) image;
            return bufferedImage.getColorModel().hasAlpha();
        }
        PixelGrabber pixelGrabber = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            pixelGrabber.grabPixels();
            ColorModel colorModel = pixelGrabber.getColorModel();
            if (colorModel != null) {
                return colorModel.hasAlpha();
            }

            return false;
        } catch (InterruptedException e) {
            logger.warning("Unable to determine alpha of image: " + image);
        }
        return false;
    }
}
