package com.fastsmartsystem.cleoeditor.intent;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fastsmartsystem.cleoeditor.MainActivity;

public class RequestOpenFile extends ActivityResultContract<Void, Uri> {
    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, Void input) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        return Intent.createChooser(intent, "Abrir archivo");
    }

    @Override
    public Uri parseResult(int resultCode, @Nullable Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            return intent.getData();
        } else {
            return null;
        }
    }
}