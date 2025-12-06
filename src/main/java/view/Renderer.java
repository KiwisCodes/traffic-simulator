package view;

// --- Java Util ---
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer; // Để xử lý click chuột

// --- JavaFX (Giao diện) ---
import javafx.scene.Group;
import javafx.scene.Cursor;
import javafx.scene.effect.DropShadow; // Hiệu ứng phát sáng
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;      // Vẽ ngã tư
import javafx.scene.shape.Polyline;    // Vẽ đường gấp khúc
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap; // Bo tròn đầu đường

// --- TraaS / SUMO (Thư viện mô phỏng) ---
import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Lane;           // Lệnh lấy thông tin Lane
import de.tudresden.sumo.cmd.Junction;       // Lệnh lấy thông tin Junction
import de.tudresden.sumo.objects.SumoGeometry;   // Chứa danh sách tọa độ hình dáng
import de.tudresden.sumo.objects.SumoPosition2D; // Tọa độ X, Y lẻ
import model.infrastructure.MapManager;
import model.infrastructure.TrafficlightObject;
// --- Project Classes (Các class của nhóm bạn) ---
import util.CoordinateConverter;
//import cần thiết cho đèn giao thông:
import de.tudresden.sumo.cmd.Trafficlight; // Lệnh lấy đèn
import de.tudresden.sumo.cmd.Junction;     // Lệnh lấy vị trí ngã tư
import javafx.scene.shape.Circle;          // Để vẽ hình tròn
	
// import cho vẽ xe
import de.tudresden.sumo.objects.SumoColor;     // Để hiểu màu sắc
import javafx.scene.shape.Polygon;   // vẽ hình 
import java.util.Map;
public class Renderer {
	
	// Controller gọi hàm này để cài đặt kích thước bản đồ
    public void setConverter(MapManager mapManager) {
        this.converter.setBound(mapManager);
    }
    // Controller gọi hàm này để lấy converter ra tính toán AutoFit
    public CoordinateConverter getConverter() {
        return this.converter;
    }
    
    // 1. KHAI BÁO BIẾN (Ở đây!)
    private CoordinateConverter converter; 
    
    private static final DropShadow HOVER_GLOW = new DropShadow();
    
    //Khang's
    private Map<Character, Color> tl_color_map = new HashMap<>(); // map the state of each traffic light to each color

    // 2. KHỞI TẠO (Trong Constructor)
    public Renderer() {
        // Tạo mới đối tượng converter khi Renderer được sinh ra
        this.converter = new CoordinateConverter(); 
        
        // Các cài đặt khác (Glow effect...)
        HOVER_GLOW.setColor(Color.CYAN);
        HOVER_GLOW.setRadius(10);
        HOVER_GLOW.setSpread(0.6);
        
        //Khang's
        this.tl_color_map.put('r', Color.RED);                         // red
		this.tl_color_map.put('R', Color.rgb(255, 80, 80));            // bright_red

		this.tl_color_map.put('y', Color.YELLOW);                      // yellow
		this.tl_color_map.put('Y', Color.rgb(255, 255, 120));          // bright_yellow

		this.tl_color_map.put('g', Color.GREEN);                       // green
		this.tl_color_map.put('G', Color.rgb(120, 255, 120));          // bright_green

		// JavaFX has no blinking colors. You must implement blinking using transitions.
		// Here: normal + brighter versions.
		this.tl_color_map.put('o', Color.rgb(255, 200, 0));            // blinking_yellow (base)
		this.tl_color_map.put('O', Color.rgb(255, 230, 50));           // bright_blinking_yellow (base)

		this.tl_color_map.put('a', Color.rgb(139, 0, 0));              // dark_red (≈ Firebrick / DarkRed)
		this.tl_color_map.put('b', Color.rgb(184, 134, 11));           // dark_yellow (≈ DarkGoldenRod)
		this.tl_color_map.put('c', Color.rgb(0, 100, 0));              // dark_green (≈ DarkGreen)
    }

