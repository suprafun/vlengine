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

package com.vlengine.util;

import com.vlengine.thread.LocalContext;
import java.util.Arrays;

/**
 * A HashMap for int,Object values
 * TODO: remove this and use TIntHash from trove
 * @author vear (Arpad Vekas)
 */
public class IntMap {
    private static final short DEFAULT_NULL_KEY = 0;
    int nullKey = 0;
    
    // the number of entryies in the map
    int size = 0;
    
    // the array holding the old values
    int[] oldKey;
    Object[] oldValue;
    
    // the array holding the new values
    int[] newKey;
    Object[] newValue;
    
    public IntMap() {
        this(16,DEFAULT_NULL_KEY);
    }
    
    private static int nextPowerOfTwo(int num) {
 	int cap=16;
        while(cap<num)
            cap=cap<<1;        
        return cap;
    }
    
    public IntMap(int capacity) {
        this(capacity,DEFAULT_NULL_KEY);
    }

    public IntMap(int capacity, int nullKey) {
        newKey=new int[nextPowerOfTwo(capacity)];
        newValue = new Object[newKey.length];
        this.nullKey = nullKey;
        clear();
    }
    
    public void clear() {
        oldKey=newKey;
        oldValue=newValue;
        Arrays.fill(newKey, nullKey );
        Arrays.fill(newValue, (Object)null );
        size = 0;
    }

    private Object get(int key, int[] keys, Object[] values) {
        if (key == nullKey)
           return null;
        
        int mask = keys.length -1;
        int hash = key & mask;

        while (true) {
            int mapKey = keys[hash];
            if (mapKey == key) {
                Object value = values[hash];
                if ( value != null || keys == newKey)
                    return value;
                // if key found with null value in old map, check new map
                return get(key, newKey, newValue);
            } else if (mapKey == nullKey ) {
                if (keys == newKey)
                    return null;
                // not found in old array,
                // check entry in new array
                return get(key, newKey, newValue);
            }
            hash = (hash + 1) & mask;
        }
   }

    public Object get(int key) {
        // try to get from the old entryes first
        return get(key, oldKey, oldValue);
    }

    public void put(int key, Object value) {
        if (key == nullKey) {
            return;
        }

        int mask = newKey.length -1;
        int hash = key & mask;

        while (true) {

            int testKey = newKey[hash];

            if (testKey == nullKey ) {
                newKey[hash] = key;
                newValue[hash] = value;
                size++;
                // if resizing needs to be done, and is not already goig on
                if (newKey.length <= 2 * size && newKey == oldKey)
                    resize();
                return;
            } else if (key != testKey ) {
                hash = (hash + 1) & mask;
                continue;
            } else {
                newKey[hash] = key;
                newValue[hash] = value;
                return;
            }
        }
    }
    
    private synchronized void resize() {
        // no need for resize, or its already going on
        if (newKey.length > 2 * size || newKey != oldKey)
            return;
        // create new array
        int newSize = 2 * newKey.length;
        int [] newKeys = new int[newSize];
        Object[] newValues = new Object[newSize];
        
        // initialize with null values
        Arrays.fill(newKeys, nullKey );
        Arrays.fill(newValues, null );
        
        // switch new to newly created, so others can write it
        newKey = newKeys;
        newValue = newValues;

        int mask = newKey.length - 1;
        
        boolean found = true;

        int oldsize = size;
        int added = 0;
        
        boolean multithreading = LocalContext.isUseMultithreading();
        
        while(found) {
            found = false;
            for (int i = 0; i < oldKey.length; i++) {

                int key = oldKey[i];
                
                if ( key == nullKey ) {
                    continue;
                }
                Object value = oldValue[i];
                
                if( value == null) {
                    // decrement size when encountered a removed entry
                    continue;
                }

                // we found something to work on, recheck needed
                found=true;
                // reinsert into new
                int hash = key & mask;

                 while (true) {
                    int testKey = newKey[hash];
                    
                    if (testKey == nullKey ) {
                        newKey[hash] = key;
                        newValue[hash] = value;
                        
                        oldValue[i] = null;
                        added++;
                        break;
                    } if (key != testKey ) {
                        hash = (hash + 1) & mask;
                   } else {
                        // put occured while we copy
                        // discard old value
                        oldValue[i] = null;
                        break;
                   }
                }
            }
            if(!multithreading)
                found=false;
        }
        // resizing finished, the old is not needed any more
        oldKey = newKey;
        // should be made thread safe
        oldValue = newValue;
        // the new size, with items added during resize, and the items brought over
        // from old map
        size = size - oldsize + added;
    }
    
    public Object remove(int key) {
        return remove(key, oldKey, oldValue);
    }

    private Object remove(int key, int[] keys, Object[] values)  {
        if (key == nullKey)
          return null;
        
         int mask = keys.length - 1;
         int hash = key & mask;

         while (true) {

           int mapKey = keys[hash];

           if (mapKey == nullKey) {
               if (keys == newKey)
                    return null;
                // not found in old array,
                // check entry in new array
                return remove(key, newKey, newValue);
           } else if (mapKey == key) {
                Object oldVal = values[hash];
                values[hash] = null;
                if(keys != newKey) {
                    Object newVal = remove(key, newKey, newValue);
                    return newVal != null ? newVal : oldVal;
                } else {
                    return oldVal;
                }
           }
           hash = (hash + 1) & mask;
        }
     }

    public FastList getValues(FastList store) {
        if(store==null)
            store = new FastList();
        store.clear();
        for(int i=0, mx=oldKey.length; i<mx; i++) {
            int key = oldKey[i];
            if(key!=nullKey) {
                if(oldValue[i]!=null)
                    store.add(oldValue[i]);
                else if(oldKey != newKey) {
                    Object newVal = this.get(key, newKey, newValue);
                    if(newVal != null ) {
                        store.add(newVal);
                    }
                }
            }
        }
        return store;
    }
}
