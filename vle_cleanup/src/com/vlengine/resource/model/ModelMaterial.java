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

import com.vlengine.app.AppContext;
import com.vlengine.image.Texture;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.material.MatParameters;
import com.vlengine.renderer.material.Material;
import com.vlengine.renderer.material.MaterialLib;
import com.vlengine.resource.ParameterMap;
import com.vlengine.scene.animation.x.XAnimatedItem;
import com.vlengine.scene.state.AlphaTestState;
import com.vlengine.util.FastList;
import com.vlengine.util.xml.Element;
import java.util.logging.Logger;

/**
 * Material information that can be applyed to ModelPart-s,
 * at runtime Material objects are created from ModelMaterial
 * objects.
 * 
 * @author vear (Arpad Vekas)
 */
public class ModelMaterial {

    private static final Logger log = Logger.getLogger(ModelMaterial.class.getName());
    
    // the generated id, same as name if its set
    protected String id;
    
    // the render-function to use (eg: FF_MT_OP)
    protected String render_func;

    // alpha value used for alpha testing
    protected float alpha = 1.0f;
    // dissolve value for making the material transparent (0 is totaly transparent)
    protected float dissolve = 1.0f;
    protected boolean alpha_test;
    protected boolean alpha_blend;
    protected float shininess = 0.0f;
    protected float refraction_index = 1.0f;
    protected ColorRGBA ambient;
    protected ColorRGBA diffuse;
    protected ColorRGBA specular;
    protected ColorRGBA emissive;
    protected ColorRGBA transmissive;
    protected FastList<ModelMaterialPart> textures = new FastList<ModelMaterialPart>();
    
    
    // created material (ready for runtime re-use)
    // the material used in current session for the rendering,
    // note that the material is created at runtime, depending
    // on what the user on the client mashine requested.
    protected Material material;
    // the parameters used for creating the material
    protected MatParameters materialParams = new MatParameters();
    
    protected void save(Element parent) {
        parent.addContent(new Element("id").setText(id));
        if(render_func!=null)
            parent.addContent(new Element("render_func").setText(render_func));
        if(alpha!=1.0f)
            parent.addContent(new Element("alpha").setText(alpha));
        if(dissolve!=1.0f)
            parent.addContent(new Element("dissolve").setText(dissolve));
        if(alpha_test)
            parent.addContent(new Element("alpha_test").setText(alpha_test));
        if(alpha_blend)
            parent.addContent(new Element("alpha_blend").setText(alpha_blend));
        if(shininess!=0.0f)
            parent.addContent(new Element("shininess").setText(shininess));
        if(refraction_index!=1.0f)
            parent.addContent(new Element("refraction_index").setText(refraction_index));
        if(ambient!=null)
            parent.addContent(new Element("ambient").setText(ambient.getColorArray()));
        if(diffuse!=null)
            parent.addContent(new Element("diffuse").setText(diffuse.getColorArray()));
        if(specular!=null)
            parent.addContent(new Element("specular").setText(specular.getColorArray()));
        if(transmissive!=null)
            parent.addContent(new Element("transmissive").setText(transmissive.getColorArray()));
        if(emissive!=null)
            parent.addContent(new Element("emissive").setText(emissive.getColorArray()));
        // save the textures
        if( textures.size() > 0) {
            Element texes = new Element("textures");
            parent.addContent(texes);
            for(int i=0; i<textures.size(); i++) {
                ModelMaterialPart mmp = textures.get(i);
                Element texe = new Element("texture");
                texes.addContent(texe);
                mmp.save(texe);
            }
        }
    }

    protected void load(Element parent) {
        id = parent.getChildText("id");
        if(parent.getChild("render_func")!=null)
            render_func = parent.getChildText("render_func");
        if(parent.getChild("alpha")!=null)
            alpha = parent.getChildfloat("alpha");
        if(parent.getChild("dissolve")!=null)
            dissolve = parent.getChildfloat("dissolve");
        if(parent.getChild("alpha_test")!=null)
            alpha_test = parent.getChildboolean("alpha_test");
        if(parent.getChild("alpha_blend")!=null)
            alpha_blend = parent.getChildboolean("alpha_blend");
        if(parent.getChild("shininess")!=null)
            shininess = parent.getChildfloat("shininess");
        if(parent.getChild("refraction_index")!=null)
            refraction_index = parent.getChildfloat("refraction_index");
        float[] col = new float[4];
        float[] color = parent.getChildfloat("ambient", col);
        if(color != null)
            ambient = new ColorRGBA().set(color);
        color = parent.getChildfloat("diffuse", col);
        if(color != null)
            diffuse = new ColorRGBA().set(color);
        color = parent.getChildfloat("specular", col);
        if(color != null)
            specular = new ColorRGBA().set(color);
        color = parent.getChildfloat("transmissive", col);
        if(color != null)
            transmissive = new ColorRGBA().set(color);
        color = parent.getChildfloat("emissive", col);
        if(color != null)
            emissive = new ColorRGBA().set(color);
        Element texes = parent.getChild("textures");
        if(texes != null ) {
            FastList<Element> texelist = texes.getChildren();
            for(int i=0; i<texelist.size(); i++) {
                Element texe = texelist.get(i);
                ModelMaterialPart mmp = new ModelMaterialPart();
                mmp.load(texe);
                textures.add(mmp);
            }
        }
    }

