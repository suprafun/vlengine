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

import com.vlengine.light.Light;
import com.vlengine.scene.Renderable;
import com.vlengine.scene.state.LightState;

/**
 * Class holding signature data about a shader
 * @author vear (Arpad Vekas)
 */
public class ShaderKey {
    
    public static final boolean LIGHTSORTER_SHADOWS = false;
            
    // applies to set shaders only, no effect on fixed functionality

    public boolean depthOnly;
    
    
    public boolean colorMap0;
    public boolean colorMap0Scale = false;
    // do we use the vertex normal for lighting
    // do we use normal map for lighting
    //  0- dont use normals (no lighting)
    //  1- vertex normal    (requires normal vertex attribute)
    //  2- normal map       (requires normalMap
    //  3- bump map         (requires bumpMap, and normal, tangent vertex attributes
    public static enum NormalType {
        No(0),
        Vertex(1),
        NormalMap(2),
        // one slot open, if more is needed, bitmasks should be corrected
        ;
        public final int value;
        NormalType(int val) {
            value = val;
        }
    }
    // maybe we need this
    //public boolean normalMapScale = false;
    
    public static final NormalType NORMALTYPE_NO = NormalType.No;
    public static final NormalType NORMALTYPE_VERTEX = NormalType.Vertex;
    public static final NormalType NORMALTYPE_MAP = NormalType.NormalMap;
    public NormalType normalType;
    
    public boolean bumpMap = false;
    // maybe we need this
    //public boolean bumpMapScale = false;

    // the number of bones with bone animation (0- no bone animation)
    // max 255
    public int numBones = 0;

    // do we use lightindex rendering?
    // needs: lightMap, lightDepthMap, lightIndexMap
    // if set, no light0 or light1 is used
    boolean lightIndex;

    // do we have forward ligting
    public boolean forwardLigting;

    // light 0, only usable is some type of normal is provided
    // 0- inactive
    // 1- directional                (requires gl_LightSource[0])
    // 2- point
    // 3- spot
    public int [] light = new int[Renderable.LOWPROFILE_LIGHTS]; 
    // does light have shadow?     (requires shadowMap0
    public boolean[] shadow = new boolean[Renderable.LOWPROFILE_LIGHTS];
    // does light hhave attenuation
    public boolean[] attenuate = new boolean[Renderable.LOWPROFILE_LIGHTS];

    // global settings, these do not change between shaders
    // do we not handle specular
    public boolean nospecular = false;
    
    // do we have depth fog
    public boolean screendepthfog = false;

    public void clear() {
        depthOnly = false;
        colorMap0 = false;
        colorMap0Scale = false;
        normalType = NormalType.No;
        bumpMap = false;
        numBones = 0;
        lightIndex = false;
        forwardLigting = false;
        for(int i=0; i<Renderable.LOWPROFILE_LIGHTS; i++) {
            light[i] = 0;
            shadow[i] = false;
            attenuate[i] = false;
        }
        nospecular = false;
        screendepthfog = false;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ShaderKey other = (ShaderKey) obj;
        if (this.depthOnly != other.depthOnly) {
            return false;
        }
        if (this.forwardLigting != other.forwardLigting) {
            return false;
        }
        if (this.colorMap0 != other.colorMap0) {
            return false;
        }
        if(this.colorMap0Scale != other.colorMap0Scale) {
            return false;
        }
        if (this.normalType != other.normalType) {
            return false;
        }
        if (this.bumpMap != other.bumpMap) {
            return false;
        }
        if (this.numBones != other.numBones) {
            return false;
        }
        if (this.lightIndex != other.lightIndex) {
            return false;
        }
        for(int i=0; i<Renderable.LOWPROFILE_LIGHTS; i++) {
            if(light[i]!=other.light[i]) {
                return false;
            }
            // dont compare shadow for now
            if(LIGHTSORTER_SHADOWS) {
                if (shadow[i] != other.shadow[i]) {
                    return false;
                }
            }
            if (attenuate[i] != other.attenuate[i]) {
                return false;
            }
        }
                
        return true;
    }

