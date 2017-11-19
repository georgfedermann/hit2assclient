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

    /**
     * import a given (generated) workspace into DocRepo.
     *
     * @param workspaceData a byte[] that contains workspace XML data
     * @param bausteinName  the name for the new workspace
     * @param elementId     the elementId was created with the new workspace (if you are using the Hit2Ass generator)
     *                      and can be extracted from the workspace data using HitAssService.extractElementIdFromWorkspace()
     */
    void importWorkspace(byte[] workspaceData, String bausteinName, String elementId);

    /**
     * technically very similar to importWorkspace(), but the workspace being imported is the one containing the
     * deployed modules for the base workspaces containing the pendants to the Hit/Clou Bausteine.
     * The workspace is imported to a different location, and the name is set to HitAssDeploymentPackageLibrary
     * automatically.
     *
     * @param workspaceData
     * @param elementId
     */
    void importDeploymentPackageWorkspace(byte[] workspaceData, String elementId);

    /**
     * Imports a deployment package for a given workspace and creates all necessary references. The deployment package
     * is created for a document within a given workspace and contains several files describing how this document
     * can be rendered. If the document is named HitAssDocument, the deployment package is a zip package containing
     * the following files: deploymentlog.log, HitAssDocument.CHARTDEF.xslt, HitAssDocument.FO.xslt,
     * Metadata.xml, TextSystem.xml.
     *
     * @param bausteinName       the name of the baustein (which also is the name of the corresponding workspace)
     * @param workspaceElementId the elementId of the corresponding workspace
     * @param documentElementId  the elementId of the document within the workspace for which a deployment package
     *                           shall be generated.
     */
    void importDeploymentPackage(String bausteinName, String workspaceElementId, String documentElementId);

    /**
     * iterates over newly created workspaces, used some naming conventions to look for
     * XML testdata at a well known test data repository within DocRepo. And if it finds any
     * it will add a reference to the XML test data within the workspace.
     */
    void assignXmlTestData();

    byte[] retrieveWorkspaceForDbkey(String dbkey);

    static DocRepoService getDocRepoService() {
        DocRepoServiceImpl service = new DocRepoServiceImpl();
        service.init();
        return service;
    }

}