    public Material getInstance(AppContext app, MatParameters params) {
        // check that the params match the materials params
        if(material!=null) {
            if(!materialParams.equals(params)) {
                material = null;
            }
        }
        
        // this material can be reused, so reuse the same instance
        if(material!=null)
            return material;

        materialParams.clear();
        materialParams.putAll(params);

        // construct parameters for material
        MatParameters par = new MatParameters();
        ParameterMap tpar = new ParameterMap();
        par.clear();
        // number of diffuse textures
        int difft = 0;
        boolean bump = false;
        boolean normal = false;

        // go over the textures
        for(int tn=0; tn<textures.size(); tn++) {
            ModelMaterialPart subm = textures.get(tn);

            // load the texture if its not already loaded
            if(subm.tex == null ) {
                tpar.clear();
                // do we set translation on the texture?
                if(subm.translation != null)
                    tpar.put("texture_translation", subm.translation);
                if(subm.scale != null)
                    tpar.put("texture_scale", subm.scale);
                tpar.put("texture_wrap", Texture.WM_WRAP_S_WRAP_T);
                tpar.put("texture_aniso_level", app.conf.textureAniso);
                //tpar.put("texture_apply", Texture.AM_MODULATE);
                    // do we set scele on the texture?
                subm.tex = app.getResourceFinder().getTexture(subm.textureName, tpar);
            }
            if(subm.tex != null) {
                MatParameters.ParamKey texname = null;
                if(subm.type == ModelMaterialPart.TextureType.Diffuse) {
                    difft++;
                    // diffuse texture
                    if(difft == 1) {
                        texname = MatParameters.ParamKey.Diffuse0Map;
                    } else {
                        texname = MatParameters.ParamKey.Diffuse1Map;
                    }
                } else {
                    // if we have enabled bumpmapping and we have TNB
                    if(app.conf.graphBumpMapping && !materialParams.containsKey(MatParameters.ParamKey.NoTNB)) {
                        if(subm.type == ModelMaterialPart.TextureType.BumpMap) {
                            // bump texture
                            texname = MatParameters.ParamKey.BumpMap;
                            bump = true;
                        } else if(subm.type == ModelMaterialPart.TextureType.NormalMap) {
                            texname = MatParameters.ParamKey.NormalMap;
                            normal = true;
                        }
                    }
                }
                if(texname != null)
                    par.put(texname, subm.tex);
            } else {
                log.warning("Could not load texture "+subm.textureName);
            }
        }
        boolean shader = false;

        // TODO: change, that we only check the render_func only if
        // its not yet created

        // create the material
        
        
        // do we have a skinned mesh
        XAnimatedItem xa = (XAnimatedItem) materialParams.get(MaterialLib.BONES);
        if(xa!=null) {
            // we need a shader
            shader = true;
            par.put(MaterialLib.BONES, xa);
        }
        
        if(transmissive != null && !transmissive.equals(ColorRGBA.white)) {
            par.put(MaterialLib.TRANSMISSIVE, transmissive);
        }
        
        if(alpha_test) {
            par.put(MaterialLib.ALPHATEST, alpha);
            par.put(MaterialLib.ALPHAFUNC, AlphaTestState.TF_GEQUAL);
            par.put(MaterialLib.NOCULL, true);
        }
        // dissolve
        if(dissolve < 1.0f) {
            par.put(MaterialLib.DISSOLVE, dissolve);
        }
        // material colors
        if(ambient != null && !app.conf.fixspecular) {
            par.put(MaterialLib.AMBIENT, ambient);
        }
        if(diffuse != null && !app.conf.fixspecular) {
            par.put(MaterialLib.DIFFUSE, diffuse);
        }
        if(specular != null && !app.conf.fixspecular) {
            // should we fix specular?
            /*
            if(app.conf.fixspecular) {
                if(diffuse!=null) {
                    if(specular.r < diffuse.r)
                        specular.r = diffuse.r;
                    if(specular.g < diffuse.g)
                        specular.g = diffuse.g;
                    if(specular.b < diffuse.b)
                        specular.b = diffuse.b;
                } else {
                    if(specular.r < 0.8f)
                        specular.r = 0.8f;
                    if(specular.g < 0.8f)
                        specular.g = 0.8f;
                    if(specular.b < 0.8f)
                        specular.b = 0.8f;
                }
            }
             */
            par.put(MaterialLib.SPECULAR, specular);
        }
        if(emissive != null) {
            par.put(MaterialLib.EMISSIVE, emissive);
        }
        if(shininess != 0) {
            par.put(MaterialLib.SHININESS, shininess);
        }
        
        render_func = getMaterialName(bump, normal, difft, params);

        if(app.conf.perpixel) {
            // force per-pixel lighting
            par.put(MaterialLib.PERPIXEL,true);
        }
        if((app.conf.nospecular || materialParams.getBoolean(MaterialLib.NOSPECULAR, false) ) && !(app.conf.graphBumpMapping && normal )) {
            par.put(MaterialLib.NOSPECULAR,true);
        }
        if(app.conf.graphDepthFog) {
            par.put(MaterialLib.SCREENDEPTHFOG,true);
        }
        // store it into the material variable to be reused
        material = app.getResourceFinder().getMaterial(render_func, par);
        return material;
    }
    
