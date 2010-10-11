package com.todesking.castleatack;

import jp.ac.washi.quinte.api.CursorAction;
import jp.ac.washi.quinte.api.GameInfo;
import jp.ac.washi.quinte.api.SoldierAction;

public interface PlayerAI {

	public SoldierAction nextSoldierAction(GameInfo info);

	public CursorAction nextCursorAction(GameInfo info);

}