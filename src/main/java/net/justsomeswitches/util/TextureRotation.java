package net.justsomeswitches.util;

/** Texture rotation options for player-controlled texture orientation. */
public enum TextureRotation {
    NORMAL(0),
    RIGHT(90), 
    LEFT(-90),
    INVERT(180);
    
    private final int degrees;
    
    TextureRotation(int degrees) {
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
    
    /** Applies rotation to UV coordinates using rotation matrix. */
    public float[] rotateUV(float u, float v) {
        float centeredU = u - 0.5f;
        float centeredV = v - 0.5f;
        
        float[] result = switch (this) {
            case NORMAL -> new float[]{centeredU, centeredV};
            case RIGHT -> new float[]{centeredV, -centeredU};
            case LEFT -> new float[]{-centeredV, centeredU};
            case INVERT -> new float[]{-centeredU, -centeredV};
        };
        
        return new float[]{result[0] + 0.5f, result[1] + 0.5f};
    }
}
