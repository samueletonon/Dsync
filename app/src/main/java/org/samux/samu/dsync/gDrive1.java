package org.samux.samu.dsync;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.Arrays;


public class gDrive1 extends ActionBarActivity {

    private static final String APPLICATION_NAME = "Dsync";
    public final static String AName = "org.samux.MESSAGE";
    static final int REQUEST_ACCOUNT_PICKER = 1;
    static final int REQUEST_AUTHORIZATION = 2;
    static final int LAUNCH_SETUP2 = 3;
    public static Drive service;
    private GoogleAccountCredential credential;
    private static final String TAG = "gDrive1";
    private static Intent gDrive2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gdrive1);
        gDrive2 = new Intent(this, gDrive2.class);
    }

    public void chooseAccount(View view){
        credential = GoogleAccountCredential.usingOAuth2(this, Arrays.asList(DriveScopes.DRIVE));
        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        credential.setSelectedAccountName(accountName);
                        service = getDriveService(credential);
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    final StringBuilder sb = new StringBuilder();
                                    FileList list = service.files().list().execute();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            return;
                                        }
                                    });

                                } catch (UserRecoverableAuthIOException e) {
                                    startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        t.start();
                        startActivity(gDrive2);
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == Activity.RESULT_OK) {
                    startActivity(gDrive2);
                } else {
                    startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
                }
                break;
        }
    }

    private Drive getDriveService(GoogleAccountCredential credential) {
        return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
            .setApplicationName(APPLICATION_NAME).build();
    }

}
