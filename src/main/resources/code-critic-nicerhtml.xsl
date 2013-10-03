<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
                doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"/>

    <xsl:decimal-format decimal-separator="." grouping-separator=","/>

    <!-- keys for violations list -->
    <xsl:key name="violations" match="violation" use="@rule"/>

    <!-- XSL for Code Critic report. Author : Fabien Bancharel. -->
    <!-- Inspired by Checkstyle -->

    <xsl:template name="timestamp">
        <!--** Timestamp processing to display date -->
        <xsl:value-of select="//pmd/@timestamp"/>
    </xsl:template>


    <xsl:template match="pmd">
        <!--** Process root node pmd : html header, style, call templates -->
        <html>
            <head>
                <title>Code Critic Report</title>
                <style type="text/css">
                    .bannercell {
                        border: 0px;
                        padding: 0px;
                    }
                    body {
                        margin-left: 10px;
                        margin-right: 10px;
                        font:normal 80% arial,helvetica,sanserif;
                        background-color:#FFFFFF;
                        color:#000000;
                    }
                    .a td {
                        background: #efefef;
                    }
                    .b td {
                        background: #fff;
                    }
                    th, td {
                        text-align: left;
                        vertical-align: top;
                    }
                    th {
                        font-weight:bold;
                        background: #ccc;
                        color: black;
                    }
                    table, th, td {
                        font-size:100%;
                        border: none
                        width:100%
                    }
                    table.log tr td, tr th {
                    }

                    h2 {
                        font-weight:bold;
                        font-size:140%;
                        margin-bottom: 5;
                    }
                    h3 {
                        font-size:100%;
                        font-weight:bold;
                        background: #525D76;
                        color: white;
                        text-decoration: none;
                        padding: 5px;
                        margin-right: 2px;
                        margin-left: 2px;
                        margin-bottom: 0px;
                    }
                    a.white:link {color: #FF9999;}
                    a.white:visited {color: #FF9999;}
                    a.white:hover {color: #FF9999;}

                    .p1 { background:#FF9999; }
                    .p2 { background:#FFCC66; }
                    .p3 { background:#FFFF99; }
                    .p4 { background:#99FF99; }
                    .p5 { background:#a6caf0; }
                </style>
                <script type="text/javascript">
                    function openTable(tableid) {
                        which = document.getElementById(tableid);
                        if (which.style.display == "") {
                            which.style.display = "none";
                        }
                        else {
                            which.style.display = "";
                            which.style.width = "100%";
                        }
                    }
                </script>
            </head>
            <body>
                <a name="top"></a>
                <table border="0" cellpadding="0" cellspacing="0" width="100%">
                    <tr>
                        <td class="bannercell" rowspan="2">
                        </td>
                        <td class="text-align:right">
                            <h2 align="center">Code Critic Report for Branch <xsl:value-of select="//pmd/@branch"/>
                                <br></br>
                                Generated on <xsl:call-template name="timestamp"/>
                            </h2>
                            <!--<h3>PMD <xsl:value-of select="//pmd/@version"/></h3>-->
                        </td>
                    </tr>
                </table>
                <hr size="1"/>

                CodeCritic has produced the following reports:<br></br>
                <ul>
                    <li>Summary</li>
                    <li><a href="#ChangeSets">ChangeSet listing</a></li>
                    <li><a href="#Java_Sources">Java source files and associated ChangeSets</a></li>
                    <li><a href="#Other_Sources">Other source files and associated ChangeSets</a></li>
                    <li><a href="#PMD_Rules_Violated">PMD Rules Violated</a></li>
                    <li><a href="#File_Violations">File Violations</a></li>
                </ul>
                <br></br>
                <hr size="1" width="100%" align="left"/>

                <!-- Summary part -->
                <xsl:apply-templates select="." mode="summary"/>
                <hr size="1" width="100%" align="left"/>

                <!-- Changeset part -->
                <xsl:apply-templates select="." mode="changeset"/>
                <hr size="1" width="100%" align="left"/>

                <!-- Sourcefiles part -->
                <xsl:apply-templates select="." mode="java-sourcefiles"/>
                <hr size="1" width="100%" align="left"/>

                <!-- Sourcefiles part -->
                <xsl:apply-templates select="." mode="other-sourcefiles"/>
                <hr size="1" width="100%" align="left"/>

                <!-- Rules part -->
                <xsl:apply-templates select="." mode="rulelist"/>
                <hr size="1" width="100%" align="left"/>

                <!-- Package List part -->
                <xsl:apply-templates select="." mode="filelist"/>
                <hr size="1" width="100%" align="left"/>

                <!-- For each file create its part -->
                <xsl:apply-templates select="file"/>

                <hr size="1" width="100%" align="left"/>

            </body>
        </html>
    </xsl:template>

    <xsl:template match="pmd" mode="rulelist">
        <!--** Process root node pmd, for mode 'rulelist' : violated rules -->
        <a name="PMD_Rules_Violated"/>
        <h3>PMD Rules Violated <a href="#" onclick="openTable('rulelist'); return false;" class="white"> #</a></h3>
        <table border="0" cellpadding="5" cellspacing="2" width="100%" id="rulelist">
            <tr>
                <th style="width:84%">Rule</th>
                <th style="width:8%">Violations</th>
                <th style="width:8%">Severity</th>
            </tr>

            <xsl:for-each select="file/violation[@rule and generate-id(.) = generate-id(key('violations', @rule))]">
                <!-- Sort by number of violations -->
                <xsl:sort data-type="number" order="descending"
                          select="count(key('violations', @rule))"/>

                <xsl:variable name="currentRule" select="@rule"/>
                <xsl:variable name="currentSeverity" select="@priority"/>
                <xsl:variable name="violationCount"
                              select="count(../../file/violation[@rule=$currentRule])"/>

                <tr>
                    <xsl:call-template name="alternated-row"/>
                    <td>
                        [<xsl:value-of select="@ruleset"/>]
                        <xsl:value-of select="$currentRule"/>
                    </td>
                    <td>
                        <xsl:value-of select="$violationCount"/>
                    </td>
                    <td>
                        <div class="p{$currentSeverity}">
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="$currentSeverity"/>
                        </div>
                    </td>
                </tr>

            </xsl:for-each>
        </table>
        <a href="#top">Back to top</a>
    </xsl:template>

    <xsl:template match="pmd" mode="filelist">
        <!--** Process root node pmd, for mode 'filelist' : number of violations for each file -->
        <a name="File_Violations"/>
        <h3>File Violations <a href="#" onclick="openTable('filelist'); return false;" class="white"> #</a></h3>
        <table class="log" border="0" cellpadding="5" cellspacing="2" width="100%" id="filelist">
            <tr>
                <th>File</th>
                <th style="width:40px">
                    <div class="p5">5</div>
                </th>
                <th style="width:40px">
                    <div class="p4">4</div>
                </th>
                <th style="width:40px">
                    <div class="p3">3</div>
                </th>
                <th style="width:40px">
                    <div class="p2">2</div>
                </th>
                <th style="width:40px">
                    <div class="p1">1</div>
                </th>
            </tr>

            <xsl:for-each select="file">
                <!-- Sort by number of violations -->
                <xsl:sort data-type="number" order="descending"
                          select="count(violation)"/>

                <xsl:variable name="currentSource" select="@name"/>

                <tr>
                    <xsl:call-template name="alternated-row"/>
                    <td>
                        <xsl:variable name="anchor" select="translate(@name, '\/', '__')"></xsl:variable>
                        <a href="#f-{$anchor}">
                            <xsl:value-of select="@name"/>
                        </a>
                    </td>
                    <td>
                        <xsl:value-of select="count(violation[@priority = 5])"/>
                    </td>
                    <td>
                        <xsl:value-of select="count(violation[@priority = 4])"/>
                    </td>
                    <td>
                        <xsl:value-of select="count(violation[@priority = 3])"/>
                    </td>
                    <td>
                        <xsl:value-of select="count(violation[@priority = 2])"/>
                    </td>
                    <td>
                        <xsl:value-of select="count(violation[@priority = 1])"/>
                    </td>
                </tr>

            </xsl:for-each>
        </table>
        <a href="#top">Back to top</a>
    </xsl:template>

    <xsl:template match="file">
        <!--** Process node 'file' : violations details -->
        <xsl:variable name="anchor" select="translate(@name, '\/', '__')"></xsl:variable>
        <a name="f-{$anchor}"></a>
        <h3>File <xsl:value-of select="@name"/><a href="#" onclick="openTable('{@name}'); return false;" class="white"> #</a>
        </h3>

        <table class="log" border="0" cellpadding="5" cellspacing="2" width="100%" id="{@name}">
            <tr>
                <th style="width:60px;">Violation</th>
                <th>Error Description</th>
                <th style="width:40px;">Line</th>
            </tr>
            <xsl:for-each select="violation">
                <xsl:variable name="currentSeverity" select="@priority"/>
                <tr>
                    <xsl:call-template name="alternated-row"/>
                    <td>
                        <div class="p{$currentSeverity}">
                            <xsl:value-of select="$currentSeverity"/>
                        </div>
                    </td>
                    <td>
                        [<xsl:value-of select="@ruleset"/>.<xsl:value-of select="@rule"/>]
                        -
                        <xsl:choose>
                            <xsl:when test="@externalInfoUrl">
                                <a href="{@externalInfoUrl}">
                                    <xsl:value-of select="." disable-output-escaping="yes"/>
                                </a>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="." disable-output-escaping="yes"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </td>
                    <td>
                        <xsl:value-of select="@beginline"/> -
                        <xsl:value-of select="@endline"/>
                    </td>
                </tr>
            </xsl:for-each>
        </table>
        <a href="#top">Back to top</a>
    </xsl:template>

    <xsl:template match="pmd" mode="changeset">
        <!--** Process root node 'pmd',  for mode 'changesets' : number of files, number of violations by severity -->
        <a name="ChangeSets"/>
        <h3>ChangeSets <a href="#" onclick="openTable('changesets'); return false;" class="white"> #</a>
        </h3>
        <table border="0" cellpadding="5" cellspacing="2" id="changesets" width="100%">
            <tr>
                <!--<th style="width:5%">Changeset</th>-->
                <th>ChangeSet</th>
                <th style="width:40%">Message</th>
                <th>Developer</th>
                <th>When</th>
                <th>ChangeSet Link</th>
            </tr>
            <xsl:for-each select="changeset">
                <tr>
                    <xsl:call-template name="alternated-row"/>
                    <td>
                        <xsl:value-of select="@number"/>
                    </td>
                    <td>
                        <xsl:value-of select="@message"/>
                    </td>
                    <td>
                        <xsl:choose>
                            <xsl:when test="@email">
                                <a href="mailto:{@email}">
                                    <xsl:value-of select="@developer" disable-output-escaping="yes"/> </a>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="@developer"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </td>
                    <td>
                        <xsl:value-of select="@when"/>
                    </td>
                    <td>
                        <a href="{@link}"> <xsl:value-of select="@changeset" disable-output-escaping="yes"/>
                        </a>
                    </td>
                </tr>
            </xsl:for-each>
        </table>
        <a href="#top">Back to top</a>
    </xsl:template>

    <xsl:template match="pmd" mode="java-sourcefiles">
        <a name="Java_Sources"/>
        <h3>Java Files Modified <a href="#" onclick="openTable('java-sourcefiles'); return false;" class="white"> #</a></h3>
        <table border="0" cellpadding="5" cellspacing="2" width="100%" id="java-sourcefiles">
            <tr>
                <!--<th style="width:5%">Changeset</th>-->
                <th>Source File</th>
                <th>ChangeSet Links</th>
            </tr>
            <xsl:for-each select="java-sourcefile">
                <tr>
                    <xsl:call-template name="alternated-row"/>
                    <td>
                        <xsl:value-of select="@name"/>
                    </td>
                    <td>
                        <xsl:for-each select="sourcefileChangeSet">
                        <a href="{@link}"> <xsl:value-of select="@changeset" disable-output-escaping="yes"/>
                        </a>
                        </xsl:for-each>
                    </td>
                </tr>
            </xsl:for-each>
        </table>
        <a href="#top">Back to top</a>
    </xsl:template>

    <xsl:template match="pmd" mode="other-sourcefiles">
        <a name="Other_Sources"/>
        <h3>Other Files Modified<a href="#" onclick="openTable('other-sourcefiles'); return false;" class="white"> #</a></h3>
        <table border="0" cellpadding="5" cellspacing="2" width="100%" id="other-sourcefiles">
            <tr>
                <!--<th style="width:5%">Changeset</th>-->
                <th>Source File</th>
                <th>ChangeSet Links</th>
            </tr>
            <xsl:for-each select="other-sourcefile">
                <tr>
                    <xsl:call-template name="alternated-row"/>
                    <td>
                        <xsl:value-of select="@name"/>
                    </td>
                    <td>
                        <xsl:for-each select="sourcefileChangeSet">
                            <a href="{@link}"> <xsl:value-of select="@changeset" disable-output-escaping="yes"/>
                            </a>
                        </xsl:for-each>
                    </td>
                </tr>
            </xsl:for-each>
        </table>
        <a href="#top">Back to top</a>
    </xsl:template>

    <xsl:template match="pmd" mode="summary">

        <h3>Report Summary</h3>
        <xsl:variable name="minPriority" select="//pmd/@minimumPriority"/>
        <xsl:variable name="repo" select="//pmd/@repository"/>
        <table border="0" width="100%">
            <tr>
                <th>Item</th>
                <th>Info</th>
            </tr>
            <tr>
                <xsl:call-template name="alternated-row"/>
                <td style="width:15%">Repository</td>
                <td><a href="{$repo}"><xsl:value-of select="$repo"/></a></td>
            </tr>
            <tr>
                <td>Branch</td>
                <td><xsl:value-of select="//pmd/@branch"/></td>
            </tr>
            <tr>
                <xsl:call-template name="alternated-row"/>
                <td>Rules Used</td>
                <td><xsl:value-of select="//pmd/@rulesUsed"/></td>
            </tr>
            <tr>
                <td>Minimum Priority</td>
                <div class="p{$minPriority}">
                    <td><xsl:value-of select="$minPriority"/></td>
                </div>
            </tr>
            <tr>
                <xsl:call-template name="alternated-row"/>
                <td>Total number of modified files</td>
                <td><xsl:value-of select="//pmd/@totalFiles"/></td>
            </tr>
            <tr>
                <td>Total number of Java files</td>
                <td><xsl:value-of select="//pmd/@totalJavaFiles"/></td>
            </tr>
        </table>
        <br></br>
        <!--** Process root node 'pmd',  for mode 'summary' : number of files, number of violations by severity -->
        <h3>Rules Violation Summary</h3>
        <table class="log" border="0" cellpadding="5" cellspacing="2" width="100%">
            <tr>
                <th style="width:25%">Files</th>
                <th>Total</th>
                <th>
                    <div class="p1">Priority 1</div>
                </th>
                <th>
                    <div class="p2">Priority 2</div>
                </th>
                <th>
                    <div class="p3">Priority 3</div>
                </th>
                <th>
                    <div class="p4">Priority 4</div>
                </th>
                <th>
                    <div class="p5">Priority 5</div>
                </th>
            </tr>
            <tr>
                <xsl:call-template name="alternated-row"/>
                <td>
                    <xsl:value-of select="count(//file)"/>
                </td>
                <td>
                    <xsl:value-of select="count(//violation)"/>
                </td>
                <td>
                    <xsl:value-of select="count(//violation[@priority = 1])"/>
                </td>
                <td>
                    <xsl:value-of select="count(//violation[@priority = 2])"/>
                </td>
                <td>
                    <xsl:value-of select="count(//violation[@priority = 3])"/>
                </td>
                <td>
                    <xsl:value-of select="count(//violation[@priority = 4])"/>
                </td>
                <td>
                    <xsl:value-of select="count(//violation[@priority = 5])"/>
                </td>
            </tr>
        </table>
        <br></br>

    </xsl:template>

    <xsl:template name="alternated-row">
        <!--** Template alternated-row, for alternated rows style in tables -->
        <xsl:attribute name="class">
            <xsl:if test="position() mod 2 = 1">a</xsl:if>
            <xsl:if test="position() mod 2 = 0">b</xsl:if>
        </xsl:attribute>
    </xsl:template>
</xsl:stylesheet>




