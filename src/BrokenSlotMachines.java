import java.util.Random;

public class BrokenSlotMachines {
	static int coins, passed, noteTime, maxTime, N, C;
	Machine[] slot;
	Random rand = new Random(0);
	
	public int playSlots(int coins, int maxTime, int noteTime, int numMachines) {
		BrokenSlotMachines.coins = coins;
		BrokenSlotMachines.C = coins;
		BrokenSlotMachines.noteTime = noteTime;
		BrokenSlotMachines.maxTime = maxTime;
		BrokenSlotMachines.N = numMachines;
		passed = 0;
		slot = new Machine[numMachines];
		for (int i = 0; i < numMachines; i++) {
			slot[i] = new Machine(i);
		}
//		solveUCB(false);
		final double searchRatio = 0.2;
		solveSearching((int) Math.min(coins * searchRatio * 2,
				maxTime * searchRatio / noteTime) / numMachines);
//		solveEpsGreedy(0.5, 0.01);
//		PlaySlots.quickPlay(0, maxTime);
//		solveExpectWeight();
		return 0;
	}
	
	public static double gameTime() {
		return (double) passed / maxTime;
	}
	
	public static double gameTimeLeft() {
		return 1 - gameTime();
	}
	
	void solveEpsGreedy(double eps, double dec) {
		for (int t = 0; passed < maxTime && coins > 0; t++) {
			if (rand.nextDouble() < eps * Math.pow(Math.E, -t * dec)) {
				// 探索パート
				slot[rand.nextInt(N)].play(1);
			} else {
				// 活用パート
				Machine use = getBest();
				use.play(1);
			}
		}
	}
	
	void solveUCB(boolean played) {
		if (!played) for (Machine s: slot) s.play(1);
		for (int t = 1; passed < maxTime && coins > 0; t++) {
			double bestScore = -1;
			Machine best = null;
			for (Machine s: slot) {
				final double score = s.expect() * 0.05 + Math.sqrt(Math.log(t) / (s.count * 2));
				if (score > bestScore) {
					bestScore = score;
					best = s;
				}
				if (best == null) {
					Log.d("Error");
				}
			}
			best.play(1);
		}
	}
	
	void solveExpectWeight() {
		for (int t = 1; passed < maxTime && coins > 0; t++) {
			double bestScore = -1;
			Machine best = null;
			for (Machine s: slot) {
				final double score = (s.expect() + 1e-2) * rand.nextDouble();
				if (score > bestScore) {
					bestScore = score;
					best = s;
				}
			}
			best.play(1);
		}
	}
	
	Machine getBest() {
		Machine bestSlot = null;
		double best = -1;
		for (Machine s: slot) {
			final double exp = s.expect();
			if (exp > best) {
				best = exp;
				bestSlot = s;
			}
		}
		return bestSlot;
	}

	void solveSearching(int numNotePlay) {
		if(numNotePlay * noteTime * N > maxTime) {
			System.err.println("Play time over.");
			numNotePlay = maxTime / (noteTime * N);
		}
		
		if (numNotePlay > 0) for (Machine s: slot) s.notePlay(numNotePlay);
		
//		solveEpsGreedy(0.2, 0.01);
		solveUCB(numNotePlay > 0);
//		solveExpectWeight();
	}
}

class Machine {
	final static int[] rate = {
		1000, 200, 100, 50, 20, 10, 5
	};
	final static double EPS = 1e-8;
	int id;
	int wins;
	int count;
	int[][] slot;
	int slotCount = 0;
	Machine(int id) {
		this.id = id;
		slot = new int[3][7];
	}
	
	int play(int time) {
		if (!playOK()) {
			BrokenSlotMachines.passed++;
			return 0;
		}
		int win = PlaySlots.quickPlay(id, time);
		wins += win;
		count++;
		BrokenSlotMachines.coins += win - time;
		BrokenSlotMachines.passed += time;
		Log.d(String.format("%d: %d/%d", id, wins, count));
		return win;
	}
	
	String[] notePlay(int time) {
		if (!playOK()) {
			BrokenSlotMachines.passed++;
			return null;
		}
		String[] res = PlaySlots.notePlay(id, time);
		int win = Integer.parseInt(res[0]);
		wins += win;
		count++;
		BrokenSlotMachines.coins += win - time;
		BrokenSlotMachines.passed += BrokenSlotMachines.noteTime * time;
		slotCount += 3;
		for (int t = 0; t < time; t++) {
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					slot[i][res[t + 1].charAt(j * 3 + i) - 'A']++;
				}
			}
		}
		Log.d(String.format("%d: %d/%d", id, wins, count));
		return res;
	}
	
	boolean playOK() {
		return BrokenSlotMachines.passed < BrokenSlotMachines.maxTime * 2 / 3 ||
				BrokenSlotMachines.coins < BrokenSlotMachines.C * 0.1 ||
				expect() >= 1;
	}
	
	double expectNote() {
		if (slotCount == 0) return EPS;
		double exp = 0;
		for (int i = 0; i < rate.length; i++) {
			int tmp = rate[i];
			for (int j = 0; j < 3; j++) {
				tmp *= slot[j][i];
			}
			exp += tmp;
		}
		return exp / Math.pow(slotCount, 3);
	}
	
	double expectQuick() {
		if(count > 0) return (double) wins / count;
		return EPS;
	}
	
	double expect() {
		return expectQuick()// * BrokenSlotMachines.gameTime()
				+ expectNote();// * BrokenSlotMachines.gameTimeLeft();
	}
}

class Log {
	public static boolean debug = true;
	public static void d(String s) {
		if (debug) System.err.println(s);
	}
}