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

import com.vlengine.util.xml.Element;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

/**
 * Represents a resource to be processed by the updater and resource system
 * @author vear (Arpad Vekas)
 */
public class Resource {

    protected Logger log = Logger.getLogger(Resource.class.getName());
    
    // the name of file
    protected String name;
    
    // the base name of the file
    protected String baseName;

    // the MD5 hash code of the file
    protected String MD5hash;
    
    // the target folder on the client we need to put it
    protected String targetPath;
    
    // the source folder it is contained in
    protected Folder sourceFolder;
    
    // the ZIP file it is contained in
    protected String zipFile;
    
    // the length of the file
    protected long length;
    
    // last modification date of the file
    protected long lastmodified;
    
    // is it marked for deletion
    protected boolean delete;
    
    // special flags for the resource
    public static enum Flag {
        // this is a color texture
        ColorTexture(0),
        // this is normal texture
        NormalTexture(1),
        
        // generate a normal texture for this color texture
        GenNormalTexture(2),
        // image texture
        ImageTexture(3),
        // compressed texture (VLT)
        CompressedTexture(4),
        
        // main modelpack file
        ModelPack(5),
        // addition modelpack file
        ModelPackFile(6),
        // X model
        XModel(7),
        
        // this is the folder index file of the folder
        FolderIndex(8),
        
        // this is a ZIP file
        ZIPFile(9)
        ;

        public final int intCode;
        Flag(int code) {
            intCode = code;
        }
        
        public static Flag fromCode(int code) {
            Flag[] flags = Flag.values();
            for(int i=0; i<flags.length; i++) {
                if(flags[i].intCode == code) {
                    return flags[i];
                }
            }
            return null;
        }
    }

    // flags for the resource
    protected EnumSet<Flag> resourceFlags;
    
    public Resource() {
    }
    
    public boolean isFlag(Flag f) {
        if(resourceFlags == null)
            return false;
        return resourceFlags.contains(f);
    }
    
    public void setFlag(Flag f, boolean state) {
        if(state == false && resourceFlags == null)
            return;
        if(resourceFlags == null)
            resourceFlags = EnumSet.noneOf(Flag.class);
        if(state)
            resourceFlags.add(f);
        else
            resourceFlags.remove(f);
    }

    public String getMD5hash() {
        return MD5hash;
    }

    public void setMD5hash(String MD5hash) {
        this.MD5hash = MD5hash;
    }

    public Folder getFolder() {
        return sourceFolder;
    }

    public void setFolder(Folder folder) {
        this.sourceFolder = folder;
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }
    
    public long getDate() {
        return this.lastmodified;
    }
    
    public void setDate(long date) {
        this.lastmodified = date;
    }
    
    public void setZipFile(String zipFile) {
        this.zipFile = zipFile;
    }
    
    public String getZipFile() {
        return zipFile;
    }
    
    /**
     * Guesses the file type by the file name
     */
    public void guessType() {
        String nm = name.toLowerCase();
        if(nm.endsWith(".vlt")) {
            // compressed texture
            setFlag(Flag.CompressedTexture, true);
            nm = nm.substring(0, nm.length()-4);
        }
        // is it a texture
        if(nm.endsWith(".png")) {
            if(baseName==null) {
                // if this was not a compressed texture
                baseName = nm;
            }
            // yep, an image
            setFlag(Flag.ImageTexture, true);
            nm = nm.substring(0, nm.length()-4);
            // is it color, or normal
            // check for normal
            // TODO: add other way to recognize
            if(nm.endsWith("_nor")) {
                // normal map
                setFlag(Flag.NormalTexture, true);
            } else {
                setFlag(Flag.ColorTexture, true);
            }
        } else if(nm.endsWith(".pack.gz")) {
            // this is modelpack main file
            setFlag(Flag.ModelPack, true);
            // base name is the modelpack name
            baseName = nm.substring(0, nm.length()-8);
        } else if(nm.endsWith(".dat")) {
            // modelpack additional file
            setFlag(Flag.ModelPackFile, true);
            // no base name here, this file is not loadable by itself
        } else if(nm.endsWith(".x")) {
            // modelpack additional file
            setFlag(Flag.XModel, true);
            // no base name here, this file is not loadable by itself
        } else if(nm.endsWith(".idx.gz")) {
            // modelpack additional file
            setFlag(Flag.FolderIndex, true);
            // no base name here, this file is not loadable by itself
        } else if(nm.endsWith(".zip")) {
            // modelpack additional file
            setFlag(Flag.ZIPFile, true);
            // no base name here, this file is not loadable by itself
        }// TODO: recognize other file types
        
    }

    public String getTargetFilePath() {
        String path = name;
        if(targetPath!=null && !"".equals(targetPath)) {
            path = targetPath + "/" + path;
        }
        return path;
    }

