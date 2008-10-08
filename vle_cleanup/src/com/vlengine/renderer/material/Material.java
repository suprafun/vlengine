/*
 * Copyright (c) 2008 VL Engine
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

package com.vlengine.renderer.material;

import com.vlengine.app.frame.Frame;
import com.vlengine.light.Light;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.CullContext;
import com.vlengine.scene.Renderable;
import com.vlengine.scene.state.AlphaBlendState;
import com.vlengine.scene.state.AlphaTestState;
import com.vlengine.scene.state.LightState;
import com.vlengine.scene.state.RenderState;
import com.vlengine.scene.state.ShaderObjectsState;
import com.vlengine.scene.state.ShaderParameters;
import com.vlengine.scene.state.TextureState;
import com.vlengine.util.FastList;
import com.vlengine.util.IntMap;

/**
 * A material (rendering state) of an object in a pass
 * 
 * @author vear (Arpad Vekas)
 */
public class Material {

    //protected int matUsage = USAGEFLAG_UNLIT;

    // material needs update ( get states from parents )
    protected boolean needupdate = true;
    
    // material needs refresh ( the attributes of renderstates need to be transfered to
    // implementing states
    protected boolean needrefresh = true;
     
    // the renderqueue mode for this material, may be used at create time
    // as a help for deciding the Renderable renderQueueMode
    // but at rendertime, the Renderable renderQueueMode decides
    long renderQueueMode = RenderQueue.QueueFilter.Opaque.value;
    
    /**
     * The compiled list of renderstates for this geometry, taking into account
     * ancestors' states - updated during cull
     */
    //public RenderState[] states;
    
    /** The render states of this material. */
    // classified by usage flags set
    public RenderState[] states;
    // shader renderstates classified by usage
    protected IntMap shaderStateMap;
    protected FastList<ShaderObjectsState> shaderStateList;
    
    //protected int matNum = -1;
    
    protected ColorRGBA defaultColor = new ColorRGBA(ColorRGBA.white);
    
    protected int lightCombineMode = LightState.INHERIT;

    public Material() {}

    /*
    public void setMatNum(int matn) {
        matNum = matn;
    }
     */
    
    public boolean isNeedUpdate() {
        return needupdate;
    }

    public void setNeedupdate( boolean needupdate ) {
        this.needupdate = needupdate;
    }
    
    public void setRenderQueueMode(long renderQueueMode) {
        this.renderQueueMode = renderQueueMode;
    }
    
    public long getRenderQueueMode() {
        return renderQueueMode;
    }
    
    /**
     * <code>getDefaultColor</code> returns the color used if no per vertex
     * colors are specified.
     * 
     * @return default color
     */
    public ColorRGBA getDefaultColor() {
        return defaultColor;
    }

    /**
     * <code>setDefaultColor</code> sets the color to be used if no per vertex
     * color buffer is set.
     * 
     * @param color
     */
    public void setDefaultColor(ColorRGBA color) {
        defaultColor = color;
    }

    // TODO: this needs to be changed so that
    // it updates data gathering methods
    public void update( CullContext ctx ) {
        //updateStates( ctx );
        
    }
    
    public boolean isNeedRefresh() {
        return needrefresh;
    }
    
    /*
    public void updateStates(CullContext ctx ) {
        RenderState[] parentstates = ctx!=null ? ctx.getRenderStateList(matNum) : null;
        
        if( parentstates == null && renderStateList == null)
            return;
        
        if( states == null)
            states = new RenderState[RenderState.RS_MAX_STATE];
        
        if( parentstates == null )
            System.arraycopy(renderStateList, 0, states, 0, RenderState.RS_MAX_STATE);
        else if( renderStateList == null )
            System.arraycopy(parentstates, 0, states, 0, RenderState.RS_MAX_STATE);
        else {
            for (int i = 0; i < RenderState.RS_MAX_STATE; i++) {
                states[i] = renderStateList[i] != null ? renderStateList[i] : parentstates[i];
            }
        }

        // combine lights
        if(this.lightCombineMode == LightState.OFF)
            states[RenderState.RS_LIGHT] = null;
        // TODO: combine shader parameters?
        
    }
     */
    
