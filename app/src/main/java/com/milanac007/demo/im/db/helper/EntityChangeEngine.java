package com.milanac007.demo.im.db.helper;

import android.text.TextUtils;

public class EntityChangeEngine {

    // 组建与解析统一地方，方便以后维护
    public static String getSessionKey(int peerId,int sessionType){
        String sessionKey = sessionType + "_" + peerId;
        return sessionKey;
    }

    public static String[] spiltSessionKey(String sessionKey){
        if(TextUtils.isEmpty(sessionKey)){
            throw new IllegalArgumentException("spiltSessionKey error,cause by empty sessionKey");
        }
        String[] sessionInfo = sessionKey.split("_",2);
        return sessionInfo;
    }
}