    public void calculateMD5() {
        
        // allocate a bytebuffer
        ByteBuffer bb = ByteBuffer.allocate((int) getLength());
        
        // retrieve the file to a bytebuffer
        sourceFolder.load(this, bb);
        bb.rewind();

        MessageDigest md5Digest;
        try {
            md5Digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            log.log(Level.SEVERE, "Cannot get MD5 hasher", ex);
            return;
        }

        // calculate the md5
        md5Digest.update(bb);
        byte[] md5Bytes = new byte[17];
        try {
            md5Digest.digest(md5Bytes, 1, 16);
        } catch (DigestException ex) {
            log.log(Level.SEVERE, "Cannot get MD5 digest for "+name, ex);
            return;
        }
			
        String md5String = new BigInteger(md5Bytes).toString(16);
        if (md5String.length() < 32) {
            StringBuilder sb = new StringBuilder(32);
            for (int i = 0; i < 32 - md5String.length(); i++) {
                sb.append("0");
            }
            sb.append(md5String);
            md5String = sb.toString();
        }
        setMD5hash(md5String);
    }
    
    public ZipEntry createZipEntry() {
        
        ZipEntry ze = new ZipEntry(getTargetFilePath());
        ze.setSize(length);
        // TODO: convert to DOS time
        ze.setTime(this.lastmodified);

        byte[] adddata = new byte[33];
        
        if(MD5hash != null) {
            try {
                byte[] hash = MD5hash.getBytes("UTF-8");
                if(hash.length != 32) {
                    // hash should be this long
                    return null;
                }
                // put it into the additional data array
                System.arraycopy(hash, 0, adddata, 0, 32);
            } catch (UnsupportedEncodingException ex) {
                log.log(Level.SEVERE, null, ex);
                return null;
            }
        }
        
        // delete flag
        adddata[32] = (byte) (delete?1:0);
        
        // set additional data
        ze.setExtra(adddata);
        
        return ze;
    }

    public void loadZipEntry(ZipEntry ze) {
        targetPath = ze.getName();
        // strip the last part into the name

        int lidx = targetPath.lastIndexOf('/');
        if(lidx>0) {
            name = targetPath.substring(lidx+1);
            targetPath = targetPath.substring(0, lidx-1);
        } else {
            name = targetPath;
            targetPath = "";
        }

        // get size
        this.length = ze.getSize();

        // TODO: convert from DOS time
        this.lastmodified = ze.getTime();

        byte[] adddata = ze.getExtra();

        if(adddata!=null) {
            if(adddata[0]!=0 && adddata.length >=32) {
                // get hash code
                this.MD5hash = new String(adddata, 0, 32, Charset.forName("UTF-8"));
            }

            // is it deletable
            if(adddata.length >32)
                this.delete = adddata[32]==1;
        }

        // guess flags and type
        guessType();
    }

    // load the data for this resource from XML
    public void load(Element e) {
        name = e.getChildText("name");
        // baseName from guess
        MD5hash = e.getChildText("MD5");
        targetPath = e.getChildText("target");
        // folder set from outside
        zipFile = e.getChildText("zip");
        length = e.getChild("length").getTextlong();
        lastmodified = e.getChild("date").getTextlong();
        delete = e.getChildboolean("delete");
        long resFlags = e.getChild("flags").getTextlong();
        for(int i=0; resFlags != 0; i++) {
            if((resFlags&0x1) != 0) {
                this.setFlag(Flag.fromCode(i), true);
            }
            resFlags = resFlags >> 1;
        }
        
        // guess type
        guessType();
    }
    
    public Element save() {
        Element e = new Element("resource");
        e.setChild("name").setText(name);
        e.setChild("MD5").setText(MD5hash);
        e.setChild("target").setText(targetPath);
        e.setChild("zip").setText(zipFile);
        e.setChild("length").setText(length);
        e.setChild("date").setText(lastmodified);
        e.setChild("delete").setText(delete);
        long resFlags = 0;
        if(resourceFlags!=null) {
            Flag[] flags = (Flag[]) resourceFlags.toArray(new Flag[4]);
            for(int i=0; i<flags.length; i++) {
                if(flags[i]!=null)
                    resFlags |= (1<<flags[i].intCode);
            }
        }
        e.setChild("flags").setText(resFlags);
        
        return e;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Resource other = (Resource) obj;
        if (this.length != other.length) {
            return false;
        }
        if (this.lastmodified != other.lastmodified) {
            return false;
        }
        if (this.name != other.name && (this.name == null || !this.name.equals(other.name))) {
            return false;
        }
        if (this.MD5hash != other.MD5hash && (this.MD5hash == null || !this.MD5hash.equals(other.MD5hash))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 79 * hash + (this.MD5hash != null ? this.MD5hash.hashCode() : 0);
        hash = 79 * hash + (int) (this.length ^ (this.length >>> 32));
        hash = 79 * hash + (int) (this.lastmodified ^ (this.lastmodified >>> 32));
        return hash;
    }
}
