/*
 * Copyright to the original author or authors.
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
package org.cochise.codecritic.support.scm;

import org.cochise.codecritic.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides basic support for a {@link SCM}
 *
 * @author Dennis Reedy
 */
public abstract class AbstractSCM implements SCM {
    /**
     * Additional directories to exclude     
     */
    private String exclude;
    /**
     * SCM branch to use
     */
    private String branch;
    /**
     * Whether to include test source
     */
    private boolean includeTests;
    /**
     * The repository to use;
     */
    private String repository;
    private File workingDirectory;
    private final List<ChangeSet> changeSets = new ArrayList<ChangeSet>();
    private final List<SourceFile> javaSources = new ArrayList<SourceFile>();
    private final List<SourceFile> nonJavaSources = new ArrayList<SourceFile>();
    private final List<ProgressListener> listeners = new ArrayList<ProgressListener>();

    public List<ChangeSet> getChangeSets() {
        return changeSets;
    }

    public List<SourceFile> getJavaSources() {
        return javaSources;
    }

    public List<SourceFile> getOtherSources() {
        return nonJavaSources;
    }

    public String getBranch() {
        return branch;
    }

    protected void setBranch(String branch) {
        this.branch = branch;
    }

    public String getRepository() {
        return repository;
    }

    protected void setRepository(String repository) {
        if(repository.contains("github.com"))
            this.repository = repository.trim().substring(0, repository.length()-5)+"/";
        else
            this.repository = repository.trim();
    }

    public boolean includeTests() {
        return includeTests;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public void initialize(File workingDirectory, String... options) throws CodeCriticException {
        this.workingDirectory = workingDirectory;
        for(String option : options) {
            if(option.startsWith("exclude")) {
                String[] parts = option.split("=");
                exclude = parts[1];
            }
            if(option.startsWith("branch")) {
                String[] parts = option.split("=");
                branch = parts[1];
            }
            if(option.startsWith("includeTests")) {
                String[] parts = option.split("=");
                includeTests = Boolean.parseBoolean(parts[1]);
            }
            if(option.startsWith("log")) {
                String[] parts = option.split("=");
                includeTests = Boolean.parseBoolean(parts[1]);
            }
        }
    }

    public void registerProgressListener(ProgressListener progressListener) {
        if(!listeners.contains(progressListener))
            listeners.add(progressListener);
    }

    protected void sendInfoMessage(String message) {
        for(ProgressListener listener : listeners) {
            listener.info(message);
        }
    }

    protected void sendDebugMessage(String message) {
        for(ProgressListener listener : listeners) {
            listener.debug(message);
        }
    }

    protected void processSourceFile(SourceFile sourceFile, ChangeSet changeSet, List<SourceFile> sourceFileList) {
        if(excluded(sourceFile.getFile())) {
            sendDebugMessage("Excluding "+sourceFile);
            return;
        }
        if(!sourceFileList.contains(sourceFile)) {
            sourceFile.addChangeSet(changeSet);
            sourceFileList.add(sourceFile);
        } else {
            int ndx = sourceFileList.indexOf(sourceFile);
            SourceFile sFile = sourceFileList.get(ndx);
            sFile.addChangeSet(changeSet);
            sourceFileList.set(ndx, sFile);
        }
    }

    protected boolean excluded(final String file) {
        boolean exclude = false;
        if(this.exclude==null) {
            return false;
        }
        if(this.exclude.length()==0) {
            return false;
        }
        String[] exclusions = this.exclude.split(",");
        for(String x : exclusions) {
            if(file.contains(x)) {
                exclude = true;
                break;
            }
        }
        sendDebugMessage("exclude "+file+"? "+exclude);
        return exclude;
    }

    protected String debugReport(final String header, final List<SourceFile> list) {
        StringBuilder builder = new StringBuilder();
        builder.append(header);
        builder.append("\n");
        builder.append("-------------------------");
        builder.append("\n");
        int length = 0;
        for(SourceFile s : list) {
            if(s.getFile().length()>length) {
                length = s.getFile().length();
            }
        }
        for(SourceFile s : list) {
            builder.append(String.format("%-"+length+"s", s.getFile()));
            builder.append(" ");
            StringBuilder changeSetBuilder = new StringBuilder();
            for(ChangeSet changeSet : s.getChangeSets()) {
                if(changeSetBuilder.length()>0)
                    changeSetBuilder.append(", ");
                changeSetBuilder.append(changeSet.getLink());
            }
            builder.append(changeSetBuilder.toString());
            builder.append("\n");
        }
        builder.append("\n");
        return builder.toString();
    }
}
