package com.todesking.castleatack;

import jp.ac.washi.quinte.api.CursorAction;
import jp.ac.washi.quinte.api.GameInfo;
import jp.ac.washi.quinte.api.Player;
import jp.ac.washi.quinte.api.RotateType;
import jp.ac.washi.quinte.api.SoldierAction;

public class PlayerAdapter extends Player {
	public PlayerAdapter(PlayerAI ai) {
		this.ai = ai;
	}

	private final PlayerAI ai;
	private int turnID = -1;

	private static final ActionCommand DO_NOTHING =
		new ActionCommand(
			new CursorAction(RotateType.NONE, 0, 0),
			SoldierAction.NONE);
	private ActionCommand actionCommand = DO_NOTHING;

	private void calcNextAction(GameInfo info) {
		if (turnID == info.getTime())
			return;
		actionCommand = ai.getNextAction(info);
		if (actionCommand == null)
			actionCommand = DO_NOTHING;
	}

	@Override
	public CursorAction nextCursorAction(GameInfo info) {
		calcNextAction(info);
		return actionCommand.cursorAction;
	}

	@Override
	public SoldierAction nextSoldierAction(GameInfo info) {
		calcNextAction(info);
		return actionCommand.soldierAction;
	}
}
