import java.io.*;
import java.util.*;

public class ParamSearcher {
    public static void main(String[] args) throws IOException {
        simulatedAnnealing();
    }

    static void simulatedAnnealing() throws IOException {
        SimulatedAnnealing sa = new SimulatedAnnealing();
        TreeSet<Ev> tree = sa.sa();
        for(Ev ev: tree) {
            // 最終評価(ここでテストケースを増やす)
            ev.evaluate();
            System.out.println(ev);
        }
    }
}

class SimulatedAnnealing {
    Ev initialEv = new Ev(Ev.PARAM_INI.clone());
    final double TEMPER = 0.01;
    final double TOP = 10;  // 最終的に残す上位
    final double SWAP_DIFF = 0.005; // 経過50%時に得点差SWAP_DIFFなら確率1/eでswap

    TreeSet<Ev> sa() throws IOException {
        TreeSet<Ev> tree = new TreeSet<>();
        Ev ev = initialEv;
        final int WARMING = 5;
        System.out.println("Start evaluate v1.0.");
        long time = System.currentTimeMillis();
        // 5回評価の平均時間を測る
        for (int i = 0; i < WARMING; i++) {
            ev.evaluate();
        }
        double score = ev.score;
        time = (System.currentTimeMillis() - time) / WARMING;
        tree.add(ev);
        System.out.println("Initial score: " + score);
        Ev best = ev;
        double bestScore = score;
        // 焼き鈍し時間を10時間で設定
        final long HOUR_10 = 10 * 3600 * 1000;
        final int MAX_ITER = (int)(HOUR_10 / time);
        System.out.println("Iter Num: " + MAX_ITER);
        Random rand = new Random(0);
        PrintWriter pw = new PrintWriter(
                new BufferedWriter(new FileWriter("log.txt")));
        pw.println(ev.csv());
        for(int i = 0; i < MAX_ITER; i++) {
            double degree = ((double)MAX_ITER - i) / MAX_ITER;
            Ev next = ev.generateNext(degree);
            double nextScore = next.evaluate();
            tree.add(next);
            if(tree.size() > TOP) tree.pollLast();
            pw.println(next.csv());
            if((i + 1) % 10 == 0) pw.flush();
            String output = String.format("Iter %d/%d: score=%f ", i, MAX_ITER, nextScore);
            if(nextScore > bestScore) {
                best = next;
                bestScore = nextScore;
                output += "Update! ";
            }
            double temperature = temper((double)i / MAX_ITER);
            double prob = prob(score, nextScore, temperature);
            output += String.format("T:%f P:%f ", temperature, prob);
            if(rand.nextDouble() <= prob) {
                ev = next;
                score = nextScore;
                output += "swap ";
            }
            output += String.format("Now: %s, Best: %s", ev, best);
            System.out.println(output);
        }
        pw.close();
        System.out.printf("Best: %s\n", best);
        return tree;
    }

    double temper(double r) {
        return Math.pow(TEMPER, r);
    }

    double prob(double curScore, double nextScore, double temper) {
        if(curScore <= nextScore) return 1;
        else return Math.pow(Math.E, (nextScore - curScore) / (temper * 10 * SWAP_DIFF));
    }
}

class Ev implements Comparable<Ev> {
    static Random rand = new Random(0);
    double score;
    int[] params;
    Ev(int[] params) {
        this.params = params;
    }

    Ev generateNext(double degree) {
        int i = rand.nextInt(PARAM_SIZE);
        int[] next = params.clone();
        final int paramSz = PARAM_MAX[i] - PARAM_BASE[i];
        int nowParam = next[i] - PARAM_BASE[i];
        nowParam += (int)Math.max(1, rand.nextDouble() * degree * paramSz) * (rand.nextBoolean() ? -1 : 1);
        next[i] = (nowParam + paramSz) % paramSz + PARAM_BASE[i];
        return new Ev(next);
    }

