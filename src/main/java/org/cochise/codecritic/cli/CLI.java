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
package org.cochise.codecritic.cli;

import org.cochise.codecritic.CodeCriticException;
import org.cochise.codecritic.ProgressListener;
import org.cochise.codecritic.ReportGenerator;
import org.cochise.codecritic.plugin.HelpMojo;
import org.cochise.codecritic.support.scm.SCM;
import org.cochise.codecritic.support.scm.SCMFactory;

import java.io.File;
import java.util.ArrayList;

/**
 * Provides support to run code-critic as a command line application
 *
 * @author Dennis Reedy
 */
public class CLI {
    private SCM scm;
    private ConsoleListener listener;
    private CommandLineParser commandLine;
    
    private boolean init(String... args) throws CodeCriticException {
        commandLine = new CommandLineParser(args);
        if(commandLine.getHelp()) {
            System.out.println(new HelpMojo().getHelp());
            return false;
        }
        scm = SCMFactory.getSCM();
        if(scm==null)
            throw new CodeCriticException("Unknown project type");
        listener = new ConsoleListener();        
        if(commandLine.debug()) {
            listener.setDebug(true);
        }            
        scm.registerProgressListener(listener);
        return true;
    }
    
    private void exec() throws CodeCriticException {
        File workingDirectory = commandLine.getProjectDir();
        scm.initialize(workingDirectory, commandLine.getLogOptions());
        scm.runLog();
        String reportDir = new File(workingDirectory, "pom.xml").exists()?"target":"build/reports";
        File outputDirectory = new File(workingDirectory, reportDir+File.separator+"code-critic-report");
        ReportGenerator reportGenerator = new ReportGenerator(scm, listener);
        reportGenerator.generate(outputDirectory, commandLine.getReportOptions());
    }

    
    public static void main(String... args) throws CodeCriticException {
        CLI cli = new CLI();
        if(cli.init(args)) {
            cli.exec();
        }
    }
    
    private class CommandLineParser {
        private boolean debug;
        private String rules;
        private String minimumPriority;
        private String branch;
        private boolean includeTests;
        private String exclude;
        private boolean help;
        private String projectDir;
        
        CommandLineParser(String... args) {
            for(String arg : args) {
                if("-includeTests".equals(arg)) {
                    includeTests = true;
                }
                if("-help".equals(arg)) {
                    help = true;
                }
                if("-debug".equals(arg)) {
                    debug = true;
                }
                if(arg.startsWith("-rules")) {
                    rules = splitArg(arg);
                }
                if(arg.startsWith("-minimumPriority")) {
                    minimumPriority = splitArg(arg);
                }
                if(arg.startsWith("-branch")) {
                    branch = splitArg(arg);
                }
                if(arg.startsWith("-exclude")) {
                    exclude = splitArg(arg);
                }
                if(arg.startsWith("-dir")) {
                    projectDir = splitArg(arg);
                }
            }
        }

        boolean getHelp() {
            return help;
        }

        String[] getLogOptions() {
            ArrayList<String> options = new ArrayList<String>();
            if(branch!=null) {
                options.add("branch="+branch);
            }
            if(includeTests) {
                options.add("includeTests="+includeTests);
            }
            if(exclude!=null) {
                options.add("exclude="+exclude);
            }
            return options.toArray(new String[options.size()]);
        }

        String[] getReportOptions() {
            ArrayList<String> options = new ArrayList<String>();
            if(debug) {
                options.add("debug");
            }
            if(rules!=null) {
                options.add("rules="+rules);
            }
            if(minimumPriority!=null) {
                options.add("minimumPriority="+minimumPriority);
            }
            return options.toArray(new String[options.size()]);
        }

        File getProjectDir() {
            String dir = projectDir==null?System.getProperty("user.dir"):projectDir;
            return new File(dir);
        }

        private String splitArg(String arg) {
            String[] parts = arg.split("=");
            return parts[1];
        }

        boolean debug() {
            return debug;
        }
    }

    private class ConsoleListener implements ProgressListener {
        private boolean debug = false;

        void setDebug(boolean debug) {
            this.debug = debug;
        }

        public void info(String message) {
            System.out.println("[INFO] " + message);
        }

        public void debug(String message) {
            if(debug)
                System.out.println("[DEBUG] "+message);
        }
    }
}
