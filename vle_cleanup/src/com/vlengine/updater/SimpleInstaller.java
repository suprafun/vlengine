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

import java.awt.EventQueue;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;
import javax.swing.UIManager;


/**
 * 
 * @author lex (Aleksey Nikiforov)
 */
public class SimpleInstaller {

	private static final Logger log = Logger.getLogger(
			SimpleInstaller.class.getName());
	
	// TODO attributes should be read from a config file
    private String appName = "X-Shift";
    private String appMain = "com.pxc.xshift.ui.login.LoginFrame";
    
    private String localDescriptorKey = "localDescriptor";
    private String localDescriptor;
    private String localDescriptorFile = "xshift.idx";
    
    private String serverDescriptor =
            "http://www.xshiftonline.com/demo/xshift.idx";
    
    private String user = "xshiftonline";
    private String password = "teammember";
    
    private Preferences prefs;
    private int guiUpdateInterval = 100;

    public SimpleInstaller() {
        prefs = Preferences.userRoot().node(appName);
    }

    public void installUpdate() throws IOException {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to set look and feel.", e);
        }

        final SimpleUpdaterFrame frame = new SimpleUpdaterFrame();

        try {
            EventQueue.invokeAndWait(new Runnable() {

                public void run() {
                    frame.init();
                    frame.setVisible(true);
                }
            });
        } catch (InterruptedException e) {
            log.log(Level.SEVERE, "Unexcpected exception.", e);
            return;
        } catch (InvocationTargetException e) {
            log.log(Level.SEVERE, "Unexcpected exception.", e);
            return;
        }

        boolean installOnly = false;
        localDescriptor = prefs.get("localDescriptor", null);

        File instDir = null;
        if (localDescriptor != null) {
            instDir = new File(localDescriptor).getAbsoluteFile().getParentFile();
        }

        if (instDir == null || !instDir.exists()) {
            JOptionPane.showMessageDialog(frame,
                    "Select a directory to install " + appName + ". (Note: " +
                    "All data in the install directory will be overwritten.)");

            instDir = new FileChooserUtil().promptDirectory(
                    frame,
                    System.getProperty("user.dir"));

            if (instDir == null) {
                frame.dispose();
                return;
            }

            localDescriptor = new File(
                    instDir, localDescriptorFile).getAbsolutePath();

            try {
                prefs.put(localDescriptorKey, localDescriptor);
                prefs.flush();
            } catch (BackingStoreException e) {
                log.log(Level.SEVERE,
                        "Error while saving installation folder info.", e);

                JOptionPane.showMessageDialog(frame,
                        "Error while saving installation folder info.",
                        "Install Error", JOptionPane.ERROR_MESSAGE);

                return;
            }
        }

        if (!instDir.equals(new File(System.getProperty("user.dir")))) {
            installOnly = true;
        }

        ClientUpdater updater = new ClientUpdater(null,
                new File(localDescriptor),
                null,
                new URL(serverDescriptor));
        updater.setAuthentication(user, password);

        frame.setUpdateStatus(updater.getUpdateStatus(), guiUpdateInterval);

        try {
            if (updater.update()) {
                if (installOnly) {
                    JOptionPane.showMessageDialog(frame, "You have installed " +
                        appName + " into '" + instDir.getAbsolutePath() + "'.");
                } else {
                    Class.forName(appMain).getDeclaredMethod(
                            "main",
                            new String[]{}.getClass())
                        .invoke(null, (Object) new String[]{});
                }
            }

        } catch (UpdateException e) {
            log.log(Level.SEVERE,
                    "Error while updating the application.", e);
            JOptionPane.showMessageDialog(frame, e.getMessage(),
                    "Update Error", JOptionPane.ERROR_MESSAGE);

        } catch (Exception e) {
            log.log(Level.SEVERE,
                    "Error while launching the application.", e);

            JOptionPane.showMessageDialog(frame,
                    "Unable to launch the application. " +
                    "Please submit your log file with an error report.",
                    "Error", JOptionPane.ERROR_MESSAGE);

        } finally {
            frame.dispose();
        }
    }

    public static void main(String[] args) {
    	try {
			PrintStream out = new PrintStream(new File("xshift.log"));
			out.println("X-SHIFT DEBUG LOG\n");
			
			StreamHandler handler =  new StreamHandler(out,
					new SimpleFormatter());
			handler.setLevel(Level.WARNING);
			
			Logger.getLogger("").addHandler(handler);
		} catch (FileNotFoundException e) { }
		
		try {
			new SimpleInstaller().installUpdate();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Unexcpected Exception", e);
		}
    }
}
