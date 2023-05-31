package com.example.tutorial3;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.FileNotFoundException;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;

import java.util.ArrayList;
import java.util.List;

public class barChartMeanStd extends AppCompatActivity {
    BarChart mpBar;
    Button toMain;
    Button openCSV;

    public barChartMeanStd() throws FileNotFoundException { }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bar_chart);

        mpBar = (BarChart) findViewById(R.id.bar_chart);
        toMain = (Button) findViewById(R.id.button_back);
        openCSV = (Button) findViewById(R.id.button_csv_show);

        toMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openMain(); }
        });

        openCSV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openCSVShow(); }
        });

        // read the data:
        ArrayList<String[]> csvData1 = LoadCSV.CsvRead("/sdcard/csv_dir/data1.csv");
        ArrayList<String[]> csvData2 = LoadCSV.CsvRead("/sdcard/csv_dir/data2.csv");
        ArrayList<Float> data1 = DataValues(csvData1);
        ArrayList<Float> data2 = DataValues(csvData2);

        // calculate mean and std:
        float m1 = mean(data1);
        float s1 = std(data1);
        float m2 = mean(data2);
        float s2 = std(data2);

        List<BarEntry> entries1 = new ArrayList<>();
        List<BarEntry> entries2 = new ArrayList<>();
        List<BarEntry> entries3 = new ArrayList<>();
        List<BarEntry> entries4 = new ArrayList<>();
        entries1.add(new BarEntry(0f, m1));
        entries2.add(new BarEntry(1f, s1));
        // gap of 2f
        entries3.add(new BarEntry(4f, m2));
        entries4.add(new BarEntry(5f, s2));

        BarDataSet bar1 = new BarDataSet(entries1, "Dataset 1 Mean");
        bar1.setColor(Color.RED);
        BarDataSet bar2 = new BarDataSet(entries2, "Dataset 1 Std");
        bar2.setColor(Color.GREEN);
        BarDataSet bar3 = new BarDataSet(entries3, "Dataset 2 Mean");
        bar3.setColor(Color.BLUE);
        BarDataSet bar4 = new BarDataSet(entries4, "Dataset 2 Std");
        bar4.setColor(Color.YELLOW);
        List<IBarDataSet> bars = new ArrayList<>();
        bars.add(bar1);
        bars.add(bar2);
        bars.add(bar3);
        bars.add(bar4);
        BarData data = new BarData(bars);
        data.setBarWidth(0.9f);  // set bar width
        mpBar.setData(data);
        String[] labels = new String[] {"Mean 1", "Std 1", "", "", "Mean 2", "Std 2"};
        mpBar.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        mpBar.setFitBars(true);  // make the x-axis fit exactly all bars
        mpBar.invalidate();  //refresh
    }

    protected static ArrayList<Float> DataValues(ArrayList<String[]> csvData){
        ArrayList<Float> dataVals = new ArrayList<Float>();
        for (int i = 0; i < csvData.size(); i++)
            dataVals.add( Float.parseFloat(csvData.get(i)[1]) );

        return dataVals;
    }

    public static float mean(ArrayList<Float> data) {
        float sum = 0;
        for (float d : data)
            sum += d;
        return sum / data.size();
    }

    public static float std(ArrayList<Float> data) {
        float sum = 0;
        for (float d : data)
            sum += d * d;
        return (float) (Math.sqrt(sum) / data.size());
    }

    private void openMain(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

    }

    private void openCSVShow(){
        Intent intent = new Intent(this, LoadCSV.class);
        startActivity(intent);

    }
}