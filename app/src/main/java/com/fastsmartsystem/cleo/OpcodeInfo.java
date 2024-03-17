/*
 * CLEO SCript Java
 * FSSRepo 2023
 */

package com.fastsmartsystem.cleo;
import java.util.*;

public  class OpcodeInfo {
	public int param_count;
	public String template;
	public String description;
	public String example;
	public int attributes;
	public ArrayList<OpcodeArgument> params = new ArrayList<>();

	public static final int is_keyword = 0;
	public static final int is_branch = 2;
	public static final int is_segment = 4;
	public static final int is_static = 8;
	public static final int is_condition = 16;
	public static final int is_variadic = 32;
	public static final int is_nop = 64;
	public static final int is_constructor = 128;
	public static final int is_overload = 256;
	public static final int is_destructor = 512;

	public static final int int_type_param = 0;
	public static final int label_type_param = 1;
	public static final int float_type_param = 2;
	public static final int arguments_type_param = 3;
	public static final int Player_type_param = 4;
	public static final int model_char_type_param = 5;
	public static final int Char_type_param = 6;
	public static final int PedType_type_param = 7;
	public static final int bool_type_param = 8;
	public static final int Car_type_param = 9;
	public static final int model_vehicle_type_param = 10;
	public static final int CarDrivingStyle_type_param = 11;
	public static final int CarMission_type_param = 12;
	public static final int gxt_key_type_param = 13;
	public static final int TextStyle_type_param = 14;
	public static final int PadId_type_param = 15;
	public static final int Button_type_param = 16;
	public static final int Sphere_type_param = 17;
	public static final int Object_type_param = 18;
	public static final int model_object_type_param = 19;
	public static final int WeaponType_type_param = 20;
	public static final int any_type_param = 21;
	public static final int CarGenerator_type_param = 22;
	public static final int TimerDirection_type_param = 23;
	public static final int zone_key_type_param = 24;
	public static final int CameraMode_type_param = 25;
	public static final int SwitchType_type_param = 26;
	public static final int Blip_type_param = 27;
	public static final int BlipColor_type_param = 28;
	public static final int BlipDisplay_type_param = 29;
	public static final int Fade_type_param = 30;
	public static final int Town_type_param = 31;
	public static final int ShadowTextureType_type_param = 32;
	public static final int ScriptSound_type_param = 33;
	public static final int Sound_type_param = 34;
	public static final int WeatherType_type_param = 35;
	public static final int model_any_type_param = 36;
	public static final int CarLock_type_param = 37;
	public static final int ExplosionType_type_param = 38;
	public static final int Pickup_type_param = 39;
	public static final int PickupType_type_param = 40;
	public static final int GarageName_type_param = 41;
	public static final int GangType_type_param = 42;
	public static final int string_type_param = 43;
	public static final int BombType_type_param = 44;
	public static final int AnimGroup_type_param = 45;
	public static final int CoronaType_type_param = 46;
	public static final int FlareType_type_param = 47;
	public static final int ControllerMode_type_param = 48;
	public static final int RadarSprite_type_param = 49;
	public static final int ScriptFire_type_param = 50;
	public static final int Boat_type_param = 51;
	public static final int GarageType_type_param = 52;
	public static final int Font_type_param = 53;
	public static final int EntityStatus_type_param = 54;
	public static final int CounterDisplay_type_param = 55;
	public static final int MissionAudioSlot_type_param = 56;
	public static final int HudObject_type_param = 57;
	public static final int RadioChannel_type_param = 58;
	public static final int TempAction_type_param = 59;
	public static final int Heli_type_param = 60;
	public static final int WeaponSlot_type_param = 61;
	public static final int Interior_type_param = 62;
	public static final int Plane_type_param = 63;
	public static final int PlayerMood_type_param = 64;
	public static final int SetPieceType_type_param = 65;
	public static final int BodyPart_type_param = 66;
	public static final int string128_type_param = 67;
	public static final int MoveState_type_param = 68;
	public static final int DecisionMakerChar_type_param = 69;
	public static final int Sequence_type_param = 70;
	public static final int Attractor_type_param = 71;
	public static final int Group_type_param = 72;
	public static final int DefaultTaskAllocator_type_param = 73;
	public static final int Particle_type_param = 74;
	public static final int CarDoor_type_param = 75;
	public static final int DecisionMaker_type_param = 76;
	public static final int DecisionMakerGroup_type_param = 77;
	public static final int Searchlight_type_param = 78;
	public static final int Checkpoint_type_param = 79;
	public static final int Train_type_param = 80;
	public static final int ModSlot_type_param = 81;
	public static final int RelationshipType_type_param = 82;
	public static final int Trailer_type_param = 83;
	public static final int script_id_type_param = 84;
	public static final int FightStyle_type_param = 85;
	public static final int Menu_type_param = 86;
	public static final int MenuGrid_type_param = 87;
	public static final int EntryexitsFlag_type_param = 88;
	public static final int User3DMarker_type_param = 89;
	public static final int ScriptBrainAttachType_type_param = 90;
	public static final int WidgetId_type_param = 91;
	public static final int WidgetFlag_type_param = 92;
	public static final int TouchPoints_type_param = 93;
	public static final int GameVerInternal_type_param = 94;


}
