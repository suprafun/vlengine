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

package com.vlengine.scene.state;

import com.vlengine.image.Image;
import com.vlengine.image.Texture;
import com.vlengine.renderer.RenderContext;
import com.vlengine.util.FastList;
import java.util.logging.Logger;

public class TextureState extends RenderState {
    private static final Logger logger = Logger.getLogger(TextureState.class
            .getName());

    protected static Texture defaultTexture = null;

    /**
     * Correction modifier makes no color corrections, and is the fastest.
     */
    public static final int CM_AFFINE = 0;

    /**
     * Correction modifier makes color corrections based on perspective and
     * is slower than CM_AFFINE.
     */
    public static final int CM_PERSPECTIVE = 1;

    /** The texture(s). */
    protected transient FastList<Texture> texture;

    /** The total number of supported texture units. */
    protected static int numTotalTexUnits = -1;

    /** The number of texture units availible for fixed functionality */
    protected static int numFixedTexUnits = -1;

    /** The number of texture units availible to vertex shader */
    protected static int numVertexTexUnits = -1;

    /** The number of texture units availible to fragment shader */
    protected static int numFragmentTexUnits = -1;

    /** The number of texture coordinate sets available */
    protected static int numFragmentTexCoordUnits = -1;

    protected static float maxAnisotropic = -1.0f;

    /** True if multitexturing is supported. */
    protected static boolean supportsMultiTexture = false;
    protected static boolean supportsMultiTextureDetected = false;

    /** True if combine dot3 is supported. */
    protected static boolean supportsEnvDot3 = false;
    protected static boolean supportsEnvDot3Detected = false;

    /** True if combine dot3 is supported. */
    protected static boolean supportsEnvCombine = false;
    protected static boolean supportsEnvCombineDetected = false;

    /** True if anisofiltering is supported. */
    protected static boolean supportsAniso = false;
    protected static boolean supportsAnisoDetected = false;

    /** True if non pow 2 texture sizes are supported. */
    protected static boolean supportsNonPowerTwo = false;
    protected static boolean supportsNonPowerTwoDetected = false;

    /** True if rectangular textures are supported (vs. only square textures) */
    protected static boolean supportsRectangular = false;
    protected static boolean supportsRectangularDetected = false;

    /** True if S3TC compression is supported. */
    protected static boolean supportsS3TCCompression = false;
    protected static boolean supportsS3TCCompressionDetected = false;

    
    // vear: depth texture handling
    protected static boolean supportsDepthTextures = false;

    protected transient int firstTexture = 0;
    protected transient int lastTexture = 0;

    /**
     * Perspective correction to use for the object rendered with this texture
     * state. Default is CM_PERSPECTIVE.
     */
    private int correction;

    /**
     * offset is used to denote where to begin access of texture coordinates. 0
     * default
     */
    protected int offset = 0;

    protected transient int[] idCache = new int[0];
    
    // unit number used for cyclic reuse of texture units in shaders
    protected int shunit = -1;
    // flag for signaling that the state is used only for shaders, no fixed functionality
    protected boolean shaderOnly = false;

    /**
     * Constructor instantiates a new <code>TextureState</code> object.
     */
    public TextureState() {
        correction = CM_PERSPECTIVE;
        // get our array of texture objects ready.
        texture = new FastList<Texture>();
    }

    /**
     * <code>getType</code> returns this type of render state. (RS_TEXTURE).
     * 
     * @see com.jme.scene.state.RenderState#getType()
     */
    public int getType() {
        return RS_TEXTURE;
    }

    /**
     * <code>setTexture</code> sets a single texture to the first texture
     * unit.
     * 
     * @param texture
     *            the texture to set.
     */
    public void setTexture(Texture texture) {
        if (this.texture.size() == 0) {
            this.texture.add(texture);
        } else {
            this.texture.set(0, texture);
        }
        setNeedsRefresh(true);

        resetFirstLast();
    }

    /**
     * <code>getTexture</code> gets the texture that is assigned to the first
     * texture unit.
     * 
     * @return the texture in the first texture unit.
     */
    public Texture getTexture() {
        if (texture.size() > 0)
            return texture.get(0);
        else return null;
    }

