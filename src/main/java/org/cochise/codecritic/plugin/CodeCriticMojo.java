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
package org.cochise.codecritic.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.cochise.codecritic.*;
import org.cochise.codecritic.support.scm.SCM;
import org.cochise.codecritic.support.scm.SCMFactory;

import java.io.File;
import java.util.ArrayList;

/**
 * Provides maven plugin support.
 *
 * @goal review
 *
 * @description Critiques files that have changed in a SCM branch.
 *
 * @requiresProject true
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("PMD.LocalVariableCouldBeFinal")
public class CodeCriticMojo extends AbstractMojo {
    /**
     * Mercurial branch to use
     *
     * @parameter expression="${branch}"
     * @optional
     */
    private String branch;

    /**
     * Whether to include test source
     *
     * @parameter expression="${includeTests}"
     * @optional
     * default-value="false"
     */
    private boolean includeTests;

    /**
     * Additional directories to exclude
     *
     * @parameter expression="${exclude}"
     * @optional
     * default-value=""
     */
    private String exclude;

    /**
     * Run in debug mode
     *
     * @parameter expression="${debug}"
     * @optional
     * default-value=""
     */
    private String debug;

    /**
     * Comma separated rules to use
     *
     * @parameter expression="${rules}"
     * @optional
     * default-value="basic,imports,unusedcode,design,optimizations,strictexception"
     */
    private String rules;

    /**
     * @parameter expression="${minimumPriority}
     * @optional
     * default-value=null
     */
    private String minimumPriority;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    @SuppressWarnings("unchecked")
    public void execute() throws MojoExecutionException, MojoFailureException {
        if(!project.isExecutionRoot()) {
            getLog().debug("Project not the execution root, do not execute");
            return;
        }

        SCM scm = SCMFactory.getSCM();
        if(scm==null)
            throw new MojoExecutionException("Unknown project type");
        Listener listener = new Listener();
        scm.registerProgressListener(listener);
        try {
            scm.initialize(project.getBasedir(), getLogOptions());
            scm.runLog();
            File outputDirectory = new File(project.getBasedir(), "target"+File.separator+"code-critic-report");
            ReportGenerator reportGenerator = new ReportGenerator(scm, listener);
            reportGenerator.generate(outputDirectory, getReportOptions());
        } catch (CodeCriticException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }

    private String[] getLogOptions() {
        ArrayList<String> options = new ArrayList<String>();
        if(branch!=null) {
            options.add("branch="+branch);
        }
        if(includeTests) {
            options.add("includeTests="+Boolean.toString(includeTests));
        }
        if(exclude!=null) {
            options.add("exclude="+exclude);
        }
        return options.toArray(new String[options.size()]);
    }

    private String[] getReportOptions() {
        ArrayList<String> options = new ArrayList<String>();
        if(debug!=null) {
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

    private class Listener implements ProgressListener {

        public void info(String message) {
            getLog().info(message);
        }

        public void debug(String message) {
            getLog().debug(message);
        }
    }
}
