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

/**
 * Provides help for the {@code code-critic}
 *
 * @goal help
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("PMD.LocalVariableCouldBeFinal")
public class HelpMojo extends AbstractMojo {
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if(!project.isExecutionRoot()) {
            getLog().debug("Project not the execution root, do not execute");
            return;
        }

        getLog().info(getHelp());
    }
    
    public String getHelp() {
        StringBuilder builder = new StringBuilder();
        builder.append("Code Critic Tool\n");
        builder.append("  A utility for use with a SCM project, that produces a\n");
        builder.append("  PMD report for source code rule violations for a branch. Only source code\n");
        builder.append("  that has been modified is considered.\n");
        builder.append("\n");
        builder.append("This plugin has 2 goals:\n");
        builder.append("\n");
        builder.append("code-critic:review\n");
        builder.append("  Creates a report for source code rule violations for branch\n");
        builder.append("  and produces a table of changeset information.\n");
        builder.append("\n");
        builder.append("  Available parameters:\n");
        builder.append("\n");
        builder.append("    branch (default:the current branch)\n");
        builder.append("      The SCM branch to use.\n");
        builder.append("\n");
        builder.append("    hg (default: \"\")\n");
        builder.append("      Specific hg log command to use. The command must be a verbose hg log command.\n");
        builder.append("      If the provided command string does not contain hg, log or \"-v\", code-critic will exit with an error.\n");
        builder.append("\n");
        builder.append("    git (default: \"\")\n");
        builder.append("      Specific git log command to use.\n");
        builder.append("\n");
        builder.append("    includeTests (default: false)\n");
        builder.append("      Whether to include source code found in src/test directories.\n");
        builder.append("\n");
        builder.append("    exclude (default: \"\")\n");
        builder.append("      A comma separated list of directory patterns.\n");
        builder.append("\n");
        builder.append("    rules (default: basic,imports,unusedcode,design,strictexception)\n");
        builder.append("      The set of rules to use.\n");
        builder.append("\n");
        builder.append("    minimumPriority (default: 5)\n");
        builder.append("      Set the minimum rule priority threshold for all Rules which are loaded from RuleSets.\n");
        builder.append("\n");
        builder.append("code-critic:help\n");
        builder.append("  Display help information for the code-critic plugin.\n");
        return builder.toString();
    }
}