    /*
    public RenderState[] getCurrentStates() {
        return states;
    }
     */
    
    
    public RenderState setRenderState( RenderState rs ) {
        if ( rs == null ) {
            return null;
        }

        needupdate = true;
        needrefresh = true;
        
        if ( states == null ) {
            states  = new RenderState[RenderState.RS_MAX_STATE];
        }

        RenderState oldState = states[ rs.getType() ];
        states[ rs.getType() ] = rs;
        return oldState;
    }
    
    public RenderState getRenderState(int type) {
        return states != null ? states[type] : null;
    }
    
    public RenderState setShaderState( ShaderObjectsState rs ) {
        if ( rs == null ) {
            return null;
        }

        needupdate = true;
        needrefresh = true;
        
        int usage = rs.getShaderKey().getLightingFlags();
        /*
        if(usage == 0) {
            // set it into main renderstate list
            return setRenderState(rs);
        }
         */
        
        if ( shaderStateMap == null ) {
            shaderStateMap  = new IntMap();
        }
        
        if ( shaderStateList == null ) {
            shaderStateList  = new FastList<ShaderObjectsState>();
        }

        ShaderObjectsState oldState = (ShaderObjectsState) shaderStateMap.get(usage);
        if(oldState!=null && oldState!=rs) {
            // remove it from the list
            shaderStateList.remove(oldState);
        }
        if(oldState!=rs) {
            shaderStateMap.put(usage, rs);
            shaderStateList.add(rs);
        }
        return oldState;
    }
    
    /**
     * Returns the requested RenderState that this Spatial currently has set or
     * null if none is set.
     * 
     * @param type
     *            the renderstate type to retrieve
     * @return a renderstate at the given position or null
     */
    public ShaderObjectsState getShaderState(int usageflags) {
        if(shaderStateList==null)
            return null;
        return (ShaderObjectsState) shaderStateList.get(usageflags);
    }

    
    public void prepare( RenderContext ctx ) {
        if(states!=null) {
            if( states !=null ) {
                for ( int i = 0; i < states.length; i++ ) {
                    if( states[i]!=null && states[i].isNeedRefresh() ) {
                        states[i].update( ctx );
                    }
                }
            }
        }
        // update the shaders too
        if(shaderStateList != null) {
            for(int i=0, mi=shaderStateList.size(); i<mi; i++) {
                ShaderObjectsState st = shaderStateList.get(i);
                if(st.isNeedRefresh()) {
                    st.update( ctx );
                }
            }
        }
        // mark that we dont need any more update
        needrefresh = false;
    }
    
