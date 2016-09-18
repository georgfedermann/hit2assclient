package org.poormanscastle.products.hit2assclient.service;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
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
