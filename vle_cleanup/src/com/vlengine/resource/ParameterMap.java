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

package com.vlengine.resource;

import java.util.HashMap;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class ParameterMap extends HashMap<String, Object> {
    
    public static final ParameterMap MAP_EMPTY = new ParameterMap();

    public static final ParameterMap NODIRECTBUFFER;
    public static final ParameterMap DIRECTBUFFER;
    
    public static final String KEY_MOD_NAME = "mod_name";
    public static final String KEY_DIRECTBUFFER = "directbuffer";

    static {
        NODIRECTBUFFER = new ParameterMap();
        NODIRECTBUFFER.put(KEY_DIRECTBUFFER, false);
        DIRECTBUFFER = new ParameterMap();
        DIRECTBUFFER.put(KEY_DIRECTBUFFER, true);
    }
    
    public static String getModName(ParameterMap params) {
        return (String) ( params == null ? null : params.get(ParameterMap.KEY_MOD_NAME) );
    }
    
    public static void setModName(ParameterMap params, String mod) {
        params.put(KEY_MOD_NAME, mod);
    }
    
    public Object get(Object key, Object object) {
        Object value = get(key);
        if(value == null )
            return object;
        return value;
    }
    
    public boolean getBoolean( String key, boolean def ) {
        if( get(key) != null ) {
            Boolean dr = (Boolean) get(key);
            if( dr != null ) {
                return dr.booleanValue();
            }
        }
        return def;
    }
    
    public void put( String key, boolean value ) {
        super.put(key, new Boolean(value));
    }
    
    public int getInt( String key, int def ) {
        if( get(key) != null ) {
            Integer dr = (Integer) get(key);
            if( dr != null ) {
                return dr.intValue();
            }
        }
        return def;
    }
    
    public void put( String key, int value ) {
        super.put(key, new Integer(value));
    }
    
    public void put( String key, byte value ) {
        super.put(key, new Byte(value));
    }
    
    public int getByte( String key, int def ) {
        if( get(key) != null ) {
            Byte dr = (Byte) get(key);
            if( dr != null ) {
                return (dr.byteValue() & 0xff );
            }
        }
        return def;
    }
    
    public float getFloat( String key, float def ) {
        if( get(key) != null ) {
            Float dr = (Float) get(key);
            if( dr != null ) {
                return dr.floatValue();
            }
        }
        return def;
    }
    
    public void put( String key, float value ) {
        super.put(key, new Float(value));
    }

}
