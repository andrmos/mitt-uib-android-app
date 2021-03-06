package no.mofifo.imber.card_view_holders;

import android.view.View;
import android.widget.TextView;

import no.mofifo.imber.R;

/**
 * View holder defining the course RecyclerView card for a users courses.
 *
 * Created by Andre on 22/02/2016.
 */
public class CourseViewHolder extends GeneralViewHolder {
    // each data item is just a string
    public TextView course_code;
    public TextView course_title;

    public CourseViewHolder(View v) {
        super(v);
        course_code = (TextView) v.findViewById(R.id.course_code);
        course_title = (TextView) v.findViewById(R.id.course_title);
    }
}