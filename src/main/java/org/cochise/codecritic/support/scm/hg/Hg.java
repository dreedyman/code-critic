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
package org.cochise.codecritic.support.scm.hg;

import org.cochise.codecritic.*;
import org.cochise.codecritic.support.scm.AbstractSCM;
import org.cochise.codecritic.ExecHelper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Provides {@link org.cochise.codecritic.support.scm.SCM} support for Mercurial.
 *
 * @author Dennis Reedy
 */
public class Hg extends AbstractSCM {
    private final StringBuilder logCommandBuilder = new StringBuilder();    

    public void initialize(File workingDirectory, String... options) throws CodeCriticException {
        super.initialize(workingDirectory, options);
        String hg = System.getProperty("hg");
        File hgFile = new File(System.getProperty("user.dir"), ".hg");
        if(hgFile.exists()) {
            if(hg ==null || hg.length()==0) {
                logCommandBuilder.append("hg log -v");
            } else {
                if(!hg.startsWith("hg") || !hg.contains("log") || !hg.contains("-v")) {
                    throw new CodeCriticException("The provided hg command must start with hg log and run verbosely (use \"-v\")");
                }
                logCommandBuilder.append(hg);
            }
        } else {
            throw new CodeCriticException("This is not a mercurial project, code-critic will now exit.");
        }
        String repository = null;
        for(File f : hgFile.listFiles()) {
            if("hgrc".equals(f.getName())) {
                try {
                    Scanner scanner = new Scanner(new FileReader(f));
                    scanner.nextLine();
                    String repositoryLine = scanner.nextLine();
                    int ndx = repositoryLine.indexOf("=");
                    sendDebugMessage("Parsing "+repositoryLine);
                    repository = repositoryLine.substring(ndx+1).trim();
                    break;
                } catch (IOException e) {
                    throw new CodeCriticException("Unable to read "+f.getPath());
                }
            }
        }
        if(repository==null) {
            throw new CodeCriticException("Unable to determine repository");
        }
        if(!repository.endsWith("/"))
            repository = repository+"/";
        setRepository(repository);
    }
    
    public void runLog() throws CodeCriticException {
        String branch = getBranch();
        if(branch==null)
            branch = ExecHelper.doExec("hg branch", null, getWorkingDirectory()).trim();
        setBranch(branch);
        sendInfoMessage("Using branch " + branch);
        sendInfoMessage("Using repository " + getRepository());
        if(!"default".equals(branch))
            logCommandBuilder.append(" -b ").append(branch);

        sendDebugMessage(logCommandBuilder.toString());
        String output = ExecHelper.doExec(logCommandBuilder.toString(), null, getWorkingDirectory());
        sendDebugMessage(output);

        boolean processDescription = false;
        ChangeSet changeSet = null;
        StringTokenizer st = new StringTokenizer(output, "\n");
        while(st.hasMoreTokens()) {
            String line = st.nextToken();
            if(line.startsWith("changeset:")) {
                if(changeSet!=null) {
                    throw new CodeCriticException("Unknown state");
                }
                line = line.substring("changeset:".length()).trim();
                int ndx = line.indexOf(":");
                int number = Integer.parseInt(line.substring(0, ndx));
                String changeSetString = line.substring(ndx+1);
                changeSet = new ChangeSet(number, getRepository()+"rev/", changeSetString);
            }
            if(line.startsWith("user:")) {
                line = line.substring("user:".length()).trim();
                if(changeSet!=null) {
                    int ndx = line.indexOf("<");
                    if(ndx!=-1) {
                        String user = line.substring(0, ndx);
                        String email = line.substring(ndx+1, line.length()-1);
                        changeSet.setDeveloper(user);
                        changeSet.setEmail(email);
                    } else {
                        changeSet.setDeveloper(line);
                    }
                }
            }
            if(line.startsWith("date:")) {
                line = line.substring("date:".length()).trim();
                if(changeSet!=null) {
                    changeSet.setDate(line);
                }
            }
            if(line.startsWith("files:")) {
                line = line.substring("files:".length());
                String[] files = line.split(" ");
                for(String file : files) {
                    if(file.length()==0)
                        continue;
                    File f = new File(getWorkingDirectory(), file);
                    if(!f.exists())
                        continue;
                    if(!includeTests() && file.contains("src"+ File.separator+"test")) {
                        continue;
                    }
                    SourceFile sourceFile = new SourceFile(file);
                    if(file.endsWith(".java")) {
                        processSourceFile(sourceFile, changeSet, getJavaSources());
                    } else {
                        processSourceFile(sourceFile, changeSet, getOtherSources());
                    }
                }
            }
            if(processDescription) {
                processDescription = false;
                if(changeSet!=null) {
                    changeSet.setMessage(line);
                    getChangeSets().add(changeSet);
                }
                changeSet = null;
            }
            if(line.startsWith("description:")) {
                processDescription = true;
            }
        }
        if(getJavaSources().isEmpty()) {
            sendInfoMessage("There are no files to analyze.");
            return;
        }
        sendInfoMessage("Total number of files modified in this branch "+getJavaSources().size()+getOtherSources().size());
        sendInfoMessage("Total number of Java files: "+getJavaSources().size());
        sendDebugMessage(debugReport("Java sources changed ", getJavaSources()));
        sendDebugMessage(debugReport("Other files changed ", getOtherSources()));
    }
}
