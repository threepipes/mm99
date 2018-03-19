import java.util.ArrayList;
import java.util.List;
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
		final double searchRatio = 0.2;
		solveSearching((int) Math.min(coins * searchRatio * 2,
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
				if (s.decline) continue;
				final double score = s.expect() + Math.sqrt(Math.log(t) / (s.count * 2));
				if (score > bestScore) {
					bestScore = score;
					best = s;
				}
			}
			if (passed > maxTime * 0.6 && best.expect() < 0.9) return;
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

	/*
	 * TODO memo: 信頼区間 分散の期待値
	 */
	void solveSearching(int numNotePlay) {
		numNotePlay = Math.min(30, numNotePlay);
		if(numNotePlay * noteTime * N > maxTime || numNotePlay * N > coins) {
			System.err.println("Play time over.");
			numNotePlay = Math.min(maxTime / (noteTime * N), coins / N);
		}
		
		if (numNotePlay > 0) for (Machine s: slot) {
			for (int i = 0; i < numNotePlay; i++) {
				if (i >= 10 && s.expectNote() * (1 + Const.meanDiff[i]) < 1) {
					break;
				}
				s.notePlay(1);
			}
			if (s.count >= 5) s.estimateSlot(rand);
		}
		
		solveUCB(numNotePlay > 0, maxTime, true);
	}
}

class Machine {
	final static int[] rate = {
		1000, 200, 100, 50, 20, 10, 5
	};
	final static double EPS = 1e-8;
	final static int SLOT_LABEL_N = 7;
	int id;
	int wins;
	int count;
	int[][] slot;
	int[] slotLen;
	boolean decline = false;
	Machine(int id) {
		this.id = id;
		slot = new int[3][SLOT_LABEL_N];

		slotLen = new int[3];
		for (int i = 0; i < 3; i++)
			slotHist[i] = new ArrayList<>();
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
	
	boolean playOK(int time) {
		return //BrokenSlotMachines.passed + time * BrokenSlotMachines.noteTime <= BrokenSlotMachines.maxTime && 
				BrokenSlotMachines.passed < BrokenSlotMachines.maxTime * 2 / 3 ||
				BrokenSlotMachines.coins > BrokenSlotMachines.C * 0.1 ||
				expect() >= 1;
	}
	

	List<int[]>[] slotHist = new List[3];

	void estimateSlot(Random r) {
		if (slotHist[0].size() < 3)
			return;
//		final double upd = expectNote();
		for (int i = 0; i < 3; i++) {
			int[][] hist = new int[slotHist[i].size()][3];
			for (int j = 0; j < slotHist[i].size(); j++) {
				hist[j] = slotHist[i].get(j);
			}
			int[] newSlot = estimateSlotEach(hist, r);
			slot[i] = newSlot;
			slotLen[i] = 0;
			for (int j = 0; j < 7; j++) {
				slotLen[i] += newSlot[j];
			}
		}
//		updateExp = Math.sqrt(upd * expectNote());
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
			final int i1 = r.nextInt(n);
			final int i2 = r.nextInt(n);
			final int d = nextDiff(hist, ord, dup, n, i1, i2);
			final int nextScore = nowScore + d;
			if (nextScore > bestScore) {
				bestScore = nowScore;
				bestState = ord.clone();
			}

			double temperature = temper((double) i / MAX_ITER);
			double prob = prob(nowScore, nextScore, temperature);
			if (r.nextDouble() <= prob) {
				next(hist, ord, dup, n, i1, i2);
				nowScore = nextScore;
			}
		}
		
		ord = bestState;
		for (int i = 0; i < n; i++) {
			dup[i] = countDuplication(hist[ord[(i - 1 + n) % n]], hist[ord[i]]);
		}
		
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
	
//	double updateExp = -1;
	double expectNote() {
//		if (updateExp >= 0) return updateExp;
		if (slotLen[0] == 0) return EPS;
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
		if(count > 0) return (double) wins / count;
		return EPS;
	}
	
	double expect() {
		final double ratio = 0.3;
		return (expectQuick() * ratio + expectNote() * (1 - ratio));
	}
	
	String displaySlot() {
		String res = "";
		for (int i = 0; i < SLOT_LABEL_N; i++) {
			res += (char)('A' + i) + "\t";
		}
		res += "\n";
		for (int i = 0; i < slot.length; i++) {
			for (int j = 0; j < slot[i].length; j++) {
				res += slot[i][j] + "\t";
			}
			res += "\n";
		}
		return res;
	}
}

class Log {
	public static boolean debug = false;
	public static void d(String s) {
		if (debug) System.err.println(s);
	}
}

class Const {
	public final static double[] meanDiff = {
			1.3589164428262415,
			1.3589164428262415,
			0.9007091091361786,
			0.6836253959899997,
			0.5554749321819902,
			0.4793351320838778,
			0.43067478810737875,
			0.3935590865526684,
			0.36346019971039717,
			0.33445689651841265,
			0.3148821688734855,
			0.30261616395837204,
			0.288396602687891,
			0.2753526015939337,
			0.26420589129250505,
			0.2563367248805319,
			0.24696743636290772,
			0.23897536757784485,
			0.23252229738843117,
			0.2258619028565174,
			0.21982846044117363,
			0.21378641502274004,
			0.20877778597288102,
			0.20420593163935388,
			0.19954988753261418,
			0.19584628091618908,
			0.19196289920154608,
			0.18777815582349053,
			0.18470385775152828,
			0.18090561009231915,
			0.17784712802968997,
	};
	public final static double[] medDiff = {
			-2.0,
			-2.0,
			-0.4565217391304349,
			-0.2617902640681684,
			-0.17119834644661935,
			-0.13914710972654898,
			-0.12075411192888064,
			-0.10347340239965919,
			-0.09073134326430221,
			-0.07605762821076612,
			-0.06718132524584153,
			-0.06901375206950577,
			-0.0646974249877379,
			-0.059885634151003675,
			-0.051575462627984736,
			-0.053211439287388584,
			-0.0530198248040572,
			-0.047338957113025826,
			-0.043305136412132406,
			-0.04075117586688126,
			-0.03628881406659201,
			-0.03850713090452973,
			-0.03771180268331542,
			-0.0343870820588007,
			-0.03310468714558812,
			-0.03257362962962962,
			-0.028675466545289185,
			-0.029465581219209613,
			-0.031157028672639475,
			-0.028084235202443963,
			-0.02526011068539813,
			0,
	};
}