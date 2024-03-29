import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class BrokenSlotMachinesVis {

	public String checkData(String seed) { return ""; }

	public String displayTestCase(String seed) {
		generateTestCase(Long.parseLong(seed));
		calculateExpected();
		return displayTestCaseNogen();
	}
	
	public String displayTestCaseNogen() {
		String s = "Seed = " + longTest.seed + "\n\nCoins: " + coins + "\n";
		s += "Max Time: " + maxTime + "\n";
		s += "Note Time: " + noteTime + "\n";
		s += "Num Machines: " + numMachines + "\n\n";
		for (int i = 0; i < numMachines; i++) {
			s += "Machine " + i + "...\n";
			for (int j = 0; j < 3; j++)
				s += "Wheel " + j + ": " + wheels[i][j].substring(0, wheelSize[i]) + "\n";
			s += "Expected payout rate: " + expected[i] + "\n\n";
		}
		return s;
	}

	private static int TIME_LIMIT = 30 * 1000;

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
			if (s.charAt(i) == c) ret++;
		}
		return ret;
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
	
	void swap(int[] a, int i1, int i2) {
		final int tmp = a[i1];
		a[i1] = a[i2];
		a[i2] = tmp;
	}

	int[] shuffle;
	static boolean doShuffle = false;
	private void generateTestCase(long seed) {
		Random r = new Random(seed);
		coins = 100 + r.nextInt(9901);
		maxTime = 100 + r.nextInt(9901);
		noteTime = 2 + r.nextInt(9);
		numMachines = r.nextInt(8) + 3; 
		machineState = new Random[numMachines];
		shuffle = new int[numMachines];
		
		Random sr = new Random();
		for (int i = 0; i < numMachines; i++) shuffle[i] = i;
		for (int i = 0; i < numMachines * 10; i++) {
			final int i1 = sr.nextInt(numMachines);
			final int i2 = sr.nextInt(numMachines);
			swap(shuffle, i1, i2);
		}
		wheels = new String[numMachines][3];
		wheelSize = new int[numMachines];
		for (int i = 0; i < numMachines; i++) {
			machineState[i] = new Random(r.nextLong());
			if (seed == 0 && i == 0)  {
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

	public int quickPlay(int machineNumber, int times) {
		if (times <= 0 || machineNumber < 0 || machineNumber >= numMachines) {
			longTest.addFatalError("Invalid parameters passed to quickPlay().\n");
			failed = true;
			return -1;
		}
		if (times > maxTime) {
			longTest.addFatalError("Attempted to play after time ran out.\n");
			failed = true;
			return -1;
		}
		maxTime -= times;  
		return Integer.parseInt(doPlay(machineNumber, times)[0]);
	}

	public String[] notePlay(int machineNumber, int times) {
		if (times <= 0 || machineNumber < 0 || machineNumber >= numMachines) {
			longTest.addFatalError("Invalid parameters passed to notePlay().\n");
			failed = true;
			return new String[]{"-1"};
		}
		if (times > maxTime || times * noteTime > maxTime) {
			longTest.addFatalError("Attempted to play after time ran out.\n");
			failed = true;
			return new String[]{"-1"};
		}
		maxTime -= times * noteTime;
		return doPlay(machineNumber, times);
	}

	public String[] doPlay(int machineNumber, int times) {
		if (doShuffle) machineNumber = shuffle[machineNumber];
		if (failed) return new String[]{"-1"};
		int win = 0;
		String[] ret = new String[times + 1];
		for (int i = 0; i < times; i++) {
			if (coins <= 0) {
				longTest.addFatalError("Attempted to play after coins ran out.\n");
				failed = true;
				return new String[]{"-1"};
			}
			result.used[machineNumber]++;
			coins--;
			Random r = machineState[machineNumber];
			int w = wheelSize[machineNumber];
			int[] res = new int[]{r.nextInt(w), r.nextInt(w), r.nextInt(w)};
			ret[i + 1] = "";
			for (int j = -1; j <= 1; j++)
				for (int k = 0; k < 3; k++)
					ret[i + 1] += wheels[machineNumber][k].charAt(res[k] + w + j);
			if ((ret[i + 1].charAt(3) != ret[i + 1].charAt(4)) || (ret[i + 1].charAt(3) != ret[i + 1].charAt(5)))
				continue;
			if (ret[i + 1].charAt(3) == 'A') win += 1000;
			if (ret[i + 1].charAt(3) == 'B') win +=  200;
			if (ret[i + 1].charAt(3) == 'C') win +=  100;
			if (ret[i + 1].charAt(3) == 'D') win +=   50;
			if (ret[i + 1].charAt(3) == 'E') win +=   20;
			if (ret[i + 1].charAt(3) == 'F') win +=   10;
			if (ret[i + 1].charAt(3) == 'G') win +=    5;
		}
		ret[0] = "" + win;
		coins += win;
		return ret;
	}
	
	void displayUsed() {
		for (int i = 0; i < numMachines; i++) {
			System.out.printf("%2d: %4d true=%f noteE=%f quickE=%f\n",
					i, result.used[i], expected[i],
					longTest.player.slot[i].expectNote(),
					longTest.player.slot[i].expectQuick());
		}
	}

	public double runTest(LongTest lt) {
		longTest = lt;
		generateTestCase(Long.parseLong(lt.getTest()));
		lt.setTimeLimit(TIME_LIMIT);
//		lt.playSlots(coins, maxTime, noteTime, numMachines);
		longTest.player = new BrokenSlotMachines();
		longTest.player.playSlots(coins, maxTime, noteTime, numMachines);
		int timeLeft = TIME_LIMIT - lt.getTime();
		if (timeLeft < 10) {
			lt.addFatalError("Time limit exceeded.\n");
			return -1;
		}
		if (!lt.getStatus()) {
			lt.addFatalError("Error calling playSlots()\n");
			return -1;
		}
//		int x = lt.getResult_playSlots();
		if (!lt.getStatus()) {
			lt.addFatalError("Error getting result from playSlots()\n");
			return -1;
		}  
		if (failed) {
			return -1;
		}
		return coins;
	}
	
	public Result run(int seed) {
		final double score = runTest(new LongTest(seed + ""));
		result.score = score;
		return result;
	}

	// Relative scoring, must be > 0.
	public double[] score(double[][] raw) {
		int coderCnt = raw.length;
		if (coderCnt == 0) return new double[0];
		int testCnt = raw[0].length;
		double[] res = new double[raw.length];
		for (int test = 0; test < testCnt; test++) {
			double best = -1e100;
			for (int i = 0; i < coderCnt; i++)
				if (raw[i][test] >= -0.5) {
					best = Math.max(best, raw[i][test]);
				}
			if (best <= 0.001) continue;
			for (int i=0; i < coderCnt; i++)
				if (raw[i][test] >= -0.5) {
					res[i] += raw[i][test] / best;
				}
		}
		for (int i=0; i < res.length; i++) {
			res[i] /= testCnt;
			res[i] *= 1000000.0;
		}
		return res;
	}
	
	static double[] readBests() {
		try {
			Scanner in = new Scanner(new FileReader("best.csv"));
			final int n = 5000;
			double[] bests = new double[n];
			for (int i = 0; i < n; i++) {
				String[] row = in.next().split(",");
				final double v = Double.parseDouble(row[1]);
				bests[i] = v;
			}
			return bests;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	static double[] bests;
	static {
		bests = readBests();
	}

	public static void main(String[] args) {
		boolean debug = true;
//		doShuffle = true;
//		debug = false;
		
//		Log.debug = debug;
		int start = 4972;
		int caseNum = 1;
		int end = start - 1 + caseNum;
		if (!debug) {
			start = 1001;
			end = 2000;
			caseNum = 1000;
		}
		double score = 0;
		List<Result> resList = new ArrayList<>();
		for (int i = start; i <= end; i++) {
			final double best = i <= 5000 ? bests[i - 1] : 1;
			BrokenSlotMachinesVis vis = new BrokenSlotMachinesVis();
			PlaySlots.field = vis;
			Result res = vis.run(i);
			resList.add(res);
			System.out.println(res + " " + best);
			score += res.score / best;
			if (debug) {
//				System.out.println(vis.displayTestCaseNogen());
				vis.displayUsed();
//				System.out.println();
			}
		}
		System.out.println(score / caseNum);
		if (!debug) outputResult(resList);
	}
	
	static double executeParallel(int caseNum) {
		final int start = 1001;
		final int end = start - 1 + caseNum;
		Log.debug = false;
		List<Generator> list = new ArrayList<>();
		for (int i = start; i <= end; i++) {
			final double best = bests[i - 1001];
			list.add(new Generator(i, best));
		}
		list.parallelStream().forEach(Generator::gen);
		double score = 0;
		for (Generator g: list) score += g.score;
		return score / caseNum;
	}
	
    static void outputResult(List<Result> results) {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MMdd_HHmmss");
        String date = sdf.format(c.getTime());
        String filename = "results/result_" + date + ".csv";
        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
            writer.println("seed,coins,maxTime,noteTime,numMachines,maxPayout,meanPayout,minPayout,score");
            for(Result r: results) {
                writer.println(r.csv());
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        saveSnapshot(date);
    }

    static void saveSnapshot(String date) {
        String filename = "snapshot/code_" + date + ".java";
        try {
            Path sourcePath = Paths.get("./src/BrokenSlotMachines.java");
            Path targetPath = Paths.get(filename);
            Files.copy(sourcePath, targetPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Generator {
	int seed;
	double score, best;
	public Generator(int seed, double best) {
		this.seed = seed;
		this.best = best;
	}
	void gen() {
		BrokenSlotMachinesVis vis = new BrokenSlotMachinesVis();
		Result res = vis.run(seed);
		score += res.score / best;
	}
}

class Result {
	long seed;
	int coins;
	int maxTime;
	int noteTime;
	int numMachines;
	double maxPay, meanPay, minPay;
	double score;
	int[] used;
	Result(long seed, int coins, int maxTime, int noteTime, int numMachines,
			double maxPayout, double meanPayout, double minPayout) {
		this.coins = coins;
		this.maxTime = maxTime;
		this.noteTime = noteTime;
		this.numMachines = numMachines;
		this.seed = seed;
		this.maxPay = maxPayout;
		this.meanPay = meanPayout;
		this.minPay = minPayout;
		used = new int[numMachines];
	}
	
	@Override
	public String toString() {
		return String.format("seed:%d coin:%d time:%d note:%d N:%d max:%f score:%f",
				seed, coins, maxTime, noteTime, numMachines, maxPay, score);
	}
	
	public String csv() {
		return String.format("%d,%d,%d,%d,%d,%f,%f,%f,%f",
				seed, coins, maxTime, noteTime, numMachines, maxPay, meanPay, minPay, score);
	}
}

class PlaySlots {
	static BrokenSlotMachinesVis field;
	public static int quickPlay(int machineNumber, int times) {
		return field.quickPlay(machineNumber, times);
	}
	
	public static String[] notePlay(int machineNumber, int times) {
		return field.notePlay(machineNumber, times);
	}
}

class LongTest {
	String seed;
	int tl;
	long time;
	BrokenSlotMachines player;
	LongTest(String seed) {
		this.seed = seed;
	}
	
	void addFatalError(String err) {
		System.err.println(err);
	}
	
	String getTest() {
		return seed;
	}
	
	void setTimeLimit(int tl) {
		this.tl = tl;
		time = System.currentTimeMillis();
	}
	
	int getTime() {
		return (int) (time - System.currentTimeMillis());
	}
	
	boolean getStatus() {
		return true;
	}
	
//	int getResult_playSlots() {
//		return 0;
//	}
	
//	int playSlots(int coins, int maxTime, int noteTime,
//			int numMachines, BrokenSlotMachinesVis vis) {
//		player = new BrokenSlotMachines(vis);
//		return player.playSlots(coins, maxTime, noteTime, numMachines);
//	}
}
