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

import com.vlengine.system.VleException;
import com.vlengine.util.FastList;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Represents a ZIP file, with resources packed into it
 * @author vear (Arpad Vekas)
 */
public class ZipFolder extends Folder {

    // the parent folder
    protected Folder parentFolder;
    // the resource in the parent folder that represents this ZIP file
    protected Resource zipFile;
    
    // the list of resources in the ZIP in order they appear in the zip
    protected FastList<Resource> resourceList = new FastList<Resource>();
    
    // input stream to the ZIP file
    protected ZipInputStream zipIn;
    // the output stream to ZIP file
    protected ZipOutputStream zipOut;
    
    // the current entry in the zip
    protected int currentEntryIndex = -1;
    protected ZipEntry currentEntry;
    
    public void setParentFolder(Folder parent) {
        this.parentFolder = parent;
    }
    
    public void setFile(Resource zip) {
        this.zipFile = zip;
    }
    
    @Override
    public boolean connect(String connectString, String user, String password) {
        disconnect();
        
        
        return true;
    }

    @Override
    public void disconnect() {
        if(zipIn!=null) {
            try {
                zipIn.close();
            } catch (IOException ex) {
            }
            zipIn = null;
        }
        if(zipOut!=null) {
            try {
                if(currentEntry!=null) {
                    zipOut.closeEntry();
                }
                zipOut.close();
            } catch (IOException ex) {
            }
            zipOut = null;
            // set length, modification date, calculate MD5 for the ZIP
            if(parentFolder instanceof LocalFolder) {
                ((LocalFolder)parentFolder).getFilesystemData(zipFile);
            }
            zipFile.calculateMD5();
        }
        currentEntryIndex = -1;
        currentEntry = null;
        resourceList.clear();
    }

    @Override
    public InputStream getInputStream(Resource r) {
        if(zipOut!=null) {
            throw new VleException("Cannot open input on writable ZIP");
        }
        if(zipIn==null) {
            try {
                // open for input the zip file from our parent folder
                zipIn = new ZipInputStream(parentFolder.getInputStream(zipFile));
                currentEntry = zipIn.getNextEntry();
                currentEntryIndex = 0;
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Cannot open ZIP "+zipFile.getName()+" for input", ex);
                return null;
            }
        }
        // did we open properly?
        if(currentEntry==null) {
            return null;
        }
        // skip to the given file, and
        String fullName = r.getTargetFilePath();
        
        try {
            while(currentEntry!= null && !fullName.equals(currentEntry.getName()) ) {
                currentEntry = zipIn.getNextEntry();
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
        }
        
        if(currentEntry != null && fullName.equals(currentEntry.getName())) {
            // we got our entry
            // open an entry inputstream to this entry
            ZipEntryInputStream zeis = new ZipEntryInputStream();
            zeis.setZipFolder(this);
            zeis.setZipStream(zipIn);
            return zeis;
        }

        return null;
    }

    protected void closeEntry() {
        if(zipIn!=null) {
            try {
                if(currentEntryIndex<resourceList.size()) {
                    currentEntry = zipIn.getNextEntry();
                } else {
                    // we reached the end
                    // close the zip
                    zipIn.close();
                    zipIn = null;
                    
                    // mark that we dont have a current entry
                    currentEntryIndex = -1;
                    currentEntry = null;
                }
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Could not step to next entry in the ZIP", ex);
            }
        } else if(zipOut!=null) {
            try {
                zipOut.closeEntry();
                //zipOut.flush();
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Could not pack file into ZIP", ex);
            }
            //zipOut = null;
        }
    }

    @Override
    public OutputStream getOutputStream(Resource r) {
        if(zipIn!=null) {
            throw new VleException("Cannot open output on readable ZIP");
        }
        if(zipOut==null) {
            try {
                // clear out contents, we will be creating a new ZIP
                resourceList.clear();
                // open for input the zip file from our parent folder
                zipOut = new ZipOutputStream(parentFolder.getOutputStream(zipFile));
                currentEntry = null;
            } catch (Exception ex) {
                log.log(Level.SEVERE, "Cannot open ZIP "+zipFile.getName()+" for output", ex);
                return null;
            }
        }
        
        try {
            if(currentEntry!=null) {
                // we need to close the current entry forcibly
                zipOut.closeEntry();
                currentEntry = null;
            }

            // create a new ZIP entry and fill it with values from the resource
            currentEntry = r.createZipEntry();
            currentEntry.setMethod(ZipEntry.DEFLATED);

            // put ZIP entry into output stream
            zipOut.putNextEntry(currentEntry);
            
            // put the resource in our contents list
            resourceList.add(r);

            // create the zipentry output stream
            ZipEntryOutputStream zeos = new ZipEntryOutputStream();
            zeos.setZipFolder(this);
            zeos.setZipStream(zipOut);
            return zeos;
        } catch(Exception e) {
            log.log(Level.SEVERE, "Cannot create output ZIP stream for "+r.getName(), e);
        }

        return null;
    }

    @Override
    public void getFileList(FastList<Resource> store) {
        getFileList(null, store);
    }
    
    public void getFileList(HashSet<String> filter, FastList<Resource> store) {
        store.clear();
        if(resourceList.size() == 0) {
            // retrieve the list
            try {
                zipIn = new ZipInputStream(parentFolder.getInputStream(zipFile));

                currentEntry = zipIn.getNextEntry();
                while(currentEntry!=null) {
                    Resource r = new Resource();
                    r.loadZipEntry(currentEntry);
                    if(filter==null || filter.contains(r.getName())) {
                        // set us as the zip
                        r.setZipFile(zipFile.getTargetFilePath());
                        r.setFolder(this);
                        resourceList.add(r);
                    }
                    currentEntry = zipIn.getNextEntry();
                }
            } catch(Exception e) {
                // this is normal, if we reached the end
            }
            
            try {
                zipIn.close();
            } catch(Exception e) {
                
            }
            zipIn = null;
            currentEntry = null;
        }
        store.addAll(resourceList);
    }

    @Override
    public void deleteFile(Resource r) {
        // just ignore
    }

    @Override
    public void mkDirs(Resource r) {
        // just ignore
    }

    @Override
    public void setFileDate(Resource r) {
        // ignore, date is already set when creating zip entries
    }

}
