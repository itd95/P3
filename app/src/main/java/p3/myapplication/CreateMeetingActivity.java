package p3.myapplication;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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
import java.util.Calendar;

public class CreateMeetingActivity extends AppCompatActivity {

	DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
	FirebaseAuth mAuth = FirebaseAuth.getInstance();
	String moduleName;

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
	int currentYear = calendar.get(Calendar.YEAR);
	int currentMonth = calendar.get(Calendar.MONTH);
	int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
	int nextHour = calendar.get(Calendar.HOUR + 1);
	int defaultMinute = 0;
	int selectedDay = currentDay;
	int selectedMonth = currentMonth;
	int selectedYear = currentYear;

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
		setDateLabel(currentYear, currentMonth, currentDay);
		Log.d("mytag", "Date label at initialisation: " + dateLabel.getText().toString());

		// date picker dialog button
		chooseDate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DatePickerDialog dialog = new DatePickerDialog(CreateMeetingActivity.this,
						dateSetListener, currentYear, currentMonth, currentDay);
				dialog.getDatePicker().setMinDate(System.currentTimeMillis());
				dialog.show();
			}
		});

		// start time picker dialog button
		chooseStartTime.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TimePickerDialog dialog = new TimePickerDialog(CreateMeetingActivity.this,
						startTimeSetListener, nextHour, defaultMinute, true);
				dialog.show();
			}
		});

		// end time picker dialog button
		chooseEndTime.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TimePickerDialog dialog = new TimePickerDialog(CreateMeetingActivity.this,
						endTimeSetListener, nextHour + 1, defaultMinute, true);
				dialog.show();
			}
		});

		// sets listener to date picker
		dateSetListener = new DatePickerDialog.OnDateSetListener() {
			@Override
			public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
				selectedYear = year;
				selectedMonth = month + 1;
				selectedDay = dayOfMonth;

				setDateLabel(year, month, dayOfMonth);
				Log.d("mytag", "Date label before selection: " + dateLabel.getText().toString());
			}
		};

		// sets listener to start time picker
		startTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
			@Override
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				setTimeLabel(startTimeLabel, hourOfDay, minute);
			}
		};

		// sets listener to end time picker
		endTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
			@Override
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				setTimeLabel(endTimeLabel, hourOfDay, minute);
			}
		};

		// check data!
		submit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (verifyData(selectedYear, selectedMonth, selectedDay)) {
					String startDate = selectedYear + "-" + checkExtraZero(selectedMonth) + "-" + selectedDay + " " + startTimeLabel.getText();
					String endDate = selectedYear + "-" + checkExtraZero(selectedMonth) + "-" + selectedDay + " " + endTimeLabel.getText();

					String pushKey = reference.child("meetings/" + moduleName).push().getKey();
					// sets all meeting information
					reference.child("meetings/" + moduleName + "/" + pushKey).setValue(new Meeting(meetingName.getText().toString(), startDate, endDate));
					reference.child("meetings/" + moduleName + "/" + pushKey + "/members/" + mAuth.getUid()).setValue("true");
					// sets user meeting participation
					reference.child("users/" + mAuth.getUid() + "/meetings/" + pushKey).setValue("true");
				}
				else {}
			}
		});
	}

	// helps set the date label to the date selected with the date picker
	public void setDateLabel (int year, int month, int dayOfMonth) {
		String dateString = String.format(getResources().getString(R.string.date_format_create),
				new DateFormatSymbols().getMonths()[month], dayOfMonth, year);
		dateLabel.setText(dateString);
	}

	public void setTimeLabel (TextView field, int hour, int minute) {
		String hourString = checkExtraZero(hour);
		String minuteString = checkExtraZero(minute);

		String timeString = String.format(getResources().getString(R.string.time_format_create), hourString, minuteString);
		field.setText(timeString);
	}

	String checkExtraZero (int number) {
		if (number < 10)
			return "0" + Integer.toString(number);
		return Integer.toString(number);
	}

	boolean verifyData (int year, int month, int day) {

		return true;
	}

	public void onStart() {
		super.onStart();
	}

	@Override
	public void onStop() {
		super.onStop();
	}
}