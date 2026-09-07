package awis.obd.config

import awis.obd.command.AirIntakeTempObdCommand
import awis.obd.command.CommandEquivRatioObdCommand
import awis.obd.command.DtcNumberObdCommand
import awis.obd.command.EngineRPMObdCommand
import awis.obd.command.EngineRunTimeObdCommand
import awis.obd.command.FuelEconomyCommandedMAPObdCommand
import awis.obd.command.FuelEconomyMAPObdCommand
import awis.obd.command.FuelEconomyObdCommand
import awis.obd.command.FuelPressureObdCommand
import awis.obd.command.FuelTrimObdCommand
import awis.obd.command.IntakeManifoldPressureObdCommand
import awis.obd.command.MassAirFlowObdCommand
import awis.obd.command.ObdCommand
import awis.obd.command.PressureObdCommand
import awis.obd.command.SpeedObdCommand
import awis.obd.command.TempObdCommand
import awis.obd.command.ThrottleObdCommand
import awis.obd.command.TimingAdvanceObdCommand
import awis.obd.command.TroubleCodesObdCommand

object ObdConfig {
    const val COOLANT_TEMP = "Coolant Temp"
    const val FUEL_ECON = "Fuel Economy"
    const val FUEL_ECON_MAP = "Fuel Economy MAP"
    const val RPM = "Engine RPM"
    const val RUN_TIME = "Engine Runtime"
    const val SPEED = "Vehicle Speed"
    const val AIR_TEMP = "Ambient Air Temp"
    const val INTAKE_TEMP = "Air Intake Temp"

    fun getCommands(): List<ObdCommand> {
        return listOf(
            AirIntakeTempObdCommand(),
            IntakeManifoldPressureObdCommand(),
            PressureObdCommand("0133", "Barometric Press", "kPa", "atm"),
            TempObdCommand("0146", AIR_TEMP, "C", "F"),
            SpeedObdCommand(),
            ThrottleObdCommand(),
            EngineRPMObdCommand(),
            FuelPressureObdCommand(),
            TempObdCommand("0105", COOLANT_TEMP, "C", "F"),
            ThrottleObdCommand("0104", "Engine Load", "%"),
            MassAirFlowObdCommand(),
            FuelEconomyObdCommand(),
            FuelEconomyMAPObdCommand(),
            FuelEconomyCommandedMAPObdCommand(),
            FuelTrimObdCommand(),
            FuelTrimObdCommand("0106", "Short Term Fuel Trim", "%"),
            EngineRunTimeObdCommand(),
            CommandEquivRatioObdCommand(),
            TimingAdvanceObdCommand(),
            TroubleCodesObdCommand("03", "Trouble Codes", "", "")
        )
    }

    fun getStaticCommands(): List<ObdCommand> {
        return listOf(
            DtcNumberObdCommand(),
            TroubleCodesObdCommand("03", "Trouble Codes", "", ""),
            ObdCommand("04", "Reset Codes", "", ""),
            ObdCommand("atz\ratz\ratz\r", "Serial Reset atz", "", ""),
            ObdCommand("atz\ratz\ratz\rate0", "Serial Echo Off ate0", "", ""),
            ObdCommand("ate1", "Serial Echo On ate1", "", ""),
            ObdCommand("atsp0", "Reset Protocol atsp0", "", ""),
            ObdCommand("atspa2", "Reset Protocol atspa2", "", "")
        )
    }

    fun getAllCommands(): List<ObdCommand> {
        return getStaticCommands() + getCommands()
    }
}
