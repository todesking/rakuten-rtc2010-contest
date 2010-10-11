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

	private void logAction(CursorAction ca) {
		final PrintStream log = Util.log("action");
		log.println("cursor: " + Util.inspect(ca));
	}

	private void logAction(SoldierAction sa) {
		final PrintStream log = Util.log("action");
		log.println("soldier: " + sa);
	}

	private static void validate(CursorAction ca, GameInfo info) {
		if (ca.getType() != RotateType.NONE) {
			check(info.getMap().canRotate(ca.getLocation()), "could not rotate");
			check(!Util.isRoadAllOwnedInCursor(
				info.getMap(),
				ca.getLocation(),
				info.getMyCountry()), "meaningless rotate");
		}
	}

	private static void validate(SoldierAction sa, GameInfo info) {
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
		final CursorAction action;
		try {
			action = ai.nextCursorAction(info);
		} catch (RuntimeException e) {
			Util.printMapInfo(info, System.err);
			e.printStackTrace(System.err);
			return null;
		}
		logAction(action);
		try {
			validate(action, info);
		} catch (AssertionError e) {
			Util.printMapInfo(info, System.err);
			e.printStackTrace(System.err);
			return null;
		}
		return action;
	}

	@Override
	public SoldierAction nextSoldierAction(GameInfo info) {
		final SoldierAction action;
		try {
			action = ai.nextSoldierAction(info);
		} catch (RuntimeException e) {
			Util.printMapInfo(info, System.err);
			e.printStackTrace(System.err);
			return null;
		}
		logAction(action);
		try {
			validate(action, info);
		} catch (AssertionError e) {
			Util.printMapInfo(info, System.err);
			e.printStackTrace(System.err);
			return null;
		}
		return action;
	}
}
