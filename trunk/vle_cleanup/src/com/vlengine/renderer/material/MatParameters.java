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

import java.util.EnumMap;


/**
 *
 * @author vear (Arpad Vekas)
 */
public class MatParameters  {
    
    public static enum ParamKey {
        Diffuse0Map("DIFFUSE0"),
        Diffuse1Map("DIFFUSE1"),
        NormalMap("NORMAL"),
        BumpMap("BUMP"),
        AlphaTest("ALPHATEST"),
        AlphaFunc("ALPHAFUNC"),
        AlphaBlend("ALPHABLEND"),
        Nocull("NOCULL"),
        AnimFrames("ANIM_FRAMES"),
        Bones("BONES"),
        Dissolve("DISSOLVE"),
        Ambient("AMBIENT"),
        Diffuse("DIFFUSE"),
        Specular("SPECULAR"),
        Shininess("SHININESS"),
        Transmissive("TRANSMISSIVE"),
        Emissive("EMISSIVE"),
        Nospecular("NOSPECULAR"),
        ScreenDepthFog("SCREENDEPTHFOG"),
        PerPixel("PERPIXEL"),
        NoTNB("NOTNB")
        ;
        String name;
        ParamKey(String nme) {
            name = nme;
        }
        
        public String getName() {
            return name;
        }
    }
    
    EnumMap<ParamKey,Object> map;
            
    public MatParameters() {
        map = new EnumMap<ParamKey,Object>(ParamKey.class);
    }

    public void clear() {
        map.clear();
    }

    public float getFloat(ParamKey key, float def) {
        if( map.get(key) != null ) {
            Float dr = (Float) map.get(key);
            if( dr != null ) {
                return dr.floatValue();
            }
        }
        return def;
    }
    
    public int getInt( ParamKey key, int def ) {
        if( map.get(key) != null ) {
            Integer dr = (Integer) map.get(key);
            if( dr != null ) {
                return dr.intValue();
            }
        }
        return def;
    }
    
    public boolean containsKey(ParamKey key) {
        return map.containsKey(key);
    }
    
    public boolean getBoolean( ParamKey key, boolean def ) {
        if( map.get(key) != null ) {
            Boolean dr = (Boolean) map.get(key);
            if( dr != null ) {
                return dr.booleanValue();
            }
        }
        return def;
    }
    
    public Object get(ParamKey key) {
        return map.get(key);
    }

    public Object get(ParamKey key, Object def) {
        Object val = map.get(key);
        return val!=null?val:def;
    }
    
    public void put(ParamKey key, Object val) {
        map.put(key, val);
    }

    public void putAll(MatParameters params) {
        map.putAll(params.map);
    }
}
