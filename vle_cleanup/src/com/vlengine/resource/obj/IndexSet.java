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

package com.vlengine.resource.obj;

/**
 * Class used during loading of OBJ models
 * @author vear (Arpad Vekas)
 */
public class IndexSet {
    int vIndex, nIndex, tIndex;
    int index;
    
    ObjModel model;
    public IndexSet(ObjModel obj) {
        model = obj;
    }

    public IndexSet(String parts) {
        parseStringArray(parts);
    }

    public void parseStringArray(String parts) {
        String[] triplet = parts.split("/");
        vIndex = Integer.parseInt(triplet[0]);
        if (vIndex < 0) {
            vIndex += model.vertexList.size();
        } else {
            vIndex--;  // obj starts at 1 not 0
        }

        if (triplet.length < 2 || triplet[1] == null
                || triplet[1].equals("")) {
            tIndex = -1;
        } else {
            tIndex = Integer.parseInt(triplet[1]);
            if (tIndex < 0) {
                tIndex += model.textureList.size();
            } else {
                tIndex--;  // obj starts at 1 not 0
            }
        }

        if (triplet.length != 3 || triplet[2] == null
                || triplet[2].equals("")) {
            nIndex = -1;
        } else {
            nIndex = Integer.parseInt(triplet[2]);
            if (nIndex < 0) {
                nIndex += model.normalList.size();
            } else {
                nIndex--;  // obj starts at 1 not 0
            }

        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IndexSet)) return false;

        IndexSet other = (IndexSet)obj;
        if (other.nIndex != this.nIndex) return false;
        if (other.tIndex != this.tIndex) return false;
        if (other.vIndex != this.vIndex) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 37;
        hash += 37 * hash + vIndex;
        hash += 37 * hash + nIndex;
        hash += 37 * hash + tIndex;
        return hash;
    }
}
