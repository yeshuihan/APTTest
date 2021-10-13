package com.yeshuihan.apttest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.yeshuihan.clickbindannotation.Click;
import com.yeshuihan.clickbindannotation.LongClick;
import com.yeshuihan.fzwclickbind.ClickBind;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ClickBind.bind2(this);

    }


    @Click(R.id.button_1)
    public void click1() {
        Log.i("fzw", "click1");
    }


    @Click(R.id.button_2)
    public static void click2() {
        Log.i("fzw", "click2");
    }


    @LongClick(R.id.button_1)
    public void longClick1() {
        Log.i("fzw", "longClick1");
    }
    @LongClick(R.id.button_2)
    public void longClick2() {
        Log.i("fzw", "longClick2");
    }
}