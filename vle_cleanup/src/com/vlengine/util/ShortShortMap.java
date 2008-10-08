/*
 * Copyright (c) 2003-2007 jMonkeyEngine, 2008 VL Engine
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
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
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

package com.vlengine.util;

import com.vlengine.thread.LocalContext;
import java.util.Arrays;

/**
 * A HashMap style map, working on short values as keys and values.
 * The reason for this is, that both a key and value fit into a single int
 * 
 * @author vear (Arpad Vekas)
 */
public class ShortShortMap {

    private static final short DEFAULT_NULL_KEY = 0;
    private static final short DEFAULT_NULL_VALUE = 0;
        
    int nullEntry = 0;
    short nullKey = 0;
    short nullValue = 0;
            
    // the number of entryies in the map
    int size = 0;
    
    // the array holding the old values
    int[] oldEntry;
    
    // the array holding the new values
    int[] newEntry;

    
    public ShortShortMap() {
        this(16,DEFAULT_NULL_KEY,DEFAULT_NULL_VALUE);
    }

    private static int nextPowerOfTwo(int num) {
 	int cap=16;
        while(cap<num)
            cap=cap<<1;        
        return cap;
    }
    
    public ShortShortMap(int capacity) {
        this(capacity,DEFAULT_NULL_KEY,DEFAULT_NULL_VALUE);
    }

    public ShortShortMap(int capacity, short nullKey, short nullValue) {
        newEntry=new int[nextPowerOfTwo(capacity)];
        this.nullKey = nullKey;
        this.nullValue = nullValue;
        nullEntry = entry(nullKey, nullValue);
        clear();
    }
    
    public void clear() {
        oldEntry=newEntry;
        Arrays.fill(newEntry, nullEntry );
        size = 0;
    }
    
    private static short key(int entry) {
        return (short) ((entry >> 16) & 0xffff);
    }
    
    private static short value(int entry) {
        return (short) (entry & 0xffff);
    }
    
    private static int entry(short key, short value) {
        return ((key&0xffff)<<16)|(value&0xffff);
    }

    private short get(short key, int[] entry) {
        
        if (key == nullKey)
           return nullValue;
        
        int mask = entry.length -1;
        int hash = key & mask;

        while (true) {
            int old = entry[hash];

            short mapKey = key(old);

            if (mapKey == key) {
                short value = value(old);
                if ( value != nullValue || entry == newEntry)
                    return value;
                // if key found with null value in old map, check new map
                return get(key, newEntry);
            } else if (mapKey == nullKey ) {
                if (entry == newEntry)
                    return nullValue;
                // not found in old array,
                // check entry in new array
                return get(key, newEntry);
            }
            hash = (hash + 1) & mask;
        }
   }

    public short get(short key) {
        // try to get from the old entryes first
        return get(key, oldEntry);
    }
    
    public void put(short key, short value) {
        if (key == nullKey) {
            nullValue = value;
            nullEntry = entry(nullKey, nullValue);
            return;
        }
        
        int mask = newEntry.length -1;
        int hash = key & mask;

        while (true) {
            int old = newEntry[hash];
            short testKey = key(old);

            if (testKey == nullKey ) {
                newEntry[hash] = entry(key, value);
                size++;
                // if resizing needs to be done, and is not already goig on
                if (newEntry.length <= 2 * size && newEntry == oldEntry)
                    resize();
                return;
            } else if (key != testKey ) {
                hash = (hash + 1) & mask;
                continue;
            } else {
                newEntry[hash] = entry(key, value);
                return;
            }
        }
    }

    private synchronized void resize() {
        // no need for resize, or its already going on
        if (newEntry.length > 2 * size || newEntry != oldEntry)
            return;
        // create new array
        int newSize = 2 * newEntry.length;
        int [] newEntries = new int[newSize];
        // initialize with null values
        Arrays.fill(newEntries, nullEntry );
        // switch new to newly created, so others can write it
        newEntry = newEntries;

        int mask = newEntry.length - 1;
        
        boolean found = true;

        int oldsize = size;
        int added = 0;
        
        boolean multithreading = LocalContext.isUseMultithreading();
        
        while(found) {
            found = false;
            for (int i = 0; i < oldEntry.length; i++) {
                int old = oldEntry[i];
                short key = key(old);
                short value = value(old);
                if ( key == nullKey ) {
                    continue;
                }

                if( value == nullValue) {
                    // decrement size when encountered a removed entry
                    continue;
                }

                // we found something to work on, recheck needed
                found=true;
                // reinsert into new
                int hash = key & mask;

                 while (true) {
                    int test = newEntry[hash];
                    short testKey = key(test);

                    if (testKey == nullKey ) {
                        newEntry[hash] = old;
                        oldEntry[i] = entry(key, nullValue);
                        added++;
                        break;
                    } if (key != testKey ) {
                        hash = (hash + 1) & mask;
                   } else {
                        // put occured while we copy
                        // discard old value
                        oldEntry[i] = entry(key, nullValue);
                        break;
                   }
                }
            }
            if(!multithreading)
                found=false;
        }
        // resizing finished, the old is not needed any more
        oldEntry = newEntry;
        // the new size, with items added during resize, and the items brought over
        // from old map
        size = size - oldsize + added;
    }

