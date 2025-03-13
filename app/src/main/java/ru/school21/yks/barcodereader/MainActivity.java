package ru.school21.yks.barcodereader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private SurfaceView surfaceView;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private ToneGenerator toneGen1;
    private TextView barcodeText;
    private TextView nameText;
    private TextView serialText;
    private TextView usernameText;
    private TextView compText;
    private TextView glpiText;
    private TextView dateInventText;
    private String barcodeData;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch switchFlash;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}
                , PackageManager.PERMISSION_GRANTED);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        surfaceView = findViewById(R.id.surface_view);
        barcodeText = findViewById(R.id.barcode_text);
        switchFlash = findViewById(R.id.flashSwitch);
        nameText = findViewById(R.id.name_text);
        serialText = findViewById(R.id.serial_text);
        usernameText = findViewById(R.id.username_text);
        compText = findViewById(R.id.comp_text);
        glpiText = findViewById(R.id.glpi_text);
        dateInventText = findViewById(R.id.date_invent_text);
        glpiText.setMovementMethod(LinkMovementMethod.getInstance());
        Button btn_inv = findViewById(R.id.buttonInv);
        btn_inv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkInBase() && "2024-01-01".contentEquals(dateInventText.getText())) {
                    AlertDialog.Builder a_builder = new AlertDialog.Builder(MainActivity.this);
                    a_builder.setMessage("Проинвентаризировать?")
                            .setCancelable(false)
                            .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    inventWriteDate(barcodeText.getText().toString().trim());
                                }
                            })
                            .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert = a_builder.create();
                    alert.setTitle("Инвентаризация");
                    alert.show();
                }
            }
        });
    }

    private boolean checkInBase() {
        new Thread(() -> {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                String connectionString = "jdbc:mysql://10.14.50.3:3306/invent2024?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false";
                String username = "android_user";
                String password = "Zrc21Flvby";
                Connection con = DriverManager.getConnection(connectionString, username, password);
                String query = "SELECT * FROM invent WHERE Num_invent like \"" + barcodeText.getText().toString().trim() + "\"";
                PreparedStatement stmt = con.prepareStatement(query);
                ResultSet rs = stmt.executeQuery();
                boolean exists = rs.next();
                if (exists) {
                    rs.absolute(1);
                    String nameInv = rs.getString("Name");
                    if (TextUtils.isEmpty(nameInv)) {
                        nameText.setText("");
                    } else {
                        nameText.setText(nameInv);
                    }
                }
                con.close();
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }).start();
        return !(nameText.getText().equals(""));
    }

    private void inventWriteDate(String invNum) {
        new Thread(() -> {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                String connectionString = "jdbc:mysql://10.14.50.3:3306/invent2024?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false";
                String username = "android_user";
                String password = "Zrc21Flvby";
                Connection con = DriverManager.getConnection(connectionString, username, password);
                String query = "SELECT * FROM invent WHERE Num_invent like \"" + invNum + "\"";
                PreparedStatement stmt = con.prepareStatement(query);
                ResultSet rs = stmt.executeQuery();
                boolean exists = rs.next();
                if (exists) {
                    try {
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        LocalDateTime now = LocalDateTime.now();
                        String queryU = "UPDATE invent SET Date_invent = \"" + dtf.format(now) + "\", User = \"" + Settings.Global.getString(getContentResolver(), "device_name") + "\" WHERE Num_invent like \"" + invNum + "\";";
                        PreparedStatement stmtU = con.prepareStatement(queryU);
                        int rsU = stmtU.executeUpdate();
                        if (rsU > 0) {
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Проинвентаризировано", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
                con.close();
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void searchScanSaveTable(String inventNumber, String glpiU) {

        new Thread(() -> {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                String connectionString = "jdbc:mysql://10.14.50.3:3306/invent2024?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false";
                String username = "android_user";
                String password = "Zrc21Flvby";
                Connection con = DriverManager.getConnection(connectionString, username, password);
                String query = "SELECT * FROM invent WHERE Num_invent like \"" + inventNumber + "\"";
                PreparedStatement stmt = con.prepareStatement(query);
                ResultSet rs = stmt.executeQuery();
                boolean exists = rs.next();
                if (exists) {
                    try {
                        rs.absolute(1);
                        String nameInv = rs.getString("Name");
                        if (TextUtils.isEmpty(nameInv)) {
                            nameText.setText("");
                        } else {
                            nameText.setText(nameInv);
                        }
                        String dateInv = rs.getString("Date_invent");
                        if (TextUtils.isEmpty(dateInv)) {
                            dateInventText.setText("");
                        } else {
                            dateInventText.setText(dateInv);
                        }
                        String usernameT = rs.getString("User");
                        if (TextUtils.isEmpty(usernameT)) {
                            usernameText.setText("");
                        } else {
                            usernameText.setText(usernameT);
                        }
                        String serNum = rs.getString("Num_serial");
                        if (TextUtils.isEmpty(serNum)) {
                            serialText.setText("");
                        } else {
                            serialText.setText(serNum);
                        }
                        String compInv = rs.getString("Comp");
                        if (TextUtils.isEmpty(compInv)) {
                            compText.setText("");
                        } else {
                            compText.setText(compInv);
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    glpiText.setText(glpiU);
                } else {
                    nameText.setText("");
                    dateInventText.setText("");
                    serialText.setText("");
                    usernameText.setText("");
                    compText.setText("");
                    glpiText.setText("");
                }
                con.close();
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }).start();
    }
    public static Camera getCamera(CameraSource cameraSource) {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();

        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    Camera camera = (Camera) field.get(cameraSource);
                    if (camera != null) {
                        return camera;
                    }

                    return null;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                break;
            }
        }

        return null;
    }
    private void initialiseDetectorsAndSources() {
        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();
        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(1080, 1080)
                .setAutoFocusEnabled(true)
                .build();
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                cameraSource.start(surfaceView.getHolder());
                                Camera _cam = getCamera(cameraSource);
                                if (_cam != null && switchFlash.isChecked()) {
                                    Camera.Parameters _pareMeters = _cam.getParameters();
                                    _pareMeters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                                    _cam.setParameters(_pareMeters);
                                    _cam.startPreview();
                                } else if (_cam != null && !switchFlash.isChecked()) {
                                    Camera.Parameters _pareMeters = _cam.getParameters();
                                    _pareMeters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                                    _cam.setParameters(_pareMeters);
                                    _cam.startPreview();
                                }
                            } else {
                                ActivityCompat.requestPermissions(MainActivity.this, new
                                        String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, 500);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                Camera _cam = getCamera(cameraSource);
                if (_cam != null && switchFlash.isChecked()) {
                    Camera.Parameters _pareMeters = _cam.getParameters();
                    _pareMeters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    _cam.setParameters(_pareMeters);
                    _cam.startPreview();
                } else if (_cam != null && !switchFlash.isChecked()) {
                    Camera.Parameters _pareMeters = _cam.getParameters();
                    _pareMeters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    _cam.setParameters(_pareMeters);
                    _cam.startPreview();
                }
                if (barcodes.size() != 0) {
                    barcodeText.post(new Runnable() {
                        @Override
                        public void run() {
                            String glpiURL = "";
                            barcodeData = barcodes.valueAt(0).displayValue;
                            if (barcodeData.contains("00-")) {
                                if (barcodeData.contains("номер")) {
                                    glpiURL = barcodeData.substring(barcodeData.indexOf("https://"), barcodeData.length());
                                    barcodeData = barcodeData.substring(barcodeData.indexOf("00-"), barcodeData.indexOf("\n", barcodeData.indexOf("00-")));
                                } else {
                                    barcodeData = barcodeData.substring(barcodeData.indexOf("00-"), barcodeData.length());
                                }
                                searchScanSaveTable(barcodeData.trim(), glpiURL);
                            }
                            barcodeText.setText(barcodeData);
                            nameText.setText("");
                            dateInventText.setText("");
                            serialText.setText("");
                            usernameText.setText("");
                            compText.setText("");
                            glpiText.setText("");
                            toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                            long[] doubleClickPattern = {0, 75, 75, 75};
                            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            if (vibrator.hasVibrator()) {
                                vibrator.vibrate(VibrationEffect.createWaveform(doubleClickPattern, -1));
                            }

                        }
                    });
                }
            }
            @Override
            public void release() {
//                Toast.makeText(getApplicationContext(), "Сканер QR-кодов остановлен для предотвращения утечек памяти", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        getSupportActionBar().hide();
        cameraSource.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportActionBar().hide();
        initialiseDetectorsAndSources();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение получено, перезапустите камеру
                initialiseDetectorsAndSources();
            } else {
                // Разрешение не получено, покажите сообщение пользователю
                Toast.makeText(this, "Разрешение на использование камеры отклонено", Toast.LENGTH_SHORT).show();
            }
        }
    }
}