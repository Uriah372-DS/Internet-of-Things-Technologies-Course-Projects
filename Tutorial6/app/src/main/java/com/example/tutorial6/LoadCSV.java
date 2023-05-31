package com.example.tutorial6;


import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import com.opencsv.CSVReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.Objects;


public class LoadCSV extends AppCompatActivity {
    private LineChart lineChart;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_csv);
        Button BackButton = findViewById(R.id.button_back);
        lineChart = findViewById(R.id.line_chart);

        // retrieve fileName from main activity
        ArrayList<String> fileNames = readFileNames();
        System.out.println(fileNames.toArray().toString());

        Spinner spinner = findViewById(R.id.saved_experiments);

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>
                (this, android.R.layout.simple_spinner_item,
                        fileNames);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String fileName = (String) parent.getItemAtPosition(pos);
                HashMap<String, Object>  csvData = CsvRead("/sdcard/csv_dir/" + fileName + ".csv");
                LineDataSet lineDataSetX =  new LineDataSet((ArrayList<Entry>) csvData.get("ACC X"), "ACC X");
                LineDataSet lineDataSetY =  new LineDataSet((ArrayList<Entry>) csvData.get("ACC Y"), "ACC Y");
                LineDataSet lineDataSetZ =  new LineDataSet((ArrayList<Entry>) csvData.get("ACC Z"), "ACC Z");
                lineDataSetX.setColor(Color.RED);
                lineDataSetX.setCircleColors(Color.RED);
                lineDataSetY.setColor(Color.BLUE);
                lineDataSetY.setCircleColors(Color.BLUE);
                lineDataSetZ.setColor(Color.GREEN);
                lineDataSetZ.setCircleColors(Color.GREEN);

                ArrayList<ILineDataSet> dataSets = new ArrayList<>();
                dataSets.add(lineDataSetX);
                dataSets.add(lineDataSetY);
                dataSets.add(lineDataSetZ);
                LineData data = new LineData(dataSets);
                lineChart.setData(data);
                lineChart.invalidate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });


        BackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClickBack();
            }
        });
    }

    private ArrayList<String> readFileNames() {
        File folder = new File("/sdcard/csv_dir/");
        File[] listOfFiles = folder.listFiles();
        ArrayList<String> fNames = new ArrayList<>();
        if (listOfFiles == null)
            return fNames;

        for (int i = 0; i < Objects.requireNonNull(listOfFiles).length; i++) {
            if (listOfFiles[i].isFile()) {
                fNames.add(listOfFiles[i].getName().replace(".csv", ""));
            }
        }
        return fNames;
    }

    private void ClickBack() {
        finish();
    }

    private HashMap<String, Object> CsvRead(String path) {
        HashMap<String, Object> experimentData = new HashMap<>();
        ArrayList<Entry> accX = new ArrayList<>();
        ArrayList<Entry> accY = new ArrayList<>();
        ArrayList<Entry> accZ = new ArrayList<>();
        experimentData.put("ACC X", accX);
        experimentData.put("ACC Y", accY);
        experimentData.put("ACC Z", accZ);
        try {
            File file = new File(path);
            CSVReader reader = new CSVReader(new FileReader(file));
            String[] nextline;
            int rowNumber = 0;
            while((nextline = reader.readNext()) != null) {
                if(nextline != null) {
                    if (rowNumber < 3) {
                        experimentData.put(nextline[0], nextline[1]);
                    }
                    if (rowNumber == 3)
                        experimentData.put(nextline[0], Integer.parseInt(nextline[1]));
                    if (rowNumber >= 7) {
                        float t = Float.parseFloat(nextline[0]);
                        float xValue = Float.parseFloat(nextline[1]);
                        float yValue = Float.parseFloat(nextline[2]);
                        float zValue = Float.parseFloat(nextline[3]);
                        accX.add(new Entry(t, xValue));
                        accY.add(new Entry(t, yValue));
                        accZ.add(new Entry(t, zValue));
                    }
                }
                rowNumber++;
            }

        }catch (Exception ignored){}
        return experimentData;
    }

//    private ArrayList<Entry> DataValues(ArrayList<String[]> csvData){
//        ArrayList<Entry> dataVals = new ArrayList<Entry>();
//        for (int i = 0; i < csvData.size(); i++){
//
//            dataVals.add(new Entry(Integer.parseInt(csvData.get(i)[1]),
//                    Float.parseFloat(csvData.get(i)[0])));
//
//
//        }
//
//        return dataVals;
//    }
}
