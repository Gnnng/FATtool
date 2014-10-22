
public class FATmain {
	public static void main(String[] args) throws Exception {
		Disk disk = new Disk("C:\\v2.vhd", "r");
		Disk.Partition pr = disk.partition1;
		pr.buildTree();
		pr.printTree();
	}
}