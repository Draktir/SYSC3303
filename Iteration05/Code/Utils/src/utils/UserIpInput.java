package utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserIpInput {
	public static InetAddress get(Scanner scan) throws UnknownHostException {
		byte[] ipBytes = null;

		do {
			System.out.print("\nEnter server IP (or press Enter to use localhost): ");
			String userInput = scan.nextLine();

			if (userInput.length() == 0) {
				ipBytes = new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 1 };
			} else {
				Pattern ipPattern = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
				Matcher m = ipPattern.matcher(userInput);

				if (!m.matches() || m.groupCount() != 4) {
					System.out.println("\nInvalid IP. Please enter a valid IP, e.g. 192.168.0.100");
				} else {
					ipBytes = new byte[4];

					for (int i = 1; i <= 4; i++) {
						Integer val = Integer.parseInt(m.group(i));
						if (val > 255 || val < 0) {
							ipBytes = null;
							System.out.println("Invalid IP. Valid range for each octet is 0 to 255, instead found " + val);
							break;
						}
						ipBytes[i - 1] = val.byteValue();
					}
				}

			}
		} while (ipBytes == null);

		return InetAddress.getByAddress(ipBytes);
	}
}
