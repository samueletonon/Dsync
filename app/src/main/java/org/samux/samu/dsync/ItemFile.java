package org.samux.samu.dsync;

import com.google.api.services.drive.model.File;

public class ItemFile {
    public String file;
    public int icon;
    public boolean canRead;
    public String driveid;
    public File dFile;


    public ItemFile(String file, Integer icon, boolean canRead,String driveid, File dFile) {
        this.file = file;
        this.icon = icon;
        this.canRead = canRead;
        this.driveid = driveid;
        this.dFile = dFile;

    }

    @Override
    public String toString() {
        return file;
    }
}