    public short remove(short key) {
        return remove(key, oldEntry);
    }

    private short remove(short key, int[] entry)  {
        if (key == nullKey)
          return nullValue;
        
         int mask = entry.length - 1;
         int hash = key & mask;

         while (true) {
           int old = entry[hash];
           short mapKey = key(old);

           if (mapKey == nullKey) {
               if (entry == newEntry)
                    return nullValue;
                // not found in old array,
                // check entry in new array
                return remove(key, newEntry);
           } else if (mapKey == key) {
                short oldVal = value(old);
                entry[hash] = entry(key, nullValue);
                if(entry != newEntry) {
                    short newVal = remove(key, newEntry);
                    return newVal != nullValue ? newVal : oldVal;
                } else {
                    return oldVal;
                }
           }
           hash = (hash + 1) & mask;
        }
     }
    
    public IntList getValues(IntList store) {
        if(store==null)
            store = new IntList();
        else
            store.clear();
        for(int i=0, mx=oldEntry.length; i<mx; i++) {
            int oldentry = oldEntry[i];
            short key = key(oldentry);
            if(key!=nullKey) {
                if(value(oldentry)!=nullValue)
                    store.add(value(oldentry));
                else if(oldEntry != newEntry) {
                    short newVal = this.get(key, newEntry);
                    if(newVal != nullValue ) {
                        store.add(newVal);
                    }
                }
            }
        }
        return store;
    }
    
    public IntList getKeys(IntList store) {
        if(store==null)
            store = new IntList();
        else
            store.clear();
        for(int i=0, mx=oldEntry.length; i<mx; i++) {
            int oldentry = oldEntry[i];
            short key = key(oldentry);
            if(key!=nullKey) {
                if(value(oldentry)!=nullValue)
                    store.add(key);
                else if(oldEntry != newEntry) {
                    short newVal = this.get(key, newEntry);
                    if(newVal != nullValue ) {
                        store.add(key);
                    }
                }
            }
        }
        return store;
    }
    
    @Override
    public boolean equals(Object other) {
        if(other==null || !(other instanceof ShortShortMap))
            return false;
        ShortShortMap otherMap = (ShortShortMap) other;
        // go over keys, and check that it matches the other map
        for(int i=0, mx=oldEntry.length; i<mx; i++) {
            int oldentry = oldEntry[i];
            short key = key(oldentry);
            if(key!=nullKey) {
                if(this.get(key) != otherMap.get(key))
                    return false;
            }
        }
        // check keys in the other to this one too
        for(int i=0, mx=otherMap.oldEntry.length; i<mx; i++) {
            int oldentry = otherMap.oldEntry[i];
            short key = key(oldentry);
            if(key!=nullKey) {
                if(this.get(key) != otherMap.get(key))
                    return false;
            }
        }
        return true;
    }
}
/*
 * Based on Resin implementation, with some changes.
1 
2  * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
3  *
4  * This file is part of Resin(R) Open Source
5  *
6  * Each copy or derived work must preserve the copyright notice and this
7  * notice unmodified.
8  *
9  * Resin Open Source is free software; you can redistribute it and/or modify
10  * it under the terms of the GNU General Public License as published by
11  * the Free Software Foundation; either version 2 of the License, or
12  * (at your option) any later version.
13  *
14  * Resin Open Source is distributed in the hope that it will be useful,
15  * but WITHOUT ANY WARRANTY; without even the implied warranty of
16  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
17  * of NON-INFRINGEMENT. See the GNU General Public License for more
18  * details.
19  *
20  * You should have received a copy of the GNU General Public License
21  * along with Resin Open Source; if not, write to the
22  * Free SoftwareFoundation, Inc.
23  * 59 Temple Place, Suite 330
24  * Boston, MA 02111-1307 USA
25  *
26  * @author Scott Ferguson
27  
 */
