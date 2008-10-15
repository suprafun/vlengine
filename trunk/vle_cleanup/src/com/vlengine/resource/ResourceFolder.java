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
import com.vlengine.app.Config;
import com.vlengine.image.Texture;
import com.vlengine.util.FastList;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class hold a reference to a folder containing resources loadable by
 * the engine. Note that a virtual folder can be 2 actual filesystem folders:
 * The "design" or "dev" path is the place where artists place their work,
 * this folder is taken as source for unconverted resources, which are converted
 * using the ResourceCreator class.
 * 
 * The "cache" path (when switched on in Config) hold resources ready to be loaded
 * by the engine. Its a helper folder during development (or runtime, when
 * converted resources are machine specific). 
 * 
 * If a resource can be loaded from the cache, it will be loaded from there,
 * if not found it will try to load from the "dev" folder.
 * 
 * All these folders are represented by a single ResourceFolder. The engine
 * create ResourceFolder classes for all subfolders of
 * Config.cache_path and Config.dev_path.
 * 
 * @author vear (Arpad Vekas)
 */
public class ResourceFolder {
    private static final Logger logger = Logger.getLogger(ResourceFolder.class.getName());
   
    // the unique id for this folder
    private String id;
    
    // the group (mod) of this folder
    private String mod;
    
    // path to dev folder, where newly designed resources are
    private String designpath;
    
    // path to resource file under root resource folder
    protected String resourcepath;
    /**
     * path to cache path containing folders with
     * extracted, decompressed, prepared, cached folders
     * extracted raw file extracted from resource file
     * decompressed decompressed/decoded file
     * prepared converted for loading, loadable by application
     * cached saved from application, loadable as a direct byte-stream,
     * in-memory representation as class, not only bytebuffer
     */ 
    private String cachepath;
    
    // class used for extracting files
    //private ZIPFile extractor;
    
    // the list of files in the development folder
    protected final HashMap<String, ByteBuffer> designed = new HashMap<String, ByteBuffer>();    
    // the bytes of prepared resources, which can be directly used
    // for creating objects
    //protected final HashMap<String, ByteBuffer> prepared = new HashMap<String, ByteBuffer>();
    // the created objects ready to be used in the engine
    protected final HashMap<Object, Object> cached = new HashMap<Object, Object>();
    
    // the list of all files in this folder
    private FastList<String> allfiles = null;
    
    // the application we are serving
    protected Config conf;
    
    
    public ResourceFolder(AppContext app) {
        this.conf = app.conf;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }
    
    public void setMod(String mod) {
        this.mod = mod;
    }
    
    public String getMod() {
        return mod;
    }

    public void setResourcePath(String respath) {
        this.resourcepath = respath;
    }
    
    public String getResourcePath() {
        return resourcepath;
    }
    
    public String getResourcePathFull() {
        return conf.res_path + "/" + resourcepath;
    }

    public void setCachePath(String cachp) {
        this.cachepath = cachp;
    }
    
    public String getCachePath() {
        return cachepath;
    }
    
    public String getCachePathFull() {
        return conf.cache_path + "/" + cachepath;
    }
    
    public void setDesignPath(String desp) {
        this.designpath = desp;
    }
    
    public String getDesignPath() {
        return designpath;
    }
    
    public String getDesignPathFull() {
        return conf.design_path!=null && designpath != null ? 
            conf.design_path + "/" + designpath
           : null;
    }
    
    public void ensureCachePaths() {
        String cp;
        String fullcp;
        File cpf;
        
        if( cachepath == null ) {
            // check if cachepath exists
            cp = id;
            fullcp = conf.cache_path + cp;
            cpf = new File(fullcp);
            if( !cpf.exists() ) {
                // create folder
                cpf.mkdirs();
            }
            cachepath = id;
        }
        cpf = new File( getCachePathFull() );
        if( !cpf.exists() ) {
            // create folder
            cpf.mkdirs();
        }
    }
        
    private void checkFolder( String folder, HashMap list ) {
        File dir=new File( folder );
        File [] drl = dir.listFiles();
        if( drl != null ) {
            for( int i=0; i<drl.length; i++ ) {
                String drf = drl[i].getName().toLowerCase();
                if( !list.containsKey(drf) ) {
                    // the file is not yet registered
                    // put it into list, but without loading any data
                    list.put(drf, null);
                }
            }
        }
    }
    
