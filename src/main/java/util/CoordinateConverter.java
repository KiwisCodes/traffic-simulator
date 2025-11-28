package util;

import javafx.geometry.Point2D;
import model.infrastructure.MapManger;
import view.MainGUI;
import de.tudresden.ws.container.SumoPosition2D; 


public class CoordinateConverter {

    // --- Static World Data (From SumoMap) ---
    private double mapMinX;
    private double mapMaxY; // The "Ceiling" of the map
    private double mapWidth;
    private double mapHeight;

    // --- Dynamic View Data (Zoom & Pan) ---
    private double scale = 1;     // Pixels per Meter
    private double offsetX = 0.0;   // Panning X
    private double offsetY = 0.0;   // Panning Y
    private final double padding = 50.0; // Empty space around map edges
    
    private int windowWidth = MainGUI.windowWidth;
    private int windowHeight = MainGUI.windowHeight;
    

    // Constructor: Locks onto a specific Map
    public CoordinateConverter() {
    	
    }
    
    
    public void setBound(MapManger map) {
        this.mapMinX = map.getMinX();
        this.mapMaxY = map.getMaxY();
        this.mapWidth = map.getWidth();
        this.mapHeight = map.getHeight();
    }
    

    // ---------------------------------------------------------
    // 1. World (SUMO) -> Screen (JavaFX)
    // ---------------------------------------------------------

    public double toScreenX(double sumoX) {
        // (WorldPos - WorldOrigin) * Zoom + PanOffset
        return (sumoX - mapMinX) * scale + offsetX + padding;
    }

    public double toScreenY(double sumoY) {
        // (WorldCeiling - WorldPos) * Zoom + PanOffset
        // This handles the Flip automatically
        return (mapMaxY - sumoY) * scale + offsetY + padding;
    }

    // Helper for Point objects
    public Point2D toScreen(SumoPosition2D sumoPoint) {
        return new Point2D(toScreenX(sumoPoint.x), toScreenY(sumoPoint.y));
    }

    // ---------------------------------------------------------
    // 2. Screen (JavaFX) -> World (SUMO)
    // Useful for: Clicking on a car with the mouse
    // ---------------------------------------------------------

    public double toSumoX(double screenX) {
        return ((screenX - padding - offsetX) / scale) + mapMinX;
    }

    public double toSumoY(double screenY) {
        return mapMaxY - ((screenY - padding - offsetY) / scale);
    }

    // ---------------------------------------------------------
    // 3. View Management (Zooming & Panning)
    // ---------------------------------------------------------

    /**
     * Automatically calculates the scale needed to fit the map 
     * inside the given window size.
     */
    
    //does not support default arguments
 // Update in util/CoordinateConverter.java

 // Add this method to dynamically set the canvas size
    public void autoFit(double paneWidth, double paneHeight) {
    	
    	// --- BỔ SUNG ĐOẠN NÀY ---
        // Nếu màn hình chưa kịp load (size = 0), hãy giả định một kích thước mặc định (ví dụ 800x600)
        if (paneWidth <= 0) paneWidth = 1000; // Đặt chiều rộng mặc định là 1000 nếu bị đưa cho paneWidth <=0)
        if (paneHeight <= 0) paneHeight = 800; // Đặt chiều dài mặc định là 1000 nếu bị đưa cho paneWidth <=0)
        // ------------------------
        
	     // 1. Calculate the width/height of the SUMO map
	     double mapW = this.mapWidth;
	     double mapH = this.mapHeight;
	
	  // Tránh chia cho 0 nếu map lỗi
	     if (mapW == 0 || mapH == 0) {
	         this.scale = 1; 
	         return;
	     }
	     
	     
	     // 2. Determine the scales required to fit width and height
	     // We subtract padding * 2 to leave room on edges
	     double scaleX = (paneWidth - (padding * 2)) / mapW;
	     double scaleY = (paneHeight - (padding * 2)) / mapH;
	
	     // 3. Choose the smaller scale (so the whole map fits)
	     this.scale = Math.min(scaleX, scaleY);
	     
	  // In ra để kiểm tra
	     System.out.println("DEBUG: PaneSize=" + paneWidth + "x" + paneHeight);
	     System.out.println("DEBUG: MapSize=" + mapW + "x" + mapH);
	     System.out.println("DEBUG: TÍNH TOÁN SCALE = " + this.scale); // <--- NẾU CÁI NÀY = 0 LÀ CHẾT
	
	     // 4. Center the map
	     // Calculate how much space is left empty
	     //nó tính ra kích thước thực tế của bản đồ sau khi thu nhỏ
	     double usedWidth = mapW * scale;
	     double usedHeight = mapH * scale;
	
	     // ĐOẠN NÀY GÂY LỖI LỆCH MAP. Việc tính toán này nhằm mục đích căn giữa bản đồ thủ công. Nhưng bạn đang dùng StackPane trong JavaFX.
	     // StackPane sinh ra là để tự động căn giữa mọi thứ.
	     // Nếu bạn tự tính thêm offsetX ở đây, bạn đang đẩy bản đồ lệch sang phải một đoạn.
	     // Sau đó StackPane lại căn giữa cái bản đồ "đã bị lệch" đó. => Kết quả: Bản đồ bị trôi khỏi màn hình.
	     // Center offset
//	     this.offsetX = (paneWidth - usedWidth) / 2;
//	     this.offsetY = (paneHeight - usedHeight) / 2;
	     
	     //Như vậy, / Không cần tính usedWidth, usedHeight làm gì nữa.
	        // Đặt Offset bằng 0 để map nằm sát góc (0,0) chuẩn.
	        // StackPane bên ngoài sẽ lo việc bưng nó ra giữa.
	     
	     this.offsetX = 0;
	     this.offsetY = 0;
	     
	     //=> Vậy Offset chính là khoảng cách bạn dùng để "đẩy" hình vẽ ra xa khỏi vị trí gốc ban đầu.
	     //Nếu Offset = 0: Bạn dán bức tranh sát sạt vào góc trên cùng bên trái của khung.
	     //Nếu Offset X = 100, Offset Y = 50: Bạn cầm bức tranh, dịch nó sang phải 100 bước và dịch xuống dưới 50 bước.
	     
	     System.out.println("Map Scaled to: " + this.scale);
    }	

    public void zoom(double factor) {
        this.scale *= factor;
    }
    
    

    public void setPan(double x, double y) {
        this.offsetX = x;
        this.offsetY = y;
    }
    
    public double getScale() { return scale; }
}