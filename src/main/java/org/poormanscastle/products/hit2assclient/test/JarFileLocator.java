package org.poormanscastle.products.hit2assclient.test;

import java.io.File;

/**
 * Created by georg on 07/09/2017.
 */
public class JarFileLocator {

    public static void main(String[] args) throws Exception {
        System.out.println(new File(JarFileLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()));
    }

}
