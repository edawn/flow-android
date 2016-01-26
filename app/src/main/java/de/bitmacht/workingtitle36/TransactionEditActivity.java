package de.bitmacht.workingtitle36;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import de.bitmacht.workingtitle36.view.TimeView;

public class TransactionEditActivity extends AppCompatActivity implements View.OnClickListener {

    private TimeView timeView;
    private TimeView dateView;
    private EditText descriptionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_edit);

        timeView = (TimeView) findViewById(R.id.time);
        dateView = (TimeView) findViewById(R.id.date);
        descriptionView = (EditText) findViewById(R.id.description);

        timeView.setTime(System.currentTimeMillis());
        timeView.setOnClickListener(this);

        dateView.setTime(System.currentTimeMillis());
        dateView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.time) {
            new TimePickerFragment().show(getFragmentManager(), "timePicker");
        } else if (id == R.id.date) {
            new DatePickerFragment().show(getFragmentManager(), "datePicker");
        }
    }
}
