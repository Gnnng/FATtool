import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.ArrayList;

class Disk extends RandomAccessFile{
	public Disk(String filepath, String mode) throws FileNotFoundException {
		super(filepath, mode);
	}
}

public class FATmain {
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		Disk disk = new Disk("F:\\v2.vhd", "r");
		disk.skipBytes(446);
		
		// read 4 partitions
		byte[] partitionEntry1 = new byte[16];
		byte[] partitionEntry2 = new byte[16];
		byte[] partitionEntry3 = new byte[16];
		byte[] partitionEntry4 = new byte[16];
		disk.read(partitionEntry1);
		disk.read(partitionEntry2);
		disk.read(partitionEntry3);
		disk.read(partitionEntry4);
		int MBREndTag = disk.readUnsignedShort();
		if (MBREndTag != 0x55aa) {
			throw new Exception("Wrong MBR: should end with 0x55aa");
		}
		
		// only deal with partition entry #1
		byte[] pe1 = partitionEntry1;
//		System.out.println(pe1[8]);
		// simple parse
		int partitionTypeCode = pe1[4];
		int lbaBegin = pe1[8] + (pe1[9] << 8) + (pe1[10] << 16) + (pe1[11] << 24);
		System.out.println("First partition begins at sector #" + lbaBegin);
		int lbaBeginAddress = lbaBegin * 512;
		
		
		// deal with partition #1
		disk.seek((long) lbaBeginAddress);
		byte[] BootRecord = new byte[512]; // first sector of partition #1
		disk.read(BootRecord);
		
//		DataInputStream BR = new DataInputStream(((InputStream) new ByteArrayInputStream(BootRecord)));
//		BR.skip(11); // skip jmp code
		
		String oemName = new String(BootRecord, 3, 8);
		int bytesPerSector = BootRecord[11] + (BootRecord[12] << 8);
		int sectorsPerCluster = BootRecord[13];
		int reservedSectors = BootRecord[14] + (BootRecord[15] << 8);
		int numberOfFAT = BootRecord[16];
		int maxRootDirectoryEntries = BootRecord[0x11] + (BootRecord[0x12] << 8);
		int numberOfSectorsInPartition = BootRecord[0x13] + (BootRecord[0x14] << 8);
		int sectorsPerFAT = BootRecord[0x16] + (BootRecord[0x17] << 8);
		int numberOfHiddenSectors = BootRecord[0x1c] + (BootRecord[0x1d] << 8) + (BootRecord[0x1e] << 16) + (BootRecord[0x1f] << 24);
//		int numberOfSectorsInPartition = BootRecord[0x20] + (BootRecord[0x21] << 8) + (BootRecord[0x22] << 16) + (BootRecord[0x23] << 24);
		String volumeName = new String(BootRecord, 0x2b, 11);		String fatName = new String(BootRecord, 0x36, 8);
		
		System.out.println("OEM name is \"" + oemName + "\"");
		System.out.println("bytes per sector " + bytesPerSector);
		System.out.println("reserved sectors " + reservedSectors);
		System.out.println("number of FAT " + numberOfFAT);
		System.out.println("sectors per FAT " + sectorsPerFAT);
		System.out.println(maxRootDirectoryEntries);
		System.out.println(numberOfHiddenSectors);
		System.out.println(volumeName);
		System.out.println(numberOfSectorsInPartition * 512 / 1024 + "KB");
		
		int fatBegin = lbaBegin + reservedSectors;
		int fatBeginAddress = fatBegin * 512; 
		System.out.println("FAT begins at " + Integer.toHexString(fatBeginAddress));
		
		int directoryBegin = lbaBegin + reservedSectors + numberOfFAT * sectorsPerFAT;
		int directoryBeginAddress = directoryBegin * 512;
		System.out.println("Directory begins at " + Integer.toHexString(directoryBeginAddress));
		
		int dataBegin = directoryBegin + maxRootDirectoryEntries * 32 / 512;
		int dataBeginAddress = dataBegin * 512;
		System.out.println("Cluster #2 begins at " + Integer.toHexString(dataBeginAddress));

	}
}
