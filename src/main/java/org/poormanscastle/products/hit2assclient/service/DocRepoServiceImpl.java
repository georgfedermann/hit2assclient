package org.poormanscastle.products.hit2assclient.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.assentis.docrepo.service.common.ServiceUtil;
import com.assentis.docrepo.service.iface.PublicMutator;
import com.assentis.docrepo.service.iface.bean.FileFinder;
import com.assentis.docrepo.service.iface.bean.Folder;

/**
 * Created by georg on 15.09.16.
 */
public class DocRepoServiceImpl implements DocRepoService {

    private final static Logger logger = Logger.getLogger(DocRepoServiceImpl.class);

    private String docRepoBaseUrl = "http://172.20.10.8:12002/DBLayer/services/";
    private String username = "uta";
    private String password = "uta";

    private long bausteinFolderId;

    String bausteinFolderPath = "HitClou/B.ek015";

    private PublicMutator docRepoProxy;

    public void init() {
        try {
            docRepoProxy = ServiceUtil.createPublicMutatorServiceStub(new URL(docRepoBaseUrl), username, password);
            Folder bausteinFolder = (Folder) docRepoProxy.getByPath(bausteinFolderPath);
            bausteinFolderId = bausteinFolder.getDBKey();
            logger.info(StringUtils.join("Found dbkey ", bausteinFolderId, " for folder with path ", bausteinFolderPath));
        } catch (MalformedURLException | ServiceException | RemoteException e) {
            logger.error(StringUtils.join("Got this problem: ", e.getMessage(), "; when trying to load this DocRepo folder: ",
                    bausteinFolderPath, ", using this DocRepo URL: ", docRepoBaseUrl));
        }
    }

    public void doSomething() {
        FileFinder fileFinder = new FileFinder();
        fileFinder.setFolder_ID_IN(new Long[]{0L});
    }

    public static void main(String[] args) {
        DocRepoServiceImpl client = new DocRepoServiceImpl();
        client.init();

    }

}
