package awis.obd.config;

import java.util.ArrayList;

import awis.obd.command.AirIntakeTempObdCommand;
import awis.obd.command.CommandEquivRatioObdCommand;
import awis.obd.command.DtcNumberObdCommand;
import awis.obd.command.EngineRPMObdCommand;
import awis.obd.command.EngineRunTimeObdCommand;
import awis.obd.command.FuelEconomyCommandedMAPObdCommand;
import awis.obd.command.FuelEconomyMAPObdCommand;
import awis.obd.command.FuelEconomyObdCommand;
import awis.obd.command.FuelPressureObdCommand;
import awis.obd.command.FuelTrimObdCommand;
import awis.obd.command.IntakeManifoldPressureObdCommand;
import awis.obd.command.MassAirFlowObdCommand;
import awis.obd.command.ObdCommand;
import awis.obd.command.PressureObdCommand;
import awis.obd.command.SpeedObdCommand;
import awis.obd.command.TempObdCommand;
import awis.obd.command.ThrottleObdCommand;
import awis.obd.command.TimingAdvanceObdCommand;
import awis.obd.command.TroubleCodesObdCommand;

public class ObdConfig {

	public final static String COOLANT_TEMP = "Coolant Temp";
	public final static String FUEL_ECON = "Fuel Economy";
	public final static String FUEL_ECON_MAP = "Fuel Economy MAP";
	public final static String RPM = "Engine RPM";
	public final static String RUN_TIME = "Engine Runtime";
	public final static String SPEED = "Vehicle Speed";
	public final static String AIR_TEMP = "Ambient Air Temp";
	public final static String INTAKE_TEMP = "Air Intake Temp";

	public static ArrayList<ObdCommand> getCommands() {
		ArrayList<ObdCommand> cmds = new ArrayList<ObdCommand>();
		cmds.add(new AirIntakeTempObdCommand());
		cmds.add(new IntakeManifoldPressureObdCommand());
		cmds.add(new PressureObdCommand("0133","Barometric Press","kPa","atm"));
		cmds.add(new TempObdCommand("0146",AIR_TEMP,"C","F"));
		cmds.add(new SpeedObdCommand());
		cmds.add(new ThrottleObdCommand());
		cmds.add(new EngineRPMObdCommand());
		cmds.add(new FuelPressureObdCommand());
		cmds.add(new TempObdCommand("0105",COOLANT_TEMP,"C","F"));
		cmds.add(new ThrottleObdCommand("0104","Engine Load","%"));
		cmds.add(new MassAirFlowObdCommand());
		cmds.add(new FuelEconomyObdCommand());
		cmds.add(new FuelEconomyMAPObdCommand());
		cmds.add(new FuelEconomyCommandedMAPObdCommand());
		cmds.add(new FuelTrimObdCommand());
		cmds.add(new FuelTrimObdCommand("0106","Short Term Fuel Trim","%"));
		cmds.add(new EngineRunTimeObdCommand());
		cmds.add(new CommandEquivRatioObdCommand());
		cmds.add(new TimingAdvanceObdCommand());
		cmds.add(new ObdCommand("03","Trouble Codes","",""));
		return cmds;
	}
	public static ArrayList<ObdCommand> getStaticCommands() {
		ArrayList<ObdCommand> cmds = new ArrayList<ObdCommand>();
		cmds.add(new DtcNumberObdCommand());
		cmds.add(new TroubleCodesObdCommand("03","Trouble Codes","",""));
		cmds.add(new ObdCommand("04","Reset Codes","",""));
		cmds.add(new ObdCommand("atz\ratz\ratz\r","Serial Reset atz","",""));
		cmds.add(new ObdCommand("atz\ratz\ratz\rate0","Serial Echo Off ate0","",""));
		cmds.add(new ObdCommand("ate1","Serial Echo On ate1","",""));
		cmds.add(new ObdCommand("atsp0","Reset Protocol astp0","",""));
		cmds.add(new ObdCommand("atspa2","Reset Protocol atspa2","",""));
		return cmds;
	}
	public static ArrayList<ObdCommand> getAllCommands() {
		ArrayList<ObdCommand> cmds = new ArrayList<ObdCommand>();
		cmds.addAll(getStaticCommands());
		cmds.addAll(getCommands());
		return cmds;
	}
}
