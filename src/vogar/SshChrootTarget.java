/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package vogar;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.ListIterator;

public class SshChrootTarget extends SshTarget {
    private final String chrootDir;

    private final String targetProcessWrapper;

    public SshChrootTarget(Log log, String hostAndPort, String chrootDir) {
        super(log, hostAndPort);
        this.chrootDir = chrootDir;
        this.targetProcessWrapper = "unshare --user --map-root-user chroot " + chrootDir + " sh -c ";
    }

    @Override
    public void await(File directory) {
        super.await(chrootToRoot(directory));
    }

    @Override
    public List<File> ls(File directory) throws FileNotFoundException {
        // Add chroot prefix to searched directory.
        List<File> files = super.ls(chrootToRoot(directory));
        // Remove chroot prefix from files found in directory.
        ListIterator<File> iterator = files.listIterator();
        while (iterator.hasNext()) {
            File file = iterator.next();
            iterator.set(rootToChroot(file));
        }
        return files;
    }

    @Override
    public void rm(File file) {
        super.rm(chrootToRoot(file));
    }

    @Override
    public void mkdirs(File file) {
        super.mkdirs(chrootToRoot(file));
    }

    @Override
    public void push(File local, File remote) {
        super.push(local, chrootToRoot(remote));
    }

    @Override
    public void pull(File remote, File local) {
        super.pull(chrootToRoot(remote), local);
    }

    @Override
    protected ImmutableList<String> targetProcessPrefix() {
        return super.targetProcessPrefix();
    }

    @Override
    protected String targetProcessWrapper() {
        return this.targetProcessWrapper;
    }

    /**
     * Convert a file relative to the chroot dir to a file relative to
     * the device's filesystem "absolute" root.
     */
    private File chrootToRoot(File file) {
        return new File(chrootDir + "/" + file.getPath());
    }

    /**
     * Convert a file relative to the device's filesystem "absolute"
     * root to a file relative to the chroot dir .
     */
    private File rootToChroot(File file) throws PathnameNotUnderChrootException {
        String pathname = file.getPath();
        if (!pathname.startsWith(chrootDir)) {
            throw new PathnameNotUnderChrootException(pathname, chrootDir);
        }
        return new File(pathname.substring(chrootDir.length()));
    }

    /**
     * Exception thrown when a pathname does not represent a file
     * under the chroot directory.
     */
    private static class PathnameNotUnderChrootException extends RuntimeException {

        public PathnameNotUnderChrootException(String pathname, String chrootDir) {
            super("Pathname " + pathname + " does not represent a file under chroot directory "
                    + chrootDir);
        }
    }
}
