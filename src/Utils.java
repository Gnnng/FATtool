public class Utils {
	public static long combineSigned(byte...bs ) {
		long result = 0;
		for(int i = 0; i < bs.length; i++) {
			result |=  (((long)bs[i]) & 0xff) << (i * 8);   
		}
		return result;
	}
}