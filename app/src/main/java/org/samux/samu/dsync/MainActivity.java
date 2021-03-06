package org.samux.samu.dsync;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity implements NonUIFragment.TaskCallbacks {
    public static final String PREF = "org.samux.samu.dsync.preffile";
    public static final String APPLICATION_NAME = "Dsync";
    public static ProgressBar mProgress;

    static final int GET_DRIVE_ACCOUNT = 1;

    private NonUIFragment fragment;
    private static final String TAG = "Mact";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgress = (ProgressBar) findViewById(R.id.progressBar);
        mProgress.setIndeterminate(false);
        mProgress.setMax(100);

        FragmentManager fm = getFragmentManager();

        if(savedInstanceState ==null){
            fragment = new NonUIFragment();
            fm.beginTransaction().add(fragment, "TaskFragment").commit();
        } else {

            fragment = (NonUIFragment) fm.findFragmentByTag("TaskFragment");
        }
        if(fragment.started==1){
            ((Button) findViewById(R.id.actionbutton)).setText(getString(R.string.stop));
        }
        SharedPreferences Pref = getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
        if (!(Pref.contains("localfolder") && Pref.contains("account") && Pref.contains("driveid"))){
            launch_setup();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case GET_DRIVE_ACCOUNT:
                if (resultCode == RESULT_OK) {
                    //mainAction();
                } else {
                    launch_setup();
                }
                break;
        }
    }

    @Override
    public void onPreExecute(){
        mProgress.setProgress(0);
        mProgress.setVisibility(View.VISIBLE);
    }

    @Override
    public void onProgressUpdate(int progress, int processed, int total, String procfile) {
        mProgress.setProgress(progress);
        String text = processed + "/" + total + "  " + getString(R.string.processedV);
        ((TextView) findViewById(R.id.processedfileText)).setText(procfile);
        ((TextView) findViewById(R.id.processedText)).setText(text);
    }

    @Override
    public void onCancelled() {
        showToast(getString(R.string.writerror));
        fragment.started = 0;
    }

    @Override
    public void onPostExecute() {
        mProgress.setProgress(0);
        ((Button) findViewById(R.id.actionbutton)).setText(getString(R.string.start));
        fragment.started = 0;
        showToast(getString(R.string.Alldone));

    }

    private void launch_setup() {
        //this is to launch a wizard
        Intent intent = new Intent(this, gDrive1.class);
        startActivityForResult(intent, GET_DRIVE_ACCOUNT);
    }

    public void onStopStart(View view){
        if (fragment.started == 0) {
            Log.d(TAG,"stop1 button");
            ((TextView) findViewById(R.id.processedText)).setText(R.string.processed);
            ((Button) findViewById(R.id.actionbutton)).setText(getString(R.string.stop));
            fragment.beginTask();
        }else {
            fragment.cancelTask();
            Log.d(TAG,"start2 button");
            ((Button) findViewById(R.id.actionbutton)).setText(getString(R.string.start));
            showToast(getString(R.string.cancelled));
        }
    }

    public void edit_config(View view){
        launch_setup();
    }


    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

}
