package org.samux.samu.dsync;

import android.content.Intent;
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
import android.widget.Toast;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class gDrive2 extends ActionBarActivity {

    private static final String TAG = "Setup2";
    List<File> mGFile;
    static final int REQUEST_CODE_PICK_FILE_TO_SAVE_INTERNAL = 1;
    static final int GET_LOCAL_FOLDER = 2;
    static final int NEXT_DFOLDER = 3;
    private ListView mListView;
    private List<ItemFile> fileList = new ArrayList<ItemFile>();
    ArrayAdapter<ItemFile> adapter;
    private File path = null;
    private File updir = null;
    private String query;

    private List<File> retrieveAllFiles(Drive service) throws IOException {
        List<File> result = new ArrayList<File>();
        Files.List request;
        Log.d(TAG, "retrieveAllFiles");
        try {
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
                loadDirectoryUp();
                loadFileList();
                adapter.notifyDataSetChanged();
                //TODO
                //updateCurrentDirectoryTextView();
            }
        });
        Button selectFolderButton = (Button) this
                .findViewById(R.id.selectDirectory2);
        selectFolderButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "onclick for selectFolderButton");
                getLocalFolder();
                //returnDirectoryFinishActivity();
            }
        });

    }

    private void loadDirectoryUp() {
        // present directory removed from list
        //String s = pathDirsList.remove(pathDirsList.size() - 1);
        // path modified to exclude present directory
        //path = new java.io.File(path.toString().substring(0,
        //        path.toString().lastIndexOf(s)));
        fileList.clear();
    }

    private void loadFileList() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mGFile = retrieveAllFiles(gDrive1.service);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int drawableID = R.drawable.folder_icon;
                        int i=0;
                        for (File file : mGFile) {
                            fileList.add(i, new ItemFile(file.getTitle(), drawableID,true));
                            i++;
                        }
                        Collections.sort(fileList, new ItemFileNameComparator());
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });
        Log.d(TAG, "loadfiles");
        t.start();
        Log.d(TAG, "loaded");

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gdrive2);
        mListView = (ListView)findViewById(R.id.listViewResults);
        mListView.setAdapter(adapter);
        setInitialDirectory();
        loadFileList();
        this.createFileListAdapter();
        this.initializeButtons();
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                // Clicking on items
            }
        });
        Log.v(TAG, "end");
    }

    private void setInitialDirectory() {
        if (this.path == null) {// No or invalid directory supplied in intent
            // parameter
            this.query = "mimeType='application/vnd.google-apps.folder' and trashed=false and ('root' in parents or sharedWithMe)";
        }
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
                if (resultCode == this.RESULT_OK) {
                    String newDir = data.getStringExtra(FileBrowserActivity.returnDirectoryParameter);
                    Toast.makeText(this, "Received path from file browser:" + newDir,
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Received NO result from file browser",
                            Toast.LENGTH_LONG).show();
                }
                super.onActivityResult(requestCode, resultCode, data);
                break;
            case GET_LOCAL_FOLDER:
                Intent fileExploreIntent = new Intent(FileBrowserActivity.INTENT_ACTION_SELECT_DIR, null, this, FileBrowserActivity.class);
                startActivityForResult(fileExploreIntent, REQUEST_CODE_PICK_FILE_TO_SAVE_INTERNAL);
                break;
            case NEXT_DFOLDER:
                Toast.makeText(this, "Next",Toast.LENGTH_LONG).show();
                break;
        }
    }

    public void getLocalFolder(){
         //gDrive2 = new Intent(this, gDrive2.class);
        Intent fileExploreIntent = new Intent(FileBrowserActivity.INTENT_ACTION_SELECT_DIR,null,this,FileBrowserActivity.class);
        startActivityForResult(fileExploreIntent,REQUEST_CODE_PICK_FILE_TO_SAVE_INTERNAL);
    }
}
