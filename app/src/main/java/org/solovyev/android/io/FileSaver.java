package org.solovyev.android.io;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileSaver extends BaseIoSaver {

    @NonNull
    private final File file;

    private FileSaver(@NonNull File file, @NonNull CharSequence data) {
        super(data);
        this.file = file;
    }

    public static void save(@NonNull File file, @NonNull CharSequence data) throws IOException {
        final FileSaver fileSaver = new FileSaver(file, data);
        fileSaver.save();
    }

    @NonNull
    protected FileOutputStream getOutputStream() throws FileNotFoundException {
        final File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        return new FileOutputStream(file);
    }
}
