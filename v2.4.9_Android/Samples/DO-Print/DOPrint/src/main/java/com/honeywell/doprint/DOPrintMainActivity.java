package com.honeywell.doprint;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import honeywell.connection.ConnectionBase;
import honeywell.connection.Connection_Bluetooth;
import honeywell.connection.Connection_TCP;
import honeywell.printer.DocumentDPL;
import honeywell.printer.DocumentDPL.ImageType;
import honeywell.printer.DocumentEZ;
import honeywell.printer.DocumentExPCL_LP;
import honeywell.printer.DocumentExPCL_PP;
import honeywell.printer.DocumentExPCL_PP.PaperWidth;
import honeywell.printer.DocumentLP;
import honeywell.printer.ParametersDPL;
import honeywell.printer.ParametersDPL.DoubleByteSymbolSet;
import honeywell.printer.ParametersEZ;
import honeywell.printer.ParametersExPCL_LP;
import honeywell.printer.ParametersExPCL_LP.BarcodeExPCL_LP;
import honeywell.printer.ParametersExPCL_LP.GS1DataBar;
import honeywell.printer.ParametersExPCL_PP;
import honeywell.printer.ParametersExPCL_PP.BarcodeExPCL_PP;
import honeywell.printer.ParametersExPCL_PP.RotationAngle;
import honeywell.printer.UPSMessage;
import honeywell.printer.configuration.dpl.AutoUpdate_DPL;
import honeywell.printer.configuration.dpl.AvalancheEnabler_DPL;
import honeywell.printer.configuration.dpl.BluetoothConfiguration_DPL;
import honeywell.printer.configuration.dpl.Fonts_DPL;
import honeywell.printer.configuration.dpl.MediaLabel_DPL;
import honeywell.printer.configuration.dpl.MemoryModules_DPL.FileInformation;
import honeywell.printer.configuration.dpl.Miscellaneous_DPL;
import honeywell.printer.configuration.dpl.NetworkGeneralSettings_DPL;
import honeywell.printer.configuration.dpl.NetworkWirelessSettings_DPL;
import honeywell.printer.configuration.dpl.PrintSettings_DPL;
import honeywell.printer.configuration.dpl.PrinterInformation_DPL;
import honeywell.printer.configuration.dpl.PrinterStatus_DPL;
import honeywell.printer.configuration.dpl.PrinterStatus_DPL.PrinterStatus;
import honeywell.printer.configuration.dpl.SensorCalibration_DPL;
import honeywell.printer.configuration.dpl.SerialPortConfiguration_DPL;
import honeywell.printer.configuration.dpl.SystemSettings_DPL;
import honeywell.printer.configuration.expcl.BatteryCondition_ExPCL;
import honeywell.printer.configuration.expcl.BluetoothConfiguration_ExPCL;
import honeywell.printer.configuration.expcl.GeneralStatus_ExPCL;
import honeywell.printer.configuration.expcl.MagneticCardData_ExPCL;
import honeywell.printer.configuration.expcl.MemoryStatus_ExPCL;
import honeywell.printer.configuration.expcl.PrinterOptions_ExPCL;
import honeywell.printer.configuration.expcl.PrintheadStatus_ExPCL;
import honeywell.printer.configuration.expcl.VersionInformation_ExPCL;
import honeywell.printer.configuration.ez.AvalancheSettings;
import honeywell.printer.configuration.ez.BatteryCondition;
import honeywell.printer.configuration.ez.BluetoothConfiguration;
import honeywell.printer.configuration.ez.FontData;
import honeywell.printer.configuration.ez.FontList;
import honeywell.printer.configuration.ez.FormatData;
import honeywell.printer.configuration.ez.FormatList;
import honeywell.printer.configuration.ez.GeneralConfiguration;
import honeywell.printer.configuration.ez.GeneralStatus;
import honeywell.printer.configuration.ez.GraphicData;
import honeywell.printer.configuration.ez.GraphicList;
import honeywell.printer.configuration.ez.IrDAConfiguration;
import honeywell.printer.configuration.ez.LabelConfiguration;
import honeywell.printer.configuration.ez.MagneticCardConfiguration;
import honeywell.printer.configuration.ez.MagneticCardData;
import honeywell.printer.configuration.ez.ManufacturingDate;
import honeywell.printer.configuration.ez.MemoryStatus;
import honeywell.printer.configuration.ez.PrinterOptions;
import honeywell.printer.configuration.ez.PrintheadStatus;
import honeywell.printer.configuration.ez.SerialNumber;
import honeywell.printer.configuration.ez.SmartCardConfiguration;
import honeywell.printer.configuration.ez.TCPIPStatus;
import honeywell.printer.configuration.ez.UpgradeData;
import honeywell.printer.configuration.ez.VersionInformation;

public class DOPrintMainActivity extends AppCompatActivity implements Runnable {

    //Keys to pass data to/from FileBrowseActivity
    static final String FOLDER_NAME_KEY = "com.honeywell.doprint.Folder_Name_Key";
    static final String FOLDER_PATH_KEY = "com.honeywell.doprint.Folder_Path_Key";

    //Keys to pass data to Connection Activity
    static final String CONNECTION_MODE_KEY = "com.honeywell.doprint.Connection_Mode_Key";
    static final String PRINTER_IPADDRESS_KEY = "com.honeywell.doprint.PRINTER_IPAddress_Key";
    static final String PRINTER_TCPIPPORT_KEY = "com.honeywell.doprint.PRINTER_TCPIPPort_Key";
    static final String BLUETOOTH_DEVICE_NAME_KEY = "com.honeywell.doprint.PRINTER_Bluetooth_Device_Name_Key";
    static final String BLUETOOTH_DEVICE_ADDR_KEY = "com.honeywell.doprint.PRINTER_Bluetooth_Device_Addr_Key";

    //Variable for folder content
    private String m_selectedPath;

    //Variable for Connection information
    private String m_printerIP = null;
    private String m_printerMAC = null;
    private int m_printerPort = 515;
    private String connectionType;
    ConnectionBase conn = null;
    private int m_printHeadWidth = 384;

    //Variable for connection status
    private String g_PrintStatusStr;

    ArrayAdapter<CharSequence> adapter = null;

    //array to contain the filenames inside a directory
    List<String> filesList = new ArrayList<>();

    static final int CONFIG_CONNECTION_REQUEST = 0; // for Connection Settings
    private static final int REQUEST_PICK_FILE = 1; //for File browsing

    //Document and Parameter Objects
    private DocumentEZ docEZ;
    private DocumentLP docLP;
    private DocumentDPL docDPL;
    private DocumentExPCL_LP docExPCL_LP;
    private DocumentExPCL_PP docExPCL_PP;

    private ParametersEZ paramEZ;
    private ParametersDPL paramDPL;
    private ParametersExPCL_LP paramExPCL_LP;
    private ParametersExPCL_PP paramExPCL_PP;
    // use to update the UI information.
    private Handler m_handler = new Handler(); // Main thread

    private String m_printerMode = "";
    private int selectedItemIndex = 0;

    //====UI Controls========//
    //Buttons
    Button m_browseButton;
    Button m_printButton;
    Button m_saveButton;
    Button m_configConnectionButton;
    RadioGroup m_performTaskRadioGroup;
    RadioButton m_printRadioButton;
    RadioButton m_queryRadioButton;

    //EditText
    TextView m_connectionInfoStatus;
    TextView m_actionTextView;

    //Spinners
    Spinner m_connectionSpinner;
    Spinner m_printerModeSpinner;
    Spinner m_printItemsSpinner;
    Spinner m_printHeadSpinner;

    byte[] printData = {0};

    // Configuration files to load
    String ApplicationConfigFilename = "applicationconfig.dat";

    DOPrintSettings g_appSettings = new DOPrintSettings("","", 0, "/", "", 0,0,0,0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doprint_main);

