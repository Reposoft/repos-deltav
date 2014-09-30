<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" 
    xmlns:fn="http://www.w3.org/2005/xpath-functions"
    xmlns:v="http://www.repos.se/namespace/v" 
    exclude-result-prefixes="xs v" version="2.0">

    <xsl:param name="revbase" select="5" as="xs:integer"/>
    <xsl:param name="revchanges" as="xs:integer*">
        <!--
        <xsl:value-of select="10"/>
        
        <xsl:value-of select="12"/>
        -->
    </xsl:param>
    <xsl:param name="revnow" select="9999999999">
        <!-- Constant: Should be equal to maximum revision allowed in indexing idstategy. -->
    </xsl:param>
    
    <xsl:variable name="revmax" select="max(($revbase, $revchanges))"/>
    <xsl:variable name="revall" select="distinct-values((//@v:start, //@v:end))[. != 'NOW']" as="xs:integer*"/>
    <xsl:variable name="revexclude" select="$revall[. > $revbase][$revmax > .][not(. = $revchanges)]" as="xs:integer*"/>
    <!--
    <xsl:key name="revexclude" match=""></xsl:key>
    -->
    
    <xsl:output indent="no"/>

    <xsl:template match="v:file">
        <!-- Nothing really to do here, except possibly validate parameters against file info. -->
        <xsl:comment select="concat('Maximum included revision: ', $revmax)"/>
        <xsl:comment select="concat('Excluded revisions: ', '')"/>
        <xsl:comment select="for $i in 1 to count($revexclude) return $revexclude[$i]"></xsl:comment>
        <xsl:apply-templates/>
    </xsl:template>

    <!-- ELEMENTS -->
    <xsl:template match="element()" priority="-10">

        <xsl:copy>
            <xsl:copy-of select="@v:start"/>
            <xsl:apply-templates select="v:attr">
                <!-- Attributes must be deduplicated when using discontinuous revisions. -->
                <!-- Sorting on start rev is easiest, ensuring the highest revision accepted by templates below is used/processed last. -->
                <!-- Sorting on name is just for easier debugging.-->
                <xsl:sort select="@v:name"/>
                <xsl:sort select="@v:start" data-type="number" order="ascending"/>
            </xsl:apply-templates>
            <xsl:apply-templates select="element()[name()!='v:attr']"/>
        </xsl:copy>

    </xsl:template>


    <xsl:template match="element()[not(v:include-start(., $revbase, $revchanges))]" priority="21">
        <!-- Suppressing elements that should not be included. -->
    </xsl:template>

    <xsl:template match="element()[v:include-end(., $revbase, $revchanges)]" priority="20">
        <!-- Suppressing elements that should not be included. -->
    </xsl:template>

    <!-- ATTRIBUTES -->
    <xsl:template match="v:attr">
        <xsl:variable name="name" select="@v:name"/>
        <xsl:attribute name="{$name}" select="text()"/>
    </xsl:template>

    <xsl:template match="v:attr[not(v:include-start(., $revbase, $revchanges))]" priority="51">
        <!-- Suppressing attributes that should not be included. -->
    </xsl:template>

    <xsl:template match="v:attr[v:include-end(., $revbase, $revchanges)]" priority="50">
        <!-- Suppressing attributes that should not be included. -->
    </xsl:template>
    
    
    <!-- TEXT - TODO: Deduplicate on p-level? -->
    <xsl:template match="v:text">
        <xsl:copy-of select="text()"/>
    </xsl:template>
    
    <xsl:template match="v:text[not(v:include-start(., $revbase, $revchanges))]" priority="51">
        <!-- Suppressing text that should not be included. -->
    </xsl:template>
    
    <xsl:template match="v:text[v:include-end(., $revbase, $revchanges)]" priority="50">
        <!-- Suppressing text that should not be included. -->
    </xsl:template>
    
    <xsl:template match="text()">
        <!-- Suppress text on its own. -->
    </xsl:template>

    <!-- COMMENTS -->
    <xsl:template match="v:comment">
        <!-- Comments might get duplicated when using changesets. -->
        <xsl:comment select="text()"/>
    </xsl:template>
    
    <!-- PIs -->
    <xsl:template match="v:pi">
        <!-- PIs are difficult to support when using changesets (discontinuous). -->
    </xsl:template>


    <xsl:function name="v:include-start" as="xs:boolean">
        <!-- True when the revision where element was added is included in the selection. -->
        <xsl:param name="e" as="element()"/>
        <xsl:param name="revbase" as="xs:integer"/>
        <xsl:param name="revchanges" as="xs:integer*"/>

        <xsl:variable name="start" as="xs:integer">
            <xsl:value-of select="$e/@v:start"/>
        </xsl:variable>

        <xsl:value-of select="($start &lt;= $revbase) or (count($revchanges[. = $start]))"/>
    </xsl:function>

    <xsl:function name="v:include-end" as="xs:boolean">
        <!-- True when the revision where element was deleted is included in the selection. -->
        <!-- Note: There is an inverse logic here, true means that the element should not be included in output. -->
        <xsl:param name="e" as="element()"/>
        <xsl:param name="revbase" as="xs:integer"/>
        <xsl:param name="revchanges" as="xs:integer*"/>

        <xsl:variable name="end" as="xs:integer">

            <xsl:choose>
                <xsl:when test="$e/@v:end castable as xs:integer">
                    <xsl:value-of select="$e/@v:end"/>
                </xsl:when>
                <xsl:otherwise>
                    <!-- All other values means NOW/HEAD -->
                    <xsl:value-of select="$revnow"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:value-of select="($end &lt;= $revbase) or (count($revchanges[. = $end]))"/>
    </xsl:function>

</xsl:stylesheet>