    public void apply( RenderContext ctx ) {
        ctx.currentMaterial = this;
        ctx.currentMaterialDepthOnly = ctx.positionmode;
        RenderState[] s = states != null ? states : ctx.defaultStateList;
        
        if(!ctx.positionmode) {
            // apply the shader at first
            boolean shaderApplyed = false;
            
            // do we apply a lightstate
            if(s[RenderState.RS_LIGHT]!=null
                && this.lightCombineMode != LightState.OFF
                && shaderStateMap!=null 
                && ctx.enforcedStateList[RenderState.RS_GLSL_SHADER_OBJECTS]==null
                    ) {
                // try to find proper shader
                int materialNo = ShaderKey.getForwardLightingFlags(false,
                        true,
                        false,
                        (LightState) s[RenderState.RS_LIGHT]);
                if(materialNo!=0) {
                    ShaderObjectsState st = (ShaderObjectsState) shaderStateMap.get(materialNo);
                    if(st!=null) {
                        // apply proper shader
                        st.apply(ctx);
                        shaderApplyed = true;
                    }
                }
            }
            
            // do we apply the unlit shader
            if(!shaderApplyed
                    && ctx.app.conf.lightmode > 0
                    && this.lightCombineMode != LightState.OFF
                    && ctx.currentPassShaderFlags==0
                    && shaderStateMap!=null 
                    && ctx.enforcedStateList[RenderState.RS_GLSL_SHADER_OBJECTS]==null
                    ) {
                int materialNo = ShaderKey.getForwardLightingFlags(false,
                        true,
                        false,
                        null);
                ShaderObjectsState st = (ShaderObjectsState) shaderStateMap.get(materialNo);
                if(st!=null) {
                    // apply proper shader
                    st.apply(ctx);
                    shaderApplyed = true;
                }
            }

            if(!shaderApplyed
                    && ctx.currentPassShaderFlags!=0 
                    && shaderStateMap!=null 
                    && ctx.enforcedStateList[RenderState.RS_GLSL_SHADER_OBJECTS]==null) {
                // try to apply the proper shader
                ShaderObjectsState st = (ShaderObjectsState) shaderStateMap.get(ctx.currentPassShaderFlags);
                if(st!=null) {
                    // apply proper shader
                    st.apply(ctx);
                    shaderApplyed = true;
                }
            }

            RenderState tempState = null;
            for ( int i = 0; i < s.length; i++ ) {
                // dont apply shader if specific version was already applyed
                if(shaderApplyed && i==RenderState.RS_GLSL_SHADER_OBJECTS)
                    continue;
                
                tempState = ctx.enforcedStateList[i] != null ? ctx.enforcedStateList[i] : s[i];
                
                if( tempState == null )
                    tempState =  ctx.defaultStateList[i];
                if ( tempState != null ) {
                    if ( tempState != ctx.currentStates[i] 
                            || (i == RenderState.RS_GLSL_SHADER_PARAM && ((ShaderParameters)tempState).getProgramID() != ctx.currentProgramid)) {
                        tempState.apply(ctx);
                    }
                }
            }
        } else {
            // fast-path to apply only things that affect position only mode
            // is there and alpha-test state?
            if(states!=null) {
                // alpha texted textures are also affecting depth
                if(ctx.enforcedStateList[RenderState.RS_ALPHATEST]==null) {
                    AlphaTestState as = (AlphaTestState) states[RenderState.RS_ALPHATEST];
                    // if alpha-testing is enabled
                    if(as != null && as.isEnabled()) {
                        as.apply(ctx);
                        // check for the texture state too
                        TextureState ts = (TextureState) states[RenderState.RS_TEXTURE];
                        if(ts != null) 
                            ts.apply(ctx);
                    }
                }
                // do we got a special shader
                if(shaderStateMap!=null) {
                    int materialNo = ShaderKey.getForwardLightingFlags(true,
                        false,
                        false,
                        null);
                    ShaderObjectsState st = (ShaderObjectsState) shaderStateMap.get(materialNo);
                    if(st!=null) {
                        // apply proper shader
                        st.apply(ctx);
                        // get the shader parameters too
                        ShaderParameters sp = (ShaderParameters) states[RenderState.RS_GLSL_SHADER_PARAM];
                        if(sp!=null) {
                            sp.apply(ctx);
                        }
                    }
                }
            }
        }
    }
    
    public void setLightCombineMode(int lightCombineMode) {
        this.lightCombineMode = lightCombineMode;
    }

    /**
     * @return the lightCombineMode set on this Spatial
     */
    public int getLightCombineMode() {
        return lightCombineMode;
    }
        
    /**
     * Specify how this material should be used by the engine:
     * USAGE_DEPTHONLY  this material does not provide (or is ignored if it does)
     *                  any color information, it is used by depth determining passes
     *                  if an object uses vertex animating shaders, then the version of
     *                  the animating shader, with no color texture lookup, and no color 
     *                  output should be set in this material
     * USAGE_AMBIENT    this material is used in passes that render using ambient color
     *                  which is not affected by any direct lights, lights will be
     *                  ignored if they are set. when using shaders, the version of the
     *                  shader should be used, which does not rely on lights
     * USAGE_LIT        this material is used by the lighing passes, with no shadowing.
     *                  one light is passed to the shader
     * USAGE_LITSHADOWED    this material is used when lighting the object with shadowmap
     *                      present. one light is passed, and one shadowmap is bound.
     *                      one texture unit should be left unoccupied for the shadow map.
     *                      when using shaders, the shadowMap shader texture is filled
     *                      with the shadowmap texture and the shadowMatrix is filled
     *                      with the perspective transform of the shadowmap. when using
     *                      shaders, a shader with depth texture check should be used here
     * @param usage
     */
    /*
    public void setMaterialUsage(int usage) {
        this.matUsage = usage;
    }
    
    public int getMaterialUsage() {
        return matUsage;
    }
     */
}
