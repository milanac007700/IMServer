
package com.milanac007.demo.im.db.helper;

import com.milanac007.demo.im.db.entity.GroupEntity;
import com.milanac007.demo.im.db.entity.MessageEntity;
import com.milanac007.demo.im.db.entity.UserEntity;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 1.专门用来分配序列号
 * 2. 本地消息的唯一msgId键值
 * todo can use AtomicInteger
 */
public class SequenceNumberMaker {

    private volatile AtomicInteger mMsgIdSquence = new AtomicInteger();
    private volatile AtomicInteger mUserIdSquence = new AtomicInteger(); // 10001
    private volatile AtomicInteger mGroupIdSquence = new AtomicInteger(); // 20001

    private static SequenceNumberMaker maker = new SequenceNumberMaker();

    private SequenceNumberMaker() {
        int msgId = MessageEntity.getLastMsgId();
        int lastUserId = UserEntity.getLastUserId();
        int lastGroupId = GroupEntity.getLastGroupId();
        if(msgId < 0) {
            throw new RuntimeException("getLastMsgId can't < 0.");
        }

        if(lastUserId < 10000) {
            throw new RuntimeException("lastUserId can't < 10001.");
        }

        if(lastGroupId < 20000) {
            throw new RuntimeException("lastGroupId can't < 20001.");
        }

        mMsgIdSquence.set(msgId);
        mUserIdSquence.set(lastUserId);
        mGroupIdSquence.set(lastGroupId);
    }

    public static SequenceNumberMaker getInstance() {
        return maker;
    }

    public int makeMsgId() {
        if(mMsgIdSquence.intValue() >= Integer.MAX_VALUE) {
            mMsgIdSquence.set(0);
        }
        return mMsgIdSquence.addAndGet(1);
    }

    public int makeUserId() {
        if(mUserIdSquence.intValue() >= Integer.MAX_VALUE) {
            mUserIdSquence.set(10000);
        }
        return mUserIdSquence.addAndGet(1);
    }

    public int makGroupId() {
        if(mGroupIdSquence.intValue() >= Integer.MAX_VALUE) {
            mGroupIdSquence.set(20000);
        }
        return mGroupIdSquence.addAndGet(1);
    }
}
