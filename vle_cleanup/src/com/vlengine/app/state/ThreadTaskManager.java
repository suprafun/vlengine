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

package com.vlengine.app.state;

import com.vlengine.app.AppContext;
import com.vlengine.app.frame.Frame;
import com.vlengine.renderer.CullContext;
import com.vlengine.renderer.RenderContext;
import com.vlengine.scene.control.UpdateContext;
import com.vlengine.system.DisplaySystem;
import com.vlengine.thread.LocalContext;
import com.vlengine.util.FastList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class executes task in the OGL thread before rendering
 * This is a hack for mean time. It only works if the rendering
 * of frames goes constantly. If the rendering thread stops, 
 * the tasks will not execute.
 * @author vear (Arpad Vekas)
 */
public class ThreadTaskManager {
    
    protected static final Logger log = Logger.getLogger(ThreadTaskManager.class.getName());
    
    protected volatile FastList<Runnable> addList = new FastList<Runnable>();
    
    protected volatile FastList<Runnable> processList = new FastList<Runnable>();
    
    protected ThreadTaskState tts;
    
    public ThreadTaskManager() {
    }
    
    public void setupManager(AppContext ctx) {
        tts = new ThreadTaskState();
        tts.setActive(true);
        tts.setName("ThreadTaskManager");
        ctx.getGameStates().attachChild(tts);
    }
    
    /**
     * The caller thread continues, and the rask is executed in the OGL thread
     * @param r
     */
    public void invokeLater(Runnable r) {
        synchronized(addList) {
            addList.add(r);
        }
    }
    
    /**
     * Pauses the caller thread until the task completes
     * @param r
     */
    public void invokeAndWait(Runnable r) {
        if(DisplaySystem.getDisplaySystem().isOglThread()) {
            // we are in Ogl context, do it right away
            try {
                r.run();
            } catch(Exception e) {
                log.log(Level.WARNING, "Runnable caused exception", e);
            }
            return;
        }
        synchronized(addList) {
            addList.add(r);
            try {
                addList.wait();
            } catch (InterruptedException ex) {
                log.log(Level.SEVERE, "OGL task interrupted");
            }
        }
    }

    public class ThreadTaskState extends GameState {
        @Override
        public void preFrame(AppContext app) {

        }

        @Override
        public void preUpdate(UpdateContext uctx) {

        }

        @Override
        public void preCull(Frame f) {

        }

        @Override
        public void preCull(CullContext cctx) {

        }

        @Override
        public void postCull(CullContext cctx) {

        }

        @Override
        public void preMaterial(RenderContext rctx) {

        }

        @Override
        public void preRender(RenderContext rctx) {
            // call the sheduled tasks in this stage
            // switch addlist and processlist
            FastList<Runnable> temp = null;
            synchronized(addList) {
                temp = addList;
                addList = processList;
                processList = temp;
            }

            for(int i=0; i<temp.size(); i++) {
                Runnable r = temp.get(i);
                if(r!=null) {
                    try {
                        r.run();
                    } catch(Exception e) {
                        log.log(Level.WARNING, "Runnable caused exception", e);
                    }
                }
            }

            synchronized(temp) {
                temp.notifyAll();
                temp.clear();
            }
        }

        @Override
        public void postRender(RenderContext rctx) {

        }

        @Override
        public void afterRender(RenderContext rctx) {

        }

        @Override
        public void cleanup() {

        }
    }
}
