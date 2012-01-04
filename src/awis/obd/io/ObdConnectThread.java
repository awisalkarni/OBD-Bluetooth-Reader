package awis.obd.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import awis.obd.command.ObdCommand;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class ObdConnectThread extends Thread implements LocationListener {

	/**
	 * @uml.property  name="dev"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	protected BluetoothDevice dev = null;
	/**
	 * @uml.property  name="sock"
	 * @uml.associationEnd  
	 */
	protected BluetoothSocket sock = null;
	/**
	 * @uml.property  name="stop"
	 */
	protected boolean stop = false;
	protected static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	/**
	 * @uml.property  name="in"
	 */
	protected InputStream in = null;
	/**
	 * @uml.property  name="out"
	 */
	protected OutputStream out = null;
	/**
	 * @uml.property  name="cmds"
	 * @uml.associationEnd  multiplicity="(0 -1)" inverse="connectThread:awis.obd.command.ObdCommand"
	 */
	protected ArrayList<ObdCommand> cmds = null;
	/**
	 * @uml.property  name="results"
	 * @uml.associationEnd  qualifier="constant:java.lang.String java.lang.String"
	 */
	protected HashMap<String, String> results = null;
	/**
	 * @uml.property  name="data"
	 * @uml.associationEnd  qualifier="constant:java.lang.String java.lang.Double"
	 */
	protected HashMap<String, Object> data = null;
	/**
	 * @uml.property  name="updateCycle"
	 */
	protected int updateCycle = 4000;
	/**
	 * @uml.property  name="service"
	 * @uml.associationEnd  multiplicity="(1 1)" inverse="connectThread:awis.obd.io.ObdReaderService"
	 */
	protected ObdReaderService service = null;
	/**
	 * @uml.property  name="uploadUrl"
	 */
	protected String uploadUrl = null;
	/**
	 * @uml.property  name="currentLocation"
	 * @uml.associationEnd  
	 */
	protected Location currentLocation = null;
	/**
	 * @uml.property  name="locationManager"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	protected LocationManager locationManager = null;
	/**
	 * @uml.property  name="engineDisplacement"
	 */
	protected double engineDisplacement = 1.0;
	/**
	 * @uml.property  name="volumetricEfficiency"
	 */
	protected double volumetricEfficiency = 1.0;
	/**
	 * @uml.property  name="imperialUnits"
	 */
	protected boolean imperialUnits = false;

	public ObdConnectThread(BluetoothDevice dev,
			LocationManager locationManager, final ObdReaderService service,
			String uploadUrl, int updateCycle, double engineDisplacement,
			double volumetricEfficiency, boolean imperialUnits,
			boolean enableGps, ArrayList<ObdCommand> cmds) {
		this.dev = dev;
		this.cmds = cmds;
		this.updateCycle = updateCycle;
		this.service = service;
		this.uploadUrl = uploadUrl;
		this.locationManager = locationManager;
		if (locationManager != null && enableGps) {
			this.locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 0, 0.0f, this);
		}
		results = new HashMap<String, String>();
		data = new HashMap<String, Object>();
		this.volumetricEfficiency = volumetricEfficiency;
		this.engineDisplacement = engineDisplacement;
		this.imperialUnits = imperialUnits;
	}

	protected void startDevice() throws IOException, InterruptedException {
		sock = this.dev.createRfcommSocketToServiceRecord(MY_UUID);
		sock.connect();
		in = sock.getInputStream();
		out = sock.getOutputStream();
		while (!stop) {
			ObdCommand echoOff = new ObdCommand("ate0", "echo off", "string",
					"string");
			String result = runCommand(echoOff).replace(" ", "");
			if (result != null && result.contains("OK")) {
				break;
			}
			Thread.sleep(1500);
		}
	}

	public void run() {
		try {
			startDevice();
			int cmdSize = cmds.size();
			for (int i = 0; i < cmdSize; i++) {
				String desc = cmds.get(i).getDesc();
				results.put(desc, "--");
				data.put(desc, -9999);
			}
			for (int i = 0; !stop; i = ((i + 1) % cmdSize)) {
				if (i == 0) {
					long obsTime = System.currentTimeMillis() / 1000;
					results.put("Obs Time", Long.toString(obsTime));
					data.put("Obs Time", obsTime);
					
					Thread.sleep(updateCycle);
				}
				ObdCommand cmd = cmds.get(i);
				try {
					cmd = getCopy(cmd); // make a copy because thread can only
										// run once
					String desc = cmd.getDesc();
					String result = runCommand(cmd);
					results.put(desc, result);
					data.put(desc, cmd.getRawValue());
				} catch (Exception e) {
					results.put(cmd.getDesc(), "--");
					if (!stop) {
						service.notifyMessage("Error running " + cmd.getDesc(),
								e.getMessage(),
								ObdReaderService.COMMAND_ERROR_NOTIFY);
					}
				}
			}
		} catch (IOException e) {
			service.notifyMessage("Bluetooth Connection Error", e.getMessage(),
					ObdReaderService.CONNECT_ERROR_NOTIFY);
		} catch (Exception e) {
			service.notifyMessage(e.getMessage(), e.toString(),
					ObdReaderService.OBD_SERVICE_ERROR_NOTIFY);
		} finally {
			close();
		}
	}

	public static ObdCommand getCopy(ObdCommand cmd)
			throws IllegalArgumentException, SecurityException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		return cmd.getClass().getConstructor(cmd.getClass()).newInstance(cmd);
	}

	public String runCommand(ObdCommand cmd) throws InterruptedException {
		cmd.setInputStream(in);
		cmd.setOutputStream(out);
		cmd.setDataMap(data);
		cmd.setConnectThread(this);
		cmd.start();
		while (!stop) {
			cmd.join(300);
			if (!cmd.isAlive()) {
				break;
			}
		}
		return cmd.formatResult();
	}

	public ArrayList<ObdCommand> getCmds() {
		return cmds;
	}

	public synchronized Map<String, String> getResults() {
		if (currentLocation != null) {
			double lat = currentLocation.getLatitude();
			double lon = currentLocation.getLongitude();
			int speed = (int) currentLocation.getSpeed();
			long gtime = currentLocation.getTime() / 1000;
			results.put("Latitude", String.format("%.5f", lat));
			results.put("Longitude", String.format("%.5f", lon));
			results.put("GPS Speed", String.format("%d m/s", speed));
			results.put("GPS Time", Long.toString(gtime));
			data.put("Latitude", lat);
			data.put("Longitude", lon);
			data.put("GPS Speed", speed);
			data.put("GPS Time", gtime);
		}
		return results;
	}

	public void cancel() {
		stop = true;
	}

	public void close() {
		try {
			locationManager.removeUpdates(this);
		} catch (Exception e) {
		}
		try {
			stop = true;
			sock.close();
		} catch (Exception e) {
		}
	}

	public String getStackTrace(Exception e) {
		StringWriter strw = new StringWriter();
		PrintWriter ptrw = new PrintWriter(strw);
		e.printStackTrace(ptrw);
		return strw.toString();
	}

	public void onLocationChanged(Location location) {
		currentLocation = location;
	}

	public void onProviderDisabled(String provider) {
		service.notifyMessage("GPS Unavailable",
				"GPS_PROVIDER disabled, please enable gps in your settings.",
				ObdReaderService.OBD_SERVICE_ERROR_NOTIFY);
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	/**
	 * @param ve
	 * @uml.property  name="volumetricEfficiency"
	 */
	public void setVolumetricEfficiency(double ve) {
		this.volumetricEfficiency = ve;
	}

	/**
	 * @param ed
	 * @uml.property  name="engineDisplacement"
	 */
	public void setEngineDisplacement(double ed) {
		this.engineDisplacement = ed;
	}

	/**
	 * @return
	 * @uml.property  name="volumetricEfficiency"
	 */
	public double getVolumetricEfficiency() {
		return volumetricEfficiency;
	}

	/**
	 * @return
	 * @uml.property  name="engineDisplacement"
	 */
	public double getEngineDisplacement() {
		return engineDisplacement;
	}

	public boolean getImperialUnits() {
		return imperialUnits;
	}
}