package org.poormanscastle.products.hit2assclient;

import org.junit.Test;
import org.poormanscastle.products.hit2assclient.service.DocRepoService;

/**
 * Created by georg on 19/11/2017.
 */
public class DocRepoServiceTest {
    
    @Test
    public void testAssignXmlTestData(){
        DocRepoService service = DocRepoService.getDocRepoService();
        service.assignXmlTestData();
    }
    
}
