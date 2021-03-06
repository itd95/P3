package p3.myapplication;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import p3.myapplication.Model.Meeting;
import p3.myapplication.Model.Message;

public class CreateMeetingActivity extends AppCompatActivity {

	DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
	FirebaseAuth mAuth = FirebaseAuth.getInstance();

	EditText meetingName;
	TextView dateLabel;
	TextView startTimeLabel;
	TextView endTimeLabel;
	DatePickerDialog.OnDateSetListener dateSetListener;
	TimePickerDialog.OnTimeSetListener startTimeSetListener;
	TimePickerDialog.OnTimeSetListener endTimeSetListener;
	Button chooseDate;
	Button chooseStartTime;
	Button chooseEndTime;
	Button submit;

	Calendar calendar = Calendar.getInstance();
	int selectedDay = calendar.get(Calendar.DAY_OF_MONTH);
	int selectedMonth = calendar.get(Calendar.MONTH);
	int selectedYear = calendar.get(Calendar.YEAR);
	int selectedStartHour = 9;
	int selectedStartMinute = 0;
	int selectedEndHour = 10;
	int selectedEndMinute = 0;
	String moduleName;
	String pushKey = reference.child("meetings/" + moduleName).push().getKey();
	Helper helper = new Helper(this);

	@SuppressWarnings("ConstantConditions")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_meeting);

		meetingName = findViewById(R.id.meetingNameCreate);
		dateLabel = findViewById(R.id.dateLabelCreate);
		startTimeLabel = findViewById(R.id.startTimeLabelCreate);
		endTimeLabel = findViewById(R.id.endTimeLabelCreate);
		chooseDate = findViewById(R.id.chooseDateButton);
		chooseStartTime = findViewById(R.id.chooseStartTimeButton);
		chooseEndTime = findViewById(R.id.chooseEndTimeButton);
		submit = findViewById(R.id.confirmCreate);

		moduleName = getIntent().getExtras().getString("p3.myapplication:module_name_list");

		// sets date picker to now
		helper.setDateLabel(dateLabel, selectedYear, selectedMonth, selectedDay);
		helper.setTimeLabel(startTimeLabel, selectedStartHour, 0);
		helper.setTimeLabel(endTimeLabel, selectedEndHour, 0);

		// date picker dialog button
		chooseDate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DatePickerDialog dialog = new DatePickerDialog(CreateMeetingActivity.this,
						dateSetListener, selectedYear, selectedMonth, selectedDay);

				long now = Calendar.getInstance().getTimeInMillis() + 3600000;
				dialog.getDatePicker().setMinDate(now);
				dialog.show();
			}
		});

		// start time picker dialog button
		chooseStartTime.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TimePickerDialog dialog = new TimePickerDialog(CreateMeetingActivity.this,
						startTimeSetListener, selectedStartHour, selectedStartMinute, true);
				dialog.show();
			}
		});

		// end time picker dialog button
		chooseEndTime.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TimePickerDialog dialog = new TimePickerDialog(CreateMeetingActivity.this,
						endTimeSetListener, selectedEndHour, selectedEndMinute, true);
				dialog.show();
			}
		});

		// sets listener to date picker
		dateSetListener = new DatePickerDialog.OnDateSetListener() {
			@Override
			public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
				selectedYear = year;
				selectedMonth = month;
				selectedDay = dayOfMonth;

				helper.setDateLabel(dateLabel, year, month, dayOfMonth);
			}
		};

		// sets listener to start time picker
		startTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
			@Override
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				selectedStartHour = hourOfDay;
				selectedStartMinute = minute;

				helper.setTimeLabel(startTimeLabel, hourOfDay, minute);
			}
		};

		// sets listener to end time picker
		endTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
			@Override
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				selectedEndHour = hourOfDay;
				selectedEndMinute = minute;

				helper.setTimeLabel(endTimeLabel, hourOfDay, minute);
			}
		};

		// confirms and moves to the module meeting list
		submit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// prepares the start and end dates from the date and time pickers, in the right format for the database
				String startDate = helper.prepareDate(selectedYear, selectedMonth, selectedDay, startTimeLabel.getText().toString());
				String endDate = helper.prepareDate(selectedYear, selectedMonth, selectedDay, endTimeLabel.getText().toString());

				// if verification passes
				if (helper.verifyData(startDate, endDate, startTimeLabel, endTimeLabel, meetingName)) {
					// sets all meeting information
					reference.child("meetings/" + pushKey).setValue(new Meeting(meetingName.getText().toString(), startDate, endDate, moduleName));
					reference.child("meetings/" + pushKey + "/members/" + mAuth.getCurrentUser().getUid()).setValue("true");
					// sets user meeting participation
					reference.child("users/" + mAuth.getCurrentUser().getUid() + "/meetings/" + pushKey).setValue("true");
					// sets the system message that the user joined the chat
					reference.addListenerForSingleValueEvent(new ValueEventListener() {
						@Override
						public void onDataChange(DataSnapshot dataSnapshot) {
							calendar = Calendar.getInstance(Locale.UK);
							calendar.add(Calendar.HOUR_OF_DAY, 1);
							reference.child("chats/" + pushKey).push().setValue(new Message(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK).format(calendar.getTime()),
																							"system",
																							dataSnapshot.child("users/" + mAuth.getCurrentUser().getUid() + "/firstName").getValue(String.class) + " joined the chat"));
							// increases user points
							helper.addPoints(dataSnapshot, reference, mAuth.getCurrentUser().getUid(), 10);
						}

						@Override
						public void onCancelled(DatabaseError databaseError) {

						}
					});

					// goes to view meeting after creation
					helper.goToViewMeeting(pushKey,
										   moduleName,
										   helper.getFriendlyDate(selectedYear + "-" + (selectedMonth + 1) + "-" + selectedDay),
										   startTimeLabel.getText().toString() + " - " + endTimeLabel.getText().toString(),
										   selectedYear + "-" + selectedMonth + "-" + selectedDay);
				}
				else {
					Toast.makeText(CreateMeetingActivity.this, "Please review your details.", Toast.LENGTH_SHORT).show();
				}
			}
		});
	}

	public void onStart() {
		super.onStart();
	}

	@Override
	public void onStop() {
		super.onStop();
	}
}