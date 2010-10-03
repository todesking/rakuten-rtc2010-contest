package com.todesking.castleatack;

import jp.ac.washi.quinte.api.CursorAction;
import jp.ac.washi.quinte.api.SoldierAction;

public class ActionCommand {
	public ActionCommand(CursorAction ca, SoldierAction sa) {
		this.cursorAction = ca;
		this.soldierAction = sa;
	}

	public final CursorAction cursorAction;
	public final SoldierAction soldierAction;
}
