package org.poormanscastle.products.hit2assclient.cli;

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.poormanscastle.products.hit2assclient.service.DocRepoService;
import org.poormanscastle.products.hit2assclient.service.Hit2AssService;

/**
 * implements functionality for the HitAssClient command line interface.
 * <p>
 * Created by georg.federmann@poormanscastle.com on 16.09.16.
 */
public class HitAssClientTools {

    public final static Logger logger = Logger.getLogger(HitAssClientTools.class);

    public static void main(String[] args) {
        checkState(!StringUtils.isBlank(System.getProperty("hit2ass.clou.encoding")), "Please set system property hit2ass.clou.encoding");
        checkState(!StringUtils.isBlank(System.getProperty("hit2ass.xml.encoding")), "Please set system property hit2ass.xml.encoding");
        logger.info(StringUtils.join("Using encoding ", System.getProperty("hit2ass.xml.encoding"), " for XMLs."));
        logger.info(StringUtils.join("Using encoding ", System.getProperty("hit2ass.clou.encoding"), " for HIT/CLOU bausteins."));

        DocRepoService client = DocRepoService.getDocRepoService();

        // Process CLOU Bausteine
        Arrays.stream(Paths.get(
                StringUtils.defaultString(System.getProperty("hit2ass.clou.path"),
                        "/Users/georg/vms/UbuntuWork/shared/hitass/reverseEngineering/hit2assentis_reworked")).toFile().
                listFiles((dir, name) -> name.startsWith("B.ue107") && !name.endsWith(".acr"))).forEach(bausteinFile -> {

            // create base folder for this baustein, containing workspace and testdata subfolders.
            client.createFolder(bausteinFile.getName().replaceAll("\\.", "_"));

            // import XML testdata
            Arrays.stream(Paths.get("/Users/georg/vms/UbuntuWork/shared/hitass/testfaelle").toFile().
                    listFiles((dir, name) -> name.toLowerCase().contains(bausteinFile.getName().toLowerCase()) && name.endsWith("xml"))).forEach(
                    xmlFile -> {
                        try {
                            client.importXmlForBaustein(
                                    new String(Files.readAllBytes(xmlFile.toPath()), System.getProperty("hit2ass.xml.encoding")).getBytes(),
                                    bausteinFile.getName().replaceAll("\\.", "_"), xmlFile.getName());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
            );

            // Try to create workspace and save it to DocRepo
            try {
                client.importWorkspace(Hit2AssService.getHit2AssService().renderBausteinToWorkspace(
                        Files.readAllBytes(bausteinFile.toPath())
                ), bausteinFile.getName().replaceAll("\\.", "_"));
            } catch (Error | Exception e) {
                // javacc parser throws Errors ...
                String errorMessage = StringUtils.join("Could not process HIT/CLOU Baustein ", bausteinFile.getName(), ", because:",
                        e.getClass().getName(), " - ", e.getMessage());
                logger.error(errorMessage, e);
            }
        });
    }

}
