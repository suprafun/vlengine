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
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;

import com.vlengine.updater.UpdateStatus.Status;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;

/**
 *
 * @author lex (Aleksey Nikiforov)
 */
public class SimpleUpdaterFrame extends JFrame {

    /**
     * 
     */
    private static final long serialVersionUID = -2776670290890723805L;

    private static final Logger log = Logger.getLogger(
            SimpleUpdaterFrame.class.getName());
    
    private UpdateStatus updateStatus;
    private StringBuilder sb = new StringBuilder();
    private DecimalFormat format = new DecimalFormat("0.00");

    private Runnable guiUpdate = new Runnable() {
        public void run() {

            if (!needsDisplay(updateStatus)) {
                currentFileLabel.setText("Pending...");
                fileProgressBar.setValue(0);

                totalSizeLabel.setText("Pending...");
                overallProgressBar.setValue(0);

            } else {
                updateInfo();
            }
        }

        private void updateInfo() {
            sb.delete(0, sb.length());

            synchronized (updateStatus) {
                currentFileLabel.setText(
                        updateStatus.getCurrentFile().getFile());
                fileProgressBar.setValue(
                        (int) (updateStatus.getFileProgressPercent() * 100));
                overallProgressBar.setValue(
                        (int) (updateStatus.getJobProgressPercent() * 100));

                float mb = 1024 * 1024;
                sb.append(format.format(
                        (updateStatus.getJobProgressBytes() +
                        updateStatus.getFileProgressBytes()) / mb));
                sb.append("MB / ");
                sb.append(
                        format.format(updateStatus.getTotalSize() / mb));
                sb.append("MB");
            }

            totalSizeLabel.setText(sb.toString());
        }
    };
    
    /** Creates new form SimpleUpdaterFrame */
    public SimpleUpdaterFrame() {
    }
    
    /** Must be call on AWT thread. */
    public void init() {
        initComponents();
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int w = this.getSize().width;
        int h = this.getSize().height;
        setBounds((screenSize.width-w)/2, (screenSize.height-h)/2, w, h);
        
        this.setMinimumSize(new Dimension(this.getSize()));
        this.setMaximizedBounds(new Rectangle(0, 0, Integer.MAX_VALUE, 0));
    }

    public void setUpdateStatus(final UpdateStatus status,
            final int updateInterval)
    {
        if (updateStatus != null) {
            throw new IllegalStateException("Update status is already set.");
        }

        updateStatus = status;
        final SimpleUpdaterFrame frame = this;

        Thread thread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {

                        if (updateStatus.isFinished() || !frame.isVisible()) {
                            break;
                        }
                        EventQueue.invokeAndWait(guiUpdate);
                        Thread.sleep(updateInterval);

                    } catch (InterruptedException e) {

                    } catch (InvocationTargetException e) {
                        log.log(Level.SEVERE, "Unexpected exception.", e);
                        break;
                    }
                }
            }
        });

        thread.start();
    }

    private boolean needsDisplay(UpdateStatus status) {
        synchronized (status) {
            if (status.getStatus() == Status.INACTIVE || status.isFinished()) {
                return false;
            }
        }

        return true;
    }

    public void cancelUpdate() {
        if (updateStatus != null) {
            updateStatus.cancel();
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        updateProgressPanel = new javax.swing.JPanel();
        fileProgressBar = new javax.swing.JProgressBar();
        fixedFileLabel = new javax.swing.JLabel();
        fixedTotalLabel = new javax.swing.JLabel();
        overallProgressBar = new javax.swing.JProgressBar();
        currentFileLabel = new javax.swing.JLabel();
        totalSizeLabel = new javax.swing.JLabel();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("X-Shift Installer");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        fileProgressBar.setStringPainted(true);

        fixedFileLabel.setText("File:");
        fixedFileLabel.setPreferredSize(null);

        fixedTotalLabel.setText("Total:");
        fixedTotalLabel.setPreferredSize(null);

        overallProgressBar.setStringPainted(true);

        currentFileLabel.setText("currentFile");

        totalSizeLabel.setText("XXX Mb");

        javax.swing.GroupLayout updateProgressPanelLayout = new javax.swing.GroupLayout(updateProgressPanel);
        updateProgressPanel.setLayout(updateProgressPanelLayout);
        updateProgressPanelLayout.setHorizontalGroup(
            updateProgressPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(updateProgressPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(updateProgressPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(overallProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 488, Short.MAX_VALUE)
                    .addGroup(updateProgressPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fixedTotalLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(totalSizeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 452, Short.MAX_VALUE))
                    .addGroup(updateProgressPanelLayout.createSequentialGroup()
                        .addComponent(fixedFileLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(currentFileLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 452, Short.MAX_VALUE))
                    .addComponent(fileProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 488, Short.MAX_VALUE))
                .addContainerGap())
        );

        updateProgressPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {fixedFileLabel, fixedTotalLabel});

        updateProgressPanelLayout.setVerticalGroup(
            updateProgressPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(updateProgressPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(updateProgressPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fixedFileLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(currentFileLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fileProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(updateProgressPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fixedTotalLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(totalSizeLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(overallProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        cancelButton.setText("Cancel");
        cancelButton.setToolTipText("Cancel the update and exit.");
        cancelButton.setPreferredSize(null);
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(updateProgressPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(430, Short.MAX_VALUE)
                .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(updateProgressPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        getAccessibleContext().setAccessibleName("X-Shift Updater");

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
            cancelUpdate();
    }//GEN-LAST:event_formWindowClosing

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        cancelUpdate();
    }//GEN-LAST:event_cancelButtonActionPerformed
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                SimpleUpdaterFrame frame = new SimpleUpdaterFrame();
                frame.init();
                frame.setVisible(true);
            }
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel currentFileLabel;
    private javax.swing.JProgressBar fileProgressBar;
    private javax.swing.JLabel fixedFileLabel;
    private javax.swing.JLabel fixedTotalLabel;
    private javax.swing.JProgressBar overallProgressBar;
    private javax.swing.JLabel totalSizeLabel;
    private javax.swing.JPanel updateProgressPanel;
    // End of variables declaration//GEN-END:variables
}
