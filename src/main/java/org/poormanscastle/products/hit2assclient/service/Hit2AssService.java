package org.poormanscastle.products.hit2assclient.service;

/**
 * Created by georg on 16.09.16.
 */
public interface Hit2AssService {

    byte[] renderBausteinToWorkspace(byte[] bausteinData);
    
    static Hit2AssService getHit2AssService(){
        return new Hit2AssServiceImpl();
    }

}
