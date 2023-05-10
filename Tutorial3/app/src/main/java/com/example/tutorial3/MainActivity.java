package com.example.tutorial3;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import com.opencsv.CSVWriter;

import java.util.ArrayList;
import java.util.Random;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

public class MainActivity extends AppCompatActivity {
    LineChart mpLineChart;
    int counter = 1;
    int val = 40;
    float val2 = 40;
    Random rand = new Random();
    private Handler mHandler = new Handler();  //Handler is used for delay definition in the loop
    Button buttonClear;
    Button buttonCsvShow;
    Button stats;



    public MainActivity() throws FileNotFoundException {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
        }

        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},0);
        }

        buttonClear = (Button) findViewById(R.id.button1);
        buttonCsvShow = (Button) findViewById(R.id.button2);
        stats = (Button) findViewById(R.id.button3);

//      clearing previous data from path, if any:
        String csvPath = "/sdcard/csv_dir/";
        clearCsv(csvPath, "data1.csv");
        clearCsv(csvPath, "data2.csv");



//      creating the data:
        mpLineChart = (LineChart) findViewById(R.id.line_chart);
        LineDataSet lineDataSet1 = new LineDataSet(dataValues1(), "Data Set 1");
        LineDataSet lineDataSet2 = new LineDataSet(dataValues1(), "Data Set 2");
        lineDataSet2.setColor(Color.GREEN);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(lineDataSet1);
        dataSets.add(lineDataSet2);
        LineData data = new LineData(dataSets);
        mpLineChart.setData(data);
        mpLineChart.invalidate();



//      updating the data:
        LineDataSet finalLineDataSet1 = lineDataSet1;
        LineDataSet finalLineDataSet2 = lineDataSet2;

        Runnable DataUpdate = new Runnable(){
            @Override
            public void run() {

                data.addEntry(new Entry(counter, val),0);
                data.addEntry(new Entry(counter, val2),1);
                finalLineDataSet1.notifyDataSetChanged(); // let the data know a dataSet changed
                finalLineDataSet2.notifyDataSetChanged(); // let the data know a dataSet changed
                mpLineChart.notifyDataSetChanged(); // let the chart know it's data changed
                mpLineChart.invalidate(); // refresh
                val = (int) (Math.random() * 80);
                val2 = (float) ((20 * rand.nextGaussian()) + 40);

                saveToCsv(csvPath, String.valueOf(counter), String.valueOf(val), "data1.csv");
                saveToCsv(csvPath, String.valueOf(counter), String.valueOf(val2), "data2.csv");

                counter += 1;
                mHandler.postDelayed(this,500);
            }
        };


//      button functions:
        buttonClear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"Clear",Toast.LENGTH_SHORT).show();
                LineData data = mpLineChart.getData();
                ILineDataSet set1 = data.getDataSetByIndex(0);
                ILineDataSet set2 = data.getDataSetByIndex(1);
                while (set1.removeLast() || set2.removeLast());
                clearCsv(csvPath, "data1.csv");
                clearCsv(csvPath, "data2.csv");
                val = 40;
                val2 = 40;
                counter = 1;
            }
        });

        buttonCsvShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenLoadCSV();
            }
        });

        stats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenBarChart();
            }
        });

        mHandler.postDelayed(DataUpdate,500);
    }



    private ArrayList<Entry> dataValues1() {
        ArrayList<Entry> dataVals = new ArrayList<Entry>();
        return dataVals;
    }

    private boolean clearCsv(String path, String fileName) {
        File file = new File(path + fileName);
        return file.delete();
    }

    private void saveToCsv(String path, String counterString, String valueString, String fileName){
        try{
            File file = new File(path);
            file.mkdirs();
            CSVWriter csvWriter = new CSVWriter(new FileWriter(path + fileName,true));
            String[] row = new String[]{counterString, valueString};
            csvWriter.writeNext(row);
            csvWriter.close();

        } catch (IOException e) {
            Toast.makeText(MainActivity.this,"ERROR",Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void OpenLoadCSV(){
        Intent intent = new Intent(this, LoadCSV.class);
        startActivity(intent);
    }

    private void OpenBarChart(){
        Intent intent = new Intent(this, barChartMeanStd.class);
        startActivity(intent);
    }
}
