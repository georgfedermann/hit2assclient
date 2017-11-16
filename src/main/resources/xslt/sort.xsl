<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
    <xsl:output method="xml" />
    
    <!-- Identity transform - copies everything that doesn't have an explicit match below -->
    <xsl:template match="node() | @*">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" />
        </xsl:copy>
    </xsl:template>
    
    <!-- Special handling for Objects of type BoModuleDeploymentLibrary -->
    <!-- Copy it and then sort contained BoModuleComposition objects by name attribute -->
    <xsl:template match="Object[@type='com.assentis.cockpit.bo.BoModuleDeploymentLibrary']">
        <xsl:copy>
            <xsl:apply-templates select="Object[@type='com.assentis.cockpit.bo.BoModuleComposition']">
                <xsl:sort select="@name" />
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>