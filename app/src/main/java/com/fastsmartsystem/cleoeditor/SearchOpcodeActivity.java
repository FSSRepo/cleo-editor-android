package com.fastsmartsystem.cleoeditor;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.fastsmartsystem.cleo.OpcodeInfo;
import com.fastsmartsystem.cleo.OpcodesLoader;
import com.fastsmartsystem.cleoeditor.views.OnOpcodeItemListener;
import com.fastsmartsystem.cleoeditor.views.OpcodeAdapter;
import com.fastsmartsystem.cleoeditor.views.OpcodeItem;

import java.util.ArrayList;
import java.util.List;

public class SearchOpcodeActivity extends AppCompatActivity implements OnOpcodeItemListener {

    private OpcodeAdapter adapter;
    OpcodesLoader op_loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_opcodes);

        // Configurar el RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        op_loader = new OpcodesLoader(App.dataDirectory + "sa_mobile.dat");
        ArrayList<OpcodeItem> opcodes = new ArrayList<>();
        for(Integer id : op_loader.ops.keySet()) {
            String temp = op_loader.ops.get(id).template;
            if(id == 0 || id > 0 && !temp.startsWith("NOP")) {
                opcodes.add(new OpcodeItem(String.format("%04X: %s", id, temp), id));
            }
        }
        adapter = new OpcodeAdapter(opcodes, this);
        recyclerView.setAdapter(adapter);
        // Configurar el Toolbar como la barra de acción
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.opcode_menu_search, menu);
        MenuItem searchItem = menu.findItem(R.id.actionTenantSearch);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                adapter.filter(s); // Llamar al método de filtrado con el texto de búsqueda
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void OnClick(OpcodeItem item) {
        showOpcodeInfoDialog(op_loader.ops.get(item.id) , item.id);
    }

    private void showOpcodeInfoDialog(OpcodeInfo info, int id) {
        String format_id = String.format("%04X", id);
        View view = LayoutInflater.from(this).inflate(R.layout.opcode_info_dialog, null);
        TextView tvOpcodeTitle = view.findViewById(R.id.opcode_id_dialog);
        TextView tvOpcodeInfo = view.findViewById(R.id.tvCodeInfoDiag);
        TextView tvOpcodeExample = view.findViewById(R.id.tvCodeExample);
        TextView tvOpcodeLink = view.findViewById(R.id.tvGTALink);
        tvOpcodeLink.setPaintFlags(tvOpcodeLink.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        tvOpcodeTitle.setText("Opcode ID: "+format_id);
        tvOpcodeInfo.setText(info.description);
        tvOpcodeExample.setText(info.example.length() > 0 ? info.example : getString(R.string.no_example_available));
        ImageView ivClose = view.findViewById(R.id.ivCloseDiag);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(view);
        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.setPositiveButton(getString(R.string.copy_template), (dialog, which) -> {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("Opcode Copied",format_id +": " + info.template);
            clipboardManager.setPrimaryClip(clipData);
            Toast.makeText(this, getString(R.string.copied_correctly), Toast.LENGTH_SHORT).show();
        });
        tvOpcodeLink.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://gtamods.com/wiki/" + format_id));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        });
        AlertDialog dialog = alertDialogBuilder.create();
        dialog.show();
        ivClose.setOnClickListener((v) -> dialog.dismiss());
    }
}