 // Trong Renderer.java

    /**
     * Vẽ toàn bộ lớp tĩnh: Đường, Ngã tư.
     * @param mapManager: chứa thông tin ID và biên (bounds)
     * @param connection: dùng để lấy hình dáng (Shape) chi tiết từ SUMO
     */
    public void renderLanes(
            MapManager mapManager, 
            SumoTraciConnection connection, 
            Pane carPane, 
            Pane bikePane,
            Pane mixedPane,
            Consumer<String> onLaneClick
    ) {
    	
    	//should input list of
        // 1. Xóa sạch bản vẽ cũ
        carPane.getChildren().clear();
        bikePane.getChildren().clear();
        mixedPane.getChildren().clear();
        
        System.out.println("Renderer: Drawing lanes...");

        try {
            List<String> laneIds = (List<String>) connection.do_job_get(Lane.getIDList());

            for (String laneId : laneIds) {
                // Bỏ qua lane nội bộ (ngã tư)
                if (laneId.startsWith(":")) continue;

                try {
                    // Kiểm tra quyền truy cập: Lane này cho phép xe gì?
                	Object response = connection.do_job_get(Lane.getAllowed(laneId));
                    @SuppressWarnings("unchecked")
					List<String> allowedClasses = (List<String>) response;
                    
                    
                    // 1. Có được phép đi Xe đạp không?
                 
                    boolean allowBike = allowedClasses.contains("bicycle")|| allowedClasses.isEmpty();
                 // 2. Có được phép đi Ô tô không?
                    // (Trong SUMO, ô tô con là "passenger". Nếu list rỗng nghĩa là cho phép tất cả -> cũng là có ô tô)
                    boolean allowCar = allowedClasses.contains("passenger") || allowedClasses.isEmpty();
                    
                 // 3. [QUAN TRỌNG] CƠ CHẾ DỰ PHÒNG (Fallback)
                    // Nếu một con đường lạ (bus, taxi, truck, delivery) không lọt vào danh sách trên,
                    // ta mặc định ném nó vào pane Ô tô để nó HIỆN LÊN thay vì biến mất.
                    if (!allowBike && !allowCar) {
                        allowCar = true; 
                    }
                    Shape laneShape = createLaneShape(laneId, connection, onLaneClick);
                    if (laneShape != null) {
                        // CASE 1: Đường Hỗn Hợp (Cả 2 cùng đi được)
                        if (allowBike && allowCar) { 
                            mixedPane.getChildren().add(laneShape); // VÀO MIXED
                        }
                     // CASE 2: Chỉ cho Xe Đạp
                        else if (allowBike) {
                            bikePane.getChildren().add(laneShape); // VÀO BIKE
                        }
                        else {
                            carPane.getChildren().add(laneShape); // VÀO CAR
                        }
                 
                     
                    }} catch (Exception e) {
                	e.printStackTrace();
                }
            }
            System.out.println("Renderer: Vẽ xong Lane.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    
    private Shape createLaneShape(String laneId, SumoTraciConnection connection,Consumer<String> onLaneClick) {
    	//should input laneObject here
    	//Consumer là một type đặc biệt, nó dùng để lưu các dòng code chứ không phải chỉ là bién int, char,... bình thường.  
    	// Ở đây như kiểu là bạn được add cái function onLaneClick của MainController.java vào cái hàm này của bạn
    	//Tuy nhiên hiểu rõ hơn là MainController nó đang uỷ quyền cho cái Renderer của mình là: Này Renderer, tôi bận lắm không đứng canh chuột được. Cầm lấy cái lệnh này, bao giờ có ai click vào đường thì ông chạy cái lệnh này giúp tôi nhé!
    	
        try {
            // 1. Hỏi SUMO hình dáng của lane này (Trả về List tọa độ X,Y)
            SumoGeometry geometry = (SumoGeometry) connection.do_job_get(Lane.getShape(laneId));
            // SumoGeometry là một đối tượng chứa danh sách một loạt các điểm tọa độ (X, Y). Khi bạn nối các điểm này lại với nhau theo thứ tự, bạn sẽ tạo ra hình dáng của con đường.
            // Khi chạy lệnh kia bạn nhận về một túi chưa public List<SumoPosition2D> coords 
            // SumoPosition2D: Là một điểm, chứa x và y (tính bằng mét trong bản đồ thật).
            // coords: Là danh sách các điểm đó.
            //Lane.getShape(laneId): Đây là bạn viết một bức thư gửi SUMO: "Gửi SUMO, làm ơn cho tôi xin tọa độ hình dáng của làn đường tên là laneId".
            
            
           
            // 2. Tạo Polyline (Đường gấp khúc) của JavaFX
            Polyline lanePolyline = new Polyline();
            
            // 3. Duyệt qua các điểm tọa độ từ SUMO, convert sang JavaFX và thêm vào Polyline
            for (SumoPosition2D pos : geometry.coords) {
                // 1. Lấy điểm tọa độ thực (Mét)
                double realX = pos.x; 
                double realY = pos.y;

                // 2. Chuyển sang tọa độ màn hình (Pixel)
                double screenX = converter.toScreenX(realX);
                double screenY = converter.toScreenY(realY);

                // 3. Thêm điểm này vào đường gấp khúc (Polyline)
                lanePolyline.getPoints().addAll(screenX, screenY);
            }
            double laneWidth = (double) connection.do_job_get(Lane.getWidth(laneId));
            // 4. Style cho đường (Màu sắc, độ dày)
            lanePolyline.setStroke(Color.rgb(50, 50, 50)); // Màu đường nhựa
            lanePolyline.setStrokeWidth(laneWidth);   // Độ rộng đường (pixel) - có thể chỉnh theo zoom nếu muốn xịn
            lanePolyline.setStrokeLineCap(StrokeLineCap.ROUND);
            
            // Lưu ID vào UserData để sau này click vào biết là đường nào
            lanePolyline.setUserData(laneId);

            // 5. Thêm hiệu ứng chuột (Logic cũ của bạn rất ổn!)
            lanePolyline.setOnMouseEntered(e -> {
                lanePolyline.setEffect(HOVER_GLOW);
                lanePolyline.setStroke(Color.LIGHTGRAY);
                lanePolyline.setCursor(Cursor.HAND);
            });
            lanePolyline.setOnMouseExited(e -> {
                lanePolyline.setEffect(null);
                lanePolyline.setStroke(Color.rgb(50, 50, 50)); // Trả về đúng màu gốc ban đầu
                lanePolyline.setCursor(Cursor.DEFAULT);
            });
            
         // [MỚI] Xử lý sự kiện Click dựa trên yêu cầu của MainController
            lanePolyline.setOnMouseClicked(e -> {
                if (onLaneClick != null) { 
                    // Lấy ID ra
                    String clickedId = (String) lanePolyline.getUserData();
                    // Kích hoạt hàm bên Controller (điền vào ô text field...)
                    onLaneClick.accept(clickedId);
                    //Trong Java, khi bạn dùng Consumer (người tiêu dùng), bản thân cái Consumer đó chỉ là một bọc chứa (một cái hộp/cái vỏ).
                    //Bên trong cái vỏ đó chứa một mệnh lệnh (đoạn code xử lý). Nhưng mệnh lệnh này nằm im, chưa chạy.
                    //Hàm accept() chính là hành động "Bấm Nút" hoặc "Kéo Cò" để mệnh lệnh đó thực sự chạy.
                    //Ý nghĩa: "Thực thi ngay đoạn code mà MainController đã gửi gắm, và dùng cái clickedId này làm nguyên liệu đầu vào cho đoạn code đó!"
                }
            });

            return lanePolyline;

        } catch (Exception e) {
            // Đôi khi có lane lỗi hoặc dữ liệu trống, bỏ qua
            return null; 
        }
    }
    
    public void renderJunctions(SumoTraciConnection connection, Pane junctionPane, Consumer<String> onJunctionClick) {
        // 1. Tự dọn dẹp Pane trước khi vẽ
        junctionPane.getChildren().clear();

        try {
            List<String> junctionIds = (List<String>) connection.do_job_get(Junction.getIDList());
            
            for (String juncId : junctionIds) {
                // Bỏ qua ngã tư nội bộ
                if (juncId.startsWith(":")) continue;

                // Tạo hình (dùng lại hàm helper cũ của bạn)
                Shape junctionShape = createJunctionShape(juncId, connection);
                
                if (junctionShape != null) {
                    // Gắn sự kiện click
                    junctionShape.setOnMouseClicked(e -> {
                        if(onJunctionClick != null) onJunctionClick.accept(juncId);
                    });
                    
                    // 2. Vẽ TRỰC TIẾP vào Pane (thay vì add vào Group)
                    junctionPane.getChildren().add(junctionShape);
                }
            }
            System.out.println("Renderer: Đã vẽ xong Junctions.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    private Shape createJunctionShape(String junctionId, SumoTraciConnection connection) {
        try {
        	SumoGeometry geometry = (SumoGeometry) connection.do_job_get(Junction.getShape(junctionId));
        	if (geometry == null || geometry.coords.isEmpty()) {
                return null; // Không có hình dáng thì bỏ qua
            }
        	// --- 2. Tạo một hình đa giác JavaFX (Polygon) ---
            Polygon junctionShape = new Polygon();
        	for (SumoPosition2D pos : geometry.coords) {
                // 1. Lấy điểm tọa độ thực (Mét)
                double realX = pos.x; 
                double realY = pos.y;

                // 2. Chuyển sang tọa độ màn hình (Pixel)
                double screenX = converter.toScreenX(realX);
                double screenY = converter.toScreenY(realY);

                // 3. Thêm điểm này vào đường gấp khúc (Polyline)
                junctionShape.getPoints().addAll(screenX, screenY);
            }
        	
        	junctionShape.setFill(Color.rgb(80, 80, 80)); // Màu xám đậm cho ngã tư
//            junctionShape.setStroke(Color.rgb(100, 100, 100)); // Viền
            junctionShape.setStrokeWidth(0.5);
            
            junctionShape.setUserData(junctionId);
            
            return junctionShape;
        } catch (Exception e) {
            return null;
        }
    }
    
    
    
 // =================================================================================
    // PHẦN VẼ ĐÈN GIAO THÔNG (TRAFFIC LIGHTS)
    // =================================================================================

    /**
     * Tạo nhóm chứa các đèn giao thông
     */
//    public Group createTrafficLightGroup(SumoTraciConnection connection, Consumer<String> onLightClick) {
//        Group tlGroup = new Group();
//
//        try {
//            // 1. Lấy danh sách tất cả các ID đèn giao thông
//            List<String> tlIds = (List<String>) connection.do_job_get(Trafficlight.getIDList());
//
//            for (String tlId : tlIds) {
//                // 2. Tạo hình cho từng đèn
//                Shape tlShape = createTrafficLightShape(tlId, connection, onLightClick);
//                
//                if (tlShape != null) {
//                    tlGroup.getChildren().add(tlShape);
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.err.println("Lỗi khi vẽ đèn giao thông: " + e.getMessage());
//        }
//
//        return tlGroup;
//    }

    /**
     * Hàm phụ trợ: Vẽ 1 cái đèn giao thông (Hình tròn)
     */
//    private Shape createTrafficLightShape(String tlId, SumoTraciConnection connection, Consumer<String> onLightClick) {
//        try {
//            // MẸO: Trong SUMO, ID của đèn giao thông thường trùng với ID của Ngã tư (Junction) nó điều khiển.
//            // Ta lấy tọa độ của Junction để đặt hình vẽ đèn giao thông.
//            SumoPosition2D pos = (SumoPosition2D) connection.do_job_get(Junction.getPosition(tlId));
//            
//            // Chuyển đổi tọa độ SUMO -> Màn hình
//            double screenX = converter.toScreenX(pos.x);
//            double screenY = converter.toScreenY(pos.y);
//
//            // Vẽ hình tròn đại diện cho đèn
//            // Bán kính = 8 (to hơn đường một chút để dễ click)
//            Circle tlCircle = new Circle(screenX, screenY, 4.0); 
//            
//            // Trang trí
//            tlCircle.setFill(Color.RED);        // Mặc định tô màu đỏ cho nổi
//            tlCircle.setStroke(Color.WHITE);    // Viền trắng
//            tlCircle.setStrokeWidth(1.0);
//            
//            // Lưu ID đèn vào (Quan trọng để sau này điều khiển)
//            tlCircle.setUserData(tlId);
//
//            // --- Hiệu ứng tương tác (Giống hệt Lane) ---
//            tlCircle.setOnMouseEntered(e -> {
//                tlCircle.setEffect(HOVER_GLOW); // Phát sáng
//                tlCircle.setCursor(Cursor.HAND);
//                tlCircle.setScaleX(1.5); // Phóng to lên chút khi di chuột vào
//                tlCircle.setScaleY(1.5);
//            });
//
//            tlCircle.setOnMouseExited(e -> {
//                tlCircle.setEffect(null);
//                tlCircle.setCursor(Cursor.DEFAULT);
//                tlCircle.setScaleX(1.0); // Trả về kích thước cũ
//                tlCircle.setScaleY(1.0);
//            });
//
//            // Xử lý Click -> Gửi ID về Controller
//            tlCircle.setOnMouseClicked(e -> {
//                if (onLightClick != null) {
//                    String clickedId = (String) tlCircle.getUserData();
//                    onLightClick.accept(clickedId);
//                }
//            });
//
//            return tlCircle;
//
//        } catch (Exception e) {
//            // Có thể đèn này không gắn với Junction nào có tọa độ cụ thể -> Bỏ qua
//            return null;
//        }
//    }
    




	public void renderVehicles(Pane vehiclePane, Map<String, Map<String, Object>> vehicleData) {
		
		// Xoá sạch xe trên 
	    vehiclePane.getChildren().clear();
	
	    if (vehicleData == null || vehicleData.isEmpty()) {
	    	System.out.println("Empty vehicle map");
	        return; // nếu không có xe nào thì thôi
	    }
	
	    for (String vehicleId : vehicleData.keySet()) {
	    	// vehicleData sẽ có key và value, thì ở đây key các bạn đặt là vehicle ID, như vậy cái keySet() đang lấy tất cả các key.
	        Map<String, Object> props = vehicleData.get(vehicleId);
	        // Ta đang tạo ra biến props như 1 cái túi chứ dữ liệu của 1 xe.
	        // Tại sao lại là Map<String, Object> ?
	        // String chính là cái key (cái ID)
	        // Còn Object là  Bởi vì trong cái túi này chứa  đủ loại dữ liệu: Position là dạng tọa độ (SumoPosition2D), Color là dạng màu (SumoColor), Speed là số (Double).
	        // Như vậy, tóm lại, Từ danh sách tổng (vehicleData), hãy lấy cho tôi bộ thông số kỹ thuật (props) của chiếc xe có biển số là vehicleId
	
	        try {
	            // --- BƯỚC 1: Đảm bảo ID có nằm trong gói props ---
	            // (Để tí nữa lấy cả cục props ra thì vẫn biết ID nó là gì)
	            props.put("vehicleId", vehicleId); 
	            // Trong vehicleData , ID xe đang nằm ở bên ngoài, làm chìa khóa để mở ngăn tủ.
	            // Trong props (Hồ sơ bên trong): Chỉ chứa Position, Color, Speed. Nó KHÔNG chứa ID bên trong.
	            // Bạn dự định lưu props vào chiếc xe hình tam giác (carShape) để sau này click vào thì lấy ra xem.
	            //Nếu bạn không thực hiện dòng code props.put("vehicleId", vehicleId), thì khi lấy hồ sơ ra, bạn sẽ không biết chiếc xe này tên là gì, vì cái nhãn tên nó nằm ở tận vòng lặp for bên ngoài và đã bị mất dấu.
	            // --- BƯỚC 2: Bóc tách dữ liệu để vẽ (Giữ nguyên như cũ) ---
	            double simX = 0;
	            double simY = 0;
	            double angle = 0;
	            Color carColor = Color.YELLOW;
	
	            // Lấy Tọa độ
	            // Ở đây chúng ta đã cẩn thận kiểm tra xem chắc chắc là trong cái props này có cái key Position không và cái Position đó chứa dữ liệu gì?
	            if (props.containsKey("Position")) {
	                Object posObj = props.get("Position"); // lấy cái dữ liệu từ cái Position đó 
	                if (posObj instanceof SumoPosition2D) { // nếu cái posObj này là dạng SumoPosition2
	                    SumoPosition2D pos = (SumoPosition2D) posObj; // ta ép kiểu posObj thành SumoPosition2
	                    simX = pos.x; // gán simX là pos.x
	                    simY = pos.y; // gán simX là pos.y
	                }
	//                System.out.println(simX + " " + simY);
	//                Thread.sleep(1000);
	            }
	
	            // kiểm tra cái kiểu của Angle và ép kiểu i như làm ở 
	            if (props.containsKey("Angle")) {
	                Object angleObj = props.get("Angle");
	                if (angleObj instanceof Number) {
	                    angle = ((Number) angleObj).doubleValue();
	                }
	            }
	
	            // Lấy màu Đỏ, Lục, Lam y nguyên của SUMO, nhưng hãy nén độ đậm đặc từ thang 255 xuống thang 1.0 cho JavaFX hiểu.
	            //this color thing is wrong
	//            if (props.containsKey("Color")) {
	//                Object colorObj = props.get("Color");
	//                if (colorObj instanceof SumoColor) {
	//                    SumoColor sc = (SumoColor) colorObj;
	//                    carColor = Color.rgb(sc.r, sc.g, sc.b, sc.a / 255.0);
	//                    //SUMO (SumoColor): Lưu trữ màu sắc theo chuẩn số nguyên từ 0 đến 255.
	//                    //Ví dụ: Đỏ=255, Xanh=0, Độ đậm đặc (Alpha)=255.
	//                    // Còn JavaFX có 3 tham số đầu (R, G, B): Chấp nhận số nguyên 0-255. (Giống SUMO).
	//                    //Nhưng tham số thứ 4 (Alpha/Opacity): Lại chỉ chấp nhận số thực từ 0.0 đến 1.0. (Khác SUMO).
	//                    // JavaFX quy định: 1.0 là đặc, 0.0 là tàng hình.
	//                    //Ta phải lấy giá trị của SUMO chia cho 255.0 để quy đổi về thang 0-1.
	//                    //Trong Java, nếu bạn viết sc.a / 255 (số nguyên chia số nguyên), kết quả sẽ bị làm tròn xuống.
	//                    //Việc thêm .0 biến nó thành phép chia số thực (double), giúp giữ lại phần thập phân (ví dụ 0.5) để hiển thị độ mờ chính xác.
	//                }
	//            }
	
	            // --- BƯỚC 3: Vẽ xe (vẽ một HÌNH TAM GIÁC CÂN hướng mũi nhọn lên trên.)
	            double screenX = converter.toScreenX(simX);
	            double screenY = converter.toScreenY(simY);
	//            System.out.println("Map: (" + simX + "," + simY + ") -> Screen: (" + screenX + "," + screenY + ")");
	
	            double size = 2;
	            //Trong JavaFX, khi bạn tạo một Polygon (Đa giác), bạn cần cung cấp các cặp tọa độ (x, y) nối tiếp nhau. Tọa độ này tính từ tâm của chiếc xe (điểm 0,0).
	            Polygon carShape = new Polygon();
	            carShape.getPoints().addAll(new Double[]{
	            		//.getPoints(): Lấy ra danh sách chứa các điểm tạo nên đa giác này (lúc đầu danh sách này rỗng).
	            //Mảng new Double[]{...} của bạn chứa 6 số, tương ứng với 3 điểm (mỗi điểm gồm x và y):
	                0.0, -size,    //y = -size: Nằm phía trên tâm (Trong JavaFX, trục Y hướng xuống dưới, nên số âm là đi lên).  
	                -size/2, size,   //x = -size/2: Lệch sang trái một nửa kích thước., y = size: Nằm phía dưới tâm.
	                size/2, size    //x = size/2: Lệch sang phải một nửa kích thước. y = size: Nằm phía dưới tâm.
	            });
	
	            carShape.setTranslateX(screenX); //Dịch chuyển" (Translate) chiếc xe từ gốc (0,0) đến đúng vị trí thực tế trên bản đồ.
	            carShape.setTranslateY(screenY);
	            carShape.setRotate(angle);
	//            carShape.setFill(carColor); temporary shut down to see yellow cars
	            if(vehicleId.contains("vehicle_")) {
	            	carShape.setFill(Color.RED);
	            }
	            else {
	            	carShape.setFill(Color.YELLOW);
	            }
	//            carShape.setStroke(Color.BLACK);
	            carShape.setStrokeWidth(1);
	
	            // --- BƯỚC 4: LƯU TOÀN BỘ INFO VÀO USERDATA (Theo ý bạn) ---
	            // Thay vì set ID, ta set cả cái Map props
	            carShape.setUserData(props); 
	
	            // --- BƯỚC 5: XỬ LÝ CLICK (Quan trọng!) ---
	            // Vì UserData giờ là Map, nên khi lấy ra phải ép kiểu về Map
	            carShape.setOnMouseClicked(e -> {
	                // Lấy lại gói hàng
	                Map<String, Object> clickedInfo = (Map<String, Object>) carShape.getUserData();
	                
	                // Lấy ID từ trong gói hàng ra
	                String clickedId = (String) clickedInfo.get("vehicleId");
	                
	                // In thử ra Console để kiểm chứng là đã lưu đủ thông tin
	                System.out.println("Bạn vừa click vào xe: " + clickedInfo); 
	                
	                // (Tạm thời) Vẫn gửi ID về Controller để điền vào ô Text
	                // Nếu sau này bạn muốn gửi cả cục data về Controller thì sửa Consumer sau
	                // Hiện tại MainController đang đợi String, nên mình gửi String
	                // Bạn cần truyền consumer vào hàm này nếu chưa có, hoặc xử lý tạm ở đây
	                System.out.println("Selected Vehicle ID: " + clickedId);
	            });
	            
	            // Hiệu ứng chuột
	            carShape.setOnMouseEntered(e -> {
	                carShape.setEffect(HOVER_GLOW);
	                carShape.setCursor(Cursor.HAND);
	            });
	            carShape.setOnMouseExited(e -> {
	                carShape.setEffect(null);
	                carShape.setCursor(Cursor.DEFAULT);
	            });
	
	            vehiclePane.getChildren().add(carShape);
	//            System.out.println("Added vehicle: " + vehicleId);
	
	        } catch (Exception e) {
	        	System.err.println("CRASHED while rendering car: " + vehicleId);
	            e.printStackTrace(); 
	            continue;
	        }
	    }
	    
		}
	//Khang
	public void renderTrafficLights(Pane trafficLightPane, Map<TrafficlightObject,Character>trafficLightsData) {
		
		trafficLightPane.getChildren().clear();
	
	    if (trafficLightsData == null || trafficLightsData.isEmpty()) {
	    		System.out.println("Empty traffic light map");
	        return;
	    }
	
	    for (TrafficlightObject tl_link : trafficLightsData.keySet()) {
	    		Character tl_color_char = trafficLightsData.get(tl_link);
	    		try {
	    			SumoPosition2D tmp_pos = tl_link.get_position();
	            double simX = tmp_pos.x;
	            double simY = tmp_pos.y;
	            double screenX = this.converter.toScreenX(simX);
	            double screenY = this.converter.toScreenY(simY);
	            Group lightGroup = new Group();
	
	            // Housing
	            Rectangle box = new Rectangle(-0.75, -2.125, 1.5, 4.25); 
	            box.setArcWidth(0.75);  
	            box.setArcHeight(0.75);  
	            box.setFill(Color.rgb(30, 30, 30)); // keep color
	            box.setStroke(Color.BLACK);
	
	            // Lamps
	            Character red_lamp = 'a';
	            Character yellow_lamp = 'b';
	            Character green_lamp = 'c';
	            if(tl_color_char == 'R' || tl_color_char == 'r') {
	                red_lamp = tl_color_char;
	            }
	            else if(tl_color_char == 'G' || tl_color_char == 'g') {
	                green_lamp = tl_color_char;
	            }
	            else if(tl_color_char == 'Y' || tl_color_char == 'y') {
	                yellow_lamp = tl_color_char;
	            }
	            else {
	                yellow_lamp = tl_color_char; // 'o' or 'O'
	            }
	
	            // Circles (lamps)
	            Circle redLamp = new Circle(0, -1.125, 0.5, this.tl_color_map.get(red_lamp));
	            Circle yellowLamp = new Circle(0, 0, 0.5, this.tl_color_map.get(yellow_lamp));
	            Circle greenLamp = new Circle(0, 1.125, 0.5, this.tl_color_map.get(green_lamp));
	
	            redLamp.setId("red");
	            yellowLamp.setId("yellow");
	            greenLamp.setId("green");
	
	            lightGroup.getChildren().addAll(box, redLamp, yellowLamp, greenLamp);
	
	            lightGroup.setTranslateX(screenX);
	            lightGroup.setTranslateY(screenY);
	            lightGroup.setUserData(tl_color_char);
	
	
	//            // --- BƯỚC 5: XỬ LÝ CLICK (Quan trọng!) ---
	//            // Vì UserData giờ là Map, nên khi lấy ra phải ép kiểu về Map
	//            carShape.setOnMouseClicked(e -> {
	//                // Lấy lại gói hàng
	//                Map<String, Object> clickedInfo = (Map<String, Object>) carShape.getUserData();
	//                
	//                // Lấy ID từ trong gói hàng ra
	//                String clickedId = (String) clickedInfo.get("vehicleId");
	//                
	//                // In thử ra Console để kiểm chứng là đã lưu đủ thông tin
	//                System.out.println("Bạn vừa click vào xe: " + clickedInfo); 
	//                
	//                // (Tạm thời) Vẫn gửi ID về Controller để điền vào ô Text
	//                // Nếu sau này bạn muốn gửi cả cục data về Controller thì sửa Consumer sau
	//                // Hiện tại MainController đang đợi String, nên mình gửi String
	//                // Bạn cần truyền consumer vào hàm này nếu chưa có, hoặc xử lý tạm ở đây
	//                System.out.println("Selected Vehicle ID: " + clickedId);
	//            });
	//            
	//            // Hiệu ứng chuột
	//            carShape.setOnMouseEntered(e -> {
	//                carShape.setEffect(HOVER_GLOW);
	//                carShape.setCursor(Cursor.HAND);
	//            });
	//            carShape.setOnMouseExited(e -> {
	//                carShape.setEffect(null);
	//                carShape.setCursor(Cursor.DEFAULT);
	//            });
	
	            trafficLightPane.getChildren().add(lightGroup);
//	            System.out.println("Added traffic light: " + tl_link.get_link_index());
	
	        } catch (Exception e) {
	        	System.err.println("CRASHED while rendering traffic light: " + tl_link.get_link_index());
	            e.printStackTrace(); 
	            continue;
	        }
	    }
	}
}