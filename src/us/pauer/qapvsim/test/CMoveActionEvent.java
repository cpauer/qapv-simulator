package us.pauer.qapvsim.test;

import java.awt.event.ActionEvent;

public class CMoveActionEvent extends ActionEvent {


	private static final long serialVersionUID = 1L;
	private Object keyObject;

	public CMoveActionEvent(Object performingObject, Object focusObject, int id) {
		super(performingObject, id, null);
		keyObject = focusObject;
	}

}
