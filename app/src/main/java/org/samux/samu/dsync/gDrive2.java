package org.samux.samu.dsync;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class gDrive2 extends ActionBarActivity {

    private static final String TAG = "Setup2";
    List<File> mGFile;
    static final int REQUEST_CODE_PICK_FILE_TO_SAVE_INTERNAL = 1;
    private List<ItemFile> fileList = new ArrayList<>();
    ArrayAdapter<ItemFile> adapter;
    private ItemFile parentddir = null;
    private ItemFile ddir = null;

    private void setParents(String driveid) {
        try {
            File file = gDrive1.service.files().get(driveid).execute();
            List<ParentReference> pr = file.getParents();
            if (pr.size() == 0) {
                parentddir = null;
            }else {
                parentddir.driveid = pr.get(0).getId();
            }
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }
    }
    private List<File> retrieveAllFiles(Drive service) {
        List<File> result = new ArrayList<>();
        Files.List request;
        try {
            String query;
            if (ddir == null) {
                query = "mimeType='application/vnd.google-apps.folder' and trashed=false and ('root' in parents or sharedWithMe)";
            } else {
                query = "mimeType='application/vnd.google-apps.folder' and trashed=false and '" + ddir.driveid +  "' in parents";
                Log.v(TAG,"q "+ query);
            }
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

    private void initializeButtons() {
        Button upDirButton = (Button) this.findViewById(R.id.upDirectory2);
        upDirButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "onclick for upDirButton");
                fileList.clear();
                ddir = parentddir;
                if (ddir != null) {
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            setParents(ddir.driveid);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.notifyDataSetChanged();
                                }
                            });
                        }
                    });
                    t.start();
                }
                loadFileList();

            }
        });
        Button selectFolderButton = (Button) this
                .findViewById(R.id.selectDirectory2);
        selectFolderButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "onclick for selectFolderButton");
                getLocalFolder();
            }
        });
    }

    private void loadFileList() {
        String msg = getString(R.string.select_text);
        if (ddir != null)
            msg = getString(R.string.curdir) + ddir.file;
        ( (TextView) this.findViewById(R.id.currentDirectoryTextView2)).setText(msg);

        this.findViewById(R.id.selectDirectory2).setEnabled(false);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                mGFile = retrieveAllFiles(gDrive1.service);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mGFile.size() == 0){
                            fileList.add(0, new ItemFile(getString(R.string.empty), -1, true,"",null));
                        } else {
                            int drawableID = R.drawable.folder_icon;
                            int i=0;
                            for (File file : mGFile) {
                                fileList.add(i, new ItemFile(file.getTitle(), drawableID, true, file.getId(),file));
                                i++;
                            }
                        }
                        Collections.sort(fileList, new ItemFileNameComparator());
                        adapter.notifyDataSetChanged();
                        findViewById(R.id.selectDirectory2).setEnabled(true);
                    }
                });
            }
        });
        t.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gdrive2);
        loadFileList();
        this.createFileListAdapter();
        this.initializeButtons();
        ListView mListView = (ListView) findViewById(R.id.listViewResults);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                parentddir = ddir;
                ddir = fileList.get(position);
                fileList.clear();
                loadFileList();
            }
        });
    }

    private void createFileListAdapter() {
        adapter = new ArrayAdapter<ItemFile>(this, android.R.layout.select_dialog_item, android.R.id.text1,fileList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // creates view
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                // put the image on the text view
                int drawableID = 0;
                if (fileList.get(position).icon != -1) {
                    // If icon == -1, then directory is empty
                    drawableID = fileList.get(position).icon;
                }
                textView.setCompoundDrawablesWithIntrinsicBounds(drawableID, 0,0, 0);
                textView.setEllipsize(null);
                int dp3 = (int) (3 * getResources().getDisplayMetrics().density + 0.5f);
                textView.setCompoundDrawablePadding(dp3);
                textView.setBackgroundColor(Color.LTGRAY);
                return view;
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PICK_FILE_TO_SAVE_INTERNAL:
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "ok");
                    Intent Idata = new Intent();
                    this.setResult(RESULT_OK, Idata);
                    this.finish();
                } else {
                    Log.d(TAG, "ahio "+ resultCode);
                    Intent fileExploreIntent = new Intent(FileBrowserActivity.INTENT_ACTION_SELECT_DIR,null,this,FileBrowserActivity.class);
                    startActivityForResult(fileExploreIntent,REQUEST_CODE_PICK_FILE_TO_SAVE_INTERNAL);
                }
                break;
        }
    }

    public void getLocalFolder(){
        SharedPreferences Pref = getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = Pref.edit();
        editor.putString("driveid", ddir.driveid);
        editor.commit();
        Intent fileExploreIntent = new Intent(FileBrowserActivity.INTENT_ACTION_SELECT_DIR,null,this,FileBrowserActivity.class);
        startActivityForResult(fileExploreIntent,REQUEST_CODE_PICK_FILE_TO_SAVE_INTERNAL);
    }
}
