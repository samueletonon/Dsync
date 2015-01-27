package org.samux.samu.dsync;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import com.google.common.io.Files;





public class MainActivity extends ActionBarActivity {
    public static final String PREF = "org.samux.samu.dsync.preffile";
    public static final String APPLICATION_NAME = "Dsync";
    private static final String TAG = "Mact";
    static final int GET_DRIVE_ACCOUNT = 1;

    public int processed, total;
    private int started=0;
    private String localpath;
    private String aName;
    private String driveId;
    public static Drive service;
    List<com.google.api.services.drive.model.File> lFile;
    private Thread t=null;
    private ProgressBar mProgress;
    InputStream dFile;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProgress = (ProgressBar) findViewById(R.id.progressBar);
        mProgress.setIndeterminate(false);
        mProgress.setMax(100);


        SharedPreferences Pref = getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
        if (! Pref.contains("localfolder")) {
            launch_setup();
        } else {
            main_routine();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case GET_DRIVE_ACCOUNT:
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "ok");
                    main_routine();
                } else {
                    Log.d(TAG, "ahio "+ resultCode);
                    launch_setup();
                }
                break;
        }
    }

    private void launch_setup() {
        //launch the wizard
        Intent intent = new Intent(this, gDrive1.class);
        startActivityForResult(intent, GET_DRIVE_ACCOUNT);
    }

    private List<com.google.api.services.drive.model.File> retrieveAllFiles(String ddir) {
        List<com.google.api.services.drive.model.File> result = new ArrayList<>();
        Drive.Files.List request;
        try {
            String query;
            query = "trashed=false and '" + ddir +  "' in parents";
            request = service.files().list().setQ(query);
        } catch (IOException e) {
            return result;
        }
        do {
            try {
                FileList files = request.execute();
                result.addAll(files.getItems());
                request.setPageToken(files.getNextPageToken());
            } catch (IOException e) {
                Log.e(TAG, "", e);
                request.setPageToken(null);
            }
        } while (request.getPageToken() != null && request.getPageToken().length() > 0);
        return result;
    }


    private void main_routine() {
        SharedPreferences Pref = getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
        localpath = Pref.getString("localfolder", null);
        aName = Pref.getString("account", null);
        driveId = Pref.getString("driveid", null);
    }

    public void onStopStart(View view){
        if(started == 1) {
            started = 0;
            if(t!=null)
                t=null;
        } else {
            started = 1;
        }
        mainAction();
    }
    public void edit_config(View view){
        launch_setup();
    }

    private void mainAction(){
        if(started == 1) {
            processed=0;
            total=0;
            mProgress.setProgress(0);
            ( (TextView) findViewById(R.id.processedText)).setText(R.string.processed);

            ( (Button) this.findViewById(R.id.actionbutton)).setText(getString(R.string.stop));

            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(this, Arrays.asList(DriveScopes.DRIVE));
            credential.setSelectedAccountName(aName);
            service = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
                    .setApplicationName(APPLICATION_NAME).build();
            Log.v(TAG, "" + driveId);
            getDrive(driveId, localpath);
        } else {
            ( (Button) this.findViewById(R.id.actionbutton)).setText(getString(R.string.start));
        }
    }

    private void getDrive(final String iddrive,final String lpath){
        t = new Thread(new Runnable() {
            @Override
            public void run() {
                lFile = retrieveAllFiles(iddrive);
                total = total + lFile.size();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (com.google.api.services.drive.model.File f: lFile){
                            if (f.getMimeType().matches("application/vnd.google-apps.folder")){
                                String tpath = lpath + "/" + f.getTitle();
                                File theDir = new File(tpath);
                                if (!theDir.exists()) {
                                    try {
                                        theDir.mkdir();
                                    } catch (SecurityException se) {
                                        Log.e(TAG, "");
                                    }
                                }
                                getDrive(f.getId(),tpath);
                            } else {
                                // it's a file, let's check
                                checkfile(lpath,f);
                                processed++;
                                String text = processed + "/" + total + getString(R.string.processedV);
                                ( (TextView) findViewById(R.id.processedText)).setText(text);
                                mProgress.setProgress(100 * processed / total);
                            }
                        }
                    }
                });
            }
        });
        t.start();
    }

    private void getdrivedataThread(final String durl,final File ff) {
        Thread dt = new Thread(new Runnable() {
            @Override
            public void run() {
                dFile = getdrivedata(durl);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            OutputStream output = new FileOutputStream(ff);
                            try {
                                int read = 0;
                                byte[] bytes = new byte[1024];
                                while ((read = dFile.read(bytes)) != -1) {
                                    output.write(bytes, 0, read);
                                }
                                Log.v(TAG,"wrote "+ff);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                if (dFile != null) {
                                    try {
                                        dFile.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (output != null) {
                                    try {
                                        output.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                }
                            }
                        } catch (FileNotFoundException e){
                            Log.e(TAG,""+e);
                        }
                    }
                });
            }
        });
        dt.start();
    }

    private void checkfile(String localPath, com.google.api.services.drive.model.File fdrive){
        String filePathString = localPath + "/" + fdrive.getTitle();
        File f = new File(filePathString);
        if(f.exists() && !f.isDirectory()) {
            String drivemd5 = fdrive.getMd5Checksum();
            String lmd5 = fileToMD5(filePathString);
            if (lmd5.equals(drivemd5)) {
                Log.v(TAG, "same");
                return;
            }
        }
        getdrivedataThread(fdrive.getDownloadUrl(),f);
    }


    private InputStream getdrivedata(String url){
        if (url != null && url.length() > 0) {
            try {
                HttpResponse resp = service.getRequestFactory().buildGetRequest(new GenericUrl(url)).execute();
                return resp.getContent();
            } catch (IOException e) {
                // An error occurred.
                Log.e(TAG,"gdd: " +e);
                //e.printStackTrace();
                return null;
            }
        } else {
            // The file doesn't have any content stored on Drive.
            Log.v(TAG,"Nothing here");
            return null;
        }
    }

    public static String fileToMD5(String filePath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            MessageDigest digest = MessageDigest.getInstance("MD5");
            int numRead = 0;
            while (numRead != -1) {
                numRead = inputStream.read(buffer);
                if (numRead > 0)
                    digest.update(buffer, 0, numRead);
            }
            byte [] md5Bytes = digest.digest();
            return convertHashToString(md5Bytes);
        } catch (Exception e) {
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    Log.e(TAG,"Error");
                }
            }
        }
    }

    private static String convertHashToString(byte[] md5Bytes) {
        String returnVal = "";
        for (int i = 0; i < md5Bytes.length; i++) {
            returnVal += Integer.toString(( md5Bytes[i] & 0xff ) + 0x100, 16).substring(1);
        }
        return returnVal.toLowerCase();
    }

}
