package logic;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Route;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.objects.SumoColor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import de.tudresden.sumo.objects.SumoStringList;

public class SimulationManager {

    // =================================================================
    // CẤU HÌNH ĐƯỜNG DẪN (QUAN TRỌNG)
    // =================================================================
    
    // Tên file chạy của SUMO.
    // Nếu bạn đã cài SUMO vào biến môi trường (PATH), chỉ cần để "sumo-gui".
    // Nếu báo lỗi không tìm thấy, hãy thay bằng đường dẫn tuyệt đối (VD: "/usr/local/bin/sumo-gui")
    private static final String SUMO_BIN = "/Users/duongquytrang/sumo/bin/sumo"; 
    
    // Đường dẫn đến file cấu hình .sumocfg trong thư mục resources
    // Lưu ý: Dấu "/" đầu tiên nghĩa là bắt đầu từ thư mục gốc của project/resources
    private static final String CONFIG_FILE = "src/main/resources/frauasmap.sumocfg"; 

    // =================================================================
    // CÁC BIẾN QUẢN LÝ
    // =================================================================
    
    private SumoTraciConnection connection;
    private Random random = new Random(); // Dùng để random màu xe

    // Constructor
    public SimulationManager() {
        // Chưa kết nối ngay khi khởi tạo, chờ lệnh connect()
    }

    /**
     * Mở kết nối tới SUMO
     */
    public void connect() throws IOException, InterruptedException {
        System.out.println("DEBUG: [1] Đang chuẩn bị kết nối...");
        
        // --- PHẦN BẠN ĐANG THIẾU ---
        // Chuyển đổi đường dẫn tương đối sang tuyệt đối để tránh lỗi file not found
        java.io.File configFile = new java.io.File(CONFIG_FILE);
        String absoluteConfigPath = configFile.getAbsolutePath();
        // ---------------------------
        
        System.out.println("DEBUG: [2] Đường dẫn file config: " + absoluteConfigPath);

        if (!configFile.exists()) {
            throw new IOException("LỖI: Không tìm thấy file config tại: " + absoluteConfigPath);
        }

        System.out.println("DEBUG: [3] Đang khởi tạo đối tượng SumoTraciConnection...");
        
        // Bây giờ biến absoluteConfigPath đã có giá trị để dùng ở đây
        connection = new SumoTraciConnection(SUMO_BIN, absoluteConfigPath);
        
        // connection.addOption("verbose", "true"); // Tùy chọn debug

        System.out.println("DEBUG: [4] Đang gọi runServer()...");
        connection.runServer();

        // --- DÒNG QUAN TRỌNG ĐỂ TRÁNH TREO (LAG) ---
        System.out.println("DEBUG: [5] Server đã chạy. Đang chạy bước khởi động...");
        try {
            connection.do_timestep();
            System.out.println("DEBUG: [6] Bước khởi động thành công!");
        } catch (Exception e) {
            System.err.println("Lỗi khởi động step đầu tiên: " + e.getMessage());
        }
    }

    /**
     * Thực hiện bước mô phỏng tiếp theo (Next Step)
     * Tương ứng với việc bấm nút "Step" trong SUMO
     */
    public void nextStep() {
        if (connection != null && !connection.isClosed()) {
            try {
                connection.do_timestep();
            } catch (Exception e) {
                System.err.println("Lỗi khi chạy simulation step: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Đóng kết nối an toàn khi tắt app
     */
    public void close() {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * Hàm lấy Connection để các class khác (như MapRenderer) dùng ké
     */
    public SumoTraciConnection getConnection() {
        return connection;
    }

    // =================================================================
    // CÁC HÀM NGHIỆP VỤ (WRAPPER METHODS)
    // Giúp Controller gọi đơn giản hơn, che giấu sự phức tạp của TraaS
    // =================================================================

    /**
     * Thêm một chiếc xe vào đường (Edge) chỉ định
     * @param edgeId ID của con đường (Lấy từ TextField trên GUI)
     */
    public void addVehicle(String edgeId) throws Exception {
        // Tạo ID xe ngẫu nhiên để không bị trùng (VD: car_1234)
        String vehicleId = "car_" + System.currentTimeMillis(); 
        
        // Tạo ID Route ngẫu nhiên
        String routeId = "route_" + vehicleId;
        
        // Trong TraaS/SUMO, để thêm xe, ta cần một "Route" (Lộ trình).
        // Ở đây ta tạo lộ trình đơn giản chỉ gồm 1 cạnh (edgeId) mà người dùng nhập.
        SumoStringList edgeList = new SumoStringList();
        edgeList.add(edgeId);
        connection.do_job_set(Route.add(routeId, edgeList));
        
        // Thêm xe vào lộ trình vừa tạo
        // add(vehicleID, typeID, routeID, departTime, pos, speed, lane)
        // typeID = "DEFAULT_VEHTYPE" (loại xe mặc định có sẵn)
        // departTime = -2 (nghĩa là xuất phát ngay lập tức - "triggered")
        connection.do_job_set(Vehicle.add(vehicleId, "DEFAULT_VEHTYPE", routeId, -2, 0.0, 0.0, (byte)0));
        
        // Đổi màu xe ngẫu nhiên cho đẹp (Feature: Color-coded icons)
        SumoColor color = new SumoColor(random.nextInt(255), random.nextInt(255), random.nextInt(255), 255);
        connection.do_job_set(Vehicle.setColor(vehicleId, color));
        
        System.out.println("Đã thêm xe: " + vehicleId + " vào đường: " + edgeId);
    }
 }