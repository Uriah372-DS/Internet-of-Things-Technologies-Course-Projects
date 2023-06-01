package com.example.chaquopy_tutorial;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textview = (TextView) findViewById(R.id.text_View);

        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        Python py =  Python.getInstance();
        PyObject pyobj = py.getModule("test");
        PyObject obj = pyobj.callAttr("main");
        textview.setText(obj.toString());


    }
}