    /**
     * <code>setTexture</code> sets the texture object to be used by the
     * state. The texture unit that this texture uses is set, if the unit is not
     * valid, i.e. less than zero or greater than the number of texture units
     * supported by the graphics card, it is ignored.
     * 
     * @param texture
     *            the texture to be used by the state.
     * @param textureUnit
     *            the texture unit this texture will fill.
     */
    public void setTexture(Texture texture, int textureUnit) {
        if (textureUnit >= 0 && textureUnit < numTotalTexUnits) {
            while (textureUnit >= this.texture.size()) {
                this.texture.add(null);
            }
            this.texture.set(textureUnit, texture);
            resetFirstLast();
        }
        setNeedsRefresh(true);
    }

    /**
     * <code>getTexture</code> retrieves the texture being used by the state
     * in a particular texture unit.
     * 
     * @param textureUnit
     *            the texture unit to retrieve the texture from.
     * @return the texture being used by the state. If the texture unit is
     *         invalid, null is returned.
     */
    public Texture getTexture(int textureUnit) {
        if (textureUnit < texture.size() && textureUnit >= 0) {
            return texture.get(textureUnit);
        }

        return null;
    }

    // returns the unit in which the texture is
    // -1 if the texture is not bound
    public int getUnit( Texture t ) {
        for(int i=lastTexture; i >= firstTexture ; i--) {
            if(texture.get(i) == t)
                return i;
        }
        return -1;
    }
        
    public int addShaderTexture(Texture t) {
        if( shunit <= 0 )
            shunit = numFragmentTexUnits - 1;
        else {
            shunit--;
        }
        setTexture(t, shunit);
        return shunit;
    }
    
    public void setShaderOnly( boolean shaderonly ) {
        if( this.shaderOnly != shaderonly )
            setNeedsRefresh(true);
        this.shaderOnly = shaderonly;
    }
    
    public boolean isShaderOnly() {
        return shaderOnly;
    }
    
    public boolean getShaderOnly( ) {
        return shaderOnly;
    }
    
    public boolean removeTexture(Texture tex) {

        int index = texture.indexOf(tex);
        if (index == -1)
            return false;

        texture.set(index, null);
        idCache[index] = 0;
        return true;
    }

    public boolean removeTexture(int textureUnit) {
        if (textureUnit < 0 || textureUnit >= numTotalTexUnits
                || textureUnit >= texture.size())
            return false;

        Texture t = getTexture(textureUnit);
        if (t == null)
            return false;

        return removeTexture(t);

    }

    /**
     * Removes all textures in this texture state. Does not delete them from the
     * graphics card.
     */
    public void clearTextures() {
        for (int i = texture.size(); --i >= 0; ) {
            removeTexture(i);
        }
    }

    /**
     * <code>setCorrection</code> sets the image correction mode for this
     * texture. If an invalid value is passed, it is set to CM_AFFINE.
     * 
     * @param correction
     *            the correction mode for this texture.
     */
    public void setCorrection(int correction) {
        if (correction < 0 || correction > 2) {
            correction = CM_AFFINE;
        }
        this.correction = correction;
        setNeedsRefresh(true);
    }

    /**
     * <code>getCorrection</code> returns the correction mode for the texture state.
     * 
     * @return the correction mode for the texture state.
     */
    public int getCorrection() {
        return correction;
    }

    /**
     * <code>getTotalNumberOfUnits</code> returns the total number of texture
     * units the computer's graphics card supports.
     * 
     * @return the total number of texture units supported by the graphics card.
     */
    public static int getTotalNumberOfUnits() {
        return numTotalTexUnits;
    }

    /**
     * <code>getNumberOfFixedUnits</code> returns the number of texture units
     * the computer's graphics card supports, for use in the fixed pipeline.
     * 
     * @return the number units.
     */
    public static int getNumberOfFixedUnits() {
        return numFixedTexUnits;
    }

    /**
     * <code>getNumberOfVertexUnits</code> returns the number of texture units
     * available to a vertex shader that this graphics card supports.
     * 
     * @return the number of units.
     */
    public static int getNumberOfVertexUnits() {
        return numVertexTexUnits;
    }

    /**
     * <code>getNumberOfFragmentUnits</code> returns the number of texture units
     * available to a fragment shader that this graphics card supports.
     * 
     * @return the number of units.
     */
    public static int getNumberOfFragmentUnits() {
        return numFragmentTexUnits;
    }

