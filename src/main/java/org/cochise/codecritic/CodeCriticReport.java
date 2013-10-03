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
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.renderers.XMLRenderer;
import net.sourceforge.pmd.util.StringUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A custom report that includes mercurial changeset links and PMD report.
 *
 * @author Dennis Reedy
 */
@SuppressWarnings({"PMD.LocalVariableCouldBeFinal", "PMD.MethodArgumentCouldBeFinal"})
public class CodeCriticReport extends XMLRenderer {
    private Transformer transformer;
    private final AtomicBoolean renderedChangeSets = new AtomicBoolean(false);
    private final AtomicBoolean renderedJavaSources = new AtomicBoolean(false);
    private final AtomicBoolean renderedOtherSources = new AtomicBoolean(false);
    private Writer outputWriter;
    private static String branch;
    private static final List<ChangeSet> changeSets = new ArrayList<ChangeSet>();
    private static final List<SourceFile> javaSources = new ArrayList<SourceFile>();
    private static final List<SourceFile> otherSources = new ArrayList<SourceFile>();
    private static String rulesUsed = "?";
    private static String repository = "?";
    private static String totalFiles = "?";
    private static String totalJavaFiles = "?";
    private static String minimumPriority = "5";

    public static void setRulesUsed(String rulesUsed) {
        CodeCriticReport.rulesUsed = rulesUsed;
    }

    public static void setRepository(String repository) {
        CodeCriticReport.repository = repository;
    }

    public static void setTotalFiles(String totalFiles) {
        CodeCriticReport.totalFiles = totalFiles;
    }

    public static void setTotalJavaFiles(String totalJavaFiles) {
        CodeCriticReport.totalJavaFiles = totalJavaFiles;
    }

    public static void setMinimumPriority(String minimumPriority) {
        CodeCriticReport.minimumPriority = minimumPriority;
    }

    public static void setBranch(String b) {
        branch = b;
    }

    public static void setChangeSetList(List<ChangeSet> changeSet) {
        if(changeSet!=null)
            changeSets.addAll(changeSet);
    }
    
    public static void setJavaSources(List<SourceFile> sourceFiles) {
        if(sourceFiles!=null)
            javaSources.addAll(sourceFiles);
    }

    public static void setOtherSources(List<SourceFile> sourceFiles) {
        if(sourceFiles!=null)
            otherSources.addAll(sourceFiles);
    }

    @Override
    public void renderFileReport(Report report) throws IOException {
        renderChangeSets();
        renderJavaSourceFiles();
        renderOtherSourceFiles();
        super.renderFileReport(report);
    }

    private  void renderChangeSets() throws IOException {
        synchronized(this){
            if(!renderedChangeSets.get()) {
                try {
                    Writer writer = getWriter();
                    StringBuffer buf = new StringBuffer();
                    for(ChangeSet changeSet : changeSets) {
                        buf.append("<changeset number=\"").append(changeSet.getNumber());
                        buf.append("\" link=\"").append(changeSet.getLink());
                        buf.append("\" changeset=\"").append(changeSet.getChangeSet());
                        buf.append("\" developer=\"").append(changeSet.getDeveloper());
                        if(changeSet.getEmail()!=null)
                            buf.append("\" email=\"").append(changeSet.getEmail());
                        buf.append("\" when=\"").append(changeSet.getDate());
                        buf.append("\" message=\"");
                        StringUtil.appendXmlEscaped(buf, changeSet.getMessage());
                        buf.append("\"/>").append(PMD.EOL);
                    }
                    writer.write(buf.toString());
                } finally {
                    renderedChangeSets.set(true);
                }
            }
        }
    }

    private void renderJavaSourceFiles() throws IOException {
        synchronized(this){
            if(!renderedJavaSources.get()) {
                try {
                    renderSourceFiles("java-sourcefile", javaSources);
                } finally {
                    renderedJavaSources.set(true);
                }
            }
        }
    }

    private void renderOtherSourceFiles() throws IOException {
        synchronized(this){
            if(!renderedOtherSources.get()) {
                try {
                    renderSourceFiles("other-sourcefile", otherSources);
                } finally {
                    renderedOtherSources.set(true);
                }
            }
        }
    }

