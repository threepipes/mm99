import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Experiment {

	private String[][] wheels;
	private int coins;
	private int maxTime;
	private int noteTime;
	private int numMachines;
	private Random[] machineState;
	private int[] payouts;
	private double[] expected;
	private int[] wheelSize;
	private boolean failed = false;
	private LongTest longTest;
	private Result result;

	private int count(String s, char c) {
		int ret = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == c)
				ret++;
		}
		return ret;
	}
	
	int[][][] slots;
	public String displayTestCaseNogen() {
		String s = "\n\nCoins: " + coins + "\n";
		s += "Max Time: " + maxTime + "\n";
		s += "Note Time: " + noteTime + "\n";
		s += "Num Machines: " + numMachines + "\n\n";
		slots = new int[numMachines][3][7];
		for (int i = 0; i < numMachines; i++) {
			s += "Machine " + i + "...\n";
			for (int j = 0; j < 3; j++) {
				String wheel = wheels[i][j].substring(0, wheelSize[i]);
				s += "Wheel " + j + ": " + wheel + "\n";
				for (char c: wheel.toCharArray()) {
					slots[i][j][c - 'A']++;
				}
			}
//			for (int j = 0; j < 3; j++) {
//				s += Arrays.toString(slots[i][j]) + "\n";
//			}
			
			s += "Expected payout rate: " + expected[i] + "\n\n";
		}
		return s;
	}

	private void calculateExpected() {
		payouts = new int[numMachines];
		expected = new double[numMachines];
		for (int i = 0; i < numMachines; i++) {
			payouts[i] += count(wheels[i][0], 'A') * count(wheels[i][1], 'A') * count(wheels[i][2], 'A') * 1000;
			payouts[i] += count(wheels[i][0], 'B') * count(wheels[i][1], 'B') * count(wheels[i][2], 'B') * 200;
			payouts[i] += count(wheels[i][0], 'C') * count(wheels[i][1], 'C') * count(wheels[i][2], 'C') * 100;
			payouts[i] += count(wheels[i][0], 'D') * count(wheels[i][1], 'D') * count(wheels[i][2], 'D') * 50;
			payouts[i] += count(wheels[i][0], 'E') * count(wheels[i][1], 'E') * count(wheels[i][2], 'E') * 20;
			payouts[i] += count(wheels[i][0], 'F') * count(wheels[i][1], 'F') * count(wheels[i][2], 'F') * 10;
			payouts[i] += count(wheels[i][0], 'G') * count(wheels[i][1], 'G') * count(wheels[i][2], 'G') * 5;
			expected[i] = 1.0 * payouts[i] / wheels[i][0].length() / wheels[i][1].length() / wheels[i][2].length();
		}
	}

	private void generateTestCase(long seed, int n) {
		Random r = new Random(seed);
		coins = 100 + r.nextInt(9901);
		maxTime = 100 + r.nextInt(9901);
		noteTime = 2 + r.nextInt(9);
		numMachines = n;
		machineState = new Random[numMachines];
		wheels = new String[numMachines][3];
		wheelSize = new int[numMachines];
		for (int i = 0; i < numMachines; i++) {
			machineState[i] = new Random(r.nextLong());
			if (seed == 0 && i == 0) {
				wheels[i][0] = wheels[i][1] = wheels[i][2] = "AABBBBCCCCCDDDDDDEEEEEEFFFFFFFGGGGGGGG";
				wheelSize[i] = wheels[i][0].length();
				continue;
			}
			wheelSize[i] = 10 + r.nextInt(21);
			for (int j = 0; j < 3; j++) {
				wheels[i][j] = "";
				for (int k = 0; k < wheelSize[i]; k++)
					wheels[i][j] += "AABBBBCCCCCDDDDDDEEEEEEFFFFFFFGGGGGGGG".charAt(r.nextInt(38));
				wheels[i][j] += wheels[i][j] + wheels[i][j];
			}
		}
		calculateExpected();
		double meanPay = 0;
		double maxPay = 0;
		double minPay = Integer.MAX_VALUE;
		for (int i = 0; i < numMachines; i++) {
			meanPay += expected[i];
			maxPay = Math.max(maxPay, expected[i]);
			minPay = Math.min(minPay, expected[i]);
		}
		result = new Result(seed, coins, maxTime, noteTime, numMachines, maxPay, meanPay / numMachines, minPay);
	}

	public String[] doPlay(int machineNumber, int times) {
		if (failed)
			return new String[] { "-1" };
		int win = 0;
		String[] ret = new String[times + 1];
		for (int i = 0; i < times; i++) {
			result.used[machineNumber]++;
			Random r = machineState[machineNumber];
			int w = wheelSize[machineNumber];
			int[] res = new int[] { r.nextInt(w), r.nextInt(w), r.nextInt(w) };
			ret[i + 1] = "";
			for (int j = -1; j <= 1; j++)
				for (int k = 0; k < 3; k++)
					ret[i + 1] += wheels[machineNumber][k].charAt(res[k] + w + j);
			if ((ret[i + 1].charAt(3) != ret[i + 1].charAt(4)) || (ret[i + 1].charAt(3) != ret[i + 1].charAt(5)))
				continue;
			if (ret[i + 1].charAt(3) == 'A')
				win += 1000;
			if (ret[i + 1].charAt(3) == 'B')
				win += 200;
			if (ret[i + 1].charAt(3) == 'C')
				win += 100;
			if (ret[i + 1].charAt(3) == 'D')
				win += 50;
			if (ret[i + 1].charAt(3) == 'E')
				win += 20;
			if (ret[i + 1].charAt(3) == 'F')
				win += 10;
			if (ret[i + 1].charAt(3) == 'G')
				win += 5;
		}
		ret[0] = "" + win;
		return ret;
	}

	public void testConvergence() {
		int n = 5000;
		generateTestCase(1, n);
		calculateExpected();
		TestMachine[] slot = new TestMachine[n];
		String sep = ",";
		String res = "trueExp";
		int times = 30;
		for (int i = 0; i < times; i++)
			res += ",exp" + (i + 1);
		for (int i = 0; i < n; i++) {
			slot[i] = new TestMachine(i);
			String row = "" + expected[i];
			for (int j = 0; j < times; j++) {
				slot[i].notePlay(this);
				row += sep + slot[i].expectNote();
			}
			// System.out.println(row);
			res += "\n" + row;
		}

		try {
			String filename = "stat.csv";
			PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
			writer.println(res);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void testCompose() {
		final int n = 20;
		generateTestCase(1, n);
		calculateExpected();
		System.out.println(displayTestCaseNogen());
		TestMachine[] slot = new TestMachine[n];
		for (int t = 0; t < n; t++) {
			slot[t] = new TestMachine(t);
			final int times = 5;
			for (int i = 0; i < times; i++) {
				slot[t].notePlay(this);
			}
			System.out.println("Machine " + t);
			for (int i = 0; i < 3; i++) {
				System.out.println(Arrays.toString(slots[t][i]));
			}
			System.out.printf("true-exp:%f est-exp:%f\n", expected[t], slot[t].expectNote());
			for (int i = 0; i < 3; i++) {
				System.out.println(Arrays.toString(slot[t].slot[i]));
			}
			Random rand = new Random(0);
			slot[t].estimateSlot(rand);
			System.out.printf("update-exp:%f\n", slot[t].expectNote());
			for (int i = 0; i < 3; i++) {
				System.out.println(Arrays.toString(slot[t].slot[i]));
			}
			System.out.println();
		}
	}
	
	public void statCompose() {
		final int n = 1000;
		
		final int times = 30;
		for (int k = 5; k <= times; k+=5) {
			System.out.println(k);
			generateTestCase(1, n);
			calculateExpected();
			TestMachine[] slot = new TestMachine[n];
			String res = "trueExp,estExp,updExp\n";
			for (int t = 0; t < n; t++) {
				slot[t] = new TestMachine(t);
				for (int i = 0; i < k; i++) {
					slot[t].notePlay(this);
				}
				Random rand = new Random(0);
				double trueExp = expected[t];
				double estExp = slot[t].expectNote();
				slot[t].estimateSlot(rand);
				double updExp = slot[t].expectNote();
				res += String.format("%f,%f,%f\n", trueExp, estExp, updExp);
			}

			try {
				String filename = "statEst" + k + ".csv";
				PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
				writer.print(res);
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		new Experiment().testCompose();
	}
}

class TestMachine {
	final static int[] rate = { 1000, 200, 100, 50, 20, 10, 5 };
	final static double EPS = 1e-8;
	int id;
	int wins;
	int count;
	int[][] slot;
	int[] slotLen;
	boolean decline = false;

	TestMachine(int id) {
		this.id = id;
		slot = new int[3][7];
		slotLen = new int[3];
		for (int i = 0; i < 3; i++)
			slotHist[i] = new ArrayList<>();
	}

	List<int[]>[] slotHist = new List[3];

	void estimateSlot(Random r) {
		if (slotHist[0].size() < 3)
			return;
		
		for (int i = 0; i < 3; i++) {
			final int len = slotHist[i].size();
			int[][] hist = new int[len][3];
			for (int j = 0; j < len; j++) {
				hist[j] = slotHist[i].get(j);
			}
			int[] newSlot = len < 10 ? estimateSlotDP(hist) : estimateSlotEach(hist, r);
			slot[i] = newSlot;
			slotLen[i] = 0;
			for (int j = 0; j < 7; j++) {
				slotLen[i] += newSlot[j];
			}
		}
	}
	
	private int[] estimateSlotDP(int[][] hist) {
		final int n = hist.length;
		int[][] table = new int[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				table[i][j] = 3 - countDuplication(hist[i], hist[j]);
			}
		}
		final int sz = 1 << n;
		int[][] dp = new int[sz][n];
		int[][] pre = new int[sz][n];
		for (int i = 0; i < sz; i++) {
			Arrays.fill(dp[i], Integer.MAX_VALUE);
			Arrays.fill(pre[i], -1);
		}
		for (int i = 0; i < n; i++) {
			dp[1 << i][i] = 3;
		}
		for (int i = 1; i < sz; i++) {
			for (int j = 0; j < n; j++) {
				if (dp[i][j] == Integer.MAX_VALUE) continue;
				for (int k = 0; k < n; k++) {
					if ((i & 1 << k) > 0) continue;
					final int cost = dp[i][j] + table[j][k];
					if (cost < dp[i | 1 << k][k]) {
						dp[i | 1 << k][k] = cost;
						pre[i | 1 << k][k] = j;
					}
				}
			}
		}
		int min = Integer.MAX_VALUE;
		int minId = -1;
		for (int i = 0; i < n; i++) {
			if (dp[sz - 1][i] < min) {
				min = dp[sz - 1][i];
				minId = i;
			}
		}
		int[] ord = new int[n];
		int id = minId;
		int bitmap = sz - 1;
		int[] dup = new int[n];
		for (int i = n - 1; i >= 0; i--) {
			ord[i] = id;
			id = pre[bitmap][id];
			bitmap = bitmap & ~(1 << ord[i]);
			if (id >= 0) dup[i] = 3 - table[id][ord[i]];
			else dup[i] = 0;
		}
		
		dumpOrder(ord, dup, hist);
		
		return updateSlot(hist, ord, dup);
	}

	final double TEMPER = 0.01;
	final double TOP = 10; // 最終的に残す上位
	final double SWAP_DIFF = 2; // 経過50%時に得点差SWAP_DIFFなら確率1/eでswap

	private int[] estimateSlotEach(int[][] hist, Random r) {
		int n = hist.length;
		int[] ord = new int[n];
		int[] dup = new int[n];
		for (int i = 0; i < n; i++)
			ord[i] = i;
		int nowScore = 0;
		for (int i = 0; i < n; i++) {
			dup[i] = countDuplication(hist[(i - 1 + n) % n], hist[i]);
			nowScore += dup[i];
		}
		int bestScore = nowScore;
		int[] bestState = ord.clone();
		final int MAX_ITER = 5000;
		for (int i = 0; i < MAX_ITER; i++) {
//			dumpOrder(ord, dup, hist);
			final int i1 = r.nextInt(n);
			final int i2 = r.nextInt(n);
			final int d = nextDiff(hist, ord, dup, n, i1, i2);
			final int nextScore = nowScore + d;
//			String output = String.format("Iter %d/%d: score=%d ", i, MAX_ITER, nextScore);
			if (nextScore > bestScore) {
				bestScore = nowScore;
				bestState = ord.clone();
//				output += "Update! ";
			}

			double temperature = temper((double) i / MAX_ITER);
			double prob = prob(nowScore, nextScore, temperature);
//			output += String.format("T:%f P:%f ", temperature, prob);
			if (r.nextDouble() <= prob) {
				next(hist, ord, dup, n, i1, i2);
				nowScore = nextScore;
//				output += "swap ";
			}
//			output += String.format("Now: %d, Best: %d", nowScore, bestScore);
//			System.out.println(output);
		}
		
		ord = bestState;
		nowScore = 0;
		for (int i = 0; i < n; i++) {
			dup[i] = countDuplication(hist[ord[(i - 1 + n) % n]], hist[ord[i]]);
			nowScore += dup[i];
		}
//		dumpOrder(ord, dup, hist);
		
		return updateSlot(hist, ord, dup);
	}
	
	int[] updateSlot(int[][] hist, int[] ord, int[] dup) {
		int[] newSlot = new int[7];
		for (int i = 0; i < ord.length; i++) {
			int[] slotUnit = hist[ord[i]];
			for (int j = (i == 0 ? 0 : dup[i]); j < 3; j++) {
				newSlot[slotUnit[j]]++;
			}
		}
		return newSlot;
	}

	double temper(double r) {
		return Math.pow(TEMPER, r);
	}

	double prob(double curScore, double nextScore, double temper) {
		if (curScore <= nextScore)
			return 1;
		else
			return Math.pow(Math.E, (nextScore - curScore) / (temper * 10 * SWAP_DIFF));
	}

	private void dumpOrder(int[] ord, int[] dup, int[][] hist) {
		for (int i = 0; i < ord.length; i++) {
			for (int j = 0; j < 3; j++) {
				System.out.print((char) (hist[ord[i]][j] + 'A'));
			}
			System.out.print(" ");
		}
		System.out.println();

		for (int i = 0; i < dup.length; i++) {
			System.out.printf("%3d ", dup[i]);
		}
		System.out.println();
	}

	int nextDiff(int[][] hist, int[] o, int[] dup, int n, int i1, int i2) {
		final int diff = dup[i1] + dup[(i1 + 1) % n] + dup[i2] + dup[(i2 + 1) % n];
		int dup10 = countDuplication(hist[o[(i1 - 1 + n) % n]], hist[o[i2]]);
		int dup11 = countDuplication(hist[o[i2]], hist[o[(i1 + 1) % n]]);
		int dup20 = countDuplication(hist[o[(i2 - 1 + n) % n]], hist[o[i1]]);
		int dup21 = countDuplication(hist[o[i1]], hist[o[(i2 + 1) % n]]);
		return -diff + (dup10 + dup11 + dup20 + dup21);
	}

	void next(int[][] hist, int[] o, int[] dup, int n, int i1, int i2) {
		final int tmp = o[i1];
		o[i1] = o[i2];
		o[i2] = tmp;
		dup[i1] = countDuplication(hist[o[(i1 - 1 + n) % n]], hist[o[i1]]);
		dup[(i1 + 1) % n] = countDuplication(hist[o[i1]], hist[o[(i1 + 1) % n]]);
		dup[i2] = countDuplication(hist[o[(i2 - 1 + n) % n]], hist[o[i2]]);
		dup[(i2 + 1) % n] = countDuplication(hist[o[i2]], hist[o[(i2 + 1) % n]]);
	}

	private int countDuplication(int[] pre, int[] post) {
		out: for (int i = 0; i < 3; i++) {
			for (int j = 0; j + i < 3; j++) {
				if (pre[i + j] != post[j])
					continue out;
			}
			return 3 - i;
		}
		return 0;
	}

	String[] notePlay(Experiment exp) {
		final int time = 1;
		String[] res = exp.doPlay(id, time);
		int win = Integer.parseInt(res[0]);
		wins += win;
		count += time;
		for (int t = 0; t < time; t++) {
			for (int i = 0; i < 3; i++) {
				slotLen[i] += 3;
				int[] h = new int[3];
				for (int j = 0; j < 3; j++) {
					final int id = res[t + 1].charAt(j * 3 + i) - 'A';
					slot[i][id]++;
					h[j] = id;
				}
				slotHist[i].add(h);
			}
		}
		Log.d(String.format("%d: %d/%d", id, wins, count));
		return res;
	}

	double expectNote() {
		if (slotLen[0] == 0)
			return EPS;
		double exp = 0;
		for (int i = 0; i < rate.length; i++) {
			double tmp = rate[i];
			for (int j = 0; j < 3; j++) {
				if (slotLen[j] == 0) continue;
				tmp *= (double) slot[j][i] / slotLen[j];
			}
			exp += tmp;
		}
		return exp;
	}

	double expectQuick() {
		if (count > 0)
			return (double) wins / count;
		return EPS;
	}

	double expect() {
		final double ratio = 0.3;
		return (expectQuick() * ratio + expectNote() * (1 - ratio));
	}
}