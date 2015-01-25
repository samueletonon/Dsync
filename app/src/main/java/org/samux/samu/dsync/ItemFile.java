package org.samux.samu.dsync;

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

