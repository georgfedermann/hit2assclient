package org.poormanscastle.products.hit2assclient.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.xml.rpc.ServiceException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.assentis.adb.common.util.ADBUtility;
import com.assentis.docrepo.service.common.DocRepoConstants;
import com.assentis.docrepo.service.common.ServiceUtil;
import com.assentis.docrepo.service.iface.PublicMutator;
import com.assentis.docrepo.service.iface.bean.File;
import com.assentis.docrepo.service.iface.bean.FileMutator;
import com.assentis.docrepo.service.iface.bean.Folder;
import com.assentis.docrepo.service.iface.bean.ProtectedItem;

/**
 * Created by georg on 15.09.16.
 */
class DocRepoServiceImpl implements DocRepoService {

    private final static Logger logger = Logger.getLogger(DocRepoServiceImpl.class);

    private String docRepoBaseUrl = "http://172.20.10.8:12002/DBLayer/services/";
    private String username = "uta";
    private String password = "uta";

    private long bausteinFolderId;

    String bausteinFolderPath = "HitClou/bausteine/";

    private PublicMutator docRepoProxy;

    @Override
    public byte[] retrieveWorkspaceForDbkey(String dbkey) {
        try {
            String url = "jdbc:derby://172.20.10.8:1527/derby/repository.db";
            Connection connection = DriverManager.getConnection(url);
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
        }
    }

    @Override
    public void importWorkspace(byte[] workspaceData, String bausteinName) {
        try {
            // extract the elementId from the new workspace
            OMElement workspaceDocument = OMXMLBuilderFactory.createOMBuilder(
                    new ByteArrayInputStream(workspaceData)).getDocumentElement();
            AXIOMXPath xPath = new AXIOMXPath("/Cockpit/Object[1]/@id");
            String elementId = xPath.stringValueOf(workspaceDocument);

            Folder parentFolder = (Folder) docRepoProxy.getByPath(StringUtils.join(bausteinFolderPath,
                    bausteinName, "/workspace"));
            FileMutator fileMutator = new FileMutator();
            fileMutator.setElementID(elementId);
            fileMutator.setFolder_ID(parentFolder.getDBKey());
            fileMutator.setName(bausteinName);
            fileMutator.setType(DocRepoConstants.FILETYPE_WORKSPACE);

            File file = docRepoProxy.createFile(fileMutator, ADBUtility.zipElementContent(workspaceData), false);
            Long workspaceDbKey = getItemDbKey(fileMutator.getName(), StringUtils.join(bausteinFolderPath, bausteinName,
                    "/workspace/"));

            logger.info(StringUtils.join("Created Workspace file ", file.getDBKey()));

            file.setElementID(elementId);
            docRepoProxy.lockFile(workspaceDbKey);
            docRepoProxy.updateFileAttributes(file);
            docRepoProxy.unlockFile(workspaceDbKey);
        } catch (Exception e) {
            String errorMessage = StringUtils.join("Could not process importWorkspace() for baustein ", bausteinName,
                    ", because of: ", e.getClass().getName(), " - ", e.getMessage());
            logger.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
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
/*
        name = name.toUpperCase();
        if (name.contains(".")) {
            name = name.split("\\.")[1];
        }
*/
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

    void init() {
        try {
            docRepoProxy = ServiceUtil.createPublicMutatorServiceStub(new URL(docRepoBaseUrl), username, password);
            Folder bausteinFolder = (Folder) docRepoProxy.getByPath(bausteinFolderPath);
            bausteinFolderId = bausteinFolder.getDBKey();
            logger.info(StringUtils.join("Found dbkey ", bausteinFolderId, " for folder with path ", bausteinFolderPath));
        } catch (MalformedURLException | ServiceException | RemoteException e) {
            logger.error(StringUtils.join("Got this problem: ", e.getClass().getName(), " - ", e.getMessage(), "; when trying to load this DocRepo folder: ",
                    bausteinFolderPath, ", using this DocRepo URL: ", docRepoBaseUrl));
        }
    }

}
