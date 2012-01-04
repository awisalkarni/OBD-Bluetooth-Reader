package awis.obd.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import awis.obd.io.ObdConnectThread;

public class ObdCommand extends Thread {

	/**
	 * @uml.property  name="in"
	 */
	protected InputStream in = null;
	/**
	 * @uml.property  name="out"
	 */
	protected OutputStream out = null;
	/**
	 * @uml.property  name="buff"
	 * @uml.associationEnd  multiplicity="(0 -1)" elementType="java.lang.Byte"
	 */
	protected ArrayList<Byte> buff = null;
	/**
	 * @uml.property  name="cmd"
	 */
	protected String cmd = null;
	/**
	 * @uml.property  name="desc"
	 */
	protected String desc = null;
	/**
	 * @uml.property  name="resType"
	 */
	protected String resType = null;
	/**
	 * @uml.property  name="error"
	 */
	protected Exception error;
	/**
	 * @uml.property  name="rawValue"
	 */
	protected Object rawValue = null;
	/**
	 * @uml.property  name="data"
	 * @uml.associationEnd  qualifier="constant:java.lang.String java.lang.Double"
	 */
	protected HashMap<String,Object> data = null;
	/**
	 * @uml.property  name="connectThread"
	 * @uml.associationEnd  inverse="cmds:awis.obd.io.ObdConnectThread"
	 */
	protected ObdConnectThread connectThread = null;
	/**
	 * @uml.property  name="impType"
	 */
	protected String impType = null;

	public ObdCommand(String cmd, String desc, String resType, String impType) {
		this.cmd = cmd;
		this.desc = desc;
		this.resType = resType;
		this.buff = new ArrayList<Byte>();
		this.impType = impType;
	}
	/**
	 * @param thread
	 * @uml.property  name="connectThread"
	 */
	public void setConnectThread(ObdConnectThread thread) {
		this.connectThread = thread;
	}
	public boolean isImperial() {
		if (connectThread != null && connectThread.getImperialUnits()) {
			return true;
		}
		return false;
	}
	public ObdCommand(ObdCommand other) {
		this(other.cmd, other.desc, other.resType, other.impType);
	}
	public void setInputStream(InputStream in) {
		this.in = in;
	}
	public void setOutputStream(OutputStream out) {
		this.out = out;
	}
	public void run() {
		sendCmd(cmd);
		readResult();
	}
	public void setDataMap(HashMap<String,Object> data) {
		this.data = data;
	}
	protected void sendCmd(String cmd) {
		try {
			cmd += "\r\n";
			out.write(cmd.getBytes());
			out.flush();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	protected void readResult() {
		byte c = 0;
		this.buff.clear();
		try {
			while ((char)(c = (byte)in.read()) != '>') {
				buff.add(c);
			}
		} catch (IOException e) {
		}
	}
	public String getResult() {
		return new String(getByteArray());
	}
	public byte[] getByteArray() {
		byte[] data = new byte[this.buff.size()];
		for (int i = 0; i < this.buff.size(); i++) {
			data[i] = this.buff.get(i);
		}
		return data;
	}
	public String formatResult() {
		String res = getResult();
		String[] ress = res.split("\r");
		res = ress[0].replace(" ","");
		return res;
	}
	/**
	 * @return
	 * @uml.property  name="in"
	 */
	public InputStream getIn() {
		return in;
	}
	/**
	 * @return
	 * @uml.property  name="out"
	 */
	public OutputStream getOut() {
		return out;
	}
	public ArrayList<Byte> getBuff() {
		return buff;
	}
	/**
	 * @return
	 * @uml.property  name="cmd"
	 */
	public String getCmd() {
		return cmd;
	}
	/**
	 * @return
	 * @uml.property  name="desc"
	 */
	public String getDesc() {
		return desc;
	}
	/**
	 * @return
	 * @uml.property  name="resType"
	 */
	public String getResType() {
		return resType;
	}
	/**
	 * @param e
	 * @uml.property  name="error"
	 */
	public void setError(Exception e) {
		error = e;
	}
	/**
	 * @return
	 * @uml.property  name="error"
	 */
	public Exception getError() {
		return error;
	}
	/**
	 * @return
	 * @uml.property  name="rawValue"
	 */
	public Object getRawValue() {
		return rawValue;
	}
}
