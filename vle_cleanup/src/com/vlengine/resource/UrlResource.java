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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author vear (Arpad Vekas)
 */
public class UrlResource {
    private static final Logger logger = Logger.getLogger(UrlResource.class.getName());
    
    public static ByteBuffer load( URL path, ParameterMap parameters) {
        if(path.getProtocol().equals("file")) {
            // load as file resource
            return FileResource.load(path.getFile(), parameters);
        }
        try {
            InputStream fis = path.openStream();
            ByteBuffer data;
            boolean direct = true;
            if (parameters != null) {
                direct = parameters.getBoolean("directbuffer", direct);
            }
            if( direct ) {
                data = BufferUtils.createByteBuffer((int) fis.available() );
            } else {
                data = ByteBuffer.allocate( (int) fis.available() );
            }
            data.position(0);
            byte buffer[] = new byte[1024];
            while ( fis.available() > 0 ) {// TODO checking available > 0 is a bug, will fail if filesystem is busy
                int read = fis.read(buffer);
                if(read>0)
                    data.put(buffer, 0, read);
            }
            fis.close();
            data.position(0);
            return data;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Cannot load file "+path, ex);
        }
        return null;
    }
}
