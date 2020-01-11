package com.tianshang.butterknifedemo;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.tianshang.annotation.BindView;
import com.tianshang.annotation.OnClick;
import com.tianshang.library.ButterKnife;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv_1)
    TextView tv1;
    @BindView(R.id.tv_2)
    TextView tv2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        tv1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @OnClick({R.id.tv_1,R.id.tv_2})
    void onClick(View view){
        Toast.makeText(MainActivity.this,"dianji",Toast.LENGTH_LONG).show();
    }
}
