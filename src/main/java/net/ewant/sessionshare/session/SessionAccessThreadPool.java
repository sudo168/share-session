package net.ewant.sessionshare.session;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SessionAccessThreadPool {
	
	public static final ExecutorService executorService;
	
	static {
		// 依据处理器数个数x2初始化线程池的大小
		executorService = Executors.newFixedThreadPool(1);
	}

	private SessionAccessThreadPool(){}
}
