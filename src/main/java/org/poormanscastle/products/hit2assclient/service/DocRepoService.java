package org.poormanscastle.products.hit2assclient.service;

/**
 * Created by georg on 15.09.16.
 */
public interface DocRepoService {

    void createFolder(String name);

    Long getFolderDbKey(String folderName, String path);

    void importXmlForBaustein(byte[] xmlData, String bausteinName, String xmlFileName);

    void importWorkspace(byte[] workspaceData, String bausteinName);
    
    static DocRepoService getDocRepoService() {
        DocRepoServiceImpl service = new DocRepoServiceImpl();
        service.init();
        return service;
    }

}
