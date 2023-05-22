package com.example.tutorial6;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected { False, Pending, True }

    private String deviceAddress;
    private SerialService service;

    // added experiment variables:
    private ArrayList<String[]> experimentData;
    private boolean started = false;
    private boolean stopped = false;
    private float startTime = 0;
    private float time = 0;
    private boolean running = false;
    private int steps;
    private String fileName;
    private final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    //    private TextView receiveText;
//    private TextView sendText;
    private TextView stepsText;
    private TextView fileNameText;
    private TextUtil.HexWatcher hexWatcher;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;

    LineChart mpLineChart;
    LineDataSet lineDataSetX;
    LineDataSet lineDataSetY;
    LineDataSet lineDataSetZ;
    ArrayList<ILineDataSet> dataSets = new ArrayList<>();
    LineData data;


    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");

    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if(service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation") // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try { getActivity().unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if(initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
//        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
//        receiveText.setTextColor(getResources().getColor(R.color.colorReceiveText)); // set as default color to reduce number of spans
//        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());
        experimentData = new ArrayList<>();

        //sendText = view.findViewById(R.id.send_text);
        stepsText = view.findViewById(R.id.steps_text);
        fileNameText = view.findViewById(R.id.filename_text);
        //hexWatcher = new TextUtil.HexWatcher(sendText);
        //hexWatcher.enable(hexEnabled);
        //sendText.addTextChangedListener(hexWatcher);
        //sendText.setHint(hexEnabled ? "HEX mode" : "");

        //View sendBtn = view.findViewById(R.id.send_btn);
        //sendBtn.setOnClickListener(v -> send(sendText.getText().toString()));

        mpLineChart = view.findViewById(R.id.line_chart);
        lineDataSetX =  new LineDataSet(emptyDataValues(), "ACC X");
        lineDataSetY =  new LineDataSet(emptyDataValues(), "ACC Y");
        lineDataSetZ =  new LineDataSet(emptyDataValues(), "ACC Z");
        lineDataSetX.setColor(Color.RED);
        lineDataSetX.setCircleColors(Color.RED);
        lineDataSetY.setColor(Color.BLUE);
        lineDataSetY.setCircleColors(Color.BLUE);
        lineDataSetZ.setColor(Color.GREEN);
        lineDataSetZ.setCircleColors(Color.GREEN);

        dataSets.add(lineDataSetX);
        dataSets.add(lineDataSetY);
        dataSets.add(lineDataSetZ);
        data = new LineData(dataSets);
        mpLineChart.setData(data);
        mpLineChart.invalidate();

        Button buttonClear = view.findViewById(R.id.button1);
        Button buttonCsvShow = view.findViewById(R.id.button2);
        Button buttonStart = view.findViewById(R.id.start_btn);
        Button buttonStop = view.findViewById(R.id.stop_btn);
        Button buttonSave = view.findViewById(R.id.save_btn);
        Button buttonReset = view.findViewById(R.id.reset_btn);
        Button checkBoxRunning = view.findViewById(R.id.cb_running);
        Button checkBoxWalking = view.findViewById(R.id.cb_walking);
        //View buttonSteps = view.findViewById(R.id.steps_btn);
        //buttonSteps.setOnClickListener(v -> setSteps(stepsText.getText().toString()));
        //View buttonFileName = view.findViewById(R.id.filename_btn);
        //buttonFileName.setOnClickListener(v -> setFileName(fileNameText.getText().toString()));


        buttonClear.setOnClickListener(v -> {
            Toast.makeText(getContext(),"Clear",Toast.LENGTH_SHORT).show();
            LineData data = mpLineChart.getData();
            for (int idx = 0; idx < 3; idx++) {
                ILineDataSet set = data.getDataSetByIndex(idx);
                data.getDataSetByIndex(idx);
                while(set.removeLast());
            }
        });

        buttonCsvShow.setOnClickListener(v -> OpenLoadCSV());

        buttonStart.setOnClickListener(v -> onClickStart());

        buttonStop.setOnClickListener(v -> onClickStop());

        buttonSave.setOnClickListener(v -> onClickSave());

        buttonReset.setOnClickListener(v -> onClickReset());

        checkBoxRunning.setOnClickListener(v -> onClickRunning());

        checkBoxWalking.setOnClickListener(v -> onclickWalking());

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
        menu.findItem(R.id.hex).setChecked(hexEnabled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
//            receiveText.setText("");
            return true;
        } else if (id == R.id.newline) {
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } else if (id == R.id.hex) {
            hexEnabled = !hexEnabled;
            //sendText.setText("");
            hexWatcher.enable(hexEnabled);
            //sendText.setHint(hexEnabled ? "HEX mode" : "");
            item.setChecked(hexEnabled);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Serial + UI
     */
    private String[] clean_str(String[] stringsArr){
        for (int i = 0; i < stringsArr.length; i++)  {
            stringsArr[i]=stringsArr[i].replaceAll(" ","");
        }


        return stringsArr;
    }
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(Objects.requireNonNull(getActivity()).getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }

    private void setSteps(String str) { steps = Integer.parseInt(str); }

    private void setFileName(String str) { fileName = str; }

    private void send(String str) {
        if(connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg;
            byte[] data;
            if(hexEnabled) {
                StringBuilder sb = new StringBuilder();
                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
                TextUtil.toHexString(sb, newline.getBytes());
                msg = sb.toString();
                data = TextUtil.fromHexString(msg);
            } else {
                msg = str;
                data = (str + newline).getBytes();
            }
            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//            receiveText.append(spn);
            service.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void receive(byte[] message) {
        System.out.println("Receive Call:");

        if(hexEnabled) {
//            receiveText.append(TextUtil.toHexString(message) + '\n');
        }
        else {
            String msg = new String(message);
            if(newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                // don't show CR as ^M if directly before LF
                String msg_to_save = msg.replace(TextUtil.newline_crlf, TextUtil.emptyString);
                msg_to_save = msg_to_save.replace(TextUtil.newline_lf, TextUtil.emptyString);
                msg_to_save = msg_to_save.replace("\r", TextUtil.emptyString);
                System.out.println(msg_to_save);
                // check message length
                if (msg_to_save.length() > 1) {
                    // split message string by ',' char
                    String[] parts = msg_to_save.split(",");
                    // function to trim blank spaces
                    parts = clean_str(parts);
                    System.out.println(Arrays.toString(parts));
                    float xValue = Float.parseFloat(parts[0]);
                    float yValue = Float.parseFloat(parts[1]);
                    float zValue = Float.parseFloat(parts[2]);
                    time = Float.parseFloat(parts[3]) / 1000;

                    // saving data to csv

                    // parse string values, in this case [0] is tmp & [1] is count (t)
                    String[] row = new String[]{String.valueOf(time - startTime),
                            parts[0],
                            parts[1],
                            parts[2]};
                    if (started) {
                        experimentData.add(row);
                    }

                    // add received values to line dataset for plotting the lineChart
                    data.addEntry(new Entry(time, xValue),0);
                    data.addEntry(new Entry(time, yValue),1);
                    data.addEntry(new Entry(time, zValue),2);
                    lineDataSetX.notifyDataSetChanged(); // let the data know a dataSet changed
                    lineDataSetY.notifyDataSetChanged(); // let the data know a dataSet changed
                    lineDataSetZ.notifyDataSetChanged(); // let the data know a dataSet changed
                    mpLineChart.notifyDataSetChanged(); // let the chart know it's data changed
                    mpLineChart.invalidate(); // refresh

                }

//                msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
//
//                // send msg to function that saves it to csv
//                // special handling if CR and LF come in separate fragments
//                if (pendingNewline && msg.charAt(0) == '\n') {
//                    Editable edt = receiveText.getEditableText();
//                    if (edt != null && edt.length() > 1)
//                        edt.replace(edt.length() - 2, edt.length(), "");
//                }
//                pendingNewline = msg.charAt(msg.length() - 1) == '\r';
            }
//            receiveText.append(msg + "\n");
//            receiveText.append(TextUtil.toCaretString(msg, newline.length() != 0));
        }
    }

    private void onClickStart() {
        startTime = time;
        started = true;
        Toast.makeText(getActivity(), "Started Recording!", Toast.LENGTH_SHORT).show();
    }

    private void onClickStop() {
        stopped = true;
        Toast.makeText(getActivity(), "Stopped Recording!", Toast.LENGTH_SHORT).show();
    }

    private void onClickRunning() {
        running = true;
    }
    private void onclickWalking() {
        running = false;
    }

    private void onClickSave() {
        if (!(started && stopped)) {
            Toast.makeText(getActivity(), "No Experiment Was Recorded", Toast.LENGTH_SHORT).show();
        }
        else {
            try {
                // create new csv unless file already exists
                fileName = fileNameText.getText().toString();
                steps = Integer.parseInt(stepsText.getText().toString());
                File file = new File("/sdcard/csv_dir/");
                file.mkdirs();
                String csv = "/sdcard/csv_dir/" + fileName + ".csv";
                CSVWriter csvWriter = new CSVWriter(new FileWriter(csv, true));
                csvWriter.writeNext(new String[]{"NAME:", fileName + ".csv"});
                csvWriter.writeNext(new String[]{"EXPERIMENT TIME:", formatter.format(new Date())});
                csvWriter.writeNext(new String[]{"ACTIVITY TYPE:", running ? "Running" : "Walking"});
                csvWriter.writeNext(new String[]{"COUNT OF ACTUAL STEPS:", String.valueOf(steps)});
                csvWriter.writeNext(new String[]{});
                csvWriter.writeNext(new String[]{"Time [sec]", "ACC X", "ACC Y", "ACC Z"});

                for (String[] row : experimentData) {
                    csvWriter.writeNext(row);
                }
                csvWriter.close();
                onClickReset();  // Reset the recording
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(getActivity(), "Saved Recording to " + fileName + ".csv", Toast.LENGTH_SHORT).show();
        }
    }

    private void onClickReset() {
        experimentData.clear();
        started = false;
        stopped = false;

    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        receiveText.append(spn);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        try {
            receive(data);}
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

    private ArrayList<Entry> emptyDataValues()
    {
        return new ArrayList<>();
    }

    private void OpenLoadCSV(){
        Intent intent = new Intent(getContext(), LoadCSV.class);
        intent.putExtra("fileName", fileName);
        startActivity(intent);
    }

}