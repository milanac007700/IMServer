package com.milanac007.demo.im.db.callback;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.milanac007.demo.im.db.config.DBConstant;
import com.milanac007.demo.im.db.entity.ExtraEntry;
import com.milanac007.demo.im.db.entity.GroupEntity;
import com.milanac007.demo.im.db.entity.GroupMemberEntity;
import com.milanac007.demo.im.db.entity.MessageEntity;
import com.milanac007.demo.im.db.entity.UserEntity;
import com.milanac007.demo.im.db.helper.SequenceNumberMaker;
import com.milanac007.demo.imserver.WebSocketClient;
import com.milanac007.demo.imserver.WebSocketServerImpl;

import org.java_websocket.WebSocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Task {
   private String message;
   private int reqUserId;
   private int tn;
   private String type;
   private String data;
   private WebSocket conn;
   private WebSocketServerImpl webSocketServer;

   public Task(WebSocket conn, String message, WebSocketServerImpl webSocketServer) {
      JSONObject msgObject = JSONObject.parseObject(message);
      reqUserId = msgObject.getIntValue("reqUserId");
      tn = msgObject.getIntValue("tn");
      type = msgObject.getString("type");
      data = msgObject.getString("data");
      this.message = message;
      this.conn = conn;
      this.webSocketServer = webSocketServer;
   }

   public void exec() {
      switch (type) {
         case IMBaseDefine.Login:{
            processLoginRequest(reqUserId, tn, type, data, conn);
         } break;
         case IMBaseDefine.AllUserList:{
            processGetAllUserList(reqUserId, tn, type, data, conn);
         } break;
         case IMBaseDefine.BuddyListUserInfo:{
            processGetBuddyListUserInfo(reqUserId, tn, type, data, conn);
         } break;
         case IMBaseDefine.ReqSearchBuddy:{
            processSearchBuddyInfo(reqUserId, tn, type, data, conn);
         } break;
         case IMBaseDefine.ReqAddBuddy:{
            processReqAddBuddy(reqUserId, tn, type, data, conn);
         } break;
         case IMBaseDefine.ConfirmAddBuddy:{
            processConfirmAddBuddy(reqUserId, tn, type, data, conn);
         } break;
         case IMBaseDefine.AllNormalGroupList:{
            processGetAllNormalGroup(reqUserId, tn, type, data, conn);
         } break;
         case IMBaseDefine.GetGroupDetailInfo:{
            processGetGroupDetailInfo(reqUserId, tn, type, data, conn);
         } break;
         case IMBaseDefine.Msg: {
            processMsgTransmit(reqUserId, tn, type, data, conn);
         } break;
         case IMBaseDefine.MsgAck: {
            processMsgAsk(reqUserId, tn, type, data, conn);
         } break;
         case IMBaseDefine.GroupChangeMember: {
            processGroupChangeMember(reqUserId, tn, type, data, conn);
         } break;
         case IMBaseDefine.ReqCreateGroup:{
            processReqCreateGroup(reqUserId, tn, type, data, conn);
         } break;
         default: {
            webSocketServer.getClients().get(conn).onMessage(message);
         } break;
      }
   }

   private void processLoginRequest(int reqUserId, int tn, String type, String data, WebSocket conn) {
      Gson gson = new Gson();
      UserEntity user = gson.fromJson(data, UserEntity.class);
      UserEntity result = UserEntity.getByPeerId(user.getPeerId());
      JSONObject loginDataAck = new JSONObject();
      if(result == null) {
         loginDataAck.put("tn", tn);
         loginDataAck.put("type", type);
         JSONObject dataObject = new JSONObject();
         dataObject.put("resultCode", -1);
         dataObject.put("errorMsg", "无此账号");
         loginDataAck.put("data", dataObject);

         WebSocketClient webSocketClient = webSocketServer.getClients().get(conn);
         if (webSocketClient != null) {
            webSocketClient.send(reqUserId, loginDataAck.toJSONString());
         }
      } else {
         //1. TODO check valid
         //2. return newest userData
         result.setFriend(true); //自己默认就是好友
         result.setAction(0);
         String userInfo = gson.toJson(result, UserEntity.class);
         loginDataAck.put("tn", tn);
         loginDataAck.put("type", type);
         JSONObject dataObject = new JSONObject();
         dataObject.put("resultCode", 0);
         dataObject.put("userInfo", userInfo);
         loginDataAck.put("data", dataObject);

         //将peerId与webSocketClient关联起来
         WebSocketClient webSocketClient = webSocketServer.getClients().get(conn);
         if (webSocketClient != null) {
            webSocketServer.getLoginClients().put(user.getPeerId(), webSocketClient);
            //应答
            webSocketClient.send(reqUserId, loginDataAck.toJSONString());
         }
      }
   }

   private void processMsgTransmit(int reqUserId, int tn, String type, String data, WebSocket conn) {
      Gson gson = new Gson();
      MessageEntity messageEntity = gson.fromJson(data, MessageEntity.class);
      int msgNo = SequenceNumberMaker.getInstance().makeMsgId();
      messageEntity.setMsgId(msgNo);
      MessageEntity.insertOrUpdateSingleData(messageEntity);
      String msg = gson.toJson(messageEntity, MessageEntity.class);
      webSocketServer.getTnMap().put(msgNo, tn);

      int sessionType = messageEntity.getSessionType();
      if(sessionType == DBConstant.SESSION_TYPE_SINGLE) {
         int toId = messageEntity.getToId();
         //TODO check 好友关系

         JSONObject messageObject = new JSONObject();
         messageObject.put("type", type);
         messageObject.put("data", msg);

         WebSocketClient toSocketClient = webSocketServer.getLoginClients().get(toId);
         if(toSocketClient != null) {
            toSocketClient.send(toId, messageObject.toJSONString());
         }
      } else if (sessionType == DBConstant.SESSION_TYPE_GROUP) {
         int toId = messageEntity.getToId();
         GroupEntity group = GroupEntity.getGroupById(toId);
         List<Integer> groupMemberIds = group.getlistGroupMemberIds();
         for (Integer memberId: groupMemberIds) {
            if (reqUserId == memberId) continue;

            JSONObject messageObject = new JSONObject();
            messageObject.put("type", type);
            messageObject.put("data", msg);
            WebSocketClient toSocketClient = webSocketServer.getLoginClients().get(memberId);
            if(toSocketClient != null) {
               toSocketClient.send(memberId, messageObject.toJSONString());
            }
         }

         // 群消息发送成功确认
         Integer sendTn = webSocketServer.getTnMap().remove(msgNo);
         JSONObject msgDataAck = new JSONObject();
         msgDataAck.put("tn", sendTn);
         msgDataAck.put("type", IMBaseDefine.MsgAck);
         JSONObject rsp = new JSONObject();
         rsp.put("resultCode", 0);
         rsp.put("msgId", msgNo);
         msgDataAck.put("data", rsp);
         WebSocketClient sendClient = webSocketServer.getLoginClients().get(reqUserId);
         if(sendClient != null) {
            sendClient.send(reqUserId, msgDataAck.toJSONString());
         }
      }
   }

   private void processMsgAsk(int reqUserId, int tn, String type, String data, WebSocket conn) {
      // 单聊消息发送成功确认
      JSONObject result = JSONObject.parseObject(data);
      int fromId = result.getIntValue("fromId");
      int msgNo = result.getIntValue("msgId");

      Integer sendTn = webSocketServer.getTnMap().remove(msgNo);
      JSONObject msgDataAck = new JSONObject();
      msgDataAck.put("tn", sendTn);
      msgDataAck.put("type", type);
      JSONObject rsp = new JSONObject();
      rsp.put("resultCode", 0);
      rsp.put("msgId", msgNo);
      msgDataAck.put("data", rsp);

      WebSocketClient sendClient = webSocketServer.getLoginClients().get(fromId);
      if(sendClient != null) {
         sendClient.send(reqUserId, msgDataAck.toJSONString());
      }
   }

   private void processGetAllUserList(int reqUserId, int tn, String type, String data, WebSocket conn) {
      WebSocketClient webSocketClient = webSocketServer.getClients().get(conn);
      if (webSocketClient == null) return;

      JSONArray userListJsonArray = new JSONArray();
      Set<Integer> friendIds = ExtraEntry.getFriendIdsByPeerId(reqUserId);
      List<UserEntity> allContacts = UserEntity.getByPeerIds(friendIds);
      for (UserEntity userEntity: allContacts) {
         userEntity.setFriend(true);
         userEntity.setAction(0);

         Gson gson = new Gson();
         String userStr = gson.toJson(userEntity, UserEntity.class);
         JSONObject userJsonObject = JSONObject.parseObject(userStr);
         userListJsonArray.add(userJsonObject);
      }

      JSONObject messageObject = new JSONObject();
      messageObject.put("tn", tn);
      messageObject.put("type", type);
      JSONObject dataObject = new JSONObject();
      messageObject.put("data", dataObject);
      dataObject.put("resultCode", 0);
      dataObject.put("userList", userListJsonArray);
      webSocketClient.send(reqUserId, messageObject.toJSONString());
   }

   private void processGetBuddyListUserInfo(int reqUserId, int tn,  String type, String data, WebSocket conn) {
      WebSocketClient webSocketClient = webSocketServer.getClients().get(conn);
      if (webSocketClient == null) return;

      JSONObject param = JSONObject.parseObject(data);
      List<Integer> userIdList = param.getObject("UserIdList", List.class);
      List<UserEntity> entityList = UserEntity.getByPeerIds(userIdList);

      JSONArray userInfoListJSONArray = new JSONArray();
      for (UserEntity userEntity: entityList) {
         Gson gson = new Gson();
         String userStr = gson.toJson(userEntity, UserEntity.class);
         JSONObject userJsonObject = JSONObject.parseObject(userStr);
         userInfoListJSONArray.add(userJsonObject);
      }

      JSONObject messageObject = new JSONObject();
      messageObject.put("tn", tn);
      messageObject.put("type", type);
      JSONObject dataObject = new JSONObject();
      messageObject.put("data", dataObject);
      dataObject.put("resultCode", 0);
      dataObject.put("UserId", reqUserId);
      dataObject.put("UserInfoList", userInfoListJSONArray);
      webSocketClient.send(reqUserId, messageObject.toJSONString());
   }

   private void processSearchBuddyInfo(int reqUserId, int tn,  String type, String data, WebSocket conn) {
      WebSocketClient webSocketClient = webSocketServer.getClients().get(conn);
      if (webSocketClient == null) return;

      JSONObject param = JSONObject.parseObject(data);
      int userId = param.getIntValue("UserId");
      String searchInfo = param.getString("SearchInfo");
      UserEntity userEntity = UserEntity.searchByUserCodeOrPhone(searchInfo);
      Gson gson = new Gson();
      String userStr = gson.toJson(userEntity, UserEntity.class);
      JSONObject userJsonObject = JSONObject.parseObject(userStr);

      JSONObject messageObject = new JSONObject();
      messageObject.put("tn", tn);
      messageObject.put("type", type);
      JSONObject dataObject = new JSONObject();
      messageObject.put("data", dataObject);
      dataObject.put("resultCode", 0);
      dataObject.put("UserId", reqUserId);
      dataObject.put("UserInfo", userJsonObject);
      webSocketClient.send(reqUserId, messageObject.toJSONString());
   }

   private void processReqAddBuddy(int reqUserId, int tn,  String type, String data, WebSocket conn) {
      JSONObject reqAddBuddy = JSONObject.parseObject(data);
      int userId = reqAddBuddy.getIntValue("UserId");
      int addUserId = reqAddBuddy.getIntValue("AddUserId");
      String addReqInfo = reqAddBuddy.getString("AddReqInfo");
      String userName = reqAddBuddy.getString("UserName");

      //notify被加好友的人
      JSONObject messageObject = new JSONObject();
      messageObject.put("type", IMBaseDefine.onNotifyAddBuddy);
      messageObject.put("data", reqAddBuddy);
      WebSocketClient toSocketClient = webSocketServer.getLoginClients().get(addUserId);
      if(toSocketClient != null) {
         toSocketClient.send(addUserId, messageObject.toJSONString());
      }

      //回复发送方, 也是请求方
      messageObject = new JSONObject();
      messageObject.put("tn", tn);
      messageObject.put("type", type);
      JSONObject dataObject = new JSONObject();
      messageObject.put("data", dataObject);
      dataObject.put("resultCode", 0);
      dataObject.put("UserId", reqUserId);
      dataObject.put("AddUserId", addUserId);
      dataObject.put("AddReqInfo", addReqInfo);
      WebSocketClient webSocketClient = webSocketServer.getLoginClients().get(reqUserId);
      if (webSocketClient != null) {
         webSocketClient.send(reqUserId, messageObject.toJSONString());
      }
   }

   private void processConfirmAddBuddy(int reqUserId, int tn,  String type, String data, WebSocket conn) {
      JSONObject reqAddBuddy = JSONObject.parseObject(data);
      int userId = reqAddBuddy.getIntValue("UserId");
      int confirmUserId = reqAddBuddy.getIntValue("ConfirmUserId");
      String confirmRsqInfo = reqAddBuddy.getString("ConfirmRsqInfo");


      //更新好友关系
      List<ExtraEntry> updateList = new ArrayList<>();

      ExtraEntry userExtraEntry = ExtraEntry.getExtraEntryPeerId(userId);
      if (userExtraEntry == null) {
         userExtraEntry = new ExtraEntry();
         userExtraEntry.setPeerId(userId);
      }
      userExtraEntry.addFriend(confirmUserId);
      updateList.add(userExtraEntry);

      ExtraEntry confirmUserExtraEntry = ExtraEntry.getExtraEntryPeerId(confirmUserId);
      if (confirmUserExtraEntry == null) {
         confirmUserExtraEntry = new ExtraEntry();
         confirmUserExtraEntry.setPeerId(confirmUserId);
      }
      confirmUserExtraEntry.addFriend(userId);
      updateList.add(confirmUserExtraEntry);

      ExtraEntry.insertOrUpdateMultiData(updateList);

      //notify加好友的人
      JSONObject messageObject = new JSONObject();
      messageObject.put("type", IMBaseDefine.onNotifyAddBuddyAccept);
      messageObject.put("data", reqAddBuddy);
      WebSocketClient toSocketClient = webSocketServer.getLoginClients().get(confirmUserId);
      if(toSocketClient != null) {
         toSocketClient.send(confirmUserId, messageObject.toJSONString());
      }

      //回复发送方, 也是同意方
      messageObject = new JSONObject();
      messageObject.put("tn", tn);
      messageObject.put("type", type);
      JSONObject dataObject = new JSONObject();
      messageObject.put("data", dataObject);
      dataObject.put("resultCode", 0);
      dataObject.put("UserId", reqUserId);
      dataObject.put("ConfirmUserId", confirmUserId);
      dataObject.put("ConfirmRsqInfo", confirmRsqInfo);
      WebSocketClient webSocketClient = webSocketServer.getLoginClients().get(userId);
      if (webSocketClient != null) {
         webSocketClient.send(userId, messageObject.toJSONString());
      }
   }

   private void processGetGroupDetailInfo(int reqUserId, int tn,  String type, String data, WebSocket conn) {
      WebSocketClient webSocketClient = webSocketServer.getClients().get(conn);
      if (webSocketClient == null) return;

      JSONObject param = JSONObject.parseObject(data);
      int groupId = param.getIntValue("GroupId");
      int version = param.getIntValue("Version");

      GroupEntity group = GroupEntity.getGroupById(groupId);
      JSONObject groupInfoListRsp = new JSONObject();
      groupInfoListRsp.put("resultCode", 0);
      groupInfoListRsp.put("UserId", reqUserId);
      groupInfoListRsp.put("GroupId", groupId);

      if (group.getVersion() > version) {
         groupInfoListRsp.put("GroupSize", group.getUserCnt());
         List<GroupMemberEntity> groupMembers = GroupMemberEntity.loadAllGroupMembersByGroupId(group.getPeerId());
         group.setGroupMemberList(groupMembers);
         Gson gson = new Gson();
         String groupStr = gson.toJson(group, GroupEntity.class);
         JSONObject groupJsonObject = JSONObject.parseObject(groupStr);
         JSONArray groupInfoList = new JSONArray();
         groupInfoList.add(groupJsonObject);
         groupInfoListRsp.put("GroupInfoList", groupInfoList);
      }

      JSONObject messageObject = new JSONObject();
      messageObject.put("tn", tn);
      messageObject.put("type", type);
      JSONObject dataObject = new JSONObject();
      messageObject.put("data", dataObject);
      dataObject.put("resultCode", 0);
      dataObject.put("UserId", reqUserId);
      dataObject.put("GroupInfoListRsp", groupInfoListRsp);
      webSocketClient.send(reqUserId, messageObject.toJSONString());
   }

   private void processGetAllNormalGroup(int reqUserId, int tn, String type, String data, WebSocket conn) {
      WebSocketClient webSocketClient = webSocketServer.getClients().get(conn);
      if (webSocketClient == null) return;


      JSONArray groupListJsonArray = new JSONArray();
      List<GroupEntity> groupEntities = GroupEntity.loadAllGroupNormal();
      if (groupEntities != null) {
         for (GroupEntity groupEntity : groupEntities) {
            List<Integer> groupMemberIds = groupEntity.getlistGroupMemberIds();
            if (!groupMemberIds.contains(reqUserId)) continue;

            List<GroupMemberEntity> groupMembers = GroupMemberEntity.loadAllGroupMembersByGroupId(groupEntity.getPeerId());
            groupEntity.setGroupMemberList(groupMembers);
            Gson gson = new Gson();
            String groupStr = gson.toJson(groupEntity, GroupEntity.class);
            JSONObject groupJsonObject = JSONObject.parseObject(groupStr);
            groupListJsonArray.add(groupJsonObject);
         }
      }

      JSONObject messageObject = new JSONObject();
      messageObject.put("tn", tn);
      messageObject.put("type", type);
      JSONObject dataObject = new JSONObject();
      messageObject.put("data", dataObject);
      dataObject.put("resultCode", 0);
      dataObject.put("groupList", groupListJsonArray);
      webSocketClient.send(reqUserId, messageObject.toJSONString());
   }

   private void processGroupChangeMember(int reqUserId, int tn, String type, String data, WebSocket conn) {
      WebSocketClient webSocketClient = webSocketServer.getClients().get(conn);
      if (webSocketClient == null) return;

      JSONObject param = JSONObject.parseObject(data);
      int userId = param.getIntValue("UserId");
      int groupId = param.getIntValue("GroupId");
      int changeType = param.getIntValue("ChangeType");
      List<Integer> changeMemberIdList = param.getObject("ChangeMemberList", List.class);

      List<Integer> chgUserIdList = new ArrayList<>();
      GroupEntity group = GroupEntity.getGroupById(groupId);
      group.setGroupMemberList(GroupMemberEntity.loadAllGroupMembersByGroupId(groupId));
      switch (changeType) {
         case DBConstant.GROUP_MODIFY_TYPE_ADD: {
            List<UserEntity> entityList = UserEntity.getByPeerIds(changeMemberIdList);
            List<GroupMemberEntity> changeMemberList = new ArrayList<>();
            List<Integer> memberIds = group.getlistGroupMemberIds();
            for (UserEntity userEntity: entityList) {
               GroupMemberEntity member = new GroupMemberEntity();
               member.setPeerId(userEntity.getPeerId());
               member.setMainName(userEntity.getMainName());
               member.setGroupId(groupId);
               member.setgKey(groupId + "_" + member.getPeerId());
               member.setStatus(0);
               member.setUpdated(System.currentTimeMillis());
               changeMemberList.add(member);
               chgUserIdList.add(member.getPeerId());
            }
            group.setUserCnt(group.getUserCnt() + changeMemberList.size());
            memberIds.addAll(chgUserIdList);
            group.setlistGroupMemberIds(memberIds);
            group.addGroupMemberList(changeMemberList);
            group.setVersion(group.getVersion() + 1);
            GroupEntity.insertOrUpdateSingleData(group);
            GroupMemberEntity.insertOrUpdateMultiData(changeMemberList);
         } break;
         case DBConstant.GROUP_MODIFY_TYPE_DEL: {
            List<Integer> memberIds = group.getlistGroupMemberIds();
            List<GroupMemberEntity> delGroupMemberList = group.delGroupMemberList(groupId, changeMemberIdList);
            group.setUserCnt(group.getUserCnt() - delGroupMemberList.size());
            for (GroupMemberEntity member: delGroupMemberList) {
               chgUserIdList.add(member.getPeerId());
            }
            memberIds.removeAll(chgUserIdList);
            group.setlistGroupMemberIds(memberIds);
            group.setVersion(group.getVersion() + 1);
            GroupEntity.insertOrUpdateSingleData(group);
            GroupMemberEntity.deleteGroupMem(groupId, chgUserIdList);
         } break;
         case DBConstant.GROUP_MODIFY_TYPE_LEAVE: {
            List<Integer> memberIds = group.getlistGroupMemberIds();
            List<GroupMemberEntity> delGroupMemberList = group.delGroupMemberList(groupId, changeMemberIdList);
            group.setUserCnt(group.getUserCnt() - delGroupMemberList.size());
            for (GroupMemberEntity member: delGroupMemberList) {
               chgUserIdList.add(member.getPeerId());
            }
            memberIds.removeAll(chgUserIdList);
            group.setlistGroupMemberIds(memberIds);
            group.setVersion(group.getVersion() + 1);
            GroupEntity.insertOrUpdateSingleData(group);
            GroupMemberEntity.deleteGroupMem(groupId, chgUserIdList);
         } break;
      }

      JSONObject groupChangeMemberRsp = new JSONObject();
      groupChangeMemberRsp.put("resultCode", 0);
      groupChangeMemberRsp.put("UserId", reqUserId);
      groupChangeMemberRsp.put("GroupId", groupId);
      groupChangeMemberRsp.put("ChangeType", changeType);
      groupChangeMemberRsp.put("ChgUserIdList", chgUserIdList);
      groupChangeMemberRsp.put("CurUserIdList", group.getlistGroupMemberIds());

      JSONObject messageObject = new JSONObject();
      messageObject.put("tn", tn);
      messageObject.put("type", type);
      JSONObject dataObject = new JSONObject();
      messageObject.put("data", dataObject);
      dataObject.put("resultCode", 0);
      dataObject.put("UserId", reqUserId);
      dataObject.put("groupChangeMemberRsp", groupChangeMemberRsp);
      webSocketClient.send(reqUserId, messageObject.toJSONString());

      // notify 其他群成员
      List<Integer> notifyroupMemberIds = group.getlistGroupMemberIds();
      if (changeType == DBConstant.GROUP_MODIFY_TYPE_DEL || changeType == DBConstant.GROUP_MODIFY_TYPE_LEAVE) { //add时，getlistGroupMemberIds即为全部要通知的集合；但del或leave时，getlistGroupMemberIds应再加上chgUserIdList
         notifyroupMemberIds.addAll(chgUserIdList);
      }
      for (Integer memberId: notifyroupMemberIds) {
         if (reqUserId == memberId) continue;

         JSONObject notifyObject = new JSONObject();
         notifyObject.put("type", IMBaseDefine.GroupChangeMemberNotify);
         JSONObject dataJSONObject = new JSONObject();
         notifyObject.put("data", dataJSONObject);
         dataJSONObject.put("OperatorId", reqUserId);
         dataJSONObject.put("GroupId", groupId);
         dataJSONObject.put("ChangeType", changeType);
         dataJSONObject.put("ChgUserIdList", chgUserIdList);
         dataJSONObject.put("CurUserIdList", group.getlistGroupMemberIds());

         WebSocketClient toSocketClient = webSocketServer.getLoginClients().get(memberId);
         if(toSocketClient != null) {
            toSocketClient.send(memberId, notifyObject.toJSONString());
         }
      }
   }

   private void processReqCreateGroup(int reqUserId, int tn, String type, String data, WebSocket conn) {
      WebSocketClient webSocketClient = webSocketServer.getClients().get(conn);
      if (webSocketClient == null) return;

      JSONObject param = JSONObject.parseObject(data);
      int userId = param.getIntValue("UserId");
      String groupName = param.getString("GroupName");
      int groupType = param.getIntValue("GroupType");

      String groupAvatar =  param.getString("GroupAvatar");
      List<Integer> allMemberIdList = param.getObject("AllMemberIdList", List.class);
      List<UserEntity> userEntityList = UserEntity.getByPeerIds(allMemberIdList);
      long now = System.currentTimeMillis();
      GroupEntity group = new GroupEntity();
      group.setPeerId(SequenceNumberMaker.getInstance().makGroupId());
      group.setCreatorId(userId);
      group.setGroupType(groupType);
      group.setMainName(groupName);
      group.setCreated(now);
      group.setAvatar(groupAvatar);
      group.setlistGroupMemberIds(allMemberIdList);
      group.setVersion(1);

      int groupId = group.getPeerId();
      List<GroupMemberEntity> memberEntityList = new ArrayList<>();
      for (UserEntity userEntity: userEntityList) {
         GroupMemberEntity member = new GroupMemberEntity();
         member.setPeerId(userEntity.getPeerId());
         member.setMainName(userEntity.getMainName());
         member.setNickName(userEntity.getNickName());
         member.setGroupId(groupId);
         member.setgKey(groupId + "_" + member.getPeerId());
         member.setStatus(0);
         member.setCreated(now);
         member.setUpdated(now);
         memberEntityList.add(member);
      }
      group.setUserCnt(memberEntityList.size());
      group.setGroupMemberList(memberEntityList);
      GroupEntity.insertOrUpdateSingleData(group);
      GroupMemberEntity.insertOrUpdateMultiData(memberEntityList);


      Gson gson = new Gson();
      String groupStr = gson.toJson(group, GroupEntity.class);
      JSONObject groupJsonObject = JSONObject.parseObject(groupStr);

      JSONObject groupCreateRsp = new JSONObject();
      groupCreateRsp.put("resultCode", 0);
      groupCreateRsp.put("UserId", reqUserId);
      groupCreateRsp.put("GroupId", groupId);
      groupCreateRsp.put("GroupInfo", groupJsonObject);

      JSONObject messageObject = new JSONObject();
      messageObject.put("tn", tn);
      messageObject.put("type", type);
      JSONObject dataObject = new JSONObject();
      messageObject.put("data", dataObject);
      dataObject.put("resultCode", 0);
      dataObject.put("UserId", reqUserId);
      dataObject.put("groupCreateRsp", groupCreateRsp);
      webSocketClient.send(reqUserId, messageObject.toJSONString());

      // notify 其他群成员
      List<Integer> notifyroupMemberIds = group.getlistGroupMemberIds();
      for (Integer memberId: notifyroupMemberIds) {
         if (reqUserId == memberId) continue;

         JSONObject notifyObject = new JSONObject();
         notifyObject.put("type", IMBaseDefine.onNotifyCreateGroup);
         JSONObject dataJSONObject = new JSONObject();
         notifyObject.put("data", dataJSONObject);
         dataJSONObject.put("OperatorId", reqUserId);
         dataJSONObject.put("GroupId", groupId);

         WebSocketClient toSocketClient = webSocketServer.getLoginClients().get(memberId);
         if(toSocketClient != null) {
            toSocketClient.send(memberId, notifyObject.toJSONString());
         }
      }
   }


}
