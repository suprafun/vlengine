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

package com.vlengine.image;

import com.vlengine.util.DataBuffer;
import com.vlengine.util.geom.BufferUtils;
import java.nio.ByteBuffer;
import java.util.Arrays;


/**
 * <code>Image</code> defines a data format for a graphical image. The image
 * is defined by a type, a height and width, and the image data. The type can
 * be any one of the following types: RGBA4444, RGB888, RGBA5551, RGBA8888.
 * The width and height must be greater than 0. The data is contained in a
 * byte buffer, and should be packed before creation of the image object.
 * @author Mark Powell
 * @author vear (Arpad Vekas) reworked for VL engine
 * 
 */
public class Image {

    /**
     * When used in texture loading, this indicates to let jME guess 
     * the format, but not to use S3TC compression, even if available.
     */
    public static final int GUESS_FORMAT_NO_S3TC = -2;
    /**
     * When used in texture loading, this indicates to let jME guess 
     * the format.  jME will use S3TC compression, if available.
     */
    public static final int GUESS_FORMAT = -1;
    /**
     * 16-bit RGBA with 4 bits for each component.
     */
    public static final int RGBA4444 = 0;
    /**
     * 24-bit RGB with 8 bits for each component.
     */
    public static final int RGB888 = 1;
    /**
     * 16-bit RGBA with 5 bits for color components and 1 bit for alpha.
     */
    public static final int RGBA5551 = 2;
    /**
     * 32-bit RGBA with 8 bits for each component.
     */
    public static final int RGBA8888 = 3;

    /**
     * 16-bit RA with 8 bits for red and 8 bits for alpha.
     */
    public static final int RA88 = 4;
    
    /**
     * RGB888, compressed to DXT-1 internally.
     */
    public static final int RGB888_DXT1 = 5;

    /**
     * RGBA8888, compressed to DXT-1A internally.
     */
    public static final int RGBA8888_DXT1A = 6;

    /**
     * RGBA8888, compressed to DXT-3 internally.
     */
    public static final int RGBA8888_DXT3 = 7;

    /**
     * RGBA8888, compressed to DXT-5 internally.
     */
    public static final int RGBA8888_DXT5 = 8;
    
    // depth 16 bit format (ushort)
    public static final int DEPTH_COMPONENT16 = 9;
    
    public static final int LAST_UNCOMPRESSED_TYPE = DEPTH_COMPONENT16;

    /**
     * DXT-1 compressed format, no alpha.
     */
    public static final int DXT1_NATIVE = 10;

    /**
     * DXT-1 compressed format, one bit alpha.
     */
    public static final int DXT1A_NATIVE = 11;

    /**
     * DXT-3 compressed format, with alpha.
     */
    public static final int DXT3_NATIVE = 12;

    /**
     * DXT-5 compressed format, with alpha.
     */
    public static final int DXT5_NATIVE = 13;

    public static final int LAST_TYPE = DXT5_NATIVE;

    //image attributes
    protected int type;
    protected int width;
    protected int height;
    protected int[] mipMapSizes;
    protected transient ByteBuffer data;

    // texture-id for this image
    protected int texture_id = 0;

    /**
     * Constructor instantiates a new <code>Image</code> object. All values are
     * undefined.
     *
     */
    public Image() {

    }

    /**
     * Constructor instantiates a new <code>Image</code> object. The attributes
     * of the image are defined during construction.
     * @param type the type of image format.
     * @param width the width of the image.
     * @param height the height of the image.
     * @param data the image data.
     * @param mipMapSizes the array of mipmap sizes, or null for no mipmaps.
     */
    public Image(int type, int width, int height, ByteBuffer data, int[] mipMapSizes) {
        if(type < 0 || type > LAST_TYPE) {
            type = 0;
        }
        
        if ( mipMapSizes != null && mipMapSizes.length <= 1 ) {
            mipMapSizes = null;
        }
        
        this.type = type;
        this.width = width;
        this.height = height;
        this.data = data;
        this.mipMapSizes = mipMapSizes;
    }
    
    /**
     * Constructor instantiates a new <code>Image</code> object. The attributes
     * of the image are defined during construction.
     * @param type the type of image format.
     * @param width the width of the image.
     * @param height the height of the image.
     * @param data the image data.
     */
    public Image(int type, int width, int height, ByteBuffer data) {
        this(type, width, height, data, null);
    }

    /**
     * <code>setData</code> sets the data that makes up the image. This data
     * is packed into a single <code>ByteBuffer</code>.
     * @param data the data that contains the image information.
     */
    public void setData(ByteBuffer data) {
        this.data = data;
    }

    /**
     * Sets the mipmap sizes stored in this image's data buffer. Mipmaps are stored
     * sequentially, and the first mipmap is the main image data. To specify no mipmaps,
     * pass null and this will automatically be expanded into a single mipmap of the full
     * 
     * @param mipMapSizes the mipmap sizes array, or null for a single image map.
     */
    public void setMipMapSizes( int[] mipMapSizes ) {
        if ( mipMapSizes != null && mipMapSizes.length <= 1 )
            mipMapSizes = null;

        this.mipMapSizes = mipMapSizes;
    }
    
    /**
     * <code>setHeight</code> sets the height value of the image. It is
     * typically a good idea to try to keep this as a multiple of 2.
     * @param height the height of the image.
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * <code>setWidth</code> sets the width value of the image. It is
     * typically a good idea to try to keep this as a multiple of 2.
     * @param width the width of the image.
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     *
     * <code>setType</code> sets the image format for this image. If an
     * invalid value is passed, the type defaults to RGBA4444.
     * @param type the image format.
     */
    public void setType(int type) {
        if(type < 0 || type > LAST_TYPE) {
            type = 0;
        }
        this.type = type;
    }

