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

package com.vlengine.scene;

import com.vlengine.app.MainGame;
import com.vlengine.image.Image;
import com.vlengine.image.Texture;
import com.vlengine.intersection.PickResults;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.material.Material;
import com.vlengine.scene.batch.TextBatch;
import com.vlengine.scene.state.AlphaBlendState;
import com.vlengine.scene.state.TextureState;
import com.vlengine.util.TextureManager;

/**
 * 
 * <code>Text</code> allows text to be displayed on the screen. The
 * renderstate of this Geometry must be a valid font texture.
 * TODO: make this class update/render safe
 * 
 * @author Mark Powell
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public class Text extends RenderSpatial {

    /**
     * Sets the style of the font to normal.
     */
    public static final int NORMAL = 0;

    /**
     * Sets the style of the font to italics.
     */
    public static final int ITALICS = 1;
    
    // the usable font types
    public static final int FONT_DEFAULT = 0;
    
    private StringBuffer text;

    private ColorRGBA textColor = new ColorRGBA();
    
    //private int font = 0;
    
    //private TextBatch batch;
    
    public Text() {
        this("text", "");
    }
    
    /**
     * Creates a texture object that starts with the given text.
     * 
     * @see com.jme.util.TextureManager
     * @param name
     *            the name of the scene element. This is required for
     *            identification and comparision purposes.
     * @param text
     *            The text to show.
     */
    public Text(String name, String text) {
        super(name);        
        setCullMode(Spatial.CullMode.NEVER);
        this.text = new StringBuffer(text);
        setRenderQueueMode(RenderQueue.QueueFilter.Ortho.value);
        /*
        childTransforms = new Transform[Frame.MAX_FRAMES];
        for(int i=0; i<childTransforms.length; i++)
            childTransforms[i] = new Transform();
         */
        batch = new TextBatch();
        batch.setParent(this);
        batch.setWorldTransform(childTransforms);
    }

/*
    @Override
    public void updateWorldVectors(UpdateContext ctx) {
        super.updateWorldVectors(ctx);
        
        // copy transforms to those used in child Batches
        Transform t = childTransforms[ctx.frameId];
        t.getRotation().set(worldRotation);
        t.getScale().set(worldScale);
        t.getTranslation().set(worldTranslation);
    }
 */
    
    /**
     * 
     * <code>print</code> sets the text to be rendered on the next render
     * pass.
     * 
     * @param text
     *            the text to display.
     */
    public void print(String text) {
        this.text.replace(0, this.text.length(), text);
        batch.setNeedUpdate(true);
    }

    /**
     * Sets the text to be rendered on the next render. This function is a more
     * efficient version of print(String).
     * 
     * @param text
     *            The text to display.
     */
    public void print(StringBuffer text) {
        this.text.setLength(0);
        this.text.append(text);
        batch.setNeedUpdate(true);
    }

    /**
     * 
     * <code>getText</code> retrieves the text string of this
     * <code>Text</code> object.
     * 
     * @return the text string of this object.
     */
    public StringBuffer getText() {
        return text;
    }

    /**
     * Sets the color of the text.
     * 
     * @param color
     *            Color to set.
     */
    public void setTextColor(ColorRGBA color) {
    	textColor = color;
        batch.setNeedUpdate(true);
    }

    /**
     * Returns the current text color.
     * 
     * @return Current text color.
     */
    public ColorRGBA getTextColor() {
        return textColor;
    }

    public float getWidth() {
        float rVal = 10f * text.length() * worldScale.x;
        return rVal;
    }

    public float getHeight() {
        float rVal = 16f * worldScale.y;
        return rVal;
    }

    /**
     * @return a Text with {@link #DEFAULT_FONT} and correct alpha state
     * @param name name of the spatial
     */
    public static Text createDefaultTextLabel( String name ) {
        return createDefaultTextLabel( name, "" );
    }

    /**
     * @return a Text with {@link #DEFAULT_FONT} and correct alpha state
     * @param name name of the spatial
     */
    public static Text createDefaultTextLabel( String name, String initialText ) {
        Text text = new Text( name, initialText );
        text.setCullMode( SceneElement.CullMode.NEVER );
        Material mat = new Material();
        // we render text unlit
        mat.setRenderState( getDefaultFontTextureState() );
        mat.setRenderState( getFontAlpha() );
        text.getBatch().setMaterial(mat);
        return text;
    }

    /*
    * @return an alpha state for doing alpha transparency
    */
    private static AlphaBlendState getFontAlpha() {
        AlphaBlendState as1 = new AlphaBlendState();
        as1.setSourceFunction( AlphaBlendState.SB_SRC_ALPHA );
        as1.setDestinationFunction( AlphaBlendState.DB_ONE_MINUS_SRC_ALPHA );
        return as1;
    }

    /**
     * texture state for the default font.
     */
    private static TextureState defaultFontTextureState;

    public static final void resetFontTexture() {
    	if (defaultFontTextureState != null) {
    		defaultFontTextureState.deleteAll(true);
    	}
        defaultFontTextureState = null;
    }
    
    /**
     * A default font cantained in the jME library.
     */
    public static final String DEFAULT_FONT = "com/vlengine/data/defaultfont.tga";
    public static final String DEFAULT_FONT_FILE = "defaultfont.tga";
    


    /**
     * Creates the texture state if not created before.
     * @return texture state for the default font
     */
    private static TextureState getDefaultFontTextureState() {
        if ( defaultFontTextureState == null ) {
            defaultFontTextureState = new TextureState();
            defaultFontTextureState.setTexture( TextureManager.loadTexture( MainGame.class
                    .getClassLoader().getResource( DEFAULT_FONT ), Texture.MM_LINEAR_LINEAR,
                    Texture.FM_LINEAR, Image.GUESS_FORMAT_NO_S3TC, 1.0f, true ) );
            defaultFontTextureState.setEnabled( true );
        }
        return defaultFontTextureState;
    }

    public void findPick(PickResults results) {
        return;
    }

    public void updateWorldBound() {
        return;
    }
/*    
    @Override
    public boolean queue( CullContext ctx) {
        if( super.queue(ctx) ) {
            return batch.queue(ctx);
        }
        return false;
    }
 */

/*    
    public TextBatch getBatch() {
        return batch;
    }
 */
}