    /**
     * <code>getNumberOfFragmentTexCoordUnits</code> returns the number of
     * texture coordinate sets available that this graphics card supports.
     * 
     * @return the number of units.
     */
    public static int getNumberOfFragmentTexCoordUnits() {
        return numFragmentTexCoordUnits;
    }

    /**
     * <code>getNumberOfTotalUnits</code> returns the number texture units the
     * computer's graphics card supports.
     * 
     * @return the number of units.
     */
    public static int getNumberOfTotalUnits() {
        return numTotalTexUnits;
    }

    /**
     * Returns the number of textures this texture manager is maintaining.
     * 
     * @return the number of textures.
     */
    public int getNumberOfSetTextures() {
        return texture.size();
    }

    /**
     * Fast access for retrieving a Texture ID. A return is guaranteed when
     * <code>textureUnit</code> is any number under or equal to the highest
     * textureunit currently in use. This value can be retrieved with
     * <code>getNumberOfSetTextures</code>. A higher value might result in
     * unexpected behaviour such as an exception being thrown.
     * 
     * @param textureUnit
     *            The texture unit from which to retrieve the ID.
     * @return the textureID, or 0 if there is none.
     */
    public final int getTextureID(int textureUnit) {
        if (textureUnit < idCache.length && textureUnit >= 0) {
            return idCache[textureUnit];
        }

        return 0;
    }

    /**
     * <code>setTextureCoordinateOffset</code> sets the offset value used to
     * determine which coordinates to use for texturing Geometry.
     * 
     * @param offset
     *            the offset (default 0).
     */
    public void setTextureCoordinateOffset(int offset) {
        this.offset = offset;
        setNeedsRefresh(true);
    }

    /**
     * <code>setTextureCoordinateOffset</code> gets the offset value used to
     * determine which coordinates to use for texturing Geometry.
     * 
     * @return the offset (default 0).
     */
    public int getTextureCoordinateOffset() {
        return this.offset;
    }

    /**
     * Loads our textures into the underlying rendering system, generating mip
     * maps if appropriate.
     */
    public void load() {
        for (int unit = 0; unit < numTotalTexUnits; unit++) {
            if (getTexture(unit) != null) {
                load(unit);
            }
        }
    }

    /**
     * Loads the texture for the given unit into the underlying rendering
     * system, generating mip maps if appropriate.
     */
    public void load(int unit) {
        
    }

    /**
     * Removes the texture of the given unit.
     * 
     * @param unit
     *            The unit of the Texture to remove.
     */
    public void delete(int unit) {
        
    }

    /**
     * Removes all Texture set in this TextureState. Does not also remove from
     * TextureManager's cache.
     */
    public void deleteAll() {
        
    }

    /**
     * Removes all Texture set in this TextureState. Also removes the textures
     * from the TextureManager cache if passed boolean is true.
     */
    public void deleteAll(boolean removeFromCache) {
        
    }

    /**
     * Returns the maximum anisotropic filter.
     * 
     * @return The maximum anisotropic filter.
     */
    public float getMaxAnisotropic() {
        return maxAnisotropic;
    }

    /**
     * Updates firstTexture to be the first non-null Texture, and lastTexture to
     * be the last non-null texture.
     */
    protected void resetFirstLast() {
        boolean foundFirst = false;
        firstTexture = 0;
        lastTexture = texture.size() - 1;
        for (int x = 0; x < texture.size(); x++) {
            if (texture.get(x) != null) {
                if (!foundFirst) {
                    firstTexture = x;
                    foundFirst = true;
                }
                lastTexture = x;
            }
        }
        if (idCache == null || idCache.length <= lastTexture) {
            if (idCache == null || idCache.length == 0) {
                idCache = new int[lastTexture + 2];
            } else {
                int[] tempCache = new int[lastTexture + 2];
                System.arraycopy(idCache, 0, tempCache, 0, idCache.length);
                idCache = tempCache;
            }
        }
    }
  
    /**
     * @return true if multi-texturing is supported in fixed function
     */
    public static boolean isMultiTextureSupported() {
        return supportsMultiTexture;
    }

    /**
     * Overide setting of fixed function multi-texturing support.
     * 
     * @param use
     */
    public static void overrideMultiTextureSupport(boolean use) {
        supportsMultiTexture = use;
    }

    /**
     * Reset fixed function multi-texturing support to driver-detected setting.
     */
    public static void resetMultiTextureSupport() {
        supportsMultiTexture = supportsMultiTextureDetected;
    }

    
    /**
     * @return true we support dot3 environment texture settings
     */
    public static boolean isEnvDot3Supported() {
        return supportsEnvDot3;
    }

