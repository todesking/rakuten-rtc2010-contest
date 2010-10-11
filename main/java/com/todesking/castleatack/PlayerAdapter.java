package com.todesking.castleatack;

import java.io.PrintStream;

import jp.ac.washi.quinte.api.CursorAction;
import jp.ac.washi.quinte.api.GameInfo;
import jp.ac.washi.quinte.api.Player;
import jp.ac.washi.quinte.api.Point;
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
		Util.log("tick").println(
			"================== " + info.getTime() + " ====================");
		actionCommand = ai.getNextAction(info);
		logAction(actionCommand);
		if (actionCommand == null)
			actionCommand = DO_NOTHING;
		validate(actionCommand, info);
	}

	private void logAction(ActionCommand ac) {
		final PrintStream log = Util.log("action");
		log.println("cursor: " + Util.inspect(ac.cursorAction));
		log.println("soldier: " + ac.soldierAction);
	}

	private void validate(ActionCommand actionCommand, GameInfo info) {
		final CursorAction ca = actionCommand.cursorAction;
		check(info.getMap().canRotate(ca.getLocation()), "could not rotate");

		final SoldierAction sa = actionCommand.soldierAction;
		final Point currentSoldierPosition = info.getMySoldier().getLocation();
		final Point nextSoldierPosition =
			sa == SoldierAction.NONE ? currentSoldierPosition : sa
				.toDirection()
				.moveFrom(currentSoldierPosition);
		check(info.getMap().getTile(nextSoldierPosition).getOwner() == info
			.getMyCountry(), "could not move");
	}

	private static void check(boolean condition, String msg) {
		if (!condition)
			throw new AssertionError(msg);
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
