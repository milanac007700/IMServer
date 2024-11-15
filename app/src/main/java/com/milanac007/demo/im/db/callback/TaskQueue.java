package com.milanac007.demo.im.db.callback;

import com.milanac007.demo.im.logger.Logger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TaskQueue {
    private static final String TAG = "TaskQueue";
    private Logger logger = Logger.getLogger();
    private static TaskQueue mInstance = new TaskQueue();
    public static TaskQueue getInstance() {
        return mInstance;
    }
    private volatile boolean stopFlag = false;

    //task队列
    private BlockingQueue<Task> taskQueue = new ArrayBlockingQueue<>(10);

    public void onStart() {
        logger.d("TaskQueue#onStart run");
        stopFlag = false;
        run();
    }

    public void onDestory() {
        logger.d("TaskQueue#onDestory ");
        taskQueue.clear();
        stopFlag = true;
    }

    private void run() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!stopFlag) {
                    try {
                        Task take = taskQueue.take();
                        take.exec();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }


    public void push(Task task) throws InterruptedException {
        taskQueue.put(task);
    }

}
