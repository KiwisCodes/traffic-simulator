//package view;
//
//import java.util.List;
//import java.util.function.Consumer; // [NEW] Needed for the callback
//
//import de.tudresden.sumo.cmd.Edge;
//import de.tudresden.sumo.cmd.Lane;
//import de.tudresden.sumo.objects.SumoGeometry;
//import de.tudresden.sumo.objects.SumoPosition2D;
//import it.polito.appeal.traci.SumoTraciConnection;
//import javafx.scene.Cursor; // [NEW] Change cursor to hand
//import javafx.scene.Group;
//import javafx.scene.effect.BlurType; // [NEW] For Glow
//import javafx.scene.effect.DropShadow; // [NEW] For Glow
//import javafx.scene.paint.Color;
//import javafx.scene.shape.Circle;
//import javafx.scene.shape.Polyline;
//import javafx.scene.shape.Rectangle;
//import javafx.scene.shape.Shape;
//import javafx.scene.shape.StrokeLineCap;
//import javafx.scene.shape.StrokeLineJoin;
//import model.infrastructure.MapManager;
//import model.vehicles.*;
//import util.CoordinateConverter;
//
//// Helper class to handle the creation of JavaFX shapes, groups
//public class Renderer {
//    private CoordinateConverter converter;
//    
//    // [NEW] Define the glow effect once to save memory
//    private static final DropShadow HOVER_GLOW = new DropShadow();
//
//    public Renderer(){
//        this.converter = new CoordinateConverter();
//        
//        // [NEW] Initialize the Glow Styling
//        HOVER_GLOW.setColor(Color.CYAN);
//        HOVER_GLOW.setRadius(15); 
//        HOVER_GLOW.setSpread(0.6);
//        HOVER_GLOW.setBlurType(BlurType.GAUSSIAN);
//    }
//    
//    public void setConverter(MapManager sumoMap) {
//        this.converter.setBound(sumoMap);
//    }
//    
//    public CoordinateConverter getConverter() {
//        return this.converter;
//    }
//
//    //implement later
////    public Shape createVehicleShape(SumoTraciConnection sumoConnection, Vehicle vehicle) {
////        if (vehicle instanceof Car) {
////            return new Circle(4, Color.BLUE);
////        } else if (vehicle instanceof Bus) {
////            return new Rectangle(12, 5, Color.RED);
////        } else if (vehicle instanceof Truck) {
////            return new Rectangle(15, 6, Color.ORANGE);
////        } else if (vehicle instanceof Bike) {
////            return new Circle(2, Color.GREEN);
////        }
////        // Default
////        return new Circle(3, Color.GRAY);
////    }
//    
//    /**
//     * [UPDATED] Now accepts a 'Consumer' callback to handle clicks
//     */
//    public Group createLaneGroup(SumoTraciConnection sumoConnection, MapManager sumoMap, Consumer<String> onLaneClick) {
//        List<String> edges =  sumoMap.getEdgeIds();
//        Group laneGroup =  new Group();
//        
//        try {
//            for(String edge : edges) {
//                int numberOfLanes = (int) sumoConnection.do_job_get(Edge.getLaneNumber(edge));
//                
//                for(int i = 0; i < numberOfLanes; i++) {
//                    Polyline singleLaneShape = new Polyline();
//                    String laneId = edge + "_" + i;
//                    
//                    // [NEW] 1. Store the ID so we can retrieve it on click
//                    singleLaneShape.setUserData(laneId);
//                    
//                    SumoGeometry geometry = (SumoGeometry) sumoConnection.do_job_get(Lane.getShape(laneId));
//                    double laneWidth = (double) sumoConnection.do_job_get(Lane.getWidth(laneId));
//                    
//                    for(SumoPosition2D point : geometry.coords) {
//                        double xScreen = this.converter.toScreenX(point.x);
//                        double yScreen = this.converter.toScreenY(point.y);
//                        singleLaneShape.getPoints().addAll(xScreen, yScreen);
//                    }
//                    
//                    // [NEW] 2. Improved Styling
//                    Color asphaltColor = Color.rgb(50, 50, 50);
//                    singleLaneShape.setStroke(asphaltColor);
//                    singleLaneShape.setFill(null); // Important: Remove fill to avoid weird artifacts
//                    
//                    // Ensure line is at least 3px wide so it is clickable, even if scaled down
//                    double visualWidth = converter.getScale() * laneWidth;
//                    singleLaneShape.setStrokeWidth(Math.max(3.0, visualWidth));
//                    
//                    singleLaneShape.setStrokeLineJoin(StrokeLineJoin.ROUND);
//                    singleLaneShape.setStrokeLineCap(StrokeLineCap.ROUND);
//                    singleLaneShape.setSmooth(true);
//                    
//                    // [NEW] 3. Add Interaction Listeners
//                    
//                    // HOVER ENTER
//                    singleLaneShape.setOnMouseEntered(e -> {
//                        singleLaneShape.setEffect(HOVER_GLOW);       // Turn on glow
//                        singleLaneShape.setStroke(Color.LIGHTGRAY);  // Lighten the road
//                        singleLaneShape.setCursor(Cursor.HAND);      // Show hand cursor
//                        singleLaneShape.toFront(); // Optional: Bring hovered road to top
//                    });
//
//                    // HOVER EXIT
//                    singleLaneShape.setOnMouseExited(e -> {
//                        singleLaneShape.setEffect(null);             // Turn off glow
//                        singleLaneShape.setStroke(asphaltColor);     // Reset color
//                        singleLaneShape.setCursor(Cursor.DEFAULT);
//                    });
//
//                    // CLICK
//                    singleLaneShape.setOnMouseClicked(e -> {
//                        // Check if a handler was provided
//                        if (onLaneClick != null) {
//                            String clickedId = (String) singleLaneShape.getUserData();
//                            onLaneClick.accept(clickedId);
//                        }
//                    });
//                    
//                    laneGroup.getChildren().add(singleLaneShape);
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        
//        return laneGroup;
//    }
//    
////implement later
//    
////    public Shape createJunctionShape(SumoTraciConnection sumoConnection, SumoMap sumoMap) {
////      // ... (kept as is) ...
////    }
//}




