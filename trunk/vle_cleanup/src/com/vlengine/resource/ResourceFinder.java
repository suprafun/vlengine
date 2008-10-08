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
import com.vlengine.audio.AudioTrack;
import com.vlengine.image.Image;
import com.vlengine.image.Texture;
import com.vlengine.renderer.material.MatParameters;
import com.vlengine.renderer.material.MaterialLib;
import com.vlengine.renderer.material.Material;
import com.vlengine.scene.animation.MD5.MD5BoneAnimation;
import com.vlengine.resource.model.Model;
import com.vlengine.resource.model.ModelPack;
import com.vlengine.resource.obj.ObjMtlLib;
import com.vlengine.resource.obj.ObjModel;
import com.vlengine.util.FastList;
import com.vlengine.util.xml.Element;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.logging.Logger;

/**
 * Maintains the list of all the resources. This is the main class to request
 * resources from. It will try to find the folder containing the resource,
 * convert the resource if needed and return it to the requester application.
 * 
 * @author vear (Arpad Vekas)
 */
public class ResourceFinder {
    private static final Logger logger = Logger.getLogger(ResourceFinder.class.getName());
    
    public static final int RESOURCE_FILE = 0;
    public static final int RESOURCE_IMAGE = 1;
    public static final int RESOURCE_TEXTURE = 2;
    public static final int RESOURCE_TYPELIST = 3;
    public static final int RESOURCE_MODEL = 4;
    public static final int RESOURCE_MODELPACK = 5;
    public static final int RESOURCE_MTLLIB = 6;
    public static final int RESOURCE_ANIMATION = 7;
    public static final int RESOURCE_AUDIO = 8;
    public static final int RESOURCE_XML = 9;
    
    /** list of all the folders to be searched */
    private final FastList<ResourceFolder> folders = new FastList<ResourceFolder>();

    /** the material libraryes, linked together */
    private MaterialLib matlib;
    // the items registryes
    //private FastList<ObjectStore> items;
    // the object creation pipeline
    private ResourceCreator roc;
    
    private Config conf;
    private AppContext app;

