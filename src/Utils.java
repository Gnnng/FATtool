public class Utils {
	public static int combineSigned(byte...bs ) {
		int result = 0;
		for(int i = 0; i < bs.length; i++) {
			result += bs[i] << (i * 8); 
		}
		return result;
	}
}