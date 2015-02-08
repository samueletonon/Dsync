package org.samux.samu.dsync;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by samu on 08/02/15.
 */

public class GetDAT extends AsyncTask<Void,Long,Boolean> {
    private static final String TAG = "GDAT";
    private Activity activity;
    private Drive service;
    private String iddrive;
    private String lpath;
    private int localn;
    List<ItemFile> GFile;
    private String procfile;
    private NonUIFragment parent;



    public GetDAT(Activity activity, NonUIFragment parent){
        this.activity = activity;
        this.parent = parent;
    }

    @Override
    protected void onPreExecute() {
        parent.mProgress.setProgress(0);
        parent.mProgress.setVisibility(View.VISIBLE);
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        GFile = retrieveAllFiles(iddrive,lpath);
        for (ItemFile sf : GFile) {
            if (this.isCancelled())
                break;
            if (sf.dFile.getMimeType().matches("application/vnd.google-apps.folder")) {
                File theDir = new File(sf.file);
                if (!theDir.exists()) {
                    try {
                        boolean v = theDir.mkdir();
                    } catch (SecurityException se) {
                        Log.e(TAG, "");
                    }
                }
            } else {
                //Main Routine to download file
                boolean same=false;
                File lf = new File(sf.file);
                if (lf.exists() && !lf.isDirectory()) {
                    String drivemd5 = sf.dFile.getMd5Checksum();
                    String lmd5 = fileToMD5(sf.file);
                    if (lmd5.equals(drivemd5)) {
                        same=true;
                    } else {
                        Log.v(TAG,lmd5 + ":"+drivemd5);
                    }
                }
                if (!this.isCancelled() && sf.dFile.getDownloadUrl() != null
                        && sf.dFile.getDownloadUrl().length() > 0 && !same) {
                    try {
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
            parent.processed++;
            publishProgress((long)0);
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (localn == 0) {
            procfile = "";
            publishProgress((long) 0);
            ((Button) this.activity.findViewById(R.id.actionbutton)).setText(parent.getString(R.string.start));
            showToast(parent.getString(R.string.Alldone));
            parent.started = 0;
        }
    }

    @Override
    protected void onCancelled(Boolean results){
        if (localn == 1){
            showToast(parent.getString(R.string.writerror));
            parent.started = 0;
        }
    }

    @Override
    protected void onProgressUpdate(Long... progress) {
        parent.mProgress.setProgress(progress[0].intValue());
        String text = parent.processed + "/" + parent.total + "  " + parent.getString(R.string.processedV);
        ((TextView) this.activity.findViewById(R.id.processedfileText)).setText(procfile);
        ((TextView) this.activity.findViewById(R.id.processedText)).setText(text);
    }

    private void showToast(String message) {
        Toast.makeText(this.activity, message, Toast.LENGTH_LONG).show();
    }

    public void onAttach(Activity activity){
        this.activity = activity;
    }

    public void onDetach(){
        this.activity = null;
    }


    public void fromroot(Drive service, String driveid, String l_path, int localint){
        this.service = service;
        this.iddrive = driveid;
        this.lpath = l_path;
        this.localn = localint;
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
                    parent.total++;
                }
                request.setPageToken(files.getNextPageToken());
            } catch (IOException e) {
                Log.e(TAG, "", e);
                request.setPageToken(null);
            }
        } while (request.getPageToken() != null && request.getPageToken().length() > 0);
        return Iresult;
    }

    public void CopyStream(long size, InputStream is, OutputStream os) {
        final int buffer_size = 4096;
        byte[] bytes = new byte[buffer_size];
        try {
            int count,prog=0;
            while ((count = is.read(bytes)) != -1) {
                os.write(bytes, 0, count); //write buffer
                prog = prog + count;
                publishProgress(((long) prog) * 100 / size);
            }

            os.flush();
            is.close();
            os.close();
        } catch (IOException e){
            Log.e(TAG, "stop" + e);
            if (!this.isCancelled()) {
                localn = 1;
            }else {
                this.cancel(true);
            }
        } catch (Exception ex) {
            Log.e(TAG,"CS "+ex);
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