    /**
     * The hashcode is unique
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 0;
        hash = hash | (this.depthOnly ? 1 : 0);
        hash = hash<<1 | (this.forwardLigting ? 1 : 0);
        hash = hash<<1 | (this.colorMap0 ? 1 : 0);
        hash = hash<<1 | (this.colorMap0Scale ? 1 : 0);
        hash = hash<<2 | this.normalType.value;
        hash = hash<<1 | (this.bumpMap ? 1 : 0);
        hash = hash<<8 | this.numBones;
        hash = hash<<1 | (this.lightIndex ? 1 : 0);
        for(int i=0; i<Renderable.LOWPROFILE_LIGHTS; i++) {
            hash = hash<<2 | light[i];
            if(LIGHTSORTER_SHADOWS)
                hash = hash<<1 | (shadow[i] ? 1 : 0);
            hash = hash<<1 | (attenuate[i] ? 1 : 0);
        }
        return hash;
    }

    public static int getForwardLightingFlags(boolean depth, boolean forwardLight, boolean indexLight, LightState ls ) {
        
        int flag = (depth ? 1 : 0);
        flag = flag<<1 | (forwardLight ? 1 : 0);
        flag<<=1; // colorMap0
        flag<<=1; // colorMap0Scale
        flag<<=2; // normalType
        flag<<=1; // bumpMap
        flag<<=8; // numbones
        flag = (flag<<1) | (indexLight ? 1 : 0);
        for(int i=0; i<Renderable.LOWPROFILE_LIGHTS; i++) {
            if(ls!=null && i<ls.getQuantity()) {
                Light l = ls.get(i);
                flag = flag<<2 | (l.getType()+1);
                if(LIGHTSORTER_SHADOWS) {
                    flag = (flag <<1) | (l.isShadowCaster()?1:0);
                }
                flag = (flag<<1) | (l.isAttenuate() ? 1 : 0);
            } else {
                flag<<=2;
                if(LIGHTSORTER_SHADOWS) {
                    flag<<=1;
                }
                flag<<=1;
            }
        }
        return flag;
    }

    public int getLightingFlags() {
        int flag = (this.depthOnly ? 1 : 0);
        flag = flag<<1 | (this.forwardLigting ? 1 : 0);
        flag<<=1; // colorMap0
        flag<<=1; // colorMap0Scale
        flag<<=2; // normalType
        flag<<=1; // bumpMap
        flag<<=8; // numbones
        flag = flag<<1 | (this.lightIndex ? 1 : 0);
        for(int i=0; i<Renderable.LOWPROFILE_LIGHTS; i++) {
            flag = flag<<2 | light[i];
            if(LIGHTSORTER_SHADOWS)
                flag = flag<<1 | (shadow[i] ? 1 : 0);
            flag = flag<<1 | (attenuate[i] ? 1 : 0);
        }
        return flag;
    }

    /**
     * Set the values in this key by the values of the id got
     * @param id
     */
    public void setById(int flag) {
        for(int i=Renderable.LOWPROFILE_LIGHTS-1; i>=0 && flag!=0; i--) {
            if((flag&1)!=0) {
                attenuate[i] = true;
            }
            flag>>=1;
            if(LIGHTSORTER_SHADOWS) {
                if((flag&1)!=0) {
                    shadow[i] = true;
                }
                flag>>=1;
            }
            // extract light type
            light[i] = (flag&3);
            flag>>=2;
        }
    }

    public boolean isLightConsistent() {
        // when we find 0 light, we should not find any more non 0 light
        boolean found0 = false;
        for(int i=0; i<Renderable.LOWPROFILE_LIGHTS; i++) {
            if(light[i]==0) {
                found0 = true;
                if(shadow[i] || attenuate[i]) {
                    return false;
                }
            } else if(found0) {
                // found a non 0 light after a 0 light
                return false;
            } else if(light[i]==1 && attenuate[i]) {
                // no attenuation on direction lights
                return false;
            }
        }
        return true;
    }
    
    @Override
    public ShaderKey clone() {
        ShaderKey copy = new ShaderKey();
        copy.depthOnly = depthOnly;
        copy.forwardLigting = forwardLigting;
        copy.colorMap0 = colorMap0;
        copy.colorMap0Scale = colorMap0Scale;
        copy.normalType = normalType;
        copy.bumpMap = bumpMap;
        copy.numBones = numBones;
        copy.lightIndex = lightIndex;
        for(int i=0; i<Renderable.LOWPROFILE_LIGHTS; i++) {
            copy.light[i] = light[i];
            copy.shadow[i] = shadow[i];
            copy.attenuate[i] = attenuate[i];
        }
        copy.nospecular = nospecular;
        copy.screendepthfog = screendepthfog;
        return copy;
    }

    public int getLightCount() {
        int lc = 0;
        for(int i=0; i<Renderable.LOWPROFILE_LIGHTS; i++) {
            if(light[i]==0)
                return lc;
            lc++;
        }
        return lc;
    }

    public int getBumpLightCount() {
        return Math.min(getLightCount(), 2);
    }
    
    public boolean hasLight() {
        for(int i=0; i<Renderable.LOWPROFILE_LIGHTS; i++) {
            if(light[i]!=0)
                return true;
        }
        return false;
    }
    
    public boolean hasPointOrSpotLight() {
        for(int i=0; i<Renderable.LOWPROFILE_LIGHTS; i++) {
            if(light[i]>1)
                return true;
        }
        return false;
    }
    
    public boolean hasDirectionaLight() {
        for(int i=0; i<Renderable.LOWPROFILE_LIGHTS; i++) {
            if(light[i]==1)
                return true;
        }
        return false;
    }
    
    public boolean hasSpotLight() {
        for(int i=0; i<Renderable.LOWPROFILE_LIGHTS; i++) {
            if(light[i]==3)
                return true;
        }
        return false;
    }
    
    public boolean hasAttenuate() {
        for(int i=0; i<Renderable.LOWPROFILE_LIGHTS; i++) {
            if(attenuate[i])
                return true;
        }
        return false;
    }
}
