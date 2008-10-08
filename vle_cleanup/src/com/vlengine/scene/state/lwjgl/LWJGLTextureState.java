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

package com.vlengine.scene.state.lwjgl;

import com.vlengine.image.Image;
import com.vlengine.image.Texture;
import com.vlengine.math.FastMath;
import com.vlengine.math.Vector3f;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.RenderContext;
import com.vlengine.scene.state.TextureState;
import com.vlengine.system.VleException;
import com.vlengine.util.TextureManager;
import com.vlengine.util.geom.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.opengl.ARBDepthTexture;
import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.ARBShadow;
import org.lwjgl.opengl.ARBTextureBorderClamp;
import org.lwjgl.opengl.ARBTextureCompression;
import org.lwjgl.opengl.ARBTextureEnvCombine;
import org.lwjgl.opengl.ARBTextureEnvDot3;
import org.lwjgl.opengl.ARBTextureMirroredRepeat;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.EXTTextureCompressionS3TC;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.Util;
import org.lwjgl.opengl.glu.GLU;
import org.lwjgl.opengl.glu.MipMap;


/**
 * <code>LWJGLTextureState</code> subclasses the TextureState object using the
 * LWJGL API to access OpenGL for texture processing.
 * 
 * @author Mark Powell
 * @author Joshua Slack - updates, optimizations, etc. also StateRecords
 * @author vear (Arpad Vekas) reworked for VL engine
 * @version 
 */
public class LWJGLTextureState extends TextureState {
    private static final Logger logger = Logger.getLogger(LWJGLTextureState.class.getName());

    private static final String NOTLOADEDTEXTURE = "com/vlengine/data/notloaded.png";
    
    // this corresponds to Image types
    private static int[] imageComponents = { GL11.GL_RGBA4, GL11.GL_RGB8,
            GL11.GL_RGB5_A1, GL11.GL_RGBA8, GL11.GL_LUMINANCE8_ALPHA8,
            EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT,
            EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT,
            EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT,
            EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT,
            GL11.GL_LUMINANCE16,
            // native compressed types
            EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT,
            EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT,
            EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT,
            EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT };

    // image formats 
    private static int[] imageFormats = { GL11.GL_RGBA, GL11.GL_RGB,
            GL11.GL_RGBA, GL11.GL_RGBA, GL11.GL_LUMINANCE_ALPHA, GL11.GL_RGB,
            GL11.GL_RGBA, GL11.GL_RGBA, GL11.GL_RGBA,
            GL11.GL_LUMINANCE};

    private static boolean inited = false;

    private static final ColorRGBA defaultColor = new ColorRGBA(0,0,0,0);
    
    // temp data
    private static final FloatBuffer colorBuffer = BufferUtils.createColorBuffer(1);
    private static final FloatBuffer tmp_matrixBuffer = BufferUtils.createFloatBuffer(16);
    private static final Vector3f tmp_rotation1 = new Vector3f();
    private static final IntBuffer tmpIntbuffer = BufferUtils.createIntBuffer(1);
    private static final FloatBuffer eyePlaneS = BufferUtils.createFloatBuffer(4);
    private static final FloatBuffer eyePlaneT = BufferUtils.createFloatBuffer(4);
    private static final FloatBuffer eyePlaneR = BufferUtils.createFloatBuffer(4);
    private static final FloatBuffer eyePlaneQ = BufferUtils.createFloatBuffer(4);
    private static final IntBuffer tmpIntbuffer4 = BufferUtils.createIntBuffer(4);
    
    
    /**
     * Constructor instantiates a new <code>LWJGLTextureState</code> object.
     * The number of textures that can be combined is determined during
     * construction. This equates the number of texture units supported by the
     * graphics card.
     */
    public LWJGLTextureState() {
        super();
    }

