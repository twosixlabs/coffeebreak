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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PendingActivity extends AppCompatActivity {
    private final static String TAG = "[PendingActivity]";
    private RecyclerView recyclerView;
    private PendingAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Creates pending activity page - currently disabled

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle("Pending Invites");

        recyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecoration);

        MeetingList meetingList = AMQPCommunication.getMeetingList();
        List<MeetingList.Meeting> pendingMeetings = meetingList.getMeetingArrayList();
        Collections.reverse(pendingMeetings);

        mAdapter = new PendingAdapter(pendingMeetings);
        recyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new PendingAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                MeetingList.Meeting meeting = pendingMeetings.get(position);
                Toast.makeText(PendingActivity.this, "Meeting " + meeting.meetingID + " was cancelled", Toast.LENGTH_SHORT).show();

                JSONObject details = meeting.details;

                Intent returnIntent = new Intent();

                try {
                    returnIntent.putExtra("meetingID", details.getString("meetingID"));
                    returnIntent.putExtra("organizer", details.getString("organizer"));

                    JSONArray json_array = details.getJSONArray("attendees");
                    ArrayList<String> arrayList = new ArrayList<String>();
                    for (int i = 0; i < json_array.length(); i++) {
                        arrayList.add(json_array.getString(i));
                    }

                    returnIntent.putStringArrayListExtra("attendees", arrayList);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                setResult(MainActivity.RESULT_OK, returnIntent);
            }
        });
    }
}
