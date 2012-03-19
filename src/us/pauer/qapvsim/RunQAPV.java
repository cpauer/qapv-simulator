package us.pauer.qapvsim;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JFrame;



public class RunQAPV {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		QAPVSim qa = new QAPVSim(getDefaultRequesterSetup());

	}
	private static String[] getDefaultRequesterSetup() {
		String[] args = new String[7];
		args[0] = "Provider";
		args[1] = "QCR";
		args[2] = "192.168.0.4";
		args[3] = "40404";
		args[4] = "QCP";
		args[5] = "192.168.0.4";
		args[6] = "40405";
		return args;
	}

}
