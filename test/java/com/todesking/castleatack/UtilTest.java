package com.todesking.castleatack;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import jp.ac.washi.quinte.api.Point;

import org.junit.Test;

import com.google.common.collect.Lists;

public class UtilTest {
	@Test
	public void test_apiralPoints() throws Exception {
		assertThat(spp(1, 0, 0).size(), is(8));
		assertThat(spp(1, 0, 0).get(0), is(p(1, 0)));
		assertThat(spp(1, 0, 0), is(Arrays.asList(p(1, 0), p(1, 1), p(0, 1), p(
			-1,
			1), p(-1, 0), p(-1, -1), p(0, -1), p(1, -1))));

		assertThat(spp(1, 10, 10).get(1), is(p(11, 11)));

		assertThat(spp(2, 0, 0).size(), is(24));
		assertThat(spp(2, 0, 0).get(0), is(p(1, 0)));
		assertThat(spp(2, 0, 0).get(8), is(p(2, 0)));
		assertThat(spp(2, 0, 0).get(10), is(p(2, 2)));
		assertThat(spp(2, 0, 0).get(23), is(p(2, -1)));
	}

	@Test
	public void test_nearPoints() throws Exception {
		assertThat(nps(0).size(), is(0));
		assertThat(nps(1).size(), is(4));
		assertThat(nps(2).size(), is(4 + 8));
		assertThat(nps(1), is(not(hasItem(p(1, 1)))));
		assertThat(nps(2), is(hasItem(p(1, 1))));
		assertThat(nps(1, p(100, 100)), is(hasItem(p(101, 100))));
	}

	private List<Point> nps(int dist) {
		return nps(dist, p(0, 0));
	}

	private List<Point> nps(int dist, Point center) {
		return Lists.newArrayList(Util.nearPoints(dist, center));
	}

	private List<Point> spp(int ring, int x, int y) {
		return Lists.newArrayList(Util.spiralPoints(ring, p(x, y)));
	}

	private Point p(int x, int y) {
		return new Point(x, y);
	}

}
