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

import org.cochise.codecritic.*;
import org.cochise.codecritic.support.scm.AbstractSCM;
import org.fusesource.jansi.HtmlAnsiOutputStream;

import java.io.*;
import java.nio.file.Files;
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
    private Config config;
    private WhileWeWaitPrintSomePeriods waiter;
    private Thread waiterThread;


    @Override
    public void initialize(File workingDirectory, String... options) throws CodeCriticException {
        super.initialize(workingDirectory, options);
        String gitLogCommand = System.getProperty("git");
        File gitFile = new File(workingDirectory, ".git");
        if(gitFile.exists()) {
            config = parseConfig(gitFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String since;
            if(config.origins.size()==1) {
                setRepository(config.origins.get(0).getUrl());
            } else {
                StringBuilder requestBuilder = new StringBuilder();
                requestBuilder.append("\nEnter the origin (repository) to use:\n");
                AtomicInteger count = new AtomicInteger(1);
                for(Origin origin : config.origins) {
                    requestBuilder.append("\n");
                    requestBuilder.append(count.getAndIncrement()).append(": ").append(origin);
                }
                int choice = getInput(requestBuilder.toString(), config.origins.size(), br);
                setRepository(config.origins.get(choice).getUrl());
            }
            if(config.branches.size()==1) {
                since = config.branches.get(0);
            } else {
                StringBuilder requestBuilder = new StringBuilder();
                requestBuilder.append("\nEnter the branch to base the comparison on:\n");
                AtomicInteger count = new AtomicInteger(1);
                for(String b : config.branches) {
                    requestBuilder.append("\n");
                    requestBuilder.append(count.getAndIncrement()).append(": ").append(b);
                }
                since = config.branches.get(getInput(requestBuilder.toString(), config.branches.size(), br));
            }

            if(gitLogCommand==null || gitLogCommand.length()==0) {
                logCommandBuilder.append("git log ").append(since).append("..");
                config.fromBranch = since;
            } else {
                logCommandBuilder.append(gitLogCommand);
            }
        } else {
            throw new CodeCriticException("This is not a git project, code-critic will now exit.");
        }
    }

    private Config parseConfig(File git) throws CodeCriticException {
        List<Origin> origins = new ArrayList<>();
        List<String> branches = new ArrayList<>();
        File config = new File(git, "config");
        if(config.exists()) {
            try(Scanner scanner = new Scanner(new FileReader(config))) {
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
                    if(line.startsWith("[branch")) {
                        String[] parts = line.split(" \"");
                        String branch = parts[1].substring(0, parts[1].indexOf("\""));
                        branches.add(branch);
                    }
                }
            } catch (IOException e) {
                throw new CodeCriticException("Unable to read "+config.getPath());
            }
        }
        String currentBranch;
        try {
            currentBranch = currentBranch(git);
        } catch (IOException e) {
            throw new CodeCriticException("Unable to obtain current branch", e);
        }
        branches.remove(currentBranch);
        setBranch(currentBranch);
        return new Config(origins, branches, currentBranch);
    }

    public void runLog() throws CodeCriticException {
        String branch = getBranch();
        logCommandBuilder.append(branch);
        logCommandBuilder.append(" --name-only");
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
                /*if(changeSet.getChangeSet().equals("b8920d31bcb40b86e33d6cfb3c2ea70e58663f65")) {
                    System.out.println("stop");
                }*/
            }
            if(line.startsWith("Merge:")) {
                changeSet.setMerge();
                processSourceFile(null, changeSet, null);
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
                SourceFile sourceFile = new SourceFile(new File(getWorkingDirectory(), file).getAbsolutePath());
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
        if(waiterThread!=null) {
            waiter.stop();
            waiterThread.interrupt();
            System.out.println();
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

    @Override
    protected void processSourceFile(SourceFile sourceFile, ChangeSet changeSet, List<SourceFile> sourceFileList) {
        if(!getRepository().startsWith("http")) {
            if(waiter==null) {
                sendInfoMessage("Generate diff files for non-http repository");
                waiter = new WhileWeWaitPrintSomePeriods();
                waiterThread = new Thread(waiter);
                waiterThread.start();
            }
            String diff;
            if(sourceFile==null) {
                diff = String.format("git diff --color %s %s", config.fromBranch, changeSet.getChangeSet());
            } else {
                diff = String.format("git diff --color %s %s -- %s",
                               config.fromBranch, changeSet.getChangeSet(), sourceFile.getFile());
            }
            String result = ExecHelper.doExec(diff, null, getWorkingDirectory());
            if (result.length() == 0) {
                result = "<span style=\"color: gray;\">no changes detected</span>";
            }
            String[] lines = result.split("\n");
            StringBuilder html = new StringBuilder();
            html.append("<html>\n").append("<body>\n").append("<p style=\"font-family: monospace\">");
            try {
                for (String line : lines) {
                    String s = colorize(line.replace(" ", "nbsp;"));
                    s = s.replace("<span style=\"color: red;\">", "<span style=\"color: red; background-color: #ffcccc;\">");
                    s = s.replace("<span style=\"color: green;\">", "<span style=\"color: green; background-color: #c6ebd9;\">");
                    html.append(s.replace("nbsp;", "&nbsp;")).append("<br>\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            html.append("</p>\n").append("</body>\n").append("</html>");
            changeSet.setDiff(html.toString());
        }
        if(sourceFile!=null)
            super.processSourceFile(sourceFile, changeSet, sourceFileList);
    }

    private String colorize(String text) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        HtmlAnsiOutputStream hos = new HtmlAnsiOutputStream(os);
        hos.write(text.getBytes("UTF-8"));
        hos.close();
        return new String(os.toByteArray(), "UTF-8");
    }

    private class Origin {
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

    private int getInput(String request, int size, BufferedReader br) throws CodeCriticException {
        int selection;
        while(true) {
            try {
                System.out.println(request+"\n");
                System.out.print("Selection: ");
                String input = br.readLine();
                try {
                    int choice = Integer.parseInt(input);
                    if(choice<=size) {
                        selection = choice-1;
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
        return selection;
    }

    private String currentBranch(File dir) throws IOException {
        File head = new File(dir, "HEAD");
        List<String> lines = Files.readAllLines(head.toPath());
        String ref = lines.get(0);
        int ndx = ref.lastIndexOf("/");
        return ref.substring(ndx+1).trim();
    }

    private class Config {
        List<Origin> origins;
        List<String> branches;
        String currentBranch;
        String fromBranch;

        Config(List<Origin> origins, List<String> branches, String currentBranch) {
            this.origins = origins;
            this.branches = branches;
            this.currentBranch = currentBranch;
        }
    }
}
