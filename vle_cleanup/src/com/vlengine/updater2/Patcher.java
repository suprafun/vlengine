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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Makes a patch file based on a file list
 * @author vear (Arpad Vekas)
 */
public class Patcher {

    protected static final Logger log = Logger.getLogger(Patcher.class.getName());
    
    // the local folder we are working on
    protected Folder localFolder;
    
    // the server folder we will be uploading the patch to
    protected Folder serverFolder;
    
    // the folder from which we are getting the status
    protected Folder statusFolder;
    
    // what are we currently doing?
    protected String statusText;
    
    protected boolean finished =false;
    
    protected float totalProgress = 0;
    
    protected boolean cancelled = false;

    public boolean setupLocalToFTP(String localFolder, String ftpServer, String ftpUser, String ftpPassword) {
        this.localFolder = new LocalFolder();
        if(!this.localFolder.connect(localFolder, null, null)) {
            return false;
        }
        this.serverFolder = new FtpFolder();
        return this.serverFolder.connect(ftpServer, ftpUser, ftpPassword);
    }

    public boolean setupLocalToLocal(String sourceFolder, String targetFolder) {
        this.localFolder = new LocalFolder();
        if(!this.localFolder.connect(sourceFolder, null, null)) {
            return false;
        }
        this.serverFolder = new LocalFolder();
        return this.serverFolder.connect(targetFolder, null, null);
    }

    public void finished() {
        if(localFolder!=null) {
            localFolder.disconnect();
        }
        if(serverFolder!=null) {
            serverFolder.disconnect();
        }
    }

