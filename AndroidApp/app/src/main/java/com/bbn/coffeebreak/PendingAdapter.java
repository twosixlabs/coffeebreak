package com.bbn.coffeebreak;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PendingAdapter extends RecyclerView.Adapter<PendingAdapter.ViewHolder> {
    private List<MeetingList.Meeting> mMeetings;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView meetingIdView;
        public TextView pendingMemView;
        public Button cancelButton;

        public ViewHolder(View v) {
            super(v);

            meetingIdView = (TextView) v.findViewById(R.id.meeting_id_view);
            pendingMemView = (TextView) v.findViewById(R.id.pending_mem_view);
            cancelButton = (Button) v.findViewById(R.id.cancel_button);
        }
    }

    public PendingAdapter(List<MeetingList.Meeting> meetings) {
        mMeetings = meetings;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public PendingAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.text_view, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(contactView);
        return viewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        // Get the data model based on position
        MeetingList.Meeting meeting = mMeetings.get(position);

        // Set item views based on your views and data model
        TextView textView = viewHolder.meetingIdView;
        textView.setText("Meeting ID: " + meeting.meetingID);

        TextView textView2 = viewHolder.pendingMemView;
        String pen = meeting.pending_invites.toString();
        pen = pen.substring(1, pen.length() - 1);
        textView2.setText("Waiting on " + pen);

        Button button = viewHolder.cancelButton;
        button.setEnabled(true);
    }

    @Override
    public int getItemCount() {
        return mMeetings.size();
    }
}