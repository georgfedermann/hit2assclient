package org.poormanscastle.products.hit2assclient.service;

import java.beans.PropertyVetoException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.rpc.ServiceException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.assentis.adb.common.util.ADBUtility;
import com.assentis.docrepo.service.common.DocRepoConstants;
import com.assentis.docrepo.service.common.ServiceUtil;
import com.assentis.docrepo.service.iface.PublicMutator;
import com.assentis.docrepo.service.iface.bean.File;
import com.assentis.docrepo.service.iface.bean.FileFinder;
import com.assentis.docrepo.service.iface.bean.FileMutator;
import com.assentis.docrepo.service.iface.bean.Folder;
import com.assentis.docrepo.service.iface.bean.ProtectedItem;
import com.assentis.docrepo.service.iface.bean.SearchOptions;
import com.assentis.docrepo.service.iface.bean.SearchResult;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import ch.assentis.dblayer.common.XMLUtils;

/**
 * Created by georg on 15.09.16.
 */
class DocRepoServiceImpl implements DocRepoService {

    private String protocol = "http://";
    private String serverName = "192.168.182.132";
    private String docRepoPort = "12002";
    private String docRepoServicesWebContext = "/DBLayer/services/";

    private String derbyUrl = StringUtils.join("jdbc:derby://", serverName, ":1527/derby/repository.db");

    private final static Logger logger = Logger.getLogger(DocRepoServiceImpl.class);

    private String docRepoBaseUrl = StringUtils.join(protocol, serverName, ":", docRepoPort, docRepoServicesWebContext);
    private String username = "uta";
    private String password = "uta";

    private long bausteinFolderId;

    String bausteinFolderPath = "HitClou/bausteine/";
    String dbLibPath = "HitClou/dblib/";

    ComboPooledDataSource cpds = null;

    private PublicMutator docRepoProxy;

    void init() {
        try {

            cpds = new ComboPooledDataSource();
            cpds.setDriverClass("org.apache.derby.jdbc.ClientDriver"); //loads the jdbc driver
            cpds.setJdbcUrl(derbyUrl);
            // cpds.setUser("swaldman");
            // cpds.setPassword("test-password");

            // the settings below are optional -- c3p0 can work with defaults
            cpds.setMinPoolSize(5);
            cpds.setAcquireIncrement(5);
            cpds.setMaxPoolSize(20);

            docRepoProxy = ServiceUtil.createPublicMutatorServiceStub(new URL(docRepoBaseUrl), username, password);
            Folder bausteinFolder = (Folder) docRepoProxy.getByPath(bausteinFolderPath);
            bausteinFolderId = bausteinFolder.getDBKey();
            logger.info(StringUtils.join("Found dbkey ", bausteinFolderId, " for folder with path ", bausteinFolderPath));
        } catch (MalformedURLException | ServiceException | RemoteException | PropertyVetoException e) {
            logger.error(StringUtils.join("Got this problem: ", e.getClass().getName(), " - ", e.getMessage(), "; when trying to load this DocRepo folder: ",
                    bausteinFolderPath, ", using this DocRepo URL: ", docRepoBaseUrl));
        }
    }

    @Override
    public byte[] retrieveWorkspaceForDbkey(String dbkey) {
        Connection connection = null;
        try {
            connection = cpds.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(StringUtils.join("select content from COCKPITSCHEMA.ELEMENTCONTENT where cockpitelement_id = ", dbkey, " for update"));
            if (resultSet.next()) {
                Blob ablob = resultSet.getBlob(1);
                InputStream blobInputStream = ablob.getBinaryStream();
                byte[] result = IOUtils.toByteArray(blobInputStream);
                resultSet.close();
                return result;
            } else {
                String errMsg = StringUtils.join("Could not find elementcontent for dbkey ", dbkey);
                logger.error(errMsg);
                throw new RuntimeException(errMsg);
            }
        } catch (IOException | SQLException e) {
            String errorMessage = StringUtils.join("Could not load zipped blob from elementcontent for dbkey: ", dbkey, ", because: ",
                    e.getClass().getName(), " - ", e.getMessage());
            logger.error(errorMessage);
            throw new RuntimeException(errorMessage, e);
        } finally {
            try {
                connection.close();
            } catch (Exception e) {
                String errMsg = StringUtils.join("Error when trying to return connection to pool: ",
                        e.getClass().getSimpleName(), " - ", e.getMessage());
                logger.error(errMsg, e);
            }
        }
    }

