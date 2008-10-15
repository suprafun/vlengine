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

import java.util.Arrays;

/**
 * TODO: remove this and use TIntArrayList from trove
 * @author vear (Arpad Vekas)
 */
public class IntList {
    private int[] _values;
    protected int _size;
    
    public IntList() {
        this(10);
    }
    
    public IntList( int initialSize ) {
        _values=new int[initialSize];
        _size = 0;
    }

    public IntList(int[] array) {
        setArray(array);
    }

    public int size() {
        return _size;
    }
    
    public int get(int index) {
        return _values[index];
    }

    public void add(int element) {
        ensureCapacity(_size+1);
        _values[_size] = element;
        _size++;
    }
    
    public void clear() {
        if(_size>0)
            Arrays.fill(_values, 0, _size, 0);
        _size = 0;
    }
    
    public int indexOf(int element) {
        int index = -1;
        for(int i=0; i<_size&&index==-1; i++) {
            if(_values[i]==element) {
                index=i;
            }
        }
        return index;
    }

    public void removeElement(int element) {
        int index = indexOf(element);
        if(index!=-1) {
            removeElementAt(index);
        }
    }
    
    public void removeElementAt(int index) {
        if(index!=_size-1) {
            // copy elements
            System.arraycopy(_values, index+1, _values, index, _size-index-1);
        }
        _size--;
        _values[_size]=0;
    }
    
    public void ensureCapacity(int capacity) {
        if(capacity>_values.length) {
            if(capacity<_values.length*3/2)
                capacity=_values.length*3/2;
            // create new array to hold data, as copy of values
            _values = Arrays.copyOf(_values, capacity);
        }
    }
    
    public void set(int index, int element) {
        if(index>=_size) {
            ensureCapacity(index+1);
            _size=index+1;
        }
        _values[index] = element;
    }

    public void addAll(IntList other) {
        if(other._size>0) {
            ensureCapacity(_size+other._size);
            System.arraycopy(other._values, 0, _values, _size, other._size);
            _size+=other._size;
        }
    }
    
    public boolean contains(int element) {
        return indexOf(element) >= 0;
    }
    
    public int[] getArray() {
        return _values;
    }
    
    public void setArray(int[] array) {
        _values = array;
        _size = _values.length;
    }
    
    @Override
    public boolean equals(Object other) {
        if(other == null || !(other instanceof IntList))
            return false;
        IntList otherList = (IntList) other;
        if(_size!=otherList._size) 
            return false;
        for(int i=0; i<_size; i++)
            if(_values[i]!=otherList._values[i])
                return false;
        return true;
    }
    
    public boolean containsAll(IntList other) {
        if(other == null && _size!=0)
            return false;
        if(_size>other._size)
            return false;
        for(int i=0; i<_size; i++) {
            boolean found = false;
            for(int j=0; j<other._size && !found; j++) {
                if(_values[i] == other._values[j]) {
                    found = true;
                }
            }
            if(!found)
                return false;
        }
        return true;
    }
}
