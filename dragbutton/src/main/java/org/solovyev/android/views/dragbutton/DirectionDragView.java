package org.solovyev.android.views.dragbutton;


import androidx.annotation.NonNull;

public interface DirectionDragView extends DragView {
    @NonNull
    DirectionText getText(@NonNull DragDirection direction);
}