package view;

// --- Java Util ---
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer; // Để xử lý click chuột

// --- JavaFX (Giao diện) ---
import javafx.scene.Group;
import javafx.scene.Cursor;
import javafx.scene.effect.DropShadow; // Hiệu ứng phát sáng
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;      // Vẽ ngã tư
import javafx.scene.shape.Polyline;    // Vẽ đường gấp khúc
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap; // Bo tròn đầu đường

// --- TraaS / SUMO (Thư viện mô phỏng) ---
import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Lane;           // Lệnh lấy thông tin Lane
import de.tudresden.sumo.cmd.Junction;       // Lệnh lấy thông tin Junction
import de.tudresden.sumo.objects.SumoGeometry;   // Chứa danh sách tọa độ hình dáng
import de.tudresden.sumo.objects.SumoPosition2D; // Tọa độ X, Y lẻ
import model.infrastructure.MapManager;
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

    // 2. KHỞI TẠO (Trong Constructor)
    public Renderer() {
        // Tạo mới đối tượng converter khi Renderer được sinh ra
        this.converter = new CoordinateConverter(); 
        
        // Các cài đặt khác (Glow effect...)
        HOVER_GLOW.setColor(Color.CYAN);
        HOVER_GLOW.setRadius(10);
        HOVER_GLOW.setSpread(0.6);
    }

 // Trong Renderer.java

    /**
     * Vẽ toàn bộ lớp tĩnh: Đường, Ngã tư.
     * @param mapManager: chứa thông tin ID và biên (bounds)
     * @param connection: dùng để lấy hình dáng (Shape) chi tiết từ SUMO
     */
    public Group createLaneGroup(MapManager mapManager, SumoTraciConnection connection,Consumer<String> onLaneClick) {
        Group staticGroup = new Group();
        
        //Thứ tự đúng: setBound (biết map to bao nhiêu) -> autoFit (tính tỷ lệ thu nhỏ) -> Vòng lặp vẽ (dùng tỷ lệ đó để vẽ).

        // BƯỚC 1: Cài đặt biên cho Converter để tính toán tỉ lệ Zoom/Pan
        // (Rất quan trọng: nếu không làm bước này, bản đồ có thể bị lệch hoặc sai tỉ lệ)
        
        if (mapManager != null) {
            // 1. Nạp dữ liệu biên bản đồ (BẮT BUỘC có trước)
            this.converter.setBound(mapManager);

            // 2. [QUAN TRỌNG] Tính toán tỷ lệ NGAY TẠI ĐÂY
            // Dùng kích thước cửa sổ trừ hao đi một chút (vì có thanh menu bên trái)
            // Ví dụ: Width lấy 75% cửa sổ, Height lấy 90% cửa sổ
            double mapDisplayWidth = view.MainGUI.windowWidth * 0.6; 
            double mapDisplayHeight = view.MainGUI.windowHeight * 0.80;
            
//            this.converter.autoFit(mapDisplayWidth, mapDisplayHeight);
        }

        try {
            // BƯỚC 2: Vẽ các Làn đường (Lanes)
            // Chúng ta lấy danh sách Lane ID từ MapManager (hoặc trực tiếp từ Connection)
            // Lưu ý: MapManger của bạn đang return edgeIds trong hàm getLaneIds(), hãy cẩn thận check lại nhé.
            // Ở đây mình ví dụ lấy trực tiếp từ TraCI cho chắc ăn:
            List<String> laneIds = (List<String>) connection.do_job_get(Lane.getIDList());

            for (String laneId : laneIds) {            
                // Gọi hàm phụ trợ để tạo hình cho từng Lane, ngoài ra truyền thêm cái hành động click vào
                Shape laneShape = createLaneShape(laneId, connection, onLaneClick); //(Helper Method call)
                // Hàm laneShape sẽ lấy ID và connection đến Sumo để hỏi Sumo là hình dáng của cái vật mang ID này là gì để trả về.
                //Như vậy cụ thể lúc này biến laneShape sẽ nhận một cái Polyline - đường gấp khúc
         
                if (laneShape != null) { //Null Check
                    staticGroup.getChildren().add(laneShape); 
                   //Nó thuộc về lớp javafx.scene.Parent (Lớp cha của Group, Pane, VBox...). 
                   //Vì biến staticGroup của bạn là một đối tượng kiểu Group (mà Group là con của Parent), nên nó được thừa hưởng và sử dụng hàm này miễn phí.
                   // một cái Group (Nhóm) không trực tiếp chứa các hình vẽ. Thay vào đó, nó quản lý một Danh sách nội bộ (gọi là ObservableList).
                   //Hàm getChildren() là "cánh cửa" bắt buộc phải đi qua nếu bạn muốn thêm, xóa, hoặc đếm số lượng các hình vẽ đang nằm trong một Group. Nếu không gọi nó, bạn không thể thay đổi nội dung bên trong Group đó.
                }
            }
            // BƯỚC 3: Vẽ Ngã tư (Junctions)
            List<String> junctionIds = mapManager.getJunctionIds();
            for (String juncId : junctionIds) {
                 Shape junctionShape = createJunctionShape(juncId, connection);
                 if (junctionShape != null) {
                     staticGroup.getChildren().add(junctionShape);
                 }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Lỗi khi render static map: " + e.getMessage());
        }
        
     

        return staticGroup;
    }
    private Shape createLaneShape(String laneId, SumoTraciConnection connection,Consumer<String> onLaneClick) {
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
            lanePolyline.setStroke(Color.GRAY); // Màu đường nhựa
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
                lanePolyline.setStroke(Color.GRAY);
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
    private Shape createJunctionShape(String junctionId, SumoTraciConnection connection) {
        try {
            // Lấy tọa độ tâm ngã tư
            SumoPosition2D pos = (SumoPosition2D) connection.do_job_get(Junction.getPosition(junctionId));
            
            double screenX = converter.toScreenX(pos.x);
            double screenY = converter.toScreenY(pos.y);

            // Vẽ một hình tròn nhỏ đại diện ngã tư
            Circle junction = new Circle(screenX, screenY, 3.0); // Bán kính 3
            junction.setFill(Color.DARKGRAY);
            junction.setUserData(junctionId);
            
            return junction;
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
    public Group createTrafficLightGroup(SumoTraciConnection connection, Consumer<String> onLightClick) {
        Group tlGroup = new Group();

        try {
            // 1. Lấy danh sách tất cả các ID đèn giao thông
            List<String> tlIds = (List<String>) connection.do_job_get(Trafficlight.getIDList());

            for (String tlId : tlIds) {
                // 2. Tạo hình cho từng đèn
                Shape tlShape = createTrafficLightShape(tlId, connection, onLightClick);
                
                if (tlShape != null) {
                    tlGroup.getChildren().add(tlShape);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Lỗi khi vẽ đèn giao thông: " + e.getMessage());
        }

        return tlGroup;
    }

    /**
     * Hàm phụ trợ: Vẽ 1 cái đèn giao thông (Hình tròn)
     */
    private Shape createTrafficLightShape(String tlId, SumoTraciConnection connection, Consumer<String> onLightClick) {
        try {
            // MẸO: Trong SUMO, ID của đèn giao thông thường trùng với ID của Ngã tư (Junction) nó điều khiển.
            // Ta lấy tọa độ của Junction để đặt hình vẽ đèn giao thông.
            SumoPosition2D pos = (SumoPosition2D) connection.do_job_get(Junction.getPosition(tlId));
            
            // Chuyển đổi tọa độ SUMO -> Màn hình
            double screenX = converter.toScreenX(pos.x);
            double screenY = converter.toScreenY(pos.y);

            // Vẽ hình tròn đại diện cho đèn
            // Bán kính = 8 (to hơn đường một chút để dễ click)
            Circle tlCircle = new Circle(screenX, screenY, 4.0); 
            
            // Trang trí
            tlCircle.setFill(Color.RED);        // Mặc định tô màu đỏ cho nổi
            tlCircle.setStroke(Color.WHITE);    // Viền trắng
            tlCircle.setStrokeWidth(1.0);
            
            // Lưu ID đèn vào (Quan trọng để sau này điều khiển)
            tlCircle.setUserData(tlId);

            // --- Hiệu ứng tương tác (Giống hệt Lane) ---
            tlCircle.setOnMouseEntered(e -> {
                tlCircle.setEffect(HOVER_GLOW); // Phát sáng
                tlCircle.setCursor(Cursor.HAND);
                tlCircle.setScaleX(1.5); // Phóng to lên chút khi di chuột vào
                tlCircle.setScaleY(1.5);
            });

            tlCircle.setOnMouseExited(e -> {
                tlCircle.setEffect(null);
                tlCircle.setCursor(Cursor.DEFAULT);
                tlCircle.setScaleX(1.0); // Trả về kích thước cũ
                tlCircle.setScaleY(1.0);
            });

            // Xử lý Click -> Gửi ID về Controller
            tlCircle.setOnMouseClicked(e -> {
                if (onLightClick != null) {
                    String clickedId = (String) tlCircle.getUserData();
                    onLightClick.accept(clickedId);
                }
            });

            return tlCircle;

        } catch (Exception e) {
            // Có thể đèn này không gắn với Junction nào có tọa độ cụ thể -> Bỏ qua
            return null;
        }
    }



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
            carShape.setFill(Color.YELLOW);
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
            System.out.println("Added vehicle: " + vehicleId);

        } catch (Exception e) {
        	System.err.println("CRASHED while rendering car: " + vehicleId);
            e.printStackTrace(); 
            continue;
        }
    }
}}