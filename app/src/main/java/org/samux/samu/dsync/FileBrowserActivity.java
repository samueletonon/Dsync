package org.samux.samu.dsync;


//General Java imports 

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class FileBrowserActivity extends ActionBarActivity {
    // Intent Action Constants
    public static final String INTENT_ACTION_SELECT_DIR = "org.samux.samu.dsync.SELECT_DIRECTORY_ACTION";
    public static final String INTENT_ACTION_SELECT_FILE = "org.samux.samu.dsync.SELECT_FILE_ACTION";

    // Intent parameters names constants
    public static final String startDirectoryParameter = "org.samux.samu.dsync.directoryPath";
    public static final String returnFileParameter = "org.samux.samu.dsync.filePathRet";
    public static final String showCannotReadParameter = "org.samux.samu.dsync.showCannotRead";

    ArrayList<String> pathDirsList = new ArrayList<>();

    private static final String LOGTAG = "F_PATH";

    private List<ItemFile> fileList = new ArrayList<>();
    private File path = null;
    private String chosenFile;

    ArrayAdapter<ItemFile> adapter;

    private boolean showHiddenFilesAndDirs = true;

    private boolean directoryShownIsEmpty = false;

    // Action constants
    private static int currentAction = -1;
    private static final int SELECT_DIRECTORY = 1;
    private static final int SELECT_FILE = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filebrowser_layout);
        Intent thisInt = this.getIntent();
        currentAction = SELECT_DIRECTORY;
        if (thisInt.getAction().equalsIgnoreCase(INTENT_ACTION_SELECT_FILE)) {
            Log.d(LOGTAG, "SELECT ACTION - SELECT FILE");
            currentAction = SELECT_FILE;
        }
        showHiddenFilesAndDirs = thisInt.getBooleanExtra(showCannotReadParameter, true);
        setInitialDirectory();
        parseDirectoryPath();
        loadFileList();
        this.createFileListAdapter();
        this.initializeButtons();
        this.initializeFileListView();
        updateCurrentDirectoryTextView();
    }

    private void setInitialDirectory() {
        Intent thisInt = this.getIntent();
        String requestedStartDir = thisInt.getStringExtra(startDirectoryParameter);
        if (requestedStartDir != null && requestedStartDir.length() > 0) {// if(requestedStartDir!=null
            File tempFile = new File(requestedStartDir);
            if (tempFile.isDirectory())
                this.path = tempFile;
        }
        if (this.path == null) {
            if (Environment.getExternalStorageDirectory().isDirectory()
                    && Environment.getExternalStorageDirectory().canRead())
                path = Environment.getExternalStorageDirectory();
            else
                path = new File("/");
        }
    }

    private void parseDirectoryPath() {
        pathDirsList.clear();
        String pathString = path.getAbsolutePath();
        String[] parts = pathString.split("/");
        int i = 0;
        while (i < parts.length) {
            pathDirsList.add(parts[i]);
            i++;
        }
    }

    private void initializeButtons() {
        Button upDirButton = (Button) this.findViewById(R.id.upDirectoryButton);
        upDirButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Log.d(LOGTAG, "onclick for upDirButton");
                loadDirectoryUp();
                loadFileList();
                adapter.notifyDataSetChanged();
                updateCurrentDirectoryTextView();
            }
        });
        Button selectFolderButton = (Button) this.findViewById(R.id.selectCurrentDirectoryButton);
        if (currentAction == SELECT_DIRECTORY) {
            selectFolderButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Log.d(LOGTAG, "onclick for selectFolderButton");
                    returnDirectoryFinishActivity();
                }
            });
        } else {// if(currentAction == this.SELECT_DIRECTORY) {
            selectFolderButton.setVisibility(View.GONE);
        }// } else {//if(currentAction == this.SELECT_DIRECTORY) {
    }// private void initializeButtons() {

    private void loadDirectoryUp() {
        // present directory removed from list
        String s = pathDirsList.remove(pathDirsList.size() - 1);
        // path modified to exclude present directory
        path = new File(path.toString().substring(0,
                path.toString().lastIndexOf(s)));
        fileList.clear();
    }

    private void updateCurrentDirectoryTextView() {
        int i = 0;
        String curDirString = "";
        while (i < pathDirsList.size()) {
            curDirString += pathDirsList.get(i) + "/";
            i++;
        }
        if (pathDirsList.size() == 0) {
            this.findViewById(R.id.upDirectoryButton).setEnabled(false);
            curDirString = "/";
        } else
            this.findViewById(R.id.upDirectoryButton).setEnabled(true);
        ( (TextView) this.findViewById(R.id.currentDirectoryTextView)).setText(getString(R.string.curdir) + curDirString);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void initializeFileListView() {
        ListView lView = (ListView) this.findViewById(R.id.fileListView);
        lView.setAdapter(this.adapter);
        lView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                chosenFile = fileList.get(position).file;
                File sel = new File(path + "/" + chosenFile);
                Log.v(LOGTAG, "item Clicked:" + chosenFile);
                if (sel.isDirectory()) {
                    if (sel.canRead()) {
                        // Adds chosen directory to list
                        pathDirsList.add(chosenFile);
                        path = new File(sel + "");
                        Log.d(LOGTAG, "Just reloading the list");
                        loadFileList();
                        adapter.notifyDataSetChanged();
                        updateCurrentDirectoryTextView();
                        Log.d(LOGTAG, path.getAbsolutePath());
                    } else {// if(sel.canRead()) {
                        showToast("Path does not exist or cannot be read");
                    }// } else {//if(sel.canRead()) {
                } else {// if (sel.isDirectory()) {
                    Log.d(LOGTAG, "item clicked");
                    if (!directoryShownIsEmpty) {
                        Log.d(LOGTAG, "File selected:" + chosenFile);
                        returnFileFinishActivity(sel.getAbsolutePath());
                    }
                }// else {//if (sel.isDirectory()) {
            }// public void onClick(DialogInterface dialog, int which) {
        });// lView.setOnClickListener(
    }// private void initializeFileListView() {

    private void returnDirectoryFinishActivity() {
        SharedPreferences Pref = getSharedPreferences(MainActivity.PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = Pref.edit();
        editor.putString("localfolder", path.getAbsolutePath());
        editor.commit();
        Intent data = new Intent();
        this.setResult(RESULT_OK, data);
        this.finish();
    }// END private void returnDirectoryFinishActivity() {

    private void returnFileFinishActivity(String filePath) {
        Intent retIntent = new Intent();
        retIntent.putExtra(returnFileParameter, filePath);
        this.setResult(RESULT_OK, retIntent);
        this.finish();
    }// END private void returnDirectoryFinishActivity() {

    private void loadFileList() {
        fileList.clear();
        if (path.exists() && path.canRead()) {
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    boolean showReadableFile = showHiddenFilesAndDirs
                            || sel.canRead();
                    // Filters based on whether the file is hidden or not
                    return currentAction != SELECT_DIRECTORY || (sel.isDirectory() && showReadableFile);
                }
            };
            String[] fList = path.list(filter);
            this.directoryShownIsEmpty = false;
            Log.d(LOGTAG,"Selected Dir");
            for (int i = 0; i < fList.length; i++) {
                File sel = new File(path, fList[i]);
                Log.d(LOGTAG,"File:" + fList[i] + " readable:" + (Boolean.valueOf(sel.canRead())).toString());
                int drawableID = R.drawable.file_icon;
                boolean canRead = sel.canRead();
                if (sel.isDirectory()) {
                    drawableID = R.drawable.folder_icon;
                }
                if (canRead) {
                    fileList.add(i, new ItemFile(fList[i], drawableID, canRead, "",null));
                }
            }
            if (fileList.size() == 0) {
                this.directoryShownIsEmpty = true;
                fileList.add(0, new ItemFile(getString(R.string.empty), -1, true,"",null));
            } else {// sort non empty list
                Collections.sort(fileList, new ItemFileNameComparator());
            }
        } else {
            Log.e(LOGTAG, "path does not exist or cannot be read");
        }
    }

    private void createFileListAdapter() {
        adapter = new ArrayAdapter<ItemFile>(this,
                android.R.layout.select_dialog_item, android.R.id.text1,
                fileList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                int drawableID = 0;
                if (fileList.get(position).icon != -1) {
                    drawableID = fileList.get(position).icon;
                }
                textView.setCompoundDrawablesWithIntrinsicBounds(drawableID, 0,
                        0, 0);

                textView.setEllipsize(null);

                int dp3 = (int) (3 * getResources().getDisplayMetrics().density + 0.5f);
                textView.setCompoundDrawablePadding(dp3);
                textView.setBackgroundColor(Color.LTGRAY);
                return view;
            }
        };
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(LOGTAG, "ORIENTATION_LANDSCAPE");
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d(LOGTAG, "ORIENTATION_PORTRAIT");
        }
    }

}
