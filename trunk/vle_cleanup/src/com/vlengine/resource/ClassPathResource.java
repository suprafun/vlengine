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
import java.net.URL;
import java.nio.ByteBuffer;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class ClassPathResource extends ResourceFolder {
    
    public ClassPathResource(AppContext app) {
        super(app);
        this.resourcepath = null;
    }
        
    @Override
    public String getResourcePathFull() {
        return resourcepath;
    }
    
    protected String getFileNameFull(String name) {
        String fullfile= getResourcePathFull() + "/" + name;
        return fullfile;
    }
    
    @Override
    protected URL getUrl(String name) {
        return this.getClass().getClassLoader().getResource(
                getFileNameFull(name));
    }
    
    public boolean isExtractable(String name) {
        URL url = getUrl(name);
        if(url!=null)
            return true;
        return false;
    }
    
    public ByteBuffer extract(String name, ParameterMap params) {
        URL url = getUrl(name);
        ByteBuffer data = UrlResource.load(url, params);
        return data;
    }
    
    @Override
    public boolean requestFile(String name) {
        if( conf.p_usecaching )
            if( cached.containsKey(name) 
             //|| prepared.containsKey(name)
             )
                return true;
        // check if PFF file contains the file
        if(isExtractable(name))
            return true;
        return false;
    }
    
    @Override
    public boolean isDesigned(String name) {
        return isExtractable(name);
    }
    
    @Override
    protected ByteBuffer loadDesigned(String name, ParameterMap params) {
        return extract(name, params);
    }
    
}
