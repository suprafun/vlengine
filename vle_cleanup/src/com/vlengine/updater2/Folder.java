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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A folder represents a virtual list of files which it contains
 * subclasses of this class implement different (file system, JAR, ZIP, FTP folders)
 * Note that a virtual Folder can represent a whole file system folder tree
 * @author vear (Arpad Vekas)
 */
public abstract class Folder {
    protected static final Logger log = Logger.getLogger(Folder.class.getName());
    
    protected float progress;
    protected boolean cancelled = false;
    protected byte[] buffer = new byte[10*1024];
    
    public Folder() {
    }

    /**
     * Connect to the folder. Before all operations are done
     * @param connectString     Folder type depedent string to connect to the folder
     * @return                  If conection was succesful
     */
    public abstract boolean connect(String connectString, String user, String password);

    public abstract void disconnect();

    public float getProgress() {
        return progress;
    }

    /**
     * Retrieve an input stream to a file in this folder
     * @param file
     * @return
     */
    public abstract InputStream getInputStream(Resource file);

    /**
     * Get output stream to a file
     * @param file
     * @return
     */
    public abstract OutputStream getOutputStream(Resource file);

    public ByteBuffer load(Resource r, ByteBuffer store) {
        cancelled = false;
        try {
            InputStream in = getInputStream(r);
            int maxLen = (int) r.getLength();
            int curRead = 0;

            store.rewind();

            int read = 0;
            while ((read = in.read(buffer)) != -1) {
                curRead += read;
                this.progress = ((float) curRead) / ((float) maxLen);

                if (cancelled) {
                    return null;
                }
                // put into store
                store.put(buffer, 0, read);

                Thread.yield();
            }
            in.close();
            return store;
        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public boolean save(Resource r, ByteBuffer data) {
        cancelled = false;
        try {
            
            deleteFile(r);
            
            mkDirs(r);
            
            OutputStream os = getOutputStream(r);
            int maxLen = data.limit();
            int curSaved = 0;
            int saved = 0;
            data.rewind();
            
            while (curSaved < maxLen) {
                data.get(buffer);
                saved = buffer.length;
                if(saved + curSaved > maxLen) {
                    saved = maxLen - curSaved;
                }
                
                os.write(buffer, 0, saved);
                
                curSaved += saved;
                
                this.progress = ((float) curSaved) / ((float) maxLen);

                if (cancelled) {
                    os.close();
                    deleteFile(r);
                    return false;
                }

                Thread.yield();
            }
            
            os.close();
            return true;

        } catch (Exception ex) {
            log.log(Level.SEVERE, "Cannot save file "+r.getName(), ex);
            return false;
        }
    }
    
    /**
     * Copy the give file to the output stream
     * @param file
     * @param os
     * @return
     */
    public boolean copy(Resource r, OutputStream os) {
        cancelled = false;
        try {
            InputStream in = getInputStream(r);
            if(in==null) {
                return false;
            }
            int curRead = 0;
            int maxLen = (int) r.getLength();

            int read = 0;
            while ((read = in.read(buffer)) != -1) {
                curRead += read;
                this.progress = ((float) curRead) / ((float) maxLen);

                if (cancelled) {
                    in.close();
                    return false;
                }
                // write to output stream
                os.write(buffer, 0, read);

                Thread.yield();
            }
            in.close();
            return true;
        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * Copy resource from this folder to the other folder
     * @param r
     * @param other
     * @return
     */
    public boolean copy(Resource r, Folder target) {
        // open output stream
        OutputStream os = target.getOutputStream( r );
        if(os==null) {
            return false;
        }
        // copy file
        boolean saved = copy(r, os);
        
        try {
            // close stream
            os.close();
        } catch (Exception ex) {
            return false;
        }
        if(saved) {
            // set modification date on the target file
            target.setFileDate(r);
        }
        return saved;
    }

    /**
     * Retrieve all files in the given folder
     * @param store
     */
    public abstract void getFileList(FastList<Resource> store);
    
    public abstract void getFileList(HashSet<String> includes, FastList<Resource> store);

    public void cancell() {
        this.cancelled = true;
    }
    
    public abstract void deleteFile(Resource r);
    
    public abstract void mkDirs(Resource r);
    
    public abstract void setFileDate(Resource r);
}