    public static void init() {
        // See if we haven't already setup a texturestate before.
        if (!inited) {
            inited = true;
            // Check for support of multitextures.
            supportsMultiTexture = supportsMultiTextureDetected = GLContext.getCapabilities().GL_ARB_multitexture;
            
            // Check for support of fixed function dot3 environment settings
            supportsEnvDot3 = supportsEnvDot3Detected = GLContext.getCapabilities().GL_ARB_texture_env_dot3;
            
            // Check for support of fixed function dot3 environment settings
            supportsEnvCombine = supportsEnvCombineDetected = GLContext.getCapabilities().GL_ARB_texture_env_combine;

            // If we do support multitexturing, find out how many textures we
            // can handle.
            if (supportsMultiTexture) {
                IntBuffer buf = BufferUtils.createIntBuffer(16);
                GL11.glGetInteger(ARBMultitexture.GL_MAX_TEXTURE_UNITS_ARB, buf);
                numFixedTexUnits = buf.get(0);
            } else {
                numFixedTexUnits = 1;
            }

            // Go on to check number of texture units supported for vertex and
            // fragment shaders
            if (GLContext.getCapabilities().GL_ARB_shader_objects
                    && GLContext.getCapabilities().GL_ARB_vertex_shader
                    && GLContext.getCapabilities().GL_ARB_fragment_shader) {
                IntBuffer buf = BufferUtils.createIntBuffer(16);
                GL11.glGetInteger(
                        ARBVertexShader.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS_ARB,
                        buf);
                numVertexTexUnits = buf.get(0);
                GL11.glGetInteger(
                        ARBFragmentShader.GL_MAX_TEXTURE_IMAGE_UNITS_ARB, buf);
                numFragmentTexUnits = buf.get(0);
                GL11.glGetInteger(
                        ARBFragmentShader.GL_MAX_TEXTURE_COORDS_ARB, buf);
                numFragmentTexCoordUnits = buf.get(0);
            } else {
                // based on nvidia dev doc:
                // http://developer.nvidia.com/object/General_FAQ.html#t6
                // "For GPUs that do not support GL_ARB_fragment_program and
                // GL_NV_fragment_program, those two limits are set equal to
                // GL_MAX_TEXTURE_UNITS."
                numFragmentTexCoordUnits = numFixedTexUnits;
                numFragmentTexUnits = numFixedTexUnits;
                
                // We'll set this to 0 for now since we do not know:
                numVertexTexUnits = 0;
            }

            // Now determine the maximum number of supported texture units
            numTotalTexUnits = Math.max(numFragmentTexCoordUnits, 
                               Math.max(numFixedTexUnits, 
                               Math.max(numFragmentTexUnits,
                                        numVertexTexUnits)));

            // Check for S3 texture compression capability.
            supportsS3TCCompression = supportsS3TCCompressionDetected = GLContext.getCapabilities().GL_EXT_texture_compression_s3tc;

            // See if we support anisotropic filtering
            supportsAniso = supportsAnisoDetected = GLContext.getCapabilities().GL_EXT_texture_filter_anisotropic;

            if (supportsAniso) {
                // Due to LWJGL buffer check, you can't use smaller sized
                // buffers (min_size = 16 for glGetFloat()).
                FloatBuffer max_a = BufferUtils.createFloatBuffer(16);
                max_a.rewind();

                // Grab the maximum anisotropic filter.
                GL11.glGetFloat(
                                EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT,
                                max_a);

                // set max.
                maxAnisotropic = max_a.get(0);
            }

            // See if we support textures that are not power of 2 in size.
            supportsNonPowerTwo = supportsNonPowerTwoDetected = GLContext.getCapabilities().GL_ARB_texture_non_power_of_two;

            // See if we support textures that do not have width == height.
            supportsRectangular = supportsRectangularDetected = GLContext.getCapabilities().GL_ARB_texture_rectangle;

            eyePlaneS.put(1.0f).put(0.0f).put(0.0f).put(0.0f);
            eyePlaneT.put(0.0f).put(1.0f).put(0.0f).put(0.0f);
            eyePlaneR.put(0.0f).put(0.0f).put(1.0f).put(0.0f);
            eyePlaneQ.put(0.0f).put(0.0f).put(0.0f).put(1.0f);
        
            // vear: see if we support depth texture handling
            supportsDepthTextures = GLContext.getCapabilities().GL_ARB_depth_texture && GLContext.getCapabilities().GL_ARB_shadow;
            
            // load a default texture
            if (defaultTexture == null)
                try {
                    defaultTexture = TextureManager.loadTexture(TextureState.class.getClassLoader()
                            .getResource(NOTLOADEDTEXTURE), Texture.MM_LINEAR,
                            Texture.FM_LINEAR, 0.0f, true);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to load default texture: notloaded.png", e);
                }
            // Setup our default texture by adding it to our array and loading
            // it, then clearing our array.
            if( defaultTexture != null ) {
                LWJGLTextureState txs = new LWJGLTextureState();
                txs.setTexture(defaultTexture);
                txs.load(0);
            }
            // We're done initing! Wee! :)
            
        }
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

    @Override
    public void load(int unit) {
        Texture texture = getTexture(unit);
        if (texture == null) {
            return;
        }
        
        // Check we are in the right unit
        checkAndSetUnit(unit);
        
        // use texture-id stored in image
        Image image = texture.getImage();
        if (image == null) {
            logger.warning("Image data for texture is null.");
        }
        
        int texture_id = image.getTextureId();
        if (texture_id != 0) {
            //texture.setTextureId(texture_id);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture_id);
            return;
        }
        /*
        // Create the texture
        if (texture.getTextureKey() != null) {
            Texture cached = TextureManager.findCachedTexture(texture
                    .getTextureKey());
            if (cached == null) {
                TextureManager.addToCache(texture);
            } else if (cached.getTextureId() != 0) {
                texture.setTextureId(cached.getTextureId());
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, cached.getTextureId());
                return;
            }
        }
         */

        IntBuffer id = BufferUtils.createIntBuffer(1);
        id.clear();
        GL11.glGenTextures(id);
        int texid = id.get(0);
        texture.setTextureId(texid);
        TextureManager.registerForCleanup(texture);
        //image.setTextureId(texture.getTextureId());

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getTextureId());

        //TextureManager.registerForCleanup(texture.getTextureKey(), texture.getTextureId());

        // pass image data to OpenGL

        
        // set alignment to support images with width % 4 != 0, as images are
        // not aligned
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        // Get texture image data. Not all textures have image data.
        // For example, AM_COMBINE modes can use primary colors,
        // texture output, and constants to modify fragments via the
        // texture units.
        if (image != null) {
            if (!supportsNonPowerTwo
                    && (!FastMath.isPowerOfTwo(image.getWidth()) || !FastMath
                            .isPowerOfTwo(image.getHeight()))) {
                logger.warning("Attempted to apply texture with size that is not power "
                                + "of 2: "
                                + image.getWidth()
                                + " x "
                                + image.getHeight());

                final int maxSize = LWJGLMipMap
                        .glGetIntegerv(GL11.GL_MAX_TEXTURE_SIZE);

                int actualWidth = image.getWidth();
                int w = LWJGLMipMap.nearestPower(actualWidth);
                if (w > maxSize) {
                    w = maxSize;
                }

                int actualHeight = image.getHeight();
                int h = LWJGLMipMap.nearestPower(actualHeight);
                if (h > maxSize) {
                    h = maxSize;
                }
                logger.warning("Rescaling image to " + w + " x " + h + " !!!");

                // must rescale image to get "top" mipmap texture image
                int format = imageFormats[image.getType()];
                int type = GL11.GL_UNSIGNED_BYTE;
                int bpp = LWJGLMipMap.bytesPerPixel(format, type);
                ByteBuffer scaledImage = BufferUtils.createByteBuffer((w + 4)
                        * h * bpp);
                int error = MipMap.gluScaleImage(format, actualWidth,
                        actualHeight, type, image.getData(), w, h, type,
                        scaledImage);
                if (error != 0) {
                    Util.checkGLError();
                }

                image.setWidth(w);
                image.setHeight(h);
                image.setData(scaledImage);
            }

            // For textures which need mipmaps auto-generating and which
            // aren't using compressed images, generate the mipmaps.
            // A new mipmap builder may be needed to build mipmaps for
            // compressed textures.
            if (texture.getMipmap() >= Texture.MM_NEAREST_NEAREST
                    && !image.hasMipmaps() && !image.isCompressedType()) {
                // insure the buffer is ready for reading
                image.getData().rewind();
                GLU.gluBuild2DMipmaps(GL11.GL_TEXTURE_2D, imageComponents[image
                        .getType()], image.getWidth(), image.getHeight(),
                        imageFormats[image.getType()], GL11.GL_UNSIGNED_BYTE,
                        image.getData());
            } else {
                // Get mipmap data sizes and amount of mipmaps to send to
                // opengl. Then loop through all mipmaps and send them.
                int[] mipSizes = image.getMipMapSizes();
                ByteBuffer data = image.getData();
                int max = 1;
                int pos = 0;
                if (mipSizes == null) {
                    mipSizes = new int[] { data.capacity() };
                } else if (texture.getMipmap() != Texture.MM_NONE) {
                    max = mipSizes.length;
                }

                for (int m = 0; m < max; m++) {
                    int width = Math.max(1, image.getWidth() >> m);
                    int height = Math.max(1, image.getHeight() >> m);

                    data.position(pos);

                    if (image.isCompressedType()) {
                        data.limit(pos+mipSizes[m]);
                        ARBTextureCompression.glCompressedTexImage2DARB(
                                GL11.GL_TEXTURE_2D, m, imageComponents[image
                                        .getType()], width, height, 0, data);
                    } else if(image.getType() == Image.DEPTH_COMPONENT16 ) {
                        data.limit(data.position() + mipSizes[m]);
                        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, m,
                                imageComponents[Image.DEPTH_COMPONENT16], width,
                                height, 0, imageFormats[Image.DEPTH_COMPONENT16],
                                GL11.GL_UNSIGNED_SHORT, data);
                    } else {
                        data.limit(data.position() + mipSizes[m]);
                        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, m,
                                imageComponents[image.getType()], width,
                                height, 0, imageFormats[image.getType()],
                                GL11.GL_UNSIGNED_BYTE, data);
                    }

                    pos += mipSizes[m];
                }
                data.clear();
            }
        }
    }

    /**
     * <code>apply</code> manages the textures being described by the state.
     * If the texture has not been loaded yet, it is generated and loaded using
     * OpenGL11. This means the initial pass to set will be longer than
     * subsequent calls. The multitexture extension is used to define the
     * multiple texture states, with the number of units being determined at
     * construction time.
     * 
     * @see com.jme.scene.state.RenderState#apply()
     */
    @Override
    public void apply(RenderContext context) {
        if(indep==null)
                context.currentStates[getType()] = this;
        if (isEnabled()) {

            applyCorrection();

            // loop through all available texture units...
            for (int i = 0; i < numTotalTexUnits; i++) {
               apply(context, i);
            }
            
        } else {
            if(  !shaderOnly ) {
                // turn off texturing
                if (supportsMultiTexture) {
                    for (int i = 0; i < numFixedTexUnits; i++) {
                        checkAndSetUnit(i);
                        GL11.glDisable(GL11.GL_TEXTURE_2D);
                    }
                } else {
                        GL11.glDisable(GL11.GL_TEXTURE_2D);
                }
            }
        }
        
    }

    public void applyCorrection() {
        int glHint = getPerspHint(getCorrection());
        // set up correction mode
        GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT,
                glHint);
    }
    
    public void apply(RenderContext ctx, int i) {
        Texture texture;
        // grab a texture for this unit, if available
        texture = getTexture(i);

        // check for invalid textures - ones that have no opengl id and
        // no image data
        if (texture != null && texture.getTextureId() == 0
                && texture.getImage() == null)
            texture = null;

        // null textures above fixed limit do not need to be disabled
        // since they are not really part of the pipeline.
        if (texture == null) {
            if (i >= numFixedTexUnits)
                return;
            else {
                // a null texture indicates no texturing at this unit
                // Disable 2D texturing on this unit if enabled.
                    // Check we are in the right unit
                checkAndSetUnit(i);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                if (i < idCache.length)
                    idCache[i] = 0;

                // next texture!
                return;
            }
        }

        checkAndSetUnit(i);

        // Time to bind the texture, so see if we need to load in image
        // data for this texture.
        if (texture.getTextureId() == 0) {
            // texture not yet loaded.
            // this will load and bind and set the records...
            load(i);
            if (texture.getTextureId() == 0) return;
        } else {
            // texture already exists in OpenGL, just bind it if needed
            // Check we are in the right unit
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getTextureId());
        }

        // Set the idCache value for this unit of this texture state
        // This is done so during state comparison we don't have to
        // spend a lot of time pulling out classes and finding field
        // data.
        idCache[i] = texture.getTextureId();

        // Some texture things only apply to fixed function pipeline
        if (i < numFixedTexUnits && !shaderOnly ) {

            // Enable 2D texturing on this unit if not enabled.
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            // Set our blend color, if needed.
            applyBlendColor(texture);
            // Set the texture environment mode if this unit isn't
            // already set properly
            int glEnvMode = getGLEnvMode(texture.getApply());
            applyEnvMode(glEnvMode);
            // If our mode is combine, and we support multitexturing
            // apply combine settings.
            if (glEnvMode == ARBTextureEnvCombine.GL_COMBINE_ARB && supportsMultiTexture && supportsEnvCombine) {
                applyCombineFactors(texture);
            }
        }

        // Other items only apply to textures below the frag unit limit
        if (i < numFragmentTexUnits) {
            // texture specific params
            applyFilter(texture);
            applyWrap(texture);
        }

        // Other items only apply to textures below the frag tex coord unit limit
        if (i < this.numFixedTexUnits ) { //numFragmentTexCoordUnits) {
            // Now time to play with texture matrices
            // Determine which transforms to do.
            applyTextureTransforms(texture);
            // Now let's look at automatic texture coordinate generation.
            applyTexCoordGeneration(texture);
        }
        
        // vear: depth texture handling
        if(supportsDepthTextures) {
            applyDepthTexture(texture);
        }
    }
    
    public static void applyCombineFactors(Texture texture) {
        // check that this is a valid fixed function unit.  glTexEnv is only supported for unit < GL_MAX_TEXTURE_UNITS
        
        // first thing's first... if we are doing dot3 and don't
        // support it, disable this texture.
        
        if (!supportsEnvDot3 && (texture.getCombineFuncAlpha() == Texture.ACF_DOT3_RGB
                || texture.getCombineFuncAlpha() == Texture.ACF_DOT3_RGBA 
                || texture.getCombineFuncRGB() == Texture.ACF_DOT3_RGB
                || texture.getCombineFuncRGB() == Texture.ACF_DOT3_RGBA)) {
        
            GL11.glDisable(GL11.GL_TEXTURE_2D);            
            // No need to continue
            return;
        }

        // Okay, now let's set our scales if we need to:
        // First RGB Combine scale
        GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_RGB_SCALE_ARB, texture
                .getCombineScaleRGB());
        // Then Alpha Combine scale
        GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_ALPHA_SCALE, texture
                .getCombineScaleAlpha());
        
        // Time to set the RGB combines
        int rgbCombineFunc = texture.getCombineFuncRGB();
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_COMBINE_RGB_ARB,
                getGLCombineFunc(rgbCombineFunc));
        
        int combSrcRGB = texture.getCombineSrc0RGB();
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_SOURCE0_RGB_ARB, getGLCombineSrc(combSrcRGB));
        
        int combOpRGB = texture.getCombineOp0RGB();
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_OPERAND0_RGB_ARB, getGLCombineOpRGB(combOpRGB));

        if (rgbCombineFunc != Texture.ACF_REPLACE) {
            
            combSrcRGB = texture.getCombineSrc1RGB();
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_SOURCE1_RGB_ARB, getGLCombineSrc(combSrcRGB));

            combOpRGB = texture.getCombineOp1RGB();
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_OPERAND1_RGB_ARB, getGLCombineOpRGB(combOpRGB));

            if (rgbCombineFunc == Texture.ACF_INTERPOLATE) {
                
                combSrcRGB = texture.getCombineSrc2RGB();
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_SOURCE2_RGB_ARB, getGLCombineSrc(combSrcRGB));
                
                combOpRGB = texture.getCombineOp2RGB();
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_OPERAND2_RGB_ARB, getGLCombineOpRGB(combOpRGB));

            }
        }

        
        // Now Alpha combines
        int alphaCombineFunc = texture.getCombineFuncAlpha();
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_COMBINE_ALPHA_ARB,
                getGLCombineFunc(alphaCombineFunc));
        
        int combSrcAlpha = texture.getCombineSrc0Alpha();
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_SOURCE0_ALPHA_ARB, getGLCombineSrc(combSrcAlpha));
        
        int combOpAlpha = texture.getCombineOp0Alpha();
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_OPERAND0_ALPHA_ARB, getGLCombineOpAlpha(combOpAlpha));

        if (alphaCombineFunc != Texture.ACF_REPLACE) {

            combSrcAlpha = texture.getCombineSrc1Alpha();
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_SOURCE1_ALPHA_ARB, getGLCombineSrc(combSrcAlpha));
            
            combOpAlpha = texture.getCombineOp1Alpha();
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_OPERAND1_ALPHA_ARB, getGLCombineOpAlpha(combOpAlpha));
            if (alphaCombineFunc == Texture.ACF_INTERPOLATE) {

                combSrcAlpha = texture.getCombineSrc2Alpha();
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_SOURCE2_ALPHA_ARB, getGLCombineSrc(combSrcAlpha));
                
                combOpAlpha = texture.getCombineOp2Alpha();
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_OPERAND2_ALPHA_ARB, getGLCombineOpAlpha(combOpAlpha));
            }
        }
    }
    
    public static int getGLCombineOpRGB(int combineOpRGB) {
        switch (combineOpRGB) {
            case Texture.ACO_SRC_COLOR:
                return GL11.GL_SRC_COLOR;
            case Texture.ACO_ONE_MINUS_SRC_COLOR:
                return GL11.GL_ONE_MINUS_SRC_COLOR;
            case Texture.ACO_SRC_ALPHA:
                return GL11.GL_SRC_ALPHA;
            case Texture.ACO_ONE_MINUS_SRC_ALPHA:
                return GL11.GL_ONE_MINUS_SRC_ALPHA;
            default:
                return GL11.GL_SRC_COLOR;
        }
    }
    
    public static int getGLCombineOpAlpha(int combineOpAlpha) {
        switch (combineOpAlpha) {
            case Texture.ACO_SRC_ALPHA:
                return GL11.GL_SRC_ALPHA;
            case Texture.ACO_ONE_MINUS_SRC_ALPHA:
                return GL11.GL_ONE_MINUS_SRC_ALPHA;
            case Texture.ACO_SRC_COLOR: // these 2 we just put here to help prevent errors.
                return GL11.GL_SRC_ALPHA;
            case Texture.ACO_ONE_MINUS_SRC_COLOR:
                return GL11.GL_ONE_MINUS_SRC_ALPHA;
            default:
                return GL11.GL_SRC_ALPHA;
        }
    }

    public static int getGLCombineSrc(int combineSrc) {
        switch (combineSrc) {
            case Texture.ACS_TEXTURE:
                return GL11.GL_TEXTURE;
            case Texture.ACS_PRIMARY_COLOR:
                return ARBTextureEnvCombine.GL_PRIMARY_COLOR_ARB;
            case Texture.ACS_CONSTANT:
                return ARBTextureEnvCombine.GL_CONSTANT_ARB;
            case Texture.ACS_PREVIOUS:
                return ARBTextureEnvCombine.GL_PREVIOUS_ARB;
            case Texture.ACS_TEXTURE0:
                return ARBMultitexture.GL_TEXTURE0_ARB;
            case Texture.ACS_TEXTURE1:
                return ARBMultitexture.GL_TEXTURE1_ARB;
            case Texture.ACS_TEXTURE2:
                return ARBMultitexture.GL_TEXTURE2_ARB;
            case Texture.ACS_TEXTURE3:
                return ARBMultitexture.GL_TEXTURE3_ARB;
            case Texture.ACS_TEXTURE4:
                return ARBMultitexture.GL_TEXTURE4_ARB;
            case Texture.ACS_TEXTURE5:
                return ARBMultitexture.GL_TEXTURE5_ARB;
            case Texture.ACS_TEXTURE6:
                return ARBMultitexture.GL_TEXTURE6_ARB;
            case Texture.ACS_TEXTURE7:
                return ARBMultitexture.GL_TEXTURE7_ARB;
            case Texture.ACS_TEXTURE8:
                return ARBMultitexture.GL_TEXTURE8_ARB;
            case Texture.ACS_TEXTURE9:
                return ARBMultitexture.GL_TEXTURE9_ARB;
            case Texture.ACS_TEXTURE10:
                return ARBMultitexture.GL_TEXTURE10_ARB;
            case Texture.ACS_TEXTURE11:
                return ARBMultitexture.GL_TEXTURE11_ARB;
            case Texture.ACS_TEXTURE12:
                return ARBMultitexture.GL_TEXTURE12_ARB;
            case Texture.ACS_TEXTURE13:
                return ARBMultitexture.GL_TEXTURE13_ARB;
            case Texture.ACS_TEXTURE14:
                return ARBMultitexture.GL_TEXTURE14_ARB;
            case Texture.ACS_TEXTURE15:
                return ARBMultitexture.GL_TEXTURE15_ARB;
            case Texture.ACS_TEXTURE16:
                return ARBMultitexture.GL_TEXTURE16_ARB;
            case Texture.ACS_TEXTURE17:
                return ARBMultitexture.GL_TEXTURE17_ARB;
            case Texture.ACS_TEXTURE18:
                return ARBMultitexture.GL_TEXTURE18_ARB;
            case Texture.ACS_TEXTURE19:
                return ARBMultitexture.GL_TEXTURE19_ARB;
            case Texture.ACS_TEXTURE20:
                return ARBMultitexture.GL_TEXTURE20_ARB;
            case Texture.ACS_TEXTURE21:
                return ARBMultitexture.GL_TEXTURE21_ARB;
            case Texture.ACS_TEXTURE22:
                return ARBMultitexture.GL_TEXTURE22_ARB;
            case Texture.ACS_TEXTURE23:
                return ARBMultitexture.GL_TEXTURE23_ARB;
            case Texture.ACS_TEXTURE24:
                return ARBMultitexture.GL_TEXTURE24_ARB;
            case Texture.ACS_TEXTURE25:
                return ARBMultitexture.GL_TEXTURE25_ARB;
            case Texture.ACS_TEXTURE26:
                return ARBMultitexture.GL_TEXTURE26_ARB;
            case Texture.ACS_TEXTURE27:
                return ARBMultitexture.GL_TEXTURE27_ARB;
            case Texture.ACS_TEXTURE28:
                return ARBMultitexture.GL_TEXTURE28_ARB;
            case Texture.ACS_TEXTURE29:
                return ARBMultitexture.GL_TEXTURE29_ARB;
            case Texture.ACS_TEXTURE30:
                return ARBMultitexture.GL_TEXTURE30_ARB;
            case Texture.ACS_TEXTURE31:
                return ARBMultitexture.GL_TEXTURE31_ARB;
            default:
                return ARBTextureEnvCombine.GL_PRIMARY_COLOR_ARB;
        }
    }
    
    public static int getGLCombineFunc(int combineFunc) {
        switch (combineFunc) {
            case Texture.ACF_REPLACE:
                return GL11.GL_REPLACE;
            case Texture.ACF_ADD:
                return GL11.GL_ADD;
            case Texture.ACF_ADD_SIGNED:
                return ARBTextureEnvCombine.GL_ADD_SIGNED_ARB;
            case Texture.ACF_SUBTRACT:
                if (GLContext.getCapabilities().OpenGL13) {
                    return GL13.GL_SUBTRACT;
                } else {
                    // XXX: lwjgl's ARBTextureEnvCombine is missing subtract?
                    // for now... a backup.
                    return GL11.GL_MODULATE;
                }
            case Texture.ACF_INTERPOLATE:
                return ARBTextureEnvCombine.GL_INTERPOLATE_ARB;
            case Texture.ACF_DOT3_RGB:
                return ARBTextureEnvDot3.GL_DOT3_RGB_ARB;
            case Texture.ACF_DOT3_RGBA:
                return ARBTextureEnvDot3.GL_DOT3_RGBA_ARB;
            case Texture.ACF_MODULATE:
            default:
                return GL11.GL_MODULATE;
        }
    }

    public static void applyEnvMode(int glEnvMode) {
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV,
                GL11.GL_TEXTURE_ENV_MODE, glEnvMode);
    }

    public static void applyBlendColor(Texture texture) {
        ColorRGBA texBlend = texture.getBlendColor();
        if (texBlend == null) texBlend = defaultColor;
            colorBuffer.clear();
            colorBuffer.put(texBlend.r).put(texBlend.g).put(texBlend.b).put(texBlend.a);
            colorBuffer.rewind();
            GL11.glTexEnv(GL11.GL_TEXTURE_ENV,
                    GL11.GL_TEXTURE_ENV_COLOR, colorBuffer);
    }

    public static void applyTextureTransforms(Texture texture) {
        
        // Should we load a base matrix?
        boolean doMatrix = (texture.getMatrix() != null && !texture.getMatrix()
                .isIdentity());

        // Should we apply transforms?
        boolean doTrans = texture.getTranslation() != null
                && (texture.getTranslation().x != 0
                        || texture.getTranslation().y != 0 
                        || texture.getTranslation().z != 0);
        boolean doRot = texture.getRotation() != null
                && !texture.getRotation().isIdentity();
        boolean doScale = texture.getScale() != null
                && (texture.getScale().x != 1 
                        || texture.getScale().y != 1 
                        || texture.getScale().z != 1);

        // Now do them.
        if (doMatrix || doTrans || doRot || doScale) {
            GL11.glMatrixMode(GL11.GL_TEXTURE);
            if (doMatrix) {
                texture.getMatrix().fillFloatBuffer(tmp_matrixBuffer, true);
                GL11.glLoadMatrix(tmp_matrixBuffer);
            } else {
                GL11.glLoadIdentity();
            }
            if (doTrans) {
                GL11.glTranslatef(texture.getTranslation().x, texture
                        .getTranslation().y, texture.getTranslation().z);
            }
            if (doRot) {
                Vector3f vRot = tmp_rotation1;
                float rot = texture.getRotation().toAngleAxis(vRot)
                        * FastMath.RAD_TO_DEG;
                GL11.glRotatef(rot, vRot.x, vRot.y, vRot.z);
            }
            if (doScale)
                GL11.glScalef(texture.getScale().x, texture.getScale().y,
                        texture.getScale().z);
        } else {
            GL11.glMatrixMode(GL11.GL_TEXTURE);
            GL11.glLoadIdentity();
        }
        // Switch back to the modelview matrix for further operations
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    public static void applyTexCoordGeneration(Texture texture) {
        
        if (texture.getEnvironmentalMapMode() == Texture.EM_NONE) {
            
            // No coordinate generation
                GL11.glDisable(GL11.GL_TEXTURE_GEN_Q);
                GL11.glDisable(GL11.GL_TEXTURE_GEN_R);
                GL11.glDisable(GL11.GL_TEXTURE_GEN_S);
                GL11.glDisable(GL11.GL_TEXTURE_GEN_T);
        } else if (texture.getEnvironmentalMapMode() == Texture.EM_SPHERE) {
            // generate spherical texture coordinates
                GL11.glTexGeni(GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE,
                        GL11.GL_SPHERE_MAP);

                GL11.glTexGeni(GL11.GL_T, GL11.GL_TEXTURE_GEN_MODE,
                        GL11.GL_SPHERE_MAP);

                GL11.glDisable(GL11.GL_TEXTURE_GEN_Q);
                GL11.glDisable(GL11.GL_TEXTURE_GEN_R);
                GL11.glEnable(GL11.GL_TEXTURE_GEN_S);
                GL11.glEnable(GL11.GL_TEXTURE_GEN_T);
        } else if (texture.getEnvironmentalMapMode() == Texture.EM_EYE_LINEAR) {
            // generate eye linear texture coordinates
                GL11.glTexGeni(GL11.GL_Q, GL11.GL_TEXTURE_GEN_MODE,
                        GL11.GL_EYE_LINEAR);

                GL11.glTexGeni(GL11.GL_R, GL11.GL_TEXTURE_GEN_MODE,
                        GL11.GL_EYE_LINEAR);

                GL11.glTexGeni(GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE,
                        GL11.GL_EYE_LINEAR);

                GL11.glTexGeni(GL11.GL_T, GL11.GL_TEXTURE_GEN_MODE,
                        GL11.GL_EYE_LINEAR);

            eyePlaneS.rewind();
            GL11.glTexGen(GL11.GL_S, GL11.GL_EYE_PLANE, eyePlaneS);
            eyePlaneT.rewind();
            GL11.glTexGen(GL11.GL_T, GL11.GL_EYE_PLANE, eyePlaneT);
            eyePlaneR.rewind();
            GL11.glTexGen(GL11.GL_R, GL11.GL_EYE_PLANE, eyePlaneR);
            eyePlaneQ.rewind();
            GL11.glTexGen(GL11.GL_Q, GL11.GL_EYE_PLANE, eyePlaneQ);

                GL11.glEnable(GL11.GL_TEXTURE_GEN_Q);
                GL11.glEnable(GL11.GL_TEXTURE_GEN_R);
                GL11.glEnable(GL11.GL_TEXTURE_GEN_S);
                GL11.glEnable(GL11.GL_TEXTURE_GEN_T);
        } else if (texture.getEnvironmentalMapMode() == Texture.EM_OBJECT_LINEAR) {
            // generate object linear texture coordinates
                GL11.glTexGeni(GL11.GL_Q, GL11.GL_TEXTURE_GEN_MODE,
                        GL11.GL_OBJECT_LINEAR);

                GL11.glTexGeni(GL11.GL_R, GL11.GL_TEXTURE_GEN_MODE,
                        GL11.GL_OBJECT_LINEAR);

                GL11.glTexGeni(GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE,
                        GL11.GL_OBJECT_LINEAR);

                GL11.glTexGeni(GL11.GL_T, GL11.GL_TEXTURE_GEN_MODE,
                        GL11.GL_OBJECT_LINEAR);

            eyePlaneS.rewind();
            GL11.glTexGen(GL11.GL_S, GL11.GL_OBJECT_PLANE, eyePlaneS);
            eyePlaneT.rewind();
            GL11.glTexGen(GL11.GL_T, GL11.GL_OBJECT_PLANE, eyePlaneT);
            eyePlaneR.rewind();
            GL11.glTexGen(GL11.GL_R, GL11.GL_OBJECT_PLANE, eyePlaneR);
            eyePlaneQ.rewind();
            GL11.glTexGen(GL11.GL_Q, GL11.GL_OBJECT_PLANE, eyePlaneQ);

                GL11.glEnable(GL11.GL_TEXTURE_GEN_Q);
                GL11.glEnable(GL11.GL_TEXTURE_GEN_R);
                GL11.glEnable(GL11.GL_TEXTURE_GEN_S);
                GL11.glEnable(GL11.GL_TEXTURE_GEN_T);
        }
    }

    public static int getGLEnvMode(int apply) {
        switch (apply) {
            case Texture.AM_REPLACE:
                return GL11.GL_REPLACE;
            case Texture.AM_BLEND:
                return GL11.GL_BLEND;
            case Texture.AM_COMBINE:
                return ARBTextureEnvCombine.GL_COMBINE_ARB;
            case Texture.AM_DECAL:
                return GL11.GL_DECAL;
            case Texture.AM_ADD:
                return GL11.GL_ADD;
            case Texture.AM_MODULATE:
            default:
                return GL11.GL_MODULATE;
        }
    }

    public static int getPerspHint(int correction) {
        switch (correction) {
            case TextureState.CM_AFFINE:
                return GL11.GL_FASTEST;
            case TextureState.CM_PERSPECTIVE:
            default:
                return GL11.GL_NICEST;
        }
    }

    // If we support multtexturing, specify the unit we are affecting.
    public static void checkAndSetUnit(int unit) {
        if (unit >= numTotalTexUnits || !supportsMultiTexture || unit < 0) {
            // ignore this request as it is not valid for the user's hardware.
            throw new VleException("Unsupported texture unit number");
        }
        ARBMultitexture.glActiveTextureARB(ARBMultitexture.GL_TEXTURE0_ARB + unit);
    }

    /**
     * Check if the filter settings of this particular texture have been changed and
     * apply as needed.
     * 
     * @param texture
     *            our texture object
     * @param texRecord
     *            our record of the last state of the texture in gl
     * @param record 
     */
    public static void applyFilter(Texture texture) {
        int magFilter = getGLMagFilter(texture.getFilter());
        // set up magnification filter
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                    GL11.GL_TEXTURE_MAG_FILTER, magFilter);

        int minFilter = getGLMinFilter(texture.getMipmap());
        // set up mipmap filter
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                    GL11.GL_TEXTURE_MIN_FILTER, minFilter);
        
        // set up aniso filter
        if (supportsAniso) {
            float aniso = texture.getAnisoLevel() * (maxAnisotropic - 1.0f);
            aniso += 1.0f;
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D,
                    EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT,
                    aniso);
        }
    }

    public static int getGLMagFilter(int magFilter) {
        switch (magFilter) {
            case Texture.FM_LINEAR:
                return GL11.GL_LINEAR;
            case Texture.FM_NEAREST:
            default: 
                return GL11.GL_NEAREST;
                
        }
    }

    public static int getGLMinFilter(int minFilter) {
        switch (minFilter) {
            case Texture.MM_LINEAR:
                return GL11.GL_LINEAR;
            case Texture.MM_LINEAR_LINEAR:
                return GL11.GL_LINEAR_MIPMAP_LINEAR;
            case Texture.MM_LINEAR_NEAREST:
                return GL11.GL_LINEAR_MIPMAP_NEAREST;
            case Texture.MM_NEAREST:
                return GL11.GL_NEAREST;
            case Texture.MM_NEAREST_NEAREST:
                return GL11.GL_NEAREST_MIPMAP_NEAREST;
            case Texture.MM_NONE:
                return GL11.GL_NEAREST;
            case Texture.MM_NEAREST_LINEAR:
            default: 
                return GL11.GL_NEAREST_MIPMAP_LINEAR;
                
        }
    }

    /**
     * Check if the wrap mode of this particular texture has been changed and
     * apply as needed.
     * 
     * @param texture
     *            our texture object
     * @param texRecord
     *            our record of the last state of the unit in gl
     * @param record 
     */
    public static void applyWrap(Texture texture) {
        int wrapS = -1;
        int wrapT = -1;
        switch (texture.getWrap()) {
            case Texture.WM_ECLAMP_S_ECLAMP_T:
                wrapS = GL12.GL_CLAMP_TO_EDGE;
                wrapT = GL12.GL_CLAMP_TO_EDGE;
                break;
            case Texture.WM_BCLAMP_S_BCLAMP_T:
                wrapS = ARBTextureBorderClamp.GL_CLAMP_TO_BORDER_ARB;
                wrapT = ARBTextureBorderClamp.GL_CLAMP_TO_BORDER_ARB;
                break;
            case Texture.WM_CLAMP_S_CLAMP_T:
                wrapS = GL11.GL_CLAMP;
                wrapT = GL11.GL_CLAMP;
                break;
            case Texture.WM_CLAMP_S_WRAP_T:
                wrapS = GL11.GL_CLAMP;
                wrapT = GL11.GL_REPEAT;
                break;
            case Texture.WM_WRAP_S_CLAMP_T:
                wrapS = GL11.GL_REPEAT;
                wrapT = GL11.GL_CLAMP;
                break;
            case Texture.WM_MIRRORED_S_MIRRORED_T:
                if (GLContext.getCapabilities().GL_ARB_texture_mirrored_repeat) {
                    wrapS = ARBTextureMirroredRepeat.GL_MIRRORED_REPEAT_ARB;
                    wrapT = ARBTextureMirroredRepeat.GL_MIRRORED_REPEAT_ARB;
                    break;
                }
                // no support, so fall through to wrap/wrap
            case Texture.WM_WRAP_S_WRAP_T:
            default:
                wrapS = GL11.GL_REPEAT;
                wrapT = GL11.GL_REPEAT;
        }
        
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_WRAP_S, wrapS);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_WRAP_T, wrapT);
        
    }

    public static void applyDepthTexture(Texture t) {
        // determine the internal format, if we dont know it yet
        int intformat = t.getInternalFormat();
        if( intformat == 0) {
            // first time, determine internal format
            tmpIntbuffer4.clear();
            GL11.glGetTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT, tmpIntbuffer4);
            tmpIntbuffer4.rewind();
            intformat = tmpIntbuffer4.get();
            t.setInternalFormat(intformat);
        }
        
        // is it a depth texture?
        if(intformat == ARBDepthTexture.GL_DEPTH_COMPONENT16_ARB
            || intformat == ARBDepthTexture.GL_DEPTH_COMPONENT24_ARB
            || intformat == ARBDepthTexture.GL_DEPTH_COMPONENT32_ARB
            || intformat == GL11.GL_DEPTH_COMPONENT
            ) {
            
            // we are working with a depth texture, set handling
            int dcm;
            int dcf = GL11.GL_GEQUAL;
            switch(t.getDepthCompareMode()) {
                case Texture.DC_GEQUAL :
                    dcm = ARBShadow.GL_COMPARE_R_TO_TEXTURE_ARB;
                    dcf = GL11.GL_GEQUAL;
                    break;
                case Texture.DC_LEQUAL :
                    dcm = ARBShadow.GL_COMPARE_R_TO_TEXTURE_ARB;
                    dcf = GL11.GL_LEQUAL;
                    break;
                default:
                    dcm=GL11.GL_NONE;
            }
            // set the compare mode
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, ARBShadow.GL_TEXTURE_COMPARE_MODE_ARB, dcm);
            if(dcm != GL11.GL_NONE) {
                // we have compare mode, set function too
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, ARBShadow.GL_TEXTURE_COMPARE_FUNC_ARB, dcf);
                // set the depth texture apply mode
                int dtm;
                switch(t.getDepthTextureMode()) {
                    case Texture.DT_ALPHA :
                        dtm = GL11.GL_ALPHA;
                        break;
                    case Texture.DT_LUMINANCE:
                        dtm = GL11.GL_LUMINANCE;
                        break;
                    default:
                        dtm = GL11.GL_INTENSITY;
                }
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, ARBDepthTexture.GL_DEPTH_TEXTURE_MODE_ARB, dtm);
            }
        //ARBShadow. 
                
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.jme.scene.state.TextureState#delete(int)
     */
    @Override
    public void delete(int unit) {
        if (unit < 0 || unit >= texture.size() || texture.get(unit) == null)
            return;
        
        Texture tex = texture.get(unit);
        Image img = tex.getImage();
        if(img == null)
            return;
        int texId = img.getTextureId();

        IntBuffer id = tmpIntbuffer;
        id.clear();
        id.put(texId);
        id.rewind();
        img.setTextureId(0);

        GL11.glDeleteTextures(id);
        
        // if the texture was currently bound glDeleteTextures reverts the binding to 0
        // however we still have to clear it from currentTexture.
        idCache[unit] = 0;
        TextureManager.removeTextureId(texId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.jme.scene.state.TextureState#deleteAll()
     */
    @Override
    public void deleteAll() {
        
        IntBuffer id = BufferUtils.createIntBuffer(texture.size());

        for (int i = 0; i < texture.size(); i++) {
            Texture tex = texture.get(i);
            if (tex == null)
                continue;
            Image img = tex.getImage();
            if(img==null)
                continue;
            //if (removeFromCache) TextureManager.releaseTexture(tex);
            int texId = img.getTextureId();
            
            id.put(texId);
            img.setTextureId(0);

            // if the texture was currently bound glDeleteTextures reverts the binding to 0
            // however we still have to clear it from currentTexture.
            idCache[i] = 0;
            TextureManager.removeTextureId(texId);
        }

        // Now delete them all from GL in one fell swoop.
        id.rewind();
        GL11.glDeleteTextures(id);
    }

    public static void deleteTextureId(int textureId) {
        IntBuffer id = BufferUtils.createIntBuffer(1);
        id.clear();
        id.put(textureId);
        id.rewind();
        GL11.glDeleteTextures(id);
    }

    /**
     * Useful for external lwjgl based classes that need to safely set the
     * current texture.
     */
    public static void doTextureBind(int textureId, int unit) {
        checkAndSetUnit(unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }

    public static int getImageFormat(int imageType) {
        return imageFormats[imageType];
    }

    @Override
    public void update(RenderContext ctx) {}
}