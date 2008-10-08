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

package com.vlengine.renderer;

import com.vlengine.image.Texture;
import com.vlengine.renderer.material.Material;
import com.vlengine.scene.Node;
import com.vlengine.scene.SceneElement;
import com.vlengine.scene.Spatial;
import com.vlengine.scene.batch.TextBatch;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.state.RenderState;
import com.vlengine.util.geom.IndexBuffer;
import com.vlengine.util.geom.VertexBuffer;
import com.vlengine.util.geom.VertexFormat;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class Renderer {
 // width and height of renderer
    protected int width;
    protected int height;
       
    protected RenderStatistics stats;
    protected boolean statisticsOn;
    
    protected ColorRGBA backgroundColor = new ColorRGBA();
    protected ViewCamera camera;
    
    private boolean headless = false;
        
    protected boolean inOrthoMode = false;

    // the rendercontext we are currently working with
    protected RenderContext ctx = null;
    
    public static Renderer createMatchingRenderer(int width, int height) {
        return new Renderer();
    }
    
    public ViewCamera createCamera(int width, int height) {
        ViewCamera cam = new ViewCamera(width, height);
        if( camera != null )
            camera.copy(cam);
        return cam;
    }
    
    public boolean lockRenderer( RenderContext ctx ) {
        boolean locked = false;
        synchronized (this ) {
            if( this.ctx == null ) {
                this.ctx = ctx;
                locked = true;
            }
        }
        return locked;
    }
    
    public void unlockRenderer( ) {
        synchronized ( this ) {
            ctx = null;
        }
        //TODO: notify threads waiting for this renderer
        //notify();
    }
    
    public void setBackgroundColor(ColorRGBA c) {
        // if color is null set background to white.
        if (c == null) {
            backgroundColor.set(ColorRGBA.white);
        } else {
            backgroundColor.set(c);
        }
    }
    
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }

    /**
     * <code>displayBackBuffer</code> swaps the back buffer with the currently
     * displayed buffer. Swapping (page flipping) allows the renderer to display
     * a prerenderer display without any flickering.
     *  
     */
    public void displayBackBuffer() {
        // do nothing
    }

    /**
     * <code>getBackgroundColor</code> retrieves the clear color of the
     * current OpenGL context.
     * 
     * @see com.jme.renderer.Renderer#getBackgroundColor()
     * @return the current clear color.
     */
    public ColorRGBA getBackgroundColor() {
        return backgroundColor;
    }

    public void setCamera(ViewCamera cam) {
        this.camera = cam;
    }
    
    public ViewCamera getCamera() {
        return camera;
    }
    
    /**
     * <code>enableStatistics</code> will turn on statistics gathering.
     * 
     * @param value
     *            true to use statistics, false otherwise.
     */
    public void enableStatistics(boolean value) {
        statisticsOn = value;
        if (stats == null && statisticsOn) stats = new RenderStatistics();
    }
    
    /**
     * <code>clearStatistics</code> resets the vertices and triangles counter
     * for the statistics information.
     */
    public void clearStatistics() {
        if (stats != null) stats.clearStatistics();
    }

    /**
     * <code>getStatistics</code> returns a string value of the rendering
     * statistics information (number of triangles and number of vertices).
     * 
     * @return the string representation of the current statistics.
     */
    public RenderStatistics getStatistics() {
        return stats;
    }
    
    /**
     * <code>getStatistics</code> returns a string value of the rendering
     * statistics information (number of triangles and number of vertices).
     * 
     * @return the string representation of the current statistics.
     */
    public StringBuffer getStatistics(StringBuffer a) {
        a.setLength(0);
        if (stats != null) 
            stats.append(a);
        return a;
    }

    public boolean takeScreenShot(String string) {
        // do nothing
        return false;
    }
    
    public void cleanup() {
        // do nothing
    }
    
    /**
     * See Renderer.isHeadless()
     * 
     * @return boolean
     */
    public boolean isHeadless() {
        return headless;
    }

    /**
     * See Renderer.setHeadless()
     */
    public void setHeadless(boolean headless) {
        this.headless = headless;
    }
    
    public void clearBuffers() {
        
    }
    
    public boolean isInOrthoMode() {
        return inOrthoMode;
    }
    
    public void setOrtho() {
        inOrthoMode = true;
    }
    
    public void setOrthoCenter() {
        inOrthoMode = true;
    }
    public void unsetOrtho() {
        inOrthoMode = false;
    }    
    
    public RenderState createState(int type) {
        return null;
    }
    
    public void setCurrentColor(ColorRGBA setTo) {
    }
    public void setCurrentColor(float red, float green, float blue, float alpha) {
    }
    
    public void draw(TextBatch aThis) {
        //throw new UnsupportedOperationException("Not yet implemented");
    }
    
    public void draw(TriBatch aThis) {
        //throw new UnsupportedOperationException("Not yet implemented");
    }
    
    public void drawDirect(SceneElement s) {
        
    }
    
    public void reset() {
        
    }
    
    public void setPolygonOffset(float factor, float offset) {
        
    }
    
    public void clearPolygonOffset() {
        
    }
    
    public void setPositionOnlyMode(boolean posonly) {
        ctx.positionmode = posonly;
    }
    
    public boolean getPositionOnlyMode() {
        return ctx.positionmode;
    }
    
    public VertexBuffer allocVertexBuffer(VertexFormat format, int vertices, VertexBuffer orig) {
        if(orig!=null)
            return orig;
        VertexBuffer vb = new VertexBuffer();
        vb.setFormat(format);
        vb.createDataBuffer();
        return vb;
    }
    
    public IndexBuffer allocIndexBuffer(int indices, int vertices, IndexBuffer orig) {
        if(orig!=null)
            return orig;
        return IndexBuffer.createBuffer(indices, vertices, null);
    }
    
    public VertexBuffer mapVertexBuffer(VertexBuffer vb) {
        return vb;
    }
    
    public IndexBuffer mapIndexBuffer(IndexBuffer idx) {
        return idx;
    }
    
    public void unMapVertexBuffer(VertexBuffer buf) {
        
    }
    
    public void unMapIndexBuffer(VertexBuffer buf) {
        
    }

    public void releaseBuffer(VertexBuffer buf) {
        // do nothing
    }
    
    public void releaseBuffer(IndexBuffer buf) {
        // do nothing
    }

}
