package com.htc.gc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class GCDeviceControllerHelper {
	private final static int MAIN_CODE_VERSION_DATA_LENGTH		= Short.SIZE / Byte.SIZE;
	private final static int MAIN_CODE_MAJOR_VERSION_OFFSET 	= 13;
	private final static int MAIN_CODE_MINOR_VERSION_OFFSET 	= 15;
	
	private final static int BOOT_CODE_VERSION_DATA_LENGTH				= Short.SIZE / Byte.SIZE;
	private final static int BOOT_CODE_MAJOR_VERSION_REVERSE_OFFSET 	= 12;
	private final static int BOOT_CODE_MINOR_VERSION_REVERSE_OFFSET		= 10; 
			
	private final static int MCU_VERSION_DATA_LENGTH 	= 1;
	private final static int MCU_VERSION_OFFSET			= 3;
	
	private static InputStream in = null;
	public static int getMainCodeFileVersion(File firmware) throws Exception {
		int majorVer = 0;
		int minorVer = 0;
		
		try {
			in = new BufferedInputStream(new FileInputStream(firmware));
			ByteBuffer buf = ByteBuffer.allocate(MAIN_CODE_VERSION_DATA_LENGTH);
			buf.order(ByteOrder.LITTLE_ENDIAN);
			
			buf.position(0);
			in.skip(MAIN_CODE_MAJOR_VERSION_OFFSET - 1);
			in.read(buf.array());
			majorVer = buf.getChar(); // unsigned short
			
			buf.position(0);
			in.skip(MAIN_CODE_MINOR_VERSION_OFFSET - MAIN_CODE_MAJOR_VERSION_OFFSET - MAIN_CODE_VERSION_DATA_LENGTH);
			in.read(buf.array());
			minorVer = buf.getChar(); // unsigned short
			
			return majorVer * 10000 + minorVer;
		} catch(Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if(in != null) {
				in.close();
			}
		}
	}
	
	public static int getBootCodeFileVersion(File firmware) throws Exception {
		int majorVer = 0;
		int minorVer = 0;
		
		try {
			long totalLength = firmware.length();
			in = new BufferedInputStream(new FileInputStream(firmware));
			ByteBuffer buf = ByteBuffer.allocate(BOOT_CODE_VERSION_DATA_LENGTH);
			buf.order(ByteOrder.LITTLE_ENDIAN);
			
			buf.position(0);
			in.skip(totalLength - BOOT_CODE_MAJOR_VERSION_REVERSE_OFFSET);
			in.read(buf.array());
			majorVer = buf.getChar();
			
			buf.position(0);
			in.skip(BOOT_CODE_MAJOR_VERSION_REVERSE_OFFSET - BOOT_CODE_MINOR_VERSION_REVERSE_OFFSET - BOOT_CODE_VERSION_DATA_LENGTH);
			in.read(buf.array());
			minorVer = buf.getChar();
			
			return majorVer * 10000 + minorVer;
		} catch(Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if(in != null) {
				in.close();
			}
		}
	}
	
	public static int getMcuFileVersion(File firmware) throws Exception {
		in = new BufferedInputStream(new FileInputStream(firmware));
		
		ByteBuffer buf = ByteBuffer.allocate(MCU_VERSION_DATA_LENGTH);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.position(0);
		in.skip(MCU_VERSION_OFFSET - 1);
		in.read(buf.array());
		
		return buf.get();
	}
}
