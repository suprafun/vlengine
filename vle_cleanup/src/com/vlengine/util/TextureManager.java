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

package com.vlengine.util;

import com.vlengine.image.BitmapHeader;
import com.vlengine.image.Image;
import com.vlengine.image.Texture;
import com.vlengine.image.util.DDSLoader;
import com.vlengine.image.util.TGALoader;
import com.vlengine.resource.ParameterMap;
import com.vlengine.resource.TextureReader;
import com.vlengine.resource.UrlResource;
import com.vlengine.scene.state.TextureState;
import com.vlengine.scene.state.lwjgl.LWJGLTextureState;
import com.vlengine.util.geom.BufferUtils;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

/**
 * 
 * vear: this class is for reference here, it should not be used, instead use
 * the ResourceFinder to get a texture
 * <code>TextureManager</code> provides static methods for building a
 * <code>Texture</code> object. Typically, the information supplied is the
 * filename and the texture properties.
 * 
 * @author Mark Powell
 * @author Joshua Slack -- cache code and enhancements
 * @version $Id: TextureManager.java,v 1.83 2007/10/27 19:45:18 renanse Exp $
 */
final public class TextureManager {
    private static final Logger logger = Logger.getLogger(TextureManager.class.getName());

    /*
    private static HashMap<TextureKey, Texture> m_tCache = new HashMap<TextureKey, Texture>();
    */
    private static IntList cleanupStore = new IntList();
    

    public static boolean COMPRESS_BY_DEFAULT = true;

    private static int DEFAULT_MAG_FILTER = Texture.FM_LINEAR;

    private static int DEFAULT_MIN_FILTER = Texture.MM_LINEAR;

    private static float DEFAULT_ANISO_LEVEL = 0.0f;

    private TextureManager() {
    }

    /**
     * <code>loadTexture</code> loads a new texture defined by the parameter
     * string. Filter parameters are used to define the filtering of the
     * texture. If there is an error loading the file, null is returned.
     *
     * @param file
     *            the filename of the texture image.
     * @param minFilter
     *            the filter for the near values.
     * @param magFilter
     *            the filter for the far values.
     *
     * @return the loaded texture. If there is a problem loading the texture,
     *         null is returned.
     */
    public static com.vlengine.image.Texture loadTexture(String file, int minFilter,
                                                    int magFilter) {
        return loadTexture(file, minFilter, magFilter, DEFAULT_ANISO_LEVEL, true);
    }

    /**
     * <code>loadTexture</code> loads a new texture defined by the parameter
     * string. Filter parameters are used to define the filtering of the
     * texture. If there is an error loading the file, null is returned.
     *
     * @param file
     *            the filename of the texture image.
     * @param minFilter
     *            the filter for the near values.
     * @param magFilter
     *            the filter for the far values.
     * @param flipped
     *            If true, the images Y values are flipped.
     *
     * @return the loaded texture. If there is a problem loading the texture,
     *         null is returned.
     */
    public static com.vlengine.image.Texture loadTexture(String file, int minFilter,
                                                    int magFilter, float anisoLevel, boolean flipped) {
        return loadTexture(file, minFilter, magFilter,
                (COMPRESS_BY_DEFAULT ? Image.GUESS_FORMAT
                        : Image.GUESS_FORMAT_NO_S3TC), anisoLevel, flipped);
    }


    /**
     * <code>loadTexture</code> loads a new texture defined by the parameter
     * string. Filter parameters are used to define the filtering of the
     * texture. If there is an error loading the file, null is returned.
     *
     * @param file
     *            the filename of the texture image.
     * @param minFilter
     *            the filter for the near values.
     * @param magFilter
     *            the filter for the far values.
     * @param flipped
     *            If true, the images Y values are flipped.
     *
     * @return the loaded texture. If there is a problem loading the texture,
     *         null is returned.
     */
    public static com.vlengine.image.Texture loadTexture(String file, int minFilter,
                                                    int magFilter, int imageType, float anisoLevel, boolean flipped) {
        URL url = getTextureURL(file);
        return loadTexture(url, minFilter, magFilter, imageType, anisoLevel,
                flipped);
    }

    /**
     * Convert the provided String file name into a Texture URL, first
     * attempting to use the {@link ResourceLocatorTool}, then trying to load
     * it as a direct file path.
     * 
     * @param file the file name
     * @return a URL
     */
    private static URL getTextureURL(String file) {
        URL url = null;
        try {
            url = new URL("file:" + file);
        } catch (MalformedURLException e) {
            logger.throwing(TextureManager.class.toString(),
                    "getTextureURL(file)", e);
        }
        return url;
    }

