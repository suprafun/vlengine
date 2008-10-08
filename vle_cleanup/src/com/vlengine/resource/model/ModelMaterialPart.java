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

package com.vlengine.resource.model;

import com.vlengine.image.Texture;
import com.vlengine.math.Vector3f;
import com.vlengine.util.xml.Element;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class ModelMaterialPart {
    
    // name of the texture
    protected String textureName;
    protected Vector3f translation;
    protected Vector3f scale;
    // type: 0-diffuse, 1-bump, 2-normal map
    public static enum TextureType {
        Diffuse(0, "DIFFUSE"),
        BumpMap(1, "BUMPMAP"),
        NormalMap(2, "NORMALMAP")
        ;
        int value;
        TextureType(int val, String name) {
            value = val;
        }
        
        static TextureType getByValue(int value) {
            for(TextureType t:TextureType.values()) {
                if(t.value==value)
                    return t;
            }
            return Diffuse;
        }
    }
    protected TextureType type;
    
    // the texture ready for runtime use
    protected Texture tex;
    
    public void save(Element parent) {
        parent.addContent(new Element("name").setText(textureName));
        parent.addContent(new Element("type").setText(type.value));
        float[] vec = new float[3];
        if(translation!=null) {
            Element te = new Element("translation");
            parent.addContent(te);
            te.setText(translation.toArray(vec));
        }
        if(scale!=null) {
            Element se = new Element("scale");
            parent.addContent(se);
            se.setText(scale.toArray(vec));
        }
    }

    public void load(Element parent) {
        textureName = parent.getChildText("name");
        type = TextureType.getByValue(parent.getChildint("type"));
        float[] vec = new float[3];
        float[] vect = parent.getChildfloat("translation", vec);
        if ( vect != null ) {
            translation = new Vector3f().set(vect);
        }
        vect = parent.getChildfloat("scale", vec);
        if ( vect != null ) {
            setScale(new Vector3f().set(vect));
        }

    }

    public void setTextureName(String textureName) {
        this.textureName = textureName;
        // TODO: determine type by the file name
        // starts with clr_
        // ends with _COLOR
        // etc..
    }

    public String getTextureName() {
        return textureName;
    }

    public void setTranslation(Vector3f translation) {
        this.translation = translation;
    }

    public void setScale(Vector3f scale) {
        this.scale = scale;
    }

    public void setType(TextureType type) {
        this.type = type;
    }
}
