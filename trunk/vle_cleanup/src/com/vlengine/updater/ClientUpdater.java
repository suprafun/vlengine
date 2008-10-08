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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.DigestException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author lex (Aleksey Nikiforov)
 *
 */
public class ClientUpdater extends AutoUpdater {

    private static final Logger log = Logger.getLogger(
            ClientUpdater.class.getName());

    public ClientUpdater() {
        super();
    }
    
    public ClientUpdater(File localDir, File localDescriptor, URL serverDir,
            URL serverDescriptorUrl)
    {
        super(localDir, localDescriptor, serverDir, serverDescriptorUrl);
    }

    /**
     * @see com.vlengine.updater.AutoUpdater#manageFileRecords()
     */
    protected RecordList[] manageFileRecords()
            throws UpdateException {
        boolean generatedRecords = false;

        destRecords = loadLocalRecords(true);

        if (destRecords == null) {
            try {
                destRecords = recordManager.generateLocalList(localDir, null);
            } catch (IOException e) {
                throw new UpdateException(
                        "Unable to generate local file list.", e);
            }
            generatedRecords = true;
        }

        sourceRecords = loadServerDescriptor(serverDescriptorUrl);

        RecordList[] diff = RecordManager.findDifference(
                destRecords, sourceRecords);
        if (generatedRecords) {
            destRecords.clearAll(diff[2]);
            diff[2] = new RecordList();
        }

        // if no update is needed
        if (diff[0].getRecords().isEmpty() && diff[1].getRecords().isEmpty()
                && diff[2].getRecords().isEmpty())
        {
            if (generatedRecords) {
                saveLocalRecords(sourceRecords);
            }
            return null;
        }

        return diff;
    }

    protected void preUpdate() throws UpdateException {
        if (!localDir.exists() && !localDir.mkdirs()) {
            throw new UpdateException("Could not create the directory '" +
                    localDir.getAbsolutePath() + "'.");
        }

        if (user != null && password != null) {
            Authenticator.setDefault(new Authenticator() {

                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            user, password.toCharArray());
                }
            });
        }
    }

    protected void postUpdate() {
        Authenticator.setDefault(null);
    }

    protected void manageUpdatedRecords() {

    }

    /**
     * @param record
     * @return true is copy operation succeeded, false if cancelled
     * @throws UpdateException
     */
    protected boolean copy(FileRecord record) throws UpdateException {
        try {

            InputStream in = getRemoteUrl(
                    record.getFile()).openStream();
            File currentFile = getLocalFile(record.getFile());

            if (currentFile.exists() && !currentFile.delete()) {
                throw new UpdateException(
                        "Unable to delete the old file '" +
                        record.getFile() + "'.");
            }

            File parentDir = currentFile.getParentFile();
            if (parentDir != null) {
                if (!parentDir.exists() && !parentDir.mkdirs()) {
                    throw new UpdateException(
                            "Could not create the directory '" +
                            parentDir.getAbsolutePath() + "'.");
                }
            }

            OutputStream out = new FileOutputStream(currentFile);
            String md5 = recordManager.copyWithMd5(in, out);

            if (md5 != null && !md5.equals(record.getMd5())) {
                currentFile.delete();

                throw new UpdateException(
                        "The checksum does not match for '" +
                        record.getFile() + "'.");

            }

            return (md5 != null);

        } catch (MalformedURLException e) {
            throw new UpdateException(
                    "Unable to resolve file '" +
                    record.getFile() + "'.", e);
        } catch (DigestException e) {
            throw new UpdateException(
                    "Unable to compute the checksum for '" +
                    record.getFile() + "'.", e);
        } catch (IOException e) {
            throw new UpdateException(
                    "Read/Write error while updating '" +
                    record.getFile() + "'.", e);
        }
    }

    protected void remove(FileRecord record) {
        File localFile = getLocalFile(record.getFile());
        if (!localFile.exists()) {
            log.log(Level.WARNING, "Deleting file that doesn't exists '" +
                    record.getFile() + "'.");
        } else if (!localFile.delete()) {
            log.log(Level.WARNING, "Unable to delete file '" +
                    record.getFile() + "'.");
        }
    }

    protected void removeEmptyDirs() {
        removeEmptyDirs(localDir);
    }

    private void removeEmptyDirs(File dir) {
        File[] files = dir.listFiles();

        for (int i = 0; i < files.length; i++) {
            File file = files[i];

            if (file.isDirectory()) {
                removeEmptyDirs(file);
                if (file.listFiles().length == 0) {
                    file.delete();
                }
            }
        }
    }
}