    public static final int[] PARAM_INI = {17, 46, 53, 17,  5,  9,  1, 18, 11, 64,  4, 14,  4,  3};  // 初期パラメータ
//  public static final int[] PARAM_INI = {20, 40, 10, 10,  6,  9,  5, 30,  5, 66, 10, 18,  5,  3};  // 初期パラメータ
    public static final int[] PARAM_MAX = {50, 90,100, 20, 10, 10, 10, 40, 15,100, 40, 30, 10,  9};  // パラメータ最大値
    public static final int[] PARAM_BASE= {10, 10,  0,  1,  5,  7,  1, 10,  3, 50,  0,  1,  1,  1};  // 下駄(パラメータ最小値)
    public static final int PARAM_SIZE = PARAM_INI.length;

    /**
     * パラメータ設定とスコアの格納
     * @return スコア
     */
    double evaluate() {
    	/*
    	BrokenSlotMachine:
	    	static double SEARCH_RATIO_TIME = 0.2; // 0.1 - 0.5
			static double SEARCH_RATIO_COIN = 0.4; // 0.1 - 0.9
			static int SWITCH_ALGO_BORDER = 1000;  // 0 - 10000
		
			static double UCB_EVAL_WEIGHT = 1.0;   // 0.1 - 2.0
			static double CUT_TIME_BORDER = 0.6;   // 0.5 - 1.0
			static double CUT_SCORE_BORDER = 0.9;  // 0.7 - 1.0
		
			static double UCBNOTE_EVAL_WEIGHT = 0.5;// 0.1 - 1.0
		
			static int NOTEPLAY_MAX = 30;   // 10 - 40
			static int ESTSLOT_BORDER = 5;  // 3 - 15
		
		Machine:
			static double PLAYOK_TIME_WEIGHT = 2.0 / 3.0;  // 0.5 - 1.0
			static double PLAYOK_COIN_WEIGHT = 0.1;        // 0.0 - 0.4
			
			static int EST_SPACE = 18;                     // 1 - 30
			static double ALP_EST_WEIGHT = 0.5;            // 0.1 - 1.0
		
			static double EXPECT_RATIO = 0.3;              // 0.1 - 0.9
    	 */
    	BrokenSlotMachines.SEARCH_RATIO_TIME = params[0] / 100.0;
    	BrokenSlotMachines.SEARCH_RATIO_COIN = params[1] / 100.0;
    	BrokenSlotMachines.SWITCH_ALGO_BORDER= params[2] * 100;
    	BrokenSlotMachines.UCB_EVAL_WEIGHT   = params[3] / 10.0;
    	BrokenSlotMachines.CUT_TIME_BORDER   = params[4] / 10.0;
    	BrokenSlotMachines.CUT_SCORE_BORDER  = params[5] / 10.0;
    	BrokenSlotMachines.UCBNOTE_EVAL_WEIGHT=params[6] / 10.0;
    	BrokenSlotMachines.NOTEPLAY_MAX      = params[7];
    	BrokenSlotMachines.ESTSLOT_BORDER    = params[8];
    	Machine.PLAYOK_TIME_WEIGHT = params[9]  / 100.0;
    	Machine.PLAYOK_COIN_WEIGHT = params[10] / 100.0;
    	Machine.EST_SPACE          = params[11];
    	Machine.ALP_EST_WEIGHT     = params[12] / 10.0;
    	Machine.EXPECT_RATIO       = params[13] / 10.0;
        return BrokenSlotMachinesVis.executeParallel(1000);
    }

    @Override
    public int compareTo(Ev o) {
        // タイブレークが必要な場合があるので注意
        return Double.compare(o.score, score);
    }

    public String csv() {
        String s = "";
        for(int i = 0; i < params.length; i++) s += params[i] + ",";
        s += score;
        return s;
    }

    @Override
    public String toString() {
        return Arrays.toString(params) + ":" + score;
    }
}