    private static class FolderComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            return o1 instanceof ClassPathResource ? +1 :
                    ((ResourceFolder)o1).getId().length() - ((ResourceFolder)o2).getId().length();
        }
    }
    
    private static FolderComparator folderSorter = new FolderComparator();
    
    public ResourceFinder(AppContext app) {
        conf = app.conf;
        this.app = app;
    }
    
    public void setResourceCreator(ResourceCreator roc) {
        this.roc = roc;
        roc.setAppContext(app);
    }

    // finds the folder containing a given resource
    protected ResourceFolder getFolder(String folderName) {
        for(int i = 0, m=folders.size(); i<m; i++) {
            ResourceFolder fl=folders.get(i);
            if(folderName.equals(fl.getId())) {
                return fl;
            }
        }
        return null;
    }

    // find all resource folder files
    private void enumResFiles( String root, String sub ) {
        String dirn= root + sub;
        // find all the PFF files
        File dir=new File( dirn );
        File [] drl = dir.listFiles();
        if( drl != null ) {
            for( int i=0; i<drl.length; i++ ) {
                String drf = drl[i].getName().toLowerCase();
                String fid = sub + "/" + drf;
                if( drf.endsWith(".zip") && !drf.equals(".zip") ) {
                    // pff file
                    ResourceFolder rff = getFolder(fid);
                    if( rff == null ) {
                        // register new resource file
                        rff = new ResourceFolder(this.app);
                        rff.setId(fid);
                        // extract the mod name
                        String mod = fid.substring(0, fid.lastIndexOf('/'));
                        rff.setMod(mod);
                        folders.add( rff );
                    }
                    rff.setResourcePath( fid );
                } else if( drl[i].isDirectory() ) {
                    // process subfolders
                    enumResFiles( root, fid );
                }
            }
        }
    }
    
    private void enumCacheFiles( String root, String sub ) {
        String dirn= root + sub;
        // find all the PFF files
        File dir=new File( dirn );
        File [] drl = dir.listFiles();
        if( drl != null ) {
            for( int i=0; i<drl.length; i++ ) {
                String drf = drl[i].getName().toLowerCase();
                String fid = sub + "/" + drf;
                if( drl[i].isDirectory() && !".svn".equals(drf)) {
                    // pff file
                    ResourceFolder rff=getFolder(fid);
                    if( rff == null ) {
                        // register new resource file
                        rff = new ResourceFolder(this.app);
                        rff.setId(fid);
                        folders.add( rff );
                    }
                    rff.setCachePath( fid );
                //} else if( drl[i].isDirectory() ) {
                    // process subfolders
                    enumCacheFiles( root, fid );
                }
            }
        }
    }
    
    private void enumDevFiles( String root, String sub ) {
        String dirn= root + sub;
        // find all the PFF files
        File dir=new File( dirn );
        File [] drl = dir.listFiles();
        if( drl != null ) {
            for( int i=0; i<drl.length; i++ ) {
                String drf = drl[i].getName().toLowerCase();
                String fid = sub + "/" + drf;
                if( drl[i].isDirectory() && !".svn".equals(drf)) {
                    // pff file
                    ResourceFolder rff=getFolder(fid);
                    if( rff == null ) {
                        // register new resource file
                        rff = new ResourceFolder(this.app);
                        rff.setId(fid);
                        folders.add( rff );
                    }
                    rff.setDesignPath(fid);
                //} else if( drl[i].isDirectory() ) {
                    // process subfolders
                    enumDevFiles( root, fid );
                }
            }
        }
    }
    
    // tryes to find all the resource files and all the
    // cache folders, matches files to cache folders
    public void refreshResFiles() {
        // clear previous folders list
        folders.clear();
        // enumerate all folders under the root resource
        String resroot = conf.res_path;
        // do not create cache for pack folder
        /*
        if( resroot != null && !resroot.equals("") ) {
            enumResFiles( resroot, "" );
        }
         */
        // create a resource folder for files on classpath
        addClassPathFolder("/vlengine", "com/vlengine/data", "");
        
        // enumerate all cached folders
        String cacheroot = conf.p_usecaching ? conf.cache_path : null;
        if( cacheroot != null && !cacheroot.equals("") ) {
            enumCacheFiles( cacheroot, "" );
        }
        String devroot = conf.p_use_design_path ? conf.design_path : null;
        if( devroot != null && !devroot.equals("") ) {
            enumDevFiles( devroot, "" );
        }
        
        // refresh all the file lists in folders
        for( int i=0, m=folders.size(); i<m; i++ ) {
            folders.get(i).readFileList();
        }
        // sort resource folders by length of their id
        folders.sort(folderSorter);
        
        // enumerate and parse all the items definitions
        /*
        if(items==null)
            items=new FastList();
        items.clear();
        // loop trough all the folders, and parse all the item definition files
        for(int i=0, mx=folders.size(); i<mx; i++) {
            ResourceFolder fol = folders.get(i);
            if(fol.requestFile(ObjectStore.OBJECT_TYPELIST)) {
                // folder contains a def file, get it
                ObjectStore st = (ObjectStore) getResource(ObjectStore.OBJECT_TYPELIST, RESOURCE_TYPELIST, ParameterMap.MAP_EMPTY, fol);
                if( st != null ) {
                    items.add(st);
                }
            }
        }
         */
    }

    /**
     * Add a folder in a JAR file on the classpath to the resource searching.
     * This is needed to find (for example) textures that are packed with
     * the application. For example, the folder to find resources needed by the
     * engine itself is created as follows:
     * <code>
     * addClassPathFolder("/vlengine", "com/vlengine/data", "");
     * </code>
     * @param id        Unique id of the folder
     * @param folder    The actual classpath folder
     * @param mod       The optional mod parameter of the folder, if given,
     *                  only resources specificaly requested from the named mod
     *                  are returned.
     */
    public void addClassPathFolder(String id, String folder, String mod) {
        ClassPathResource fol = new ClassPathResource(this.app);
        fol.setId(id);
        fol.setCachePath(id);
        fol.setMod(mod);
        fol.setResourcePath(folder);
        folders.add( fol );
        /*
        // read in the list of game objects from the folder
        if(fol.requestFile(ObjectStore.OBJECT_TYPELIST)) {
                // folder contains a def file, get it
                ObjectStore st = (ObjectStore) getResource(ObjectStore.OBJECT_TYPELIST, RESOURCE_TYPELIST, ParameterMap.MAP_EMPTY, fol);
                if( st != null ) {
                    items.add(st);
                }
            }
         */
    }
    
    public FastList<ResourceFolder> getFolderList() {
        return folders;
    }
    
    /*
    public FastList<ObjectStore> getObjectLists() {
        return items;
    }
     */
    
    // add a renderlib to serve request for materials
    public void addRenderLib( MaterialLib lib ) {
        // if we have already a matlib, set it as fallback
        // library to the new one
        if(matlib != null )
            lib.setParent(matlib);
        // and set the new lib as the first we ask for materials
        matlib = lib;
    }
    
    // finds the folder containing a given resource
    protected ResourceFolder findResource(String resourceName, ParameterMap params) {
        String mod = ParameterMap.getModName(params);
        //String name = resourceName.toLowerCase();
        String nm = getConvertedName(resourceName);
        for(int i = 0, m=folders.size(); i<m; i++) {
            ResourceFolder fl=folders.get(i);
            if((mod==null || mod.equals(fl.getMod())) &&
               ( fl.requestFile(resourceName) || fl.requestFile(nm))) {
                return fl;
            }
        }
        return null;
    }
    
    // request a resource
    public boolean requestResource(String name, ParameterMap params) {
        ResourceFolder rf = findResource(name, params);
        return rf != null;
    }
    
    // get image
    public Image getImage(String name, ParameterMap params) {
        ResourceFolder rf = findResource(name, params);
        if( rf != null ) {
            return  (Image)getResource(name, RESOURCE_IMAGE, params, rf);
        }
        return null;
    }

    // get a texture
    public Texture getTexture(String name, ParameterMap params) {
        ResourceFolder rf = findResource(name, params);
        if( rf != null ) {
            return  (Texture)getResource(name, RESOURCE_TEXTURE, params, rf);
        }
        return null;
    }
    
    public ByteBuffer getFile(String name, ParameterMap params ) {
        ResourceFolder rf = findResource(name, params);
        if(rf != null ) {
            return (ByteBuffer)getResource(name, RESOURCE_FILE, params, rf);
        }
        return null;
    }

    public Model getModel(String name, ParameterMap params) {
        ResourceFolder rf = findResource(name, params);
        if( rf != null ) {
            Model obj = (Model) getResource(name, RESOURCE_MODEL, params, rf);
            return obj;
        }
        return null;
    }
    
    public ModelPack getModelPack(String name, ParameterMap params) {
        if(!name.endsWith(".pack.gz"))
            name = name + ".pack.gz";
        ResourceFolder rf = findResource(name, params);
        if( rf != null ) {
            ModelPack obj = (ModelPack) getResource(name, RESOURCE_MODELPACK, params, rf);
            return obj;
        }
        return null;
    }

    public ObjMtlLib getObjMaterialLib(String name, ParameterMap params) {
        ResourceFolder rf = findResource(name, params);
        if( rf != null ) {
            ObjMtlLib obj = (ObjMtlLib) getResource(name, RESOURCE_MTLLIB, params, rf);
            return obj;
        }
        return null;
    }
    
    public AudioTrack getAudioTrack(String name, ParameterMap params) {
        ResourceFolder rf = findResource(name, params);
        if( rf != null ) {
            AudioTrack track = (AudioTrack) getResource(name, RESOURCE_AUDIO, params, rf);
            return track;
        }
        return null;
    }

    public Material getMaterial(String name, MatParameters params) {
        if(matlib==null) {
            logger.severe("Material library is not set!");
            return null;
        }
        return matlib.getMaterial(name, params);
    }
    
    public MD5BoneAnimation getAnimation(String name, ParameterMap params) {
        ResourceFolder rf = findResource(name, params);
        if( rf != null ) {
            MD5BoneAnimation obj = (MD5BoneAnimation) getResource(name, RESOURCE_ANIMATION, params, rf);
            return obj;
        }
        return null;
    }
    
    public Element getXML(String name, ParameterMap params) {
        ResourceFolder rf = findResource(name, params);
        if( rf != null ) {
            Element obj = (Element) getResource(name, RESOURCE_XML, params, rf);
            return obj;
        }
        return null;
    }
    
    /*
    public TerrainInfo getTerrainInfo(String name, ParameterMap params) {
        ResourceFolderNL rf = findResource(name, params);
        if( rf != null ) {
            return  (TerrainInfo)rf.getResource(name, params);
        }
        return null;
    }
     */

    /*
    public ObjectInfo getObjectInfo(int type_id, ParameterMap params) {
        String mod = ParameterMap.getModName(params);
        for(int i=0, mx=items.size(); i<mx; i++) {
            ObjectStore st = items.get(i);
            if( mod==null || mod.equals(st.getFolder().getMod())) {
                ObjectInfo mi = st.getModelInfo(type_id);
                if(mi!=null)
                    return mi;
            }
        }
        return null;
    }
     */
    
    public static String getConvertedName(String name) {
        String nm = name.toLowerCase();
        if( nm.endsWith(".mdt") ) {
            nm += ".tga";
        }
        if( nm.endsWith(".png") 
            || nm.endsWith(".jpg") 
            || nm.endsWith(".tga")
            || nm.endsWith(".bmp")
                ) {
            nm += ".vlt";
        }
        return nm;
    }
    /*
    private ByteBuffer extractUrlResource(URL url, String name, ParameterMap parameters, ResourceFolder rf) {
        ByteBuffer data = UrlResource.load(url, parameters);
        if( data != null ) {
            rf.addPrepared(name, data);
        }
        return data;
    }
     */

    protected synchronized Object getResource( String name, int type, ParameterMap parameters, ResourceFolder rof ) {
        // name contains the file name as requested by the application
        // nm contains the name as stored on disk and cache, but not in the resource file

        String nm = getConvertedName(name);
        
        Object reto=null;
        boolean step = true;
        while ( step ) {
            step =false;
            // load resource from native format
            
            // create resource
            if( //rof.isPrepared(nm)
                //|| 
                rof.isCached(nm) ) {
                // the prepared folder contains the file, decompress it
                reto = roc.createResource( nm, type, parameters, rof );
                return reto;
            }
            
            if( //rof.isPrepared(name)
                //|| 
                rof.isCached(name) ) {
                // the prepared folder contains the file, decompress it
                reto = roc.createResource( name, type, parameters, rof );
                return reto;
            }
            
            // get from resource pack file
            /*
            if( rof.isExtractable(nm)) {
                // we found the already converted resource
                // save it to prepared
                roc.convertResource(nm, nm, type, parameters, rof);

                // extract was succesfull
                if( //rof.isPrepared(nm)
                    //|| 
                    rof.isCached(nm)
                        ) {
                    step = true;
                }
            } else if( rof.isExtractable(name)) {
                // we found the unconverted resource
                // convert it
                roc.convertResource(name, nm, type, parameters, rof);
                // extract was succesfull
                if( //rof.isPrepared(name)
                    //|| 
                    rof.isCached(name)
                    //|| rof.isPrepared(nm)
                    || rof.isCached(nm)
                        ) {
                    step = true;
                }
            } else*/ if(rof.isDesigned(name)) {
                // we found it in design folder, need to convert it
                roc.convertResource(name, nm, type, parameters, rof);
                // convert was succesfull?
                if( //rof.isPrepared(nm)
                   //|| 
                   rof.isCached(nm)
                   //|| rof.isPrepared(name)
                   || rof.isCached(name)
                        ) {
                    step = true;
                }
            }
        }
        return null;
    }
    
    public void cleanup() {
        // go trough all the folders
        for(int i=0; i<folders.size(); i++) {
            ResourceFolder rf = folders.get(i);
            // cleanup (delete texture id-s)
            roc.cleanup(rf);
            // release all object references
            rf.clearMemory();
        }
    }
}
