/**
 *
 */
package cz.hsrs.hsform.picture;

import java.io.File;

/**
 * @author mkepka
 *
 */
public abstract class AlbumStorageDirFactory {
    public abstract File getAlbumStorageDir(String albumName);
}
