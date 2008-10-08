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

import com.vlengine.image.Texture;
import com.vlengine.math.Vector3f;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class TextureKey {

    protected String image_name;
    protected int texture_mag_filter = Texture.FM_LINEAR;
    protected int texture_min_filter = Texture.MM_LINEAR;
    protected float texture_aniso_level = 0.0f;
    protected int texture_wrap = Texture.WM_WRAP_S_WRAP_T;
    protected int texture_apply = Texture.AM_MODULATE;
    protected Vector3f texture_translation;
    protected Vector3f texture_scale;
    
    public TextureKey(String name, ParameterMap parameters) {
        this.image_name = name;
        if(parameters!=null) {
            texture_mag_filter = parameters.getInt("texture_mag_filter", Texture.FM_LINEAR);
            texture_min_filter = parameters.getInt("texture_min_filter", Texture.MM_LINEAR_LINEAR);
            texture_aniso_level = parameters.getFloat("texture_aniso_level", 0.0f);
            texture_wrap = parameters.getInt("texture_wrap", Texture.WM_WRAP_S_WRAP_T);
            texture_apply = parameters.getInt("texture_apply", Texture.AM_MODULATE);
            texture_translation = (Vector3f) parameters.get("texture_translation");
            texture_scale = (Vector3f) parameters.get("texture_scale");
        }
    }

    public float getTexture_aniso_level() {
        return texture_aniso_level;
    }

    public void setTexture_aniso_level(float texture_aniso_level) {
        this.texture_aniso_level = texture_aniso_level;
    }

    public int getTexture_apply() {
        return texture_apply;
    }

    public void setTexture_apply(int texture_apply) {
        this.texture_apply = texture_apply;
    }

    public int getTexture_mag_filter() {
        return texture_mag_filter;
    }

    public void setTexture_mag_filter(int texture_mag_filter) {
        this.texture_mag_filter = texture_mag_filter;
    }

    public int getTexture_min_filter() {
        return texture_min_filter;
    }

    public void setTexture_min_filter(int texture_min_filter) {
        this.texture_min_filter = texture_min_filter;
    }

    public Vector3f getTexture_scale() {
        return texture_scale;
    }

    public void setTexture_scale(Vector3f texture_scale) {
        this.texture_scale = texture_scale;
    }

    public Vector3f getTexture_translation() {
        return texture_translation;
    }

    public void setTexture_translation(Vector3f texture_translation) {
        this.texture_translation = texture_translation;
    }

    public int getTexture_wrap() {
        return texture_wrap;
    }

    public void setTexture_wrap(int texture_wrap) {
        this.texture_wrap = texture_wrap;
    }
    

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TextureKey other = (TextureKey) obj;
        if (!this.image_name.equals(other.image_name)) {
            return false;
        }
        if (this.texture_mag_filter != other.texture_mag_filter) {
            return false;
        }
        if (this.texture_min_filter != other.texture_min_filter) {
            return false;
        }
        if (this.texture_aniso_level != other.texture_aniso_level) {
            return false;
        }
        if (this.texture_wrap != other.texture_wrap) {
            return false;
        }
        if (this.texture_apply != other.texture_apply) {
            return false;
        }
        if (this.texture_translation != other.texture_translation && (this.texture_translation == null 
                || this.texture_translation.x != other.texture_translation.x
                || this.texture_translation.y != other.texture_translation.y
                || this.texture_translation.z != other.texture_translation.z
                )) {
            return false;
        }
        if (this.texture_scale != other.texture_scale && (this.texture_scale == null 
                || this.texture_scale.equals(other.texture_scale)
                )) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (this.image_name != null ? this.image_name.hashCode() : 0);
        hash = 79 * hash + this.texture_mag_filter;
        hash = 79 * hash + this.texture_min_filter;
        hash = 79 * hash + Float.floatToIntBits(this.texture_aniso_level);
        hash = 79 * hash + this.texture_wrap;
        hash = 79 * hash + this.texture_apply;
        hash = 79 * hash + (this.texture_translation != null ? this.texture_translation.hashCode() : 0);
        hash = 79 * hash + (this.texture_scale != null ? this.texture_scale.hashCode() : 0);
        return hash;
    }
}
