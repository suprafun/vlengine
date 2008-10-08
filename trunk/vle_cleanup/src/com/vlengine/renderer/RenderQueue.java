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

package com.vlengine.renderer;

import com.vlengine.renderer.pass.RenderPass;
import com.vlengine.scene.Renderable;
import com.vlengine.system.VleException;
import com.vlengine.util.FastList;
import com.vlengine.util.SortUtil;
import java.util.Comparator;

/**
 * Holds the rendering queues for each pass, multiple passes can use the same queues,
 * but then the queue is sorted only by the primary pass materials id.
 * The opaque/ortho/alpha queues are considered different passes
 * The batches are allways rendered by the material ID
 * 
 * @author vear (Arpad Vekas)
 */
public class RenderQueue {
    
    public static enum StandardQueue {
        Opaque(0),
        AlphaTested(1),
        Ortho(2),
        AlphaBlended(3),
        BackGround(4),
        Ligh(5),
        User(6)
        ;
        
        public final int queuId;
        
        StandardQueue(int qId) {
            queuId = qId;
        }
    }
    
    public static enum QueueFilter {
        None(0),
        Opaque(1<<StandardQueue.Opaque.queuId),
        AlphaTested(1<<StandardQueue.AlphaTested.queuId),
        Ortho(1<<StandardQueue.Ortho.queuId),
        AlphaBlended(1<<StandardQueue.AlphaBlended.queuId),
        BackGround(1<<StandardQueue.BackGround.queuId),
        Light(1<<StandardQueue.Ligh.queuId),
        Any(Long.MAX_VALUE)
        ;
        
        public final long value;
        QueueFilter(long v) {
            value = v;
        }
    }

    public static final long FILTER_OPAQUE = QueueFilter.Opaque.value;
            
    // the sort id types supported
    public static enum SortType {
        // distance from camera, the frustum range expanded to 0-Integer.MAX_VALUE
        // results in front to back rendering
        DistanceSquaredFromCamera,
        // Integer.MAX_INTEGER / distance squared from camera
        // results in back to front rendering
        InverseDistanceSquaredFromCamera,
        // used by ortho pass, the the values with most negative depth at the start, values with 0 depth at the end of queue
        AbsoluteDepth
        
        ;
    }
    
    // the id of the frame we are working with
    private int frameId = -1;
    
    private FastList<RenderableList> queue = new FastList<RenderableList>(3);
    
    public RenderQueue( int frameId ) {
        this.frameId = frameId;
    }

    public RenderQueue() {}
    
    public void setFrameId(int frameId) {
        this.frameId = frameId;
    }

    // clear the queues
    public void clear() {
        for(int i=0, mx=queue.size(); i < mx; i++) {
            RenderableList rl = queue.get(i);
            if( rl != null )
                rl.clear();
        }
    }

    public void createQueue( int queueId, SortType sortId ) {
        queue.ensureCapacity(queueId + 1);
        RenderableList l = queue.get(queueId);
        if( l == null ) {
            // create new queue
            l= new RenderableList();
            queue.set(queueId, l);
        }
        l.clear();
        l.setSortId(sortId);
        l.setQueueId(queueId);
    }
    
    // create queues as in the parent
    // no content is copyed only the queue definitions
    public void createQueue( RenderQueue parent  ) {
        frameId = parent.frameId;
        
        queue.ensureCapacity(parent.queue.size());
        for(int i=0, mx=parent.queue.size(); i<mx; i++ ) {
            RenderableList rlp = parent.queue.get(i);
            if(rlp!=null)
                createQueue( i, rlp.sortId  );
        }
    }
    
    public void add( int qn, Renderable r ) {
        queue.get(qn).list.add(r);
    }
    
    public void addAll(int qn, FastList<Renderable> objects) {
        queue.get(qn).list.addAll(objects);
    }
    
    public void merge( RenderQueue child  ) {
        FastList<RenderableList> otq = child.queue;
        for(int i = 0; i < otq.size() ; i++  ) {
            RenderableList rl = otq.get(i);
            if(rl!=null) {
                if( i >= queue.size() || queue.get(i) == null ) {
                    // we dont have the queue yet
                    createQueue(i, rl.sortId);
                }
                RenderableList rlo = queue.get(i);
                if( rl.sortId != rlo.sortId ) {
                    throw new VleException("Cannot merge queues, different sortId set");
                }
                rlo.list.addAll(rl.list);
            }
        }
    }
    
    public int getQuantity() {
        return queue.size();
    }

    public SortType getSortId( int qn ) {
        return queue.get(qn).getSortId();
    }

    public FastList<Renderable> getQueue(int qn) {
        if(qn<0 || qn>=queue.size())
            return null;
        return queue.get(qn).list;
    }
            
    public void sortAll() {
        for(int i=0, mi=queue.size(); i<mi; i++) {
            queue.get(i).sort();
        }
    }
    
    private class RenderableList {
              

        FastList<Renderable> list, tlist;

        private static final int DEFAULT_SIZE = 32;

        private Comparator<Renderable> c;

        // determines how the queue shouldbe sorted
        private SortType sortId = null;
        
        // the unique id of the queue
        private int queueId = -1;
        
        RenderableList() {
            list = new FastList<Renderable>(DEFAULT_SIZE);
            this.c = new IdComparator();
            
        }
        
        void setSortId(SortType sortId) {
            this.sortId = sortId;
        }
        
        void setQueueId(int qId) {
            this.queueId = qId;
        }

        protected SortType getSortId() {
            return sortId;
        }

        /**
         * Resets list size to 0.
         */
        void clear() {
            list.clear();
            if (tlist != null)
                tlist.clear();
        }

        /**
         * Sorts the elements in the list acording to their Comparator.
         */
        void sort() {
            if ( list.size() > 1 && sortId != null ) {
                // resize or populate our temporary array as necessary
                if (tlist == null ) {
                    tlist = new FastList<Renderable>(list.size());
                } else {
                    tlist.clear();
                }
                tlist.addAll(list);
                // now merge sort tlist into list
                SortUtil.msort(tlist.getArray(), list.getArray(), 0, list.size(), c);
            }
        }
        
        private class IdComparator implements Comparator<Renderable> {

            //private int comparebyMat;
            public IdComparator() {
            }
            
            public int compare(Renderable o1, Renderable o2) {
                // not checked if
                int mi1 = o1.getSortId(queueId);
                int mi2 = o2.getSortId(queueId);
                if( mi1 == mi2 )
                    return 0;
                if( mi1 < mi2 )
                    return 1;
                else
                    return -1;
            }
        }
    }
}
