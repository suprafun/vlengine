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

package com.vlengine.app;

import com.vlengine.app.frame.Frame;
import com.vlengine.app.state.ForwardRenderPath;
import com.vlengine.app.state.GameStateNode;
import com.vlengine.app.state.ThreadTaskManager;
import com.vlengine.input.InputHandler;
import com.vlengine.input.InputSystem;
import com.vlengine.input.LWJGLKeyInput;
import com.vlengine.input.LWJGLMouseInput;
import com.vlengine.light.DirectionalLight;
import com.vlengine.light.LightSorterGameState;
import com.vlengine.math.Vector3f;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.material.DefaultMaterialLib;
import com.vlengine.renderer.material.ShaderMaterialLib;
import com.vlengine.resource.ResourceCreator;
import com.vlengine.resource.ResourceFinder;
import com.vlengine.scene.CameraNode;
import com.vlengine.scene.LightNode;
import com.vlengine.scene.SetNode;
import com.vlengine.scene.Spatial;
import com.vlengine.scene.Text;
import com.vlengine.scene.control.KeyboardMoveController;
import com.vlengine.scene.control.MouseLookController;
import com.vlengine.scene.control.UpdateContext;
import com.vlengine.system.DisplaySystem;
import com.vlengine.system.PropertiesDialog;
import com.vlengine.system.PropertiesIO;
import com.vlengine.system.VleException;
import com.vlengine.thread.LocalContext;
import com.vlengine.util.IntList;
import com.vlengine.util.TextureManager;
import com.vlengine.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GLContext;

/**
 *
 * @author vear (Arpad Vekas)
 */
public abstract class MainGame {
   
    public static final String VLENGINE_VERSION = "0.1";
    
    protected static final Logger logger = Logger.getLogger(MainGame.class.getName());
    
    protected AppContext app;

    protected StringBuffer updateBuffer = new StringBuffer(30);
    
    /**
     * Location of the font for jME's text at the bottom
     */
    public static String fontLocation = Text.DEFAULT_FONT;

    protected IntList startedFrames = new IntList();
    /**
     * This is used to recieve getStatistics calls.
     */
    //protected StringBuffer tempBuffer = new StringBuffer();
    
    public String getVersion() {
        return VLENGINE_VERSION;
    }
    
    protected void getAttributes() {
        app.properties = new PropertiesIO("properties.cfg");
        boolean loaded = app.properties.load(app.conf);

        if ((!loaded) || ( app.conf.dialog ) ) {

            PropertiesDialog dialog = new PropertiesDialog(app.properties, (String)null, app.conf);

            while (dialog.isVisible()) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    logger.warning( "Error waiting for dialog system, using defaults.");
                }
            }

