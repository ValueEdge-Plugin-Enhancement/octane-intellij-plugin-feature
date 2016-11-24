package com.hpe.adm.octane.ideplugins.services;

import com.hpe.adm.nga.sdk.model.EntityModel;

import java.util.Collection;

public class TestService extends ServiceBase{

    public Collection<EntityModel> getDefects(){
        return getOctane().entityList("defects").get().execute();
    }

    /**
     * Check if the current connection settings are valid
     */
    public void testConnection() throws Exception {
        try {
            getOctane();
            //rethrow runtime exceptions as checked exceptions
        } catch (Exception ex){
            throw ex;
        }
    }


}
