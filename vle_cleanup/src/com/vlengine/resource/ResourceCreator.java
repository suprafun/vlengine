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

import com.vlengine.app.AppContext;
import com.vlengine.audio.openal.OpenALAudioBuffer;
import com.vlengine.audio.openal.OpenALAudioTrack;
import com.vlengine.audio.util.AudioLoader;
import com.vlengine.image.Image;
import com.vlengine.image.Texture;
import com.vlengine.resource.md5.Md5AnimLoader;
import com.vlengine.resource.md5.Md5MeshLoader;
import com.vlengine.scene.animation.MD5.MD5BoneAnimation;
import com.vlengine.resource.model.Model;
import com.vlengine.resource.model.ModelPack;
import com.vlengine.resource.obj.ObjMtlLib;
import com.vlengine.resource.obj.ObjLoader;
import com.vlengine.resource.x.XLoader;
import com.vlengine.scene.state.lwjgl.LWJGLTextureState;
import com.vlengine.util.TextureManager;
import com.vlengine.util.xml.Element;
import com.vlengine.util.xml.XMLFile;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class ResourceCreator {

    public static final Logger log = Logger.getLogger(ResourceCreator.class.getName());
    
    // loader for collada models
    //private DaeLoader cLoader = new DaeLoader();
    // loader for obj models
    private ObjLoader oLoader;
    // loader for obj materials
    private ObjLoader omLoader;
    // the texture converter
    private TextureConverter tConvert;
    // loader for MD5 meshed
    private Md5MeshLoader md5meshLoader;
    private Md5AnimLoader md5animLoader;
    // loader for X meshes and animations
    private XLoader xLoader;
    
    AppContext app;
    
    public ResourceCreator() {

    }
    
    public void setAppContext(AppContext app) {
        if( app != this.app) {
            this.app = app;
            omLoader = new ObjLoader(app);
            md5meshLoader = new Md5MeshLoader();
            md5animLoader = new Md5AnimLoader();
        }
    }
    
    // this method is responsible for instantiating java objects
    // which can be used by the engine
    public Object createResource( String name, int type, ParameterMap parameters, ResourceFolder rof) {
        
        if( name.endsWith(".tga")
          || name.endsWith(".dds")
          || name.endsWith(".png")
          || name.endsWith(".jpg")
          || name.endsWith(".bmp")
          || name.endsWith(".vlt")
          ) {
            // do we only need the image, and not the texture?
            if(type==ResourceFinder.RESOURCE_IMAGE)
                return getImage(name, parameters, rof);

            Texture t=null;
            // construct the texture key
            TextureKey tkey = (TextureKey) parameters.get("tkey");
            if(tkey == null) {
                ParameterMap parm = new ParameterMap();
                parm.putAll(parameters);
                parameters = parm;
                tkey = new TextureKey(name, parameters);
                parameters.put("tkey", tkey);
            }
            // construct a texture key to find the texture?
            t = (Texture)rof.getCached(tkey);
            if( t == null ) {
                Image img = getImage(name, parameters, rof);
                /*
                // load the image file
                Image img = (Image) rof.getCached(name);
                if(img == null ) {
                    // no need for directbuffer, because right now image data
                    // are extracted using a stream from the buffer
                    ByteBuffer data = rof.loadPrepared( name, ParameterMap.NODIRECTBUFFER );
                    if( data == null ) {
                            return null;
                    }
                    img = TextureReader.createImage(data, name, parameters);
                    if(img==null)
                        return null;
                    rof.addCached(name, img);
                }
                 */
                
                t = TextureReader.createTexture(img, name, parameters);
                if( t != null ) {
                    rof.addCached( tkey, t );
                }
            }
            return t;
        /*} else if( name.endsWith(".dae") ) {
            DaeObject dao = (DaeObject) rof.getCached( name );
            if( dao == null ) {
                ByteBuffer data = rof.loadPrepared(name, ParameterMap.NODIRECTBUFFER);
                if( data == null ) {
                    return null;
                }
                if(cLoader==null)
                    cLoader = new DaeLoader();
                dao = cLoader.load(data, parameters);
                if( dao!=null ) {
                    rof.addCached( name, dao);
                }
            }
            return dao;
           */
        } else if( name.endsWith(".mtl") ) {
            ObjMtlLib omod = (ObjMtlLib) rof.getCached(name);
            return omod;
        } else if( name.endsWith(".obj") ) {
            return getObjModel(name, parameters, rof);
        } else if( name.endsWith(".x") ) {
            return getXModel(name, parameters, rof);
            /*
            // obj model file
            ByteBuffer dao = (ByteBuffer) rof.getCached( name );
            if( dao == null ) {
                ByteBuffer data = rof.loadPrepared(name, ParameterMap.NODIRECTBUFFER);
                if( data == null ) {
                    return null;
                }
                // we dont save mtl files
            }
            return dao;
             */
        } else if( name.endsWith(".md5mesh") || name.endsWith(".md5anim") ) {
            return rof.getCached(name);
        } else if (name.endsWith(".ogg") || name.endsWith(".wav")) {
            return getAudioTrack(name, parameters, rof);
        } else if(name.endsWith(".pack.gz")) {
            // modelpack
            return getModelPack(name, parameters, rof);
        } else if(name.endsWith(".xml") || type==ResourceFinder.RESOURCE_XML) {
            return getXML(name, parameters, rof);
        }
        return null;
    }

    public void convertResource( String name, String newName, int type, ParameterMap parameters, ResourceFolder rof) {
        name = name.toLowerCase();

        if( name.endsWith(".tga")
          || name.endsWith(".dds")
          || name.endsWith(".png")
          || name.endsWith(".jpg")
          || name.endsWith(".bmp")
          ) {
            getImage(newName, parameters, rof);
            /*
            ByteBuffer data = rof.loadDesigned(name, ParameterMap.NODIRECTBUFFER);
            if(data != null ) {
                // extract the image data from it
                Image img = TextureReader.createImage(data, name, ParameterMap.DIRECTBUFFER);
                if( img == null)
                    return;
                // do we need to convert it?
                if(!name.equals(newName)) {
                    // rescale if not power-of-two
                    if(tConvert==null)
                        tConvert = new TextureConverter();
                    Image imagec = tConvert.convertTexture(img);
                    if( imagec != null) {
                        // get the converted data
                        data = imagec.save();
                        // add it to prepared
                        rof.savePrepared(newName, data);
                        rof.addCached(newName, imagec);
                    } else {
                        // add the unconverted to prepared, we cannot convert it
                        rof.addCached(name, img);
                    }
                } else {
                    rof.addCached(name, img);
                }
            }
             */
        } else if( name.endsWith(".obj") ) {
            getObjModel(name, parameters, rof);
            return;
        } else if( name.endsWith(".x") ) {
            getXModel(name, parameters, rof);
            return;
        } else if( name.endsWith(".mtl") ) {
            // we need to load the obj file
            ByteBuffer data = rof.loadDesigned(name, ParameterMap.NODIRECTBUFFER);
            if( data != null ) {
                ObjMtlLib omod = omLoader.convertMtl(data, parameters);
                // TODO: save it to archive and prepared as a modelpack
                omod.setName(name);
                // add it to the cashed
                rof.addCached(newName, omod);
                return;
            }
        } else if(name.endsWith(".md5mesh")) {
            // MD5 mesh
            ByteBuffer data = rof.loadDesigned(name, ParameterMap.NODIRECTBUFFER);
            if( data != null ) {
                Model omod = md5meshLoader.convert(data, parameters);
                omod.setName(name);
                // add it to the cashed
                rof.addCached(newName, omod);
                return;
            }
        } else if(name.endsWith(".md5anim")) {
            // MD5 mesh
            ByteBuffer data = rof.loadDesigned(name, ParameterMap.NODIRECTBUFFER);
            if( data != null ) {
                MD5BoneAnimation omod = md5animLoader.convert(data, parameters);
                omod.name = name;
                // add it to the cashed
                rof.addCached(newName, omod);
                return;
            }
        } else if (name.endsWith(".ogg") || name.endsWith(".wav")) {
            /*
            File folder = new File(rof.getCachePathFull());
            if (folder.isFile()) {
                log.log(Level.SEVERE,
                   "Unable to create folder \"{0}\", " +
                   "file with the same name exists.", folder.getAbsolutePath());
                return;
            }
            
            if (!folder.exists()) folder.mkdirs();
            
            rof.savePrepared(name,
                    rof.loadDesigned(name, ParameterMap.NODIRECTBUFFER));
            
            // is cache overloaded to mean two entirely different things?!
            */
            // mark as cashed, we can fetch it from dev without problem
            rof.addCached(name, null);
             
        } else if(name.endsWith(".xml") || type==ResourceFinder.RESOURCE_XML) {
            getXML(name, parameters, rof);
        }
        
        return;
    }

    void cleanup(ResourceFolder rf) {
        // go trough all the image objects, and delete all the texture id-s
        for(Object o : rf.cached.values()) {
            if(o instanceof Image) {
                Image img = (Image) o;
                int texid = img.getTextureId();
                if(texid != 0) {
                    LWJGLTextureState.deleteTextureId(texid);
                    img.setTextureId(0);
                    TextureManager.removeTextureId(texid);
                }
            }
        }
    }
    
    protected Element getXML(String name, ParameterMap parameters, ResourceFolder rof) {
        String nm = name;
        Element el = (Element)rof.getCached(name);
        if(el==null) {
            // compressed or uncompressed?
            if(parameters.getBoolean("compressed", true)) {
                // try load compressed
                
                // TODO: encoded?
                if(!nm.endsWith(".xml.gz")) {
                    nm = nm + ".xml.gz";
                }
                el = XMLFile.fromXML(rof.getCachePathFull()+"/"+nm, true);
            } else {
                // try load directly
                if(!nm.endsWith(".xml")) {
                    nm = nm + ".xml";
                }
                el = XMLFile.fromXML(rof.getCachePathFull()+"/"+nm, false);
            }
            // if not in chache, load from dev
            if(el==null) {
                if(parameters.getBoolean("compressed", true)) {
                    // try load compressed

                    // TODO: encoded?
                    if(!nm.endsWith(".xml.gz")) {
                        nm = nm + ".xml.gz";
                    }
                    el = XMLFile.fromXML(rof.getDesignPathFull()+"/"+nm, true);
                } else {
                    // try load directly
                    if(!nm.endsWith(".xml")) {
                        nm = nm + ".xml";
                    }
                    el = XMLFile.fromXML(rof.getDesignPathFull()+"/"+nm, false);
                }            }
            if(el!=null) {
                rof.addCached(name, el);
            }
        }
        return el;
    }

    protected ModelPack getModelPack(String name, ParameterMap parameters, ResourceFolder rof) {
        ModelPack mp = (ModelPack) rof.getCached(name);
        if(mp==null) {
            mp = new ModelPack();
            mp.setName(name.substring(0, name.length()-8));
            mp.load(rof.getCachePathFull());
            if(mp!=null) {
                rof.addCached(name, mp);
            }
        }
        return mp;
    }
                
    protected Image getImage(String newName, ParameterMap parameters, ResourceFolder rof) {
        String name = newName;
        if(name.endsWith(".vlt"))
            name = newName.substring(0, name.length()-4);
        else
            newName = name + ".vlt";

        // load the image file from cached
        Image img = (Image) rof.getCached(newName);
        if(img == null ) {
            // no need for directbuffer, because right now image data
            // are extracted using a stream from the buffer
            ByteBuffer data = rof.loadPrepared( newName, ParameterMap.NODIRECTBUFFER );
            if( data != null ) {
                img = TextureReader.createImage(data, newName, parameters);
                if(img!=null)
                    rof.addCached(newName, img);
            }
        }
        if(img == null ) {
            ByteBuffer data = rof.loadDesigned(name, ParameterMap.NODIRECTBUFFER);
            if(data != null ) {
                // extract the image data from it
                img = TextureReader.createImage(data, name, ParameterMap.DIRECTBUFFER);
                if( img == null)
                    return null;
                // do we need to convert it?
                if(!name.equals(newName)) {
                    // rescale if not power-of-two
                    if(tConvert==null)
                        tConvert = new TextureConverter();
                    Image imagec = tConvert.convertTexture(img);
                    if( imagec != null) {
                        img = imagec;
                        // get the converted data
                        data = img.save();
                        // add it to prepared
                        rof.savePrepared(newName, data);
                        rof.addCached(newName, img);
                    } else {
                        rof.addCached(name, img);
                    }
                } else {
                    rof.addCached(name, img);
                }
            }
        }
        return img;
    }

    protected Model getObjModel(String name, ParameterMap params, ResourceFolder rof) {
        boolean shared = params.getBoolean("shared", true);
        // TODO: enable loading from resource pack
        Model omod = null;
        if(shared)
            omod = (Model) rof.getCached(name);
        if(omod == null) {
            // we need to load the obj file
            ByteBuffer data = rof.loadDesigned(name, ParameterMap.NODIRECTBUFFER);
            if( data != null ) {
                if(oLoader==null)
                    oLoader = new ObjLoader(app);
                omod = oLoader.convert(data, params);
                // TODO: save it to archive and prepared as a modelpack
                omod.setName(name);
                // add it to the cashed
                if(shared)
                    rof.addCached(name, omod);
                else if(!rof.cached.containsKey(name))
                    rof.addCached(name, null);
            }
        }
        return omod;
    }
    
    protected Model getXModel(String name, ParameterMap params, ResourceFolder rof) {
        boolean shared = params.getBoolean("shared", true);
        // TODO: enable loading from resource pack
        Model omod = null;
        if(shared)
            omod = (Model) rof.getCached(name);
        if(omod == null) {
            // we need to load the obj file
            ByteBuffer data = rof.loadDesigned(name, ParameterMap.NODIRECTBUFFER);
            if( data != null ) {
                if(xLoader==null)
                    xLoader = new XLoader(app);
                omod = xLoader.convert(data, params);
                // TODO: save it to archive and prepared as a modelpack
                omod.setName(name);
                // add it to the cashed
                if(shared)
                    rof.addCached(name, omod);
                else if(!rof.cached.containsKey(name))
                    rof.addCached(name, null);
            }
        }
        return omod;
    }

    /**
     * TODO: remove dependency on OpenALAudioBuffer?
     * 
     * @param name
     * @param parameters
     * @param rof
     * @return
     */
    private Object getAudioTrack(String name, ParameterMap parameters, ResourceFolder rof) {
        boolean stream = !parameters.getBoolean(ParameterMap.KEY_DIRECTBUFFER, false);
        
        URL resource = rof.getDesignUrl(name);
        if (resource == null) return null;

        String urlString = resource.toString();
        if (!stream) {
            OpenALAudioBuffer buff = (OpenALAudioBuffer) rof.getCached(name);
            if(buff==null) {
                buff = OpenALAudioBuffer.generateBuffer();
                try {
                    AudioLoader.fillBuffer(buff, resource);
                    rof.addCached(name, buff);
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Exception", e);
                    return null;
                }
            }
            return new OpenALAudioTrack(resource, buff);
        }
        return new OpenALAudioTrack(resource, stream);
    }
}
