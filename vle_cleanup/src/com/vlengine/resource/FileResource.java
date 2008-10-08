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

import com.vlengine.util.geom.BufferUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author vear (Arpad Vekas)
 */
public class FileResource {
    private static final Logger logger = Logger.getLogger(FileResource.class.getName());
    
    public static void save(String path, Object data) {
        if(data instanceof ByteBuffer) {
            save(path,(ByteBuffer)data);
        } else if(data instanceof FloatBuffer) {
            save(path,(FloatBuffer)data);
        } else if(data instanceof ShortBuffer) {
            save(path,(ShortBuffer)data);
        } else if(data instanceof IntBuffer) {
            save(path,(IntBuffer)data);
        } else {
            logger.log(Level.WARNING, "Cannot save "+path+" unknown data type");
        }
    }

    public static ByteBuffer load( String path, ParameterMap parameters) {
        try {
            FileChannel fis = new FileInputStream(path).getChannel();
            ByteBuffer data;
            boolean direct = true;
            if (parameters != null) {
                direct = parameters.getBoolean(ParameterMap.KEY_DIRECTBUFFER, direct);
            }
            if( direct ) {
                data = BufferUtils.createByteBuffer((int) fis.size() );
            } else {
                data = ByteBuffer.allocate( (int) fis.size() );
            }
            data.rewind();
            while ( fis.position() < fis.size() ) {
                fis.read(data);
            }
            fis.close();
            data.rewind();
            return data;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Cannot load file "+path, ex);
        }
        return null;
    }
    
    public static boolean save( String path, ByteBuffer data ) {
        try {
            data.position(0);
            FileChannel fos = new FileOutputStream(path).getChannel();
            fos.write(data);
            fos.close();
            data.position(0);
            return true;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    public static boolean save( String path, FloatBuffer data ) {
        try {
            data.rewind();
            DataOutputStream fos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path)));
            int limit = data.limit();
            for(int i=0; i<limit; i++)
                fos.writeFloat(data.get());
            fos.close();
            data.position(0);
            return true;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public static FloatBuffer loadFloatBuffer(String path, ParameterMap params) {
        DataInputStream dis = null;
        FloatBuffer fb = null;
        try {
            File inf = new File(path);
            int len = (int) inf.length();
            if (len == 0) {
                return null;
            }

            int datasize = 4;
            
            if (params.getBoolean(ParameterMap.KEY_DIRECTBUFFER, true)) {
                fb = BufferUtils.createFloatBuffer(len / datasize);
            } else {
                fb = ByteBuffer.allocate(len/datasize).asFloatBuffer();
            }
            fb.clear();
            dis = new DataInputStream(new BufferedInputStream(new FileInputStream(path)));
            int dlen = len/datasize;
            for(int i=0; i<dlen; i++) {
                fb.put(dis.readFloat());
            }
            fb.rewind();
        } catch (Exception ex) {
            Logger.getLogger(FileResource.class.getName()).log(Level.SEVERE, null, ex);
            fb = null;
        } finally {
            try {
                if(dis!=null)
                    dis.close();
            } catch (IOException ex) {
                Logger.getLogger(FileResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return fb;
    }

    public static boolean save( String path, ShortBuffer data ) {
        try {
            data.position(0);
            DataOutputStream fos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path)));
            int limit = data.limit();
            for(int i=0; i<limit; i++)
                fos.writeShort(data.get());
            fos.close();
            data.position(0);
            return true;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    public static ShortBuffer loadShortBuffer(String path, ParameterMap params) {
        DataInputStream dis = null;
        ShortBuffer fb = null;
        try {
            File inf = new File(path);
            int len = (int) inf.length();
            if (len == 0) {
                return null;
            }

            if (params.getBoolean(ParameterMap.KEY_DIRECTBUFFER, true)) {
                fb = BufferUtils.createShortBuffer(len / 2);
            } else {
                fb = ByteBuffer.allocate(len).asShortBuffer();
            }
            fb.clear();
            dis = new DataInputStream(new BufferedInputStream(new FileInputStream(path)));
            int dlen = len/2;
            for(int i=0; i<dlen; i++) {
                fb.put(dis.readShort());
            }
            fb.rewind();
        } catch (Exception ex) {
            Logger.getLogger(FileResource.class.getName()).log(Level.SEVERE, null, ex);
            fb = null;
        } finally {
            try {
                if(dis!=null)
                    dis.close();
            } catch (IOException ex) {
                Logger.getLogger(FileResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return fb;
    }

    public static boolean save( String path, IntBuffer data ) {
        try {
            data.position(0);
            DataOutputStream fos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path)));
            int limit = data.limit();
            for(int i=0; i<limit; i++)
                fos.writeInt(data.get());
            fos.close();
            data.position(0);
            return true;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    public static IntBuffer loadIntBuffer(String path, ParameterMap params) {
        DataInputStream dis = null;
        IntBuffer fb = null;
        try {
            File inf = new File(path);
            int len = (int) inf.length();
            if (len == 0) {
                return null;
            }

            if (params.getBoolean(ParameterMap.KEY_DIRECTBUFFER, true)) {
                fb = BufferUtils.createIntBuffer(len / 4);
            } else {
                fb = ByteBuffer.allocate(len).asIntBuffer();
            }
            fb.clear();
            dis = new DataInputStream(new BufferedInputStream(new FileInputStream(path)));
            int dlen = len/4;
            for(int i=0; i<dlen; i++) {
                fb.put(dis.readInt());
            }
            fb.rewind();
        } catch (Exception ex) {
            Logger.getLogger(FileResource.class.getName()).log(Level.SEVERE, null, ex);
            fb = null;
        } finally {
            try {
                if(dis!=null)
                    dis.close();
            } catch (IOException ex) {
                Logger.getLogger(FileResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return fb;
    }
}
