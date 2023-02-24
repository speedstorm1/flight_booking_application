package flightapp;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;


/**
 * A collection of utility methods to help with managing passwords
 */
public class PasswordUtils {
  /**
   * Generates a cryptographically-secure salted password.
   */
  public static byte[] hashPassword(String password) {
    byte[] salt = generateSalt();
    byte[] saltedHash = generateSaltedPassword(password, salt);
    byte[] returnArray = new byte[salt.length + saltedHash.length];
    for (int i = 0; i < salt.length; i++) {
      returnArray[i] = salt[i];
    }
    for (int i = salt.length; i < returnArray.length; i++) {
      returnArray[i] = saltedHash[i - salt.length];
    }
    return returnArray;
  }

  /**
   * Verifies whether the plaintext password can be hashed to provided salted hashed password.
   */
  public static boolean plaintextMatchesHash(String plaintext, byte[] saltedHashed) {
    // TODO: extract the salt from the byte array (i.e., undo the logic implemented in 
    // hashPassword), then use the salt to salt the plaintext and check whether
    // it matches the password hash.
    byte[] salt = new byte[SALT_LENGTH];
    for (int i = 0; i < salt.length; i++) {
      salt[i] = saltedHashed[i];
    }
    byte[] newSaltedHash = generateSaltedPassword(plaintext, salt);
    for (int i = salt.length; i < saltedHashed.length; i++) {
      if (saltedHashed[i] != newSaltedHash[i - salt.length]) {
        return false;
      }
    }
//    byte[] newSaltedHash = ge
    return true;
  }
  
  // Password hashing parameter constants.
  private static final int HASH_STRENGTH = 65536;
  private static final int KEY_LENGTH = 128;
  private static final int SALT_LENGTH = 16;

  /**
   * Generate a small bit of randomness to serve as a password "salt"
   */
  static byte[] generateSalt() {
    // TODO: implement this.
    byte[] salt = new byte[SALT_LENGTH];
    Random r = new Random();
    r.nextBytes(salt);
    return salt;
  }

  /**
   * Uses the provided salt to generate a cryptographically-secure hash of the provided password.
   * The resultant byte array should be KEY_LENGTH bytes long.
   */
  static byte[] generateSaltedPassword(String password, byte[] salt)
    throws IllegalStateException {
    // Specify the hash parameters, including the salt
    KeySpec spec = new PBEKeySpec(password.toCharArray(), salt,
                                  HASH_STRENGTH, KEY_LENGTH * 8 /* length in bits */);

    // Hash the whole thing
    SecretKeyFactory factory = null;
    byte[] hash = null; 
    try {
      factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      hash = factory.generateSecret(spec).getEncoded();
      return hash;
    } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
      throw new IllegalStateException();
    }
  }

}
