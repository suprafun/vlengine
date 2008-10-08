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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.logging.Level;

/**
 * Represents a local filesystem folder
 * @author vear (Arpad Vekas)
 */
public class LocalFolder extends Folder {

    public static final String TEMP_SUFFIX = ".temp";
    
    protected File folder;

    @Override
    public boolean connect(String connectString, String username, String password) {
        folder = new File(connectString);
        if(!folder.exists() || !folder.isDirectory()) {
            // does not exists, or is not a folder
            folder = null;
            return false;
        }
        return true;
    }

    @Override
    public void disconnect() {
        folder = null;
    }

    @Override
    public InputStream getInputStream(Resource r) {
        String fullFile = folder.toString() + "/" + r.getTargetFilePath();
        try {
            InputStream is = new FileInputStream(fullFile);
            return is;
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public OutputStream getOutputStream(Resource r) {
        
        deleteFile(r);
        
        mkDirs(r);
        
        String fullFile = folder.toString() + "/" + r.getTargetFilePath();
        
        try {
            OutputStream os = new FileOutputStream(fullFile);
            return os;
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public ByteBuffer load(Resource r, ByteBuffer store) {
        try {
            FileChannel fis = ((FileInputStream)getInputStream(r)).getChannel();
            store.rewind();
            while (fis.position() < fis.size()) {
                fis.read(store);
            }
            fis.close();
            boolean readall = store.limit() == store.position();
            store.rewind();
            return readall ? store : null;
        } catch (Exception ex) {
        }
        return null;
    }

    @Override
    public boolean save(Resource r, ByteBuffer data) {
        try {
            data.position(0);
            FileChannel fos = ((FileOutputStream)getOutputStream(r)).getChannel();
            fos.write(data);
            fos.close();
            data.position(0);
            
            // mark the file date as in resource
            setFileDate(r);
            
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
    
    @Override
    public void getFileList(FastList<Resource> store) {
        // recursively go and collect all the files
        scan(folder, null, store);
        calcMD5(store);
    }

    @Override
    public void getFileList(HashSet<String> includes, FastList<Resource> store) {
        scan(folder, includes, store);
        calcMD5(store);
    }
    
    protected void calcMD5(FastList<Resource> store) {
        
        this.progress = 0;
        
        // calculate total size
        long totalSize = 0;
        for(int i=0; i<store.size(); i++) {
            Resource r=store.get(i);
            totalSize += r.getLength();
        }
        
        // go over and calculate MD5-s
        
        // calculate MD5 for the file
        long currentSize = 0;
        for(int i=0; i<store.size(); i++) {
            Resource r=store.get(i);
            r.calculateMD5();

            currentSize += r.getLength();
            progress = ((float)currentSize) / ((float)totalSize);
        }
    }

    protected void scan(File d, HashSet<String> includes, FastList<Resource> store) {
        File[] fileList = d.listFiles();
        if (fileList == null) {
            return;
        }
        
        for (int i = 0, s = fileList.length; i < s; ++i) {
            File file = fileList[i];
            
            String fName = file.getName().toLowerCase();
            
            // ignore some files
            if(".svn".equals(fName) 
                    || "workspace".equals(fName)
                    || "pack".equals(fName)
                    )
                continue;

            // if we already have it, ignore
            if (file.isDirectory()) {
                // scan subdirectory
                scan(file, includes, store);
            }
            else {
                
                if (fName.endsWith(TEMP_SUFFIX)) {
                    if (!file.delete()) {
                        
                    }
                }
                else if (file.length() <= Integer.MAX_VALUE) {
                    // we found a resource
                    // if we dont filter the list, or the list does contain
                    if(includes==null || includes.contains(fName)) {
                        String path = file.getPath().toLowerCase();

                        // remove the first part of the path
                        path = path.substring(folder.toString().length(), path.length());
                        path = path.replace('\\', '/');
                        if(path.startsWith("/")) {
                            path = path.substring(1);
                        }
                        if(path.length() > fName.length() + 1) {
                            path = path.substring(0, path.length() - fName.length() - 1);
                        } else {
                            path = "";
                        }

                        Resource res = new Resource();
                        res.setFolder(this);
                        res.setName(fName);
                        res.setTargetPath(path);

                        getFilesystemData(res);

                        // figure out type
                        res.guessType();

                        // add it to the list
                        store.add(res);
                    }
                }
            }
        }
    }

    public void getFilesystemData(Resource res) {
        File file = new File(folder.toString() + "/" + res.getTargetFilePath());
        res.setLength(file.length());
        res.setDate(file.lastModified());
    }

    @Override
    public void deleteFile(Resource r) {
        String fullFile = folder.toString() + "/" + r.getTargetFilePath();
        // delete previous file
        File ff = new File(fullFile);
        if(ff.exists()) {
            ff.delete();
        }
    }

    @Override
    public void mkDirs(Resource r) {
        String fullFile = folder.toString() + "/" + r.getTargetFilePath();
        // delete previous file
        File ff = new File(fullFile);
        // get folder part
        String path = ff.getParent();
        boolean mkdirs = new File(path).mkdirs();
    }

    @Override
    public void setFileDate(Resource r) {
        String fullFile = folder.toString() + "/" + r.getTargetFilePath();
        new File(fullFile).setLastModified(r.getDate());
    }

}
