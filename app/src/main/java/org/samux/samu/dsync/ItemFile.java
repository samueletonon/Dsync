package org.samux.samu.dsync;

/**
 * Created by samu on 19/01/15.
 */
public class ItemFile {
    public String file;
    public int icon;
    public boolean canRead;

    public ItemFile(String file, Integer icon, boolean canRead) {
        this.file = file;
        this.icon = icon;
    }

    @Override
    public String toString() {
        return file;
    }
}

