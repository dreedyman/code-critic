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
package org.cochise.codecritic.support.scm.git;

import org.cochise.codecritic.ChangeSet;
import org.cochise.codecritic.CodeCriticException;
import org.cochise.codecritic.ExecHelper;
import org.cochise.codecritic.SourceFile;
import org.cochise.codecritic.support.scm.AbstractSCM;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides {@link org.cochise.codecritic.support.scm.SCM} support for Git.
 *
 * @author Dennis Reedy
 */
public class Git extends AbstractSCM {
    private final StringBuilder logCommandBuilder = new StringBuilder();

    @Override
    public void initialize(File workingDirectory, String... options) throws CodeCriticException {
        super.initialize(workingDirectory, options);
        String gitLogCommand = System.getProperty("git");
        File gitFile = new File(System.getProperty("user.dir"), ".git");
        if(gitFile.exists()) {
            List<Origin> origins = parseConfig(gitFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String since = null;
            boolean getInput = false;
            StringBuilder requestBuilder = new StringBuilder();
            if(origins.isEmpty()) {
                getInput = true;
                requestBuilder.append("Enter the branch <since> to base the comparison on: ");
            } else if(origins.size()==1) {
                since = origins.get(0).getId();
                setRepository(origins.get(0).getUrl());
            } else {
                getInput = true;
                requestBuilder.append("\nEnter the branch <since> to base the comparison on:\n");
                AtomicInteger count = new AtomicInteger(1);
                for(Origin origin : origins) {
                    requestBuilder.append("\n");
                    requestBuilder.append(count.getAndIncrement()).append(": ").append(origin);
                }
            }
            if(getInput) {
                while(true) {
                    try {
                        System.out.println(requestBuilder.toString()+"\n");
                        System.out.print("Selection: ");
                        String input = br.readLine();
                        try {
                            int choice = Integer.parseInt(input);
                            if(choice<=origins.size()) {
                                since = origins.get(choice-1).getId();
                                setRepository(origins.get(choice-1).getUrl());
                                break;
                            } else {
                                System.out.println("Invalid choice.");
                            }
                        } catch(NumberFormatException e) {
                            System.out.println("Invalid choice.");
                        }
                    } catch (IOException e) {
                        throw new CodeCriticException("Error reading from input, code-critic will now exit.", e);
                    }
                }
            }
            if(gitLogCommand==null || gitLogCommand.length()==0) {
                logCommandBuilder.append("git log ").append(since).append("..");
            } else {
                logCommandBuilder.append(gitLogCommand);
            }
        } else {
            throw new CodeCriticException("This is not a git project, code-critic will now exit.");
        }
    }

    private List<Origin> parseConfig(File git) throws CodeCriticException {
        List<Origin> origins = new ArrayList<Origin>();
        File config = new File(git, "config");
        if(config.exists()) {
            try {
                Scanner scanner = new Scanner(new FileReader(config));

                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if(line.startsWith("[remote")) {
                        StringTokenizer st = new StringTokenizer(line.substring("[remote".length()), "\"] ");
                        String remote = st.nextToken();
                        String url;
                        while(!line.startsWith("url")) {
                            line = scanner.nextLine().trim();
                        }
                        st = new StringTokenizer(line.substring("url".length()), "= ");
                        url = st.nextToken();
                        Origin origin = new Origin(remote, url);

                        /* Look for duplicate urls */
                        boolean duplicate = false;
                        for(Origin o : origins) {
                            if(origin.equals(o)) {
                                duplicate = true;
                                break;
                            }
                        }
                        if(!duplicate) {
                            origins.add(origin);
                        }
                    }
                }
            } catch (IOException e) {
                throw new CodeCriticException("Unable to read "+config.getPath());
            }
        }
        return origins;
    }

    public void runLog() throws CodeCriticException {
        String branch = getBranch();
        if(branch==null && System.getProperty("git")==null) {
            //git log master..rio-5.0 --name-only
            String branchOutput = ExecHelper.doExec("git branch", null, getWorkingDirectory()).trim();
            StringTokenizer st = new StringTokenizer(branchOutput, "\n");
            while(st.hasMoreTokens()) {
                String s = st.nextToken();
                if(s.startsWith("*")) {
                    s = s.substring(1);
                    branch = s.trim();
                    break;
                }
            }
            logCommandBuilder.append(branch);
            logCommandBuilder.append(" --name-only");
        }
        setBranch(branch);
        sendInfoMessage("Using branch " + branch);
        sendInfoMessage("Using repository " + getRepository());
        sendInfoMessage("Using log command \""+logCommandBuilder.toString()+"\"");

        sendDebugMessage(logCommandBuilder.toString());
        String output = ExecHelper.doExec(logCommandBuilder.toString(), null, getWorkingDirectory());
        sendDebugMessage(output);

        boolean processFiles = false;
        boolean processDescription = false;
        ChangeSet changeSet = null;
        StringTokenizer st = new StringTokenizer(output, "\n");
        AtomicInteger number = new AtomicInteger();
        while(st.hasMoreTokens()) {
            String line = st.nextToken();
            if(line.startsWith("commit")) {
                processFiles = false;
                if(changeSet!=null) {
                    getChangeSets().add(changeSet);
                }
                String changeSetString = line.substring("commit".length()).trim();
                changeSet = new ChangeSet(number.incrementAndGet(), getRepository()+"commit/", changeSetString);
            }
            if(line.startsWith("Author:")) {
                line = line.substring("Author:".length()).trim();
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

            if(processFiles) {
                String file = line.trim();
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

            if(processDescription) {
                line = line.trim();
                if(line.length()>0) {
                    processDescription = false;
                    if(changeSet!=null) {
                        changeSet.setMessage(line);
                    }
                    processFiles = true;
                }
            }

            if(line.startsWith("Date:")) {
                line = line.substring("Date:".length()).trim();
                if(changeSet!=null) {
                    changeSet.setDate(line);
                }
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
        //throw new CodeCriticException("Git support not implemented yet");
    }

    class Origin {
        String id;
        String url;

        Origin(String id, String url) {
            this.id = id;
            this.url = url;
        }

        public String getId() {
            return id;
        }

        public String getUrl() {
            if(!url.endsWith("/"))
                url = url+"/";
            return url;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Origin origin = (Origin) o;
            return !(url != null ? !url.equals(origin.url) : origin.url != null);
        }

        @Override
        public int hashCode() {
            return url != null ? url.hashCode() : 0;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(id).append(":").append(url);
            return builder.toString();
        }
    }
}
