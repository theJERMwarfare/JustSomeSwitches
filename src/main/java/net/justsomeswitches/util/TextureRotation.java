package net.justsomeswitches.util;

/**
 * Enumeration for texture rotation options.
 * Provides player-controlled texture rotation independent of lever physical orientation.
 */
public enum TextureRotation {
    NORMAL("Normal", 0),
    RIGHT("Right", 90), 
    LEFT("Left", -90),
    INVERT("Invert", 180);
    
    private final String displayName;
    private final int degrees;
    
    TextureRotation(String displayName, int degrees) {
        this.displayName = displayName;
        this.degrees = degrees;
    }
    
    public String getDisplayName() {
        return switch (this) {
            case NORMAL -> "0°";
            case RIGHT -> "90°"; 
            case LEFT -> "-90°";
            case INVERT -> "180°";
        };
    }
    
    public int getDegrees() {
        return degrees;
    }
    
    /**
     * Gets rotation as radians for mathematical operations.
     */
    public double getRadians() {
        return Math.toRadians(degrees);
    }
    
    /**
     * Applies rotation to UV coordinates using rotation matrix.
     * 
     * @param u original U coordinate (0.0 to 1.0)
     * @param v original V coordinate (0.0 to 1.0) 
     * @return rotated UV coordinates as [u, v]
     */
    public float[] rotateUV(float u, float v) {
        // Center coordinates around (0.5, 0.5) for rotation
        float centeredU = u - 0.5f;
        float centeredV = v - 0.5f;
        
        float rotatedU, rotatedV;
        
        switch (this) {
            case NORMAL:
                rotatedU = centeredU;
                rotatedV = centeredV;
                break;
            case RIGHT: // 90° rotation
                rotatedU = -centeredV;
                rotatedV = centeredU;
                break;
            case LEFT: // -90° rotation  
                rotatedU = centeredV;
                rotatedV = -centeredU;
                break;
            case INVERT: // 180° rotation
                rotatedU = -centeredU;
                rotatedV = -centeredV;
                break;
            default:
                rotatedU = centeredU;
                rotatedV = centeredV;
                break;
        }
        
        // Convert back to texture space (0.0 to 1.0)
        return new float[]{rotatedU + 0.5f, rotatedV + 0.5f};
    }
}
