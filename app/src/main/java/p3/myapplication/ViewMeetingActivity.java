package p3.myapplication;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import p3.myapplication.ArrayAdapters.ParticipantDetailsArrayAdapter;
import p3.myapplication.Model.Message;

@SuppressWarnings("ConstantConditions")
public class ViewMeetingActivity extends AppCompatActivity {

	DatabaseReference reference;

	TextView titleLabel;
	TextView moduleLabel;
	Button messageButton;
	ListView participantsList;
	TextView date;
	TextView time;
	Button leaveMeeting;
	Button joinMeeting;
	Button endMeeting;
	LinearLayout manageMeetingPanel;
	Button editButton;

	String referenceString;
	String meetingID;
	String meetingName;
	String currentUserName;
	Helper helper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_meeting);

		reference = FirebaseDatabase.getInstance().getReference();

		meetingID = getIntent().getExtras().getString("p3.myapplication:meeting_id");
		referenceString = "meetings/" + meetingID;
		helper = new Helper(ViewMeetingActivity.this);

		titleLabel = findViewById(R.id.meetingTitleView);
		moduleLabel = findViewById(R.id.meetingModuleView);
		messageButton = findViewById(R.id.messageButtonView);
		participantsList = findViewById(R.id.participantsListView);
		date = findViewById(R.id.dateView);
		time = findViewById(R.id.timeView);
		leaveMeeting = findViewById(R.id.leaveMeetingButtonView);
		joinMeeting = findViewById(R.id.joinMeetingButtonView);
		endMeeting = findViewById(R.id.endMeetingButtonView);
		manageMeetingPanel = findViewById(R.id.manageMeetingPanel);
		editButton = findViewById(R.id.editButtonView);

		// by default, the end meeting option is not visible
		endMeeting.setVisibility(View.GONE);

		BottomNavigationView navigation = findViewById(R.id.navigationViewMeeting);
		navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem item) {
				switch (item.getItemId()) {
					case R.id.action_home: {
						helper.goHome();
						return false;
					}
					case R.id.action_messages: {
						helper.goToMessages();
						return false;
					}
					case R.id.action_profile: {
						helper.goToProfile(true, FirebaseAuth.getInstance().getCurrentUser().getUid());
						return false;
					}
				}
				return false;
			}
		});
		navigation.getMenu().findItem(navigation.getSelectedItemId()).setCheckable(false);

		reference.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				try {
					showData(dataSnapshot);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				Log.d("mytag", "realtimeDatabase:error");
			}
		});

		editButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				goToEdit();
			}
		});
	}

	public void showData (final DataSnapshot dataSnapshot) throws ParseException {
		titleLabel.setText(dataSnapshot.child(referenceString + "/name").getValue(String.class));
		moduleLabel.setText(getIntent().getExtras().getString("p3.myapplication:module_id"));
		date.setText(getIntent().getExtras().getString("p3.myapplication:date"));
		time.setText(getIntent().getExtras().getString("p3.myapplication:hours"));

		meetingName = dataSnapshot.child(referenceString + "/name").getValue(String.class);
		currentUserName = dataSnapshot.child("users/" + FirebaseAuth.getInstance().getCurrentUser().getUid() + "/firstName").getValue(String.class);

		// gets all the participants in /[meeting]/members and looks for the matching key information in /users
		List<String[]> values = new ArrayList<>();
		for (DataSnapshot data : dataSnapshot.child(referenceString + "/members").getChildren()) {
			DataSnapshot user = dataSnapshot.child("users/" + data.getKey());
			String rating = String.format(java.util.Locale.US,"%.1f", helper.getRating(dataSnapshot.child("users/" + data.getKey())));
			values.add(new String[] {user.getKey(), // key of the user in the list [0]
									 user.child("firstName").getValue(String.class), // first name of the user in the list [1]
									 user.child("lastName").getValue(String.class), // last name of the user in the list [2]
									 rating}); // rating of the user in the list [3]
		}

		// if meeting still exists
		if (dataSnapshot.child("meetings/" + meetingID).exists()) {
			// customises if user is participant
			if (dataSnapshot.child("users/" + FirebaseAuth.getInstance().getCurrentUser().getUid() + "/meetings/" + meetingID).exists()) {

				// calculates the current date and time
				Calendar calendar = Calendar.getInstance(Locale.UK);
				calendar.add(Calendar.HOUR_OF_DAY, 1);
				// gets the end time for the meeting
				Date meetingEndDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.UK).parse(dataSnapshot.child("meetings/" + meetingID + "/endDate").getValue(String.class));

				// checks if the meeting has more than one participant and if the endTime for the meeting has passed
				// in which case, it will block the meeting management and leave meeting options in the favour of the end meeting button
					// this way, the user will be encouraged to give feedback
				if (dataSnapshot.child("meetings/" + meetingID + "/members").getChildrenCount() > 1 && meetingEndDate.before(calendar.getTime())) {
					manageMeetingPanel.setVisibility(View.GONE);
					leaveMeeting.setVisibility(View.GONE);
					joinMeeting.setVisibility(View.GONE);
					endMeeting.setVisibility(View.VISIBLE);

					endMeeting.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							helper.goToRating(meetingID, dataSnapshot.child("meetings/" + meetingID + "/name").getValue(String.class));
						}
					});
				}
				// if meeting has one participant, and the endTime has passed
				// the meeting will be cancelled automatically
				else if (dataSnapshot.child("meetings/" + meetingID + "/members").getChildrenCount() == 1 && meetingEndDate.before(calendar.getTime())) {
					helper.deleteMeeting(meetingID, reference, meetingID);
					// redirects the user to the home page
					helper.goHome();
				}
				// otherwise
				else {
					manageMeetingPanel.setVisibility(View.VISIBLE);
					leaveMeeting.setVisibility(View.VISIBLE);
					joinMeeting.setVisibility(View.GONE);

					leaveMeeting.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {

							reference.addListenerForSingleValueEvent(new ValueEventListener() {
								@Override
								public void onDataChange(DataSnapshot dataSnapshot) {
									// todo: POTENTIAL BUG ?????
									// if current user is the only participant
									if (dataSnapshot.child("meetings/" + meetingID + "/members").getChildrenCount() <= 1) {
										helper.deleteMeeting(FirebaseAuth.getInstance().getCurrentUser().getUid(), reference, meetingID);
										// redirects the user to the home page
										helper.goHome();
									}
									else
										removeUserFromMeeting(meetingID, dataSnapshot);

									// subtract 10 points if meeting is left
									helper.addPoints(dataSnapshot, reference, FirebaseAuth.getInstance().getCurrentUser().getUid(), -10);
								}

								@Override
								public void onCancelled(DatabaseError databaseError) {

								}
							});
						}
					});

					messageButton.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							helper.goToChat(meetingID, titleLabel.getText().toString());
						}
					});
				}
			}
			// customises if user is not participant
			else {
				manageMeetingPanel.setVisibility(View.GONE);
				leaveMeeting.setVisibility(View.GONE);
				joinMeeting.setVisibility(View.VISIBLE);

				joinMeeting.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						reference.child("users/" + FirebaseAuth.getInstance().getCurrentUser().getUid() + "/meetings/" + meetingID).setValue("true");
						reference.child("meetings/" + meetingID + "/members/" + FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue("true");

						Calendar calendar = Calendar.getInstance(Locale.UK);
						calendar.add(Calendar.HOUR_OF_DAY, 1);
						reference.child("chats/" + meetingID).push().setValue(new Message(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK).format(calendar.getTime()),
								"system",
								dataSnapshot.child("users/" + FirebaseAuth.getInstance().getCurrentUser().getUid() + "/firstName").getValue(String.class) + " joined the chat"));

						// add 10 points if meeting is joined
						helper.addPoints(dataSnapshot, reference, FirebaseAuth.getInstance().getCurrentUser().getUid(), 10);

						manageMeetingPanel.setVisibility(View.VISIBLE);
						leaveMeeting.setVisibility(View.VISIBLE);
						joinMeeting.setVisibility(View.GONE);
					}
				});
			}
		}

		ParticipantDetailsArrayAdapter participantListAdapter = new ParticipantDetailsArrayAdapter(0, this, values);
		participantsList.setAdapter(participantListAdapter);
	}

	// removes user from meeting
	public void removeUserFromMeeting (final String meetingID, DataSnapshot dataSnapshot) {
		// removes meeting reference from [user]
		reference.child("users/" + FirebaseAuth.getInstance().getCurrentUser().getUid() + "/meetings/" + meetingID).removeValue();

		reference.child("meetings/" + meetingID + "/members/" + FirebaseAuth.getInstance().getCurrentUser().getUid()).removeValue();
		// adds system message that the user left the meeting
		Calendar calendar = Calendar.getInstance(Locale.UK);
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		reference.child("chats/" + meetingID).push().setValue(new Message(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK).format(calendar.getTime()),
																		"system",
																		dataSnapshot.child("users/" + FirebaseAuth.getInstance().getCurrentUser().getUid() + "/firstName").getValue(String.class) + " left the chat"));
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	void goToEdit () {
		Intent i = new Intent(ViewMeetingActivity.this, EditMeeting.class);

		String[] dateArray = getIntent().getExtras().getString("p3.myapplication:dateString").split("-");
		String[] hoursArray = getIntent().getExtras().getString("p3.myapplication:hours").split(" - ");
		String[] startDate = hoursArray[0].split(":");
		String[] endDate = hoursArray[1].split(":");

		i.putExtra("p3.myapplication:meetingReference", meetingID);
		i.putExtra("p3.myapplication:module", moduleLabel.getText().toString());
		i.putExtra("p3.myapplication:meetingName", meetingName);
		i.putExtra("p3.myapplication:year", Integer.parseInt(dateArray[0]));
		i.putExtra("p3.myapplication:month", Integer.parseInt(dateArray[1]) - 1);
		i.putExtra("p3.myapplication:day", Integer.parseInt(dateArray[2]));
		i.putExtra("p3.myapplication:startHour", Integer.parseInt(startDate[0]));
		i.putExtra("p3.myapplication:startMinute", Integer.parseInt(startDate[1]));
		i.putExtra("p3.myapplication:endHour", Integer.parseInt(endDate[0]));
		i.putExtra("p3.myapplication:endMinute", Integer.parseInt(endDate[1]));
		i.putExtra("p3.myapplication:name", currentUserName);

		startActivity(i);
		finish();
	}
}