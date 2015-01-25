package org.samux.samu.dsync;

/**
 * Created by samu on 19/01/15.
 */

public class ItemFile {
    public String file;
    public int icon;
    public boolean canRead;
    public String driveid;

    public ItemFile(String file, Integer icon, boolean canRead, String driveId) {
        this.file = file;
        this.icon = icon;
        this.canRead = canRead;
        this.driveid = driveId;
    }

    @Override
    public String toString() {
        return file;
    }
}