    @Override
    public void importDeploymentPackage(String bausteinName, String workspaceElementId, String documentElementId) {
        // create zipped file with dummy deployment package
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ZipOutputStream zipper = new ZipOutputStream(buffer);
        // DeploymentPackage dummy data is contained in the jar file at the location dpData.
        // try to find the jar file from which the hitAssClient is executed and gain access to this folder.
        java.io.File jarFile = null;
        FileSystem jarFileSystem = null;
        try {
            jarFile = new java.io.File(DocRepoServiceImpl.class.getProtectionDomain().getCodeSource().getLocation().
                    toURI().getPath());
            jarFileSystem = FileSystems.newFileSystem(Paths.get(jarFile.getAbsolutePath()), null);
        } catch (Exception e) {
            String errMsg = StringUtils.join("Could not access jar hitAssClient.jar file, because: ", e.getMessage());
            logger.error(errMsg, e);
            return;
        }
        // Path dpInputFolder = Paths.get(getClass().getClassLoader().getResource("dpData").getPath());
        Path dpInputFolder = jarFileSystem.getPath("/dpData");
        try {
            Files.walk(dpInputFolder)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                                ZipEntry zipEntry = new ZipEntry(path.getFileName().toString());

                                try {
                                    byte[] data = Files.readAllBytes(path);
                                    // replace workspaceElementId in file Metadata.xml
                                    if (!StringUtils.isBlank(path.getFileName().toString()) &&
                                            path.getFileName().toString().endsWith("Metadata.xml")) {
                                        data = new String(data).replace("__workspaceid__", workspaceElementId).getBytes();
                                    }
                                    zipper.putNextEntry(zipEntry);
                                    zipper.write(data);
                                    zipper.closeEntry();
                                } catch (IOException e) {
                                    String errorMessage = StringUtils.join("Could not add ", path.getFileName().toString(),
                                            " to deployment package zip stream because: ", e.getClass().getName(), " - ",
                                            e.getMessage());
                                    logger.error(errorMessage, e);
                                    throw new RuntimeException(errorMessage, e);
                                }
                            }
                    );
        } catch (IOException e) {
            String errorMessage = StringUtils.join("Could not import deployment package because: ",
                    e.getClass().getName(), " - ", e.getMessage());
            logger.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        } finally {
            try {
                zipper.flush();
                zipper.close();
//                OutputStream testStream = new BufferedOutputStream(new FileOutputStream("/Users/georg/tmp/parking/test.zip"));
//                IOUtils.write(buffer.toByteArray(), testStream);
//                testStream.flush();
//                testStream.close();
            } catch (IOException e) {
                // Underlying stream is a ByteArrayOutputStream. There should be no troubles here.
                logger.error(e);
            }
        }
        // Write zipped file to repository
        if (buffer.toByteArray() == null || buffer.toByteArray().length == 0) {
            logger.error("Could not import deployment package since no zipped data available.");
            return;
        }

        try {
            long takeStartTime = System.currentTimeMillis();
            Folder parentFolder = (Folder) docRepoProxy.getByPath(StringUtils.join(
                    bausteinFolderPath, bausteinName, "/workspace"));
            FileMutator fileMutator = new FileMutator();
            fileMutator.setElementID(StringUtils.join(documentElementId, ".dp"));
            fileMutator.setFolder_ID(parentFolder.getDBKey());
            fileMutator.setName(StringUtils.join(bausteinName, "_dp"));
            fileMutator.setType(DocRepoConstants.FILETYPE_DEPLOYMENTPACKAGE);

            File file = docRepoProxy.createFile(fileMutator, ADBUtility.zipElementContent(buffer.toByteArray()), false);
            logger.info(StringUtils.join("Created deployment package file ", file.getDBKey()));
            logger.info(StringUtils.join("Uploading deployment package to DocRepo took ", (System.currentTimeMillis() - takeStartTime), " ms for Baustein ", bausteinName, "."));

            takeStartTime = System.currentTimeMillis();
            // add dependency between workspace and deployment package to the REF table;
            String sql = StringUtils.join("INSERT INTO COCKPITSCHEMA.deploymentpackage(DEPLOYMENTPACKAGE_ID, WORKSPACE_ID, DEPLPKGCOCKPITELEMENT_ID, ID, DEPENDENTID) VALUES ((select INTEGER(max(deploymentpackage_id)+1) from COCKPITSCHEMA.deploymentpackage), null, null, '",
                    documentElementId, ".dp', '", workspaceElementId, "')");
            Connection connection = cpds.getConnection();
            Statement statement = connection.createStatement();
            statement.executeUpdate(sql);
            // set deployment package alias name
            sql = StringUtils.join("update COCKPITSCHEMA.cockpitelement set DOCBASEALIAS = '", bausteinName,
                    "' where elementid = '", documentElementId, ".dp'");
            statement.executeUpdate(sql);
            statement.close();
            connection.close();
            logger.info(StringUtils.join("Updating database to reflect new deployment package for ", bausteinName, " took ",
                    System.currentTimeMillis() - takeStartTime, " ms."));

        } catch (Exception e) {
            String errorMessage = StringUtils.join("Could not process importDeploymentPackage() for baustein ",
                    bausteinName, ", because of: ", e.getClass().getName(), " - ", e.getMessage());
            logger.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    @Override
    public void importWorkspace(byte[] workspaceData, String bausteinName, String elementId) {
        try {
            Folder parentFolder = (Folder) docRepoProxy.getByPath(StringUtils.join(
                    bausteinFolderPath, bausteinName, "/workspace"));
            FileMutator fileMutator = new FileMutator();
            fileMutator.setElementID(elementId);
            fileMutator.setFolder_ID(parentFolder.getDBKey());
            fileMutator.setName(bausteinName);
            fileMutator.setType(DocRepoConstants.FILETYPE_WORKSPACE);
            File file = docRepoProxy.createFile(fileMutator, ADBUtility.zipElementContent(workspaceData), false);
            logger.info(StringUtils.join("Created Workspace file ", file.getDBKey(), " for baustein ", bausteinName, "."));
        } catch (Exception e) {
            String errorMessage = StringUtils.join("Could not process importWorkspace() for baustein ", bausteinName,
                    ", because of: ", e.getClass().getName(), " - ", e.getMessage());
            logger.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    @Override
    public void importDeploymentPackageWorkspace(byte[] workspaceData, String elementId) {
        try {
            Folder parentFolder = (Folder) docRepoProxy.getByPath(dbLibPath);
            FileMutator fileMutator = new FileMutator();
            fileMutator.setElementID(elementId);
            fileMutator.setFolder_ID(parentFolder.getDBKey());
            fileMutator.setName("HitAssDeploymentPackageLibrary");
            fileMutator.setType(DocRepoConstants.FILETYPE_WORKSPACE);
            File file = docRepoProxy.createFile(fileMutator, ADBUtility.zipElementContent(workspaceData), false);
            logger.info(StringUtils.join("Created HitAssDeploymentPackageLibrary in DocRepo."));
        } catch (Exception e) {
            String errMsg = StringUtils.join("Could not import the given workspace data as the deployment package library, because of: ",
                    e.getClass().getName(), " - ", e.getMessage());
            logger.error(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }
    }

    @Override
    public void importXmlForBaustein(byte[] xmlData, String bausteinName, String xmlFileName) {
        try {
            Folder parentFolder = (Folder) docRepoProxy.getByPath(StringUtils.join(bausteinFolderPath,
                    bausteinName, "/testdata"));
            FileMutator fileMutator = new FileMutator();
            fileMutator.setFolder_ID(parentFolder.getDBKey());
            fileMutator.setName(xmlFileName);
            fileMutator.setType(DocRepoConstants.FILETYPE_XML);
            File file = docRepoProxy.createFile(fileMutator, ADBUtility.zipElementContent(xmlData), false);
            logger.info(StringUtils.join("Created XML file ", file.getDBKey()));

        } catch (RemoteException e) {
            String errorMessage = StringUtils.join("Could not process importXmlForBaustein() for baustein ", bausteinName, ", because of: ",
                    e.getClass().getName(), " - ", e.getMessage());
            logger.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    @Override
    public void createFolder(String name) {
        logger.info(StringUtils.join("Creating folder for ", name));

        try {
            Folder bausteinFolder = new Folder();
            bausteinFolder.setName(name);
            bausteinFolder.setParent_ID(bausteinFolderId);
            docRepoProxy.createFolder(bausteinFolder);
            bausteinFolder.setDBKey(getItemDbKey(name, bausteinFolderPath));

            Folder workspaceFolder = new Folder();
            workspaceFolder.setName("workspace");
            workspaceFolder.setParent_ID(bausteinFolder.getDBKey());
            docRepoProxy.createFolder(workspaceFolder);

            Folder testDataFolder = new Folder();
            testDataFolder.setName("testdata");
            testDataFolder.setParent_ID(bausteinFolder.getDBKey());
            docRepoProxy.createFolder(testDataFolder);

            logger.info("Done.");
        } catch (RemoteException e) {
            String errorMessage = StringUtils.join("Could not process createFolder for baustein ", name, ", because of: ",
                    e.getClass().getName(), " - ", e.getMessage());
            logger.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    @Override
    public Long getItemDbKey(String itemName, String path) {
        try {
            ProtectedItem item = docRepoProxy.getByPath(StringUtils.join(path.endsWith("/") ?
                    path : StringUtils.join(path, "/"), itemName));
            item.getDBKey();
            return item.getDBKey();
        } catch (RemoteException e) {
            String errorMessage = StringUtils.join("Could not process getFolderDbKey for path ", path, " and folderName ", itemName,
                    ", because of: ",
                    e.getClass().getName(), " - ", e.getMessage());
            logger.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    @Override
    public void assignXmlTestData() {
        String xmlDateiPath = "/Cockpit/Object/Object[@name='Variable Groups']/Object/Object/Object[@name='HitAss XML File']/Properties/Attributes/Attribute[@name='URI']/Object/Properties/Attributes/Attribute";

        try {
            PublicMutator proxy = ServiceUtil.createPublicMutatorServiceStub(new URL(docRepoBaseUrl), username, password);

            FileFinder ff = new FileFinder();
            ff.setType(DocRepoConstants.FILETYPE_WORKSPACE);
            ProtectedItem pi = proxy.getByPath("HitClou/bausteine/");
            SearchOptions options = new SearchOptions();
            options.setContext(pi);
            SearchResult[] foundWorkspaces = proxy.search(new FileFinder[]{ff}, options);

            for (SearchResult workspaceSearchResult : foundWorkspaces) {
                ProtectedItem workspaceProtectedItem = workspaceSearchResult.getItem();

                if (workspaceProtectedItem instanceof File) {
                    File workspaceFile = (File) workspaceProtectedItem;

                    FileFinder ffXml = new FileFinder();
                    ffXml.setType(DocRepoConstants.FILETYPE_XML);
                    ffXml.setName(StringUtils.join("%", workspaceFile.getName(), "%"));
                    ffXml.setNamePattern(true);
                    ProtectedItem piXmlFolder = proxy.getByPath("HitClouTestdata/BA/all");
                    SearchOptions optionsXml = new SearchOptions();
                    optionsXml.setContext(piXmlFolder);
                    SearchResult[] foundXml = proxy.search(new FileFinder[]{ffXml}, optionsXml);
                    if (foundXml.length > 0) {
                        ProtectedItem piResXml = foundXml[0].getItem();
                        if (piResXml instanceof File) {
                            File xmlFile = (File) piResXml;
                            String elementName = xmlFile.getName();
                            String elementDBID = xmlFile.getDBKey().toString();
                            String elementID = xmlFile.getElementID();

                            long dbkey = workspaceFile.getDBKey();
                            byte[] data = proxy.getFile(dbkey, true).getContentZipped();
                            byte[] unzipped = ADBUtility.unzipElementContent(data);
                            Document doc = XMLUtils.parseDocument(unzipped);

                            XMLUtils.findOrCreateNode(doc, xmlDateiPath + "[@name='RepoElementID']", false).setTextContent(elementID);
                            XMLUtils.findOrCreateNode(doc, xmlDateiPath + "[@name='RepoElementName']", false).setTextContent(elementName);
                            XMLUtils.findOrCreateNode(doc, xmlDateiPath + "[@name='RepoElementExtension']", false).setTextContent("xml");
                            XMLUtils.findOrCreateNode(doc, xmlDateiPath + "[@name='RepoElementDBID']", false).setTextContent(elementDBID);
                            byte[] newContent = XMLUtils.serializeDocument(doc);

                            proxy.lockFile(dbkey);
                            proxy.updateFileContent(dbkey, ADBUtility.zipElementContent(newContent), false, true);
                        }
                    }
                }
            }
        } catch (Exception exception) {
            String errMsg = StringUtils.join("Failed to map XML test data to newly created workspaces, because: ", exception.getMessage());
            logger.error(errMsg, exception);
        }
    }

}
