package com.example.tutorial3;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import com.opencsv.CSVReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import java.util.List;


public class LoadCSV extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_load_csv);

        Button BackButton = (Button) findViewById(R.id.button_back);
        Button barChartButton = (Button) findViewById(R.id.button_bar_chart);
        LineChart lineChart = (LineChart) findViewById(R.id.line_chart);



        ArrayList<String[]> csvData1 = CsvRead("/sdcard/csv_dir/data1.csv");
        ArrayList<String[]> csvData2 = CsvRead("/sdcard/csv_dir/data2.csv");
        LineDataSet lineDataSet1 =  new LineDataSet(DataValues(csvData1),"Data Set 1");
        LineDataSet lineDataSet2 =  new LineDataSet(DataValues(csvData2),"Data Set 2");
        lineDataSet2.setColor(Color.GREEN);
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(lineDataSet1);
        dataSets.add(lineDataSet2);
        LineData data = new LineData(dataSets);
        lineChart.setData(data);
        lineChart.invalidate();




        BackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openMain(); }
        });
        barChartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openBarChart(); }
        });
    }

    private void openMain(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void openBarChart(){
        Intent intent = new Intent(this, barChartMeanStd.class);
        startActivity(intent);
    }

    protected static ArrayList<String[]> CsvRead(String path){
        ArrayList<String[]> CsvData = new ArrayList<>();
        try {
            File file = new File(path);
            CSVReader reader = new CSVReader(new FileReader(file));
            String[] nextLine;
            while((nextLine = reader.readNext()) != null) {
                if (nextLine != null)
                    CsvData.add(nextLine);
            }

        } catch (Exception ignored) {}

        return CsvData;
    }

    protected static ArrayList<Entry> DataValues(ArrayList<String[]> csvData){
        ArrayList<Entry> dataVals = new ArrayList<Entry>();
        for (int i = 0; i < csvData.size(); i++){
            dataVals.add(new Entry(i, Float.parseFloat(csvData.get(i)[1])));
        }
        return dataVals;
    }

}
