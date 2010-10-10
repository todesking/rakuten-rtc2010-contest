package com.todesking.castleatack;

import java.io.PrintStream;

import jp.ac.washi.quinte.api.GameInfo;
import jp.ac.washi.quinte.api.Point;
import jp.ac.washi.quinte.api.TileInfo;

public class Util {

	public static void printMapInfo(GameInfo info, final PrintStream out)
			throws AssertionError {
		for (int y = 0; y < info.getMap().getSize(); y++) {
			for (int x = 0; x < info.getMap().getSize(); x++) {
				final TileInfo tile = info.getMap().getTile(Point.create(x, y));
				final char c;
				switch (tile.getType()) {
				case BIG_GATE:
					c = 'G';
					break;
				case SMALL_GATE:
					c = 'g';
					break;
				case ROAD:
					if (tile.getOwner() == info.getLeftCountry())
						c = '1';
					else if (tile.getOwner() == info.getOppositeCountry())
						c = '2';
					else if (tile.getOwner() == info.getRightCountry())
						c = '3';
					else if (tile.getOwner() == info.getMyCountry())
						c = '0';
					else if (tile.getOwner() == null)
						c = '?';
					else
						throw new AssertionError();
					break;
				case WALL:
					c = '#';
					break;
				default:
					throw new AssertionError();
				}
				out.print(c);
			}
			out.println();
		}
	}

}
