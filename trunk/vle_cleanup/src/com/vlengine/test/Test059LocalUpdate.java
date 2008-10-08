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

package com.vlengine.test;

import com.vlengine.updater2.Index;
import com.vlengine.updater2.LocalFolder;
import com.vlengine.updater2.Resource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Autoupdater2 test for local update
 * @author vear (Arpad Vekas)
 */
public class Test059LocalUpdate {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // create the folder
        LocalFolder lfSource = new LocalFolder();
        if(!lfSource.connect("C:/temp/demo/demo1", null, null)) {
            return;
        }
        // create the index file
        Index idxSource = new Index();
        idxSource.setName("server");
        idxSource.setFolder(lfSource);
        // create the index
        idxSource.loadIndex();        
        // save the index
        idxSource.saveIndex();
        
        boolean patch = true;
        
        if(patch) {
            // create the target folder
            LocalFolder lfTarget = new LocalFolder();
            if(!lfTarget.connect("C:/temp/demo/demo1_update", null, null)) {
                return;
            }

            // create the index file
            Index idxTarget = new Index();
            idxTarget.setName("client");
            idxTarget.setFolder(lfTarget);
            // create the index
            idxTarget.loadIndex();

            // create a new index of differing files
            Index idxPatch = new Index();
            idxPatch.setName("patch");
            idxPatch.setFolder(lfTarget);

            // create patch index as diff between source and target
            idxPatch.createUpdateList(idxSource, idxTarget);
            // process the updates
            Collection<Resource> res = idxPatch.getResources();
            for(Resource r:res) {
                // open output stream
                OutputStream os = lfTarget.getOutputStream( r );

                // copy file
                boolean saved = lfSource.copy(r, os);
                if(!saved) {
                    System.out.println("Cannot save "+r.getName());
                } else {
                    System.out.println("Updated "+r.getName());
                }
                try {
                    // close stream
                    os.close();
                } catch (IOException ex) {
                }
                idxTarget.addResource(r);
            }

            // save the patched index
            idxTarget.saveIndex();
            lfTarget.disconnect();
        }
        
        lfSource.disconnect();
         
        
    }
}
