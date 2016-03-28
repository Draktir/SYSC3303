package utils;

import java.net.DatagramPacket;

import configuration.Configuration;

/**
 * Util class containing a packet print method to print a DatagramPacket
 *
 */

public class PacketPrinter {
	public static void print(DatagramPacket packet) {
		if (Configuration.get().VERBOSE) {
			byte[] data = new byte[packet.getLength()];
			System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
			String contents = new String(data);

			System.out.println("\n-------------------------------------------");
			System.out.println("\tAddress: " + packet.getAddress());
			System.out.println("\tPort: " + packet.getPort());
			System.out.println("\tPacket contents: ");
			System.out.println("\t" + contents.replaceAll("\n", "\t\n"));

			System.out.println("\tPacket contents (bytes): ");
			System.out.print("\t");
			for (int i = 0; i < data.length; i++) {
				System.out.print(data[i] + " ");
			}
			System.out.println("\n-------------------------------------------\n");
		}

	}
}