    /**
     *
     * <code>getType</code> returns the image format for this image.
     * @return the image format.
     */
    public int getType() {
        return type;
    }
    
    /**
     * @return true if the image has an alpha channel, false otherwise
     */
    public boolean hasAlpha() {
        return (type == Image.RA88
                || type == Image.RGBA4444
                || type == Image.RGBA5551
                || type == Image.RGBA8888
                || type == Image.RGBA8888_DXT1A
                || type == Image.RGBA8888_DXT3
                || type == Image.RGBA8888_DXT5
                || type == Image.DXT1A_NATIVE
                || type == Image.DXT3_NATIVE
                || type == Image.DXT5_NATIVE
                );
    }

    /**
     * Returns whether the image type is compressed.
     * @return true if the image type is compressed, false otherwise.
     */
    public boolean isCompressedType() {
        return type > LAST_UNCOMPRESSED_TYPE;
    }
    
    /**
     *
     * <code>getWidth</code> returns the width of this image.
     * @return the width of this image.
     */
    public int getWidth() {
        return width;
    }

    /**
     *
     * <code>getHeight</code> returns the height of this image.
     * @return the height of this image.
     */
    public int getHeight() {
        return height;
    }

    /**
     *
     * <code>getData</code> returns the data for this image. If the data
     * is undefined, null will be returned.
     * @return the data for this image.
     */
    public ByteBuffer getData() {
        return data;
    }
    
    /**
     * Returns whether the image data contains mipmaps.
     * @return true if the image data contains mipmaps, false if not.
     */
    public boolean hasMipmaps() {
        return mipMapSizes != null;
    }
    
    /**
     * Returns the mipmap sizes for this image.
     * @return the mipmap sizes for this image.
     */
    public int[] getMipMapSizes() {
        return mipMapSizes;
    }

    public int getTextureId() {
        return texture_id;
    }
    
    public void setTextureId(int texid) {
        this.texture_id = texid;
    }
    
    @Override
    public boolean equals(Object other) {
      if (other == this) {
        return true;
      }
      if (!(other instanceof Image)) {
        return false;
      }
      Image that = (Image)other;
      if (this.getType() != that.getType()) return false;
      if (this.getWidth() != that.getWidth()) return false;
      if (this.getHeight() != that.getHeight()) return false;
      if (this.getData() != null && !this.getData().equals(that.getData())) return false;
      if (this.getData() == null && that.getData() != null) return false;
      if (this.getMipMapSizes() != null && !Arrays.equals(this.getMipMapSizes(), that.getMipMapSizes())) return false;
      if (this.getMipMapSizes() == null && that.getMipMapSizes() != null) return false;

      return true;
    }
    
     /**
     * Saves the given image to a ByteBuffer, which can later be
     * saved to a file.
     * @return The image data ready to be saved
     */
    public ByteBuffer save() {
        if(data==null)
            return null;
        // compute, how big a buffer we will need
        int buffsize = 4 + //header
                       4 + //type
                       4 + //width
                       4 + //height
                       4 + //number of mips
                       ( mipMapSizes == null ? 4 : mipMapSizes.length*4 ) + // size for each mip
                       data.limit(); // the total data buffer length
                
        // create the buffer
        DataBuffer saved = DataBuffer.allocate(buffsize);
        saved.clear();
        // header "VLT1"
        saved.put("VLT1");

        // type
        saved.put(type);
        // width
        saved.put(width);
        // height
        saved.put(height);
        // number of mips
        saved.put(mipMapSizes == null ? 1 : mipMapSizes.length);
        // for each mip, the size of the mip in bytes
        if(mipMapSizes == null || mipMapSizes.length == 1) {
            // if only one mip, the put in the data lenght
            saved.put(data.limit());
        } else {
            // put in all the mip sizes
            for(int i=0; i<mipMapSizes.length; i++)
                saved.put(mipMapSizes[i]);
        }
        // image data
        data.rewind();
        saved.put(data);
        
        return saved.getData();
    }
    
    /**
     * Loads an Image object from the given buffer.
     * @param buf The buffer containing the image data
     */
    public boolean load(ByteBuffer buf) {
        // create a DataBuffer
        DataBuffer load = new DataBuffer();
        load.setData(buf);
        load.rewind();
        
        // read in the header
        if(!"VLT1".equals(load.getChars(4))) {
            // not a texture file
            return false;
        }
        // type
        type = load.getInt();
        // width
        width = load.getInt();
        // height
        height = load.getInt();
        // number of mips
        int loadmips = load.getInt();
        int mips = loadmips;
        if(mips<1)
            mips = 1;
        mipMapSizes = new int[mips];
        
        int size = 0;
        if( loadmips == 0) {
            size = load.remaining();
            mipMapSizes[0] = size;
        } else {
            // load all the mips sizes
            for(int i=0; i<loadmips; i++) {
                mipMapSizes[i] = load.getInt();
                size += mipMapSizes[i];
            }
        }
        
        if( size != load.remaining()) {
            // the file lenght does not match the total mipmap size
            return false;
        }
        // create the data buffer
        data = BufferUtils.createByteBuffer(size);
        data.clear();
        // put the remaining bytes into the buffer
        data.put(load.getData());
        
        return true;
    }

}
