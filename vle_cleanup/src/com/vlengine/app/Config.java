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

package com.vlengine.app;

import com.vlengine.math.Vector3f;

/**
 * Global configuration parameters storage class
 * gets loaded and saved to properties.cfg
 * You should sublass this class to add your own parameters,
 * and set it in AppContext conf
 * Precede parameters which you dont want saved/loaded with "p_"
 * 
 * @author vear (Arpad Vekas)
 */
public class Config {

    public boolean dialog = true;
    
    // config parameters for fullscreen, values given are the defaults
    public int display_width = 640;
    public int display_height = 480;
    public int display_depth = 16;
    public int display_freq = 60;
    public boolean display_fullscreen = true;
    // field of view we are working with
    public float p_frustum_fovy = 60.0f;

    // used desing path?, should be false for distribution
    public boolean p_use_design_path = true;
    // the path to unconverted designed resources
    public String design_path = System.getProperty("user.dir")+"/dev";   //"C:/temp/xshift3"; //C:/temp/xshift_work/dev";
    
    // use memory cache when extracting from archive files?
    // use resource packs for resource loading
    public boolean p_useres = true;
    // if false, createcache must be true
    public boolean p_usememcache = false;
    /**
     *  The path to resource dir root. This folder can contain ZIP archives staticaly
     *  installed. This folder is not updated by the autoupdater.
     */
    public String res_path =System.getProperty("user.dir")+"/pack";
    // use caching folder for resource loading? should be false for distribution
    public boolean p_usecaching = true;
    // extract resource into cache folders?
    public boolean p_createcache = true;
    // clean up intermediate cache files (anything not in cached subfolder)
    public boolean p_cleanche = true;
    // the path to cached dir root
    public String cache_path = System.getProperty("user.dir")+"/cache";
    
    public static final boolean p_storeshadersource = true;

    // parameters for bone animation
    // 0 -
    // 1 -software ( compute bones in software, morph vertices when needed)
    // 2 -GPU ( compute bones in software, do vertex morph on GPU )
    public int boneanim_type = 1;
    // limit of bones for a single geometry
    // GPU's cant handle unlimited number of bones, so we need to break up
    // geometry into smaller parts, which dont reference more than the given number of bones
    public int p_bone_limit = 60;
    //public int p_weight_limit = 6;
    
    // 0 block lod
    // 1 clip
    public int terrain_engine = 0;
    // terrain parameters
    // the total heightmap size, also the color texture size
    public int terrain_size = 1024;
    // sector size
    public int terrain_sector_size = 512;
    // block size
    public int terrain_blocksize = 32;
    // how many blocks are visible arond the viewer
    public int terrain_viewblocks = 2;
    // use VBO for terrain
    public boolean terrain_vbo = true;
    // use display list for terrain?
    public boolean terrain_dlist = false;
    // terrain scale ( the size of the terrain_size long heightmap) in world units
    public final Vector3f terrain_scale = new Vector3f(6144f,2048f,6144f);
    // falloff stepping of lod (units)
    public int terrain_lod_step_block = 64;
    
    public int view_frustrum_far = 5000;
    
    /**
     * Alpha bits to use for the renderer. Must be set in the constructor.
     */
    public int alphaBits = 0;
    /**
     * Depth bits to use for the renderer. Must be set in the constructor.
     */
    public int depthBits = 8;
    /**
     * Stencil bits to use for the renderer. Must be set in the constructor.
     */
    public int stencilBits = 0;
    /**
     * Number of samples to use for the multisample buffer. Must be set in the constructor.
     */
    public int samples = 0;
    
    // the lighting model we use
    // 0-no light management, 1- up to 8 most influential light per batch, 2-indexed lighting, 3-deferred shading
    public int lightmode = 1;

    // per-pixel lighting
    public boolean perpixel = true;
    
    // enable bump and normal maps
    public boolean graphBumpMapping = true;
    
    // disable shaders altogether
    public boolean graphNoShaders=false;
    
    // try to fix specular (broken models with specular set to gray instead of light)
    public boolean fixspecular = true;
    // disable specular altogether
    public boolean nospecular = true;

    // shadowmapping parameters
    // type: 0-off, 1-perspective shadowmap
    public int shadowmap = 1;
    
    // the number of view frustum splits for shadow mapping
    public int shadowmap_splits = 4;

    // the dimension of the shadowmap texture in the perspective split 0
    // must be power-of-two
    public int shadowmap_dimension = 1024;
    
    // the ratio to reduce the shadowmap dimension with each new split (1-no reduction, 2-halve the dimension)
    public int shadowmap_reduction_ratio = 2;
    
    // memory allowed for use by the shadowmap textures, expressed in MegaPixels
    // if shadowmap_dimension = 1024, then one unit of this corresponds to one full sized
    // shadow map
    public int shadowmap_retain = 3;
    
    public boolean nomultithread = true;
    
    public int maxCullThreads = 1;
    
    public boolean graphPostprocess = false;
    public boolean graphSSAO = false;
    public boolean graphBloom = false;
    //public int graphMinSamples = 0;
    
    // constant fog based on screen depth
    public boolean graphDepthFog = false;
    public float graphDepthFogStart = 0.8f;
    public float graphDepthFogDensity = 0.003f;
    
    public float soundMasterVolume = 1f;
    public float soundMusicVolume = 0.1f;
    public float soundSFXVolume = 0.5f;
    
    public float textureAniso = 0.25f;
}
