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

package com.vlengine.updater2;

import com.vlengine.util.FastList;
import com.vlengine.util.xml.Element;
import com.vlengine.util.xml.XMLFile;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;

/**
 * Class representing an index file, which holds a list of resources.
 * Update operations are processed using index files.
 * @author vear (Arpad Vekas)
 */
public class Index {
    
    // the name of the index file
    protected String name;
    
    // the resource index file
    protected Resource indexFile;
    
    // the resource files this index file contains
    protected HashMap<String, Resource> resources = new HashMap<String, Resource>();
    
    // the folder this index is in
    protected Folder folder;

    public Index() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Resource getResource(String name) {
        return resources.get(name);
    }

    public void addResource(Resource res) {
        resources.put(res.getName(), res);
    }

    public void addResources(FastList<Resource> ress) {
        for(int i=0; i<ress.size(); i++) {
            Resource res=ress.get(i);
            resources.put(res.getName(), res);
        }
    }
    
    public Collection<Resource> getResources() {
        return resources.values();
    }
    
    public void setFolder(Folder f) {
        this.folder = f;
    }

    // creates this index as list of updatable resources
    public void createUpdateList(Index source, Index target) {
        Collection<Resource> sources = source.getResources();
        // find all that are in the source, but not in the target
        for(Resource r : sources) {
            Resource tr = target.getResource(r.getName());
            if(tr == null   // local file does not exists
               || !r.getMD5hash().equals(tr.getMD5hash())  // local file is different
               || r.delete // remote file is marked for deletion
                    ) {
                // this is not in the target, add it as updatable
                resources.put(r.getName(), r);
            }
        }
    }
    
    // create this index as list of removable resources, which do not exists
    // in the source
    public void createRemoveList(Index source, Index target) {
        Collection<Resource> targets = target.getResources();
        
        // if we are called as synch, then mark as deletable everything we dont find in source
        for(Resource r : targets) {
            Resource sr = source.getResource(r.getName());
            if(sr == null) {
                // the resource does not exists in the source any more
                // mark is as deletable
                r.delete = true;
                resources.put(r.getName(), r);
            }
        }
    }

    protected void createIndexResource() {
        if(indexFile==null) {
            indexFile=new Resource();
            indexFile.setName(name+".idx.gz");
        }
    }

    public void loadIndex() {
        // create the index resource if not yet created
        createIndexResource();
        
        // load the index file as resource
        InputStream is = folder.getInputStream(indexFile);
        // read in the XML
        Element e = null;
        if(is != null)
            e = XMLFile.fromXML(is, true);
        if( e==null ) {
            // we need to index the folder manualy
            if(!(folder instanceof FtpFolder))
                createIndex();
        } else {
            // process the XML
            FastList<Element> resList = e.getChildren();
            for(int i=0; i<resList.size(); i++) {
                Element re = resList.get(i);
                // create resource
                Resource r = new Resource();
                r.load(re);
                resources.put(r.getName(), r);
            }
        }
    }

    public void createIndex() {

        FastList<Resource> zips = new FastList<Resource>();

        // load the folder contents list
        FastList<Resource> list = new FastList<Resource>();
        folder.getFileList(list);
        for(int i=0; i<list.size(); i++) {
            Resource r = list.get(i);
            // if its the index file itself, ignore it
            if(r.isFlag(Resource.Flag.FolderIndex))
                continue;
            // if its a zip, mark for postprocess
            // but only if we fond it in pack subfolder
            if(r.isFlag(Resource.Flag.ZIPFile) && "pack".equals(r.getTargetPath())) {
                zips.add(r);
            } else {
                // add the resources to the map
                resources.put(r.getName(), r);
            }
        }
        
        FastList<Resource> zipfiles = new FastList<Resource>();
        // process the zips
        for(int i=0; i<zips.size(); i++) {
            Resource rz = zips.get(i);
            // load the ZIP contents
            ZipFolder zf = new ZipFolder();
            zf.setParentFolder(folder);
            zf.setFile(rz);
            zf.connect(null, null, null);

            zipfiles.clear();
            // get the list of resources
            zf.getFileList(zipfiles);
            
            // go over the files in the zip, check if we have in the extracte, which is older than
            // the file in the ZIP
            for(int j=0; j<zipfiles.size(); j++) {
                Resource r= zipfiles.get(j);
                
                // check if we got the file
                Resource gotr=resources.get(r.getName());
                
                // check its same
                if(!gotr.getMD5hash().equals(r.getMD5hash())) {
                    // is the ZIP file newer that the local file?
                    if(r.getDate()>gotr.getDate()) {
                        // replace our file with that in the ZIP
                        resources.put(r.getName(), r);
                    }
                }
            }
        }
    }

    public boolean saveIndex() {
        // put it into the root of the folder
        Element e = new Element("resources");
        Collection<Resource> resList = resources.values();
        for(Resource r:resList) {
            e.addContent(r.save());
        }
        
        // create the index resource if not yet created
        createIndexResource();

        // open an output stream to the file
        OutputStream os = folder.getOutputStream(indexFile);
        if(os==null)
            return false;
        // save the XML
        return XMLFile.toXML(os, e, true);
    }
}
