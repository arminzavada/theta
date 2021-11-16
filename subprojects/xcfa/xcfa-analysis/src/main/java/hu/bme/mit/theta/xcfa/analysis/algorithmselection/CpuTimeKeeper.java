package hu.bme.mit.theta.xcfa.analysis.algorithmselection;

import hu.bme.mit.theta.common.Tuple2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;

public final class CpuTimeKeeper {
	static long closedSolverTimes = 0;
	static Set<Long> closedSolverPids = new HashSet<>();

	/**
	 * Temporary solution:
	 * We cannot measure the time spent in child processes that are closed already,
	 * so this function has to be called before closing the solvers (with SolverManager.closeAll())
	 * and it saves the cpu time of these processes, before they are closed and
	 * adds it to the cpu time measured in getCurrentCpuTime()
	 */
	public static void saveSolverTimes() {
		long pid = ProcessHandle.current().pid();
		long solvertimes = 0;
		try {
			Process process = Runtime.getRuntime().exec("ps --ppid " + pid + " -o %p%x");
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = "";
			String cputimeString = reader.readLine(); // skip the TIME header
			while((cputimeString = reader.readLine())!=null) {
				Tuple2<Long, Long> psOutput = parsePsOutputLine(cputimeString);
				closedSolverPids.add(psOutput.get1());
				solvertimes += psOutput.get2();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		closedSolverTimes+=solvertimes;
	}

	/**
	 * Measures the time spent in this process and child processes (solver processes)
	 */
	public static long getCurrentCpuTime() {
		long pid = ProcessHandle.current().pid();
		long cputime = 0;
		try {
			Process process = Runtime.getRuntime().exec("ps --pid " + pid + " --ppid " + pid + " -o %p%x");
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = "";
			String cputimeString = reader.readLine(); // skip the TIME header
			while((cputimeString = reader.readLine())!=null) {
				Tuple2<Long, Long> psOutput = parsePsOutputLine(cputimeString);
				if(!closedSolverPids.contains(psOutput.get1())) {
					cputime+=psOutput.get2();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		cputime+=closedSolverTimes;
		return cputime;
	}

	private static Tuple2<Long, Long> parsePsOutputLine(String line) {
		String[] split = line.stripLeading().split(" ");
		checkState(split.length==2);
		long pid = Long.parseLong(split[0]);
		String[] timeSplit = split[1].split(":");
		long time = Long.parseLong(timeSplit[0])*3600+Long.parseLong(timeSplit[1])*60+Long.parseLong(timeSplit[2]);

		return Tuple2.of(pid, time);
	}
}
