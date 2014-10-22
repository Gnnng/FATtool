public class FATmain {
	public static void main(String[] args) throws Exception {
		Disk disk = new Disk("C:\\v2.vhd", "r");
		Disk.Partition pr = disk.partition1;
		pr.buildTree();
		pr.printTree();
		Disk.Partition.Xbel xbel = pr.new Xbel();
		xbel.writeFile("result.xbel");
		if (args.length > 0) {
			try {
				switch (args[0]) {
				case "cp": {
					 
				}
				case "rm": {

				}
				case "touch": {

				}
				case "mkdir": {

				}
				default:
					// do nothing
					throw new Exception("Unknown command");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
