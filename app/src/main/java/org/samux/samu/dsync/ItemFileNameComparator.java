package org.samux.samu.dsync;

import java.util.Comparator;

/**
 * Created by samu on 19/01/15.
 */

public class ItemFileNameComparator implements Comparator<ItemFile> {
    public int compare(ItemFile lhs, ItemFile rhs) {
        return lhs.file.compareTo(rhs.file);
    }
}