    public void cancel() {
        cancelled = true;
        if(localFolder!=null) {
            localFolder.cancell();
        }
        if(serverFolder!=null) {
            serverFolder.cancell();
        }
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
    
    // returns the total progress
    public float getTotalProgress() {
        return finished?-1:totalProgress;
    }

    public String getStatus() {
        return statusText;
    }

    public float getProgress() {
        if(statusFolder!=null) {
            return statusFolder.getProgress();
        }
        // TODO:
        return 0;
    }

    public void setLocalFolder(Folder f) {
        localFolder = f;
    }

    public void setServerFolder(Folder f) {
        serverFolder = f;
    }

    public void createPatch(HashSet<String> fileList) {
        FastList<String> files = null;
        if(fileList!=null) {
            files = new FastList<String>();
            for(String s:fileList) {
                files.add(s);
            }
        }
        createPatch(files);
    }

    public void createPatch(FastList<String> fileList) {
        finished = false;

        // create the index file
        Index idxSource = new Index();
        idxSource.setName("editor");
        idxSource.setFolder(localFolder);
        
        HashSet<String> files = null;
        if(fileList!=null) {
            files = new HashSet<String>();

            // go over the files
            for(int i=0; i<fileList.size(); i++) {
                String f = fileList.get(i).toLowerCase();

                // if we have a path, strip it
                f = f.replace('\\', '/');
                int sp = f.lastIndexOf('/');
                if(sp!=-1) {
                    f = f.substring(sp+1);
                }

                // if it ends with png add an vlt to it
                if(f.endsWith(".png")
                    || f.endsWith(".jpg")) {
                    f += ".vlt";
                }

                // put it into the set
                files.add(f);
            }
        }

        FastList<Resource> sres = new FastList<Resource>();
        
        // create the index
        statusText = "Creating file index";
        statusFolder = localFolder;
        // create the index, with only the given files in it
        localFolder.getFileList(files, sres);
        for(int i=0; i<sres.size(); i++) {
            Resource r = sres.get(i);
            if(r.getTargetPath().startsWith("workspace") || r.getTargetPath().startsWith("pack")) {
                sres.remove(i);
                i--;
            }
        }
        statusFolder = null;
        
        if(sres.size() == 0) {
            finished = true;
            return;
        }

        if(cancelled) {
            finished = true;
            return;
        }
        
        // add the resources to the index
        idxSource.addResources(sres);

        statusText = "Loading server index";
        statusFolder = serverFolder;
        // load the server index
        Index idxTarget = new Index();
        idxTarget.setName("server");
        idxTarget.setFolder(serverFolder);
        idxTarget.loadIndex();
        statusFolder = null;

        if(cancelled) {
            finished = true;
            return;
        }

        // create patch index
        Index idxPatch = new Index();
        String pname = "patch_"+System.currentTimeMillis()+".zip";
        idxPatch.setName(pname);
        // create it as a diff
        idxPatch.createUpdateList(idxSource, idxTarget);
        
        if(idxPatch.getResources().size() == 0) {
            // nothing to patch
            finished = true;
            return;
        }

        // create the zip resource
        Resource zipR = new Resource();
        zipR.setFolder(localFolder);
        zipR.setName(pname);
        zipR.setTargetPath("pack");

        // create the zip folder
        ZipFolder zf = new ZipFolder();
        zf.setFile(zipR);
        zf.setParentFolder(localFolder);
        zf.connect(null, null, null);

        // go over and copy files in the list to the ZIP
        // set status
        statusText = "Packing files";
        long totalSize = 0;
        
        // calculate total size
        for(Resource r:idxPatch.getResources()) {
            totalSize += r.getLength();
        }
        
        long currentSize = 0;
        
        // set local folder as progress 
        statusFolder = localFolder;
        
        for(Resource r:idxPatch.getResources()) {
            localFolder.copy(r, zf);
            currentSize += r.getLength();
            totalProgress = ((float)currentSize) / ((float)totalSize);
            
            if(cancelled) {
                finished = true;
                return;
            }
        }

        // close the ZIP
        zf.disconnect();
        statusFolder = null;

        if(cancelled) {
            finished = true;
            return;            
        }
        
        // upload the zip to the server
        statusText = "Uploading patch";
        totalProgress = 0;
        
        statusFolder = localFolder;
        localFolder.copy(zipR, serverFolder);
        statusFolder = null;
        
        // update resources to be known to be in the ZIP
        String zipFull = zipR.getTargetFilePath();
        for(Resource r:idxPatch.getResources()) {
            r.setZipFile(zipFull);
            // update the server index with the files in the zip
            idxTarget.addResource(r);
        }
        
        // add the ZIP file itself to the server index
        idxTarget.addResource(zipR);
        
        if(cancelled) {
            finished = true;
            return;            
        }
        
        statusText = "Uploading index";
        totalProgress = 0;
        // upload the index to the server
        statusFolder = serverFolder;
        if(!idxTarget.saveIndex()) {
            cancelled = true;
            statusFolder = null;
            return;
        }
        statusFolder = null;
        
        finished = true;
    }
    
    public void applyPatches() {
        finished = false;
        
        // load the server index
        statusText = "Loading server index";
        statusFolder = serverFolder;
        Index idxServer = new Index();
        idxServer.setName("server");
        idxServer.setFolder(serverFolder);
        idxServer.loadIndex();
        statusFolder = null;
        
        if(cancelled) {
            return;            
        }
        
        // load/create local index
        statusText = "Loading/creating local index";
        statusFolder = localFolder;
        Index idxClient = new Index();
        idxClient.setName("client");
        idxClient.setFolder(localFolder);
        idxClient.loadIndex();
        statusFolder = null;
        
        if(cancelled) {
            return;            
        }
        
        // create patchable files
        Index idxPatch = new Index();
        String pname = "patch";
        idxPatch.setName(pname);
        // create it as a diff
        idxPatch.createUpdateList(idxServer, idxClient);
        
        if(idxPatch.getResources().size() == 0) {
            // nothing to patch
            statusText = "No patching needed";
            finished = true;
            return;
        }
        
        // get all the required ZIP file names
        //HashSet<String> requiredZips = new HashSet<String>();
        
        // all the extracteable zips by the zip they are in
        HashMap<String, HashMap<String, Resource>> extractable = new  HashMap<String, HashMap<String, Resource>>();
        
        // get all the zips available for download
        // which are not yet in our local patch folder
        HashMap<String,Resource> downloadZips = new HashMap<String,Resource>();
        
        // get the list of files
        Collection<Resource> crl = idxPatch.getResources();
        for(Resource r: crl) {
            
            if(r.isFlag(Resource.Flag.ZIPFile)) {
                // this is a zip file
                String zpath = r.getTargetFilePath();
                downloadZips.put(zpath, r);
            } else {
                // this is a resource we will need
                
                // put it into proper bucket
                String zip = r.getZipFile();
                
                HashMap<String, Resource> zipres = extractable.get(zip);
                if(zipres==null) {
                    zipres = new HashMap<String, Resource>();
                    extractable.put(zip, zipres);
                }
                zipres.put(r.getName(), r);
                
            }
        }

        // download ZIP patches
        int numZips = extractable.size();
        if(numZips>0) {
            statusText = "Downloading patches";
            statusFolder = serverFolder;
            int numZip = 0;
            // go over the zips which need to be downloaded
            for(String zn: extractable.keySet()) {
                // get the resource from the downloadable
                Resource rz = downloadZips.get(zn);
                if(rz!=null) {
                    statusText = "Downloading patch: "+rz.getName();
                    // we gotta download this file
                    if(!serverFolder.copy(rz, localFolder)) {
                        // download did not success
                        statusText = "Failed downloading patch";
                        cancelled = true;
                        return;
                    }
                    // add the zip resource to local index
                    idxClient.addResource(rz);
                }
                if(cancelled) {
                    return;            
                }
                numZip ++;
                this.totalProgress = ((float)numZip) / ((float)numZips);
            }
            statusFolder = null;
        }
        
        int numFiles = 0;
        // calculate the total number of files
        for(HashMap<String, Resource> f: extractable.values()) {
            numFiles += f.size();
        }
        
        int numFile = 0;
        
        // go over and extract required files from local zips
        statusText = "Extracting files";
        for(String zp: extractable.keySet()) {
            
            // get it from downloaded
            Resource zr = downloadZips.get(zp);
            if(zr==null) {
                // try to get it from local index
                zp.replace('\\', '/');
                int sppos = zp.lastIndexOf('/');
                if(sppos>0) {
                    zp = zp.substring(sppos+1);
                }
                // we got the zip name, try to get it from local index
                zr = idxClient.getResource(zp);
            }
            
            if(zr == null) {
                // its bad, we still dont have the zip
                statusText = "Missing patch file "+zp;
                cancelled=true;
                return;
            }
            
            // mount the zip folder
            ZipFolder zf = new ZipFolder();
            zf.setFile(zr);
            zf.setParentFolder(localFolder);
            zf.connect(null, null, null);
            // read in the file list of the zip, ordered properly, filtered for files we need
            FastList<Resource> zipRess = new FastList<Resource>();
            zf.getFileList(zipRess);
            
            if(cancelled) {
                return;            
            }

            // get the list of files to extract
            HashMap<String, Resource> zipFilt = extractable.get(zp);
            
            
            // go over the file list, and extract the files we need
            for(int i=0; i<zipRess.size(); i++) {
                Resource rzipped = zipRess.get(i);
                Resource r = zipFilt.get(rzipped.getName());
                if(r!=null) {
                    // we need to extract this file
                    statusText = "Extracting file: "+r.getName();
                    statusFolder = zf;
                    // copy out the file from the zip
                    if(!zf.copy(r, localFolder)) {
                        // error extracting
                        statusText = "Error unpacking file from "+zr.getName();
                        cancelled = true;
                        return;
                    }

                    if(cancelled) {
                        return;            
                    }

                    // update progress
                    numFile ++;
                    this.totalProgress = ((float)numFile) / ((float)numFiles);
                    // set status to empty
                    statusFolder = null;
                    
                    // remove it from the map, to know if sometinhg is missing
                    zipFilt.remove(r.getName());
                    // add it to local index
                    idxClient.addResource(r);
                }
            }
            
            zf.disconnect();
            
            if(zipFilt.size() > 0) {
                // we did not manage to extract all files we needed?
                statusText = "Error extracting all files from "+zr.getName();
                cancelled = true;
                return;
            }

            
        }
        
        // save the local index
        statusText = "Saving index";
        statusFolder = localFolder;
        try {
            idxClient.saveIndex();
        } catch(Exception e) {
            log.log(Level.WARNING, "Exception in saving local index", e);
        }
        statusFolder = null;
        
        statusText = "Finished update";

        finished = true;
    }
}