    // reads in list of files in the cache folders
    public void readFileList() {
        if( allfiles == null ) {
            allfiles = new FastList<String>();
        }
        allfiles.clear();
        if( conf.p_usecaching ) {
            ensureCachePaths();
            // check cached folder
            checkFolder(getCachePathFull(), cached);
            // check prepared folder
            //checkFolder(getCachePathFull(), prepared);
        }
        /*
        if( resourcepath != null ) {
            // get filelist from PFF
            if( extractor == null ) {
                extractor = new ZIPFile();
                extractor.setResourceFile(this);
                extractor.refreshFileList();
                allfiles.addAll(extractor.getFileList());
            }
        }
         */
        
        if( getDesignPathFull() != null ) {
            // there is a design path set, get the filelist from there also
            checkFolder(getDesignPathFull(), designed);
            // check if the file is newer, than what we have in other place
            for(int i=0, mx=allfiles.size(); i<mx; i++) {
                String fl = allfiles.get(i);
                if( designed.containsKey(fl)) {
                    // we have the file in designed aswell
                    // TODO
                    // check if cached is older than the designed, delete
                    // check if prepared is older than the designed, delete
                    // check if packed is older than the designed, delete from archive, add to archive
                }
            }
        }
    }
   
    public FastList<String> getFileList() {
        if( allfiles == null ) {
            readFileList();
        }
        return allfiles;
    }
    
    protected void addCached( String name, Object data ) {
        cached.put(name, data);
    }
    
    protected void addCached( TextureKey name, Texture data ) {
        cached.put(name, data);
    }
    
    public Object getCached(String name) {
        return cached.get(name);
    }
    
    public Texture getCached(TextureKey name) {
        return (Texture) cached.get(name);
    }
    
    public boolean isCached(String name) {
        String nm = name.toLowerCase();
        return cached.containsKey(nm);
    }
    
    public boolean isDesigned(String name) {
        return designed.containsKey(name.toLowerCase());
    }

    protected void savePrepared(String name, Object data ) {
        if( conf.p_createcache && data instanceof Buffer) {
            // save the file
            FileResource.save( getCachePathFull() + "/" + name, data );
        }
    } 

    protected ByteBuffer loadPrepared(String name, ParameterMap params) {
        ByteBuffer data = null;//prepared.get(name);
        //if( data == null ) {
            // load it from file, no need for direct buffer
        String fullname = getCachePathFull() + "/" + name;
        if(new File(fullname).exists()) {
            data = FileResource.load( fullname, params);
        }
            //if( conf.p_usememcache && data != null)
            //    prepared.put(name, data);
        //}
        return data;
 }
    
    /**
     * @param file
     * @return
     */
    protected URL getUrl(String file) {
        try {
            URL b = new File(isCached(file) ?
                this.getCachePathFull() : this.getDesignPathFull())
                .toURI().toURL();
            return new URL(b.getProtocol(),
                    b.getHost(),
                    b.getPort(),
                    b.getFile() + "/" + file);
        } catch (MalformedURLException ex) {
            logger.log(Level.SEVERE, "Unable to resolve file", ex);
        }
        return null;
    }
    
    protected URL getDesignUrl(String file) {
        try {
            URL b = new File(this.getDesignPathFull())
                .toURI().toURL();
            return new URL(b.getProtocol(),
                    b.getHost(),
                    b.getPort(),
                    b.getFile() + "/" + file);
        } catch (MalformedURLException ex) {
            logger.log(Level.SEVERE, "Unable to resolve file", ex);
        }
        return null;
    }
 
    
    protected ByteBuffer loadDesigned(String name, ParameterMap params) {
        ByteBuffer data = null;
                //prepared.get(name);
        //if( data == null ) {
            // load it from file, no need for direct buffer
            data = FileResource.load( getDesignPathFull() + "/" + name, params);
        //}
        return data;
    }

    // request a file, return true, if the file is availible
    // false, if this file cannot be retrieved from this folder
    // the procedure is to request all the files needed
    // then to fetch them
    // the convention is to use lower-case for file names
    public boolean requestFile(String name) {
        String nm = name.toLowerCase();
        if( conf.p_usecaching )
            if( cached.containsKey(nm) 
             //|| prepared.containsKey(name)
             )
                return true;
        if( conf.p_use_design_path) {
            // check the design path
            return isDesigned(nm);
        }
        return false;
    }
    
    public void clearMemory() {
        //prepared.clear();
        cached.clear();
        designed.clear();
        if(allfiles!=null)
            allfiles.clear();
    }
}
