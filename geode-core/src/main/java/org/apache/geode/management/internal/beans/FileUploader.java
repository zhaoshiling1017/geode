/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.geode.management.internal.beans;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.healthmarketscience.rmiio.RemoteInputStream;
import com.healthmarketscience.rmiio.RemoteInputStreamClient;
import org.apache.commons.io.IOUtils;

public class FileUploader implements FileUploaderMBean {
  @Override
  public List<String> uploadFile(Map<String, RemoteInputStream> remoteFiles) throws IOException {
    List<String> stagedFiles = new ArrayList<>();

    Set<PosixFilePermission> perms = new HashSet<>();
    perms.add(PosixFilePermission.OWNER_READ);
    perms.add(PosixFilePermission.OWNER_WRITE);
    perms.add(PosixFilePermission.OWNER_EXECUTE);
    Path tempDir =
        Files.createTempDirectory("uploaded-", PosixFilePermissions.asFileAttribute(perms));

    for (String filename : remoteFiles.keySet()) {
      File stagedFile = new File(tempDir.toString(), filename);
      FileOutputStream fos = new FileOutputStream(stagedFile);

      InputStream input = RemoteInputStreamClient.wrap(remoteFiles.get(filename));
      IOUtils.copyLarge(input, fos);

      fos.close();
      input.close();

      stagedFiles.add(stagedFile.getAbsolutePath());
    }

    return stagedFiles;
  }
}