    /**
     * Overide support for dot3 environment texture settings
     * 
     * @param use
     */
    public static void overrideEnvDot3Support(boolean use) {
        supportsEnvDot3 = use;
    }

    /**
     * Reset dot3 environment texture support to driver-detected setting.
     */
    public static void resetEnvDot3Support() {
        supportsEnvDot3 = supportsEnvDot3Detected;
    }

    
    /**
     * @return true we support combine environment texture settings
     */
    public static boolean isEnvCombineSupported() {
        return supportsEnvCombine;
    }

    /**
     * Overide support for combine environment texture settings
     * 
     * @param use
     */
    public static void overrideEnvCombineSupport(boolean use) {
        supportsEnvCombine = use;
    }

    /**
     * Reset combine environment texture support to driver-detected setting.
     */
    public static void resetEnvCombineSupport() {
        supportsEnvCombine = supportsEnvCombineDetected;
    }
    
    
    /**
     * Returns if S3TC compression is available for textures.
     * 
     * @return true if S3TC is available.
     */
    public static boolean isS3TCSupported() {
        return supportsS3TCCompression;
    }

    /**
     * Overide setting of S3TC compression support.
     * 
     * @param use
     */
    public static void overrideS3TCSupport(boolean use) {
        supportsS3TCCompression = use;
    }

    /**
     * Reset dot3 environment texture support to driver-detected setting.
     */
    public static void resetS3TCSupport() {
        supportsS3TCCompression = supportsS3TCCompressionDetected;
    }
    

    /**
     * @return if Anisotropic texture filtering is supported
     */
    public static boolean isAnisoSupported() {
        return supportsAniso;
    }

    /**
     * Overide setting of support for Anisotropic texture filtering.
     * 
     * @param use
     */
    public static void overrideAnisoSupport(boolean use) {
        supportsAniso = use;
    }

    /**
     * Reset dot3 environment texture support to driver-detected setting.
     */
    public static void resetAnisoSupport() {
        supportsAniso = supportsAnisoDetected;
    }

    
    /**
     * @return true if non pow 2 texture sizes are supported
     */
    public static boolean isNonPowerOfTwoTextureSupported() {
        return supportsNonPowerTwo;
    }

    /**
     * Overide setting of support for non-pow2 texture sizes.
     * 
     * @param use
     */
    public static void overrideNonPowerOfTwoTextureSupport(boolean use) {
        supportsNonPowerTwo = use;
    }

    /**
     * Reset support for non-pow2 texture sizes to driver-detected setting.
     */
    public static void resetNonPowerOfTwoTextureSupport() {
        supportsNonPowerTwo = supportsNonPowerTwoDetected;
    }

    
    /**
     * @return if rectangular texture sizes are supported (width != height)
     */
    public static boolean isRectangularTextureSupported() {
        return supportsRectangular;
    }

    /**
     * Overide auto-detected setting of support for rectangular texture sizes (width != height).
     * 
     * @param use
     */
    public static void overrideRectangularTextureSupport(boolean use) {
        supportsRectangular = use;
    }

    /**
     * Reset support for rectangular texture sizes to driver-detected setting.
     */
    public static void resetRectangularTextureSupport() {
        supportsRectangular = supportsRectangularDetected;
    }

    public static Image getDefaultTextureImage() {
        return defaultTexture != null ? defaultTexture.getImage() : null;
    }

    public static Texture getDefaultTexture() {
        return defaultTexture;
    }

    @Override
    public void update(RenderContext ctx) {
        if( indep != null )
            return;
        super.update(ctx);
        if( needsRefresh ) {
            ((TextureState)impl).enabled = this.enabled;
            ((TextureState)impl).needsRefresh = true;

            ((TextureState)impl).texture.clear();
            if( texture.size() > 0 ) {
                ((TextureState)impl).texture.addAll(texture);
            }
            ((TextureState)impl).resetFirstLast();
            ((TextureState)impl).shaderOnly = this.shaderOnly;
            
            needsRefresh = false;
        }
    }
    
    // vear: depth texture support check function
    /**
     * Returns if hadvare OpenGL shadow textures are supported
     * @return
     */
    public static boolean isDepthTextureSupportex() {
        return supportsDepthTextures;
    }
}