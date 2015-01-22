package org.samux.samu.dsync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;



public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        launch_setup();
        setContentView(R.layout.activity_main);

    }

    private void launch_setup() {
        SharedPreferences Pref = getPreferences(MODE_PRIVATE);
        if (! Pref.contains("src")) {
            //launch the wizard
            Intent intent = new Intent(this, gDrive1.class);
            startActivity(intent);
        } else {
            //go for main activity and download files
            Toast.makeText(this, "Full", Toast.LENGTH_SHORT).show();
        }

    }

}
