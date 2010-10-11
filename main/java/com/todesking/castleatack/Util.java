package com.todesking.castleatack;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import jp.ac.washi.quinte.api.CountryInfo;
import jp.ac.washi.quinte.api.CursorAction;
import jp.ac.washi.quinte.api.GameInfo;
import jp.ac.washi.quinte.api.MapInfo;
import jp.ac.washi.quinte.api.Point;
import jp.ac.washi.quinte.api.RotateType;
import jp.ac.washi.quinte.api.SoldierInfo;
import jp.ac.washi.quinte.api.TileInfo;
import jp.ac.washi.quinte.api.TileType;

import org.apache.commons.lang.math.RandomUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;

public class Util {
	public static boolean isRoadAllOwnedInCursor(MapInfo map, Point p,
			CountryInfo country) {
		for (int x = p.x; x < p.x + 2; x++) {
			for (int y = p.y; y < p.y + 2; y++) {
				final TileInfo tile = map.getTile(x, y);
				if (tile.getType() == TileType.ROAD
					&& tile.getOwner() != country)
					return false;
			}
		}
		return true;
	}

	public static Point up(Point p) {
		return new Point(p.x, p.y - 1);
	}

	public static Point upleft(Point p) {
		return new Point(p.x - 1, p.y - 1);
	}

	public static Point left(Point p) {
		return new Point(p.x - 1, p.y);
	}

	public static Point down(Point p) {
		return new Point(p.x, p.y + 1);
	}

	public static boolean cointoss() {
		return RandomUtils.nextBoolean();
	}

	public static String inspect(CursorAction ca) {
		if (ca.getType() == RotateType.NONE)
			return "CURSOR: (none)";
		return "CURSOR: (" + ca.getX() + "," + ca.getY() + "), " + ca.getType();
	}

	public static String inspect(Point p) {
		if (p == null)
			return "NULL";
		else
			return "(" + p.x + "," + p.y + ")";
	}

	public static int manhattanDistance(Point p1, Point p2) {
		return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y);
	}

	/**
	 * 指定した座標からマンハッタン距離で近い順に位置を列挙。位置ゼロ(自分)は含まない
	 * 
	 * @param maxDist
	 * @return
	 */
	public static Iterable<Point> nearPoints(final int maxDist,
			final Point center) {
		return new Iterable<Point>() {
			@Override
			public Iterator<Point> iterator() {
				return new UnmodifiableIterator<Point>() {
					private int currentDist = 1;
					private int index = 0;
					private final List<Point> points = Lists.newArrayList();

					@Override
					public boolean hasNext() {
						return maxDist > 0 && currentDist <= maxDist;
					}

					@Override
					public Point next() {
						if (points.isEmpty())
							initialize(currentDist);
						final Point p = center.add(points.get(index));
						increment();
						return p;
					}

					private void increment() {
						index++;
						if (points.size() <= index) {
							initialize(currentDist + 1);
						}
					}

					private void initialize(int d) {
						currentDist = d;
						index = 0;
						points.clear();
						for (int i = 0; i < currentDist; i++) {
							points.add(new Point(i, i - currentDist));
							points.add(new Point(currentDist - i, i));
							points.add(new Point(-i, currentDist - i));
							points.add(new Point(i - currentDist, -i));
						}
					}
				};
			}
		};

	}

	/**
	 * centerを中心にしたらせん状の座標を返す。centerは含まず。
	 * 
	 * @param map
	 * @param center
	 * @return
	 */
	public static Iterable<Point> spiralPoints(final int ring,
			final Point center) {
		return new Iterable<Point>() {
			@Override
			public Iterator<Point> iterator() {
				return new UnmodifiableIterator<Point>() {
					int index = 1;
					int x = 1;
					int y = 0;

					@Override
					public boolean hasNext() {
						return index <= ring;
					}

					@Override
					public Point next() {
						// エレガントじゃないなー
						final Point p = new Point(center.x + x, center.y + y);
						increment();
						return p;
					}

					private void increment() {
						if (y < index && x == index) {
							y++;
						} else if (y == index && -index < x) {
							x--;
						} else if (x == -index && -index < y) {
							y--;
						} else if (y == -index && x < index) {
							x++;
						}
						if (y == 0 && x == index) {
							index++;
							x = index;
						}
					}
				};
			}
		};
	}

	/**
	 * min,max is inclusive(like sql)
	 * 
	 * @param val
	 * @param min
	 * @param max
	 * @return
	 */
	public static boolean between(int val, int min, int max) {
		return min <= val && val <= max;
	}

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

	private static final PrintStream nullPrintStream =
		new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
			}
		});

	public static PrintStream log(String type) {
		if (System.getProperty("config.log." + type) != null)
			return System.err;
		else {
			return nullPrintStream;
		}
	}

	public static boolean inCursor(Point cursor, Point point) {
		return between(point.x, cursor.x, cursor.x + 1)
			&& between(point.y, cursor.y, cursor.y + 1);
	}

	public static List<SoldierInfo> getSoldiers(GameInfo info) {
		final List<SoldierInfo> result = Lists.newArrayList();
		for (CountryInfo c : allCountries(info))
			result.add(info.getMap().getSoldier(c));
		return result;
	}

	public static List<CountryInfo> allCountries(GameInfo info) {
		return Arrays.asList(info.getMyCountry(), info.getLeftCountry(), info
			.getRightCountry(), info.getOppositeCountry());
	}

}
