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

package com.vlengine.test;

import com.vlengine.app.AppContext;
import com.vlengine.app.frame.Frame;
import com.vlengine.awt.CanvasMainGame;
import com.vlengine.awt.VLECanvas;
import com.vlengine.image.Texture;
import com.vlengine.math.FastMath;
import com.vlengine.math.Quaternion;
import com.vlengine.math.Vector3f;
import com.vlengine.model.BaseGeometry;
import com.vlengine.model.Box;
import com.vlengine.model.Quad;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.material.Material;
import com.vlengine.resource.ParameterMap;
import com.vlengine.scene.LodMesh;
import com.vlengine.scene.Spatial;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.state.CullState;
import com.vlengine.scene.state.TextureState;
import com.vlengine.system.VleException;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;


/**
 * <code>Test040SwingTest</code> is a test demoing the VLECanvas that enables
 * integrating the VL engine into an AWT/Swing application.
 * 
 * Note the Repaint thread and how you grab a canvas and add an implementor to it.
 * 
 * @author Joshua Slack
 * @author vear (Arpad Vekas) reworked for VL engine
 */

public class Test040SwingTest {
    private static final Logger logger = Logger.getLogger(Test040SwingTest.class
            .getName());

    int width = 640, height = 480;

    // Swing frame
    private SwingFrame frame;

    public Test040SwingTest() {
        frame = new SwingFrame();
        // center the frame
        frame.setLocationRelativeTo(null);
        
    }

