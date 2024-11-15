package com.milanac007.demo.im.db.entity;

import android.content.Context;
import android.text.TextUtils;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.milanac007.demo.im.db.DataBaseHelper;
import com.milanac007.demo.im.logger.Logger;
import com.milanac007.demo.imserver.App;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/*******************UserEntry的额外信息，存取DB**********************/
@DatabaseTable(tableName = "extra")
public
class ExtraEntry {
    @DatabaseField(generatedId = true) //指定字段为自增主键, 唯一
    protected long id;

    @DatabaseField
    protected int peerId;

    @DatabaseField
    private String friendList;    //好友peerid列表，逗号隔开

    //ORMLite does not know how to store interface java.util.Map for field actionMap. Use another class or a custom persister.
//    @DatabaseField
//    private Map<Integer, Integer> actionMap;  //用户信息变更类型，0：更新，1：删除，2：被删

    @DatabaseField
    private int friendListVersion; //好友列表版本号，初始时为1

    @DatabaseField
    private String groupIdList; //个人所在的群id的集合

    private static Context context;

    public ExtraEntry() {

    }

    public static void checkValid() {
        if(context == null) {
            context = App.getContext();
        }
    }

    @Override
    public String toString() {
        return "UserEntity{" +
                ", peerId=" + peerId +
                ", friendList=" + friendList +
                ", friendListVersion=" + friendListVersion +
                ", groupIdList=" + groupIdList +
                '}';
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }


    public int getPeerId() {
        return peerId;
    }

    public void setPeerId(int peerId) {
        this.peerId = peerId;
    }

    public String getFriendList() {
        return friendList;
    }

    public void setFriendList(String friendList) {
        this.friendList = friendList;
    }

    public int getFriendListVersion() {
        return friendListVersion;
    }

    public void setFriendListVersion(int friendListVersion) {
        this.friendListVersion = friendListVersion;
    }

    public String getGroupIdList() {
        return groupIdList;
    }

    public void setGroupIdList(String groupIdList) {
        this.groupIdList = groupIdList;
    }

    public Set<Integer> getFriendIds(){
        if(TextUtils.isEmpty(friendList)){
            return Collections.emptySet();
        }
        String[] arrayUserIds =  friendList.trim().split(",");
        if(arrayUserIds.length <=0){
            return Collections.emptySet();
        }

        Set<Integer> result = new HashSet<>();
        for(int index=0;index < arrayUserIds.length;index++){
            int userId =  Integer.parseInt(arrayUserIds[index]);
            result.add(userId);
        }
        return result;
    }

    public void setGroupIds(Set<Integer>groupIdList){
        String groupListStr = TextUtils.join(",", groupIdList);
        setGroupIdList(groupListStr);
    }

    public Set<Integer> getGroupIds(){
        if(TextUtils.isEmpty(groupIdList)){
            return Collections.emptySet();
        }
        String[] arrayGroupIds =  groupIdList.trim().split(",");
        if(arrayGroupIds.length <=0){
            return Collections.emptySet();
        }

        Set<Integer> result = new HashSet<>();
        for(int index=0;index < arrayGroupIds.length;index++){
            int groupId =  Integer.parseInt(arrayGroupIds[index]);
            result.add(groupId);
        }
        return result;
    }

    public void setFriendIds(Set<Integer> friendList){
        String friendListStr = TextUtils.join(",", friendList);
        setFriendList(friendListStr);
    }

    public void addFriend(int friendId) {
        if (friendList == null) {
            friendList = "";
        }

        if (!friendList.contains(String.valueOf(friendId))) {
            if (!TextUtils.isEmpty(friendList)) {
                friendList += ",";
            }
            friendList += friendId;
            friendListVersion++;
//            if (actionMap == null) {
//                actionMap = new HashMap<>();
//            }
//            actionMap.put(friendId, 0);
        }
    }

    public void delFriend(int friendId){
        if (friendList == null) {
            friendList = "";
        }
        if (friendList.contains(String.valueOf(friendId))) {
            Set<Integer> friendIds = getFriendIds();
            friendIds.remove(friendId);
            setFriendIds(friendIds);
            friendListVersion++;
//            if (actionMap != null) {
//                actionMap = new HashMap<>();
//                actionMap.remove(friendId);
//            }
        }
    }

    public void addGroupId(int groupId){
        if (!groupIdList.contains(String.valueOf(groupId))) {
            groupIdList += ",";
            groupIdList += groupId;
        }
    }

    public void delGroup(int groupId){
        if (groupIdList.contains(String.valueOf(groupId))) {
            Set<Integer> groupIds = getGroupIds();
            groupIds.remove(groupId);
            setFriendIds(groupIds);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    //DB操作

    public static void insertOrUpdateSingleData(ExtraEntry field){
        checkValid();

        final DataBaseHelper helper = DataBaseHelper.getHelper(context);
        try {
            Dao<ExtraEntry, String> dao = helper.getDao(ExtraEntry.class);
            int peerId = field.getPeerId();
            QueryBuilder<ExtraEntry, String> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq("peerId", peerId);
            List<ExtraEntry> dataField = queryBuilder.query();
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

    /**
     * 事务提交
     * @param fields
     * @return
     */
    public static boolean insertOrUpdateMultiData(final List<ExtraEntry> fields){

        if(fields == null || fields.isEmpty())
            return true;

        checkValid();
        boolean result = false;
        final DataBaseHelper helper = DataBaseHelper.getHelper(context);
        try {
            final Dao<ExtraEntry, String> dao = helper.getDao(ExtraEntry.class);
            ConnectionSource connectionSource = dao.getConnectionSource();
            TransactionManager manager = new TransactionManager(connectionSource);
            Callable<Boolean> callable = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    for(ExtraEntry field : fields){
                        int peerId = field.getPeerId();
                        QueryBuilder<ExtraEntry, String> queryBuilder = dao.queryBuilder();
                        queryBuilder.where().eq("peerId", peerId);
                        List<ExtraEntry> dataField = queryBuilder.query();

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

    public static Set<Integer> getFriendIdsByPeerId(int peerId){
        checkValid();
        Set<Integer> dataField = new HashSet<>();
        dataField.add(peerId); //添加自己

        final DataBaseHelper helper = DataBaseHelper.getHelper(context);
        try {
            Dao<ExtraEntry, String> dao = helper.getDao(ExtraEntry.class);
            QueryBuilder<ExtraEntry, String> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq("peerId", peerId);
            List<ExtraEntry> field = queryBuilder.query();
            if(field != null && !field.isEmpty()) {
                if (!field.get(0).getFriendIds().isEmpty()) {
                    dataField.addAll(field.get(0).getFriendIds());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return dataField;
    }


    public static ExtraEntry getExtraEntryPeerId(int peerId){
        checkValid();
        ExtraEntry dataField = null;
        final DataBaseHelper helper = DataBaseHelper.getHelper(context);
        try {
            Dao<ExtraEntry, String> dao = helper.getDao(ExtraEntry.class);
            QueryBuilder<ExtraEntry, String> queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq("peerId", peerId);
            List<ExtraEntry> field = queryBuilder.query();
            if(field != null && !field.isEmpty()) {
                dataField = field.get(0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return dataField;
    }
}