    private void renderSourceFiles(String tag, List<SourceFile> sources) throws IOException {
        Writer writer = getWriter();
        StringBuilder buf = new StringBuilder();
        for(SourceFile sourceFile : sources) {
            buf.append("<").append(tag).append(" name=\"").append(sourceFile.getFile()).append("\">").append(PMD.EOL);
            Collections.sort(sourceFile.getChangeSets());
            for(ChangeSet changeSet : sourceFile.getChangeSets()) {
                buf.append("    <sourcefileChangeSet");
                buf.append(" link=\"").append(changeSet.getLink()).append("\"");
                buf.append(" changeset=\"").append(changeSet.getNumber()).append(":").append(changeSet.getChangeSet()).append("\"");
                buf.append("/>").append(PMD.EOL);
            }
            buf.append("</").append(tag).append(">").append(PMD.EOL);
        }
        writer.write(buf.toString());
    }

    @Override
    public void start() throws IOException {
        // We keep the initial writer to put the final html output
        this.outputWriter = getWriter();
        // We use a new one to store the XML...
        Writer writer = new StringWriter();
        setWriter(writer);
        InputStream xslt;
        String xsltFileName = "code-critic-nicerhtml.xsl";
        File file = new File(xsltFileName);
        if (file.exists() && file.canRead()) {
            xslt = new FileInputStream(file);
        } else {
            xslt = this.getClass().getClassLoader().getResourceAsStream(xsltFileName);
        }
        if (xslt == null) {
            throw new FileNotFoundException("Can't file XSLT sheet :" + xsltFileName);
        }
        this.prepareTransformer(xslt);
        // Now we build the XML file
        //Writer writer = getWriter();
        StringBuffer buf = new StringBuffer();
        buf.append("<?xml version=\"1.0\" encoding=\"").append(this.encoding).append("\"?>").append(PMD.EOL);
        createVersionAttr(buf);
        createTimestampAttr(buf);
        createBranchAttr(buf);
        createRulesUsedAttr(buf);
        createMinimumPriorityAttr(buf);
        createRepository(buf);
        createTotalFiles(buf);
        createTotalJavaFiles(buf);
        buf.append('>').append(PMD.EOL);
        writer.write(buf.toString());
    }

    /**
     * Prepare the transformer, doing the proper "building"...
     *
     * @param xslt, the xslt provided as an InputStream
     */
    private void prepareTransformer(InputStream xslt) {
        if (xslt != null) {
            try {
                //Get a TransformerFactory object
                TransformerFactory factory = TransformerFactory.newInstance();
                StreamSource src = new StreamSource(xslt);
                //Get an XSL Transformer object
                this.transformer = factory.newTransformer(src);
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void end() throws IOException {
        // First we finish the XML report
        super.end();
        // Now we transform it using XSLT
        Writer writer = super.getWriter();
        if (writer instanceof StringWriter) {
            StringWriter w = (StringWriter) writer;
            StringBuffer buffer = w.getBuffer();
            InputStream xml = new ByteArrayInputStream(buffer.toString().getBytes(this.encoding));
            Document doc = this.getDocument(xml);
            this.transform(doc);
        } else {
            // Should not happen !
            new IOException("Wrong writer").printStackTrace();
        }

    }

    private void transform(Document doc) {
        DOMSource source = new DOMSource(doc);
        this.setWriter(new StringWriter());
        StreamResult result = new StreamResult(this.outputWriter);
        try {
            transformer.transform(source, result);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    private Document getDocument(InputStream xml) {
        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return parser.parse(xml);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void createVersionAttr(StringBuffer buffer) {
        buffer.append("<pmd version=\"").append(PMD.VERSION).append('"');
    }

    private void createTimestampAttr(StringBuffer buffer) {
        buffer.append(" timestamp=\"").append(new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Date())).append('"');
    }

    private void createBranchAttr(StringBuffer buffer) {
        buffer.append(" branch=\"").append(branch).append('"');
    }

    private void createRulesUsedAttr(StringBuffer buffer) {
        buffer.append(" rulesUsed=\"").append(rulesUsed).append('"');
    }

    private void createMinimumPriorityAttr(StringBuffer buffer) {
        buffer.append(" minimumPriority=\"").append(minimumPriority).append('"');
    }

    private void createRepository(StringBuffer buffer) {
        buffer.append(" repository=\"").append(repository).append('"');
    }

    private void createTotalFiles(StringBuffer buffer) {
        buffer.append(" totalFiles=\"").append(totalFiles).append('"');
    }

    private void createTotalJavaFiles(StringBuffer buffer) {
        buffer.append(" totalJavaFiles=\"").append(totalJavaFiles).append('"');
    }
}