    /**
     * Main Entry point...
     * 
     * @param args
     *            String[]
     */
    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.logp(Level.SEVERE, Test040SwingTest.class.toString(), "main(args)", "Exception", e);
        }
        // show frame
        Test040SwingTest t40 = new Test040SwingTest();
        t40.frame.setVisible(true);
    }

    // **************** SWING FRAME ****************

    // Our custom Swing frame... Nothing really special here.
    class SwingFrame extends JFrame {
        private static final long serialVersionUID = 1L;

        JPanel contentPane;
        JPanel mainPanel = new JPanel();
        VLECanvas comp = null;
        JButton coolButton = new JButton();
        JButton uncoolButton = new JButton();
        JPanel spPanel = new JPanel();
        JScrollPane scrollPane = new JScrollPane();
        JTree jTree1 = new JTree();
        JCheckBox scaleBox = new JCheckBox("Scale GL Image");
        JPanel colorPanel = new JPanel();
        JLabel colorLabel = new JLabel("BG Color:");
        CanvasMainGame impl;
        private javax.swing.JPanel view3d;
        
        boolean viewfocus = false;

        // Construct the frame
        public SwingFrame() {
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    if(comp!=null)
                        comp.cleanup();
                    dispose();
                }
            });

            init();
            pack();

            comp.addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent e) {
                    viewfocus = true;
                }
                public void focusLost(FocusEvent e) {
                    //viewfocus = false;
                }                 
            });
            
            // MAKE SURE YOU REPAINT SOMEHOW OR YOU WON'T SEE THE UPDATES...
            new Thread() {
                { setDaemon(true); }
                @Override
                public void run() {
                    while (true) {
                        if(comp!=null && comp.isVisible() && viewfocus) {
                            comp.repaint();
                        }
                        yield();
                    }
                }
            }.start();

        }

        // Component initialization
        private void init() {
            contentPane = (JPanel) this.getContentPane();
            contentPane.setLayout(new BorderLayout());

            mainPanel.setLayout(new GridBagLayout());

            setTitle("JME - SWING INTEGRATION TEST");

            // -------------GL STUFF------------------

            // make the canvas:
            try {
                comp = new VLECanvas(width, height);
            } catch(Exception e) {
                logger.log(Level.SEVERE, "Could not init canvas");
                System.exit(1);
            }



            // Important!  Here is where we add the guts to the panel:
            impl = new MyImplementor();
            impl.setMouseLookDragOnly(true);
            impl.setCanvas(comp);
            if(comp!=null) {
                comp.setMainGame(impl);
                comp.setUpdateInput(true);
                comp.setBounds(0, 0, 640, 480);
            }

            // -----------END OF GL STUFF-------------

            coolButton.setText("Cool Button");
            uncoolButton.setText("Uncool Button");

            colorPanel.setBackground(java.awt.Color.black);
            colorPanel.setToolTipText("Click here to change Panel BG color.");
            colorPanel.setBorder(BorderFactory.createRaisedBevelBorder());
            colorPanel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    final java.awt.Color color = JColorChooser.showDialog(
                            SwingFrame.this, "Choose new background color:",
                            colorPanel.getBackground());
                    if (color == null)
                        return;
                    colorPanel.setBackground(color);
                    comp.setBackground(color);
                }
            });

            scaleBox.setOpaque(false);
            scaleBox.setSelected(true);
            scaleBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (comp != null)
                        doResize();
                }
            });

            spPanel.setLayout(new BorderLayout());
            contentPane.add(mainPanel, BorderLayout.WEST);
            mainPanel.add(scaleBox,
                    new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER,
                            GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0,
                                    5), 0, 0));
            mainPanel.add(colorLabel,
                    new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER,
                            GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0,
                                    5), 0, 0));
            mainPanel.add(colorPanel, new GridBagConstraints(0, 2, 1, 1, 0.0,
                    0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    new Insets(5, 5, 0, 5), 25, 25));
            mainPanel.add(coolButton,
                    new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER,
                            GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0,
                                    5), 0, 0));
            mainPanel.add(uncoolButton,
                    new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER,
                            GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0,
                                    5), 0, 0));
            mainPanel.add(spPanel, new GridBagConstraints(0, 5, 1, 1, 1.0, 1.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(5, 5, 0, 5), 0, 0));
            spPanel.add(scrollPane, BorderLayout.CENTER);
            
            scrollPane.setViewportView(jTree1);
            if(comp!=null) {
                comp.setBounds(0, 0, width, height);
                view3d = new JPanel();
                contentPane.add(view3d, BorderLayout.CENTER);
                view3d.add(comp);
                // add a listener... if window is resized, we can do something about it.
                view3d.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent ce) {
                        doResize();
                    }
                });
            }
        }

        protected void doResize() {
            if (scaleBox != null && scaleBox.isSelected()) {
                comp.resizeCanvas(view3d.getWidth(), view3d.getHeight());
            } else {
                comp.resizeCanvas(width, height);
            }
        }

        // Overridden so we can exit when window is closed
        @Override
        protected void processWindowEvent(WindowEvent e) {
            super.processWindowEvent(e);
            if (e.getID() == WindowEvent.WINDOW_CLOSING) {
                System.exit(0);
            }
        }
    }

    
    // IMPLEMENTING THE SCENE:
    
    class MyImplementor extends CanvasMainGame {

        private Quaternion rotQuat;
        private float angle = 0;
        private Vector3f axis;
        private LodMesh box;
        long startTime = 0;
        long fps = 0;

        public MyImplementor() {
            super();
        }

        protected void setupTonOfQuad() {
            Texture tex = app.getResourceFinder().getTexture("tangents.jpg", ParameterMap.MAP_EMPTY);
            if ( tex == null ) {
                throw new VleException("Cannot load texture");
            }

            // create texturestate
            TextureState txs = new TextureState();
            txs.setTexture(tex);
            txs.setEnabled(true);

            CullState cs = new CullState();
            cs.setCullMode(CullState.CS_NONE);
            cs.setEnabled(true);

            // create material
            Material mat = new Material();
            mat.setRenderState(txs);
            mat.setRenderState(cs);
        
            // create quad geometry
            //Quad q = new Quad(100,100);
            //VBOInfo vi = new VBOInfo(false);
            //q.setVBOInfo(vi);
            //VBOAttributeInfo vboindex = new VBOAttributeInfo();
            //vboindex.useVBO = false;
            //q.getIndexBuffer().setVBOInfo(vboindex);
            
            Quad q1 = new Quad(100,100);
            // enable VBO for the quad
            // create VBO only once, we will not change the quad geometry
            q1.setVBOMode(BaseGeometry.VBO_LONGLIVED);
            q1.createVBOInfos();
            
            Quad q2 = new Quad(100,100);
            // enable VBO for the quad
            q2.setVBOMode(BaseGeometry.VBO_LONGLIVED);
            q2.createVBOInfos();
            
            for(int i=0; i< 1000; i++) {
                // create the renderable batch
                TriBatch t = new TriBatch();
                if(i%2==0)
                    t.setModel(q1);
                else
                    t.setModel(q2);
                t.setRenderQueueMode(RenderQueue.FILTER_OPAQUE);
                t.setCullMode(Spatial.CULL_NEVER);
                // set material to batch as opaque material
                t.setMaterial(mat);
                // creeate mesh
                LodMesh m = new LodMesh("Quad "+i);
                // add batch as default lod
                m.addBatch(0, t);
                m.getLocalTranslation().set(FastMath.rand.nextFloat()*1000-500,FastMath.rand.nextFloat()*1000-500,FastMath.rand.nextFloat()*1000-500);
                app.rootNode.attachChild(m);

            }
        }
        
        protected void setupBox() {
            Box q = new Box(new Vector3f(0,0,0), 100,100,100);
            
            TriBatch t = new TriBatch();
            t.setModel(q);
            t.setRenderQueueMode(RenderQueue.FILTER_OPAQUE);
            t.setCullMode(Spatial.CULL_NEVER);

            Texture tex = app.getResourceFinder().getTexture("tangents.jpg", ParameterMap.MAP_EMPTY);

            if ( tex == null ) {
                throw new VleException("Cannot load texture");
            }

            // create texturestate
            TextureState txs = new TextureState();
            txs.setTexture(tex);
            txs.setEnabled(true);

            // crate material
            Material mat = new Material();
            mat.setRenderState(txs);

            // set material to batch as opaque material
            t.setMaterial(mat);

            LodMesh m = new LodMesh("Box");
            m.addBatch(0, t);
            m.getLocalTranslation().set(0,0,-400);
            m.updateWorldVectors(app.nullUpdate);
            app.rootNode.attachChild(m);
            app.camn.getLocalTranslation().set(0, 0, 25);
            app.camn.lookAt(m.getWorldTranslation(), app.cam.getUp());
        }
        
        public void simpleInitGame(AppContext app) {

            // Normal Scene setup stuff...
            /*
            rotQuat = new Quaternion();
            axis = new Vector3f(1, 1, 0.5f);
            axis.normalizeLocal();
             */
             setupTonOfQuad();
            
            
        }

        public void simpleUpdate(Frame f) {
            /*
            float tpf = f.getUpdateContext().time;

            // Code for rotating the box... no surprises here.
            if (tpf < 1) {
                angle = angle + (tpf * 25);
                if (angle > 360) {
                    angle = 0;
                }
            }
            rotQuat.fromAngleNormalAxis(angle * FastMath.DEG_TO_RAD, axis);
            box.setLocalRotation(rotQuat);
            
            if (startTime > System.currentTimeMillis()) {
                fps++;
            } else {
                long timeUsed = 5000 + (startTime - System.currentTimeMillis());
                startTime = System.currentTimeMillis() + 5000;
                logger.info(fps + " frames in " + (timeUsed / 1000f) + " seconds = "
                                + (fps / (timeUsed / 1000f))+" FPS (average)");
                fps = 0;
            }
             */				
        }
    }
}