package com.milanac007.demo.fileserver;

import android.content.Context;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.milanac007.demo.util.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import fi.iki.elonen.NanoHTTPD;

public class HttpService2 extends NanoHTTPD {

    public static final String TAG = HttpService2.class.getSimpleName();
    String rootPath = "";
    private Context mContext;

    public HttpService2(int port) {
        super(port);
    }

    public HttpService2(String hostname, int port) {
        super(hostname, port);
    }

    public HttpService2(Context context, String hostname, int port, String path) {
        super(hostname, port);
        rootPath = path;
        mContext = context;
    }

    //重写Serve方法，每次请求时会调用该方法
    @Override
    public Response serve(IHTTPSession session) {
        //通过session获取请求的方式和类型
        Method method = session.getMethod();
        // 判断post请求并且是上传文件
        //将上传数据解析到files集合并且存在NanoHTTPD缓存区
        Map<String, String> files = new HashMap<>();
        if (Method.POST.equals(method) || Method.PUT.equals(method)) {
            try {
                session.parseBody(files);
            } catch (IOException ioe) {
//                return getResponse("Internal Error IO Exception: " + ioe.getMessage());
                return getResponse(false,null, "Internal Error IO Exception: " + ioe.getMessage());
            } catch (ResponseException re) {
                return NanoHTTPD.newFixedLengthResponse(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
            }
        }
        //after the body parsed, by default nanoHTTPD will save the file
        // to cache and put it into params

        //2.将解析出来的文件根据需要保存在本地（或者上传服务器）
        Map<String, String> params = session.getParms();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            final String paramsKey = entry.getKey();
            //"file"是上传文件参数的key值
            if (paramsKey.contains("file")) {
                final String tmpFilePath = files.get(paramsKey);
                //可以直接拿上传的文件名保存，也可以解析一下然后自己命名保存
                final String fileName = entry.getValue();
                final File tmpFile = new File(tmpFilePath);
                //targetFile是你要保存的file，这里是保存在SD卡的根目录（需要获取文件读写权限）
                final File targetFile = new File(rootPath, fileName);

                com.milanac007.demo.im.logger.Logger.getLogger().i("copy file now, source file path: %s<>target file path: %s", tmpFile.getAbsolutePath(), targetFile.getAbsolutePath());

                //a copy file methoed just what you like
//                copyFile(tmpFile, targetFile);
                try {
                    zeroCopyFile(tmpFile, targetFile);
                } catch (IOException e) {
                    e.printStackTrace();
                    return getResponse(false, null, "Internal Error IO Exception: " + e.getMessage());
                }
            }
        }
//                return getResponse("Success");
        return getResponse(true, params.values(), null);
    }

    //页面不存在，或者文件不存在时
    public Response response404(IHTTPSession session, String url) {
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html><html>body>");
        builder.append("404 Not Found" + url + " !");
        builder.append("</body></html>\n");
        return NanoHTTPD.newFixedLengthResponse(builder.toString());
    }

    //成功请求
    public Response getResponse(String success) {
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html><html>body>");
        builder.append(success+ " !");
        builder.append("</body></html>\n");
        return NanoHTTPD.newFixedLengthResponse(builder.toString());
    }

    public Response getResponse(boolean result, Collection<String> fileNames, String message) {
        JSONObject resultObject = new JSONObject();
        if(!result) {
            resultObject.put("status", 404);
            resultObject.put("message", "upload fail: " + message);
        }else {
            resultObject.put("status", 200);
            JSONArray pathList = new JSONArray();

            for(String fileName: fileNames) {
                JSONObject item = new JSONObject();
                item.put("path", String.format("http://%s:%d/%s", Utils.getLocalIpAddress(mContext), 8080, fileName));
                pathList.add(item);
            }

            resultObject.put("pathList", pathList);
        }

//        return NanoHTTPD.newFixedLengthResponse(resultObject.toJSONString());
        return newFixedLengthResponse(resultObject.toJSONString());
    }

    public void zeroCopyFile(File srcfile, File destfile) throws IOException {
        try (FileInputStream in = new FileInputStream(srcfile);
             FileChannel inChannel = in.getChannel();
             FileOutputStream out = new FileOutputStream(destfile);
             FileChannel outChannel = out.getChannel();){
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
    }

    public void copyFile(File file, File targetfile) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        if (!file.exists()) {
            System.err.println("File not exists!");
            return;
        }
        try {
            fis = new FileInputStream(file);
            fos = new FileOutputStream(targetfile);
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
                fos.flush();
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(HttpService2.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HttpService2.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (fis != null) {

                    fis.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(HttpService2.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}


//public class HttpService {}
//extends NanoHTTPD {
//
//    public HttpService(int port) {
//        super(port);
//    }
//
//    //重写Service方法，每次请求时会调用该方法
//    @Override
//    public Response serve(IHTTPSession session) {
////        return super.serve(session);
//        String uri = session.getUri();
//        return NanoHTTPD.newFixedLengthResponse(uri);
//    }
//}
