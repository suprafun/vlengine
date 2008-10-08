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
import java.util.Comparator;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class FastList<E extends Object> {

    private Object[] _values;
    protected int _size;
    
    public FastList(E[] elements) {
        _values = elements;
        _size=_values.length;
    }

    public FastList() {
        this(8);
    }

    public FastList(int initialSize) {
        _values=new Object[initialSize];
        _size = 0;
    }

    public int size() {
        return _size;
    }
    
    public E get(int index) {
        return (E) _values[index];
    }
    
    public void add(E element) {
        ensureCapacity(_size+1);
        _values[_size] = element;
        _size++;
    }
    
    public void clear() {
        if(_size>0)
            Arrays.fill(_values, 0, _size, null);
        _size = 0;
    }
    
    public int indexOf(E element) {
        int index = -1;
        for(int i=0; i<_size&&index==-1; i++) {
            if(_values[i]==element) {
                index=i;
            }
        }
        return index;
    }

    public void remove(E element) {
        int index = indexOf(element);
        if(index!=-1) {
            remove(index);
        }
    }
    
    public void remove(int index) {
        if(index!=_size-1) {
            // copy elements
            System.arraycopy(_values, index+1, _values, index, _size-index-1);
        }
        _size--;
        _values[_size]=null;
    }

    public void ensureCapacity(int capacity) {
        if(capacity>_values.length) {
            // create new array to hold data, as copy of values
            if(capacity<_values.length*3/2)
                capacity=_values.length*3/2;
            _values = Arrays.copyOf(_values, capacity);
        }
    }
    
    public void set(int index, E element) {
        if(index>=_size) {
            ensureCapacity(index+1);
            _size=index+1;
        }
        _values[index] = element;
    }

    public void addAll(FastList<E> other) {
        if(other._size>0) {
            ensureCapacity(_size+other._size);
            System.arraycopy(other._values, 0, _values, _size, other._size);
            _size+=other._size;
        }
    }
    
    public boolean contains(E element) {
        return indexOf(element) >= 0;
    }
    
    public E[] getArray() {
        return (E[]) _values;
    }
    
    public void sort(Comparator c) {
        Arrays.sort(_values, 0, _size, c);
    }
    
    public boolean isEmpty() {
        return _size == 0;
    }
    
    public void add(int index, E element) {
        if(index>=_size) {
            ensureCapacity(index+1);
            _values[index]=element;
            _size=index+1;
        } else {
            ensureCapacity(_size+1);
            // copy elements above index one up
            System.arraycopy(_values, index, _values, index+1, _size-index);
            _values[index]=element;
            _size=_size+1;
        }

    }
    
    public E[] toArray(E[] store) {
        if(store==null)
            store=(E[]) new Object[_size];
        System.arraycopy(_values, 0, store, 0, _size);
        return store;
    }
}
