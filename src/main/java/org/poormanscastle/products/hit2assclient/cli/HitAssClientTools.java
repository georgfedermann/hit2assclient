package org.poormanscastle.products.hit2assclient.cli;

import static com.google.common.base.Preconditions.checkState;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.poormanscastle.products.hit2ass.renderer.DeployedModuleLibrary;
import org.poormanscastle.products.hit2assclient.service.DocRepoService;
import org.poormanscastle.products.hit2assclient.service.Hit2AssService;

/**
 * implements functionality for the HitAssClient command line interface.
 * <p>
 * Created by georg.federmann@poormanscastle.com on 16.09.16.
 */
public class HitAssClientTools {

    public final static Logger logger = Logger.getLogger(HitAssClientTools.class);

    public static void main(String[] args) throws Exception {
        // HitAssClientTools.processAllWorkingBausteineLocally();
        HitAssClientTools.importIntoDocRepo();
    }

    public static void processAllWorkingBausteineLocally() throws IOException {
/*
        List<String> whiteList = new ArrayList<>();
        Files.lines(Paths.get("/Users/georg/vms/UbuntuWork/shared/hitass/reverseEngineering/hit2assentis_reworked/workingBausteine.txt"))
                .forEach(line -> whiteList.add(line));
*/
        List<String> blackList = new ArrayList<>();
        Files.lines(Paths.get("/Users/georg/vms/UbuntuWork/shared/hitass/reverseEngineering/hit2assentis_reworked/ignoreList.txt"))
                .forEach(line -> {
                    System.out.println(StringUtils.join("Ignoring ", line));
                    blackList.add(line.trim());
                });
        Files.lines(Paths.get("/Users/georg/vms/UbuntuWork/shared/hitass/reverseEngineering/hit2assentis_reworked/repairList.txt"))
                .forEach(line -> {
                    System.out.println(StringUtils.join("Adding from repairlist to ignorelist: ", line));
                    blackList.add(line.trim());
                });

        Hit2AssService hit2AssService = Hit2AssService.getHit2AssService();
        Arrays.stream(Paths.get(StringUtils.defaultString(System.getProperty("hit2ass.clou.path"),
                "/Users/georg/vms/UbuntuWork/shared/hitass/reverseEngineering/hit2assentis_reworked")).toFile().
                listFiles((dir, name) -> name.startsWith("B.") && !name.endsWith(".acr") && !blackList.contains(name.trim()))).forEach(bausteinFile -> {
            try {
                System.out.println(StringUtils.join("Processing Baustein ", bausteinFile.getName()));
                byte[] workspaceData = hit2AssService.renderBausteinToWorkspace(bausteinFile.getName(), Files.readAllBytes(bausteinFile.toPath()));
                Files.write(Paths.get(bausteinFile.getParent(), StringUtils.join(bausteinFile.getName(), ".acr")), workspaceData);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    public static void importIntoDocRepo() throws IOException {
        checkState(!StringUtils.isBlank(System.getProperty("hit2ass.clou.encoding")), "Please set system property hit2ass.clou.encoding");
        // if I get this right, this check is not necessary any more, because -Dfile.encoding=ISO-8859-1 is set and this
        // covers it for hit baustein input.
        // checkState(!StringUtils.isBlank(System.getProperty("hit2ass.xml.encoding")), "Please set system property hit2ass.xml.encoding");
        checkState(!StringUtils.isBlank(System.getProperty("hit2ass.clou.pathToDeployedModuleLibrary")), "Please set system property hit2ass.clou.pathToDeployedModuleLibrary");
        logger.info(StringUtils.join("Using encoding ", System.getProperty("hit2ass.xml.encoding"), " for XMLs."));
        logger.info(StringUtils.join("Using encoding ", System.getProperty("hit2ass.clou.encoding"), " for HIT/CLOU bausteins."));
        logger.info(StringUtils.join("Using deployed module library workspace ", System.getProperty("hit2ass.clou.pathToDeployedModuleLibrary")));


        DocRepoService client = DocRepoService.getDocRepoService();

        // workingBausteine.txt contains a white list of bausteine known to be working with the HIT/CLOU parser. Or the other way round.
        // this white list is read into an array and used as a filter for the bausteine that shall be processed into DocFamily.
        List<String> whiteList = new ArrayList<>();
        Files.lines(Paths.get("/Users/georg/vms/UbuntuWork/shared/hitass/reverseEngineering/hit2assentis_reworked/workingBausteine.txt"))
                .forEach(line -> whiteList.add(line));

/*
        whiteList.clear();
        whiteList.add("B.al001");
*/

        Hit2AssService hit2AssService = Hit2AssService.getHit2AssService();
        // Process CLOU Bausteine
        Arrays.stream(Paths.get(
                StringUtils.defaultString(System.getProperty("hit2ass.clou.path"),
                        "/Users/georg/vms/UbuntuWork/shared/hitass/reverseEngineering/hit2assentis_reworked")).toFile().
                listFiles((dir, name) -> whiteList.contains(name))).forEach(bausteinFile -> {
            System.out.println(StringUtils.join("Processing Baustein ", bausteinFile));
            // create base folder for this baustein, containing workspace and testdata subfolders.
            client.createFolder(bausteinFile.getName());

            // import XML testdata
/*
            Arrays.stream(Paths.get("/Users/georg/vms/UbuntuWork/shared/hitass/testfaelle").toFile().
                    listFiles((dir, name) -> name.toLowerCase().contains(bausteinFile.getName().toLowerCase()) && name.endsWith("xml"))).forEach(
                    xmlFile -> {
                        try {
                            client.importXmlForBaustein(
                                    new String(Files.readAllBytes(xmlFile.toPath()), System.getProperty("hit2ass.xml.encoding")).getBytes(),
                                    bausteinFile.getName(), xmlFile.getName());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
            );
*/

            // Try to create workspace and save it to DocRepo
            try {
                long takeStartTime = System.currentTimeMillis();
                byte[] workspaceData = hit2AssService.renderBausteinToWorkspace(bausteinFile.getName(),
                        Files.readAllBytes(bausteinFile.toPath()));
                logger.info(StringUtils.join("workspace rendering took ", (System.currentTimeMillis() - takeStartTime), " ms for Baustein ", bausteinFile.getName(), "."));
                String workspaceElementId = hit2AssService.extractElementIdFromWorkspace(workspaceData);
                String documentElementId = hit2AssService.extractElementIdFromDocument(workspaceData);
                takeStartTime = System.currentTimeMillis();
                client.importWorkspace(workspaceData, bausteinFile.getName(), workspaceElementId);
                logger.info(StringUtils.join("DocRepo-import workspace took ", (System.currentTimeMillis() - takeStartTime), " ms for Baustein ", bausteinFile.getName(), "."));
                takeStartTime = System.currentTimeMillis();
                client.importDeploymentPackage(bausteinFile.getName(), workspaceElementId, documentElementId);
                logger.info(StringUtils.join("DocRepo-import deployment package took ", (System.currentTimeMillis() - takeStartTime), " ms for Baustein ", bausteinFile.getName(), "."));
            } catch (Error | Exception e) {
                // javacc parser throws Errors ...
                String errorMessage = StringUtils.join("Could not process HIT/CLOU Baustein ", bausteinFile.getName(), ", because:",
                        e.getClass().getName(), " - ", e.getMessage());
                logger.error(errorMessage, e);
            }
        });

        // Store deployed module library
        DeployedModuleLibrary.storeHitAssDeployedModuleLibrary();
        byte[] xmlData = DeployedModuleLibrary.peekHitAssDeployedModuleLibrary();
        // sort deployed modules alphabetically by attribute @name
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        logger.info(StringUtils.join("Loaded sorting xsl:\n", new String(IOUtils.toByteArray(HitAssClientTools.class.getResourceAsStream("/xslt/sort.xsl")))));
        try {
            StreamSource styleSheet = new StreamSource(HitAssClientTools.class.getResourceAsStream("/xslt/sort.xsl"));
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(styleSheet);
            transformer.transform(new StreamSource(new ByteArrayInputStream(xmlData)), new StreamResult(result));
        } catch (TransformerException exception) {
            String errMsg = StringUtils.join("Could not sort DeployedModule library alphabetically because: ", exception.getMessage());
            logger.error(errMsg, exception);
            throw new RuntimeException(errMsg, exception);
        }
        logger.info(StringUtils.join("Resulting deployed module library size:\n", result.toByteArray().length, " bytes"));
        IOUtils.write(result.toByteArray(), new BufferedOutputStream(new FileOutputStream("/Users/georg/vms/UbuntuWork/shared/hitass/reverseEngineering/hit2assentis_reworked/zzz.xml")));
        client.importDeploymentPackageWorkspace(xmlData, hit2AssService.extractElementIdFromWorkspace(result.toByteArray()));
        logger.info(StringUtils.join("Trying to inject XML test data into newly created workspaces."));
        client.assignXmlTestData();
    }

}
