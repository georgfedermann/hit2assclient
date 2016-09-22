package org.poormanscastle.products.hit2assclient.service;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jaxen.JaxenException;
import org.poormanscastle.products.hit2ass.ast.domain.ClouBaustein;
import org.poormanscastle.products.hit2ass.parser.javacc.HitAssAstParser;
import org.poormanscastle.products.hit2ass.parser.javacc.ParseException;
import org.poormanscastle.products.hit2ass.renderer.IRTransformer;
import org.poormanscastle.products.hit2ass.transformer.ClouBausteinMergerVisitor;
import org.poormanscastle.products.hit2ass.transformer.EraseBlanksVisitor;

/**
 * Created by georg on 16.09.16.
 */
class Hit2AssServiceImpl implements Hit2AssService {

    private final static Logger logger = Logger.getLogger(Hit2AssServiceImpl.class);

    void init() {

    }

    @Override
    public String extractElementIdFromWorkspace(byte[] workspaceData) {
        try {
            // extract the elementId from the new workspace
            OMElement workspaceDocument = OMXMLBuilderFactory.createOMBuilder(
                    new ByteArrayInputStream(workspaceData)).getDocumentElement();
            AXIOMXPath xPath = new AXIOMXPath("/Cockpit/Object[1]/@id");
            return xPath.stringValueOf(workspaceDocument);
        } catch (JaxenException e) {
            String errorMessage = StringUtils.join("Could not extract elementId from given workspace because: ",
                    e.getClass().getName(), " - ", e.getMessage());
            logger.error(errorMessage, e);
            // TODO implement sensible Exception hierarchy
            throw new RuntimeException(errorMessage, e);
        }
    }

    @Override
    public String extractElementIdFromDocument(byte[] workspaceData) {
        try {
            OMElement workspaceDocument = OMXMLBuilderFactory.createOMBuilder(
                    new ByteArrayInputStream(workspaceData)).getDocumentElement();
            AXIOMXPath xPath = new AXIOMXPath("/Cockpit/Object[@type='com.assentis.cockpit.bo.BoWorkspace']/" +
                    "Object[@type='com.assentis.cockpit.bo.BoProjectGroup']/" +
                    "Object[@type='com.assentis.cockpit.bo.BoProject']/" +
                    "Object[@type='com.assentis.cockpit.bo.BoDocument']/@id");
            return xPath.stringValueOf(workspaceDocument);
        } catch (JaxenException e) {
            String errorMessage = StringUtils.join("Could not extract document elementId from given workspace because: ",
                    e.getClass().getName(), " - ", e.getMessage());
            logger.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    @Override
    public byte[] renderBausteinToWorkspace(byte[] bausteinData) {
        String encoding = System.getProperty("hit2ass.clou.encoding", "ISO8859_1");
        checkArgument(!StringUtils.isBlank(encoding), "Encoding for HIT/CLOU Bausteine must be defined in system property hit2ass.clou.encoding!");
        try {
            ClouBaustein baustein = new HitAssAstParser(new ByteArrayInputStream(bausteinData), encoding).CB();
            baustein.accept(new ClouBausteinMergerVisitor());
            baustein.accept(new EraseBlanksVisitor());
            IRTransformer irTransformer = new IRTransformer();
            baustein.accept(irTransformer);

            return irTransformer.getWorkspace().getContent().getBytes("ISO-8859-1");
        } catch (ParseException | UnsupportedEncodingException e) {
            String errorMessage = StringUtils.join("Could not parse textbaustein because: ", e.getClass().getName(),
                    " - ", e.getMessage());
            logger.error(errorMessage);
            throw new RuntimeException(errorMessage, e);
        }
    }
}
