package util;

public class ConversionUtil {
    public static byte[] integerToByteConverter(int integerValue) {
        byte[] byteArray = new byte[4];
        for (int index = 0; index < 4; index++) {
            int offset = (byteArray.length - 1 - index) * 8;
            byteArray[index] = (byte) ((integerValue >>> offset) & 0xFF);
        }
        return byteArray;
    }

    public static int byteArrayToIntegerConverter(byte[] byteArray, int offset) {
        int integerValue = 0;
        for (int index = 0; index < 4; index++) {
            int shift = (4 - 1 - index) * 8;
            integerValue += (byteArray[index + offset] & 0x000000FF) << shift;
        }
        return integerValue;
    }

    public static int byteArrayToIntegerConverter(byte[] b) {
        return byteArrayToIntegerConverter(b, 0);
    }
}
