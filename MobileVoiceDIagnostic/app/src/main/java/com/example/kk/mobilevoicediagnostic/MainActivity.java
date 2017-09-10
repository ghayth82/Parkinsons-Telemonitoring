package com.example.kk.mobilevoicediagnostic;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.SEND_SMS;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.SystemClock.currentThreadTimeMillis;
import static android.provider.AlarmClock.EXTRA_MESSAGE;
import static com.example.kk.mobilevoicediagnostic.R.id.nav_instructions;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SensorEventListener {

    Button buttonPlayStopLastRecordAudio,
            buttonReset, buttonUpload;
    ImageButton buttonStartStop;
    String AudioSavePathInDevice = null;
    MediaRecorder mediaRecorder;
    public static final int RequestPermissionCode = 1;
    private StorageReference mStorageRef;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private String phoneNumber;
    private String medications;
    MediaPlayer mediaPlayer;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        medications = intent.getStringExtra(EXTRA_MESSAGE);
        phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);

        buttonStartStop = (ImageButton) findViewById(R.id.button);
        buttonStartStop.setImageResource(R.drawable.record);
        buttonStartStop.setTag("Start");

        buttonPlayStopLastRecordAudio = (Button) findViewById(R.id.button2);
        buttonReset = (Button)findViewById(R.id.button3);
        buttonUpload = (Button)findViewById(R.id.button4);

        buttonPlayStopLastRecordAudio.setEnabled(false);
        buttonReset.setEnabled(false);
        buttonUpload.setEnabled(false);

        textView = (TextView)findViewById(R.id.countdown);
        textView.setText("Start");

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        buttonStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {

                if(checkPermission()) {
                    if(buttonStartStop.getTag().toString().equalsIgnoreCase("start")) {
                        AudioSavePathInDevice =
                                Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
                                        + "AudioRecording.wav";

                        MediaRecorderReady();

                        try {
                            mediaRecorder.prepare();
                            mediaRecorder.start();
                        } catch (IllegalStateException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                        buttonStartStop.setImageResource(R.drawable.recordstop);
                        buttonStartStop.setTag("stop");
                        buttonPlayStopLastRecordAudio.setEnabled(false);
                        onStartClick(view);

                        Toast.makeText(MainActivity.this, "Recording started",
                                Toast.LENGTH_LONG).show();

                        new CountDownTimer(10000, 1000) {

                            public void onTick(long millisUntilFinished) {
                                textView.setText(Integer.toString((int)millisUntilFinished/1000));
                            }

                            public void onFinish() {
                                buttonStartStop.setTag("Start");
                                textView.setText("Done");

                                mediaRecorder.stop();
                                buttonStartStop.setImageResource(R.drawable.record);
                                buttonStartStop.setTag("start");
                                buttonPlayStopLastRecordAudio.setEnabled(true);
                                buttonStartStop.setEnabled(true);
                                buttonReset.setEnabled(true);
                                buttonUpload.setEnabled(true);
                                buttonPlayStopLastRecordAudio.setEnabled(true);
                                onStopClick(view);

                                Toast.makeText(MainActivity.this, "Recording Completed",
                                        Toast.LENGTH_LONG).show();
                            }
                        }.start();
                    }
                    else if(buttonStartStop.getTag().toString().equalsIgnoreCase("stop")) {
                        mediaRecorder.stop();
                        buttonStartStop.setImageResource(R.drawable.record);
                        buttonStartStop.setTag("start");
                        buttonPlayStopLastRecordAudio.setEnabled(true);
                        buttonStartStop.setEnabled(true);
                        buttonReset.setEnabled(true);
                        buttonUpload.setEnabled(true);
                        buttonPlayStopLastRecordAudio.setEnabled(true);
                        onStopClick(view);

                        Toast.makeText(MainActivity.this, "Recording Completed",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    requestPermission();
                }

            }
        });

        buttonPlayStopLastRecordAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) throws IllegalArgumentException,
                    SecurityException, IllegalStateException {

                if(buttonPlayStopLastRecordAudio.getText().equals("PLAY")) {
                    buttonStartStop.setEnabled(false);

                    mediaPlayer = new MediaPlayer();
                    try {
                        mediaPlayer.setDataSource(AudioSavePathInDevice);
                        mediaPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mediaPlayer.start();
                    buttonPlayStopLastRecordAudio.setText("PAUSE");

                    Toast.makeText(MainActivity.this, "Recording Playing",
                            Toast.LENGTH_LONG).show();
                }
                else if(buttonPlayStopLastRecordAudio.getText().equals("PAUSE")) {
                    buttonStartStop.setEnabled(true);
                    buttonPlayStopLastRecordAudio.setText("PLAY");

                    if(mediaPlayer != null){
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        MediaRecorderReady();
                    }
                }
            }
        });



        buttonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonUpload.setEnabled(false);

                Uri audioFile = Uri.fromFile(new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/" + "AudioRecording.wav"));
                Uri sensorFile = Uri.fromFile(new File(Environment.getExternalStorageDirectory().getAbsolutePath(),
                        "RawAccelerometerData.txt"));
                StorageReference audioFileRef = mStorageRef.child("AudioRecording.wav");
                StorageReference sensorFileRef = mStorageRef.child("RawAccelerometerData.txt");

                audioFileRef.putFile(audioFile)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // Get a URL to the uploaded content
                                //Uri downloadUrl = taskSnapshot.getDownloadUrl();

                                Toast.makeText(MainActivity.this, "Audio Success!",
                                        Toast.LENGTH_LONG).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle unsuccessful uploads
                                // ...
                            }
                        });

                sensorFileRef.putFile(sensorFile)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // Get a URL to the uploaded content
                                //Uri downloadUrl = taskSnapshot.getDownloadUrl();

                                Toast.makeText(MainActivity.this, "Sensor Success!",
                                        Toast.LENGTH_LONG).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle unsuccessful uploads
                                // ...
                            }
                        });

                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage("+1" + phoneNumber, null, "Test SMS!", null, null);
            }
        });

    }

    public void MediaRecorderReady(){
        mediaRecorder=new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setOutputFile(AudioSavePathInDevice);
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO, SEND_SMS}, RequestPermissionCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RequestPermissionCode:
                if (grantResults.length> 0) {
                    boolean StoragePermission = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean RecordPermission = grantResults[1] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean SmsPermission = grantResults[2] ==
                            PackageManager.PERMISSION_GRANTED;

                    if (StoragePermission && RecordPermission && SmsPermission) {
                        Toast.makeText(MainActivity.this, "Permission Granted",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Permission Denied",
                                Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO);
        int result2 = ContextCompat.checkSelfPermission(getApplicationContext(),
                SEND_SMS);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED &&
                result2 == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsNewActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_voice) {

        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsNewActivity.class);
            startActivity(intent);
        } else if(id == R.id.nav_instructions) {
            Intent intent = new Intent(this, InstructionActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onStartClick(View view) {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onStopClick(View view) {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long time = currentThreadTimeMillis();
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        String string = time+","+x+","+y+","+z+"\n";
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),
                "RawAccelerometerData.txt");

        try {
            FileOutputStream outputStream = new FileOutputStream(file, true);
            outputStream.write(string.getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        medications = intent.getStringExtra(EXTRA_MESSAGE);
        phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
    }
}