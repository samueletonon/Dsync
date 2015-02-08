package org.samux.samu.dsync;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {
    public static final String PREF = "org.samux.samu.dsync.preffile";
    public static final String APPLICATION_NAME = "Dsync";

    static final int GET_DRIVE_ACCOUNT = 1;

    private NonUIFragment fragment;
    private static final String TAG = "Mact";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if(savedInstanceState ==null){
            fragment = new NonUIFragment();
            getSupportFragmentManager().beginTransaction().add(fragment, "TaskFragment").commit();
        } else {
            fragment = (NonUIFragment) getSupportFragmentManager().findFragmentByTag("TaskFragment");
        }
        SharedPreferences Pref = getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
        if (! Pref.contains("localfolder")) {
            launch_setup();
        } else {
            mainAction();
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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

    public void onStopStart(View view){
        if (fragment.started == 0) {
            fragment.beginTask();
        }else {
            //TODO stop activity
            showToast(getString(R.string.cancelled));
        }
        mainAction();
    }

    public void edit_config(View view){
        launch_setup();
    }

    private void mainAction(){
        if (fragment.started ==1) {
            ((Button) this.findViewById(R.id.actionbutton)).setText(getString(R.string.stop));
        } else {
            ( (Button) this.findViewById(R.id.actionbutton)).setText(getString(R.string.start));
        }
    }
}
