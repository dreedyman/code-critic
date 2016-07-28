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
package org.cochise.codecritic;

import net.sourceforge.pmd.PMD;
import org.cochise.codecritic.support.scm.SCM;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates a code critic report.
 */
public class ReportGenerator {
    private SCM scm;
    private ProgressListener listener;

    public ReportGenerator(SCM scm, ProgressListener listener) {
        if(scm==null)
            throw new IllegalArgumentException("scm should never be null");
        this.scm = scm;
        this.listener = listener;
    }

    public void generate(File outputDirectory, String...options) {
        if(outputDirectory==null)
            throw new IllegalArgumentException("report outputDirectory should never be null");

        boolean debug = false;
        String rules = null;
        String minimumPriority = null;
        for(String option : options) {
            if(option.equals("debug")) {
                debug = true;
            }
            if(option.startsWith("rules")) {
                String[] parts = option.split("=");
                rules = parts[1];
            }
            if(option.startsWith("minimumPriority")) {
                String[] parts = option.split("=");
                minimumPriority = parts[1];
            }
        }
        if(!outputDirectory.exists()) {
            if(outputDirectory.mkdirs()) {
                sendMessage("Created "+outputDirectory.getPath(), false);
            }
        }

        List<ChangeSet> changeSets = scm.getChangeSets();
        List<SourceFile> javaSources = scm.getJavaSources();
        List<SourceFile> otherSources = scm.getOtherSources();

        if(javaSources.isEmpty()) {
            sendMessage("There are no source files to analyze", true);
            return;
        }

        String sourceToAnalyze = sourceFileListToString(javaSources);
        if(rules==null) {
            rules = "basic,imports,unusedcode,design,junit,imports,coupling,optimizations,strings,strictexception";
        }
        sendMessage("Setting CodeCriticReport branch to "+scm.getBranch(), true);
        CodeCriticReport.setBranch(scm.getBranch());

        Collections.sort(changeSets);
        Collections.sort(javaSources);
        Collections.sort(otherSources);
        CodeCriticReport.setOutputDirectory(outputDirectory);
        CodeCriticReport.setChangeSetList(changeSets);
        CodeCriticReport.setJavaSources(javaSources);
        CodeCriticReport.setOtherSources(otherSources);
        CodeCriticReport.setRepository(scm.getRepository());
        CodeCriticReport.setTotalFiles(Integer.toString(javaSources.size()+otherSources.size()));
        CodeCriticReport.setTotalJavaFiles(Integer.toString(javaSources.size()));
        File report = new File(outputDirectory, scm.getBranch()+"-branch-report.html");

        List<String> pmdArgs = new ArrayList<String>();
        pmdArgs.add(sourceToAnalyze);
        pmdArgs.add(CodeCriticReport.class.getName());
        pmdArgs.add(rules);
        pmdArgs.add("-reportfile");
        pmdArgs.add(report.getPath());
        if(debug)
            pmdArgs.add("-debug");
        if(minimumPriority!=null) {
            pmdArgs.add("-minimumpriority");
            pmdArgs.add(minimumPriority);
            CodeCriticReport.setMinimumPriority(minimumPriority);
        }
        CodeCriticReport.setRulesUsed(rules);

        sendMessage("pmd args: " + listToString(pmdArgs), false);

        sendMessage("Running PMD against " + javaSources.size() + " files with the following rules: "+rules, true);
        WhileWeWaitPrintSomePeriods w = new WhileWeWaitPrintSomePeriods();
        Thread t = new Thread(w);
        t.start();
        PMD.main(pmdArgs.toArray(new String[pmdArgs.size()]));
        t.interrupt();
        w.keepRunning = false;
        sendMessage("\nCode Critic report generated "+report.getPath(), true);
    }

    private void sendMessage(String message, boolean info) {
        if(listener!=null) {
            if(info) {
                listener.info(message);
            } else {
                listener.debug(message);
            }
        }
    }

    private String listToString(final List<String> list) {
        StringBuilder stringBuilder = new StringBuilder();
        for(String s: list) {
            if(stringBuilder.length()>0)
                stringBuilder.append(",");
            stringBuilder.append(s);
        }
        return  stringBuilder.toString();
    }

    private String sourceFileListToString(final List<SourceFile> list) {
        StringBuilder stringBuilder = new StringBuilder();
        for(SourceFile s: list) {
            if(stringBuilder.length()>0)
                stringBuilder.append(",");
            stringBuilder.append(s);
        }
        return  stringBuilder.toString();
    }
}