    /**
     * <code>loadTexture</code> loads a new texture defined by the parameter
     * url.
     * If there is an error loading the file, null is returned.
     * 
     * @param file
     *            the url of the texture image.
     * @param flipped
     *            If true, the images Y values are flipped.
     * @return the loaded texture. If there is a problem loading the texture,
     *         null is returned.
     */
    public static com.vlengine.image.Texture loadTexture(URL file) {
        return loadTexture(file, true);
    }

    /**
     * <code>loadTexture</code> loads a new texture defined by the parameter
     * url.
     * If there is an error loading the file, null is returned.
     * 
     * @param file
     *            the url of the texture image.
     * @param flipped
     *            If true, the images Y values are flipped.
     * @return the loaded texture. If there is a problem loading the texture,
     *         null is returned.
     */
    public static com.vlengine.image.Texture loadTexture(URL file, boolean flipped) {
        return loadTexture(file, DEFAULT_MIN_FILTER, DEFAULT_MAG_FILTER,
                (COMPRESS_BY_DEFAULT ? Image.GUESS_FORMAT
                        : Image.GUESS_FORMAT_NO_S3TC), DEFAULT_ANISO_LEVEL , flipped);
    }

    /**
     * <code>loadTexture</code> loads a new texture defined by the parameter
     * url. Filter parameters are used to define the filtering of the texture.
     * If there is an error loading the file, null is returned.
     * 
     * @param file
     *            the url of the texture image.
     * @param minFilter
     *            the filter for the near values.
     * @param magFilter
     *            the filter for the far values.
     * @return the loaded texture. If there is a problem loading the texture,
     *         null is returned.
     */
    public static com.vlengine.image.Texture loadTexture(URL file, int minFilter,
                                                    int magFilter) {
        return loadTexture(file, minFilter, magFilter,
                (COMPRESS_BY_DEFAULT ? Image.GUESS_FORMAT
                        : Image.GUESS_FORMAT_NO_S3TC), DEFAULT_ANISO_LEVEL, true);
    }

    public static com.vlengine.image.Texture loadTexture(URL file, int minFilter,
                                                    int magFilter, float anisoLevel, boolean flipped) {
        return loadTexture(file, minFilter, magFilter,
                (COMPRESS_BY_DEFAULT ? Image.GUESS_FORMAT
                        : Image.GUESS_FORMAT_NO_S3TC), anisoLevel, flipped);
    }

    public static com.vlengine.image.Image loadImage(URL file, boolean flipped) {
        if(file == null) {
            logger.warning("loadImage(URL file, boolean flipped): file is null, defaultTexture used.");
            return TextureState.getDefaultTextureImage();
        }
        
        String fileName = file.getFile();
        if (fileName == null) {
            logger.warning("loadImage(URL file, boolean flipped): fileName is null, defaultTexture used.");
            return TextureState.getDefaultTextureImage();
        }
        
        ParameterMap params = new ParameterMap();
        params.put("image_flipped", flipped);
        
        ByteBuffer imageData = UrlResource.load(file, ParameterMap.NODIRECTBUFFER);
        if( imageData != null ) {
            return TextureReader.createImage(imageData, fileName, params);
        }
        return null;
    }

    /**
     * <code>loadTexture</code> loads a new texture defined by the parameter
     * url. Filter parameters are used to define the filtering of the texture.
     * If there is an error loading the file, null is returned.
     *
     * @param file
     *            the url of the texture image.
     * @param minFilter
     *            the filter for the near values.
     * @param magFilter
     *            the filter for the far values.
     * @param imageType
     *            the image type to use. if -1, the type is determined by jME.
     *            If S3TC/DXT1[A] is available we use that. if -2, the type is
     *            determined by jME without using S3TC, even if available. See
     *            com.jme.image.Image for possible types.
     * @param flipped
     *            If true, the images Y values are flipped.
     *
     * @return the loaded texture. If there is a problem loading the texture,
     *         null is returned.
     */
    public static com.vlengine.image.Texture loadTexture(URL file, int minFilter,
                                                    int magFilter, int imageType, float anisoLevel, boolean flipped) {
    
        if (null == file) {
            logger.warning("Could not load image...  URL was null. defaultTexture used.");
            return TextureState.getDefaultTexture();
        }
        
        String fileName = file.getFile();
        if (fileName == null) {
            logger.warning( "Could not load image...  fileName was null. defaultTexture used.");
            return TextureState.getDefaultTexture();
        }
        
        // construct the parameter map
        ParameterMap params = new ParameterMap();
        params.put("texture_min_filter", minFilter);
        params.put("texture_mag_filter", magFilter);
        params.put("image_type", imageType);
        params.put("image_flipped", flipped);
        params.put("texture_aniso_level", anisoLevel);
        
        
        // load the image file
        
        ByteBuffer imageData = UrlResource.load(file, ParameterMap.NODIRECTBUFFER);
        if( imageData != null ) {
            Image img = TextureReader.createImage(imageData, fileName, params);
            if( img != null ) {
                Texture t = TextureReader.createTexture(img, fileName, params);
                return t;
            }
        }
        //TextureKey tkey = new TextureKey(file, flipped, imageType);
        
        //return loadTexture(null, tkey, null, minFilter, magFilter, anisoLevel);
        return null;
    }
    
