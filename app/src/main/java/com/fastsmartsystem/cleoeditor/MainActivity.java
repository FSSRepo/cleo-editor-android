package com.fastsmartsystem.cleoeditor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.fastsmartsystem.cleo.IDECollector;
import com.fastsmartsystem.cleo.OpcodesLoader;
import com.fastsmartsystem.cleo.ScriptCompiler;
import com.fastsmartsystem.cleo.ScriptDecompiler;
import com.fastsmartsystem.cleoeditor.code.CodeEngine;
import com.fastsmartsystem.cleoeditor.intent.RequestOpenFile;
import com.fastsmartsystem.cleoeditor.intent.RequestSaveFile;
import com.fastsmartsystem.cleoeditor.views.CodeInspectorDialog;
import com.github.ahmadaghazadeh.editor.processor.TextNotFoundException;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    CodeEngine engine;
    IDECollector ide_collector;
    OpcodesLoader scm_loader;
    Menu menu;
    String file_name = null;
    public boolean saved_file = true;
    private DrawerLayout drawerLayout;
    ScriptCompiler compiler;
    ScriptDecompiler decompiler;
    private static final int NEW_FILE = 1;
    private static final int OPEN_FILE = 2;
    private static final int SAVE_FILE = 3;
    private static final int DECOMPILE = 4;
    private static final int COMPILE = 5;
    private int type_function = -1;
    private int save_temp_func = -1;
    private ActivityResultLauncher<Void> OpenLauncher;
    private ActivityResultLauncher<String> SaveLauncher;
    private Button btnopenFile;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        drawerLayout = findViewById(R.id.drawer);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout.addDrawerListener(
                new ActionBarDrawerToggle(
                        this,
                        drawerLayout,
                        toolbar,
                        R.string.open_drawer,
                        R.string.close_drawer));
        btnopenFile = findViewById(R.id.open_file);
        btnopenFile.setOnClickListener((v) -> {
            performFunction(OPEN_FILE);
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        NavigationView navigationViewStart = findViewById(R.id.navigation_view_start);
        initNavigationView(navigationViewStart);
        initFiles();
        initEngine();
        initIntent();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editor_menu, menu);
        this.menu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final int menuItemId = item.getItemId();
        if (menuItemId == R.id.undo) {
            try {
                engine.getEditor().undo();
            } catch (TextNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        else if (menuItemId == R.id.redo) {
            try {
                engine.getEditor().redo();
            } catch (TextNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else if(menuItemId == R.id.code_errors) {
            new CodeInspectorDialog(engine).show(getSupportFragmentManager(), "CodeInspector");
        }
        return super.onOptionsItemSelected(item);
    }

    private void initFiles() {
        App.dataDirectory = getExternalFilesDir(null).getAbsolutePath() + "/";
        // TODO: Select custom ide
        extractAssets();
        ide_collector = new IDECollector();
        ide_collector.load(App.dataDirectory + "default.ide");
        ide_collector.load(App.dataDirectory + "globals.ide");
        ide_collector.load(App.dataDirectory + "peds.ide");
        ide_collector.load(App.dataDirectory + "vehicles.ide");
        scm_loader = new OpcodesLoader(App.dataDirectory + "sa_mobile.dat");
    }


    public boolean permissionsAllowed() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100) {
            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                performFunction(type_function);
            } else {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package",getPackageName(),null);
                intent.setData(uri);
                startActivity(intent);
            }
        }
    }

    private void initEngine() {
        engine = new CodeEngine(this, findViewById(R.id.editor), ide_collector, scm_loader);
        compiler = new ScriptCompiler(scm_loader, ide_collector);
        decompiler = new ScriptDecompiler(scm_loader, ide_collector);
    }

    private void initIntent() {
        OpenLauncher = registerForActivityResult(new RequestOpenFile(), (data) -> {
            if(data != null) {
                file_name = getFileName(data);
                try{
                    InputStream is = getContentResolver().openInputStream(data);
                    if(type_function == DECOMPILE && (file_name.endsWith(".cs") || file_name.endsWith(".csa") || file_name.endsWith(".csi"))) {
                        new Thread(() -> {
                            String decompiled = decompiler.decompile(is, file_name.substring(file_name.lastIndexOf(".")));
                            new File(App.dataDirectory + file_name).delete();
                            file_name = file_name.replaceAll(".csa|.csi|.cs", ".txt");
                            engine.enqueue(() -> runOnUiThread(() -> {
                                loadTextFile(decompiled);
                                saved_file = false;
                            }));
                        }).start();
                    } else if(type_function == OPEN_FILE && file_name.endsWith(".txt")) {
                        engine.enqueue(() -> runOnUiThread(() -> {
                            loadTextFile(readFileText(is));
                            saved_file = true;
                        }));
                    } else {
                        is.close();
                        Toast.makeText(getApplicationContext(), type_function == DECOMPILE ? getString(R.string.no_cleo_file) : getString(R.string.no_txt_file), Toast.LENGTH_SHORT).show();
                    }
                }catch (Exception e) {}
            }
        });
        SaveLauncher = registerForActivityResult(new RequestSaveFile(), (data) -> {
            if(data != null) {
                try {
                    OutputStream out = getContentResolver().openOutputStream(data);
                    if(type_function == COMPILE) {
                       new Thread(() -> {
                           String result = compiler.compile(engine.getEditor().getText(), out);
                           runOnUiThread(() -> showDialogResult(result));
                       }).start();
                    } else if(type_function == SAVE_FILE) {
                        saved_file = true;
                        saveFileText(out);
                        if(save_temp_func != -1) {
                            performFunction(save_temp_func);
                            save_temp_func = -1;
                        }
                    }
                } catch (Exception e) {
                }
            }
        });
    }

    @SuppressLint("NonConstantResourceId")
    private void initNavigationView(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    switch (menuItem.getItemId()) {
                        case R.id.new_file:
                            performFunction(NEW_FILE);
                            break;
                        case R.id.open:
                            performFunction(OPEN_FILE);
                            break;
                        case R.id.save:
                            performFunction(SAVE_FILE);
                            break;
                        case R.id.search:
                            Intent intent = new Intent(MainActivity.this, SearchOpcodeActivity.class);
                            startActivity(intent);
                            break;
                        case R.id.compile:
                            performFunction(COMPILE);
                            break;
                        case R.id.decompile:
                            performFunction(DECOMPILE);
                            break;
                    }
                    drawerLayout.closeDrawer(navigationView);
                    return true;
                });
    }

    private void performFunction(int function) {
        this.type_function = function;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(permissionsAllowed()) {
                if(file_name == null && (function == COMPILE || function == SAVE_FILE)) {
                    Toast.makeText(getApplicationContext(), getString(R.string.open_a_file), Toast.LENGTH_SHORT).show();
                    return;
                }
                if(file_name != null && !saved_file && (function == NEW_FILE || function == DECOMPILE || function == OPEN_FILE)) {
                    showSaveConfirmationDialog(function);
                    return;
                }
                if(function == OPEN_FILE || function == DECOMPILE) {
                    OpenLauncher.launch(null);
                } else if(function == SAVE_FILE) {
                    SaveLauncher.launch(file_name);
                } else if(function == COMPILE) {
                    if(engine.format != null) {
                        SaveLauncher.launch(file_name.replace(".txt", engine.format));
                    } else {
                        Toast.makeText(this, getString(R.string.no_cleo_format), Toast.LENGTH_SHORT).show();
                    }
                } else if(function == NEW_FILE) {
                    showNewFileDialog();
                }
            } else {
                requestPermissions(new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE },100);
            }
        }
    }

    private void loadTextFile(String text) {
        engine.getEditor().setText(text, 1);
        menu.findItem(R.id.undo).setVisible(true);
        menu.findItem(R.id.redo).setVisible(true);
        menu.findItem(R.id.code_errors).setVisible(true);
        engine.getEditor().setVisibility(View.VISIBLE);
        btnopenFile.setVisibility(View.GONE);
        engine.start();
        toolbar.setTitle("Editor - " + file_name);
    }

    public void updateMenuIcon() {
        if(menu != null) {
            if(engine.hasErrors()) {
                menu.findItem(R.id.code_errors).setIcon(R.drawable.ic_error);
            } else {
                menu.findItem(R.id.code_errors).setIcon(R.drawable.ic_code);
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public String readFileText(InputStream is) {
        try {
            byte[] data = new byte[is.available()];
            is.read(data);
            is.close();
            is = null;
            return new String(data);
        } catch (Exception e) {}
        return null;
    }

    public void saveFileText(OutputStream out) {
        try{
            out.write(engine.getEditor().getText().getBytes());
            out.close();
            saved_file = true;
        } catch(Exception e) {
        }
    }

    private void showSaveConfirmationDialog(int current_func) {
        save_temp_func = current_func;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.save_changes));
        builder.setMessage(getString(R.string.save_changes_content));
        builder.setPositiveButton(getString(R.string.save), (dialog, which) -> {
            performFunction(SAVE_FILE);
            dialog.dismiss();
        });
        builder.setNegativeButton(getString(R.string.no_save), (dialog, which) -> {
            saved_file = true;
            performFunction(current_func);
            saved_file = false;
        });
        builder.setNeutralButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showNewFileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.create_new_file));
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_file, null);
        final EditText editTextFileName = dialogView.findViewById(R.id.editTextFileName);
        builder.setView(dialogView);
        builder.setPositiveButton(getString(R.string.create), (dialog, which) -> {
            file_name = editTextFileName.getText().toString() + ".txt";
            loadTextFile("");
            saved_file = false;
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) ->  dialog.dismiss());
        AlertDialog alertDialog = builder.create();
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    private void showDialogResult(String result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(compiler.hasError() ? "Error" : getString(R.string.result));
        builder.setMessage(result);
        builder.setPositiveButton(getString(R.string.accept), (dialog, which) -> {
            if(compiler.hasError()) {
                try {
                    engine.getEditor().gotoLine(compiler.line_idx);
                } catch (TextNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
            dialog.dismiss();
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void extractAssets() {
        try{
            String[] files = getAssets().list("gtasa");
            for(String file : files) {
                if(!new File(App.dataDirectory + file).exists()) {
                    InputStream is = getAssets().open("gtasa/" + file);
                    FileOutputStream os = new FileOutputStream(App.dataDirectory + file);
                    byte[] data = new byte[is.available()];
                    is.read(data);
                    os.write(data);
                    is.close();
                    os.close();
                    data = null;
                }
            }
        } catch(Exception ignored) {
        }
    }
}