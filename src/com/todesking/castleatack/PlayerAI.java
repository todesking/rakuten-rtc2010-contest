package com.todesking.castleatack;

import jp.ac.washi.quinte.api.GameInfo;

public interface PlayerAI {
	public abstract ActionCommand getNextAction(GameInfo info);
}