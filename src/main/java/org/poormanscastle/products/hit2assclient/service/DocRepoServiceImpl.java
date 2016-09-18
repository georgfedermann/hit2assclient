package org.poormanscastle.products.hit2assclient.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.assentis.adb.common.util.ADBUtility;
import com.assentis.docrepo.service.common.DocRepoConstants;
import com.assentis.docrepo.service.common.ServiceUtil;
import com.assentis.docrepo.service.iface.PublicMutator;
import com.assentis.docrepo.service.iface.bean.File;
import com.assentis.docrepo.service.iface.bean.FileMutator;
import com.assentis.docrepo.service.iface.bean.Folder;

/**
 * Created by georg on 15.09.16.
 */
class DocRepoServiceImpl implements DocRepoService {

    private final static Logger logger = Logger.getLogger(DocRepoServiceImpl.class);

    private String docRepoBaseUrl = "http://192.168.188.60:12002/DBLayer/services/";
    private String username = "uta";
    private String password = "uta";

    private long bausteinFolderId;

    String bausteinFolderPath = "HitClou/bausteine/";

    private PublicMutator docRepoProxy;

    @Override
    public void importWorkspace(byte[] workspaceData, String bausteinName) {
        try {
            Folder parentFolder = (Folder) docRepoProxy.getByPath(StringUtils.join(bausteinFolderPath,
                    bausteinName, "/workspace"));
            FileMutator fileMutator = new FileMutator();
            fileMutator.setFolder_ID(parentFolder.getDBKey());
            fileMutator.setName(bausteinName);
            fileMutator.setType(DocRepoConstants.FILETYPE_WORKSPACE);
            File file = docRepoProxy.createFile(fileMutator, ADBUtility.zipElementContent(workspaceData), false);
            logger.info(StringUtils.join("Created Workspace file ", file.getDBKey()));

        } catch (RemoteException e) {
            String errorMessage = StringUtils.join("Could not process importWorkspace() for baustein ", bausteinName, ", because of: ",
                    e.getClass().getName(), " - ", e.getMessage());
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
            bausteinFolder.setDBKey(getFolderDbKey(name, bausteinFolderPath));

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
    public Long getFolderDbKey(String folderName, String path) {
        try {
            Folder folder = (Folder) docRepoProxy.getByPath(StringUtils.join(path.endsWith("/") ?
                    path : StringUtils.join(path, "/"), folderName));
            return folder.getDBKey();
        } catch (RemoteException e) {
            String errorMessage = StringUtils.join("Could not process getFolderDbKey for path ", path, " and folderName ", folderName,
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
