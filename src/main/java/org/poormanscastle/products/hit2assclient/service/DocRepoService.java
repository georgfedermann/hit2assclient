package org.poormanscastle.products.hit2assclient.service;

/**
 * Created by georg on 15.09.16.
 */
public interface DocRepoService {

    void createFolder(String name);

    /**
     * use this method to retrieve the dbkey of DocRepo files and folders.
     *
     * @param itemName the name of the folder or file
     *                 e.g. if there is a repository named HitClou containing the folder structure
     *                 bausteine/workspace/ and you're looking for the dbkey of the Baustein Beschwerde_117
     *                 contained in workspace, the itemName would be "Beschwerde_117".
     * @param path     the path leading to the folder or file, including the containing repository.
     *                 e.g. if there is a repository named HitClou containing the folder structure
     *                 bausteine/workspace/ and you're looking for the dbkey of the Baustein Beschwerde_117
     *                 contained in workspace, the path would be "HitClou/bausteine/workspace/".
     * @return the dbkey used in the CockpitSchema to refer to this thing.
     */
    Long getItemDbKey(String itemName, String path);

    void importXmlForBaustein(byte[] xmlData, String bausteinName, String xmlFileName);

    void importWorkspace(byte[] workspaceData, String bausteinName);

    byte[] retrieveWorkspaceForDbkey(String dbkey);

    static DocRepoService getDocRepoService() {
        DocRepoServiceImpl service = new DocRepoServiceImpl();
        service.init();
        return service;
    }

}
