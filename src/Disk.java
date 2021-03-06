import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
 





import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

public class Disk extends RandomAccessFile {
	Partition partition1, partition2, partition3, partition4;
	
	public Disk(String filepath, String mode) throws IOException {
		super(filepath, mode);
		partition1 = new Partition(this.readByteArray(446, 16));
	}
	
	public byte[] readByteArray(long start, int len) {
		byte[] byteArray = new byte[len];
		try {
			this.seek(start);
			this.read(byteArray);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return byteArray;
	}
	
	public byte[] getMBR() {
		byte[] mbr = new byte[512];
		try {
			this.seek(0);
			this.read(mbr);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return mbr;
	}
	
	// inner classes
	public class Partition {
		byte[] data;
		int typeCode;
		long maxRootDirectoryEntries;
		byte[] br;
		String oemName, volumeName, fatName;
		long lbaBegin, fatBegin, rootDirectoryBegin, dataBegin;
		long lbaBeginAddress, fatBeginAddress, rootDirectoryBeginAddress, dataBeginAddress;
		long size;
		BaseNode fileTree;
		
		public Partition(byte[] data) {
			typeCode = data[4];
			lbaBegin = Utils.combineSigned(data[8], data[9], data[10], data[11]);
			lbaBeginAddress = lbaBegin * 512;
			byte[] br = Disk.this.readByteArray(lbaBeginAddress, 512);
			oemName = new String(br, 3, 8);
			volumeName = new String(br, 0x2b, 11);		
			fatName = new String(br, 0x36, 8);
			
			long reservedSectors = Utils.combineSigned(br[14], br[15]);
			long numberOfFAT = Utils.combineSigned(br[16]);
			maxRootDirectoryEntries = Utils.combineSigned(br[0x11], br[0x12]);
			long numberOfSectorsInPartition = Utils.combineSigned(br[0x13], br[0x14]);
			long sectorsPerFAT = Utils.combineSigned(br[0x16], br[0x17]);
			
			size = numberOfSectorsInPartition * 512;
			
			fatBegin = lbaBegin + reservedSectors;
			fatBeginAddress = fatBegin * 512; 
			
			rootDirectoryBegin = lbaBegin + reservedSectors + numberOfFAT * sectorsPerFAT;
			rootDirectoryBeginAddress = rootDirectoryBegin * 512;
			
			dataBegin = rootDirectoryBegin + maxRootDirectoryEntries * 32 / 512;
			dataBeginAddress = dataBegin * 512;
//			System.out.println("Fat begins at " + fatBeginAddress);
//			System.out.println("OEM name is \"" + oemName + "\"");
//			System.out.println("Fat name is " + fatName);
//			System.out.println("reserved sectors " + reservedSectors);
//			System.out.println("number of FAT " + numberOfFAT);
//			System.out.println("sectors per FAT " + sectorsPerFAT);
//			System.out.println("Root dir " + Integer.toHexString((int)rootDirectoryBeginAddress));
//			System.out.println("Root dir " + rootDirectoryBeginAddress);

		}
		public void buildTree() {
			fileTree = new BaseNode();
			for(int i = 0; i < maxRootDirectoryEntries; i++) {
				RootDirectoryEntry entry = new RootDirectoryEntry(rootDirectoryBeginAddress + i * 32);
				if (entry.getFirstByte() == 0)
					break;
				if (entry.isValid()) 
					fileTree.add(entry);
			}
			for(BaseNode i: fileTree.subList) {
//				System.out.println(i.getName());
				i.dfs(0);
			}
		}

		public void printTree() {
			System.out.println("In Partition 1");
			for(BaseNode i: fileTree.subList) {
				System.out.println("\\---" + i.getName());
				i.print("----");
			}
//			for(BaseNode i: fileTree.subList) {
//				i.print("");
//			}
		}
		
		public class Xbel {
			Document doc;
			public Xbel() {
				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = null;
				try {
					docBuilder = docFactory.newDocumentBuilder();
				} catch (ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				doc = docBuilder.newDocument();
				Element root = doc.createElement("xbel");
				root.setAttribute("version", "1.0");
				
				for(BaseNode i: fileTree.subList) {
					root.appendChild(buildNode(i));
				}
				
				doc.appendChild(root);
			}
			
			public Element buildNode(BaseNode bn) {
				Element node = createNode(bn);
				for(BaseNode i: bn.subList) {
					if (!i.first.isDotDirectory()) {
						node.appendChild(buildNode(i));
					}
				}
				return node;
			}
			
			public void writeFile(String fileName) {
				TransformerFactory transformerFactory = TransformerFactory
						.newInstance();
				Transformer transformer = null;
				try {
					transformer = transformerFactory.newTransformer();
				} catch (TransformerConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(new File(
						fileName));
				try {
					transformer.transform(source, result);
				} catch (TransformerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			public Element createNode(BaseNode bn) {
				Element node = null;
				if (bn.first.isDirectory()) {
					node = doc.createElement("folder");
					node.setAttribute("folded", "yes");
				} else {
					node = doc.createElement("bookmark");
					node.setAttribute("href", Long.toString(bn.first.getSize()));
				}
				Element nodeTitle = doc.createElement("title");
				nodeTitle.setTextContent(bn.getName());
				node.appendChild(nodeTitle);
				return node;
			}
		}		
		
		public class BaseNode {
			RootDirectoryEntry first;
			LinkedList<BaseNode> subList;
			
			public BaseNode(RootDirectoryEntry f) {
				this();
				first = f;
			}
			
			public BaseNode() {
				first = null;
				subList = new LinkedList<BaseNode>();
			}
			
			public long getAttribute() {
				return first.getAttribute();
			}
			
			public String getName() {
				return first.getFullName();
			}
			
			public void add(RootDirectoryEntry entry) {
				if (entry.isDirectory()) {
					if (!entry.isDotDirectory())
						subList.add(new DirNode(entry));
				} else
					subList.add(new FileNode(entry));
			}
			
			public boolean isFile() {
				return !first.isDirectory();
			}

			public boolean isDirectory() {
				return first.isDirectory();
			}

			public void dfs(int depth) {
				if (!first.isDirectory() || first.isDotDirectory())
					return;
//				System.out.println("Search " + getName());
//				System.out.println("Cluster number " + first.getCluster());
				Cluster cluster = new Cluster(first.getCluster());
				while(true) {
//					System.out.println("Searching ");
					long clusterBeginAddress = dataBeginAddress + (cluster.number - 2) * 512;
					for(int i = 0; i < 512 / 32; i++) {
						RootDirectoryEntry entry = new RootDirectoryEntry(clusterBeginAddress + i * 32);
						if (entry.getFirstByte() == 0)
							break;
						if (entry.isValid())
							this.add(entry);
					}
					if (cluster.hasNext())
						cluster = cluster.next();
					else
						break;
				};
				if (depth == 0) {
					for(BaseNode i: subList) {
//						System.out.println(i.getName());
						i.dfs(0);
					}
				} else {
					for(BaseNode i: subList) {
						i.dfs(depth - 1);
					}
				}
			}

			public void print(String level) {
				if (first.isDotDirectory())
					return;
//				if (first.isDirectory())
//					System.out.println(level + "In \"" + getName() + "\"");
				for(BaseNode i: subList) {
					System.out.println(level + "\\---" + i.getName());
					i.print(level + "----");
				}
				
//				for(BaseNode i: subList) {
//					i.print(level + "\t");
//				}
			}
		}
		
		public class FileNode extends BaseNode {
			public FileNode(RootDirectoryEntry entry) {
				super(entry);
			}
		}
		
		public class DirNode extends BaseNode {
			BaseNode upperDir;
			public DirNode(RootDirectoryEntry entry) {
				super(entry);
			}
			public void add(RootDirectoryEntry entry) {
//				System.out.println("Got called: add dir , in " + this.getName());
				if (entry.isCurrentDirectory()) {
//					System.out.println("Dot and " + entry.getShortName());
					
				}
				if (entry.isUpperDirectory()) {
//					System.out.println("Dot dot and " + entry.getShortName());
					upperDir = new DirNode(entry);
				}
				super.add(entry);
			}
		}

		public class Cluster{
			long number;
			long nextCluster;
			public Cluster(long clusterNumber) {
				number = clusterNumber;
//				System.out.println("FAT item address " + (Disk.Partition.this.fatBeginAddress + (number * 2)));
				byte[] ft = Disk.this.readByteArray(Disk.Partition.this.fatBeginAddress + number * 2, 2);
				nextCluster = Utils.combineSigned(ft[0], ft[1]);
//				System.out.println("Next cluster " + nextCluster);
//				System.out.println("Has next " + hasNext());
//				System.out.println("Is last " + isLast());
			}
			public boolean hasNext() {
				return nextCluster >= 0x0002 && nextCluster <= 0xffef;
			}
			public boolean isEmpty() {
				return nextCluster == 0;
			}
			public boolean isBad() {
				return nextCluster == 0xfff7;
			}
			public boolean isReserved() {
				return nextCluster >= 0xfff0 && nextCluster <= 0xfff6;
			}
			public boolean isLast() {
				return nextCluster > 0xfff7;
			}
			public byte[] getData() {
				return Disk.this.readByteArray(Disk.Partition.this.dataBeginAddress + (number - 2) * 512, 512);
			}
			public Cluster next() {
				if (hasNext())
					return new Cluster(nextCluster);
				else
					return null;
			}
		}
		public class RootDirectoryEntry {
			byte[] data;
			public RootDirectoryEntry(long address) {
				data = Disk.this.readByteArray(address, 32);
			}
			public boolean isDotDirectory() {
				return isCurrentDirectory() || isUpperDirectory();
			}
			public boolean isCurrentDirectory() {
				return isDirectory() && getShortName().equals(".       ");
			}
			public boolean isUpperDirectory() {
				return isDirectory() && getShortName().equals("..      ");
			}
			public RootDirectoryEntry(byte[] data) {
				this.data = data;
			}
			public byte getFirstByte() {
				return data[0];
			}
			public void setFirstByte(byte newFirst) {
				data[0] = newFirst;
			}
			public String getShortName() {
				return new String(data, 0, 8);
			}
			public String getExtension() {
				return new String(data, 8, 3);
			}
			public String getFullName() {
				return getShortName().trim() + ((isValid() && !isDirectory() && !getExtension().equals("   ")) ? "." + getExtension() : "");
			}
			public long getAttribute() {
				return Utils.combineSigned(data[11]);
			}
			public boolean isValid() {
				byte b = getFirstByte(); 
				return !isVolume() && b != 0x0f && (b & 0xff) != 0xe5 & b != 0x00; 
			}
			public boolean isDirectory() {
				return (data[11] & 0b10000) != 0; 
			}
			
			public boolean isHidden() {
				return (data[11] & 0b10) != 0;
			}
			
			public boolean isSystem() {
				return (data[11] & 0b100) != 0;
			}
			
			public boolean isVolume() {
				return (data[11] & 0b1000) != 0;
			}
			
			public boolean archive() {
				return (data[11] & 0b100000) != 0;
			}
			public boolean isReadOnly() {
				return (data[11] & 0b1) != 0;
			}
			public long getTime() {
				return Utils.combineSigned(data[22], data[23]);
			}
			public long getDate() {
				return Utils.combineSigned(data[24], data[25]);
			}
			public long getCluster() {
				return Utils.combineSigned(data[26], data[27]);
			}
			public long getSize() {
				return Utils.combineSigned(data[28], data[29], data[30], data[31]);
			}
		}
	}
}
