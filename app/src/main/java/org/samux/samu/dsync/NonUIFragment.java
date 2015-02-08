package org.samux.samu.dsync;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.Arrays;

/**
 * Created by samu on 08/02/15.
 */
public class NonUIFragment extends Fragment {

    public static final String APPLICATION_NAME = "Dsync";

    private Activity activity;
    private  GetDAT gd=null;
    private String localpath;
    private String aName;
    private String driveId;
    public static Drive service;
    public int started=0;
    public int processed, total;

    static interface TaskCallbacks {
        void onPreExecute();
        void onProgressUpdate(int percent, int processed, int total, String procfile);
        void onCancelled();
        void onPostExecute();
    }


    public NonUIFragment(){

    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        if (gd!=null){
            gd.onAttach(activity);
        }
    }

    @Override
    public void onDetach(){
        super.onDetach();
        if(gd!=null){
            gd.onDetach();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.activity_main, container, false);
    }

    public void beginTask(){
        SharedPreferences Pref = activity.getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
        localpath = Pref.getString("localfolder", null);
        aName = Pref.getString("account", null);
        driveId = Pref.getString("driveid", null);
        processed=0;
        total=0;
        //activity.mProgress.setProgress(0);
        ( (TextView) this.activity.findViewById(R.id.processedText)).setText(R.string.processed);
        ( (Button) this.activity.findViewById(R.id.actionbutton)).setText(getString(R.string.stop));

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(this.activity, Arrays.asList(DriveScopes.DRIVE));
        credential.setSelectedAccountName(aName);
        service = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
                .setApplicationName(APPLICATION_NAME).build();
        gd = new GetDAT(this.activity);
        gd.fromroot(service, driveId, localpath,0);
        gd.execute();
    }
}