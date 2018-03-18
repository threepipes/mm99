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
		final double searchRatio = 0.2;// * Math.pow((double) 1000 / coins, 0.1);
		solveSearching((int) Math.min(coins * searchRatio,
				maxTime * searchRatio / noteTime) / numMachines);
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
//				slot[rand.nextInt(N)].play(1);
				double sum = 0;
				for (int i = 0; i < N; i++) {
					sum += slot[i].expect();
				}
				double r = rand.nextDouble() * sum;
				for (int i = 0; i < N; i++) {
					r -= slot[i].expect();
					if (r <= 0) {
						slot[i].play(1);
						break;
					}
				}
			} else {
				// 活用パート
				Machine use = getBest();
				use.play(1);
			}
		}
	}
	
	void solveUCB(boolean played, int maxPlay, boolean useNote) {
		if (!played) for (Machine s: slot) s.play(1);
		for (int t = 1; passed < maxTime && coins > 0 && t < maxPlay; t++) {
			double bestScore = Integer.MIN_VALUE;
			Machine best = null;
			for (Machine s: slot) {
				final double score = s.expect() + Math.sqrt(Math.log(t) / (s.count * 2));
				if (score > bestScore) {
					bestScore = score;
					best = s;
				}
			}
			if (useNote && best.expect() < 0.9 && noteTime + passed <= maxTime) best.notePlay(1);
			else best.play(1);
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
		numNotePlay = Math.min(30, numNotePlay);
		if(numNotePlay * noteTime * N > maxTime || numNotePlay * N > coins) {
			System.err.println("Play time over.");
			numNotePlay = Math.min(maxTime / (noteTime * N), coins / N);
		}
		
		if (numNotePlay > 0) for (Machine s: slot) {
			for (int i = 0; i < numNotePlay; i++) {
				if (i >= 10 && s.expectNote() < 0.5 + i / 100.0) break;
				s.notePlay(1);
			}
		}
		
		solveUCB(numNotePlay > 0, Integer.MAX_VALUE, true);
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
		if (!playOK(time)) {
			BrokenSlotMachines.passed++;
			return 0;
		}
		int win = PlaySlots.quickPlay(id, time);
		wins += win;
		count += time;
		BrokenSlotMachines.coins += win - time;
		BrokenSlotMachines.passed += time;
		Log.d(String.format("%d: %d/%d", id, wins, count));
		return win;
	}
	
	String[] notePlay(int time) {
		if (!playOK(time)) {
			BrokenSlotMachines.passed++;
			return null;
		}
		String[] res = PlaySlots.notePlay(id, time);
		int win = Integer.parseInt(res[0]);
		wins += win;
		count += time;
		BrokenSlotMachines.coins += win - time;
		BrokenSlotMachines.passed += BrokenSlotMachines.noteTime * time;
		slotCount += 3 * time;
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
	
	boolean playOK(int time) {
		return //BrokenSlotMachines.passed + time * BrokenSlotMachines.noteTime <= BrokenSlotMachines.maxTime && 
				(BrokenSlotMachines.passed < BrokenSlotMachines.maxTime * 2 / 3 ||
				BrokenSlotMachines.coins > BrokenSlotMachines.C * 0.1 ||
				expect() >= 1);
	}
	
	double expectNote() {
		if (slotCount == 0) return EPS;
		double exp = 0;
		for (int i = 0; i < rate.length; i++) {
			double tmp = rate[i];
			for (int j = 0; j < 3; j++) {
				tmp *= (double) slot[j][i] / slotCount;
			}
			exp += tmp;
		}
		return exp;
	}
	
	double expectQuick() {
		if(count > 0) return (double) wins / count;
		return EPS;
	}
	
	double expect() {
		return 0.5 * (expectQuick() + expectNote());
	}
}

class Log {
	public static boolean debug = false;
	public static void d(String s) {
		if (debug) System.err.println(s);
	}
}