/*
 * Copyright 2021 Raytheon BBN Technologies Corp.
 * Copyright 2021 Two Six Technologies
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
 
package com.bbn.coffeebreak;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PendingAdapter extends RecyclerView.Adapter<PendingAdapter.ViewHolder> {
    private final static String TAG = "[PendingAdapter]";
    private List<MeetingList.Meeting> mMeetings;
    private OnItemClickListener listener;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView meetingIdView;
        public TextView pendingMemView;
        public Button cancelButton;

        public ViewHolder(View v) {
            super(v);

            meetingIdView = (TextView) v.findViewById(R.id.meeting_id_view);
            pendingMemView = (TextView) v.findViewById(R.id.pending_mem_view);
            cancelButton = (Button) v.findViewById(R.id.cancel_button);

            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Triggers click upwards to the adapter on click
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            Log.d(TAG, "meeting cancelled - removed from pending page");

                            listener.onItemClick(cancelButton, position);

                            MeetingList.Meeting meeting = mMeetings.get(position);

                            MeetingList meetingList = AMQPCommunication.getMeetingList();
                            meetingList.removeMeeting(meeting.meetingID);
                            notifyItemRemoved(position);
                        }
                    }
                }
            });
        }
    }

    // Define the listener interface
    public interface OnItemClickListener {
        void onItemClick(View itemView, int position);
    }

    // Define the method that allows the parent activity or fragment to define the listener
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
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

        Log.d(TAG, "adding meeting to RecyclerView: " + meeting.meetingID);

        TextView textView2 = viewHolder.pendingMemView;
        String pen = meeting.pending_invites.toString();
        pen = pen.substring(1, pen.length() - 1);
        textView2.setText("Waiting on " + pen);

        Button cancel_button = viewHolder.cancelButton;
        cancel_button.setEnabled(true);
    }

    @Override
    public int getItemCount() {
        return mMeetings.size();
    }
}