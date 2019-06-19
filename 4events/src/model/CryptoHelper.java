package model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CryptoHelper {
    /**
     * Hashes a password with SHA512 and given salt
     * @param password A char array to hash
     * @param salt A byte array to salt the password with
     * @return A String result of salt + hashing operation
     */
    public static String SHA512PasswordHash(char[] password, byte[] salt) {
        byte[] byteArrayPassword = charArrayToByteArray(password);
        String generatedPassword = null; // Just to shut the compiler up, this variable WILL be initialized once we return
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(salt);
            byte[] bytes = md.digest(byteArrayPassword);
            StringBuilder sb = new StringBuilder();
            for(int i=0; i < bytes.length; i++)
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            generatedPassword = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("ALERT: Missing hashing algorithm SHA-512");
            e.printStackTrace();
        }
        java.util.Arrays.fill(byteArrayPassword, (byte)0x00); // It will still be somewhere in memory due to Java's Almighty Garbage Collector (TM), but at least we tried.
        return generatedPassword;
    }

    /**
     * Converts a char array to byte array to be used with Java hashing methods
     * @param charArray char array
     * @return A byte array representing our chars
     */
    public static byte[] charArrayToByteArray(char[] charArray) {
        byte[] byteArray = new byte[charArray.length];
        for(int i= 0; i < charArray.length; i++) {
            byteArray[i] = (byte)(0xFF & (int)charArray[i]);
        }
        return byteArray;
    }
}
