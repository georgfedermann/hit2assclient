package org.poormanscastle.products.hit2assclient.cli;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.poormanscastle.products.hit2assclient.service.DocRepoService;

/**
 * Created by georg on 20.09.16.
 */
public class ExportWorkspace {

    public static void main(String[] args) throws Exception {
        DocRepoService client = DocRepoService.getDocRepoService();

        OutputStream foo = new BufferedOutputStream(
                new FileOutputStream("/Users/georg/tmp/parking/foo_31354.acr"));
        foo.write(client.retrieveWorkspaceForDbkey("31354"));
        foo.flush();
        foo.close();

        foo = new BufferedOutputStream(
                new FileOutputStream("/Users/georg/tmp/parking/foo_31357.acr"));
        foo.write(client.retrieveWorkspaceForDbkey("31357"));
        foo.flush();
        foo.close();
    }

}
