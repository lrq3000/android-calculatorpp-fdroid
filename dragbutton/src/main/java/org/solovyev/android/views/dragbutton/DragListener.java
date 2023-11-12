package org.solovyev.android.views.dragbutton;

import androidx.annotation.NonNull;
import android.view.View;

import java.util.EventListener;


public interface DragListener extends EventListener {
    boolean onDrag(@NonNull View view, @NonNull DragEvent event);
}
