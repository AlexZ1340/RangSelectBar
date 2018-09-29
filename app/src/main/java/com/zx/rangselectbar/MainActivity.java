package com.zx.rangselectbar;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by zhangxuan on 2018/9/29.<br/>
 */
public class MainActivity extends Activity {
    private Context mContext;

    private RangeSelectBar rsbIncome;
    private TextView tvPosition;
    private EditText etLeft;
    private EditText etRight;
    private Button btnSure;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        rsbIncome = findViewById(R.id.rsb_income);
        tvPosition = findViewById(R.id.tv_position);
        etLeft = findViewById(R.id.et_left);
        etRight = findViewById(R.id.et_right);
        btnSure = findViewById(R.id.btn_sure);

        rsbIncome.setOnRangeSelectedListener(new RangeSelectBar.OnRangeSelectedListener() {
            @Override
            public void onRangeSelected(int left, int right) {
                tvPosition.setText("滑块位置：" + left + "," + right);
            }
        });
        btnSure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(etLeft.getText().toString().trim())) return;
                if (TextUtils.isEmpty(etRight.getText().toString().trim())) return;

                int left = Integer.parseInt(etLeft.getText().toString().trim());
                int right = Integer.parseInt(etRight.getText().toString().trim());


                rsbIncome.setRange(left, right);
            }
        });
    }
}
