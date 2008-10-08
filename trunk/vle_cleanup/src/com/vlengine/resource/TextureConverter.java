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

import com.vlengine.image.Image;
import com.vlengine.math.FastMath;
import com.vlengine.util.geom.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.opengl.ARBTextureCompression;
import org.lwjgl.opengl.EXTTextureCompressionS3TC;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.Util;
import org.lwjgl.opengl.glu.GLU;
import org.lwjgl.opengl.glu.MipMap;

/**
 * This class takes an uncompressed image and compresses it using
 * OpenGL to a file for later loading. This conversion should be
 * done by the developer in a batch-processing, or during installing
 * and not during the rendering.
 * 
 * Lots of code taken fron LWJGLTextureState
 * 
 * @author vear (Arpad Vekas)
 */
public class TextureConverter {
    
    // the input image componenets we handle
    private static int[] imageComponents = { 
            GL11.GL_RGBA4, 
            GL11.GL_RGB8,
            GL11.GL_RGB5_A1, 
            GL11.GL_RGBA8, 
            GL11.GL_LUMINANCE8_ALPHA8,
            EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT,
            EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT,
            EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT,
            EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT,
            GL11.GL_LUMINANCE16 };
    // the corresponding output compressed format
    private static int[] compressedComponents = { 
            ARBTextureCompression.GL_COMPRESSED_RGBA_ARB,
            ARBTextureCompression.GL_COMPRESSED_RGB_ARB,
            ARBTextureCompression.GL_COMPRESSED_RGBA_ARB,
            ARBTextureCompression.GL_COMPRESSED_RGBA_ARB,
            ARBTextureCompression.GL_COMPRESSED_LUMINANCE_ALPHA_ARB,
            ARBTextureCompression.GL_COMPRESSED_RGB_ARB,
            ARBTextureCompression.GL_COMPRESSED_RGBA_ARB,
            ARBTextureCompression.GL_COMPRESSED_RGBA_ARB,
            ARBTextureCompression.GL_COMPRESSED_RGBA_ARB,
            ARBTextureCompression.GL_COMPRESSED_LUMINANCE_ARB };
    
    private static int[] imageFormats = { 
            GL11.GL_RGBA, 
            GL11.GL_RGB,
            GL11.GL_RGBA, 
            GL11.GL_RGBA, 
            GL11.GL_LUMINANCE_ALPHA, 
            GL11.GL_RGB,
            GL11.GL_RGBA, 
            GL11.GL_RGBA, 
            GL11.GL_RGBA,
            GL11.GL_LUMINANCE};
    
    private static int[] nativeCompressed = { 
            EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT,
            EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT,
            EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT,
            EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT
    };

    public TextureConverter() {};

    public Image convertTexture(Image image) {
        int imageHeight = image.getHeight();
        int imageWidth = image.getWidth();
        int imageType = image.getType();
               
        IntBuffer res = BufferUtils.createIntBuffer(4);
        res.clear();
        GL11.glGenTextures(res);
        int textureid = res.get(0);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureid);
        
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        
        if(!FastMath.isPowerOfTwo(imageWidth) 
             || !FastMath.isPowerOfTwo(imageHeight)) {
            
            final int maxSize = LWJGLMipMap.glGetIntegerv(GL11.GL_MAX_TEXTURE_SIZE);
            int w = LWJGLMipMap.nearestPower(imageWidth);
            if (w > maxSize) {
                w = maxSize;
            }

            int h = LWJGLMipMap.nearestPower(imageHeight);
            if (h > maxSize) {
                h = maxSize;
            }
            int format = imageFormats[imageType];//LWJGLTextureState.getImageFormat(imageType);
            int type = GL11.GL_UNSIGNED_BYTE;
            int bpp = LWJGLMipMap.bytesPerPixel(format, type);
            int size = (w + 4) * h * bpp;
            ByteBuffer scaledImage = BufferUtils.createByteBuffer(size);
            int error = MipMap.gluScaleImage(format, imageWidth,
                    imageHeight, type, image.getData(), w, h, type,
                    scaledImage);
            if (error != 0) {
                Util.checkGLError();
            }

            image.setWidth(w);
            image.setHeight(h);
            image.setData(scaledImage);
        }
        imageHeight = image.getHeight();
        imageWidth = image.getWidth();
        
