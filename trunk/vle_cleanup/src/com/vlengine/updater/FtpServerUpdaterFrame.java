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

package com.vlengine.updater;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 *
 * @author lex (Aleksey Nikiforov)
 */
public class FtpServerUpdaterFrame extends javax.swing.JFrame {
    
    private static final Logger log =
            Logger.getLogger(FtpServerUpdaterFrame.class.getName());
    
    private static final String PROPS_FILE = "updater.cfg";
    private static final String COMMENT = "FtpServerUpdater config file.";
    
    private static final String LOCAL_DIR_KEY = "localDir";
    private static final String SERVER_URL_KEY = "serverUrl";
    private static final String SERVER_DIR_KEY = "serverDir";
    private static final String DESCRIPTOR_KEY = "descriptor";
    private static final String SERVER_LOCK_KEY = "lockServer";
    private static final String USER_KEY = "userName";
    
    private Properties props;
    
    private FileChooserUtil chooser;
    private FtpServerUpdater updater;
    private int guiUpdateInterval = 100;
    
    /** Creates new form FtpServerUpdaterFrame */
    public FtpServerUpdaterFrame() {
        props = new Properties();
        props.setProperty(SERVER_LOCK_KEY, "false");
        
        try {
            File pf = new File(PROPS_FILE);
            if (pf.exists()) {
                props.load(new FileInputStream(pf));
            }
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Unable to load config.", ex);
        }
        
        updater = new FtpServerUpdater();
    }
    
    /** Must be called on AWT thread. */
    public void init() {
        initComponents();
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int w = this.getSize().width;
        int h = this.getSize().height;
        setBounds((screenSize.width-w)/2, (screenSize.height-h)/2, w, h);
        
        this.setMinimumSize(new Dimension(this.getSize()));
        this.setMaximizedBounds(new Rectangle(0, 0, Integer.MAX_VALUE, 0));
        
        srcDirTextField.setText(props.getProperty(LOCAL_DIR_KEY, ""));
        
        addressTextField.setText(props.getProperty(SERVER_URL_KEY, ""));
        destDirTextField.setText(props.getProperty(SERVER_DIR_KEY, ""));
        descriptorTextField.setText(props.getProperty(DESCRIPTOR_KEY, ""));
        if (Boolean.parseBoolean(props.getProperty(SERVER_LOCK_KEY))) {
            addressTextField.setEditable(false);
            destDirTextField.setEditable(false);
            descriptorTextField.setEditable(false);
        }
        
        String user = props.getProperty(USER_KEY);
        if (user != null) {
            userTextField.setText(user);
            rememberCheckBox.setSelected(true);
        }
        
        String descriptor = props.getProperty(DESCRIPTOR_KEY);
        String localDir = props.getProperty(LOCAL_DIR_KEY);
        decideUpdateButtonText(localDir, descriptor);
    }
    
    private void decideUpdateButtonText(String localDir, String descriptor) {
        if (descriptor != null && !"".equals(descriptor)
                && localDir != null && !"".equals(localDir))
        {
            descriptor = descriptor.substring(descriptor.lastIndexOf('/') + 1);
            File descFile = new File(new File(localDir), descriptor);
            updater.setLocalDescriptor(descFile);
            if (updater.willResume()) {
                updateButton.setText("Resume");
            } else {
                updateButton.setText("Update");
            }
        }
    }
    
