package ProjectMain;
import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Trafficlight;
import java.util.Collection;


public class Milestone1Demo { 


    public static void main(String[] args) {
        
      
        //  đường dẫn này để trỏ đến file thực thi "sumo" (KHÔNG phải sumo-gui)
        String SUMO_CMD = "/Users/duongquytrang/sumo/bin/sumo"; 


        //  đường dẫn này để trỏ đến file map mà mình dùng, ở đây, mình dùng file map trường mình đã được tải về 
        String CONFIG_FILE = "/Users/duongquytrang/eclipse-workspace/traffic-simulator/src/main/resources/frauasmap.sumocfg";
      

        System.out.println("Bắt đầu Demo Mốc 1...");
        SumoTraciConnection sumo = null; // SumoTraciConnection là một class trong it.polito.appeal.traci.SumoTraciConnection (đọc traas 1.1 API để biết )
        // ở đây mình tạo một instance tên là sumo của classumoTraciCponnection và cho nó giá trị 
        try {
            // 2. KHỞI CHẠY SUMO
            sumo = new SumoTraciConnection(SUMO_CMD, CONFIG_FILE); // ở đây mình khởi tạo biến sumo nhưng mà mình vẫn phải khởi tạo nó ở ngoài try 
            //vì finnally sẽ ko tìm được biến sumo nếu nó nằm trong try.
            sumo.runServer(); 
            System.out.println("Đã khởi chạy SUMO.");

            // 3. THỰC HIỆN "STEP SIMULATION"
            sumo.do_timestep();
            System.out.println("Đã thực hiện 1 bước mô phỏng (timestep).");


            // 4. THỰC HIỆN "LIST TRAFFIC LIGHTS"
 //Khai báo một biến mới tên allTrafficLightIDs kiểu Collection<String> và gán kết quả đã được ép kiểu vào nó.
            Collection<String> allTrafficLightIDs = (Collection<String>) sumo.do_job_get(Trafficlight.getIDList());
 //Hàm do_job_get trả về một Object chung chung. Bạn biết chắc chắn rằng kết quả cho yêu cầu getIDList phải là một "Bộ sưu tập các Chuỗi" (danh sách ID), vì vậy bạn ép kiểu nó thành Collection<String> để Java hiểu.
            System.out.println("--- DANH SÁCH ĐÈN GIAO THÔNG ---");
            if (allTrafficLightIDs.isEmpty()) {
                System.out.println("Không tìm thấy đèn giao thông nào trong bản đồ.");
            } else {
                for (String id : allTrafficLightIDs) {
                    System.out.println("ID Đèn: " + id);
                }
            }
            System.out.println("---------------------------------");

        } catch (Exception e) {
            System.err.println("Đã xảy ra lỗi!");
            e.printStackTrace();
        } finally {
            // 5. ĐÓNG KẾT NỐI
            if (sumo != null) {
                sumo.close();
                System.out.println("Đã đóng kết nối SUMO.");
            }
        }
        
        System.out.println("Demo Mốc 1 kết thúc.");
    }
}