    protected String getMaterialName(boolean bump, boolean normal, int difft, MatParameters params) {
        String render_func = "";
        boolean shader = false;

        if(params.containsKey(MaterialLib.BONES)) {
            // we need a shader
            shader = true;
            render_func = "_SK" + render_func;
        }

        if(normal) {
            shader = true;
            render_func = "NORMDIFFT";
        } else if(bump) {
            shader = true;
            render_func = "BUMPDIFFT";
        } else {
            if(shader) {
                render_func += "PHONGT";
            } else {
                // multi or single texture
                if(params.containsKey(MaterialLib.TRANSMISSIVE)) {
                    render_func += "P_GLASS";
                } else {
                    if(difft<2) {
                        render_func = "_ST"+render_func;
                    } else {
                        render_func = "_MT"+render_func;
                    }
                    if(alpha_blend) {
                        // alpha blend?
                        render_func = "_AB";
                    }
                }
            }
        }

        // shader of FF?
        if(shader) {
            render_func = "VS"+render_func;
        } else {
            render_func = "FF"+render_func;
        }
        
        return render_func;
    }

    public void setAmbient(ColorRGBA amb) {
        ambient = amb;
    }
    
    public ColorRGBA getAmbient() {
        return ambient;
    }

    public void setDiffuse(ColorRGBA dif) {
        diffuse = dif;
    }
    
    public ColorRGBA getDiffuse() {
        return diffuse;
    }

    public void setSpecular(ColorRGBA spc) {
        specular = spc;
    }
    
    public ColorRGBA getSpecular() {
        return specular;
    }
    
    public void setEmissive(ColorRGBA spc) {
        emissive = spc;
    }
    
    public void setShininess(float shine) {
        shininess = shine;
    }
    
    public float getShininess() {
        return shininess;
    }
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public void setDissolve(float dissolve) {
        this.dissolve = dissolve;
    }

    public void setAlphaTest(boolean alpha_test) {
        this.alpha_test = alpha_test;
    }

    public void setAlphaBlend(boolean alpha_blend) {
        this.alpha_blend = alpha_blend;
    }
    
    public void addTexture(ModelMaterialPart texture) {
        textures.add(texture);
    }

    public void setTransmissive(ColorRGBA transmissive) {
        this.transmissive = transmissive;
    }

    public void setRefractionIndex(float refraction_index) {
        this.refraction_index = refraction_index;
    }
    
    public FastList<ModelMaterialPart> getTextures() {
            return textures;
    }
    
    public boolean hasNormalMap() {
        for(int tn=0; tn<textures.size(); tn++) {
            ModelMaterialPart subm = textures.get(tn);
            if(subm.type == ModelMaterialPart.TextureType.NormalMap) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasBumpMap() {
        for(int tn=0; tn<textures.size(); tn++) {
            ModelMaterialPart subm = textures.get(tn);
            if(subm.type == ModelMaterialPart.TextureType.BumpMap) {
                // bump texture
                return true;
            }
        }
        return false;
    }
}