        if(getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowHomeEnabled(true);

        DOPrintSettings appSettings = ReadApplicationSettingFromFile();
        if(appSettings != null)
            g_appSettings = appSettings;

        //Get parameters from application settings
        m_printerIP = g_appSettings.getPrinterIP();
        m_printerMAC = g_appSettings.getPrinterMAC();
        m_printerPort = g_appSettings.getPrinterPort();
        m_selectedPath = g_appSettings.getSelectedFilePath();
        selectedItemIndex = g_appSettings.getSelectedItemIndex();

        //======Mapping UI controls from our activity xml===========//
        m_connectionInfoStatus= (TextView)findViewById(R.id.communication_status_information);
        m_connectionSpinner = (Spinner)findViewById(R.id.connection_spinner);
        m_configConnectionButton = (Button)findViewById(R.id.configConn_button);
        m_printerModeSpinner = (Spinner)findViewById(R.id.printer_mode_spinner);
        m_performTaskRadioGroup = (RadioGroup)findViewById(R.id.performTaskRadioGroup);
        m_printRadioButton= (RadioButton)findViewById(R.id.print_radioButton);
        m_queryRadioButton = (RadioButton)findViewById(R.id.query_radioButton);
        m_printItemsSpinner = (Spinner)findViewById(R.id.print_items_spinner);
        m_browseButton = (Button)findViewById(R.id.browse_button);
        m_printButton = (Button)findViewById(R.id.print_button);
        m_actionTextView = (TextView)findViewById(R.id.actionMode);
        m_saveButton = (Button)findViewById(R.id.saveSettings_button);
        m_printHeadSpinner = (Spinner)findViewById(R.id.printHeadSpinner);

        if(g_appSettings.getSelectedAction() == 0)
        {
            m_printRadioButton.setChecked(true);
            m_browseButton.setVisibility(View.VISIBLE);
            m_printButton.setText(R.string.print);
        }
        else if (g_appSettings.getSelectedAction() == 1)
        {
            m_queryRadioButton.setChecked(true);
            m_browseButton.setVisibility(View.GONE);
            m_printButton.setText(R.string.query);
        }
        m_connectionSpinner.setSelection(g_appSettings.getCommunicationMethod());
        m_printerModeSpinner.setSelection(g_appSettings.getSelectedModeIndex());
        m_printItemsSpinner.setSelection(selectedItemIndex);
        // ------------------------------------------------
        // Event handler when user select communication method
        // -------------------------------------------------
        m_connectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                connectionType = m_connectionSpinner.getSelectedItem().toString();

                //=======Display correct connection information when user selects connection=====//
                if (connectionType.equals("TCP/IP"))
                {
                    if (m_printerIP.length() == 0)
                    {
                        m_connectionInfoStatus.setText(R.string.connection_not_configured);
                    }
                    else {
                        String printerInfo = "Printer's IP Address/Port: "+ m_printerIP + ":"+ Integer.toString(m_printerPort);
                        m_connectionInfoStatus.setText(printerInfo);
                    }
                }

                else if(connectionType.equals("Bluetooth"))
                {
                    if (m_printerMAC.length() == 0)
                    {
                        m_connectionInfoStatus.setText(R.string.connection_not_configured);
                    }
                    else {
                        String printerInfo = "Printer's MAC Address: "+ m_printerMAC;
                        m_connectionInfoStatus.setText(printerInfo);
                    }
                }
                g_appSettings.setCommunicationMethod(m_connectionSpinner.getSelectedItemPosition());

            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

        // ------------------------------------------------
        // Handles when user presses connection config button
        // -------------------------------------------------
        m_configConnectionButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                //==================Open Connection Configuration Activity=======================================//
                Intent connSettingsIntent=new Intent("com.honeywell.doprint.ConnectionSettingsActivity");
                Spinner connectionSpinner = (Spinner)findViewById(R.id.connection_spinner);
                String connectionType = connectionSpinner.getSelectedItem().toString();
                connSettingsIntent.putExtra(CONNECTION_MODE_KEY, connectionType);

                connSettingsIntent.putExtra(PRINTER_IPADDRESS_KEY,m_printerIP);
                connSettingsIntent.putExtra(PRINTER_TCPIPPORT_KEY, m_printerPort);
                connSettingsIntent.putExtra(BLUETOOTH_DEVICE_ADDR_KEY, m_printerMAC);

                startActivityForResult(connSettingsIntent, CONFIG_CONNECTION_REQUEST);

            }
        });

        // ------------------------------------------------
        // Event handler when user select printer mode
        // -------------------------------------------------
        m_printerModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3)
            {
                //=======Display correct printer mode information when user selects connection=====//
                m_printerMode=m_printerModeSpinner.getSelectedItem().toString();
                g_appSettings.setSelectedModeIndex(m_printerModeSpinner.getSelectedItemPosition());

                String browsedItem = "";
                //Get the selected item from spinner
                if(m_printItemsSpinner.getSelectedItem() != null)
                {
                    //check if selected item is a item we browsed.
                    if(m_printItemsSpinner.getSelectedItem().toString().contains("/"))
                        browsedItem = m_printItemsSpinner.getSelectedItem().toString();
                }
                adapter = null;
                //Check which mode we are in
                switch (m_printerMode) {
                    case "EZ":
                        if (m_printRadioButton.isChecked()) {
                            CharSequence[] itemArray = getResources().getTextArray(R.array.print_list_EZ);
                            List<CharSequence> itemList = new ArrayList<>(Arrays.asList(itemArray));
                            adapter = new ArrayAdapter<>(DOPrintMainActivity.this, android.R.layout.simple_spinner_item, itemList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            m_printItemsSpinner.setAdapter(adapter);
                        } else if (m_queryRadioButton.isChecked()) {
                            CharSequence[] itemArray = getResources().getTextArray(R.array.query_list_Legacy);
                            List<CharSequence> itemList = new ArrayList<>(Arrays.asList(itemArray));
                            adapter = new ArrayAdapter<>(DOPrintMainActivity.this, android.R.layout.simple_spinner_item, itemList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            m_printItemsSpinner.setAdapter(adapter);
                        }
                        break;
                    case "LP":
                        if (m_printRadioButton.isChecked()) {
                            CharSequence[] itemArray = getResources().getTextArray(R.array.print_list_LP);
                            List<CharSequence> itemList = new ArrayList<>(Arrays.asList(itemArray));
                            adapter = new ArrayAdapter<>(DOPrintMainActivity.this, android.R.layout.simple_spinner_item, itemList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            m_printItemsSpinner.setAdapter(adapter);
                        } else if (m_queryRadioButton.isChecked()) {
                            CharSequence[] itemArray = getResources().getTextArray(R.array.query_list_Legacy);
                            List<CharSequence> itemList = new ArrayList<>(Arrays.asList(itemArray));
                            adapter = new ArrayAdapter<>(DOPrintMainActivity.this, android.R.layout.simple_spinner_item, itemList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            m_printItemsSpinner.setAdapter(adapter);
                        }
                        break;
                    case "DPL":
                        if (m_printRadioButton.isChecked()) {
                            CharSequence[] itemArray = getResources().getTextArray(R.array.print_list_DPL);
                            List<CharSequence> itemList = new ArrayList<>(Arrays.asList(itemArray));
                            adapter = new ArrayAdapter<>(DOPrintMainActivity.this, android.R.layout.simple_spinner_item, itemList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            m_printItemsSpinner.setAdapter(adapter);
                        } else if (m_queryRadioButton.isChecked()) {
                            CharSequence[] itemArray = getResources().getTextArray(R.array.query_list_DPL);
                            List<CharSequence> itemList = new ArrayList<>(Arrays.asList(itemArray));
                            adapter = new ArrayAdapter<>(DOPrintMainActivity.this, android.R.layout.simple_spinner_item, itemList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            m_printItemsSpinner.setAdapter(adapter);
                        }
                        break;
                    case "ExPCL_LP":
                        if (m_printRadioButton.isChecked()) {
                            CharSequence[] itemArray = getResources().getTextArray(R.array.print_list_ExPCL_LP);
                            List<CharSequence> itemList = new ArrayList<>(Arrays.asList(itemArray));
                            adapter = new ArrayAdapter<>(DOPrintMainActivity.this, android.R.layout.simple_spinner_item, itemList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            m_printItemsSpinner.setAdapter(adapter);
                        } else if (m_queryRadioButton.isChecked()) {
                            CharSequence[] itemArray = getResources().getTextArray(R.array.query_list_ExPCL);
                            List<CharSequence> itemList = new ArrayList<>(Arrays.asList(itemArray));
                            adapter = new ArrayAdapter<>(DOPrintMainActivity.this, android.R.layout.simple_spinner_item, itemList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            m_printItemsSpinner.setAdapter(adapter);
                        }
                        break;
                    case "ExPCL_PP":
                        if (m_printRadioButton.isChecked()) {
                            CharSequence[] itemArray = getResources().getTextArray(R.array.print_list_ExPCL_PP);
                            List<CharSequence> itemList = new ArrayList<>(Arrays.asList(itemArray));
                            adapter = new ArrayAdapter<>(DOPrintMainActivity.this, android.R.layout.simple_spinner_item, itemList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            m_printItemsSpinner.setAdapter(adapter);
                        } else if (m_queryRadioButton.isChecked()) {
                            CharSequence[] itemArray = getResources().getTextArray(R.array.query_list_ExPCL);
                            List<CharSequence> itemList = new ArrayList<>(Arrays.asList(itemArray));
                            adapter = new ArrayAdapter<>(DOPrintMainActivity.this, android.R.layout.simple_spinner_item, itemList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            m_printItemsSpinner.setAdapter(adapter);
                        }
                        break;
                }
                if (m_printRadioButton.isChecked()) {
                    g_appSettings.setSelectedAction(0);

                    //add file names
                    for (String fileName : filesList) {
                        adapter.add(fileName);
                    }
                    adapter.notifyDataSetChanged();
                }
                else if(m_queryRadioButton.isChecked())
                {
                    g_appSettings.setSelectedAction(1);
                }
                //if its a browsed item, get the index of it
                if (browsedItem.length() != 0)
                {
                    if(adapter != null)
                        g_appSettings.setSelectedItemIndex(adapter.getPosition(browsedItem));
                }
                //check if selected item index is greater than item list
                if(selectedItemIndex > m_printItemsSpinner.getCount()-1)
                {
                    //if so, set selected item to the first item
                    g_appSettings.setSelectedItemIndex(m_printItemsSpinner.getSelectedItemPosition());
                    m_printItemsSpinner.setSelection(g_appSettings.getSelectedItemIndex());
                }
                else {
                    selectedItemIndex = g_appSettings.getSelectedItemIndex();
                    m_printItemsSpinner.setSelection(selectedItemIndex);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                m_printerMode = m_printerModeSpinner.getSelectedItem().toString();
                g_appSettings.setSelectedModeIndex(m_printerModeSpinner.getSelectedItemPosition());
                m_printItemsSpinner.setSelection(g_appSettings.getSelectedItemIndex());

                if (m_printRadioButton.isChecked()) {
                    g_appSettings.setSelectedAction(0);

                    //add file names
                    for (String fileName : filesList) {
                        adapter.add(fileName);
                    }
                    adapter.notifyDataSetChanged();
                }
                else if(m_queryRadioButton.isChecked())
                {
                    g_appSettings.setSelectedAction(1);
                }
            }
        });
        // --------------------------------------------------
        // Event handler when user selects print head width
        // --------------------------------------------------
        m_printHeadSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                String value = (String)m_printHeadSpinner.getSelectedItem();
                m_printHeadWidth = Integer.parseInt(value.substring(0, 3));
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                String value = (String)m_printHeadSpinner.getSelectedItem();
                m_printHeadWidth = Integer.parseInt(value.substring(0, 3));
            }
        });
        // --------------------------------------------------
        // Event handler when user selects an item from list
        // --------------------------------------------------
        m_printItemsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                selectedItemIndex = m_printItemsSpinner.getSelectedItemPosition();
                g_appSettings.setSelectedItemIndex(selectedItemIndex);
                m_printItemsSpinner.setSelection(selectedItemIndex);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                selectedItemIndex = m_printItemsSpinner.getSelectedItemPosition();
                g_appSettings.setSelectedItemIndex(selectedItemIndex);
                m_printItemsSpinner.setSelection(selectedItemIndex);
            }
        });
        // ------------------------------------------------
        // Event handler when user select what task to perform(eg. print or query
        // -------------------------------------------------
        m_performTaskRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                m_printerMode = m_printerModeSpinner.getSelectedItem().toString();
                adapter = null;

                //Hide or show browse button
                if (m_printRadioButton.isChecked()) {
                    m_browseButton.setVisibility(View.VISIBLE);
                    m_actionTextView.setText(R.string.print_items);
                    m_printButton.setText(R.string.print);
                }
                else if (m_queryRadioButton.isChecked()){
                    m_browseButton.setVisibility(View.GONE);
                    m_actionTextView.setText(R.string.query_items);
                    m_printButton.setText(R.string.query);
                }
                //Check which mode we are in
                switch (m_printerMode) {
                    case "EZ":
                        if (m_printRadioButton.isChecked()) {
                            CharSequence[] itemArray = getResources().getTextArray(R.array.print_list_EZ);
                            List<CharSequence> itemList = new ArrayList<>(Arrays.asList(itemArray));
                            adapter = new ArrayAdapter<>(DOPrintMainActivity.this, android.R.layout.simple_spinner_item, itemList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            m_printItemsSpinner.setAdapter(adapter);
                        } else if (m_queryRadioButton.isChecked()) {
                            CharSequence[] itemArray = getResources().getTextArray(R.array.query_list_Legacy);
                            List<CharSequence> itemList = new ArrayList<>(Arrays.asList(itemArray));
                            adapter = new ArrayAdapter<>(DOPrintMainActivity.this, android.R.layout.simple_spinner_item, itemList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            m_printItemsSpinner.setAdapter(adapter);
                        }
                        break;
                    case "LP":
                        if (m_printRadioButton.isChecked()) {
                            CharSequence[] itemArray = getResources().getTextArray(R.array.print_list_LP);
                            List<CharSequence> itemList = new ArrayList<>(Arrays.asList(itemArray));
                            adapter = new ArrayAdapter<>(DOPrintMainActivity.this, android.R.layout.simple_spinner_item, itemList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            m_printItemsSpinner.setAdapter(adapter);
                        } else if (m_queryRadioButton.isChecked()) {
                            CharSequence[] itemArray = getResources().getTextArray(R.array.query_list_Legacy);
                            List<CharSequence> itemList = new ArrayList<>(Arrays.asList(itemArray));
                            adapter = new ArrayAdapter<>(DOPrintMainActivity.this, android.R.layout.simple_spinner_item, itemList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            m_printItemsSpinner.setAdapter(adapter);
                        }
                        break;
                    case "DPL":
                        if (m_printRadioButton.isChecked()) {
                            CharSequence[] itemArray = getResources().getTextArray(R.array.print_list_DPL);
                            List<CharSequence> itemList = new ArrayList<>(Arrays.asList(itemArray));
                            adapter = new ArrayAdapter<>(DOPrintMainActivity.this, android.R.layout.simple_spinner_item, itemList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            m_printItemsSpinner.setAdapter(adapter);
                        } else if (m_queryRadioButton.isChecked()) {
                            CharSequence[] itemArray = getResources().getTextArray(R.array.query_list_DPL);
                            List<CharSequence> itemList = new ArrayList<>(Arrays.asList(itemArray));
                            adapter = new ArrayAdapter<>(DOPrintMainActivity.this, android.R.layout.simple_spinner_item, itemList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            m_printItemsSpinner.setAdapter(adapter);
                        }
                        break;
                    case "ExPCL_LP":
                        if (m_printRadioButton.isChecked()) {
                            CharSequence[] itemArray = getResources().getTextArray(R.array.print_list_ExPCL_LP);
                            List<CharSequence> itemList = new ArrayList<>(Arrays.asList(itemArray));
                            adapter = new ArrayAdapter<>(DOPrintMainActivity.this, android.R.layout.simple_spinner_item, itemList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            m_printItemsSpinner.setAdapter(adapter);
                        } else if (m_queryRadioButton.isChecked()) {
                            CharSequence[] itemArray = getResources().getTextArray(R.array.query_list_ExPCL);
                            List<CharSequence> itemList = new ArrayList<>(Arrays.asList(itemArray));
                            adapter = new ArrayAdapter<>(DOPrintMainActivity.this, android.R.layout.simple_spinner_item, itemList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            m_printItemsSpinner.setAdapter(adapter);
                        }
                        break;
                    case "ExPCL_PP":
                        if (m_printRadioButton.isChecked()) {
                            CharSequence[] itemArray = getResources().getTextArray(R.array.print_list_ExPCL_PP);
                            List<CharSequence> itemList = new ArrayList<>(Arrays.asList(itemArray));
                            adapter = new ArrayAdapter<>(DOPrintMainActivity.this, android.R.layout.simple_spinner_item, itemList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            m_printItemsSpinner.setAdapter(adapter);
                        } else if (m_queryRadioButton.isChecked()) {
                            CharSequence[] itemArray = getResources().getTextArray(R.array.query_list_ExPCL);
                            List<CharSequence> itemList = new ArrayList<>(Arrays.asList(itemArray));
                            adapter = new ArrayAdapter<>(DOPrintMainActivity.this, android.R.layout.simple_spinner_item, itemList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            m_printItemsSpinner.setAdapter(adapter);
                        }
                        break;
                }
                if (m_printRadioButton.isChecked()) {
                    g_appSettings.setSelectedAction(0);

                    //add file names
                    for (String fileName : filesList) {
                        adapter.add(fileName);
                    }
                    adapter.notifyDataSetChanged();
                }
                else if(m_queryRadioButton.isChecked())
                {
                    g_appSettings.setSelectedAction(1);
                }
                //if the saved index is larger than the list it self
                if(selectedItemIndex > m_printItemsSpinner.getCount()-1)
                {
                    selectedItemIndex = m_printItemsSpinner.getSelectedItemPosition();
                    g_appSettings.setSelectedItemIndex(selectedItemIndex);
                }
                m_printItemsSpinner.setSelection(selectedItemIndex);
            }
        });

        //--------------------------------------------------------------------------
        //Handle Browse Button Click - Browse file content/image path into data to print.
        //----------------------------------------------------------------------------
        m_browseButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                //==========Start file browsing activity==================//
                Intent intent = new Intent("com.honeywell.doprint.FileBrowseActivity");
                startActivityForResult(intent,REQUEST_PICK_FILE);
            }
        });

        //--------------------------------------
        //Handle Print/Send Function
        //--------------------------------------
        m_printButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    printData = new byte[]{0};
                    selectedItemIndex = m_printItemsSpinner.getSelectedItemPosition();
                    docDPL = new DocumentDPL();
                    docEZ = new DocumentEZ("MF204");
                    docLP = new DocumentLP("!");
                    docExPCL_LP = new DocumentExPCL_LP(3);
                    docExPCL_PP = new DocumentExPCL_PP(PaperWidth.PaperWidth_384);

                    paramEZ = new ParametersEZ();
                    paramDPL = new ParametersDPL();
                    paramExPCL_LP = new ParametersExPCL_LP();
                    paramExPCL_PP = new ParametersExPCL_PP();
                    //if we are printing
                    if (m_printRadioButton.isChecked()) {
                        //Checks current Mode
                        switch (m_printerMode) {
                            case "EZ":
                                //3-in sample
                                if (selectedItemIndex == 0) {
                                    //=============GENERATING RECEIPT====================================//
                                    docEZ.writeText("For", 1, 200);

                                    //Bold delivery
                                    paramEZ.setIsBold(true);
                                    docEZ.writeText("Delivery", 1, 240, paramEZ);

                                    //print image on same Delivery line
                                    docEZ.writeImage("DOLGO", 1, 350);

                                    docEZ.writeText("Customer Code: ", 50, 1);

                                    //Use italic font
                                    paramEZ.setFont("ZP96P");
                                    docEZ.writeText("00146", 50, 150, paramEZ);

                                    docEZ.writeText("Address: Manila", 75, 1);
                                    docEZ.writeText("Tin No.: 27987641", 100, 1);
                                    docEZ.writeText("Area Code: PN1-0004", 125, 1);
                                    docEZ.writeText("Business Style: SUPERMARKET A", 150, 1);

                                    docEZ.writeText("PRODUCT CODE  PRODUCT DESCRIPTION         QTY.  Delivr.", 205, 1);
                                    docEZ.writeText("------------  --------------------------  ----  -------", 230, 1);
                                    docEZ.writeText("    111       Wht Bread Classic 400g       51      51  ", 255, 1);
                                    docEZ.writeText("    112       Clsc Wht Bread 600g          77      77  ", 280, 1);
                                    docEZ.writeText("    113       Wht Bread Clsc 600g          153     25  ", 305, 1);
                                    docEZ.writeText("    121       H Fiber Wheat Bread 600g     144     77  ", 330, 1);
                                    docEZ.writeText("    122       H Fiber Wheat Bread 400g     112     36  ", 355, 1);
                                    docEZ.writeText("    123       H Calcium Loaf 400g          81      44  ", 380, 1);
                                    docEZ.writeText("    211       California Raisin Loaf       107     44  ", 405, 1);
                                    docEZ.writeText("    212       Chocolate Chip Loaf          159     102 ", 430, 1);
                                    docEZ.writeText("    213       Dbl Delights(Ube & Chse)     99      80  ", 455, 1);
                                    docEZ.writeText("    214       Dbl Delights(Choco & Mocha)  167     130 ", 480, 1);
                                    docEZ.writeText("    215       Mini Wonder Ube Cheese       171     179 ", 505, 1);
                                    docEZ.writeText("    216       Mini Wonder Ube Mocha        179     100 ", 530, 1);
                                    docEZ.writeText("  ", 580, 1);
                                    printData = docEZ.getDocumentData();
                                    //======================================================================//
                                }

                                //4-in sample
                                else if (selectedItemIndex == 1) {
                                    docEZ.writeText("For Delivery", 1, 300);
                                    docEZ.writeText("Customer Code: 00146", 50, 1);
                                    docEZ.writeText("Address: Manila", 75, 1);
                                    docEZ.writeText("Tin No.: 27987641", 100, 1);
                                    docEZ.writeText("Area Code: PN1-0004", 125, 1);
                                    docEZ.writeText("Business Style: SUPERMARKET A", 150, 1);

                                    docEZ.writeText("PRODUCT CODE      PRODUCT DESCRIPTION             QTY.    Delivered ", 205, 1);
                                    docEZ.writeText("------------      --------------------------      ----    ----------", 230, 1);
                                    docEZ.writeText("    111           Wht Bread Classic 400g           51          51   ", 255, 1);
                                    docEZ.writeText("    112           Clsc Wht Bread 600g              77          77   ", 280, 1);
                                    docEZ.writeText("    113           Wht Bread Clsc 600g              153         25   ", 305, 1);
                                    docEZ.writeText("    121           H Fiber Wheat Bread 600g         144         77   ", 330, 1);
                                    docEZ.writeText("    122           H Fiber Wheat Bread 400g         112         36   ", 355, 1);
                                    docEZ.writeText("    123           H Calcium Loaf 400g              81          44   ", 380, 1);
                                    docEZ.writeText("    211           California Raisin Loaf           107         44   ", 405, 1);
                                    docEZ.writeText("    212           Chocolate Chip Loaf              159         102  ", 430, 1);
                                    docEZ.writeText("    213           Dbl Delights(Ube & Chse)         99          80   ", 455, 1);
                                    docEZ.writeText("    214           Dbl Delights(Choco & Mocha)      167         130  ", 480, 1);
                                    docEZ.writeText("    215           Mini Wonder Ube Cheese           171         179  ", 505, 1);
                                    docEZ.writeText("    216           Mini Wonder Ube Mocha            179         100  ", 530, 1);
                                    docEZ.writeText("  ", 580, 1);
                                    printData = docEZ.getDocumentData();
                                }
                                //Barcode Sample
                                else if (selectedItemIndex == 2) {
                                    paramEZ.setHorizontalMultiplier(1);
                                    paramEZ.setVerticalMultiplier(2);

                                    //write GS1 barcodes with 2d Composite data
                                    int pixelMult = 3;

                                    docEZ.writeText("GS1 Barcode", 1, 1);
                                    docEZ.writeBarCodeGS1DataBar("GSONE", "123456789", "123", pixelMult, pixelMult, 1, 1, 22, 30, 1, paramEZ);

                                    docEZ.writeText("GS1 Truncated", 330, 1);
                                    docEZ.writeBarCodeGS1DataBar("GS1TR", "123456789", "123", pixelMult, pixelMult, 1, 1, 22, 360, 1, paramEZ);

                                    docEZ.writeText("GS1 Limited", 530, 1);
                                    docEZ.writeBarCodeGS1DataBar("GS1LM", "123456789", "123", pixelMult, pixelMult, 1, 1, 22, 560, 1, paramEZ);

                                    docEZ.writeText("GS1 Stacked", 730, 1);
                                    docEZ.writeBarCodeGS1DataBar("GS1ST", "123456789", "123", pixelMult, pixelMult, 1, 1, 22, 760, 1, paramEZ);


                                    docEZ.writeText("GS1 Stacked Omnidirection", 930, 1);
                                    docEZ.writeBarCodeGS1DataBar("GS1SO", "123456789", "123", pixelMult, pixelMult, 1, 1, 22, 960, 1, paramEZ);

                                    docEZ.writeText("GS1 Expanded", 1530, 1);
                                    docEZ.writeBarCodeGS1DataBar("GS1EX", "ABCDEFGHIJKL", "helloWorld!123", pixelMult, 2 * pixelMult, 1, 1, 4, 1560, 1, paramEZ);

                                    paramEZ.setHorizontalMultiplier(2);
                                    paramEZ.setVerticalMultiplier(10);
                                    //Interleave 2of 5 barcode ratio 2:1
                                    docEZ.writeText("Interleave 2of5 Barcode ratio 2:1", 2230, 1);
                                    docEZ.writeBarCode("BCI25", "0123456789", 2260, 1, paramEZ);

                                    //barcode 128
                                    docEZ.writeText("Barcode 128", 2330, 1);
                                    docEZ.writeBarCode("BC128", "00010203040506070809", 2360, 1, paramEZ);

                                    //barcode EAN 128
                                    docEZ.writeText("EAN 128", 2430, 1);
                                    docEZ.writeBarCode("EN128", "00010203040506070809", 2460, 1, paramEZ);

                                    //Code 39 barcodes
                                    docEZ.writeText("Code 39 Barcodes", 2530, 1);
                                    docEZ.writeBarCode("BC39N", "0123456789", 2560, 1, paramEZ);
                                    docEZ.writeBarCode("BC39W", "0123456789", 2660, 1, paramEZ);

                                    //Code 93 barcode
                                    docEZ.writeText("Code 93", 2730, 1);
                                    docEZ.writeBarCode("BC093", "0123456789", 2760, 1, paramEZ);

                                    //Codabar
                                    docEZ.writeText("CODABAR", 2830, 1);
                                    docEZ.writeBarCode("COBAR", "00010203040506070809", 2860, 1, paramEZ);

                                    //8 digit europe art num
                                    docEZ.writeText("8 DIGIT EUROPE ART NUM", 2930, 1);
                                    docEZ.writeBarCode("EAN08", "0123456", 2960, 1, paramEZ);

                                    //13 digit europ art num
                                    docEZ.writeText("13 DIGIT Europe Art Num", 3030, 1);
                                    docEZ.writeBarCode("EAN13", "000123456789", 3060, 1, paramEZ);

                                    //INTLV 2of5
                                    docEZ.writeText("Interleaved 2of5", 3130, 1);
                                    docEZ.writeBarCode("I2OF5", "0123456789", 3160, 1, paramEZ);

                                    //PDF417Co
                                    docEZ.writeText("PDF417", 3230, 1);
                                    docEZ.writeBarCodePDF417("00010203040506070809", 3260, 1, 2, 1, paramEZ);

                                    //Plessy
                                    docEZ.writeText("Plessy", 3350, 1);
                                    docEZ.writeBarCode("PLESY", "8052", 3380, 1, paramEZ);

                                    //UPC-A
                                    docEZ.writeText("UPC-A", 3450, 1);
                                    docEZ.writeBarCode("UPC-A", "01234567890", 3480, 1, paramEZ);

                                    //UPC-E
                                    docEZ.writeText("UPC-E", 3550, 1);
                                    docEZ.writeBarCode("UPC-E", "0123456", 3580, 1, paramEZ);

                                    paramEZ.setHorizontalMultiplier(10);
                                    paramEZ.setVerticalMultiplier(1);
                                    //QR
                                    docEZ.writeText("QR Barcode Manual Formating", 3650, 1);
                                    docEZ.writeBarCodeQRCode("N0123456789,B0004(&#),QR//BARCODE", 2, 9, 1, 3680, 1, paramEZ);

                                    docEZ.writeText("QR Barcode Auto Formatting 1", 3950, 1);
                                    docEZ.writeBarCodeQRCode("0123456789012345678901234567890123456789", 2, 9, 0, 3980, 1, paramEZ);

                                    paramEZ.setHorizontalMultiplier(8);
                                    docEZ.writeText("QR Barcode Auto Formatting 2", 4250, 1);
                                    docEZ.writeBarCodeQRCode("0123456789ABCDE", 2, 9, 0, 4280, 1, paramEZ);

                                    //Aztec
                                    docEZ.writeText("Aztec", 4550, 1);
                                    docEZ.writeBarCodeAztec("Code 2D!", 104, 4580, 1, paramEZ);
                                    docEZ.writeText("", 4500, 1);
                                    printData = docEZ.getDocumentData();

                                }
                                //User selected an unpredefine item(eg from browsing file)
                                else {
                                    String selectedItem = (String) m_printItemsSpinner.getSelectedItem();
                                    Bitmap anImage = null;
                                    //Check if item is an image
                                    String[] okFileExtensions = new String[]{".jpg", ".png", ".gif", ".jpeg", ".bmp", ".tif", ".tiff", ".pcx"};
                                    for (String extension : okFileExtensions) {
                                        if (selectedItem.toLowerCase().endsWith(extension)) {
                                            anImage = BitmapFactory.decodeFile(selectedItem);
                                            break;
                                        }
                                    }
                                    //selected item is not an image file
                                    if (selectedItem.toLowerCase(Locale.US).endsWith(".pdf")) {
                                        docLP.writePDF(selectedItem, m_printHeadWidth);
                                        printData = docLP.getDocumentData();
                                    }
                                    //selected item is not an image file
                                    else if (anImage == null) {
                                        File file = new File(selectedItem);
                                        byte[] readBuffer = new byte[(int) file.length()];
                                        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                                        if(inputStream.read(readBuffer) == 0)
                                            throw new Exception("Unable to read file or file is empty.");
                                        inputStream.close();
                                        printData = readBuffer;
                                    } else {
                                        DisplayPrintingStatusMessage("Processing image..");
                                        docLP.writeImage(anImage, m_printHeadWidth);
                                        printData = docLP.getDocumentData();
                                    }
                                }
                                break;
                            //for LP
                            case "LP":
                                //3-inch sample to generate
                                if (selectedItemIndex == 0) {
                                    //docLP.setCharacterRemap(DocumentLP.CharacterSet.Spain);
                                    docLP.writeText("                   For Delivery");
                                    docLP.writeText(" ");
                                    docLP.writeText("Customer Code: 00146");
                                    docLP.writeText("Address: Manila");
                                    docLP.writeText("Tin No.: 27987641");
                                    docLP.writeText("Area Code: PN1-0004");
                                    docLP.writeText("Business Style: SUPERMARKET A");
                                    docLP.writeText(" ");
                                    docLP.writeText("PRODUCT CODE   PRODUCT DESCRIPTION          QTY.  Delivr.");
                                    docLP.writeText("------------   --------------------------   ----  -------");
                                    docLP.writeText("    111        Wht Bread Classic 400g        51     51   ");
                                    docLP.writeText("    112        Clsc Wht Bread 600g           77     77   ");
                                    docLP.writeText("    113        Wht Bread Clsc 600g           153    25   ");
                                    docLP.writeText("    121        H Fiber Wheat Bread 600g      144    77   ");
                                    docLP.writeText("    122        H Fiber Wheat Bread 400g      112    36   ");
                                    docLP.writeText("    123        H Calcium Loaf 400g           81     44   ");
                                    docLP.writeText("    211        California Raisin Loaf        107    44   ");
                                    docLP.writeText("    212        Chocolate Chip Loaf           159    102  ");
                                    docLP.writeText("    213        Dbl Delights(Ube & Chse)      99     80   ");
                                    docLP.writeText("    214        Dbl Delights(Choco & Mocha)   167    130  ");
                                    docLP.writeText("    215        Mini Wonder Ube Cheese        171    79   ");
                                    docLP.writeText("    216        Mini Wonder Ube Mocha         179    100  ");
                                    docLP.writeText("  ");
                                    docLP.writeText("  ");
                                    printData = docLP.getDocumentData();
                                }
                                //4-inch sample to generate
                                else if (selectedItemIndex == 1) {
                                    docLP.writeText("                            For Delivery");
                                    docLP.writeText(" ");
                                    docLP.writeText("Customer Code: 00146");
                                    docLP.writeText("Address: Manila");
                                    docLP.writeText("Tin No.: 27987641");
                                    docLP.writeText("Area Code: PN1-0004");
                                    docLP.writeText("Business Style: SUPERMARKET A");
                                    docLP.writeText(" ");
                                    docLP.writeText("PRODUCT CODE         PRODUCT DESCRIPTION          QTY.    Delivered");
                                    docLP.writeText("------------      --------------------------      ----    ---------- ");
                                    docLP.writeText("    111           Wht Bread Classic 400g           51         51     ");
                                    docLP.writeText("    112           Clsc Wht Bread 600g              77         77     ");
                                    docLP.writeText("    113           Wht Bread Clsc 600g              153        25     ");
                                    docLP.writeText("    121           H Fiber Wheat Bread 600g         144        77     ");
                                    docLP.writeText("    122           H Fiber Wheat Bread 400g         112        36     ");
                                    docLP.writeText("    123           H Calcium Loaf 400g              81         44     ");
                                    docLP.writeText("    211           California Raisin Loaf           107        44     ");
                                    docLP.writeText("    212           Chocolate Chip Loaf              159        102    ");
                                    docLP.writeText("    213           Dbl Delights(Ube & Chse)         99         80     ");
                                    docLP.writeText("    214           Dbl Delights(Choco & Mocha)      167        130    ");
                                    docLP.writeText("    215           Mini Wonder Ube Cheese           171        179    ");
                                    docLP.writeText("    216           Mini Wonder Ube Mocha            179        100    ");
                                    docLP.writeText("  ");
                                    printData = docLP.getDocumentData();
                                }
                                //Print Image
                                else if (selectedItemIndex == 2) {
                                    DisplayPrintingStatusMessage("Processing image..");
                                    Bitmap anImage = BitmapFactory.decodeStream(getAssets().open("dologo.png"));

                                    docLP.writeImage(anImage, m_printHeadWidth);
                                    printData = docLP.getDocumentData();
                                }
                                //User selected an item not predefined on list(from browsing file)
                                else {
                                    String selectedItem = (String) m_printItemsSpinner.getSelectedItem();
                                    Bitmap anImage = null;
                                    //Check if item is an image
                                    String[] okFileExtensions = new String[]{".jpg", ".png", ".gif", ".jpeg", ".bmp", ".tif", ".tiff", ".pcx"};
                                    for (String extension : okFileExtensions) {
                                        if (selectedItem.toLowerCase().endsWith(extension)) {
                                            anImage = BitmapFactory.decodeFile(selectedItem);
                                            break;
                                        }
                                    }
                                    //selected item is not an image file
                                    if (selectedItem.toLowerCase(Locale.US).endsWith(".pdf")) {
                                        docLP.writePDF(selectedItem, m_printHeadWidth);
                                        printData = docLP.getDocumentData();
                                    }
                                    //selected item is not an image file
                                    else if (anImage == null) {
                                        File file = new File(selectedItem);
                                        byte[] readBuffer = new byte[(int) file.length()];
                                        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                                        if (inputStream.read(readBuffer) == 0)
                                            throw new Exception("Unable to read file or file is empty.");
                                        inputStream.close();
                                        printData = readBuffer;
                                    } else {
                                        DisplayPrintingStatusMessage("Processing image..");
                                        docLP.writeImage(anImage, m_printHeadWidth);
                                        printData = docLP.getDocumentData();
                                    }
                                }
                                break;
                            //for EXPCL(Apex Printers)
                            case "ExPCL_LP":
//                            boolean TEST_PAPER_ADVANCE = false;

                                // TEXT SAMPLES
                                if (selectedItemIndex == 0) {

                                    docExPCL_LP.writeText("Hello World I am a printing sample");
                                    paramExPCL_LP.setFontIndex(5);
                                    docExPCL_LP.writeText("Hello World I am a printing sample (Font - K5)", paramExPCL_LP);
                                    paramExPCL_LP.setFontIndex(3);
                                    paramExPCL_LP.setIsBold(true);
                                    docExPCL_LP.writeText("Hello World I am a printing sample (Bold)", paramExPCL_LP);
                                    paramExPCL_LP.setIsBold(false);
                                    paramExPCL_LP.setIsInverse(true);
                                    docExPCL_LP.writeText("Hello World I am a printing sample (White On Black)", paramExPCL_LP);
                                    paramExPCL_LP.setIsInverse(false);
                                    paramExPCL_LP.setIsPCLineDrawCharSet(true);
                                    docExPCL_LP.writeText("Hello World I am a printing sample (PC Line Draw)", paramExPCL_LP);
                                    for (int i = 179; i < 256; i++) {
                                        docExPCL_LP.writeTextPartial(String.valueOf((char) i), paramExPCL_LP);
                                    }
                                    paramExPCL_LP.setIsPCLineDrawCharSet(false);
                                    docExPCL_LP.writeText("Hello World I am a printing sample (International)", paramExPCL_LP);
                                    for (int i = 179; i < 256; i++) {
                                        docExPCL_LP.writeTextPartial(String.valueOf((char) i), paramExPCL_LP);
                                    }
                                    docExPCL_LP.writeText("", paramExPCL_LP);
                                    docExPCL_LP.writeTextPartial("one ");
                                    docExPCL_LP.writeTextPartial("two ");
                                    docExPCL_LP.writeTextPartial("three ");
                                    docExPCL_LP.writeText("<CR>");
                                    paramExPCL_LP.setIsRightToLeftTextDirection(true);
                                    docExPCL_LP.writeText("Hello World I am a printing sample (Right-To-Left Text Direction)", paramExPCL_LP);
                                    paramExPCL_LP.setIsRightToLeftTextDirection(false);

                                    paramExPCL_LP.setIsUnderline(true);
                                    docExPCL_LP.writeText("Hello World I am a printing sample (Underline)", paramExPCL_LP);
                                    paramExPCL_LP.setIsUnderline(false);
                                    paramExPCL_LP.setPrintContrastLevel(6);
                                    docExPCL_LP.writeText("Hello World I am a printing sample (Contrast = 6)", paramExPCL_LP);
                                    paramExPCL_LP.setPrintContrastLevel(2);
                                    paramExPCL_LP.setLineSpacing((byte) 30);
                                    docExPCL_LP.writeText("Hello World I am a printing sample (Line Spacing = 30)", paramExPCL_LP);
                                    paramExPCL_LP.setLineSpacing((byte) 3);
                                    paramExPCL_LP.setVerticalTabHeight((byte) 150);

                                    String data = String.format(Locale.getDefault(), "Tab0%cTab1%cTab2%cHello World I am a printing sample (Vertical Tab = 50)", 11, 11, 11);
                                    docExPCL_LP.writeText(data, paramExPCL_LP);
                                    //paramExPCL_LP.setVerticalTabHeight(203);
                                    paramExPCL_LP.setHorizontalTabWidth((byte) 50);

                                    data = String.format(Locale.getDefault(), "Tab0%cTab1%cTab2%cHello World I am a printing sample (Horizontal Tab = 200)", 9, 9, 9);
                                    docExPCL_LP.writeText(data, paramExPCL_LP);

                                    //paramExPCL_LP.setHorizontalTabWidth(100);
                                    paramExPCL_LP.setSensorSensitivity((byte) 100);
                                    docExPCL_LP.writeText("Hello World I am a printing sample (Sensor Sensitivity = 100)", paramExPCL_LP);
                                    paramExPCL_LP.setSensorSensitivity((byte) 255);
                                    paramExPCL_LP.setPaperPresenter((byte) 100);
                                    docExPCL_LP.writeText("Hello World I am a printing sample (Paper Presenter = 100)", paramExPCL_LP);
                                    paramExPCL_LP.setPaperPresenter((byte) 190);
                                    paramExPCL_LP.setAutoPowerDownTimer(30);
                                    docExPCL_LP.writeText("Hello World I am a printing sample (Auto Power Down = 9 seconds)", paramExPCL_LP);
                                    paramExPCL_LP.setAutoPowerDownTimer(0);
                                    docExPCL_LP.writeText("Hello World I am a printing sample (Auto Power Down = 0 seconds)", paramExPCL_LP);
                                    printData = docExPCL_LP.getDocumentData();
                                }

                                // BARCODE SAMPLES
                                else if (selectedItemIndex == 1) {
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code39, "DMITRIY", true, (byte) 100);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code39, "DMITRIY", false, (byte) 50);

                                    paramExPCL_LP.setBarCodeHeight((byte) 100);
                                    paramExPCL_LP.setIsAnnotate(false);
                                    paramExPCL_LP.setBarCodeType(BarcodeExPCL_LP.Code128);
                                    paramExPCL_LP.setFontIndex(5);
                                    paramExPCL_LP.setIsUnderline(true);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code39, "DMITRIY", true, (byte) 25, paramExPCL_LP);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code39, "DMITRIY", false, (byte) 50);

                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code128, "DMITRIY", true, (byte) 25);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code128, "dmitriy", true, (byte) 25);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code128, "1234567890", true, (byte) 25);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Interleaved2of5, "1234567890", true, (byte) 25);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.UPC, "1234567890$", true, (byte) 25);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Codabar, "12345678901", true, (byte) 25);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Codabar, "123456", true, (byte) 25);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Codabar, "1234567", true, (byte) 25);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Codabar, "123456789012", true, (byte) 25);

                                    paramExPCL_LP.setFontIndex(1);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code39, "DMITRIY", true, (byte) 25, paramExPCL_LP);
                                    paramExPCL_LP.setFontIndex(2);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code128, "DMITRIY", true, (byte) 25, paramExPCL_LP);
                                    paramExPCL_LP.setFontIndex(3);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code128, "dmitriy", true, (byte) 25, paramExPCL_LP);
                                    paramExPCL_LP.setFontIndex(4);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Code128, "1234567890", true, (byte) 25, paramExPCL_LP);
                                    paramExPCL_LP.setFontIndex(5);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Interleaved2of5, "1234567890", true, (byte) 25, paramExPCL_LP);
                                    paramExPCL_LP.setFontIndex(6);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.UPC, "1234567890$", true, (byte) 25, paramExPCL_LP);
                                    paramExPCL_LP.setFontIndex(7);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Codabar, "12345678901", true, (byte) 25, paramExPCL_LP);
                                    paramExPCL_LP.setFontIndex(8);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Codabar, "123456", true, (byte) 25, paramExPCL_LP);
                                    paramExPCL_LP.setFontIndex(9);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Codabar, "1234567", true, (byte) 25, paramExPCL_LP);
                                    paramExPCL_LP.setFontIndex(10);
                                    docExPCL_LP.writeBarCode(BarcodeExPCL_LP.Codabar, "123456789012", true, (byte) 25, paramExPCL_LP);

                                    docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarOmnidirectional, "1234567890123", true, (byte) 2, (byte) 0, (byte) 0, (byte) 1);
                                    docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarOmnidirectional, "1234567890123", false, (byte) 3, (byte) 0, (byte) 0, (byte) 2);
                                    docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarOmnidirectional, "1234567890123", true, (byte) 4, (byte) 0, (byte) 0, (byte) 3);
                                    docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarOmnidirectional, "1234567890123", false, (byte) 5, (byte) 0, (byte) 0, (byte) 4);
                                    docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarOmnidirectional, "1234567890123", true, (byte) 6, (byte) 0, (byte) 0, (byte) 5);

                                    docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarTruncated, "1234567890123", true, (byte) 2, (byte) 0, (byte) 0, (byte) 1);
                                    docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarStacked, "1234567890123", true, (byte) 2, (byte) 0, (byte) 0, (byte) 1);
                                    docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarStackedOmnidirectional, "1234567890123", true, (byte) 2, (byte) 0, (byte) 0, (byte) 1);
                                    docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarLimited, "1234567890123", true, (byte) 2, (byte) 0, (byte) 0, (byte) 1);
                                    docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarExpanded, "DATAMAX-O'NEIL", true, (byte) 2, (byte) 0, (byte) 0, (byte) 1);
                                    docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.UPCA, "12345678901", true, (byte) 2, (byte) 0, (byte) 0, (byte) 1);
                                    docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.UPCE, "1234500006", true, (byte) 2, (byte) 0, (byte) 0, (byte) 1);
                                    docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.EAN13, "123456789012", true, (byte) 2, (byte) 0, (byte) 0, (byte) 1);
                                    docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.EAN8, "1234567", true, (byte) 2, (byte) 0, (byte) 0, (byte) 1);
                                    docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.UCCEAN128CCAB, "123456789012", true, (byte) 2, (byte) 0, (byte) 0, (byte) 1);
                                    docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.UCCEAN128CCC, "123456789012", true, (byte) 2, (byte) 0, (byte) 0, (byte) 1);

                                    paramExPCL_LP.setFontIndex(1);
                                    docExPCL_LP.writeBarCodeGS1DataBar(GS1DataBar.GS1DataBarOmnidirectional, "1234567890123", true, (byte) 2, (byte) 0, (byte) 0, (byte) 1, paramExPCL_LP);

                                    docExPCL_LP.writeBarCodeQRCode("www.datamax-oneil.com", false, 2, (byte) 'H', 2);

                                    paramExPCL_LP.setFontIndex(10);
                                    docExPCL_LP.writeBarCodeQRCode("www.datamax-oneil.com", true, 2, (byte) 'L', 3, paramExPCL_LP);

                                    docExPCL_LP.writeBarCodePDF417("www.datamax-oneil.com", 2);

                                    paramExPCL_LP.setFontIndex(1);
                                    docExPCL_LP.writeBarCodePDF417("www.datamax-oneil.com", 2, paramExPCL_LP);
                                    printData = docExPCL_LP.getDocumentData();
                                }
                                //Graphics
                                else if (selectedItemIndex == 2) {
                                    DisplayPrintingStatusMessage("Processing image..");
                                    Bitmap anImage = BitmapFactory.decodeStream(getAssets().open("dologo.png"));
                                    docExPCL_LP.writeImage(anImage, m_printHeadWidth);
                                    printData = docExPCL_LP.getDocumentData();
                                }
                                //User selected an item not predefined on list(from browsing file)
                                else {
                                    String selectedItem = (String) m_printItemsSpinner.getSelectedItem();
                                    Bitmap anImage = null;
                                    //Check if item is an image
                                    String[] okFileExtensions = new String[]{".jpg", ".png", ".gif", ".jpeg", ".bmp", ".tif", ".tiff", ".pcx"};
                                    for (String extension : okFileExtensions) {
                                        if (selectedItem.toLowerCase().endsWith(extension)) {
                                            anImage = BitmapFactory.decodeFile(selectedItem);
                                            break;
                                        }
                                    }
                                    //selected item is not an image file
                                    if (selectedItem.toLowerCase(Locale.US).endsWith(".pdf")) {
                                        docExPCL_LP.writePDF(selectedItem, m_printHeadWidth);
                                        printData = docExPCL_LP.getDocumentData();
                                    }
                                    //selected item is not an image file
                                    else if (anImage == null) {
                                        File file = new File(selectedItem);
                                        byte[] readBuffer = new byte[(int) file.length()];
                                        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                                        if(inputStream.read(readBuffer) == 0)
                                            throw new Exception("Unable to read file or file is empty.");
                                        inputStream.close();
                                        printData = readBuffer;
                                    } else {
                                        DisplayPrintingStatusMessage("Processing image..");
                                        docExPCL_LP.writeImage(anImage, m_printHeadWidth);
                                        printData = docExPCL_LP.getDocumentData();
                                    }
                                }
                                break;
                            //for ExPCL_PP (PPC Printers)
                            case "ExPCL_PP":
                                // Text
                                if (selectedItemIndex == 0) {
                                    docExPCL_PP.drawText(0, 1600, true, RotationAngle.RotationAngle_0, "<f=1>This is a sample");
                                    docExPCL_PP.drawText(0, 1625, true, RotationAngle.RotationAngle_0, "<f=2>This is a sample");
                                    docExPCL_PP.drawText(0, 1650, true, RotationAngle.RotationAngle_0, "<f=3>This is a sample");
                                    docExPCL_PP.drawText(0, 1675, true, RotationAngle.RotationAngle_0, "<f=4>This is a sample");
                                    docExPCL_PP.drawText(0, 1700, true, RotationAngle.RotationAngle_0, "<f=5>This is a sample");
                                    docExPCL_PP.drawText(0, 1725, true, RotationAngle.RotationAngle_0, "<f=6>This is a sample");
                                    docExPCL_PP.drawText(0, 1750, true, RotationAngle.RotationAngle_0, "<f=7>This is a sample");
                                    docExPCL_PP.drawText(0, 1775, true, RotationAngle.RotationAngle_0, "<f=8>This is a sample");
                                    docExPCL_PP.drawText(0, 1800, true, RotationAngle.RotationAngle_0, "<f=9>This is a sample");
                                    docExPCL_PP.drawText(0, 1825, true, RotationAngle.RotationAngle_0, "<f=10>This is a sample");
                                    docExPCL_PP.drawText(0, 1850, true, RotationAngle.RotationAngle_0, "<f=11>This is a sample");
                                    docExPCL_PP.drawText(0, 1875, true, RotationAngle.RotationAngle_0, "<f=12>This is a sample");
                                    docExPCL_PP.drawText(0, 1900, true, RotationAngle.RotationAngle_0, "<f=13>This is a sample");
                                    docExPCL_PP.drawText(0, 1950, true, RotationAngle.RotationAngle_0, "<f=14>This is a sample");
                                    docExPCL_PP.drawText(0, 2000, true, RotationAngle.RotationAngle_0, "<f=15>This is a sample");

                                    // Rotate text by 180
                                    docExPCL_PP.drawText(384, 2425, true, RotationAngle.RotationAngle_180, "<f=1>This is a sample");
                                    docExPCL_PP.drawText(384, 2400, true, RotationAngle.RotationAngle_180, "<f=2>This is a sample");
                                    docExPCL_PP.drawText(384, 2375, true, RotationAngle.RotationAngle_180, "<f=3>This is a sample");
                                    docExPCL_PP.drawText(384, 2350, true, RotationAngle.RotationAngle_180, "<f=4>This is a sample");
                                    docExPCL_PP.drawText(384, 2325, true, RotationAngle.RotationAngle_180, "<f=5>This is a sample");
                                    docExPCL_PP.drawText(384, 2300, true, RotationAngle.RotationAngle_180, "<f=6>This is a sample");
                                    docExPCL_PP.drawText(384, 2275, true, RotationAngle.RotationAngle_180, "<f=7>This is a sample");
                                    docExPCL_PP.drawText(384, 2250, true, RotationAngle.RotationAngle_180, "<f=8>This is a sample");
                                    docExPCL_PP.drawText(384, 2225, true, RotationAngle.RotationAngle_180, "<f=9>This is a sample");
                                    docExPCL_PP.drawText(384, 2200, true, RotationAngle.RotationAngle_180, "<f=10>This is a sample");
                                    docExPCL_PP.drawText(384, 2175, true, RotationAngle.RotationAngle_180, "<f=11>This is a sample");
                                    docExPCL_PP.drawText(384, 2150, true, RotationAngle.RotationAngle_180, "<f=12>This is a sample");
                                    docExPCL_PP.drawText(384, 2125, true, RotationAngle.RotationAngle_180, "<f=13>This is a sample");
                                    docExPCL_PP.drawText(384, 2100, true, RotationAngle.RotationAngle_180, "<f=14>This is a sample");
                                    docExPCL_PP.drawText(384, 2050, true, RotationAngle.RotationAngle_180, "<f=15>This is a sample");

                                    // Text
                                    docExPCL_PP.writeText("<f=1>This is a sample", 2450, 0);
                                    paramExPCL_PP.setIsAnnotate(true);
                                    paramExPCL_PP.setIsBold(true);
                                    paramExPCL_PP.setIsUnderline(true);
                                    paramExPCL_PP.setFontIndex(5);
                                    docExPCL_PP.writeText("This is a sample", 2475, 0, paramExPCL_PP);
                                    paramExPCL_PP.setIsAnnotate(false);
                                    paramExPCL_PP.setIsBold(false);
                                    paramExPCL_PP.setIsUnderline(false);
                                    paramExPCL_PP.setHorizontalMultiplier(2);
                                    paramExPCL_PP.setVerticalMultiplier(2);
                                    paramExPCL_PP.setFontIndex(5);
                                    docExPCL_PP.writeText("This is a sample", 2425, 0, paramExPCL_PP);
                                    docExPCL_PP.setPageHeight(3000);
                                    printData = docExPCL_PP.getDocumentData();
                                }

                                // Print all barcodes
                                else if (selectedItemIndex == 1) {
                                    docExPCL_PP.drawBarCode(0, 0, RotationAngle.RotationAngle_0, true, BarcodeExPCL_PP.Code39, (byte) 25, "12345");
                                    docExPCL_PP.drawBarCode(0, 50, RotationAngle.RotationAngle_0, true, BarcodeExPCL_PP.Code128, (byte) 25, "SAMPLE");
                                    docExPCL_PP.drawBarCode(0, 100, RotationAngle.RotationAngle_0, true, BarcodeExPCL_PP.Code128, (byte) 25, "sample");
                                    docExPCL_PP.drawBarCode(0, 150, RotationAngle.RotationAngle_0, true, BarcodeExPCL_PP.Code128, (byte) 25, "12");
                                    docExPCL_PP.drawBarCode(0, 200, RotationAngle.RotationAngle_0, true, BarcodeExPCL_PP.Interleaved2of5, (byte) 25, "1234567890");
                                    docExPCL_PP.drawBarCode(0, 250, RotationAngle.RotationAngle_0, true, BarcodeExPCL_PP.UPC, (byte) 40, "123456789012");
                                    docExPCL_PP.drawBarCode(0, 325, RotationAngle.RotationAngle_0, true, BarcodeExPCL_PP.UPC, (byte) 40, "1234567");
                                    docExPCL_PP.drawBarCode(0, 400, RotationAngle.RotationAngle_0, true, BarcodeExPCL_PP.UPC, (byte) 40, "12345678");
                                    docExPCL_PP.drawBarCode(0, 475, RotationAngle.RotationAngle_0, true, BarcodeExPCL_PP.UPC, (byte) 40, "1234567890123");
                                    docExPCL_PP.drawBarCode(0, 550, RotationAngle.RotationAngle_0, true, BarcodeExPCL_PP.Codabar, (byte) 15, "1234567890");

                                    // Rotate 180 all barcodes
                                    docExPCL_PP.drawBarCode(384, 1175, RotationAngle.RotationAngle_180, true, BarcodeExPCL_PP.Code39, (byte) 25, "12345");
                                    docExPCL_PP.drawBarCode(384, 1125, RotationAngle.RotationAngle_180, true, BarcodeExPCL_PP.Code128, (byte) 25, "SAMPLE");
                                    docExPCL_PP.drawBarCode(384, 1075, RotationAngle.RotationAngle_180, true, BarcodeExPCL_PP.Code128, (byte) 25, "sample");
                                    docExPCL_PP.drawBarCode(384, 1025, RotationAngle.RotationAngle_180, true, BarcodeExPCL_PP.Code128, (byte) 25, "12");
                                    docExPCL_PP.drawBarCode(384, 975, RotationAngle.RotationAngle_180, true, BarcodeExPCL_PP.Interleaved2of5, (byte) 25, "1234567890");
                                    docExPCL_PP.drawBarCode(384, 925, RotationAngle.RotationAngle_180, true, BarcodeExPCL_PP.UPC, (byte) 40, "123456789012");
                                    docExPCL_PP.drawBarCode(384, 850, RotationAngle.RotationAngle_180, true, BarcodeExPCL_PP.UPC, (byte) 40, "1234567");
                                    docExPCL_PP.drawBarCode(384, 775, RotationAngle.RotationAngle_180, true, BarcodeExPCL_PP.UPC, (byte) 40, "12345678");
                                    docExPCL_PP.drawBarCode(384, 700, RotationAngle.RotationAngle_180, true, BarcodeExPCL_PP.UPC, (byte) 40, "1234567890123");
                                    docExPCL_PP.drawBarCode(384, 625, RotationAngle.RotationAngle_180, true, BarcodeExPCL_PP.Codabar, (byte) 15, "1234567890");

                                    // Barcodes
                                    docExPCL_PP.writeBarCode(BarcodeExPCL_PP.Code39, 3, "sample", 2500, 0);
                                    paramExPCL_PP.setIsAnnotate(true);
                                    paramExPCL_PP.setIsBold(true);
                                    paramExPCL_PP.setIsUnderline(true);
                                    paramExPCL_PP.setBarCodeHeight((byte) 50);
                                    paramExPCL_PP.setRotation(RotationAngle.RotationAngle_180);
                                    docExPCL_PP.writeBarCode(BarcodeExPCL_PP.Code39, 3, "sample", 2650, 384, paramExPCL_PP);
                                    //paramExPCL_PP.setIsAnnotate(true);
                                    paramExPCL_PP.setIsBold(false);
                                    paramExPCL_PP.setIsUnderline(false);
                                    paramExPCL_PP.setBarCodeHeight((byte) 40);
                                    paramExPCL_PP.setRotation(RotationAngle.RotationAngle_0);
                                    docExPCL_PP.writeBarCode(BarcodeExPCL_PP.Code39, 5, "sample", 2650, 0);
                                    docExPCL_PP.setPageHeight(3000);
                                    printData = docExPCL_PP.getDocumentData();

                                }
                                // Rectangle
                                else if (selectedItemIndex == 2) {
                                    docExPCL_PP.drawRectangle(0, 1200, 384, 1584, true, 0);
                                    docExPCL_PP.drawRectangle(20, 1220, 364, 1564, false, 3);
                                    docExPCL_PP.drawRectangle(40, 1240, 344, 1544, false, 10);
                                    docExPCL_PP.drawRectangle(80, 1280, 304, 1504, false, 0);
                                    docExPCL_PP.drawRectangle(110, 1310, 274, 1474, true, 3);
                                    docExPCL_PP.drawRectangle(130, 1330, 254, 1454, true, 10);

                                    // Lines
                                    docExPCL_PP.writeHorizontalLine(2450, 0, 384, 10);
                                    docExPCL_PP.writeHorizontalLine(2475, 0, 384, 5, paramExPCL_PP);
                                    docExPCL_PP.writeVerticalLine(2550, 5, 84, 10);
                                    docExPCL_PP.writeVerticalLine(2550, 200, 84, 5, paramExPCL_PP);
                                    docExPCL_PP.writeRectangle(2550, 50, 2634, 150);
                                    paramExPCL_PP.setLineThickness(10);
                                    docExPCL_PP.writeRectangle(2550, 250, 2634, 379, paramExPCL_PP);
                                    docExPCL_PP.setPageHeight(3000);
                                    printData = docExPCL_PP.getDocumentData();
                                }
                                //User selected an item not predefined on list(from browsing file)
                                else {
                                    String selectedItem = (String) m_printItemsSpinner.getSelectedItem();
                                    Bitmap anImage = null;
                                    //Check if item is an image
                                    String[] okFileExtensions = new String[]{".jpg", ".png", ".gif", ".jpeg", ".bmp", ".tif", ".tiff", ".pcx"};
                                    for (String extension : okFileExtensions) {
                                        if (selectedItem.toLowerCase().endsWith(extension)) {
                                            anImage = BitmapFactory.decodeFile(selectedItem);
                                            break;
                                        }
                                    }
                                    //selected item is not an image file
                                    if (selectedItem.toLowerCase(Locale.US).endsWith(".pdf")) {
                                        docExPCL_LP.writePDF(selectedItem, m_printHeadWidth);
                                        printData = docExPCL_LP.getDocumentData();
                                    }
                                    //selected item is not an image file
                                    else if (anImage == null) {
                                        File file = new File(selectedItem);
                                        byte[] readBuffer = new byte[(int) file.length()];
                                        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                                        if (inputStream.read(readBuffer) == 0)
                                            throw new Exception("Unable to read file or file is empty.");
                                        inputStream.close();
                                        printData = readBuffer;
                                    } else {
                                        DisplayPrintingStatusMessage("Processing image..");
                                        docExPCL_LP.writeImage(anImage, m_printHeadWidth);
                                        printData = docExPCL_LP.getDocumentData();
                                    }
                                }
                                break;
                            //DPL printers
                            case "DPL":
                                //text sample to generate
                                if (selectedItemIndex == 0) {

                                    //Enable text formatting (eg. bold, italic, underline)
                                    docDPL.setEnableAdvanceFormatAttribute(true);

                                    //Using Internal Bitmapped Font with ID 0
                                    docDPL.writeTextInternalBitmapped("Hello World", 0, 100, 5, paramDPL);

                                    //Using Downloaed Bitmapped Font with ID 100
                                    docDPL.writeTextDownloadedBitmapped("Hello World", 100, 125, 5, paramDPL);

                                    //Using Internal Smooth Font with size 14
                                    paramDPL.setIsBold(true);
                                    paramDPL.setIsItalic(true);
                                    paramDPL.setIsUnderline(true);
                                    docDPL.writeTextInternalSmooth("Hello World", 14, 150, 5, paramDPL);

                                    //write normal ASCII Text Scalable
                                    paramDPL.setIsBold(true);
                                    paramDPL.setIsItalic(false);
                                    paramDPL.setIsUnderline(false);
                                    docDPL.writeTextScalable("Hello World", "00", 175, 5, paramDPL);

                                    //write normal ASCII Text Scalable
                                    paramDPL.setIsBold(false);
                                    paramDPL.setIsItalic(false);
                                    paramDPL.setIsUnderline(true);
                                    docDPL.writeTextScalable("Hello World", "00", 200, 5, paramDPL);

                                    //write normal ASCII Text Scalable
                                    paramDPL.setIsBold(false);
                                    paramDPL.setIsItalic(true);
                                    paramDPL.setIsUnderline(false);
                                    docDPL.writeTextScalable("Hello World", "00", 225, 5, paramDPL);

                                    //Using Chinese Font example
                                    paramDPL.setIsUnicode(true);
                                    paramDPL.setDBSymbolSet(DoubleByteSymbolSet.Unicode);
                                    paramDPL.setFontHeight(8);
                                    paramDPL.setFontWidth(8);

                                    int width = 5;

                                    paramDPL.setIsBold(true);
                                    paramDPL.setIsItalic(true);
                                    paramDPL.setIsUnderline(false);
                                    docDPL.writeTextScalable("你好，世界 (Hello World in Chinese!)", "50", 250, width, paramDPL);

                                    printData = docDPL.getDocumentData();
                                }
                                //Incrementing Text Sample
                                else if (selectedItemIndex == 1) {
                                    docDPL.setPrintQuantity(3);
                                    paramDPL.setEmbeddedEnable(false);
                                    paramDPL.setIncrementDecrementValue(5);
                                    paramDPL.setIncrementDecrementType(ParametersDPL.IncrementDecrementTypeValue.NumericIncrement);
                                    docDPL.writeTextInternalBitmapped("12345", 3, 0, 0, paramDPL);

                                    paramDPL.setEmbeddedEnable(false);
                                    paramDPL.setIncrementDecrementValue(5);
                                    paramDPL.setIncrementDecrementType(ParametersDPL.IncrementDecrementTypeValue.AlphanumericIncrement);
                                    docDPL.writeTextInternalBitmapped("ABC123", 3, 35, 0, paramDPL);

                                    paramDPL.setEmbeddedEnable(false);
                                    paramDPL.setIncrementDecrementValue(5);
                                    paramDPL.setIncrementDecrementType(ParametersDPL.IncrementDecrementTypeValue.HexdecimalIncrement);
                                    docDPL.writeTextInternalBitmapped("0A0D", 3, 70, 0, paramDPL);

                                    paramDPL.setEmbeddedEnable(true);
                                    paramDPL.setEmbeddedIncrementDecrementValue("010010010");
                                    paramDPL.setIncrementDecrementType(ParametersDPL.IncrementDecrementTypeValue.NumericIncrement);
                                    docDPL.writeTextInternalBitmapped("AB1CD1EF1", 3, 105, 0, paramDPL);

                                    printData = docDPL.getDocumentData();
                                }
                                //Barcodes
                                else if (selectedItemIndex == 2) {
                                    //Test print Code 3 of 9
                                    //Code 39 with default parameter
                                    docDPL.writeBarCode("A", "BRCDA", 0, 0);
                                    docDPL.writeTextInternalBitmapped("Barcode A", 1, 60, 0);

                                    //Barecode A with specified parameters
                                    paramDPL.setIsUnicode(false);
                                    paramDPL.setWideBarWidth(3);
                                    paramDPL.setNarrowBarWidth(1);
                                    paramDPL.setSymbolHeight(20);

                                    docDPL.writeBarCode("A", "BRCDA", 100, 0, paramDPL);
                                    docDPL.writeTextInternalBitmapped("Barcode A", 1, 135, 0);

                                    //UPC-A with specified parameters
                                    paramDPL.setIsUnicode(false);
                                    paramDPL.setWideBarWidth(3);
                                    paramDPL.setNarrowBarWidth(1);
                                    paramDPL.setSymbolHeight(10);
                                    docDPL.writeBarCode("B", "012345678912", 160, 0, paramDPL);
                                    docDPL.writeTextInternalBitmapped("UPC-A", 1, 185, 0);

                                    //Code 128 with specified parameters
                                    paramDPL.setIsUnicode(false);
                                    paramDPL.setWideBarWidth(3);
                                    paramDPL.setNarrowBarWidth(1);
                                    paramDPL.setSymbolHeight(20);
                                    docDPL.writeBarCode("E", "ACODE128", 210, 0, paramDPL);
                                    docDPL.writeTextInternalBitmapped("Code 128", 1, 250, 0);

                                    //EAN-13
                                    paramDPL.setIsUnicode(false);
                                    paramDPL.setWideBarWidth(3);
                                    paramDPL.setNarrowBarWidth(1);
                                    paramDPL.setSymbolHeight(20);
                                    docDPL.writeBarCode("F", "0123456789012", 285, 0, paramDPL);
                                    docDPL.writeTextInternalBitmapped("EAN-13", 1, 315, 0);
                                    //EAN Code 128
                                    paramDPL.setIsUnicode(false);
                                    paramDPL.setWideBarWidth(3);
                                    paramDPL.setNarrowBarWidth(1);
                                    paramDPL.setSymbolHeight(20);
                                    docDPL.writeBarCode("Q", "0123456789012345678", 355, 0, paramDPL);
                                    docDPL.writeTextInternalBitmapped("EAN Code 128", 1, 395, 0);
                                    //UPS MaxiCode, Mode 2 & 3
                                    paramDPL.setIsUnicode(false);
                                    paramDPL.setWideBarWidth(0);
                                    paramDPL.setNarrowBarWidth(0);
                                    paramDPL.setSymbolHeight(0);

                                    UPSMessage upsMessage = new UPSMessage("920243507", 840, 1, "1Z00004951", "UPSN", "9BCJ43", 365, "625TH9", 1, 1, 10, true, "669 SECOND ST", "ENCINITAS", "CA");

                                    docDPL.writeBarCodeUPSMaxiCode(2, upsMessage, 445, 0, paramDPL);
                                    docDPL.writeTextInternalBitmapped("UPS MaxiCode", 1, 560, 0);

                                    //PDF-417
                                    paramDPL.setIsUnicode(false);
                                    paramDPL.setWideBarWidth(0);
                                    paramDPL.setNarrowBarWidth(0);
                                    paramDPL.setSymbolHeight(0);
                                    docDPL.writeBarCodePDF417("ABCDEF1234", false, 1, 0, 0, 0, 590, 0, paramDPL);
                                    docDPL.writeTextInternalBitmapped("PDF-417", 1, 630, 0);

                                    //Data Matrix
                                    paramDPL.setIsUnicode(false);
                                    paramDPL.setWideBarWidth(4);
                                    paramDPL.setNarrowBarWidth(4);
                                    paramDPL.setSymbolHeight(0);

                                    docDPL.writeBarCodeDataMatrix("DATAMAX", 140, 0, 0, 0, 670, 0, paramDPL);
                                    docDPL.writeTextInternalBitmapped("Data Matrix w/ ECC 140", 1, 770, 0);
                                    docDPL.writeBarCodeDataMatrix("DATAMAX", 200, 0, 0, 0, 810, 0, paramDPL);
                                    docDPL.writeTextInternalBitmapped("Data Matrix w/ ECC 200", 1, 880, 0);

                                    //QRCODE
                                    paramDPL.setIsUnicode(false);
                                    paramDPL.setWideBarWidth(4);
                                    paramDPL.setNarrowBarWidth(4);
                                    paramDPL.setSymbolHeight(0);
                                    //AutoFormatting
                                    docDPL.writeBarCodeQRCode("This is the data portion", true, 0, "", "", "", "", 920, 0, paramDPL);
                                    docDPL.writeTextInternalBitmapped("QR Barcode w/ Auto Formatting", 1, 1030, 0);

                                    //Manual Formatting
                                    docDPL.writeBarCodeQRCode("1234This is the data portion", false, 2, "H", "4", "M", "A", 1070, 0, paramDPL);
                                    docDPL.writeTextInternalBitmapped("QR Barcode w/ Manual formatting", 1, 1200, 0);

                                    //Test BarcodeAzTec
                                    paramDPL.setIsUnicode(false);
                                    paramDPL.setWideBarWidth(12);
                                    paramDPL.setNarrowBarWidth(12);
                                    paramDPL.setSymbolHeight(0);
                                    docDPL.writeBarCodeAztec("ABCD1234", 0, false, 0, 1240, 0, paramDPL);
                                    docDPL.writeTextInternalBitmapped("Aztec Barcode ECI 0, ECC 0", 1, 1360, 0);
                                    docDPL.writeBarCodeAztec("ABCD1234", 17, true, 232, 1400, 0, paramDPL);
                                    docDPL.writeTextInternalBitmapped("Aztec Barcode ECI 1, ECC 232", 1, 1500, 0);

                                    //GS1 Databars
                                    paramDPL.setWideBarWidth(2);
                                    paramDPL.setNarrowBarWidth(2);
                                    paramDPL.setSymbolHeight(0);

                                    docDPL.writeBarCodeGS1DataBar("2001234567890", "", "E", 1, 0, 0, 2, 1540, 0, paramDPL);
                                    docDPL.writeTextInternalBitmapped("GS1 Databar Expanded", 1, 1760, 0);

                                    docDPL.writeBarCodeGS1DataBar("2001234567890", "hello123World", "D", 1, 0, 0, 0, 1800, 0, paramDPL);
                                    docDPL.writeTextInternalBitmapped("GS1 Stacked Omni Direction", 1, 1980, 0);

                                    //Austrailia 4-State
                                    docDPL.writeBarCodeAusPost4State("A124B", true, 59, 32211324, 2020, 0, paramDPL);
                                    docDPL.writeTextInternalBitmapped("Aus Post 4 State readable", 1, 2100, 0);
                                    docDPL.writeBarCodeAusPost4State("123456789012345", false, 62, 39987520, 2140, 0, paramDPL);
                                    docDPL.writeTextInternalBitmapped("Aus Post 4 State non readable", 1, 2190, 0);


                                    //write CodaBlock
                                    paramDPL.setWideBarWidth(0);
                                    paramDPL.setNarrowBarWidth(0);
                                    docDPL.writeBarCodeCODABLOCK("12345678", 25, "E", false, 4, 2, 2230, 0, paramDPL);
                                    docDPL.writeTextInternalBitmapped("CODABLOCK", 1, 2320, 0);

                                    //write TCIF
                                    paramDPL.setWideBarWidth(0);
                                    paramDPL.setNarrowBarWidth(0);
                                    docDPL.writeBarCodeTLC39("ABCD12345678901234589ABED", 0, 123456, 2360, 0, paramDPL);
                                    docDPL.writeTextInternalBitmapped("TCIF", 1, 2480, 0);

                                    //write MicroPDF417
                                    paramDPL.setWideBarWidth(0);
                                    paramDPL.setNarrowBarWidth(0);
                                    docDPL.writeBarCodeMicroPDF417("PDF417", 4, 4, false, false, 2520, 0, paramDPL);
                                    docDPL.writeTextInternalBitmapped("Micro PDF417", 1, 2560, 0);
                                    printData = docDPL.getDocumentData();
                                }
                                //graphics
                                else if (selectedItemIndex == 3) {
                                    //writeLine
                                    docDPL.writeLine(0, 0, 10, 25);

                                    //writeBox
                                    docDPL.writeBox(50, 0, 25, 25, 1, 1);

                                    //writeRectangle
                                    docDPL.writeRectangle(9, 100, 10, 150, 10, 150, 200, 100, 200);
                                    docDPL.writeTriangle(7, 200, 10, 250, 25, 200, 40);
                                    docDPL.writeCircle(4, 300, 25, 25);
                                    printData = docDPL.getDocumentData();
                                }
                                //image
                                else if (selectedItemIndex == 4) {
                                    DisplayPrintingStatusMessage("Processing image..");
                                    Bitmap anImage = BitmapFactory.decodeStream(getAssets().open("dologo.png"));
                                    docDPL.writeTextInternalBitmapped("This is a D-O Logo", 1, 130, 200);
                                    docDPL.writeImage(anImage, 0, 0, paramDPL);
                                    printData = docDPL.getDocumentData();
                                }
                                //User selected a browsed file
                                else {
                                    boolean isImage = false;
                                    String selectedItem = (String) m_printItemsSpinner.getSelectedItem();
                                    //Check if item is an image
                                    String[] okFileExtensions = new String[]{".jpg", ".png", ".gif", ".jpeg", ".bmp", ".tif", ".tiff", ".pcx"};
                                    for (String extension : okFileExtensions) {
                                        if (selectedItem.toLowerCase(Locale.US).endsWith(extension)) {
                                            isImage = true;
                                            break;
                                        }
                                    }
                                    //selected item is a pdf file
                                    if (selectedItem.toLowerCase(Locale.US).endsWith(".pdf")) {
                                        docDPL.writePDF(selectedItem, m_printHeadWidth, 0, 0);
                                        printData = docDPL.getDocumentData();
                                    }
                                    //selected item is not an image file
                                    else if (!isImage) {
                                        File file = new File(selectedItem);
                                        byte[] readBuffer = new byte[(int) file.length()];
                                        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                                        if (inputStream.read(readBuffer) == 0)
                                            throw new Exception("Unable to read file or file is empty.");
                                        inputStream.close();
                                        printData = readBuffer;
                                    } else {
                                        DisplayPrintingStatusMessage("Processing image..");
                                        ImageType imgType = ImageType.Other;
                                        if (selectedItem.toLowerCase().endsWith(".pcx")) {
                                            imgType = ImageType.PCXFlipped_8Bit;
                                        }

                                        Bitmap anImage = BitmapFactory.decodeFile(selectedItem);

                                        docDPL.writeImage(anImage, 150, 0, 0, paramDPL);
                                        printData = docDPL.getDocumentData();
                                    }
                                }
                                break;
                        }
                    }
                    //=====================Start Connection Thread=======================================//
                    new Thread(DOPrintMainActivity.this, "PrintingTask").start();
                }
                catch (Exception e) {
                    // Application error message box
                    e.printStackTrace();
                    AlertDialog.Builder builder = new AlertDialog.Builder(DOPrintMainActivity.this);
                    builder.setTitle("Application Error")
                            .setMessage(e.getMessage())
                            .setCancelable(false)
                            .setNegativeButton("Close",new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            }

        });

        //ON press save settings
        m_saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                SaveApplicationSettingToFile();
            }

        });
    }

    /**Display printing status message to the Textview in the
     * DOPrinterDemoActivity
     * @param MsgStr - message to display
     */
    public void DisplayPrintingStatusMessage(String MsgStr) {
        g_PrintStatusStr = MsgStr;

        m_handler.post(new Runnable() {
            public void run() {
                ((TextView)findViewById(R.id.printing_status_textview)).setText(g_PrintStatusStr);
            }// run()
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.doprint_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.menu_about) {
            // Display the about dialog box
            DisplayAboutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ----------------------------
    // Display About Dialog Box
    // ------------------------------
    void DisplayAboutDialog() {
        final AppCompatDialog about = new AppCompatDialog(DOPrintMainActivity.this);
        about.setContentView(R.layout.doabout);
        about.setCancelable(true);
        about.setTitle(R.string.about);

        // get version of the application.
        PackageInfo pinfo;
        try
        {
            pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);

            if (pinfo != null) {

                // set up the text view
                TextView descTextView = (TextView) about.findViewById(R.id.AboutDescription);
                String descString = " " + getString(R.string.app_name) + "\n"
                        + " Version Code:"
                        + String.valueOf(pinfo.versionCode) + "\n"
                        + " Version Name:" + pinfo.versionName+"\n"
                        + " Copyright: 2016" + "\n"
                        + " Company: Honeywell International Inc." + "\r\n"
                        + " D-O Print is a sample application that allows users to print to a Datamax-O'Neil by Honeywell printer.";

                if(descTextView != null)
                    descTextView.setText(descString);

                // set up the image view
                ImageView AboutImgView = (ImageView) about
                        .findViewById(R.id.AboutImageView);

                if (AboutImgView != null)
                    AboutImgView.setImageResource(R.drawable.honeywell_icon_transparent);

                // set up button
                Button closeButton = (Button) about.findViewById(R.id.AboutCloseButton);
                if (closeButton != null) {
                    closeButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            about.dismiss();
                        }
                    });
                }

                about.show();
            }
        }
        catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode)
        {
            //get results from connection settings activity
            case CONFIG_CONNECTION_REQUEST:
            {
                if (resultCode == RESULT_OK)
                {
                    // get the bundle data from the TCP/IP Config Intent.
                    Bundle extras = data.getExtras();
                    if (extras != null)
                    {
                        //===============Get data from Bluetooth configuration=================//
                        if(connectionType.equals("Bluetooth"))
                        {
                            String btAddressString = extras.getString(BLUETOOTH_DEVICE_ADDR_KEY);
                            if(btAddressString!= null)
                                m_printerMAC = btAddressString.toUpperCase(Locale.US);
                            if(!m_printerMAC.matches("[0-9A-fa-f:]{17}"))
                            {
                                m_printerMAC = formatBluetoothAddress(m_printerMAC);
                            }

                            String printerInfo = "Printer's MAC Address: "+ m_printerMAC;
                            m_connectionInfoStatus.setText(printerInfo);
                        }
                        //==============Get data from TCP/IP configuration===================//
                        else if (connectionType.equals("TCP/IP"))
                        {
                            m_printerIP = extras.getString(PRINTER_IPADDRESS_KEY);
                            m_printerPort = extras.getInt(PRINTER_TCPIPPORT_KEY);

                            String printerInfo = "Printer's IP Address/Port: "+ m_printerIP+ ":"+ Integer.toString(m_printerPort);
                            m_connectionInfoStatus.setText(printerInfo); //valid values are 3inch
                        }
                        g_appSettings.setPrinterMAC(m_printerMAC);
                        g_appSettings.setPrinterIP(m_printerIP);
                        g_appSettings.setPrinterPort(m_printerPort);
                    }
                }
                break;
            }

            //results from file browsing activity
            case REQUEST_PICK_FILE:
            {
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    if(extras != null) {
                        //========Get the file path===============//
                        m_selectedPath = extras.getString(FOLDER_PATH_KEY);
                        if(!filesList.contains(m_selectedPath))
                            filesList.add(m_selectedPath);
                        if(adapter != null)
                        {
                            m_printItemsSpinner.setAdapter(adapter);

                            //if item is not on the list, then add
                            if(adapter.getPosition(m_selectedPath) < 0)
                            {
                                adapter.add(m_selectedPath);
                                adapter.notifyDataSetChanged();
                            }
                            //selects the selected one.
                            m_printItemsSpinner.setSelection(adapter.getPosition(m_selectedPath));
                        }
                    }
                }
                break;
            }
        }
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //reload your ScrollBars by checking the newConfig

    }

    // -------------------------------------------------
    // Read the application configuration information from a file.
    // -------------------------------------------------
    DOPrintSettings ReadApplicationSettingFromFile() {
        DOPrintSettings ret = null;
        InputStream instream;
        try {
            showToast("Loading configuration");
            instream = openFileInput(ApplicationConfigFilename);
        } catch (FileNotFoundException e) {

            Log.e("DOPrint", e.getMessage(), e);
            showToast("No configuration loaded");
            return null;
        }

        try {
            ObjectInputStream ois = new ObjectInputStream(instream);

            try {
                ret = (DOPrintSettings) ois.readObject();
            } catch (ClassNotFoundException e) {
                Log.e("DOPrint", e.getMessage(), e);
                ret = null;
            }
        } catch (Exception e) {
            Log.e("DOPrint", e.getMessage(), e);
            ret = null;
        } finally {
            try {
                if (instream != null)
                    instream.close();
            } catch (IOException ignored) { }
        }
        return ret;
    }

    public boolean SaveApplicationSettingToFile() {

        boolean bRet = true;
        FileOutputStream fos = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            // write the object to the output stream object.
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(g_appSettings);

            // convert the output stream object to array of bytes
            byte[] buf = bos.toByteArray();

            // write the array of bytes to file output stream
            fos = openFileOutput(ApplicationConfigFilename,
                    Context.MODE_PRIVATE);
            fos.write(buf);

            File f = getDir(ApplicationConfigFilename, 0);
            Log.e("DOPrint", "Save Application settings to file: " + f.getName());
            showToast("Application Settings saved");
        } catch (IOException ioe) {
            Log.e("DOPrint", "error", ioe);
            showToast(ioe.getMessage());
            bRet = false;
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException ioe) {

                showToast(ioe.getMessage());
            }
        }
        return bRet;
    }// SaveApplicationSettingToFile()

    public void showToast(final String toast) {
        Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_LONG).show();
    }
    /** Converts Bluetooth Address string from 00ABCDEF0102 format => 00:AB:CD:EF:01:02 format
     * @param bluetoothAddr - Bluetooth Address string to convert
     */
    public String formatBluetoothAddress(String bluetoothAddr)
    {
        //Format MAC address string
        StringBuilder formattedBTAddress = new StringBuilder(bluetoothAddr);
        for (int bluetoothAddrPosition = 2; bluetoothAddrPosition <= formattedBTAddress.length() - 2; bluetoothAddrPosition += 3)
            formattedBTAddress.insert(bluetoothAddrPosition, ":");
        return formattedBTAddress.toString();
    }

    /** Display message in an Alert Dialog
     * @param message - message to display
     */
    public void ShowMessageBox(final String message, final String title) {
        m_handler.post(new Runnable() {

            @Override
            public void run() {
                LayoutInflater inflater = LayoutInflater.from(DOPrintMainActivity.this);
                View view = inflater.inflate(R.layout.messagebox, new LinearLayout(getApplicationContext()));

                TextView textview=(TextView)view.findViewById(R.id.textmsg);

                textview.setText(message);
                AlertDialog.Builder builder = new AlertDialog.Builder(DOPrintMainActivity.this);
                builder.setTitle(title)
                        .setCancelable(false)
                        .setNegativeButton("Close",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                builder.setView(view);
                AlertDialog alert = builder.create();

                alert.show();

            }
        });
    }
    // --------------------------------
    // Enable controls after printing has completed
    // ---------------------------------
    public void EnableControls(final boolean value) {
        m_handler.post(new Runnable() {

            @Override
            public void run() { m_printButton.setEnabled(value); }
        });
    }

//	public void run() 
//	{
//		//Connection
//		try
//		{
//			//====FOR BLUETOOTH CONNECTIONS========//
//			conn=null;
//			Looper.prepare();
//			DisplayPrintingStatusMessage("EST: Establishing connection.."+m_printerAddress);
//			
//			conn = Connection_Bluetooth.createClient(m_printerAddress,false);
//
//			if(!conn.getIsOpen())
//			{
//				if(conn.open())
//				{
//					DisplayPrintingStatusMessage("EST: Sending data..");
//					byte[] data = {0};
//					data = printData;   
//					int bytesWritten = 0;
//			
//					int bytesToWrite = 1024;
//					int totalBytes = data.length;
//					int remainingBytes = totalBytes;
//					while (bytesWritten < totalBytes)
//					{
//						if (remainingBytes < bytesToWrite)
//							bytesToWrite = remainingBytes;
//					                                
//						//Send data, 1024 bytes at a time until all data sent
//						conn.write(data, (int)bytesWritten, (int)bytesToWrite);
//						bytesWritten += bytesToWrite;
//						remainingBytes = remainingBytes - bytesToWrite;
//						Thread.sleep(100);
//					}
//					//signals to close connection
//					DisplayPrintingStatusMessage("EST: Closing connection..");
//					conn.close();
//				
//					DisplayPrintingStatusMessage("EST: Print Success!");
//				}
//				else {
//					DisplayPrintingStatusMessage("EST: Establishing connection failed..");
//				}
//			}
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//			DisplayPrintingStatusMessage("EST: Error: "+e.getMessage());
//			EnableControls(true);
//		}
//	}// run()

    @Override
    public void run() {
        //Connection
        try
        {
            EnableControls(false);
            //Reset connection object
            conn = null;
            //====FOR BLUETOOTH CONNECTIONS========//
            if(connectionType.equals("Bluetooth"))
            {
                Looper.prepare();
                conn = Connection_Bluetooth.createClient(m_printerMAC, false);
            }
            //====FOR TCP Connection==//
            else if(connectionType.equals("TCP/IP")) { conn = Connection_TCP.createClient(m_printerIP, m_printerPort, false); }

            if (m_printRadioButton.isChecked())
            {
                DisplayPrintingStatusMessage("Establishing connection..");
                //Open bluetooth socket
                if(!conn.getIsOpen()) { conn.open(); }

                //Sends data to printer
                DisplayPrintingStatusMessage("Sending data to printer..");

                int bytesWritten = 0;
                int bytesToWrite = 1024;
                int totalBytes = printData.length;
                int remainingBytes = totalBytes;
                while (bytesWritten < totalBytes)
                {
                    if (remainingBytes < bytesToWrite)
                        bytesToWrite = remainingBytes;

                    //Send data, 1024 bytes at a time until all data sent
                    conn.write(printData, bytesWritten, bytesToWrite);
                    bytesWritten += bytesToWrite;
                    remainingBytes = remainingBytes - bytesToWrite;
                   Thread.sleep(100);
                }

                //signals to close connection
                conn.close();

                DisplayPrintingStatusMessage("Print success.");
                EnableControls(true);
            }
            else if(m_queryRadioButton.isChecked())
            {
                String message = "";
                DisplayPrintingStatusMessage("Establishing connection..");
                //Open bluetooth socket
                if(!conn.getIsOpen()) { conn.open(); }

                DisplayPrintingStatusMessage("Querying data..");
                //If ExPCL is selected
                if (m_printerModeSpinner.getSelectedItemPosition() == 3 || m_printerModeSpinner.getSelectedItemPosition() == 4 ) {

                    //Battery Condition
                    if (selectedItemIndex == 0)
                    {
                        BatteryCondition_ExPCL batteryCond = new BatteryCondition_ExPCL(conn);
                        batteryCond.queryPrinter(1000);

                        if (!batteryCond.getValid())
                        {
                            message += "No response from printer\r\n";
                        }
                        else {
                            message += String.format(Locale.getDefault(), "Battery Voltage: %.2f\r\n", batteryCond.getVoltageBatterySingle());
                        }
                        ShowMessageBox(message, "Battery Condition");

                    }
                    //Bluetooth
                    else if (selectedItemIndex == 1)
                    {
                        BluetoothConfiguration_ExPCL btConfig = new BluetoothConfiguration_ExPCL(conn);
                        btConfig.queryPrinter(1000);

                        if (!btConfig.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Local Classic Name: %s\n", btConfig.getLocalClassicName());
                            message += String.format(Locale.getDefault(), "Local COD: %s\n", btConfig.getDeviceClass());
                            message += String.format(Locale.getDefault(), "Power Save Mode: %s\n", btConfig.getPowerSave());
                            message += String.format(Locale.getDefault(), "Security Mode: %s\n", btConfig.getSecurity());
                            message += String.format(Locale.getDefault(), "Discoverable: %s\n", btConfig.getDiscoverable());
                            message += String.format(Locale.getDefault(), "Connectable: %s\n", btConfig.getConnectable());
                            message += String.format(Locale.getDefault(), "Bondable: %s\n", btConfig.getBondable());
                            message += String.format(Locale.getDefault(), "Bluetooth Address: %s\n", btConfig.getBluetoothAddress());
                        }
                        ShowMessageBox(message, "Bluetooth Config");
                    }
                    //General Status
                    if (selectedItemIndex == 2)
                    {
                        GeneralStatus_ExPCL generalStatus = new GeneralStatus_ExPCL(conn);
                        generalStatus.queryPrinter(1000);

                        if (!generalStatus.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Printer Error: %s\r\n", generalStatus.getPrinterError() ? "Yes" : "No");
                            message += String.format(Locale.getDefault(), "Head Lever Latched: %s\r\n", generalStatus.getHeadLeverLatched() ? "Yes" : "No");
                            message += String.format(Locale.getDefault(), "Paper Present: %s\r\n", generalStatus.getPaperPresent() ? "Yes" : "No");
                            message += String.format(Locale.getDefault(), "Battery Status: %s\r\n", generalStatus.getBatteryVoltageStatus());
                            message += String.format(Locale.getDefault(), "Print Head Temperature Acceptable: %s\r\n", generalStatus.getPrintheadTemperatureAcceptable() ? "Yes" : "No");
                            message += String.format(Locale.getDefault(), "Text Queue Empty: %s\r\n", generalStatus.getTextQueueEmpty() ? "Yes" : "No");
                        }
                        ShowMessageBox(message, "General Status");

                    }
                    //Magnetic Card Data
                    else if (selectedItemIndex == 3)
                    {
                        MagneticCardData_ExPCL mcrData = new MagneticCardData_ExPCL(conn);
                        mcrData.queryPrinter(1000);

                        if (!mcrData.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Track 1: %s\n", mcrData.getTrack1Data());
                            message += String.format(Locale.getDefault(), "Track 2: %s\n", mcrData.getTrack2Data());
                            message += String.format(Locale.getDefault(), "Track 3: %s\n", mcrData.getTrack3Data());
                        }
                        ShowMessageBox(message, "Magnetic Card Data");
                    }
                    //Memory Status
                    if (selectedItemIndex == 4)
                    {
                        MemoryStatus_ExPCL memoryStatus = new MemoryStatus_ExPCL(conn);
                        memoryStatus.queryPrinter(1000);

                        if (!memoryStatus.getValid())
                        {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Print Buffer KB Remaining: %d\n", memoryStatus.getRemainingRAM());
                            message += String.format(Locale.getDefault(), "Used RAM: %d\n", memoryStatus.getUsedRAM());
                        }
                        ShowMessageBox(message, "Memory Status");

                    }
                    //Printer Options
                    if (selectedItemIndex == 5)
                    {
                        PrinterOptions_ExPCL printerOpt = new PrinterOptions_ExPCL(conn);
                        printerOpt.queryPrinter(1000);

                        if (!printerOpt.getValid())
                        {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Power Down Timer: %d\r\n", printerOpt.getPowerDownTimer());
                        }
                        ShowMessageBox(message, "Printer Options");

                    }
                    //Printhead Status
                    if (selectedItemIndex == 6)
                    {
                        PrintheadStatus_ExPCL printheadStatus = new PrintheadStatus_ExPCL(conn);
                        printheadStatus.queryPrinter(1000);

                        if (!printheadStatus.getValid())
                        {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "PrintHead Temperature: %.2f\r\n", printheadStatus.getPrintheadTemperature());
                        }
                        ShowMessageBox(message, "General Status");

                    }
                    //Version information
                    else if (selectedItemIndex == 7)
                    {
                        VersionInformation_ExPCL versionInfo = new VersionInformation_ExPCL(conn);
                        versionInfo.queryPrinter(1000);

                        if (!versionInfo.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Hardware Version: %s\n", versionInfo.getHardwareControllerVersion());
                            message += String.format(Locale.getDefault(), "Firmware Version: %s\n", versionInfo.getFirmwareVersion());

                        }
                        ShowMessageBox(message, "Version Information");
                    }

                }//end of ExPCL mode
                //DPL Mode
                else if (m_printerModeSpinner.getSelectedItemPosition() == 2 )
                {
                    //Printer Info
                    if (selectedItemIndex == 0)
                    {
                        //Query Printer info
                        PrinterInformation_DPL printerInfo = new PrinterInformation_DPL(conn);
                        printerInfo.queryPrinter(1000);

                        if (!printerInfo.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Serial Number: %s\n", printerInfo.getPrinterSerialNumber());
                            message += String.format(Locale.getDefault(), "Boot 1 Version: %s\n", printerInfo.getBoot1Version());
                            message += String.format(Locale.getDefault(), "Boot 1 Part Number: %s\n", printerInfo.getBoot1PartNumber());
                            message += String.format(Locale.getDefault(), "Boot 2 Version: %s\n", printerInfo.getBoot2Version());
                            message += String.format(Locale.getDefault(), "Boot 2 PartNumber: %s\n", printerInfo.getBoot1PartNumber());
                            message += String.format(Locale.getDefault(), "Firmware Version: %s\n", printerInfo.getVersionInformation());
                            message += String.format(Locale.getDefault(), "AVR Version: %s\n", printerInfo.getAVRVersionInformation());
                            message += String.format(Locale.getDefault(), "xAVR Version: %s\n", printerInfo.getXAVRVersionInformation());
                        }
                        ShowMessageBox(message, "Printer Information");

                    }
                    //Fonts and files
                    else if (selectedItemIndex == 1)
                    {
                        //Query Memory Module
                        Fonts_DPL fontsDPL = new Fonts_DPL(conn);
                        fontsDPL.queryPrinter(1000);


                        if (!fontsDPL.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += "FILES IN G: \n";

                            //Get All Files
                            FileInformation[] files = fontsDPL.getFiles("G");
                            if(files != null)
                            {
                                if(files.length == 0)
                                    message+= "No files found in module.\n";
                                else {
                                    for (FileInformation file : files) {
                                        message += String.format(Locale.getDefault(), "Name: %s, Size: %d, Type: %s\n",file.getFileName(),file.getFileSize(),file.getFileType());
                                    }
                                }
                            }
                            //Get internal Fonts
                            message += "INTERNAL FONTS: \n";
                            String[] internalFonts = fontsDPL.getInternalFonts();
                            for (String internalFont:internalFonts) {
                                message += String.format(Locale.getDefault(), "Name: %s\n",internalFont);

                            }
                        }
                        ShowMessageBox(message, "Files and Internal Fonts");
                    }

                    //Media Label
                    else if (selectedItemIndex == 2)
                    {
                        //Query Media Label
                        MediaLabel_DPL mediaLabel = new MediaLabel_DPL(conn);
                        mediaLabel.queryPrinter(1000);

                        if (!mediaLabel.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Media Type: %s\n", mediaLabel.getMediaType());
                            message += String.format(Locale.getDefault(), "Max Label Length: %d\n", mediaLabel.getMaxLabelLength());
                            message += String.format(Locale.getDefault(), "Continuous Label Length: %d\n", mediaLabel.getContinuousLabelLength());
                            message += String.format(Locale.getDefault(), "Sensor Type: %s\n", mediaLabel.getSensorType());
                            message += String.format(Locale.getDefault(), "Paper Empty Distance: %d\n", mediaLabel.getPaperEmptyDistance());
                            message += String.format(Locale.getDefault(), "Label Width: %d\n", mediaLabel.getLabelWidth());
                            message += String.format(Locale.getDefault(), "Head Cleaning Threshold: %d\n", mediaLabel.getHeadCleaningThreshold());
                            message += String.format(Locale.getDefault(), "Ribbon Low Diameter: %d\n", mediaLabel.getRibbonLowDiameter());
                            message += String.format(Locale.getDefault(), "Ribbon Low Pause Enable: %s\n", mediaLabel.getRibbonLowPause()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Label Length Limit Enable: %s\n", mediaLabel.getLabelLengthLimit()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Present Backup Enable: %s\n", mediaLabel.getPresentBackup()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Present Distance: %d\n", mediaLabel.getPresentDistance());
                            message += String.format(Locale.getDefault(), "Backup Distance: %d\n", mediaLabel.getBackupDistance());
                            message += String.format(Locale.getDefault(), "Stop Location: %s\n", mediaLabel.getStopLocation());
                            message += String.format(Locale.getDefault(), "Backup After Print Enable: %s\n", mediaLabel.getBackupAfterPrint()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Gap Alternative Mode: %s\n", mediaLabel.getGapAlternateMode()?"Yes":"No");
                        }

                        ShowMessageBox(message, "Media Label");
                    }

                    //Print Controls
                    else if (selectedItemIndex == 3){
                        //Print Controls
                        PrintSettings_DPL printSettings = new PrintSettings_DPL(conn);
                        printSettings.queryPrinter(1000);

                        if (!printSettings.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Backup Delay: %d\n", printSettings.getBackupDelay());
                            message += String.format(Locale.getDefault(), "Row Offset: %d\n", printSettings.getRowOffset());
                            message += String.format(Locale.getDefault(), "Column Offset: %d\n", printSettings.getColumnOffset());
                            message += String.format(Locale.getDefault(), "Row Adjusted Fine Tune: %d\n", printSettings.getRowAdjustFineTune());
                            message += String.format(Locale.getDefault(), "Column Adjusted Fine Tune: %d\n", printSettings.getColumnAdjustFineTune());
                            message += String.format(Locale.getDefault(), "Present Fine Tune: %d\n", printSettings.getPresentAdjustFineTune());
                            message += String.format(Locale.getDefault(), "Darkness Level: %d\n", printSettings.getDarknessLevel());
                            message += String.format(Locale.getDefault(), "Contrast Level: %d\n", printSettings.getContrastLevel());
                            message += String.format(Locale.getDefault(), "Heat Level: %d\n", printSettings.getHeatLevel());
                            message += String.format(Locale.getDefault(), "Backup Speed: %.1f\n", printSettings.getBackupSpeed());
                            message += String.format(Locale.getDefault(), "Feed Speed: %.1f\n", printSettings.getFeedSpeed());
                            message += String.format(Locale.getDefault(), "Print Speed: %.1f\n", printSettings.getPrintSpeed());
                            message += String.format(Locale.getDefault(), "Slew Speed: %.1f\n", printSettings.getSlewSpeed());
                        }

                        ShowMessageBox(message,"Print Controls");
                    }

                    //System Settings
                    else if (selectedItemIndex == 4){

                        //System Settings
                        SystemSettings_DPL sysSettings = new SystemSettings_DPL(conn);
                        sysSettings.queryPrinter(1000);

                        if (!sysSettings.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Unit Measure: %s\n", sysSettings.getUnitMeasure());
                            message += String.format(Locale.getDefault(), "ESC Sequence Enable: %s\n", sysSettings.getEscapeSequences()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Single Byte Symbol: %s\n", sysSettings.getSingleByteSymbolSet());
                            message += String.format(Locale.getDefault(), "Double Byte Symbol: %s\n", sysSettings.getDoubleByteSymbolSet());
                            message += String.format(Locale.getDefault(), "Disable Symbol Set Value Selection: %s\n", sysSettings.getSymbolSetSelection()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Menu Mode: %s\n", sysSettings.getMenuMode());
                            message += String.format(Locale.getDefault(), "Start of Print Emulation: %s\n", sysSettings.getStartOfPrintEmulation());
                            message += String.format(Locale.getDefault(), "Image mode: %s\n", sysSettings.getImageMode());
                            message += String.format(Locale.getDefault(), "Menu Language: %s\n", sysSettings.getMenuLanguage());
                            message += String.format(Locale.getDefault(), "Display Mode: %s\n", sysSettings.getDisplayMode());
                            message += String.format(Locale.getDefault(), "Block Allocated for Internal Module: %d\n", sysSettings.getInternalModuleSize());
                            message += String.format(Locale.getDefault(), "Scalable Font Cache: %d\n", sysSettings.getScalableFontCache());
                            message += String.format(Locale.getDefault(), "Legacy Emulation: %s\n", sysSettings.getLegacyEmulation());
                            message += String.format(Locale.getDefault(), "Column Emulation: %d\n", sysSettings.getColumnEmulation());
                            message += String.format(Locale.getDefault(), "Row Emulation: %d\n", sysSettings.getRowEmulation());
                            message += String.format(Locale.getDefault(), "Fault Handling Level: %s\n", sysSettings.getFaultHandlingLevel().name());
                            message += String.format(Locale.getDefault(), "Fault Handling Void Distance: %d\n", sysSettings.getFaultHandlingVoidDistance());
                            message += String.format(Locale.getDefault(), "Fault Handling Retry Counts: %d\n", sysSettings.getFaultHandlingRetryCount());
                            message += String.format(Locale.getDefault(), "Font Emulation: %s\n", sysSettings.getFontEmulation().name());
                            message += String.format(Locale.getDefault(), "Input Mode: %s\n", sysSettings.getInputMode().name());
                            message += String.format(Locale.getDefault(), "Retract Delay: %d\n", sysSettings.getRetractDelay());
                            message += String.format(Locale.getDefault(), "Label Rotation: %s\n", sysSettings.getLabelRotation().name());
                            message += String.format(Locale.getDefault(), "Label Store Level: %s\n", sysSettings.getLabelStoreLevel());
                            message += String.format(Locale.getDefault(), "Scalable Font Bolding: %d\n", sysSettings.getScalableFontBolding());
                            message += String.format(Locale.getDefault(), "Format Attribute: %s\n", sysSettings.getFormatAttribute());
                            message += String.format(Locale.getDefault(), "Beeper State: %s\n", sysSettings.getBeeperState());
                            message += String.format(Locale.getDefault(), "Host Timeout: %d\n", sysSettings.getHostTimeout());
                            message += String.format(Locale.getDefault(), "Printer Sleep Timeout: %d\n", sysSettings.getPrinterSleepTimeout());
                            message += String.format(Locale.getDefault(), "Backlight Mode: %s\n", sysSettings.getBacklightMode().name());
                            message += String.format(Locale.getDefault(), "Backlight Timer: %d\n", sysSettings.getBacklightTimer());
                            message += String.format(Locale.getDefault(), "Power Down Timeout: %d\n", sysSettings.getPowerDownTimeout());
                            message += String.format(Locale.getDefault(), "RF Power Down Timeout: %d\n", sysSettings.getRFPowerDownTimeout());
                            message += String.format(Locale.getDefault(), "User Label Mode Enable: %s\n", sysSettings.getUserLabelMode()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Radio Status: %s\n", sysSettings.getRadioPowerState()?"Radio on":"Radio off");
                            message += String.format(Locale.getDefault(), "Supress Auto Reset: %s\n", sysSettings.getSuppressAutoReset()?"Yes":"No");
                        }

                        ShowMessageBox(message,"System Settings");
                    }

                    //Sensor Calibration
                    else if (selectedItemIndex == 5){
                        //Sensor Calibration
                        SensorCalibration_DPL sensorCalibration = new SensorCalibration_DPL(conn);
                        sensorCalibration.queryPrinter(1000);

                        if (!sensorCalibration.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Black Mark Paper value: %d\n", sensorCalibration.getBlackMarkPaperValue());
                            message += String.format(Locale.getDefault(), "Black Mark Sensor Gain value: %d\n", sensorCalibration.getBlackMarkSensorGain());
                            message += String.format(Locale.getDefault(), "Black Mark value: %d\n", sensorCalibration.getBlackMarkValue());
                            message += String.format(Locale.getDefault(), "Gap Sensor Gain value: %d\n", sensorCalibration.getGapSensorGain());
                            message += String.format(Locale.getDefault(), "Gap Sensor Gain should be used with Thermal Transfer Media value: %d\n", sensorCalibration.getGapSensorGainWithThermalTransferMedia());
                            message += String.format(Locale.getDefault(), "Gap Mark Level value: %d\n", sensorCalibration.getGapMarkLevel());
                            message += String.format(Locale.getDefault(), "Gap Mark Level should be used with Thermal Transfer Media value: %d\n", sensorCalibration.getGapMarkLevelWithThermalTransferMedia());
                            message += String.format(Locale.getDefault(), "Paper Level value: %d\n", sensorCalibration.getPaperLevel());
                            message += String.format(Locale.getDefault(), "Paper Level should be used with Thermal Transfer Media value: %d\n", sensorCalibration.getPaperLevelWithThermalTransferMedia());
                            message += String.format(Locale.getDefault(), "Presenter Sensor Gain value: %d\n", sensorCalibration.getPresenterSensorGain());
                            message += String.format(Locale.getDefault(), "Sensor Clear Value: %d\n", sensorCalibration.getSensorClearValue());
                            message += String.format(Locale.getDefault(), "Sensor Clear Value should be used with Thermal Transfer Media: %d\n", sensorCalibration.getSensorClearValueWithThermalTransferMedia());
                            message += String.format(Locale.getDefault(), "Auto Calibration Mode Enable: %s\n", sensorCalibration.getAutoCalibrationMode()?"Yes":"No");
                        }
                        ShowMessageBox(message,"Sensor Calibration");
                    }

                    //Miscellaneous
                    else if (selectedItemIndex == 6){

                        //Misc
                        Miscellaneous_DPL misc = new Miscellaneous_DPL(conn);
                        misc.queryPrinter(1000);

                        if (!misc.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Delay Rate: %d\n", misc.getDelayRate());
                            message += String.format(Locale.getDefault(), "Present Sensor Equipped: %s\n", misc.getPresentSensorEquipped());
                            message += String.format(Locale.getDefault(), "Cutter Equipped: %s\n", misc.getCutterEquipped());
                            message += String.format(Locale.getDefault(), "Control Code: %s\n", misc.getControlCode());
                            message += String.format(Locale.getDefault(), "Start of Print Signal: %s\n", misc.getStartOfPrintSignal().name());
                            message += String.format(Locale.getDefault(), "End of Print Signal: %s\n", misc.getEndOfPrintSignal().name());
                            message += String.format(Locale.getDefault(), "GPIO Slew: %s\n", misc.getGPIOSlew().name());
                            message += String.format(Locale.getDefault(), "Feedback Mode Enable: %s\n", misc.getFeedbackMode()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Comm Heat Commands Enable: %s\n", misc.getCommunicationHeatCommands()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Comm Speed Commands Enable: %s\n", misc.getCommunicationSpeedCommands()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Comm TOF Commands Enable: %s\n", misc.getCommunicationTOFCommands()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "British Pound Enable: %s\n", misc.getBritishPound()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "GPIO Backup Label: %s\n", misc.getGPIOBackupLabel().name());
                            message += String.format(Locale.getDefault(), "Ignore Control Code Enable: %s\n", misc.getIgnoreControlCode()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Sofware Switch Enable: %s\n", misc.getSoftwareSwitch()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Max Length Ignore Enable: %s\n", misc.getMaximumLengthIgnore()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Pause Mode Enable: %s\n", misc.getPauseMode()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Peel Mode Enable: %s\n", misc.getPeelMode()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "USB Mode: %s\n", misc.getUSBMode().name());
                            message += String.format(Locale.getDefault(), "Windows Driver For EZ RLE Enable: %s\n", misc.getWindowsDriverForEZ_RLE()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Hex Dump Enable: %s\n", misc.getHexDumpMode()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Display Mode for IP Host Name: %s\n", misc.getDisplayModeForIPHostname().name());
                        }
                        ShowMessageBox(message,"Miscellaneous");
                    }
                    //Serial Port
                    else if (selectedItemIndex == 7){
                        //SerialPort
                        SerialPortConfiguration_DPL serialConfig = new SerialPortConfiguration_DPL(conn);
                        serialConfig.queryPrinter(1000);

                        if (!serialConfig.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Serial Port A Baud Rate: %s\n", serialConfig.getBaudRate().name());
                            message += String.format(Locale.getDefault(), "Serial Port A Stop Bit: %d\n", serialConfig.getStopBit());
                            message += String.format(Locale.getDefault(), "Serial Port A Data Bits: %d\n", serialConfig.getDataBits());
                            message += String.format(Locale.getDefault(), "Serial Port A Parity: %s\n", serialConfig.getParity().name());
                            message += String.format(Locale.getDefault(), "Serial Port A HandShaking: %s\n", serialConfig.getHandshaking().name());
                        }

                        ShowMessageBox(message,"Serial Port");
                    }

                    //Auto Update
                    else if (selectedItemIndex == 8){
                        //AutoUpdate
                        AutoUpdate_DPL autoUpdate = new AutoUpdate_DPL(conn);
                        autoUpdate.queryPrinter(1000);

                        if (!autoUpdate.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Wireless Upgrade Type: %s\n", autoUpdate.getWirelessUpgradeType().name());
                            message += String.format(Locale.getDefault(), "Status Message Print mode: %s\n", autoUpdate.getStatusMessagePrintMode().name());
                            message += String.format(Locale.getDefault(), "Security Credential File Format: %s\n", autoUpdate.getSecurityCredentialFileFormat().name());
                            message += String.format(Locale.getDefault(), "Config File Name: %s\n", autoUpdate.getConfigurationFileName());
                            message += String.format(Locale.getDefault(), "TFTP Server IP: %s\n", autoUpdate.getTFTPServerIPAddress());
                            message += String.format(Locale.getDefault(), "Upgrade Package Version: %s\n", autoUpdate.getUpgradePackageVersion());
                            message += String.format(Locale.getDefault(), "Beeper Enable: %s\n", autoUpdate.getBeeper()?"Yes":"No");
                            message += String.format(Locale.getDefault(), " FTP Username: %s\n", autoUpdate.getFTPUsername());
                            message += String.format(Locale.getDefault(), "FTP Server Name: %s\n", autoUpdate.getFTPServerName());
                            message += String.format(Locale.getDefault(), "FTP Server Port: %d\n", autoUpdate.getFTPServerPort());
                        }
                        ShowMessageBox(message,"Auto Update");
                    }

                    //Avalanche
                    else if (selectedItemIndex == 9){
                        //Avalanche
                        AvalancheEnabler_DPL avaEnabler = new AvalancheEnabler_DPL(conn);
                        avaEnabler.queryPrinter(1000);

                        if (!avaEnabler.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Agent IP Address: %s\n", avaEnabler.getAgentIPAddress());
                            message += String.format(Locale.getDefault(), "Agent Port: %d\n", avaEnabler.getAgentPort());
                            message += String.format(Locale.getDefault(), "Agent DNS Name: %s\n", avaEnabler.getAgentDNSName());
                            message += String.format(Locale.getDefault(), "Connectivity Type: %s\n", avaEnabler.getConnectivityType().name());
                            message += String.format(Locale.getDefault(), "Printer Name: %s\n", avaEnabler.getPrinterName());
                            message += String.format(Locale.getDefault(), "Printer Model: %s\n", avaEnabler.getPrinterModel());
                            message += String.format(Locale.getDefault(), "Update Package Version: %s\n", avaEnabler.getUpdatePackageVersion());
                            message += String.format(Locale.getDefault(), "Update Mode: %s\n", avaEnabler.getUpdateMode());
                            message += String.format(Locale.getDefault(), "Update Interval: %d\n", avaEnabler.getUpdateInterval());
                            message += String.format(Locale.getDefault(), "Update Package Name: %s\n", avaEnabler.getUpdatePackageName());
                            message += String.format(Locale.getDefault(), "Print Status Result Enable: %s\n", avaEnabler.getPrintStatusResult()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Avalanche Enabler Active: %s\n", avaEnabler.getAvalancheEnablerActive()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Remove old updates: %s\n", avaEnabler.getRemoveOldUpdatesBeforeUpdate()?"Yes":"No");
                        }
                        ShowMessageBox(message,"Avalanche Enable Settings");
                    }
                    //Bluetooth Config
                    else if (selectedItemIndex == 10) {
                        BluetoothConfiguration_DPL btConfig = new BluetoothConfiguration_DPL(conn);
                        btConfig.queryPrinter(1000);

                        if (!btConfig.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else {
                            message += String.format(Locale.getDefault(), "Bluetooth Device Name: %s\n", btConfig.getBluetoothDeviceName());
                            message += String.format(Locale.getDefault(), "Bluetooth Service Name: %s\n", btConfig.getBluetoothServiceName());
                            message += String.format(Locale.getDefault(), "Authentication Type:%s\n", btConfig.getAuthenticationType().name());
                            message += String.format(Locale.getDefault(), "Discoverable: %s\n", btConfig.getDiscoverable()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Connectable: %s\n", btConfig.getConnectable()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Bondable: %s\n", btConfig.getBondable()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Encryption: %s\n", btConfig.getEncryption()?"Yes":"No");
//                            message += String.format(Locale.getDefault(), "PassKey: %s\n", btConfig.getPassKey());
                            message += String.format(Locale.getDefault(), "Inactive Disconnect Time: %d\n",btConfig.getInactiveDisconnectTime());
                            message += String.format(Locale.getDefault(), "Power Down Time: %d\n", btConfig.getPowerDownTime());
                            message += String.format(Locale.getDefault(), "Bluetooth Device Address: %s\n", btConfig.getBluetoothDeviceAddress());
                        }
                        ShowMessageBox(message, "Bluetooth Configuration");
                    }
                    //Network General
                    else if (selectedItemIndex == 11){

                        NetworkGeneralSettings_DPL netGen = new NetworkGeneralSettings_DPL(conn);
                        netGen.queryPrinter(1000);

                        if (!netGen.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Primary Interface: %s\n", netGen.getPrimaryInterface());
//                            message += String.format(Locale.getDefault(), "WiFi Module Type: %s\n", netGen.getWiFiType().name());
                            message += String.format(Locale.getDefault(), "Network Password:%s\n", netGen.getNetworkPassword());
                            message += String.format(Locale.getDefault(), "SNMP Enable: %s\n", netGen.getSNMPEnable()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Telnet Enable: %s\n", netGen.getTelnetEnable()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "FTP Enable: %s\n", netGen.getFTPEnable()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "HTTP Enable: %s\n", netGen.getHTTPEnable()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "LPD Enable: %s\n", netGen.getLPDEnable()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "NetBIOS Enable: %s\n", netGen.getNetBIOSEnable()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Netcenter Enable: %s\n", netGen.getNetcenterEnable()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Gratuitous ARP Period: %d\n", netGen.getGratuitousARPPeriod());
                        }
                        ShowMessageBox(message,"Network General Settings");
                    }
                    //Wifi
                    else if (selectedItemIndex == 12){

                        NetworkWirelessSettings_DPL wifiSettings = new NetworkWirelessSettings_DPL(conn);
                        wifiSettings.queryPrinter(1000);

                        if (!wifiSettings.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            //DNS Settings
                            message += String.format(Locale.getDefault(), "Static DNS Enable: %s\n", wifiSettings.getStaticDNS()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Preferred DNS Server: %s\n", wifiSettings.getPreferredDNSServerIP());
                            message += String.format(Locale.getDefault(), "Secondary DNS Server: %s\n", wifiSettings.getSecondaryDNSServerIP());
                            message += String.format(Locale.getDefault(), "DNS Suffix: %s\n", wifiSettings.getDNSSuffix());

                            //Network Settings
                            message += String.format(Locale.getDefault(), "Inactive Timeout: %d\n", wifiSettings.getInactiveTimeout());
                            message += String.format(Locale.getDefault(), "IP Address Method: %s\n", wifiSettings.getIPAddressMethod());
                            message += String.format(Locale.getDefault(), "Active IP Address: %s\n", wifiSettings.getActiveIPAddress());
                            message += String.format(Locale.getDefault(), "Active Subnet Mask: %s\n", wifiSettings.getActiveSubnetMask());
                            message += String.format(Locale.getDefault(), "Active Gateway Address: %s\n", wifiSettings.getActiveSubnetMask());
                            message += String.format(Locale.getDefault(), "Printer DNS name: %s\n", wifiSettings.getPrinterDNSName());
                            message += String.format(Locale.getDefault(), "Register to DNS: %s\n", wifiSettings.getRegisterToDNS()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Active Gateway: %s\n", wifiSettings.getActiveGatewayAddress());
                            message += String.format(Locale.getDefault(), "UDP Port: %d\n", wifiSettings.getUDPPort());
                            message += String.format(Locale.getDefault(), "TCP Port: %d\n", wifiSettings.getTCPPort());
                            message += String.format(Locale.getDefault(), "Use DNS Suffix: %s\n", wifiSettings.getUseDNSSuffix()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Enable Connection Status: %s\n", wifiSettings.getEnableConnectionStatusReport()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "DHCP User Class Option: %s\n", new String(wifiSettings.getDHCPUserClassOption()));
                            message += String.format(Locale.getDefault(), "Static IP Address: %s\n", wifiSettings.getStaticIPAddress());
                            message += String.format(Locale.getDefault(), "Static Subnet Mask: %s\n", wifiSettings.getStaticSubnetMask());
                            message += String.format(Locale.getDefault(), "Static Gateway: %s\n", wifiSettings.getStaticGateway());
                            message += String.format(Locale.getDefault(), "LPD Port: %d\n", wifiSettings.getLPDPort());
                            message += String.format(Locale.getDefault(), "LPD Enable: %s\n", wifiSettings.getLPDEnable()?"Yes":"No");


                            //Wifi Settings
                            message += String.format(Locale.getDefault(), "Network Type: %s\n", wifiSettings.getNetworkType());
                            message += String.format(Locale.getDefault(), "ESSID: %s\n", wifiSettings.getESSID());
                            message += String.format(Locale.getDefault(), "Network Authentication: %s\n", wifiSettings.getNetworkAuthenticationType().name());
                            message += String.format(Locale.getDefault(), "EAP Type: %s\n", wifiSettings.getEAPType().name());
                            message += String.format(Locale.getDefault(), "Phase 2 Method: %s\n", wifiSettings.getPhase2Method().name());
                            message += String.format(Locale.getDefault(), "WEP Authentication Type: %s\n", wifiSettings.getWEPAuthenticationMethod().name());
                            message += String.format(Locale.getDefault(), "WEP Data Encryption: %s\n", wifiSettings.getWEPDataEncryption()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Selected WEP Key: %d\n", wifiSettings.getWEPKeySelected().value());
                            message += String.format(Locale.getDefault(), "Show Signal Strength: %s\n", wifiSettings.getShowSignalStrength()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Power Saving Mode: %s\n", wifiSettings.getPowerSavingMode()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Group Cipher: %s\n", wifiSettings.getGroupCipher().name());
                            message += String.format(Locale.getDefault(), "MAC Address: %s\n", wifiSettings.getWiFiMACAddress());
                            message += String.format(Locale.getDefault(), "Regulatory Domain: %s\n", wifiSettings.getRegulatoryDomain().name());
                            message += String.format(Locale.getDefault(), "Radio Mode: %s\n", wifiSettings.getRadioMode().name());
                            message += String.format(Locale.getDefault(), "Max Active Channel Dwell Time: %d\n", wifiSettings.getMaxActiveChannelDwellTime());
                            message += String.format(Locale.getDefault(), "Min Active Channel Dwell Time: %d\n", wifiSettings.getMinActiveChannelDwellTime());
                            message += String.format(Locale.getDefault(), "Active Scanning Radio Channel: %s\n", wifiSettings.getRadioChannelSelection());
                            message += String.format(Locale.getDefault(), "Use Hex PSK: %s\n", wifiSettings.getUseHexPSK()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "WiFi Testing Mode: %s\n", wifiSettings.getWiFiTestingMode()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Use Client Certificate: %s\n", wifiSettings.getUseClientCertificate()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Signal Strength: %s\n", wifiSettings.getSignalStrength());
                            message += String.format(Locale.getDefault(), "SSL Port: %d\n", wifiSettings.getSSLPort());
                            message += String.format(Locale.getDefault(), "DHCP Host Name: %s\n", wifiSettings.getDHCPHostName());
                        }

                        ShowMessageBox(message,"Network Wireless Settings");
                    }
                    else if (selectedItemIndex == 13)
                    {
                        PrinterStatus_DPL printerStatus = new PrinterStatus_DPL(conn);
                        printerStatus.queryPrinter( 3000);

                        if(!printerStatus.getValid())
                        {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            PrinterStatus currentStatus = printerStatus.getCurrentStatus();
                            switch(currentStatus)
                            {
                                case PrinterReady:
                                    message += "Printer is ready.";
                                    break;
                                case BusyPrinting:
                                    message += "Printer is busy.";
                                    break;
                                case PaperOutFault:
                                    message += "Printer is out of paper.";
                                    break;
                                case PrintHeadUp:
                                    message += "Print head lid is open.";
                                    break;
                                default:
                                    message += "Printer status unknown";
                                    break;
                            }
                        }
                        ShowMessageBox(message,"Printer Status");
                    }
                }
                //EZ and LP mode
                else {

                    //Avalanche Settings
                    if (selectedItemIndex == 0){

                        AvalancheSettings avaSettings = new AvalancheSettings(conn);
                        avaSettings.queryPrinter(1000);

                        if (!avaSettings.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Agent IP: %s\n", avaSettings.getAgentIP());
                            message += String.format(Locale.getDefault(), "Show All Data on Self Test: %s\n", avaSettings.getShowAllData()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Agent Name: %s\n", avaSettings.getAgentName());
                            message += String.format(Locale.getDefault(), "Agent Port: %d\n", avaSettings.getAgentPort());
                            message += String.format(Locale.getDefault(), "Connection Type: %s\n", avaSettings.getConnectionType().name());
                            message += String.format(Locale.getDefault(), "Avalanche Enable: %s\n", avaSettings.getIsAvalancheEnabled()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Printer Name: %s\n", avaSettings.getPrinterName());
                            message += String.format(Locale.getDefault(), "Printer Model: %s\n", avaSettings.getPrinterModelName());
                            message += String.format(Locale.getDefault(), "Is Prelicensed: %s\n", avaSettings.getIsPrelicensed()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Printer Result Flag: %s\n", avaSettings.getPrinterResultFlag()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Update Interval: %d\n", avaSettings.getUpdateInterval());
                            message += String.format(Locale.getDefault(), "Update Mode: %s\n", avaSettings.getUpdateFlags().name());
                            message += String.format(Locale.getDefault(), "Is Wired: %s\n", avaSettings.getIsWired()?"Yes":"No");
                        }
                        ShowMessageBox(message,"Avalanche Settings");
                    }

                    //Battery Condition
                    else if (selectedItemIndex == 1){

                        BatteryCondition battCond = new BatteryCondition(conn);
                        battCond.queryPrinter(1000);

                        if (!battCond.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Power Source Plugged in: %s\n", battCond.getChargerConnected()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Power Source: %s\n", battCond.getPowerSource().name());
                            message += String.format(Locale.getDefault(), "Battery Temperature: %f\n", battCond.getBatteryTemperature());
                            message += String.format(Locale.getDefault(), "Voltage Battery: %f\n", battCond.getVoltageBatterySingle());
                            message += String.format(Locale.getDefault(), "Voltage Battery 1: %f\n", battCond.getVoltageBattery1());
                            message += String.format(Locale.getDefault(), "Votlage Battery 2: %f\n", battCond.getVoltageBattery2());
                            message += String.format(Locale.getDefault(), "Voltage of Battery Eliminator: %f\n", battCond.getVoltageBatteryEliminator());
                        }
                        ShowMessageBox(message,"Battery Condition");

                    }
                    //Bluetooth Config
                    else if (selectedItemIndex == 2){
                        BluetoothConfiguration btConfig = new BluetoothConfiguration(conn);
                        btConfig.queryPrinter(1000);

                        if (!btConfig.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Authentication Enable: %s\n", btConfig.getAuthentication()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "MAC Address: %s\n", btConfig.getBluetoothAddress());
                            message += String.format(Locale.getDefault(), "Bondable: %s\n", btConfig.getBondable()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Connectable: %s\n", btConfig.getConnectable()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Discoverable: %s\n", btConfig.getDiscoverable()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Friendly Name: %s\n", btConfig.getFriendlyName());
                            message += String.format(Locale.getDefault(), "Inactivity timeout: %d\n", btConfig.getInactivityTimeout());
                            message += String.format(Locale.getDefault(), "Passkey enable: %s\n", btConfig.getPasskey()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Bluetooth Profile: %s\n", btConfig.getProfile());
                            message += String.format(Locale.getDefault(), "Service Name: %s\n", btConfig.getServiceName());
                            message += String.format(Locale.getDefault(), "Watchdog: %s\n", btConfig.getWatchdogPeriod()?"Yes":"No");
                        }
                        ShowMessageBox(message,"Bluetooth Config");
                    }

                    //Font List
                    else if (selectedItemIndex == 3){
                        FontList fontList = new FontList(conn);
                        fontList.queryPrinter(1000);

                        if (!fontList.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            List<FontData> files = fontList.getFonts();
                            for (FontData font : files) {
                                message += String.format(Locale.getDefault(), "Five Character Name: %s\n", font.getFiveCharacterName());
                                message += String.format(Locale.getDefault(), "One Character Name: %s\n", font.getOneCharacterName());
                                message += String.format(Locale.getDefault(), "Memory Location: %s\n", font.getMemoryLocation());
                                message += String.format(Locale.getDefault(), "User Date: %s\n", font.getUserDate());
                                message += String.format(Locale.getDefault(), "Description: %s\n", font.getUserDescription());
                                message += String.format(Locale.getDefault(), "Version: %s\n", font.getUserVersion());
                                message += "\n";
                            }
                        }
                        ShowMessageBox(message,"Font List");
                    }
                    //Format list
                    else if (selectedItemIndex == 4){
                        FormatList formatList = new FormatList(conn);
                        formatList.queryPrinter(1000);

                        if (!formatList.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            List<FormatData> files = formatList.getFormats();
                            for (FormatData formatData : files) {
                                message += String.format(Locale.getDefault(), "Five Character Name: %s\n", formatData.getFiveCharacterName());
                                message += String.format(Locale.getDefault(), "One Character Name: %s\n", formatData.getOneCharacterName());
                                message += String.format(Locale.getDefault(), "Memory Location: %s\n", formatData.getMemoryLocation());
                                message += String.format(Locale.getDefault(), "User Date: %s\n", formatData.getUserDate());
                                message += String.format(Locale.getDefault(), "Description: %s\n", formatData.getUserDescription());
                                message += String.format(Locale.getDefault(), "Version: %s\n", formatData.getUserVersion());
                                message += "\n";
                            }
                        }
                        ShowMessageBox(message,"Format List");

                    }
                    //General Config
                    else if (selectedItemIndex == 5){
                        GeneralConfiguration genConfig = new GeneralConfiguration(conn);
                        genConfig.queryPrinter(1000);

                        if (!genConfig.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "White Space Advance Enable: %s\n", genConfig.getWhiteSpaceAdvance()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Darkness Adjustment: %s\n", genConfig.getDarknessAdjustment().name());
                            message += String.format(Locale.getDefault(), "Form Feed Enable: %s\n", genConfig.getFormFeed()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Charger Beep Enable: %s\n", genConfig.getChargerBeep()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Sound Enable(Beeper On): %s\n", genConfig.getSoundEnabled()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Lines Per Page: %d\n", genConfig.getLinesPerPage());
                            message += String.format(Locale.getDefault(), "Print Job Status Report Enable: %s\n", genConfig.getEZPrintJobStatusReport()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Default Protocol: %s\n", genConfig.getDefaultProtocol());
                            message += String.format(Locale.getDefault(), "Self Test Print Language: %d\n", genConfig.getSelfTestPrintLanguage());
                            message += String.format(Locale.getDefault(), "Form Feed Centering: %s\n", genConfig.getFormFeedCentering()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Form Feed Button Disabled: %s\n", genConfig.getFormfeedButtonDisabled()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Power Button Disabled: %s\n", genConfig.getPowerButtonDisabled()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "RF Button Disabled: %s\n", genConfig.getPowerButtonDisabled()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "QStop Multiplier: %d\n", genConfig.getQStopMultiplier());
                            message += String.format(Locale.getDefault(), "RF Timeout: %d\n", genConfig.getRFPowerTimeout());
                            message += String.format(Locale.getDefault(), "System Timeout: %s\n", genConfig.getSystemTimeout());
                            message += String.format(Locale.getDefault(), "Special Test Print: %d\n", genConfig.getSpecialTestPrint());
                            message += String.format(Locale.getDefault(), "Paper Out Beep: %s\n", genConfig.getPaperOutBeep().name());
                            message += String.format(Locale.getDefault(), "USB Class: %s\n", genConfig.getUSBClass().name());
                            message += String.format(Locale.getDefault(), "Using USB: %s\n", genConfig.getUsingUSB()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Deep Sleep Enable: %s\n", genConfig.getDeepSleep()?"Yes":"No");
                        }
                        ShowMessageBox(message,"General Configuration");

                    }
                    //General status
                    else if (selectedItemIndex == 6){
                        GeneralStatus genStatus = new GeneralStatus(conn);
                        genStatus.queryPrinter(1000);

                        if (!genStatus.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Error Status: %s\n", genStatus.getErrorStatus().name());
                            message += String.format(Locale.getDefault(), "Printer Status: %s\n", genStatus.getPrinterStatus().name());
                            message += String.format(Locale.getDefault(), "Head Lever Position: %s\n", genStatus.getHeadLeverPosition());
                            message += String.format(Locale.getDefault(), "Paper Present: %s\n", genStatus.getPaperPresent());
                            message += String.format(Locale.getDefault(), "Paper Jam: %s\n", genStatus.getPaperJam());
                            message += String.format(Locale.getDefault(), "Remaining RAM: %d\n", genStatus.getRemainingRAM());
                            message += String.format(Locale.getDefault(), "Battery Temp and Voltage Status: %s\n", genStatus.getBatteryTempandVoltageStatus().name());
                        }
                        ShowMessageBox(message,"General Status");
                    }
                    //Graphic List
                    else if (selectedItemIndex == 7){
                        GraphicList graphList = new GraphicList(conn);
                        graphList.queryPrinter(1000);

                        if (!graphList.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            List<GraphicData> files = graphList.getGraphics();
                            for (GraphicData graphic : files) {
                                message += String.format(Locale.getDefault(), "Name: %s\n", graphic.getFiveCharacterName());
                                message += String.format(Locale.getDefault(), "Name: %s\n", graphic.getOneCharacterName());
                                message += String.format(Locale.getDefault(), "Memory Location: %s\n", graphic.getMemoryLocation());
                                message += String.format(Locale.getDefault(), "User Date: %s\n", graphic.getUserDate());
                                message += String.format(Locale.getDefault(), "Description: %s\n", graphic.getUserDescription());
                                message += String.format(Locale.getDefault(), "Version: %s\n", graphic.getUserVersion());
                                message += "\n";
                            }
                        }

                        ShowMessageBox(message,"Graphic List");
                    }
                    //IrDA Config
                    else if (selectedItemIndex == 8){
                        IrDAConfiguration irDAConfig = new IrDAConfiguration(conn);
                        irDAConfig.queryPrinter(1000);

                        if (!irDAConfig.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Direct Version: %s\n", irDAConfig.getDirectVersion());
                            message += String.format(Locale.getDefault(), "IrDA Name: %s\n", irDAConfig.getIrDAName());
                            message += String.format(Locale.getDefault(), "IrDA Nickname: %s\n", irDAConfig.getIrDANickname());
                            message += String.format(Locale.getDefault(), "IrDA Version: %s\n", irDAConfig.getIrDAVersion());
                            message += String.format(Locale.getDefault(), "Protocol: %s\n", irDAConfig.getProtocol());
                        }
                        ShowMessageBox(message,"IrDA Config");
                    }
                    //Label Config
                    else if (selectedItemIndex == 9){
                        LabelConfiguration labelConfig = new LabelConfiguration(conn);
                        labelConfig.queryPrinter(1000);

                        if (!labelConfig.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Backup Distance: %d\n", labelConfig.getBackUpDistance());
                            message += String.format(Locale.getDefault(), "Use Presenter: %s\n", labelConfig.getUsePresenter()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Auto QMark Advance: %s\n", labelConfig.getAutoQMarkAdvance()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Backup Offset: %d\n", labelConfig.getBackupOffset());
                            message += String.format(Locale.getDefault(), "Horizontal Offset: %d\n", labelConfig.getHorizontalOffset());
                            message += String.format(Locale.getDefault(), "QMark Stop Length: %d\n", labelConfig.getQMarkStopLength());
                            message += String.format(Locale.getDefault(), "Additional Self Test Prints: %d\n", labelConfig.getAdditionalSelfTestPrints());
                            message += String.format(Locale.getDefault(), "Max QMark Advance: %d\n", labelConfig.getMaximumQMarkAdvance());
                            message += String.format(Locale.getDefault(), "QMARKB offset: %d\n", labelConfig.getQMARKBOffset());
                            message += String.format(Locale.getDefault(), "QMARKG Offset: %d\n", labelConfig.getQMARKGOffset());
                            message += String.format(Locale.getDefault(), "QMARKT Offset: %d\n", labelConfig.getQMARKTOffset());
                            message += String.format(Locale.getDefault(), "White QMark Enable: %s\n", labelConfig.getWhiteQMark()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Paperout Sensor: %s\n", labelConfig.getPaperoutSensor().name());
                            message += String.format(Locale.getDefault(), "Paper Stock Type: %s\n", labelConfig.getPaperStockType().name());
                            message += String.format(Locale.getDefault(), "Presenter Timeout: %d\n", labelConfig.getPresenterTimeout());
                            message += String.format(Locale.getDefault(), "Auto QMark Backup: %s\n", labelConfig.getAutoQMarkBackup()?"Yes":"No");
                        }
                        ShowMessageBox(message,"Label Config");

                    }
                    //Magnetic Card
                    else if (selectedItemIndex == 10){
                        MagneticCardConfiguration magConfig = new MagneticCardConfiguration(conn);
                        magConfig.queryPrinter(1000);

                        if (!magConfig.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Auto Print: %s\n", magConfig.getAutoPrint()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Card Read Direction: %s\n", magConfig.getCardReadDirection());
                            message += String.format(Locale.getDefault(), "Magnetic Card Enabled: %s\n", magConfig.getEnabled()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Auto Send: %s\n", magConfig.getAutoSend()?"On":"Off");
                            message += String.format(Locale.getDefault(), "Track 1 Enabled: %s\n", magConfig.getTrack1Enabled()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Track 2 Enabled: %s\n", magConfig.getTrack2Enabled()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Track 3 Enabled: %s\n", magConfig.getTrack3Enabled()?"Yes":"No");
                        }
                        ShowMessageBox(message,"Magnetic Card Config");
                    }
                    //Magnetic Card Data
                    else if (selectedItemIndex == 11){
                        MagneticCardData magCardData = new MagneticCardData(conn);
                        magCardData.queryPrinter(1000);

                        if (!magCardData.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Track 1 Data: %s\n", magCardData.getTrack1Data());
                            message += String.format(Locale.getDefault(), "Track 2 Data: %s\n", magCardData.getTrack2Data());
                            message += String.format(Locale.getDefault(), "Track 3 Data: %s\n", magCardData.getTrack3Data());
                        }
                        ShowMessageBox(message,"Magnetic Card Data");

                    }
                    //Manufacturing Date
                    else if (selectedItemIndex == 12){
                        ManufacturingDate manuDate = new ManufacturingDate(conn);
                        manuDate.queryPrinter(1000);

                        if (!manuDate.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Manufacturing Date: %s\n", manuDate.getMD());
                        }
                        ShowMessageBox(message,"Manufacturing Date");
                    }
                    //Memory status
                    else if (selectedItemIndex == 13){
                        MemoryStatus memStatus = new MemoryStatus(conn);
                        memStatus.queryPrinter(1000);

                        if (!memStatus.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Download memory remaining: %d\n", memStatus.getDownloadMemoryRemaining());
                            message += String.format(Locale.getDefault(), "Download memory total: %d\n", memStatus.getDownloadMemoryTotal());
                            message += String.format(Locale.getDefault(), "EEPROM Size: %d\n", memStatus.getEEPROMSize());
                            message += String.format(Locale.getDefault(), "Flash Memory Size: %d\n", memStatus.getFlashMemorySize());
                            message += String.format(Locale.getDefault(), "RAM size: %d\n", memStatus.getRAMSize());
                            message += String.format(Locale.getDefault(), "Flash type: %s\n", memStatus.getFlashType());
                            message += String.format(Locale.getDefault(), "Download Format Memory Remaining: %d\n", memStatus.getDownloadFormatMemoryRemaining());
                            message += String.format(Locale.getDefault(), "Download Format Memory Total: %d\n", memStatus.getDownloadFormatMemoryTotal());
                        }
                        ShowMessageBox(message,"Memory status");

                    }
                    //Printer Options
                    else if (selectedItemIndex == 14){
                        PrinterOptions printerOpt = new PrinterOptions(conn);
                        printerOpt.queryPrinter(1000);

                        if (!printerOpt.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "SCR Device: %s\n", printerOpt.getSCRDevice().name());
                            message += String.format(Locale.getDefault(), "CF Device: %s\n", printerOpt.getCFDevice().name());
                            message += String.format(Locale.getDefault(), "Printer Description: %s\n", printerOpt.getPrinterDescription());
                            message += String.format(Locale.getDefault(), "Part Number: %s\n", printerOpt.getPartNumber());
                            message += String.format(Locale.getDefault(), "Serial Number: %s\n", printerOpt.getSerialNumber());
                            message += String.format(Locale.getDefault(), "Printer Type: %s\n", printerOpt.getPrinterType().name());
                            message += String.format(Locale.getDefault(), "SPI Device: %d\n", printerOpt.getSPIDevice());
                            message += String.format(Locale.getDefault(), "Manufacturing Date: %s\n", printerOpt.getManufacturingDate());
                            message += String.format(Locale.getDefault(), "Text Fixture String: %s\n", printerOpt.getTextFixtureString());
                            message += String.format(Locale.getDefault(), "SDIO Device: %s\n", printerOpt.getSDIODevice().name());
                            message += String.format(Locale.getDefault(), "Certification Flag Status: %s\n", printerOpt.getCertificationFlagStatus()?"On":"Off");
                        }
                        ShowMessageBox(message,"Printer Options");
                    }
                    //PrintHead Status
                    else if (selectedItemIndex == 15){
                        PrintheadStatus printHeadStats = new PrintheadStatus(conn);
                        printHeadStats.queryPrinter(1000);

                        if (!printHeadStats.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "DPI: %d\n", printHeadStats.getDPI());
                            message += String.format(Locale.getDefault(), "PrintHead Model: %s\n", printHeadStats.getPrintheadModel());
                            message += String.format(Locale.getDefault(), "Print Time: %d\n", printHeadStats.getPrintTime());
                            message += String.format(Locale.getDefault(), "PrintHead Pins: %d\n", printHeadStats.getPrintheadPins());
                            message += String.format(Locale.getDefault(), "PrintHead Temperature: %f\n", printHeadStats.getPrintheadTemperature());
                            message += String.format(Locale.getDefault(), "PrintHead Width: %d\n", printHeadStats.getPrintheadWidth());
                            message += String.format(Locale.getDefault(), "Page Width: %d\n", printHeadStats.getPageWidth());
                        }
                        ShowMessageBox(message,"Print Head Status");

                    }
                    //Serial Number
                    else if (selectedItemIndex == 16){
                        SerialNumber serialNum = new SerialNumber(conn);
                        serialNum.queryPrinter(1000);

                        if (!serialNum.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Serial Number: %s\n", serialNum.getSN());
                        }
                        ShowMessageBox(message,"Serial Number");
                    }
                    //Smart Card Config
                    else if (selectedItemIndex == 17){
                        SmartCardConfiguration smartCardConfig = new SmartCardConfiguration(conn);
                        smartCardConfig.queryPrinter(1000);

                        if (!smartCardConfig.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Command Format: %s\n", smartCardConfig.getCommandFormat());
                            message += String.format(Locale.getDefault(), "Enable: %s\n", smartCardConfig.getEnabled()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Memory Tye: %s\n", smartCardConfig.getMemoryType());
                            message += String.format(Locale.getDefault(), "Response Format: %s\n", smartCardConfig.getResponseFormat());
                            message += String.format(Locale.getDefault(), "Smart Card Protocol: %s\n", smartCardConfig.getProtocol());
                            message += String.format(Locale.getDefault(), "Smart Card Type: %s\n", smartCardConfig.getType());
                        }
                        ShowMessageBox(message,"Smart Card Config");
                    }
                    //Serial Config
                    else if (selectedItemIndex == 18){
                        GeneralConfiguration genConfig = new GeneralConfiguration(conn);
                        genConfig.queryPrinter(600);

                        if (!genConfig.getValid())
                        {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Baud Rate: %s\r\n", genConfig.getBaudRate());
                            message += String.format(Locale.getDefault(), "Handshake: %s\r\n", genConfig.getRS232Handshake());
                            message += String.format(Locale.getDefault(), "Data Bits: %s\r\n", genConfig.getRS232DataBits());
                            message += String.format(Locale.getDefault(), "Parity: %s\r\n", genConfig.getRS232Parity());
                        }
                        ShowMessageBox(message,"Serial Port Config");
                    }
                    //TCPIPStatus
                    else if (selectedItemIndex == 19){
                        TCPIPStatus tcpStatus = new TCPIPStatus(conn);
                        tcpStatus.queryPrinter(1000);

                        if (!tcpStatus.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Wireless Card Info: %s\n", tcpStatus.getWirelessCardInfo());
                            message += String.format(Locale.getDefault(), "Valid Cert. Present: %s\n", tcpStatus.getValidCertificatePresent()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Conn. Reporting Enable: %s\n", tcpStatus.getConnectionReporting()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Acquired IP: %s\n", tcpStatus.getAcquireIP().name());
                            message += String.format(Locale.getDefault(), "Radio Disable: %s\n", tcpStatus.getRadioDisabled()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "ESSID: %s\n", tcpStatus.getESSID());
                            message += String.format(Locale.getDefault(), "EAP Type: %s\n", tcpStatus.getEAPType().name());
                            message += String.format(Locale.getDefault(), "Gateway Address: %s\n", tcpStatus.getGatewayAddress());
                            message += String.format(Locale.getDefault(), "IP Address: %s\n", tcpStatus.getIPAddress());
                            message += String.format(Locale.getDefault(), "Inactivity Timeout: %d\n", tcpStatus.getInactivityTimeout());
                            message += String.format(Locale.getDefault(), "Key to Use: %d\n", tcpStatus.getKeyToUse().value());
                            message += String.format(Locale.getDefault(), "Key 1 Type: %s\n", tcpStatus.getKey1Type().name());
                            message += String.format(Locale.getDefault(), "Key 2 Type: %s\n", tcpStatus.getKey2Type().name());
                            message += String.format(Locale.getDefault(), "Key 3 Type: %s\n", tcpStatus.getKey3Type().name());
                            message += String.format(Locale.getDefault(), "Key 4 Type: %s\n", tcpStatus.getKey4Type().name());
                            message += String.format(Locale.getDefault(), "Subnet Mask: %s\n", tcpStatus.getSubnetMask());
                            message += String.format(Locale.getDefault(), "MAC Address: %s\n", tcpStatus.getMACAddress());
                            message += String.format(Locale.getDefault(), "Station Name: %s\n", tcpStatus.getStationName());
                            message += String.format(Locale.getDefault(), "Network Authentication: %s\n", tcpStatus.getNetworkAuthentication().name());
                            message += String.format(Locale.getDefault(), "TCP Printing Port: %d\n", tcpStatus.getTCPPrintingPort());
                            message += String.format(Locale.getDefault(), "Power Saving Mode: %s\n", tcpStatus.getPowerSavingMode()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Phase 2 Method: %s\n", tcpStatus.getPhase2Method().name());
                            message += String.format(Locale.getDefault(), "UDP Printing Port: %d\n", tcpStatus.getUDPPrintingPort());
                            message += String.format(Locale.getDefault(), "Card Powered: %s\n", tcpStatus.getCardPowered()?"On":"Off");
                            message += String.format(Locale.getDefault(), "Signal Quality Indicator: %s\n", tcpStatus.getSignalQualityIndicator()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Authentication Algorithm: %s\n", tcpStatus.getAuthenticationAlgorithm().name());
                            message += String.format(Locale.getDefault(), "Station Type: %s\n", tcpStatus.getNetworkType().name());
                            message += String.format(Locale.getDefault(), "Encryption Enabled: %s\n", tcpStatus.getEncryptionEnabled()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Current Certificate CRC: %s\n", tcpStatus.getCurrentCertificateCRC());
                            message += String.format(Locale.getDefault(), "DNS1 Address: %s\n", tcpStatus.getDNS1Address());
                            message += String.format(Locale.getDefault(), "Register to DNS: %s\n", tcpStatus.getRegisterToDNS()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "DNS2 Address: %s\n", tcpStatus.getDNS2Address());
                            message += String.format(Locale.getDefault(), "Static DNS Enable: %s\n", tcpStatus.getStaticDNS()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "Group Cipher: %s\n", tcpStatus.getGroupCipher().name());
                            message += String.format(Locale.getDefault(), "Radio Type: %s\n", tcpStatus.getRadioType().name());
                            message += String.format(Locale.getDefault(), "Use DNS: %s\n", tcpStatus.getUseDNS()?"Yes":"No");
                            message += String.format(Locale.getDefault(), "DNS Suffix: %s\n", tcpStatus.getDNSSuffix());
                            message += String.format(Locale.getDefault(), "Encryption Key Size: %d\n", tcpStatus.getEncryptionKeySize());
                            message += String.format(Locale.getDefault(), "Encryption Key Type: %d\n", tcpStatus.getEncryptionKeyType());
                        }
                        ShowMessageBox(message,"TCP/IP Status");

                    }
                    //Upgrade Data
                    else if (selectedItemIndex == 20){
                        UpgradeData upgradeData = new UpgradeData(conn);
                        upgradeData.queryPrinter(1000);

                        if (!upgradeData.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Path and File: %s\n", upgradeData.getPathAndFile());
                            message += String.format(Locale.getDefault(), "Server IP: %s\n", upgradeData.getServerIPAddress());
                            message += String.format(Locale.getDefault(), "Server Port: %d\n", upgradeData.getServerPort());
                            message += String.format(Locale.getDefault(), "Upgrade Type: %s\n", upgradeData.getDataType().name());
                            message += String.format(Locale.getDefault(), "Upgrade Package Version: %s\n", upgradeData.getVersion());
                        }
                        ShowMessageBox(message,"Auto Update Settings");

                    }
                    //Version Info
                    else if (selectedItemIndex == 21){
                        VersionInformation versionInfo = new VersionInformation(conn);
                        versionInfo.queryPrinter(1000);

                        if (!versionInfo.getValid()) {
                            message += "No response from printer\r\n";
                        }
                        else
                        {
                            message += String.format(Locale.getDefault(), "Boot Version: %s\n", versionInfo.getBootVersion());
                            message += String.format(Locale.getDefault(), "Comm Controller Version: %s\n", versionInfo.getCommControllerVersion());
                            message += String.format(Locale.getDefault(), "Download version: %s\n", versionInfo.getDownloadVersion());
                            message += String.format(Locale.getDefault(), "Firmware version: %s\n", versionInfo.getFirmwareVersion());
                            message += String.format(Locale.getDefault(), "Hardware Controller Version: %s\n", versionInfo.getHardwareControllerVersion());
                            message += String.format(Locale.getDefault(), "SCR Version: %s\n", versionInfo.getSCRVersion());
                            message += String.format(Locale.getDefault(), "Build Timestamp: %s\n", versionInfo.getBuildTimestamp());
                        }

                        ShowMessageBox(message,"Version Info");
                    }
                }
                //signals to close connection
                conn.close();
                DisplayPrintingStatusMessage("Query success.");
                EnableControls(true);
            }

        }
        catch (Exception e) {
            //signals to close connection
            if(conn != null)
                conn.close();
            e.printStackTrace();
            DisplayPrintingStatusMessage("Error: " + e.getMessage());
            ShowMessageBox(e.getMessage(),"Application Error");
            EnableControls(true);
        }
    }// run()


}
