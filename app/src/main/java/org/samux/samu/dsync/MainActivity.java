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

    public int processed, total;
    private int started=0;
    private String localpath;
    private String aName;
    private String driveId;
    public static Drive service;
    List<ItemFile> GFile;
    private GetDAT gdt=null;
    private ProgressBar mProgress;
    private String procfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProgress = (ProgressBar) findViewById(R.id.progressBar);
        mProgress.setIndeterminate(false);
        mProgress.setMax(100);
        procfile="";

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

    private List<ItemFile> retrieveAllFiles(String ddir,String Lpath) {
        List<ItemFile> Iresult = new ArrayList<>();
        Drive.Files.List request;
        try {
            String query;
            query = "trashed=false and '" + ddir +  "' in parents";
            request = service.files().list().setQ(query);
        } catch (IOException e) {
            return Iresult;
        }
        do {
            try {
                FileList files = request.execute();
                //result.addAll(files.getItems());
                for (com.google.api.services.drive.model.File f: files.getItems()){
                    if (f.getMimeType().matches("application/vnd.google-apps.folder")){
                        Iresult.addAll(retrieveAllFiles(f.getId(),Lpath + "/" + f.getTitle()));
                    }
                    Iresult.add(new ItemFile(Lpath + "/" + f.getTitle(), 0, true, f.getId(),f));
                    total++;
                }
                request.setPageToken(files.getNextPageToken());
            } catch (IOException e) {
                Log.e(TAG, "", e);
                request.setPageToken(null);
            }
        } while (request.getPageToken() != null && request.getPageToken().length() > 0);
        return Iresult;
    }

    public void onStopStart(View view){
        if(started == 1) {
            started = 0;
            if (gdt!=null)
                gdt.cancel(true);
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
            gdt = new GetDAT();
            gdt.fromroot(service, driveId, localpath,0);
            gdt.execute();
            Log.v(TAG,"main routine");
        } else {
            ( (Button) this.findViewById(R.id.actionbutton)).setText(getString(R.string.start));
        }
    }

    public class GetDAT extends AsyncTask<Void,Long,Boolean>{
        private Drive service;
        private String iddrive;
        private String lpath;
        private int localn;

        public void fromroot(Drive service, String driveid, String l_path, int localint){
            this.service = service;
            this.iddrive = driveid;
            this.lpath = l_path;
            this.localn = localint;
        }

        @Override
        protected void onPreExecute() {
            mProgress.setProgress(0);
            mProgress.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            GFile = retrieveAllFiles(iddrive,lpath);
            for (ItemFile sf : GFile) {
                if (sf.dFile.getMimeType().matches("application/vnd.google-apps.folder")) {
                    //String tpath = lpath + "/" + f.getTitle();
                    Log.v(TAG,"new dir:"+ sf.file);
                    File theDir = new File(sf.file);
                    if (!theDir.exists()) {
                        try {
                            theDir.mkdir();
                        } catch (SecurityException se) {
                            Log.e(TAG, "");
                        }
                    }
                } else {
                    //Main Routine to download file
                    File lf = new File(sf.file);
                    if (lf.exists() && !lf.isDirectory()) {
                        String drivemd5 = sf.dFile.getMd5Checksum();
                        String lmd5 = fileToMD5(sf.file);
                        if (lmd5.equals(drivemd5)) {
                            Log.v(TAG, "same");
                        }
                    }
                    if (!isCancelled() && sf.dFile.getDownloadUrl() != null
                            && sf.dFile.getDownloadUrl().length() > 0) {
                        try {
                            Log.v(TAG, "Go:" +sf.file);
                            procfile = "Downloading file: " + sf.dFile.getTitle();
                            publishProgress((long)0);
                            HttpResponse resp = service.getRequestFactory()
                                    .buildGetRequest(new GenericUrl(sf.dFile.getDownloadUrl()))
                                    .execute();
                            OutputStream os = new FileOutputStream(lf);
                            CopyStream(sf.dFile.getFileSize(), resp.getContent(), os);
                            os.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Error");
                        }
                    }
                }
                processed++;
                publishProgress((long)0);
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(localn == 0){
                Log.v(TAG,"all done");
            }
        }
        @Override
        protected void onProgressUpdate(Long... progress) {
            mProgress.setProgress(progress[0].intValue());
            String text = processed + "/" + total + "  " + getString(R.string.processedV);
            ((TextView) findViewById(R.id.processedfileText)).setText(procfile);
            ((TextView) findViewById(R.id.processedText)).setText(text);
        }

        public void CopyStream(long size, InputStream is, OutputStream os) {
            final int buffer_size = 4096;
            try {
                byte[] bytes = new byte[buffer_size];
                for (int count=0,prog=0;count!=-1;) {
                    count = is.read(bytes);
                    os.write(bytes, 0, count);
                    prog=prog+count;
                    publishProgress(((long) prog)*100/size);
                }
                os.flush();
                is.close();
                os.close();
            } catch (Exception ex) {
                Log.e(TAG,"CS "+ex);
            }
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
