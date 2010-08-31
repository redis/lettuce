<?xml version="1.0" encoding="UTF-8"?>
<!--
    Copyright 2010 The myBatis Team

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
-->

<!--
    Simple highlighter for FO/PDF output. Follows the Eclipse color scheme.

    version: $Id$
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xslthl="http://xslthl.sf.net"
    exclude-result-prefixes="xslthl" version='1.0'>

    <xsl:template match='xslthl:keyword'>
        <fo:inline font-weight="bold" color="#7F0055">
            <xsl:apply-templates />
        </fo:inline>
    </xsl:template>

    <xsl:template match='xslthl:comment'>
        <fo:inline font-style="italic" color="#3F5F5F">
            <xsl:apply-templates />
        </fo:inline>
    </xsl:template>

    <xsl:template match='xslthl:oneline-comment'>
        <fo:inline font-style="italic" color="#3F5F5F">
            <xsl:apply-templates />
        </fo:inline>
    </xsl:template>

    <xsl:template match='xslthl:multiline-comment'>
        <fo:inline font-style="italic" color="#3F5FBF">
            <xsl:apply-templates />
        </fo:inline>
    </xsl:template>

    <xsl:template match='xslthl:tag'>
        <fo:inline color="#3F7F7F">
            <xsl:apply-templates />
        </fo:inline>
    </xsl:template>

    <xsl:template match='xslthl:attribute'>
        <fo:inline olor="#7F007F">
            <xsl:apply-templates />
        </fo:inline>
    </xsl:template>

    <xsl:template match='xslthl:value'>
        <fo:inline color="#2A00FF">
            <xsl:apply-templates />
        </fo:inline>
    </xsl:template>

    <xsl:template match='xslthl:string'>
        <fo:inline color="#2A00FF">
            <xsl:apply-templates />
        </fo:inline>
    </xsl:template>

</xsl:stylesheet>
