package com.milanac007.demo.im.db.entity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.milanac007.demo.im.db.DataBaseHelper;
import com.milanac007.demo.im.db.config.DBConstant;
import com.milanac007.demo.im.logger.Logger;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

/*******************好友，存取DB**********************/
@DatabaseTable(tableName = "contacts")
public class UserEntity extends PeerEntity {

    @DatabaseField
    private String userCode; //账号

    @DatabaseField
    private int gender; //性别 0：男  1：女 -1: unknown

    @DatabaseField
    private String nickName; //备注名

    @DatabaseField
    private String nickNamePinyin; //备注名拼音

    @DatabaseField
    private String phone;

    @DatabaseField
    private String email;

    @DatabaseField
    private int status; //在线/离线

    private boolean isFriend;

    private int action; //用户信息变更类型，0：更新，1：删除，2：被删

    public UserEntity() {
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }


    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getNickNamePinyin() {
        return nickNamePinyin;
    }

    public void setNickNamePinyin(String nickNamePinyin) {
        this.nickNamePinyin = nickNamePinyin;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isFriend() {
        return isFriend;
    }

    public void setFriend(boolean friend) {
        isFriend = friend;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return "UserEntity{" +
                ", peerId=" + peerId +
                ", mainName='" + mainName + '\'' +
                ", pinyinName='" + pinyinName + '\'' +
                ", created=" + created +
                ", updated=" + updated +
                ", avatar='" + avatar + '\'' +
                ", avatarLocalPath=" + avatarLocalPath + '\'' +

                ", userCode='" + userCode + '\'' +
                ", gender=" + gender +
                ", nickName='" + nickName + '\'' +
                ", nickNamePinyin='" + nickNamePinyin + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                ", isFriend=" + isFriend +
                ", action=" + action +
                '}';
    }

    @Override
    public int getType() {
        return DBConstant.SESSION_TYPE_SINGLE;
    }




    ////////////////////////////////////////////////////////////////////////////
    //DB操作

    public static void insertOrUpdateSingleData(UserEntity field){
        checkValid();

        final DataBaseHelper helper = DataBaseHelper.getHelper(context);
        try {
            Dao<UserEntity, String> dao = helper.getDao(UserEntity.class);
            int peerId = field.getPeerId();
            QueryBuilder<UserEntity, String> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq("peerId", peerId);
            List<UserEntity> dataField = queryBuilder.query();
            if(dataField != null && !dataField.isEmpty()) { //dao.update方法更新，需设置id,根据id更新
                long id = dataField.get(0).getId();
                field.setId(id);
                int update = dao.update(field);
                Logger.getLogger().i("update result = %s", update > 0 ? "success": "fail");
            }else {
                dao.create(field);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void insertOrUpdateSingleData(JSONObject data) {
        Gson gson = new Gson();
        UserEntity field = gson.fromJson(data.toJSONString(), UserEntity.class);
        insertOrUpdateSingleData(field);
    }

    /**
     * 事务提交
     * @param fields
     * @return
     */
    public static boolean insertOrUpdateMultiData(final List<UserEntity> fields){

        if(fields == null || fields.isEmpty())
            return true;

        checkValid();
        boolean result = false;
        final DataBaseHelper helper = DataBaseHelper.getHelper(context);
        try {
            final Dao<UserEntity, String> dao = helper.getDao(UserEntity.class);
            ConnectionSource connectionSource = dao.getConnectionSource();
            TransactionManager manager = new TransactionManager(connectionSource);
            Callable<Boolean> callable = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    for(UserEntity field : fields){
                        int peerId = field.getPeerId();
                        QueryBuilder<UserEntity, String> queryBuilder = dao.queryBuilder();
                        queryBuilder.where().eq("peerId", peerId);
                        List<UserEntity> dataField = queryBuilder.query();

                        if(dataField != null && !dataField.isEmpty()) {
                            dao.update(field);
                        }else {
                            dao.create(field);
                        }
                    }
                    return true;
                }
            };

            result = manager.callInTransaction(callable);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static boolean insertOrUpdateMultiData(JSONArray datas) {
//        List<UserEntity> list = new ArrayList<>();
//        Gson gson = new Gson();
//        List<UserEntity> fields = gson.fromJson(datas.toJSONString(), list.getClass());
//        return insertOrUpdateMultiData(fields);

        for(int i = 0; i<datas.size(); i++) {
            JSONObject data =  datas.getJSONObject(i);
            insertOrUpdateSingleData(data);
        }
        return true;
    }



    public static List<UserEntity> getAllContacts() {
        checkValid();
        List<UserEntity> dataField = null;
        final DataBaseHelper helper = DataBaseHelper.getHelper(context);
        try {
            Dao<UserEntity, String> dao = helper.getDao(UserEntity.class);
            QueryBuilder<UserEntity, String> queryBuilder = dao.queryBuilder();
            dataField = queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return dataField;
    }


    public static UserEntity getByPeerId(int peerId){
        checkValid();
        List<UserEntity> dataField = null;
        final DataBaseHelper helper = DataBaseHelper.getHelper(context);
        try {
            Dao<UserEntity, String> dao = helper.getDao(UserEntity.class);
            QueryBuilder<UserEntity, String> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq("peerId", peerId);
            dataField = queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if(dataField.size() == 0) {
            return null;
        }

        return dataField.get(0);
    }

    public static List<UserEntity> getByPeerIds(Collection<Integer> peerIds){
        checkValid();
        List<UserEntity> dataField = null;
        final DataBaseHelper helper = DataBaseHelper.getHelper(context);
        try {
            Dao<UserEntity, String> dao = helper.getDao(UserEntity.class);
            QueryBuilder<UserEntity, String> queryBuilder = dao.queryBuilder();
            queryBuilder.where().in("peerId", peerIds);
            dataField = queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return dataField;
    }

    public static UserEntity getByPhone(String phone){
        checkValid();
        List<UserEntity> dataField = null;
        final DataBaseHelper helper = DataBaseHelper.getHelper(context);
        try {
            Dao<UserEntity, String> dao = helper.getDao(UserEntity.class);
            QueryBuilder<UserEntity, String> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq("phone", phone);
            dataField = queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if(dataField.size() == 0) {
            return null;
        }

        return dataField.get(0);
    }

    public static UserEntity searchByUserCodeOrPhone(String searchInfo){
        checkValid();
        List<UserEntity> dataField = null;
        final DataBaseHelper helper = DataBaseHelper.getHelper(context);
        try {
            Dao<UserEntity, String> dao = helper.getDao(UserEntity.class);
            QueryBuilder<UserEntity, String> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq("phone", searchInfo).or().eq("userCode", searchInfo);
            dataField = queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if(dataField.size() == 0) {
            return null;
        }

        return dataField.get(0);
    }


    public static int getLastUserId(){
        checkValid();
        final DataBaseHelper helper = DataBaseHelper.getHelper(context);
        try {
            Dao<UserEntity, String> dao = helper.getDao(UserEntity.class);
            QueryBuilder<UserEntity, String> queryBuilder = dao.queryBuilder();
            queryBuilder.orderBy("created", false)
                    .orderBy("updated", false)
                    .orderBy("id", false).limit(1);
            List<UserEntity> userEntityList = queryBuilder.query();

            if(userEntityList == null || userEntityList.isEmpty())
                return 10000;

            return userEntityList.get(0).getPeerId();

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    // TODO db->缓存，减少IO
//    public UserEntity findContactByPhone(String phone){
//        Collection<UserEntity> userEntities = userMap.values();
//        for(UserEntity userEntity : userEntities){
//            if(userEntity.getPhone().equals(phone))
//                return userEntity;
//        }
//        return null;
//    }
}
