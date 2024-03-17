package com.fastsmartsystem.cleoeditor.intent;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class RequestSaveFile extends ActivityResultContract<String, Uri> {

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, String file_name) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, file_name);
        return Intent.createChooser(intent, "Guardar archivo");
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
