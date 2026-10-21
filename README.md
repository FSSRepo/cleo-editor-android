# Cleo Editor for Android

An unofficial Sanny Builder port for Android, written in Java. It allows you to write, edit, and manage CLEO scripts directly on your Android device.

## Features

- Write and edit CLEO scripts on Android
- Syntax highlighting for CLEO opcodes
- Support for GTA: San Andreas Android CLEO scripts
- Compatible with Android versions up to Android 13

## Requirements

- Android device running Android 13 or lower
- GTA: San Andreas Android with CLEO library installed

## Dependencies

- [CodeEditor](https://github.com/ahmadaghazadeh/CodeEditor) — Lightweight code editor for Android

## Usage

1. Open the app and create or import a CLEO script.
2. Edit your script using the built-in code editor.
3. Save and export the script to your GTA:SA CLEO folder.

## Compiler and decompiler

The text syntax is the one of Sanny Builder's `SASCM.ini` (`0AB3: cleo_shared_var 200 = 0`), with a few extras:

- `00D6: if`, `00D6: if and`, `00D6: if or` count the conditions that follow (opcodes marked as conditions) and write the number for you;
  `00D6: if 5` writes the number as is.
- Arrays `$VAR(3@,25i)` / `8@(2@,10f)` (element type `i`, `f`, `s` text8 or `v` text16), text variables (`s$N`, `v$N`, `12@s`, `12@v`),
  `#MODEL` names, and `{$VAR name index}` to declare a global variable.
- Opcodes with a variable argument list (`start_new_script`, ...) end the list with a 0x00 byte; the compiler writes it and the decompiler reads it.
- Errors report the line (`Line 12: ...`); the editor jumps to it. Decompiling stops with a `// warning: ...` comment at the end instead of
  silently cutting the script.
- Scripts compiled by Sanny Builder carry a footer (`__SBFTR`) with the global variable names; the decompiler uses them
  (declared as `{$VAR ...}` when the project does not define them) and stops at the end of the code.
- "Blocked" scripts (a jump over junk bytes at the start) are decompiled from the jump target.
- For the PC target the CLEO opcodes (which `sa.dat` does not have) are added from `assets/gtasa/SASCM.CLEO.ini`.

The code is plain Java without Android classes (package `com.fastsmartsystem.cleo`), so it can be checked on a desktop JVM: a similar engine
lives in [cleo-script-java](https://github.com/FSSRepo/cleo-script-java), whose tests decompile and recompile real `.cs` files.

## Disclaimer

This is an unofficial tool and is not affiliated with Sanny Builder or Rockstar Games.

## License

This project is open source. See the repository for license details.
