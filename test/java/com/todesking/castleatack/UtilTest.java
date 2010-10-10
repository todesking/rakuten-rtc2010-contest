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
		assertThat(spp(1, 0, 0).get(1), is(p(1, 1)));
		assertThat(spp(1, 0, 0).get(3), is(p(-1, 1)));
		assertThat(spp(1, 0, 0).get(5), is(p(-1, -1)));
		assertThat(spp(1, 0, 0).get(7), is(p(1, -1)));
		assertThat(spp(1, 0, 0), is(Arrays.asList(p(1, 0), p(1, 1), p(0, 1), p(
			-1,
			1), p(-1, 0), p(-1, -1), p(0, -1), p(1, -1))));
	}

	private List<Point> spp(int size, int x, int y) {
		return Lists.newArrayList(Util.spiralPoints(size, p(x, y)));
	}

	private Point p(int x, int y) {
		return new Point(x, y);
	}

}