            if (dialog.isCancelled()) {
                //System.exit(0);
                finish();
            }
        }
    }
    
    protected void initApp() {
        // create the application context
        app = new AppContext();
        app.conf = new Config();
        // set it into the thread context
        LocalContext.getContext().app = app;
        app.mainGame = this;
    }

    public void gameLoop() {

        /*
        app.frame[0].startFrame();
        
        // handle input events prior to updating the scene
        // - some applications may want to put this into update of
        // the game state
        if(app.updateInput)
            app.inputSystem.update();

        // call pre-frame event in gamestates
        app.getGameStates().preFrame(app);        

        // update game state, do not use interpolation parameter
        // TODO: convert to multiple frames
        app.frame[0].update();

        simpleUpdate(app.frame[0]);

        // TODO: convert to multiple frames
        app.frame[0].cullscene();

        app.frame[0].render();

        if( app.isShowFps() ) {
            updateBuffer.setLength( 0 );
            updateBuffer.append( "FPS: " );
            int fr = (int) app.timer.getFrameRate();
            if(fr<1000)
                updateBuffer.append( "0" );
            if(fr<100)
                updateBuffer.append( "0" );
            if(fr<10)
                updateBuffer.append( "0" );
            updateBuffer.append( fr );
            app.fps.print( updateBuffer );
            float fpx = DisplaySystem.getDisplaySystem().getWidth();
            float fpy = DisplaySystem.getDisplaySystem().getHeight();
            app.fps.getLocalTranslation().set(0, fpy-20,0);
            app.fps.getWorldTranslation().set(0, fpy-20,0);
            app.fps.getLocalScale().set(0.8f, 0.8f, 0.8f);
            app.fps.getWorldScale().set(0.8f, 0.8f, 0.8f);
            app.fps.updateGeometricState(app.frame[0].getUpdateContext(), true);
            

            updateBuffer.setLength( 0 );
            updateBuffer.append( app.display.getRenderer().getStatistics( tempBuffer ) );
            // Send the fps to our fps bar at the bottom.
            app.stats.print( updateBuffer );
        }

        app.frame[0].endFrame();
        */
       // go over frames
        if(app.isMultithreaded()) {
            boolean hasStarted = true;
            startedFrames.clear();
            
            while(hasStarted) {
                hasStarted = false;
                
                boolean hasNoStarted = true;
                boolean hasEnded = false;
                int readyFrame = -1;
                for(int i=0; i<app.frame.length; i++) {
                    int fstate = app.frame[i].getState();
                    if(fstate==Frame.FRAME_FINISHED || fstate == Frame.FRAME_INCOMPLETE) {
                        // a frame thats ready to process
                        if(readyFrame == -1 && !startedFrames.contains(i)) {
                            readyFrame = i;
                        }
                    } else if(fstate>Frame.FRAME_INCOMPLETE && fstate <Frame.FRAME_MATERIAL) {
                        // we cannot yet start a frame
                        hasNoStarted = false;
                    } else if(fstate == Frame.FRAME_ENDED) {
                        hasEnded = true;
                    }
                }
                // do we have an ended frame?
                if(hasEnded) {
                    app.finished = true;
                }
                // can we start a frame?//
                if(hasNoStarted && readyFrame >= 0) { // >=
                    // is the thread started?
                    if(app.frame[readyFrame].getState() == Frame.FRAME_INCOMPLETE) {
                        new Thread(app.frame[readyFrame]).start();
                        hasStarted = true;
                        startedFrames.add(readyFrame);
                    } else {
                        // start given frame
                        app.frame[readyFrame].setState(Frame.FRAME_STARTING);
                    }
                }
            }
            
            if(startedFrames.size()>0) {
                for(int i=0; i<startedFrames.size(); i++) {
                    app.frame[startedFrames.get(i)].setState(Frame.FRAME_STARTING);
                }
            }
        } else {
            app.frame[0].setState(Frame.FRAME_STARTING);
            app.frame[0].run();
        }
    }
    
    public void start() {
        logger.info( "Application started.");
        try {
            
            initApp();

            getAttributes();

            if (!app.finished) {
                
                initSystem();
                
                assertDisplayCreated();
                 
                initGame();
                
                // initilize proper lighting system
                initRenderPath();

                if(LocalContext.isUseMultithreading()) {
                    try {
                        Display.releaseContext();
                    } catch(Exception e) {

                    }
                }
                // main loop
                while (!app.finished && !app.display.isClosing()) {
                    
                    gameLoop();
                    
                    Thread.yield();
                }
                app.finished = true;
                // wait for threads to stop
                if(LocalContext.isUseMultithreading()) {
                    boolean allstopped = false;
                    while(!allstopped) {
                        allstopped = true;
                        for(int i=0; i<app.frame.length; i++) {
                            if(app.frame[i].getState()>Frame.FRAME_INCOMPLETE && app.frame[i].getState()<Frame.FRAME_ENDED) {
                                allstopped = false;
                            }
                        }
                        Thread.yield();
                    }
                }
            }
        } catch (Throwable t) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "start()", "Exception in game loop", t);
        }

        cleanup(app);
        logger.info( "Application ending.");

        quit();
    }

    protected void initInput() {
        // by default initialize LWJGL input systems
        // this method should be overriden in canvas application
        app.keyInput = new LWJGLKeyInput();
        app.mouseInput = new LWJGLMouseInput();
    }

    protected void initDisplay() {
        /**
         * Get a DisplaySystem acording to the renderer selected in the
         * startup box.
         */
        app.display = DisplaySystem.getDisplaySystem( );

        app.display.setBits(app.conf.depthBits, app.conf.stencilBits, app.conf.alphaBits, app.conf.samples);

        /** Create a window with the startup box's information. */
        app.display.createWindow( app.conf.display_width, app.conf.display_height,
                app.conf.display_depth, app.conf.display_freq, app.conf.display_fullscreen );

        /**
         * Create a camera specific to the DisplaySystem that works with the
         * display's width and height
         */
        app.cam = app.display.getRenderer().createCamera( app.display.getWidth(),
                app.display.getHeight() );
        
        
    }

    protected void initSystem() throws VleException {
        // initialize resource finder
        ResourceFinder rf = new ResourceFinder(app);
        // set the class responsible for instantiating resources
        rf.setResourceCreator(new ResourceCreator());
        // set the default material library
        rf.addRenderLib(new DefaultMaterialLib());
        if(!app.conf.graphNoShaders)
            rf.addRenderLib(new ShaderMaterialLib());
        rf.refreshResFiles();
        // let AppContext hold the reference
        app.setResourceFinder(rf);
                
        logger.info(getVersion());
        try {
            initDisplay();
            logger.info("Running on: " + app.display.getDisplaySignature());
        } catch ( VleException e ) {
            /**
             * If the displaysystem can't be initialized correctly, exit
             * instantly.
             */
            logger.log(Level.SEVERE, "Could not create displaySystem", e);
            System.exit( 1 );
        }

        initInput();
        
        // initialize the input system
        app.inputSystem = new InputSystem(app);
        app.inputSystem.activate();
        
        /** Set a black background. */
        app.display.getRenderer().setBackgroundColor( ColorRGBA.black.clone() );

        /** Set up how our camera sees. */
        cameraPerspective();
        Vector3f loc = new Vector3f( 0.0f, 0.0f, 25.0f );
        Vector3f left = new Vector3f( -1.0f, 0.0f, 0.0f );
        Vector3f up = new Vector3f( Vector3f.UNIT_Y );
        Vector3f dir = new Vector3f( 0.0f, 0f, -1.0f );
        /** Move our camera to a correct place and orientation. */
        app.cam.setFrame( loc, left, up, dir );
        /** Signal that we've changed our camera's location/frustum. */
        app.cam.update();
        /** Assign the camera to this renderer. */
        // camera is only set before rendering
        //display.getRenderer().setCamera( cam );

        /** Get a high resolution timer for FPS updates. */
        app.timer = Timer.getTimer();

        /** Sets the title of our display. */
        String className = getClass().getName();
        if ( className.lastIndexOf( '.' ) > 0 ) className = className.substring( className.lastIndexOf( '.' )+1 );
        app.display.setTitle( className );
        /**
         * Signal to the renderer that it should keep track of rendering
         * information.
         */
        app.display.getRenderer().enableStatistics( true );
        
        //FrameCounter fc = new FrameCounter();

        if(LocalContext.isUseMultithreading()) 
            app.frame = new Frame[Frame.MAX_FRAMES];
        else
            app.frame = new Frame[1];
        for(int i=0; i< app.frame.length; i++) {
            app.frame[i] = new Frame(i, app);
            //app.frame[i].setRenderer(DisplaySystem.getDisplaySystem().getRenderer());
        }

        //

        // set up root gamestate
        app.setRootGameState( new GameStateNode("RootState") );
        
        // set up the threaded task manager
        app.glQueue = new ThreadTaskManager();
        app.glQueue.setupManager(app);
        
    }
    
    protected void assertDisplayCreated() throws VleException {
        if (app.display == null) {
            logger.severe( "Display system is null.");

            throw new VleException("Window must be created during" + " initialization.");
        }
        if (!app.display.isCreated()) {
            logger.severe( "Display system not initialized.");

            throw new VleException("Window must be created during" + " initialization.");
        }
    }
    
    protected void initKeybindings() {
                // set up camera node
        app.camn = new CameraNode("Camera Node");
        app.camn.setCamera(app.cam);
        
        // create mouse look controller
        MouseLookController mlc = new MouseLookController(app.mouseInput);
        mlc.setInputSystem(app.inputSystem);
        mlc.setUpCommands();
        mlc.setControlledNode(app.camn);
        mlc.setSpeed(2);
        mlc.setActive(true);
        app.camn.addController(mlc);
        
        // create keyboard move controller
        KeyboardMoveController kmc = new KeyboardMoveController();
        kmc.setInputSystem(app.inputSystem);
        kmc.setUpCommands();
        kmc.setRotationSource(app.camn);
        kmc.setControlledNode(app.camn);
        kmc.setSpeed(50);
        kmc.setActive(true);
        app.camn.addController(kmc);
        
        app.rootNode.attachChild(app.camn);

        // create handler for basic commands
        app.input = new InputHandler(app);
        //app.input.setMainGame(this);
        app.input.setInputSystem(app.inputSystem);
        app.input.setUpBasicCommands();
        // enable debugging commands also
        app.input.setUpDefaultDebugCommands();
    }

    protected void initGame() {
        
        app.frame[0].startFrame();
        
        // create dummy contexts for preloading
        app.nullUpdate = new UpdateContext();
        app.nullUpdate.frame = app.frame[0];
        app.nullUpdate.frameId = 0;
        app.nullRender = new RenderContext();
        app.nullRender.frame = app.frame[0];
                
        /** Create rootNode */
        app.rootNode = new SetNode( "rootNode" );

        // Then our font Text object.
        /** This is what will actually have the text at the bottom. */
        app.fps = Text.createDefaultTextLabel( "FPS" );
        app.fps.setCullMode( Spatial.CullMode.NEVER );
        
        
        app.stats = Text.createDefaultTextLabel( "Stats" );
        app.stats.setCullMode( Spatial.CullMode.NEVER );
        app.stats.getLocalTranslation().set(0, 0, 0);

        // Finally, a stand alone node (not attached to root on purpose)
        app.fpsNode = new SetNode( "Debug node" );
        //fpsNode.setRenderState( fps.getRenderState( RenderState.RS_ALPHA ) );
        //fpsNode.setRenderState( fps.getRenderState( RenderState.RS_TEXTURE ) );
        app.fpsNode.attachChild( app.fps );
        app.fpsNode.attachChild( app.stats );
        app.fpsNode.setCullMode( Spatial.CullMode.NEVER );

        // setup keybindings and mouse-look
        initKeybindings();

        /** Let derived classes initialize. */
        simpleInitGame(app);
        
        app.timer.reset();

        // compile data trought the scene
        //app.rootNode.compile();
        //app.fpsNode.compile();
        
        /**
         * Update geometric and rendering information for both the rootNode and
         * fpsNode.
         */
        app.rootNode.updateGeometricState( app.nullUpdate, true );
        app.fpsNode.updateGeometricState( app.nullUpdate, true );
        app.fpsNode.lockBounds();
        app.fpsNode.lockTransforms();
        app.fpsNode.lockBranch();

        app.timer.reset();
        
        //app.nullUpdate = null;
        //app.nullRender = null;
        
        app.frame[0].endFrame();
    }

    public LightNode createLight() {
        // ---- LIGHTS
        /** Set up a basic, default light. */
        DirectionalLight light = new DirectionalLight();
        light.setDiffuse( new ColorRGBA( 0.75f, 0.75f, 0.75f, 0.75f ) );
        light.setAmbient( new ColorRGBA( 0.5f, 0.5f, 0.5f, 1.0f ) );
        //light.setLocation( new Vector3f( 100, 100, 100 ) );
        light.setDirection(new Vector3f(0.5f, -1f, 0.5f).normalizeLocal());
        light.setEnabled( true );

        return new LightNode("demoLight", light);
    }

    protected void initRenderPath() {
        if(app.renderPath == null) {
            app.renderPath = new ForwardRenderPath(app);
            app.renderPath.setup();
        }
    }

    public void debugToggleParallel() {
        if ( app.cam.isParallelProjection() ) {
            cameraPerspective();
        }
        else {
            cameraParallel();
        }
    }
    
    protected void cameraPerspective() {
        app.cam.setFrustumPerspective( app.conf.p_frustum_fovy, (float) app.display.getWidth()
                / (float) app.display.getHeight(), 1f, app.conf.view_frustrum_far );
        app.cam.setParallelProjection( false );
        app.cam.update();
    }

    protected void cameraParallel() {
        app.cam.setParallelProjection( true );
        float aspect = (float) app.display.getWidth() / app.display.getHeight();
        app.cam.setFrustum( -100, 1000, -50 * aspect, 50 * aspect, -50, 50 );
        app.cam.update();
    }
    
    protected void cleanup(AppContext app) {
        logger.info("Cleaning up resources.");
        try {
            Display.makeCurrent();
          } catch (Exception ex) {
          }
        
        try {
            GLContext.useContext(app.display.getOGLContext());
          } catch (Exception ex) {
          }
        
          if(app.display != null && app.display.getRenderer() != null)
                app.display.getRenderer().lockRenderer(null);
            TextureManager.doTextureCleanup();
            if (app != null) {
                ResourceFinder rf = app.getResourceFinder();
                if (rf != null) {
                    rf.cleanup();
                }
            }
            if (app.display != null && app.display.getRenderer() != null) {
                app.display.getRenderer().cleanup();
            }
            if (app.keyInput != null) {
                app.keyInput.destroyIfInitialized();
            }
            if (app.mouseInput != null) {
                app.mouseInput.destroyIfInitialized();
            }
            if(app.display != null && app.display.getRenderer() != null)
                app.display.getRenderer().unlockRenderer();
            //JoystickInput.destroyIfInitalized();
        
        //JoystickInput.destroyIfInitalized();
    }
    
    public void finish() {
      app.finished = true;
    }

    protected abstract void simpleInitGame(AppContext app);
    
    public void simpleUpdateFromFrame(Frame f) {
        simpleUpdate(f);
    }
    
    protected abstract void simpleUpdate(Frame f);

    protected void quit() {
        if (app.display != null)
            app.display.close();
        System.exit(0);
    }

    protected void simpleRender() {
        
    }
    
    public AppContext getAppContext() {
        return app;
    }
    
}
