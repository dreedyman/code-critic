code-critic
===========

CodeCritic is a Maven plugin that determines the files that have changes for a SCM branch, performs code analysis and produces reports using PMD. Code Critic supports Git and Mercurial.

Use Code Critic as follows:

mvn code-critic:review

Code Critic will produce a report in target/code-critic-report/<branch-name>-branch-report.html