    /*
    public static com.vlengine.image.Texture loadTexture(TextureKey tkey) {
        return loadTexture(null, tkey);
    }
    
    public static com.vlengine.image.Texture loadTexture(Texture texture, TextureKey tkey) {
        return loadTexture(texture, tkey, null, DEFAULT_MIN_FILTER, DEFAULT_MAG_FILTER, DEFAULT_ANISO_LEVEL);
    }
    
    public static com.vlengine.image.Texture loadTexture(Texture texture, TextureKey tkey, com.vlengine.image.Image imageData, int minFilter,
            int magFilter, float anisoLevel) {
        if(tkey == null) {
            logger.warning("TextureKey is null, cannot load");
            return TextureState.getDefaultTexture();
        }
        
        Texture cache = findCachedTexture(tkey);
        if(cache != null) {
            //look into cache.
            //Uncomment if you want to see when this occurs.
            //logging.info("******** REUSING TEXTURE ******** "+cache);
            if(texture == null) {
                Texture tClone = cache.createSimpleClone();
                if(tClone.getTextureKey() == null) {
                    tClone.setTextureKey(tkey);
                }
                return tClone;
            }
            cache.createSimpleClone(texture);
            return texture;
        }

        if (texture == null) {
            texture = new Texture();
        }

        if (imageData == null)
            imageData = loadImage(tkey);

        if (null == imageData) {
            logger.warning("(image null) Could not load: "
                    + (tkey.getLocation() != null ? tkey.getLocation()
                            .getFile() : tkey.getFileType()));
            return TextureState.getDefaultTexture();
        }

        // Use a tex state only to determine if S3TC is available.
        LWJGLTextureState.init();

        // we've already guessed the format. override if given.
        if (tkey.imageType != Image.GUESS_FORMAT_NO_S3TC
                && tkey.imageType != Image.GUESS_FORMAT) {
            imageData.setType(tkey.imageType);
        } else if (tkey.imageType == Image.GUESS_FORMAT && LWJGLTextureState.isS3TCSupported()) {
            // Enable S3TC DXT1 compression if available and we're guessing
            // format.
            if (imageData.getType() == com.vlengine.image.Image.RGB888) {
                imageData.setType(com.vlengine.image.Image.RGB888_DXT1);
            } else if (imageData.getType() == com.vlengine.image.Image.RGBA8888) {
                imageData.setType(com.vlengine.image.Image.RGBA8888_DXT5);
            }
        }

        texture.setTextureKey(tkey);
        texture.setFilter(magFilter);
        texture.setImage(imageData);
        texture.setAnisoLevel(anisoLevel);
        texture.setMipmapState(minFilter);
        if (tkey.location != null) {
            texture.setImageLocation(tkey.location.toString());
        }

        addToCache(texture);
        return texture;
    }
    
    public static void addToCache(Texture t) {
        if (TextureState.getDefaultTexture() == null
                || (t != TextureState.getDefaultTexture()
                && t.getImage() != TextureState.getDefaultTextureImage())) {
            m_tCache.put(t.getTextureKey(), t);
        }
    }

    public static com.vlengine.image.Texture loadTexture(java.awt.Image image,
            int minFilter, int magFilter, boolean flipped) {
        return loadTexture(image, minFilter, magFilter, DEFAULT_ANISO_LEVEL,
                (COMPRESS_BY_DEFAULT ? Image.GUESS_FORMAT
                        : Image.GUESS_FORMAT_NO_S3TC), flipped);
    }

    public static com.vlengine.image.Texture loadTexture(java.awt.Image image,
            int minFilter, int magFilter, float anisoLevel, boolean flipped) {
        return loadTexture(image, minFilter, magFilter, anisoLevel,
                (COMPRESS_BY_DEFAULT ? Image.GUESS_FORMAT
                        : Image.GUESS_FORMAT_NO_S3TC), flipped);
    }
    

    public static com.vlengine.image.Texture loadTexture(java.awt.Image image,
            int minFilter, int magFilter, float anisoLevel, int imageFormat,
            boolean flipped) {
        com.vlengine.image.Image imageData = loadImage(image, flipped);

        TextureKey tkey = new TextureKey(null, flipped, imageFormat);
        if (image != null)
            tkey.setFileType("" + image.hashCode());
        return loadTexture(null, tkey, imageData, minFilter, magFilter,
                anisoLevel);
    }
    
    public static com.vlengine.image.Image loadImage(TextureKey key) {
        if(key == null) {
            return null;
        }
        
        return loadImage(key.location, key.flipped);
    }

    public static com.vlengine.image.Image loadImage(String fileName, boolean flipped) {
        return loadImage(getTextureURL(fileName), flipped);
    }
    
    public static com.vlengine.image.Image loadImage(String fileExt, InputStream stream, boolean flipped) {
        
        com.vlengine.image.Image imageData = null;
        try {
            if (".TGA".equalsIgnoreCase(fileExt)) { // TGA, direct to imageData
                imageData = TGALoader.loadImage(stream, flipped);
            } else if (".DDS".equalsIgnoreCase(fileExt)) { // DDS, direct to
                // imageData
                imageData = DDSLoader.loadImage(stream, flipped);
            } else if (".BMP".equalsIgnoreCase(fileExt)) { // BMP, awtImage to
                // imageData
                java.awt.Image image = loadBMPImage(stream);
                imageData = loadImage(image, flipped);
            } else { // Anything else
                java.awt.Image image = ImageIO.read(stream);
                imageData = loadImage(image, flipped);
            }
            if (imageData == null) {
                logger.warning("loadImage(String fileExt, InputStream stream, boolean flipped): no imageData found.  defaultTexture used.");
                imageData = TextureState.getDefaultTextureImage();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not load Image.", e);
            imageData = TextureState.getDefaultTextureImage();
        }
        return imageData;
    }

    /**
     *
     * <code>loadImage</code> sets the image data.
     *
     * @param image
     *            The image data.
     * @param flipImage
     *            if true will flip the image's y values.
     * @return the loaded image.
     */
    /*
    public static com.vlengine.image.Image loadImage(java.awt.Image image,
                                                boolean flipImage) {
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
                return TextureState.getDefaultTextureImage();
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
*/
    /**
     * <code>loadBMPImage</code> because bitmap is not directly supported by
     * Java, we must load it manually. The requires opening a stream to the file
     * and reading in each byte. After the image data is read, it is used to
     * create a new <code>Image</code> object. This object is returned to be
     * used for normal use.
     *
     * @param fs
     *            The bitmap file stream.
     *
     * @return <code>Image</code> object that contains the bitmap information.
     */
    /*
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
    */
    /**
     * <code>hasAlpha</code> returns true if the specified image has
     * transparent pixels
     *
     * @param image
     *            Image to check
     * @return true if the specified image has transparent pixels
     */
    /*
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
*/

    public static boolean releaseTexture(Texture texture) {
        if (texture == null)
            return false;
        int texid = texture.getTextureId();
        if(texid==0)
            return false;
        int idx = cleanupStore.indexOf(texid);
        if(idx==-1)
            return false;
        LWJGLTextureState.deleteTextureId(texid);
        cleanupStore.removeElementAt(idx);
        texture.setTextureId(0);
        return true;
    }

    public static void removeTextureId(int texid) {
        if(texid==0)
            return;
        cleanupStore.removeElement(texid);
    }

    public static void registerForCleanup(Texture tex) {
        int textureId = tex.getTextureId();
        int idx = cleanupStore.indexOf(textureId);
        if(idx!=-1)
            return;
        cleanupStore.add(textureId);
    }

    public static void doTextureCleanup() {
        for (int i=0;i<cleanupStore.size();i++) {
            Integer id=cleanupStore.get(i);
            if (id != null) {
                try {
                    LWJGLTextureState.deleteTextureId(id.intValue());
                } catch (Exception e) {} // ignore.
            }
        }
        cleanupStore.clear();
    }
}