    /** Must be called on AWT thread. */
    public void saveProps() {
        props.setProperty(LOCAL_DIR_KEY, srcDirTextField.getText());
        if (!Boolean.parseBoolean(props.getProperty(SERVER_LOCK_KEY))) {
            props.setProperty(SERVER_URL_KEY, addressTextField.getText());
            props.setProperty(SERVER_DIR_KEY, destDirTextField.getText());
            props.setProperty(DESCRIPTOR_KEY, descriptorTextField.getText());
        }
        
        if (rememberCheckBox.isSelected()) {
            props.setProperty(USER_KEY, userTextField.getText());
        } else {
            props.remove(USER_KEY);
        }
        
        try {
            props.store(new FileOutputStream(new File(PROPS_FILE)), COMMENT);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Unable to save config.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sourcePanel = new javax.swing.JPanel();
        srcDirLabel = new javax.swing.JLabel();
        srcDirTextField = new javax.swing.JTextField();
        selectButton = new javax.swing.JButton();
        destPanel = new javax.swing.JPanel();
        addressLabel = new javax.swing.JLabel();
        destDirLabel = new javax.swing.JLabel();
        descriptorLabel = new javax.swing.JLabel();
        addressTextField = new javax.swing.JTextField();
        descriptorTextField = new javax.swing.JTextField();
        destDirTextField = new javax.swing.JTextField();
        userLabel = new javax.swing.JLabel();
        userTextField = new javax.swing.JTextField();
        passwordLabel = new javax.swing.JLabel();
        passwordField = new javax.swing.JPasswordField();
        rememberCheckBox = new javax.swing.JCheckBox();
        exitButton = new javax.swing.JButton();
        updateButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Ftp Server Updater");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setMinimumSize(null);
        setName("FtpServerUpdaterFrame"); // NOI18N

        sourcePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Local"));

        srcDirLabel.setText("Directory:  ");
        srcDirLabel.setMaximumSize(new java.awt.Dimension(120, 14));
        srcDirLabel.setMinimumSize(new java.awt.Dimension(60, 14));
        srcDirLabel.setOpaque(true);

        srcDirTextField.setMinimumSize(new java.awt.Dimension(4, 24));
        srcDirTextField.setPreferredSize(null);

        selectButton.setText("Select");
        selectButton.setMaximumSize(new java.awt.Dimension(72, 24));
        selectButton.setMinimumSize(new java.awt.Dimension(72, 24));
        selectButton.setPreferredSize(null);
        selectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout sourcePanelLayout = new javax.swing.GroupLayout(sourcePanel);
        sourcePanel.setLayout(sourcePanelLayout);
        sourcePanelLayout.setHorizontalGroup(
            sourcePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sourcePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(srcDirLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(srcDirTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 223, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(selectButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        sourcePanelLayout.setVerticalGroup(
            sourcePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sourcePanelLayout.createSequentialGroup()
                .addGroup(sourcePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(srcDirLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(selectButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(srcDirTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        destPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Server"));

        addressLabel.setText("Address:");
        addressLabel.setMaximumSize(new java.awt.Dimension(120, 14));
        addressLabel.setMinimumSize(new java.awt.Dimension(60, 14));
        addressLabel.setPreferredSize(null);

        destDirLabel.setText("Directory:");
        destDirLabel.setMaximumSize(new java.awt.Dimension(120, 14));
        destDirLabel.setMinimumSize(new java.awt.Dimension(60, 14));
        destDirLabel.setPreferredSize(null);

        descriptorLabel.setText("Descriptor:");
        descriptorLabel.setMaximumSize(new java.awt.Dimension(120, 14));
        descriptorLabel.setPreferredSize(null);

        addressTextField.setMinimumSize(new java.awt.Dimension(4, 24));
        addressTextField.setPreferredSize(null);

        descriptorTextField.setMinimumSize(new java.awt.Dimension(4, 24));
        descriptorTextField.setPreferredSize(null);
        descriptorTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                descriptorTextFieldFocusLost(evt);
            }
        });

        destDirTextField.setMinimumSize(new java.awt.Dimension(4, 24));
        destDirTextField.setPreferredSize(null);

        userLabel.setText("User:");
        userLabel.setMaximumSize(new java.awt.Dimension(120, 14));
        userLabel.setMinimumSize(new java.awt.Dimension(60, 14));
        userLabel.setPreferredSize(null);

        userTextField.setMinimumSize(new java.awt.Dimension(4, 24));
        userTextField.setPreferredSize(null);

        passwordLabel.setText("Password:");
        passwordLabel.setMaximumSize(new java.awt.Dimension(120, 14));
        passwordLabel.setMinimumSize(new java.awt.Dimension(60, 14));
        passwordLabel.setPreferredSize(null);

        passwordField.setMinimumSize(new java.awt.Dimension(4, 24));
        passwordField.setPreferredSize(null);

        rememberCheckBox.setText("Remember");
        rememberCheckBox.setMaximumSize(new java.awt.Dimension(160, 22));
        rememberCheckBox.setPreferredSize(null);

        javax.swing.GroupLayout destPanelLayout = new javax.swing.GroupLayout(destPanel);
        destPanel.setLayout(destPanelLayout);
        destPanelLayout.setHorizontalGroup(
            destPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(destPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(destPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(addressLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(destDirLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(descriptorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(userLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(passwordLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(destPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(destPanelLayout.createSequentialGroup()
                        .addGroup(destPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(passwordField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(userTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 168, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(rememberCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(addressTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 299, Short.MAX_VALUE)
                    .addComponent(destDirTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 299, Short.MAX_VALUE)
                    .addComponent(descriptorTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 299, Short.MAX_VALUE))
                .addContainerGap())
        );
        destPanelLayout.setVerticalGroup(
            destPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(destPanelLayout.createSequentialGroup()
                .addGroup(destPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addressLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addressTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(destPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(destDirLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(destDirTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(destPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(descriptorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(descriptorTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(destPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE, false)
                    .addComponent(userLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(userTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rememberCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(destPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(passwordLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(passwordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        exitButton.setText("Exit");
        exitButton.setMaximumSize(new java.awt.Dimension(72, 24));
        exitButton.setMinimumSize(new java.awt.Dimension(72, 24));
        exitButton.setPreferredSize(null);
        exitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitButtonActionPerformed(evt);
            }
        });

        updateButton.setText("Update");
        updateButton.setPreferredSize(null);
        updateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sourcePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(destPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(updateButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(exitButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sourcePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(destPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(exitButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(updateButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitButtonActionPerformed
        this.dispose();
    }//GEN-LAST:event_exitButtonActionPerformed

    private void selectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectButtonActionPerformed
        if (chooser == null) chooser = new FileChooserUtil();
        File dir = chooser.promptDirectory(this, srcDirTextField.getText());
        if (dir != null) {
            String localDir = dir.getAbsolutePath();
            srcDirTextField.setText(localDir);
            decideUpdateButtonText(localDir, descriptorTextField.getText());
        }
    }//GEN-LAST:event_selectButtonActionPerformed

    private void updateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateButtonActionPerformed
        saveProps();
        
        // local dir
        if ("".equals(srcDirTextField.getText())) {
            JOptionPane.showMessageDialog(this,
                    "Source folder is not selected.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        File localDir = new File(srcDirTextField.getText());
        if (!localDir.isDirectory()) {
            JOptionPane.showMessageDialog(this,
                    "Selected source folder is a file.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!localDir.exists()) {
            JOptionPane.showMessageDialog(this,
                    "Selected source folder does not exists.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // server url
        URL serverUrl;
        try {
            serverUrl = new URL(this.addressTextField.getText());
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(this,
                    "Invalid server address url.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // server dir
        URL serverDirUrl;
        String serverDir = this.destDirTextField.getText();
        if ("".equals(serverDir)) {
            JOptionPane.showMessageDialog(this,
                        "Server directory is not selected.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } else {
            try {
                serverDirUrl = new URL(serverUrl, serverDir);
            } catch (MalformedURLException e) {
                JOptionPane.showMessageDialog(this,
                        "Invalid server directory url.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        
        // local and server descriptor
        String descriptor = this.descriptorTextField.getText();
        File localDescFile;
        if (!"".equals(descriptor)) {
            String descFileStr = descriptor.substring(
                    descriptor.lastIndexOf('/') + 1);
            localDescFile = new File(localDir, descFileStr);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Server descriptor is not specified.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        URL serverDescUrl;
        try {
            serverDescUrl = new URL(serverUrl, descriptor);
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(this,
                    "Invalid server descriptor url.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        updater.setLocalDir(localDir);
        updater.setLocalDescriptor(localDescFile);
        updater.setServerDescriptorUrl(serverDescUrl);
        updater.setServerDirUrl(serverDirUrl);
        
        String user = this.userTextField.getText();
        String password = new String(this.passwordField.getPassword());
        if (!"".equals(user) && !"".equals(password)) {
            updater.setAuthentication(user, password);
        }
        
        final FtpServerUpdaterFrame mainFrame = this;
        mainFrame.setEnabled(false);
        final SimpleUpdaterFrame progressFrame = new SimpleUpdaterFrame();
        progressFrame.init();
        progressFrame.setUpdateStatus(
                updater.getUpdateStatus(),
                guiUpdateInterval);
        progressFrame.setVisible(true);
        
        new Thread(new Runnable() {
             public void run() {

                boolean updated = false;
                boolean exception = false;
                String errorMessage = "";

                try {
                    updated = updater.update();
                    
                } catch (UpdateException e) {
                    log.log(Level.SEVERE,
                            "Error while updating.", e);
                    errorMessage = e.getMessage();
                    exception = true;
                    
                } catch (Exception e) {
                    log.log(Level.SEVERE,
                            "Unexpected exception.", e);
                    errorMessage = e.getMessage();
                    exception = true;
                    
                } finally {
                    
                    try {
                        EventQueue.invokeAndWait(new Runnable() {
                            public void run() {
                                progressFrame.dispose();
                                mainFrame.setEnabled(true);
                                mainFrame.decideUpdateButtonText(
                                    mainFrame.srcDirTextField.getText(),
                                    mainFrame.descriptorTextField.getText());
                            }
                        });
                    } catch (InterruptedException ex) {
                        log.log(Level.SEVERE, "Unexpected exception.", ex);
                    } catch (InvocationTargetException ex) {
                       log.log(Level.SEVERE, "Unexpected exception.", ex);
                    }
                }

                if (exception) {
                    JOptionPane.showMessageDialog(mainFrame, errorMessage,
                            "Update Error", JOptionPane.ERROR_MESSAGE);
                } else if (updated) {
                    JOptionPane.showMessageDialog(mainFrame,
                            "Updated successfully.");
                } else {
                    JOptionPane.showMessageDialog(mainFrame,
                            "Update was interrupted. You can resume " +
                            "later by using the same source directory.");
                }
            }
        }).start();
    }//GEN-LAST:event_updateButtonActionPerformed

    private void descriptorTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_descriptorTextFieldFocusLost
        decideUpdateButtonText(srcDirTextField.getText(),
                descriptorTextField.getText());
    }//GEN-LAST:event_descriptorTextFieldFocusLost
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            PrintStream out = new PrintStream(new File("updater.log"));
            out.println("UPDATER LOG\n");

            StreamHandler handler = new StreamHandler(out,
                    new SimpleFormatter());
            handler.setLevel(Level.WARNING);

            Logger.getLogger("").addHandler(handler);
        } catch (FileNotFoundException e) {
            log.log(Level.SEVERE, "Unable to create log file.", e);
        }
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to set look and feel.", e);
        }

        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    FtpServerUpdaterFrame frame = new FtpServerUpdaterFrame();
                    frame.init();
                    frame.setVisible(true);
                }
            });
        } catch (Exception e) {
            log.log(Level.SEVERE, "Unexcpected Exception", e);
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel addressLabel;
    private javax.swing.JTextField addressTextField;
    private javax.swing.JLabel descriptorLabel;
    private javax.swing.JTextField descriptorTextField;
    private javax.swing.JLabel destDirLabel;
    private javax.swing.JTextField destDirTextField;
    private javax.swing.JPanel destPanel;
    private javax.swing.JButton exitButton;
    private javax.swing.JPasswordField passwordField;
    private javax.swing.JLabel passwordLabel;
    private javax.swing.JCheckBox rememberCheckBox;
    private javax.swing.JButton selectButton;
    private javax.swing.JPanel sourcePanel;
    private javax.swing.JLabel srcDirLabel;
    private javax.swing.JTextField srcDirTextField;
    private javax.swing.JButton updateButton;
    private javax.swing.JLabel userLabel;
    private javax.swing.JTextField userTextField;
    // End of variables declaration//GEN-END:variables

}
