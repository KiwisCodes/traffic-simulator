package data;

import java.util.concurrent.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.*;


public class SimulationQueue {
	private final BlockingQueue<SimulationState> queue;
	public SimulationQueue(int capacity) {
        this.queue = new ArrayBlockingQueue<>(capacity);
    }
	public void putState(SimulationState frame) throws InterruptedException {
        queue.put(frame);
    }

    public SimulationState takeState() throws InterruptedException {
        return queue.take(); 
    }
    
    //File hiện tại của bạn chỉ có hàm takeState() (cơ chế Blocking - chờ đến chết). Nếu dùng hàm này cho giao diện, ứng dụng sẽ bị đơ (Not Responding) ngay lập tức khi Queue rỗng.
    //Bạn cần thêm hàm pollState() (cơ chế Non-blocking - có thì lấy, không có thì thôi).
    
    /**
     * Hàm lấy dữ liệu cho Giao diện (Không được phép chờ/block)
     * @return SimulationState mới nhất hoặc null nếu queue rỗng
     */
    public SimulationState pollState() {
        return queue.poll();}

}
