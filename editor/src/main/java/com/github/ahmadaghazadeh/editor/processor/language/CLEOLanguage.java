/*
 * Copyright (C) 2018 Light Team Software
 *
 * This file is part of ModPE IDE.
 *
 * ModPE IDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ModPE IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.ahmadaghazadeh.editor.processor.language;

import com.github.ahmadaghazadeh.editor.processor.utils.text.ArrayUtils;

import java.util.regex.Pattern;

public class CLEOLanguage {

    private static final Pattern SYNTAX_NUMBERS = Pattern.compile("\\b(\\d+(?:\\.\\d+)?)(?!\\S*?:)|\\#\\w+");

    public final Pattern getSyntaxNumbers() {
        return SYNTAX_NUMBERS;
    }

    private static final Pattern SYNTAX_SYMBOLS = Pattern.compile(
            "(!|\\+|-|\\*|<|>|=|\\?|\\||:|%|&)");

    public final Pattern getSyntaxSymbols() {
        return SYNTAX_SYMBOLS;
    }

    private static final Pattern SYNTAX_BRACKETS = Pattern.compile("\\{(.*?)\\}");

    public final Pattern getSyntaxBrackets() {
        return SYNTAX_BRACKETS;
    }

    private static final Pattern SYNTAX_KEYWORDS = Pattern.compile(
            "\\d+\\@|[sv][$@]\\d+|\\$\\w+|(?<=\\b)((jump)|(jump_if_false)|(if)|(not))(?=\\b)"); // CASE_INSENSITIVE

    public final Pattern getSyntaxKeywords() {
        return SYNTAX_KEYWORDS;
    }

    // Label Paint
    private static final Pattern SYNTAX_METHODS = Pattern.compile(
            "\\s\\@\\w+|\\:\\w+", Pattern.CASE_INSENSITIVE);

    public final Pattern getSyntaxMethods() {
        return SYNTAX_METHODS;
    }

    private static final Pattern SYNTAX_STRINGS = Pattern.compile("\"(.*?)\"|'(.*?)'");

    public final Pattern getSyntaxStrings() {
        return SYNTAX_STRINGS;
    }

    private static final Pattern SYNTAX_COMMENTS = Pattern.compile("/\\*(?:.|[\\n\\r])*?\\*/|//.*");

    public final Pattern getSyntaxComments() {
        return SYNTAX_COMMENTS;
    }

    private static final char[] LANGUAGE_BRACKETS = new char[]{'{', '[', '(', '}', ']', ')'}; //do not change

    public final char[] getLanguageBrackets() {
        return LANGUAGE_BRACKETS;
    }

    public static final String[] types = {
            "int",
            "label",
            "float",
            "arguments",
            "Player",
            "model_char",
            "Char",
            "PedType",
            "bool",
            "Car",
            "model_vehicle",
            "CarDrivingStyle",
            "CarMission",
            "gxt_key",
            "TextStyle",
            "PadId",
            "Button",
            "Sphere",
            "Object",
            "model_object",
            "WeaponType",
            "any",
            "CarGenerator",
            "TimerDirection",
            "zone_key",
            "CameraMode",
            "SwitchType",
            "Blip",
            "BlipColor",
            "BlipDisplay",
            "Fade",
            "Town",
            "ShadowTextureType",
            "ScriptSound",
            "Sound",
            "WeatherType",
            "model_any",
            "CarLock",
            "ExplosionType",
            "Pickup",
            "PickupType",
            "GarageName",
            "GangType",
            "string",
            "BombType",
            "AnimGroup",
            "CoronaType",
            "FlareType",
            "ControllerMode",
            "RadarSprite",
            "ScriptFire",
            "Boat",
            "GarageType",
            "Font",
            "EntityStatus",
            "CounterDisplay",
            "MissionAudioSlot",
            "HudObject",
            "RadioChannel",
            "TempAction",
            "Heli",
            "WeaponSlot",
            "Interior",
            "Plane",
            "PlayerMood",
            "SetPieceType",
            "BodyPart",
            "string128",
            "MoveState",
            "DecisionMakerChar",
            "Sequence",
            "Attractor",
            "Group",
            "DefaultTaskAllocator",
            "Particle",
            "CarDoor",
            "DecisionMaker",
            "DecisionMakerGroup",
            "Searchlight",
            "Checkpoint",
            "Train",
            "ModSlot",
            "RelationshipType",
            "Trailer",
            "script_id",
            "FightStyle",
            "Menu",
            "MenuGrid",
            "EntryexitsFlag",
            "User3DMarker",
            "ScriptBrainAttachType",
            "WidgetId",
            "WidgetFlag",
            "TouchPoints",
            "GameVerInternal",
    };
}
