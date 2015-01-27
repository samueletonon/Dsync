package org.samux.samu.dsync;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends ActionBarActivity {
    public static final String PREF = "org.samux.samu.dsync.preffile";
    public static final String APPLICATION_NAME = "Dsync";
    private static final String TAG = "Mact";
    static final int GET_DRIVE_ACCOUNT = 1;

    private List<ItemFile> fileList = new ArrayList<>();
    public int processed, total;
    private int started=0;
    private String localpath;
    private String aName;
    private String driveId;
    public static Drive service;
    List<com.google.api.services.drive.model.File> GFile;
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
            mainAction();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case GET_DRIVE_ACCOUNT:
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "ok");
                    mainAction();
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
        mainAction();
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
        SharedPreferences Pref = getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
        localpath = Pref.getString("localfolder", null);
        aName = Pref.getString("account", null);
        driveId = Pref.getString("driveid", null);
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
            t = getDrive(driveId, localpath);
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.v(TAG,"main routine");
            //Main Routine to download file
            for (ItemFile sf : fileList) {
                File lf = new File(sf.file);
                if (lf.exists() && !lf.isDirectory()) {
                    String drivemd5 = sf.dFile.getMd5Checksum();
                    String lmd5 = fileToMD5(sf.file);
                    if (lmd5.equals(drivemd5)) {
                        Log.v(TAG, "same");
                        return;
                    }
                }
                DDFile md = new DDFile();
                md.DownloadFile(service, sf.dFile, lf);
                md.execute();
            }

        } else {
            ( (Button) this.findViewById(R.id.actionbutton)).setText(getString(R.string.start));
        }
    }

    private Thread getDrive(final String iddrive,final String lpath){
        Thread lt = new Thread(new Runnable() {
            @Override
            public void run() {
                GFile = retrieveAllFiles(iddrive);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (com.google.api.services.drive.model.File f: GFile){
                            Log.v(TAG,"elaborating");
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
                                Thread xt = getDrive(f.getId(),tpath);
                            } else {
                                fileList.add(total, new ItemFile(lpath + "/" + f.getTitle(), 0, true, f.getId(),f));
                                total++;
                                String text = processed + "/" + total + "  " + getString(R.string.processedV);
                                ( (TextView) findViewById(R.id.processedText)).setText(text);
                            }
                        }
                    }
                });
            }
        });
        lt.start();
        return lt;

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

    public class DDFile extends AsyncTask<Void, Integer, Boolean> {

        private Drive service;
        private com.google.api.services.drive.model.File driveFile;
        private java.io.File file;

        public void DownloadFile(Drive dservice, com.google.api.services.drive.model.File driveFile, java.io.File lfile) {
            this.driveFile = driveFile;
            this.file = lfile;
            this.service =  dservice;
        }

        @Override
        protected void onPreExecute() {
            //textView.setText("Hello !!!");
            //mProgress = (ProgressBar) findViewById(R.id.progressBar);
            mProgress.setProgress(0);
            mProgress.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (driveFile.getDownloadUrl() != null
                    && driveFile.getDownloadUrl().length() > 0) {
                try {
                    Log.v(TAG,"Go");
                    HttpResponse resp = service.getRequestFactory()
                            .buildGetRequest(new GenericUrl(driveFile.getDownloadUrl()))
                            .execute();
                    OutputStream os = new FileOutputStream(file);
                    CopyStream(driveFile.size(),resp.getContent(), os);
                    os.close();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            } else {
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            mProgress.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            //use the file
            Log.v(TAG,"done");
            mProgress.setVisibility(View.INVISIBLE);
            processed++;
            String text = processed + "/" + total + getString(R.string.processedV);
            ( (TextView) findViewById(R.id.processedText)).setText(text);
        }

        public void CopyStream(int size, InputStream is, OutputStream os) {
            final int buffer_size = 1024;
            try {
                byte[] bytes = new byte[buffer_size];
                for (int count=0,prog=0;count!=-1;) {
                    count = is.read(bytes, 0, buffer_size);
                    os.write(bytes, 0, count);
                    prog=prog+count;
                    publishProgress(prog*100/size);
                }
            } catch (Exception ex) {
                Log.e(TAG,"CS "+ex);
            }
        }
    }



}