        ByteBuffer data = image.getData();

        // do the compression
        GLU.gluBuild2DMipmaps(GL11.GL_TEXTURE_2D, 
                compressedComponents[imageType], 
                imageWidth, 
                imageHeight,
            imageFormats[imageType], 
            GL11.GL_UNSIGNED_BYTE,
            data);

        // calculate the number of mips
        int nummips = (int) FastMath.log2(FastMath.max(imageHeight, imageWidth)) +1;
        
        // retrieve the compressed mips of the image
        
        // create the array to hold data of the mips
        int[] internal_format = new int[nummips];
        int[] mip_size = new int[nummips];
        ByteBuffer[] mip_data = new ByteBuffer[nummips];
        
        int total_size = 0;

        // for each mip level
        for(int mip=0; mip<nummips; mip++) {
            res.clear();
            GL11.glGetTexLevelParameter(GL11.GL_TEXTURE_2D, mip, ARBTextureCompression.GL_TEXTURE_COMPRESSED_ARB, res);
            res.rewind();
            int compressed = res.get();
            //if(compressed == GL11.GL_TRUE) {
                // the compression was succesfull
                res.clear();
                GL11.glGetTexLevelParameter(GL11.GL_TEXTURE_2D, mip, GL11.GL_TEXTURE_INTERNAL_FORMAT, res);
                res.rewind();
                internal_format[mip] = res.get();
                res.clear();
                GL11.glGetTexLevelParameter(GL11.GL_TEXTURE_2D, mip, 
                        ARBTextureCompression.GL_TEXTURE_COMPRESSED_IMAGE_SIZE_ARB,
                        res);
                res.rewind();
                mip_size[mip] = res.get();
                if(mip_size[mip] > 0) {
                    total_size += mip_size[mip];
                    // allocate buffer for the mip
                    mip_data[mip] = BufferUtils.createByteBuffer(mip_size[mip]);
                    mip_data[mip].clear();
                    // get the compressed image data
                    ARBTextureCompression.glGetCompressedTexImageARB(GL11.GL_TEXTURE_2D, mip, mip_data[mip]);
                }
            //}
        }
        
        // release the texture
        res.clear();
        res.put(textureid);
        res.rewind();
        GL11.glDeleteTextures(res);
        
        // save the compressed texture with all its mips into a new Image object
        // figure out the new compressed image type
        int components = internal_format[0];
        int format = 0;
        for(int i=0; i<nativeCompressed.length; i++) {
            if(components == nativeCompressed[i] ) {
                // this is it
                format = Image.LAST_UNCOMPRESSED_TYPE+1+i;
            }
        }
        if(format == 0) {
            // not found among compressed, try to find
            // among normal ones
            for(int i=0; i<imageComponents.length; i++) {
                if(components == imageComponents[i] ) {
                    // this is it
                    format = i;
                }
            }
        }
        if(format==0) {
            // other way to find out?
        }
        if(format!=0) {
            // create a new image
            Image img = new Image();
            img.setHeight(imageHeight);
            img.setWidth(imageWidth);
            img.setType(format);
            img.setMipMapSizes(mip_size);
            // create a buffer to hold all the data
            ByteBuffer img_data = BufferUtils.createByteBuffer(total_size);
            // put all the mips in
            img_data.clear();
            for(int i=0;i<nummips; i++) {
                if(mip_data[i]!=null) {
                    mip_data[i].rewind();
                    img_data.put(mip_data[i]);
                }
            }
            img.setData(img_data);
            return img;
        }
        // still not found, huh?
        // TODO: save the OGL component type?
        return null;
    }

    /**
     * override MipMap to access helper methods
     */
    protected static class LWJGLMipMap extends MipMap {
        /**
         * @see MipMap#glGetIntegerv(int)
         */
        protected static int glGetIntegerv(int what) {
            return org.lwjgl.opengl.glu.Util.glGetIntegerv(what);
        }

        /**
         * @see MipMap#nearestPower(int)
         */
        protected static int nearestPower(int value) {
            return org.lwjgl.opengl.glu.Util.nearestPower(value);
        }

        /**
         * @see MipMap#bytesPerPixel(int, int)
         */
        protected static int bytesPerPixel(int format, int type) {
            return org.lwjgl.opengl.glu.Util.bytesPerPixel(format, type);
        }
    }

}
