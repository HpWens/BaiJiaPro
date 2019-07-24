package com.mei.baijiaproject;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.mei.baijiaproject.R;

/**
 * @author wenshi
 * @github
 * @Description
 * @since 2019/7/15
 */
public class DemoActivity extends AppCompatActivity {

    EditText mEditText;
    TextView mTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_activity);

        mEditText = findViewById(R.id.et_hint);
        mTextView = findViewById(R.id.tv_ok);

        mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("-------------", "***************" + mEditText.getSelectionStart()
                        + "****" + mEditText.getSelectionEnd());

                String before = mEditText.getText().subSequence(0, mEditText.getSelectionStart()).toString();

                String after = mEditText.getText().subSequence(mEditText.getSelectionStart(), mEditText.getText().length()).toString();

                Log.e("-------------", "***************" + before + "***" + after);
            }
        });
    }
}
