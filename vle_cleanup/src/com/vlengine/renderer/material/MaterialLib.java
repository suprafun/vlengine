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

import com.vlengine.app.AppContext;
import com.vlengine.renderer.material.Material;
import com.vlengine.resource.ParameterMap;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * A RenderLib is responsible for creating shaders and setting up
 * OpenGL states for rendering objects
 * The idea is that each ModelBatch contains information on the way it would
 * like to be rendered. The RenderLib converts these informations to OpenGL
 * states and shaders.
 * Another application of renderlibs is using them in renderpasses. Diferent 
 * passes can use diferent RenderLibs to render the scene.
 * 
 * The loaded model contains named resources like:
 * Vertex coords, Normals, Texture coordinates, Shader attribute arrays, Textures
 * a RenderFunction string
 * For textures it is defined, which coordinate set it uses
 * For each vertex attribute, is it constant, or it changes during time
 * Not changing attributes are put into and interleaved array, and put into a
 * 2 to 4 MB large shared VBO
 * The renderfunction translates to a material in the renderlib
 * A Material defines the required resources:
 * Vertex coords, Normals, Number and type of used textures, Texture coordinates
 * Behind each material is a shader and some OpenGL state setting
 * 
 * The renderlib takes resources and effects from the scene like Lights, Fog
 * 
 * The renderlib then searches its Material database to try to match the 
 * requested material based on the provided renderfunction, and resources.
 * 
 * The given configuration is considered, like the graphic card brand and type.
 * The list contains preferred material names and incompatible material names
 * 
 * Shaders throwing exception are marked as non-compatible. 
 * 
 * The renderlib creates/reuses shaders and takes binding information on attributes
 * and uniforms (OpenGL id-s), and writes them to ModelBatch. There is an array
 * in ModelBatch for these binding information, so that each diferent
 * Material in ModelBatch has its binding information.
 * 
 * @author vear (Arpad Vekas)
 */

public abstract class MaterialLib {
    protected static final Logger logger = Logger.getLogger(MaterialLib.class.getName());
    
    // keywords in the parametermap
    

    // alpha test value passed <ALPHATEST,Byte>
    public static final MatParameters.ParamKey ALPHATEST = MatParameters.ParamKey.AlphaTest;
    // <ALPHAFUNC,Integer>
    public static final MatParameters.ParamKey ALPHAFUNC = MatParameters.ParamKey.AlphaFunc;
    // <NOCULL,Boolean>
    public static final MatParameters.ParamKey NOCULL = MatParameters.ParamKey.Nocull;
    // <ANIM_FRAMES,Integer>
    public static final MatParameters.ParamKey ANIM_FRAMES = MatParameters.ParamKey.AnimFrames;
    
    public static final MatParameters.ParamKey DIFFUSE0MAP = MatParameters.ParamKey.Diffuse0Map;
    public static final MatParameters.ParamKey DIFFUSE1MAP = MatParameters.ParamKey.Diffuse1Map;
    public static final MatParameters.ParamKey NORMALMAP = MatParameters.ParamKey.NormalMap;
    public static final MatParameters.ParamKey BUMPMAP = MatParameters.ParamKey.BumpMap;
    
    public static final MatParameters.ParamKey BONES = MatParameters.ParamKey.Bones;
    
    //public static final String  = "ALPHAFUNC";
    // texture object passed <TEXTURE[x],Texture> or <TEXTURE[x],FastList<Texture>>
    //public static final String TEXTURE[] = new String[] { "null", "DIFFUSE0", "DIFFUSE1", "NORMAL", "NORMBUMP" };
            
    //public static final String ALPHATEST = "ALPHATEST";
    // <ALPHAFUNC,Integer>
    //public static final String ALPHAFUNC = "ALPHAFUNC";
    //public static final String ALPHABLEND = "ALPHABLEND";
    
    // <NOCULL,Boolean>
    //public static final String NOCULL = "NOCULL";
    // <ANIM_FRAMES,Integer>
    //public static final String ANIM_FRAMES = "ANIM_FRAMES";
    
    // skinning
    //public static final String BONES = "BONES";
    
    public static final MatParameters.ParamKey DISSOLVE = MatParameters.ParamKey.Dissolve;
    public static final MatParameters.ParamKey AMBIENT = MatParameters.ParamKey.Ambient;
    public static final MatParameters.ParamKey DIFFUSE = MatParameters.ParamKey.Diffuse;
    public static final MatParameters.ParamKey SPECULAR = MatParameters.ParamKey.Specular;
    public static final MatParameters.ParamKey SHININESS = MatParameters.ParamKey.Shininess;
    public static final MatParameters.ParamKey TRANSMISSIVE = MatParameters.ParamKey.Transmissive;
    public static final MatParameters.ParamKey EMISSIVE = MatParameters.ParamKey.Emissive;
    
    public static final MatParameters.ParamKey NOSPECULAR = MatParameters.ParamKey.Nospecular;
    public static final MatParameters.ParamKey SCREENDEPTHFOG = MatParameters.ParamKey.ScreenDepthFog;
    public static final MatParameters.ParamKey PERPIXEL = MatParameters.ParamKey.PerPixel;
    
    // the renderlib we ask for a material, if we cannot provide it
    protected MaterialLib parent;
       
    public void setParent(MaterialLib parent) {
        this.parent = parent;
    }

    public Material getMaterial(String name, MatParameters params) {
        
        Material mat = null;
        /*
        try {
            
            Method m = this.getClass().getMethod(name, handlerSig);
            if(m!=null)
                mat=(Material) m.invoke(this, params);
            
        } catch (Exception ex) {
            // do not throw exception, it is normal for a material library to not handle all the
            // material types
            //logger.log(Level.SEVERE, null, ex);
        }
        if(mat==null) {
         */
            // we could not provide the material, check if our parent can do it
            if(parent!=null)
                mat=parent.getMaterial(name, params);
        //}
        return mat;
    }
}
